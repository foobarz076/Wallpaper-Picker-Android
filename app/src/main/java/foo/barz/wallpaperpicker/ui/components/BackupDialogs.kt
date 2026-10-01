package foo.barz.wallpaperpicker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.ui.RestoreSummary

/**
 * Encryption modes available for configuration backup export.
 */
enum class BackupExportMode {
    NATIVE_AES_GCM,
    OPENPGP,
    UNENCRYPTED
}

/**
 * Modern Material 3 Bottom Sheet prompting user for export content options (configs & favorite images),
 * encryption modes (AES-GCM, OpenPGP, or Plaintext), credentials redaction, and SAF notices.
 * Handles IME padding gracefully and keeps action buttons visible at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupExportSheet(
    hasSensitiveData: Boolean,
    isOpenPgpAvailable: Boolean,
    favoritesCount: Int = 0,
    favoritesSizeBytes: Long = 0L,
    onDismissRequest: () -> Unit,
    onConfirmExport: (mode: BackupExportMode, password: String?, sanitize: Boolean, sign: Boolean, includeFavorites: Boolean, includeAllHistory: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMode by remember { mutableStateOf(BackupExportMode.NATIVE_AES_GCM) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var sanitize by remember { mutableStateOf(true) }
    var signOpenPgp by remember { mutableStateOf(false) }
    var includeFavorites by remember { mutableStateOf(favoritesCount > 0) }
    var includeAllHistory by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val passwordEmptyError = stringResource(R.string.backup_export_password_empty)
    val passwordMismatchError = stringResource(R.string.backup_export_password_mismatch)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.backup_export_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.backup_export_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_cancel))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Backup Content
                Text(
                    text = stringResource(R.string.backup_export_content_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.backup_export_content_base),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.backup_export_content_base_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (favoritesCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { includeFavorites = !includeFavorites },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = includeFavorites,
                                    onCheckedChange = { includeFavorites = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.backup_export_include_favorites_checkbox),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.backup_export_include_favorites_desc,
                                            favoritesCount,
                                            foo.barz.wallpaperpicker.ui.tabs.settings.SettingsHelpers.formatFileSize(favoritesSizeBytes)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { includeAllHistory = !includeAllHistory },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeAllHistory,
                                onCheckedChange = { includeAllHistory = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_export_include_all_history_checkbox),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.backup_export_include_all_history_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Section 2: Encryption & Security
                Text(
                    text = stringResource(R.string.backup_export_mode_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. Native AES-GCM
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = BackupExportMode.NATIVE_AES_GCM },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == BackupExportMode.NATIVE_AES_GCM,
                                onClick = { selectedMode = BackupExportMode.NATIVE_AES_GCM }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.backup_export_mode_native),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 2. OpenPGP
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isOpenPgpAvailable) { selectedMode = BackupExportMode.OPENPGP },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == BackupExportMode.OPENPGP,
                                onClick = { selectedMode = BackupExportMode.OPENPGP },
                                enabled = isOpenPgpAvailable
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOpenPgpAvailable) {
                                    stringResource(R.string.backup_export_mode_openpgp)
                                } else {
                                    stringResource(R.string.backup_export_mode_openpgp_unavailable)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOpenPgpAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                        }

                        // 3. Plaintext
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = BackupExportMode.UNENCRYPTED },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == BackupExportMode.UNENCRYPTED,
                                onClick = { selectedMode = BackupExportMode.UNENCRYPTED }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.backup_export_mode_none),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Dynamic sub-controls for selected mode
                when (selectedMode) {
                    BackupExportMode.NATIVE_AES_GCM -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    validationError = null
                                },
                                label = { Text(stringResource(R.string.backup_export_password_label)) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    validationError = null
                                },
                                label = { Text(stringResource(R.string.backup_export_password_confirm)) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (validationError != null) {
                                Text(
                                    text = validationError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Text(
                                text = stringResource(R.string.backup_export_crypto_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    BackupExportMode.OPENPGP -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.backup_export_openpgp_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { signOpenPgp = !signOpenPgp }
                                ) {
                                    Checkbox(
                                        checked = signOpenPgp,
                                        onCheckedChange = { signOpenPgp = it }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.backup_export_openpgp_sign_checkbox),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = stringResource(R.string.backup_export_openpgp_sign_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BackupExportMode.UNENCRYPTED -> {
                        if (hasSensitiveData) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.backup_export_warning_sensitive),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { sanitize = !sanitize }
                                    ) {
                                        Checkbox(
                                            checked = sanitize,
                                            onCheckedChange = { sanitize = it }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.backup_export_sanitize_checkbox),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SAF Security notice
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.backup_export_saf_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sticky Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        when (selectedMode) {
                            BackupExportMode.NATIVE_AES_GCM -> {
                                if (password.isBlank()) {
                                    validationError = passwordEmptyError
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    validationError = passwordMismatchError
                                    return@Button
                                }
                                onConfirmExport(BackupExportMode.NATIVE_AES_GCM, password, false, false, includeFavorites, includeAllHistory)
                            }
                            BackupExportMode.OPENPGP -> {
                                onConfirmExport(BackupExportMode.OPENPGP, null, false, signOpenPgp, includeFavorites, includeAllHistory)
                            }
                            BackupExportMode.UNENCRYPTED -> {
                                onConfirmExport(BackupExportMode.UNENCRYPTED, null, if (hasSensitiveData) sanitize else false, false, includeFavorites, includeAllHistory)
                            }
                        }
                    },
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(stringResource(R.string.backup_export_btn_proceed))
                }
            }
        }
    }
}

/**
 * Backward-compatible delegating dialog wrapper that forwards to [BackupExportSheet].
 */
