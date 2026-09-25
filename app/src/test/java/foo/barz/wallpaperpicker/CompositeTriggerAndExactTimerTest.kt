package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import foo.barz.wallpaperpicker.ui.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests verifying Phase 4.5:
 * - Composite Triggers (Quiet Hours overnight and daytime, Cooldown Suppression, Daily Anchor)
 * - Exact Timer configurations in MainUiState
 */
class CompositeTriggerAndExactTimerTest {

    @Test
    fun testQuietHoursOvernightWindow() {
        val startHour = 23
        val startMinute = 0
        val endHour = 7
        val endMinute = 0

        fun createTimestamp(hour: Int, minute: Int): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        // 23:00 -> within quiet hours
        assertTrue(CompositeTriggerHelper.isInQuietHours(createTimestamp(23, 0), startHour, startMinute, endHour, endMinute))
        // 23:45 -> within quiet hours
        assertTrue(CompositeTriggerHelper.isInQuietHours(createTimestamp(23, 45), startHour, startMinute, endHour, endMinute))
        // 03:30 (Midnight past) -> within quiet hours
        assertTrue(CompositeTriggerHelper.isInQuietHours(createTimestamp(3, 30), startHour, startMinute, endHour, endMinute))
        // 06:59 -> within quiet hours
        assertTrue(CompositeTriggerHelper.isInQuietHours(createTimestamp(6, 59), startHour, startMinute, endHour, endMinute))
        // 07:00 (Boundary end) -> not within quiet hours
        assertFalse(CompositeTriggerHelper.isInQuietHours(createTimestamp(7, 0), startHour, startMinute, endHour, endMinute))
        // 12:00 (Noon) -> not within quiet hours
        assertFalse(CompositeTriggerHelper.isInQuietHours(createTimestamp(12, 0), startHour, startMinute, endHour, endMinute))
        // 22:59 -> not within quiet hours
        assertFalse(CompositeTriggerHelper.isInQuietHours(createTimestamp(22, 59), startHour, startMinute, endHour, endMinute))
    }

    @Test
    fun testQuietHoursDaytimeWindow() {
        val startHour = 13
        val startMinute = 30
        val endHour = 15
        val endMinute = 0

        fun createTimestamp(hour: Int, minute: Int): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        // 13:29 -> not within quiet hours
        assertFalse(CompositeTriggerHelper.isInQuietHours(createTimestamp(13, 29), startHour, startMinute, endHour, endMinute))
        // 13:30 -> within quiet hours
        assertTrue(CompositeTriggerHelper.isInQuietHours(createTimestamp(13, 30), startHour, startMinute, endHour, endMinute))
        // 14:15 -> within quiet hours
        assertTrue(CompositeTriggerHelper.isInQuietHours(createTimestamp(14, 15), startHour, startMinute, endHour, endMinute))
        // 15:00 -> not within quiet hours (end boundary exclusive)
        assertFalse(CompositeTriggerHelper.isInQuietHours(createTimestamp(15, 0), startHour, startMinute, endHour, endMinute))
        // 18:00 -> not within quiet hours
        assertFalse(CompositeTriggerHelper.isInQuietHours(createTimestamp(18, 0), startHour, startMinute, endHour, endMinute))
    }

    @Test
    fun testCooldownSuppression() {
        val now = 1000_000_000L
        val cooldownMinutes = 10L // 600,000 ms

        // Case 1: First run (lastChangedTimestamp = 0) -> never suppressed
        assertFalse(CompositeTriggerHelper.isCooldownSuppressed(now, 0L, cooldownMinutes))

        // Case 2: Cooldown disabled (cooldownMinutes = 0) -> never suppressed
        assertFalse(CompositeTriggerHelper.isCooldownSuppressed(now, now - 60_000L, 0L))

        // Case 3: Changed 3 minutes ago -> suppressed (within 10-minute cooldown)
        val changed3MinAgo = now - 3 * 60 * 1000L
        assertTrue(CompositeTriggerHelper.isCooldownSuppressed(now, changed3MinAgo, cooldownMinutes))

        // Case 4: Changed exactly 10 minutes ago -> not suppressed
        val changed10MinAgo = now - 10 * 60 * 1000L
        assertFalse(CompositeTriggerHelper.isCooldownSuppressed(now, changed10MinAgo, cooldownMinutes))

        // Case 5: Changed 15 minutes ago -> not suppressed
        val changed15MinAgo = now - 15 * 60 * 1000L
        assertFalse(CompositeTriggerHelper.isCooldownSuppressed(now, changed15MinAgo, cooldownMinutes))
    }

    @Test
    fun testDelayUntilNextAnchor() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        // Target: 08:00 (30 minutes in the future today)
        val delayToday = CompositeTriggerHelper.getDelayUntilNextAnchor(now, 8, 0)
        assertEquals(30 * 60 * 1000L, delayToday)

