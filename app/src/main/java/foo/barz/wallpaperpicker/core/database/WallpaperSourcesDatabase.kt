package foo.barz.wallpaperpicker.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import foo.barz.wallpaperpicker.core.model.FavoritesSourceConfig
import foo.barz.wallpaperpicker.core.model.HttpApiSourceConfig
import foo.barz.wallpaperpicker.core.model.ImmichSourceConfig
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * SQLite database managing persistent wallpaper source instances.
 * Supports multi-instance configurations (e.g., multiple local folders or Immich libraries).
 */
class WallpaperSourcesDatabase(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COLUMN_CONFIG_JSON TEXT NOT NULL,
                $COLUMN_CREATED_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_UPDATED_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_sources_enabled ON $TABLE_NAME ($COLUMN_ENABLED)")
        db.execSQL("CREATE INDEX idx_sources_created ON $TABLE_NAME ($COLUMN_CREATED_TIMESTAMP ASC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Retrieves all configured wallpaper sources ordered by creation timestamp.
     */
    fun getAllSources(): List<WallpaperSourceEntity> {
        val list = mutableListOf<WallpaperSourceEntity>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATED_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseSourceEntity(it))
            }
        }
        return list
    }

    /**
     * Retrieves all currently enabled wallpaper sources.
     */
    fun getEnabledSources(): List<WallpaperSourceEntity> {
        val list = mutableListOf<WallpaperSourceEntity>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ENABLED = 1",
            null,
            null,
            null,
            "$COLUMN_CREATED_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseSourceEntity(it))
            }
        }
        return list
    }

    /**
     * Retrieves a single wallpaper source by its unique ID.
     */
    fun getSourceById(id: String): WallpaperSourceEntity? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return parseSourceEntity(it)
            }
        }
        return null
    }

    /**
     * Inserts a new wallpaper source entity into the database.
     */
    fun insertSource(entity: WallpaperSourceEntity) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, entity.id)
            put(COLUMN_TYPE, entity.type.name)
            put(COLUMN_TITLE, entity.title)
            put(COLUMN_ENABLED, if (entity.isEnabled) 1 else 0)
            put(COLUMN_CONFIG_JSON, entity.configJson)
            put(COLUMN_CREATED_TIMESTAMP, entity.createdTimestamp)
            put(COLUMN_UPDATED_TIMESTAMP, entity.updatedTimestamp)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Updates an existing wallpaper source entity.
     */
    fun updateSource(entity: WallpaperSourceEntity) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TYPE, entity.type.name)
            put(COLUMN_TITLE, entity.title)
            put(COLUMN_ENABLED, if (entity.isEnabled) 1 else 0)
            put(COLUMN_CONFIG_JSON, entity.configJson)
            put(COLUMN_UPDATED_TIMESTAMP, System.currentTimeMillis())
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(entity.id))
    }

    /**
     * Updates the enabled status of a specific wallpaper source.
     */
    fun updateSourceEnabled(id: String, enabled: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ENABLED, if (enabled) 1 else 0)
            put(COLUMN_UPDATED_TIMESTAMP, System.currentTimeMillis())
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    /**
     * Deletes a wallpaper source by its ID.
     */
    fun deleteSource(id: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id))
        foo.barz.wallpaperpicker.core.source.CustomPhotosSource.cleanupSourceDirectory(context, id)
    }

    /**
     * Clears all configured sources from the database.
     */
    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }

    /**
     * Auto-migrates existing settings from PreferencesManager if the sources table is empty.
     */
    fun migrateFromPreferencesIfNeeded(prefs: PreferencesManager) {
        if (getAllSources().isNotEmpty()) return

        val compositeEnabled = prefs.compositeEnabledSources
        val isCompositeActive = prefs.sourceType == WallpaperSourceType.COMPOSITE

        fun isTypeActive(type: WallpaperSourceType): Boolean {
            return if (isCompositeActive) {
                compositeEnabled.contains(type)
            } else {
                prefs.sourceType == type
            }
        }

        // 1. Local Folder
        val folderUri = prefs.folderUri
        if (folderUri != null) {
            val docName = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)?.name ?: "本地文件夹"
            val config = LocalFolderSourceConfig(
                folderUri = folderUri.toString(),
                folderName = docName,
                imageCount = 0
            )
            insertSource(
                WallpaperSourceEntity(
                    type = WallpaperSourceType.LOCAL_FOLDER,
                    title = docName,
                    isEnabled = isTypeActive(WallpaperSourceType.LOCAL_FOLDER),
                    configJson = config.toJson()
                )
            )
        }

        // 2. MediaStore
        val mediaStoreConfig = MediaStoreSourceConfig(
            albumIds = prefs.mediaStoreAlbumIds,
            albumNames = prefs.mediaStoreAlbumName ?: "全部照片 (全库随机)"
        )
        insertSource(
            WallpaperSourceEntity(
                type = WallpaperSourceType.MEDIA_STORE,
                title = prefs.mediaStoreAlbumName ?: "系统相册",
                isEnabled = isTypeActive(WallpaperSourceType.MEDIA_STORE),
                configJson = mediaStoreConfig.toJson()
            )
        )

        // 3. Immich
        if (prefs.immichServerUrl.isNotBlank()) {
            val immichConfig = ImmichSourceConfig(
                serverUrl = prefs.immichServerUrl,
                apiKey = prefs.immichApiKey,
                albumIds = prefs.immichAlbumIds,
                albumNames = prefs.immichAlbumName ?: "全部相册 (全库随机)",
                quality = prefs.immichQuality,
                ignoreSsl = prefs.immichIgnoreSsl,
                wifiOnly = prefs.immichWifiOnly
            )
            insertSource(
                WallpaperSourceEntity(
                    type = WallpaperSourceType.IMMICH,
                    title = "Immich 相册",
                    isEnabled = isTypeActive(WallpaperSourceType.IMMICH),
                    configJson = immichConfig.toJson()
                )
            )
        }

        // 4. HTTP API
        val httpConfig = HttpApiSourceConfig(
            preset = prefs.httpPresetType,
            customUrl = prefs.httpCustomUrl,
            customJsonPath = prefs.httpCustomJsonPath,
            wifiOnly = prefs.wifiOnly
        )
        insertSource(
            WallpaperSourceEntity(
                type = WallpaperSourceType.HTTP_API,
                title = prefs.httpPresetType.label,
                isEnabled = isTypeActive(WallpaperSourceType.HTTP_API),
                configJson = httpConfig.toJson()
            )
        )

        // 5. Favorites
        val favConfig = FavoritesSourceConfig()
        insertSource(
            WallpaperSourceEntity(
                type = WallpaperSourceType.FAVORITES,
                title = "我的收藏",
                isEnabled = isTypeActive(WallpaperSourceType.FAVORITES),
                configJson = favConfig.toJson()
            )
        )
    }

    private fun parseSourceEntity(cursor: Cursor): WallpaperSourceEntity {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
        val type = runCatching { WallpaperSourceType.valueOf(typeStr) }.getOrDefault(WallpaperSourceType.LOCAL_FOLDER)
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
        val enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1
        val configJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONFIG_JSON))
        val created = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIMESTAMP))
        val updated = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_TIMESTAMP))
        return WallpaperSourceEntity(
            id = id,
            type = type,
            title = title,
            isEnabled = enabled,
            configJson = configJson,
            createdTimestamp = created,
            updatedTimestamp = updated
        )
    }

    companion object {
        const val DATABASE_NAME = "wallpaper_sources.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "sources"

        const val COLUMN_ID = "id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ENABLED = "enabled"
        const val COLUMN_CONFIG_JSON = "config_json"
        const val COLUMN_CREATED_TIMESTAMP = "created_timestamp"
        const val COLUMN_UPDATED_TIMESTAMP = "updated_timestamp"
    }
}
