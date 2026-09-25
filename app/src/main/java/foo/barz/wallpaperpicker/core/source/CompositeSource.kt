package foo.barz.wallpaperpicker.core.source

import android.content.Context
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composite wallpaper source that blends multiple active sources together.
 * Randomly delegates wallpaper selection across all user-enabled source candidates.
 */
class CompositeSource(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val bypassNetworkConstraints: Boolean = false
) : WallpaperSource {

    override val id: String = "composite"
    override val displayName: String = "多源混合"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            val enabledTypes = prefs.compositeEnabledSources
                .filter { it != WallpaperSourceType.COMPOSITE }

            if (enabledTypes.isEmpty()) {
                throw IllegalStateException("未勾选任何用于混合轮播的图源，请在图源设置中至少启用一个图源")
            }

            // Shuffle available sources and attempt each candidate until one succeeds
            val shuffledTypes = enabledTypes.shuffled()
            var lastError: Throwable? = null

            for (type in shuffledTypes) {
                val source = try {
                    WallpaperSourceFactory.createSourceByType(context, prefs, type, bypassNetworkConstraints)
                } catch (e: Exception) {
                    lastError = e
                    continue
                }

                val result = source.getNextWallpaper()
                if (result.isSuccess) {
                    return@runCatching result.getOrThrow()
                } else {
                    lastError = result.exceptionOrNull()
                }
            }

            throw lastError ?: IllegalStateException("未能从任何已启用的图源中获取到有效壁纸")
        }
    }
}
