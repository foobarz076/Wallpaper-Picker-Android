package foo.barz.wallpaperpicker.core.action

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles interactions with the current active wallpaper:
 * - View in external gallery
 * - Save to public Pictures/Wallpapers gallery
 * - System share sheet
 */
object WallpaperActionManager {

    /**
     * Prepares a shareable or viewable content URI.
     * Converts raw file:// URIs or private files to FileProvider content:// URIs.
     * For SAF documents that external gallery apps cannot read, creates a temporary cached copy.
     */
    suspend fun getShareableUri(context: Context, sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        val scheme = sourceUri.scheme

        if (scheme == "file") {
            val file = File(sourceUri.path ?: throw IllegalArgumentException("无效的文件路径: $sourceUri"))
            return@withContext FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        // For MediaStore or existing FileProvider URIs, return directly
        if (scheme == "content" && sourceUri.authority?.contains("media") == true) {
            return@withContext sourceUri
        }

        // For SAF tree document URIs or network streams, copy to a temporary share file
        val shareDir = File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
        val shareFile = File(shareDir, "current_wallpaper.jpg")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(shareFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("无法读取图片数据流: $sourceUri")

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
    }

    /**
     * Opens the wallpaper in an external photo viewer or gallery app.
     */
    suspend fun openInGallery(context: Context, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val viewableUri = getShareableUri(context, sourceUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(viewableUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "在图库中打开").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    /**
     * Shares the current wallpaper via Android system share sheet.
     */
    suspend fun shareWallpaper(context: Context, sourceUri: Uri, title: String? = null): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val shareableUri = getShareableUri(context, sourceUri)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, shareableUri)
                if (!title.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, title)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "分享壁纸").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    /**
     * Saves the current wallpaper to the public Pictures/Wallpapers album.
     * Uses Scoped Storage on Android 10+ (API 29+) and legacy storage on Android 6.0~9.0.
     */
    suspend fun saveToGallery(context: Context, sourceUri: Uri, preferredName: String? = null): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileName = (preferredName ?: "wallpaper_${System.currentTimeMillis()}")
                    .let { if (it.contains('.')) it else "$it.jpg" }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Modern Android 10+ Scoped Storage
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wallpapers")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("创建相册目标条目失败")

                    resolver.openOutputStream(itemUri).use { out ->
                        if (out == null) throw IllegalStateException("无法写入目标图片")
                        resolver.openInputStream(sourceUri)?.use { input ->
                            input.copyTo(out)
                        } ?: throw IllegalStateException("无法读取源壁纸图片")
                    }

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    itemUri
                } else {
                    // Legacy Android 6.0~9.0 (API 23~28)
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val wallpapersDir = File(picturesDir, "Wallpapers").apply {
                        if (!exists()) mkdirs()
                    }
                    val targetFile = File(wallpapersDir, fileName)

                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("无法读取源壁纸图片")

                    // Notify media scanner to make it immediately visible in gallery
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                    Uri.fromFile(targetFile)
                }
            }
        }
}
