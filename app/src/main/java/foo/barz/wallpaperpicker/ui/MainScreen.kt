package foo.barz.wallpaperpicker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onFolderSelected: (Uri) -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onChangeNow: () -> Unit,
    onClearStatus: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onFolderSelected(uri)
        }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Picker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Button(
                    onClick = onChangeNow,
                    enabled = !state.isChanging && state.folderUri != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.isChanging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("正在处理并更换壁纸…")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即更换壁纸")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前状态", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "激活来源: 本地文件夹",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "上次更换: ${state.lastChangedText}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (state.isScheduled) "定时调度: 已开启 (每 ${state.intervalMinutes} 分钟)" else "定时调度: 未开启",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 2. Folder Source Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("壁纸文件夹来源", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.folderName?.let { "已选目录: $it" } ?: "尚未选择任何文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.folderUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.folderUri != null) "更换文件夹授权" else "选择文件夹授权")
                    }
                }
            }

            // 3. Target Screen Card
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

            // 4. Wallpaper Scroll Mode Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("壁纸随桌面滚动", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            WallpaperScrollMode.AUTO,
                            WallpaperScrollMode.ALWAYS,
                            WallpaperScrollMode.NEVER
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
                }
            }

            // 5. Schedule Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                }
            }

            // 5. Battery Optimization Hint
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

            Spacer(modifier = Modifier.height(80.dp)) // Extra padding for bottom bar
        }
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
            // Ignore if device doesn't support
        }
    }
}
