package foo.barz.wallpaperpicker.core.worker

import android.content.Context
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
        val applyResult = applier.apply(bitmap, prefs.target)
        if (applyResult.isFailure) {
            return handleFailure(
                prefs,
                applyResult.exceptionOrNull() ?: Exception("Failed to apply wallpaper")
            )
        }

        prefs.lastChangedTimestamp = System.currentTimeMillis()
        prefs.lastExecutionTimestamp = System.currentTimeMillis()
        prefs.lastWallpaperTitle = wallpaperData.title
        prefs.lastWallpaperUri = wallpaperData.sourceUri
        prefs.lastExecutionStatus = "成功"
        prefs.lastErrorMessage = null

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
