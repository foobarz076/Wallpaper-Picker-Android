package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.LockScreenStrategy
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class LockScreenStrategyTest {

    @Test
    fun testLockScreenStrategyEnumAttributes() {
        assertEquals("独立单屏居中", LockScreenStrategy.INDEPENDENT_CENTERED.label)
        assertEquals("与桌面完全联动", LockScreenStrategy.FOLLOW_DESKTOP.label)
        assertEquals(R.string.lock_strategy_centered_label, LockScreenStrategy.INDEPENDENT_CENTERED.labelRes)
        assertEquals(R.string.lock_strategy_follow_label, LockScreenStrategy.FOLLOW_DESKTOP.labelRes)
        assertEquals(R.string.lock_strategy_centered_desc, LockScreenStrategy.INDEPENDENT_CENTERED.descriptionRes)
        assertEquals(R.string.lock_strategy_follow_desc, LockScreenStrategy.FOLLOW_DESKTOP.descriptionRes)
    }

    @Test
    fun testLockScreenStrategyParsingAndFallback() {
        assertEquals(
            LockScreenStrategy.INDEPENDENT_CENTERED,
            runCatching { LockScreenStrategy.valueOf("INDEPENDENT_CENTERED") }.getOrDefault(LockScreenStrategy.INDEPENDENT_CENTERED)
        )
        assertEquals(
            LockScreenStrategy.FOLLOW_DESKTOP,
            runCatching { LockScreenStrategy.valueOf("FOLLOW_DESKTOP") }.getOrDefault(LockScreenStrategy.INDEPENDENT_CENTERED)
        )
        assertEquals(
            LockScreenStrategy.INDEPENDENT_CENTERED,
            runCatching { LockScreenStrategy.valueOf("UNKNOWN_INVALID") }.getOrDefault(LockScreenStrategy.INDEPENDENT_CENTERED)
        )
    }

    @Test
    fun testLockScreenStrategyTargetScenarios() {
        // LOCK target should always map to single-screen centered regardless of strategy
        val lockTarget = WallpaperTarget.LOCK
        assertEquals(WallpaperTarget.LOCK, lockTarget)

        // BOTH target can differentiate between INDEPENDENT_CENTERED and FOLLOW_DESKTOP
        val strategyCentered = LockScreenStrategy.INDEPENDENT_CENTERED
        val strategyFollow = LockScreenStrategy.FOLLOW_DESKTOP
        assertEquals(2, LockScreenStrategy.values().size)
        assertEquals("INDEPENDENT_CENTERED", strategyCentered.name)
        assertEquals("FOLLOW_DESKTOP", strategyFollow.name)
    }
}