@Composable
fun BackupExportDialog(
    hasSensitiveData: Boolean,
    isOpenPgpAvailable: Boolean,
    favoritesCount: Int = 0,
    favoritesSizeBytes: Long = 0L,
    onDismissRequest: () -> Unit,
    onConfirmExport: (mode: BackupExportMode, password: String?, sanitize: Boolean, sign: Boolean, includeFavorites: Boolean, includeAllHistory: Boolean) -> Unit
) {
    BackupExportSheet(
        hasSensitiveData = hasSensitiveData,
        isOpenPgpAvailable = isOpenPgpAvailable,
        favoritesCount = favoritesCount,
        favoritesSizeBytes = favoritesSizeBytes,
        onDismissRequest = onDismissRequest,
        onConfirmExport = onConfirmExport
    )
}

/**
 * Dialog prompting for password to decrypt an encrypted backup file.
 */
@Composable
fun BackupRestorePasswordDialog(
    onDismissRequest: () -> Unit,
    onConfirmRestore: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isBlankError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.backup_restore_pwd_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_restore_pwd_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isBlankError = false
                    },
                    label = { Text(stringResource(R.string.backup_restore_pwd_label)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isBlankError) {
                    Text(
                        text = stringResource(R.string.backup_export_password_empty),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        isBlankError = true
                    } else {
                        onConfirmRestore(password)
                    }
                }
            ) {
                Text(stringResource(R.string.backup_restore_btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Dialog confirming restoration of unencrypted configuration backup.
 */
@Composable
fun BackupRestoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirmRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.backup_restore_confirm_title))
        },
        text = {
            Text(
                text = stringResource(R.string.backup_restore_confirm_msg),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirmRestore) {
                Text(stringResource(R.string.backup_restore_btn_overwrite))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Dialog prompting user to batch download missing favorited wallpapers after restore.
 */
@Composable
fun BackupRestoreMissingFavoritesDialog(
    missingCount: Int,
    onDismissRequest: () -> Unit,
    onConfirmDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.backup_restore_missing_fav_title))
        },
        text = {
            Text(
                text = stringResource(R.string.backup_restore_missing_fav_desc, missingCount),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirmDownload) {
                Text(stringResource(R.string.backup_restore_missing_fav_btn_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.backup_restore_missing_fav_btn_later))
            }
        }
    )
}

/**
 * Guided summary dialog shown immediately after backup restore completes.
 * Summarizes restored sources, rules, and favorite memory, displays OpenPGP signature verification,
 * and provides actionable one-tap deep-links for re-authorizing local SAF folders or
 * batch-downloading missing remote favorite images.
 */
@Composable
fun BackupRestoreSummaryDialog(
    summary: RestoreSummary,
    onDismissRequest: () -> Unit,
    onNavigateToSources: () -> Unit,
    onBatchDownloadFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    isDownloadingFavorites: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.backup_restore_summary_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.backup_restore_summary_metrics,
                        summary.restoredSourcesCount,
                        summary.restoredRulesCount,
                        summary.restoredFavoritesCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!summary.sigNotice.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = summary.sigNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                val hasActionableItems = summary.needsReauthorizationCount > 0 ||
                        summary.missingFavoritesCount > 0 ||
                        summary.missingRemoteHistoryCount > 0
                if (hasActionableItems) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Text(
                        text = stringResource(R.string.backup_restore_action_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // 1. SAF local folder re-authorization notice
                    if (summary.needsReauthorizationCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.backup_restore_saf_warning_title, summary.needsReauthorizationCount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.backup_restore_saf_warning_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onNavigateToSources,
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.backup_restore_saf_btn_sources),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    // 2. Missing favorite images batch redownload notice
                    if (summary.missingFavoritesCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.backup_restore_fav_missing_title, summary.missingFavoritesCount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.backup_restore_fav_missing_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                val isBusy = isDownloadingFavorites
                                Button(
                                    onClick = onBatchDownloadFavorites,
                                    enabled = !isBusy,
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.backup_restore_fav_btn_downloading),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.backup_restore_fav_btn_batch_download),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Missing remote history images notice
                    if (summary.missingRemoteHistoryCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.backup_restore_remote_missing_title, summary.missingRemoteHistoryCount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.backup_restore_remote_missing_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onNavigateToHistory,
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.backup_restore_remote_btn_history),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(stringResource(R.string.backup_restore_btn_done))
            }
        }
    )
}


