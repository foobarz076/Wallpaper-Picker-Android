package foo.barz.wallpaperpicker.ui.tabs.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Root categorized navigation menu for settings with dynamic real-time summaries.
 */
@Composable
fun SettingsRootContent(
    state: MainUiState,
    isIgnoringBatteryOptimizations: Boolean,
    onNavigateToSubPage: (SettingsSubPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Compact Battery Warning Banner if optimizations are not yet exempted
        if (!isIgnoringBatteryOptimizations) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSubPage(SettingsSubPage.ABOUT) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "开启电池无限制以保障准时更换",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "点击前往配置系统白名单与厂商保活指南",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Category Cards
        SettingsSubPage.values().forEach { subPage ->
            val summaryText = when (subPage) {
                SettingsSubPage.DISPLAY -> {
                    "${state.target.label} · ${state.cropMode.label} · ${state.scrollMode.label}"
                }
                SettingsSubPage.SCHEDULING -> {
                    if (!state.isScheduled) {
                        "已停止自动轮换"
                    } else if (state.ruleEngineEnabled) {
                        val activeCount = state.scheduleRules.count { it.isEnabled }
                        "规则日程表已生效 ($activeCount 条规则启用)"
                    } else {
                        val triggers = mutableListOf<String>()
                        if (state.intervalScheduleEnabled) triggers.add("周期 ${state.intervalMinutes}m")
                        if (state.dailyAnchorEnabled) triggers.add("定点打卡 (${state.dailyAnchorTimes.size}次)")
                        if (state.screenOffTriggerEnabled) triggers.add("锁屏熄屏换")
                        if (triggers.isEmpty()) "已开启 · 待激活触发器" else "已开启 · ${triggers.joinToString(" + ")}"
                    }
                }
                SettingsSubPage.SAFEGUARDS -> {
                    val quietText = if (state.quietHoursEnabled) "免打扰生效中" else "免打扰已关闭"
                    val shuffleText = if (state.fairShuffle) "智能洗牌 (${state.fairShuffleCapacity}张)" else "纯随机挑选"
                    "$quietText · $shuffleText"
                }
                SettingsSubPage.WIDGETS -> {
                    val scaleText = if (state.widgetScaleType == WidgetScaleType.CROP) "居中铺满" else "完整原比"
                    "微件缩略图 ($scaleText) · 桌面快捷方式与磁贴"
                }
                SettingsSubPage.STORAGE -> {
                    val cacheText = SettingsHelpers.formatFileSize(state.cacheSizeBytes)
                    val tierLabel = state.cacheSizeTier.displayName
                    "缓存 $cacheText / $tierLabel · 收藏 ${state.favoritesList.size} 张"
                }
                SettingsSubPage.ABOUT -> {
                    val batteryStatus = if (isIgnoringBatteryOptimizations) "电池无限制已就绪" else "电池需配置"
                    "v1.0.0 · $batteryStatus · 诊断日志"
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSubPage(subPage) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = subPage.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subPage.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subPage.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
