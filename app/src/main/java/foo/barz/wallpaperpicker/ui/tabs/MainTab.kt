package foo.barz.wallpaperpicker.ui.tabs

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import foo.barz.wallpaperpicker.R

/**
 * Top-level navigation destination tabs for the main screen.
 */
enum class MainTab(
    @get:StringRes val titleRes: Int,
    val icon: ImageVector
) {
    DASHBOARD(
        titleRes = R.string.tab_dashboard,
        icon = Icons.Default.Dashboard
    ),
    HISTORY(
        titleRes = R.string.tab_history,
        icon = Icons.Default.History
    ),
    SOURCES(
        titleRes = R.string.tab_sources,
        icon = Icons.Default.PhotoLibrary
    ),
    SETTINGS(
        titleRes = R.string.tab_settings,
        icon = Icons.Default.Tune
    )
}
