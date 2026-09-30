package foo.barz.wallpaperpicker.core.backup

import foo.barz.wallpaperpicker.core.model.HttpApiSourceConfig
import foo.barz.wallpaperpicker.core.model.ImmichSourceConfig
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.util.LogSanitizer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Representation of favorite status and per-image framing overrides to preserve in backups.
 */
data class BackupHistoryOverride(
    val sourceUri: String,
    val title: String? = null,
    val sourceType: WallpaperSourceType = WallpaperSourceType.LOCAL_FOLDER,
    val sourceTitle: String? = null,
    val isFavorite: Boolean = false,
    val favoriteTimestamp: Long? = null,
    val favoriteFileName: String? = null,
    val customScrollMode: WallpaperScrollMode? = null,
    val cropFocusX: Float? = null,
    val cropFocusY: Float? = null,
    val lockCropFocusX: Float? = null,
    val lockCropFocusY: Float? = null,
    val flipHorizontal: Boolean = false,
    val remoteUrl: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("sourceUri", sourceUri)
        put("title", title)
        put("sourceType", sourceType.name)
        put("sourceTitle", sourceTitle)
        put("isFavorite", isFavorite)
        put("favoriteTimestamp", favoriteTimestamp ?: JSONObject.NULL)
        put("favoriteFileName", favoriteFileName ?: JSONObject.NULL)
        put("customScrollMode", customScrollMode?.name ?: JSONObject.NULL)
        put("cropFocusX", cropFocusX?.let { it.toDouble() } ?: JSONObject.NULL)
        put("cropFocusY", cropFocusY?.let { it.toDouble() } ?: JSONObject.NULL)
        put("lockCropFocusX", lockCropFocusX?.let { it.toDouble() } ?: JSONObject.NULL)
        put("lockCropFocusY", lockCropFocusY?.let { it.toDouble() } ?: JSONObject.NULL)
        put("flipHorizontal", flipHorizontal)
        put("remoteUrl", remoteUrl ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupHistoryOverride {
            val typeStr = json.optString("sourceType", WallpaperSourceType.LOCAL_FOLDER.name)
            val scrollModeStr = if (!json.isNull("customScrollMode")) json.optString("customScrollMode") else null
            return BackupHistoryOverride(
                sourceUri = json.getString("sourceUri"),
                title = if (!json.isNull("title")) json.optString("title") else null,
                sourceType = runCatching { WallpaperSourceType.valueOf(typeStr) }.getOrDefault(WallpaperSourceType.LOCAL_FOLDER),
                sourceTitle = if (!json.isNull("sourceTitle")) json.optString("sourceTitle") else null,
                isFavorite = json.optBoolean("isFavorite", false),
                favoriteTimestamp = if (!json.isNull("favoriteTimestamp")) json.optLong("favoriteTimestamp") else null,
                favoriteFileName = if (!json.isNull("favoriteFileName")) json.optString("favoriteFileName") else null,
                customScrollMode = scrollModeStr?.let { runCatching { WallpaperScrollMode.valueOf(it) }.getOrNull() },
                cropFocusX = if (!json.isNull("cropFocusX")) json.optDouble("cropFocusX").toFloat() else null,
                cropFocusY = if (!json.isNull("cropFocusY")) json.optDouble("cropFocusY").toFloat() else null,
                lockCropFocusX = if (!json.isNull("lockCropFocusX")) json.optDouble("lockCropFocusX").toFloat() else null,
                lockCropFocusY = if (!json.isNull("lockCropFocusY")) json.optDouble("lockCropFocusY").toFloat() else null,
                flipHorizontal = json.optBoolean("flipHorizontal", false),
                remoteUrl = if (!json.isNull("remoteUrl")) json.optString("remoteUrl") else null
            )
        }
    }
}

/**
 * Portable configuration payload encapsulating app preferences.
 */
