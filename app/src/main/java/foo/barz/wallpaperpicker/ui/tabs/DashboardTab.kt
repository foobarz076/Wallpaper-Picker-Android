package foo.barz.wallpaperpicker.ui.tabs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Dashboard tab displaying current wallpaper preview, immediate actions, status diagnostics,
 * and primary manual change control.
 */
@Composable
fun DashboardTab(
    state: MainUiState,
    onOpenInGallery: () -> Unit,
    onShareWallpaper: () -> Unit,
    onSaveToGallery: () -> Unit,
    onToggleFavoriteCurrent: () -> Unit,
    onChangeNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Write external storage permission for legacy Android versions (API <= 28)
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

    // Media permission check for MediaStore source validity
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val hasMediaPermission = ContextCompat.checkSelfPermission(
        context,
        mediaPermission
    ) == PackageManager.PERMISSION_GRANTED

    val enabledSources = state.sourcesList.filter { it.isEnabled }
    val canChange = !state.isChanging && (enabledSources.isNotEmpty() || state.folderUri != null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Current Wallpaper Card & Actions
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

                    // 2x2 Action Button Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenInGallery,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("图库打开", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = handleSaveClick,
                                enabled = !state.isSavingWallpaper,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                if (state.isSavingWallpaper) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("保存相册", maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onShareWallpaper,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("分享", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onToggleFavoriteCurrent,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isCurrentFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (state.isCurrentFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.isCurrentFavorite) "已收藏" else "设为收藏", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // 2. Status Diagnostics Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("当前状态", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val activeSourceLabel = when {
                    enabledSources.isEmpty() -> state.folderName?.let { "本地文件夹 ($it)" } ?: "未启用任何图源"
                    enabledSources.size == 1 -> enabledSources.first().title
                    else -> "多源混合 (${enabledSources.size} 个启用图源)"
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

        // 3. Primary Manual Change Action Button
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
