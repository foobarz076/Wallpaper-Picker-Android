package foo.barz.wallpaperpicker.ui.tabs.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Sub-page configuring advanced execution safeguards: quiet hours, collision cooldown,
 * interaction avoidance, and fair-shuffle anti-repetition memory.
 */
@Composable
fun SafeguardsSettingsSubPage(
    state: MainUiState,
    onToggleQuietHours: (Boolean) -> Unit,
    onSetQuietHours: (Int, Int, Int, Int) -> Unit,
    onToggleCooldownSuppression: (Boolean) -> Unit,
    onSetCooldownMinutes: (Long) -> Unit,
    onToggleDeferDuringInteraction: (Boolean) -> Unit,
    onToggleFairShuffle: (Boolean) -> Unit,
    onFairShuffleCapacitySelected: (Int) -> Unit,
    onResetFairShuffleDeck: () -> Unit,
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

        // 1. Quiet Hours Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("夜间免打扰时段 (Quiet Hours)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "在睡眠时段内静默暂停自动更换，避免夜间唤醒与屏幕耗电",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = state.quietHoursEnabled,
                        onCheckedChange = onToggleQuietHours
                    )
                }

                if (state.quietHoursEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("休眠时段：", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
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
                                TimePickerDialog(
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
            }
        }

        // 2. Cooldown Suppression Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("防碰撞冷却抑制 (Cooldown)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "距上次更换不足指定时间时拦截自动任务，避免与定点时刻或手动更换重叠",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = state.cooldownSuppressionEnabled,
                        onCheckedChange = onToggleCooldownSuppression
                    )
                }

                if (state.cooldownSuppressionEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
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
            }
        }

        // 3. Defer during interaction Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("使用手机时推迟更换", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "检测到亮屏或正在使用手机时暂缓更换壁纸，防止游戏、观影或打字时发生掉帧卡顿",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = state.deferDuringInteraction,
                        onCheckedChange = onToggleDeferDuringInteraction
                    )
                }
            }
        }

        // 4. Fair Shuffle Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("智能洗牌防重复 (Fair Shuffle)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "记忆最近已用壁纸，在一整轮展示完之前避免高频抽取相同图片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = state.fairShuffle,
                        onCheckedChange = onToggleFairShuffle
                    )
                }

                if (state.fairShuffle) {
                    Spacer(modifier = Modifier.height(12.dp))
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

                    Spacer(modifier = Modifier.height(10.dp))
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