data class BackupPreferences(
    val intervalMinutes: Long = 60L,
    val target: String = "BOTH",
    val scrollMode: String = "AUTO",
    val cropMode: String = "FIT_HEIGHT",
    val lockScreenStrategy: String = "INDEPENDENT_CENTERED",
    val reapplyOnScrollChange: Boolean = true,
    val isScheduled: Boolean = false,
    val widgetScaleType: String = "CROP",
    val sourceType: String = "LOCAL_FOLDER",
    val httpPresetType: String = "BING",
    val httpCustomUrl: String = "",
    val httpCustomJsonPath: String = "",
    val wifiOnly: Boolean = true,
    val immichServerUrl: String = "",
    val immichApiKey: String = "",
    val immichAlbumId: String? = null,
    val immichAlbumName: String? = null,
    val immichQuality: String = "PREVIEW",
    val immichIgnoreSsl: Boolean = false,
    val immichWifiOnly: Boolean = true,
    val mediaStoreAlbumId: String? = null,
    val mediaStoreAlbumName: String? = null,
    val mediaStoreAlbumIds: Set<String> = emptySet(),
    val immichAlbumIds: Set<String> = emptySet(),
    val compositeEnabledSources: Set<String> = emptySet(),
    val deferDuringInteraction: Boolean = true,
    val fairShuffle: Boolean = true,
    val fairShuffleCapacity: Int = 50,
    val cacheSizeTier: String = "STANDARD",
    val intervalScheduleEnabled: Boolean = true,
    val exactTimerEnabled: Boolean = false,
    val dailyAnchorEnabled: Boolean = false,
    val dailyAnchorTimes: Set<String> = setOf("08:00"),
    val dailyAnchorHour: Int = 8,
    val dailyAnchorMinute: Int = 0,
    val screenOffTriggerEnabled: Boolean = false,
    val screenOffDelaySeconds: Int = 3,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 23,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    val cooldownSuppressionEnabled: Boolean = true,
    val cooldownMinutes: Long = 10L,
    val ruleEngineEnabled: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("intervalMinutes", intervalMinutes)
        put("target", target)
        put("scrollMode", scrollMode)
        put("cropMode", cropMode)
        put("lockScreenStrategy", lockScreenStrategy)
        put("reapplyOnScrollChange", reapplyOnScrollChange)
        put("isScheduled", isScheduled)
        put("widgetScaleType", widgetScaleType)
        put("sourceType", sourceType)
        put("httpPresetType", httpPresetType)
        put("httpCustomUrl", httpCustomUrl)
        put("httpCustomJsonPath", httpCustomJsonPath)
        put("wifiOnly", wifiOnly)
        put("immichServerUrl", immichServerUrl)
        put("immichApiKey", immichApiKey)
        put("immichAlbumId", immichAlbumId ?: JSONObject.NULL)
        put("immichAlbumName", immichAlbumName ?: JSONObject.NULL)
        put("immichQuality", immichQuality)
        put("immichIgnoreSsl", immichIgnoreSsl)
        put("immichWifiOnly", immichWifiOnly)
        put("mediaStoreAlbumId", mediaStoreAlbumId ?: JSONObject.NULL)
        put("mediaStoreAlbumName", mediaStoreAlbumName ?: JSONObject.NULL)
        put("mediaStoreAlbumIds", JSONArray(mediaStoreAlbumIds))
        put("immichAlbumIds", JSONArray(immichAlbumIds))
        put("compositeEnabledSources", JSONArray(compositeEnabledSources))
        put("deferDuringInteraction", deferDuringInteraction)
        put("fairShuffle", fairShuffle)
        put("fairShuffleCapacity", fairShuffleCapacity)
        put("cacheSizeTier", cacheSizeTier)
        put("intervalScheduleEnabled", intervalScheduleEnabled)
        put("exactTimerEnabled", exactTimerEnabled)
        put("dailyAnchorEnabled", dailyAnchorEnabled)
        put("dailyAnchorTimes", JSONArray(dailyAnchorTimes))
        put("dailyAnchorHour", dailyAnchorHour)
        put("dailyAnchorMinute", dailyAnchorMinute)
        put("screenOffTriggerEnabled", screenOffTriggerEnabled)
        put("screenOffDelaySeconds", screenOffDelaySeconds)
        put("quietHoursEnabled", quietHoursEnabled)
        put("quietHoursStartHour", quietHoursStartHour)
        put("quietHoursStartMinute", quietHoursStartMinute)
        put("quietHoursEndHour", quietHoursEndHour)
        put("quietHoursEndMinute", quietHoursEndMinute)
        put("cooldownSuppressionEnabled", cooldownSuppressionEnabled)
        put("cooldownMinutes", cooldownMinutes)
        put("ruleEngineEnabled", ruleEngineEnabled)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupPreferences {
            fun parseStringSet(key: String): Set<String> {
                val array = json.optJSONArray(key) ?: return emptySet()
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    set.add(array.optString(i))
                }
                return set
            }

            return BackupPreferences(
                intervalMinutes = json.optLong("intervalMinutes", 60L),
                target = json.optString("target", "BOTH"),
                scrollMode = json.optString("scrollMode", "AUTO"),
                cropMode = json.optString("cropMode", "FIT_HEIGHT"),
                lockScreenStrategy = json.optString("lockScreenStrategy", "INDEPENDENT_CENTERED"),
                reapplyOnScrollChange = json.optBoolean("reapplyOnScrollChange", true),
                isScheduled = json.optBoolean("isScheduled", false),
                widgetScaleType = json.optString("widgetScaleType", "CROP"),
                sourceType = json.optString("sourceType", "LOCAL_FOLDER"),
                httpPresetType = json.optString("httpPresetType", "BING"),
                httpCustomUrl = json.optString("httpCustomUrl", ""),
                httpCustomJsonPath = json.optString("httpCustomJsonPath", ""),
                wifiOnly = json.optBoolean("wifiOnly", true),
                immichServerUrl = json.optString("immichServerUrl", ""),
                immichApiKey = json.optString("immichApiKey", ""),
                immichAlbumId = if (!json.isNull("immichAlbumId")) json.optString("immichAlbumId") else null,
                immichAlbumName = if (!json.isNull("immichAlbumName")) json.optString("immichAlbumName") else null,
                immichQuality = json.optString("immichQuality", "PREVIEW"),
                immichIgnoreSsl = json.optBoolean("immichIgnoreSsl", false),
                immichWifiOnly = json.optBoolean("immichWifiOnly", true),
                mediaStoreAlbumId = if (!json.isNull("mediaStoreAlbumId")) json.optString("mediaStoreAlbumId") else null,
                mediaStoreAlbumName = if (!json.isNull("mediaStoreAlbumName")) json.optString("mediaStoreAlbumName") else null,
                mediaStoreAlbumIds = parseStringSet("mediaStoreAlbumIds"),
                immichAlbumIds = parseStringSet("immichAlbumIds"),
                compositeEnabledSources = parseStringSet("compositeEnabledSources"),
                deferDuringInteraction = json.optBoolean("deferDuringInteraction", true),
                fairShuffle = json.optBoolean("fairShuffle", true),
                fairShuffleCapacity = json.optInt("fairShuffleCapacity", 50),
                cacheSizeTier = json.optString("cacheSizeTier", "STANDARD"),
                intervalScheduleEnabled = json.optBoolean("intervalScheduleEnabled", true),
                exactTimerEnabled = json.optBoolean("exactTimerEnabled", false),
                dailyAnchorEnabled = json.optBoolean("dailyAnchorEnabled", false),
                dailyAnchorTimes = parseStringSet("dailyAnchorTimes").ifEmpty { setOf("08:00") },
                dailyAnchorHour = json.optInt("dailyAnchorHour", 8),
                dailyAnchorMinute = json.optInt("dailyAnchorMinute", 0),
                screenOffTriggerEnabled = json.optBoolean("screenOffTriggerEnabled", false),
                screenOffDelaySeconds = json.optInt("screenOffDelaySeconds", 3),
                quietHoursEnabled = json.optBoolean("quietHoursEnabled", false),
                quietHoursStartHour = json.optInt("quietHoursStartHour", 23),
                quietHoursStartMinute = json.optInt("quietHoursStartMinute", 0),
                quietHoursEndHour = json.optInt("quietHoursEndHour", 7),
                quietHoursEndMinute = json.optInt("quietHoursEndMinute", 0),
                cooldownSuppressionEnabled = json.optBoolean("cooldownSuppressionEnabled", true),
                cooldownMinutes = json.optLong("cooldownMinutes", 10L),
                ruleEngineEnabled = json.optBoolean("ruleEngineEnabled", false)
            )
        }
    }
}

