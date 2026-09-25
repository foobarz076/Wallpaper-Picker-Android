package foo.barz.wallpaperpicker.core.source

import android.content.Context
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Wallpaper source that draws exclusively from the user's permanent favorites.
 * Supports Fair Shuffle exclusion and automatic skipping of deleted/broken local originals.
 */
class FavoritesSource(
    private val context: Context,
    private val database: WallpaperHistoryDatabase
) : WallpaperSource {

    override val id: String = "favorites"
    override val displayName: String = "我的收藏"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            val favorites = database.getFavoritesList()
            if (favorites.isEmpty()) {
                throw IllegalStateException("尚未添加任何收藏壁纸，请先在历史或控制台中点击爱心进行收藏")
            }

            val prefs = PreferencesManager(context)
            val excludedKeys = if (prefs.fairShuffle) prefs.getRecentWallpaperKeys().toSet() else emptySet()

            // Filter out recently applied items when pool size permits
            val candidates = if (excludedKeys.isNotEmpty() && favorites.size > excludedKeys.size) {
                val filtered = favorites.filter { item ->
                    !excludedKeys.contains(item.sourceUri) &&
                        (item.favoriteFilePath == null || !excludedKeys.contains(item.favoriteFilePath))
                }
                filtered.ifEmpty { favorites }
            } else {
                favorites
            }

            // Shuffle candidates and attempt to open each until a valid one is found
            val shuffled = candidates.shuffled()
            for (item in shuffled) {
                val uri = item.displayUri
                val isUsable = runCatching {
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        file.exists() && file.canRead() && file.length() > 0L
                    } else {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                    }
                }.getOrDefault(false)

                if (isUsable) {
                    return@runCatching WallpaperData(
                        openStream = {
                            if (uri.scheme == "file") {
                                FileInputStream(File(uri.path!!))
                            } else {
                                context.contentResolver.openInputStream(uri)
                                    ?: throw FileNotFoundException("无法读取收藏图片: $uri")
                            }
                        },
                        title = item.title ?: "收藏壁纸 #${item.id}",
                        sourceUri = uri
                    )
                }
            }

            throw IllegalStateException("所有收藏壁纸均已失效或原图已被删除")
        }
    }
}
