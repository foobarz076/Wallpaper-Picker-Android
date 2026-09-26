package foo.barz.wallpaperpicker.ui.tabs.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Sub-page for configuring launcher shortcuts, desktop app widgets,
 * and quick settings notification tiles.
 */
@Composable
fun WidgetsSettingsSubPage(
    state: MainUiState,
    onWidgetScaleTypeSelected: (WidgetScaleType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Desktop Shortcuts Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("桌面快捷方式 (Pin Shortcuts)", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "支持长按桌面应用图标快捷调起，也可直接将独立图标固定到桌面。点击后静默在后台完成操作并弹出提示，无需打开应用主界面。",
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
                            val success = ShortcutHelper.requestPinNextWallpaperShortcut(context)
                            if (!success) {
                                Toast.makeText(context, "可长按桌面应用图标，将「立即换壁纸」拖拽至桌面", Toast.LENGTH_LONG).show()
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
                            val success = ShortcutHelper.requestPinViewCurrentShortcut(context)
                            if (!success) {
                                Toast.makeText(context, "可长按桌面应用图标，将「查看壁纸」拖拽至桌面", Toast.LENGTH_LONG).show()
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
            }
        }

        // 2. Desktop AppWidget Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("桌面微件：当前壁纸缩略图", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "在启动器添加「当前壁纸」微件，点击主体可查看大图，右上角按钮可立即换壁纸。请选择微件内的缩略图裁切模式：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(6.dp))
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

        // 3. Quick Settings Tiles Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("控制中心快捷磁贴 (Quick Settings)", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "支持将快捷开关加入 Android 顶部下拉控制中心：\n" +
                        "•「下一张壁纸」：点击即在后台静默换壁纸并发送小浮条提醒；\n" +
                        "•「自动更换开关」：高亮显示当前调度状态，点击可直接启停轮播。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "使用提示：从屏幕顶部下拉展开控制中心，点击「编辑 (铅笔)」图标，即可将「下一张壁纸」和「自动轮播」拖动至常用磁贴区域。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
