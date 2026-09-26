package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.tile.NextWallpaperTileService
import foo.barz.wallpaperpicker.core.tile.ToggleScheduleTileService
import foo.barz.wallpaperpicker.core.worker.WallpaperSchedulerHelper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests validating Quick Settings Tiles configuration, manifest declarations,
 * drawables, string resources, and scheduler helper integration.
 */
class QuickSettingsTilesTest {

    @Test
    fun testManifestTileDeclarations() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue(manifestFile.exists(), "AndroidManifest.xml must exist")

        val content = manifestFile.readText()

        // 1. NextWallpaperTileService declaration
        assertTrue(content.contains(".core.tile.NextWallpaperTileService"))
        assertTrue(content.contains("@drawable/ic_tile_next_wallpaper"))
        assertTrue(content.contains("@string/tile_next_wallpaper_label"))

        // 2. ToggleScheduleTileService declaration
        assertTrue(content.contains(".core.tile.ToggleScheduleTileService"))
        assertTrue(content.contains("@drawable/ic_tile_toggle_schedule"))
        assertTrue(content.contains("@string/tile_toggle_schedule_label"))

        // 3. Security & system binding permission
        assertTrue(content.contains("android:permission=\"android.permission.BIND_QUICK_SETTINGS_TILE\""))
        assertTrue(content.contains("android:name=\"android.service.quicksettings.action.QS_TILE\""))
    }

    @Test
    fun testStringsResourceTileLabels() {
        val stringsFile = File("src/main/res/values/strings.xml")
        assertTrue(stringsFile.exists(), "strings.xml must exist")

        val content = stringsFile.readText()
        assertTrue(content.contains("name=\"tile_next_wallpaper_label\""))
        assertTrue(content.contains("立即换壁纸"))
        assertTrue(content.contains("name=\"tile_toggle_schedule_label\""))
        assertTrue(content.contains("自动换壁纸"))
    }

    @Test
    fun testTileDrawablesExistAndValid() {
        val nextTileIcon = File("src/main/res/drawable/ic_tile_next_wallpaper.xml")
        val toggleTileIcon = File("src/main/res/drawable/ic_tile_toggle_schedule.xml")

        assertTrue(nextTileIcon.exists(), "ic_tile_next_wallpaper.xml must exist")
        assertTrue(toggleTileIcon.exists(), "ic_tile_toggle_schedule.xml must exist")

        val nextContent = nextTileIcon.readText()
        assertTrue(nextContent.contains("<vector"))
        assertTrue(nextContent.contains("android:viewportWidth=\"24\""))
        assertTrue(nextContent.contains("android:fillColor=\"#FFFFFFFF\""))

        val toggleContent = toggleTileIcon.readText()
        assertTrue(toggleContent.contains("<vector"))
        assertTrue(toggleContent.contains("android:viewportWidth=\"24\""))
        assertTrue(toggleContent.contains("android:fillColor=\"#FFFFFFFF\""))
    }

    @Test
    fun testTileServiceClassesExist() {
        assertNotNull(NextWallpaperTileService::class.java)
        assertNotNull(ToggleScheduleTileService::class.java)

        // Validate requestUpdate companion method existence
        val nextUpdateMethod = NextWallpaperTileService.Companion::class.java.getMethod("requestUpdate", android.content.Context::class.java)
        assertNotNull(nextUpdateMethod)

        val toggleUpdateMethod = ToggleScheduleTileService.Companion::class.java.getMethod("requestUpdate", android.content.Context::class.java)
        assertNotNull(toggleUpdateMethod)
    }

    @Test
    fun testWallpaperSchedulerHelperMethods() {
        assertNotNull(WallpaperSchedulerHelper::class.java)

        val refreshMethod = WallpaperSchedulerHelper::class.java.getMethod("refreshScheduling", android.content.Context::class.java)
        assertNotNull(refreshMethod)

        val cancelMethod = WallpaperSchedulerHelper::class.java.getMethod("cancelScheduling", android.content.Context::class.java)
        assertNotNull(cancelMethod)

        val toggleMethod = WallpaperSchedulerHelper::class.java.methods.firstOrNull { it.name.startsWith("toggleScheduling") }
        assertNotNull(toggleMethod)
    }

    @Test
    fun testTileLongPressPreferencesDeclaration() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue(manifestFile.exists(), "AndroidManifest.xml must exist")

        val content = manifestFile.readText()
        // Ensure MainActivity declares ACTION_QS_TILE_PREFERENCES so long pressing any tile opens MainActivity
        assertTrue(content.contains("android.service.quicksettings.action.QS_TILE_PREFERENCES"))
        assertTrue(content.contains("android:launchMode=\"singleTop\""))
    }
}
