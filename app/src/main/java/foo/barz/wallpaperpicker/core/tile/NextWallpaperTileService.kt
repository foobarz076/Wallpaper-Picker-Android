package foo.barz.wallpaperpicker.core.tile

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.core.worker.WallpaperChangeExecutor
import foo.barz.wallpaperpicker.core.worker.WallpaperExecutionResult
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Quick Settings Tile (Android 7.0+ / API 24+) allowing users to immediately trigger
 * the next wallpaper rotation from the system quick settings panel.
 */
@RequiresApi(Build.VERSION_CODES.N)
class NextWallpaperTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val sourcesDb = WallpaperSourcesDatabase(this)
        val enabledSources = sourcesDb.getEnabledSources()
        if (enabledSources.isEmpty()) {
            Toast.makeText(this, "未启用任何图源，请先在应用内添加或启用图源", Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        if (!isSwitching.compareAndSet(false, true)) {
            Toast.makeText(this, "正在更换壁纸中，请稍候…", Toast.LENGTH_SHORT).show()
            return
        }

        val tile = qsTile
        if (tile != null) {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "正在更换…"
            }
            tile.updateTile()
        }

        serviceScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    WallpaperChangeExecutor.execute(applicationContext, isManualTrigger = true)
                }

                when (result) {
                    is WallpaperExecutionResult.Success -> {
                        val titleDesc = result.title?.let { "「$it」" } ?: "新壁纸"
                        Toast.makeText(applicationContext, "已更换壁纸: $titleDesc", Toast.LENGTH_SHORT).show()
                    }
                    is WallpaperExecutionResult.Skipped -> {
                        Toast.makeText(applicationContext, result.reason, Toast.LENGTH_SHORT).show()
                    }
                    is WallpaperExecutionResult.Failure -> {
                        val error = result.error.localizedMessage ?: "更换失败"
                        Toast.makeText(applicationContext, "更换失败: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "更换失败: ${e.localizedMessage ?: "未知错误"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isSwitching.set(false)
                updateTileState()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = PreferencesManager(this)

        tile.label = getString(R.string.tile_next_wallpaper_label)
        tile.state = Tile.STATE_INACTIVE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val title = prefs.lastWallpaperTitle
            tile.subtitle = if (!title.isNullOrBlank()) title else "点此立即更换"
        }
        tile.updateTile()
    }

    companion object {
        private val isSwitching = AtomicBoolean(false)

        /**
         * Requests the system to refresh this tile's appearance and subtitle.
         */
        fun requestUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching {
                    requestListeningState(
                        context,
                        ComponentName(context, NextWallpaperTileService::class.java)
                    )
                }
            }
        }
    }
}
