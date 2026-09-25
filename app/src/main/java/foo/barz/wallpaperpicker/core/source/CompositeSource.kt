package foo.barz.wallpaperpicker.core.source

import android.content.Context
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composite wallpaper source that blends multiple active sources together.
 * Randomly delegates wallpaper selection across all supplied source candidates with fallback resilience.
 */
class CompositeSource(
    private val sources: List<WallpaperSource>
) : WallpaperSource {

    override val id: String = "composite"
    override val displayName: String = "多源混合"

    constructor(
        context: Context,
        prefs: PreferencesManager,
        bypassNetworkConstraints: Boolean = false
    ) : this(
        WallpaperSourceFactory.createEnabledSources(context, prefs, bypassNetworkConstraints)
    )

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        if (sources.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("未配置任何用于混合轮播的图源"))
        }

        // Shuffle sources and attempt each candidate until one succeeds
        val shuffled = sources.shuffled()
        var lastError: Throwable? = null

        for (source in shuffled) {
            val result = source.getNextWallpaper()
            if (result.isSuccess) {
                return@withContext result
            } else {
                lastError = result.exceptionOrNull()
            }
        }

        Result.failure(lastError ?: IllegalStateException("所有已启用的图源均未能提供有效壁纸"))
    }
}
