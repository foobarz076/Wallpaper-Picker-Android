package foo.barz.wallpaperpicker.core.source

import android.content.Context
import android.net.Uri
import foo.barz.wallpaperpicker.core.model.CustomPhotosSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.util.AppLog
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Wallpaper source consisting of user-curated photos imported via PhotoPicker.
 * All selected images are copied to persistent internal storage (filesDir) and isolated
 * from standard cache pruning, ensuring zero permission loss and offline reliability.
 */
class CustomPhotosSource(
    private val context: Context,
    val sourceId: String,
    private val sourceTitle: String,
    private val config: CustomPhotosSourceConfig = CustomPhotosSourceConfig()
) : WallpaperSource {

    override val id: String = "custom_photos_$sourceId"
    override val displayName: String = sourceTitle.ifBlank { "自选照片集" }

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            val photos = getPhotos(context, sourceId)
            if (photos.isEmpty()) {
                throw NoSuchElementException("自选照片集中未找到可用图片，请先在图源配置中添加照片")
            }

            val prefs = PreferencesManager(context)
            val excluded = if (prefs.fairShuffle) prefs.getRecentWallpaperKeys().toSet() else emptySet()

            val candidates = if (excluded.isNotEmpty()) {
                photos.filter { !excluded.contains(it.name) && !excluded.contains(Uri.fromFile(it).toString()) }
            } else {
                emptyList()
            }

            val chosen = if (candidates.isNotEmpty()) {
                candidates.random()
            } else {
                photos.random()
            }

            val fileUri = Uri.fromFile(chosen)
            WallpaperData(
                openStream = { FileInputStream(chosen) },
                title = chosen.nameWithoutExtension,
                sourceUri = fileUri,
                sourceType = WallpaperSourceType.CUSTOM_PHOTOS,
                sourceTitle = displayName
            )
        }
    }

    companion object {
        private const val DIRECTORY_NAME = "custom_sources"
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        /**
         * Resolves the dedicated persistent directory for this custom source.
         */
        fun getSourceDirectory(context: Context, sourceId: String): File {
            val root = File(context.filesDir, DIRECTORY_NAME)
            val dir = File(root, sourceId)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

        /**
         * Returns all valid image files currently saved in the source directory.
         */
        fun getPhotos(context: Context, sourceId: String): List<File> {
            val dir = getSourceDirectory(context, sourceId)
            return dir.listFiles { file ->
                file.isFile && file.length() > 0 && file.extension.lowercase() in SUPPORTED_EXTENSIONS
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        }

        /**
         * Imports image streams from picked URIs and persists them into the source directory.
         */
        fun importPhotos(context: Context, sourceId: String, uris: List<Uri>): List<File> {
            val dir = getSourceDirectory(context, sourceId)
            val imported = mutableListOf<File>()

            for (uri in uris) {
                try {
                    val mimeType = context.contentResolver.getType(uri)
                    val ext = when {
                        mimeType?.contains("png") == true -> "png"
                        mimeType?.contains("webp") == true -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext"
                    val targetFile = File(dir, fileName)

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (targetFile.exists() && targetFile.length() > 0) {
                        imported.add(targetFile)
                    }
                } catch (e: Exception) {
                    AppLog.w("CustomPhotosSource", "Failed to import photo from URI: $uri", e)
                }
            }
            return imported
        }

        /**
         * Deletes a specific photo file from the source directory.
         */
        fun deletePhoto(file: File): Boolean {
            return runCatching { file.delete() }.getOrDefault(false)
        }

        /**
         * Deletes all files and the directory for this custom source.
         */
        fun cleanupSourceDirectory(context: Context, sourceId: String) {
            runCatching {
                val dir = File(File(context.filesDir, DIRECTORY_NAME), sourceId)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }.onFailure {
                AppLog.w("CustomPhotosSource", "Failed to cleanup directory for source: $sourceId", it)
            }
        }
    }
}
