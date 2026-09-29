package foo.barz.wallpaperpicker.ui.tabs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.backup.BackupFormat
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.core.model.LockScreenStrategy
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import foo.barz.wallpaperpicker.ui.MainUiState
import foo.barz.wallpaperpicker.ui.components.ScheduleRuleEditDialog
import foo.barz.wallpaperpicker.ui.tabs.settings.AboutSettingsSubPage
import foo.barz.wallpaperpicker.ui.tabs.settings.DisplaySettingsSubPage
import foo.barz.wallpaperpicker.ui.tabs.settings.SafeguardsSettingsSubPage
import foo.barz.wallpaperpicker.ui.tabs.settings.SchedulingSettingsSubPage
import foo.barz.wallpaperpicker.ui.tabs.settings.SettingsHelpers
import foo.barz.wallpaperpicker.ui.tabs.settings.SettingsRootContent
import foo.barz.wallpaperpicker.ui.tabs.settings.SettingsSubPage
import foo.barz.wallpaperpicker.ui.tabs.settings.StorageSettingsSubPage
import foo.barz.wallpaperpicker.ui.tabs.settings.WidgetsSettingsSubPage

/**
 * Settings tab orchestrating normalized sub-pages: Display, Scheduling, Safeguards,
 * Widgets, Storage, and About / Diagnostics.
 */
