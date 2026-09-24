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

    var isScheduled: Boolean
        get() = prefs.getBoolean(KEY_IS_SCHEDULED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SCHEDULED, value).apply()

    var lastChangedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_TIMESTAMP, value).apply()

    var lastWallpaperTitle: String?
        get() = prefs.getString(KEY_LAST_TITLE, null)
        set(value) = prefs.edit().putString(KEY_LAST_TITLE, value).apply()

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

    companion object {
        private const val PREF_NAME = "wallpaper_picker_prefs"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_TARGET = "target"
        private const val KEY_SCROLL_MODE = "scroll_mode"
        private const val KEY_IS_SCHEDULED = "is_scheduled"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val KEY_LAST_TITLE = "last_title"
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_HTTP_PRESET_TYPE = "http_preset_type"
        private const val KEY_HTTP_CUSTOM_URL = "http_custom_url"
        private const val KEY_HTTP_CUSTOM_JSON_PATH = "http_custom_json_path"
        private const val KEY_WIFI_ONLY = "wifi_only"
    }
}
