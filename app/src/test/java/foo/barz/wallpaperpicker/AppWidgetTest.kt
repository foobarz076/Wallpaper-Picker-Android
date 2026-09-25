package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppWidgetTest {

    @Test
    fun testWidgetScaleTypeEnum() {
        assertEquals(2, WidgetScaleType.values().size)
        assertEquals("铺满裁切", WidgetScaleType.CROP.displayName)
        assertEquals("完整展示", WidgetScaleType.FIT.displayName)

        // Test safe string parsing
        assertEquals(WidgetScaleType.CROP, WidgetScaleType.valueOf("CROP"))
        assertEquals(WidgetScaleType.FIT, WidgetScaleType.valueOf("FIT"))
    }

    @Test
    fun testWidgetProviderXmlIntegrity() {
        val widgetInfoFile = File("src/main/res/xml/widget_current_wallpaper_info.xml")
        assertTrue(widgetInfoFile.exists(), "widget_current_wallpaper_info.xml must exist in res/xml")

        val content = widgetInfoFile.readText()
        assertTrue(content.contains("xmlns:android=\"http://schemas.android.com/apk/res/android\""))
        assertTrue(content.contains("android:initialLayout=\"@layout/widget_current_wallpaper\""))
        assertTrue(content.contains("android:minWidth=\"40dp\""))
        assertTrue(content.contains("android:minHeight=\"40dp\""))
        assertTrue(content.contains("android:minResizeWidth=\"40dp\""))
        assertTrue(content.contains("android:minResizeHeight=\"40dp\""))
        assertTrue(content.contains("android:maxResizeWidth=\"1024dp\""))
        assertTrue(content.contains("android:maxResizeHeight=\"1024dp\""))
        assertTrue(content.contains("android:targetCellWidth=\"2\""))
        assertTrue(content.contains("android:targetCellHeight=\"2\""))
        assertTrue(content.contains("android:resizeMode=\"horizontal|vertical\""))
    }

    @Test
    fun testWidgetLayoutIntegrity() {
        val layoutFile = File("src/main/res/layout/widget_current_wallpaper.xml")
        assertTrue(layoutFile.exists(), "widget_current_wallpaper.xml must exist in res/layout")

        val content = layoutFile.readText()
        assertTrue(content.contains("android:id=\"@+id/widget_container\""))
        assertTrue(content.contains("android:id=\"@+id/widget_image_crop\""))
        assertTrue(content.contains("android:id=\"@+id/widget_image_fit\""))
        assertTrue(content.contains("android:id=\"@+id/widget_btn_refresh\""))
        assertTrue(content.contains("android:id=\"@+id/widget_title\""))
        assertTrue(content.contains("android:id=\"@+id/widget_subtitle\""))
        assertTrue(content.contains("android:id=\"@+id/widget_empty_text\""))
        assertTrue(content.contains("android:scaleType=\"centerCrop\""))
        assertTrue(content.contains("android:scaleType=\"fitCenter\""))
    }

    @Test
    fun testManifestWidgetProviderDeclaration() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue(manifestFile.exists(), "AndroidManifest.xml must exist")

        val content = manifestFile.readText()
        assertTrue(content.contains(".core.widget.CurrentWallpaperWidgetProvider"))
        assertTrue(content.contains("android.appwidget.action.APPWIDGET_UPDATE"))
        assertTrue(content.contains("android.appwidget.provider"))
        assertTrue(content.contains("@xml/widget_current_wallpaper_info"))
    }

    @Test
    fun testWidgetDrawablesExist() {
        val cardBg = File("src/main/res/drawable/widget_card_background.xml")
        val refreshBg = File("src/main/res/drawable/widget_refresh_button_bg.xml")
        val scrim = File("src/main/res/drawable/widget_bottom_scrim.xml")
        val refreshIcon = File("src/main/res/drawable/ic_widget_refresh.xml")

        assertTrue(cardBg.exists(), "widget_card_background.xml must exist")
        assertTrue(refreshBg.exists(), "widget_refresh_button_bg.xml must exist")
        assertTrue(scrim.exists(), "widget_bottom_scrim.xml must exist")
        assertTrue(refreshIcon.exists(), "ic_widget_refresh.xml must exist")
    }
}
