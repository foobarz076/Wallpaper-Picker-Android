package foo.barz.wallpaperpicker.core.applier

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles applying bitmaps to the system wallpaper, with backward compatibility for Android 6.0.
 */
class WallpaperApplier(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)

    suspend fun apply(bitmap: Bitmap, target: WallpaperTarget): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val whichFlag = when (target) {
                    WallpaperTarget.SYSTEM -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, whichFlag)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            Unit
        }.also {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
