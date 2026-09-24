package foo.barz.wallpaperpicker.core.source

import android.content.Context
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.model.HttpApiConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * Factory for creating the active WallpaperSource based on user preferences.
 */
object WallpaperSourceFactory {

    fun createActiveSource(
        context: Context,
        prefs: PreferencesManager,
        bypassNetworkConstraints: Boolean = false
    ): WallpaperSource {
        return when (prefs.sourceType) {
            WallpaperSourceType.LOCAL_FOLDER -> {
                val folderUri = prefs.folderUri
                    ?: throw IllegalStateException("未选择壁纸文件夹，请先授权选择文件夹")
                LocalFolderSource(context, folderUri)
            }
            WallpaperSourceType.MEDIA_STORE -> {
                MediaStoreSource(
                    context = context,
                    bucketId = prefs.mediaStoreAlbumId,
                    albumName = prefs.mediaStoreAlbumName
                )
            }
            WallpaperSourceType.HTTP_API -> {
                val config = HttpApiConfig(
                    presetType = prefs.httpPresetType,
                    customUrl = prefs.httpCustomUrl,
                    customJsonPath = prefs.httpCustomJsonPath,
                    wifiOnly = prefs.wifiOnly
                )
                val cacheManager = WallpaperCacheManager(context)
                HttpApiSource(
                    context = context,
                    config = config,
                    cacheManager = cacheManager,
                    bypassNetworkConstraints = bypassNetworkConstraints
                )
            }
            WallpaperSourceType.IMMICH -> {
                val config = foo.barz.wallpaperpicker.core.model.ImmichConfig(
                    serverUrl = prefs.immichServerUrl,
                    apiKey = prefs.immichApiKey,
                    albumId = prefs.immichAlbumId,
                    albumName = prefs.immichAlbumName,
                    downloadQuality = prefs.immichQuality,
                    ignoreSslErrors = prefs.immichIgnoreSsl,
                    wifiOnly = prefs.immichWifiOnly
                )
                val cacheManager = WallpaperCacheManager(context)
                ImmichSource(
                    context = context,
                    config = config,
                    cacheManager = cacheManager,
                    bypassNetworkConstraints = bypassNetworkConstraints
                )
            }
        }
    }
}
