package foo.barz.wallpaperpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import foo.barz.wallpaperpicker.ui.MainScreen
import foo.barz.wallpaperpicker.ui.MainViewModel
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WallpaperPickerTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                MainScreen(
                    state = uiState,
                    onSourceTypeSelected = viewModel::onSourceTypeSelected,
                    onFolderSelected = viewModel::onFolderSelected,
                    onHttpPresetSelected = viewModel::onHttpPresetSelected,
                    onHttpCustomUrlChanged = viewModel::onHttpCustomUrlChanged,
                    onHttpCustomJsonPathChanged = viewModel::onHttpCustomJsonPathChanged,
                    onToggleWifiOnly = viewModel::onToggleWifiOnly,
                    onClearCache = viewModel::onClearCache,
                    onIntervalSelected = viewModel::onIntervalSelected,
                    onTargetSelected = viewModel::onTargetSelected,
                    onScrollModeSelected = viewModel::onScrollModeSelected,
                    onToggleSchedule = viewModel::toggleSchedule,
                    onChangeNow = viewModel::changeNow,
                    onClearStatus = viewModel::clearStatusMessage
                )
            }
        }
    }
}
