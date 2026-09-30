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
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Result of a backup restoration operation.
 */
data class RestoreResult(
    val success: Boolean,
    val restoredSourcesCount: Int = 0,
    val restoredRulesCount: Int = 0,
    val restoredFavoritesCount: Int = 0,
    val needsReauthorizationCount: Int = 0,
    val missingFavoritesCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Recognized backup encapsulation formats.
 */
enum class BackupFormat {
    NATIVE_ENCRYPTED,
    OPENPGP,
    PLAINTEXT
}

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
     * Identifies the format of a backup file from its initial bytes.
     */
    fun detectBackupFormat(inputStream: InputStream): BackupFormat {
        val buffered = if (inputStream.markSupported()) inputStream else BufferedInputStream(inputStream)
        if (isEncryptedBackup(buffered)) {
            return BackupFormat.NATIVE_ENCRYPTED
        }
        if (OpenPgpBackupEngine.isOpenPgpMessage(buffered)) {
            return BackupFormat.OPENPGP
        }
        return BackupFormat.PLAINTEXT
    }

    /**
     * Checks if the given input stream starts with a standard ZIP archive magic header (PK\x03\x04).
     * Uses mark/reset to avoid consuming stream contents.
     */
    fun isZipStream(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) {
            return false
        }
        val header = ByteArray(4)
        inputStream.mark(header.size)
        val read = inputStream.read(header)
        inputStream.reset()
        return read == 4 &&
                header[0] == 0x50.toByte() &&
                header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
    }

    /**
     * Assembles the application configuration payload.
     *
     * @param context Application context.
     * @param sanitize If true, strips API keys and sensitive query tokens before export.
     * @param includeFavoriteImages If true, correlates local favorite image filenames for inclusion in the ZIP archive.
     */
    fun buildBackupPayload(
        context: Context,
        sanitize: Boolean = false,
        includeFavoriteImages: Boolean = false
    ): BackupPayload {
        val prefs = PreferencesManager(context)
        val sourcesDb = WallpaperSourcesDatabase(context)
        val rulesDb = ScheduleRulesDatabase(context)
        val historyDb = WallpaperHistoryDatabase(context)

        val sources = sourcesDb.getAllSources()
        val rules = rulesDb.getAllRules()
        val favDir = File(context.filesDir, "favorites")
        val historyOverrides = historyDb.getFavoriteAndCustomOverrides().map { item ->
            val favFileName = if (includeFavoriteImages && item.isFavorite && !item.favoriteFilePath.isNullOrBlank()) {
                val file = File(item.favoriteFilePath)
                if (file.exists()) file.name else null
            } else null

            BackupHistoryOverride(
                sourceUri = item.sourceUri.toString(),
                title = item.title,
                sourceType = item.sourceType,
                sourceTitle = item.sourceTitle,
                isFavorite = item.isFavorite,
                favoriteTimestamp = item.favoriteTimestamp,
                favoriteFileName = favFileName,
                customScrollMode = item.customScrollMode,
                cropFocusX = item.cropFocusX,
                cropFocusY = item.cropFocusY,
                lockCropFocusX = item.lockCropFocusX,
                lockCropFocusY = item.lockCropFocusY,
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

        return payload
    }

    /**
     * Packages the backup configuration JSON and offline favorite image files into a single ZIP archive stream.
     */
    fun writeBackupZip(context: Context, outputStream: OutputStream, payload: BackupPayload) {
        val zipOut = ZipOutputStream(outputStream)
        val jsonBytes = payload.toJson().toByteArray(Charsets.UTF_8)
        val jsonEntry = ZipEntry("backup.json")
        zipOut.putNextEntry(jsonEntry)
        zipOut.write(jsonBytes)
        zipOut.closeEntry()

        val favDir = File(context.filesDir, "favorites")
        val writtenFileNames = mutableSetOf<String>()
        payload.historyOverrides.forEach { override ->
            val fileName = override.favoriteFileName
            if (fileName != null && !writtenFileNames.contains(fileName)) {
                val file = File(favDir, fileName)
                if (file.exists()) {
                    writtenFileNames.add(fileName)
                    val imgEntry = ZipEntry("favorites/$fileName")
                    zipOut.putNextEntry(imgEntry)
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
        zipOut.finish()
    }

    /**
     * Builds and exports the application backup payload to the given OutputStream.
     *
     * @param context Application context.
     * @param outputStream Destination stream (e.g., from SAF URI).
     * @param password Optional password for AES-256-GCM encryption. If null or blank, exported unencrypted.
     * @param sanitize If true, strips API keys and sensitive query tokens before export.
     * @param includeFavoriteImages If true, packages offline favorited images into a ZIP container before encryption.
     */
    fun exportBackup(
        context: Context,
        outputStream: OutputStream,
        password: String? = null,
        sanitize: Boolean = false,
        includeFavoriteImages: Boolean = false
    ): BackupPayload {
        AppLog.i(TAG, "Exporting backup (encrypted=${!password.isNullOrBlank()}, sanitize=$sanitize, includeFavorites=$includeFavoriteImages)")
        val payload = buildBackupPayload(context, sanitize, includeFavoriteImages)

        val targetStream = if (!password.isNullOrBlank()) {
            BackupCryptoEngine.wrapEncryptStream(outputStream, password.toCharArray())
        } else {
            outputStream
        }

        targetStream.use { rawOut ->
            if (includeFavoriteImages) {
                writeBackupZip(context, rawOut, payload)
            } else {
                rawOut.write(payload.toJson().toByteArray(Charsets.UTF_8))
                rawOut.flush()
            }
        }

        AppLog.i(TAG, "Backup successfully exported with ${payload.sources.size} sources, ${payload.rules.size} rules")
        return payload
    }

    /**
     * Restores application preferences, sources, schedule rules, and favorites from a backup stream.
     * Transparently handles both native encrypted (.wpbak) and unencrypted (.json or .zip) streams.
     */
    fun restoreBackup(
        context: Context,
        rawInput: InputStream,
        password: String? = null
    ): RestoreResult {
        AppLog.i(TAG, "Starting backup restoration")
        val bufferedInput = if (rawInput.markSupported()) rawInput else BufferedInputStream(rawInput)

        val isEncrypted = isEncryptedBackup(bufferedInput)
        return try {
            if (isEncrypted) {
                if (password.isNullOrBlank()) {
                    return RestoreResult(
                        success = false,
                        errorMessage = "此备份文件已加密，需要输入恢复密码"
                    )
                }
                val decryptStream = BackupCryptoEngine.wrapDecryptStream(bufferedInput, password.toCharArray())
                restoreFromDecryptedStream(context, decryptStream)
            } else {
                restoreFromDecryptedStream(context, bufferedInput)
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to read or decrypt backup stream", e)
            val message = if (isEncrypted) "解密失败：密码错误或文件已损坏" else "读取备份文件失败：${e.localizedMessage ?: "格式不正确"}"
            RestoreResult(success = false, errorMessage = message)
        }
    }

    /**
     * Restores application state from an unencrypted or already-decrypted stream (e.g. from OpenPGP decrypted output).
     * Automatically detects whether the stream is a ZIP container with image media or a raw JSON string.
     */
    fun restoreFromDecryptedStream(context: Context, stream: InputStream): RestoreResult {
        val buffered = if (stream.markSupported()) stream else BufferedInputStream(stream)
        return if (isZipStream(buffered)) {
            restoreFromZip(context, buffered)
        } else {
            val jsonStr = buffered.bufferedReader(Charsets.UTF_8).use { it.readText() }
            restoreFromJsonString(context, jsonStr)
        }
    }

    /**
     * Restores application state from a raw JSON string.
     */
    fun restoreFromJsonString(context: Context, jsonStr: String): RestoreResult {
        val payload = try {
            BackupPayload.fromJson(jsonStr)
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to parse backup JSON payload", e)
            return RestoreResult(success = false, errorMessage = "解析备份数据失败：JSON 格式无效")
        }

        return restoreFromPayload(context, payload)
    }

    /**
     * Restores application state and unzips favorite image media from a ZIP archive stream.
     */
    fun restoreFromZip(context: Context, inputStream: InputStream): RestoreResult {
        val zipIn = ZipInputStream(inputStream)
        var jsonText: String? = null
        val extractedFavorites = mutableMapOf<String, File>()
        val favDir = File(context.filesDir, "favorites").apply { if (!exists()) mkdirs() }

        var entry = zipIn.nextEntry
        while (entry != null) {
            val entryName = entry.name
            if (entryName == "backup.json") {
                val bytes = zipIn.readBytes()
                jsonText = String(bytes, Charsets.UTF_8)
            } else if (entryName.startsWith("favorites/") && !entry.isDirectory) {
                val fileName = entryName.removePrefix("favorites/").substringAfterLast('/')
                val targetFile = File(favDir, fileName)
                targetFile.outputStream().use { fos ->
                    zipIn.copyTo(fos)
                }
                extractedFavorites[fileName] = targetFile
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        if (jsonText.isNullOrBlank()) {
            return RestoreResult(success = false, errorMessage = "备份归档包中未包含有效的 backup.json 配置")
        }

        val payload = try {
            BackupPayload.fromJson(jsonText)
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to parse backup JSON from ZIP", e)
            return RestoreResult(success = false, errorMessage = "解析归档配置失败：JSON 格式无效")
        }

        return restoreFromPayload(context, payload, extractedFavorites)
    }

    /**
     * Restores application preferences, sources, schedule rules, and favorites from a BackupPayload.
     * Correlates extracted favorite image files if available, and detects missing remote favorites.
     */
    fun restoreFromPayload(
        context: Context,
        payload: BackupPayload,
        extractedFavorites: Map<String, File> = emptyMap()
    ): RestoreResult {
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
        val favDir = File(context.filesDir, "favorites")
        var missingFavoritesCount = 0

        payload.historyOverrides.forEach { override ->
            val localFile = if (override.favoriteFileName != null) {
                extractedFavorites[override.favoriteFileName]
                    ?: File(favDir, override.favoriteFileName).takeIf { it.exists() }
            } else null

            val favPath = localFile?.absolutePath
            val effectiveSourceUri = if (localFile != null && override.sourceUri.startsWith("file://")) {
                android.net.Uri.fromFile(localFile).toString()
            } else {
                override.sourceUri
            }

            historyDb.restoreFavoriteOverride(
                sourceUri = effectiveSourceUri,
                title = override.title,
                sourceType = override.sourceType,
                sourceTitle = override.sourceTitle,
                isFavorite = override.isFavorite,
                favoriteTimestamp = override.favoriteTimestamp,
                customScrollMode = override.customScrollMode,
                cropFocusX = override.cropFocusX,
                cropFocusY = override.cropFocusY,
                flipHorizontal = override.flipHorizontal,
                remoteUrl = override.remoteUrl,
                favoriteFilePath = favPath,
                lockCropFocusX = override.lockCropFocusX,
                lockCropFocusY = override.lockCropFocusY
            )

            // Detect whether this favorite wallpaper lacks a local file but can be redownloaded
            if (override.isFavorite && (favPath == null || !File(favPath).exists())) {
                if (!override.remoteUrl.isNullOrBlank() || override.sourceUri.startsWith("http")) {
                    missingFavoritesCount++
                }
            }
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
            "Restore complete: ${payload.sources.size} sources, ${payload.rules.size} rules, $needsReauthorizationCount need re-auth, $missingFavoritesCount missing favorites"
        )

        return RestoreResult(
            success = true,
            restoredSourcesCount = payload.sources.size,
            restoredRulesCount = payload.rules.size,
            restoredFavoritesCount = payload.historyOverrides.size,
            needsReauthorizationCount = needsReauthorizationCount,
            missingFavoritesCount = missingFavoritesCount
        )
    }
}
