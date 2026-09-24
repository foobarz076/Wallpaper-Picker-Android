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
    val cropFocusY: Float? = null
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
}
