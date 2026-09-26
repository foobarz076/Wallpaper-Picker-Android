package foo.barz.wallpaperpicker.ui.tabs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.CustomPhotosSourceConfig
import foo.barz.wallpaperpicker.core.model.FavoritesSourceConfig
import foo.barz.wallpaperpicker.core.model.HttpApiSourceConfig
import foo.barz.wallpaperpicker.core.model.ImmichSourceConfig
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Modernized Sources Tab presenting a unified, multi-instance list of configured wallpaper sources.
 * Any combination of enabled sources seamlessly rotates in composite mode.
 */
@Composable
fun SourcesTab(
    state: MainUiState,
    onToggleSourceEnabled: (String, Boolean) -> Unit,
    onDeleteSource: (String) -> Unit,
    onOpenAddSource: () -> Unit,
    onOpenEditSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledSources = remember(state.sourcesList) {
        state.sourcesList.filter { it.isEnabled }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Header status overview card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (enabledSources.size > 1) Icons.Default.Layers else Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "壁纸图源管理",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val statusSummary = when {
                                enabledSources.isEmpty() -> "未启用任何图源，自动轮播将暂停"
                                enabledSources.size == 1 -> "已启用 1 个图源: ${enabledSources.first().title}"
                                else -> "已启用 ${enabledSources.size} 个图源 · 自动混合随机轮播"
                            }
                            Text(
                                text = statusSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (enabledSources.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Empty state
            if (state.sourcesList.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "尚未配置任何图源",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "添加本地文件夹、系统相册、Immich 或网络图源后即可自动轮播",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onOpenAddSource) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("添加图源")
                            }
                        }
                    }
                }
            } else {
                // List of source cards
                items(state.sourcesList, key = { it.id }) { source ->
                    SourceItemCard(
                        source = source,
                        favoritesCount = state.favoritesList.size,
                        onToggleEnabled = { enabled -> onToggleSourceEnabled(source.id, enabled) },
                        onClick = { onOpenEditSource(source.id) }
                    )
                }
            }

            // Bottom spacer for FAB clearance
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        // Floating Action Button to Add Source
        ExtendedFloatingActionButton(
            onClick = onOpenAddSource,
            icon = { Icon(Icons.Default.Add, contentDescription = "添加图源") },
            text = { Text("添加图源") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

/**
 * Single source item card representing a configured source entity.
 */
@Composable
private fun SourceItemCard(
    source: WallpaperSourceEntity,
    favoritesCount: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (source.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (source.type) {
                        WallpaperSourceType.CUSTOM_PHOTOS -> Icons.Default.Image
                        WallpaperSourceType.LOCAL_FOLDER -> Icons.Default.Folder
                        WallpaperSourceType.MEDIA_STORE -> Icons.Default.Collections
                        WallpaperSourceType.IMMICH -> Icons.Default.PhotoLibrary
                        WallpaperSourceType.HTTP_API -> Icons.Default.Cloud
                        WallpaperSourceType.FAVORITES -> Icons.Default.Favorite
                        WallpaperSourceType.COMPOSITE -> Icons.Default.Layers
                    },
                    contentDescription = null,
                    tint = if (source.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (source.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = source.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val subtitle = remember(source) {
                    when (source.type) {
                        WallpaperSourceType.CUSTOM_PHOTOS -> {
                            val config = CustomPhotosSourceConfig.fromJson(source.configJson)
                            "${config.imageCount} 张自选照片"
                        }
                        WallpaperSourceType.LOCAL_FOLDER -> {
                            val config = LocalFolderSourceConfig.fromJson(source.configJson)
                            if (config.folderName.isNotBlank()) "${config.folderName} · ${config.imageCount} 张图片" else "尚未选择文件夹"
                        }
                        WallpaperSourceType.MEDIA_STORE -> {
                            val config = MediaStoreSourceConfig.fromJson(source.configJson)
                            config.albumNames
                        }
                        WallpaperSourceType.IMMICH -> {
                            val config = ImmichSourceConfig.fromJson(source.configJson)
                            if (config.serverUrl.isNotBlank()) "${config.albumNames} · ${config.serverUrl}" else "尚未配置服务器"
                        }
                        WallpaperSourceType.HTTP_API -> {
                            val config = HttpApiSourceConfig.fromJson(source.configJson)
                            config.preset.label
                        }
                        WallpaperSourceType.FAVORITES -> {
                            "离线持久收藏 · $favoritesCount 张已收藏"
                        }
                        WallpaperSourceType.COMPOSITE -> "多源混合"
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch to enable/disable source
            Switch(
                checked = source.isEnabled,
                onCheckedChange = onToggleEnabled
            )
        }
    }
}
