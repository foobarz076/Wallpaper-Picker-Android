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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
                    label = { Text("全部历史 (${state.historyList.size})") }
                )
                FilterChip(
                    selected = subTab == HistorySubTab.FAVORITES,
                    onClick = { subTab = HistorySubTab.FAVORITES },
                    label = { Text("我的收藏 (${state.favoritesList.size})") }
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
                            contentDescription = "历史清理选项",
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
                                    Text("一键清理失效记录", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "仅移除原图已丢失的非收藏条目",
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
                                        text = "清空全部非收藏历史",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "删除所有未加星标的历史记录",
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
                        text = if (subTab == HistorySubTab.FAVORITES) "暂无收藏壁纸" else "暂无历史壁纸记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (subTab == HistorySubTab.FAVORITES) {
                            "在历史壁纸或控制台中点击爱心，即可永久收藏并支持离线使用"
                        } else {
                            "每次成功更换壁纸后，均会自动记录在此处"
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
            title = { Text("删除历史记录") },
            text = {
                val detailText = if (targetItem.isFavorite) {
                    "确定要删除「${targetItem.title ?: "此壁纸"}」吗？该壁纸已被收藏，删除历史记录将一并移除收藏状态及离线保护副本。"
                } else {
                    "确定要从历史记录中删除「${targetItem.title ?: "此壁纸"}」吗？此操作不可撤销。"
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
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("取消")
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
            title = { Text("清理失效历史记录") },
            text = {
                Text("将扫描并移除所有本地原图已丢失、且未加入收藏的历史记录条目。已收藏的壁纸受离线持久保护，不受影响。是否继续？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearInvalidConfirmDialog = false
                        onClearInvalidHistory()
                    }
                ) {
                    Text("立即清理")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearInvalidConfirmDialog = false }) {
                    Text("取消")
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
            title = { Text("清空历史记录") },
            text = {
                Text("确定要清空所有未收藏的历史壁纸记录吗？已加入收藏的壁纸将完整保留。此操作不可恢复。")
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
                    Text("清空全部")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("取消")
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
                        text = if (item.canRedownload) "原图失效 · 可重下" else "原图已失效",
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
                        contentDescription = "收藏",
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
                        text = item.title ?: "未知壁纸",
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
                    text = item.title ?: "未知壁纸",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "来源: ${item.displaySourceDetail} · 更换于 ${formatDateTime(item.appliedTimestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (item.isFavorite) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "已加入收藏 (离线永久保留)",
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
                                text = "原图已被清理或无法访问",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (item.canRedownload) {
                                    "本地缓存已丢失，但检测到远端下载源，可点击下方重新下载以恢复原图并设为壁纸。"
                                } else {
                                    "该原图在本地相册中已失效或未包含远端地址，无法直接恢复。您可以删除此记录以清理列表。"
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
                                Text("正在重新下载…")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("从网络重新下载原图")
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
                Text("正在应用壁纸…")
            } else if (!isAccessible) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("原图已失效无法应用")
            } else {
                Icon(Icons.Default.Wallpaper, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("设为当前壁纸")
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
                    Text(if (item.isFavorite) "已收藏" else "设为收藏", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpen,
                    enabled = isAccessible,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("图库打开", maxLines = 1)
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
                    Text("保存相册", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onShare,
                    enabled = isAccessible,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("系统分享", maxLines = 1)
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
            ) " (已个性化)" else ""
            Text("微调构图与滚动$customBadge", maxLines = 1)
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
            Text("删除此记录", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days < 7 -> "${days}天前"
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

