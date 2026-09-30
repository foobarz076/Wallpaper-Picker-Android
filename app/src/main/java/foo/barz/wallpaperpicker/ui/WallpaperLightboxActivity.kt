package foo.barz.wallpaperpicker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import foo.barz.wallpaperpicker.MainActivity
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.action.WallpaperActionManager
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.data.PreferencesManager
import foo.barz.wallpaperpicker.ui.components.WallpaperAdjustmentScreen
import foo.barz.wallpaperpicker.ui.components.WallpaperLightboxViewer
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Fullscreen activity displaying the immersive wallpaper lightbox viewer.
 * Launched when tapping the desktop wallpaper widget or viewing the current wallpaper.
 */
class WallpaperLightboxActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PreferencesManager(this)
        val rawUri: Uri? = intent.data
            ?: intent.getStringExtra(EXTRA_WALLPAPER_URI)?.let { Uri.parse(it) }
            ?: prefs.lastWallpaperUri

        if (rawUri == null) {
            Toast.makeText(this, getString(R.string.dash_empty_title), Toast.LENGTH_SHORT).show()
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(mainIntent)
            finish()
            return
        }

        val uri: Uri = rawUri
        val initialTitle = intent.getStringExtra(EXTRA_WALLPAPER_TITLE) ?: prefs.lastWallpaperTitle
        val initialSourceTitle = intent.getStringExtra(EXTRA_WALLPAPER_SOURCE_TITLE) ?: prefs.lastWallpaperSourceTitle
        val historyDb = WallpaperHistoryDatabase(this)

        setContent {
            WallpaperPickerTheme {
                val scope = rememberCoroutineScope()
                var isFavorite by remember {
                    mutableStateOf(historyDb.getItemByUri(uri.toString())?.isFavorite ?: false)
                }
                var isSaving by remember { mutableStateOf(false) }
                var showLightbox by remember { mutableStateOf(true) }
                var showAdjustmentScreen by remember { mutableStateOf(false) }

                val writeStorageLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        isSaving = true
                        scope.launch {
                            val result = WallpaperActionManager.saveToGallery(this@WallpaperLightboxActivity, uri, initialTitle)
                            isSaving = false
                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_saved_to_gallery), Toast.LENGTH_SHORT).show()
                                } else {
                                    val err = result.exceptionOrNull()?.localizedMessage ?: getString(R.string.status_failed)
                                    Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_save_failed, err), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                val handleSaveClick: () -> Unit = {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@WallpaperLightboxActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            isSaving = true
                            scope.launch {
                                val result = WallpaperActionManager.saveToGallery(this@WallpaperLightboxActivity, uri, initialTitle)
                                isSaving = false
                                withContext(Dispatchers.Main) {
                                    if (result.isSuccess) {
                                        Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_saved_to_gallery), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val err = result.exceptionOrNull()?.localizedMessage ?: getString(R.string.status_failed)
                                        Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_save_failed, err), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            writeStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    } else {
                        isSaving = true
                        scope.launch {
                            val result = WallpaperActionManager.saveToGallery(this@WallpaperLightboxActivity, uri, initialTitle)
                            isSaving = false
                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_saved_to_gallery), Toast.LENGTH_SHORT).show()
                                } else {
                                    val err = result.exceptionOrNull()?.localizedMessage ?: getString(R.string.status_failed)
                                    Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_save_failed, err), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                BackHandler {
                    if (showAdjustmentScreen) {
                        showAdjustmentScreen = false
                        showLightbox = true
                    } else {
                        finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (showLightbox) {
                        WallpaperLightboxViewer(
                            imageUri = uri,
                            title = initialTitle,
                            sourceBadge = initialSourceTitle,
                            isFavorite = isFavorite,
                            isSaving = isSaving,
                            onDismiss = { finish() },
                            onToggleFavorite = {
                                val newFav = !isFavorite
                                isFavorite = newFav
                                scope.launch(Dispatchers.IO) {
                                    historyDb.updateFavoriteByUri(uri.toString(), newFav)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@WallpaperLightboxActivity,
                                            if (newFav) getString(R.string.lightbox_added_favorite) else getString(R.string.lightbox_removed_favorite),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onOpenInGallery = {
                                scope.launch {
                                    val result = WallpaperActionManager.openInGallery(this@WallpaperLightboxActivity, uri)
                                    if (result.isFailure) {
                                        Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_cannot_open_gallery), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSaveToGallery = handleSaveClick,
                            onShareWallpaper = {
                                scope.launch {
                                    val result = WallpaperActionManager.shareWallpaper(this@WallpaperLightboxActivity, uri, initialTitle)
                                    if (result.isFailure) {
                                        Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_share_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenAdjustment = {
                                showLightbox = false
                                showAdjustmentScreen = true
                            }
                        )
                    }

                    if (showAdjustmentScreen) {
                        val historyItem = historyDb.getItemByUri(uri.toString()) ?: WallpaperHistoryItem(
                            sourceUri = uri.toString(),
                            title = initialTitle,
                            sourceType = prefs.lastWallpaperSourceType ?: WallpaperSourceType.LOCAL_FOLDER,
                            appliedTimestamp = prefs.lastChangedTimestamp
                        )
                        WallpaperAdjustmentScreen(
                            item = historyItem,
                            globalScrollMode = prefs.scrollMode,
                            onDismiss = {
                                showAdjustmentScreen = false
                                showLightbox = true
                            },
                            onSave = { customScrollMode, cropFocusX, cropFocusY, flipHorizontal, applyImmediately ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        historyDb.updateCustomPreferencesByUri(
                                            sourceUri = uri.toString(),
                                            customScrollMode = customScrollMode,
                                            cropFocusX = cropFocusX,
                                            cropFocusY = cropFocusY,
                                            flipHorizontal = flipHorizontal
                                        )
                                        if (applyImmediately) {
                                            val processor = WallpaperProcessor(applicationContext)
                                            val applier = WallpaperApplier(applicationContext)
                                            val processResult = processor.processForTarget(
                                                openStream = {
                                                    if (uri.scheme == "file") {
                                                        FileInputStream(File(uri.path!!))
                                                    } else {
                                                        contentResolver.openInputStream(uri)
                                                            ?: throw FileNotFoundException("无法打开图片流: $uri")
                                                    }
                                                },
                                                target = prefs.target,
                                                scrollMode = customScrollMode ?: prefs.scrollMode,
                                                cropMode = prefs.cropMode,
                                                cropFocusX = cropFocusX ?: 0.5f,
                                                cropFocusY = cropFocusY ?: 0.5f,
                                                flipHorizontal = flipHorizontal,
                                                lockScreenStrategy = prefs.lockScreenStrategy
                                            )
                                            if (processResult.isSuccess) {
                                                applier.apply(processResult.getOrThrow())
                                            }
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@WallpaperLightboxActivity, getString(R.string.lightbox_composition_saved), Toast.LENGTH_SHORT).show()
                                        showAdjustmentScreen = false
                                        showLightbox = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_WALLPAPER_URI = "extra_wallpaper_uri"
        const val EXTRA_WALLPAPER_TITLE = "extra_wallpaper_title"
        const val EXTRA_WALLPAPER_SOURCE_TITLE = "extra_wallpaper_source_title"

        fun createIntent(
            context: Context,
            uri: Uri? = null,
            title: String? = null,
            sourceTitle: String? = null
        ): Intent {
            return Intent(context, WallpaperLightboxActivity::class.java).apply {
                if (uri != null) putExtra(EXTRA_WALLPAPER_URI, uri.toString())
                if (title != null) putExtra(EXTRA_WALLPAPER_TITLE, title)
                if (sourceTitle != null) putExtra(EXTRA_WALLPAPER_SOURCE_TITLE, sourceTitle)
            }
        }
    }
}
