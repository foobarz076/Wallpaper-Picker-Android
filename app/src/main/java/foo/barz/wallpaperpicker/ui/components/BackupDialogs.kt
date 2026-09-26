package foo.barz.wallpaperpicker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import foo.barz.wallpaperpicker.R

/**
 * Encryption modes available for configuration backup export.
 */
enum class BackupExportMode {
    NATIVE_AES_GCM,
    OPENPGP,
    UNENCRYPTED
}

/**
 * Dialog prompting user for encryption options (AES-GCM, OpenKeychain, or Plaintext),
 * redaction options, and SAF migration notices.
 */
@Composable
fun BackupExportDialog(
    hasSensitiveData: Boolean,
    isOpenPgpAvailable: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmExport: (mode: BackupExportMode, password: String?, sanitize: Boolean, sign: Boolean) -> Unit
) {
    var selectedMode by remember { mutableStateOf(BackupExportMode.NATIVE_AES_GCM) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var sanitize by remember { mutableStateOf(true) }
    var signOpenPgp by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val passwordEmptyError = stringResource(R.string.backup_export_password_empty)
    val passwordMismatchError = stringResource(R.string.backup_export_password_mismatch)

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
            Text(stringResource(R.string.backup_export_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_export_mode_label),
                    style = MaterialTheme.typography.titleSmall
                )

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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 2. OpenPGP (OpenKeychain)
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

                // Contextual sub-controls based on selected mode
                when (selectedMode) {
                    BackupExportMode.NATIVE_AES_GCM -> {
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

                    BackupExportMode.OPENPGP -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
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
                                Spacer(modifier = Modifier.height(8.dp))
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
                                            style = MaterialTheme.typography.bodyMedium
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
        },
        confirmButton = {
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
                            onConfirmExport(BackupExportMode.NATIVE_AES_GCM, password, false, false)
                        }

                        BackupExportMode.OPENPGP -> {
                            onConfirmExport(BackupExportMode.OPENPGP, null, false, signOpenPgp)
                        }

                        BackupExportMode.UNENCRYPTED -> {
                            onConfirmExport(BackupExportMode.UNENCRYPTED, null, if (hasSensitiveData) sanitize else false, false)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.backup_export_btn_proceed))
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
