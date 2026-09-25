package foo.barz.wallpaperpicker.core.model

/**
 * Scale and display mode for the current wallpaper thumbnail on home screen widgets.
 */
enum class WidgetScaleType(val displayName: String, val description: String) {
    CROP("铺满裁切", "按微件大小居中铺满，无留白，视觉感饱满"),
    FIT("完整展示", "保持原图长宽比完整显示，两边根据比例自适应留空")
}
