package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichConfig
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.core.model.CustomPhotosSourceConfig
import foo.barz.wallpaperpicker.core.model.FavoritesSourceConfig
import foo.barz.wallpaperpicker.core.model.HttpApiSourceConfig
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.isDefaultPreset
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.source.WallpaperSource
import foo.barz.wallpaperpicker.ui.MainUiState
import foo.barz.wallpaperpicker.ui.components.PickerAlbumItem
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Phase4MultiSourceTest {

    @Test
    fun testSourceTypeEnumExpansion() {
        val types = WallpaperSourceType.values()
        assertEquals(7, types.size)
        assertTrue(types.contains(WallpaperSourceType.CUSTOM_PHOTOS))
        assertTrue(types.contains(WallpaperSourceType.FAVORITES))
        assertTrue(types.contains(WallpaperSourceType.COMPOSITE))
        assertEquals("自选照片集", WallpaperSourceType.CUSTOM_PHOTOS.displayName)
        assertEquals("我的收藏", WallpaperSourceType.FAVORITES.displayName)
        assertEquals("多源混合轮播", WallpaperSourceType.COMPOSITE.displayName)
    }

    @Test
    fun testMediaStoreAlbumCoverSupport() {
        val albumWithoutCover = MediaStoreAlbum(
            id = "bucket_1",
            name = "Screenshots",
            count = 50
        )
        assertNull(albumWithoutCover.coverUri)

        val albumWithExplicitNullCover = MediaStoreAlbum(
            id = "bucket_2",
            name = "Camera",
            count = 120,
            coverUri = null
        )
        assertNull(albumWithExplicitNullCover.coverUri)
        assertEquals("bucket_2", albumWithExplicitNullCover.id)
        assertEquals("Camera", albumWithExplicitNullCover.name)
        assertEquals(120, albumWithExplicitNullCover.count)
    }

    @Test
    fun testImmichAlbumThumbnailAssetIdSupport() {
        val album = ImmichAlbum(
            id = "album_uuid_123",
            name = "Summer Trip",
            assetCount = 88,
            thumbnailAssetId = "asset_uuid_456"
        )
        assertEquals("album_uuid_123", album.id)
        assertEquals("Summer Trip", album.name)
        assertEquals(88, album.assetCount)
        assertEquals("asset_uuid_456", album.thumbnailAssetId)
    }

    @Test
    fun testImmichConfigMultiAlbumSupport() {
        val config = ImmichConfig(
            serverUrl = "https://immich.home.arpa",
            apiKey = "secret_key",
            albumId = "album_1",
            albumIds = setOf("album_1", "album_2", "album_3"),
            downloadQuality = ImmichQuality.PREVIEW,
            ignoreSslErrors = true,
            wifiOnly = true
        )

        assertEquals(3, config.albumIds.size)
        assertTrue(config.albumIds.contains("album_1"))
        assertTrue(config.albumIds.contains("album_2"))
        assertTrue(config.albumIds.contains("album_3"))
        assertEquals("album_1", config.albumId)
        assertEquals(ImmichQuality.PREVIEW, config.downloadQuality)
        assertTrue(config.ignoreSslErrors)
    }

    @Test
    fun testPickerAlbumItemModel() {
        val item = PickerAlbumItem(
            id = "album_1",
            name = "Wallpapers",
            count = 42,
            coverUri = null,
            coverUrl = "https://example.com/cover.jpg",
            apiKey = "test_key"
        )

        assertEquals("album_1", item.id)
        assertEquals("Wallpapers", item.name)
        assertEquals(42, item.count)
        assertNull(item.coverUri)
        assertEquals("https://example.com/cover.jpg", item.coverUrl)
        assertEquals("test_key", item.apiKey)
    }

    @Test
    fun testMainUiStateMultiAlbumAndCompositeProperties() {
        val defaultState = MainUiState()
        assertTrue(defaultState.mediaStoreAlbumIds.isEmpty())
        assertTrue(defaultState.immichAlbumIds.isEmpty())
        assertTrue(defaultState.compositeEnabledSources.isEmpty())

        val configuredState = defaultState.copy(
            mediaStoreAlbumIds = setOf("101", "102"),
            immichAlbumIds = setOf("immich_a", "immich_b"),
            compositeEnabledSources = setOf(
                WallpaperSourceType.LOCAL_FOLDER,
                WallpaperSourceType.MEDIA_STORE,
                WallpaperSourceType.FAVORITES
            )
        )

        assertEquals(2, configuredState.mediaStoreAlbumIds.size)
        assertTrue(configuredState.mediaStoreAlbumIds.contains("101"))
        assertTrue(configuredState.mediaStoreAlbumIds.contains("102"))

        assertEquals(2, configuredState.immichAlbumIds.size)
        assertTrue(configuredState.immichAlbumIds.contains("immich_a"))

        assertEquals(3, configuredState.compositeEnabledSources.size)
        assertTrue(configuredState.compositeEnabledSources.contains(WallpaperSourceType.LOCAL_FOLDER))
        assertTrue(configuredState.compositeEnabledSources.contains(WallpaperSourceType.MEDIA_STORE))
        assertTrue(configuredState.compositeEnabledSources.contains(WallpaperSourceType.FAVORITES))
        assertFalse(configuredState.compositeEnabledSources.contains(WallpaperSourceType.HTTP_API))
    }

    @Test
    fun testWallpaperDataContract() {
        val data = WallpaperData(
            openStream = { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)) },
            title = "Test Image",
            sourceUri = null
        )

        assertEquals("Test Image", data.title)
        assertNull(data.sourceUri)

        val stream = data.openStream()
        assertEquals(4, stream.available())
        assertEquals(1, stream.read())
        stream.close()
    }

    @Test
    fun testWallpaperSourceInterfaceContract() = runBlocking {
        val mockData = WallpaperData(
            openStream = { ByteArrayInputStream(byteArrayOf(0x42)) },
            title = "Mock Wallpaper"
        )

        val mockSource = object : WallpaperSource {
            override val id: String = "mock_test"
            override val displayName: String = "Mock Test Provider"
            override suspend fun getNextWallpaper(): Result<WallpaperData> = Result.success(mockData)
        }

        assertEquals("mock_test", mockSource.id)
        assertEquals("Mock Test Provider", mockSource.displayName)

        val result = mockSource.getNextWallpaper()
        assertTrue(result.isSuccess)
        assertEquals("Mock Wallpaper", result.getOrNull()?.title)
    }

    @Test
    fun testWallpaperSourceFailureContract() = runBlocking {
        val mockSource = object : WallpaperSource {
            override val id: String = "failing_mock"
            override val displayName: String = "Failing Provider"
            override suspend fun getNextWallpaper(): Result<WallpaperData> {
                return Result.failure(IllegalStateException("Source unavailable"))
            }
        }

        val result = mockSource.getNextWallpaper()
        assertTrue(result.isFailure)
        assertEquals("Source unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun testWallpaperSourceEntityAndConfigSerialization() {
        val folderConfig = foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig(
            folderUri = "content://sample/folder",
            folderName = "Anime Wallpapers",
            imageCount = 500
        )
        val json = folderConfig.toJson()
        val parsed = foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig.fromJson(json)
        assertEquals("content://sample/folder", parsed.folderUri)
        assertEquals("Anime Wallpapers", parsed.folderName)
        assertEquals(500, parsed.imageCount)

        val mediaStoreConfig = foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig(
            albumIds = setOf("101", "102"),
            albumNames = "Camera, Screenshots"
        )
        val msParsed = foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig.fromJson(mediaStoreConfig.toJson())
        assertEquals(2, msParsed.albumIds.size)
        assertTrue(msParsed.albumIds.contains("101"))
        assertEquals("Camera, Screenshots", msParsed.albumNames)

        val immichConfig = foo.barz.wallpaperpicker.core.model.ImmichSourceConfig(
            serverUrl = "https://immich.test:2283",
            apiKey = "key_xyz",
            albumIds = setOf("a1"),
            albumNames = "Travel",
            quality = ImmichQuality.ORIGINAL,
            ignoreSsl = true,
            wifiOnly = false
        )
        val immichParsed = foo.barz.wallpaperpicker.core.model.ImmichSourceConfig.fromJson(immichConfig.toJson())
        assertEquals("https://immich.test:2283", immichParsed.serverUrl)
        assertEquals("key_xyz", immichParsed.apiKey)
        assertEquals(ImmichQuality.ORIGINAL, immichParsed.quality)
        assertTrue(immichParsed.ignoreSsl)
        assertFalse(immichParsed.wifiOnly)

        val entity = foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity(
            id = "src_1",
            type = WallpaperSourceType.IMMICH,
            title = "My Immich",
            isEnabled = true,
            configJson = immichConfig.toJson()
        )
        assertEquals("src_1", entity.id)
        assertEquals(WallpaperSourceType.IMMICH, entity.type)
        assertEquals("My Immich", entity.title)
        assertTrue(entity.isEnabled)
    }

    @Test
    fun testCompositeSourceListDelegation() = runBlocking {
        val testWallpaper = WallpaperData(
            openStream = { ByteArrayInputStream(byteArrayOf(1)) },
            title = "Test Wallpaper"
        )

        val failingSource = object : WallpaperSource {
            override val id: String = "fail"
            override val displayName: String = "Fail"
            override suspend fun getNextWallpaper(): Result<WallpaperData> =
                Result.failure(IllegalStateException("Network offline"))
        }

        val workingSource = object : WallpaperSource {
            override val id: String = "work"
            override val displayName: String = "Work"
            override suspend fun getNextWallpaper(): Result<WallpaperData> =
                Result.success(testWallpaper)
        }

        // Composite should fallback from failingSource to workingSource
        val composite = foo.barz.wallpaperpicker.core.source.CompositeSource(listOf(failingSource, workingSource))
        val result = composite.getNextWallpaper()
        assertTrue(result.isSuccess)
        assertEquals("Test Wallpaper", result.getOrNull()?.title)
    }

    @Test
    fun testMainUiStateSourcesListProperty() {
        val state = MainUiState()
        assertTrue(state.sourcesList.isEmpty())

        val entity = foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity(
            id = "src_test",
            type = WallpaperSourceType.LOCAL_FOLDER,
            title = "Folder A",
            isEnabled = true,
            configJson = "{}"
        )
        val updated = state.copy(sourcesList = listOf(entity))
        assertEquals(1, updated.sourcesList.size)
        assertEquals("Folder A", updated.sourcesList[0].title)
    }

    @Test
    fun testCustomPhotosSourceConfigSerialization() {
        val config = foo.barz.wallpaperpicker.core.model.CustomPhotosSourceConfig(
            fileNames = listOf("photo1.jpg", "photo2.png"),
            imageCount = 2
        )
        val json = config.toJson()
        val parsed = foo.barz.wallpaperpicker.core.model.CustomPhotosSourceConfig.fromJson(json)
        assertEquals(2, parsed.imageCount)
        assertEquals(2, parsed.fileNames.size)
        assertEquals("photo1.jpg", parsed.fileNames[0])
        assertEquals("photo2.png", parsed.fileNames[1])
    }

    @Test
    fun testIsDefaultPresetDetection() {
        val defaultMediaStore = WallpaperSourceEntity(
            type = WallpaperSourceType.MEDIA_STORE,
            title = "系统相册",
            isEnabled = false,
            configJson = MediaStoreSourceConfig(emptySet(), "全部照片 (全库随机)").toJson()
        )
        assertTrue(defaultMediaStore.isDefaultPreset())

        val customMediaStore = WallpaperSourceEntity(
            type = WallpaperSourceType.MEDIA_STORE,
            title = "相机胶卷",
            isEnabled = true,
            configJson = MediaStoreSourceConfig(setOf("bucket_camera"), "相机").toJson()
        )
        assertFalse(customMediaStore.isDefaultPreset())

        val defaultBing = WallpaperSourceEntity(
            type = WallpaperSourceType.HTTP_API,
            title = "Bing 每日壁纸",
            isEnabled = false,
            configJson = HttpApiSourceConfig(preset = HttpPresetType.BING).toJson()
        )
        assertTrue(defaultBing.isDefaultPreset())

        val customHttp = WallpaperSourceEntity(
            type = WallpaperSourceType.HTTP_API,
            title = "Custom API",
            isEnabled = true,
            configJson = HttpApiSourceConfig(preset = HttpPresetType.CUSTOM, customUrl = "https://api.example.com").toJson()
        )
        assertFalse(customHttp.isDefaultPreset())

        val defaultFavorites = WallpaperSourceEntity(
            type = WallpaperSourceType.FAVORITES,
            title = "我的收藏",
            isEnabled = false,
            configJson = FavoritesSourceConfig().toJson()
        )
        assertTrue(defaultFavorites.isDefaultPreset())

        val localFolder = WallpaperSourceEntity(
            type = WallpaperSourceType.LOCAL_FOLDER,
            title = "Wallpapers",
            isEnabled = true,
            configJson = LocalFolderSourceConfig("content://sample/folder", "Wallpapers").toJson()
        )
        assertFalse(localFolder.isDefaultPreset())

        val customPhotos = WallpaperSourceEntity(
            type = WallpaperSourceType.CUSTOM_PHOTOS,
            title = "My Photos",
            isEnabled = true,
            configJson = CustomPhotosSourceConfig().toJson()
        )
        assertFalse(customPhotos.isDefaultPreset())
    }

    @Test
    fun testFreshInstallOnboardingVisibility() {
        val defaultPresets = listOf(
            WallpaperSourceEntity(
                type = WallpaperSourceType.MEDIA_STORE,
                title = "系统相册",
                isEnabled = false,
                configJson = MediaStoreSourceConfig().toJson()
            ),
            WallpaperSourceEntity(
                type = WallpaperSourceType.HTTP_API,
                title = "Bing 每日壁纸",
                isEnabled = false,
                configJson = HttpApiSourceConfig(preset = HttpPresetType.BING).toJson()
            ),
            WallpaperSourceEntity(
                type = WallpaperSourceType.FAVORITES,
                title = "我的收藏",
                isEnabled = false,
                configJson = FavoritesSourceConfig().toJson()
            )
        )

        // Case 1: Fresh install state with unconfigured, disabled default presets
        val freshState = MainUiState(
            sourcesList = defaultPresets,
            lastWallpaperUri = null,
            folderUri = null,
            hasUserAddedSource = false
        )
        assertTrue(freshState.isOnboardingRestoreVisible)

        // Case 2: User enables one of the presets
        val presetEnabledState = freshState.copy(
            sourcesList = defaultPresets.mapIndexed { idx, src ->
                if (idx == 1) src.copy(isEnabled = true) else src
            }
        )
        assertFalse(presetEnabledState.isOnboardingRestoreVisible)

        // Case 3: User adds a custom source
        val addedSource = WallpaperSourceEntity(
            type = WallpaperSourceType.LOCAL_FOLDER,
            title = "Local",
            isEnabled = true,
            configJson = LocalFolderSourceConfig("uri", "Local").toJson()
        )
        val customSourceState = freshState.copy(
            sourcesList = defaultPresets + addedSource
        )
        assertFalse(customSourceState.isOnboardingRestoreVisible)

        // Case 4: hasUserAddedSource is explicitly flagged
        val flaggedState = freshState.copy(hasUserAddedSource = true)
        assertFalse(flaggedState.isOnboardingRestoreVisible)
    }
}
