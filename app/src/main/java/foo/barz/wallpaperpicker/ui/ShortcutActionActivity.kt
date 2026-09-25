package foo.barz.wallpaperpicker.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import foo.barz.wallpaperpicker.core.action.WallpaperActionManager
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Transparent action activity that handles app shortcut actions without displaying a full UI.
 * Automatically performs the requested wallpaper change or gallery view and finishes cleanly.
 */
class ShortcutActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            ShortcutHelper.ACTION_NEXT_WALLPAPER -> {
                handleNextWallpaper()
            }
            ShortcutHelper.ACTION_VIEW_CURRENT_WALLPAPER -> {
                handleViewCurrentWallpaper()
            }
            else -> {
                finish()
            }
        }
    }

    private fun handleNextWallpaper() {
        if (!isSwitching.compareAndSet(false, true)) {
            Toast.makeText(applicationContext, "正在更换壁纸中，请稍候…", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Run in application-level CoroutineScope so execution isn't prematurely killed
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val prefs = PreferencesManager(applicationContext)
                val source = WallpaperSourceFactory.createActiveSource(
                    context = applicationContext,
                    prefs = prefs,
                    bypassNetworkConstraints = true
                )

                val wallpaperResult = source.getNextWallpaper()
                if (wallpaperResult.isFailure) {
                    val errorMsg = wallpaperResult.exceptionOrNull()?.localizedMessage ?: "获取壁纸失败"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "更换失败: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val wallpaperData = wallpaperResult.getOrThrow()
                val processor = WallpaperProcessor(applicationContext)
                val bitmapResult = processor.process(
                    openStream = wallpaperData.openStream,
                    scrollMode = prefs.scrollMode,
                    cropMode = prefs.cropMode
                )
                if (bitmapResult.isFailure) {
                    val errorMsg = bitmapResult.exceptionOrNull()?.localizedMessage ?: "图片解码失败"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "更换失败: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val bitmap = bitmapResult.getOrThrow()
                val applier = WallpaperApplier(applicationContext)
                val applyResult = applier.apply(bitmap, prefs.target)
                if (applyResult.isFailure) {
                    val errorMsg = applyResult.exceptionOrNull()?.localizedMessage ?: "设置壁纸失败"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "更换失败: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val now = System.currentTimeMillis()
                val concreteSourceType = wallpaperData.sourceType ?: prefs.sourceType
                val concreteSourceTitle = wallpaperData.sourceTitle

                prefs.lastChangedTimestamp = now
                prefs.lastExecutionTimestamp = now
                prefs.lastWallpaperTitle = wallpaperData.title
                prefs.lastWallpaperUri = wallpaperData.sourceUri
                prefs.lastWallpaperSourceType = concreteSourceType
                prefs.lastWallpaperSourceTitle = concreteSourceTitle
                prefs.lastExecutionStatus = "成功"
                prefs.lastErrorMessage = null

                val wallpaperKey = wallpaperData.sourceUri?.toString() ?: wallpaperData.title
                if (wallpaperKey != null) {
                    prefs.recordRecentWallpaperKey(wallpaperKey)
                }

                wallpaperData.sourceUri?.let { uri ->
                    WallpaperHistoryDatabase(applicationContext).recordAppliedWallpaper(
                        sourceUri = uri,
                        title = wallpaperData.title,
                        sourceType = concreteSourceType,
                        appliedTimestamp = now,
                        sourceTitle = concreteSourceTitle,
                        remoteUrl = wallpaperData.remoteUrl
                    )
                }

                withContext(Dispatchers.Main) {
                    val titleDesc = wallpaperData.title?.let { "「$it」" } ?: "新壁纸"
                    val sourceBadge = concreteSourceTitle?.let { " · $it" } ?: ""
                    Toast.makeText(applicationContext, "已更换壁纸: $titleDesc$sourceBadge", Toast.LENGTH_SHORT).show()
                }
                foo.barz.wallpaperpicker.core.widget.CurrentWallpaperWidgetProvider.updateAllWidgets(applicationContext)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "更换失败: ${e.localizedMessage ?: e.javaClass.simpleName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isSwitching.set(false)
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun handleViewCurrentWallpaper() {
        val prefs = PreferencesManager(applicationContext)
        val uri = prefs.lastWallpaperUri
        if (uri == null) {
            Toast.makeText(applicationContext, "尚未更换过壁纸", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            val result = WallpaperActionManager.openInGallery(this@ShortcutActionActivity, uri)
            if (result.isFailure) {
                Toast.makeText(
                    applicationContext,
                    "无法打开图库: ${result.exceptionOrNull()?.localizedMessage ?: "未知错误"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }
    }

    companion object {
        private val isSwitching = AtomicBoolean(false)
    }
}
