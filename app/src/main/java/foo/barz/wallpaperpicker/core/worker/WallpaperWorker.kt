package foo.barz.wallpaperpicker.core.worker

import android.content.Context
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit

class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)

        // Check if the user opted to defer wallpaper updates while the device is in active interactive use
        if (prefs.deferDuringInteraction) {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isInteractive == true) {
                return if (runAttemptCount < MAX_INTERACTIVE_RETRIES) {
                    prefs.lastExecutionStatus = "设备正在使用中，已推迟更换 (待重试)"
                    prefs.lastExecutionTimestamp = System.currentTimeMillis()
                    Result.retry()
                } else {
                    prefs.lastExecutionStatus = "设备正在使用中，已顺延至下次调度"
                    prefs.lastExecutionTimestamp = System.currentTimeMillis()
                    Result.success()
                }
            }
        }

        val source = runCatching {
            WallpaperSourceFactory.createActiveSource(applicationContext, prefs)
        }.getOrElse { error ->
            return handleFailure(prefs, error)
        }

        val processor = WallpaperProcessor(applicationContext)
        val applier = WallpaperApplier(applicationContext)

        val sourceResult = source.getNextWallpaper()
        if (sourceResult.isFailure) {
            return handleFailure(
                prefs,
                sourceResult.exceptionOrNull() ?: Exception("Failed to fetch wallpaper")
            )
        }

        val wallpaperData = sourceResult.getOrThrow()

        // Skip re-processing and re-applying if the chosen wallpaper is identical to current active wallpaper
        val isSameWallpaper = wallpaperData.sourceUri != null &&
                wallpaperData.sourceUri == prefs.lastWallpaperUri &&
                wallpaperData.title == prefs.lastWallpaperTitle

        if (isSameWallpaper) {
            prefs.lastExecutionStatus = "壁纸无变化，已跳过重复应用"
            prefs.lastErrorMessage = null
            prefs.lastExecutionTimestamp = System.currentTimeMillis()
            return Result.success()
        }

        val processResult = processor.process(wallpaperData.openStream, prefs.scrollMode, prefs.cropMode)
        if (processResult.isFailure) {
            return handleFailure(
                prefs,
                processResult.exceptionOrNull() ?: Exception("Failed to process wallpaper")
            )
        }

        val bitmap = processResult.getOrThrow()

        // Re-check interactivity before applying to avoid frame drops if the user just woke the screen
        if (prefs.deferDuringInteraction) {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isInteractive == true) {
                prefs.lastExecutionStatus = "设备正在使用中，已推迟更换 (待重试)"
                prefs.lastExecutionTimestamp = System.currentTimeMillis()
                return Result.retry()
            }
        }

        val applyResult = applier.apply(bitmap, prefs.target)
        if (applyResult.isFailure) {
            return handleFailure(
                prefs,
                applyResult.exceptionOrNull() ?: Exception("Failed to apply wallpaper")
            )
        }

        val concreteSourceType = wallpaperData.sourceType ?: prefs.sourceType
        val concreteSourceTitle = wallpaperData.sourceTitle

        prefs.lastChangedTimestamp = System.currentTimeMillis()
        prefs.lastExecutionTimestamp = System.currentTimeMillis()
        prefs.lastWallpaperTitle = wallpaperData.title
        prefs.lastWallpaperUri = wallpaperData.sourceUri
        prefs.lastWallpaperSourceType = concreteSourceType
        prefs.lastWallpaperSourceTitle = concreteSourceTitle
        prefs.lastExecutionStatus = "成功"
        prefs.lastErrorMessage = null

        // Record wallpaper key for Fair Shuffle history
        val wallpaperKey = wallpaperData.sourceUri?.toString() ?: wallpaperData.title
        if (wallpaperKey != null) {
            prefs.recordRecentWallpaperKey(wallpaperKey)
        }

        // Record into persistent history database
        wallpaperData.sourceUri?.let { uri ->
            runCatching {
                foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase(applicationContext).recordAppliedWallpaper(
                    sourceUri = uri,
                    title = wallpaperData.title,
                    sourceType = concreteSourceType,
                    appliedTimestamp = System.currentTimeMillis(),
                    sourceTitle = concreteSourceTitle
                )
            }
        }

        return Result.success()
    }

    private fun handleFailure(prefs: PreferencesManager, error: Throwable): Result {
        val errorMsg = error.localizedMessage ?: error.javaClass.simpleName
        prefs.lastErrorMessage = errorMsg
        prefs.lastExecutionStatus = "失败: $errorMsg"
        prefs.lastExecutionTimestamp = System.currentTimeMillis()

        // Only retry for transient I/O network errors when below MAX_RETRIES threshold
        val isTransient = error is IOException && error !is FileNotFoundException
        return if (isTransient && runAttemptCount < MAX_RETRIES) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "periodic_wallpaper_changer"
        private const val MAX_RETRIES = 2
        private const val MAX_INTERACTIVE_RETRIES = 3

        fun schedule(context: Context, intervalMinutes: Long) {
            // Keep battery constraint to protect critical battery levels (< 15%),
            // but remove hard network constraints so the worker can fire and gracefully fallback to LRU cache.
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
