package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.ui.tabs.settings.SettingsHelpers
import foo.barz.wallpaperpicker.ui.tabs.settings.SettingsSubPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying normalized settings sub-page definitions and helper behaviors.
 */
class SettingsSubPageArchitectureTest {

    @Test
    fun testAllSubPagesHaveValidMetadata() {
        val subPages = SettingsSubPage.values()
        assertEquals(6, subPages.size)

        val uniqueTitles = subPages.map { it.title }.toSet()
        assertEquals(6, uniqueTitles.size)

        subPages.forEach { page ->
            assertTrue(page.title.isNotBlank())
            assertTrue(page.description.isNotBlank())
            assertNotNull(page.icon)
        }
    }

    @Test
    fun testFormatFileSizeHelper() {
        assertEquals("0 B", SettingsHelpers.formatFileSize(0L))
        assertEquals("0 B", SettingsHelpers.formatFileSize(-100L))
        assertEquals("1.0 KB", SettingsHelpers.formatFileSize(1024L))
        assertEquals("500.0 KB", SettingsHelpers.formatFileSize(500 * 1024L))
        assertEquals("1.0 MB", SettingsHelpers.formatFileSize(1024 * 1024L))
        assertEquals("23.5 MB", SettingsHelpers.formatFileSize((23.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testDontKillMyAppUrlFallback() {
        val url = SettingsHelpers.getDontKillMyAppUrl()
        assertTrue(url.startsWith("https://dontkillmyapp.com"))
    }
}
