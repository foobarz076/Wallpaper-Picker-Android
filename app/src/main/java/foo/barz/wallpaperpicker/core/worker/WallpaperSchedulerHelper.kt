package foo.barz.wallpaperpicker.core.worker

import android.content.Context
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * Unified helper for orchestrating wallpaper rotation scheduling across background workers,
 * alarms, and screen-off listeners.
 */
object WallpaperSchedulerHelper {

    /**
     * Re-evaluates and schedules all active triggers according to user preferences.
     * If scheduling is disabled, cancels all running triggers.
     */
    fun refreshScheduling(context: Context) {
        val prefs = PreferencesManager(context)
        if (!prefs.isScheduled) {
            cancelScheduling(context)
            return
        }

        if (prefs.ruleEngineEnabled) {
            WallpaperAlarmScheduler.schedule(context)
            WallpaperWorker.schedule(context, 15L) // Background fallback heartbeat
        } else if (prefs.exactTimerEnabled) {
            WallpaperWorker.cancel(context)
            WallpaperAlarmScheduler.schedule(context)
        } else {
            WallpaperAlarmScheduler.cancel(context)
            WallpaperWorker.schedule(context, prefs.intervalMinutes)
        }
        ScreenOffWatcherService.syncWithPreferences(context)
    }

    /**
     * Completely cancels all scheduled triggers (WorkManager, AlarmManager, and foreground ScreenOffWatcherService).
     */
    fun cancelScheduling(context: Context) {
        WallpaperAlarmScheduler.cancel(context)
        WallpaperWorker.cancel(context)
        ScreenOffWatcherService.stop(context)
    }

    /**
     * Toggles global wallpaper scheduling state.
     * Validates that at least one wallpaper source is enabled before activating.
     *
     * @return Result.success with the new boolean state, or Result.failure with error message.
     */
    fun toggleScheduling(context: Context): Result<Boolean> {
        val prefs = PreferencesManager(context)
        val targetState = !prefs.isScheduled

        if (targetState) {
            val sourcesDb = WallpaperSourcesDatabase(context)
            val enabledSources = sourcesDb.getEnabledSources()
            if (enabledSources.isEmpty()) {
                return Result.failure(IllegalStateException("未启用任何图源，请先在图源管理中添加或启用图源"))
            }
            prefs.isScheduled = true
            refreshScheduling(context)
            return Result.success(true)
        } else {
            prefs.isScheduled = false
            cancelScheduling(context)
            return Result.success(false)
        }
    }
}
