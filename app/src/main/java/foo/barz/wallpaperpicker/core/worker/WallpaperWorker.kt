package foo.barz.wallpaperpicker.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for scheduled periodic wallpaper changes.
 * Delegates actual execution to WallpaperChangeExecutor.
 */
class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val result = WallpaperChangeExecutor.execute(applicationContext, isManualTrigger = false)
        return when (result) {
            is WallpaperExecutionResult.Success -> Result.success()
            is WallpaperExecutionResult.Skipped -> Result.success()
            is WallpaperExecutionResult.Failure -> {
                if (result.isTransient && runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "periodic_wallpaper_changer"
        private const val MAX_RETRIES = 2

        fun schedule(context: Context, intervalMinutes: Long) {
            val prefs = PreferencesManager(context)

            // If scheduling is off or neither interval nor daily anchor is enabled, cancel periodic work
            if (!prefs.isScheduled || (!prefs.intervalScheduleEnabled && !prefs.dailyAnchorEnabled)) {
                cancel(context)
                return
            }

            // Keep battery constraint to protect critical battery levels (< 15%),
            // but remove hard network constraints so worker can fallback to LRU cache.
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // If interval is disabled but daily anchor is enabled, repeat daily (1440 minutes)
            val effectiveInterval = if (prefs.intervalScheduleEnabled) intervalMinutes else 1440L

            val builder = PeriodicWorkRequestBuilder<WallpaperWorker>(
                effectiveInterval, TimeUnit.MINUTES
            ).setConstraints(constraints)

            // If daily anchor is enabled, align WorkManager's initial delay with the nearest anchor time
            if (prefs.dailyAnchorEnabled) {
                val now = System.currentTimeMillis()
                val anchorDelay = CompositeTriggerHelper.getDelayUntilNearestAnchor(
                    now,
                    prefs.dailyAnchorTimes
                )
                if (anchorDelay != null) {
                    builder.setInitialDelay(anchorDelay, TimeUnit.MILLISECONDS)
                }
            }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                builder.build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
