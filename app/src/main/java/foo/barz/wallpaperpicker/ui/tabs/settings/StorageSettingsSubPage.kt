package foo.barz.wallpaperpicker.ui.tabs.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Sub-page for storage utilization, network cache eviction limits,
 * favorites backup export, and space management.
 */
@Composable
fun StorageSettingsSubPage(
    state: MainUiState,
    onCacheSizeTierSelected: (CacheSizeTier) -> Unit,
    onExportFavorites: () -> Unit,
    onOpenManageSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("存储与缓存概览", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Favorites Storage Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("已收藏壁纸 (私有持久化)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "存放在内部专属目录，断网永久可用，绝不受系统或缓存清理影响",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.favoritesList.size} 张 · ${SettingsHelpers.formatFileSize(state.favoritesSizeBytes)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Transient Cache Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("临时网络缓存 (LRU 缓存池)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "网络图源下载的临时图片，用于离线降级复用。超出容量上限后按最久未展示 (LRU) 顺序自动淘汰，已收藏壁纸受离线保护永不删除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SettingsHelpers.formatFileSize(state.cacheSizeBytes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Cache Size Tier Configuration & Disabled Option
                Text("自动清理与缓存容量上限", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "配置网络缓存池的最大淘汰容量；达到上限后自动淘汰最旧图片：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.cacheSizeTier == CacheSizeTier.SMALL,
                            onClick = { onCacheSizeTierSelected(CacheSizeTier.SMALL) },
                            label = { Text("小 (20MB)") }
                        )
                        FilterChip(
                            selected = state.cacheSizeTier == CacheSizeTier.STANDARD,
                            onClick = { onCacheSizeTierSelected(CacheSizeTier.STANDARD) },
                            label = { Text("标准 (50MB)") }
                        )
                        FilterChip(
                            selected = state.cacheSizeTier == CacheSizeTier.LARGE,
                            onClick = { onCacheSizeTierSelected(CacheSizeTier.LARGE) },
                            label = { Text("大 (100MB)") }
                        )
                    }

                    FilterChip(
                        selected = state.cacheSizeTier == CacheSizeTier.DISABLED,
                        onClick = { onCacheSizeTierSelected(CacheSizeTier.DISABLED) },
                        label = { Text("禁用自动清理 (不限制)") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.cacheSizeTier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.cacheSizeTier == CacheSizeTier.DISABLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button: Export Favorites
                OutlinedButton(
                    onClick = onExportFavorites,
                    enabled = state.favoritesList.isNotEmpty() && !state.isExportingFavorites,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isExportingFavorites) "正在导出…" else "导出全部收藏")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manage Space Activity launcher
                OutlinedButton(
                    onClick = onOpenManageSpace,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("管理存储空间与清理缓存…")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
