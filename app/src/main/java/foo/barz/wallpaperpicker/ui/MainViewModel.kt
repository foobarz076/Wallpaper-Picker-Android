package foo.barz.wallpaperpicker.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.core.worker.WallpaperWorker
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MainUiState(
    val sourceType: WallpaperSourceType = WallpaperSourceType.LOCAL_FOLDER,
    val folderUri: Uri? = null,
    val folderName: String? = null,
    val httpPresetType: HttpPresetType = HttpPresetType.BING,
    val httpCustomUrl: String = "",
    val httpCustomJsonPath: String = "",
    val wifiOnly: Boolean = true,
    val cacheSizeBytes: Long = 0L,
    val intervalMinutes: Long = 60L,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val scrollMode: WallpaperScrollMode = WallpaperScrollMode.AUTO,
    val isScheduled: Boolean = false,
    val isChanging: Boolean = false,
    val lastChangedText: String = "尚未更换过",
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val processor = WallpaperProcessor(application)
    private val applier = WallpaperApplier(application)
    private val cacheManager = WallpaperCacheManager(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            sourceType = prefs.sourceType,
            folderUri = prefs.folderUri,
            folderName = resolveFolderName(prefs.folderUri),
            httpPresetType = prefs.httpPresetType,
            httpCustomUrl = prefs.httpCustomUrl,
            httpCustomJsonPath = prefs.httpCustomJsonPath,
            wifiOnly = prefs.wifiOnly,
            cacheSizeBytes = cacheManager.getCacheSizeBytes(),
            intervalMinutes = prefs.intervalMinutes,
            target = prefs.target,
            scrollMode = prefs.scrollMode,
            isScheduled = prefs.isScheduled,
            lastChangedText = formatTimestamp(prefs.lastChangedTimestamp, prefs.lastWallpaperTitle)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onSourceTypeSelected(type: WallpaperSourceType) {
        prefs.sourceType = type
        _uiState.update { it.copy(sourceType = type) }
        if (prefs.isScheduled) {
            WallpaperWorker.schedule(getApplication(), prefs.intervalMinutes)
        }
    }

    fun onFolderSelected(uri: Uri) {
        val context = getApplication<Application>()
        try {
            // Persist SAF permission across reboots and process deaths
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
                statusMessage = "已选择文件夹: $name"
            )
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

    fun onClearCache() {
        val success = cacheManager.clearCache()
        val newSize = cacheManager.getCacheSizeBytes()
        _uiState.update {
            it.copy(
                cacheSizeBytes = newSize,
                statusMessage = if (success) "已清空缓存" else "清理缓存失败"
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
    }

    fun toggleSchedule(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled) {
            if (prefs.sourceType == WallpaperSourceType.LOCAL_FOLDER && prefs.folderUri == null) {
                _uiState.update { it.copy(statusMessage = "请先选择壁纸文件夹") }
                return
            }
            if (prefs.sourceType == WallpaperSourceType.HTTP_API &&
                prefs.httpPresetType == HttpPresetType.CUSTOM &&
                prefs.httpCustomUrl.isBlank()) {
                _uiState.update { it.copy(statusMessage = "请先输入自定义 API 网址") }
                return
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
        if (prefs.sourceType == WallpaperSourceType.LOCAL_FOLDER && prefs.folderUri == null) {
            _uiState.update { it.copy(statusMessage = "请先选择壁纸文件夹") }
            return
        }
        if (prefs.sourceType == WallpaperSourceType.HTTP_API &&
            prefs.httpPresetType == HttpPresetType.CUSTOM &&
            prefs.httpCustomUrl.isBlank()) {
            _uiState.update { it.copy(statusMessage = "请先输入自定义 API 网址") }
            return
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
                _uiState.update { it.copy(isChanging = false, statusMessage = error) }
                return@launch
            }

            val source = sourceResult.getOrThrow()
            val nextResult = source.getNextWallpaper()

            if (nextResult.isFailure) {
                val error = nextResult.exceptionOrNull()?.message ?: "获取图片失败"
                _uiState.update { it.copy(isChanging = false, statusMessage = error) }
                return@launch
            }

            val data = nextResult.getOrThrow()
            val processResult = processor.process(data.openStream, prefs.scrollMode)

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

            val now = System.currentTimeMillis()
            prefs.lastChangedTimestamp = now
            prefs.lastWallpaperTitle = data.title

            _uiState.update {
                it.copy(
                    isChanging = false,
                    lastChangedText = formatTimestamp(now, data.title),
                    cacheSizeBytes = cacheManager.getCacheSizeBytes(),
                    statusMessage = "更换成功: ${data.title ?: "未知图片"}"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
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
}
