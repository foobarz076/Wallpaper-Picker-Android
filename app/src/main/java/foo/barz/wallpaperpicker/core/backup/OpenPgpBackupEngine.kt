package foo.barz.wallpaperpicker.core.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import foo.barz.wallpaperpicker.core.util.AppLog
import org.openintents.openpgp.IOpenPgpService2
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import org.openintents.openpgp.util.OpenPgpServiceConnection
import java.io.InputStream
import java.io.OutputStream

/**
 * Result of an OpenPGP cryptographic operation (encryption, decryption, or key resolution).
 *
 * @property resultIntent Populated with the raw API result intent on success, allowing callers to
 *     extract extras such as [OpenPgpApi.RESULT_SIGN_KEY_ID] after a key-selection operation.
 */
sealed class OpenPgpOperationResult {
    data class Success(val resultIntent: Intent? = null) : OpenPgpOperationResult()
    data class UserInteractionRequired(val pendingIntent: PendingIntent) : OpenPgpOperationResult()
    data class Error(val message: String) : OpenPgpOperationResult()
}

/**
 * Engine encapsulating OpenPGP protocol interaction with OpenKeychain (or compatible OpenPGP providers).
 */
object OpenPgpBackupEngine {

    private const val TAG = "OpenPgpBackupEngine"
    const val OPENKEYCHAIN_PACKAGE = "org.sufficientlysecure.keychain"

    /**
     * Checks if OpenKeychain or any compatible OpenPGP provider is installed on the device.
     */
    fun isProviderInstalled(context: Context): Boolean {
        return runCatching {
            val intent = Intent(OpenPgpApi.SERVICE_INTENT_2)
            intent.setPackage(OPENKEYCHAIN_PACKAGE)
            val packageManager = context.packageManager
            val list: List<ResolveInfo>? = packageManager.queryIntentServices(intent, 0)
            if (!list.isNullOrEmpty()) {
                return@runCatching true
            }
            // Fallback check for any OpenPGP API v2 compatible service
            val genericList: List<ResolveInfo>? = packageManager.queryIntentServices(Intent(OpenPgpApi.SERVICE_INTENT_2), 0)
            !genericList.isNullOrEmpty()
        }.getOrDefault(false)
    }

    /**
     * Resolves the primary OpenPGP provider package name.
     */
    fun getProviderPackageName(context: Context): String? {
        return runCatching {
            val intent = Intent(OpenPgpApi.SERVICE_INTENT_2)
            intent.setPackage(OPENKEYCHAIN_PACKAGE)
            val packageManager = context.packageManager
            val list: List<ResolveInfo>? = packageManager.queryIntentServices(intent, 0)
            if (!list.isNullOrEmpty()) {
                return@runCatching OPENKEYCHAIN_PACKAGE
            }
            val genericList: List<ResolveInfo>? = packageManager.queryIntentServices(Intent(OpenPgpApi.SERVICE_INTENT_2), 0)
            genericList?.firstOrNull()?.serviceInfo?.packageName
        }.getOrNull()
    }

    /**
     * Creates an OpenPgpServiceConnection for asynchronous service binding.
     */
    fun createServiceConnection(
        context: Context,
        onBound: (OpenPgpApi) -> Unit,
        onError: (Exception) -> Unit = {}
    ): OpenPgpServiceConnection? {
        val packageName = getProviderPackageName(context) ?: return null
        return OpenPgpServiceConnection(
            context,
            packageName,
            object : OpenPgpServiceConnection.OnBound {
                override fun onBound(service: IOpenPgpService2) {
                    AppLog.i(TAG, "Successfully bound to OpenPgpService ($packageName)")
                    onBound(OpenPgpApi(context, service))
                }

                override fun onError(e: Exception) {
                    AppLog.e(TAG, "Error binding to OpenPgpService", e)
                    onError(e)
                }
            }
        )
    }

    /**
     * Detects if the given stream begins with OpenPGP ASCII Armor or binary packet headers.
     */
    fun isOpenPgpMessage(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) {
            return false
        }
        val buffer = ByteArray(128)
        inputStream.mark(buffer.size)
        val read = inputStream.read(buffer)
        inputStream.reset()

        if (read < 4) return false

        // Check for ASCII Armor: "-----BEGIN PGP MESSAGE-----"
        val headerStr = String(buffer, 0, read, Charsets.US_ASCII)
        if (headerStr.contains("-----BEGIN PGP")) {
            return true
        }

        // Check for OpenPGP binary packet headers (RFC 4880 / RFC 9580)
        // Bit 7 must always be 1 (byte >= 0x80 or byte < 0 when signed)
        val firstByte = buffer[0].toInt() and 0xFF
        if ((firstByte and 0x80) != 0) {
            val isOldFormat = (firstByte and 0x40) == 0
            val tag = if (isOldFormat) {
                (firstByte and 0x3C) shr 2
            } else {
                firstByte and 0x3F
            }
            // Tags: 1 = PKESK, 3 = SKESK, 9 = Symmetrically Encrypted Data, 18 = SEIPD
            if (tag == 1 || tag == 3 || tag == 9 || tag == 18) {
                return true
            }
        }

