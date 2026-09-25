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
enum class WallpaperTarget(val label: String) {
    SYSTEM("仅桌面"), // Home screen only
    LOCK("仅锁屏"),   // Lock screen only
    BOTH("桌面与锁屏")    // Both home and lock screens
}
