package foo.barz.wallpaperpicker.core.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager for exact-time wallpaper updates.
 * Holds a partial wake lock during asynchronous processing and reschedules the next alarm upon completion.
 */
class WallpaperAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != WallpaperAlarmScheduler.ACTION_ALARM_TRIGGER) return

        val prefs = PreferencesManager(context)
        if (!prefs.isScheduled || !prefs.exactTimerEnabled) {
            WallpaperAlarmScheduler.cancel(context)
            return
        }

        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "wallpaperpicker:alarm_receiver"
        )?.apply {
            acquire(30_000L) // Safety timeout
        }

        val ruleId = intent.getStringExtra(WallpaperAlarmScheduler.EXTRA_RULE_ID)
        foo.barz.wallpaperpicker.core.util.AppLog.i("WallpaperAlarmReceiver", "Exact alarm triggered: ruleId=$ruleId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                WallpaperChangeExecutor.execute(
                    context = context,
                    isManualTrigger = false,
                    ruleId = ruleId,
                    eventContext = TriggerEventContext.EXACT_ALARM
                )
            } finally {
                // Ensure the next alarm is registered
                WallpaperAlarmScheduler.scheduleNext(context)
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                pendingResult.finish()
            }
        }
    }
}
