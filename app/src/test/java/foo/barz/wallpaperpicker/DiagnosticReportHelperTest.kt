package foo.barz.wallpaperpicker

import android.content.Context
import foo.barz.wallpaperpicker.core.util.DiagnosticReportHelper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests validating DiagnosticReportHelper and export diagnostics configuration integrity.
 */
class DiagnosticReportHelperTest {

    @Test
    fun testDiagnosticReportHelperMethodsSignature() {
        val helperClass = DiagnosticReportHelper::class.java
        val buildReportMethod = helperClass.getMethod("buildReport", Context::class.java, Boolean::class.java)
        assertNotNull(buildReportMethod, "buildReport(Context, Boolean) must exist")

        val copyMethod = helperClass.getMethod("copyToClipboard", Context::class.java, String::class.java)
        assertNotNull(copyMethod, "copyToClipboard(Context, String) must exist")

        val shareMethod = helperClass.getMethod("shareReport", Context::class.java, String::class.java)
        assertNotNull(shareMethod, "shareReport(Context, String) must exist")
    }

    @Test
    fun testStringsResourceForDiagnosticDialog() {
        val enFile = File("src/main/res/values/strings.xml")
        assertTrue(enFile.exists(), "values/strings.xml must exist")
        val enContent = enFile.readText()
        assertTrue(enContent.contains("about_log_dialog_full_report"), "Must define about_log_dialog_full_report")
        assertTrue(enContent.contains("about_log_copied_full_snackbar"), "Must define about_log_copied_full_snackbar")

        val zhFile = File("src/main/res/values-zh-rCN/strings.xml")
        assertTrue(zhFile.exists(), "values-zh-rCN/strings.xml must exist")
        val zhContent = zhFile.readText()
        assertTrue(zhContent.contains("about_log_dialog_full_report"), "Must define about_log_dialog_full_report in zh-rCN")
        assertTrue(zhContent.contains("about_log_copied_full_snackbar"), "Must define about_log_copied_full_snackbar in zh-rCN")
    }

    @Test
    fun testDiagnosticReportSectionsCoverage() {
        val helperSourceFile = File("src/main/java/foo/barz/wallpaperpicker/core/util/DiagnosticReportHelper.kt")
        assertTrue(helperSourceFile.exists(), "DiagnosticReportHelper.kt must exist")
        val content = helperSourceFile.readText()

        // 1. Environment & Platform Information
        assertTrue(content.contains("--- [1] 运行环境与设备信息 (Environment) ---"))
        assertTrue(content.contains("精确闹钟权限 (Exact Alarm)"))
        assertTrue(content.contains("系统通知权限 (Notification)"))
        assertTrue(content.contains("电池优化白名单 (Doze豁免)"))

        // 2. Automation & Scheduling Configuration
        assertTrue(content.contains("--- [2] 调度配置与状态 (Automation & Scheduling) ---"))
        assertTrue(content.contains("自动轮播总开关"))
        assertTrue(content.contains("周期调度 (WorkManager)"))
        assertTrue(content.contains("精细定时器 (AlarmManager)"))
        assertTrue(content.contains("网络下载约束"))
        assertTrue(content.contains("独立日程规则引擎"))
        assertTrue(content.contains("夜间免打扰时段"))
        assertTrue(content.contains("防密集冷却抑制"))

        // 3. Current State
        assertTrue(content.contains("--- [3] 当前壁纸与执行状态 (Current State) ---"))

        // 4. Execution Logs
        assertTrue(content.contains("--- [4] 运行与诊断日志 (Recent Execution Logs) ---"))
    }
}
