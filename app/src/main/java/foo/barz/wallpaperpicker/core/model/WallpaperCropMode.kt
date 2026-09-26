package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Strategy for scaling and cropping wallpapers to fit device screen dimensions.
 */
enum class WallpaperCropMode(
    val label: String,
    @get:StringRes val labelRes: Int,
    val description: String,
    @get:StringRes val descriptionRes: Int
) {
    FIT_HEIGHT(
        "高度优先 (防切头)",
        R.string.crop_fit_height_label,
        "以屏幕高度为准铺满，100% 完整保留画面纵向内容，保护人像头部",
        R.string.crop_fit_height_desc
    ),
    CENTER_CROP(
        "居中充满",
        R.string.crop_center_crop_label,
        "等比缩放至完全填满屏幕，四周不留黑边，适合风景图",
        R.string.crop_center_crop_desc
    ),
    FIT_CENTER(
        "原图完整",
        R.string.crop_fit_center_label,
        "等比缩放至画面完整可见，不足区域保持黑底，保留画面全貌",
        R.string.crop_fit_center_desc
    )
}
