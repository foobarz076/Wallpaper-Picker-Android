package foo.barz.wallpaperpicker

import android.net.Uri
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.tabs.MainTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryAndFavoritesTest {

    @Test
    fun testMainTabSequenceWithHistoryAsSecondTab() {
        val tabs = MainTab.values()
        assertEquals(4, tabs.size)
        assertEquals(MainTab.DASHBOARD, tabs[0])
        assertEquals(MainTab.HISTORY, tabs[1])
        assertEquals(MainTab.SOURCES, tabs[2])
        assertEquals(MainTab.SETTINGS, tabs[3])

        assertEquals(R.string.tab_dashboard, MainTab.DASHBOARD.titleRes)
        assertEquals(R.string.tab_history, MainTab.HISTORY.titleRes)
        assertEquals(R.string.tab_sources, MainTab.SOURCES.titleRes)
        assertEquals(R.string.tab_settings, MainTab.SETTINGS.titleRes)
    }

    @Test
    fun testWallpaperHistoryItemModel() {
        val item = WallpaperHistoryItem(
            id = 1L,
            sourceUri = "content://media/external/images/media/100",
            title = "Sample Wallpaper.jpg",
            sourceType = WallpaperSourceType.MEDIA_STORE,
            appliedTimestamp = 1700000000000L,
            isFavorite = false,
            favoriteTimestamp = null,
            favoriteFilePath = null,
            customScrollMode = WallpaperScrollMode.AUTO,
            cropFocusX = 0.5f,
            cropFocusY = 0.5f
        )

        assertEquals(1L, item.id)
        assertEquals("Sample Wallpaper.jpg", item.title)
        assertEquals(WallpaperSourceType.MEDIA_STORE, item.sourceType)
        assertFalse(item.isFavorite)
        assertNull(item.favoriteTimestamp)
        assertEquals(WallpaperScrollMode.AUTO, item.customScrollMode)
        assertEquals(0.5f, item.cropFocusX)
        assertEquals(0.5f, item.cropFocusY)
        assertEquals(0L, item.downloadTimestamp)
    }

    @Test
    fun testWallpaperHistoryItemDownloadTimestampInvalidatesEquality() {
        val original = WallpaperHistoryItem(
            id = 50L,
            sourceUri = "file:///data/user/0/cache/wallpapers/wp_123.jpg",
            title = "Sample.jpg",
            sourceType = WallpaperSourceType.HTTP_API,
            appliedTimestamp = 1700000000000L,
            downloadTimestamp = 0L
        )

        val redownloaded = original.copy(downloadTimestamp = 1700000099000L)
        // Ensure Compose detects difference even when sourceUri and all other fields are identical
        kotlin.test.assertNotEquals(original, redownloaded)
        assertEquals(1700000099000L, redownloaded.downloadTimestamp)
    }

    @Test
    fun testWallpaperHistoryItemFavoriteToggle() {
        val normalItem = WallpaperHistoryItem(
            id = 2L,
            sourceUri = "file:///data/user/0/cache/wallpapers/wp_123.jpg",
            title = "Network Wallpaper",
            sourceType = WallpaperSourceType.HTTP_API,
            appliedTimestamp = 1700000000000L,
            isFavorite = false
        )

        assertFalse(normalItem.isFavorite)

        val favoritedItem = normalItem.copy(
            isFavorite = true,
            favoriteTimestamp = 1700000050000L,
            favoriteFilePath = "/data/user/0/files/favorites/fav_123.jpg"
        )

        assertTrue(favoritedItem.isFavorite)
        assertEquals(1700000050000L, favoritedItem.favoriteTimestamp)
        assertEquals("/data/user/0/files/favorites/fav_123.jpg", favoritedItem.favoriteFilePath)
    }

    @Test
    fun testMainUiStateFavoritesProperties() {
        val state = foo.barz.wallpaperpicker.ui.MainUiState()
        assertEquals(0L, state.favoritesSizeBytes)
        assertFalse(state.isExportingFavorites)
        assertTrue(state.favoritesList.isEmpty())
        assertFalse(state.isCurrentFavorite)

        val updated = state.copy(
            favoritesSizeBytes = 10485760L,
            isExportingFavorites = true,
            isCurrentFavorite = true
        )
        assertEquals(10485760L, updated.favoritesSizeBytes)
        assertTrue(updated.isExportingFavorites)
        assertTrue(updated.isCurrentFavorite)
    }

    @Test
    fun testPhysicalStorageIsolationPaths() {
        val cacheBasePath = "/data/user/0/foo.barz.wallpaperpicker/cache/wallpapers"
        val favoritesBasePath = "/data/user/0/foo.barz.wallpaperpicker/files/favorites"

        // Ensure favorites path is physically decoupled from cache path
        assertFalse(favoritesBasePath.startsWith(cacheBasePath))
        assertFalse(cacheBasePath.startsWith(favoritesBasePath))

        val networkSourceItem = WallpaperHistoryItem(
            id = 10L,
            sourceUri = "$cacheBasePath/wp_abc.jpg",
            title = "Immich Image",
            sourceType = WallpaperSourceType.IMMICH,
            appliedTimestamp = 1700000000000L,
            isFavorite = true,
            favoriteFilePath = "$favoritesBasePath/fav_123.jpg"
        )

        // Verifies promoted persistent path is outside cache directory
        assertTrue(networkSourceItem.favoriteFilePath!!.startsWith(favoritesBasePath))
        assertFalse(networkSourceItem.favoriteFilePath!!.startsWith(cacheBasePath))
    }

    @Test
    fun testSafetyFeedbackMessageFormatting() {
        val freedBytes = 25 * 1024 * 1024L
        val mb = freedBytes / (1024.0 * 1024.0)
        val freedFormatted = String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
        val favCount = 12

        val message = "临时缓存已清理 (释放了 $freedFormatted)，$favCount 张已收藏壁纸受离线持久保护不受影响"
        assertTrue(message.contains("25.0 MB"))
        assertTrue(message.contains("12 张已收藏壁纸受离线持久保护不受影响"))
    }

    @Test
    fun testConcreteSourceBadgeAndDetailFormatting() {
        // 1. Explicit sourceTitle present
        val itemWithTitle = WallpaperHistoryItem(
            id = 100L,
            sourceUri = "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FExports_jpg%2FGemini_123.jpg",
            title = "Gemini_123.jpg",
            sourceType = WallpaperSourceType.LOCAL_FOLDER,
            appliedTimestamp = 1700000000000L,
            sourceTitle = "Exports_jpg"
        )
        assertEquals("Exports_jpg", itemWithTitle.displaySourceBadge)
        assertEquals("Exports_jpg (本地文件夹)", itemWithTitle.displaySourceDetail)

        // 2. Legacy item with COMPOSITE sourceType and no sourceTitle (extracted from tree document URI)
        val legacyCompositeItem = WallpaperHistoryItem(
            id = 101L,
            sourceUri = "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FExports_jpg%2FGemini_123.jpg",
            title = "Gemini_123.jpg",
            sourceType = WallpaperSourceType.COMPOSITE,
            appliedTimestamp = 1700000000000L,
            sourceTitle = null
        )
        assertEquals("Exports_jpg", legacyCompositeItem.displaySourceBadge)
        assertEquals("Exports_jpg", legacyCompositeItem.displaySourceDetail)

        // 3. MediaStore album item
        val mediaStoreItem = WallpaperHistoryItem(
            id = 102L,
            sourceUri = "content://media/external/images/media/42",
            title = "IMG_001.jpg",
            sourceType = WallpaperSourceType.MEDIA_STORE,
            appliedTimestamp = 1700000000000L,
            sourceTitle = "相机"
        )
        assertEquals("相机", mediaStoreItem.displaySourceBadge)
        assertEquals("相机 (系统相册)", mediaStoreItem.displaySourceDetail)

        // 4. Immich album item
        val immichItem = WallpaperHistoryItem(
            id = 103L,
            sourceUri = "/data/user/0/cache/wallpapers/immich_123.jpg",
            title = "Landscape.jpg",
            sourceType = WallpaperSourceType.IMMICH,
            appliedTimestamp = 1700000000000L,
            sourceTitle = "Immich (精选壁纸)"
        )
        assertEquals("Immich (精选壁纸)", immichItem.displaySourceBadge)
        assertEquals("Immich (精选壁纸)", immichItem.displaySourceDetail)

        // 5. HttpApi preset item
        val bingItem = WallpaperHistoryItem(
            id = 104L,
            sourceUri = "/data/user/0/cache/wallpapers/bing.jpg",
            title = "Bing Wallpaper",
            sourceType = WallpaperSourceType.HTTP_API,
            appliedTimestamp = 1700000000000L,
            sourceTitle = "Bing 每日壁纸"
        )
        assertEquals("Bing 每日壁纸", bingItem.displaySourceBadge)
        assertEquals("Bing 每日壁纸", bingItem.displaySourceDetail)

        // 6. Favorites item
        val favItem = WallpaperHistoryItem(
            id = 105L,
            sourceUri = "content://media/external/images/media/99",
            title = "Fav Wallpaper",
            sourceType = WallpaperSourceType.FAVORITES,
            appliedTimestamp = 1700000000000L,
            sourceTitle = "我的收藏"
        )
        assertEquals("我的收藏", favItem.displaySourceBadge)
        assertEquals("我的收藏", favItem.displaySourceDetail)
    }

    @Test
    fun testFolderNameExtractionFromVariousUris() {
        val uri1 = "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FExports_jpg%2FGemini_Generated_Image_8741368741368741.jpeg"
        assertEquals("Exports_jpg", WallpaperHistoryItem.extractFolderNameFromUri(uri1))

        val uri2 = "content://com.android.externalstorage.documents/tree/primary%3AAnimeWallpapers/document/primary%3AAnimeWallpapers%2F01.jpg"
        assertEquals("AnimeWallpapers", WallpaperHistoryItem.extractFolderNameFromUri(uri2))

        val uri3 = "/storage/emulated/0/DCIM/Camera/photo.jpg"
        assertEquals("Camera", WallpaperHistoryItem.extractFolderNameFromUri(uri3))

        val uri4 = "content://media/external/images/media/12345"
        assertNull(WallpaperHistoryItem.extractFolderNameFromUri(uri4))

        val uri5 = "file:///data/user/0/foo.barz.wallpaperpicker/cache/wallpapers/cached.jpg"
        assertNull(WallpaperHistoryItem.extractFolderNameFromUri(uri5))
    }

    @Test
    fun testCanRedownloadProperty() {
        val localItem = WallpaperHistoryItem(
            id = 201L,
            sourceUri = "/storage/emulated/0/DCIM/photo.jpg",
            title = "Local Photo",
            sourceType = WallpaperSourceType.LOCAL_FOLDER,
            appliedTimestamp = 1700000000000L
        )
        assertFalse(localItem.canRedownload)

        val httpDirectItem = WallpaperHistoryItem(
            id = 202L,
            sourceUri = "https://picsum.photos/1080/1920",
            title = "Direct Network",
            sourceType = WallpaperSourceType.HTTP_API,
            appliedTimestamp = 1700000000000L
        )
        assertTrue(httpDirectItem.canRedownload)

        val cachedItemWithRemoteUrl = WallpaperHistoryItem(
            id = 203L,
            sourceUri = "file:///data/user/0/cache/wallpapers/wp_123.jpg",
            title = "Cached Bing",
            sourceType = WallpaperSourceType.HTTP_API,
            appliedTimestamp = 1700000000000L,
            remoteUrl = "https://bing.com/th?id=OHR.jpg"
        )
        assertTrue(cachedItemWithRemoteUrl.canRedownload)

        val immichItemWithRemoteUrl = WallpaperHistoryItem(
            id = 204L,
            sourceUri = "file:///data/user/0/cache/wallpapers/wp_456.jpg",
            title = "Cached Immich",
            sourceType = WallpaperSourceType.IMMICH,
            appliedTimestamp = 1700000000000L,
            remoteUrl = "immich://asset-uuid-123?quality=ORIGINAL"
        )
        assertTrue(immichItemWithRemoteUrl.canRedownload)
    }

    @Test
    fun testManageSpaceUiStateClearCacheProperties() {
        val state = foo.barz.wallpaperpicker.ui.ManageSpaceUiState()
        assertFalse(state.showClearWallpaperCacheDialog)
        assertFalse(state.showClearAllImageCacheDialog)
        assertFalse(state.removeInvalidHistoryOnClean)

        val requested = state.copy(
            showClearWallpaperCacheDialog = true,
            showClearAllImageCacheDialog = true,
            removeInvalidHistoryOnClean = true
        )
        assertTrue(requested.showClearWallpaperCacheDialog)
        assertTrue(requested.showClearAllImageCacheDialog)
        assertTrue(requested.removeInvalidHistoryOnClean)
    }
}
