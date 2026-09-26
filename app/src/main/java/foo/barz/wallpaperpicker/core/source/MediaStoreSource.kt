package foo.barz.wallpaperpicker.core.source

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import kotlin.random.Random

/**
 * Wallpaper source that indexes and retrieves wallpapers directly from Android's system MediaStore.
 * Supports whole-gallery random selection as well as filtering by specific album / bucket (e.g., Camera).
 */
class MediaStoreSource(
    private val context: Context,
    private val bucketIds: Set<String> = emptySet(),
    private val albumName: String? = null
) : WallpaperSource {

    constructor(context: Context, bucketId: String?, albumName: String?) : this(
        context = context,
        bucketIds = bucketId?.let { setOf(it) } ?: emptySet(),
        albumName = albumName
    )

    override val id: String = "media_store"
    override val displayName: String = albumName?.let { "系统相册 ($it)" } ?: "系统相册"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasAnyPermission(context)) {
                throw SecurityException("缺少读取相册权限，请先授权")
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
            )

            val prefs = foo.barz.wallpaperpicker.data.PreferencesManager(context)
            val excludedUris = if (prefs.fairShuffle) prefs.getRecentWallpaperKeys().toSet() else emptySet()
            val excludedIds = excludedUris.mapNotNull {
                runCatching { ContentUris.parseId(Uri.parse(it)) }.getOrNull()
            }

            var (selection, selectionArgs) = if (bucketIds.isNotEmpty()) {
                val placeholders = bucketIds.joinToString(",") { "?" }
                "${MediaStore.Images.Media.MIME_TYPE} LIKE ? AND ${MediaStore.Images.Media.BUCKET_ID} IN ($placeholders)" to
                        (arrayOf("image/%") + bucketIds.toTypedArray())
            } else {
                "${MediaStore.Images.Media.MIME_TYPE} LIKE ?" to
                        arrayOf("image/%")
            }

            var cursor: Cursor? = null

            // 1. First attempt: Query with SQL RANDOM() LIMIT 1, excluding recent IDs if available
            if (excludedIds.isNotEmpty()) {
                val placeholders = excludedIds.joinToString(",") { "?" }
                val shuffleSelection = "$selection AND ${MediaStore.Images.Media._ID} NOT IN ($placeholders)"
                val shuffleArgs = selectionArgs + excludedIds.map { it.toString() }.toTypedArray()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val queryArgs = Bundle().apply {
                            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, shuffleSelection)
                            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, shuffleArgs)
                            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "RANDOM()")
                            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 1)
                        }
                        cursor = resolver.query(collectionUri, projection, queryArgs, null)
                    } else {
                        cursor = resolver.query(collectionUri, projection, shuffleSelection, shuffleArgs, "RANDOM() LIMIT 1")
                    }
                } catch (_: Exception) {
                    cursor?.close()
                    cursor = null
                }
            }

            // 2. If no cursor or shuffle query was empty (deck exhausted), query with standard selection
            if (cursor == null || cursor.count == 0) {
                cursor?.close()
                cursor = null
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val queryArgs = Bundle().apply {
                            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "RANDOM()")
                            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 1)
                        }
                        cursor = resolver.query(collectionUri, projection, queryArgs, null)
                    } else {
                        cursor = resolver.query(collectionUri, projection, selection, selectionArgs, "RANDOM() LIMIT 1")
                    }
                } catch (_: Exception) {
                    cursor?.close()
                    cursor = null
                }
            }

            // 2. Fallback: Query all IDs and pick a random position in the cursor
            val chosenData: Pair<Uri, String?> = if (cursor != null && cursor.moveToFirst()) {
                cursor.use {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    ContentUris.withAppendedId(collectionUri, id) to displayName
                }
            } else {
                cursor?.close()
                resolver.query(collectionUri, projection, selection, selectionArgs, null)?.use { fallbackCursor ->
                    val total = fallbackCursor.count
                    if (total == 0) {
                        throw NoSuchElementException("系统相册中未找到图片")
                    }
                    val randomIndex = Random.nextInt(total)
                    fallbackCursor.moveToPosition(randomIndex)
                    val id = fallbackCursor.getLong(fallbackCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val displayName = fallbackCursor.getString(fallbackCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    ContentUris.withAppendedId(collectionUri, id) to displayName
                } ?: throw NoSuchElementException("无法查询系统相册")
            }

            val (photoUri, photoName) = chosenData

            val concreteTitle = if (!albumName.isNullOrBlank()) albumName else "系统相册"
            WallpaperData(
                openStream = {
                    resolver.openInputStream(photoUri)
                        ?: throw FileNotFoundException("无法打开系统相册图片流: $photoUri")
                },
                title = photoName,
                sourceUri = photoUri,
                sourceType = foo.barz.wallpaperpicker.core.model.WallpaperSourceType.MEDIA_STORE,
                sourceTitle = concreteTitle
            )
        }
    }

    companion object {
        /**
         * Returns permissions required to request photo access based on Android OS version.
         * On Android 14+ (API 34+), includes READ_MEDIA_VISUAL_USER_SELECTED for Selected Photos Access.
         */
        fun getRequiredPermissions(): Array<String> {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
                else -> arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

        /**
         * Checks whether the app has full access to the device photo library.
         */
        fun hasFullPermission(context: Context): Boolean {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                }
                else -> {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
            }
        }

        /**
         * Checks whether the user granted partial photo access (Selected Photos Access) on Android 14+.
         */
        fun hasPartialPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                !hasFullPermission(context) &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }

        /**
         * Checks whether the app can read at least some photos from MediaStore (full or partial).
         */
        fun hasAnyPermission(context: Context): Boolean {
            return hasFullPermission(context) || hasPartialPermission(context)
        }

        /**
         * Queries all image buckets/albums from the system MediaStore.
         */
        suspend fun fetchAlbums(context: Context): List<MediaStoreAlbum> = withContext(Dispatchers.IO) {
            if (!hasAnyPermission(context)) {
                return@withContext emptyList()
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("image/%")
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            val albumMap = mutableMapOf<String, Triple<String, Int, Uri?>>()
            var totalCount = 0
            var firstOverallCover: Uri? = null

            try {
                resolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val imageIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                    while (cursor.moveToNext()) {
                        val imageId = cursor.getLong(imageIdCol)
                        val imageUri = ContentUris.withAppendedId(collectionUri, imageId)
                        if (firstOverallCover == null) {
                            firstOverallCover = imageUri
                        }

                        totalCount++
                        val bucketId = cursor.getString(idCol) ?: continue
                        val bucketName = cursor.getString(nameCol) ?: "未命名相册"
                        val current = albumMap[bucketId]
                        albumMap[bucketId] = if (current != null) {
                            Triple(current.first, current.second + 1, current.third)
                        } else {
                            Triple(bucketName, 1, imageUri)
                        }
                    }
                }
            } catch (_: SecurityException) {
                return@withContext emptyList()
            }

            val list = mutableListOf<MediaStoreAlbum>()
            list.add(MediaStoreAlbum(id = null, name = "全部照片", count = totalCount, coverUri = firstOverallCover))

            albumMap.entries
                .sortedByDescending { it.value.second }
                .forEach { (id, triple) ->
                    list.add(MediaStoreAlbum(id = id, name = triple.first, count = triple.second, coverUri = triple.third))
                }

            list
        }
    }
}
