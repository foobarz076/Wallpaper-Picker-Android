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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import foo.barz.wallpaperpicker.ui.MainUiState
import java.util.Locale

/**
 * Settings tab configuring global automation scheduling, screen targets, image cropping,
 * parallax scrolling, storage and cache management, and battery optimization guarantees.
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
    onToggleIntervalSchedule: (Boolean) -> Unit = {},
    onIntervalSelected: (Long) -> Unit,
    onToggleExactTimer: (Boolean) -> Unit = {},
    onToggleDailyAnchor: (Boolean) -> Unit = {},
    onSetDailyAnchorTime: (Int, Int) -> Unit = { _, _ -> },
    onAddDailyAnchorTime: (Int, Int) -> Unit = { _, _ -> },
    onRemoveDailyAnchorTime: (String) -> Unit = {},
    onToggleScreenOffTrigger: (Boolean) -> Unit = {},
    onSetScreenOffDelaySeconds: (Int) -> Unit = {},
    onToggleQuietHours: (Boolean) -> Unit = {},
    onSetQuietHours: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onToggleCooldownSuppression: (Boolean) -> Unit = {},
    onSetCooldownMinutes: (Long) -> Unit = {},
    onToggleDeferDuringInteraction: (Boolean) -> Unit,
    onToggleFairShuffle: (Boolean) -> Unit,
    onFairShuffleCapacitySelected: (Int) -> Unit = {},
    onResetFairShuffleDeck: () -> Unit = {},
    onCacheSizeTierSelected: (CacheSizeTier) -> Unit = {},
    onWidgetScaleTypeSelected: (WidgetScaleType) -> Unit = {},
    onClearCache: () -> Unit = {},
    onExportFavorites: () -> Unit = {},
    onOpenManageSpace: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }
    var showBatteryOptimizationPrompt by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Top Banner: Battery Optimization Guidance Card (Dismisses automatically once exempted)
        if (!isIgnoringBatteryOptimizations) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开启电池无限制以保障准时更换", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "低电耗 (Doze) 模式与系统省电策略可能会延迟休眠期间的定时换壁纸。建议在系统设置中将本应用设为「无限制」或移入白名单。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openAppBatteryDetailsSettings(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "应用详情设置",
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                openBatteryOptimizationList(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "系统白名单",
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            openDontKillMyApp(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OEM 定制系统保活指南 (DontKillMyApp)",
                            maxLines = 1
                        )
                    }
                }
            }
        }

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

        // 4. Scheduling & Composable Triggers Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row with Master Automation Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("自动更换调度", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (state.isScheduled) "自动轮换中，下方启用的触发方式将并行生效" else "自动更换已停止",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = state.isScheduled,
                        onCheckedChange = onToggleSchedule
                    )
                }

                if (state.isScheduled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "可组合触发方式 (支持多选组合生效)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Trigger 1: 按固定间隔周期轮换
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("周期轮换 (固定时间间隔)", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "每隔指定时间自动抽取并应用新壁纸",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = state.intervalScheduleEnabled,
                                    onCheckedChange = onToggleIntervalSchedule
                                )
                            }

                            if (state.intervalScheduleEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val intervals = listOf(
                                    15L to "15分钟",
                                    30L to "30分钟",
                                    60L to "1小时",
                                    360L to "6小时",
                                    1440L to "每天"
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    intervals.forEach { (minutes, label) ->
                                        FilterChip(
                                            selected = state.intervalMinutes == minutes,
                                            onClick = { onIntervalSelected(minutes) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trigger 2: 每日定点打卡 (支持多个定点时刻)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("每日定点打卡", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "在每天指定的固定时刻准点打卡换新（可添加多个时刻）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = state.dailyAnchorEnabled,
                                    onCheckedChange = onToggleDailyAnchor
                                )
                            }

                            if (state.dailyAnchorEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    state.dailyAnchorTimes.sorted().forEach { timeStr ->
                                        FilterChip(
                                            selected = true,
                                            onClick = { onRemoveDailyAnchorTime(timeStr) },
                                            label = { Text(timeStr) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "删除时刻",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val cal = java.util.Calendar.getInstance()
                                            android.app.TimePickerDialog(
                                                context,
                                                { _, hour, minute -> onAddDailyAnchorTime(hour, minute) },
                                                cal.get(java.util.Calendar.HOUR_OF_DAY),
                                                cal.get(java.util.Calendar.MINUTE),
                                                true
                                            ).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("添加时刻", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trigger 3: 锁屏熄屏后切换 (防抖延迟)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("锁屏熄屏后切换", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "手机锁屏熄屏后延迟数秒悄悄换好壁纸，再次亮屏解锁即见新图",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = state.screenOffTriggerEnabled,
                                    onCheckedChange = onToggleScreenOffTrigger
                                )
                            }

                            if (state.screenOffTriggerEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("熄屏防抖延迟：", style = MaterialTheme.typography.bodySmall)
                                    listOf(3 to "3秒", 5 to "5秒", 10 to "10秒").forEach { (sec, label) ->
                                        FilterChip(
                                            selected = state.screenOffDelaySeconds == sec,
                                            onClick = { onSetScreenOffDelaySeconds(sec) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Precision Mode Row: AlarmManager vs WorkManager
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("精细定时器 (AlarmManager 高精度)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (state.exactTimerEnabled) {
                                    "已启用高精度时钟，规避系统批处理延迟与随机漂移"
                                } else {
                                    "关闭时采用系统节能 WorkManager 调度，功耗更低但存在数分钟漂移"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.exactTimerEnabled,
                            onCheckedChange = { willEnable ->
                                if (willEnable) {
                                    if (!isIgnoringBatteryOptimizations) {
                                        showBatteryOptimizationPrompt = true
                                    } else {
                                        onToggleExactTimer(true)
                                    }
                                } else {
                                    onToggleExactTimer(false)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 4.1 Advanced Safeguards Card (Collapsible)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("高级触发防护与策略", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "夜间免打扰、防碰撞冷却、使用时推迟、洗牌防重复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    TextButton(onClick = { isAdvancedExpanded = !isAdvancedExpanded }) {
                        Text(if (isAdvancedExpanded) "收起" else "展开")
                        Icon(
                            if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }

                if (isAdvancedExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quiet Hours Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("夜间免打扰时段 (Quiet Hours)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "在睡眠时段内静默暂停自动更换，避免夜间唤醒与屏幕耗电",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.quietHoursEnabled,
                            onCheckedChange = onToggleQuietHours
                        )
                    }

                    if (state.quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("休眠时段：", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            onSetQuietHours(hour, minute, state.quietHoursEndHour, state.quietHoursEndMinute)
                                        },
                                        state.quietHoursStartHour,
                                        state.quietHoursStartMinute,
                                        true
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text("从 ${CompositeTriggerHelper.formatTime(state.quietHoursStartHour, state.quietHoursStartMinute)}")
                            }

                            Text("至", style = MaterialTheme.typography.bodySmall)

                            OutlinedButton(
                                onClick = {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            onSetQuietHours(state.quietHoursStartHour, state.quietHoursStartMinute, hour, minute)
                                        },
                                        state.quietHoursEndHour,
                                        state.quietHoursEndMinute,
                                        true
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text("到 ${CompositeTriggerHelper.formatTime(state.quietHoursEndHour, state.quietHoursEndMinute)}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cooldown Suppression Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("防碰撞冷却抑制 (Cooldown)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "距上次更换不足指定时间时拦截自动任务，避免与定点时刻或手动更换重叠",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.cooldownSuppressionEnabled,
                            onCheckedChange = onToggleCooldownSuppression
                        )
                    }

                    if (state.cooldownSuppressionEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5L to "5分钟", 10L to "10分钟", 15L to "15分钟", 30L to "30分钟").forEach { (minutes, label) ->
                                FilterChip(
                                    selected = state.cooldownMinutes == minutes,
                                    onClick = { onSetCooldownMinutes(minutes) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Defer during interaction
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
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Fair Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("智能洗牌防重复 (Fair Shuffle)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "记忆最近已用壁纸，在一整轮展示完之前避免高频抽取相同图片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.fairShuffle,
                            onCheckedChange = onToggleFairShuffle
                        )
                    }

                    if (state.fairShuffle) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "记忆窗口深度：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(20, 50, 100).forEach { capacity ->
                                FilterChip(
                                    selected = state.fairShuffleCapacity == capacity,
                                    onClick = { onFairShuffleCapacitySelected(capacity) },
                                    label = { Text("$capacity 张") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "当前牌堆记忆: ${state.fairShuffleRecordedCount} / ${state.fairShuffleCapacity} 张",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(
                                onClick = onResetFairShuffleDeck,
                                enabled = state.fairShuffleRecordedCount > 0,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("重置洗牌牌堆", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 5. Desktop Shortcuts & AppWidgets Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("桌面快捷方式与微件", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "支持长按桌面应用图标快捷调起，也可直接将独立图标或壁纸微件添加到桌面。点击后静默在后台完成操作并弹出提示，无需打开应用主界面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val success = foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper.requestPinNextWallpaperShortcut(context)
                            if (!success) {
                                android.widget.Toast.makeText(context, "可长按桌面应用图标，将「立即换壁纸」拖拽至桌面", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加「换壁纸」到桌面", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            val success = foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper.requestPinViewCurrentShortcut(context)
                            if (!success) {
                                android.widget.Toast.makeText(context, "可长按桌面应用图标，将「查看壁纸」拖拽至桌面", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加「看原图」到桌面", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // AppWidget Thumbnail Scaling Configuration
                Text("桌面微件：当前壁纸缩略图", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "在启动器添加「当前壁纸」微件，点击主体可查看大图，右上角按钮可立即换壁纸。请选择微件内的缩略图裁切模式：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.widgetScaleType == WidgetScaleType.CROP,
                        onClick = { onWidgetScaleTypeSelected(WidgetScaleType.CROP) },
                        label = { Text("居中裁切铺满") }
                    )
                    FilterChip(
                        selected = state.widgetScaleType == WidgetScaleType.FIT,
                        onClick = { onWidgetScaleTypeSelected(WidgetScaleType.FIT) },
                        label = { Text("保持原比完整") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (state.widgetScaleType == WidgetScaleType.CROP) {
                        "铺满微件卡片，边缘无留白，与桌面卡片风格融为一体（默认）"
                    } else {
                        "保持原图比例居中展示，上下或左右留黑边，完整展示构图细节"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 6. Storage and Cache Management Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("存储与缓存管理", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Favorites Storage Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("已收藏壁纸 (私有持久化)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "存放在内部专属目录，断网永久可用，绝不受系统或缓存清理影响",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.favoritesList.size} 张 · ${formatFileSize(state.favoritesSizeBytes)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Transient Cache Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("临时网络缓存 (LRU 缓存池)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "网络图源下载的临时图片，用于离线降级复用。超出容量上限后按最久未展示 (LRU) 顺序自动淘汰，已收藏壁纸受离线保护永不删除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatFileSize(state.cacheSizeBytes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // 4.1.3 Cache Size Tier Configuration & Disabled Option
                Text("自动清理与缓存容量上限", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "配置网络缓存池的最大淘汰容量；达到上限后自动淘汰最旧图片：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.cacheSizeTier == CacheSizeTier.SMALL,
                            onClick = { onCacheSizeTierSelected(CacheSizeTier.SMALL) },
                            label = { Text("小 (20MB)") }
                        )
                        FilterChip(
                            selected = state.cacheSizeTier == CacheSizeTier.STANDARD,
                            onClick = { onCacheSizeTierSelected(CacheSizeTier.STANDARD) },
                            label = { Text("标准 (50MB)") }
                        )
                        FilterChip(
                            selected = state.cacheSizeTier == CacheSizeTier.LARGE,
                            onClick = { onCacheSizeTierSelected(CacheSizeTier.LARGE) },
                            label = { Text("大 (100MB)") }
                        )
                    }

                    FilterChip(
                        selected = state.cacheSizeTier == CacheSizeTier.DISABLED,
                        onClick = { onCacheSizeTierSelected(CacheSizeTier.DISABLED) },
                        label = { Text("禁用自动清理 (不限制)") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.cacheSizeTier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.cacheSizeTier == CacheSizeTier.DISABLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button: Export Favorites
                OutlinedButton(
                    onClick = onExportFavorites,
                    enabled = state.favoritesList.isNotEmpty() && !state.isExportingFavorites,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isExportingFavorites) "正在导出…" else "导出全部收藏")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manage Space Activity launcher
                OutlinedButton(
                    onClick = onOpenManageSpace,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("管理存储空间与清理缓存…")
                }
            }
        }

        // 6. About and Open Source Licensing Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关于与开源许可", style = MaterialTheme.typography.titleMedium)
                    }
                    SuggestionChip(
                        onClick = onOpenAbout,
                        label = { Text("GPL-3.0-or-later") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wallpaper Picker (壁纸随心换) v1.0.0\n轻量、极低功耗、跨代兼容 Android 6.0 ~ 16 的多源壁纸轮换工具。遵循 GPL-3.0 协议开源。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Policy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("查看应用关于、诊断与第三方许可…")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showBatteryOptimizationPrompt) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationPrompt = false },
            icon = {
                Icon(
                    Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("开启精细定时器需忽略电池优化") },
            text = {
                Text(
                    "Android 系统的低电耗模式 (Doze) 会在息屏休眠时冻结定时器唤醒，导致壁纸无法准时更换。\n\n建议前往系统设置将本应用设为「无限制」或加入电池优化白名单。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryOptimizationPrompt = false
                        openAppBatteryDetailsSettings(context)
                    }
                ) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showBatteryOptimizationPrompt = false
                            onToggleExactTimer(true)
                        }
                    ) {
                        Text("仍然开启")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { showBatteryOptimizationPrompt = false }
                    ) {
                        Text("取消")
                    }
                }
            }
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.1f KB", kb)
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Opens the application details settings screen where the user can configure
 * app battery usage to "Unrestricted" / "No restrictions".
 */
