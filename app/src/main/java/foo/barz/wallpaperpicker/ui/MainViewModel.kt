package foo.barz.wallpaperpicker.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import foo.barz.wallpaperpicker.core.action.WallpaperActionManager
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.database.LocalFolderFastScanner
import foo.barz.wallpaperpicker.core.database.LocalFolderIndexDatabase
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.source.ImmichSource
import foo.barz.wallpaperpicker.core.source.MediaStoreSource
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.core.worker.WallpaperWorker
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MainUiState(
    val sourceType: WallpaperSourceType = WallpaperSourceType.LOCAL_FOLDER,
    val folderUri: Uri? = null,
    val folderName: String? = null,
    val indexedImageCount: Int = 0,
    val isIndexingFolder: Boolean = false,
    val mediaStoreAlbumId: String? = null,
    val mediaStoreAlbumName: String? = null,
    val mediaStoreAlbums: List<MediaStoreAlbum> = emptyList(),
    val isLoadingMediaStoreAlbums: Boolean = false,
    val httpPresetType: HttpPresetType = HttpPresetType.BING,
    val httpCustomUrl: String = "",
    val httpCustomJsonPath: String = "",
    val wifiOnly: Boolean = true,
    val immichServerUrl: String = "",
    val immichApiKey: String = "",
    val immichAlbumId: String? = null,
    val immichAlbumName: String? = null,
    val immichQuality: ImmichQuality = ImmichQuality.PREVIEW,
    val immichIgnoreSsl: Boolean = false,
    val immichWifiOnly: Boolean = true,
    val immichAlbums: List<ImmichAlbum> = emptyList(),
    val isLoadingAlbums: Boolean = false,
    val cacheSizeBytes: Long = 0L,
    val intervalMinutes: Long = 60L,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val scrollMode: WallpaperScrollMode = WallpaperScrollMode.AUTO,
    val cropMode: WallpaperCropMode = WallpaperCropMode.FIT_HEIGHT,
    val reapplyOnScrollChange: Boolean = true,
    val isScheduled: Boolean = false,
    val isChanging: Boolean = false,
    val isSavingWallpaper: Boolean = false,
    val lastWallpaperTitle: String? = null,
    val lastWallpaperUri: Uri? = null,
    val lastChangedText: String = "尚未更换过",
    val lastExecutionStatus: String? = null,
    val lastErrorMessage: String? = null,
    val deferDuringInteraction: Boolean = true,
    val fairShuffle: Boolean = true,
    val historyList: List<WallpaperHistoryItem> = emptyList(),
    val favoritesList: List<WallpaperHistoryItem> = emptyList(),
    val isCurrentFavorite: Boolean = false,
    val favoritesSizeBytes: Long = 0L,
    val isExportingFavorites: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val processor = WallpaperProcessor(application)
    private val applier = WallpaperApplier(application)
    private val cacheManager = WallpaperCacheManager(application)
    private val folderIndexDb = LocalFolderIndexDatabase(application)
    private val historyDb = WallpaperHistoryDatabase(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            sourceType = prefs.sourceType,
            folderUri = prefs.folderUri,
            folderName = resolveFolderName(prefs.folderUri),
            indexedImageCount = prefs.folderUri?.let { folderIndexDb.getIndexCount(it) } ?: 0,
            mediaStoreAlbumId = prefs.mediaStoreAlbumId,
            mediaStoreAlbumName = prefs.mediaStoreAlbumName,
            httpPresetType = prefs.httpPresetType,
            httpCustomUrl = prefs.httpCustomUrl,
            httpCustomJsonPath = prefs.httpCustomJsonPath,
            wifiOnly = prefs.wifiOnly,
            immichServerUrl = prefs.immichServerUrl,
            immichApiKey = prefs.immichApiKey,
            immichAlbumId = prefs.immichAlbumId,
            immichAlbumName = prefs.immichAlbumName,
            immichQuality = prefs.immichQuality,
            immichIgnoreSsl = prefs.immichIgnoreSsl,
            immichWifiOnly = prefs.immichWifiOnly,
            cacheSizeBytes = cacheManager.getCacheSizeBytes(),
            intervalMinutes = prefs.intervalMinutes,
            target = prefs.target,
            scrollMode = prefs.scrollMode,
            cropMode = prefs.cropMode,
            reapplyOnScrollChange = prefs.reapplyOnScrollChange,
            isScheduled = prefs.isScheduled,
            lastWallpaperTitle = prefs.lastWallpaperTitle,
            lastWallpaperUri = prefs.lastWallpaperUri,
            lastChangedText = formatTimestamp(prefs.lastChangedTimestamp, prefs.lastWallpaperTitle),
            lastExecutionStatus = prefs.lastExecutionStatus,
            lastErrorMessage = prefs.lastErrorMessage,
            deferDuringInteraction = prefs.deferDuringInteraction,
            fairShuffle = prefs.fairShuffle
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Retroactively populate history database with last active wallpaper if empty
        val lastUri = prefs.lastWallpaperUri
        if (lastUri != null && historyDb.getHistoryCount() == 0) {
            historyDb.recordAppliedWallpaper(
                sourceUri = lastUri,
                title = prefs.lastWallpaperTitle,
                sourceType = prefs.sourceType,
                appliedTimestamp = prefs.lastChangedTimestamp
            )
        }
        refreshHistoryAndFavorites()
    }

    fun onSourceTypeSelected(type: WallpaperSourceType) {
        prefs.sourceType = type
        _uiState.update { it.copy(sourceType = type) }
        if (type == WallpaperSourceType.MEDIA_STORE && _uiState.value.mediaStoreAlbums.isEmpty() && hasMediaPermission()) {
            fetchMediaStoreAlbums()
        }
        if (prefs.isScheduled) {
            WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
        }
    }

    fun onFolderSelected(uri: Uri) {
        val context = getApplication<Application>()
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Ignore if already taken or not supported
        }

        prefs.folderUri = uri
        val name = resolveFolderName(uri)
        _uiState.update {
            it.copy(
                folderUri = uri,
                folderName = name,
                statusMessage = "已选择文件夹: $name，正在建立极速索引…"
            )
        }
        rescanFolder(uri)
    }

    fun rescanFolder(targetUri: Uri? = prefs.folderUri) {
        val uri = targetUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isIndexingFolder = true) }
            val records = LocalFolderFastScanner.scanFolder(getApplication(), uri)
            folderIndexDb.replaceFolderIndex(uri, records)
            _uiState.update {
                it.copy(
                    isIndexingFolder = false,
                    indexedImageCount = records.size,
                    statusMessage = "本地文件夹索引完成，共找到 ${records.size} 张图片"
                )
            }
        }
    }

    fun fetchMediaStoreAlbums() {
        if (!hasMediaPermission()) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMediaStoreAlbums = true) }
            val albums = runCatching {
                MediaStoreSource.fetchAlbums(getApplication())
            }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoadingMediaStoreAlbums = false,
                    mediaStoreAlbums = albums
                )
            }
        }
    }

    fun onMediaStoreAlbumSelected(album: MediaStoreAlbum?) {
        prefs.mediaStoreAlbumId = album?.id
        prefs.mediaStoreAlbumName = album?.name
        _uiState.update {
            it.copy(
                mediaStoreAlbumId = album?.id,
                mediaStoreAlbumName = album?.name,
                statusMessage = if (album != null) "已选择相册: ${album.name}" else "已选择: 全部照片"
            )
        }
    }

    fun openCurrentWallpaperInGallery() {
        val uri = _uiState.value.lastWallpaperUri ?: return
        viewModelScope.launch {
            val result = WallpaperActionManager.openInGallery(getApplication(), uri)
            if (result.isFailure) {
                _uiState.update { it.copy(statusMessage = "打开图库失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun shareCurrentWallpaper() {
        val uri = _uiState.value.lastWallpaperUri ?: return
        viewModelScope.launch {
            val result = WallpaperActionManager.shareWallpaper(
                getApplication(),
                uri,
                _uiState.value.lastWallpaperTitle
            )
            if (result.isFailure) {
                _uiState.update { it.copy(statusMessage = "调起分享失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun saveCurrentWallpaperToGallery() {
        val uri = _uiState.value.lastWallpaperUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingWallpaper = true, statusMessage = "正在保存到相册…") }
            val result = WallpaperActionManager.saveToGallery(
                getApplication(),
                uri,
                _uiState.value.lastWallpaperTitle
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSavingWallpaper = false,
                        statusMessage = "成功保存至相册 (Pictures/Wallpapers)"
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "保存失败"
                _uiState.update {
                    it.copy(
                        isSavingWallpaper = false,
                        statusMessage = "保存失败: $error"
                    )
                }
            }
        }
    }

    fun onHttpPresetSelected(preset: HttpPresetType) {
        prefs.httpPresetType = preset
        _uiState.update { it.copy(httpPresetType = preset) }
    }

    fun onHttpCustomUrlChanged(url: String) {
        prefs.httpCustomUrl = url
        _uiState.update { it.copy(httpCustomUrl = url) }
    }

    fun onHttpCustomJsonPathChanged(path: String) {
        prefs.httpCustomJsonPath = path
        _uiState.update { it.copy(httpCustomJsonPath = path) }
    }

    fun onToggleWifiOnly(enabled: Boolean) {
        prefs.wifiOnly = enabled
        _uiState.update { it.copy(wifiOnly = enabled) }
        if (prefs.isScheduled && prefs.sourceType == WallpaperSourceType.HTTP_API) {
            WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
        }
    }

    fun onImmichServerUrlChanged(url: String) {
        prefs.immichServerUrl = url
        _uiState.update { it.copy(immichServerUrl = url) }
    }

    fun onImmichApiKeyChanged(key: String) {
        prefs.immichApiKey = key
        _uiState.update { it.copy(immichApiKey = key) }
    }

    fun onImmichAlbumSelected(album: ImmichAlbum?) {
        prefs.immichAlbumId = album?.id
        prefs.immichAlbumName = album?.name
        _uiState.update {
            it.copy(
                immichAlbumId = album?.id,
                immichAlbumName = album?.name,
                statusMessage = if (album != null) "已选择相册: ${album.name}" else "已选择: 全部相册"
            )
        }
    }

    fun onImmichQualitySelected(quality: ImmichQuality) {
        prefs.immichQuality = quality
        _uiState.update { it.copy(immichQuality = quality) }
    }

    fun onToggleImmichIgnoreSsl(enabled: Boolean) {
        prefs.immichIgnoreSsl = enabled
        _uiState.update { it.copy(immichIgnoreSsl = enabled) }
    }

    fun onToggleImmichWifiOnly(enabled: Boolean) {
        prefs.immichWifiOnly = enabled
        _uiState.update { it.copy(immichWifiOnly = enabled) }
        if (prefs.isScheduled && prefs.sourceType == WallpaperSourceType.IMMICH) {
            WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
        }
    }

    fun fetchImmichAlbums() {
        val serverUrl = prefs.immichServerUrl
        val apiKey = prefs.immichApiKey
        if (serverUrl.isBlank() || apiKey.isBlank()) {
            _uiState.update { it.copy(statusMessage = "请先填写 Immich 服务器地址与 API Key") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlbums = true, statusMessage = "正在获取 Immich 相册列表…") }
            val result = ImmichSource.fetchAlbums(serverUrl, apiKey, prefs.immichIgnoreSsl)
            result.onSuccess { albums ->
                _uiState.update {
                    it.copy(
                        isLoadingAlbums = false,
                        immichAlbums = albums,
                        statusMessage = "成功获取到 ${albums.size} 个相册"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingAlbums = false,
                        statusMessage = "获取相册失败: ${error.message}"
                    )
                }
            }
        }
    }

    fun onClearCache() {
        val oldCacheSize = cacheManager.getCacheSizeBytes()
        val success = cacheManager.clearCache()
        val newSize = cacheManager.getCacheSizeBytes()
        val freedBytes = maxOf(0L, oldCacheSize - newSize)
        val freedFormatted = formatBytes(freedBytes)
        val favCount = _uiState.value.favoritesList.size
        _uiState.update {
            it.copy(
                cacheSizeBytes = newSize,
                statusMessage = if (success) {
                    "临时缓存已清理 (释放了 $freedFormatted)，$favCount 张已收藏壁纸受离线持久保护不受影响"
                } else {
                    "清理缓存失败"
                }
            )
        }
    }

    fun onIntervalSelected(minutes: Long) {
        prefs.intervalMinutes = minutes
        _uiState.update { it.copy(intervalMinutes = minutes) }

        if (prefs.isScheduled) {
            WallpaperWorker.schedule(getApplication(), minutes)
        }
    }

    fun onTargetSelected(target: WallpaperTarget) {
        prefs.target = target
        _uiState.update { it.copy(target = target) }
    }

    fun onScrollModeSelected(mode: WallpaperScrollMode) {
        prefs.scrollMode = mode
        _uiState.update { it.copy(scrollMode = mode) }
        if (prefs.reapplyOnScrollChange && _uiState.value.lastWallpaperUri != null) {
            reapplyCurrentWallpaper()
        }
    }

    fun onCropModeSelected(mode: WallpaperCropMode) {
        prefs.cropMode = mode
        _uiState.update { it.copy(cropMode = mode) }
        if (prefs.reapplyOnScrollChange && _uiState.value.lastWallpaperUri != null) {
            reapplyCurrentWallpaper()
        }
    }

    fun onToggleReapplyOnScrollChange(enabled: Boolean) {
        prefs.reapplyOnScrollChange = enabled
        _uiState.update { it.copy(reapplyOnScrollChange = enabled) }
    }

    fun onToggleDeferDuringInteraction(enabled: Boolean) {
        prefs.deferDuringInteraction = enabled
        _uiState.update { it.copy(deferDuringInteraction = enabled) }
    }

    fun onToggleFairShuffle(enabled: Boolean) {
        prefs.fairShuffle = enabled
        _uiState.update { it.copy(fairShuffle = enabled) }
        if (!enabled) {
            prefs.clearRecentWallpaperKeys()
        }
    }

    fun reapplyCurrentWallpaper() {
        val uri = _uiState.value.lastWallpaperUri ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChanging = true,
                    statusMessage = "正在按「${prefs.cropMode.label} + ${prefs.scrollMode.label}」重新应用壁纸…"
                )
            }
            val processResult = processor.process(
                openStream = {
                    if (uri.scheme == "file") {
                        java.io.FileInputStream(java.io.File(uri.path!!))
                    } else {
                        getApplication<Application>().contentResolver.openInputStream(uri)
                            ?: throw java.io.FileNotFoundException("无法打开图片流: $uri")
                    }
                },
                scrollMode = prefs.scrollMode,
                cropMode = prefs.cropMode
            )

            if (processResult.isFailure) {
                val error = processResult.exceptionOrNull()?.message ?: "图片处理失败"
                _uiState.update { it.copy(isChanging = false, statusMessage = error) }
                return@launch
            }

            val bitmap = processResult.getOrThrow()
            val applyResult = applier.apply(bitmap, prefs.target)

            if (applyResult.isFailure) {
                val error = applyResult.exceptionOrNull()?.message ?: "设置壁纸失败"
                _uiState.update { it.copy(isChanging = false, statusMessage = error) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isChanging = false,
                    statusMessage = "已按「${prefs.cropMode.label} + ${prefs.scrollMode.label}」重新应用当前壁纸"
                )
            }
        }
    }

    fun toggleSchedule(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled) {
            when (prefs.sourceType) {
                WallpaperSourceType.LOCAL_FOLDER -> {
                    if (prefs.folderUri == null) {
                        _uiState.update { it.copy(statusMessage = "请先选择壁纸文件夹") }
                        return
                    }
                }
                WallpaperSourceType.MEDIA_STORE -> {
                    if (!hasMediaPermission()) {
                        _uiState.update { it.copy(statusMessage = "请先授予相册访问权限") }
                        return
                    }
                }
                WallpaperSourceType.HTTP_API -> {
                    if (prefs.httpPresetType == HttpPresetType.CUSTOM && prefs.httpCustomUrl.isBlank()) {
                        _uiState.update { it.copy(statusMessage = "请先输入自定义 API 网址") }
                        return
                    }
                }
                WallpaperSourceType.IMMICH -> {
                    if (prefs.immichServerUrl.isBlank() || prefs.immichApiKey.isBlank()) {
                        _uiState.update { it.copy(statusMessage = "请先配置 Immich 服务器地址与 API Key") }
                        return
                    }
                }
            }
        }

        prefs.isScheduled = enabled
        _uiState.update { it.copy(isScheduled = enabled) }

        if (enabled) {
            WallpaperWorker.schedule(context, prefs.intervalMinutes)
            _uiState.update { it.copy(statusMessage = "已启动定时切换 (每 ${prefs.intervalMinutes} 分钟)") }
        } else {
            WallpaperWorker.cancel(context)
            _uiState.update { it.copy(statusMessage = "已停止定时切换") }
        }
    }

    fun changeNow() {
        when (prefs.sourceType) {
            WallpaperSourceType.LOCAL_FOLDER -> {
                if (prefs.folderUri == null) {
                    _uiState.update { it.copy(statusMessage = "请先选择壁纸文件夹") }
                    return
                }
            }
            WallpaperSourceType.MEDIA_STORE -> {
                if (!hasMediaPermission()) {
                    _uiState.update { it.copy(statusMessage = "请先授予相册访问权限") }
                    return
                }
            }
            WallpaperSourceType.HTTP_API -> {
                if (prefs.httpPresetType == HttpPresetType.CUSTOM && prefs.httpCustomUrl.isBlank()) {
                    _uiState.update { it.copy(statusMessage = "请先输入自定义 API 网址") }
                    return
                }
            }
            WallpaperSourceType.IMMICH -> {
                if (prefs.immichServerUrl.isBlank() || prefs.immichApiKey.isBlank()) {
                    _uiState.update { it.copy(statusMessage = "请先配置 Immich 服务器地址与 API Key") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChanging = true, statusMessage = "正在更换壁纸…") }

            val sourceResult = runCatching {
                // User-triggered manual update bypasses network/wifi restrictions
                WallpaperSourceFactory.createActiveSource(
                    getApplication(),
                    prefs,
                    bypassNetworkConstraints = true
                )
            }

            if (sourceResult.isFailure) {
                val error = sourceResult.exceptionOrNull()?.message ?: "初始化图源失败"
                prefs.lastErrorMessage = error
                prefs.lastExecutionStatus = "失败: $error"
                prefs.lastExecutionTimestamp = System.currentTimeMillis()
                _uiState.update { it.copy(isChanging = false, statusMessage = error, lastErrorMessage = error, lastExecutionStatus = "失败: $error") }
                return@launch
            }

            val source = sourceResult.getOrThrow()
            val nextResult = source.getNextWallpaper()

            if (nextResult.isFailure) {
                val error = nextResult.exceptionOrNull()?.message ?: "获取图片失败"
                prefs.lastErrorMessage = error
                prefs.lastExecutionStatus = "失败: $error"
                prefs.lastExecutionTimestamp = System.currentTimeMillis()
                _uiState.update { it.copy(isChanging = false, statusMessage = error, lastErrorMessage = error, lastExecutionStatus = "失败: $error") }
                return@launch
            }

            val data = nextResult.getOrThrow()
            val processResult = processor.process(data.openStream, prefs.scrollMode, prefs.cropMode)

            if (processResult.isFailure) {
                val error = processResult.exceptionOrNull()?.message ?: "图片处理失败"
                prefs.lastErrorMessage = error
                prefs.lastExecutionStatus = "失败: $error"
                prefs.lastExecutionTimestamp = System.currentTimeMillis()
                _uiState.update { it.copy(isChanging = false, statusMessage = error, lastErrorMessage = error, lastExecutionStatus = "失败: $error") }
                return@launch
            }

            val bitmap = processResult.getOrThrow()
            val applyResult = applier.apply(bitmap, prefs.target)

            if (applyResult.isFailure) {
                val error = applyResult.exceptionOrNull()?.message ?: "设置壁纸失败"
                prefs.lastErrorMessage = error
                prefs.lastExecutionStatus = "失败: $error"
                prefs.lastExecutionTimestamp = System.currentTimeMillis()
                _uiState.update { it.copy(isChanging = false, statusMessage = error, lastErrorMessage = error, lastExecutionStatus = "失败: $error") }
                return@launch
            }

            val now = System.currentTimeMillis()
            prefs.lastChangedTimestamp = now
            prefs.lastExecutionTimestamp = now
            prefs.lastWallpaperTitle = data.title
            prefs.lastWallpaperUri = data.sourceUri
            prefs.lastErrorMessage = null
            prefs.lastExecutionStatus = "成功"

            val wallpaperKey = data.sourceUri?.toString() ?: data.title
            if (wallpaperKey != null) {
                prefs.recordRecentWallpaperKey(wallpaperKey)
            }

            data.sourceUri?.let { uri ->
                historyDb.recordAppliedWallpaper(
                    sourceUri = uri,
                    title = data.title,
                    sourceType = prefs.sourceType,
                    appliedTimestamp = now
                )
            }

            val history = historyDb.getHistoryList()
            val favorites = historyDb.getFavoritesList()
            val isFav = data.sourceUri?.let { uri -> favorites.any { it.sourceUri == uri.toString() } } ?: false

            _uiState.update {
                it.copy(
                    isChanging = false,
                    lastWallpaperTitle = data.title,
                    lastWallpaperUri = data.sourceUri,
                    lastChangedText = formatTimestamp(now, data.title),
                    lastErrorMessage = null,
                    lastExecutionStatus = "成功",
                    cacheSizeBytes = cacheManager.getCacheSizeBytes(),
                    historyList = history,
                    favoritesList = favorites,
                    isCurrentFavorite = isFav,
                    statusMessage = "更换成功: ${data.title ?: "未知图片"}"
                )
            }
        }
    }

    fun openUriInGallery(uri: Uri) {
        viewModelScope.launch {
            val result = WallpaperActionManager.openInGallery(getApplication(), uri)
            if (result.isFailure) {
                _uiState.update { it.copy(statusMessage = "无法打开图库: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun shareUri(uri: Uri, title: String?) {
        viewModelScope.launch {
            val result = WallpaperActionManager.shareWallpaper(getApplication(), uri, title)
            if (result.isFailure) {
                _uiState.update { it.copy(statusMessage = "分享失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun saveUriToGallery(uri: Uri, preferredName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingWallpaper = true) }
            val result = WallpaperActionManager.saveToGallery(getApplication(), uri, preferredName)
            _uiState.update {
                it.copy(
                    isSavingWallpaper = false,
                    statusMessage = if (result.isSuccess) "已保存到相册 Pictures/Wallpapers" else "保存失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun refreshHistoryAndFavorites() {
        val history = historyDb.getHistoryList()
        val favorites = historyDb.getFavoritesList()
        val currentUri = prefs.lastWallpaperUri?.toString()
        val isFav = if (currentUri != null) {
            favorites.any { it.sourceUri == currentUri }
        } else {
            false
        }
        val favDir = File(getApplication<Application>().filesDir, "favorites")
        val favSizeBytes = if (favDir.exists()) favDir.listFiles()?.sumOf { it.length() } ?: 0L else 0L
        _uiState.update {
            it.copy(
                historyList = history,
                favoritesList = favorites,
                isCurrentFavorite = isFav,
                favoritesSizeBytes = favSizeBytes
            )
        }
    }

    fun toggleFavorite(item: WallpaperHistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val feedbackMsg: String
            if (item.isFavorite) {
                if (!item.favoriteFilePath.isNullOrBlank()) {
                    val file = File(item.favoriteFilePath)
                    if (file.exists()) file.delete()
                }
                historyDb.updateFavorite(
                    id = item.id,
                    isFavorite = false,
                    favoriteTimestamp = null,
                    favoriteFilePath = null
                )
                feedbackMsg = "已从收藏中移除"
            } else {
                val isNetworkSource = item.sourceType == WallpaperSourceType.IMMICH || item.sourceType == WallpaperSourceType.HTTP_API
                val promotedPath = if (isNetworkSource) {
                    promoteToPermanentFavorite(Uri.parse(item.sourceUri))
                } else {
                    null
                }
                historyDb.updateFavorite(
                    id = item.id,
                    isFavorite = true,
                    favoriteTimestamp = System.currentTimeMillis(),
                    favoriteFilePath = promotedPath
                )
                feedbackMsg = if (isNetworkSource) {
                    "已加入收藏并离线固化（免受缓存清理影响）"
                } else {
                    "已加入收藏（已轻量持久化原图索引）"
                }
            }
            withContext(Dispatchers.Main) {
                refreshHistoryAndFavorites()
                _uiState.update { it.copy(statusMessage = feedbackMsg) }
            }
        }
    }

    fun toggleFavoriteCurrent() {
        val currentUri = prefs.lastWallpaperUri ?: return
        val currentUriStr = currentUri.toString()
        val existing = historyDb.getItemByUri(currentUriStr)
        if (existing != null) {
            toggleFavorite(existing)
        } else {
            val id = historyDb.recordAppliedWallpaper(
                sourceUri = currentUri,
                title = prefs.lastWallpaperTitle,
                sourceType = prefs.sourceType,
                appliedTimestamp = prefs.lastChangedTimestamp
            )
            historyDb.getItemById(id)?.let { toggleFavorite(it) }
        }
    }

    private fun promoteToPermanentFavorite(sourceUri: Uri): String? {
        val context = getApplication<Application>()
        val favDir = File(context.filesDir, "favorites").apply { if (!exists()) mkdirs() }
        val targetFile = File(favDir, "fav_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteHistoryItem(item: WallpaperHistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!item.favoriteFilePath.isNullOrBlank()) {
                val file = File(item.favoriteFilePath)
                if (file.exists()) file.delete()
            }
            historyDb.deleteRecord(item.id)
            withContext(Dispatchers.Main) {
                refreshHistoryAndFavorites()
                _uiState.update { it.copy(statusMessage = "已删除历史记录") }
            }
        }
    }

    fun clearUnfavoritedHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDb.clearUnfavoritedHistory()
            withContext(Dispatchers.Main) {
                refreshHistoryAndFavorites()
                _uiState.update { it.copy(statusMessage = "已清除非收藏历史记录") }
            }
        }
    }

    fun applyWallpaperFromHistory(item: WallpaperHistoryItem) {
        if (_uiState.value.isChanging) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChanging = true, statusMessage = "正在应用所选壁纸…") }
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = item.displayUri
                    val streamProvider = {
                        context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("无法打开图片流")
                    }
                    val bitmap = processor.process(streamProvider, prefs.scrollMode, prefs.cropMode).getOrThrow()
                    applier.apply(bitmap, prefs.target).getOrThrow()
                }
            }
            if (result.isSuccess) {
                val now = System.currentTimeMillis()
                prefs.lastChangedTimestamp = now
                prefs.lastWallpaperTitle = item.title
                prefs.lastWallpaperUri = Uri.parse(item.sourceUri)
                prefs.lastErrorMessage = null
                prefs.lastExecutionStatus = "成功"
                historyDb.recordAppliedWallpaper(Uri.parse(item.sourceUri), item.title, item.sourceType, now)
                refreshHistoryAndFavorites()
                _uiState.update {
                    it.copy(
                        isChanging = false,
                        lastWallpaperTitle = item.title,
                        lastWallpaperUri = Uri.parse(item.sourceUri),
                        lastChangedText = formatTimestamp(now, item.title),
                        lastErrorMessage = null,
                        lastExecutionStatus = "成功",
                        statusMessage = "壁纸已更换: ${item.title ?: "历史壁纸"}"
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "应用壁纸失败"
                _uiState.update {
                    it.copy(
                        isChanging = false,
                        lastErrorMessage = error,
                        statusMessage = error
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun clearErrorMessage() {
        prefs.lastErrorMessage = null
        prefs.lastExecutionStatus = null
        _uiState.update { it.copy(lastErrorMessage = null, lastExecutionStatus = null) }
    }

    private fun resolveFolderName(uri: Uri?): String? {
        if (uri == null) return null
        val doc = DocumentFile.fromTreeUri(getApplication(), uri)
        return doc?.name ?: uri.lastPathSegment
    }

    private fun formatTimestamp(timestamp: Long, title: String?): String {
        if (timestamp <= 0L) return "尚未更换过"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeStr = df.format(Date(timestamp))
        return if (title.isNullOrEmpty()) timeStr else "$timeStr ($title)"
    }

    /**
     * Exports all user favorites to the public Pictures/Wallpapers gallery directory.
     */
    fun exportAllFavoritesToGallery() {
        val favorites = _uiState.value.favoritesList
        if (favorites.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "暂无收藏壁纸可导出") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingFavorites = true,
                    statusMessage = "正在导出 ${favorites.size} 张收藏壁纸到系统相册…"
                )
            }
            var successCount = 0
            withContext(Dispatchers.IO) {
                for (item in favorites) {
                    val uri = item.displayUri
                    val title = item.title ?: "favorite_${item.id}"
                    val result = WallpaperActionManager.saveToGallery(getApplication(), uri, title)
                    if (result.isSuccess) {
                        successCount++
                    }
                }
            }
            _uiState.update {
                it.copy(
                    isExportingFavorites = false,
                    statusMessage = if (successCount == favorites.size) {
                        "已成功将全部 ${favorites.size} 张收藏壁纸导出到相册 Pictures/Wallpapers"
                    } else {
                        "导出完成: 成功 $successCount 张，失败 ${favorites.size - successCount} 张"
                    }
                )
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
    }
}
