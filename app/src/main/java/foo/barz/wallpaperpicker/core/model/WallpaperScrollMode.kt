package foo.barz.wallpaperpicker.core.model

/**
 * Strategy for wallpaper horizontal scrolling on the home launcher.
 */
enum class WallpaperScrollMode(val label: String, val description: String) {
    AUTO("自动探测", "横屏/宽图随桌面滚动，竖图保持单屏居中防裁切"),
    ALWAYS("始终滚动", "所有壁纸均按多屏宽度裁切，强制随桌面翻页"),
    NEVER("固定单屏", "所有壁纸严格适配单屏大小，不随桌面滚动")
}
