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
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
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
    onRequestRestoreBackup: () -> Unit = {},
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

        // Fresh install onboarding banner to guide user to restore backup
        if (state.isOnboardingRestoreVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestRestoreBackup() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_onboarding_restore_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_onboarding_restore_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

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
                            text = stringResource(R.string.settings_battery_banner_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_battery_banner_desc),
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
                    "${stringResource(state.target.labelRes)} · ${stringResource(state.cropMode.labelRes)} · ${stringResource(state.scrollMode.labelRes)}"
                }
                SettingsSubPage.SCHEDULING -> {
                    if (!state.isScheduled) {
                        stringResource(R.string.settings_summary_scheduling_stopped)
                    } else if (state.ruleEngineEnabled) {
                        val activeCount = state.scheduleRules.count { it.isEnabled }
                        stringResource(R.string.settings_summary_scheduling_rules_format, activeCount)
                    } else {
                        val triggers = mutableListOf<String>()
                        if (state.intervalScheduleEnabled) triggers.add(stringResource(R.string.settings_summary_trigger_interval_format, state.intervalMinutes))
                        if (state.dailyAnchorEnabled) triggers.add(stringResource(R.string.settings_summary_trigger_anchors_format, state.dailyAnchorTimes.size))
                        if (state.screenOffTriggerEnabled) triggers.add(stringResource(R.string.settings_summary_trigger_screen_off))
                        if (triggers.isEmpty()) stringResource(R.string.settings_summary_scheduling_standby) else stringResource(R.string.settings_summary_scheduling_active_format, triggers.joinToString(" + "))
                    }
                }
                SettingsSubPage.SAFEGUARDS -> {
                    val quietText = if (state.quietHoursEnabled) stringResource(R.string.settings_summary_quiet_active) else stringResource(R.string.settings_summary_quiet_inactive)
                    val shuffleText = if (state.fairShuffle) stringResource(R.string.settings_summary_smart_shuffle_format, state.fairShuffleCapacity) else stringResource(R.string.settings_summary_pure_random)
                    "$quietText · $shuffleText"
                }
                SettingsSubPage.WIDGETS -> {
                    val scaleText = stringResource(state.widgetScaleType.displayNameRes)
                    stringResource(R.string.settings_summary_widget_format, scaleText)
                }
                SettingsSubPage.STORAGE -> {
                    val cacheText = SettingsHelpers.formatFileSize(state.cacheSizeBytes)
                    val tierLabel = stringResource(state.cacheSizeTier.displayNameRes)
                    stringResource(R.string.settings_summary_storage_format, cacheText, tierLabel, state.favoritesList.size)
                }
                SettingsSubPage.ABOUT -> {
                    val batteryStatus = if (isIgnoringBatteryOptimizations) stringResource(R.string.settings_summary_battery_ready) else stringResource(R.string.settings_summary_battery_needed)
                    stringResource(R.string.settings_summary_about_format, batteryStatus)
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
                            text = stringResource(subPage.titleRes),
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
                            text = stringResource(subPage.descriptionRes),
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
