package foo.barz.wallpaperpicker.ui.components

import android.app.TimePickerDialog
import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import java.util.Calendar

/**
 * Dialog for creating or modifying a schedule rule in the Schedule Rule Engine (Phase 4.2).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleRuleEditDialog(
    initialRule: ScheduleRule?,
    availableSources: List<WallpaperSourceEntity>,
    onDismissRequest: () -> Unit,
    onSaveRule: (ScheduleRule) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = initialRule != null

    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var triggerType by remember {
        mutableStateOf(initialRule?.triggerType ?: ScheduleRuleTriggerType.DAILY_TIME)
    }
    var targetTime by remember { mutableStateOf(initialRule?.targetTime ?: "08:00") }
    var windowStartTime by remember { mutableStateOf(initialRule?.windowStartTime ?: "09:00") }
    var windowEndTime by remember { mutableStateOf(initialRule?.windowEndTime ?: "18:00") }
    var intervalMinutes by remember { mutableStateOf(initialRule?.intervalMinutes ?: 120L) }
    var screenOffDelaySeconds by remember { mutableStateOf(initialRule?.screenOffDelaySeconds ?: 3) }

    var sourceBinding by remember {
        mutableStateOf(initialRule?.sourceBinding ?: ScheduleRuleSourceBinding.ACTIVE_DEFAULT)
    }
    var selectedSourceId by remember { mutableStateOf(initialRule?.specificSourceId) }
    var selectedSourceTitle by remember { mutableStateOf(initialRule?.specificSourceTitle) }

    var targetScreen by remember { mutableStateOf(initialRule?.targetScreen ?: WallpaperTarget.BOTH) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emptyNameErr = stringResource(R.string.rule_err_name_empty)
    val selectSourceErr = stringResource(R.string.rule_err_select_source)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(if (isEditMode) stringResource(R.string.rule_dialog_title_edit) else stringResource(R.string.rule_dialog_title_create))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Rule Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.rule_name_label)) },
                    placeholder = { Text(stringResource(R.string.rule_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Trigger Type Selection
                Text(stringResource(R.string.rule_trigger_type_section), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScheduleRuleTriggerType.values().forEach { type ->
                        FilterChip(
                            selected = triggerType == type,
                            onClick = { triggerType = type },
                            label = { Text(stringResource(type.displayNameRes)) }
                        )
                    }
                }

                // 3. Trigger Details depending on type
                when (triggerType) {
                    ScheduleRuleTriggerType.DAILY_TIME -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.rule_daily_time_label), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = {
                                    val parsed = CompositeTriggerHelper.parseTime(targetTime) ?: Pair(8, 0)
                                    TimePickerDialog(
                                        context,
                                        { _, h, m -> targetTime = CompositeTriggerHelper.formatTime(h, m) },
                                        parsed.first,
                                        parsed.second,
                                        true
                                    ).show()
                                }
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(targetTime)
                            }
                        }
                    }
                    ScheduleRuleTriggerType.TIME_WINDOW -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(stringResource(R.string.rule_window_range_label), style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = {
                                        val parsed = CompositeTriggerHelper.parseTime(windowStartTime) ?: Pair(9, 0)
                                        TimePickerDialog(
                                            context,
                                            { _, h, m -> windowStartTime = CompositeTriggerHelper.formatTime(h, m) },
                                            parsed.first,
                                            parsed.second,
                                            true
                                        ).show()
                                    }
                                ) {
                                    Text(stringResource(R.string.rule_window_from_format, windowStartTime))
                                }
                                Text(stringResource(R.string.rule_window_to_label), style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = {
                                        val parsed = CompositeTriggerHelper.parseTime(windowEndTime) ?: Pair(18, 0)
                                        TimePickerDialog(
                                            context,
                                            { _, h, m -> windowEndTime = CompositeTriggerHelper.formatTime(h, m) },
                                            parsed.first,
                                            parsed.second,
                                            true
                                        ).show()
                                    }
                                ) {
                                    Text(stringResource(R.string.rule_window_to_format, windowEndTime))
                                }
                            }

                            Text(stringResource(R.string.rule_interval_label), style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    15L to stringResource(R.string.rule_interval_15m),
                                    30L to stringResource(R.string.rule_interval_30m),
                                    60L to stringResource(R.string.rule_interval_1h),
                                    120L to stringResource(R.string.rule_interval_2h),
                                    240L to stringResource(R.string.rule_interval_4h),
                                    360L to stringResource(R.string.rule_interval_6h)
                                ).forEach { (min, label) ->
                                    FilterChip(
                                        selected = intervalMinutes == min,
                                        onClick = { intervalMinutes = min },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                    ScheduleRuleTriggerType.SCREEN_OFF -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.rule_screen_off_delay_label), style = MaterialTheme.typography.bodySmall)
                            listOf(3, 5, 10).forEach { sec ->
                                FilterChip(
                                    selected = screenOffDelaySeconds == sec,
                                    onClick = { screenOffDelaySeconds = sec },
                                    label = { Text(stringResource(R.string.rule_delay_seconds_format, sec)) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 4. Source Binding Strategy
                Text(stringResource(R.string.rule_binding_section), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScheduleRuleSourceBinding.values().forEach { binding ->
                        FilterChip(
                            selected = sourceBinding == binding,
                            onClick = { sourceBinding = binding },
                            label = { Text(stringResource(binding.displayNameRes)) }
                        )
                    }
                }

                if (sourceBinding == ScheduleRuleSourceBinding.SPECIFIC_SOURCE) {
                    Text(stringResource(R.string.rule_binding_select_label), style = MaterialTheme.typography.bodySmall)
                    if (availableSources.isEmpty()) {
                        Text(
                            text = stringResource(R.string.rule_binding_no_sources),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableSources.forEach { entity ->
                                val isSelected = selectedSourceId == entity.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSourceId = entity.id
                                        selectedSourceTitle = entity.title
                                    },
                                    label = { Text(entity.title) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 5. Target Screen
                Text(stringResource(R.string.rule_target_section), style = MaterialTheme.typography.labelLarge)
                val canSeparateTarget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetScreen == WallpaperTarget.BOTH,
                        onClick = { targetScreen = WallpaperTarget.BOTH },
                        label = { Text(stringResource(WallpaperTarget.BOTH.labelRes)) }
                    )
                    if (canSeparateTarget) {
                        FilterChip(
                            selected = targetScreen == WallpaperTarget.SYSTEM,
                            onClick = { targetScreen = WallpaperTarget.SYSTEM },
                            label = { Text(stringResource(WallpaperTarget.SYSTEM.labelRes)) }
                        )
                        FilterChip(
                            selected = targetScreen == WallpaperTarget.LOCK,
                            onClick = { targetScreen = WallpaperTarget.LOCK },
                            label = { Text(stringResource(WallpaperTarget.LOCK.labelRes)) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = emptyNameErr
                        return@Button
                    }
                    if (sourceBinding == ScheduleRuleSourceBinding.SPECIFIC_SOURCE && selectedSourceId.isNullOrBlank()) {
                        errorMessage = selectSourceErr
                        return@Button
                    }

                    val updatedRule = (initialRule ?: ScheduleRule(name = trimmedName)).copy(
                        name = trimmedName,
                        triggerType = triggerType,
                        targetTime = targetTime,
                        windowStartTime = windowStartTime,
                        windowEndTime = windowEndTime,
                        intervalMinutes = intervalMinutes,
                        screenOffDelaySeconds = screenOffDelaySeconds,
                        sourceBinding = sourceBinding,
                        specificSourceId = if (sourceBinding == ScheduleRuleSourceBinding.SPECIFIC_SOURCE) selectedSourceId else null,
                        specificSourceTitle = if (sourceBinding == ScheduleRuleSourceBinding.SPECIFIC_SOURCE) selectedSourceTitle else null,
                        targetScreen = targetScreen,
                        updatedTimestamp = System.currentTimeMillis()
                    )
                    onSaveRule(updatedRule)
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
