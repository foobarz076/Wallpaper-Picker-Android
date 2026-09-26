package foo.barz.wallpaperpicker.ui.tabs.settings

import android.os.Build
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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Sub-page for display targets, image crop modes, and parallax wallpaper scrolling settings.
 */
@Composable
fun DisplaySettingsSubPage(
    state: MainUiState,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onCropModeSelected: (WallpaperCropMode) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onToggleReapplyOnScrollChange: (Boolean) -> Unit,
    onReapplyCurrentWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("更换应用目标", style = MaterialTheme.typography.titleMedium)
                }
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
