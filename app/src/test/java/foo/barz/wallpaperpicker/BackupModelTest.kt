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
            lockScreenStrategy = "FOLLOW_DESKTOP",
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
                flipHorizontal = true,
                lockCropFocusX = 0.80f,
                lockCropFocusY = 0.20f
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
        assertEquals("FOLLOW_DESKTOP", parsedPayload.preferences.lockScreenStrategy)
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
        assertEquals(0.80f, override.lockCropFocusX)
        assertEquals(0.20f, override.lockCropFocusY)
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

    @Test
    fun testOpenPgpMessageDetection() {
        // 1. ASCII Armor detection
        val asciiArmored = """
            -----BEGIN PGP MESSAGE-----
            Version: OpenKeychain v5.7.1

            wcBMAxV36zWj...
            -----END PGP MESSAGE-----
        """.trimIndent().toByteArray(Charsets.US_ASCII)

        assertTrue(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(asciiArmored)))

        // 2. Binary OpenPGP packet headers (RFC 4880 / RFC 9580)
        // Tag 1 (PKESK) old format: 10_0001_00 = 0x84
        val pkeskPacket = byteArrayOf(0x84.toByte(), 0x01, 0x02, 0x03, 0x04)
        assertTrue(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(pkeskPacket)))

        // Tag 3 (SKESK) old format: 10_0011_00 = 0x8C
        val skeskPacket = byteArrayOf(0x8C.toByte(), 0x05, 0x01, 0x02, 0x03)
        assertTrue(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(skeskPacket)))

        // Tag 9 (SED) old format: 10_1001_00 = 0xA4
        val sedPacket = byteArrayOf(0xA4.toByte(), 0x00, 0x10, 0x20)
        assertTrue(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(sedPacket)))

        // Tag 18 (SEIPD) new format: 11_010010 = 0xD2
        val seipdPacket = byteArrayOf(0xD2.toByte(), 0x01, 0x02, 0x03, 0x04)
        assertTrue(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(seipdPacket)))

        // 3. Rejects non-OpenPGP messages
        val jsonStream = ByteArrayInputStream("{\"schemaVersion\": 1}".toByteArray(Charsets.UTF_8))
        assertFalse(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(jsonStream))

        val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        assertFalse(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(randomBytes)))

        val shortBytes = byteArrayOf(0x84.toByte(), 0x01)
        assertFalse(foo.barz.wallpaperpicker.core.backup.OpenPgpBackupEngine.isOpenPgpMessage(ByteArrayInputStream(shortBytes)))
    }

    @Test
    fun testDetectBackupFormat() {
        // Native Encrypted
        val encryptedData = ByteArrayOutputStream().apply {
            val encryptStream = BackupCryptoEngine.wrapEncryptStream(this, "testPass".toCharArray())
            encryptStream.write("{}".toByteArray(Charsets.UTF_8))
            encryptStream.close()
        }.toByteArray()
        val nativeFormat = foo.barz.wallpaperpicker.core.backup.BackupManager.detectBackupFormat(ByteArrayInputStream(encryptedData))
        assertEquals(foo.barz.wallpaperpicker.core.backup.BackupFormat.NATIVE_ENCRYPTED, nativeFormat)

        // OpenPGP Armor
        val asciiArmored = "-----BEGIN PGP MESSAGE-----\n\n...".toByteArray(Charsets.US_ASCII)
        val openPgpFormat = foo.barz.wallpaperpicker.core.backup.BackupManager.detectBackupFormat(ByteArrayInputStream(asciiArmored))
        assertEquals(foo.barz.wallpaperpicker.core.backup.BackupFormat.OPENPGP, openPgpFormat)

        // Plaintext JSON
        val plainJson = "{\"schemaVersion\": 1, \"preferences\": {}}".toByteArray(Charsets.UTF_8)
        val plaintextFormat = foo.barz.wallpaperpicker.core.backup.BackupManager.detectBackupFormat(ByteArrayInputStream(plainJson))
        assertEquals(foo.barz.wallpaperpicker.core.backup.BackupFormat.PLAINTEXT, plaintextFormat)
    }

    @Test
    fun testFavoriteFileNameRoundTrip() {
        val overrideWithFile = BackupHistoryOverride(
            sourceUri = "https://example.com/art.jpg",
            title = "Offline Artwork",
            sourceType = WallpaperSourceType.HTTP_API,
            isFavorite = true,
            favoriteFileName = "fav_123456789.jpg"
        )
        val json = overrideWithFile.toJson()
        val parsed = BackupHistoryOverride.fromJson(json)
        assertEquals("fav_123456789.jpg", parsed.favoriteFileName)
        assertEquals("Offline Artwork", parsed.title)

        val overrideWithoutFile = BackupHistoryOverride(
            sourceUri = "https://example.com/art2.jpg",
            title = "No File Artwork",
            sourceType = WallpaperSourceType.HTTP_API,
            isFavorite = true,
            favoriteFileName = null
        )
        val jsonWithout = overrideWithoutFile.toJson()
        val parsedWithout = BackupHistoryOverride.fromJson(jsonWithout)
        assertEquals(null, parsedWithout.favoriteFileName)
    }

    @Test
    fun testIsZipStreamDetection() {
        // Valid ZIP magic header PK\x03\x04
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        assertTrue(foo.barz.wallpaperpicker.core.backup.BackupManager.isZipStream(ByteArrayInputStream(zipHeader)))

        // Non-ZIP bytes
        val nonZip = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
        assertFalse(foo.barz.wallpaperpicker.core.backup.BackupManager.isZipStream(ByteArrayInputStream(nonZip)))

        val jsonBytes = "{\"schema\": 1}".toByteArray(Charsets.UTF_8)
        assertFalse(foo.barz.wallpaperpicker.core.backup.BackupManager.isZipStream(ByteArrayInputStream(jsonBytes)))

        val shortBytes = byteArrayOf(0x50, 0x4B)
        assertFalse(foo.barz.wallpaperpicker.core.backup.BackupManager.isZipStream(ByteArrayInputStream(shortBytes)))
    }

    @Test
    fun testZipArchiveStructure() {
        val baos = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("backup.json"))
            zos.write("{\"schemaVersion\": 1}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("favorites/test_fav.jpg"))
            zos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            zos.closeEntry()
        }

        val zipBytes = baos.toByteArray()
        val inStream = ByteArrayInputStream(zipBytes)
        assertTrue(foo.barz.wallpaperpicker.core.backup.BackupManager.isZipStream(inStream))

        // Verify entries
        val entryNames = mutableListOf<String>()
        java.util.zip.ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        assertEquals(listOf("backup.json", "favorites/test_fav.jpg"), entryNames)
    }
}
