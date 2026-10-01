package foo.barz.wallpaperpicker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.viewModels
import foo.barz.wallpaperpicker.ui.AboutActivity
import foo.barz.wallpaperpicker.ui.MainScreen
import foo.barz.wallpaperpicker.ui.MainViewModel
import foo.barz.wallpaperpicker.ui.ManageSpaceActivity
import foo.barz.wallpaperpicker.ui.SourceConfigActivity
import foo.barz.wallpaperpicker.ui.tabs.MainTab
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val targetTabState = mutableStateOf<MainTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper.updateDynamicShortcuts(this)

        setContent {
            WallpaperPickerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val targetTab by targetTabState

                MainScreen(
                    state = uiState,
                    targetTab = targetTab,
                    onTabNavigated = { targetTabState.value = null },
                    onSourceTypeSelected = viewModel::onSourceTypeSelected,
                    onFolderSelected = viewModel::onFolderSelected,
                    onRescanFolder = { viewModel.rescanFolder() },
                    onFetchMediaStoreAlbums = viewModel::fetchMediaStoreAlbums,
                    onMediaStoreAlbumSelected = viewModel::onMediaStoreAlbumSelected,
                    onMediaStoreAlbumsSelected = viewModel::onMediaStoreAlbumsSelected,
                    onToggleSourceEnabled = viewModel::onToggleSourceEnabled,
                    onDeleteSource = viewModel::onDeleteSource,
                    onOpenAddSource = {
                        startActivity(SourceConfigActivity.createIntent(this))
                    },
                    onOpenEditSource = { sourceId ->
                        startActivity(SourceConfigActivity.createIntent(this, sourceId))
                    },
                    onOpenInGallery = viewModel::openCurrentWallpaperInGallery,
                    onShareWallpaper = viewModel::shareCurrentWallpaper,
                    onSaveToGallery = viewModel::saveCurrentWallpaperToGallery,
                    onHttpPresetSelected = viewModel::onHttpPresetSelected,
                    onHttpCustomUrlChanged = viewModel::onHttpCustomUrlChanged,
                    onHttpCustomJsonPathChanged = viewModel::onHttpCustomJsonPathChanged,
                    onToggleWifiOnly = viewModel::onToggleWifiOnly,
                    onImmichServerUrlChanged = viewModel::onImmichServerUrlChanged,
                    onImmichApiKeyChanged = viewModel::onImmichApiKeyChanged,
                    onImmichAlbumSelected = viewModel::onImmichAlbumSelected,
                    onImmichAlbumsSelected = viewModel::onImmichAlbumsSelected,
                    onImmichQualitySelected = viewModel::onImmichQualitySelected,
                    onToggleImmichIgnoreSsl = viewModel::onToggleImmichIgnoreSsl,
                    onToggleImmichWifiOnly = viewModel::onToggleImmichWifiOnly,
                    onFetchImmichAlbums = viewModel::fetchImmichAlbums,
                    onToggleCompositeSource = viewModel::onToggleCompositeSource,
                    onClearCache = viewModel::onClearCache,
                    onIntervalSelected = viewModel::onIntervalSelected,
                    onTargetSelected = viewModel::onTargetSelected,
                    onCropModeSelected = viewModel::onCropModeSelected,
                    onScrollModeSelected = viewModel::onScrollModeSelected,
                    onLockScreenStrategySelected = viewModel::onLockScreenStrategySelected,
                    onToggleReapplyOnScrollChange = viewModel::onToggleReapplyOnScrollChange,
                    onReapplyCurrentWallpaper = viewModel::reapplyCurrentWallpaper,
                    onToggleSchedule = viewModel::toggleSchedule,
                    onToggleIntervalSchedule = viewModel::onToggleIntervalSchedule,
                    onToggleExactTimer = viewModel::onToggleExactTimer,
                    onToggleDailyAnchor = viewModel::onToggleDailyAnchor,
                    onSetDailyAnchorTime = viewModel::onSetDailyAnchorTime,
                    onAddDailyAnchorTime = viewModel::onAddDailyAnchorTime,
                    onRemoveDailyAnchorTime = viewModel::onRemoveDailyAnchorTime,
                    onToggleScreenOffTrigger = viewModel::onToggleScreenOffTrigger,
                    onSetScreenOffDelaySeconds = viewModel::onSetScreenOffDelaySeconds,
                    onToggleQuietHours = viewModel::onToggleQuietHours,
                    onSetQuietHours = viewModel::onSetQuietHours,
                    onToggleCooldownSuppression = viewModel::onToggleCooldownSuppression,
                    onSetCooldownMinutes = viewModel::onSetCooldownMinutes,
                    onToggleDeferDuringInteraction = viewModel::onToggleDeferDuringInteraction,
                    onToggleFairShuffle = viewModel::onToggleFairShuffle,
                    onFairShuffleCapacitySelected = viewModel::onFairShuffleCapacitySelected,
                    onResetFairShuffleDeck = viewModel::onResetFairShuffleDeck,
                    onCacheSizeTierSelected = viewModel::onCacheSizeTierSelected,
                    onWidgetScaleTypeSelected = viewModel::onWidgetScaleTypeSelected,
                    onToggleFavoriteCurrent = viewModel::toggleFavoriteCurrent,
                    onApplyWallpaperFromHistory = viewModel::applyWallpaperFromHistory,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDeleteHistoryItem = viewModel::deleteHistoryItem,
                    onClearHistory = viewModel::clearUnfavoritedHistory,
                    onClearInvalidHistory = viewModel::clearInvalidHistory,
                    onRedownloadHistoryItem = viewModel::redownloadHistoryItem,
                    onBatchRedownloadMissingHistory = viewModel::batchRedownloadMissingHistory,
                    onOpenCustomUriInGallery = viewModel::openUriInGallery,
                    onShareCustomWallpaper = viewModel::shareUri,
                    onSaveCustomWallpaper = viewModel::saveUriToGallery,
                    onUpdateCurrentWallpaperPreferences = viewModel::updateCurrentWallpaperPreferences,
                    onUpdateWallpaperPreferences = viewModel::updateWallpaperPreferences,
                    onExportFavorites = viewModel::exportAllFavoritesToGallery,
                    onOpenManageSpace = {
                        startActivity(Intent(this, ManageSpaceActivity::class.java))
                    },
                    onOpenAbout = {
                        startActivity(Intent(this, AboutActivity::class.java))
                    },
                    onToggleRuleEngine = viewModel::onToggleRuleEngine,
                    onOpenRuleDialog = viewModel::onOpenRuleDialog,
                    onCloseRuleDialog = viewModel::onCloseRuleDialog,
                    onSaveScheduleRule = viewModel::onSaveScheduleRule,
                    onDeleteScheduleRule = viewModel::onDeleteScheduleRule,
                    onToggleScheduleRuleEnabled = viewModel::onToggleScheduleRuleEnabled,
                    onPopulateDefaultRules = viewModel::onPopulateDefaultRules,
                    hasSensitiveData = viewModel.hasSensitiveDataForBackup(),
                    isOpenPgpAvailable = viewModel.isOpenPgpProviderAvailable(),
                    onExportBackup = viewModel::exportBackup,
                    onExportBackupWithOpenPgp = viewModel::exportBackupWithOpenPgp,
                    onDetectBackupFormat = viewModel::detectBackupFileFormat,
                    onCheckIsEncryptedBackup = viewModel::checkIsEncryptedBackup,
                    onRestoreBackup = viewModel::restoreBackup,
                    onRestoreBackupWithOpenPgp = viewModel::restoreBackupWithOpenPgp,
                    onDismissMissingFavoritesPrompt = viewModel::dismissMissingFavoritesPrompt,
                    onConfirmBatchDownloadMissingFavorites = viewModel::confirmBatchDownloadMissingFavorites,
                    onBatchRedownloadMissingFavorites = viewModel::batchRedownloadMissingFavorites,
                    onDismissRestoreSummary = viewModel::dismissRestoreSummary,
                    onChangeNow = viewModel::changeNow,
                    onClearStatus = viewModel::clearStatusMessage
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromBackground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            targetTabState.value = MainTab.DASHBOARD
        }
    }
}
