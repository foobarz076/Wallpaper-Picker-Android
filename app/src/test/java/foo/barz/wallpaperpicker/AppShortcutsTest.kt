package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppShortcutsTest {

    @Test
    fun testShortcutHelperConstants() {
        assertEquals("foo.barz.wallpaperpicker.ACTION_NEXT_WALLPAPER", ShortcutHelper.ACTION_NEXT_WALLPAPER)
        assertEquals("foo.barz.wallpaperpicker.ACTION_VIEW_CURRENT_WALLPAPER", ShortcutHelper.ACTION_VIEW_CURRENT_WALLPAPER)
        assertEquals("next_wallpaper", ShortcutHelper.ID_NEXT_WALLPAPER)
        assertEquals("view_current", ShortcutHelper.ID_VIEW_CURRENT)
    }

    @Test
    fun testShortcutsXmlIntegrity() {
        val shortcutsXmlFile = File("src/main/res/xml/shortcuts.xml")
        assertTrue(shortcutsXmlFile.exists(), "shortcuts.xml must exist in res/xml")

        val content = shortcutsXmlFile.readText()
        assertTrue(content.contains("android:shortcutId=\"next_wallpaper\""))
        assertTrue(content.contains("android:shortcutId=\"view_current\""))
        assertTrue(content.contains("foo.barz.wallpaperpicker.ACTION_NEXT_WALLPAPER"))
        assertTrue(content.contains("foo.barz.wallpaperpicker.ACTION_VIEW_CURRENT_WALLPAPER"))
        assertTrue(content.contains("foo.barz.wallpaperpicker.ui.ShortcutActionActivity"))
    }

    @Test
    fun testManifestShortcutsDeclaration() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue(manifestFile.exists(), "AndroidManifest.xml must exist")

        val content = manifestFile.readText()
        assertTrue(content.contains("android.app.shortcuts"))
        assertTrue(content.contains("@xml/shortcuts"))
        assertTrue(content.contains(".ui.ShortcutActionActivity"))
        assertTrue(content.contains("foo.barz.wallpaperpicker.ACTION_NEXT_WALLPAPER"))
        assertTrue(content.contains("foo.barz.wallpaperpicker.ACTION_VIEW_CURRENT_WALLPAPER"))
        assertTrue(content.contains("@style/Theme.TransparentAction"))
    }

    @Test
    fun testShortcutDrawableIconsExist() {
        val nextIcon = File("src/main/res/drawable/ic_shortcut_next.xml")
        val viewIcon = File("src/main/res/drawable/ic_shortcut_view.xml")
        assertTrue(nextIcon.exists(), "ic_shortcut_next.xml must exist")
        assertTrue(viewIcon.exists(), "ic_shortcut_view.xml must exist")

        val nextContent = nextIcon.readText()
        assertTrue(nextContent.contains("<vector"))
        assertTrue(nextContent.contains("#6750A4"))

        val viewContent = viewIcon.readText()
        assertTrue(viewContent.contains("<vector"))
        assertTrue(viewContent.contains("#006874"))
    }
}
