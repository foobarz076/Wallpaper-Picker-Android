package foo.barz.wallpaperpicker.core.cache

import android.content.Context
import android.net.Uri
import foo.barz.wallpaperpicker.core.model.WallpaperData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Manages locally cached wallpapers downloaded from network sources.
 * Employs a pure LRU (Least Recently Used) policy bounded by count and storage size.
 */
class WallpaperCacheManager(private val context: Context) {

    private val cacheDirectory: File
        get() = File(context.cacheDir, "wallpapers").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    /**
     * Saves an input stream into the cache directory and triggers LRU pruning.
     * Direct streaming prevents excessive memory usage on low-RAM devices.
     */
    fun saveStream(
        inputStream: InputStream,
        preferredTitle: String? = null,
        extension: String = "jpg"
    ): File {
        val dir = cacheDirectory
        val tempFile = File(dir, "temp_${UUID.randomUUID()}.$extension")
        val targetFile = File(dir, "wp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension")

        FileOutputStream(tempFile).use { out ->
            inputStream.copyTo(out)
        }

        if (tempFile.renameTo(targetFile)) {
            targetFile.setLastModified(System.currentTimeMillis())
        } else {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
            targetFile.setLastModified(System.currentTimeMillis())
        }

        pruneCache()
        return targetFile
    }

    /**
     * Picks a random wallpaper from the cache pool for offline or cellular fallback.
     * Updates the file's last modified timestamp to refresh its LRU standing.
     */
    fun getRandomCachedWallpaper(): WallpaperData? {
        val files = getCachedFiles()
        if (files.isEmpty()) return null

        val targetFile = files.random()
        targetFile.setLastModified(System.currentTimeMillis())

        return WallpaperData(
            openStream = { FileInputStream(targetFile) },
            title = targetFile.name,
            sourceUri = Uri.fromFile(targetFile)
        )
    }

    /**
     * Returns the list of cached wallpaper files.
     */
    fun getCachedFiles(): List<File> {
        val dir = cacheDirectory
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.isFile && !file.name.startsWith("temp_") }?.toList() ?: emptyList()
    }

    /**
     * Calculates the total size of all cached wallpapers in bytes.
     */
    fun getCacheSizeBytes(): Long {
        return getCachedFiles().sumOf { it.length() }
    }

    /**
     * Deletes all cached wallpapers from disk.
     */
    fun clearCache(): Boolean {
        val dir = cacheDirectory
        if (!dir.exists()) return true
        val files = dir.listFiles() ?: return true
        var allDeleted = true
        for (file in files) {
            if (!file.delete()) {
                allDeleted = false
            }
        }
        return allDeleted
    }

    /**
     * Prunes the cache pool according to LRU order (oldest lastModified first).
     */
    fun pruneCache(
        maxCount: Int = DEFAULT_MAX_COUNT,
        maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES
    ) {
        val files = getCachedFiles().sortedBy { it.lastModified() } // Oldest first
        var currentCount = files.size
        var currentSize = files.sumOf { it.length() }

        for (file in files) {
            if (currentCount <= maxCount && currentSize <= maxSizeBytes) {
                break
            }
            val fileSize = file.length()
            if (file.delete()) {
                currentCount--
                currentSize -= fileSize
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_COUNT = 25
        const val DEFAULT_MAX_SIZE_BYTES = 50L * 1024L * 1024L // 50 MB
    }
}
