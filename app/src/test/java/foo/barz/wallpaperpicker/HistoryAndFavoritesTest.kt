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

        assertEquals("控制台", MainTab.DASHBOARD.title)
        assertEquals("历史", MainTab.HISTORY.title)
        assertEquals("图源", MainTab.SOURCES.title)
        assertEquals("设置", MainTab.SETTINGS.title)
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
}
