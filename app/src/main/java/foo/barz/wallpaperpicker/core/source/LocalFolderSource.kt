package foo.barz.wallpaperpicker.core.source

import android.content.Context
import android.net.Uri
import foo.barz.wallpaperpicker.core.database.LocalFolderFastScanner
import foo.barz.wallpaperpicker.core.database.LocalFolderIndexDatabase
import foo.barz.wallpaperpicker.core.model.WallpaperData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * High-performance wallpaper source that picks a random image from a user-selected SAF folder.
 * Uses native DocumentsContract cursor indexing and SQLite order-by-random for 0ms selection.
 */
class LocalFolderSource(
    private val context: Context,
    private val folderUri: Uri,
    private val folderName: String? = null,
    private val database: LocalFolderIndexDatabase = LocalFolderIndexDatabase(context)
) : WallpaperSource {

    constructor(
        context: Context,
        folderUri: Uri,
        database: LocalFolderIndexDatabase
    ) : this(context, folderUri, null, database)

    private val resolvedFolderName: String by lazy {
        folderName?.ifBlank { null }
            ?: androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)?.name
            ?: foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem.extractFolderNameFromUri(folderUri.toString())
            ?: "本地文件夹"
    }

    override val id: String = "local_folder"
    override val displayName: String
        get() = if (resolvedFolderName != "本地文件夹") "本地文件夹 ($resolvedFolderName)" else "本地文件夹"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = foo.barz.wallpaperpicker.data.PreferencesManager(context)
            val excluded = if (prefs.fairShuffle) prefs.getRecentWallpaperKeys().toSet() else emptySet()

            // Check if index exists; if not, perform fast scan and indexing
            var record = database.getRandomImage(folderUri, excluded)
            if (record == null) {
                val scanned = LocalFolderFastScanner.scanFolder(context, folderUri)
                if (scanned.isEmpty()) {
                    throw NoSuchElementException("所选文件夹中未找到任何图片文件 (.jpg, .jpeg, .png, .webp)")
                }
                database.replaceFolderIndex(folderUri, scanned)
                record = database.getRandomImage(folderUri, excluded)
                    ?: throw NoSuchElementException("无法从本地索引中获取图片")
            }

            val targetUri = Uri.parse(record.documentUri)
            WallpaperData(
                openStream = {
                    try {
                        context.contentResolver.openInputStream(targetUri)
                            ?: throw FileNotFoundException("无法打开图片文件流: $targetUri")
                    } catch (e: Exception) {
                        // Invalidate deleted file record and re-throw
                        database.removeByDocumentUri(targetUri)
                        throw e
                    }
                },
                title = record.fileName,
                sourceUri = targetUri,
                sourceType = foo.barz.wallpaperpicker.core.model.WallpaperSourceType.LOCAL_FOLDER,
                sourceTitle = resolvedFolderName
            )
        }
    }
}
