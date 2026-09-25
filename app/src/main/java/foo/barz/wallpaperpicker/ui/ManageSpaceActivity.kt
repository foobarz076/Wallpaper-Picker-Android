package foo.barz.wallpaperpicker.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * UI state for the Manage Space screen.
 */
data class ManageSpaceUiState(
    val wallpaperCacheBytes: Long = 0L,
    val coilCacheBytes: Long = 0L,
    val isCalculating: Boolean = true,
    val isClearingCache: Boolean = false,
    val showClearAllDialog: Boolean = false,
    val showClearWallpaperCacheDialog: Boolean = false,
    val showClearAllImageCacheDialog: Boolean = false,
    val removeInvalidHistoryOnClean: Boolean = false,
    val statusMessage: String? = null
) {
    val totalImageCacheBytes: Long
        get() = wallpaperCacheBytes + coilCacheBytes
}

/**
 * ViewModel responsible for calculating and purging app storage categories.
 */
@OptIn(coil.annotation.ExperimentalCoilApi::class)
class ManageSpaceViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheManager = WallpaperCacheManager(application)
    private val historyDb = WallpaperHistoryDatabase(application)
    private val _uiState = MutableStateFlow(ManageSpaceUiState())
    val uiState: StateFlow<ManageSpaceUiState> = _uiState.asStateFlow()

    init {
        refreshStorageUsage()
    }

    /**
     * Recalculates storage usage across wallpaper files and Coil image cache.
     */
    fun refreshStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val wpCache = cacheManager.getCacheSizeBytes()
            val coilCache = runCatching {
                Coil.imageLoader(getApplication()).diskCache?.size ?: 0L
            }.getOrDefault(0L)

            _uiState.update {
                it.copy(
                    wallpaperCacheBytes = wpCache,
                    coilCacheBytes = coilCache,
                    isCalculating = false
                )
            }
        }
    }

    fun requestClearWallpaperCacheOnly() {
        _uiState.update { it.copy(showClearWallpaperCacheDialog = true) }
    }

    fun dismissClearWallpaperCacheDialog() {
        _uiState.update { it.copy(showClearWallpaperCacheDialog = false) }
    }

    fun confirmClearWallpaperCacheOnly() {
        val removeHistory = _uiState.value.removeInvalidHistoryOnClean
        dismissClearWallpaperCacheDialog()
        clearWallpaperCacheOnly(removeHistory)
    }

    /**
     * Clears only downloaded wallpaper files, preserving Coil thumbnail cache.
     * Optionally purges broken non-favorite history entries whose local cached files are deleted.
     */
    fun clearWallpaperCacheOnly(removeInvalidHistory: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isClearingCache = true) }

            val oldWp = cacheManager.getCacheSizeBytes()
            cacheManager.clearCache()
            val remainingWp = cacheManager.getCacheSizeBytes()
            val freed = maxOf(0L, oldWp - remainingWp)

            var removedHistoryCount = 0
            if (removeInvalidHistory) {
                removedHistoryCount = historyDb.clearInvalidUnfavoritedRecords(getApplication())
            }

            val msg = if (removedHistoryCount > 0) {
                "已清理下载壁纸原图缓存 (释放了 ${formatFileSize(freed)})，并同步移除了 $removedHistoryCount 条失效历史记录（缩略图已保留）"
            } else {
                "已清理下载壁纸原图缓存 (释放了 ${formatFileSize(freed)})，缩略图已保留"
            }

            _uiState.update {
                it.copy(
                    wallpaperCacheBytes = remainingWp,
                    isClearingCache = false,
                    statusMessage = msg
                )
            }
        }
    }

    fun requestClearAllImageCache() {
        _uiState.update { it.copy(showClearAllImageCacheDialog = true) }
    }

    fun dismissClearAllImageCacheDialog() {
        _uiState.update { it.copy(showClearAllImageCacheDialog = false) }
    }

    fun confirmClearAllImageCache() {
        val removeHistory = _uiState.value.removeInvalidHistoryOnClean
        dismissClearAllImageCacheDialog()
        clearAllImageCache(removeHistory)
    }

    /**
     * Clears all image cache files including wallpaper downloads and Coil thumbnail/disk caches.
     * Optionally purges broken non-favorite history entries whose local cached files are deleted.
     */
    fun clearAllImageCache(removeInvalidHistory: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isClearingCache = true) }

            // Clear wallpaper cache pool
            cacheManager.clearCache()

            // Clear Coil memory and disk caches
            runCatching {
                val loader = Coil.imageLoader(getApplication())
                loader.diskCache?.clear()
                loader.memoryCache?.clear()
            }

            var removedHistoryCount = 0
            if (removeInvalidHistory) {
                removedHistoryCount = historyDb.clearInvalidUnfavoritedRecords(getApplication())
            }

            val remainingWp = cacheManager.getCacheSizeBytes()
            val remainingCoil = runCatching {
                Coil.imageLoader(getApplication()).diskCache?.size ?: 0L
            }.getOrDefault(0L)

            val msg = if (removedHistoryCount > 0) {
                "全部图片缓存已成功清理，并同步移除了 $removedHistoryCount 条失效历史记录"
            } else {
                "全部图片缓存已成功清理"
            }

            _uiState.update {
                it.copy(
                    wallpaperCacheBytes = remainingWp,
                    coilCacheBytes = remainingCoil,
                    isClearingCache = false,
                    statusMessage = msg
                )
            }
        }
    }

    fun toggleRemoveInvalidHistoryOnClean(checked: Boolean) {
        _uiState.update { it.copy(removeInvalidHistoryOnClean = checked) }
    }

    /**
     * Completely resets all application user data and terminates the process.
     */
    fun clearAllData() {
        val activityManager = getApplication<Application>()
            .getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.clearApplicationUserData()
    }

    fun showClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    fun dismissClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = false) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

