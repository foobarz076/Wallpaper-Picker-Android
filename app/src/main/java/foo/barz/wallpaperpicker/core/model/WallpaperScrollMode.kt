package foo.barz.wallpaperpicker.core.model

/**
 * Strategy for wallpaper horizontal scrolling on the home launcher.
 */
enum class WallpaperScrollMode(val label: String, val description: String) {
    AUTO("智能自适应", "横图/宽图自动随桌面平移；竖图锁定单屏，保持清晰不拉伸"),
    NEVER("锁定居中", "严格单屏显示不滚动，推荐不支持壁纸平移的定制 ROM 或桌面"),
    ALWAYS("强制视差", "生成超宽幅壁纸，适合原生 Pixel/AOSP 等多屏平移桌面")
}
