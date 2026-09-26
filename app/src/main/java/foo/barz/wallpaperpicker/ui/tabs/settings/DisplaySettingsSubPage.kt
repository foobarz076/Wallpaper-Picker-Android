package foo.barz.wallpaperpicker.ui.tabs.settings

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperTarget
import foo.barz.wallpaperpicker.ui.MainUiState

/**
 * Sub-page for display targets, image crop modes, and parallax wallpaper scrolling settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplaySettingsSubPage(
    state: MainUiState,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onCropModeSelected: (WallpaperCropMode) -> Unit,
    onScrollModeSelected: (WallpaperScrollMode) -> Unit,
    onToggleReapplyOnScrollChange: (Boolean) -> Unit,
    onReapplyCurrentWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Target Screen Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.display_target_card_title), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val canSeparateTarget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                if (!canSeparateTarget) {
                    Text(
                        text = stringResource(R.string.display_target_legacy_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.target == WallpaperTarget.BOTH,
                        onClick = { onTargetSelected(WallpaperTarget.BOTH) },
                        label = { Text(stringResource(WallpaperTarget.BOTH.labelRes)) }
                    )
                    if (canSeparateTarget) {
                        FilterChip(
                            selected = state.target == WallpaperTarget.SYSTEM,
                            onClick = { onTargetSelected(WallpaperTarget.SYSTEM) },
                            label = { Text(stringResource(WallpaperTarget.SYSTEM.labelRes)) }
                        )
                        FilterChip(
                            selected = state.target == WallpaperTarget.LOCK,
                            onClick = { onTargetSelected(WallpaperTarget.LOCK) },
                            label = { Text(stringResource(WallpaperTarget.LOCK.labelRes)) }
                        )
                    }
                }
            }
        }

        // 2. Wallpaper Crop Mode Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.display_crop_card_title), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        WallpaperCropMode.FIT_HEIGHT,
                        WallpaperCropMode.CENTER_CROP,
                        WallpaperCropMode.FIT_CENTER
                    ).forEach { mode ->
                        FilterChip(
                            selected = state.cropMode == mode,
                            onClick = { onCropModeSelected(mode) },
                            label = { Text(stringResource(mode.labelRes)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(state.cropMode.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // 3. Wallpaper Scroll Mode Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ViewCarousel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.display_scroll_card_title), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        WallpaperScrollMode.AUTO,
                        WallpaperScrollMode.NEVER,
                        WallpaperScrollMode.ALWAYS
                    ).forEach { mode ->
                        FilterChip(
                            selected = state.scrollMode == mode,
                            onClick = { onScrollModeSelected(mode) },
                            label = { Text(stringResource(mode.labelRes)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(state.scrollMode.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.display_scroll_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.display_reapply_on_change_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(R.string.display_reapply_on_change_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.reapplyOnScrollChange,
                        onCheckedChange = onToggleReapplyOnScrollChange
                    )
                }

                if (state.lastWallpaperUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReapplyCurrentWallpaper,
                        enabled = !state.isChanging,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.display_btn_reapply_now))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
