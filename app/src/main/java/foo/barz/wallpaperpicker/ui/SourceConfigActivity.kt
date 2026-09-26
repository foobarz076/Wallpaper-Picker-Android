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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.core.model.CustomPhotosSourceConfig
import foo.barz.wallpaperpicker.core.source.CustomPhotosSource
import java.io.File
import java.util.UUID
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val workingSourceId = remember(sourceId) { sourceId ?: UUID.randomUUID().toString() }

    var selectedType by remember { mutableStateOf(if (isEditMode) WallpaperSourceType.LOCAL_FOLDER else WallpaperSourceType.CUSTOM_PHOTOS) }
    var title by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Custom Photos state
    var customPhotosList by remember { mutableStateOf<List<File>>(emptyList()) }
    var isImportingPhotos by remember { mutableStateOf(false) }

    // Local Folder state
    var folderUriStr by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var indexedImageCount by remember { mutableStateOf(0) }
    var isScanningFolder by remember { mutableStateOf(false) }

    // MediaStore state
    var mediaStoreAlbumIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mediaStoreAlbumNames by remember { mutableStateOf(context.getString(R.string.source_config_media_all_photos)) }
    var mediaStoreAlbums by remember { mutableStateOf<List<MediaStoreAlbum>>(emptyList()) }
    var isLoadingMediaStoreAlbums by remember { mutableStateOf(false) }
    var showMediaStorePicker by remember { mutableStateOf(false) }

    // Immich state
    var immichServerUrl by remember { mutableStateOf("") }
    var immichApiKey by remember { mutableStateOf("") }
    var immichAlbumIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var immichAlbumNames by remember { mutableStateOf(context.getString(R.string.source_config_immich_all_albums)) }
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
                    WallpaperSourceType.CUSTOM_PHOTOS -> {
                        withContext(Dispatchers.IO) {
                            customPhotosList = CustomPhotosSource.getPhotos(context, workingSourceId)
                        }
                    }
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
            title = context.getString(selectedType.displayNameRes)
        }

        // Query favorites count
        favoritesCount = historyDb.getFavoritesList().size

        // Pre-load MediaStore albums if permission is already available
        if (MediaStoreSource.hasAnyPermission(context)) {
            mediaStoreAlbums = MediaStoreSource.fetchAlbums(context)
        }
    }

    // Media permission state
    var hasFullMediaPermission by remember { mutableStateOf(MediaStoreSource.hasFullPermission(context)) }
    var hasPartialMediaPermission by remember { mutableStateOf(MediaStoreSource.hasPartialPermission(context)) }
    val hasAnyMediaPermission = hasFullMediaPermission || hasPartialMediaPermission

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFullMediaPermission = MediaStoreSource.hasFullPermission(context)
                hasPartialMediaPermission = MediaStoreSource.hasPartialPermission(context)
                if (hasFullMediaPermission || hasPartialMediaPermission) {
                    coroutineScope.launch {
                        mediaStoreAlbums = MediaStoreSource.fetchAlbums(context)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasFullMediaPermission = MediaStoreSource.hasFullPermission(context)
        hasPartialMediaPermission = MediaStoreSource.hasPartialPermission(context)
        if (hasFullMediaPermission || hasPartialMediaPermission) {
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
            try {
                // Persist read access across app restarts and background workers
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                foo.barz.wallpaperpicker.core.util.AppLog.w("SourceConfig", "Failed to take persistable URI permission for $uri", e)
            }

            folderUriStr = uri.toString()
            val docFile = DocumentFile.fromTreeUri(context, uri)
            folderName = docFile?.name ?: context.getString(R.string.source_type_local_folder)
            if (title.isBlank() || title == "本地文件夹" || title == context.getString(WallpaperSourceType.LOCAL_FOLDER.displayNameRes)) {
                title = folderName
            }
            coroutineScope.launch {
                isScanningFolder = true
                val records = withContext(Dispatchers.IO) {
                    try {
                        LocalFolderFastScanner.scanFolder(context, uri)
                    } catch (e: Exception) {
                        foo.barz.wallpaperpicker.core.util.AppLog.w("SourceConfig", "Failed to scan folder $uri", e)
                        emptyList()
                    }
                }
                indexedImageCount = records.size
                if (records.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        foo.barz.wallpaperpicker.core.database.LocalFolderIndexDatabase(context).replaceFolderIndex(uri, records)
                    }
                }
                isScanningFolder = false
            }
        }
    }

    // PhotoPicker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isImportingPhotos = true
                withContext(Dispatchers.IO) {
                    CustomPhotosSource.importPhotos(context, workingSourceId, uris)
                    customPhotosList = CustomPhotosSource.getPhotos(context, workingSourceId)
                }
                isImportingPhotos = false
            }
        }
    }

    val handleCancel = {
        if (!isEditMode) {
            CustomPhotosSource.cleanupSourceDirectory(context, workingSourceId)
        }
        onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) stringResource(R.string.source_config_edit_title) else stringResource(R.string.source_config_add_title)) },
                navigationIcon = {
                    IconButton(onClick = handleCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.source_config_delete_tooltip), tint = MaterialTheme.colorScheme.error)
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
                            val trimmedTitle = title.trim().ifBlank { context.getString(selectedType.displayNameRes) }

                            // Validation based on type
                            when (selectedType) {
                                WallpaperSourceType.CUSTOM_PHOTOS -> {
                                    if (customPhotosList.isEmpty()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_photos_empty)) }
                                        return@Button
                                    }
                                }
                                WallpaperSourceType.LOCAL_FOLDER -> {
                                    if (folderUriStr.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_folder_empty)) }
                                        return@Button
                                    }
                                }
                                WallpaperSourceType.HTTP_API -> {
                                    if (httpPreset == HttpPresetType.CUSTOM && httpCustomUrl.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_http_empty)) }
                                        return@Button
                                    }
                                }
                                WallpaperSourceType.IMMICH -> {
                                    if (immichServerUrl.isBlank() || immichApiKey.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_immich_empty)) }
                                        return@Button
                                    }
                                }
                                WallpaperSourceType.MEDIA_STORE -> {
                                    if (!hasAnyMediaPermission) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_media_permission)) }
                                        return@Button
                                    }
                                    if (hasPartialMediaPermission && (mediaStoreAlbums.firstOrNull()?.count ?: 0) == 0) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_media_empty)) }
                                        return@Button
                                    }
                                }
                                else -> {}
                            }

                            val configJson = when (selectedType) {
                                WallpaperSourceType.CUSTOM_PHOTOS -> CustomPhotosSourceConfig(
                                    fileNames = customPhotosList.map { it.name },
                                    imageCount = customPhotosList.size
                                ).toJson()
                                WallpaperSourceType.LOCAL_FOLDER -> LocalFolderSourceConfig(
                                    folderUri = folderUriStr,
                                    folderName = folderName,
                                    imageCount = indexedImageCount
                                ).toJson()
                                WallpaperSourceType.MEDIA_STORE -> {
                                    val concreteAlbumNames = if (hasPartialMediaPermission) {
                                        val count = mediaStoreAlbums.firstOrNull()?.count ?: 0
                                        if (count > 0) context.getString(R.string.source_config_media_selected_count_format, count) else context.getString(R.string.source_type_media_store)
                                    } else {
                                        mediaStoreAlbumNames
                                    }
                                    MediaStoreSourceConfig(
                                        albumIds = mediaStoreAlbumIds,
                                        albumNames = concreteAlbumNames
                                    ).toJson()
                                }
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
                                    id = workingSourceId,
                                    type = selectedType,
                                    title = trimmedTitle,
                                    isEnabled = true,
                                    configJson = configJson
                                )
                                db.insertSource(newSource)
                                foo.barz.wallpaperpicker.data.PreferencesManager(context).hasUserAddedSource = true
                            }
                            onFinish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEditMode) stringResource(R.string.source_config_btn_save_changes) else stringResource(R.string.source_config_btn_save_and_enable))
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
                        text = if (isEditMode) stringResource(R.string.source_config_step1_edit) else stringResource(R.string.source_config_step1_add),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val availableTypes = listOf(
                        WallpaperSourceType.CUSTOM_PHOTOS,
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
                                            WallpaperSourceType.LOCAL_FOLDER -> if (folderName.isNotBlank()) folderName else context.getString(type.displayNameRes)
                                            WallpaperSourceType.HTTP_API -> context.getString(httpPreset.labelRes)
                                            else -> context.getString(type.displayNameRes)
                                        }
                                    }
                                },
                                enabled = !isEditMode,
                                label = { Text(stringResource(type.displayNameRes)) }
                            )
                        }
                    }
                }
            }

            // Step 2: Name Input
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.source_config_name_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.source_config_name_label)) },
                        placeholder = { Text(stringResource(R.string.source_config_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Step 3: Type Specific Configuration
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.source_config_detail_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedType) {
                        WallpaperSourceType.CUSTOM_PHOTOS -> {
                            Text(
                                text = stringResource(R.string.source_config_photos_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (customPhotosList.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.source_config_photos_none),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isImportingPhotos,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isImportingPhotos) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.source_config_photos_importing))
                                    } else {
                                        Text(stringResource(R.string.source_config_photos_btn_pick))
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.source_config_photos_count_format, customPhotosList.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(customPhotosList, key = { it.name }) { photoFile ->
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = photoFile,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = {
                                                    CustomPhotosSource.deletePhoto(photoFile)
                                                    customPhotosList = CustomPhotosSource.getPhotos(context, workingSourceId)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.action_delete),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isImportingPhotos,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isImportingPhotos) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.source_config_photos_importing))
                                        } else {
                                            Text(stringResource(R.string.source_config_photos_btn_append))
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            CustomPhotosSource.cleanupSourceDirectory(context, workingSourceId)
                                            customPhotosList = emptyList()
                                        },
                                        enabled = !isImportingPhotos,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.source_config_photos_btn_clear))
                                    }
                                }
                            }
                        }

                        WallpaperSourceType.LOCAL_FOLDER -> {
                            Text(
                                text = if (folderUriStr.isNotBlank()) stringResource(R.string.source_config_folder_selected_format, folderName) else stringResource(R.string.source_config_folder_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (folderUriStr.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                            if (folderUriStr.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isScanningFolder) stringResource(R.string.source_config_folder_scanning) else stringResource(R.string.source_config_folder_count_format, indexedImageCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (folderUriStr.isNotBlank()) stringResource(R.string.source_config_folder_btn_change) else stringResource(R.string.source_config_folder_btn_select))
                            }
                        }

                        WallpaperSourceType.MEDIA_STORE -> {
                            if (!hasAnyMediaPermission) {
                                Text(
                                    text = stringResource(R.string.source_config_media_unauthorized),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { mediaPermissionLauncher.launch(MediaStoreSource.getRequiredPermissions()) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.source_config_media_btn_grant))
                                }
                            } else if (hasPartialMediaPermission) {
                                val selectedCount = mediaStoreAlbums.firstOrNull()?.count ?: 0
                                Text(
                                    text = if (selectedCount > 0) stringResource(R.string.source_config_media_partial_format, selectedCount) else stringResource(R.string.source_config_photos_none),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { mediaPermissionLauncher.launch(MediaStoreSource.getRequiredPermissions()) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (selectedCount > 0) stringResource(R.string.source_config_media_btn_manage) else stringResource(R.string.source_config_media_btn_pick))
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.source_config_media_selected_format, mediaStoreAlbumNames),
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
                                    Text(stringResource(R.string.source_config_media_btn_pick_combo))
                                }
                            }
                        }

                        WallpaperSourceType.IMMICH -> {
                            OutlinedTextField(
                                value = immichServerUrl,
                                onValueChange = { immichServerUrl = it },
                                label = { Text(stringResource(R.string.source_config_immich_server_url)) },
                                placeholder = { Text("https://192.168.1.100:2283") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = immichApiKey,
                                onValueChange = { immichApiKey = it },
                                label = { Text(stringResource(R.string.source_config_immich_api_key)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.source_config_immich_album_filter_format, immichAlbumNames),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (immichServerUrl.isBlank() || immichApiKey.isBlank()) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_immich_empty)) }
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
                                            snackbarHostState.showSnackbar(context.getString(R.string.source_config_err_fetch_albums_failed, it.message ?: ""))
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
                                Text(stringResource(R.string.source_config_media_btn_pick_combo))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.source_config_immich_quality), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    ImmichQuality.PREVIEW to stringResource(R.string.source_config_immich_quality_preview),
                                    ImmichQuality.ORIGINAL to stringResource(R.string.source_config_immich_quality_original)
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
                                    Text(stringResource(R.string.source_config_immich_ignore_ssl), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.source_config_immich_ignore_ssl_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                                    Text(stringResource(R.string.source_config_wifi_only), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.source_config_immich_wifi_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                                            if (title.isBlank() || HttpPresetType.values().any { it.label == title || context.getString(it.labelRes) == title }) {
                                                title = context.getString(preset.labelRes)
                                            }
                                        },
                                        label = { Text(stringResource(preset.labelRes)) }
                                    )
                                }
                            }

                            if (httpPreset == HttpPresetType.CUSTOM) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = httpCustomUrl,
                                    onValueChange = { httpCustomUrl = it },
                                    label = { Text(stringResource(R.string.source_config_http_url)) },
                                    placeholder = { Text("https://api.example.com/wallpaper") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = httpCustomJsonPath,
                                    onValueChange = { httpCustomJsonPath = it },
                                    label = { Text(stringResource(R.string.source_config_http_json_path)) },
                                    placeholder = { Text(stringResource(R.string.source_config_http_json_path_hint)) },
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
                                    Text(stringResource(R.string.source_config_wifi_only), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.source_config_http_wifi_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = httpWifiOnly, onCheckedChange = { httpWifiOnly = it })
                            }
                        }

                        WallpaperSourceType.FAVORITES -> {
                            Text(
                                text = stringResource(R.string.source_config_fav_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.source_config_fav_count_format, favoritesCount),
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
        val allPhotosText = stringResource(R.string.source_config_media_all_photos)
        val pickerItems = remember(mediaStoreAlbums, allPhotosText) {
            val list = mutableListOf<PickerAlbumItem>()
            val allPhotosCount = mediaStoreAlbums.firstOrNull { it.id == null }?.count
                ?: mediaStoreAlbums.sumOf { it.count }
            val firstCover = mediaStoreAlbums.firstOrNull { it.coverUri != null }?.coverUri
            list.add(
                PickerAlbumItem(
                    id = null,
                    name = allPhotosText,
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

        val defaultAlbumText = stringResource(R.string.source_type_media_store)
        AlbumPickerSheet(
            title = stringResource(R.string.source_config_media_picker_title),
            albums = pickerItems,
            selectedIds = mediaStoreAlbumIds,
            onSelectionConfirmed = { selectedIds ->
                mediaStoreAlbumIds = selectedIds
                mediaStoreAlbumNames = when {
                    selectedIds.isEmpty() -> context.getString(R.string.source_config_media_all_photos)
                    selectedIds.size == 1 -> mediaStoreAlbums.find { it.id == selectedIds.first() }?.name ?: defaultAlbumText
                    else -> context.getString(R.string.source_config_media_selected_albums_format, selectedIds.size)
                }
                showMediaStorePicker = false
            },
            onDismissRequest = { showMediaStorePicker = false }
        )
    }

    // Immich Album Picker Sheet
    if (showImmichPicker) {
        val allAlbumsText = stringResource(R.string.source_config_immich_all_albums)
        val pickerItems = remember(immichAlbums, immichServerUrl, immichApiKey, allAlbumsText) {
            val list = mutableListOf<PickerAlbumItem>()
            val baseUrl = immichServerUrl.trimEnd('/')
            val totalAssets = immichAlbums.sumOf { it.assetCount }
            val firstThumbnailId = immichAlbums.firstOrNull { !it.thumbnailAssetId.isNullOrBlank() }?.thumbnailAssetId
            list.add(
                PickerAlbumItem(
                    id = null,
                    name = allAlbumsText,
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

        val defaultAlbumText = stringResource(R.string.source_type_immich)
        AlbumPickerSheet(
            title = stringResource(R.string.source_config_immich_picker_title),
            albums = pickerItems,
            selectedIds = immichAlbumIds,
            onSelectionConfirmed = { selectedIds ->
                immichAlbumIds = selectedIds
                immichAlbumNames = when {
                    selectedIds.isEmpty() -> context.getString(R.string.source_config_immich_all_albums)
                    selectedIds.size == 1 -> immichAlbums.find { it.id == selectedIds.first() }?.name ?: defaultAlbumText
                    else -> context.getString(R.string.source_config_media_selected_albums_format, selectedIds.size)
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
            title = { Text(stringResource(R.string.source_config_delete_dialog_title)) },
            text = {
                val displayName = title.ifBlank { stringResource(R.string.source_config_this_source) }
                Text(stringResource(R.string.source_config_delete_dialog_msg, displayName))
            },
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
                    Text(stringResource(R.string.source_config_btn_confirm_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