        // Target: 07:00 (30 minutes in the past today -> rolls over to tomorrow at 07:00)
        val delayTomorrow = CompositeTriggerHelper.getDelayUntilNextAnchor(now, 7, 0)
        assertEquals(23 * 60 * 60 * 1000L + 30 * 60 * 1000L, delayTomorrow)
    }

    @Test
    fun testFormatTime() {
        assertEquals("08:05", CompositeTriggerHelper.formatTime(8, 5))
        assertEquals("23:00", CompositeTriggerHelper.formatTime(23, 0))
        assertEquals("00:00", CompositeTriggerHelper.formatTime(0, 0))
    }

    @Test
    fun testParseTime() {
        assertEquals(Pair(8, 0), CompositeTriggerHelper.parseTime("08:00"))
        assertEquals(Pair(12, 30), CompositeTriggerHelper.parseTime("12:30"))
        assertEquals(Pair(23, 59), CompositeTriggerHelper.parseTime("23:59"))
        assertEquals(Pair(0, 0), CompositeTriggerHelper.parseTime("00:00"))

        // Invalid inputs
        assertEquals(null, CompositeTriggerHelper.parseTime(""))
        assertEquals(null, CompositeTriggerHelper.parseTime("invalid"))
        assertEquals(null, CompositeTriggerHelper.parseTime("24:00"))
        assertEquals(null, CompositeTriggerHelper.parseTime("12:60"))
        assertEquals(null, CompositeTriggerHelper.parseTime("8:0"))
    }

    @Test
    fun testGetDelayUntilNearestAnchor() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        // Anchors at 08:00 and 12:00. At 07:30, 08:00 is closer (30 min vs 4.5 hours)
        val anchors = setOf("08:00", "12:00")
        val nearestDelay = CompositeTriggerHelper.getDelayUntilNearestAnchor(now, anchors)
        assertEquals(30 * 60 * 1000L, nearestDelay)

        // Anchors when both have passed today (e.g. 06:00 and 07:00 at 07:30)
        // 06:00 tomorrow is in 22.5 hours; 07:00 tomorrow is in 23.5 hours
        val pastAnchors = setOf("06:00", "07:00")
        val nearestPastDelay = CompositeTriggerHelper.getDelayUntilNearestAnchor(now, pastAnchors)
        assertEquals(22 * 60 * 60 * 1000L + 30 * 60 * 1000L, nearestPastDelay)

        // Empty set
        assertEquals(null, CompositeTriggerHelper.getDelayUntilNearestAnchor(now, emptySet()))
    }

    @Test
    fun testComposableTriggerSelection() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis
        val intervalMs = 30 * 60 * 1000L // 30 minutes -> 08:15

        // Case A: Next anchor is 08:00 (15 min away).
        // Since anchor (08:00) is sooner than interval (08:15), anchor fires first.
        val anchorDelay1 = CompositeTriggerHelper.getDelayUntilNearestAnchor(now, setOf("08:00", "12:00"))!!
        val nextTrigger1 = minOf(now + intervalMs, now + anchorDelay1)
        assertEquals(now + 15 * 60 * 1000L, nextTrigger1)

        // Case B: Current time is 08:00. Next anchor is 12:00 (4 hours away).
        // Next interval is 08:30 (30 min away). Interval fires first.
        val cal2 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now2 = cal2.timeInMillis
        val anchorDelay2 = CompositeTriggerHelper.getDelayUntilNearestAnchor(now2, setOf("08:00", "12:00"))!!
        val nextTrigger2 = minOf(now2 + intervalMs, now2 + anchorDelay2)
        assertEquals(now2 + 30 * 60 * 1000L, nextTrigger2)
    }

    @Test
    fun testMainUiStateExactTimerAndCompositeTriggers() {
        val defaultState = MainUiState()
        assertTrue(defaultState.intervalScheduleEnabled)
        assertFalse(defaultState.exactTimerEnabled)
        assertFalse(defaultState.dailyAnchorEnabled)
        assertEquals(8, defaultState.dailyAnchorHour)
        assertEquals(0, defaultState.dailyAnchorMinute)
        assertEquals(setOf("08:00"), defaultState.dailyAnchorTimes)
        assertFalse(defaultState.screenOffTriggerEnabled)
        assertEquals(3, defaultState.screenOffDelaySeconds)
        assertFalse(defaultState.quietHoursEnabled)
        assertEquals(23, defaultState.quietHoursStartHour)
        assertEquals(0, defaultState.quietHoursStartMinute)
        assertEquals(7, defaultState.quietHoursEndHour)
        assertEquals(0, defaultState.quietHoursEndMinute)
        assertTrue(defaultState.cooldownSuppressionEnabled)
        assertEquals(10L, defaultState.cooldownMinutes)

        // Test updating state
        val updated = defaultState.copy(
            intervalScheduleEnabled = false,
            exactTimerEnabled = true,
            dailyAnchorEnabled = true,
            dailyAnchorHour = 9,
            dailyAnchorMinute = 30,
            dailyAnchorTimes = setOf("08:00", "12:00", "18:00"),
            screenOffTriggerEnabled = true,
            screenOffDelaySeconds = 5,
            quietHoursEnabled = true,
            quietHoursStartHour = 22,
            quietHoursStartMinute = 30,
            quietHoursEndHour = 8,
            quietHoursEndMinute = 0,
            cooldownSuppressionEnabled = false,
            cooldownMinutes = 15L
        )

        assertFalse(updated.intervalScheduleEnabled)
        assertTrue(updated.exactTimerEnabled)
        assertTrue(updated.dailyAnchorEnabled)
        assertEquals(9, updated.dailyAnchorHour)
        assertEquals(30, updated.dailyAnchorMinute)
        assertEquals(setOf("08:00", "12:00", "18:00"), updated.dailyAnchorTimes)
        assertTrue(updated.screenOffTriggerEnabled)
        assertEquals(5, updated.screenOffDelaySeconds)
        assertTrue(updated.quietHoursEnabled)
        assertEquals(22, updated.quietHoursStartHour)
        assertEquals(30, updated.quietHoursStartMinute)
        assertEquals(8, updated.quietHoursEndHour)
        assertEquals(0, updated.quietHoursEndMinute)
        assertFalse(updated.cooldownSuppressionEnabled)
        assertEquals(15L, updated.cooldownMinutes)
    }
}
