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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import foo.barz.wallpaperpicker.ui.tabs.DashboardTab
import foo.barz.wallpaperpicker.ui.tabs.HistoryTab
import foo.barz.wallpaperpicker.ui.tabs.MainTab
import foo.barz.wallpaperpicker.ui.tabs.SettingsTab
import foo.barz.wallpaperpicker.ui.tabs.SourcesTab
import foo.barz.wallpaperpicker.ui.tabs.settings.SettingsSubPage

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
    onMediaStoreAlbumsSelected: (Set<String>, List<MediaStoreAlbum>) -> Unit = { _, _ -> },
    onToggleSourceEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteSource: (String) -> Unit = {},
    onOpenAddSource: () -> Unit = {},
    onOpenEditSource: (String) -> Unit = {},
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
    onImmichAlbumsSelected: (Set<String>, List<ImmichAlbum>) -> Unit = { _, _ -> },
    onImmichQualitySelected: (ImmichQuality) -> Unit,
    onToggleImmichIgnoreSsl: (Boolean) -> Unit,
    onToggleImmichWifiOnly: (Boolean) -> Unit,
    onFetchImmichAlbums: () -> Unit,
    onToggleCompositeSource: (WallpaperSourceType, Boolean) -> Unit = { _, _ -> },
    onClearCache: () -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onCropModeSelected: (WallpaperCropMode) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onToggleReapplyOnScrollChange: (Boolean) -> Unit,
    onReapplyCurrentWallpaper: () -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onToggleIntervalSchedule: (Boolean) -> Unit = {},
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
    onToggleFavoriteCurrent: () -> Unit = {},
    onApplyWallpaperFromHistory: (WallpaperHistoryItem) -> Unit = {},
    onToggleFavorite: (WallpaperHistoryItem) -> Unit = {},
    onDeleteHistoryItem: (WallpaperHistoryItem) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onClearInvalidHistory: () -> Unit = {},
    onRedownloadHistoryItem: (WallpaperHistoryItem) -> Unit = {},
    onOpenCustomUriInGallery: (Uri) -> Unit = {},
    onShareCustomWallpaper: (Uri, String?) -> Unit = { _, _ -> },
    onSaveCustomWallpaper: (Uri, String?) -> Unit = { _, _ -> },
    onUpdateCurrentWallpaperPreferences: (
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    onUpdateWallpaperPreferences: (
        item: WallpaperHistoryItem,
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },
    onExportFavorites: () -> Unit = {},
    onOpenManageSpace: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onToggleRuleEngine: (Boolean) -> Unit = {},
    onOpenRuleDialog: (foo.barz.wallpaperpicker.core.model.ScheduleRule?) -> Unit = {},
    onCloseRuleDialog: () -> Unit = {},
    onSaveScheduleRule: (foo.barz.wallpaperpicker.core.model.ScheduleRule) -> Unit = {},
    onDeleteScheduleRule: (String) -> Unit = {},
    onToggleScheduleRuleEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onPopulateDefaultRules: () -> Unit = {},
    targetTab: MainTab? = null,
    onTabNavigated: () -> Unit = {},
    onChangeNow: () -> Unit,
    onClearStatus: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    var settingsSubPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }
    var isHistorySheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(targetTab) {
        if (targetTab != null) {
            selectedTab = targetTab
            settingsSubPage = null
            onTabNavigated()
        }
    }

    LaunchedEffect(state.statusMessage, isHistorySheetOpen) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank() && !isHistorySheetOpen) {
            snackbarHostState.showSnackbar(msg)
            onClearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val currentTitle = if (selectedTab == MainTab.SETTINGS && settingsSubPage != null) {
                        settingsSubPage!!.title
                    } else {
                        selectedTab.title
                    }
                    AnimatedContent(
                        targetState = currentTitle,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 150))
                        },
                        label = "TopAppBarTitleAnimation"
                    ) { title ->
                        Text(title)
                    }
                },
                navigationIcon = {
                    if (selectedTab == MainTab.SETTINGS && settingsSubPage != null) {
                        IconButton(onClick = { settingsSubPage = null }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回设置"
                            )
                        }
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
                        onClick = {
                            selectedTab = tab
                            if (tab != MainTab.SETTINGS) {
                                settingsSubPage = null
                            }
                        },
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
                        onUpdateCurrentWallpaperPreferences = onUpdateCurrentWallpaperPreferences,
                        onChangeNow = onChangeNow
                    )

                    MainTab.HISTORY -> HistoryTab(
                        state = state,
                        onApplyWallpaper = onApplyWallpaperFromHistory,
                        onToggleFavorite = onToggleFavorite,
                        onDeleteHistoryItem = onDeleteHistoryItem,
                        onClearHistory = onClearHistory,
                        onClearInvalidHistory = onClearInvalidHistory,
                        onRedownloadHistoryItem = onRedownloadHistoryItem,
                        onOpenInGallery = onOpenCustomUriInGallery,
                        onShareWallpaper = onShareCustomWallpaper,
                        onSaveToGallery = onSaveCustomWallpaper,
                        onUpdateWallpaperPreferences = onUpdateWallpaperPreferences,
                        onClearStatus = onClearStatus,
                        onSheetActiveChanged = { isHistorySheetOpen = it }
                    )

                    MainTab.SOURCES -> SourcesTab(
                        state = state,
                        onToggleSourceEnabled = onToggleSourceEnabled,
                        onDeleteSource = onDeleteSource,
                        onOpenAddSource = onOpenAddSource,
                        onOpenEditSource = onOpenEditSource
                    )

                    MainTab.SETTINGS -> SettingsTab(
                        state = state,
                        currentSubPage = settingsSubPage,
                        onNavigateToSubPage = { settingsSubPage = it },
                        onTargetSelected = onTargetSelected,
                        onCropModeSelected = onCropModeSelected,
                        onScrollModeSelected = onScrollModeSelected,
                        onToggleReapplyOnScrollChange = onToggleReapplyOnScrollChange,
                        onReapplyCurrentWallpaper = onReapplyCurrentWallpaper,
                        onToggleSchedule = onToggleSchedule,
                        onToggleIntervalSchedule = onToggleIntervalSchedule,
                        onIntervalSelected = onIntervalSelected,
                        onToggleExactTimer = onToggleExactTimer,
                        onToggleDailyAnchor = onToggleDailyAnchor,
                        onSetDailyAnchorTime = onSetDailyAnchorTime,
                        onAddDailyAnchorTime = onAddDailyAnchorTime,
                        onRemoveDailyAnchorTime = onRemoveDailyAnchorTime,
                        onToggleScreenOffTrigger = onToggleScreenOffTrigger,
                        onSetScreenOffDelaySeconds = onSetScreenOffDelaySeconds,
                        onToggleQuietHours = onToggleQuietHours,
                        onSetQuietHours = onSetQuietHours,
                        onToggleCooldownSuppression = onToggleCooldownSuppression,
                        onSetCooldownMinutes = onSetCooldownMinutes,
                        onToggleDeferDuringInteraction = onToggleDeferDuringInteraction,
                        onToggleFairShuffle = onToggleFairShuffle,
                        onFairShuffleCapacitySelected = onFairShuffleCapacitySelected,
                        onResetFairShuffleDeck = onResetFairShuffleDeck,
                        onCacheSizeTierSelected = onCacheSizeTierSelected,
                        onWidgetScaleTypeSelected = onWidgetScaleTypeSelected,
                        onClearCache = onClearCache,
                        onExportFavorites = onExportFavorites,
                        onOpenManageSpace = onOpenManageSpace,
                        onOpenAbout = onOpenAbout,
                        onToggleRuleEngine = onToggleRuleEngine,
                        onOpenRuleDialog = onOpenRuleDialog,
                        onCloseRuleDialog = onCloseRuleDialog,
                        onSaveScheduleRule = onSaveScheduleRule,
                        onDeleteScheduleRule = onDeleteScheduleRule,
                        onToggleScheduleRuleEnabled = onToggleScheduleRuleEnabled,
                        onPopulateDefaultRules = onPopulateDefaultRules
                    )
                }
            }
        }
    }
}
