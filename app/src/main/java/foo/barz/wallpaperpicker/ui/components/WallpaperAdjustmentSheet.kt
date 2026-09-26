package foo.barz.wallpaperpicker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import kotlin.math.roundToInt

/**
 * Bottom sheet modal providing interactive adjustment for per-image attributes:
 * parallax scroll mode override, crop focal center (X/Y), and horizontal flip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperAdjustmentSheet(
    item: WallpaperHistoryItem,
    globalScrollMode: WallpaperScrollMode,
    onDismiss: () -> Unit,
    onSave: (
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State initialized from existing item preferences (or defaults)
    var selectedScrollMode by remember(item) { mutableStateOf(item.customScrollMode) }
    var focusX by remember(item) { mutableFloatStateOf(item.cropFocusX ?: 0.5f) }
    var focusY by remember(item) { mutableFloatStateOf(item.cropFocusY ?: 0.5f) }
    var flipHorizontal by remember(item) { mutableStateOf(item.flipHorizontal) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.adjust_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.title ?: stringResource(R.string.dash_current_wallpaper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Live Interactive Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.adjust_preview_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated 9:16 phone screen frame
                    Box(
                        modifier = Modifier
                            .height(240.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Compose BiasAlignment maps -1f..+1f. Normalized focus (0.0..1.0) maps via (focus - 0.5f) * 2f
                        val horizontalBias = (focusX - 0.5f) * 2f
                        val verticalBias = (focusY - 0.5f) * 2f

                        AsyncImage(
                            model = item.displayUri,
                            contentDescription = stringResource(R.string.adjust_preview_desc),
                            alignment = BiasAlignment(horizontalBias, verticalBias),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = if (flipHorizontal) -1f else 1f
                                }
                        )

                        // Subtle center focal point reticle
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f))
                                .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val mirroredSuffix = if (flipHorizontal) stringResource(R.string.adjust_mirrored_badge) else ""
                    Text(
                        text = stringResource(R.string.adjust_current_focus, (focusX * 100).roundToInt(), (focusY * 100).roundToInt()) + mirroredSuffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Horizontal Flip Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.adjust_flip_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.adjust_flip_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = flipHorizontal,
                        onCheckedChange = { flipHorizontal = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Crop Focus Adjustment (X & Y Sliders + Presets)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.adjust_crop_focus_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal focus slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.adjust_focus_x_label), style = MaterialTheme.typography.bodySmall)
                        val xPercent = (focusX * 100).roundToInt()
                        Text(
                            text = when {
                                focusX < 0.35f -> stringResource(R.string.adjust_focus_x_left, xPercent)
                                focusX > 0.65f -> stringResource(R.string.adjust_focus_x_right, xPercent)
                                else -> stringResource(R.string.adjust_focus_x_center, xPercent)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = focusX,
                        onValueChange = { focusX = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vertical focus slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.adjust_focus_y_label), style = MaterialTheme.typography.bodySmall)
                        val yPercent = (focusY * 100).roundToInt()
                        Text(
                            text = when {
                                focusY < 0.35f -> stringResource(R.string.adjust_focus_y_top, yPercent)
                                focusY > 0.65f -> stringResource(R.string.adjust_focus_y_bottom, yPercent)
                                else -> stringResource(R.string.adjust_focus_y_center, yPercent)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = focusY,
                        onValueChange = { focusY = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick focal presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { focusX = 0.5f; focusY = 0.15f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.adjust_preset_head), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { focusX = 0.5f; focusY = 0.5f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.adjust_preset_center), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { focusX = 0.5f; focusY = 0.85f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.adjust_preset_bottom), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Parallax Scroll Strategy Override
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.adjust_scroll_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.adjust_scroll_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedScrollMode == null,
                            onClick = { selectedScrollMode = null },
                            label = { Text(stringResource(R.string.adjust_scroll_follow_global), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedScrollMode == WallpaperScrollMode.AUTO,
                            onClick = { selectedScrollMode = WallpaperScrollMode.AUTO },
                            label = { Text(stringResource(R.string.adjust_scroll_auto), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedScrollMode == WallpaperScrollMode.ALWAYS,
                            onClick = { selectedScrollMode = WallpaperScrollMode.ALWAYS },
                            label = { Text(stringResource(R.string.adjust_scroll_always), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedScrollMode == WallpaperScrollMode.NEVER,
                            onClick = { selectedScrollMode = WallpaperScrollMode.NEVER },
                            label = { Text(stringResource(R.string.adjust_scroll_never), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (selectedScrollMode) {
                            null -> stringResource(R.string.adjust_scroll_global_format, globalScrollMode.label)
                            WallpaperScrollMode.AUTO -> stringResource(R.string.adjust_scroll_auto_desc)
                            WallpaperScrollMode.ALWAYS -> stringResource(R.string.adjust_scroll_always_desc)
                            WallpaperScrollMode.NEVER -> stringResource(R.string.adjust_scroll_never_desc)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset action
            OutlinedButton(
                onClick = {
                    selectedScrollMode = null
                    focusX = 0.5f
                    focusY = 0.5f
                    flipHorizontal = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.adjust_reset_global))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSave(
                            selectedScrollMode,
                            if (focusX == 0.5f && focusY == 0.5f) null else focusX,
                            if (focusX == 0.5f && focusY == 0.5f) null else focusY,
                            flipHorizontal,
                            false
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.adjust_save_memory_only))
                }

                Button(
                    onClick = {
                        onSave(
                            selectedScrollMode,
                            if (focusX == 0.5f && focusY == 0.5f) null else focusX,
                            if (focusX == 0.5f && focusY == 0.5f) null else focusY,
                            flipHorizontal,
                            true
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.adjust_save_and_apply))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
