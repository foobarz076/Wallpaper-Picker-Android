package foo.barz.wallpaperpicker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import foo.barz.wallpaperpicker.core.database.LocalFolderFastScanner
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.core.model.FavoritesSourceConfig
import foo.barz.wallpaperpicker.core.model.HttpApiSourceConfig
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.ImmichSourceConfig
import foo.barz.wallpaperpicker.core.model.LocalFolderSourceConfig
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.MediaStoreSourceConfig
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.source.ImmichSource
import foo.barz.wallpaperpicker.core.source.MediaStoreSource
import foo.barz.wallpaperpicker.ui.components.AlbumPickerSheet
import foo.barz.wallpaperpicker.ui.components.PickerAlbumItem
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Activity for adding and editing wallpaper source instances.
 */
class SourceConfigActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE_ID = "extra_source_id"

        fun createIntent(context: Context, sourceId: String? = null): Intent {
            return Intent(context, SourceConfigActivity::class.java).apply {
                if (sourceId != null) {
                    putExtra(EXTRA_SOURCE_ID, sourceId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID)
        val isEditMode = sourceId != null

        setContent {
            WallpaperPickerTheme {
                SourceConfigScreen(
                    sourceId = sourceId,
                    isEditMode = isEditMode,
                    onFinish = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceConfigScreen(
    sourceId: String?,
    isEditMode: Boolean,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val db = remember { WallpaperSourcesDatabase(context) }
    val historyDb = remember { WallpaperHistoryDatabase(context) }

    var selectedType by remember { mutableStateOf(WallpaperSourceType.LOCAL_FOLDER) }
    var title by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Local Folder state
    var folderUriStr by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var indexedImageCount by remember { mutableStateOf(0) }
    var isScanningFolder by remember { mutableStateOf(false) }

    // MediaStore state
    var mediaStoreAlbumIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mediaStoreAlbumNames by remember { mutableStateOf("全部照片 (全库随机)") }
    var mediaStoreAlbums by remember { mutableStateOf<List<MediaStoreAlbum>>(emptyList()) }
    var isLoadingMediaStoreAlbums by remember { mutableStateOf(false) }
    var showMediaStorePicker by remember { mutableStateOf(false) }

    // Immich state
    var immichServerUrl by remember { mutableStateOf("") }
    var immichApiKey by remember { mutableStateOf("") }
    var immichAlbumIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var immichAlbumNames by remember { mutableStateOf("全部相册 (全库随机)") }
    var immichAlbums by remember { mutableStateOf<List<ImmichAlbum>>(emptyList()) }
    var immichQuality by remember { mutableStateOf(ImmichQuality.PREVIEW) }
    var immichIgnoreSsl by remember { mutableStateOf(false) }
    var immichWifiOnly by remember { mutableStateOf(true) }
    var isLoadingImmichAlbums by remember { mutableStateOf(false) }
    var showImmichPicker by remember { mutableStateOf(false) }

    // HTTP state
    var httpPreset by remember { mutableStateOf(HttpPresetType.BING) }
    var httpCustomUrl by remember { mutableStateOf("") }
    var httpCustomJsonPath by remember { mutableStateOf("") }
    var httpWifiOnly by remember { mutableStateOf(true) }

    // Favorites state
    var favoritesCount by remember { mutableStateOf(0) }

    // Load existing source if in edit mode
    LaunchedEffect(sourceId) {
        if (sourceId != null) {
            val entity = db.getSourceById(sourceId)
            if (entity != null) {
                selectedType = entity.type
                title = entity.title
                when (entity.type) {
                    WallpaperSourceType.LOCAL_FOLDER -> {
                        val config = LocalFolderSourceConfig.fromJson(entity.configJson)
                        folderUriStr = config.folderUri
                        folderName = config.folderName
                        indexedImageCount = config.imageCount
                    }
                    WallpaperSourceType.MEDIA_STORE -> {
                        val config = MediaStoreSourceConfig.fromJson(entity.configJson)
                        mediaStoreAlbumIds = config.albumIds
                        mediaStoreAlbumNames = config.albumNames
                    }
                    WallpaperSourceType.IMMICH -> {
                        val config = ImmichSourceConfig.fromJson(entity.configJson)
                        immichServerUrl = config.serverUrl
                        immichApiKey = config.apiKey
                        immichAlbumIds = config.albumIds
                        immichAlbumNames = config.albumNames
                        immichQuality = config.quality
                        immichIgnoreSsl = config.ignoreSsl
                        immichWifiOnly = config.wifiOnly
                    }
                    WallpaperSourceType.HTTP_API -> {
                        val config = HttpApiSourceConfig.fromJson(entity.configJson)
                        httpPreset = config.preset
                        httpCustomUrl = config.customUrl
                        httpCustomJsonPath = config.customJsonPath
                        httpWifiOnly = config.wifiOnly
                    }
                    WallpaperSourceType.FAVORITES -> {
                        // favorites count queried below
                    }
                    WallpaperSourceType.COMPOSITE -> {}
                }
            }
        } else {
            // Default title for Add mode
            title = when (selectedType) {
                WallpaperSourceType.LOCAL_FOLDER -> "本地文件夹"
                WallpaperSourceType.MEDIA_STORE -> "系统相册"
                WallpaperSourceType.IMMICH -> "Immich 相册"
                WallpaperSourceType.HTTP_API -> "Bing 每日壁纸"
                WallpaperSourceType.FAVORITES -> "我的收藏"
                WallpaperSourceType.COMPOSITE -> "混合轮播"
            }
        }

        // Query favorites count
        favoritesCount = historyDb.getFavoritesList().size
    }

    // Media permission
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
            coroutineScope.launch {
                mediaStoreAlbums = MediaStoreSource.fetchAlbums(context)
            }
        }
    }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            folderUriStr = uri.toString()
            val docFile = DocumentFile.fromTreeUri(context, uri)
            folderName = docFile?.name ?: "自定义文件夹"
            if (title.isBlank() || title == "本地文件夹") {
                title = folderName
            }
            coroutineScope.launch {
                isScanningFolder = true
                val count = withContext(Dispatchers.IO) {
                    LocalFolderFastScanner.scanFolder(context, uri).size
                }
                indexedImageCount = count
                isScanningFolder = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑壁纸图源" else "添加壁纸图源") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除图源", tint = MaterialTheme.colorScheme.error)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val trimmedTitle = title.trim().ifBlank { selectedType.displayName }

                            // Validation based on type
                            when (selectedType) {
                                WallpaperSourceType.LOCAL_FOLDER -> {
                                    if (folderUriStr.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("请先选择壁纸文件夹") }
                                        return@Button
                                    }
                                }
                                WallpaperSourceType.HTTP_API -> {
                                    if (httpPreset == HttpPresetType.CUSTOM && httpCustomUrl.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("请填写自定义 HTTP 网址") }
                                        return@Button
                                    }
                                }
                                WallpaperSourceType.IMMICH -> {
                                    if (immichServerUrl.isBlank() || immichApiKey.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("请填写 Immich 服务器地址与 API Key") }
                                        return@Button
                                    }
                                }
                                else -> {}
                            }

                            val configJson = when (selectedType) {
                                WallpaperSourceType.LOCAL_FOLDER -> LocalFolderSourceConfig(
                                    folderUri = folderUriStr,
                                    folderName = folderName,
                                    imageCount = indexedImageCount
                                ).toJson()
                                WallpaperSourceType.MEDIA_STORE -> MediaStoreSourceConfig(
                                    albumIds = mediaStoreAlbumIds,
                                    albumNames = mediaStoreAlbumNames
                                ).toJson()
                                WallpaperSourceType.IMMICH -> ImmichSourceConfig(
                                    serverUrl = immichServerUrl,
                                    apiKey = immichApiKey,
                                    albumIds = immichAlbumIds,
                                    albumNames = immichAlbumNames,
                                    quality = immichQuality,
                                    ignoreSsl = immichIgnoreSsl,
                                    wifiOnly = immichWifiOnly
                                ).toJson()
                                WallpaperSourceType.HTTP_API -> HttpApiSourceConfig(
                                    preset = httpPreset,
                                    customUrl = httpCustomUrl,
                                    customJsonPath = httpCustomJsonPath,
                                    wifiOnly = httpWifiOnly
                                ).toJson()
                                WallpaperSourceType.FAVORITES -> FavoritesSourceConfig().toJson()
                                WallpaperSourceType.COMPOSITE -> ""
                            }

                            if (isEditMode && sourceId != null) {
                                val updated = WallpaperSourceEntity(
                                    id = sourceId,
                                    type = selectedType,
                                    title = trimmedTitle,
                                    configJson = configJson
                                )
                                db.updateSource(updated)
                            } else {
                                val newSource = WallpaperSourceEntity(
                                    type = selectedType,
                                    title = trimmedTitle,
                                    isEnabled = true,
                                    configJson = configJson
                                )
                                db.insertSource(newSource)
                            }
                            onFinish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEditMode) "保存更改" else "保存并启用")
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

            // Step 1: Source Type (only selectable in Add mode)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEditMode) "图源类型" else "1. 选择图源类型",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val availableTypes = listOf(
                        WallpaperSourceType.LOCAL_FOLDER,
                        WallpaperSourceType.MEDIA_STORE,
                        WallpaperSourceType.IMMICH,
                        WallpaperSourceType.HTTP_API,
                        WallpaperSourceType.FAVORITES
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTypes.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    if (!isEditMode) {
                                        selectedType = type
                                        title = when (type) {
                                            WallpaperSourceType.LOCAL_FOLDER -> if (folderName.isNotBlank()) folderName else "本地文件夹"
                                            WallpaperSourceType.MEDIA_STORE -> "系统相册"
                                            WallpaperSourceType.IMMICH -> "Immich 相册"
                                            WallpaperSourceType.HTTP_API -> httpPreset.label
                                            WallpaperSourceType.FAVORITES -> "我的收藏"
                                            WallpaperSourceType.COMPOSITE -> "混合轮播"
                                        }
                                    }
                                },
                                enabled = !isEditMode,
                                label = { Text(type.displayName) }
                            )
                        }
                    }
                }
            }

            // Step 2: Name Input
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "图源名称",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("自定义标题") },
                        placeholder = { Text("例如：相机精选壁纸") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Step 3: Type Specific Configuration
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "图源详细配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedType) {
                        WallpaperSourceType.LOCAL_FOLDER -> {
                            Text(
                                text = if (folderUriStr.isNotBlank()) "已选目录: $folderName" else "尚未选择文件夹",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (folderUriStr.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                            if (folderUriStr.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isScanningFolder) "正在扫描图片…" else "检索到 $indexedImageCount 张图片",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (folderUriStr.isNotBlank()) "更换文件夹" else "选择文件夹")
                            }
                        }

                        WallpaperSourceType.MEDIA_STORE -> {
                            if (!hasMediaPermission) {
                                Text(
                                    text = "需要读取相册权限以直接检索系统照片与相机相册",
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
                                Text(
                                    text = "当前选择: $mediaStoreAlbumNames",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isLoadingMediaStoreAlbums = true
                                            mediaStoreAlbums = MediaStoreSource.fetchAlbums(context)
                                            isLoadingMediaStoreAlbums = false
                                            showMediaStorePicker = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isLoadingMediaStoreAlbums) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("选择相册组合")
                                }
                            }
                        }

                        WallpaperSourceType.IMMICH -> {
                            OutlinedTextField(
                                value = immichServerUrl,
                                onValueChange = { immichServerUrl = it },
                                label = { Text("服务器地址 (Server URL)") },
                                placeholder = { Text("https://192.168.1.100:2283") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = immichApiKey,
                                onValueChange = { immichApiKey = it },
                                label = { Text("API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "相册筛选: $immichAlbumNames",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (immichServerUrl.isBlank() || immichApiKey.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("请先填写服务器地址与 API Key") }
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        isLoadingImmichAlbums = true
                                        val result = ImmichSource.fetchAlbums(immichServerUrl, immichApiKey, immichIgnoreSsl)
                                        isLoadingImmichAlbums = false
                                        result.onSuccess {
                                            immichAlbums = it
                                            showImmichPicker = true
                                        }.onFailure {
                                            snackbarHostState.showSnackbar("获取相册失败: ${it.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoadingImmichAlbums) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("选择相册组合")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("下载画质", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    ImmichQuality.PREVIEW to "高清预览 (省流)",
                                    ImmichQuality.ORIGINAL to "原始全尺寸"
                                ).forEach { (quality, label) ->
                                    FilterChip(
                                        selected = immichQuality == quality,
                                        onClick = { immichQuality = quality },
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
                                    Text("忽略自签名证书", style = MaterialTheme.typography.bodyMedium)
                                    Text("局域网自签名 HTTPS 建议开启", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = immichIgnoreSsl, onCheckedChange = { immichIgnoreSsl = it })
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("仅在 Wi-Fi 下下载", style = MaterialTheme.typography.bodyMedium)
                                    Text("移动蜂窝网络下复用缓存或跳过", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = immichWifiOnly, onCheckedChange = { immichWifiOnly = it })
                            }
                        }

                        WallpaperSourceType.HTTP_API -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HttpPresetType.values().forEach { preset ->
                                    FilterChip(
                                        selected = httpPreset == preset,
                                        onClick = {
                                            httpPreset = preset
                                            if (title.isBlank() || HttpPresetType.values().any { it.label == title }) {
                                                title = preset.label
                                            }
                                        },
                                        label = { Text(preset.label) }
                                    )
                                }
                            }

                            if (httpPreset == HttpPresetType.CUSTOM) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = httpCustomUrl,
                                    onValueChange = { httpCustomUrl = it },
                                    label = { Text("API 网址 (URL)") },
                                    placeholder = { Text("https://api.example.com/wallpaper") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = httpCustomJsonPath,
                                    onValueChange = { httpCustomJsonPath = it },
                                    label = { Text("图片字段路径 (可选 JSONPath)") },
                                    placeholder = { Text("例如 images[0].url") },
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
                                    Text("避免在移动网络下消耗流量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = httpWifiOnly, onCheckedChange = { httpWifiOnly = it })
                            }
                        }

                        WallpaperSourceType.FAVORITES -> {
                            Text(
                                text = "离线收藏图源仅从您收藏的历史壁纸中随机挑选。高可靠性、完全离线可用。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "当前已收藏壁纸数量: $favoritesCount 张",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (favoritesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        WallpaperSourceType.COMPOSITE -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // MediaStore Album Picker Sheet
    if (showMediaStorePicker) {
        val pickerItems = remember(mediaStoreAlbums) {
            val list = mutableListOf<PickerAlbumItem>()
            val allPhotosCount = mediaStoreAlbums.firstOrNull { it.id == null }?.count
                ?: mediaStoreAlbums.sumOf { it.count }
            val firstCover = mediaStoreAlbums.firstOrNull { it.coverUri != null }?.coverUri
            list.add(
                PickerAlbumItem(
                    id = null,
                    name = "全部照片 (全库随机)",
                    count = allPhotosCount,
                    coverUri = firstCover
                )
            )
            mediaStoreAlbums.filter { it.id != null }.forEach { album ->
                list.add(
                    PickerAlbumItem(
                        id = album.id,
                        name = album.name,
                        count = album.count,
                        coverUri = album.coverUri
                    )
                )
            }
            list
        }

        AlbumPickerSheet(
            title = "选择系统相册组合",
            albums = pickerItems,
            selectedIds = mediaStoreAlbumIds,
            onSelectionConfirmed = { selectedIds ->
                mediaStoreAlbumIds = selectedIds
                mediaStoreAlbumNames = when {
                    selectedIds.isEmpty() -> "全部照片 (全库随机)"
                    selectedIds.size == 1 -> mediaStoreAlbums.find { it.id == selectedIds.first() }?.name ?: "相册"
                    else -> "已选 ${selectedIds.size} 个相册"
                }
                showMediaStorePicker = false
            },
            onDismissRequest = { showMediaStorePicker = false }
        )
    }

    // Immich Album Picker Sheet
    if (showImmichPicker) {
        val pickerItems = remember(immichAlbums, immichServerUrl, immichApiKey) {
            val list = mutableListOf<PickerAlbumItem>()
            val baseUrl = immichServerUrl.trimEnd('/')
            val totalAssets = immichAlbums.sumOf { it.assetCount }
            val firstThumbnailId = immichAlbums.firstOrNull { !it.thumbnailAssetId.isNullOrBlank() }?.thumbnailAssetId
            list.add(
                PickerAlbumItem(
                    id = null,
                    name = "全部相册 (全库随机)",
                    count = totalAssets,
                    coverUrl = firstThumbnailId?.let { "$baseUrl/api/assets/$it/thumbnail" },
                    apiKey = immichApiKey
                )
            )
            immichAlbums.filter { !it.id.isNullOrBlank() }.forEach { album ->
                list.add(
                    PickerAlbumItem(
                        id = album.id,
                        name = album.name,
                        count = album.assetCount,
                        coverUrl = album.thumbnailAssetId?.let { "$baseUrl/api/assets/$it/thumbnail" },
                        apiKey = immichApiKey
                    )
                )
            }
            list
        }

        AlbumPickerSheet(
            title = "选择 Immich 相册组合",
            albums = pickerItems,
            selectedIds = immichAlbumIds,
            onSelectionConfirmed = { selectedIds ->
                immichAlbumIds = selectedIds
                immichAlbumNames = when {
                    selectedIds.isEmpty() -> "全部相册 (全库随机)"
                    selectedIds.size == 1 -> immichAlbums.find { it.id == selectedIds.first() }?.name ?: "相册"
                    else -> "已选 ${selectedIds.size} 个相册"
                }
                showImmichPicker = false
            },
            onDismissRequest = { showImmichPicker = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除图源") },
            text = { Text("确定要删除「${title.ifBlank { "此图源" }}」吗？删除后此图源将不再参与轮播。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sourceId != null) {
                            db.deleteSource(sourceId)
                        }
                        showDeleteConfirmDialog = false
                        onFinish()
                    }
                ) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
