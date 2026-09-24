package foo.barz.wallpaperpicker.ui.tabs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Settings tab configuring global automation scheduling, screen targets, image cropping,
 * parallax scrolling, and battery optimization guarantees.
 */
@Composable
fun SettingsTab(
    state: MainUiState,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onCropModeSelected: (WallpaperCropMode) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onToggleReapplyOnScrollChange: (Boolean) -> Unit,
    onReapplyCurrentWallpaper: () -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onToggleDeferDuringInteraction: (Boolean) -> Unit,
    onToggleFairShuffle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Target Screen Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("更换应用目标", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val canSeparateTarget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                if (!canSeparateTarget) {
                    Text(
                        text = "当前设备运行 Android 6.0，系统规范限制将同时更新桌面与锁屏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.target == WallpaperTarget.BOTH,
                        onClick = { onTargetSelected(WallpaperTarget.BOTH) },
                        label = { Text("桌面与锁屏") }
                    )
                    if (canSeparateTarget) {
                        FilterChip(
                            selected = state.target == WallpaperTarget.SYSTEM,
                            onClick = { onTargetSelected(WallpaperTarget.SYSTEM) },
                            label = { Text("仅桌面") }
                        )
                        FilterChip(
                            selected = state.target == WallpaperTarget.LOCK,
                            onClick = { onTargetSelected(WallpaperTarget.LOCK) },
                            label = { Text("仅锁屏") }
                        )
                    }
                }
            }
        }

        // 2. Wallpaper Crop Mode Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("壁纸裁切与构图", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        WallpaperCropMode.FIT_HEIGHT,
                        WallpaperCropMode.CENTER_CROP,
                        WallpaperCropMode.FIT_CENTER
                    ).forEach { mode ->
                        FilterChip(
                            selected = state.cropMode == mode,
                            onClick = { onCropModeSelected(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.cropMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // 3. Wallpaper Scroll Mode Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ViewCarousel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("桌面视差随动", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        WallpaperScrollMode.AUTO,
                        WallpaperScrollMode.NEVER,
                        WallpaperScrollMode.ALWAYS
                    ).forEach { mode ->
                        FilterChip(
                            selected = state.scrollMode == mode,
                            onClick = { onScrollModeSelected(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.scrollMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "提示：部分定制系统（如 HyperOS/MIUI 或已关闭壁纸随动的桌面）不支持壁纸平移。若滑动桌面时壁纸静止，建议选择「锁定居中」以单屏最清晰画质呈现。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("修改设置时重设当前壁纸", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "更改裁切或滚动设置时，立即按新模式重新渲染并应用当前壁纸",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.reapplyOnScrollChange,
                        onCheckedChange = onToggleReapplyOnScrollChange
                    )
                }

                if (state.lastWallpaperUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReapplyCurrentWallpaper,
                        enabled = !state.isChanging,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("按当前设置重设正在使用的壁纸")
                    }
                }
            }
        }

        // 4. Periodic Schedule Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("后台自动定时更换", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = state.isScheduled,
                        onCheckedChange = onToggleSchedule
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("更换频率", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val intervals = listOf(
                    15L to "15分钟",
                    30L to "30分钟",
                    60L to "1小时",
                    360L to "6小时",
                    1440L to "每天"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervals.take(3).forEach { (minutes, label) ->
                        FilterChip(
                            selected = state.intervalMinutes == minutes,
                            onClick = { onIntervalSelected(minutes) },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervals.drop(3).forEach { (minutes, label) ->
                        FilterChip(
                            selected = state.intervalMinutes == minutes,
                            onClick = { onIntervalSelected(minutes) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("使用手机时推迟更换", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "检测到亮屏或正在使用手机时暂缓更换壁纸，防止游戏、观影或打字时发生掉帧卡顿",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.deferDuringInteraction,
                        onCheckedChange = onToggleDeferDuringInteraction
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("智能洗牌防重复 (Fair Shuffle)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "记忆最近 50 张已用壁纸，在一整轮展示完之前避免高频抽取相同图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.fairShuffle,
                        onCheckedChange = onToggleFairShuffle
                    )
                }
            }
        }

        // 5. Battery Optimization Hint Card
        if (!isIgnoringBatteryOptimizations) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开启电池白名单以保障准时更换", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "低电耗 (Doze) 模式可能会延迟休眠期间的定时任务，建议为本应用加入电池优化白名单。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            requestIgnoreBatteryOptimization(context)
                            isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
                        }
                    ) {
                        Text("前往设置白名单")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            // Ignore if device does not support
        }
    }
}
