package foo.barz.wallpaperpicker.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

/**
 * Data record for an indexed image file within a user-selected SAF folder.
 */
data class LocalFolderImageRecord(
    val folderUri: String,
    val documentId: String,
    val documentUri: String,
    val fileName: String?,
    val mimeType: String?,
    val lastIndexed: Long = System.currentTimeMillis()
)

/**
 * Lightweight native SQLite database for caching SAF directory indexes.
 * Provides instant O(1) / 0ms random wallpaper selection without rescanning large directories.
 */
class LocalFolderIndexDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FOLDER_URI TEXT NOT NULL,
                $COLUMN_DOCUMENT_ID TEXT NOT NULL,
                $COLUMN_DOCUMENT_URI TEXT NOT NULL,
                $COLUMN_FILE_NAME TEXT,
                $COLUMN_MIME_TYPE TEXT,
                $COLUMN_LAST_INDEXED INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX idx_folder_uri ON $TABLE_NAME ($COLUMN_FOLDER_URI)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Retrieves the count of indexed images for a given folder URI.
     */
    fun getIndexCount(folderUri: Uri): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COLUMN_FOLDER_URI = ?",
            arrayOf(folderUri.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * Picks a random image record for the given folder using SQLite's RANDOM() order.
     */
    fun getRandomImage(folderUri: Uri): LocalFolderImageRecord? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT $COLUMN_FOLDER_URI, $COLUMN_DOCUMENT_ID, $COLUMN_DOCUMENT_URI, $COLUMN_FILE_NAME, $COLUMN_MIME_TYPE, $COLUMN_LAST_INDEXED
            FROM $TABLE_NAME
            WHERE $COLUMN_FOLDER_URI = ?
            ORDER BY RANDOM() LIMIT 1
            """.trimIndent(),
            arrayOf(folderUri.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) {
                LocalFolderImageRecord(
                    folderUri = it.getString(0),
                    documentId = it.getString(1),
                    documentUri = it.getString(2),
                    fileName = it.getString(3),
                    mimeType = it.getString(4),
                    lastIndexed = it.getLong(5)
                )
            } else {
                null
            }
        }
    }

    /**
     * Atomically clears any existing records for this folder and batch inserts the new image records.
     */
    fun replaceFolderIndex(folderUri: Uri, records: List<LocalFolderImageRecord>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_NAME, "$COLUMN_FOLDER_URI = ?", arrayOf(folderUri.toString()))
            for (record in records) {
                val values = ContentValues().apply {
                    put(COLUMN_FOLDER_URI, record.folderUri)
                    put(COLUMN_DOCUMENT_ID, record.documentId)
                    put(COLUMN_DOCUMENT_URI, record.documentUri)
                    put(COLUMN_FILE_NAME, record.fileName)
                    put(COLUMN_MIME_TYPE, record.mimeType)
                    put(COLUMN_LAST_INDEXED, record.lastIndexed)
                }
                db.insert(TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Removes an invalid or missing image record by document URI.
     */
    fun removeByDocumentUri(documentUri: Uri) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_DOCUMENT_URI = ?", arrayOf(documentUri.toString()))
    }

    /**
     * Clears all cached index entries for a specific folder.
     */
    fun clearFolderIndex(folderUri: Uri) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_FOLDER_URI = ?", arrayOf(folderUri.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "wallpaper_folder_index.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "local_folder_images"
        const val COLUMN_ID = "id"
        const val COLUMN_FOLDER_URI = "folder_uri"
        const val COLUMN_DOCUMENT_ID = "document_id"
        const val COLUMN_DOCUMENT_URI = "document_uri"
        const val COLUMN_FILE_NAME = "file_name"
        const val COLUMN_MIME_TYPE = "mime_type"
        const val COLUMN_LAST_INDEXED = "last_indexed"
    }
}
