package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.backup.BackupCryptoEngine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupCryptoEngineTest {

    // ---- Round-trip tests ----

    @Test
    fun testEncryptAndDecryptRoundTrip() {
        val originalText = "Hello, Wallpaper Picker! 这是一个备份测试数据。" * 50
        val password = "StrongPassword123!".toCharArray()

        val rawOut = ByteArrayOutputStream()
        BackupCryptoEngine.wrapEncryptStream(rawOut, password).use { it.write(originalText.toByteArray(Charsets.UTF_8)) }

        val encryptedBytes = rawOut.toByteArray()
        // v2 header: 4 (magic) + 1 (version) + 1 (kdfId) + 16 (salt) + 12 (iv) = 34 bytes minimum
        assertTrue(encryptedBytes.size > 34, "Ciphertext with v2 header must be longer than 34 bytes")
        assertTrue(BackupCryptoEngine.isWpbkEncrypted(encryptedBytes))

        val decryptedText = BackupCryptoEngine
            .wrapDecryptStream(ByteArrayInputStream(encryptedBytes), password)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testEncryptAndDecryptLargeBinaryPayload() {
        val original = ByteArray(512 * 1024) { it.toByte() }
        val password = "BinaryTestPass".toCharArray()

        val rawOut = ByteArrayOutputStream()
        BackupCryptoEngine.wrapEncryptStream(rawOut, password).use { it.write(original) }

        val decrypted = BackupCryptoEngine
            .wrapDecryptStream(ByteArrayInputStream(rawOut.toByteArray()), password)
            .use { it.readBytes() }

        assertTrue(original.contentEquals(decrypted), "Decrypted bytes must match original")
    }

    // ---- Wrong password / tamper detection ----

    @Test
    fun testWrongPasswordThrowsException() {
        val password = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val rawOut = ByteArrayOutputStream()
        BackupCryptoEngine.wrapEncryptStream(rawOut, password).use { it.write("Secret data".toByteArray()) }

        assertFailsWith<Exception>("Wrong password must throw") {
            BackupCryptoEngine
                .wrapDecryptStream(ByteArrayInputStream(rawOut.toByteArray()), wrongPassword)
                .use { it.readBytes() }
        }
    }

    @Test
    fun testTamperedCiphertextThrowsException() {
        val password = "Password".toCharArray()

        val rawOut = ByteArrayOutputStream()
        BackupCryptoEngine.wrapEncryptStream(rawOut, password).use { it.write("Tamper detection test".toByteArray()) }

        val encrypted = rawOut.toByteArray()
        // Flip a bit near the end (inside the ciphertext / GCM tag region)
        val idx = encrypted.size - 5
        encrypted[idx] = (encrypted[idx].toInt() xor 0x01).toByte()

        assertFailsWith<Exception>("Tampered ciphertext must throw") {
            BackupCryptoEngine
                .wrapDecryptStream(ByteArrayInputStream(encrypted), password)
                .use { it.readBytes() }
        }
    }

    // ---- v2 header format ----

    @Test
    fun testV2HeaderVersionByte() {
        val rawOut = ByteArrayOutputStream()
        BackupCryptoEngine.wrapEncryptStream(rawOut, "pass".toCharArray()).use { it.write(42) }

        val bytes = rawOut.toByteArray()
        // Byte index 4 is the version field (after 4-byte magic)
        assertEquals(0x02, bytes[4].toInt() and 0xFF, "New backups must use version 0x02")
    }

    @Test
    fun testV2HeaderKdfIdByteIsPresent() {
        val rawOut = ByteArrayOutputStream()
        BackupCryptoEngine.wrapEncryptStream(rawOut, "pass".toCharArray()).use { it.write(42) }

        val bytes = rawOut.toByteArray()
        // Byte index 5 is the KdfId field in v2. Must be 0x01 (SHA-1) or 0x02 (SHA-256).
        val kdfId = bytes[5].toInt() and 0xFF
        assertTrue(kdfId == 0x01 || kdfId == 0x02, "KdfId must be 0x01 or 0x02, got 0x${kdfId.toString(16)}")
    }

    // ---- v1 backward compatibility (simulated) ----

    /**
     * Constructs a synthetic v1-format ciphertext and verifies that [wrapDecryptStream]
     * can still decrypt it correctly, assuming PBKDF2WithHmacSHA256 as the implicit KDF.
     *
     * This test runs with PBKDF2WithHmacSHA256 directly (javax.crypto), so it will only
     * pass on API 26+ hosts — matching exactly where v1 backups were produced.
     */
    @Test
    fun testV1BackupDecryptedWithImplicitSha256() {
        val password = "LegacyPassword".toCharArray()
        val plaintext = "v1 legacy backup content"

        // Build a synthetic v1 blob manually replicating the old write logic
        val v1Blob = buildLegacyV1Blob(plaintext.toByteArray(Charsets.UTF_8), password)

        // wrapDecryptStream must handle the v1 header transparently
        val decrypted = BackupCryptoEngine
            .wrapDecryptStream(ByteArrayInputStream(v1Blob), password)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testInvalidMagicThrowsIllegalArgumentException() {
        val garbage = "not a backup file at all".toByteArray()
        assertFailsWith<IllegalArgumentException> {
            BackupCryptoEngine.wrapDecryptStream(ByteArrayInputStream(garbage), "pw".toCharArray())
                .use { it.readBytes() }
        }
    }

    @Test
    fun testUnsupportedVersionThrowsIllegalArgumentException() {
        // Craft a header with a future version byte (0x99) to verify rejection
        val header = BackupCryptoEngine.MAGIC_HEADER + byteArrayOf(0x99.toByte())
        assertFailsWith<IllegalArgumentException> {
            BackupCryptoEngine.wrapDecryptStream(ByteArrayInputStream(header), "pw".toCharArray())
                .use { it.readBytes() }
        }
    }

    // ---- Helpers ----

    /**
     * Builds a synthetic v1-format encrypted blob using raw JCE to simulate a backup produced
     * before the v2 header was introduced.
     *
     * v1 layout: [Magic 4B][0x01 1B][Salt 16B][IV 12B][Ciphertext+GCM-Tag]
     * KDF: PBKDF2WithHmacSHA256 (implicit)
     */
    private fun buildLegacyV1Blob(plaintext: ByteArray, password: CharArray): ByteArray {
        val random = java.security.SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }

        // Derive key using SHA-256 (the only algorithm v1 ever used)
        val keySpec = javax.crypto.spec.PBEKeySpec(password, salt, 100_000, 256)
        val keyBytes = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(keySpec).encoded
        val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))

        val gzipped = ByteArrayOutputStream().also { gzOut ->
            java.util.zip.GZIPOutputStream(gzOut).use { it.write(plaintext) }
        }.toByteArray()
        val ciphertext = cipher.doFinal(gzipped)

        // Assemble v1 header + ciphertext
        return ByteArrayOutputStream().also { out ->
            out.write(BackupCryptoEngine.MAGIC_HEADER)
            out.write(0x01)    // version = 1
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
        }.toByteArray()
    }

    private operator fun String.times(n: Int): String = repeat(n)
}
