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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import foo.barz.wallpaperpicker.MainActivity
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.action.WallpaperActionManager
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.widget.CurrentWallpaperWidgetProvider
import foo.barz.wallpaperpicker.data.PreferencesManager
import foo.barz.wallpaperpicker.ui.components.WallpaperLightboxViewer
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Fullscreen activity displaying the immersive wallpaper lightbox viewer.
 * Supports viewing both current active wallpaper (console mode) and historical/favorited
 * wallpapers with contextual actions (setting as wallpaper and deleting record).
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
        val historyId = intent.getLongExtra(EXTRA_HISTORY_ID, 0L)
        val canApplyWallpaper = intent.getBooleanExtra(EXTRA_CAN_APPLY, false)
        val canDeleteRecord = intent.getBooleanExtra(EXTRA_CAN_DELETE, false)
        val rawSourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE)?.let {
            runCatching { WallpaperSourceType.valueOf(it) }.getOrNull()
        }
        val effectiveSourceType = rawSourceType
            ?: prefs.lastWallpaperSourceType
            ?: WallpaperSourceType.LOCAL_FOLDER

        val historyDb = WallpaperHistoryDatabase(this)

        setContent {
            WallpaperPickerTheme {
                val scope = rememberCoroutineScope()
                val isAccessible = remember(uri) {
                    WallpaperActionManager.isUriAccessible(this@WallpaperLightboxActivity, uri)
                }
                val initialHistoryItem = remember {
                    if (historyId > 0) historyDb.getItemById(historyId) else historyDb.getItemByUri(uri.toString())
                }
                var isFavorite by remember {
                    mutableStateOf(initialHistoryItem?.isFavorite ?: (historyDb.getItemByUri(uri.toString())?.isFavorite ?: false))
                }
                var isSaving by remember { mutableStateOf(false) }
                var isApplying by remember { mutableStateOf(false) }
                var isApplied by remember {
                    mutableStateOf(prefs.lastWallpaperUri != null && prefs.lastWallpaperUri == uri)
                }
                var showDeleteDialog by remember { mutableStateOf(false) }

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

                val handleApplyWallpaper: () -> Unit = {
                    if (isApplied) {
                        Toast.makeText(
                            this@WallpaperLightboxActivity,
                            getString(R.string.hist_action_already_applied),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!isApplying && isAccessible) {
                        isApplying = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val targetItem = (if (historyId > 0) historyDb.getItemById(historyId) else null)
                                        ?: historyDb.getItemByUri(uri.toString())

                                    val streamProvider = {
                                        if (uri.scheme == "file") {
                                            FileInputStream(File(uri.path ?: throw IllegalArgumentException("Invalid file path")))
                                        } else {
                                            contentResolver.openInputStream(uri)
                                                ?: throw IllegalStateException("Cannot open input stream: $uri")
                                        }
                                    }

                                    val effectiveScrollMode = targetItem?.customScrollMode ?: prefs.scrollMode
                                    val effectiveCropFocusX = targetItem?.cropFocusX ?: 0.5f
                                    val effectiveCropFocusY = targetItem?.cropFocusY ?: 0.5f
                                    val effectiveLockCropFocusX = targetItem?.lockCropFocusX
                                    val effectiveLockCropFocusY = targetItem?.lockCropFocusY
                                    val effectiveFlipHorizontal = targetItem?.flipHorizontal ?: false

                                    val processor = WallpaperProcessor(this@WallpaperLightboxActivity)
                                    val applier = WallpaperApplier(this@WallpaperLightboxActivity)

                                    val processed = processor.processForTarget(
                                        openStream = streamProvider,
                                        target = prefs.target,
                                        scrollMode = effectiveScrollMode,
                                        cropMode = prefs.cropMode,
                                        cropFocusX = effectiveCropFocusX,
                                        cropFocusY = effectiveCropFocusY,
                                        flipHorizontal = effectiveFlipHorizontal,
                                        lockScreenStrategy = prefs.lockScreenStrategy,
                                        lockCropFocusX = effectiveLockCropFocusX,
                                        lockCropFocusY = effectiveLockCropFocusY
                                    ).getOrThrow()
                                    applier.apply(processed).getOrThrow()

                                    val now = System.currentTimeMillis()
                                    val concreteTitle = targetItem?.displaySourceBadge ?: initialSourceTitle ?: getString(R.string.tab_history)
                                    val concreteSourceType = targetItem?.sourceType ?: effectiveSourceType

                                    prefs.lastChangedTimestamp = now
                                    prefs.lastWallpaperTitle = initialTitle
                                    prefs.lastWallpaperUri = uri
                                    prefs.lastWallpaperSourceType = concreteSourceType
                                    prefs.lastWallpaperSourceTitle = concreteTitle
                                    prefs.lastErrorMessage = null
                                    prefs.lastExecutionStatus = "成功"

                                    historyDb.recordAppliedWallpaper(
                                        sourceUri = uri,
                                        title = initialTitle,
                                        sourceType = concreteSourceType,
                                        appliedTimestamp = now,
                                        sourceTitle = concreteTitle,
                                        remoteUrl = targetItem?.remoteUrl
                                    )
                                    CurrentWallpaperWidgetProvider.updateAllWidgets(this@WallpaperLightboxActivity)
                                }
                            }
                            isApplying = false
                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    isApplied = true
                                    Toast.makeText(
                                        this@WallpaperLightboxActivity,
                                        getString(R.string.status_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val err = result.exceptionOrNull()?.localizedMessage ?: getString(R.string.status_failed)
                                    Toast.makeText(
                                        this@WallpaperLightboxActivity,
                                        getString(R.string.status_failed) + ": " + err,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }

                val handleDeleteConfirmed: () -> Unit = {
                    scope.launch(Dispatchers.IO) {
                        val targetItem = (if (historyId > 0) historyDb.getItemById(historyId) else null)
                            ?: historyDb.getItemByUri(uri.toString())
                        if (targetItem != null) {
                            if (!targetItem.favoriteFilePath.isNullOrBlank()) {
                                val f = File(targetItem.favoriteFilePath)
                                if (f.exists()) f.delete()
                            }
                            historyDb.deleteRecord(targetItem.id)
                        } else if (historyId > 0) {
                            historyDb.deleteRecord(historyId)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@WallpaperLightboxActivity,
                                getString(R.string.hist_record_deleted),
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    WallpaperLightboxViewer(
                        imageUri = uri,
                        title = initialTitle,
                        sourceBadge = initialSourceTitle,
                        isFavorite = isFavorite,
                        isSaving = isSaving,
                        canApplyWallpaper = canApplyWallpaper,
                        isApplyingWallpaper = isApplying,
                        isApplyEnabled = isAccessible,
                        isApplied = isApplied,
                        canDeleteRecord = canDeleteRecord,
                        onDismiss = { finish() },
                        onToggleFavorite = {
                            val newFav = !isFavorite
                            isFavorite = newFav
                            scope.launch(Dispatchers.IO) {
                                val targetItem = (if (historyId > 0) historyDb.getItemById(historyId) else null)
                                    ?: historyDb.getItemByUri(uri.toString())
                                if (targetItem != null) {
                                    val isNetwork = targetItem.sourceType == WallpaperSourceType.IMMICH || targetItem.sourceType == WallpaperSourceType.HTTP_API
                                    val promotedPath = if (newFav && isNetwork) {
                                        WallpaperActionManager.promoteToPermanentFavorite(this@WallpaperLightboxActivity, Uri.parse(targetItem.sourceUri))
                                    } else {
                                        null
                                    }
                                    if (!newFav && !targetItem.favoriteFilePath.isNullOrBlank()) {
                                        val f = File(targetItem.favoriteFilePath)
                                        if (f.exists()) f.delete()
                                    }
                                    historyDb.updateFavorite(
                                        id = targetItem.id,
                                        isFavorite = newFav,
                                        favoriteTimestamp = if (newFav) System.currentTimeMillis() else null,
                                        favoriteFilePath = if (newFav) promotedPath else null
                                    )
                                } else {
                                    historyDb.updateFavoriteByUri(uri.toString(), newFav)
                                }
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
                            val adjustIntent = WallpaperAdjustmentActivity.createIntent(
                                context = this@WallpaperLightboxActivity,
                                uri = uri,
                                title = initialTitle,
                                sourceType = effectiveSourceType,
                                historyId = historyId
                            )
                            startActivity(adjustIntent)
                        },
                        onApplyWallpaper = handleApplyWallpaper,
                        onDeleteRecord = { showDeleteDialog = true }
                    )

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = { Text(stringResource(R.string.hist_dialog_delete_title)) },
                            text = {
                                val wallpaperName = initialTitle ?: stringResource(R.string.hist_item_this_wallpaper)
                                val detailText = if (isFavorite) {
                                    stringResource(R.string.hist_dialog_delete_fav_msg, wallpaperName)
                                } else {
                                    stringResource(R.string.hist_dialog_delete_msg, wallpaperName)
                                }
                                Text(detailText)
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteDialog = false
                                        handleDeleteConfirmed()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text(stringResource(R.string.action_delete))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text(stringResource(R.string.action_cancel))
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
        const val EXTRA_HISTORY_ID = "extra_history_id"
        const val EXTRA_SOURCE_TYPE = "extra_source_type"
        const val EXTRA_CAN_APPLY = "extra_can_apply"
        const val EXTRA_CAN_DELETE = "extra_can_delete"

        fun createIntent(
            context: Context,
            uri: Uri? = null,
            title: String? = null,
            sourceTitle: String? = null,
            historyId: Long = 0L,
            sourceType: WallpaperSourceType? = null,
            canApplyWallpaper: Boolean = false,
            canDeleteRecord: Boolean = false
        ): Intent {
            return Intent(context, WallpaperLightboxActivity::class.java).apply {
                if (uri != null) putExtra(EXTRA_WALLPAPER_URI, uri.toString())
                if (title != null) putExtra(EXTRA_WALLPAPER_TITLE, title)
                if (sourceTitle != null) putExtra(EXTRA_WALLPAPER_SOURCE_TITLE, sourceTitle)
                if (historyId > 0) putExtra(EXTRA_HISTORY_ID, historyId)
                if (sourceType != null) putExtra(EXTRA_SOURCE_TYPE, sourceType.name)
                putExtra(EXTRA_CAN_APPLY, canApplyWallpaper)
                putExtra(EXTRA_CAN_DELETE, canDeleteRecord)
            }
        }
    }
}
