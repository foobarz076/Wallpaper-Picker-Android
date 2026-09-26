package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.backup.BackupCryptoEngine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupCryptoEngineTest {

    @Test
    fun testEncryptAndDecryptRoundTrip() {
        val originalText = "Hello, Wallpaper Picker! 这是一个备份测试数据。" * 50
        val password = "StrongPassword123!".toCharArray()

        val rawOut = ByteArrayOutputStream()
        val encryptStream = BackupCryptoEngine.wrapEncryptStream(rawOut, password)
        encryptStream.write(originalText.toByteArray(Charsets.UTF_8))
        encryptStream.close()

        val encryptedBytes = rawOut.toByteArray()
        assertTrue(encryptedBytes.size > 33, "Ciphertext with header must be longer than 33 bytes")
        assertTrue(BackupCryptoEngine.isWpbkEncrypted(encryptedBytes))

        val rawIn = ByteArrayInputStream(encryptedBytes)
        val decryptStream = BackupCryptoEngine.wrapDecryptStream(rawIn, password)
        val decryptedText = decryptStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testWrongPasswordThrowsException() {
        val originalText = "Secret configuration data"
        val password = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val rawOut = ByteArrayOutputStream()
        val encryptStream = BackupCryptoEngine.wrapEncryptStream(rawOut, password)
        encryptStream.write(originalText.toByteArray(Charsets.UTF_8))
        encryptStream.close()

        val encryptedBytes = rawOut.toByteArray()
        val rawIn = ByteArrayInputStream(encryptedBytes)

        assertFailsWith<Exception> {
            val decryptStream = BackupCryptoEngine.wrapDecryptStream(rawIn, wrongPassword)
            decryptStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }

    @Test
    fun testTamperedCiphertextThrowsException() {
        val originalText = "Tamper detection test"
        val password = "Password".toCharArray()

        val rawOut = ByteArrayOutputStream()
        val encryptStream = BackupCryptoEngine.wrapEncryptStream(rawOut, password)
        encryptStream.write(originalText.toByteArray(Charsets.UTF_8))
        encryptStream.close()

        val encryptedBytes = rawOut.toByteArray()
        // Flip a bit in the ciphertext area (after header of 33 bytes)
        val idx = encryptedBytes.size - 5
        encryptedBytes[idx] = (encryptedBytes[idx].toInt() xor 0x01).toByte()

        val rawIn = ByteArrayInputStream(encryptedBytes)
        assertFailsWith<Exception> {
            val decryptStream = BackupCryptoEngine.wrapDecryptStream(rawIn, password)
            decryptStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }

    private operator fun String.times(n: Int): String {
        val sb = StringBuilder()
        repeat(n) { sb.append(this) }
        return sb.toString()
    }
}
