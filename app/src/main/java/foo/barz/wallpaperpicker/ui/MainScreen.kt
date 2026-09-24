package foo.barz.wallpaperpicker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import java.util.Locale

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
    onChangeNow: () -> Unit,
    onClearStatus: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

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

    val writeStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSaveToGallery()
        }
    }

    val handleSaveClick = {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onSaveToGallery()
            } else {
                writeStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            onSaveToGallery()
        }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearStatus()
        }
    }

    val canChange = !state.isChanging && when (state.sourceType) {
        WallpaperSourceType.LOCAL_FOLDER -> state.folderUri != null
        WallpaperSourceType.MEDIA_STORE -> hasMediaPermission
        WallpaperSourceType.IMMICH -> state.immichServerUrl.isNotBlank() && state.immichApiKey.isNotBlank()
        WallpaperSourceType.HTTP_API -> state.httpPresetType != HttpPresetType.CUSTOM || state.httpCustomUrl.isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Picker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Button(
                    onClick = onChangeNow,
                    enabled = canChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.isChanging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("正在处理并更换壁纸…")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即更换壁纸")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前状态", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val activeSourceLabel = when (state.sourceType) {
                        WallpaperSourceType.LOCAL_FOLDER -> "本地文件夹 (${state.folderName ?: "未选择"})"
                        WallpaperSourceType.MEDIA_STORE -> "系统相册 (${state.mediaStoreAlbumName ?: "全部照片"})"
                        WallpaperSourceType.IMMICH -> "Immich (${state.immichAlbumName ?: "全部相册"})"
                        WallpaperSourceType.HTTP_API -> state.httpPresetType.label
                    }
                    Text(
                        text = "激活来源: $activeSourceLabel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "上次更换: ${state.lastChangedText}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (state.isScheduled) "定时调度: 已开启 (每 ${state.intervalMinutes} 分钟)" else "定时调度: 未开启",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    if (state.lastErrorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "最近执行异常: ${state.lastErrorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (state.lastExecutionStatus != null && state.lastExecutionStatus != "成功") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "最近执行状态: ${state.lastExecutionStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 2. Current Wallpaper Actions Card (Phase 3.4: Open, Save, Share)
            if (state.lastWallpaperUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("当前壁纸操作", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = state.lastWallpaperUri,
                                contentDescription = state.lastWallpaperTitle ?: "当前壁纸预览",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.lastWallpaperTitle ?: "当前正在使用的壁纸",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "支持原图全屏缩放、外部编辑、系统分享及保存到公共相册",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Adaptive button grid: 2-column grid on narrow containers, 1-row layout on wide screens
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isNarrow = maxWidth < 360.dp

                            val openButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                                OutlinedButton(
                                    onClick = onOpenInGallery,
                                    modifier = buttonModifier,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("图库打开", maxLines = 1)
                                }
                            }

                            val saveButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                                OutlinedButton(
                                    onClick = handleSaveClick,
                                    enabled = !state.isSavingWallpaper,
                                    modifier = buttonModifier,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    if (state.isSavingWallpaper) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("保存相册", maxLines = 1)
                                }
                            }

                            val shareButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                                OutlinedButton(
                                    onClick = onShareWallpaper,
                                    modifier = buttonModifier,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("分享", maxLines = 1)
                                }
                            }

                            if (isNarrow) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        openButton(Modifier.weight(1f))
                                        saveButton(Modifier.weight(1f))
                                    }
                                    shareButton(Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    openButton(Modifier.weight(1f))
                                    saveButton(Modifier.weight(1f))
                                    shareButton(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Wallpaper Source Configuration Card
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

                    // Source Switcher (Local vs MediaStore vs Immich vs HTTP)
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
                                    text = if (state.isIndexingFolder) "正在扫描并建立极速索引…" else "已建立轻量索引: ${state.indexedImageCount} 张图片 (0ms 零耗切换)",
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
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
                                placeholder = { Text("例如 http://192.168.1.100:2283") },
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
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
                            // HTTP API Configuration
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
                                    placeholder = { Text("https://api.example.com/wallpaper") },
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

            // 4. Target Screen Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("更换应用目标", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val canSeparateTarget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    if (!canSeparateTarget) {
                        Text(
                            text = "当前设备运行 Android 6.0，系统规范限制将同时更新桌面与锁屏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.target == WallpaperTarget.BOTH,
                            onClick = { onTargetSelected(WallpaperTarget.BOTH) },
                            label = { Text("桌面与锁屏") }
                        )
                        if (canSeparateTarget) {
                            FilterChip(
                                selected = state.target == WallpaperTarget.SYSTEM,
                                onClick = { onTargetSelected(WallpaperTarget.SYSTEM) },
                                label = { Text("仅桌面") }
                            )
                            FilterChip(
                                selected = state.target == WallpaperTarget.LOCK,
                                onClick = { onTargetSelected(WallpaperTarget.LOCK) },
                                label = { Text("仅锁屏") }
                            )
                        }
                    }
                }
            }

            // 5. Wallpaper Crop Mode Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Crop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("壁纸裁切与构图", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            WallpaperCropMode.FIT_HEIGHT,
                            WallpaperCropMode.CENTER_CROP,
                            WallpaperCropMode.FIT_CENTER
                        ).forEach { mode ->
                            FilterChip(
                                selected = state.cropMode == mode,
                                onClick = { onCropModeSelected(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.cropMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 6. Wallpaper Scroll Mode Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewCarousel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("桌面视差随动", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            WallpaperScrollMode.AUTO,
                            WallpaperScrollMode.NEVER,
                            WallpaperScrollMode.ALWAYS
                        ).forEach { mode ->
                            FilterChip(
                                selected = state.scrollMode == mode,
                                onClick = { onScrollModeSelected(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.scrollMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "提示：部分定制系统（如 HyperOS/MIUI 或已关闭壁纸随动的桌面）不支持壁纸平移。若滑动桌面时壁纸静止，建议选择「锁定居中」以单屏最清晰画质呈现。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("修改设置时重设当前壁纸", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "更改裁切或滚动设置时，立即按新模式重新渲染并应用当前壁纸",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.reapplyOnScrollChange,
                            onCheckedChange = onToggleReapplyOnScrollChange
                        )
                    }

                    if (state.lastWallpaperUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onReapplyCurrentWallpaper,
                            enabled = !state.isChanging,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("按当前设置重设正在使用的壁纸")
                        }
                    }
                }
            }

            // 6. Schedule Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("后台自动定时更换", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = state.isScheduled,
                            onCheckedChange = onToggleSchedule
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("更换频率", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val intervals = listOf(
                        15L to "15分钟",
                        30L to "30分钟",
                        60L to "1小时",
                        360L to "6小时",
                        1440L to "每天"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.take(3).forEach { (minutes, label) ->
                            FilterChip(
                                selected = state.intervalMinutes == minutes,
                                onClick = { onIntervalSelected(minutes) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.drop(3).forEach { (minutes, label) ->
                            FilterChip(
                                selected = state.intervalMinutes == minutes,
                                onClick = { onIntervalSelected(minutes) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("使用手机时推迟更换", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "检测到亮屏或正在使用手机时暂缓更换壁纸，防止游戏、观影或打字时发生掉帧卡顿",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.deferDuringInteraction,
                            onCheckedChange = onToggleDeferDuringInteraction
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("智能洗牌防重复 (Fair Shuffle)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "记忆最近 50 张已用壁纸，在一整轮展示完之前避免高频抽取相同图片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.fairShuffle,
                            onCheckedChange = onToggleFairShuffle
                        )
                    }
                }
            }

            // 7. Battery Optimization Hint
            if (!isIgnoringBatteryOptimizations) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开启电池白名单以保障准时更换", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "低电耗 (Doze) 模式可能会延迟休眠期间的定时任务，建议为本应用加入电池优化白名单。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                requestIgnoreBatteryOptimization(context)
                                isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
                            }
                        ) {
                            Text("前往设置白名单")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Extra padding for bottom bar
        }
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            // Ignore if device doesn't support
        }
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
