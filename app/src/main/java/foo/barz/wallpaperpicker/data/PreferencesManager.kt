package foo.barz.wallpaperpicker.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget

/**
 * Lightweight preferences manager for saving app settings and execution state.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var folderUri: Uri?
        get() = prefs.getString(KEY_FOLDER_URI, null)?.let { Uri.parse(it) }
        set(value) = prefs.edit().putString(KEY_FOLDER_URI, value?.toString()).apply()

    var intervalMinutes: Long
        get() = prefs.getLong(KEY_INTERVAL_MINUTES, 60L)
        set(value) = prefs.edit().putLong(KEY_INTERVAL_MINUTES, value).apply()

    var target: WallpaperTarget
        get() {
            val name = prefs.getString(KEY_TARGET, WallpaperTarget.BOTH.name)
            return runCatching { WallpaperTarget.valueOf(name!!) }.getOrDefault(WallpaperTarget.BOTH)
        }
        set(value) = prefs.edit().putString(KEY_TARGET, value.name).apply()

    var scrollMode: WallpaperScrollMode
        get() {
            val name = prefs.getString(KEY_SCROLL_MODE, WallpaperScrollMode.AUTO.name)
            return runCatching { WallpaperScrollMode.valueOf(name!!) }.getOrDefault(WallpaperScrollMode.AUTO)
        }
        set(value) = prefs.edit().putString(KEY_SCROLL_MODE, value.name).apply()

    var cropMode: foo.barz.wallpaperpicker.core.model.WallpaperCropMode
        get() {
            val name = prefs.getString(KEY_CROP_MODE, foo.barz.wallpaperpicker.core.model.WallpaperCropMode.FIT_HEIGHT.name)
            return runCatching { foo.barz.wallpaperpicker.core.model.WallpaperCropMode.valueOf(name!!) }
                .getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperCropMode.FIT_HEIGHT)
        }
        set(value) = prefs.edit().putString(KEY_CROP_MODE, value.name).apply()

    var reapplyOnScrollChange: Boolean
        get() = prefs.getBoolean(KEY_REAPPLY_ON_SCROLL_CHANGE, true)
        set(value) = prefs.edit().putBoolean(KEY_REAPPLY_ON_SCROLL_CHANGE, value).apply()

    var isScheduled: Boolean
        get() = prefs.getBoolean(KEY_IS_SCHEDULED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SCHEDULED, value).apply()

    var lastChangedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_TIMESTAMP, value).apply()

    var lastWallpaperTitle: String?
        get() = prefs.getString(KEY_LAST_TITLE, null)
        set(value) = prefs.edit().putString(KEY_LAST_TITLE, value).apply()

    var lastWallpaperUri: Uri?
        get() = prefs.getString(KEY_LAST_URI, null)?.let { Uri.parse(it) }
        set(value) = prefs.edit().putString(KEY_LAST_URI, value?.toString()).apply()

    var lastWallpaperSourceType: foo.barz.wallpaperpicker.core.model.WallpaperSourceType?
        get() {
            val name = prefs.getString(KEY_LAST_SOURCE_TYPE, null) ?: return null
            return runCatching { foo.barz.wallpaperpicker.core.model.WallpaperSourceType.valueOf(name) }.getOrNull()
        }
        set(value) = prefs.edit().putString(KEY_LAST_SOURCE_TYPE, value?.name).apply()

    var lastWallpaperSourceTitle: String?
        get() = prefs.getString(KEY_LAST_SOURCE_TITLE, null)
        set(value) = prefs.edit().putString(KEY_LAST_SOURCE_TITLE, value).apply()

    var widgetScaleType: foo.barz.wallpaperpicker.core.model.WidgetScaleType
        get() {
            val name = prefs.getString(KEY_WIDGET_SCALE_TYPE, foo.barz.wallpaperpicker.core.model.WidgetScaleType.CROP.name)
            return runCatching { foo.barz.wallpaperpicker.core.model.WidgetScaleType.valueOf(name!!) }
                .getOrDefault(foo.barz.wallpaperpicker.core.model.WidgetScaleType.CROP)
        }
        set(value) = prefs.edit().putString(KEY_WIDGET_SCALE_TYPE, value.name).apply()

    var mediaStoreAlbumId: String?
        get() = prefs.getString(KEY_MEDIA_STORE_ALBUM_ID, null)
        set(value) = prefs.edit().putString(KEY_MEDIA_STORE_ALBUM_ID, value).apply()

    var mediaStoreAlbumName: String?
        get() = prefs.getString(KEY_MEDIA_STORE_ALBUM_NAME, null)
        set(value) = prefs.edit().putString(KEY_MEDIA_STORE_ALBUM_NAME, value).apply()

    var sourceType: foo.barz.wallpaperpicker.core.model.WallpaperSourceType
        get() {
            val name = prefs.getString(KEY_SOURCE_TYPE, foo.barz.wallpaperpicker.core.model.WallpaperSourceType.LOCAL_FOLDER.name)
            return runCatching { foo.barz.wallpaperpicker.core.model.WallpaperSourceType.valueOf(name!!) }
                .getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperSourceType.LOCAL_FOLDER)
        }
        set(value) = prefs.edit().putString(KEY_SOURCE_TYPE, value.name).apply()

    var httpPresetType: foo.barz.wallpaperpicker.core.model.HttpPresetType
        get() {
            val name = prefs.getString(KEY_HTTP_PRESET_TYPE, foo.barz.wallpaperpicker.core.model.HttpPresetType.BING.name)
            return runCatching { foo.barz.wallpaperpicker.core.model.HttpPresetType.valueOf(name!!) }
                .getOrDefault(foo.barz.wallpaperpicker.core.model.HttpPresetType.BING)
        }
        set(value) = prefs.edit().putString(KEY_HTTP_PRESET_TYPE, value.name).apply()

    var httpCustomUrl: String
        get() = prefs.getString(KEY_HTTP_CUSTOM_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HTTP_CUSTOM_URL, value).apply()

    var httpCustomJsonPath: String
        get() = prefs.getString(KEY_HTTP_CUSTOM_JSON_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HTTP_CUSTOM_JSON_PATH, value).apply()

    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    var immichServerUrl: String
        get() = prefs.getString(KEY_IMMICH_SERVER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IMMICH_SERVER_URL, value).apply()

    var immichApiKey: String
        get() = prefs.getString(KEY_IMMICH_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IMMICH_API_KEY, value).apply()

    var immichAlbumId: String?
        get() = prefs.getString(KEY_IMMICH_ALBUM_ID, null)
        set(value) = prefs.edit().putString(KEY_IMMICH_ALBUM_ID, value).apply()

    var immichAlbumName: String?
        get() = prefs.getString(KEY_IMMICH_ALBUM_NAME, null)
        set(value) = prefs.edit().putString(KEY_IMMICH_ALBUM_NAME, value).apply()

    var immichQuality: foo.barz.wallpaperpicker.core.model.ImmichQuality
        get() {
            val name = prefs.getString(KEY_IMMICH_QUALITY, foo.barz.wallpaperpicker.core.model.ImmichQuality.PREVIEW.name)
            return runCatching { foo.barz.wallpaperpicker.core.model.ImmichQuality.valueOf(name!!) }
                .getOrDefault(foo.barz.wallpaperpicker.core.model.ImmichQuality.PREVIEW)
        }
        set(value) = prefs.edit().putString(KEY_IMMICH_QUALITY, value.name).apply()

    var immichIgnoreSsl: Boolean
        get() = prefs.getBoolean(KEY_IMMICH_IGNORE_SSL, false)
        set(value) = prefs.edit().putBoolean(KEY_IMMICH_IGNORE_SSL, value).apply()

    var immichWifiOnly: Boolean
        get() = prefs.getBoolean(KEY_IMMICH_WIFI_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_IMMICH_WIFI_ONLY, value).apply()

    var mediaStoreAlbumIds: Set<String>
        get() {
            val set = prefs.getStringSet(KEY_MEDIA_STORE_ALBUM_IDS, null)
            if (!set.isNullOrEmpty()) return set
            return mediaStoreAlbumId?.let { setOf(it) } ?: emptySet()
        }
        set(value) {
            prefs.edit().putStringSet(KEY_MEDIA_STORE_ALBUM_IDS, value).apply()
            mediaStoreAlbumId = value.firstOrNull()
        }

    var immichAlbumIds: Set<String>
        get() {
            val set = prefs.getStringSet(KEY_IMMICH_ALBUM_IDS, null)
            if (!set.isNullOrEmpty()) return set
            return immichAlbumId?.let { setOf(it) } ?: emptySet()
        }
        set(value) {
            prefs.edit().putStringSet(KEY_IMMICH_ALBUM_IDS, value).apply()
            immichAlbumId = value.firstOrNull()
        }

    var compositeEnabledSources: Set<foo.barz.wallpaperpicker.core.model.WallpaperSourceType>
        get() {
            val raw = prefs.getStringSet(KEY_COMPOSITE_ENABLED_SOURCES, null)
            if (raw.isNullOrEmpty()) {
                return setOf(
                    foo.barz.wallpaperpicker.core.model.WallpaperSourceType.LOCAL_FOLDER,
                    foo.barz.wallpaperpicker.core.model.WallpaperSourceType.MEDIA_STORE,
                    foo.barz.wallpaperpicker.core.model.WallpaperSourceType.FAVORITES
                )
            }
            return raw.mapNotNull { name ->
                runCatching { foo.barz.wallpaperpicker.core.model.WallpaperSourceType.valueOf(name) }.getOrNull()
            }.toSet()
        }
        set(value) {
            val set = value.map { it.name }.toSet()
            prefs.edit().putStringSet(KEY_COMPOSITE_ENABLED_SOURCES, set).apply()
        }

    var lastExecutionStatus: String?
        get() = prefs.getString(KEY_LAST_EXECUTION_STATUS, null)
        set(value) = prefs.edit().putString(KEY_LAST_EXECUTION_STATUS, value).apply()

    var lastErrorMessage: String?
        get() = prefs.getString(KEY_LAST_ERROR_MESSAGE, null)
        set(value) = prefs.edit().putString(KEY_LAST_ERROR_MESSAGE, value).apply()

    var lastExecutionTimestamp: Long
        get() = prefs.getLong(KEY_LAST_EXECUTION_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_EXECUTION_TIMESTAMP, value).apply()

    var deferDuringInteraction: Boolean
        get() = prefs.getBoolean(KEY_DEFER_DURING_INTERACTION, true)
        set(value) = prefs.edit().putBoolean(KEY_DEFER_DURING_INTERACTION, value).apply()

    var fairShuffle: Boolean
        get() = prefs.getBoolean(KEY_FAIR_SHUFFLE, true)
        set(value) = prefs.edit().putBoolean(KEY_FAIR_SHUFFLE, value).apply()

    /**
     * Retrieves the FIFO list of recently applied wallpaper keys.
     */
    fun getRecentWallpaperKeys(): List<String> {
        val raw = prefs.getString(KEY_RECENT_WALLPAPER_KEYS, null) ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    /**
     * Records a wallpaper key into the FIFO history, evicting the oldest when exceeding capacity.
     */
    fun recordRecentWallpaperKey(key: String, maxCapacity: Int = 50) {
        if (key.isBlank()) return
        val current = getRecentWallpaperKeys().filter { it != key }.toMutableList()
        current.add(key)
        while (current.size > maxCapacity) {
            current.removeAt(0)
        }
        prefs.edit().putString(KEY_RECENT_WALLPAPER_KEYS, current.joinToString("\n")).apply()
    }

    /**
     * Clears the recently used wallpaper history to reset the shuffle deck.
     */
    fun clearRecentWallpaperKeys() {
        prefs.edit().remove(KEY_RECENT_WALLPAPER_KEYS).apply()
    }

    companion object {
        private const val PREF_NAME = "wallpaper_picker_prefs"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_TARGET = "target"
        private const val KEY_SCROLL_MODE = "scroll_mode"
        private const val KEY_CROP_MODE = "crop_mode"
        private const val KEY_REAPPLY_ON_SCROLL_CHANGE = "reapply_on_scroll_change"
        private const val KEY_IS_SCHEDULED = "is_scheduled"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val KEY_LAST_TITLE = "last_title"
        private const val KEY_LAST_URI = "last_uri"
        private const val KEY_LAST_SOURCE_TYPE = "last_source_type"
        private const val KEY_LAST_SOURCE_TITLE = "last_source_title"
        private const val KEY_LAST_EXECUTION_STATUS = "last_execution_status"
        private const val KEY_LAST_ERROR_MESSAGE = "last_error_message"
        private const val KEY_LAST_EXECUTION_TIMESTAMP = "last_execution_timestamp"
        private const val KEY_DEFER_DURING_INTERACTION = "defer_during_interaction"
        private const val KEY_FAIR_SHUFFLE = "fair_shuffle"
        private const val KEY_RECENT_WALLPAPER_KEYS = "recent_wallpaper_keys"
        private const val KEY_MEDIA_STORE_ALBUM_ID = "media_store_album_id"
        private const val KEY_MEDIA_STORE_ALBUM_NAME = "media_store_album_name"
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_HTTP_PRESET_TYPE = "http_preset_type"
        private const val KEY_HTTP_CUSTOM_URL = "http_custom_url"
        private const val KEY_HTTP_CUSTOM_JSON_PATH = "http_custom_json_path"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_IMMICH_SERVER_URL = "immich_server_url"
        private const val KEY_IMMICH_API_KEY = "immich_api_key"
        private const val KEY_IMMICH_ALBUM_ID = "immich_album_id"
        private const val KEY_IMMICH_ALBUM_NAME = "immich_album_name"
        private const val KEY_IMMICH_QUALITY = "immich_quality"
        private const val KEY_IMMICH_IGNORE_SSL = "immich_ignore_ssl"
        private const val KEY_IMMICH_WIFI_ONLY = "immich_wifi_only"
        private const val KEY_MEDIA_STORE_ALBUM_IDS = "media_store_album_ids"
        private const val KEY_IMMICH_ALBUM_IDS = "immich_album_ids"
        private const val KEY_COMPOSITE_ENABLED_SOURCES = "composite_enabled_sources"
        private const val KEY_WIDGET_SCALE_TYPE = "widget_scale_type"
    }
}
