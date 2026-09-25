package foo.barz.wallpaperpicker.core.model

import java.util.UUID

/**
 * Trigger type for a schedule rule in the Schedule Rule Engine (Phase 4.2).
 */
enum class ScheduleRuleTriggerType(val displayName: String) {
    DAILY_TIME("每日定点打卡"),
    TIME_WINDOW("时段周期轮播"),
    SCREEN_OFF("锁屏熄屏触发")
}

/**
 * Source binding strategy for a schedule rule.
 */
enum class ScheduleRuleSourceBinding(val displayName: String) {
    ACTIVE_DEFAULT("跟随全局激活源"),
    SPECIFIC_SOURCE("指定专属图源"),
    FAVORITES("我的收藏")
}

/**
 * An independent schedule rule representing an automated wallpaper change job.
 * Allows binding specific wall-clock times or windows to dedicated wallpaper sources.
 */
data class ScheduleRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isEnabled: Boolean = true,
    val triggerType: ScheduleRuleTriggerType = ScheduleRuleTriggerType.DAILY_TIME,
    val targetTime: String = "08:00", // "HH:mm" for DAILY_TIME
    val windowStartTime: String = "09:00", // "HH:mm" for TIME_WINDOW
    val windowEndTime: String = "18:00", // "HH:mm" for TIME_WINDOW
    val intervalMinutes: Long = 120L, // for TIME_WINDOW
    val screenOffDelaySeconds: Int = 3, // for SCREEN_OFF
    val sourceBinding: ScheduleRuleSourceBinding = ScheduleRuleSourceBinding.ACTIVE_DEFAULT,
    val specificSourceId: String? = null,
    val specificSourceTitle: String? = null,
    val targetScreen: WallpaperTarget = WallpaperTarget.BOTH,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)