@Composable
fun SettingsTab(
    state: MainUiState,
    currentSubPage: SettingsSubPage? = null,
    onNavigateToSubPage: (SettingsSubPage?) -> Unit = {},
    onTargetSelected: (WallpaperTarget) -> Unit,
    onCropModeSelected: (WallpaperCropMode) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onLockScreenStrategySelected: (LockScreenStrategy) -> Unit = {},
    onToggleReapplyOnScrollChange: (Boolean) -> Unit,
    onReapplyCurrentWallpaper: () -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onToggleIntervalSchedule: (Boolean) -> Unit = {},
    onIntervalSelected: (Long) -> Unit,
    onToggleExactTimer: (Boolean) -> Unit = {},
    onToggleDailyAnchor: (Boolean) -> Unit = {},
    onSetDailyAnchorTime: (Int, Int) -> Unit = { _, _ -> },
    onAddDailyAnchorTime: (Int, Int) -> Unit = { _, _ -> },
    onRemoveDailyAnchorTime: (String) -> Unit = {},
    onToggleScreenOffTrigger: (Boolean) -> Unit = {},
    onSetScreenOffDelaySeconds: (Int) -> Unit = {},
    onToggleQuietHours: (Boolean) -> Unit = {},
    onSetQuietHours: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onToggleCooldownSuppression: (Boolean) -> Unit = {},
    onSetCooldownMinutes: (Long) -> Unit = {},
    onToggleDeferDuringInteraction: (Boolean) -> Unit,
    onToggleFairShuffle: (Boolean) -> Unit,
    onFairShuffleCapacitySelected: (Int) -> Unit = {},
    onResetFairShuffleDeck: () -> Unit = {},
    onCacheSizeTierSelected: (CacheSizeTier) -> Unit = {},
    onWidgetScaleTypeSelected: (WidgetScaleType) -> Unit = {},
    onClearCache: () -> Unit = {},
    onExportFavorites: () -> Unit = {},
    onOpenManageSpace: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onToggleRuleEngine: (Boolean) -> Unit = {},
    onOpenRuleDialog: (ScheduleRule?) -> Unit = {},
    onCloseRuleDialog: () -> Unit = {},
    onSaveScheduleRule: (ScheduleRule) -> Unit = {},
    onDeleteScheduleRule: (String) -> Unit = {},
    onToggleScheduleRuleEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onPopulateDefaultRules: () -> Unit = {},
    hasSensitiveData: Boolean = false,
    isOpenPgpAvailable: Boolean = false,
    onExportBackup: (android.net.Uri, String?, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onExportBackupWithOpenPgp: (android.net.Uri, Boolean, Boolean, Boolean, Intent?, ((PendingIntent) -> Unit)) -> Unit = { _, _, _, _, _, _ -> },
    onDetectBackupFormat: (android.net.Uri) -> BackupFormat = { BackupFormat.PLAINTEXT },
    onCheckIsEncryptedBackup: (android.net.Uri) -> Boolean = { false },
    onRestoreBackup: (android.net.Uri, String?) -> Unit = { _, _ -> },
    onRestoreBackupWithOpenPgp: (android.net.Uri, Intent?, ((PendingIntent) -> Unit)) -> Unit = { _, _, _ -> },
    onDismissMissingFavoritesPrompt: () -> Unit = {},
    onConfirmBatchDownloadMissingFavorites: () -> Unit = {},
    onBatchRedownloadMissingFavorites: () -> Unit = {},
    onDismissRestoreSummary: () -> Unit = {},
    onNavigateToSources: () -> Unit = {},
    onRequestRestoreBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var localSubPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }
    val activeSubPage = currentSubPage ?: localSubPage
    val navigate: (SettingsSubPage?) -> Unit = { target ->
        localSubPage = target
        onNavigateToSubPage(target)
    }

    // Intercept back gesture when inside a subpage
    BackHandler(enabled = activeSubPage != null) {
        navigate(null)
    }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(SettingsHelpers.checkBatteryOptimization(context))
    }
    var showBatteryOptimizationPrompt by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = SettingsHelpers.checkBatteryOptimization(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AnimatedContent(
        targetState = activeSubPage,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                // Entering sub-page from root: slide in from right
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) { width -> (width * 0.15f).toInt() } + fadeIn(
                    animationSpec = tween(durationMillis = 240)
                )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) { width -> -(width * 0.15f).toInt() } + fadeOut(
                    animationSpec = tween(durationMillis = 180)
                ))
            } else {
                // Returning to root or navigating back: slide in from left
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) { width -> -(width * 0.15f).toInt() } + fadeIn(
                    animationSpec = tween(durationMillis = 240)
                )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) { width -> (width * 0.15f).toInt() } + fadeOut(
                    animationSpec = tween(durationMillis = 180)
                ))
            }
        },
        label = "SettingsSubPageTransition",
        modifier = modifier.fillMaxSize()
    ) { subPage ->
        when (subPage) {
            null -> SettingsRootContent(
                state = state,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onNavigateToSubPage = navigate,
                onRequestRestoreBackup = onRequestRestoreBackup
            )

            SettingsSubPage.DISPLAY -> DisplaySettingsSubPage(
                state = state,
                onTargetSelected = onTargetSelected,
                onCropModeSelected = onCropModeSelected,
                onScrollModeSelected = onScrollModeSelected,
                onLockScreenStrategySelected = onLockScreenStrategySelected,
                onToggleReapplyOnScrollChange = onToggleReapplyOnScrollChange,
                onReapplyCurrentWallpaper = onReapplyCurrentWallpaper
            )

            SettingsSubPage.SCHEDULING -> SchedulingSettingsSubPage(
                state = state,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onToggleSchedule = onToggleSchedule,
                onToggleRuleEngine = onToggleRuleEngine,
                onToggleIntervalSchedule = onToggleIntervalSchedule,
                onIntervalSelected = onIntervalSelected,
                onToggleDailyAnchor = onToggleDailyAnchor,
                onAddDailyAnchorTime = onAddDailyAnchorTime,
                onRemoveDailyAnchorTime = onRemoveDailyAnchorTime,
                onToggleScreenOffTrigger = onToggleScreenOffTrigger,
                onSetScreenOffDelaySeconds = onSetScreenOffDelaySeconds,
                onToggleExactTimer = onToggleExactTimer,
                onRequestBatteryOptimizationPrompt = { showBatteryOptimizationPrompt = true },
                onOpenRuleDialog = onOpenRuleDialog,
                onDeleteScheduleRule = onDeleteScheduleRule,
                onToggleScheduleRuleEnabled = onToggleScheduleRuleEnabled,
                onPopulateDefaultRules = onPopulateDefaultRules
            )

            SettingsSubPage.SAFEGUARDS -> SafeguardsSettingsSubPage(
                state = state,
                onToggleQuietHours = onToggleQuietHours,
                onSetQuietHours = onSetQuietHours,
                onToggleCooldownSuppression = onToggleCooldownSuppression,
                onSetCooldownMinutes = onSetCooldownMinutes,
                onToggleDeferDuringInteraction = onToggleDeferDuringInteraction,
                onToggleFairShuffle = onToggleFairShuffle,
                onFairShuffleCapacitySelected = onFairShuffleCapacitySelected,
                onResetFairShuffleDeck = onResetFairShuffleDeck
            )

            SettingsSubPage.WIDGETS -> WidgetsSettingsSubPage(
                state = state,
                onWidgetScaleTypeSelected = onWidgetScaleTypeSelected
            )

            SettingsSubPage.STORAGE -> StorageSettingsSubPage(
                state = state,
                onCacheSizeTierSelected = onCacheSizeTierSelected,
                onExportFavorites = onExportFavorites,
                onOpenManageSpace = onOpenManageSpace,
                hasSensitiveData = hasSensitiveData,
                isOpenPgpAvailable = isOpenPgpAvailable,
                onExportBackup = onExportBackup,
                onExportBackupWithOpenPgp = onExportBackupWithOpenPgp,
                onDetectBackupFormat = onDetectBackupFormat,
                onRestoreBackup = onRestoreBackup,
                onRestoreBackupWithOpenPgp = onRestoreBackupWithOpenPgp,
                onDismissMissingFavoritesPrompt = onDismissMissingFavoritesPrompt,
                onConfirmBatchDownloadMissingFavorites = onConfirmBatchDownloadMissingFavorites,
                onBatchRedownloadMissingFavorites = onBatchRedownloadMissingFavorites,
                onDismissRestoreSummary = onDismissRestoreSummary,
                onNavigateToSources = onNavigateToSources,
                onRequestRestoreBackup = onRequestRestoreBackup
            )

            SettingsSubPage.ABOUT -> AboutSettingsSubPage(
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onOpenAbout = onOpenAbout
            )
        }
    }

    if (showBatteryOptimizationPrompt) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationPrompt = false },
            icon = {
                Icon(
                    Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.battery_dialog_title)) },
            text = {
                Text(stringResource(R.string.battery_dialog_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryOptimizationPrompt = false
                        SettingsHelpers.openAppBatteryDetailsSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.battery_dialog_btn_settings))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showBatteryOptimizationPrompt = false
                            onToggleExactTimer(true)
                        }
                    ) {
                        Text(stringResource(R.string.battery_dialog_btn_enable_anyway))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { showBatteryOptimizationPrompt = false }
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }

    if (state.isRuleDialogOpen) {
        ScheduleRuleEditDialog(
            initialRule = state.editingRule,
            availableSources = state.sourcesList,
            onDismissRequest = onCloseRuleDialog,
            onSaveRule = onSaveScheduleRule
        )
    }
}
