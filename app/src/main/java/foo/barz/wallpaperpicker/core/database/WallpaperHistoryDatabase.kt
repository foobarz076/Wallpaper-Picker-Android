package foo.barz.wallpaperpicker.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType

/**
 * Native SQLite database managing historical wallpaper logs and user favorites.
 * Strictly isolates favorites so they are never purged during cache cleanup or history pruning.
 */
class WallpaperHistoryDatabase(context: Context) : SQLiteOpenHelper(
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
                $COLUMN_SOURCE_URI TEXT NOT NULL,
                $COLUMN_TITLE TEXT,
                $COLUMN_SOURCE_TYPE TEXT NOT NULL,
                $COLUMN_APPLIED_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COLUMN_FAVORITE_TIMESTAMP INTEGER,
                $COLUMN_FAVORITE_FILE_PATH TEXT,
                $COLUMN_CUSTOM_SCROLL_MODE TEXT,
                $COLUMN_CROP_FOCUS_X REAL,
                $COLUMN_CROP_FOCUS_Y REAL,
                $COLUMN_SOURCE_TITLE TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_history_applied_time ON $TABLE_NAME ($COLUMN_APPLIED_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX idx_history_source_uri ON $TABLE_NAME ($COLUMN_SOURCE_URI)")
        db.execSQL("CREATE INDEX idx_history_favorite ON $TABLE_NAME ($COLUMN_IS_FAVORITE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_SOURCE_TITLE TEXT")
            }
            healLegacyRecords(db)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        healLegacyRecords(db)
    }

    /**
     * Retroactively repairs legacy records where sourceType was stored as COMPOSITE
     * or sourceTitle was missing.
     */
    private fun healLegacyRecords(db: SQLiteDatabase) {
        runCatching {
            val cursor = db.rawQuery(
                """
                SELECT $COLUMN_ID, $COLUMN_SOURCE_URI, $COLUMN_SOURCE_TYPE, $COLUMN_SOURCE_TITLE 
                FROM $TABLE_NAME 
                WHERE $COLUMN_SOURCE_TITLE IS NULL OR $COLUMN_SOURCE_TYPE = 'COMPOSITE'
                """.trimIndent(),
                null
            )
            cursor.use {
                val idCol = it.getColumnIndexOrThrow(COLUMN_ID)
                val uriCol = it.getColumnIndexOrThrow(COLUMN_SOURCE_URI)
                val typeCol = it.getColumnIndexOrThrow(COLUMN_SOURCE_TYPE)
                val titleCol = it.getColumnIndexOrThrow(COLUMN_SOURCE_TITLE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val uri = it.getString(uriCol)
                    val typeStr = it.getString(typeCol)
                    val existingSourceTitle = if (!it.isNull(titleCol)) it.getString(titleCol) else null

                    var needUpdate = false
                    val values = ContentValues()

                    var concreteType = runCatching { WallpaperSourceType.valueOf(typeStr) }
                        .getOrDefault(WallpaperSourceType.LOCAL_FOLDER)

                    // If recorded as COMPOSITE, infer real concrete type from URI
                    if (concreteType == WallpaperSourceType.COMPOSITE) {
                        concreteType = when {
                            uri.contains("content://media/") -> WallpaperSourceType.MEDIA_STORE
                            uri.contains("/cache/wallpapers") && uri.contains("immich") -> WallpaperSourceType.IMMICH
                            uri.contains("/cache/wallpapers") -> WallpaperSourceType.HTTP_API
                            uri.startsWith("http://") || uri.startsWith("https://") -> WallpaperSourceType.HTTP_API
                            uri.contains("/files/favorites") -> WallpaperSourceType.FAVORITES
                            else -> WallpaperSourceType.LOCAL_FOLDER
                        }
                        values.put(COLUMN_SOURCE_TYPE, concreteType.name)
                        needUpdate = true
                    }

                    // Infer source title if missing
                    if (existingSourceTitle.isNullOrBlank()) {
                        val inferredTitle = WallpaperHistoryItem.inferSourceTitleFromUri(uri, concreteType)
                        values.put(COLUMN_SOURCE_TITLE, inferredTitle)
                        needUpdate = true
                    }

                    if (needUpdate) {
                        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
                    }
                }
            }
        }
    }

    /**
     * Records a newly applied wallpaper. If the source URI already exists in history,
     * refreshes its applied timestamp and title while preserving existing favorite status.
     */
    fun recordAppliedWallpaper(
        sourceUri: Uri,
        title: String?,
        sourceType: WallpaperSourceType,
        appliedTimestamp: Long = System.currentTimeMillis(),
        sourceTitle: String? = null
    ): Long {
        val db = writableDatabase
        val uriString = sourceUri.toString()
        val concreteSourceTitle = sourceTitle?.ifBlank { null }
            ?: WallpaperHistoryItem.inferSourceTitleFromUri(uriString, sourceType)

        val existing = getItemByUri(uriString)
        return if (existing != null) {
            val values = ContentValues().apply {
                put(COLUMN_APPLIED_TIMESTAMP, appliedTimestamp)
                if (!title.isNullOrBlank()) {
                    put(COLUMN_TITLE, title)
                }
                put(COLUMN_SOURCE_TYPE, sourceType.name)
                put(COLUMN_SOURCE_TITLE, concreteSourceTitle)
            }
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(existing.id.toString()))
            existing.id
        } else {
            val values = ContentValues().apply {
                put(COLUMN_SOURCE_URI, uriString)
                put(COLUMN_TITLE, title)
                put(COLUMN_SOURCE_TYPE, sourceType.name)
                put(COLUMN_SOURCE_TITLE, concreteSourceTitle)
                put(COLUMN_APPLIED_TIMESTAMP, appliedTimestamp)
                put(COLUMN_IS_FAVORITE, 0)
            }
            db.insert(TABLE_NAME, null, values)
        }
    }

    /**
     * Returns history items ordered from most recent to oldest.
     */
    fun getHistoryList(limit: Int = 200): List<WallpaperHistoryItem> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_APPLIED_TIMESTAMP DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        return cursor.use { extractItems(it) }
    }

    /**
     * Returns all user favorited wallpapers ordered by favorite timestamp.
     */
    fun getFavoritesList(): List<WallpaperHistoryItem> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME 
            WHERE $COLUMN_IS_FAVORITE = 1 
            ORDER BY $COLUMN_FAVORITE_TIMESTAMP DESC, $COLUMN_APPLIED_TIMESTAMP DESC
            """.trimIndent(),
            null
        )
        return cursor.use { extractItems(it) }
    }

    /**
     * Updates the favorite status and persistent storage file path for a record.
     */
    fun updateFavorite(
        id: Long,
        isFavorite: Boolean,
        favoriteTimestamp: Long? = if (isFavorite) System.currentTimeMillis() else null,
        favoriteFilePath: String? = null
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
            put(COLUMN_FAVORITE_TIMESTAMP, favoriteTimestamp)
            put(COLUMN_FAVORITE_FILE_PATH, favoriteFilePath)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    /**
     * Updates the favorite status by source URI string.
     */
    fun updateFavoriteByUri(
        sourceUri: String,
        isFavorite: Boolean,
        favoriteTimestamp: Long? = if (isFavorite) System.currentTimeMillis() else null,
        favoriteFilePath: String? = null
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
            put(COLUMN_FAVORITE_TIMESTAMP, favoriteTimestamp)
            put(COLUMN_FAVORITE_FILE_PATH, favoriteFilePath)
        }
        db.update(TABLE_NAME, values, "$COLUMN_SOURCE_URI = ?", arrayOf(sourceUri))
    }

    /**
     * Fetches an item by its source URI.
     */
    fun getItemByUri(sourceUri: String): WallpaperHistoryItem? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_SOURCE_URI = ? LIMIT 1",
            arrayOf(sourceUri)
        )
        return cursor.use {
            if (it.moveToFirst()) parseRow(it) else null
        }
    }

    /**
     * Fetches an item by its primary key ID.
     */
    fun getItemById(id: Long): WallpaperHistoryItem? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = ? LIMIT 1",
            arrayOf(id.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) parseRow(it) else null
        }
    }

    /**
     * Deletes a specific history record by ID.
     */
    fun deleteRecord(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    /**
     * Clears all non-favorite history records, keeping user favorites intact.
     */
    fun clearUnfavoritedHistory() {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_IS_FAVORITE = 0", null)
    }

    /**
     * Returns total count of history items.
     */
    fun getHistoryCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Returns total count of favorited items.
     */
    fun getFavoritesCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME WHERE $COLUMN_IS_FAVORITE = 1", null)
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    private fun extractItems(cursor: Cursor): List<WallpaperHistoryItem> {
        val items = mutableListOf<WallpaperHistoryItem>()
        while (cursor.moveToNext()) {
            items.add(parseRow(cursor))
        }
        return items
    }

    private fun parseRow(cursor: Cursor): WallpaperHistoryItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val sourceUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE_URI))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
        val sourceTypeName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE_TYPE))
        val appliedTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_APPLIED_TIMESTAMP))
        val isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1
        val favoriteTimestamp = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE_TIMESTAMP))) {
            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE_TIMESTAMP))
        } else {
            null
        }
        val favoriteFilePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE_FILE_PATH))
        val customScrollModeName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOM_SCROLL_MODE))
        val cropFocusX = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_CROP_FOCUS_X))) {
            cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_CROP_FOCUS_X))
        } else {
            null
        }
        val cropFocusY = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_CROP_FOCUS_Y))) {
            cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_CROP_FOCUS_Y))
        } else {
            null
        }

        val sourceType = runCatching { WallpaperSourceType.valueOf(sourceTypeName) }
            .getOrDefault(WallpaperSourceType.LOCAL_FOLDER)
        val customScrollMode = customScrollModeName?.let {
            runCatching { WallpaperScrollMode.valueOf(it) }.getOrNull()
        }
        val sourceTitle = if (cursor.getColumnIndex(COLUMN_SOURCE_TITLE) != -1 && !cursor.isNull(cursor.getColumnIndex(COLUMN_SOURCE_TITLE))) {
            cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE_TITLE))
        } else {
            null
        }

        return WallpaperHistoryItem(
            id = id,
            sourceUri = sourceUri,
            title = title,
            sourceType = sourceType,
            appliedTimestamp = appliedTimestamp,
            isFavorite = isFavorite,
            favoriteTimestamp = favoriteTimestamp,
            favoriteFilePath = favoriteFilePath,
            customScrollMode = customScrollMode,
            cropFocusX = cropFocusX,
            cropFocusY = cropFocusY,
            sourceTitle = sourceTitle
        )
    }

    companion object {
        private const val DATABASE_NAME = "wallpaper_history.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_NAME = "wallpaper_history"
        const val COLUMN_ID = "id"
        const val COLUMN_SOURCE_URI = "source_uri"
        const val COLUMN_TITLE = "title"
        const val COLUMN_SOURCE_TYPE = "source_type"
        const val COLUMN_SOURCE_TITLE = "source_title"
        const val COLUMN_APPLIED_TIMESTAMP = "applied_timestamp"
        const val COLUMN_IS_FAVORITE = "is_favorite"
        const val COLUMN_FAVORITE_TIMESTAMP = "favorite_timestamp"
        const val COLUMN_FAVORITE_FILE_PATH = "favorite_file_path"
        const val COLUMN_CUSTOM_SCROLL_MODE = "custom_scroll_mode"
        const val COLUMN_CROP_FOCUS_X = "crop_focus_x"
        const val COLUMN_CROP_FOCUS_Y = "crop_focus_y"
    }
}
