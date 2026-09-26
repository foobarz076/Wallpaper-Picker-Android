package foo.barz.wallpaperpicker.core.backup

import android.content.Context
import foo.barz.wallpaperpicker.BuildConfig
import foo.barz.wallpaperpicker.core.database.ScheduleRulesDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.util.AppLog
import foo.barz.wallpaperpicker.core.worker.WallpaperAlarmScheduler
import foo.barz.wallpaperpicker.core.worker.WallpaperWorker
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Result of a backup restoration operation.
 */
data class RestoreResult(
    val success: Boolean,
    val restoredSourcesCount: Int = 0,
    val restoredRulesCount: Int = 0,
    val restoredFavoritesCount: Int = 0,
    val needsReauthorizationCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Central orchestrator for packaging, encrypting, inspecting, and restoring app configurations.
 */
object BackupManager {

    private const val TAG = "BackupManager"

    /**
     * Checks if the given input stream starts with WPBK encrypted magic header.
     * Uses mark/reset to avoid consuming stream contents.
     */
    fun isEncryptedBackup(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) {
            return false
        }
        inputStream.mark(BackupCryptoEngine.MAGIC_HEADER.size)
        val header = ByteArray(BackupCryptoEngine.MAGIC_HEADER.size)
        val read = inputStream.read(header)
        inputStream.reset()
        return read == header.size && BackupCryptoEngine.isWpbkEncrypted(header)
    }

    /**
     * Builds and exports the application backup payload to the given OutputStream.
     *
     * @param context Application context.
     * @param outputStream Destination stream (e.g., from SAF URI).
     * @param password Optional password for AES-256-GCM encryption. If null or blank, exported unencrypted.
     * @param sanitize If true, strips API keys and sensitive query tokens before export.
     */
    fun exportBackup(
        context: Context,
        outputStream: OutputStream,
        password: String? = null,
        sanitize: Boolean = false
    ): BackupPayload {
        AppLog.i(TAG, "Exporting backup (encrypted=${!password.isNullOrBlank()}, sanitize=$sanitize)")
        val prefs = PreferencesManager(context)
        val sourcesDb = WallpaperSourcesDatabase(context)
        val rulesDb = ScheduleRulesDatabase(context)
        val historyDb = WallpaperHistoryDatabase(context)

        val sources = sourcesDb.getAllSources()
        val rules = rulesDb.getAllRules()
        val historyOverrides = historyDb.getFavoriteAndCustomOverrides().map { item ->
            BackupHistoryOverride(
                sourceUri = item.sourceUri.toString(),
                title = item.title,
                sourceType = item.sourceType,
                sourceTitle = item.sourceTitle,
                isFavorite = item.isFavorite,
                favoriteTimestamp = item.favoriteTimestamp,
                customScrollMode = item.customScrollMode,
                cropFocusX = item.cropFocusX,
                cropFocusY = item.cropFocusY,
                flipHorizontal = item.flipHorizontal,
                remoteUrl = item.remoteUrl
            )
        }

        var payload = BackupPayload(
            schemaVersion = 1,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            createdAt = System.currentTimeMillis(),
            preferences = prefs.exportBackupPreferences(),
            sources = sources,
            rules = rules,
            historyOverrides = historyOverrides
        )

        if (sanitize) {
            payload = payload.sanitize()
        }

        val jsonStr = payload.toJson()

        if (!password.isNullOrBlank()) {
            val encryptStream = BackupCryptoEngine.wrapEncryptStream(outputStream, password.toCharArray())
            encryptStream.use {
                it.write(jsonStr.toByteArray(Charsets.UTF_8))
                it.flush()
            }
        } else {
            outputStream.use {
                it.write(jsonStr.toByteArray(Charsets.UTF_8))
                it.flush()
            }
        }

        AppLog.i(TAG, "Backup successfully exported with ${sources.size} sources and ${rules.size} rules")
        return payload
    }

    /**
     * Restores application preferences, sources, schedule rules, and favorites from a backup stream.
     */
    fun restoreBackup(
        context: Context,
        rawInput: InputStream,
        password: String? = null
    ): RestoreResult {
        AppLog.i(TAG, "Starting backup restoration")
        val bufferedInput = if (rawInput.markSupported()) rawInput else BufferedInputStream(rawInput)

        val isEncrypted = isEncryptedBackup(bufferedInput)
        val jsonStr = try {
            if (isEncrypted) {
                if (password.isNullOrBlank()) {
                    return RestoreResult(
                        success = false,
                        errorMessage = "此备份文件已加密，需要输入恢复密码"
                    )
                }
                val decryptStream = BackupCryptoEngine.wrapDecryptStream(bufferedInput, password.toCharArray())
                decryptStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                bufferedInput.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to read or decrypt backup stream", e)
            val message = if (isEncrypted) "解密失败：密码错误或文件已损坏" else "读取备份文件失败：${e.localizedMessage ?: "格式不正确"}"
            return RestoreResult(success = false, errorMessage = message)
        }

        val payload = try {
            BackupPayload.fromJson(jsonStr)
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to parse backup JSON payload", e)
            return RestoreResult(success = false, errorMessage = "解析备份数据失败：JSON 格式无效")
        }

        val prefs = PreferencesManager(context)
        val sourcesDb = WallpaperSourcesDatabase(context)
        val rulesDb = ScheduleRulesDatabase(context)
        val historyDb = WallpaperHistoryDatabase(context)

        // 1. Restore Preferences
        prefs.importBackupPreferences(payload.preferences)

        // 2. Restore Sources & check SAF permissions
        sourcesDb.clearAll()
        var needsReauthorizationCount = 0
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions

        payload.sources.forEach { source ->
            sourcesDb.insertSource(source)
            if (source.type == WallpaperSourceType.LOCAL_FOLDER) {
                val config = LocalFolderSourceConfig.fromJson(source.configJson)
                if (config.folderUri.isNotBlank()) {
                    val hasPermission = persistedUriPermissions.any {
                        it.uri.toString() == config.folderUri && it.isReadPermission
                    }
                    if (!hasPermission) {
                        needsReauthorizationCount++
                    }
                }
            }
        }

        // 3. Restore Schedule Rules
        rulesDb.clearAllRules()
        payload.rules.forEach { rule ->
            rulesDb.insertRule(rule)
        }

        // 4. Restore Favorite and Framing Overrides
        payload.historyOverrides.forEach { override ->
            historyDb.restoreFavoriteOverride(
                sourceUri = override.sourceUri,
                title = override.title,
                sourceType = override.sourceType,
                sourceTitle = override.sourceTitle,
                isFavorite = override.isFavorite,
                favoriteTimestamp = override.favoriteTimestamp,
                customScrollMode = override.customScrollMode,
                cropFocusX = override.cropFocusX,
                cropFocusY = override.cropFocusY,
                flipHorizontal = override.flipHorizontal,
                remoteUrl = override.remoteUrl
            )
        }

        // 5. Reschedule automation
        if (prefs.isScheduled) {
            if (prefs.exactTimerEnabled) {
                WallpaperAlarmScheduler.schedule(context)
            } else {
                WallpaperWorker.schedule(context, prefs.intervalMinutes)
            }
        } else {
            WallpaperWorker.cancel(context)
            WallpaperAlarmScheduler.cancel(context)
        }

        AppLog.i(
            TAG,
            "Restore complete: ${payload.sources.size} sources, ${payload.rules.size} rules, $needsReauthorizationCount need re-auth"
        )

        return RestoreResult(
            success = true,
            restoredSourcesCount = payload.sources.size,
            restoredRulesCount = payload.rules.size,
            restoredFavoritesCount = payload.historyOverrides.size,
            needsReauthorizationCount = needsReauthorizationCount
        )
    }
}
