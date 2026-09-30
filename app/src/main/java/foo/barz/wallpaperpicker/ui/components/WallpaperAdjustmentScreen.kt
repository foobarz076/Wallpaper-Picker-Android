package foo.barz.wallpaperpicker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import kotlin.math.roundToInt

private enum class ScreenAdjustmentTarget {
    HOME,
    LOCK
}

/**
 * Dedicated fullscreen adjustment screen for fine-tuning wallpaper presentation:
 * - Dynamic device frame preview reflecting the actual physical aspect ratio of the host display.
 * - Interactive focal point shifting via direct touch dragging or precision X/Y sliders.
 * - Dual screen framing: independent focal coordinates for Home Screen and Lock Screen with link/sync toggle.
 * - Real-time horizontal flip mirroring.
 * - Per-wallpaper parallax scroll strategy override.
 * - Pinned bottom actions: save preference in metadata only or apply immediately to system wallpaper.
 */
@Composable
fun WallpaperAdjustmentScreen(
    item: WallpaperHistoryItem,
    globalScrollMode: WallpaperScrollMode,
    onDismiss: () -> Unit,
    onSave: (
        customScrollMode: WallpaperScrollMode?,
        cropFocusX: Float?,
        cropFocusY: Float?,
        flipHorizontal: Boolean,
        applyImmediately: Boolean,
        lockCropFocusX: Float?,
        lockCropFocusY: Float?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current active adjustment tab (Home or Lock)
    var activeTargetTab by remember { mutableStateOf(ScreenAdjustmentTarget.HOME) }

    // State initialized from existing item preferences (or defaults)
    var selectedScrollMode by remember(item) { mutableStateOf(item.customScrollMode) }
    var homeFocusX by remember(item) { mutableFloatStateOf(item.cropFocusX ?: 0.5f) }
    var homeFocusY by remember(item) { mutableFloatStateOf(item.cropFocusY ?: 0.5f) }

    // Lock screen focal coordinates; if null initially, defaults to home coordinates
    val initialHasDistinctLock = item.lockCropFocusX != null || item.lockCropFocusY != null
    var isLockLinkedToHome by remember(item) { mutableStateOf(!initialHasDistinctLock) }
    var lockFocusX by remember(item) { mutableFloatStateOf(item.lockCropFocusX ?: item.cropFocusX ?: 0.5f) }
    var lockFocusY by remember(item) { mutableFloatStateOf(item.lockCropFocusY ?: item.cropFocusY ?: 0.5f) }

    var flipHorizontal by remember(item) { mutableStateOf(item.flipHorizontal) }

    // Current focal coordinates based on active tab
    val currentFocusX = if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) homeFocusX else lockFocusX
    val currentFocusY = if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) homeFocusY else lockFocusY

    // Dynamic physical device aspect ratio
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val deviceAspectRatio = (screenWidthDp / screenHeightDp).coerceIn(0.25f, 3.0f)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top App Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.adjust_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.title ?: stringResource(R.string.dash_current_wallpaper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        selectedScrollMode = null
                        homeFocusX = 0.5f
                        homeFocusY = 0.5f
                        lockFocusX = 0.5f
                        lockFocusY = 0.5f
                        isLockLinkedToHome = true
                        flipHorizontal = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.adjust_reset_global),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Screen Target Selector Tabs (Home Screen vs Lock Screen)
            TabRow(
                selectedTabIndex = if (activeTargetTab == ScreenAdjustmentTarget.HOME) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Tab(
                    selected = activeTargetTab == ScreenAdjustmentTarget.HOME,
                    onClick = { activeTargetTab = ScreenAdjustmentTarget.HOME },
                    text = { Text(stringResource(R.string.adjust_tab_home)) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTargetTab == ScreenAdjustmentTarget.LOCK,
                    onClick = { activeTargetTab = ScreenAdjustmentTarget.LOCK },
                    text = { Text(stringResource(R.string.adjust_tab_lock)) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // 2. Top Preview Section
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Simulated physical device frame using host screen aspect ratio
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .aspectRatio(deviceAspectRatio, matchHeightConstraintsFirst = true)
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                            .background(Color.Black)
                            .pointerInput(flipHorizontal, activeTargetTab, isLockLinkedToHome) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val flipScale = if (flipHorizontal) -1f else 1f
                                    val deltaX = (dragAmount.x / size.width.toFloat()) * flipScale
                                    val deltaY = dragAmount.y / size.height.toFloat()
                                    if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) {
                                        homeFocusX = (homeFocusX - deltaX).coerceIn(0f, 1f)
                                        homeFocusY = (homeFocusY - deltaY).coerceIn(0f, 1f)
                                        if (isLockLinkedToHome) {
                                            lockFocusX = homeFocusX
                                            lockFocusY = homeFocusY
                                        }
                                    } else {
                                        lockFocusX = (lockFocusX - deltaX).coerceIn(0f, 1f)
                                        lockFocusY = (lockFocusY - deltaY).coerceIn(0f, 1f)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Compose BiasAlignment maps -1f..+1f. Normalized focus (0.0..1.0) maps via (focus - 0.5f) * 2f
                        val horizontalBias = (currentFocusX - 0.5f) * 2f
                        val verticalBias = (currentFocusY - 0.5f) * 2f

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

                        // Center focal point reticle
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f))
                                .border(1.dp, Color.Black.copy(alpha = 0.45f), CircleShape)
                        )

                        // Top camera cutout pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        // Lock Screen vs Home Screen Simulated Visual Clues
                        if (activeTargetTab == ScreenAdjustmentTarget.LOCK) {
                            // Lock screen clock simulation header
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "08:00",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "Sun, Oct 1 · 24°C",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }

                            // Lock icon at bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            // Home screen bottom home indicator bar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                                    .size(width = 44.dp, height = 3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Current coordinate indicator pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    ) {
                        val mirroredSuffix = if (flipHorizontal) stringResource(R.string.adjust_mirrored_badge) else ""
                        val lockBadge = if (activeTargetTab == ScreenAdjustmentTarget.LOCK) {
                            if (isLockLinkedToHome) " · 跟随主屏" else " · 独立构图"
                        } else ""
                        Text(
                            text = stringResource(
                                R.string.adjust_current_focus,
                                (currentFocusX * 100).roundToInt(),
                                (currentFocusY * 100).roundToInt()
                            ) + mirroredSuffix + lockBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 3. Bottom Control Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(
                            top = 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                ) {
                    // Scrollable control parameters
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // When on Lock tab: Link with Home Screen toggle card
                        if (activeTargetTab == ScreenAdjustmentTarget.LOCK) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isLockLinkedToHome) Icons.Default.Link else Icons.Default.LinkOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.adjust_lock_link_title),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = stringResource(R.string.adjust_lock_link_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isLockLinkedToHome,
                                        onCheckedChange = { linked ->
                                            isLockLinkedToHome = linked
                                            if (linked) {
                                                lockFocusX = homeFocusX
                                                lockFocusY = homeFocusY
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Section: Focal Point Sliders & Presets
                        val targetFocusX = if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) homeFocusX else lockFocusX
                        val targetFocusY = if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) homeFocusY else lockFocusY
                        val isSlidersEnabled = activeTargetTab == ScreenAdjustmentTarget.HOME || !isLockLinkedToHome

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (activeTargetTab == ScreenAdjustmentTarget.HOME) {
                                            "主屏幕 · 焦点微调"
                                        } else if (isLockLinkedToHome) {
                                            "锁屏幕 (联动中，跟随主屏幕)"
                                        } else {
                                            "锁屏幕 · 独立焦点微调"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Horizontal focus slider (X)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.adjust_focus_x_label), style = MaterialTheme.typography.bodySmall)
                                    val xPercent = (targetFocusX * 100).roundToInt()
                                    Text(
                                        text = when {
                                            targetFocusX < 0.35f -> stringResource(R.string.adjust_focus_x_left, xPercent)
                                            targetFocusX > 0.65f -> stringResource(R.string.adjust_focus_x_right, xPercent)
                                            else -> stringResource(R.string.adjust_focus_x_center, xPercent)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = targetFocusX,
                                    onValueChange = { newVal ->
                                        if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) {
                                            homeFocusX = newVal
                                            if (isLockLinkedToHome) lockFocusX = newVal
                                        } else {
                                            lockFocusX = newVal
                                        }
                                    },
                                    valueRange = 0f..1f,
                                    enabled = isSlidersEnabled,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Vertical focus slider (Y)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.adjust_focus_y_label), style = MaterialTheme.typography.bodySmall)
                                    val yPercent = (targetFocusY * 100).roundToInt()
                                    Text(
                                        text = when {
                                            targetFocusY < 0.35f -> stringResource(R.string.adjust_focus_y_top, yPercent)
                                            targetFocusY > 0.65f -> stringResource(R.string.adjust_focus_y_bottom, yPercent)
                                            else -> stringResource(R.string.adjust_focus_y_center, yPercent)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = targetFocusY,
                                    onValueChange = { newVal ->
                                        if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) {
                                            homeFocusY = newVal
                                            if (isLockLinkedToHome) lockFocusY = newVal
                                        } else {
                                            lockFocusY = newVal
                                        }
                                    },
                                    valueRange = 0f..1f,
                                    enabled = isSlidersEnabled,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Focal presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) {
                                                homeFocusX = 0.5f; homeFocusY = 0.15f
                                                if (isLockLinkedToHome) { lockFocusX = 0.5f; lockFocusY = 0.15f }
                                            } else {
                                                lockFocusX = 0.5f; lockFocusY = 0.15f
                                            }
                                        },
                                        enabled = isSlidersEnabled,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.adjust_preset_head), style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) {
                                                homeFocusX = 0.5f; homeFocusY = 0.5f
                                                if (isLockLinkedToHome) { lockFocusX = 0.5f; lockFocusY = 0.5f }
                                            } else {
                                                lockFocusX = 0.5f; lockFocusY = 0.5f
                                            }
                                        },
                                        enabled = isSlidersEnabled,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.adjust_preset_center), style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (activeTargetTab == ScreenAdjustmentTarget.HOME || isLockLinkedToHome) {
                                                homeFocusX = 0.5f; homeFocusY = 0.85f
                                                if (isLockLinkedToHome) { lockFocusX = 0.5f; lockFocusY = 0.85f }
                                            } else {
                                                lockFocusX = 0.5f; lockFocusY = 0.85f
                                            }
                                        },
                                        enabled = isSlidersEnabled,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.adjust_preset_bottom), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Section: Horizontal Flip Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Section: Parallax Scroll Strategy Override (Relevant for Home Launcher)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.adjust_scroll_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.adjust_scroll_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedScrollMode == null,
                                        onClick = { selectedScrollMode = null },
                                        label = { Text(stringResource(R.string.adjust_scroll_follow_global), style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = selectedScrollMode == WallpaperScrollMode.AUTO,
                                        onClick = { selectedScrollMode = WallpaperScrollMode.AUTO },
                                        label = { Text(stringResource(R.string.adjust_scroll_auto), style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = selectedScrollMode == WallpaperScrollMode.ALWAYS,
                                        onClick = { selectedScrollMode = WallpaperScrollMode.ALWAYS },
                                        label = { Text(stringResource(R.string.adjust_scroll_always), style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = selectedScrollMode == WallpaperScrollMode.NEVER,
                                        onClick = { selectedScrollMode = WallpaperScrollMode.NEVER },
                                        label = { Text(stringResource(R.string.adjust_scroll_never), style = MaterialTheme.typography.labelSmall) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
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

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Pinned Action Buttons at Bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val finalLockFocusX = if (isLockLinkedToHome) null else lockFocusX
                                val finalLockFocusY = if (isLockLinkedToHome) null else lockFocusY
                                onSave(
                                    selectedScrollMode,
                                    if (homeFocusX == 0.5f && homeFocusY == 0.5f) null else homeFocusX,
                                    if (homeFocusX == 0.5f && homeFocusY == 0.5f) null else homeFocusY,
                                    flipHorizontal,
                                    false,
                                    finalLockFocusX,
                                    finalLockFocusY
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.adjust_save_memory_only))
                        }

                        Button(
                            onClick = {
                                val finalLockFocusX = if (isLockLinkedToHome) null else lockFocusX
                                val finalLockFocusY = if (isLockLinkedToHome) null else lockFocusY
                                onSave(
                                    selectedScrollMode,
                                    if (homeFocusX == 0.5f && homeFocusY == 0.5f) null else homeFocusX,
                                    if (homeFocusX == 0.5f && homeFocusY == 0.5f) null else homeFocusY,
                                    flipHorizontal,
                                    true,
                                    finalLockFocusX,
                                    finalLockFocusY
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
                }
            }
        }
    }
}