/**
 * Activity invoked by Android Settings when the user chooses "Manage Space".
 */
class ManageSpaceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WallpaperPickerTheme {
                val viewModel: ManageSpaceViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                ManageSpaceScreen(
                    uiState = uiState,
                    onBackClick = { finish() },
                    onRequestClearWallpaperCache = viewModel::requestClearWallpaperCacheOnly,
                    onConfirmClearWallpaperCache = viewModel::confirmClearWallpaperCacheOnly,
                    onDismissClearWallpaperCacheDialog = viewModel::dismissClearWallpaperCacheDialog,
                    onRequestClearAllImageCache = viewModel::requestClearAllImageCache,
                    onConfirmClearAllImageCache = viewModel::confirmClearAllImageCache,
                    onDismissClearAllImageCacheDialog = viewModel::dismissClearAllImageCacheDialog,
                    onToggleRemoveInvalidHistory = viewModel::toggleRemoveInvalidHistoryOnClean,
                    onRequestClearAll = viewModel::showClearAllDialog,
                    onConfirmClearAll = viewModel::clearAllData,
                    onDismissDialog = viewModel::dismissClearAllDialog,
                    onClearStatusMessage = viewModel::clearStatusMessage
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSpaceScreen(
    uiState: ManageSpaceUiState,
    onBackClick: () -> Unit,
    onRequestClearWallpaperCache: () -> Unit,
    onConfirmClearWallpaperCache: () -> Unit,
    onDismissClearWallpaperCacheDialog: () -> Unit,
    onRequestClearAllImageCache: () -> Unit,
    onConfirmClearAllImageCache: () -> Unit,
    onDismissClearAllImageCacheDialog: () -> Unit,
    onToggleRemoveInvalidHistory: (Boolean) -> Unit,
    onRequestClearAll: () -> Unit,
    onConfirmClearAll: () -> Unit,
    onDismissDialog: () -> Unit,
    onClearStatusMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onClearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("存储空间管理") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "可清理图片缓存",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (uiState.isCalculating) {
                            Text(
                                text = "正在计算占用空间…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Text(
                                text = formatFileSize(uiState.totalImageCacheBytes),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Section 1: Image Cache
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "图片缓存管理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "已下载的网络壁纸与缩略图缓存独立统计。您可根据需要仅清理下载壁纸大图以保留缩略图预览，或一次性清理全部图片缓存。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option A: Wallpaper Download Cache Only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "下载壁纸原图缓存",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "网络图源下载的大图文件，用于离线轮换：${formatFileSize(uiState.wallpaperCacheBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = onRequestClearWallpaperCache,
                            enabled = !uiState.isCalculating && !uiState.isClearingCache && uiState.wallpaperCacheBytes > 0L,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("仅清理下载壁纸", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Item B: Thumbnail Cache Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "界面缩略图与视图缓存",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "历史记录与图源列表中的缩略图磁盘缓存：${formatFileSize(uiState.coilCacheBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option B: Clear All Image Caches
                    Button(
                        onClick = onRequestClearAllImageCache,
                        enabled = !uiState.isCalculating && !uiState.isClearingCache && uiState.totalImageCacheBytes > 0L,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isClearingCache) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("清理中…")
                        } else {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("清理全部图片缓存 (${formatFileSize(uiState.totalImageCacheBytes)})")
                        }
                    }
                }
            }

            // Section 2: Clear All Data
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "清除所有数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "将完全重置应用，彻底删除所有已保存的图源配置、文件夹索引数据库、定时切换任务及本地缓存。操作后应用将直接关闭，重新打开时如同初次安装。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onRequestClearAll,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("清空全部数据")
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing wallpaper download cache only
    if (uiState.showClearWallpaperCacheDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearWallpaperCacheDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "仅清理下载壁纸原图？")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "此操作仅释放网络图源下载的壁纸原图文件 (${formatFileSize(uiState.wallpaperCacheBytes)})。\n\n" +
                                "• 缩略图缓存将得到保留，历史记录与图源列表仍可流畅预览。\n" +
                                "• 已收藏壁纸（已持久化隔离）：不受任何影响。\n" +
                                "• 未收藏的网络壁纸原图：本地文件将被清理，历史记录中将显示失效（后续可按需从网络重新下载）。"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleRemoveInvalidHistory(!uiState.removeInvalidHistoryOnClean)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.removeInvalidHistoryOnClean,
                            onCheckedChange = onToggleRemoveInvalidHistory
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "同时移除失去原图的非收藏历史记录",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmClearWallpaperCache
                ) {
                    Text("确认清理下载壁纸")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearWallpaperCacheDialog) {
                    Text("取消")
                }
            }
        )
    }

    // Confirmation dialog for clearing all image caches
    if (uiState.showClearAllImageCacheDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearAllImageCacheDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "清理全部图片缓存？")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "此操作将同时释放所有下载的壁纸原图以及界面缩略图与预加载缓存 (${formatFileSize(uiState.totalImageCacheBytes)})。\n\n" +
                                "• 已收藏壁纸（已持久化隔离）：不受任何影响。\n" +
                                "• 未收藏的网络壁纸原图：本地文件将被清理，历史记录中将显示失效。\n" +
                                "• 缩略图将在再次浏览相关列表时按需重新生成。"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleRemoveInvalidHistory(!uiState.removeInvalidHistoryOnClean)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.removeInvalidHistoryOnClean,
                            onCheckedChange = onToggleRemoveInvalidHistory
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "同时移除失去原图的非收藏历史记录",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmClearAllImageCache
                ) {
                    Text("确认清理全部图片")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearAllImageCacheDialog) {
                    Text("取消")
                }
            }
        )
    }

    // Confirmation dialog for clearing all data
    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "确认清空应用所有数据？")
            },
            text = {
                Text(
                    text = "此操作不可撤销！这将会清除所有配置、定时任务、本地索引及图片缓存。执行后应用将立即退出。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissDialog()
                        onConfirmClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认清空并退出")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("取消")
                }
            }
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.1f KB", kb)
    }
}
