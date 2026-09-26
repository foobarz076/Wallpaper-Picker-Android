package foo.barz.wallpaperpicker.core.backup

import android.os.Build
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Lightweight, zero-dependency streaming crypto engine.
 * Employs AES-256-GCM authenticated encryption paired with PBKDF2 key derivation
 * and transparent GZIP compression.
 *
 * ## Binary File Format
 *
 * ### Version 1 (legacy, read-only)
 * ```
 * [Magic 4B: 'W','P','B','K'] [Version 1B: 0x01] [Salt 16B] [IV 12B] [Ciphertext+GCM-Tag]
 * ```
 * KDF algorithm is implicitly PBKDF2WithHmacSHA256. Version 1 files were only ever produced
 * on API 26+ devices (PBKDF2WithHmacSHA256 unavailable below API 26), so this assumption holds.
 *
 * ### Version 2 (current)
 * ```
 * [Magic 4B: 'W','P','B','K'] [Version 1B: 0x02] [KdfId 1B] [Salt 16B] [IV 12B] [Ciphertext+GCM-Tag]
 * ```
 * KDF algorithm is explicitly encoded in KdfId, enabling safe cross-device restore:
 * - KdfId 0x01 → PBKDF2WithHmacSHA1   (used on API 23-25)
 * - KdfId 0x02 → PBKDF2WithHmacSHA256 (used on API 26+)
 */
object BackupCryptoEngine {

    val MAGIC_HEADER = byteArrayOf('W'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())

    /** Version written by this build. All new backups use v2. */
    const val CURRENT_VERSION: Byte = 0x02

    /** Legacy version supported for decryption only. */
    private const val VERSION_1: Int = 0x01

    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val AES_KEY_LENGTH_BITS = 256

    /** KDF algorithm identifiers stored in the v2 header. */
    private const val KDF_ID_PBKDF2_SHA1: Byte = 0x01
    private const val KDF_ID_PBKDF2_SHA256: Byte = 0x02

    /**
     * Checks if the given byte array starts with the WPBK magic header.
     */
    fun isWpbkEncrypted(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < MAGIC_HEADER.size) return false
        for (i in MAGIC_HEADER.indices) {
            if (headerBytes[i] != MAGIC_HEADER[i]) return false
        }
        return true
    }

    /**
     * Wraps [rawOutput] with AES-256-GCM encryption and GZIP compression.
     *
     * Writes a v2 binary header (Magic 4B + Version 1B + KdfId 1B + Salt 16B + IV 12B)
     * then returns a stream whose writes will be compressed and encrypted.
     */
    fun wrapEncryptStream(rawOutput: OutputStream, passwordChars: CharArray): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }
        val kdfId = currentKdfId()

        // Write v2 header: [Magic 4B] + [Version 1B] + [KdfId 1B] + [Salt 16B] + [IV 12B]
        rawOutput.write(MAGIC_HEADER)
        rawOutput.write(CURRENT_VERSION.toInt())
        rawOutput.write(kdfId.toInt())
        rawOutput.write(salt)
        rawOutput.write(iv)
        rawOutput.flush()

        val secretKey = deriveKey(passwordChars, salt, kdfId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return GZIPOutputStream(CipherOutputStream(rawOutput, cipher))
    }

    /**
     * Reads the binary header from [rawInput], verifies magic and version, derives the AES key
     * using the algorithm recorded in the header, then returns a decrypted, decompressed stream.
     *
     * Supports both v1 (implicit SHA-256 KDF) and v2 (explicit KDF id) headers.
     *
     * Note: Uses [Cipher.doFinal] internally rather than [javax.crypto.CipherInputStream] to
     * ensure [javax.crypto.AEADBadTagException] is always propagated (CipherInputStream
     * silently swallows authentication failures on many Android versions — JDK-8016171).
     *
     * @throws IllegalArgumentException if the magic or version bytes are invalid.
     * @throws javax.crypto.AEADBadTagException if the password is wrong or the file is corrupted.
     * @throws java.io.EOFException if the stream is truncated.
     */
    fun wrapDecryptStream(rawInput: InputStream, passwordChars: CharArray): InputStream {
        val magic = ByteArray(MAGIC_HEADER.size)
        readFully(rawInput, magic)
        if (!isWpbkEncrypted(magic)) {
            throw IllegalArgumentException("Not a valid WPBK encrypted backup file")
        }

        val version = rawInput.read()
        val kdfId: Byte
        when (version) {
            VERSION_1 -> {
                // v1 header has no KdfId byte. PBKDF2WithHmacSHA256 was the only algorithm
                // ever used (v1 was only produced on API 26+ where SHA-256 is available).
                kdfId = KDF_ID_PBKDF2_SHA256
            }
            CURRENT_VERSION.toInt() -> {
                // v2 header: read the explicit KDF algorithm identifier.
                val kdfByte = rawInput.read()
                if (kdfByte == -1) throw java.io.EOFException("Premature end of backup stream while reading KDF id")
                kdfId = kdfByte.toByte()
            }
            else -> throw IllegalArgumentException("Unsupported backup file version: $version")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { readFully(rawInput, it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { readFully(rawInput, it) }

        val secretKey = deriveKey(passwordChars, salt, kdfId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        // Use doFinal instead of CipherInputStream so that AEADBadTagException (wrong password
        // or tampered ciphertext) is always thrown rather than silently returning empty bytes.
        val ciphertext = rawInput.readBytes()
        val plaintext = cipher.doFinal(ciphertext)
        return GZIPInputStream(ByteArrayInputStream(plaintext))
    }

    // ---- Private helpers ----

    /**
     * Returns the KDF algorithm identifier appropriate for the current device's API level.
     * PBKDF2WithHmacSHA256 is only available on API 26+; SHA-1 is used as a fallback.
     */
    private fun currentKdfId(): Byte =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) KDF_ID_PBKDF2_SHA256
        else KDF_ID_PBKDF2_SHA1

    /**
     * Derives a 256-bit AES key from [passwordChars] and [salt] using the PBKDF2 variant
     * identified by [kdfId].
     *
     * @throws IllegalArgumentException for unrecognised [kdfId] values.
     */
    private fun deriveKey(passwordChars: CharArray, salt: ByteArray, kdfId: Byte): SecretKeySpec {
        val algorithm = when (kdfId) {
            KDF_ID_PBKDF2_SHA1 -> "PBKDF2WithHmacSHA1"
            KDF_ID_PBKDF2_SHA256 -> "PBKDF2WithHmacSHA256"
            else -> throw IllegalArgumentException("Unknown KDF id in backup header: 0x${kdfId.toInt().and(0xFF).toString(16)}")
        }
        val keySpec = PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS)
        val keyBytes = SecretKeyFactory.getInstance(algorithm).generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val bytesRead = input.read(buffer, offset, buffer.size - offset)
            if (bytesRead == -1) throw java.io.EOFException("Premature end of backup stream while reading header")
            offset += bytesRead
        }
    }
}
