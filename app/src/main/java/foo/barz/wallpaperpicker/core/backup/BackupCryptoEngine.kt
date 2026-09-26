package foo.barz.wallpaperpicker.core.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Lightweight, zero-dependency streaming crypto engine.
 * Employs AES-256-GCM authenticated encryption paired with PBKDF2WithHmacSHA256
 * key derivation and transparent GZIP compression.
 */
object BackupCryptoEngine {

    val MAGIC_HEADER = byteArrayOf('W'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    const val CURRENT_VERSION: Byte = 0x01

    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val AES_KEY_LENGTH_BITS = 256

    /**
     * Checks if the given input stream starts with the WPBK magic header without consuming
     * if supported, or checks the first 4 bytes of a mark-supported stream.
     */
    fun isWpbkEncrypted(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < MAGIC_HEADER.size) return false
        for (i in MAGIC_HEADER.indices) {
            if (headerBytes[i] != MAGIC_HEADER[i]) return false
        }
        return true
    }

    /**
     * Wraps an existing OutputStream with AES-GCM encryption and GZIP compression.
     * Writes the binary header (Magic 4B + Version 1B + Salt 16B + IV 12B) to the output stream.
     *
     * Data written to the returned OutputStream will be compressed and encrypted.
     */
    fun wrapEncryptStream(rawOutput: OutputStream, passwordChars: CharArray): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)

        // Write header: [Magic 4B] + [Version 1B] + [Salt 16B] + [IV 12B]
        rawOutput.write(MAGIC_HEADER)
        rawOutput.write(CURRENT_VERSION.toInt())
        rawOutput.write(salt)
        rawOutput.write(iv)
        rawOutput.flush()

        val secretKey = deriveKey(passwordChars, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val cipherOut = CipherOutputStream(rawOutput, cipher)
        return GZIPOutputStream(cipherOut)
    }

    /**
     * Reads the binary header from rawInput, verifies magic and version, derives the key,
     * and returns a decompressed, decrypted InputStream.
     *
     * Throws GeneralSecurityException or IOException if header is invalid, password is wrong,
     * or data is corrupted.
     */
    fun wrapDecryptStream(rawInput: InputStream, passwordChars: CharArray): InputStream {
        val magic = ByteArray(MAGIC_HEADER.size)
        readFully(rawInput, magic)
        if (!isWpbkEncrypted(magic)) {
            throw IllegalArgumentException("Not a valid WPBK encrypted backup file")
        }

        val version = rawInput.read()
        if (version != CURRENT_VERSION.toInt()) {
            throw IllegalArgumentException("Unsupported backup file version: $version")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        readFully(rawInput, salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        readFully(rawInput, iv)

        val secretKey = deriveKey(passwordChars, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val cipherIn = CipherInputStream(rawInput, cipher)
        return GZIPInputStream(cipherIn)
    }

    private fun deriveKey(passwordChars: CharArray, salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val bytesRead = input.read(buffer, offset, buffer.size - offset)
            if (bytesRead == -1) {
                throw java.io.EOFException("Premature end of backup stream while reading header")
            }
            offset += bytesRead
        }
    }
}
