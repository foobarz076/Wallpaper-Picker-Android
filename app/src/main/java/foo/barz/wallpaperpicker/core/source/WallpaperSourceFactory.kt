package foo.barz.wallpaperpicker.core.source

import android.content.Context
import android.net.Uri
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.core.model.HttpApiConfig
import foo.barz.wallpaperpicker.core.model.HttpApiSourceConfig
import foo.barz.wallpaperpicker.core.model.ImmichConfig
import foo.barz.wallpaperpicker.core.model.ImmichSourceConfig
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * Factory for creating active WallpaperSources based on persistent sources database or preferences.
 */
object WallpaperSourceFactory {

    /**
     * Creates the active WallpaperSource based on currently enabled source entities.
     * If multiple sources are enabled, returns a CompositeSource blending them.
     */
    fun createActiveSource(
        context: Context,
        prefs: PreferencesManager,
        bypassNetworkConstraints: Boolean = false
    ): WallpaperSource {
        val db = WallpaperSourcesDatabase(context)
        db.migrateFromPreferencesIfNeeded(prefs)
        val enabledEntities = db.getEnabledSources()

        return when {
            enabledEntities.isEmpty() -> {
                createSourceByType(context, prefs, prefs.sourceType, bypassNetworkConstraints)
            }
            enabledEntities.size == 1 -> {
                createSourceFromEntity(context, enabledEntities[0], bypassNetworkConstraints)
            }
            else -> {
                val sources = enabledEntities.map { createSourceFromEntity(context, it, bypassNetworkConstraints) }
                CompositeSource(sources)
            }
        }
    }

    /**
     * Resolves all enabled WallpaperSource instances from the sources database.
     */
    fun createEnabledSources(
        context: Context,
        prefs: PreferencesManager,
        bypassNetworkConstraints: Boolean = false
    ): List<WallpaperSource> {
        val db = WallpaperSourcesDatabase(context)
        db.migrateFromPreferencesIfNeeded(prefs)
        val enabledEntities = db.getEnabledSources()

        return if (enabledEntities.isNotEmpty()) {
            enabledEntities.map { createSourceFromEntity(context, it, bypassNetworkConstraints) }
        } else {
            val enabledTypes = prefs.compositeEnabledSources.filter { it != WallpaperSourceType.COMPOSITE }
            enabledTypes.map { createSourceByType(context, prefs, it, bypassNetworkConstraints) }
        }
    }

    /**
     * Instantiates a WallpaperSource from a persistent WallpaperSourceEntity.
     */
    fun createSourceFromEntity(
        context: Context,
        entity: WallpaperSourceEntity,
        bypassNetworkConstraints: Boolean = false
    ): WallpaperSource {
        return when (entity.type) {
            WallpaperSourceType.LOCAL_FOLDER -> {
                val config = LocalFolderSourceConfig.fromJson(entity.configJson)
                if (config.folderUri.isBlank()) {
                    throw IllegalStateException("未配置壁纸文件夹，请先在图源配置中选择文件夹")
                }
                val folderName = config.folderName.ifBlank { entity.title.ifBlank { null } }
                LocalFolderSource(context, Uri.parse(config.folderUri), folderName)
            }
            WallpaperSourceType.MEDIA_STORE -> {
                val config = MediaStoreSourceConfig.fromJson(entity.configJson)
                MediaStoreSource(
                    context = context,
                    bucketIds = config.albumIds,
                    albumName = config.albumNames
                )
            }
            WallpaperSourceType.IMMICH -> {
                val config = ImmichSourceConfig.fromJson(entity.configJson)
                val immichConfig = ImmichConfig(
                    serverUrl = config.serverUrl,
                    apiKey = config.apiKey,
                    albumIds = config.albumIds,
                    albumName = config.albumNames,
                    downloadQuality = config.quality,
                    ignoreSslErrors = config.ignoreSsl,
                    wifiOnly = config.wifiOnly
                )
                val cacheManager = WallpaperCacheManager(context)
                ImmichSource(
                    context = context,
                    config = immichConfig,
                    cacheManager = cacheManager,
                    bypassNetworkConstraints = bypassNetworkConstraints
                )
            }
            WallpaperSourceType.HTTP_API -> {
                val config = HttpApiSourceConfig.fromJson(entity.configJson)
                val httpConfig = HttpApiConfig(
                    presetType = config.preset,
                    customUrl = config.customUrl,
                    customJsonPath = config.customJsonPath,
                    wifiOnly = config.wifiOnly
                )
                val cacheManager = WallpaperCacheManager(context)
                HttpApiSource(
                    context = context,
                    config = httpConfig,
                    cacheManager = cacheManager,
                    bypassNetworkConstraints = bypassNetworkConstraints
                )
            }
            WallpaperSourceType.FAVORITES -> {
                FavoritesSource(
                    context = context,
                    database = WallpaperHistoryDatabase(context)
                )
            }
            WallpaperSourceType.COMPOSITE -> {
                throw IllegalArgumentException("Composite cannot be instantiated directly from single entity")
            }
        }
    }

    /**
     * Instantiates a WallpaperSource by its concrete WallpaperSourceType.
     */
    fun createSourceByType(
        context: Context,
        prefs: PreferencesManager,
        type: WallpaperSourceType,
        bypassNetworkConstraints: Boolean = false
    ): WallpaperSource {
        return when (type) {
            WallpaperSourceType.LOCAL_FOLDER -> {
                val folderUri = prefs.folderUri
                    ?: throw IllegalStateException("未选择壁纸文件夹，请先授权选择文件夹")
                val folderName = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)?.name
                LocalFolderSource(context, folderUri, folderName)
            }
            WallpaperSourceType.MEDIA_STORE -> {
                MediaStoreSource(
                    context = context,
                    bucketIds = prefs.mediaStoreAlbumIds,
                    albumName = prefs.mediaStoreAlbumName
                )
            }
            WallpaperSourceType.FAVORITES -> {
                FavoritesSource(
                    context = context,
                    database = WallpaperHistoryDatabase(context)
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
                val config = ImmichConfig(
                    serverUrl = prefs.immichServerUrl,
                    apiKey = prefs.immichApiKey,
                    albumId = prefs.immichAlbumId,
                    albumName = prefs.immichAlbumName,
                    albumIds = prefs.immichAlbumIds,
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
            WallpaperSourceType.COMPOSITE -> {
                CompositeSource(
                    context = context,
                    prefs = prefs,
                    bypassNetworkConstraints = bypassNetworkConstraints
                )
            }
        }
    }
}
