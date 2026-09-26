package foo.barz.wallpaperpicker.ui.tabs.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.ui.MainUiState
import java.util.Calendar

/**
 * Sub-page configuring wallpaper automation schedules, multi-trigger composable strategies,
 * the independent Schedule Rule Engine, and precision timers.
 */
@Composable
fun SchedulingSettingsSubPage(
    state: MainUiState,
    isIgnoringBatteryOptimizations: Boolean,
    onToggleSchedule: (Boolean) -> Unit,
    onToggleRuleEngine: (Boolean) -> Unit,
    onToggleIntervalSchedule: (Boolean) -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onToggleDailyAnchor: (Boolean) -> Unit,
    onAddDailyAnchorTime: (Int, Int) -> Unit,
    onRemoveDailyAnchorTime: (String) -> Unit,
    onToggleScreenOffTrigger: (Boolean) -> Unit,
    onSetScreenOffDelaySeconds: (Int) -> Unit,
    onToggleExactTimer: (Boolean) -> Unit,
    onRequestBatteryOptimizationPrompt: () -> Unit,
    onOpenRuleDialog: (ScheduleRule?) -> Unit,
    onDeleteScheduleRule: (String) -> Unit,
    onToggleScheduleRuleEnabled: (String, Boolean) -> Unit,
    onPopulateDefaultRules: () -> Unit,
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

        // Automation Master Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row with Master Switch
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
                            Text("自动更换总开关", style = MaterialTheme.typography.titleMedium)
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("调度引擎模式", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !state.ruleEngineEnabled,
                            onClick = { onToggleRuleEngine(false) },
                            label = { Text("主从复合模式") }
                        )
                        FilterChip(
                            selected = state.ruleEngineEnabled,
                            onClick = {
                                onToggleRuleEngine(true)
                                if (state.scheduleRules.isEmpty()) {
                                    onPopulateDefaultRules()
                                }
                            },
                            label = { Text("独立规则日程表 (Rule Engine)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.ruleEngineEnabled) {
                        Text(
                            text = "规则日程表：根据多条独立规则在指定时段或时刻切换专属图源与目标屏幕",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.scheduleRules.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("当前暂无调度规则", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onOpenRuleDialog(null) }) {
                                            Text("添加新规则")
                                        }
                                        OutlinedButton(onClick = onPopulateDefaultRules) {
                                            Text("载入默认预设")
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.scheduleRules.forEach { rule ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (rule.isEnabled)
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = rule.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                    )
                                                    val triggerDesc = when (rule.triggerType) {
                                                        ScheduleRuleTriggerType.DAILY_TIME -> "每日 ${rule.targetTime} 定点打卡"
                                                        ScheduleRuleTriggerType.TIME_WINDOW -> "${rule.windowStartTime} ~ ${rule.windowEndTime} (每 ${rule.intervalMinutes} 分钟)"
                                                        ScheduleRuleTriggerType.SCREEN_OFF -> "锁屏熄屏切换 (延迟 ${rule.screenOffDelaySeconds} 秒)"
                                                    }
                                                    Text(
                                                        text = triggerDesc,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Switch(
                                                    checked = rule.isEnabled,
                                                    onCheckedChange = { onToggleScheduleRuleEnabled(rule.id, it) }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val sourceText = when (rule.sourceBinding) {
                                                    ScheduleRuleSourceBinding.ACTIVE_DEFAULT -> "跟随全局激活源"
                                                    ScheduleRuleSourceBinding.FAVORITES -> "专属: 我的收藏"
                                                    ScheduleRuleSourceBinding.SPECIFIC_SOURCE -> "专属: ${rule.specificSourceTitle ?: "指定图源"}"
                                                }
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(sourceText) }
                                                )
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(rule.targetScreen.label) }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = { onOpenRuleDialog(rule) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("编辑", style = MaterialTheme.typography.labelMedium)
                                                }
                                                TextButton(
                                                    onClick = { onDeleteScheduleRule(rule.id) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "删除",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenRuleDialog(null) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("添加调度规则")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = onPopulateDefaultRules) {
                                        Text("重置默认预设")
                                    }
                                }
                            }
                        }
                    } else {
                        // Master-Slave Composite Triggers
                        Text(
                            text = "可组合触发方式 (支持多选组合生效)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Trigger 1: Fixed interval
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

                        // Trigger 2: Daily Anchor
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
                                                val cal = Calendar.getInstance()
                                                TimePickerDialog(
                                                    context,
                                                    { _, hour, minute -> onAddDailyAnchorTime(hour, minute) },
                                                    cal.get(Calendar.HOUR_OF_DAY),
                                                    cal.get(Calendar.MINUTE),
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

                        // Trigger 3: Screen-Off debounce
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
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                        onRequestBatteryOptimizationPrompt()
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
