package foo.barz.wallpaperpicker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import foo.barz.wallpaperpicker.core.util.AppLog
import foo.barz.wallpaperpicker.core.util.DiagnosticReportHelper
import foo.barz.wallpaperpicker.data.PreferencesManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.ui.theme.WallpaperPickerTheme
import kotlinx.coroutines.launch

/**
 * Metadata model representing an open-source library used by this application.
 */
data class OpenSourceLibrary(
    val name: String,
    val artifact: String,
    val version: String,
    val licenseName: String,
    val licenseText: String,
    val websiteUrl: String,
    val description: String
)

/**
 * Information and constants relating to the application's open-source distribution.
 */
object AppOpenSourceInfo {
    const val APP_NAME = "Wallpaper Picker"
    const val APP_CHINESE_NAME = "壁纸随心换"
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1
    const val LICENSE_NAME = "GPL-3.0-or-later"
    const val GITHUB_URL = "https://github.com/foobarz076/Wallpaper-Picker-Android"

    val GPL_3_LICENSE_TEXT = """
GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

Preamble

The GNU General Public License is a free, copyleft license for software and other kinds of works.

The licenses for most software and other practical works are designed to take away your freedom to share and change the works. By contrast, the GNU General Public License is intended to guarantee your freedom to share and change all versions of a program--to make sure it remains free software for all its users. We, the Free Software Foundation, use the GNU General Public License for most of our software; it applies also to any other work released this way by its authors. You can apply it to your programs, too.

When we speak of free software, we are referring to freedom, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software (and charge for them if you wish), that you receive source code or can get it if you want it, that you can change the software or use pieces of it in new free programs, and that you know you can do these things.

To protect your rights, we need to prevent others from denying you these rights or asking you to surrender the rights. Therefore, you have certain responsibilities if you distribute copies of the software, or if you modify it: responsibilities to respect the freedom of others.

TERMS AND CONDITIONS

0. Definitions.
"This License" refers to version 3 of the GNU General Public License.
"The Program" refers to any copyrightable work licensed under this License. Each licensee is addressed as "you".

1. Source Code.
The "source code" for a work means the preferred form of the work for making modifications to it. "Object code" means any non-source form of a work.

2. Basic Permissions.
All rights granted under this License are granted for the term of copyright on the Program, and are irrevocable provided the stated conditions are met. This License explicitly affirms your unlimited permission to run the unmodified Program.

3. Protecting Users' Legal Rights From Anti-Circumvention Law.
No covered work shall be deemed part of an effective technological measure under any applicable law fulfilling obligations under article 11 of the WIPO copyright treaty.

4. Conveying Verbatim Copies.
You may convey verbatim copies of the Program's source code as you receive it, in any medium, provided that you conspicuously and appropriately publish on each copy an appropriate copyright notice; keep intact all notices stating that this License and any non-permissive terms added in accord with section 7 apply to the code; keep intact all notices of the absence of any warranty; and give all recipients a copy of this License along with the Program.

5. Conveying Modified Source Versions.
You may convey a work based on the Program, or the modifications to produce it from the Program, in the form of source code under the terms of section 4, provided that you also meet all of these conditions:
  a) The work must carry prominent notices stating that you modified it, and giving a relevant date.
  b) The work must carry prominent notices stating that it is released under this License and any conditions added under section 7.
  c) You must license the entire work, as a whole, under this License to anyone who comes into possession of a copy.

6. Conveying Non-Source Forms.
You may convey a covered work in object code form under the terms of sections 4 and 5, provided that you also convey the machine-readable Corresponding Source under the terms of this License.

7. Disclaimer of Warranty.
THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED.
""".trimIndent()

    val APACHE_2_LICENSE_TEXT = """
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.
"License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.
"Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.
"Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity.

2. Grant of Copyright License.
Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License.
Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this section) patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work.

4. Redistribution.
You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions:
(a) You must give any other recipients of the Work or Derivative Works a copy of this License; and
(b) You must cause any modified files to carry prominent notices stating that You changed the files; and
(c) You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work; and
(d) If the Work includes a "NOTICE" text file as part of its distribution, then any Derivative Works that You distribute must include a readable copy of the attribution notices contained within such NOTICE file.

7. Disclaimer of Warranty.
Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

8. Limitation of Liability.
In no event and under no legal theory, whether in tort (including negligence), contract, or otherwise, shall any Contributor be liable to You for damages.
""".trimIndent()

