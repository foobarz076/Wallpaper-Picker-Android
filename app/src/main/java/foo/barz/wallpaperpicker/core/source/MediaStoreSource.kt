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
    private val bucketId: String? = null,
    private val albumName: String? = null
) : WallpaperSource {

    override val id: String = "media_store"
    override val displayName: String = albumName?.let { "系统相册 ($it)" } ?: "系统相册"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
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

            var (selection, selectionArgs) = if (bucketId != null) {
                "${MediaStore.Images.Media.MIME_TYPE} LIKE ? AND ${MediaStore.Images.Media.BUCKET_ID} = ?" to
                        arrayOf("image/%", bucketId)
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

            WallpaperData(
                openStream = {
                    resolver.openInputStream(photoUri)
                        ?: throw FileNotFoundException("无法打开系统相册图片流: $photoUri")
                },
                title = photoName,
                sourceUri = photoUri
            )
        }
    }

    companion object {
        /**
         * Queries all image buckets/albums from the system MediaStore.
         */
        suspend fun fetchAlbums(context: Context): List<MediaStoreAlbum> = withContext(Dispatchers.IO) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return@withContext emptyList()
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("image/%")

            val albumMap = mutableMapOf<String, Pair<String, Int>>()
            var totalCount = 0

            try {
                resolver.query(collectionUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        totalCount++
                        val bucketId = cursor.getString(idCol) ?: continue
                        val bucketName = cursor.getString(nameCol) ?: "未命名相册"
                        val current = albumMap[bucketId]
                        albumMap[bucketId] = if (current != null) {
                            current.first to (current.second + 1)
                        } else {
                            bucketName to 1
                        }
                    }
                }
            } catch (_: SecurityException) {
                return@withContext emptyList()
            }

            val list = mutableListOf<MediaStoreAlbum>()
            list.add(MediaStoreAlbum(id = null, name = "全部照片", count = totalCount))

            albumMap.entries
                .sortedByDescending { it.value.second }
                .forEach { (id, pair) ->
                    list.add(MediaStoreAlbum(id = id, name = pair.first, count = pair.second))
                }

            list
        }
    }
}