/**
 * Top-level backup data container.
 */
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersionName: String,
    val appVersionCode: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val preferences: BackupPreferences,
    val sources: List<WallpaperSourceEntity>,
    val rules: List<ScheduleRule>,
    val historyOverrides: List<BackupHistoryOverride> = emptyList()
) {
    /**
     * Checks if the backup payload contains sensitive credentials or confidential endpoints.
     */
    fun containsSensitiveData(): Boolean {
        if (preferences.immichApiKey.isNotBlank()) return true
        if (hasUnredactedCredentials(preferences.httpCustomUrl)) return true

        for (source in sources) {
            when (source.type) {
                WallpaperSourceType.IMMICH -> {
                    val config = ImmichSourceConfig.fromJson(source.configJson)
                    if (config.apiKey.isNotBlank()) return true
                }
                WallpaperSourceType.HTTP_API -> {
                    val config = HttpApiSourceConfig.fromJson(source.configJson)
                    if (hasUnredactedCredentials(config.customUrl)) return true
                }
                else -> {}
            }
        }
        return false
    }

    private fun hasUnredactedCredentials(url: String): Boolean {
        if (url.isBlank()) return false
        val regex = Regex("""(?i)[?&](?:api[_-]?key|access[_-]?token|token|auth|key|secret)=([^&\s]+)""")
        return regex.findAll(url).any { match ->
            val value = match.groupValues[1]
            value != "[REDACTED]" && value.isNotBlank()
        }
    }

    /**
     * Sanitizes credentials (API keys, query tokens) for unencrypted or shared export.
     */
    fun sanitize(): BackupPayload {
        val sanitizedPrefs = preferences.copy(
            immichApiKey = "",
            httpCustomUrl = LogSanitizer.sanitize(preferences.httpCustomUrl)
        )

        val sanitizedSources = sources.map { entity ->
            when (entity.type) {
                WallpaperSourceType.IMMICH -> {
                    val config = ImmichSourceConfig.fromJson(entity.configJson)
                    val sanitizedConfig = config.copy(apiKey = "")
                    entity.copy(configJson = sanitizedConfig.toJson())
                }
                WallpaperSourceType.HTTP_API -> {
                    val config = HttpApiSourceConfig.fromJson(entity.configJson)
                    val sanitizedConfig = config.copy(customUrl = LogSanitizer.sanitize(config.customUrl))
                    entity.copy(configJson = sanitizedConfig.toJson())
                }
                else -> entity
            }
        }

        return copy(
            preferences = sanitizedPrefs,
            sources = sanitizedSources
        )
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", schemaVersion)
        root.put("appVersionName", appVersionName)
        root.put("appVersionCode", appVersionCode)
        root.put("createdAt", createdAt)
        root.put("preferences", preferences.toJson())

        val sourcesArray = JSONArray()
        sources.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("type", s.type.name)
                put("title", s.title)
                put("isEnabled", s.isEnabled)
                put("configJson", s.configJson)
                put("createdTimestamp", s.createdTimestamp)
                put("updatedTimestamp", s.updatedTimestamp)
            }
            sourcesArray.put(obj)
        }
        root.put("sources", sourcesArray)

        val rulesArray = JSONArray()
        rules.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("isEnabled", r.isEnabled)
                put("triggerType", r.triggerType.name)
                put("targetTime", r.targetTime)
                put("windowStartTime", r.windowStartTime)
                put("windowEndTime", r.windowEndTime)
                put("intervalMinutes", r.intervalMinutes)
                put("screenOffDelaySeconds", r.screenOffDelaySeconds)
                put("sourceBinding", r.sourceBinding.name)
                put("specificSourceId", r.specificSourceId ?: JSONObject.NULL)
                put("specificSourceTitle", r.specificSourceTitle ?: JSONObject.NULL)
                put("targetScreen", r.targetScreen.name)
                put("createdTimestamp", r.createdTimestamp)
                put("updatedTimestamp", r.updatedTimestamp)
            }
            rulesArray.put(obj)
        }
        root.put("rules", rulesArray)

        val historyArray = JSONArray()
        historyOverrides.forEach { historyArray.put(it.toJson()) }
        root.put("historyOverrides", historyArray)

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): BackupPayload {
            val root = JSONObject(jsonStr)
            val schemaVersion = root.optInt("schemaVersion", 1)
            val appVersionName = root.optString("appVersionName", "1.0.0")
            val appVersionCode = root.optInt("appVersionCode", 1)
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())

            val prefsObj = root.optJSONObject("preferences") ?: JSONObject()
            val preferences = BackupPreferences.fromJson(prefsObj)

            val sources = mutableListOf<WallpaperSourceEntity>()
            val sourcesArray = root.optJSONArray("sources")
            if (sourcesArray != null) {
                for (i in 0 until sourcesArray.length()) {
                    val sObj = sourcesArray.getJSONObject(i)
                    val typeStr = sObj.optString("type", WallpaperSourceType.LOCAL_FOLDER.name)
                    sources.add(
                        WallpaperSourceEntity(
                            id = sObj.getString("id"),
                            type = runCatching { WallpaperSourceType.valueOf(typeStr) }.getOrDefault(WallpaperSourceType.LOCAL_FOLDER),
                            title = sObj.optString("title", "未命名源"),
                            isEnabled = sObj.optBoolean("isEnabled", true),
                            configJson = sObj.optString("configJson", "{}"),
                            createdTimestamp = sObj.optLong("createdTimestamp", System.currentTimeMillis()),
                            updatedTimestamp = sObj.optLong("updatedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val rules = mutableListOf<ScheduleRule>()
            val rulesArray = root.optJSONArray("rules")
            if (rulesArray != null) {
                for (i in 0 until rulesArray.length()) {
                    val rObj = rulesArray.getJSONObject(i)
                    rules.add(
                        ScheduleRule(
                            id = rObj.getString("id"),
                            name = rObj.optString("name", "未命名规则"),
                            isEnabled = rObj.optBoolean("isEnabled", true),
                            triggerType = runCatching { foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType.valueOf(rObj.getString("triggerType")) }
                                .getOrDefault(foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType.DAILY_TIME),
                            targetTime = rObj.optString("targetTime", "08:00"),
                            windowStartTime = rObj.optString("windowStartTime", "09:00"),
                            windowEndTime = rObj.optString("windowEndTime", "18:00"),
                            intervalMinutes = rObj.optLong("intervalMinutes", 60L),
                            screenOffDelaySeconds = rObj.optInt("screenOffDelaySeconds", 3),
                            sourceBinding = runCatching { foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding.valueOf(rObj.getString("sourceBinding")) }
                                .getOrDefault(foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding.ACTIVE_DEFAULT),
                            specificSourceId = if (!rObj.isNull("specificSourceId")) rObj.optString("specificSourceId") else null,
                            specificSourceTitle = if (!rObj.isNull("specificSourceTitle")) rObj.optString("specificSourceTitle") else null,
                            targetScreen = runCatching { foo.barz.wallpaperpicker.core.model.WallpaperTarget.valueOf(rObj.getString("targetScreen")) }
                                .getOrDefault(foo.barz.wallpaperpicker.core.model.WallpaperTarget.BOTH),
                            createdTimestamp = rObj.optLong("createdTimestamp", System.currentTimeMillis()),
                            updatedTimestamp = rObj.optLong("updatedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val historyOverrides = mutableListOf<BackupHistoryOverride>()
            val historyArray = root.optJSONArray("historyOverrides")
            if (historyArray != null) {
                for (i in 0 until historyArray.length()) {
                    historyOverrides.add(BackupHistoryOverride.fromJson(historyArray.getJSONObject(i)))
                }
            }

            return BackupPayload(
                schemaVersion = schemaVersion,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                createdAt = createdAt,
                preferences = preferences,
                sources = sources,
                rules = rules,
                historyOverrides = historyOverrides
            )
        }
    }
}
