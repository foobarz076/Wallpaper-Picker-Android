package foo.barz.wallpaperpicker.core.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * High-precision scheduler backed by Android AlarmManager.
 * Bypasses WorkManager's 15-minute minimum clamp and Doze-window jitter for exact wall-clock
 * and short-interval wallpaper rotations.
 */
object WallpaperAlarmScheduler {

    const val ACTION_ALARM_TRIGGER = "foo.barz.wallpaperpicker.ACTION_ALARM_TRIGGER"
    const val EXTRA_RULE_ID = "extra_rule_id"
    private const val REQUEST_CODE = 2001

    /**
     * Schedules the next exact alarm based on the user's interval and daily anchor settings.
     */
    fun schedule(context: Context) {
        val prefs = PreferencesManager(context)
        if (!prefs.isScheduled) {
            cancel(context)
            return
        }

        if (!prefs.exactTimerEnabled) {
            cancel(context)
            WallpaperWorker.schedule(context, prefs.intervalMinutes)
            return
        }

        // Cancel WorkManager periodic work to avoid duplicate executions
        WallpaperWorker.cancel(context)

        val now = System.currentTimeMillis()

        if (prefs.ruleEngineEnabled) {
            val nextSchedule = ScheduleRuleEngine.computeNextTriggerMillis(context, now)
            if (nextSchedule != null) {
                val (triggerAtMillis, rule) = nextSchedule
                setAlarm(context, triggerAtMillis, rule.id)
            } else {
                cancel(context)
            }
            return
        }

        val intervalMs = prefs.intervalMinutes * 60 * 1000L

        val intervalTrigger = if (prefs.intervalScheduleEnabled) now + intervalMs else null
        val anchorDelay = if (prefs.dailyAnchorEnabled) {
            CompositeTriggerHelper.getDelayUntilNearestAnchor(now, prefs.dailyAnchorTimes)
        } else null
        val anchorTrigger = anchorDelay?.let { now + it }

        val triggerAtMillis = when {
            intervalTrigger != null && anchorTrigger != null -> minOf(intervalTrigger, anchorTrigger)
            intervalTrigger != null -> intervalTrigger
            anchorTrigger != null -> anchorTrigger
            else -> {
                // If neither interval nor daily anchor is enabled, cancel any pending alarm
                cancel(context)
                return
            }
        }

        setAlarm(context, triggerAtMillis, null)
    }

    /**
     * Reschedules the next alarm after an execution completes.
     */
    fun scheduleNext(context: Context) {
        schedule(context)
    }

    /**
     * Cancels any pending AlarmManager wakeups.
     */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = getPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun setAlarm(context: Context, triggerAtMillis: Long, ruleId: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = getPendingIntent(context, ruleId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun getPendingIntent(context: Context, ruleId: String? = null): PendingIntent {
        val intent = Intent(context, WallpaperAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            if (!ruleId.isNullOrBlank()) {
                putExtra(EXTRA_RULE_ID, ruleId)
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