    val THIRD_PARTY_LIBRARIES = listOf(
        OpenSourceLibrary(
            name = "Jetpack Compose & Material 3",
            artifact = "androidx.compose.ui / material3",
            version = "2024.10.01 (BOM)",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://developer.android.com/jetpack/compose",
            description = "Android 官方现代声明式 UI 工具包与 Material Design 3 组件库，支持流畅过渡与动态取色。"
        ),
        OpenSourceLibrary(
            name = "Jetpack WorkManager",
            artifact = "androidx.work:work-runtime-ktx",
            version = "2.10.0",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://developer.android.com/topic/libraries/architecture/workmanager",
            description = "Android 官方可靠后台调度引擎，自动无缝适配 AlarmManager 与 JobScheduler，支持电量与网络约束。"
        ),
        OpenSourceLibrary(
            name = "Coil Compose",
            artifact = "io.coil-kt:coil-compose",
            version = "2.7.0",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://coil-kt.github.io/coil/",
            description = "基于 Kotlin 协程的轻量级快速图片加载与采样缓存管线。"
        ),
        OpenSourceLibrary(
            name = "OkHttp",
            artifact = "com.squareup.okhttp3:okhttp",
            version = "4.12.0",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://square.github.io/okhttp/",
            description = "高效可靠的 HTTP & HTTP/2 客户端，支持流式下载与连接池复用。"
        ),
        OpenSourceLibrary(
            name = "Conscrypt Android",
            artifact = "org.conscrypt:conscrypt-android",
            version = "2.5.2",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://github.com/google/conscrypt",
            description = "Google 基于 BoringSSL 构建的 Java 安全提供者，为 Android 6.0 等老旧系统注入 TLS 1.3 及现代 CA 根证书支持。"
        ),
        OpenSourceLibrary(
            name = "AndroidX DocumentFile",
            artifact = "androidx.documentfile:documentfile",
            version = "1.0.1",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://developer.android.com/jetpack/androidx/releases/documentfile",
            description = "Storage Access Framework (SAF) 文档树持久化授权与目录安全访问辅助库。"
        ),
        OpenSourceLibrary(
            name = "AndroidX Core & Lifecycle",
            artifact = "androidx.core:core-ktx / lifecycle",
            version = "1.15.0 / 2.8.7",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://developer.android.com/jetpack/androidx",
            description = "AndroidX 核心 Kotlin 扩展与生命周期感知架构组件。"
        ),
        OpenSourceLibrary(
            name = "Kotlin Standard Library & Coroutines",
            artifact = "org.jetbrains.kotlin / kotlinx-coroutines",
            version = "2.0.21",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://kotlinlang.org/",
            description = "Kotlin 标准库与异步协程并发调度基础设施。"
        ),
        OpenSourceLibrary(
            name = "Desugar JDK Libs",
            artifact = "com.android.tools:desugar_jdk_libs",
            version = "2.1.3",
            licenseName = "Apache-2.0",
            licenseText = APACHE_2_LICENSE_TEXT,
            websiteUrl = "https://github.com/google/desugar_jdk_libs",
            description = "D8/R8 Java 语言高级特性与新版 API 在老旧 Android 版本上的脱糖兼容支持。"
        )
    )
}

/**
 * Activity presenting application information, GPL-3.0 licensing details,
 * runtime diagnostics, and third-party open-source licenses.
 */
