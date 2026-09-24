package foo.barz.wallpaperpicker.ui.tabs

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.MainUiState
import java.util.Locale

/**
 * Sources tab managing all wallpaper providers (Local Folder, MediaStore, Immich, HTTP API)
 * and their source-specific configuration parameters.
 */
@Composable
fun SourcesTab(
    state: MainUiState,
    onSourceTypeSelected: (WallpaperSourceType) -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onRescanFolder: () -> Unit,
    onFetchMediaStoreAlbums: () -> Unit,
    onMediaStoreAlbumSelected: (MediaStoreAlbum?) -> Unit,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onFolderSelected(uri)
        }
    }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasMediaPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMediaPermission = isGranted
        if (isGranted) {
            onFetchMediaStoreAlbums()
        }
    }

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
                        when (state.sourceType) {
                            WallpaperSourceType.LOCAL_FOLDER -> Icons.Default.Folder
                            WallpaperSourceType.MEDIA_STORE -> Icons.Default.Collections
                            WallpaperSourceType.IMMICH -> Icons.Default.PhotoLibrary
                            WallpaperSourceType.HTTP_API -> Icons.Default.Cloud
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("壁纸来源配置", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Source Type Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.sourceType == WallpaperSourceType.LOCAL_FOLDER,
                        onClick = { onSourceTypeSelected(WallpaperSourceType.LOCAL_FOLDER) },
                        label = { Text("本地文件夹") }
                    )
                    FilterChip(
                        selected = state.sourceType == WallpaperSourceType.MEDIA_STORE,
                        onClick = { onSourceTypeSelected(WallpaperSourceType.MEDIA_STORE) },
                        label = { Text("系统相册") }
                    )
                    FilterChip(
                        selected = state.sourceType == WallpaperSourceType.IMMICH,
                        onClick = { onSourceTypeSelected(WallpaperSourceType.IMMICH) },
                        label = { Text("Immich 相册") }
                    )
                    FilterChip(
                        selected = state.sourceType == WallpaperSourceType.HTTP_API,
                        onClick = { onSourceTypeSelected(WallpaperSourceType.HTTP_API) },
                        label = { Text("通用 HTTP") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (state.sourceType) {
                    WallpaperSourceType.LOCAL_FOLDER -> {
                        Text(
                            text = state.folderName?.let { "已选目录: $it" } ?: "尚未选择任何文件夹",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.folderUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        if (state.folderUri != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (state.isIndexingFolder) {
                                    "正在扫描并建立极速索引…"
                                } else {
                                    "已建立轻量索引: ${state.indexedImageCount} 张图片 (0ms 零耗切换)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (state.folderUri != null) "更换文件夹" else "选择文件夹")
                            }
                            if (state.folderUri != null) {
                                OutlinedButton(
                                    onClick = onRescanFolder,
                                    enabled = !state.isIndexingFolder,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (state.isIndexingFolder) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("重新扫描")
                                }
                            }
                        }
                    }

                    WallpaperSourceType.MEDIA_STORE -> {
                        Text("系统相册直连 (MediaStore 原生索引)", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))

                        if (!hasMediaPermission) {
                            Text(
                                text = "需要读取相册权限以直接检索系统生活照与相机相册",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { mediaPermissionLauncher.launch(mediaPermission) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("授予相册访问权限")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.mediaStoreAlbumName?.let { "已绑定相册: $it" } ?: "全部照片 (全库随机)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                OutlinedButton(
                                    onClick = onFetchMediaStoreAlbums,
                                    enabled = !state.isLoadingMediaStoreAlbums
                                ) {
                                    if (state.isLoadingMediaStoreAlbums) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(if (state.mediaStoreAlbums.isEmpty()) "获取相册" else "刷新相册")
                                }
                            }

                            if (state.mediaStoreAlbums.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.mediaStoreAlbums.forEach { album ->
                                        FilterChip(
                                            selected = state.mediaStoreAlbumId == album.id,
                                            onClick = { onMediaStoreAlbumSelected(if (album.id == null) null else album) },
                                            label = { Text("${album.name} (${album.count})") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    WallpaperSourceType.IMMICH -> {
                        Text("Immich 自建服务配置", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.immichServerUrl,
                            onValueChange = onImmichServerUrlChanged,
                            label = { Text("服务器地址 (Server URL)") },
                            placeholder = { Text("例如 https://192.168.1.100:2283 或 http://immich.local:2283") },
                            supportingText = {
                                if (isLikelyBlockedCleartext(state.immichServerUrl)) {
                                    Text(
                                        text = "提示：系统限制未加密 HTTP 明文。局域网 IP 建议改用 https://（并勾选下方「忽略自签名证书」），或使用 .local / .lan 主机名",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.immichApiKey,
                            onValueChange = onImmichApiKeyChanged,
                            label = { Text("Immich API Key") },
                            placeholder = { Text("在 Immich 账号设置中生成") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Album selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("相册筛选", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = state.immichAlbumName?.let { "已绑定相册: $it" } ?: "全部相册 (全库随机)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedButton(
                                onClick = onFetchImmichAlbums,
                                enabled = !state.isLoadingAlbums && state.immichServerUrl.isNotBlank() && state.immichApiKey.isNotBlank()
                            ) {
                                if (state.isLoadingAlbums) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(if (state.immichAlbums.isEmpty()) "获取相册" else "刷新相册")
                            }
                        }

                        if (state.immichAlbums.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = state.immichAlbumId == null,
                                    onClick = { onImmichAlbumSelected(null) },
                                    label = { Text("全部相册") }
                                )
                                state.immichAlbums.forEach { album ->
                                    FilterChip(
                                        selected = state.immichAlbumId == album.id,
                                        onClick = { onImmichAlbumSelected(album) },
                                        label = { Text("${album.name} (${album.assetCount})") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("下载画质", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ImmichQuality.PREVIEW to "高清预览 (推荐省流)",
                                ImmichQuality.ORIGINAL to "原始全尺寸"
                            ).forEach { (quality, label) ->
                                FilterChip(
                                    selected = state.immichQuality == quality,
                                    onClick = { onImmichQualitySelected(quality) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("忽略自签名证书校验", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "局域网自签名 HTTPS 证书请开启",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = state.immichIgnoreSsl,
                                onCheckedChange = onToggleImmichIgnoreSsl
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("仅在 Wi-Fi 下下载", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "移动网络时自动复用本地缓存池，避免消耗蜂窝流量",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = state.immichWifiOnly,
                                onCheckedChange = onToggleImmichWifiOnly
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "缓存占用: ${formatFileSize(state.cacheSizeBytes)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = onClearCache,
                                enabled = state.cacheSizeBytes > 0L
                            ) {
                                Text("清理缓存")
                            }
                        }
                    }

                    WallpaperSourceType.HTTP_API -> {
                        Text("预设或自定义", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HttpPresetType.values().forEach { preset ->
                                FilterChip(
                                    selected = state.httpPresetType == preset,
                                    onClick = { onHttpPresetSelected(preset) },
                                    label = { Text(preset.label) }
                                )
                            }
                        }

                        if (state.httpPresetType == HttpPresetType.CUSTOM) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.httpCustomUrl,
                                onValueChange = onHttpCustomUrlChanged,
                                label = { Text("API 网址 (URL)") },
                                placeholder = { Text("https://api.example.com/wallpaper 或 http://api.local:8080") },
                                supportingText = {
                                    if (isLikelyBlockedCleartext(state.httpCustomUrl)) {
                                        Text(
                                            text = "提示：系统限制非豁免域名的 HTTP 明文。建议改用 https://，或使用以 .local / .lan 结尾的主机名",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.httpCustomJsonPath,
                                onValueChange = onHttpCustomJsonPathChanged,
                                label = { Text("图片字段路径 (可选 JSONPath)") },
                                placeholder = { Text("例如 images[0].url 或留空表示直接图片") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("仅在 Wi-Fi 下下载", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "移动网络时自动复用本地缓存池，避免消耗蜂窝流量",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = state.wifiOnly,
                                onCheckedChange = onToggleWifiOnly
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "缓存占用: ${formatFileSize(state.cacheSizeBytes)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = onClearCache,
                                enabled = state.cacheSizeBytes > 0L
                            ) {
                                Text("清理缓存")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.1f KB", kb)
    }
}

/**
 * Evaluates whether a given URL is using unencrypted HTTP on a non-exempted domain or IP address,
 * which will likely be rejected by the platform's Network Security Configuration.
 */
private fun isLikelyBlockedCleartext(url: String): Boolean {
    val trimmed = url.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true)) return false

    val host = runCatching { Uri.parse(trimmed).host?.lowercase(Locale.ROOT) }.getOrNull()
    if (host.isNullOrEmpty()) return false

    val isPermitted = host == "localhost" ||
            host == "127.0.0.1" ||
            host == "10.0.2.2" ||
            host.endsWith(".local") || host == "local" ||
            host.endsWith(".lan") || host == "lan" ||
            host.endsWith(".internal") || host == "internal" ||
            host.endsWith(".home.arpa") || host == "home.arpa" ||
            host.endsWith(".home") || host == "home" ||
            host.endsWith(".corp") || host == "corp"

    return !isPermitted
}
