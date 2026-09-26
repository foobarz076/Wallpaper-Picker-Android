package foo.barz.wallpaperpicker.ui.tabs

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.MainUiState
import foo.barz.wallpaperpicker.ui.components.TopFloatingPillNotification
import foo.barz.wallpaperpicker.ui.components.WallpaperAdjustmentSheet
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistorySubTab {
    HISTORY,
    FAVORITES
}

/**
 * History and Favorites tab presenting a gallery grid of previously applied wallpapers
 * and user-curated permanent offline favorites.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    state: MainUiState,
    onApplyWallpaper: (WallpaperHistoryItem) -> Unit,
    onToggleFavorite: (WallpaperHistoryItem) -> Unit,
    onDeleteHistoryItem: (WallpaperHistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onClearInvalidHistory: () -> Unit = {},
    onRedownloadHistoryItem: (WallpaperHistoryItem) -> Unit = {},
    onOpenInGallery: (Uri) -> Unit,
    onShareWallpaper: (Uri, String?) -> Unit,
    onSaveToGallery: (Uri, String?) -> Unit,
    onUpdateWallpaperPreferences: (
        item: WallpaperHistoryItem,
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },
    onClearStatus: () -> Unit = {},
    onSheetActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var subTab by rememberSaveable { mutableStateOf(HistorySubTab.HISTORY) }
    var selectedItemForDetail by remember { mutableStateOf<WallpaperHistoryItem?>(null) }
    var selectedItemForAdjustment by remember { mutableStateOf<WallpaperHistoryItem?>(null) }
    var showHistoryActionMenu by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<WallpaperHistoryItem?>(null) }
    var showClearInvalidConfirmDialog by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isAnySheetOpen = selectedItemForDetail != null || selectedItemForAdjustment != null
    LaunchedEffect(isAnySheetOpen) {
        onSheetActiveChanged(isAnySheetOpen)
    }

    val currentList = when (subTab) {
        HistorySubTab.HISTORY -> state.historyList
        HistorySubTab.FAVORITES -> state.favoritesList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Sub-Tab Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = subTab == HistorySubTab.HISTORY,
                    onClick = { subTab = HistorySubTab.HISTORY },
                    label = { Text(stringResource(R.string.hist_subtab_all_format, state.historyList.size)) }
                )
                FilterChip(
                    selected = subTab == HistorySubTab.FAVORITES,
                    onClick = { subTab = HistorySubTab.FAVORITES },
                    label = { Text(stringResource(R.string.hist_subtab_favorites_format, state.favoritesList.size)) }
                )
            }

            if (subTab == HistorySubTab.HISTORY && state.historyList.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = { showHistoryActionMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.hist_clean_options),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    DropdownMenu(
                        expanded = showHistoryActionMenu,
                        onDismissRequest = { showHistoryActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(R.string.hist_menu_clean_invalid), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = stringResource(R.string.hist_menu_clean_invalid_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showHistoryActionMenu = false
                                showClearInvalidConfirmDialog = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.hist_menu_clear_all),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = stringResource(R.string.hist_menu_clear_all_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showHistoryActionMenu = false
                                showClearAllConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (subTab == HistorySubTab.FAVORITES) {
                            Icons.Default.FavoriteBorder
                        } else {
                            Icons.Default.History
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (subTab == HistorySubTab.FAVORITES) {
                            stringResource(R.string.hist_empty_fav_title)
                        } else {
                            stringResource(R.string.hist_empty_all_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (subTab == HistorySubTab.FAVORITES) {
                            stringResource(R.string.hist_empty_fav_desc)
                        } else {
                            stringResource(R.string.hist_empty_all_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = currentList,
                    key = { it.id }
                ) { item ->
                    WallpaperGridCard(
                        item = item,
                        onClick = { selectedItemForDetail = item },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }
        }
    }

    // Detail Action Bottom Sheet
    selectedItemForDetail?.let { currentDetailItem ->
        val item = state.historyList.find { it.id == currentDetailItem.id }
            ?: state.favoritesList.find { it.id == currentDetailItem.id }
            ?: currentDetailItem
        ModalBottomSheet(
            onDismissRequest = { selectedItemForDetail = null },
            sheetState = sheetState
        ) {
            WallpaperDetailSheet(
                item = item,
                isApplying = state.isChanging,
                isRedownloading = state.isRedownloadingHistoryId == item.id,
                onApply = {
                    onApplyWallpaper(item)
                    selectedItemForDetail = null
                },
                onToggleFavorite = {
                    onToggleFavorite(item)
                    selectedItemForDetail = item.copy(isFavorite = !item.isFavorite)
                },
                onRedownload = {
                    onRedownloadHistoryItem(item)
                },
                onDelete = {
                    itemToDelete = item
                },
                onOpen = { onOpenInGallery(item.displayUri) },
                onShare = { onShareWallpaper(item.displayUri, item.title) },
                onSave = { onSaveToGallery(item.displayUri, item.title) },
                onOpenAdjustment = {
                    val target = item
                    selectedItemForDetail = null
                    selectedItemForAdjustment = target
                }
            )

            // Top floating pill banner notification displayed on top of the sheet and scrim
            TopFloatingPillNotification(
                message = state.statusMessage,
                onDismiss = onClearStatus
            )
        }
    }

    // Per-image personalized attribute adjustment sheet
    selectedItemForAdjustment?.let { item ->
        WallpaperAdjustmentSheet(
            item = item,
            globalScrollMode = state.scrollMode,
            onDismiss = { selectedItemForAdjustment = null },
            onSave = { customScrollMode, cropFocusX, cropFocusY, flipHorizontal, applyImmediately ->
                onUpdateWallpaperPreferences(
                    item,
                    customScrollMode,
                    cropFocusX,
                    cropFocusY,
                    flipHorizontal,
                    applyImmediately
                )
            }
        )
    }

    // Confirmation dialog for single history item deletion
    itemToDelete?.let { targetItem ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.hist_dialog_delete_title)) },
            text = {
                val wallpaperName = targetItem.title ?: stringResource(R.string.hist_item_this_wallpaper)
                val detailText = if (targetItem.isFavorite) {
                    stringResource(R.string.hist_dialog_delete_fav_msg, wallpaperName)
                } else {
                    stringResource(R.string.hist_dialog_delete_msg, wallpaperName)
                }
                Text(detailText)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = targetItem
                        itemToDelete = null
                        selectedItemForDetail = null
                        onDeleteHistoryItem(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Confirmation dialog for clearing invalid history records
    if (showClearInvalidConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearInvalidConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.hist_dialog_clear_invalid_title)) },
            text = {
                Text(stringResource(R.string.hist_dialog_clear_invalid_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearInvalidConfirmDialog = false
                        onClearInvalidHistory()
                    }
                ) {
                    Text(stringResource(R.string.hist_dialog_clear_invalid_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearInvalidConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Confirmation dialog for clearing all unfavorited history
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.hist_dialog_clear_all_title)) },
            text = {
                Text(stringResource(R.string.hist_dialog_clear_all_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirmDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.hist_dialog_clear_all_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun WallpaperGridCard(
    item: WallpaperHistoryItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val isAccessible = remember(item.displayUri, item.downloadTimestamp) {
        isUriAccessible(context, item.displayUri)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
        ) {
            val cardImageRequest = remember(item.displayUri, item.downloadTimestamp) {
                ImageRequest.Builder(context)
                    .data(item.displayUri)
                    .memoryCacheKey("${item.displayUri}_${item.downloadTimestamp}")
                    .build()
            }
            AsyncImage(
                model = cardImageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Semi-transparent overlay badge for inaccessible images
            if (!isAccessible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.canRedownload) {
                            stringResource(R.string.hist_badge_inaccessible_redownloadable)
                        } else {
                            stringResource(R.string.hist_badge_inaccessible)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Top gradient & favorite action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                // Source label tag
                Text(
                    text = item.displaySourceBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = 36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                // Favorite icon toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.hist_action_favorite),
                        tint = if (item.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom gradient overlay for title & date
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = item.title ?: stringResource(R.string.hist_unknown_wallpaper),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimeAgo(item.appliedTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperDetailSheet(
    item: WallpaperHistoryItem,
    isApplying: Boolean,
    isRedownloading: Boolean = false,
    onApply: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRedownload: () -> Unit = {},
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onOpenAdjustment: () -> Unit
) {
    val context = LocalContext.current
    val isAccessible = remember(item.displayUri, item.downloadTimestamp) {
        isUriAccessible(context, item.displayUri)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sheetImageRequest = remember(item.displayUri, item.downloadTimestamp) {
                ImageRequest.Builder(context)
                    .data(item.displayUri)
                    .memoryCacheKey("${item.displayUri}_${item.downloadTimestamp}")
                    .build()
            }
            AsyncImage(
                model = sheetImageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title ?: stringResource(R.string.hist_unknown_wallpaper),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.hist_detail_source_format,
                        item.displaySourceDetail,
                        formatDateTime(item.appliedTimestamp)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (item.isFavorite) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.hist_detail_favorited_badge),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isAccessible) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.hist_detail_missing_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (item.canRedownload) {
                                    stringResource(R.string.hist_detail_missing_redownload_desc)
                                } else {
                                    stringResource(R.string.hist_detail_missing_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    if (item.canRedownload) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRedownload,
                            enabled = !isRedownloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRedownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.hist_action_redownloading))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.hist_action_redownload))
                            }
                        }
                    }
                }
            }
        }

        // Main Action: Apply as Wallpaper
        Button(
            onClick = onApply,
            enabled = !isApplying && isAccessible,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isApplying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hist_action_applying))
            } else if (!isAccessible) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hist_action_cannot_apply))
            } else {
                Icon(Icons.Default.Wallpaper, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hist_action_set_current))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Action Buttons Grid (avoids label truncation on narrow screens)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleFavorite,
                    enabled = isAccessible || item.isFavorite,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (item.isFavorite) stringResource(R.string.hist_action_favorited)
                        else stringResource(R.string.hist_action_favorite),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onOpen,
                    enabled = isAccessible,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.hist_action_gallery), maxLines = 1)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    enabled = isAccessible,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.hist_action_save), maxLines = 1)
                }

                OutlinedButton(
                    onClick = onShare,
                    enabled = isAccessible,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share), maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenAdjustment,
            enabled = isAccessible,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            val customBadge = if (item.customScrollMode != null ||
                (item.cropFocusX != null && item.cropFocusX != 0.5f) ||
                (item.cropFocusY != null && item.cropFocusY != 0.5f) ||
                item.flipHorizontal
            ) stringResource(R.string.hist_badge_customized) else ""
            Text("${stringResource(R.string.hist_action_tune)}$customBadge", maxLines = 1)
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.hist_action_delete_record), color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> stringResource(R.string.time_minutes_ago, minutes.toInt())
        hours < 24 -> stringResource(R.string.time_hours_ago, hours.toInt())
        days < 7 -> stringResource(R.string.time_days_ago, days.toInt())
        else -> {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Checks if the target URI is currently accessible and readable.
 * Handles file paths and content resolver queries safely.
 */
private fun isUriAccessible(context: Context, uri: Uri): Boolean {
    return runCatching {
        if (uri.scheme == "file") {
            val path = uri.path ?: return false
            val file = File(path)
            file.exists() && file.canRead() && file.length() > 0
        } else {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }
    }.getOrDefault(false)
}

