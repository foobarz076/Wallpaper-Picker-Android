package foo.barz.wallpaperpicker.ui.tabs.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.backup.BackupFormat
import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import foo.barz.wallpaperpicker.ui.MainUiState
import foo.barz.wallpaperpicker.ui.components.BackupExportDialog
import foo.barz.wallpaperpicker.ui.components.BackupExportMode
import foo.barz.wallpaperpicker.ui.components.BackupRestoreConfirmDialog
import foo.barz.wallpaperpicker.ui.components.BackupRestorePasswordDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sub-page for storage utilization, network cache eviction limits,
 * favorites backup export, and full configuration backup & restore.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageSettingsSubPage(
    state: MainUiState,
    onCacheSizeTierSelected: (CacheSizeTier) -> Unit,
    onExportFavorites: () -> Unit,
    onOpenManageSpace: () -> Unit,
    hasSensitiveData: Boolean = false,
    isOpenPgpAvailable: Boolean = false,
    onExportBackup: (Uri, String?, Boolean) -> Unit = { _, _, _ -> },
    onExportBackupWithOpenPgp: (Uri, Boolean, Boolean, Intent?, ((PendingIntent) -> Unit)) -> Unit = { _, _, _, _, _ -> },
    onDetectBackupFormat: (Uri) -> BackupFormat = { BackupFormat.PLAINTEXT },
    onRestoreBackup: (Uri, String?) -> Unit = { _, _ -> },
    onRestoreBackupWithOpenPgp: (Uri, Intent?, ((PendingIntent) -> Unit)) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportMode by remember { mutableStateOf(BackupExportMode.NATIVE_AES_GCM) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var pendingExportSanitize by remember { mutableStateOf(false) }
    var pendingExportSign by remember { mutableStateOf(false) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    var pendingOpenPgpAction by remember { mutableStateOf<((Intent?) -> Unit)?>(null) }

    val openPgpIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            pendingOpenPgpAction?.invoke(activityResult.data)
        }
        pendingOpenPgpAction = null
    }

    fun startOpenPgpExport(uri: Uri, sanitize: Boolean, sign: Boolean, resumeIntent: Intent? = null) {
        onExportBackupWithOpenPgp(uri, sanitize, sign, resumeIntent) { pendingIntent ->
            pendingOpenPgpAction = { returnedIntent ->
                startOpenPgpExport(uri, sanitize, sign, returnedIntent)
            }
            openPgpIntentSenderLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
    }

    fun startOpenPgpRestore(uri: Uri, resumeIntent: Intent? = null) {
        onRestoreBackupWithOpenPgp(uri, resumeIntent) { pendingIntent ->
            pendingOpenPgpAction = { returnedIntent ->
                startOpenPgpRestore(uri, returnedIntent)
            }
            openPgpIntentSenderLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            when (pendingExportMode) {
                BackupExportMode.NATIVE_AES_GCM, BackupExportMode.UNENCRYPTED -> {
                    onExportBackup(uri, pendingExportPassword, pendingExportSanitize)
                }
                BackupExportMode.OPENPGP -> {
                    startOpenPgpExport(uri, pendingExportSanitize, pendingExportSign)
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            when (onDetectBackupFormat(uri)) {
                BackupFormat.NATIVE_ENCRYPTED -> {
                    showRestorePasswordDialog = true
                }
                BackupFormat.OPENPGP -> {
                    startOpenPgpRestore(uri)
                }
                BackupFormat.PLAINTEXT -> {
                    showRestoreConfirmDialog = true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Storage Overview Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.storage_overview_title), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Favorites Storage Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.storage_favorites_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(R.string.storage_favorites_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.storage_favorites_summary, state.favoritesList.size, SettingsHelpers.formatFileSize(state.favoritesSizeBytes)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Transient Cache Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.storage_cache_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(R.string.storage_cache_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SettingsHelpers.formatFileSize(state.cacheSizeBytes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Cache Size Tier Configuration & Disabled Option
                Text(stringResource(R.string.storage_tier_title), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.storage_tier_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CacheSizeTier.entries.forEach { tier ->
                        FilterChip(
                            selected = state.cacheSizeTier == tier,
                            onClick = { onCacheSizeTierSelected(tier) },
                            label = { Text(stringResource(tier.displayNameRes)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(state.cacheSizeTier.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.cacheSizeTier == CacheSizeTier.DISABLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button: Export Favorites
                OutlinedButton(
                    onClick = onExportFavorites,
                    enabled = state.favoritesList.isNotEmpty() && !state.isExportingFavorites,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isExportingFavorites) stringResource(R.string.storage_exporting_favorites) else stringResource(R.string.storage_export_favorites))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manage Space Activity launcher
                OutlinedButton(
                    onClick = onOpenManageSpace,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.storage_btn_manage_space))
                }
            }
        }

        // Backup & Restore Configuration Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_card_title), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.backup_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_btn_export))
                    }

                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_btn_restore))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showExportDialog) {
        BackupExportDialog(
            hasSensitiveData = hasSensitiveData,
            isOpenPgpAvailable = isOpenPgpAvailable,
            onDismissRequest = { showExportDialog = false },
            onConfirmExport = { mode, password, sanitize, sign ->
                showExportDialog = false
                pendingExportMode = mode
                pendingExportPassword = password
                pendingExportSanitize = sanitize
                pendingExportSign = sign
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = when (mode) {
                    BackupExportMode.NATIVE_AES_GCM -> "wallpaper_picker_$timestamp.wpbak"
                    BackupExportMode.OPENPGP -> "wallpaper_picker_$timestamp.asc"
                    BackupExportMode.UNENCRYPTED -> "wallpaper_picker_$timestamp.json"
                }
                createDocumentLauncher.launch(filename)
            }
        )
    }

    if (showRestorePasswordDialog) {
        BackupRestorePasswordDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                pendingRestoreUri = null
            },
            onConfirmRestore = { password ->
                showRestorePasswordDialog = false
                pendingRestoreUri?.let { uri ->
                    onRestoreBackup(uri, password)
                }
                pendingRestoreUri = null
            }
        )
    }

    if (showRestoreConfirmDialog) {
        BackupRestoreConfirmDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            onConfirmRestore = {
                showRestoreConfirmDialog = false
                pendingRestoreUri?.let { uri ->
                    onRestoreBackup(uri, null)
                }
                pendingRestoreUri = null
            }
        )
    }
}
