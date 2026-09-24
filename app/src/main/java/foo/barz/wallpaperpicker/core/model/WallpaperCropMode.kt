package foo.barz.wallpaperpicker.core.model

/**
 * Strategy for scaling and cropping wallpapers to fit device screen dimensions.
 */
enum class WallpaperCropMode(val label: String, val description: String) {
    FIT_HEIGHT("高度优先 (防切头)", "以屏幕高度为准铺满，100% 完整保留画面纵向内容，保护人像头部"),
    CENTER_CROP("居中充满", "等比缩放至完全填满屏幕，四周不留黑边，适合风景图"),
    FIT_CENTER("原图完整", "等比缩放至画面完整可见，不足区域保持黑底，保留画面全貌")
}
