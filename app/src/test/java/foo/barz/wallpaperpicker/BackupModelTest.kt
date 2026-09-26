package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.backup.BackupCryptoEngine
import foo.barz.wallpaperpicker.core.backup.BackupHistoryOverride
import foo.barz.wallpaperpicker.core.backup.BackupPayload
import foo.barz.wallpaperpicker.core.backup.BackupPreferences
import foo.barz.wallpaperpicker.core.model.ImmichSourceConfig
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupModelTest {

    @Test
    fun testPayloadJsonRoundTrip() {
        val prefs = BackupPreferences(
            intervalMinutes = 45L,
            target = "HOME",
            scrollMode = "ALWAYS",
            cropMode = "FILL_SCREEN",
            reapplyOnScrollChange = false,
            isScheduled = true,
            fairShuffleCapacity = 100,
            immichServerUrl = "https://immich.local",
            immichApiKey = "secret-key-12345",
            dailyAnchorTimes = setOf("07:30", "12:00", "20:00")
        )

        val sources = listOf(
            WallpaperSourceEntity(
                id = "src-1",
                type = WallpaperSourceType.IMMICH,
                title = "My Immich",
                isEnabled = true,
                configJson = ImmichSourceConfig(
                    serverUrl = "https://immich.local",
                    apiKey = "secret-key-12345"
                ).toJson()
            ),
            WallpaperSourceEntity(
                id = "src-2",
                type = WallpaperSourceType.LOCAL_FOLDER,
                title = "Wallpapers Folder",
                isEnabled = false,
                configJson = LocalFolderSourceConfig(
                    folderUri = "content://com.android.externalstorage.documents/tree/primary%3APictures",
                    folderName = "Pictures",
                    imageCount = 12
                ).toJson()
            )
        )

        val rules = listOf(
            ScheduleRule(
                id = "rule-1",
                name = "Morning Rule",
                isEnabled = true,
                triggerType = ScheduleRuleTriggerType.DAILY_TIME,
                targetTime = "07:30",
                sourceBinding = ScheduleRuleSourceBinding.SPECIFIC_SOURCE,
                specificSourceId = "src-1"
            )
        )

        val overrides = listOf(
            BackupHistoryOverride(
                sourceUri = "content://media/external/images/media/42",
                title = "Favorite Landscape",
                sourceType = WallpaperSourceType.LOCAL_FOLDER,
                isFavorite = true,
                favoriteTimestamp = 1700000000000L,
                customScrollMode = WallpaperScrollMode.NEVER,
                cropFocusX = 0.25f,
                cropFocusY = 0.75f,
                flipHorizontal = true
            )
        )

        val originalPayload = BackupPayload(
            schemaVersion = 1,
            appVersionName = "1.2.3",
            appVersionCode = 123,
            preferences = prefs,
            sources = sources,
            rules = rules,
            historyOverrides = overrides
        )

        val jsonString = originalPayload.toJson()
        val parsedPayload = BackupPayload.fromJson(jsonString)

        assertEquals(originalPayload.schemaVersion, parsedPayload.schemaVersion)
        assertEquals(originalPayload.appVersionName, parsedPayload.appVersionName)
        assertEquals(originalPayload.appVersionCode, parsedPayload.appVersionCode)
        assertEquals(originalPayload.preferences.intervalMinutes, parsedPayload.preferences.intervalMinutes)
        assertEquals(originalPayload.preferences.target, parsedPayload.preferences.target)
        assertEquals(originalPayload.preferences.immichApiKey, parsedPayload.preferences.immichApiKey)
        assertEquals(originalPayload.preferences.dailyAnchorTimes, parsedPayload.preferences.dailyAnchorTimes)

        assertEquals(2, parsedPayload.sources.size)
        assertEquals("src-1", parsedPayload.sources[0].id)
        assertEquals(WallpaperSourceType.IMMICH, parsedPayload.sources[0].type)
        assertEquals(true, parsedPayload.sources[0].isEnabled)

        assertEquals(1, parsedPayload.rules.size)
        assertEquals("rule-1", parsedPayload.rules[0].id)
        assertEquals("07:30", parsedPayload.rules[0].targetTime)
        assertEquals(ScheduleRuleTriggerType.DAILY_TIME, parsedPayload.rules[0].triggerType)

        assertEquals(1, parsedPayload.historyOverrides.size)
        val override = parsedPayload.historyOverrides[0]
        assertEquals("Favorite Landscape", override.title)
        assertTrue(override.isFavorite)
        assertEquals(WallpaperScrollMode.NEVER, override.customScrollMode)
        assertEquals(0.25f, override.cropFocusX)
        assertEquals(0.75f, override.cropFocusY)
        assertTrue(override.flipHorizontal)
    }

    @Test
    fun testSensitiveDataDetection() {
        val safePayload = BackupPayload(
            appVersionName = "1.0.0",
            appVersionCode = 1,
            preferences = BackupPreferences(
                immichApiKey = "",
                httpCustomUrl = "https://example.com/random.jpg"
            ),
            sources = listOf(
                WallpaperSourceEntity(
                    id = "folder-src",
                    type = WallpaperSourceType.LOCAL_FOLDER,
                    title = "Local Pictures",
                    configJson = "{}"
                )
            ),
            rules = emptyList()
        )
        assertFalse(safePayload.containsSensitiveData())

        // Sensitive via Preferences immichApiKey
        val payloadWithImmichKey = safePayload.copy(
            preferences = safePayload.preferences.copy(immichApiKey = "secret_api_key_abc")
        )
        assertTrue(payloadWithImmichKey.containsSensitiveData())

        // Sensitive via URL api_key query param
        val payloadWithTokenUrl = safePayload.copy(
            preferences = safePayload.preferences.copy(
                httpCustomUrl = "https://api.example.com/v1/image?access_token=xyz123"
            )
        )
        assertTrue(payloadWithTokenUrl.containsSensitiveData())

        // Sensitive via Immich source config
        val payloadWithImmichSource = safePayload.copy(
            sources = listOf(
                WallpaperSourceEntity(
                    id = "immich-src",
                    type = WallpaperSourceType.IMMICH,
                    title = "Home Immich",
                    configJson = ImmichSourceConfig(
                        serverUrl = "https://my.immich",
                        apiKey = "source_immich_key_999"
                    ).toJson()
                )
            )
        )
        assertTrue(payloadWithImmichSource.containsSensitiveData())
    }

    @Test
    fun testSanitizeRemovesCredentials() {
        val sensitivePayload = BackupPayload(
            appVersionName = "1.0.0",
            appVersionCode = 1,
            preferences = BackupPreferences(
                immichApiKey = "super_secret_immich_key",
                httpCustomUrl = "https://api.unsplash.com/photos/random?api_key=my_secret_token"
            ),
            sources = listOf(
                WallpaperSourceEntity(
                    id = "immich-src",
                    type = WallpaperSourceType.IMMICH,
                    title = "Home Immich",
                    configJson = ImmichSourceConfig(
                        serverUrl = "https://immich.home",
                        apiKey = "inner_secret_key"
                    ).toJson()
                )
            ),
            rules = emptyList()
        )

        assertTrue(sensitivePayload.containsSensitiveData())

        val sanitized = sensitivePayload.sanitize()
        assertFalse(sanitized.containsSensitiveData())
        assertEquals("", sanitized.preferences.immichApiKey)
        assertTrue(sanitized.preferences.httpCustomUrl.contains("[REDACTED]"))

        val immichConfig = ImmichSourceConfig.fromJson(sanitized.sources[0].configJson)
        assertEquals("", immichConfig.apiKey)
        assertEquals("https://immich.home", immichConfig.serverUrl)
    }

    @Test
    fun testBackupCryptoStreamingRoundTrip() {
        val payload = BackupPayload(
            appVersionName = "1.0.0",
            appVersionCode = 1,
            preferences = BackupPreferences(intervalMinutes = 15L),
            sources = listOf(
                WallpaperSourceEntity(
                    id = "s1",
                    type = WallpaperSourceType.LOCAL_FOLDER,
                    title = "Wallpapers",
                    configJson = "{}"
                )
            ),
            rules = emptyList()
        )

        val json = payload.toJson()
        val password = "SuperSecurePassword987!".toCharArray()

        // 1. Encrypt to stream
        val outStream = ByteArrayOutputStream()
        val encryptStream = BackupCryptoEngine.wrapEncryptStream(outStream, password)
        encryptStream.use {
            it.write(json.toByteArray(Charsets.UTF_8))
            it.flush()
        }

        val encryptedBytes = outStream.toByteArray()
        assertTrue(BackupCryptoEngine.isWpbkEncrypted(encryptedBytes))

        // 2. Decrypt from stream
        val inStream = ByteArrayInputStream(encryptedBytes)
        val decryptStream = BackupCryptoEngine.wrapDecryptStream(inStream, password)
        val decryptedJson = decryptStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        val restoredPayload = BackupPayload.fromJson(decryptedJson)
        assertEquals(15L, restoredPayload.preferences.intervalMinutes)
        assertEquals(1, restoredPayload.sources.size)
        assertEquals("s1", restoredPayload.sources[0].id)
    }

    @Test
    fun testIsEncryptedBackupDetection() {
        val encryptedData = ByteArrayOutputStream().apply {
            val encryptStream = BackupCryptoEngine.wrapEncryptStream(this, "testPass".toCharArray())
            encryptStream.write("{}".toByteArray(Charsets.UTF_8))
            encryptStream.close()
        }.toByteArray()

        val encryptedStream = ByteArrayInputStream(encryptedData)
        assertTrue(foo.barz.wallpaperpicker.core.backup.BackupManager.isEncryptedBackup(encryptedStream))
        // Verify stream position was reset and content is still readable
        val recheck = foo.barz.wallpaperpicker.core.backup.BackupManager.isEncryptedBackup(encryptedStream)
        assertTrue(recheck)

        val plaintextStream = ByteArrayInputStream("{\"schemaVersion\": 1}".toByteArray(Charsets.UTF_8))
        assertFalse(foo.barz.wallpaperpicker.core.backup.BackupManager.isEncryptedBackup(plaintextStream))
    }
}
