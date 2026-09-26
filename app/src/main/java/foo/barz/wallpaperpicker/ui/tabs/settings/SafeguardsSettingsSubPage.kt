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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
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
                            Text(stringResource(R.string.safeguard_quiet_hours_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.safeguard_quiet_hours_desc),
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
                        Text(stringResource(R.string.safeguard_quiet_hours_period), style = MaterialTheme.typography.bodySmall)
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
                            Text(stringResource(R.string.safeguard_quiet_hours_from, CompositeTriggerHelper.formatTime(state.quietHoursStartHour, state.quietHoursStartMinute)))
                        }

                        Text(stringResource(R.string.safeguard_quiet_hours_to_label), style = MaterialTheme.typography.bodySmall)

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
                            Text(stringResource(R.string.safeguard_quiet_hours_to, CompositeTriggerHelper.formatTime(state.quietHoursEndHour, state.quietHoursEndMinute)))
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
                            Text(stringResource(R.string.safeguard_cooldown_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.safeguard_cooldown_desc),
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
                        listOf(5L, 10L, 15L, 30L).forEach { minutes ->
                            FilterChip(
                                selected = state.cooldownMinutes == minutes,
                                onClick = { onSetCooldownMinutes(minutes) },
                                label = { Text(stringResource(R.string.safeguard_cooldown_mins, minutes)) }
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
                            Text(stringResource(R.string.safeguard_defer_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.safeguard_defer_desc),
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
                            Text(stringResource(R.string.safeguard_fair_shuffle_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.safeguard_fair_shuffle_desc),
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
                        text = stringResource(R.string.safeguard_fair_shuffle_capacity_label),
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
                                label = { Text(stringResource(R.string.safeguard_fair_shuffle_capacity_items, capacity)) }
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
                            text = stringResource(R.string.safeguard_fair_shuffle_status, state.fairShuffleRecordedCount, state.fairShuffleCapacity),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = onResetFairShuffleDeck,
                            enabled = state.fairShuffleRecordedCount > 0,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.safeguard_fair_shuffle_reset), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
