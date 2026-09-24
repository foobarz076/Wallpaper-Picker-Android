package foo.barz.wallpaperpicker.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.ui.tabs.DashboardTab
import foo.barz.wallpaperpicker.ui.tabs.HistoryTab
import foo.barz.wallpaperpicker.ui.tabs.MainTab
import foo.barz.wallpaperpicker.ui.tabs.SettingsTab
import foo.barz.wallpaperpicker.ui.tabs.SourcesTab

/**
 * Root screen orchestrating top-level scaffold, top app bar, navigation bar, and tab routing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onSourceTypeSelected: (WallpaperSourceType) -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onRescanFolder: () -> Unit,
    onFetchMediaStoreAlbums: () -> Unit,
    onMediaStoreAlbumSelected: (MediaStoreAlbum?) -> Unit,
    onOpenInGallery: () -> Unit,
    onShareWallpaper: () -> Unit,
    onSaveToGallery: () -> Unit,
    onHttpPresetSelected: (HttpPresetType) -> Unit,
    onHttpCustomUrlChanged: (String) -> Unit,
    onHttpCustomJsonPathChanged: (String) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onImmichServerUrlChanged: (String) -> Unit,
    onImmichApiKeyChanged: (String) -> Unit,
    onImmichAlbumSelected: (ImmichAlbum?) -> Unit,
    onImmichQualitySelected: (ImmichQuality) -> Unit,
    onToggleImmichIgnoreSsl: (Boolean) -> Unit,
    onToggleImmichWifiOnly: (Boolean) -> Unit,
    onFetchImmichAlbums: () -> Unit,
    onClearCache: () -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onCropModeSelected: (WallpaperCropMode) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onToggleReapplyOnScrollChange: (Boolean) -> Unit,
    onReapplyCurrentWallpaper: () -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onToggleDeferDuringInteraction: (Boolean) -> Unit,
    onToggleFairShuffle: (Boolean) -> Unit,
    onToggleFavoriteCurrent: () -> Unit = {},
    onApplyWallpaperFromHistory: (WallpaperHistoryItem) -> Unit = {},
    onToggleFavorite: (WallpaperHistoryItem) -> Unit = {},
    onDeleteHistoryItem: (WallpaperHistoryItem) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onOpenCustomUriInGallery: (Uri) -> Unit = {},
    onShareCustomWallpaper: (Uri, String?) -> Unit = { _, _ -> },
    onSaveCustomWallpaper: (Uri, String?) -> Unit = { _, _ -> },
    onExportFavorites: () -> Unit = {},
    onOpenManageSpace: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onChangeNow: () -> Unit,
    onClearStatus: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = selectedTab.title,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 150))
                        },
                        label = "TopAppBarTitleAnimation"
                    ) { title ->
                        Text(title)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        // Moving forward: slide in from right, slide out to left
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ) { width -> (width * 0.15f).toInt() } + fadeIn(
                            animationSpec = tween(durationMillis = 260)
                        )) togetherWith (slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) { width -> -(width * 0.15f).toInt() } + fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        ))
                    } else {
                        // Moving backward: slide in from left, slide out to right
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ) { width -> -(width * 0.15f).toInt() } + fadeIn(
                            animationSpec = tween(durationMillis = 260)
                        )) togetherWith (slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) { width -> (width * 0.15f).toInt() } + fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        ))
                    }
                },
                label = "TabContentAnimation"
            ) { currentTab ->
                when (currentTab) {
                    MainTab.DASHBOARD -> DashboardTab(
                        state = state,
                        onOpenInGallery = onOpenInGallery,
                        onShareWallpaper = onShareWallpaper,
                        onSaveToGallery = onSaveToGallery,
                        onToggleFavoriteCurrent = onToggleFavoriteCurrent,
                        onChangeNow = onChangeNow
                    )

                    MainTab.HISTORY -> HistoryTab(
                        state = state,
                        onApplyWallpaper = onApplyWallpaperFromHistory,
                        onToggleFavorite = onToggleFavorite,
                        onDeleteHistoryItem = onDeleteHistoryItem,
                        onClearHistory = onClearHistory,
                        onOpenInGallery = onOpenCustomUriInGallery,
                        onShareWallpaper = onShareCustomWallpaper,
                        onSaveToGallery = onSaveCustomWallpaper
                    )

                    MainTab.SOURCES -> SourcesTab(
                        state = state,
                        onSourceTypeSelected = onSourceTypeSelected,
                        onFolderSelected = onFolderSelected,
                        onRescanFolder = onRescanFolder,
                        onFetchMediaStoreAlbums = onFetchMediaStoreAlbums,
                        onMediaStoreAlbumSelected = onMediaStoreAlbumSelected,
                        onHttpPresetSelected = onHttpPresetSelected,
                        onHttpCustomUrlChanged = onHttpCustomUrlChanged,
                        onHttpCustomJsonPathChanged = onHttpCustomJsonPathChanged,
                        onToggleWifiOnly = onToggleWifiOnly,
                        onImmichServerUrlChanged = onImmichServerUrlChanged,
                        onImmichApiKeyChanged = onImmichApiKeyChanged,
                        onImmichAlbumSelected = onImmichAlbumSelected,
                        onImmichQualitySelected = onImmichQualitySelected,
                        onToggleImmichIgnoreSsl = onToggleImmichIgnoreSsl,
                        onToggleImmichWifiOnly = onToggleImmichWifiOnly,
                        onFetchImmichAlbums = onFetchImmichAlbums,
                        onClearCache = onClearCache
                    )

                    MainTab.SETTINGS -> SettingsTab(
                        state = state,
                        onTargetSelected = onTargetSelected,
                        onCropModeSelected = onCropModeSelected,
                        onScrollModeSelected = onScrollModeSelected,
                        onToggleReapplyOnScrollChange = onToggleReapplyOnScrollChange,
                        onReapplyCurrentWallpaper = onReapplyCurrentWallpaper,
                        onToggleSchedule = onToggleSchedule,
                        onIntervalSelected = onIntervalSelected,
                        onToggleDeferDuringInteraction = onToggleDeferDuringInteraction,
                        onToggleFairShuffle = onToggleFairShuffle,
                        onClearCache = onClearCache,
                        onExportFavorites = onExportFavorites,
                        onOpenManageSpace = onOpenManageSpace,
                        onOpenAbout = onOpenAbout
                    )
                }
            }
        }
    }
}