class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialOpenLogs = intent?.getBooleanExtra(EXTRA_OPEN_LOGS, false) ?: false

        setContent {
            WallpaperPickerTheme {
                AboutScreen(
                    onBack = { finish() },
                    initialOpenLogs = initialOpenLogs
                )
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_LOGS = "extra_open_logs"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    initialOpenLogs: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeLicenseDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeLicenseDialogText by remember { mutableStateOf<String?>(null) }
    var showLogDialog by remember { mutableStateOf(initialOpenLogs) }
    var sanitizeLogs by remember { mutableStateOf(true) }
    var logCount by remember { mutableStateOf(AppLog.getLogCount()) }
    val prefs = remember { PreferencesManager(context) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. App Header & Branding Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = AppOpenSourceInfo.APP_NAME,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = AppOpenSourceInfo.APP_CHINESE_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("v${AppOpenSourceInfo.VERSION_NAME} (${AppOpenSourceInfo.VERSION_CODE})") }
                        )
                        AssistChip(
                            onClick = {
                                activeLicenseDialogTitle = "GNU General Public License v3.0"
                                activeLicenseDialogText = AppOpenSourceInfo.GPL_3_LICENSE_TEXT
                            },
                            label = { Text(AppOpenSourceInfo.LICENSE_NAME) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Policy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.about_screen_app_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openBrowserUrl(context, AppOpenSourceInfo.GITHUB_URL) { errorMsg ->
                                    scope.launch { snackbarHostState.showSnackbar(errorMsg) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.about_btn_repo))
                        }

                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, AppOpenSourceInfo.GITHUB_URL)
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.about_copied_repo_snackbar))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.about_btn_copy_link))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            activeLicenseDialogTitle = "GNU General Public License v3.0"
                            activeLicenseDialogText = AppOpenSourceInfo.GPL_3_LICENSE_TEXT
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Policy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.about_btn_view_gpl))
                    }
                }
            }

            // 2. Runtime Environment Diagnostics Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.about_diag_title), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticItem(stringResource(R.string.about_diag_os_version), "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    DiagnosticItem(stringResource(R.string.about_diag_device_model), "${Build.MANUFACTURER} ${Build.MODEL}")
                    DiagnosticItem(stringResource(R.string.about_diag_abis), Build.SUPPORTED_ABIS.joinToString(", ").ifEmpty { context.getString(R.string.about_diag_abi_universal) })
                    DiagnosticItem(stringResource(R.string.about_diag_target_sdk), "API 36 (Android 16)")
                    DiagnosticItem(stringResource(R.string.about_diag_min_sdk), "API 23 (Android 6.0 Marshmallow)")
                }
            }

            // 3. Execution & Diagnostic Logs Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.about_logs_card_title), style = MaterialTheme.typography.titleMedium)
                        }
                        SuggestionChip(
                            onClick = {
                                logCount = AppLog.getLogCount()
                                showLogDialog = true
                            },
                            label = { Text(stringResource(R.string.about_logs_count_chip, logCount)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_logs_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticItem(stringResource(R.string.about_logs_last_status), prefs.lastExecutionStatus ?: stringResource(R.string.about_logs_status_not_run))
                    DiagnosticItem(stringResource(R.string.about_logs_last_error), prefs.lastErrorMessage ?: stringResource(R.string.about_logs_error_none))

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                logCount = AppLog.getLogCount()
                                showLogDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.about_logs_btn_view))
                        }

                        Button(
                            onClick = {
                                val report = DiagnosticReportHelper.buildReport(context, sanitize = true)
                                DiagnosticReportHelper.shareReport(context, report)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.about_logs_btn_export))
                        }
                    }
                }
            }

            // 4. Third-party Open Source Licenses Section Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.about_third_party_title), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.about_third_party_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Third-party Libraries List
            AppOpenSourceInfo.THIRD_PARTY_LIBRARIES.forEach { lib ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lib.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            SuggestionChip(
                                onClick = {
                                    activeLicenseDialogTitle = "${lib.name} (${lib.licenseName})"
                                    activeLicenseDialogText = lib.licenseText
                                },
                                label = { Text(lib.licenseName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${lib.artifact}:${lib.version}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lib.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    openBrowserUrl(context, lib.websiteUrl) { errorMsg ->
                                        scope.launch { snackbarHostState.showSnackbar(errorMsg) }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.about_lib_website))
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = {
                                    activeLicenseDialogTitle = "${lib.name} (${lib.licenseName})"
                                    activeLicenseDialogText = lib.licenseText
                                }
                            ) {
                                Text(stringResource(R.string.about_lib_view_license))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // License Dialog
    if (activeLicenseDialogTitle != null && activeLicenseDialogText != null) {
        Dialog(
            onDismissRequest = {
                activeLicenseDialogTitle = null
                activeLicenseDialogText = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxSize(0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = activeLicenseDialogTitle ?: stringResource(R.string.about_dialog_license_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = activeLicenseDialogText ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                activeLicenseDialogTitle = null
                                activeLicenseDialogText = null
                            }
                        ) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        val currentLogs = remember(sanitizeLogs, logCount, showLogDialog) {
            AppLog.formatLogs(sanitizeLogs)
        }

        Dialog(
            onDismissRequest = { showLogDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.about_log_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.about_log_dialog_sanitize),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = sanitizeLogs,
                                onCheckedChange = { sanitizeLogs = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                DiagnosticReportHelper.copyToClipboard(context, currentLogs)
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.about_log_copied_snackbar)) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_copy))
                        }

                        OutlinedButton(
                            onClick = {
                                DiagnosticReportHelper.shareReport(context, currentLogs)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_share))
                        }

                        OutlinedButton(
                            onClick = {
                                AppLog.clearLogs()
                                logCount = 0
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.about_log_cleared_snackbar)) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_clear))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!sanitizeLogs) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.about_log_warning_unsanitized),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            SelectionContainer {
                                Text(
                                    text = currentLogs,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLogDialog = false }) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("URL", text)
    clipboard?.setPrimaryClip(clip)
}

private fun openBrowserUrl(
    context: Context,
    url: String,
    onError: (String) -> Unit
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        onError(context.getString(R.string.about_browser_open_failed))
        copyToClipboard(context, url)
    }
}
