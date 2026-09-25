package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichConfig
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperData
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
        assertEquals(6, types.size)
        assertTrue(types.contains(WallpaperSourceType.FAVORITES))
        assertTrue(types.contains(WallpaperSourceType.COMPOSITE))
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
}
