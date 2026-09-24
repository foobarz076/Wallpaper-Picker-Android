package foo.barz.wallpaperpicker.core.source

import foo.barz.wallpaperpicker.core.model.WallpaperData

/**
 * Universal abstraction for wallpaper providers (Local, Immich, HTTP API, etc.).
 */
interface WallpaperSource {
    val id: String
    val displayName: String

    /**
     * Retrieve the next wallpaper.
     * Returns Result.success(WallpaperData) on success, or Result.failure on error.
     */
    suspend fun getNextWallpaper(): Result<WallpaperData>
}
