package foo.barz.wallpaperpicker.ui.components

import androidx.compose.runtime.Composable
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode

/**
 * Backward compatibility wrapper for [WallpaperAdjustmentScreen].
 */
@Deprecated(
    message = "Use WallpaperAdjustmentScreen instead which provides a dedicated fullscreen adjustment UI.",
    replaceWith = ReplaceWith("WallpaperAdjustmentScreen(item, globalScrollMode, onDismiss, onSave)")
)
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
    WallpaperAdjustmentScreen(
        item = item,
        globalScrollMode = globalScrollMode,
        onDismiss = onDismiss,
        onSave = onSave
    )
}
