package foo.barz.wallpaperpicker.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper

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

    var fairShuffleCapacity: Int
        get() = prefs.getInt(KEY_FAIR_SHUFFLE_CAPACITY, 50)
        set(value) = prefs.edit().putInt(KEY_FAIR_SHUFFLE_CAPACITY, value).apply()

    var cacheSizeTier: CacheSizeTier
        get() {
            val name = prefs.getString(KEY_CACHE_SIZE_TIER, CacheSizeTier.STANDARD.name)
            return runCatching { CacheSizeTier.valueOf(name!!) }.getOrDefault(CacheSizeTier.STANDARD)
        }
        set(value) = prefs.edit().putString(KEY_CACHE_SIZE_TIER, value.name).apply()

    var intervalScheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_INTERVAL_SCHEDULE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INTERVAL_SCHEDULE_ENABLED, value).apply()

    var exactTimerEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXACT_TIMER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_EXACT_TIMER_ENABLED, value).apply()

    var dailyAnchorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_ANCHOR_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_ANCHOR_ENABLED, value).apply()

    var dailyAnchorTimes: Set<String>
        get() {
            val set = prefs.getStringSet(KEY_DAILY_ANCHOR_TIMES, null)
            if (!set.isNullOrEmpty()) return set
            val legacyTime = CompositeTriggerHelper.formatTime(dailyAnchorHour, dailyAnchorMinute)
            return setOf(legacyTime)
        }
        set(value) {
            prefs.edit().putStringSet(KEY_DAILY_ANCHOR_TIMES, value).apply()
            value.firstOrNull()?.let { timeStr ->
                CompositeTriggerHelper.parseTime(timeStr)?.let { (h, m) ->
                    dailyAnchorHour = h
                    dailyAnchorMinute = m
                }
            }
        }

    var dailyAnchorHour: Int
        get() = prefs.getInt(KEY_DAILY_ANCHOR_HOUR, 8)
        set(value) = prefs.edit().putInt(KEY_DAILY_ANCHOR_HOUR, value).apply()

    var dailyAnchorMinute: Int
        get() = prefs.getInt(KEY_DAILY_ANCHOR_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_ANCHOR_MINUTE, value).apply()

    var screenOffTriggerEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_OFF_TRIGGER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_OFF_TRIGGER_ENABLED, value).apply()

    var screenOffDelaySeconds: Int
        get() = prefs.getInt(KEY_SCREEN_OFF_DELAY_SECONDS, 3)
        set(value) = prefs.edit().putInt(KEY_SCREEN_OFF_DELAY_SECONDS, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, value).apply()

    var quietHoursStartHour: Int
        get() = prefs.getInt(KEY_QUIET_HOURS_START_HOUR, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_START_HOUR, value).apply()

    var quietHoursStartMinute: Int
        get() = prefs.getInt(KEY_QUIET_HOURS_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_START_MINUTE, value).apply()

    var quietHoursEndHour: Int
        get() = prefs.getInt(KEY_QUIET_HOURS_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_END_HOUR, value).apply()

    var quietHoursEndMinute: Int
        get() = prefs.getInt(KEY_QUIET_HOURS_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_END_MINUTE, value).apply()

    var cooldownSuppressionEnabled: Boolean
        get() = prefs.getBoolean(KEY_COOLDOWN_SUPPRESSION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_COOLDOWN_SUPPRESSION_ENABLED, value).apply()

    var cooldownMinutes: Long
        get() = prefs.getLong(KEY_COOLDOWN_MINUTES, 10L)
        set(value) = prefs.edit().putLong(KEY_COOLDOWN_MINUTES, value).apply()

    var ruleEngineEnabled: Boolean
        get() = prefs.getBoolean(KEY_RULE_ENGINE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_RULE_ENGINE_ENABLED, value).apply()

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
    fun recordRecentWallpaperKey(key: String, maxCapacity: Int = fairShuffleCapacity) {
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

    fun exportBackupPreferences(): foo.barz.wallpaperpicker.core.backup.BackupPreferences {
        return foo.barz.wallpaperpicker.core.backup.BackupPreferences(
            intervalMinutes = intervalMinutes,
            target = target.name,
            scrollMode = scrollMode.name,
            cropMode = cropMode.name,
            reapplyOnScrollChange = reapplyOnScrollChange,
            isScheduled = isScheduled,
            widgetScaleType = widgetScaleType.name,
            sourceType = sourceType.name,
            httpPresetType = httpPresetType.name,
            httpCustomUrl = httpCustomUrl,
            httpCustomJsonPath = httpCustomJsonPath,
            wifiOnly = wifiOnly,
            immichServerUrl = immichServerUrl,
            immichApiKey = immichApiKey,
            immichAlbumId = immichAlbumId,
            immichAlbumName = immichAlbumName,
            immichQuality = immichQuality.name,
            immichIgnoreSsl = immichIgnoreSsl,
            immichWifiOnly = immichWifiOnly,
            mediaStoreAlbumId = mediaStoreAlbumId,
            mediaStoreAlbumName = mediaStoreAlbumName,
            mediaStoreAlbumIds = mediaStoreAlbumIds,
            immichAlbumIds = immichAlbumIds,
            compositeEnabledSources = compositeEnabledSources.map { it.name }.toSet(),
            deferDuringInteraction = deferDuringInteraction,
            fairShuffle = fairShuffle,
            fairShuffleCapacity = fairShuffleCapacity,
            cacheSizeTier = cacheSizeTier.name,
            intervalScheduleEnabled = intervalScheduleEnabled,
            exactTimerEnabled = exactTimerEnabled,
            dailyAnchorEnabled = dailyAnchorEnabled,
            dailyAnchorTimes = dailyAnchorTimes,
            dailyAnchorHour = dailyAnchorHour,
            dailyAnchorMinute = dailyAnchorMinute,
            screenOffTriggerEnabled = screenOffTriggerEnabled,
            screenOffDelaySeconds = screenOffDelaySeconds,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStartHour = quietHoursStartHour,
            quietHoursStartMinute = quietHoursStartMinute,
            quietHoursEndHour = quietHoursEndHour,
            quietHoursEndMinute = quietHoursEndMinute,
            cooldownSuppressionEnabled = cooldownSuppressionEnabled,
            cooldownMinutes = cooldownMinutes,
            ruleEngineEnabled = ruleEngineEnabled
        )
    }

    fun importBackupPreferences(bp: foo.barz.wallpaperpicker.core.backup.BackupPreferences) {
        intervalMinutes = bp.intervalMinutes
        target = runCatching { foo.barz.wallpaperpicker.core.model.WallpaperTarget.valueOf(bp.target) }.getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperTarget.BOTH)
        scrollMode = runCatching { foo.barz.wallpaperpicker.core.model.WallpaperScrollMode.valueOf(bp.scrollMode) }.getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperScrollMode.AUTO)
        cropMode = runCatching { foo.barz.wallpaperpicker.core.model.WallpaperCropMode.valueOf(bp.cropMode) }.getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperCropMode.FIT_HEIGHT)
        reapplyOnScrollChange = bp.reapplyOnScrollChange
        isScheduled = bp.isScheduled
        widgetScaleType = runCatching { foo.barz.wallpaperpicker.core.model.WidgetScaleType.valueOf(bp.widgetScaleType) }.getOrDefault(foo.barz.wallpaperpicker.core.model.WidgetScaleType.CROP)
        sourceType = runCatching { foo.barz.wallpaperpicker.core.model.WallpaperSourceType.valueOf(bp.sourceType) }.getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperSourceType.LOCAL_FOLDER)
        httpPresetType = runCatching { foo.barz.wallpaperpicker.core.model.HttpPresetType.valueOf(bp.httpPresetType) }.getOrDefault(foo.barz.wallpaperpicker.core.model.HttpPresetType.BING)
        httpCustomUrl = bp.httpCustomUrl
        httpCustomJsonPath = bp.httpCustomJsonPath
        wifiOnly = bp.wifiOnly
        immichServerUrl = bp.immichServerUrl
        immichApiKey = bp.immichApiKey
        immichAlbumId = bp.immichAlbumId
        immichAlbumName = bp.immichAlbumName
        immichQuality = runCatching { foo.barz.wallpaperpicker.core.model.ImmichQuality.valueOf(bp.immichQuality) }.getOrDefault(foo.barz.wallpaperpicker.core.model.ImmichQuality.PREVIEW)
        immichIgnoreSsl = bp.immichIgnoreSsl
        immichWifiOnly = bp.immichWifiOnly
        mediaStoreAlbumId = bp.mediaStoreAlbumId
        mediaStoreAlbumName = bp.mediaStoreAlbumName
        mediaStoreAlbumIds = bp.mediaStoreAlbumIds
        immichAlbumIds = bp.immichAlbumIds
        compositeEnabledSources = bp.compositeEnabledSources.mapNotNull { runCatching { foo.barz.wallpaperpicker.core.model.WallpaperSourceType.valueOf(it) }.getOrNull() }.toSet()
        deferDuringInteraction = bp.deferDuringInteraction
        fairShuffle = bp.fairShuffle
        fairShuffleCapacity = bp.fairShuffleCapacity
        cacheSizeTier = runCatching { foo.barz.wallpaperpicker.core.model.CacheSizeTier.valueOf(bp.cacheSizeTier) }.getOrDefault(foo.barz.wallpaperpicker.core.model.CacheSizeTier.STANDARD)
        intervalScheduleEnabled = bp.intervalScheduleEnabled
        exactTimerEnabled = bp.exactTimerEnabled
        dailyAnchorEnabled = bp.dailyAnchorEnabled
        dailyAnchorTimes = bp.dailyAnchorTimes
        dailyAnchorHour = bp.dailyAnchorHour
        dailyAnchorMinute = bp.dailyAnchorMinute
        screenOffTriggerEnabled = bp.screenOffTriggerEnabled
        screenOffDelaySeconds = bp.screenOffDelaySeconds
        quietHoursEnabled = bp.quietHoursEnabled
        quietHoursStartHour = bp.quietHoursStartHour
        quietHoursStartMinute = bp.quietHoursStartMinute
        quietHoursEndHour = bp.quietHoursEndHour
        quietHoursEndMinute = bp.quietHoursEndMinute
        cooldownSuppressionEnabled = bp.cooldownSuppressionEnabled
        cooldownMinutes = bp.cooldownMinutes
        ruleEngineEnabled = bp.ruleEngineEnabled
    }

    var hasUserAddedSource: Boolean
        get() = prefs.getBoolean(KEY_HAS_USER_ADDED_SOURCE, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_USER_ADDED_SOURCE, value).apply()

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREF_NAME = "wallpaper_picker_prefs"
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        const val KEY_TARGET = "target"
        const val KEY_SCROLL_MODE = "scroll_mode"
        const val KEY_CROP_MODE = "crop_mode"
        const val KEY_REAPPLY_ON_SCROLL_CHANGE = "reapply_on_scroll_change"
        const val KEY_IS_SCHEDULED = "is_scheduled"
        const val KEY_LAST_TIMESTAMP = "last_timestamp"
        const val KEY_LAST_TITLE = "last_title"
        const val KEY_LAST_URI = "last_uri"
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
        private const val KEY_CACHE_SIZE_TIER = "cache_size_tier"
        private const val KEY_FAIR_SHUFFLE_CAPACITY = "fair_shuffle_capacity"
        private const val KEY_EXACT_TIMER_ENABLED = "exact_timer_enabled"
        private const val KEY_DAILY_ANCHOR_ENABLED = "daily_anchor_enabled"
        private const val KEY_DAILY_ANCHOR_HOUR = "daily_anchor_hour"
        private const val KEY_DAILY_ANCHOR_MINUTE = "daily_anchor_minute"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START_HOUR = "quiet_hours_start_hour"
        private const val KEY_QUIET_HOURS_START_MINUTE = "quiet_hours_start_minute"
        private const val KEY_QUIET_HOURS_END_HOUR = "quiet_hours_end_hour"
        private const val KEY_QUIET_HOURS_END_MINUTE = "quiet_hours_end_minute"
        private const val KEY_COOLDOWN_SUPPRESSION_ENABLED = "cooldown_suppression_enabled"
        private const val KEY_COOLDOWN_MINUTES = "cooldown_minutes"
        private const val KEY_INTERVAL_SCHEDULE_ENABLED = "interval_schedule_enabled"
        private const val KEY_DAILY_ANCHOR_TIMES = "daily_anchor_times"
        private const val KEY_SCREEN_OFF_TRIGGER_ENABLED = "screen_off_trigger_enabled"
        private const val KEY_SCREEN_OFF_DELAY_SECONDS = "screen_off_delay_seconds"
        private const val KEY_RULE_ENGINE_ENABLED = "rule_engine_enabled"
        private const val KEY_HAS_USER_ADDED_SOURCE = "has_user_added_source"
    }
}

