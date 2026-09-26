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
import foo.barz.wallpaperpicker.core.database.WallpaperSourcesDatabase
import foo.barz.wallpaperpicker.core.database.ScheduleRulesDatabase
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.core.model.WallpaperSourceEntity
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
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.source.ImmichSource
import foo.barz.wallpaperpicker.core.source.MediaStoreSource
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.core.widget.CurrentWallpaperWidgetProvider
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import foo.barz.wallpaperpicker.core.worker.ScreenOffWatcherService
import foo.barz.wallpaperpicker.core.worker.WallpaperAlarmScheduler
import foo.barz.wallpaperpicker.core.worker.WallpaperChangeExecutor
import foo.barz.wallpaperpicker.core.worker.WallpaperExecutionResult
import foo.barz.wallpaperpicker.core.worker.WallpaperSchedulerHelper
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request

data class MainUiState(
    val sourceType: WallpaperSourceType = WallpaperSourceType.LOCAL_FOLDER,
    val folderUri: Uri? = null,
    val folderName: String? = null,
    val indexedImageCount: Int = 0,
    val isIndexingFolder: Boolean = false,
    val mediaStoreAlbumId: String? = null,
    val mediaStoreAlbumName: String? = null,
    val mediaStoreAlbumIds: Set<String> = emptySet(),
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
    val immichAlbumIds: Set<String> = emptySet(),
    val immichQuality: ImmichQuality = ImmichQuality.PREVIEW,
    val immichIgnoreSsl: Boolean = false,
    val immichWifiOnly: Boolean = true,
    val immichAlbums: List<ImmichAlbum> = emptyList(),
    val isLoadingAlbums: Boolean = false,
    val compositeEnabledSources: Set<WallpaperSourceType> = emptySet(),
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
    val fairShuffleCapacity: Int = 50,
    val fairShuffleRecordedCount: Int = 0,
    val cacheSizeTier: CacheSizeTier = CacheSizeTier.STANDARD,
    val historyList: List<WallpaperHistoryItem> = emptyList(),
    val favoritesList: List<WallpaperHistoryItem> = emptyList(),
    val isCurrentFavorite: Boolean = false,
    val currentWallpaperItem: WallpaperHistoryItem? = null,
    val favoritesSizeBytes: Long = 0L,
    val isExportingFavorites: Boolean = false,
    val isRedownloadingHistoryId: Long? = null,
    val sourcesList: List<WallpaperSourceEntity> = emptyList(),
    val widgetScaleType: WidgetScaleType = WidgetScaleType.CROP,
    val intervalScheduleEnabled: Boolean = true,
    val exactTimerEnabled: Boolean = false,
    val dailyAnchorEnabled: Boolean = false,
    val dailyAnchorHour: Int = 8,
    val dailyAnchorMinute: Int = 0,
    val dailyAnchorTimes: Set<String> = setOf("08:00"),
    val screenOffTriggerEnabled: Boolean = false,
    val screenOffDelaySeconds: Int = 3,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 23,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    val cooldownSuppressionEnabled: Boolean = true,
    val cooldownMinutes: Long = 10L,
    val ruleEngineEnabled: Boolean = false,
    val scheduleRules: List<ScheduleRule> = emptyList(),
    val isRuleDialogOpen: Boolean = false,
    val editingRule: ScheduleRule? = null,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val processor = WallpaperProcessor(application)
    private val applier = WallpaperApplier(application)
    private val cacheManager = WallpaperCacheManager(application)
    private val folderIndexDb = LocalFolderIndexDatabase(application)
    private val historyDb = WallpaperHistoryDatabase(application)
    private val sourcesDb = WallpaperSourcesDatabase(application)
    private val scheduleRulesDb = ScheduleRulesDatabase(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            sourceType = prefs.sourceType,
            folderUri = prefs.folderUri,
            folderName = resolveFolderName(prefs.folderUri),
            indexedImageCount = prefs.folderUri?.let { folderIndexDb.getIndexCount(it) } ?: 0,
            mediaStoreAlbumId = prefs.mediaStoreAlbumId,
            mediaStoreAlbumName = prefs.mediaStoreAlbumName,
            mediaStoreAlbumIds = prefs.mediaStoreAlbumIds,
            httpPresetType = prefs.httpPresetType,
            httpCustomUrl = prefs.httpCustomUrl,
            httpCustomJsonPath = prefs.httpCustomJsonPath,
            wifiOnly = prefs.wifiOnly,
            immichServerUrl = prefs.immichServerUrl,
            immichApiKey = prefs.immichApiKey,
            immichAlbumId = prefs.immichAlbumId,
            immichAlbumName = prefs.immichAlbumName,
            immichAlbumIds = prefs.immichAlbumIds,
            immichQuality = prefs.immichQuality,
            immichIgnoreSsl = prefs.immichIgnoreSsl,
            immichWifiOnly = prefs.immichWifiOnly,
            compositeEnabledSources = prefs.compositeEnabledSources,
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
            fairShuffle = prefs.fairShuffle,
            fairShuffleCapacity = prefs.fairShuffleCapacity,
            fairShuffleRecordedCount = prefs.getRecentWallpaperKeys().size,
            cacheSizeTier = prefs.cacheSizeTier,
            widgetScaleType = prefs.widgetScaleType,
            intervalScheduleEnabled = prefs.intervalScheduleEnabled,
            exactTimerEnabled = prefs.exactTimerEnabled,
            dailyAnchorEnabled = prefs.dailyAnchorEnabled,
            dailyAnchorHour = prefs.dailyAnchorHour,
            dailyAnchorMinute = prefs.dailyAnchorMinute,
            dailyAnchorTimes = prefs.dailyAnchorTimes,
            screenOffTriggerEnabled = prefs.screenOffTriggerEnabled,
            screenOffDelaySeconds = prefs.screenOffDelaySeconds,
            quietHoursEnabled = prefs.quietHoursEnabled,
            quietHoursStartHour = prefs.quietHoursStartHour,
            quietHoursStartMinute = prefs.quietHoursStartMinute,
            quietHoursEndHour = prefs.quietHoursEndHour,
            quietHoursEndMinute = prefs.quietHoursEndMinute,
            cooldownSuppressionEnabled = prefs.cooldownSuppressionEnabled,
            cooldownMinutes = prefs.cooldownMinutes,
            ruleEngineEnabled = prefs.ruleEngineEnabled
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            PreferencesManager.KEY_IS_SCHEDULED -> {
                _uiState.update { it.copy(isScheduled = prefs.isScheduled) }
            }
            PreferencesManager.KEY_LAST_TIMESTAMP,
            PreferencesManager.KEY_LAST_TITLE,
            PreferencesManager.KEY_LAST_URI -> {
                _uiState.update {
                    it.copy(
                        lastWallpaperTitle = prefs.lastWallpaperTitle,
                        lastWallpaperUri = prefs.lastWallpaperUri,
                        lastChangedText = formatTimestamp(prefs.lastChangedTimestamp, prefs.lastWallpaperTitle),
                        lastExecutionStatus = prefs.lastExecutionStatus,
                        lastErrorMessage = prefs.lastErrorMessage,
                        cacheSizeBytes = cacheManager.getCacheSizeBytes(),
                        fairShuffleRecordedCount = prefs.getRecentWallpaperKeys().size
                    )
                }
                refreshHistoryAndFavorites()
            }
        }
    }

    init {
        // Retroactively populate history database with last active wallpaper if empty
        val lastUri = prefs.lastWallpaperUri
        if (lastUri != null && historyDb.getHistoryCount() == 0) {
            val concreteType = prefs.lastWallpaperSourceType ?: prefs.sourceType
            val concreteTitle = prefs.lastWallpaperSourceTitle
            historyDb.recordAppliedWallpaper(
                sourceUri = lastUri,
                title = prefs.lastWallpaperTitle,
                sourceType = concreteType,
                appliedTimestamp = prefs.lastChangedTimestamp,
                sourceTitle = concreteTitle
            )
        }
        sourcesDb.migrateFromPreferencesIfNeeded(prefs)
        refreshHistoryAndFavorites()
        reloadSources()
        loadScheduleRules()
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    fun reloadSources() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                sourcesDb.getAllSources()
            }
            _uiState.update { it.copy(sourcesList = list) }
        }
    }

    fun onToggleSourceEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentSources = _uiState.value.sourcesList
            val enabledCount = currentSources.count { it.isEnabled }
            if (!enabled && enabledCount <= 1 && currentSources.any { it.id == sourceId && it.isEnabled }) {
                _uiState.update { it.copy(statusMessage = "至少需要保留一个启用的壁纸图源") }
                return@launch
            }
            withContext(Dispatchers.IO) {
                sourcesDb.updateSourceEnabled(sourceId, enabled)
            }
            reloadSources()
            if (prefs.isScheduled) {
                WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
            }
        }
    }

    fun onDeleteSource(sourceId: String) {
        viewModelScope.launch {
            val currentSources = _uiState.value.sourcesList
            val target = currentSources.find { it.id == sourceId } ?: return@launch
            if (currentSources.size <= 1) {
                _uiState.update { it.copy(statusMessage = "无法删除最后一个图源") }
                return@launch
            }
            withContext(Dispatchers.IO) {
                sourcesDb.deleteSource(sourceId)
            }
            reloadSources()
            _uiState.update { it.copy(statusMessage = "已删除图源: ${target.title}") }
        }
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
        prefs.mediaStoreAlbumIds = album?.id?.let { setOf(it) } ?: emptySet()
        _uiState.update {
            it.copy(
                mediaStoreAlbumId = album?.id,
                mediaStoreAlbumIds = album?.id?.let { setOf(it) } ?: emptySet(),
                mediaStoreAlbumName = album?.name,
                statusMessage = if (album != null) "已选择相册: ${album.name}" else "已选择: 全部照片"
            )
        }
    }

    fun onMediaStoreAlbumsSelected(albumIds: Set<String>, albums: List<MediaStoreAlbum>) {
        prefs.mediaStoreAlbumIds = albumIds
        val albumName = when {
            albumIds.isEmpty() -> "全部照片 (全库随机)"
            albumIds.size == 1 -> {
                albums.find { it.id == albumIds.first() }?.name ?: "相册"
            }
            else -> {
                val names = albumIds.mapNotNull { id -> albums.find { it.id == id }?.name }.take(2).joinToString(", ")
                "已选 ${albumIds.size} 个相册 ($names…)"
            }
        }
        prefs.mediaStoreAlbumName = albumName
        _uiState.update {
            it.copy(
                mediaStoreAlbumId = albumIds.firstOrNull(),
                mediaStoreAlbumIds = albumIds,
                mediaStoreAlbumName = albumName,
                statusMessage = "系统相册已更新: $albumName"
            )
        }
        if (prefs.isScheduled && prefs.sourceType == WallpaperSourceType.MEDIA_STORE) {
            WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
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
        prefs.immichAlbumIds = album?.id?.let { setOf(it) } ?: emptySet()
        _uiState.update {
            it.copy(
                immichAlbumId = album?.id,
                immichAlbumIds = album?.id?.let { setOf(it) } ?: emptySet(),
                immichAlbumName = album?.name,
                statusMessage = if (album != null) "已选择相册: ${album.name}" else "已选择: 全部相册"
            )
        }
    }

    fun onImmichAlbumsSelected(albumIds: Set<String>, albums: List<ImmichAlbum>) {
        prefs.immichAlbumIds = albumIds
        val albumName = when {
            albumIds.isEmpty() -> "全部相册 (全库随机)"
            albumIds.size == 1 -> {
                albums.find { it.id == albumIds.first() }?.name ?: "相册"
            }
            else -> {
                val names = albumIds.mapNotNull { id -> albums.find { it.id == id }?.name }.take(2).joinToString(", ")
                "已选 ${albumIds.size} 个相册 ($names…)"
            }
        }
        prefs.immichAlbumName = albumName
        _uiState.update {
            it.copy(
                immichAlbumId = albumIds.firstOrNull(),
                immichAlbumIds = albumIds,
                immichAlbumName = albumName,
                statusMessage = "Immich 相册已更新: $albumName"
            )
        }
        if (prefs.isScheduled && prefs.sourceType == WallpaperSourceType.IMMICH) {
            WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
        }
    }

    fun onToggleCompositeSource(type: WallpaperSourceType, enabled: Boolean) {
        val current = _uiState.value.compositeEnabledSources.toMutableSet()
        if (enabled) {
            current.add(type)
        } else {
            if (current.size > 1) {
                current.remove(type)
            } else {
                _uiState.update { it.copy(statusMessage = "多源混合模式下至少需要保留一个启用的图源") }
                return
            }
        }
        prefs.compositeEnabledSources = current
        _uiState.update {
            it.copy(
                compositeEnabledSources = current,
                statusMessage = "已更新混合图源: ${current.joinToString { it.displayName }}"
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
            refreshScheduling()
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

    fun onCacheSizeTierSelected(tier: CacheSizeTier) {
        prefs.cacheSizeTier = tier
        if (tier.isAutoPruneEnabled) {
            cacheManager.pruneCache(tier)
        }
        val currentSize = cacheManager.getCacheSizeBytes()
        _uiState.update {
            it.copy(
                cacheSizeTier = tier,
                cacheSizeBytes = currentSize,
                statusMessage = if (tier.isAutoPruneEnabled) {
                    "自动清理已设定为「${tier.displayName}」"
                } else {
                    "已禁用自动清理，壁纸将不再自动淘汰"
                }
            )
        }
    }

    fun onToggleFairShuffle(enabled: Boolean) {
        prefs.fairShuffle = enabled
        if (!enabled) {
            prefs.clearRecentWallpaperKeys()
        }
        _uiState.update {
            it.copy(
                fairShuffle = enabled,
                fairShuffleRecordedCount = if (enabled) prefs.getRecentWallpaperKeys().size else 0,
                statusMessage = if (enabled) "智能洗牌防重复已开启" else "智能洗牌已关闭，已清空近期抽取记忆"
            )
        }
    }

    fun onFairShuffleCapacitySelected(capacity: Int) {
        prefs.fairShuffleCapacity = capacity
        val currentKeys = prefs.getRecentWallpaperKeys()
        if (currentKeys.size > capacity) {
            val trimmed = currentKeys.takeLast(capacity)
            prefs.clearRecentWallpaperKeys()
            trimmed.forEach { prefs.recordRecentWallpaperKey(it, capacity) }
        }
        _uiState.update {
            it.copy(
                fairShuffleCapacity = capacity,
                fairShuffleRecordedCount = prefs.getRecentWallpaperKeys().size,
                statusMessage = "洗牌防重复记忆容量已调整为 $capacity 张"
            )
        }
    }

    fun onResetFairShuffleDeck() {
        prefs.clearRecentWallpaperKeys()
        _uiState.update {
            it.copy(
                fairShuffleRecordedCount = 0,
                statusMessage = "洗牌历史已重置，已开启新一轮全量随机抽取"
            )
        }
    }

    fun onWidgetScaleTypeSelected(type: WidgetScaleType) {
        prefs.widgetScaleType = type
        _uiState.update { it.copy(widgetScaleType = type) }
        CurrentWallpaperWidgetProvider.updateAllWidgets(getApplication())
    }

    fun reapplyCurrentWallpaper() {
        val uri = _uiState.value.lastWallpaperUri ?: return
        viewModelScope.launch {
            val currentUriStr = uri.toString()
            val customPref = withContext(Dispatchers.IO) { historyDb.getItemByUri(currentUriStr) }
            val effectiveScrollMode = customPref?.customScrollMode ?: prefs.scrollMode
            val effectiveCropFocusX = customPref?.cropFocusX ?: 0.5f
            val effectiveCropFocusY = customPref?.cropFocusY ?: 0.5f
            val effectiveFlipHorizontal = customPref?.flipHorizontal ?: false

            _uiState.update {
                it.copy(
                    isChanging = true,
                    statusMessage = "正在按「${prefs.cropMode.label} + ${effectiveScrollMode.label}」重新应用壁纸…"
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
                scrollMode = effectiveScrollMode,
                cropMode = prefs.cropMode,
                cropFocusX = effectiveCropFocusX,
                cropFocusY = effectiveCropFocusY,
                flipHorizontal = effectiveFlipHorizontal
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
            val enabledSources = sourcesDb.getEnabledSources()
            if (enabledSources.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "未启用任何图源，请先在图源管理中添加或启用图源") }
                return
            }
        }

        prefs.isScheduled = enabled
        _uiState.update { it.copy(isScheduled = enabled) }

        if (enabled) {
            WallpaperSchedulerHelper.refreshScheduling(context)
            _uiState.update { it.copy(statusMessage = "壁纸自动轮播已启动") }
        } else {
            WallpaperSchedulerHelper.cancelScheduling(context)
            _uiState.update { it.copy(statusMessage = "已停止自动轮播") }
        }
        foo.barz.wallpaperpicker.core.tile.ToggleScheduleTileService.requestUpdate(context)
    }

    fun onToggleIntervalSchedule(enabled: Boolean) {
        prefs.intervalScheduleEnabled = enabled
        _uiState.update { it.copy(intervalScheduleEnabled = enabled) }
        if (prefs.isScheduled) {
            refreshScheduling()
        }
    }

    fun onToggleExactTimer(enabled: Boolean) {
        prefs.exactTimerEnabled = enabled
        _uiState.update { it.copy(exactTimerEnabled = enabled) }
        if (prefs.isScheduled) {
            refreshScheduling()
            _uiState.update {
                it.copy(
                    statusMessage = if (enabled) "已启用高精度 AlarmManager 定时器" else "已切换为系统节能 WorkManager 定时器"
                )
            }
        }
    }

    fun onToggleDailyAnchor(enabled: Boolean) {
        prefs.dailyAnchorEnabled = enabled
        _uiState.update { it.copy(dailyAnchorEnabled = enabled) }
        if (prefs.isScheduled) {
            refreshScheduling()
        }
    }

    fun onSetDailyAnchorTime(hour: Int, minute: Int) {
        prefs.dailyAnchorHour = hour
        prefs.dailyAnchorMinute = minute
        val timeStr = CompositeTriggerHelper.formatTime(hour, minute)
        val currentSet = prefs.dailyAnchorTimes.toMutableSet()
        currentSet.add(timeStr)
        prefs.dailyAnchorTimes = currentSet
        _uiState.update {
            it.copy(
                dailyAnchorHour = hour,
                dailyAnchorMinute = minute,
                dailyAnchorTimes = currentSet
            )
        }
        if (prefs.isScheduled) {
            refreshScheduling()
        }
    }

    fun onAddDailyAnchorTime(hour: Int, minute: Int) {
        val timeStr = CompositeTriggerHelper.formatTime(hour, minute)
        val currentSet = prefs.dailyAnchorTimes.toMutableSet()
        currentSet.add(timeStr)
        prefs.dailyAnchorTimes = currentSet
        _uiState.update { it.copy(dailyAnchorTimes = currentSet) }
        if (prefs.isScheduled) {
            refreshScheduling()
        }
    }

    fun onRemoveDailyAnchorTime(timeStr: String) {
        val currentSet = prefs.dailyAnchorTimes.toMutableSet()
        currentSet.remove(timeStr)
        if (currentSet.isEmpty()) {
            currentSet.add("08:00")
        }
        prefs.dailyAnchorTimes = currentSet
        _uiState.update { it.copy(dailyAnchorTimes = currentSet) }
        if (prefs.isScheduled) {
            refreshScheduling()
        }
    }

    fun onToggleScreenOffTrigger(enabled: Boolean) {
        prefs.screenOffTriggerEnabled = enabled
        _uiState.update { it.copy(screenOffTriggerEnabled = enabled) }
        ScreenOffWatcherService.syncWithPreferences(getApplication())
    }

    fun onSetScreenOffDelaySeconds(seconds: Int) {
        prefs.screenOffDelaySeconds = seconds
        _uiState.update { it.copy(screenOffDelaySeconds = seconds) }
    }

    fun onToggleQuietHours(enabled: Boolean) {
        prefs.quietHoursEnabled = enabled
        _uiState.update { it.copy(quietHoursEnabled = enabled) }
    }

    fun onSetQuietHours(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        prefs.quietHoursStartHour = startHour
        prefs.quietHoursStartMinute = startMinute
        prefs.quietHoursEndHour = endHour
        prefs.quietHoursEndMinute = endMinute
        _uiState.update {
            it.copy(
                quietHoursStartHour = startHour,
                quietHoursStartMinute = startMinute,
                quietHoursEndHour = endHour,
                quietHoursEndMinute = endMinute
            )
        }
    }

    fun onToggleCooldownSuppression(enabled: Boolean) {
        prefs.cooldownSuppressionEnabled = enabled
        _uiState.update { it.copy(cooldownSuppressionEnabled = enabled) }
    }

    fun onSetCooldownMinutes(minutes: Long) {
        prefs.cooldownMinutes = minutes
        _uiState.update { it.copy(cooldownMinutes = minutes) }
    }

    fun onToggleRuleEngine(enabled: Boolean) {
        prefs.ruleEngineEnabled = enabled
        _uiState.update { it.copy(ruleEngineEnabled = enabled) }
        refreshScheduling()
    }

    fun loadScheduleRules() {
        viewModelScope.launch {
            val rules = withContext(Dispatchers.IO) {
                scheduleRulesDb.getAllRules()
            }
            _uiState.update { it.copy(scheduleRules = rules) }
        }
    }

    fun onOpenRuleDialog(rule: ScheduleRule? = null) {
        _uiState.update { it.copy(isRuleDialogOpen = true, editingRule = rule) }
    }

    fun onCloseRuleDialog() {
        _uiState.update { it.copy(isRuleDialogOpen = false, editingRule = null) }
    }

    fun onSaveScheduleRule(rule: ScheduleRule) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val exists = scheduleRulesDb.getRuleById(rule.id) != null
                if (exists) {
                    scheduleRulesDb.updateRule(rule)
                } else {
                    scheduleRulesDb.insertRule(rule)
                }
            }
            loadScheduleRules()
            refreshScheduling()
            onCloseRuleDialog()
            _uiState.update { it.copy(statusMessage = "已保存规则「${rule.name}」") }
        }
    }

    fun onDeleteScheduleRule(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                scheduleRulesDb.deleteRule(id)
            }
            loadScheduleRules()
            refreshScheduling()
            _uiState.update { it.copy(statusMessage = "已删除规则") }
        }
    }

    fun onToggleScheduleRuleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                scheduleRulesDb.setRuleEnabled(id, enabled)
            }
            loadScheduleRules()
            refreshScheduling()
        }
    }

    fun onPopulateDefaultRules() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                scheduleRulesDb.populateDefaultPresetRulesIfNeeded()
            }
            loadScheduleRules()
            refreshScheduling()
            _uiState.update { it.copy(statusMessage = "已载入默认预设规则") }
        }
    }

    private fun refreshScheduling() {
        val context = getApplication<Application>()
        WallpaperSchedulerHelper.refreshScheduling(context)
        foo.barz.wallpaperpicker.core.tile.ToggleScheduleTileService.requestUpdate(context)
    }

    fun changeNow() {
        val enabledSources = sourcesDb.getEnabledSources()
        if (enabledSources.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "未启用任何图源，请先在图源管理中添加或启用图源") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChanging = true, statusMessage = "正在更换壁纸…") }

            val result = withContext(Dispatchers.IO) {
                WallpaperChangeExecutor.execute(getApplication(), isManualTrigger = true)
            }

            when (result) {
                is WallpaperExecutionResult.Success -> {
                    refreshHistoryAndFavorites()
                    val titleDesc = result.title?.let { "「$it」" } ?: "新壁纸"
                    _uiState.update {
                        it.copy(
                            isChanging = false,
                            statusMessage = "更换成功: $titleDesc",
                            lastWallpaperTitle = prefs.lastWallpaperTitle,
                            lastWallpaperUri = prefs.lastWallpaperUri,
                            lastChangedText = formatTimestamp(prefs.lastChangedTimestamp, prefs.lastWallpaperTitle),
                            lastExecutionStatus = prefs.lastExecutionStatus,
                            lastErrorMessage = null,
                            cacheSizeBytes = cacheManager.getCacheSizeBytes(),
                            fairShuffleRecordedCount = prefs.getRecentWallpaperKeys().size
                        )
                    }
                }
                is WallpaperExecutionResult.Skipped -> {
                    _uiState.update {
                        it.copy(
                            isChanging = false,
                            statusMessage = result.reason,
                            lastExecutionStatus = result.reason
                        )
                    }
                }
                is WallpaperExecutionResult.Failure -> {
                    val error = result.error.message ?: "更换壁纸失败"
                    _uiState.update {
                        it.copy(
                            isChanging = false,
                            statusMessage = error,
                            lastErrorMessage = error,
                            lastExecutionStatus = "失败: $error"
                        )
                    }
                }
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

    /**
     * Refreshes UI state when returning from background or after a shortcut/worker execution.
     */
    fun refreshFromBackground() {
        val now = prefs.lastChangedTimestamp
        _uiState.update {
            it.copy(
                lastWallpaperTitle = prefs.lastWallpaperTitle,
                lastWallpaperUri = prefs.lastWallpaperUri,
                lastChangedText = formatTimestamp(now, prefs.lastWallpaperTitle),
                lastExecutionStatus = prefs.lastExecutionStatus,
                lastErrorMessage = prefs.lastErrorMessage,
                cacheSizeBytes = cacheManager.getCacheSizeBytes(),
                widgetScaleType = prefs.widgetScaleType
            )
        }
        refreshHistoryAndFavorites()
        reloadSources()
    }

    fun refreshHistoryAndFavorites() {
        val history = historyDb.getHistoryList()
        val favorites = historyDb.getFavoritesList()
        val currentUri = prefs.lastWallpaperUri?.toString()
        val currentItem = currentUri?.let { uriStr ->
            history.find { it.sourceUri == uriStr } ?: historyDb.getItemByUri(uriStr)
        }
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
                currentWallpaperItem = currentItem,
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
            val concreteType = prefs.lastWallpaperSourceType ?: prefs.sourceType
            val concreteTitle = prefs.lastWallpaperSourceTitle
            val id = historyDb.recordAppliedWallpaper(
                sourceUri = currentUri,
                title = prefs.lastWallpaperTitle,
                sourceType = concreteType,
                appliedTimestamp = prefs.lastChangedTimestamp,
                sourceTitle = concreteTitle
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

    /**
     * Purges only broken or missing non-favorite history entries whose local files are inaccessible.
     */
    fun clearInvalidHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = historyDb.clearInvalidUnfavoritedRecords(getApplication())
            withContext(Dispatchers.Main) {
                refreshHistoryAndFavorites()
                _uiState.update {
                    it.copy(
                        statusMessage = if (count > 0) "已清理 $count 条失效历史记录" else "当前历史记录全部有效，无需清理"
                    )
                }
            }
        }
    }

    /**
     * Re-attempts downloading a historical wallpaper from its remote URL or Immich server.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun redownloadHistoryItem(item: WallpaperHistoryItem) {
        if (_uiState.value.isRedownloadingHistoryId != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRedownloadingHistoryId = item.id,
                    statusMessage = "正在重新下载「${item.title ?: "壁纸"}」…"
                )
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    executeRedownload(item)
                }
            }
            result.onSuccess { restoredFile ->
                val newUri = Uri.fromFile(restoredFile).toString()
                val newFavPath = if (item.isFavorite) {
                    promoteToPermanentFavorite(Uri.fromFile(restoredFile))
                } else {
                    item.favoriteFilePath
                }
                val now = System.currentTimeMillis()
                historyDb.updateSourceUri(
                    id = item.id,
                    sourceUri = newUri,
                    remoteUrl = item.remoteUrl ?: if (item.sourceUri.startsWith("http")) item.sourceUri else null,
                    favoriteFilePath = newFavPath,
                    downloadTimestamp = now
                )
                runCatching {
                    val loader = coil.Coil.imageLoader(getApplication())
                    loader.diskCache?.remove(newUri)
                    loader.memoryCache?.remove(coil.memory.MemoryCache.Key(newUri))
                }
                refreshHistoryAndFavorites()
                _uiState.update {
                    it.copy(
                        isRedownloadingHistoryId = null,
                        statusMessage = "壁纸原图已成功重新下载！"
                    )
                }
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "重新下载失败"
                _uiState.update {
                    it.copy(
                        isRedownloadingHistoryId = null,
                        statusMessage = "重新下载失败: $msg"
                    )
                }
            }
        }
    }

    private suspend fun executeRedownload(item: WallpaperHistoryItem): File {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val remoteUrl = item.remoteUrl ?: item.sourceUri

        if (remoteUrl.startsWith("immich://") || item.sourceType == WallpaperSourceType.IMMICH) {
            val assetId = if (remoteUrl.startsWith("immich://")) {
                remoteUrl.removePrefix("immich://").substringBefore('?')
            } else {
                remoteUrl.substringAfter("/assets/").substringBefore('/')
            }
            val qualityParam = if (remoteUrl.contains("quality=")) {
                remoteUrl.substringAfter("quality=").substringBefore('&')
            } else {
                "ORIGINAL"
            }
            val quality = runCatching { ImmichQuality.valueOf(qualityParam) }
                .getOrDefault(ImmichQuality.ORIGINAL)

            val baseUrl = prefs.immichServerUrl.trim().removeSuffix("/")
            val apiKey = prefs.immichApiKey.trim()
            if (baseUrl.isBlank()) {
                throw IllegalStateException("未配置 Immich 服务器地址，请在设置中配置后再试")
            }

            val url = if (quality == ImmichQuality.ORIGINAL) {
                "$baseUrl/api/assets/$assetId/original"
            } else {
                "$baseUrl/api/assets/$assetId/thumbnail?size=preview"
            }

            fun buildReq(u: String) = Request.Builder()
                .url(u)
                .header("x-api-key", apiKey)
                .header("Accept", "image/*,*/*")
                .header("User-Agent", "WallpaperPicker/1.0 (Android)")
                .build()

            var response = client.newCall(buildReq(url)).execute()
            if (!response.isSuccessful) {
                response.close()
                val fallbackUrl = "$baseUrl/api/assets/$assetId/original"
                val fallbackResp = client.newCall(buildReq(fallbackUrl)).execute()
                if (!fallbackResp.isSuccessful) {
                    val code = fallbackResp.code
                    fallbackResp.close()
                    throw IOException("从 Immich 下载原图失败: HTTP $code")
                }
                response = fallbackResp
            }
            val body = response.body ?: throw IOException("Immich 返回空数据")
            return body.byteStream().use { input ->
                cacheManager.saveStream(
                    inputStream = input,
                    preferredTitle = item.title,
                    cacheKey = "immich_${assetId}_${quality.name}"
                )
            }
        } else if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            val request = Request.Builder()
                .url(remoteUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw IOException("网络下载失败: HTTP $code")
            }
            val body = response.body ?: throw IOException("响应体为空")
            return body.byteStream().use { input ->
                cacheManager.saveStream(
                    inputStream = input,
                    preferredTitle = item.title,
                    cacheKey = remoteUrl
                )
            }
        } else {
            throw IllegalStateException("该记录为本地相册或未包含远端地址，无法重新下载")
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
                    val latest = historyDb.getItemById(item.id) ?: historyDb.getItemByUri(item.sourceUri) ?: item
                    val effectiveScrollMode = latest.customScrollMode ?: prefs.scrollMode
                    val effectiveCropFocusX = latest.cropFocusX ?: 0.5f
                    val effectiveCropFocusY = latest.cropFocusY ?: 0.5f
                    val effectiveFlipHorizontal = latest.flipHorizontal

                    val bitmap = processor.process(
                        openStream = streamProvider,
                        scrollMode = effectiveScrollMode,
                        cropMode = prefs.cropMode,
                        cropFocusX = effectiveCropFocusX,
                        cropFocusY = effectiveCropFocusY,
                        flipHorizontal = effectiveFlipHorizontal
                    ).getOrThrow()
                    applier.apply(bitmap, prefs.target).getOrThrow()
                }
            }
            if (result.isSuccess) {
                val now = System.currentTimeMillis()
                val concreteTitle = item.displaySourceBadge
                prefs.lastChangedTimestamp = now
                prefs.lastWallpaperTitle = item.title
                prefs.lastWallpaperUri = Uri.parse(item.sourceUri)
                prefs.lastWallpaperSourceType = item.sourceType
                prefs.lastWallpaperSourceTitle = concreteTitle
                prefs.lastErrorMessage = null
                prefs.lastExecutionStatus = "成功"
                historyDb.recordAppliedWallpaper(
                    sourceUri = Uri.parse(item.sourceUri),
                    title = item.title,
                    sourceType = item.sourceType,
                    appliedTimestamp = now,
                    sourceTitle = concreteTitle,
                    remoteUrl = item.remoteUrl
                )
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
                CurrentWallpaperWidgetProvider.updateAllWidgets(getApplication())
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

    /**
     * Updates per-image personalized preferences (scroll mode, crop focus, horizontal flip) in database memory.
     */
    fun updateWallpaperPreferences(
        item: WallpaperHistoryItem,
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var targetId = item.id
                if (targetId == 0L) {
                    val existing = historyDb.getItemByUri(item.sourceUri)
                    if (existing != null) {
                        targetId = existing.id
                    } else {
                        targetId = historyDb.recordAppliedWallpaper(
                            sourceUri = Uri.parse(item.sourceUri),
                            title = item.title,
                            sourceType = item.sourceType,
                            appliedTimestamp = item.appliedTimestamp,
                            sourceTitle = item.sourceTitle,
                            remoteUrl = item.remoteUrl
                        )
                    }
                }
                historyDb.updateCustomPreferences(
                    id = targetId,
                    customScrollMode = customScrollMode,
                    cropFocusX = cropFocusX,
                    cropFocusY = cropFocusY,
                    flipHorizontal = flipHorizontal
                )
            }
            refreshHistoryAndFavorites()
            _uiState.update { it.copy(statusMessage = "已保存「${item.title ?: "壁纸"}」个性化属性记忆") }

            val currentUriStr = prefs.lastWallpaperUri?.toString()
            if (applyImmediately || (currentUriStr != null && currentUriStr == item.sourceUri)) {
                applyWallpaperFromHistory(item.copy(
                    customScrollMode = customScrollMode,
                    cropFocusX = cropFocusX,
                    cropFocusY = cropFocusY,
                    flipHorizontal = flipHorizontal
                ))
            }
        }
    }

    /**
     * Updates personalized preferences for currently applied wallpaper.
     */
    fun updateCurrentWallpaperPreferences(
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean = true
    ) {
        val currentUri = prefs.lastWallpaperUri ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val currentUriStr = currentUri.toString()
                var existing = historyDb.getItemByUri(currentUriStr)
                if (existing == null) {
                    val concreteType = prefs.lastWallpaperSourceType ?: prefs.sourceType
                    val concreteTitle = prefs.lastWallpaperSourceTitle
                    val id = historyDb.recordAppliedWallpaper(
                        sourceUri = currentUri,
                        title = prefs.lastWallpaperTitle,
                        sourceType = concreteType,
                        appliedTimestamp = prefs.lastChangedTimestamp,
                        sourceTitle = concreteTitle
                    )
                    existing = historyDb.getItemById(id)
                }
                if (existing != null) {
                    historyDb.updateCustomPreferences(
                        id = existing.id,
                        customScrollMode = customScrollMode,
                        cropFocusX = cropFocusX,
                        cropFocusY = cropFocusY,
                        flipHorizontal = flipHorizontal
                    )
                }
            }
            refreshHistoryAndFavorites()
            _uiState.update { it.copy(statusMessage = "已更新当前壁纸个性化属性记忆") }

            if (applyImmediately) {
                reapplyCurrentWallpaper()
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
        return foo.barz.wallpaperpicker.core.source.MediaStoreSource.hasAnyPermission(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }
}
