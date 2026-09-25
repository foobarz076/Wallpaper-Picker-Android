package foo.barz.wallpaperpicker.core.model

import android.net.Uri
import java.io.File

/**
 * Entity representing a historically applied wallpaper and its favorite status.
 */
data class WallpaperHistoryItem(
    val id: Long = 0L,
    val sourceUri: String,
    val title: String?,
    val sourceType: WallpaperSourceType,
    val appliedTimestamp: Long,
    val isFavorite: Boolean = false,
    val favoriteTimestamp: Long? = null,
    val favoriteFilePath: String? = null,
    val customScrollMode: WallpaperScrollMode? = null,
    val cropFocusX: Float? = null,
    val cropFocusY: Float? = null,
    val flipHorizontal: Boolean = false,
    val sourceTitle: String? = null
) {
    /**
     * Resolves the safest URI for image loading.
     * When favorited with a promoted persistent file, returns the permanent file URI.
     */
    val displayUri: Uri
        get() = if (isFavorite && !favoriteFilePath.isNullOrBlank()) {
            val file = File(favoriteFilePath)
            if (file.exists() && file.length() > 0) {
                Uri.fromFile(file)
            } else {
                Uri.parse(sourceUri)
            }
        } else {
            Uri.parse(sourceUri)
        }

    /**
     * Returns a concise badge name representing the concrete source (e.g., "Exports_jpg", "相机", "我的收藏").
     * Falls back to intelligent URI inference for legacy records.
     */
    val displaySourceBadge: String
        get() {
            if (!sourceTitle.isNullOrBlank()) {
                return sourceTitle
            }
            return inferSourceTitleFromUri(sourceUri, sourceType)
        }

    /**
     * Returns a descriptive source label suitable for the detail sheet (e.g., "Exports_jpg (本地文件夹)").
     */
    val displaySourceDetail: String
        get() {
            val badge = displaySourceBadge
            return when (sourceType) {
                WallpaperSourceType.LOCAL_FOLDER -> if (badge != "本地文件夹") "$badge (本地文件夹)" else badge
                WallpaperSourceType.MEDIA_STORE -> if (badge != "系统相册") "$badge (系统相册)" else badge
                WallpaperSourceType.IMMICH -> if (!badge.startsWith("Immich")) "Immich · $badge" else badge
                WallpaperSourceType.HTTP_API -> if (!badge.contains("HTTP") && !badge.contains("网络") && !badge.contains("Bing") && !badge.contains("Picsum")) "$badge (网络壁纸)" else badge
                WallpaperSourceType.FAVORITES -> "我的收藏"
                WallpaperSourceType.COMPOSITE -> badge
            }
        }

    companion object {
        /**
         * Infers a user-friendly source title from the source URI and recorded source type.
         */
        fun inferSourceTitleFromUri(sourceUri: String, sourceType: WallpaperSourceType): String {
            val folderName = extractFolderNameFromUri(sourceUri)
            if (!folderName.isNullOrBlank()) {
                return folderName
            }
            return when {
                sourceUri.contains("content://media/") -> "系统相册"
                sourceUri.contains("/cache/wallpapers") && sourceType == WallpaperSourceType.IMMICH -> "Immich"
                sourceUri.contains("/cache/wallpapers") && sourceType == WallpaperSourceType.HTTP_API -> "网络壁纸"
                sourceUri.startsWith("http://") || sourceUri.startsWith("https://") -> "网络壁纸"
                sourceType == WallpaperSourceType.FAVORITES || sourceUri.contains("/files/favorites") -> "我的收藏"
                sourceType == WallpaperSourceType.LOCAL_FOLDER -> "本地文件夹"
                sourceType == WallpaperSourceType.MEDIA_STORE -> "系统相册"
                sourceType == WallpaperSourceType.IMMICH -> "Immich"
                sourceType == WallpaperSourceType.HTTP_API -> "网络壁纸"
                sourceType == WallpaperSourceType.COMPOSITE -> "混合轮播"
                else -> sourceType.displayName
            }
        }

        /**
         * Extracts folder name from tree documents or storage file URIs.
         */
        fun extractFolderNameFromUri(uriString: String): String? {
            val decoded = runCatching {
                java.net.URLDecoder.decode(uriString.replace("+", "%2B"), "UTF-8")
            }.getOrElse {
                runCatching { Uri.decode(uriString) }.getOrDefault(uriString)
            }
            if (decoded.contains("/cache/wallpapers")) return null
            if (decoded.contains("content://media/")) return null

            val fullPath = when {
                decoded.contains("/document/") -> decoded.substringAfter("/document/").substringAfterLast(':')
                decoded.contains("/tree/") -> decoded.substringAfter("/tree/").substringAfterLast(':')
                decoded.startsWith("file://") -> decoded.removePrefix("file://")
                decoded.startsWith("/") -> decoded
                else -> null
            } ?: return null

            val dirPath = fullPath.substringBeforeLast('/', "").substringAfterLast(':')
            if (dirPath.isBlank()) return null
            val folderName = dirPath.substringAfterLast('/')
            val ignored = setOf("primary", "documents", "tree", "0", "emulated", "storage")
            return if (folderName.isNotBlank() && !ignored.contains(folderName.lowercase())) {
                folderName
            } else {
                null
            }
        }
    }
}
