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
import androidx.compose.ui.unit.dp
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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(if (isEditMode) "编辑调度规则" else "新建调度规则")
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
                    label = { Text("规则名称") },
                    placeholder = { Text("例如：晨间相册、工作时段轮换") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Trigger Type Selection
                Text("触发方式", style = MaterialTheme.typography.labelLarge)
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
                            label = { Text(type.displayName) }
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
                            Text("定点打卡时刻：", style = MaterialTheme.typography.bodySmall)
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
                                Text("时段区间：", style = MaterialTheme.typography.bodySmall)
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
                                    Text("从 $windowStartTime")
                                }
                                Text("至", style = MaterialTheme.typography.bodySmall)
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
                                    Text("到 $windowEndTime")
                                }
                            }

                            Text("轮换间隔：", style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    15L to "15分钟",
                                    30L to "30分钟",
                                    60L to "1小时",
                                    120L to "2小时",
                                    240L to "4小时",
                                    360L to "6小时"
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
                            Text("熄屏防抖延迟：", style = MaterialTheme.typography.bodySmall)
                            listOf(3 to "3秒", 5 to "5秒", 10 to "10秒").forEach { (sec, label) ->
                                FilterChip(
                                    selected = screenOffDelaySeconds == sec,
                                    onClick = { screenOffDelaySeconds = sec },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 4. Source Binding Strategy
                Text("专属图源绑定", style = MaterialTheme.typography.labelLarge)
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
                            label = { Text(binding.displayName) }
                        )
                    }
                }

                if (sourceBinding == ScheduleRuleSourceBinding.SPECIFIC_SOURCE) {
                    Text("选择绑定的图源实例：", style = MaterialTheme.typography.bodySmall)
                    if (availableSources.isEmpty()) {
                        Text(
                            text = "尚未在「图源管理」中配置图源，请先添加图源",
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
                Text("更换应用目标", style = MaterialTheme.typography.labelLarge)
                val canSeparateTarget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetScreen == WallpaperTarget.BOTH,
                        onClick = { targetScreen = WallpaperTarget.BOTH },
                        label = { Text("桌面与锁屏") }
                    )
                    if (canSeparateTarget) {
                        FilterChip(
                            selected = targetScreen == WallpaperTarget.SYSTEM,
                            onClick = { targetScreen = WallpaperTarget.SYSTEM },
                            label = { Text("仅桌面") }
                        )
                        FilterChip(
                            selected = targetScreen == WallpaperTarget.LOCK,
                            onClick = { targetScreen = WallpaperTarget.LOCK },
                            label = { Text("仅锁屏") }
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
                        errorMessage = "请输入规则名称"
                        return@Button
                    }
                    if (sourceBinding == ScheduleRuleSourceBinding.SPECIFIC_SOURCE && selectedSourceId.isNullOrBlank()) {
                        errorMessage = "请选择绑定的专属图源"
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
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
