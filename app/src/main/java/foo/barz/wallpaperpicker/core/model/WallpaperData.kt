package foo.barz.wallpaperpicker.core.model

import android.net.Uri
import java.io.InputStream

/**
 * Standard data model representing a wallpaper fetched from any source.
 */
data class WallpaperData(
    val openStream: () -> InputStream,
    val title: String? = null,
    val sourceUri: Uri? = null,
    val sourceType: WallpaperSourceType? = null,
    val sourceTitle: String? = null,
    val remoteUrl: String? = null
)

/**
 * Target screen where the wallpaper should be applied.
 */
enum class WallpaperTarget {
    SYSTEM, // Home screen only
    LOCK,   // Lock screen only
    BOTH    // Both home and lock screens
}