private fun openAppBatteryDetailsSettings(context: Context) {
    try {
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appDetailsIntent)
    } catch (_: Exception) {
        openBatteryOptimizationList(context)
    }
}

/**
 * Opens the system battery optimization whitelist settings page.
 * Falls back to general system settings if unsupported on the current device.
 */
private fun openBatteryOptimizationList(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val generalSettingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalSettingsIntent)
        } catch (_: Exception) {
            // Ignore if device does not support settings activity
        }
    }
}

/**
 * Resolves the device manufacturer-specific guide on DontKillMyApp.
 * Falls back to the root website if the manufacturer is not specifically categorized.
 */
private fun getDontKillMyAppUrl(): String {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ->
            "https://dontkillmyapp.com/xiaomi"
        manufacturer.contains("huawei") || manufacturer.contains("honor") ->
            "https://dontkillmyapp.com/huawei"
        manufacturer.contains("samsung") ->
            "https://dontkillmyapp.com/samsung"
        manufacturer.contains("oneplus") ->
            "https://dontkillmyapp.com/oneplus"
        manufacturer.contains("oppo") || manufacturer.contains("realme") ->
            "https://dontkillmyapp.com/oppo"
        manufacturer.contains("vivo") || manufacturer.contains("iqoo") ->
            "https://dontkillmyapp.com/vivo"
        manufacturer.contains("meizu") ->
            "https://dontkillmyapp.com/meizu"
        manufacturer.contains("sony") ->
            "https://dontkillmyapp.com/sony"
        manufacturer.contains("asus") ->
            "https://dontkillmyapp.com/asus"
        manufacturer.contains("nokia") ->
            "https://dontkillmyapp.com/nokia"
        manufacturer.contains("lenovo") || manufacturer.contains("motorola") ->
            "https://dontkillmyapp.com/motorola"
        else ->
            "https://dontkillmyapp.com/"
    }
}

/**
 * Opens DontKillMyApp website targeting the current device manufacturer in the user's browser.
 */
private fun openDontKillMyApp(context: Context) {
    try {
        val url = getDontKillMyAppUrl()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore if no suitable browser application is available
    }
}
