package foo.barz.wallpaperpicker.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import foo.barz.wallpaperpicker.BuildConfig
import foo.barz.wallpaperpicker.core.worker.CompositeTriggerHelper
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper utility to build, format, and share comprehensive diagnostic reports
 * containing device specifications, automation configurations, and sanitized execution logs.
 */
object DiagnosticReportHelper {

    /**
     * Builds a structured text diagnostic report.
     *
     * @param context Application context.
     * @param sanitize Whether to redact sensitive credentials, API keys, and private IPs (default true).
     */
    fun buildReport(context: Context, sanitize: Boolean = true): String {
        val prefs = PreferencesManager(context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryExempt = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        val lastChangedStr = if (prefs.lastChangedTimestamp > 0) {
            dateFormat.format(Date(prefs.lastChangedTimestamp))
        } else {
            "从未更换 (Never)"
        }

        val quietHoursStr = if (prefs.quietHoursEnabled) {
            val start = CompositeTriggerHelper.formatTime(prefs.quietHoursStartHour, prefs.quietHoursStartMinute)
            val end = CompositeTriggerHelper.formatTime(prefs.quietHoursEndHour, prefs.quietHoursEndMinute)
            "已开启 ($start ~ $end)"
        } else {
            "已关闭 (Disabled)"
        }

        val anchorTimesStr = if (prefs.dailyAnchorEnabled) {
            prefs.dailyAnchorTimes.joinToString(", ")
        } else {
            "已关闭 (Disabled)"
        }

        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine("Wallpaper Picker 诊断报告 (Diagnostic Report)")
        sb.appendLine("生成时间: $generatedAt")
        sb.appendLine("脱敏保护: ${if (sanitize) "已启用 (Redacted)" else "未启用 (Raw)"}")
        sb.appendLine("========================================")
        sb.appendLine()

        sb.appendLine("--- [1] 运行环境与设备信息 (Environment) ---")
        sb.appendLine("应用版本: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) [${BuildConfig.BUILD_TYPE}]")
        sb.appendLine("设备型号: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        sb.appendLine("系统版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("支持架构: ${Build.SUPPORTED_ABIS.joinToString(", ").ifEmpty { "通用" }}")
        sb.appendLine("电池优化白名单 (Doze豁免): ${if (isBatteryExempt) "已加入白名单 (Ignored)" else "未加入白名单 (Restricted)"}")
        sb.appendLine()

        sb.appendLine("--- [2] 调度配置与状态 (Automation & Scheduling) ---")
        sb.appendLine("自动轮播总开关: ${if (prefs.isScheduled) "已开启" else "已关闭"}")
        sb.appendLine("周期调度 (WorkManager): ${if (prefs.intervalScheduleEnabled) "开启 (${prefs.intervalMinutes} 分钟)" else "已关闭"}")
        sb.appendLine("精细定时器 (AlarmManager): ${if (prefs.exactTimerEnabled) "已开启" else "已关闭"}")
        sb.appendLine("每日定点打卡: $anchorTimesStr")
        sb.appendLine("息屏自动触发: ${if (prefs.screenOffTriggerEnabled) "已开启 (${prefs.screenOffDelaySeconds}s 延迟)" else "已关闭"}")
        sb.appendLine("独立日程规则引擎: ${if (prefs.ruleEngineEnabled) "已开启" else "已关闭"}")
        sb.appendLine("夜间免打扰时段: $quietHoursStr")
        sb.appendLine("防密集冷却抑制: ${if (prefs.cooldownSuppressionEnabled) "开启 (${prefs.cooldownMinutes} 分钟)" else "已关闭"}")
        sb.appendLine("亮屏防打扰推迟: ${if (prefs.deferDuringInteraction) "已开启" else "已关闭"}")
        sb.appendLine("去重防连抽 (Fair Shuffle): ${if (prefs.fairShuffle) "开启 (容量 ${prefs.fairShuffleCapacity})" else "已关闭"}")
        sb.appendLine("缓存容量档位: ${prefs.cacheSizeTier.name}")
        sb.appendLine()

        sb.appendLine("--- [3] 当前壁纸与执行状态 (Current State) ---")
        sb.appendLine("激活图源类型: ${prefs.sourceType.name}")
        sb.appendLine("壁纸应用目标: ${prefs.target.name}")
        sb.appendLine("裁切对齐策略: ${prefs.cropMode.name}")
        sb.appendLine("桌面视差滚动: ${prefs.scrollMode.name}")
        sb.appendLine("上次更换时间: $lastChangedStr")
        sb.appendLine("上次壁纸标题: ${prefs.lastWallpaperTitle ?: "无"}")
        sb.appendLine("上次执行状态: ${prefs.lastExecutionStatus ?: "无"}")
        sb.appendLine("上次异常报错: ${prefs.lastErrorMessage ?: "无"}")
        sb.appendLine()

        sb.appendLine("--- [4] 运行与诊断日志 (Recent Execution Logs) ---")
        val logs = AppLog.formatLogs(sanitize)
        sb.appendLine(logs)
        sb.appendLine("========================================")
        sb.appendLine("报告结束 (End of Diagnostic Report)")
        sb.appendLine("========================================")

        return sb.toString()
    }

    /**
     * Copies the diagnostic report text to system clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("WallpaperPicker-Diagnostic-Report", text)
        clipboard?.setPrimaryClip(clip)
    }

    /**
     * Shares the diagnostic report using Android's system share sheet.
     */
    fun shareReport(context: Context, report: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Wallpaper Picker 运行诊断报告")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "导出诊断报告")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
