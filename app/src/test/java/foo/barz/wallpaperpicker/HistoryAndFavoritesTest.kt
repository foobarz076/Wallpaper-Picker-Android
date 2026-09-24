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
}
