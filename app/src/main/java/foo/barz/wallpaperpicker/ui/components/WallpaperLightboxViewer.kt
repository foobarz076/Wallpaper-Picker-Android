package foo.barz.wallpaperpicker.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.R

/**
 * Immersive full-screen media lightbox viewer mimicking social media gallery experience.
 * Features:
 * - Edge-to-edge full-bleed presentation
 * - Double-tap zoom/reset and pinch-to-zoom with panning
 * - Single-tap toggling of UI overlays
 * - Floating translucent action pill (Favorite, Tune composition, Save, Share)
 * - Quick action to open image in external system gallery / editor
 */
@Composable
fun WallpaperLightboxViewer(
    imageUri: Uri,
    title: String?,
    sourceBadge: String? = null,
    isFavorite: Boolean,
    isSaving: Boolean = false,
    canApplyWallpaper: Boolean = false,
    isApplyingWallpaper: Boolean = false,
    isApplyEnabled: Boolean = true,
    isApplied: Boolean = false,
    canDeleteRecord: Boolean = false,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenInGallery: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShareWallpaper: () -> Unit,
    onOpenAdjustment: () -> Unit,
    onApplyWallpaper: () -> Unit = {},
    onDeleteRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showControls by remember { mutableStateOf(true) }
    var isImageError by remember { mutableStateOf(!isApplyEnabled) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = newScale
                        if (newScale > 1f) {
                            val maxOffsetX = (newScale - 1f) * 500f
                            val maxOffsetY = (newScale - 1f) * 800f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            // 1. Zoomable & pannable full-bleed image
            AsyncImage(
                model = imageUri,
                contentDescription = title ?: stringResource(R.string.lightbox_full_image),
                contentScale = ContentScale.Fit,
                onSuccess = { isImageError = false },
                onError = { isImageError = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            if (isImageError) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.hist_detail_missing_title),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.hist_detail_missing_desc),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 2. Top Navigation Bar (Close, Title, Open in External Gallery)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(
                            top = 8.dp,
                            start = 12.dp,
                            end = 12.dp,
                            bottom = 8.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title ?: stringResource(R.string.lightbox_original_image),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!sourceBadge.isNullOrBlank()) {
                                Text(
                                    text = sourceBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Secondary action: Open in external system gallery / photo editor
                        IconButton(
                            onClick = onOpenInGallery,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.lightbox_open_in_gallery),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Floating Translucent Action Pill at bottom
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(
                            bottom = 24.dp,
                            start = 16.dp,
                            end = 16.dp,
                            top = 20.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        tonalElevation = 6.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        val hasExtraActions = canApplyWallpaper || canDeleteRecord
                        val buttonSpacing = if (hasExtraActions) 10.dp else 16.dp
                        val buttonSize = if (hasExtraActions) 40.dp else 42.dp

                        Row(
                            modifier = Modifier.padding(
                                horizontal = if (hasExtraActions) 10.dp else 14.dp,
                                vertical = 6.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Set as wallpaper (Apply) - Available in History mode
                            if (canApplyWallpaper) {
                                IconButton(
                                    onClick = onApplyWallpaper,
                                    enabled = !isApplyingWallpaper && isApplyEnabled,
                                    modifier = Modifier.size(buttonSize)
                                ) {
                                    if (isApplyingWallpaper) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Crossfade(
                                            targetState = isApplied,
                                            label = "ApplyIconCrossfade"
                                        ) { applied ->
                                            if (applied) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.hist_action_applied),
                                                    tint = Color(0xFF81C784),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Wallpaper,
                                                    contentDescription = stringResource(R.string.hist_action_set_current),
                                                    tint = if (isApplyEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Favorite toggle
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) {
                                        stringResource(R.string.dash_action_favorited)
                                    } else {
                                        stringResource(R.string.dash_action_favorite)
                                    },
                                    tint = if (isFavorite) Color.Red else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 3. Tune Composition & Scroll
                            IconButton(
                                onClick = onOpenAdjustment,
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.dash_action_tune),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 4. Save to public gallery
                            IconButton(
                                onClick = onSaveToGallery,
                                enabled = !isSaving,
                                modifier = Modifier.size(buttonSize)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = stringResource(R.string.dash_action_save),
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // 5. System Share sheet
                            IconButton(
                                onClick = onShareWallpaper,
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.dash_action_share),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 6. Delete history record - Available in History mode
                            if (canDeleteRecord) {
                                IconButton(
                                    onClick = onDeleteRecord,
                                    modifier = Modifier.size(buttonSize)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.hist_action_delete_record),
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}
