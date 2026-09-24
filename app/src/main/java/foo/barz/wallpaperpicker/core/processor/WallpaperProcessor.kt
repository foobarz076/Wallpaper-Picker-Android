package foo.barz.wallpaperpicker.core.processor

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

/**
 * Image processor that handles downsampling, aspect ratio scaling, anti-OOM decoding,
 * and adaptive parallax scrolling width calculation.
 */
class WallpaperProcessor(private val context: Context) {

    /**
     * Decodes and scales an image stream according to target screen dimensions and scroll mode.
     */
    suspend fun process(
        openStream: () -> InputStream,
        scrollMode: WallpaperScrollMode = WallpaperScrollMode.AUTO
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: Decode image bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openStream().use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                throw IllegalArgumentException("无法识别图片尺寸或图片数据已损坏")
            }

            // Step 2: Determine target dimensions based on scrollMode & image aspect ratio
            val (targetWidth, targetHeight) = getTargetDimensions(originalWidth, originalHeight, scrollMode)

            // Step 3: Calculate inSampleSize (power of 2)
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // Step 4: Decode downsampled bitmap
            val downsampled = openStream().use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: throw IllegalStateException("解码图片失败")

            // Step 5: Center-crop to target dimensions
            centerCrop(downsampled, targetWidth, targetHeight)
        }
    }

    private fun getTargetDimensions(
        originalWidth: Int,
        originalHeight: Int,
        scrollMode: WallpaperScrollMode
    ): Pair<Int, Int> {
        val (screenWidth, screenHeight) = getScreenDimensions()
        val wm = WallpaperManager.getInstance(context)
        val desiredWidth = wm.desiredMinimumWidth
        val desiredHeight = wm.desiredMinimumHeight

        // Desired width for launcher parallax scrolling (typically 1.5x - 2x screen width)
        val scrollWidth = if (desiredWidth > screenWidth) desiredWidth else screenWidth * 2
        val scrollHeight = if (desiredHeight > 0) desiredHeight else screenHeight

        val shouldScroll = when (scrollMode) {
            WallpaperScrollMode.NEVER -> false
            WallpaperScrollMode.ALWAYS -> true
            // In AUTO mode: scroll if landscape/wide (width >= height); keep fixed single-screen if portrait
            WallpaperScrollMode.AUTO -> originalWidth >= originalHeight
        }

        return if (shouldScroll) {
            Pair(scrollWidth, scrollHeight)
        } else {
            Pair(screenWidth, screenHeight)
        }
    }

    private fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun centerCrop(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (src.width == targetWidth && src.height == targetHeight) {
            return src
        }

        val scale = max(
            targetWidth.toFloat() / src.width.toFloat(),
            targetHeight.toFloat() / src.height.toFloat()
        )

        val scaledWidth = (src.width * scale).toInt()
        val scaledHeight = (src.height * scale).toInt()

        val left = (targetWidth - scaledWidth) / 2
        val top = (targetHeight - scaledHeight) / 2

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
        val srcRect = Rect(0, 0, src.width, src.height)

        canvas.drawBitmap(src, srcRect, destRect, paint)

        if (src != output && !src.isRecycled) {
            src.recycle()
        }

        return output
    }

    @Suppress("DEPRECATION")
    private fun getScreenDimensions(): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val display = wm.defaultDisplay
            val point = android.graphics.Point()
            display.getRealSize(point)
            Pair(point.x, point.y)
        }
    }
}
