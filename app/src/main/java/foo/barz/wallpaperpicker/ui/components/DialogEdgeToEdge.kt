package foo.barz.wallpaperpicker.ui.components

import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

data class DialogWindowInsets(
    val statusBarTop: Dp,
    val navBarBottom: Dp
)

/**
 * Configures the underlying [android.view.Window] of a Compose Dialog to be fully edge-to-edge:
 * - Extends layout through display cutouts (notches / camera punch holes) via [WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES].
 * - Disables decor system window fitting so content can draw into status bar and navigation bar areas.
 * - Sets status bar and navigation bar colors to transparent.
 * - Adjusts appearance of system bar icons according to [isLightBars].
 * - Dynamically observes real system window insets (status bars, cutouts, navigation bars) from the dialog decor view,
 *   bypassing the known Compose Dialog bug where WindowInsets.navigationBars / statusBars returns 0.
 */
@Composable
fun rememberEdgeToEdgeDialog(isLightBars: Boolean = false): DialogWindowInsets {
    val dialogView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val dialogWindow = remember(dialogView, lifecycleOwner) {
        (lifecycleOwner as? DialogWindowProvider)?.window
            ?: (dialogView.parent as? DialogWindowProvider)?.window
            ?: (dialogView.context as? DialogWindowProvider)?.window
    }

    val decorView = dialogWindow?.decorView ?: dialogView.rootView

    val initialInsets = remember(decorView) { ViewCompat.getRootWindowInsets(decorView) }
    val initialStatusPx = initialInsets?.getInsets(
        WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
    )?.top ?: 0
    val initialNavPx = initialInsets?.getInsets(
        WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
    )?.bottom ?: 0

    var statusBarTopPx by remember(decorView) { mutableIntStateOf(initialStatusPx) }
    var navBarBottomPx by remember(decorView) { mutableIntStateOf(initialNavPx) }

    DisposableEffect(dialogView, lifecycleOwner, isLightBars, dialogWindow) {
        dialogWindow?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = isLightBars
            insetsController.isAppearanceLightNavigationBars = isLightBars
        }

        fun updateInsets(insets: WindowInsetsCompat?) {
            if (insets == null) return
            val status = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val nav = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
            if (status.top > 0) statusBarTopPx = status.top
            if (nav.bottom > 0) navBarBottomPx = nav.bottom
        }

        updateInsets(ViewCompat.getRootWindowInsets(decorView))

        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            updateInsets(insets)
            insets
        }
        ViewCompat.requestApplyInsets(decorView)

        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(decorView, null)
        }
    }

    val composeStatusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val composeNavBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val statusBarDp = with(density) { statusBarTopPx.toDp() }
    val navBarDp = with(density) { navBarBottomPx.toDp() }

    val resolvedStatusTop = if (statusBarDp > 0.dp) statusBarDp else composeStatusBarTop
    val resolvedNavBarBottom = if (navBarDp > 0.dp) navBarDp else composeNavBarBottom

    return remember(resolvedStatusTop, resolvedNavBarBottom) {
        DialogWindowInsets(
            statusBarTop = resolvedStatusTop,
            navBarBottom = resolvedNavBarBottom
        )
    }
}

/**
 * Backwards-compatible convenience function for configuring an edge-to-edge dialog.
 */
@Composable
fun ConfigureEdgeToEdgeDialog(isLightBars: Boolean = false) {
    rememberEdgeToEdgeDialog(isLightBars = isLightBars)
}
