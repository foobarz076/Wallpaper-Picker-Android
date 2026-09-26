package foo.barz.wallpaperpicker.core.tile

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.worker.WallpaperSchedulerHelper
import foo.barz.wallpaperpicker.data.PreferencesManager

/**
 * Quick Settings Tile (Android 7.0+ / API 24+) allowing users to toggle global automatic
 * wallpaper scheduling directly from the notification shade.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ToggleScheduleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val toggleResult = WallpaperSchedulerHelper.toggleScheduling(applicationContext)
        toggleResult.onSuccess { isNowScheduled ->
            if (isNowScheduled) {
                Toast.makeText(this, "壁纸自动轮播已启动", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "壁纸自动轮播已暂停", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { error ->
            Toast.makeText(this, error.localizedMessage ?: "操作失败", Toast.LENGTH_SHORT).show()
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = PreferencesManager(this)
        val isScheduled = prefs.isScheduled

        tile.label = getString(R.string.tile_toggle_schedule_label)

        if (isScheduled) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (prefs.ruleEngineEnabled) {
                    "规则引擎运行中"
                } else {
                    "已开启 (每 ${prefs.intervalMinutes} 分钟)"
                }
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "已暂停"
            }
        }
        tile.updateTile()
    }

    companion object {
        /**
         * Requests the system to refresh this tile's appearance and active state.
         */
        fun requestUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching {
                    requestListeningState(
                        context,
                        ComponentName(context, ToggleScheduleTileService::class.java)
                    )
                }
            }
        }
    }
}
