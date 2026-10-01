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
                $COLUMN_LOCK_CROP_FOCUS_X REAL,
                $COLUMN_LOCK_CROP_FOCUS_Y REAL,
                $COLUMN_FLIP_HORIZONTAL INTEGER NOT NULL DEFAULT 0,
                $COLUMN_SOURCE_TITLE TEXT,
                $COLUMN_REMOTE_URL TEXT,
                $COLUMN_DOWNLOAD_TIMESTAMP INTEGER NOT NULL DEFAULT 0
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
        if (oldVersion < 3) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_FLIP_HORIZONTAL INTEGER NOT NULL DEFAULT 0")
            }
        }
        if (oldVersion < 4) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_REMOTE_URL TEXT")
            }
        }
        if (oldVersion < 5) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DOWNLOAD_TIMESTAMP INTEGER NOT NULL DEFAULT 0")
            }
        }
        if (oldVersion < 6) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LOCK_CROP_FOCUS_X REAL")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LOCK_CROP_FOCUS_Y REAL")
            }
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureColumnsExist(db)
        healLegacyRecords(db)
    }

    /**
     * Dynamically verifies and creates any missing columns across app version updates,
     * protecting against out-of-order schema states or skipped migration increments.
     */
    private fun ensureColumnsExist(db: SQLiteDatabase) {
        runCatching {
            val existingColumns = mutableSetOf<String>()
            val cursor = db.rawQuery("PRAGMA table_info($TABLE_NAME)", null)
            cursor.use {
                val nameCol = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (nameCol != -1) {
                        existingColumns.add(it.getString(nameCol).lowercase())
                    }
                }
            }

            if (!existingColumns.contains(COLUMN_SOURCE_TITLE.lowercase())) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_SOURCE_TITLE TEXT")
            }
            if (!existingColumns.contains(COLUMN_FLIP_HORIZONTAL.lowercase())) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_FLIP_HORIZONTAL INTEGER NOT NULL DEFAULT 0")
            }
            if (!existingColumns.contains(COLUMN_REMOTE_URL.lowercase())) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_REMOTE_URL TEXT")
            }
            if (!existingColumns.contains(COLUMN_DOWNLOAD_TIMESTAMP.lowercase())) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DOWNLOAD_TIMESTAMP INTEGER NOT NULL DEFAULT 0")
            }
            if (!existingColumns.contains(COLUMN_LOCK_CROP_FOCUS_X.lowercase())) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LOCK_CROP_FOCUS_X REAL")
            }
            if (!existingColumns.contains(COLUMN_LOCK_CROP_FOCUS_Y.lowercase())) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LOCK_CROP_FOCUS_Y REAL")
            }
        }
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
                            uri.contains("/files/custom_sources") -> WallpaperSourceType.CUSTOM_PHOTOS
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
        sourceTitle: String? = null,
        remoteUrl: String? = null
    ): Long {
        val db = writableDatabase
        val uriString = sourceUri.toString()
        val rawSourceTitle = sourceTitle?.ifBlank { null }
            ?: WallpaperHistoryItem.inferSourceTitleFromUri(uriString, sourceType)
        val concreteSourceTitle = if (rawSourceTitle.contains(" · ")) {
            rawSourceTitle.substringAfter(" · ").trim()
        } else {
            rawSourceTitle.trim()
        }

        val existing = getItemByUri(uriString)
        return if (existing != null) {
            val values = ContentValues().apply {
                put(COLUMN_APPLIED_TIMESTAMP, appliedTimestamp)
                if (!title.isNullOrBlank()) {
                    put(COLUMN_TITLE, title)
                }
                put(COLUMN_SOURCE_TYPE, sourceType.name)
                put(COLUMN_SOURCE_TITLE, concreteSourceTitle)
                if (!remoteUrl.isNullOrBlank()) {
                    put(COLUMN_REMOTE_URL, remoteUrl)
                }
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
                if (!remoteUrl.isNullOrBlank()) {
                    put(COLUMN_REMOTE_URL, remoteUrl)
                }
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
     * Updates per-image custom scroll mode, crop focus coordinates (system & lock), and horizontal flip preferences by ID.
     */
    fun updateCustomPreferences(
        id: Long,
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        lockCropFocusX: Float? = null,
        lockCropFocusY: Float? = null
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CUSTOM_SCROLL_MODE, customScrollMode?.name)
            put(COLUMN_CROP_FOCUS_X, cropFocusX)
            put(COLUMN_CROP_FOCUS_Y, cropFocusY)
            put(COLUMN_LOCK_CROP_FOCUS_X, lockCropFocusX)
            put(COLUMN_LOCK_CROP_FOCUS_Y, lockCropFocusY)
            put(COLUMN_FLIP_HORIZONTAL, if (flipHorizontal) 1 else 0)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    /**
     * Updates per-image custom scroll mode, crop focus coordinates (system & lock), and horizontal flip preferences by source URI.
     */
    fun updateCustomPreferencesByUri(
        sourceUri: String,
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        lockCropFocusX: Float? = null,
        lockCropFocusY: Float? = null
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CUSTOM_SCROLL_MODE, customScrollMode?.name)
            put(COLUMN_CROP_FOCUS_X, cropFocusX)
            put(COLUMN_CROP_FOCUS_Y, cropFocusY)
            put(COLUMN_LOCK_CROP_FOCUS_X, lockCropFocusX)
            put(COLUMN_LOCK_CROP_FOCUS_Y, lockCropFocusY)
            put(COLUMN_FLIP_HORIZONTAL, if (flipHorizontal) 1 else 0)
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
     * Deletes non-favorite history records whose local files do not exist or are inaccessible.
     * Retains all favorited items intact.
     * @return The number of records removed.
     */
    fun clearInvalidUnfavoritedRecords(context: Context): Int {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_SOURCE_URI, $COLUMN_SOURCE_TYPE FROM $TABLE_NAME WHERE $COLUMN_IS_FAVORITE = 0",
            null
        )
        val idsToDelete = mutableListOf<Long>()
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(COLUMN_ID)
            val uriCol = it.getColumnIndexOrThrow(COLUMN_SOURCE_URI)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uriStr = it.getString(uriCol)
                val uri = Uri.parse(uriStr)
                val isAccessible = runCatching {
                    if (uri.scheme == "file") {
                        val path = uri.path ?: return@runCatching false
                        val file = java.io.File(path)
                        file.exists() && file.canRead() && file.length() > 0L
                    } else if (uri.scheme == "content") {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                    } else {
                        true
                    }
                }.getOrDefault(false)

                if (!isAccessible) {
                    idsToDelete.add(id)
                }
            }
        }

        if (idsToDelete.isEmpty()) return 0

        db.beginTransaction()
        try {
            for (id in idsToDelete) {
                db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return idsToDelete.size
    }

    /**
     * Updates source URI, optional remote URL, and persistent favorite file path upon re-download.
     */
    fun updateSourceUri(
        id: Long,
        sourceUri: String,
        remoteUrl: String? = null,
        favoriteFilePath: String? = null,
        downloadTimestamp: Long = System.currentTimeMillis()
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SOURCE_URI, sourceUri)
            put(COLUMN_DOWNLOAD_TIMESTAMP, downloadTimestamp)
            if (!remoteUrl.isNullOrBlank()) {
                put(COLUMN_REMOTE_URL, remoteUrl)
            }
            if (!favoriteFilePath.isNullOrBlank()) {
                put(COLUMN_FAVORITE_FILE_PATH, favoriteFilePath)
            }
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
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
        val lockCropFocusX = if (cursor.getColumnIndex(COLUMN_LOCK_CROP_FOCUS_X) != -1 && !cursor.isNull(cursor.getColumnIndex(COLUMN_LOCK_CROP_FOCUS_X))) {
            cursor.getFloat(cursor.getColumnIndex(COLUMN_LOCK_CROP_FOCUS_X))
        } else {
            null
        }
        val lockCropFocusY = if (cursor.getColumnIndex(COLUMN_LOCK_CROP_FOCUS_Y) != -1 && !cursor.isNull(cursor.getColumnIndex(COLUMN_LOCK_CROP_FOCUS_Y))) {
            cursor.getFloat(cursor.getColumnIndex(COLUMN_LOCK_CROP_FOCUS_Y))
        } else {
            null
        }
        val flipHorizontal = if (cursor.getColumnIndex(COLUMN_FLIP_HORIZONTAL) != -1 && !cursor.isNull(cursor.getColumnIndex(COLUMN_FLIP_HORIZONTAL))) {
            cursor.getInt(cursor.getColumnIndex(COLUMN_FLIP_HORIZONTAL)) == 1
        } else {
            false
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
        val remoteUrl = if (cursor.getColumnIndex(COLUMN_REMOTE_URL) != -1 && !cursor.isNull(cursor.getColumnIndex(COLUMN_REMOTE_URL))) {
            cursor.getString(cursor.getColumnIndex(COLUMN_REMOTE_URL))
        } else {
            null
        }
        val downloadTimestamp = if (cursor.getColumnIndex(COLUMN_DOWNLOAD_TIMESTAMP) != -1 && !cursor.isNull(cursor.getColumnIndex(COLUMN_DOWNLOAD_TIMESTAMP))) {
            cursor.getLong(cursor.getColumnIndex(COLUMN_DOWNLOAD_TIMESTAMP))
        } else {
            0L
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
            lockCropFocusX = lockCropFocusX,
            lockCropFocusY = lockCropFocusY,
            flipHorizontal = flipHorizontal,
            sourceTitle = sourceTitle,
            remoteUrl = remoteUrl,
            downloadTimestamp = downloadTimestamp
        )
    }

    /**
     * Retrieves all history items that are either marked as favorite or contain custom
     * framing / scrolling preferences for backup serialization.
     */
    fun getFavoriteAndCustomOverrides(): List<WallpaperHistoryItem> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME 
            WHERE $COLUMN_IS_FAVORITE = 1 
               OR $COLUMN_CUSTOM_SCROLL_MODE IS NOT NULL 
               OR $COLUMN_CROP_FOCUS_X IS NOT NULL 
               OR $COLUMN_LOCK_CROP_FOCUS_X IS NOT NULL 
               OR $COLUMN_FLIP_HORIZONTAL = 1
            """.trimIndent(),
            null
        )
        return cursor.use { extractItems(it) }
    }

    /**
     * Restores a favorite or custom framing preference record from backup.
     */
    fun restoreFavoriteOverride(
        sourceUri: String,
        title: String?,
        sourceType: WallpaperSourceType,
        sourceTitle: String?,
        isFavorite: Boolean,
        favoriteTimestamp: Long?,
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        remoteUrl: String?,
        favoriteFilePath: String? = null,
        lockCropFocusX: Float? = null,
        lockCropFocusY: Float? = null
    ) {
        val db = writableDatabase
        val existing = getItemByUri(sourceUri)
        val sanitizedSourceTitle = sourceTitle?.let {
            if (it.contains(" · ")) it.substringAfter(" · ").trim() else it.trim()
        }
        val values = ContentValues().apply {
            put(COLUMN_SOURCE_URI, sourceUri)
            if (title != null) put(COLUMN_TITLE, title)
            put(COLUMN_SOURCE_TYPE, sourceType.name)
            if (sanitizedSourceTitle != null) put(COLUMN_SOURCE_TITLE, sanitizedSourceTitle)
            put(COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
            put(COLUMN_FAVORITE_TIMESTAMP, favoriteTimestamp)
            if (favoriteFilePath != null) put(COLUMN_FAVORITE_FILE_PATH, favoriteFilePath)
            put(COLUMN_CUSTOM_SCROLL_MODE, customScrollMode?.name)
            put(COLUMN_CROP_FOCUS_X, cropFocusX)
            put(COLUMN_CROP_FOCUS_Y, cropFocusY)
            put(COLUMN_LOCK_CROP_FOCUS_X, lockCropFocusX)
            put(COLUMN_LOCK_CROP_FOCUS_Y, lockCropFocusY)
            put(COLUMN_FLIP_HORIZONTAL, if (flipHorizontal) 1 else 0)
            if (remoteUrl != null) put(COLUMN_REMOTE_URL, remoteUrl)
        }
        if (existing != null) {
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(existing.id.toString()))
        } else {
            values.put(COLUMN_APPLIED_TIMESTAMP, favoriteTimestamp ?: System.currentTimeMillis())
            db.insert(TABLE_NAME, null, values)
        }
    }

    companion object {
        private const val DATABASE_NAME = "wallpaper_history.db"
        private const val DATABASE_VERSION = 6

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
        const val COLUMN_LOCK_CROP_FOCUS_X = "lock_crop_focus_x"
        const val COLUMN_LOCK_CROP_FOCUS_Y = "lock_crop_focus_y"
        const val COLUMN_FLIP_HORIZONTAL = "flip_horizontal"
        const val COLUMN_REMOTE_URL = "remote_url"
        const val COLUMN_DOWNLOAD_TIMESTAMP = "download_timestamp"
    }
}
