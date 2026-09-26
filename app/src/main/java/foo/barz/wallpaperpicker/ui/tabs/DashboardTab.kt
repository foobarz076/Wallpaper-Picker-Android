package foo.barz.wallpaperpicker.ui.tabs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.MainUiState
import foo.barz.wallpaperpicker.ui.components.WallpaperAdjustmentSheet
import foo.barz.wallpaperpicker.ui.components.WallpaperLightboxViewer

/**
 * Dashboard tab displaying hero wallpaper pictorial card, quick secondary actions,
 * status diagnostics, and primary manual change control.
 */
@Composable
fun DashboardTab(
    state: MainUiState,
    onOpenInGallery: () -> Unit,
    onShareWallpaper: () -> Unit,
    onSaveToGallery: () -> Unit,
    onToggleFavoriteCurrent: () -> Unit,
    onUpdateCurrentWallpaperPreferences: (
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    onChangeNow: () -> Unit,
    onNavigateToSources: () -> Unit = {},
    onRequestRestoreBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAdjustmentSheet by remember { mutableStateOf(false) }
    var showLightbox by remember { mutableStateOf(false) }

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

    val enabledSources = state.sourcesList.filter { it.isEnabled }
    val canChange = !state.isChanging && (enabledSources.isNotEmpty() || state.folderUri != null)

    val activeSourceLabel = when {
        enabledSources.isEmpty() -> state.folderName?.let { stringResource(R.string.dash_local_folder_format, it) }
            ?: stringResource(R.string.dash_no_sources_enabled)
        enabledSources.size == 1 -> enabledSources.first().title
        else -> stringResource(R.string.dash_multiple_sources_format, enabledSources.size)
    }

    val sourceBadge = state.currentWallpaperItem?.sourceType?.displayName
        ?: when {
            enabledSources.isEmpty() -> state.folderName?.let { stringResource(R.string.dash_local_folder_format, it) }
                ?: stringResource(R.string.dash_current_wallpaper)
            enabledSources.size == 1 -> enabledSources.first().title
            else -> stringResource(R.string.dash_multiple_sources_badge)
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Hero Pictorial Wallpaper Card or Empty State
        if (state.lastWallpaperUri != null) {
            Card(
                onClick = { showLightbox = true },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Full-bleed wallpaper preview
                    AsyncImage(
                        model = state.lastWallpaperUri,
                        contentDescription = state.lastWallpaperTitle ?: stringResource(R.string.dash_preview_content_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top gradient scrim for top controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                                )
                            )
                    )

                    // Bottom gradient scrim for title & cues
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )

                    // Top Bar overlay: Source badge & Quick favorite button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = sourceBadge,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onToggleFavoriteCurrent,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (state.isCurrentFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (state.isCurrentFavorite) {
                                    stringResource(R.string.dash_action_favorited)
                                } else {
                                    stringResource(R.string.dash_action_favorite)
                                },
                                tint = if (state.isCurrentFavorite) Color.Red else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bottom Bar overlay: Title, subtitle/status, and full screen pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = state.lastWallpaperTitle ?: stringResource(R.string.dash_current_in_use),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val customBadge = if (state.currentWallpaperItem?.customScrollMode != null ||
                                (state.currentWallpaperItem?.cropFocusX != null && state.currentWallpaperItem?.cropFocusX != 0.5f) ||
                                (state.currentWallpaperItem?.cropFocusY != null && state.currentWallpaperItem?.cropFocusY != 0.5f) ||
                                state.currentWallpaperItem?.flipHorizontal == true
                            ) stringResource(R.string.dash_customized_composition) else ""
                            Text(
                                text = "$customBadge${stringResource(R.string.dash_tap_fullscreen_hint)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = stringResource(R.string.dash_fullscreen_btn),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.dash_fullscreen_btn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Secondary Quick Actions Row below Hero Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAdjustmentSheet = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.dash_action_tune), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = handleSaveClick,
                    enabled = !state.isSavingWallpaper,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isSavingWallpaper) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.dash_action_save), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onShareWallpaper,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.dash_action_share), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onOpenInGallery,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.dash_action_open_gallery), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        } else {
            if (state.isOnboardingRestoreVisible) {
                // Onboarding hero card for fresh install / new device
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.dash_onboarding_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.dash_onboarding_desc),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onRequestRestoreBackup,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.dash_onboarding_btn_restore))
                            }
                            OutlinedButton(
                                onClick = onNavigateToSources,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.dash_onboarding_btn_add_source))
                            }
                        }
                    }
                }
            } else {
                // Friendly Empty State when no wallpaper has been applied yet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.dash_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.dash_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        // 2. Status Diagnostics Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.dash_status_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dash_active_source, activeSourceLabel),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.dash_last_changed, state.lastChangedText),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (state.isScheduled) {
                        stringResource(R.string.dash_schedule_on_format, state.intervalMinutes)
                    } else {
                        stringResource(R.string.dash_schedule_off)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                if (state.lastErrorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dash_last_error, state.lastErrorMessage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (state.lastExecutionStatus != null && state.lastExecutionStatus != "成功") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dash_last_status, state.lastExecutionStatus),
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
                Text(stringResource(R.string.dash_changing))
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.dash_change_now))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Fullscreen gestures lightbox viewer
    if (showLightbox && state.lastWallpaperUri != null) {
        WallpaperLightboxViewer(
            imageUri = state.lastWallpaperUri,
            title = state.lastWallpaperTitle,
            sourceBadge = sourceBadge,
            isFavorite = state.isCurrentFavorite,
            isSaving = state.isSavingWallpaper,
            onDismiss = { showLightbox = false },
            onToggleFavorite = onToggleFavoriteCurrent,
            onOpenInGallery = onOpenInGallery,
            onSaveToGallery = handleSaveClick,
            onShareWallpaper = onShareWallpaper,
            onOpenAdjustment = {
                showLightbox = false
                showAdjustmentSheet = true
            }
        )
    }

    // Composition and scroll tuning bottom sheet
    if (showAdjustmentSheet && state.lastWallpaperUri != null) {
        val targetItem = state.currentWallpaperItem ?: WallpaperHistoryItem(
            sourceUri = state.lastWallpaperUri.toString(),
            title = state.lastWallpaperTitle,
            sourceType = WallpaperSourceType.LOCAL_FOLDER,
            appliedTimestamp = System.currentTimeMillis()
        )
        WallpaperAdjustmentSheet(
            item = targetItem,
            globalScrollMode = state.scrollMode,
            onDismiss = { showAdjustmentSheet = false },
            onSave = { customScrollMode, cropFocusX, cropFocusY, flipHorizontal, applyImmediately ->
                onUpdateCurrentWallpaperPreferences(
                    customScrollMode,
                    cropFocusX,
                    cropFocusY,
                    flipHorizontal,
                    applyImmediately
                )
            }
        )
    }
}
