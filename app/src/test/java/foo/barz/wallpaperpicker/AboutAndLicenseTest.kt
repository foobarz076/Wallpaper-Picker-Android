package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.ui.AppOpenSourceInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests verifying application branding, GPL-3.0 licensing constraints,
 * and third-party open-source library catalog metadata.
 */
class AboutAndLicenseTest {

    @Test
    fun testAppLicenseAndRepositoryConfiguration() {
        assertEquals("Wallpaper Picker", AppOpenSourceInfo.APP_NAME)
        assertEquals("壁纸随心换", AppOpenSourceInfo.APP_CHINESE_NAME)
        assertEquals("1.0.0", AppOpenSourceInfo.VERSION_NAME)
        assertEquals(1, AppOpenSourceInfo.VERSION_CODE)
        assertEquals("GPL-3.0-or-later", AppOpenSourceInfo.LICENSE_NAME)
        assertEquals("https://github.com/foobarz076/Wallpaper-Picker-Android", AppOpenSourceInfo.GITHUB_URL)

        assertTrue(AppOpenSourceInfo.GITHUB_URL.startsWith("https://github.com/"))
    }

    @Test
    fun testGpl3LicenseTextCompleteness() {
        val licenseText = AppOpenSourceInfo.GPL_3_LICENSE_TEXT
        assertTrue(licenseText.contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(licenseText.contains("Version 3, 29 June 2007"))
        assertTrue(licenseText.contains("Preamble"))
        assertTrue(licenseText.contains("TERMS AND CONDITIONS"))
        assertTrue(licenseText.contains("THERE IS NO WARRANTY FOR THE PROGRAM"))
    }

    @Test
    fun testApache2LicenseTextCompleteness() {
        val licenseText = AppOpenSourceInfo.APACHE_2_LICENSE_TEXT
        assertTrue(licenseText.contains("Apache License"))
        assertTrue(licenseText.contains("Version 2.0, January 2004"))
        assertTrue(licenseText.contains("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION"))
        assertTrue(licenseText.contains("Grant of Copyright License"))
        assertTrue(licenseText.contains("Redistribution"))
    }

    @Test
    fun testThirdPartyLibrariesCatalogIntegrity() {
        val libraries = AppOpenSourceInfo.THIRD_PARTY_LIBRARIES
        assertTrue(libraries.isNotEmpty(), "Third-party libraries list must not be empty")

        val libraryNames = libraries.map { it.name }
        assertTrue(libraryNames.any { it.contains("Coil") }, "Should include Coil")
        assertTrue(libraryNames.any { it.contains("OkHttp") }, "Should include OkHttp")
        assertTrue(libraryNames.any { it.contains("Conscrypt") }, "Should include Conscrypt")
        assertTrue(libraryNames.any { it.contains("Compose") }, "Should include Compose")
        assertTrue(libraryNames.any { it.contains("WorkManager") }, "Should include WorkManager")
        assertTrue(libraryNames.any { it.contains("DocumentFile") }, "Should include DocumentFile")
        assertTrue(libraryNames.any { it.contains("Kotlin") }, "Should include Kotlin")

        libraries.forEach { lib ->
            assertTrue(lib.name.isNotBlank(), "Library name must not be blank")
            assertTrue(lib.artifact.isNotBlank(), "Library artifact must not be blank: ${lib.name}")
            assertTrue(lib.version.isNotBlank(), "Library version must not be blank: ${lib.name}")
            assertTrue(lib.licenseName.isNotBlank(), "License name must not be blank: ${lib.name}")
            assertTrue(lib.licenseText.isNotBlank(), "License text must not be blank: ${lib.name}")
            assertTrue(lib.websiteUrl.startsWith("http://") || lib.websiteUrl.startsWith("https://"),
                "Website URL must be valid HTTP/HTTPS: ${lib.websiteUrl}")
            assertTrue(lib.description.isNotBlank(), "Description must not be blank: ${lib.name}")
        }
    }
}
