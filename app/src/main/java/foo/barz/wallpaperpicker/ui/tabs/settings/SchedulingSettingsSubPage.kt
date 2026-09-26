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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
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
                            Text(stringResource(R.string.sched_master_switch_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (state.isScheduled) stringResource(R.string.sched_master_switch_active) else stringResource(R.string.sched_master_switch_stopped),
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

                    Text(stringResource(R.string.sched_engine_mode_title), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !state.ruleEngineEnabled,
                            onClick = { onToggleRuleEngine(false) },
                            label = { Text(stringResource(R.string.sched_engine_mode_composite)) }
                        )
                        FilterChip(
                            selected = state.ruleEngineEnabled,
                            onClick = {
                                onToggleRuleEngine(true)
                                if (state.scheduleRules.isEmpty()) {
                                    onPopulateDefaultRules()
                                }
                            },
                            label = { Text(stringResource(R.string.sched_engine_mode_rules)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.ruleEngineEnabled) {
                        Text(
                            text = stringResource(R.string.sched_rules_desc),
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
                                    Text(stringResource(R.string.sched_rules_empty_title), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onOpenRuleDialog(null) }) {
                                            Text(stringResource(R.string.sched_rules_btn_add))
                                        }
                                        OutlinedButton(onClick = onPopulateDefaultRules) {
                                            Text(stringResource(R.string.sched_rules_btn_load_default))
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
                                                        ScheduleRuleTriggerType.DAILY_TIME -> stringResource(R.string.sched_rule_trigger_daily_format, rule.targetTime)
                                                        ScheduleRuleTriggerType.TIME_WINDOW -> stringResource(R.string.sched_rule_trigger_window_format, rule.windowStartTime, rule.windowEndTime, rule.intervalMinutes)
                                                        ScheduleRuleTriggerType.SCREEN_OFF -> stringResource(R.string.sched_rule_trigger_screen_off_format, rule.screenOffDelaySeconds)
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
                                                    ScheduleRuleSourceBinding.ACTIVE_DEFAULT -> stringResource(rule.sourceBinding.displayNameRes)
                                                    ScheduleRuleSourceBinding.FAVORITES -> stringResource(R.string.sched_rule_binding_favorites)
                                                    ScheduleRuleSourceBinding.SPECIFIC_SOURCE -> stringResource(R.string.sched_rule_binding_dedicated_prefix, rule.specificSourceTitle ?: stringResource(R.string.sched_rule_binding_specified_fallback))
                                                }
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(sourceText) }
                                                )
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(stringResource(rule.targetScreen.labelRes)) }
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
                                                    Text(stringResource(R.string.sched_rule_btn_edit), style = MaterialTheme.typography.labelMedium)
                                                }
                                                TextButton(
                                                    onClick = { onDeleteScheduleRule(rule.id) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        stringResource(R.string.action_delete),
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
                                        Text(stringResource(R.string.sched_rules_btn_add))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = onPopulateDefaultRules) {
                                        Text(stringResource(R.string.sched_rules_btn_reset_defaults))
                                    }
                                }
                            }
                        }
                    } else {
                        // Master-Slave Composite Triggers
                        Text(
                            text = stringResource(R.string.sched_composite_section_title),
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
                                        Text(stringResource(R.string.sched_trigger_interval_title), style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = stringResource(R.string.sched_trigger_interval_desc),
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
                                        15L to stringResource(R.string.rule_interval_15m),
                                        30L to stringResource(R.string.rule_interval_30m),
                                        60L to stringResource(R.string.rule_interval_1h),
                                        360L to stringResource(R.string.rule_interval_6h),
                                        1440L to stringResource(R.string.sched_interval_every_day)
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
                                        Text(stringResource(R.string.sched_trigger_daily_title), style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = stringResource(R.string.sched_trigger_daily_desc),
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
                                                        contentDescription = stringResource(R.string.sched_daily_btn_delete_tooltip),
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
                                            Text(stringResource(R.string.sched_daily_btn_add_time), style = MaterialTheme.typography.bodySmall)
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
                                        Text(stringResource(R.string.sched_trigger_screen_off_title), style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = stringResource(R.string.sched_trigger_screen_off_desc),
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
                                        Text(stringResource(R.string.rule_screen_off_delay_label), style = MaterialTheme.typography.bodySmall)
                                        listOf(3, 5, 10).forEach { sec ->
                                            FilterChip(
                                                selected = state.screenOffDelaySeconds == sec,
                                                onClick = { onSetScreenOffDelaySeconds(sec) },
                                                label = { Text(stringResource(R.string.rule_delay_seconds_format, sec)) }
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
                            Text(stringResource(R.string.sched_exact_timer_title), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (state.exactTimerEnabled) {
                                    stringResource(R.string.sched_exact_timer_active_desc)
                                } else {
                                    stringResource(R.string.sched_exact_timer_inactive_desc)
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