        return false
    }

    /**
     * Resolves or interactively selects the user's signing key from OpenKeychain.
     *
     * On the first call (resumeIntent = null), OpenKeychain typically returns
     * [OpenPgpOperationResult.UserInteractionRequired] prompting the user to select or confirm
     * their signing key in OpenKeychain's UI. After the user completes the interaction and the
     * caller re-invokes this function with the activity result as [resumeIntent], OpenKeychain
     * returns [OpenPgpOperationResult.Success] with [OpenPgpApi.RESULT_SIGN_KEY_ID] in the
     * result intent.
     *
     * Note: Both input and output are null for this operation — no data is streamed.
     */
    fun getSignKeyId(
        api: OpenPgpApi,
        resumeIntent: Intent? = null
    ): OpenPgpOperationResult {
        val data = resumeIntent ?: Intent(OpenPgpApi.ACTION_GET_SIGN_KEY_ID).apply {
            putExtra(OpenPgpApi.EXTRA_API_VERSION, OpenPgpApi.API_VERSION)
            // Empty user ID — OpenKeychain will display its own key-picker UI
            putExtra(OpenPgpApi.EXTRA_USER_ID, "")
        }
        // Key resolution does not stream any data
        val resultIntent = api.executeApi(data, null as InputStream?, null as OutputStream?)
        return handleApiResult(resultIntent)
    }

    /**
     * Encrypts data from input stream to output stream using OpenPGP API.
     *
     * [keyIds] must contain at least one key ID previously obtained via [getSignKeyId].
     * Passing an empty array will cause OpenKeychain to return CLIENT_SIDE_ERROR(-1) because
     * it cannot determine which public key to use for encryption without explicit recipients.
     */
    fun executeEncrypt(
        api: OpenPgpApi,
        rawInput: InputStream,
        encryptedOutput: OutputStream,
        keyIds: LongArray,
        signKeyId: Long? = null,
        asciiArmor: Boolean = true,
        resumeIntent: Intent? = null
    ): OpenPgpOperationResult {
        val data = resumeIntent ?: Intent(
            if (signKeyId != null) OpenPgpApi.ACTION_SIGN_AND_ENCRYPT else OpenPgpApi.ACTION_ENCRYPT
        ).apply {
            putExtra(OpenPgpApi.EXTRA_API_VERSION, OpenPgpApi.API_VERSION)
            putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIds)
            if (signKeyId != null) {
                putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, signKeyId)
            }
            putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, asciiArmor)
            putExtra(OpenPgpApi.EXTRA_ENABLE_COMPRESSION, true)
        }

        val resultIntent = api.executeApi(data, rawInput, encryptedOutput)
        return handleApiResult(resultIntent)
    }

    /**
     * Decrypts OpenPGP encrypted data from input stream to output stream.
     */
    fun executeDecrypt(
        api: OpenPgpApi,
        encryptedInput: InputStream,
        decryptedOutput: OutputStream,
        resumeIntent: Intent? = null
    ): OpenPgpOperationResult {
        val data = resumeIntent ?: Intent(OpenPgpApi.ACTION_DECRYPT_VERIFY).apply {
            putExtra(OpenPgpApi.EXTRA_API_VERSION, OpenPgpApi.API_VERSION)
        }

        val resultIntent = api.executeApi(data, encryptedInput, decryptedOutput)
        return handleApiResult(resultIntent)
    }

    private fun handleApiResult(resultIntent: Intent): OpenPgpOperationResult {
        return when (resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                // Pass the full result intent back so callers can extract extras
                // (e.g. RESULT_SIGN_KEY_ID after a key-selection operation)
                OpenPgpOperationResult.Success(resultIntent)
            }

            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                val pendingIntent = androidx.core.content.IntentCompat.getParcelableExtra(
                    resultIntent,
                    OpenPgpApi.RESULT_INTENT,
                    PendingIntent::class.java
                )
                if (pendingIntent != null) {
                    OpenPgpOperationResult.UserInteractionRequired(pendingIntent)
                } else {
                    OpenPgpOperationResult.Error("OpenKeychain 请求用户交互，但未提供有效的 PendingIntent")
                }
            }

            OpenPgpApi.RESULT_CODE_ERROR -> {
                val error = androidx.core.content.IntentCompat.getParcelableExtra(
                    resultIntent,
                    OpenPgpApi.RESULT_ERROR,
                    OpenPgpError::class.java
                )
                val msg = if (error != null) {
                    "OpenPGP error: ${error.message} (errorId=${error.errorId})"
                } else {
                    "OpenPGP operation failed — service may have disconnected or crashed"
                }
                AppLog.e(TAG, msg)
                OpenPgpOperationResult.Error(
                    if (error != null) "OpenPGP 操作失败: ${error.message} (errorId=${error.errorId})"
                    else "OpenPGP 操作失败（服务异常或连接中断）"
                )
            }

            else -> OpenPgpOperationResult.Error("未知 OpenPgpApi 返回状态")
        }
    }
}
