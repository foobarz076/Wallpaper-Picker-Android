package foo.barz.wallpaperpicker.core.source

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import foo.barz.wallpaperpicker.core.model.WallpaperData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Wallpaper source that picks a random image from a user-selected SAF folder.
 */
class LocalFolderSource(
    private val context: Context,
    private val folderUri: Uri
) : WallpaperSource {

    override val id: String = "local_folder"
    override val displayName: String = "本地文件夹"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw IllegalStateException("无法访问所选文件夹，请重新选择并授权")

            val imageFiles = rootDoc.listFiles().filter { doc ->
                doc.isFile && isImageFile(doc)
            }

            if (imageFiles.isEmpty()) {
                throw NoSuchElementException("所选文件夹中未找到任何图片文件 (.jpg, .jpeg, .png, .webp)")
            }

            val targetDoc = imageFiles.random()

            WallpaperData(
                openStream = {
                    context.contentResolver.openInputStream(targetDoc.uri)
                        ?: throw FileNotFoundException("无法打开图片文件流: ${targetDoc.uri}")
                },
                title = targetDoc.name,
                sourceUri = targetDoc.uri
            )
        }
    }

    private fun isImageFile(doc: DocumentFile): Boolean {
        val mime = doc.type
        if (mime != null && mime.startsWith("image/")) {
            return true
        }
        val name = doc.name?.lowercase() ?: return false
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".webp")
    }
}
