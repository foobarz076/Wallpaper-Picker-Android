package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import foo.barz.wallpaperpicker.core.worker.ScheduleRuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests verifying Phase 4.2 Composite Triggers: Schedule Rule Engine.
 */
class ScheduleRuleEngineTest {

    @Test
    fun testScheduleRuleModelDefaults() {
        val rule = ScheduleRule(name = "Test Morning Rule")
        assertEquals("Test Morning Rule", rule.name)
        assertTrue(rule.isEnabled)
        assertEquals(ScheduleRuleTriggerType.DAILY_TIME, rule.triggerType)
        assertEquals("08:00", rule.targetTime)
        assertEquals("09:00", rule.windowStartTime)
        assertEquals("18:00", rule.windowEndTime)
        assertEquals(120L, rule.intervalMinutes)
        assertEquals(3, rule.screenOffDelaySeconds)
        assertEquals(ScheduleRuleSourceBinding.ACTIVE_DEFAULT, rule.sourceBinding)
        assertNull(rule.specificSourceId)
        assertNull(rule.specificSourceTitle)
        assertEquals(WallpaperTarget.BOTH, rule.targetScreen)
    }

    @Test
    fun testIsWithinTimeWindowSameDay() {
        val start = "09:00" // 540 min
        val end = "18:00"   // 1080 min

        // 08:59 -> false
        assertFalse(ScheduleRuleEngine.isWithinTimeWindow(8 * 60 + 59, start, end))
        // 09:00 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(9 * 60, start, end))
        // 12:30 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(12 * 60 + 30, start, end))
        // 17:59 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(17 * 60 + 59, start, end))
        // 18:00 -> false
        assertFalse(ScheduleRuleEngine.isWithinTimeWindow(18 * 60, start, end))
        // 20:00 -> false
        assertFalse(ScheduleRuleEngine.isWithinTimeWindow(20 * 60, start, end))
    }

    @Test
    fun testIsWithinTimeWindowOvernight() {
        val start = "22:00" // 1320 min
        val end = "06:00"   // 360 min

        // 21:59 -> false
        assertFalse(ScheduleRuleEngine.isWithinTimeWindow(21 * 60 + 59, start, end))
        // 22:00 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(22 * 60, start, end))
        // 23:45 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(23 * 60 + 45, start, end))
        // 03:00 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(3 * 60, start, end))
        // 05:59 -> true
        assertTrue(ScheduleRuleEngine.isWithinTimeWindow(5 * 60 + 59, start, end))
        // 06:00 -> false
        assertFalse(ScheduleRuleEngine.isWithinTimeWindow(6 * 60, start, end))
        // 14:00 -> false
        assertFalse(ScheduleRuleEngine.isWithinTimeWindow(14 * 60, start, end))
    }

    @Test
    fun testComputeNextWindowTriggerOutsideWindow() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis
        val rule = ScheduleRule(
            name = "Work Window",
            triggerType = ScheduleRuleTriggerType.TIME_WINDOW,
            windowStartTime = "09:00",
            windowEndTime = "18:00",
            intervalMinutes = 60L
        )

        // At 07:00, outside window -> next trigger should be when window opens at 09:00 (2 hours away)
        val nextTrigger = ScheduleRuleEngine.computeNextWindowTrigger(now, 0L, rule)
        assertNotNull(nextTrigger)
        assertEquals(now + 2 * 60 * 60 * 1000L, nextTrigger)
    }

    @Test
    fun testComputeNextWindowTriggerInsideWindow() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis
        val lastChanged = now - 30 * 60 * 1000L // 30 minutes ago
        val rule = ScheduleRule(
            name = "Work Window",
            triggerType = ScheduleRuleTriggerType.TIME_WINDOW,
            windowStartTime = "09:00",
            windowEndTime = "18:00",
            intervalMinutes = 60L
        )

        // Last changed at 09:30, interval is 60m -> next trigger should be 10:30 (30 min from now)
        val nextTrigger = ScheduleRuleEngine.computeNextWindowTrigger(now, lastChanged, rule)
        assertNotNull(nextTrigger)
        assertEquals(lastChanged + 60 * 60 * 1000L, nextTrigger)
    }

    @Test
    fun testComputeNextWindowTriggerNearWindowEndRollsOver() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis
        val lastChanged = now // 17:30
        val rule = ScheduleRule(
            name = "Work Window",
            triggerType = ScheduleRuleTriggerType.TIME_WINDOW,
            windowStartTime = "09:00",
            windowEndTime = "18:00",
            intervalMinutes = 60L // 17:30 + 60m = 18:30 (outside window)
        )

        // Since 18:30 is outside 09:00-18:00, next trigger should rollover to tomorrow at 09:00 (15.5 hours away)
        val nextTrigger = ScheduleRuleEngine.computeNextWindowTrigger(now, lastChanged, rule)
        assertNotNull(nextTrigger)
        val expectedDelay = (15 * 60 + 30) * 60 * 1000L
        assertEquals(now + expectedDelay, nextTrigger)
    }

    @Test
    fun testComputeNextWindowTriggerRespectsInterval() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis
        val lastChanged = now - 15 * 60 * 1000L // 15 minutes ago
        val rule = ScheduleRule(
            name = "Work Window",
            triggerType = ScheduleRuleTriggerType.TIME_WINDOW,
            windowStartTime = "09:00",
            windowEndTime = "18:00",
            intervalMinutes = 60L
        )

        // Interval is 60 min, changed 15 min ago -> next trigger is at 11:45 (45 min from now)
        val nextTrigger = ScheduleRuleEngine.computeNextWindowTrigger(now, lastChanged, rule)
        assertNotNull(nextTrigger)
        assertEquals(lastChanged + 60 * 60 * 1000L, nextTrigger)
    }

    @Test
    fun testTriggerTypeDisplayNames() {
        assertEquals("每日定点打卡", ScheduleRuleTriggerType.DAILY_TIME.displayName)
        assertEquals("时段周期轮播", ScheduleRuleTriggerType.TIME_WINDOW.displayName)
        assertEquals("锁屏熄屏触发", ScheduleRuleTriggerType.SCREEN_OFF.displayName)
    }

    @Test
    fun testSourceBindingDisplayNames() {
        assertEquals("跟随全局激活源", ScheduleRuleSourceBinding.ACTIVE_DEFAULT.displayName)
        assertEquals("指定专属图源", ScheduleRuleSourceBinding.SPECIFIC_SOURCE.displayName)
        assertEquals("我的收藏", ScheduleRuleSourceBinding.FAVORITES.displayName)
    }

    @Test
    fun testWallpaperTargetLabels() {
        assertEquals("桌面与锁屏", WallpaperTarget.BOTH.label)
        assertEquals("仅桌面", WallpaperTarget.SYSTEM.label)
        assertEquals("仅锁屏", WallpaperTarget.LOCK.label)
    }
}
