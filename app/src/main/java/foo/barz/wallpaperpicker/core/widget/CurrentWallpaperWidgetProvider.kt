package foo.barz.wallpaperpicker.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import foo.barz.wallpaperpicker.MainActivity
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.core.model.WidgetScaleType
import foo.barz.wallpaperpicker.core.shortcut.ShortcutHelper
import foo.barz.wallpaperpicker.core.util.AppLog
import foo.barz.wallpaperpicker.data.PreferencesManager
import foo.barz.wallpaperpicker.ui.ShortcutActionActivity
import foo.barz.wallpaperpicker.ui.WallpaperLightboxActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AppWidgetProvider managing the current wallpaper thumbnail desktop widget.
 * Safely downsamples images to prevent Binder transaction limits while providing
 * quick view-in-gallery and one-touch next wallpaper triggers.
 */
class CurrentWallpaperWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AppLog.d(TAG) { "onUpdate called for widgetIds: ${appWidgetIds.joinToString()}" }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        AppLog.d(TAG) { "onAppWidgetOptionsChanged: widgetId=$appWidgetId" }
        updateWidgetsAsync(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        private const val TAG = "CurrentWallpaperWidget"
        private const val MAX_WIDGET_DIMENSION = 480

        /**
         * Dispatches an asynchronous update to all active desktop widgets.
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, CurrentWallpaperWidgetProvider::class.java)
            val appWidgetIds = runCatching { appWidgetManager.getAppWidgetIds(componentName) }.getOrNull()
            AppLog.d(TAG) { "updateAllWidgets called, found widgetIds: ${appWidgetIds?.joinToString()}" }
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateWidgetsAsync(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            AppLog.d(TAG) { "updateWidgetsAsync called for ids: ${appWidgetIds.joinToString()}" }
            val appContext = context.applicationContext
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val result = runCatching {
                    val prefs = PreferencesManager(appContext)
                    val uri = prefs.lastWallpaperUri
                    val title = prefs.lastWallpaperTitle
                    val sourceTitle = prefs.lastWallpaperSourceTitle
                    val timestamp = prefs.lastChangedTimestamp
                    val scaleType = prefs.widgetScaleType

                    AppLog.d(TAG) { "Widget update info - uri: $uri, title: $title, scaleType: $scaleType" }

                    val bitmap = if (uri != null) decodeSafeThumbnail(appContext, uri) else null
                    AppLog.d(TAG) { "Decoded bitmap: ${bitmap?.width}x${bitmap?.height}, byteCount: ${bitmap?.byteCount}" }

                    for (appWidgetId in appWidgetIds) {
                        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                        val views = RemoteViews(appContext.packageName, R.layout.widget_current_wallpaper)
                        renderWidgetViews(appContext, views, uri, title, sourceTitle, timestamp, scaleType, bitmap, options)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        AppLog.d(TAG) { "Updated appWidgetId: $appWidgetId" }
                    }
                }
                result.exceptionOrNull()?.let { e ->
                    AppLog.e(TAG, "Error updating widgets", e)
                }
            }
        }

        private fun renderWidgetViews(
            context: Context,
            views: RemoteViews,
            uri: Uri?,
            title: String?,
            sourceTitle: String?,
            timestamp: Long,
            scaleType: WidgetScaleType,
            bitmap: Bitmap?,
            options: Bundle? = null
        ) {
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0

            // Ultra-compact (e.g. 1x1 tile): Clean image tile without cluttered text
            val isUltraCompact = (minHeight in 1..65) || (minWidth in 1..80)

            // Narrow or short height (e.g. 2x1 ribbon or narrow column): Show title only, hide subtitle
            val isNarrowOrShort = (minHeight in 1..95) || (minWidth in 1..110)

            if (bitmap != null && uri != null) {
                // Configure scale mode (Crop vs Fit)
                if (scaleType == WidgetScaleType.CROP) {
                    views.setViewVisibility(R.id.widget_image_crop, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_image_fit, View.GONE)
                    views.setImageViewBitmap(R.id.widget_image_crop, bitmap)
                } else {
                    views.setViewVisibility(R.id.widget_image_crop, View.GONE)
                    views.setViewVisibility(R.id.widget_image_fit, View.VISIBLE)
                    views.setImageViewBitmap(R.id.widget_image_fit, bitmap)
                }

                views.setViewVisibility(R.id.widget_empty_text, View.GONE)

                if (isUltraCompact) {
                    views.setViewVisibility(R.id.widget_scrim, View.GONE)
                    views.setViewVisibility(R.id.widget_text_layout, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_scrim, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_text_layout, View.VISIBLE)

                    // Set wallpaper details
                    views.setTextViewText(R.id.widget_title, title?.ifBlank { null } ?: "当前壁纸")

                    if (isNarrowOrShort) {
                        views.setViewVisibility(R.id.widget_subtitle, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_subtitle, View.VISIBLE)
                        val formattedTime = if (timestamp > 0) formatWidgetTime(timestamp) else ""
                        val subtitle = when {
                            !sourceTitle.isNullOrBlank() && formattedTime.isNotBlank() -> "$sourceTitle · $formattedTime"
                            !sourceTitle.isNullOrBlank() -> sourceTitle
                            formattedTime.isNotBlank() -> formattedTime
                            else -> "正在使用"
                        }
                        views.setTextViewText(R.id.widget_subtitle, subtitle)
                    }
                }

                // Main card click: View full original in WallpaperLightboxViewer
                val viewIntent = Intent(context, WallpaperLightboxActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context,
                    100,
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, viewPendingIntent)
            } else {
                // Empty state when no wallpaper has been chosen yet
                views.setViewVisibility(R.id.widget_image_crop, View.GONE)
                views.setViewVisibility(R.id.widget_image_fit, View.GONE)
                views.setViewVisibility(R.id.widget_scrim, View.GONE)
                views.setViewVisibility(R.id.widget_text_layout, View.GONE)
                views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)

                views.setTextViewText(
                    R.id.widget_empty_text,
                    if (isUltraCompact) "点此应用" else "暂无当前壁纸\n点击打开应用"
                )

                // Main card click in empty state: open main app
                val openMainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openMainPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    openMainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, openMainPendingIntent)
            }

            // Quick Refresh Button: triggers next wallpaper immediately
            val nextIntent = Intent(context, ShortcutActionActivity::class.java).apply {
                action = ShortcutHelper.ACTION_NEXT_WALLPAPER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val nextPendingIntent = PendingIntent.getActivity(
                context,
                102,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, nextPendingIntent)
        }

        /**
         * Safely downsamples the wallpaper image to under MAX_WIDGET_DIMENSION to strictly prevent
         * RemoteViews Binder TransactionTooLargeException.
         */
        fun decodeSafeThumbnail(context: Context, uri: Uri): Bitmap? {
            return runCatching {
                AppLog.d(TAG) { "decodeSafeThumbnail start for uri: $uri, scheme=${uri.scheme}" }
                fun openStream(): InputStream? {
                    return if (uri.scheme == "file") {
                        val path = uri.path ?: return null
                        val file = File(path)
                        if (file.exists() && file.canRead()) file.inputStream() else null
                    } else {
                        try {
                            context.contentResolver.openInputStream(uri)
                        } catch (e: Exception) {
                            AppLog.e(TAG, "Failed to openInputStream for uri: $uri", e)
                            null
                        }
                    }
                }

                // 1. Measure dimensions
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val stream1 = openStream()
                if (stream1 == null) {
                    AppLog.e(TAG, "openStream returned null when reading bounds")
                    return null
                }
                stream1.use { BitmapFactory.decodeStream(it, null, options) }

                val origWidth = options.outWidth
                val origHeight = options.outHeight
                AppLog.d(TAG) { "Image bounds: ${origWidth}x${origHeight}" }
                if (origWidth <= 0 || origHeight <= 0) {
                    AppLog.e(TAG, "Invalid dimensions: ${origWidth}x${origHeight}")
                    return null
                }

                // 2. Compute sample size
                var inSampleSize = 1
                while ((origWidth / inSampleSize) > MAX_WIDGET_DIMENSION || (origHeight / inSampleSize) > MAX_WIDGET_DIMENSION) {
                    inSampleSize *= 2
                }
                AppLog.d(TAG) { "Computed inSampleSize: $inSampleSize" }

                // 3. Decode downscaled bitmap using RGB_565 for minimal memory footprint
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    this.inPreferredConfig = Bitmap.Config.RGB_565
                }
                val stream2 = openStream()
                if (stream2 == null) {
                    AppLog.e(TAG, "openStream returned null when decoding bitmap")
                    return null
                }
                val resultBitmap = stream2.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
                AppLog.d(TAG) { "Decoded resultBitmap: $resultBitmap" }
                resultBitmap
            }.getOrElse { e ->
                AppLog.e(TAG, "decodeSafeThumbnail exception", e)
                null
            }
        }

        private fun formatWidgetTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diffMs = now - timestamp
            val diffMins = diffMs / (60 * 1000)
            val diffHours = diffMins / 60

            return when {
                diffMins < 1 -> "刚刚"
                diffMins < 60 -> "${diffMins}分钟前"
                diffHours < 24 -> "${diffHours}小时前"
                else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
