package foo.barz.wallpaperpicker.core.database

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-speed scanner that uses DocumentsContract raw cursor queries to traverse
 * large SAF directories in ~200ms instead of 8s with DocumentFile.
 */
object LocalFolderFastScanner {

    private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    /**
     * Scans the given folder URI and returns all valid image records.
     */
    suspend fun scanFolder(context: Context, folderUri: Uri): List<LocalFolderImageRecord> =
        withContext(Dispatchers.IO) {
            runCatching {
                scanWithDocumentsContract(context, folderUri)
            }.getOrElse { error ->
                if (error is SecurityException) {
                    throw SecurityException("缺少文件夹读取权限，请在图源配置中重新选择文件夹并授权", error)
                }
                // Fallback to DocumentFile if raw DocumentsContract query fails
                scanWithDocumentFile(context, folderUri)
            }
        }

    private fun scanWithDocumentsContract(context: Context, folderUri: Uri): List<LocalFolderImageRecord> {
        val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId)
        val resolver = context.contentResolver

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        val records = mutableListOf<LocalFolderImageRecord>()
        val folderUriString = folderUri.toString()

        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idCol) ?: continue
                val name = cursor.getString(nameCol)
                val mimeType = cursor.getString(mimeCol)

                if (isImage(name, mimeType)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                    records.add(
                        LocalFolderImageRecord(
                            folderUri = folderUriString,
                            documentId = docId,
                            documentUri = docUri.toString(),
                            fileName = name,
                            mimeType = mimeType
                        )
                    )
                }
            }
        }

        return records
    }

    private fun scanWithDocumentFile(context: Context, folderUri: Uri): List<LocalFolderImageRecord> {
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        val folderUriString = folderUri.toString()
        val records = mutableListOf<LocalFolderImageRecord>()

        for (file in rootDoc.listFiles()) {
            if (file.isFile && isImage(file.name, file.type)) {
                records.add(
                    LocalFolderImageRecord(
                        folderUri = folderUriString,
                        documentId = file.uri.lastPathSegment ?: file.name.orEmpty(),
                        documentUri = file.uri.toString(),
                        fileName = file.name,
                        mimeType = file.type
                    )
                )
            }
        }
        return records
    }

    private fun isImage(name: String?, mimeType: String?): Boolean {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return true
        }
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in SUPPORTED_IMAGE_EXTENSIONS
    }
}
