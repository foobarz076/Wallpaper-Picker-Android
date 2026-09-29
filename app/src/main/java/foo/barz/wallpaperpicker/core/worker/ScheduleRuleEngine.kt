package foo.barz.wallpaperpicker.core.worker

import android.content.Context
import foo.barz.wallpaperpicker.core.database.ScheduleRulesDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.core.source.FavoritesSource
import foo.barz.wallpaperpicker.core.source.WallpaperSource
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.util.Calendar

/**
 * Context of what event triggered the execution.
 */
enum class TriggerEventContext {
    EXACT_ALARM,
    PERIODIC_WORKER,
    SCREEN_OFF,
    MANUAL
}

/**
 * Core engine for evaluating and scheduling multi-rule automation in Phase 4.2.
 */
object ScheduleRuleEngine {

    /**
     * Resolves the matching schedule rule for the given timestamp and event context.
     * If [specificRuleId] is provided, attempts to load that rule directly.
     */
    fun findMatchingRule(
        context: Context,
        now: Long,
        eventContext: TriggerEventContext,
        specificRuleId: String? = null
    ): ScheduleRule? {
        val db = ScheduleRulesDatabase(context)
        if (!specificRuleId.isNullOrBlank()) {
            val rule = db.getRuleById(specificRuleId)
            if (rule != null && rule.isEnabled) return rule
        }

        val enabledRules = db.getEnabledRules()
        if (enabledRules.isEmpty()) return null

        when (eventContext) {
            TriggerEventContext.SCREEN_OFF -> {
                // Find screen off rule
                return enabledRules.firstOrNull { it.triggerType == ScheduleRuleTriggerType.SCREEN_OFF }
            }
            TriggerEventContext.EXACT_ALARM, TriggerEventContext.PERIODIC_WORKER -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = now }
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val currentMinuteOfDay = currentHour * 60 + currentMinute

                // 1. Check DAILY_TIME rules matching current minute (tolerance within +/- 2 minutes)
                val matchingDailyRule = enabledRules.firstOrNull { rule ->
                    if (rule.triggerType == ScheduleRuleTriggerType.DAILY_TIME) {
                        CompositeTriggerHelper.parseTime(rule.targetTime)?.let { (h, m) ->
                            val targetMinuteOfDay = h * 60 + m
                            Math.abs(currentMinuteOfDay - targetMinuteOfDay) <= 2
                        } ?: false
                    } else false
                }
                if (matchingDailyRule != null) return matchingDailyRule

                // 2. Check TIME_WINDOW rules if current time falls within window
                val prefs = PreferencesManager(context)
                val matchingWindowRule = enabledRules.firstOrNull { rule ->
                    if (rule.triggerType == ScheduleRuleTriggerType.TIME_WINDOW) {
                        if (isWithinTimeWindow(currentMinuteOfDay, rule.windowStartTime, rule.windowEndTime)) {
                            // If triggered via fallback PeriodicWorker, verify interval has passed since last change
                            if (eventContext == TriggerEventContext.PERIODIC_WORKER && prefs.lastChangedTimestamp > 0L) {
                                val intervalMs = rule.intervalMinutes.coerceAtLeast(5L) * 60 * 1000L
                                (now - prefs.lastChangedTimestamp) >= (intervalMs - 60_000L)
                            } else {
                                true
                            }
                        } else false
                    } else false
                }
                if (matchingWindowRule != null) return matchingWindowRule
            }
            TriggerEventContext.MANUAL -> {
                // Manual trigger doesn't bind to a specific rule automatically
                return null
            }
        }

        return null
    }

    /**
     * Determines if a minute of day falls within [startTime, endTime).
     * Supports both same-day and overnight windows.
     */
    fun isWithinTimeWindow(minuteOfDay: Int, startTime: String, endTime: String): Boolean {
        val start = CompositeTriggerHelper.parseTime(startTime) ?: return false
        val end = CompositeTriggerHelper.parseTime(endTime) ?: return false
        val startMinuteOfDay = start.first * 60 + start.second
        val endMinuteOfDay = end.first * 60 + end.second

        return if (startMinuteOfDay <= endMinuteOfDay) {
            minuteOfDay in startMinuteOfDay until endMinuteOfDay
        } else {
            minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
        }
    }

    /**
     * Calculates the nearest upcoming trigger timestamp among all enabled rules.
     * Returns a pair of (triggerTimestamp, targetRule), or null if no triggers are scheduled.
     */
    fun computeNextTriggerMillis(
        context: Context,
        now: Long
    ): Pair<Long, ScheduleRule>? {
        val db = ScheduleRulesDatabase(context)
        val enabledRules = db.getEnabledRules()
        if (enabledRules.isEmpty()) return null

        val prefs = PreferencesManager(context)
        val candidates = mutableListOf<Pair<Long, ScheduleRule>>()

        for (rule in enabledRules) {
            when (rule.triggerType) {
                ScheduleRuleTriggerType.DAILY_TIME -> {
                    CompositeTriggerHelper.parseTime(rule.targetTime)?.let { (h, m) ->
                        val delay = CompositeTriggerHelper.getDelayUntilNextAnchor(now, h, m)
                        candidates.add(Pair(now + delay, rule))
                    }
                }
                ScheduleRuleTriggerType.TIME_WINDOW -> {
                    computeNextWindowTrigger(now, prefs.lastChangedTimestamp, rule)?.let { triggerTime ->
                        candidates.add(Pair(triggerTime, rule))
                    }
                }
                ScheduleRuleTriggerType.SCREEN_OFF -> {
                    // Screen-off is purely event-driven, not scheduled via AlarmManager
                }
            }
        }

        return candidates.minByOrNull { it.first }
    }

    /**
     * Computes the next scheduled time for a TIME_WINDOW rule.
     */
    fun computeNextWindowTrigger(
        now: Long,
        lastChangedTimestamp: Long,
        rule: ScheduleRule
    ): Long? {
        val start = CompositeTriggerHelper.parseTime(rule.windowStartTime) ?: return null
        val end = CompositeTriggerHelper.parseTime(rule.windowEndTime) ?: return null

        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinuteOfDay = start.first * 60 + start.second
        val endMinuteOfDay = end.first * 60 + end.second

        val intervalMs = rule.intervalMinutes.coerceAtLeast(5L) * 60 * 1000L

        val inWindow = if (startMinuteOfDay <= endMinuteOfDay) {
            currentMinuteOfDay in startMinuteOfDay until endMinuteOfDay
        } else {
            currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay < endMinuteOfDay
        }

        if (inWindow) {
            // Currently inside window. Next trigger is lastChangedTimestamp + intervalMs
            val nextByInterval = if (lastChangedTimestamp > 0L) {
                lastChangedTimestamp + intervalMs
            } else {
                now + intervalMs
            }
            // Ensure next trigger is strictly in the future
            val effectiveNext = if (nextByInterval <= now) now + intervalMs else nextByInterval

            // Verify if effectiveNext still falls inside the window
            val nextCal = Calendar.getInstance().apply { timeInMillis = effectiveNext }
            val nextMinuteOfDay = nextCal.get(Calendar.HOUR_OF_DAY) * 60 + nextCal.get(Calendar.MINUTE)

            val stillInWindow = if (startMinuteOfDay <= endMinuteOfDay) {
                nextMinuteOfDay in startMinuteOfDay until endMinuteOfDay
            } else {
                nextMinuteOfDay >= startMinuteOfDay || nextMinuteOfDay < endMinuteOfDay
            }

            return if (stillInWindow) {
                effectiveNext
            } else {
                // Rollover to the next start of the window
                now + CompositeTriggerHelper.getDelayUntilNextAnchor(now, start.first, start.second)
            }
        } else {
            // Currently outside window. Next trigger is when the window opens
            return now + CompositeTriggerHelper.getDelayUntilNextAnchor(now, start.first, start.second)
        }
    }

    /**
     * Resolves the appropriate WallpaperSource for the specified rule.
     * Falls back to active default source if specific source is unavailable.
     */
    fun resolveSourceForRule(
        context: Context,
        rule: ScheduleRule,
        prefs: PreferencesManager,
        bypassNetworkConstraints: Boolean = false
    ): WallpaperSource {
        return when (rule.sourceBinding) {
            ScheduleRuleSourceBinding.ACTIVE_DEFAULT -> {
                WallpaperSourceFactory.createActiveSource(context, prefs, bypassNetworkConstraints)
            }
            ScheduleRuleSourceBinding.FAVORITES -> {
                FavoritesSource(context, WallpaperHistoryDatabase(context))
            }
            ScheduleRuleSourceBinding.SPECIFIC_SOURCE -> {
                val sourceId = rule.specificSourceId
                if (!sourceId.isNullOrBlank()) {
                    val sourcesDb = WallpaperSourcesDatabase(context)
                    val entity = sourcesDb.getSourceById(sourceId)
                    if (entity != null) {
                        return WallpaperSourceFactory.createSourceFromEntity(
                            context = context,
                            entity = entity,
                            bypassNetworkConstraints = bypassNetworkConstraints
                        )
                    }
                }
                // Fallback to active source if specific entity was deleted or missing
                WallpaperSourceFactory.createActiveSource(context, prefs, bypassNetworkConstraints)
            }
        }
    }
}
