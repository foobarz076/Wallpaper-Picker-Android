package foo.barz.wallpaperpicker.ui.tabs.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

/**
 * Common formatting and system intent utility helpers for settings sub-pages.
 */
object SettingsHelpers {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }

    fun checkBatteryOptimization(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the application details settings screen where the user can configure
     * app battery usage to "Unrestricted" / "No restrictions".
     */
    fun openAppBatteryDetailsSettings(context: Context) {
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetailsIntent)
        } catch (_: Exception) {
            openBatteryOptimizationList(context)
        }
    }

    /**
     * Opens the system battery optimization whitelist settings page.
     * Falls back to general system settings if unsupported on the current device.
     */
    fun openBatteryOptimizationList(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val generalSettingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(generalSettingsIntent)
            } catch (_: Exception) {
                // Ignore if device does not support settings activity
            }
        }
    }

    /**
     * Resolves the device manufacturer-specific guide on DontKillMyApp.
     * Falls back to the root website if the manufacturer is not specifically categorized.
     */
    fun getDontKillMyAppUrl(): String {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ->
                "https://dontkillmyapp.com/xiaomi"
            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                "https://dontkillmyapp.com/huawei"
            manufacturer.contains("samsung") ->
                "https://dontkillmyapp.com/samsung"
            manufacturer.contains("oneplus") ->
                "https://dontkillmyapp.com/oneplus"
            manufacturer.contains("oppo") || manufacturer.contains("realme") ->
                "https://dontkillmyapp.com/oppo"
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") ->
                "https://dontkillmyapp.com/vivo"
            manufacturer.contains("meizu") ->
                "https://dontkillmyapp.com/meizu"
            manufacturer.contains("sony") ->
                "https://dontkillmyapp.com/sony"
            manufacturer.contains("asus") ->
                "https://dontkillmyapp.com/asus"
            manufacturer.contains("nokia") ->
                "https://dontkillmyapp.com/nokia"
            manufacturer.contains("lenovo") || manufacturer.contains("motorola") ->
                "https://dontkillmyapp.com/motorola"
            else ->
                "https://dontkillmyapp.com/"
        }
    }

    /**
     * Opens DontKillMyApp website targeting the current device manufacturer in the user's browser.
     */
    fun openDontKillMyApp(context: Context) {
        try {
            val url = getDontKillMyAppUrl()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore if no suitable browser application is available
        }
    }
}
