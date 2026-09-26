package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Scale and display mode for the current wallpaper thumbnail on home screen widgets.
 */
enum class WidgetScaleType(
    val displayName: String,
    @get:StringRes val displayNameRes: Int,
    val description: String,
    @get:StringRes val descriptionRes: Int
) {
    CROP(
        "铺满裁切",
        R.string.widget_scale_crop_label,
        "按微件大小居中铺满，无留白，视觉感饱满",
        R.string.widget_scale_crop_desc
    ),
    FIT(
        "完整展示",
        R.string.widget_scale_fit_label,
        "保持原图长宽比完整显示，两边根据比例自适应留空",
        R.string.widget_scale_fit_desc
    )
}
