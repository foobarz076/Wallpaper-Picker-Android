package foo.barz.wallpaperpicker.core.processor

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Image processor that handles downsampling, aspect ratio scaling, anti-OOM decoding,
 * and adaptive parallax scrolling width calculation.
 */
class WallpaperProcessor(private val context: Context) {

    /**
     * Decodes and scales an image stream according to target screen dimensions,
     * scroll mode, and visual cropping preference.
     */
    suspend fun process(
        openStream: () -> InputStream,
        scrollMode: WallpaperScrollMode = WallpaperScrollMode.AUTO,
        cropMode: WallpaperCropMode = WallpaperCropMode.FIT_HEIGHT,
        cropFocusX: Float = 0.5f,
        cropFocusY: Float = 0.5f,
        flipHorizontal: Boolean = false
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

            // Step 2: Determine target dimensions based on scrollMode, cropMode & image aspect ratio
            val (targetWidth, targetHeight) = getTargetDimensions(originalWidth, originalHeight, scrollMode, cropMode)

            // Step 3: Calculate inSampleSize (power of 2)
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // Step 4: Decode downsampled bitmap
            val downsampled = openStream().use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: throw IllegalStateException("解码图片失败")

            // Step 5: Render scaled and cropped bitmap onto canvas
            renderScaledBitmap(downsampled, targetWidth, targetHeight, cropMode, cropFocusX, cropFocusY, flipHorizontal)
        }
    }

    private fun getTargetDimensions(
        originalWidth: Int,
        originalHeight: Int,
        scrollMode: WallpaperScrollMode,
        cropMode: WallpaperCropMode
    ): Pair<Int, Int> {
        val (screenWidth, screenHeight) = getScreenDimensions()
        val wm = WallpaperManager.getInstance(context)
        val desiredWidth = wm.desiredMinimumWidth
        val maxParallaxWidth = if (desiredWidth > screenWidth) desiredWidth else screenWidth * 2

        val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val imageRatio = originalWidth.toFloat() / originalHeight.toFloat()

        return when (scrollMode) {
            // Never scroll: strictly bounded to single screen dimensions (ideal for non-scrolling ROMs)
            WallpaperScrollMode.NEVER -> {
                Pair(screenWidth, screenHeight)
            }

            // Auto adaptive: wide images (imageRatio > screenRatio) scroll naturally;
            // portrait images (imageRatio <= screenRatio) stay single-screen to avoid 2x magnification and head cropping.
            WallpaperScrollMode.AUTO -> {
                if (imageRatio > screenRatio) {
                    val naturalWidth = (screenHeight * imageRatio).toInt()
                    val targetWidth = min(maxParallaxWidth, max(screenWidth, naturalWidth))
                    Pair(targetWidth, screenHeight)
                } else {
                    Pair(screenWidth, screenHeight)
                }
            }

            // Always parallax: wide canvas for launchers supporting page panning.
            // For FIT_HEIGHT, we avoid zooming into height by using natural width if narrower than maxParallaxWidth.
            WallpaperScrollMode.ALWAYS -> {
                if (cropMode == WallpaperCropMode.FIT_HEIGHT && imageRatio < screenRatio) {
                    Pair(screenWidth, screenHeight)
                } else {
                    Pair(maxParallaxWidth, screenHeight)
                }
            }
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

    private fun renderScaledBitmap(
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        cropMode: WallpaperCropMode,
        cropFocusX: Float = 0.5f,
        cropFocusY: Float = 0.5f,
        flipHorizontal: Boolean = false
    ): Bitmap {
        // Step 5.1: Apply horizontal flip if enabled
        val preparedSrc = if (flipHorizontal) {
            val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
            val flipped = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            if (src != flipped && !src.isRecycled) {
                src.recycle()
            }
            flipped
        } else {
            src
        }

        if (preparedSrc.width == targetWidth && preparedSrc.height == targetHeight && cropFocusX == 0.5f && cropFocusY == 0.5f) {
            return preparedSrc
        }

        val scale = when (cropMode) {
            // Fit height: scale matches target height exactly, preserving 100% vertical content (no cutting heads/feet)
            WallpaperCropMode.FIT_HEIGHT -> {
                targetHeight.toFloat() / preparedSrc.height.toFloat()
            }
            // Center crop: fill entire target rectangle without black bars
            WallpaperCropMode.CENTER_CROP -> {
                max(
                    targetWidth.toFloat() / preparedSrc.width.toFloat(),
                    targetHeight.toFloat() / preparedSrc.height.toFloat()
                )
            }
            // Fit center: full image uncropped, letterboxed with black background
            WallpaperCropMode.FIT_CENTER -> {
                min(
                    targetWidth.toFloat() / preparedSrc.width.toFloat(),
                    targetHeight.toFloat() / preparedSrc.height.toFloat()
                )
            }
        }

        val scaledWidth = (preparedSrc.width * scale).toInt()
        val scaledHeight = (preparedSrc.height * scale).toInt()

        // Calculate crop offset using normalized cropFocusX and cropFocusY (0.0 to 1.0)
        val left = if (scaledWidth > targetWidth) {
            val excessWidth = scaledWidth - targetWidth
            -(excessWidth * cropFocusX.coerceIn(0f, 1f)).toInt()
        } else {
            (targetWidth - scaledWidth) / 2
        }

        val top = if (scaledHeight > targetHeight) {
            val excessHeight = scaledHeight - targetHeight
            -(excessHeight * cropFocusY.coerceIn(0f, 1f)).toInt()
        } else {
            (targetHeight - scaledHeight) / 2
        }

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Fill background with black for letterboxed margins if any
        if (cropMode == WallpaperCropMode.FIT_CENTER || scaledWidth < targetWidth || scaledHeight < targetHeight) {
            canvas.drawColor(Color.BLACK)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
        val srcRect = Rect(0, 0, preparedSrc.width, preparedSrc.height)

        canvas.drawBitmap(preparedSrc, srcRect, destRect, paint)

        if (preparedSrc != output && !preparedSrc.isRecycled) {
            preparedSrc.recycle()
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
