package foo.barz.wallpaperpicker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.viewModels
import foo.barz.wallpaperpicker.ui.AboutActivity
import foo.barz.wallpaperpicker.ui.MainScreen
import foo.barz.wallpaperpicker.ui.MainViewModel
import foo.barz.wallpaperpicker.ui.ManageSpaceActivity
import foo.barz.wallpaperpicker.ui.SourceConfigActivity
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper.updateDynamicShortcuts(this)

        setContent {
            WallpaperPickerTheme {
                val uiState by viewModel.uiState.collectAsState()

                MainScreen(
                    state = uiState,
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
                    onToggleReapplyOnScrollChange = viewModel::onToggleReapplyOnScrollChange,
                    onReapplyCurrentWallpaper = viewModel::reapplyCurrentWallpaper,
                    onToggleSchedule = viewModel::toggleSchedule,
                    onToggleDeferDuringInteraction = viewModel::onToggleDeferDuringInteraction,
                    onToggleFairShuffle = viewModel::onToggleFairShuffle,
                    onWidgetScaleTypeSelected = viewModel::onWidgetScaleTypeSelected,
                    onToggleFavoriteCurrent = viewModel::toggleFavoriteCurrent,
                    onApplyWallpaperFromHistory = viewModel::applyWallpaperFromHistory,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDeleteHistoryItem = viewModel::deleteHistoryItem,
                    onClearHistory = viewModel::clearUnfavoritedHistory,
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
}
