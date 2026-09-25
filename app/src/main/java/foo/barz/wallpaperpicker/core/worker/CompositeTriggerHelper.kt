package foo.barz.wallpaperpicker.core.worker

import java.util.Calendar
import java.util.Locale

/**
 * Utility helper for composite trigger scheduling:
 * - Quiet Hours checking (supports overnight and same-day intervals)
 * - Cooldown suppression to avoid collisions between scheduled and manual/anchor changes
 * - Wall-clock daily anchor delay calculations
 */
object CompositeTriggerHelper {

    /**
     * Determines whether the given [timestamp] falls within the configured quiet hours window.
     * Supports both overnight windows (e.g. 23:00 to 07:00) and same-day windows (e.g. 13:00 to 15:00).
     */
    fun isInQuietHours(
        timestamp: Long,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinuteOfDay = startHour * 60 + startMinute
        val endMinuteOfDay = endHour * 60 + endMinute

        return if (startMinuteOfDay <= endMinuteOfDay) {
            // Same-day range (e.g. 13:00 - 15:00)
            currentMinuteOfDay in startMinuteOfDay until endMinuteOfDay
        } else {
            // Overnight range (e.g. 23:00 - 07:00)
            currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay < endMinuteOfDay
        }
    }

    /**
     * Checks if a new change should be suppressed because the last wallpaper change occurred too recently.
     * Prevents flashing rapid transitions when a periodic interval and a daily anchor trigger close together.
     */
    fun isCooldownSuppressed(
        now: Long,
        lastChangedTimestamp: Long,
        cooldownMinutes: Long
    ): Boolean {
        if (lastChangedTimestamp <= 0L || cooldownMinutes <= 0L) return false
        val elapsed = now - lastChangedTimestamp
        return elapsed in 0 until (cooldownMinutes * 60 * 1000L)
    }

    /**
     * Computes the millisecond delay from [now] until the next occurrence of [targetHour]:[targetMinute].
     * If the time for today has already passed, rolls over to the next day.
     */
    fun getDelayUntilNextAnchor(now: Long, targetHour: Int, targetMinute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var targetTime = calendar.timeInMillis
        if (targetTime <= now) {
            targetTime += 24 * 60 * 60 * 1000L
        }
        return targetTime - now
    }

    /**
     * Parses a 24-hour time string ("HH:mm") into a pair of (hour, minute).
     * Returns null if format is invalid.
     */
    fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        if (parts[0].length != 2 || parts[1].length != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }

    /**
     * Computes the shortest millisecond delay from [now] until the next upcoming anchor across multiple times.
     * For example, given {"08:00", "12:00"}, returns delay to whichever occurs next.
     * Returns null if [anchorTimes] is empty or has no valid entries.
     */
    fun getDelayUntilNearestAnchor(now: Long, anchorTimes: Set<String>): Long? {
        if (anchorTimes.isEmpty()) return null
        val delays = anchorTimes.mapNotNull { timeStr ->
            parseTime(timeStr)?.let { (h, m) ->
                getDelayUntilNextAnchor(now, h, m)
            }
        }
        return delays.minOrNull()
    }

    /**
     * Formats an hour and minute into a readable 24-hour string (e.g. "08:00").
     */
    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }
}
