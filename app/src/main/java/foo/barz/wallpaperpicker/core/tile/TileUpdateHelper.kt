package foo.barz.wallpaperpicker.core.tile

import android.content.Context
import android.os.Build

/**
 * Safe helper to trigger Quick Settings Tile updates without triggering class verification
 * or loading of [android.service.quicksettings.TileService] on Android versions below API 24 (Nougat).
 */
object TileUpdateHelper {

    /**
     * Requests the system to refresh the Next Wallpaper tile state, if supported by the OS (Android 7.0+).
     */
    fun requestNextWallpaperTileUpdate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NextWallpaperTileService.requestUpdate(context)
        }
    }

    /**
     * Requests the system to refresh the Toggle Schedule tile state, if supported by the OS (Android 7.0+).
     */
    fun requestToggleScheduleTileUpdate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ToggleScheduleTileService.requestUpdate(context)
        }
    }
}
