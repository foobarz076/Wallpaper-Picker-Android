package foo.barz.wallpaperpicker.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.source.LocalFolderSource
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.util.concurrent.TimeUnit

class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        val folderUri = prefs.folderUri ?: return Result.failure()

        val source = LocalFolderSource(applicationContext, folderUri)
        val processor = WallpaperProcessor(applicationContext)
        val applier = WallpaperApplier(applicationContext)

        val sourceResult = source.getNextWallpaper()
        if (sourceResult.isFailure) {
            return Result.retry()
        }

        val wallpaperData = sourceResult.getOrThrow()
        val processResult = processor.process(wallpaperData.openStream, prefs.scrollMode)
        if (processResult.isFailure) {
            return Result.retry()
        }

        val bitmap = processResult.getOrThrow()
        val applyResult = applier.apply(bitmap, prefs.target)
        if (applyResult.isFailure) {
            return Result.retry()
        }

        prefs.lastChangedTimestamp = System.currentTimeMillis()
        prefs.lastWallpaperTitle = wallpaperData.title

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "periodic_wallpaper_changer"

        fun schedule(context: Context, intervalMinutes: Long) {
            val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
                intervalMinutes, TimeUnit.MINUTES
            ).build()

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
