package foo.barz.wallpaperpicker.ui.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation destination tabs for the main screen.
 */
enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD(
        title = "控制台",
        icon = Icons.Default.Dashboard
    ),
    HISTORY(
        title = "历史",
        icon = Icons.Default.History
    ),
    SOURCES(
        title = "图源",
        icon = Icons.Default.PhotoLibrary
    ),
    SETTINGS(
        title = "设置",
        icon = Icons.Default.Tune
    )
}
