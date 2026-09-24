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

    companion object {
        private const val PREF_NAME = "wallpaper_picker_prefs"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_TARGET = "target"
        private const val KEY_SCROLL_MODE = "scroll_mode"
        private const val KEY_IS_SCHEDULED = "is_scheduled"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val KEY_LAST_TITLE = "last_title"
    }
}
