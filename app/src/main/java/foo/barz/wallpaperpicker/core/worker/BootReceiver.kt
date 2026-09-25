package foo.barz.wallpaperpicker.core.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * Restores wallpaper scheduling when the device reboots or the application is updated.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferencesManager(context)
            if (prefs.isScheduled) {
                if (prefs.exactTimerEnabled) {
                    WallpaperAlarmScheduler.schedule(context)
                } else {
                    WallpaperWorker.schedule(context, prefs.intervalMinutes)
                }
            }
            ScreenOffWatcherService.syncWithPreferences(context)
        }
    }
}
