package foo.barz.wallpaperpicker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.data.PreferencesManager
import foo.barz.wallpaperpicker.ui.components.WallpaperAdjustmentScreen
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Dedicated standalone activity for wallpaper composition adjustment and parallax tuning.
 * Replaces the fullscreen Dialog implementation to eliminate double system navigation bar artifacts.
 */
class WallpaperAdjustmentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PreferencesManager(this)
        val rawUri: Uri? = intent.data
            ?: intent.getStringExtra(EXTRA_WALLPAPER_URI)?.let { Uri.parse(it) }
            ?: prefs.lastWallpaperUri

        if (rawUri == null) {
            Toast.makeText(this, getString(R.string.dash_empty_title), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri: Uri = rawUri
        val initialTitle = intent.getStringExtra(EXTRA_WALLPAPER_TITLE) ?: prefs.lastWallpaperTitle
        val sourceTypeStr = intent.getStringExtra(EXTRA_WALLPAPER_SOURCE_TYPE)
        val initialSourceType = sourceTypeStr?.let {
            runCatching { WallpaperSourceType.valueOf(it) }.getOrNull()
        } ?: prefs.lastWallpaperSourceType ?: WallpaperSourceType.LOCAL_FOLDER
        val historyId = intent.getLongExtra(EXTRA_HISTORY_ID, 0L)

        val historyDb = WallpaperHistoryDatabase(this)
        val item = (if (historyId > 0) historyDb.getItemById(historyId) else null)
            ?: historyDb.getItemByUri(uri.toString())
            ?: WallpaperHistoryItem(
                id = historyId,
                sourceUri = uri.toString(),
                title = initialTitle,
                sourceType = initialSourceType,
                appliedTimestamp = prefs.lastChangedTimestamp
            )

        setContent {
            WallpaperPickerTheme {
                WallpaperAdjustmentScreen(
                    item = item,
                    globalScrollMode = prefs.scrollMode,
                    onDismiss = { finish() },
                    onSave = { customScrollMode, cropFocusX, cropFocusY, flipHorizontal, applyImmediately ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val existingItem = historyDb.getItemByUri(uri.toString())
                                val targetId = existingItem?.id ?: if (historyId > 0) historyId else {
                                    historyDb.recordAppliedWallpaper(
                                        sourceUri = uri,
                                        title = initialTitle,
                                        sourceType = initialSourceType,
                                        appliedTimestamp = System.currentTimeMillis()
                                    )
                                }
                                historyDb.updateCustomPreferences(
                                    id = targetId,
                                    customScrollMode = customScrollMode,
                                    cropFocusX = cropFocusX,
                                    cropFocusY = cropFocusY,
                                    flipHorizontal = flipHorizontal
                                )

                                if (applyImmediately) {
                                    val processor = WallpaperProcessor(applicationContext)
                                    val applier = WallpaperApplier(applicationContext)
                                    val processResult = processor.processForTarget(
                                        openStream = {
                                            if (uri.scheme == "file") {
                                                FileInputStream(File(uri.path!!))
                                            } else {
                                                contentResolver.openInputStream(uri)
                                                    ?: throw FileNotFoundException("Cannot open image stream: $uri")
                                            }
                                        },
                                        target = prefs.target,
                                        scrollMode = customScrollMode ?: prefs.scrollMode,
                                        cropMode = prefs.cropMode,
                                        cropFocusX = cropFocusX ?: 0.5f,
                                        cropFocusY = cropFocusY ?: 0.5f,
                                        flipHorizontal = flipHorizontal,
                                        lockScreenStrategy = prefs.lockScreenStrategy
                                    )
                                    if (processResult.isSuccess) {
                                        applier.apply(processResult.getOrThrow())
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@WallpaperAdjustmentActivity,
                                    getString(R.string.lightbox_composition_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                                setResult(RESULT_OK)
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_WALLPAPER_URI = "extra_wallpaper_uri"
        const val EXTRA_WALLPAPER_TITLE = "extra_wallpaper_title"
        const val EXTRA_WALLPAPER_SOURCE_TYPE = "extra_wallpaper_source_type"
        const val EXTRA_HISTORY_ID = "extra_history_id"

        fun createIntent(
            context: Context,
            uri: Uri? = null,
            title: String? = null,
            sourceType: WallpaperSourceType? = null,
            historyId: Long = 0L
        ): Intent {
            return Intent(context, WallpaperAdjustmentActivity::class.java).apply {
                if (uri != null) putExtra(EXTRA_WALLPAPER_URI, uri.toString())
                if (title != null) putExtra(EXTRA_WALLPAPER_TITLE, title)
                if (sourceType != null) putExtra(EXTRA_WALLPAPER_SOURCE_TYPE, sourceType.name)
                if (historyId > 0) putExtra(EXTRA_HISTORY_ID, historyId)
            }
        }
    }
}
