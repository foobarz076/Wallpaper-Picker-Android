package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Strategy for wallpaper horizontal scrolling on the home launcher.
 */
enum class WallpaperScrollMode(
    val label: String,
    @get:StringRes val labelRes: Int,
    val description: String,
    @get:StringRes val descriptionRes: Int
) {
    AUTO(
        "智能自适应",
        R.string.scroll_auto_label,
        "横图/宽图自动随桌面平移；竖图锁定单屏，保持清晰不拉伸",
        R.string.scroll_auto_desc
    ),
    NEVER(
        "锁定居中",
        R.string.scroll_never_label,
        "严格单屏显示不滚动，推荐不支持壁纸平移的定制 ROM 或桌面",
        R.string.scroll_never_desc
    ),
    ALWAYS(
        "强制视差",
        R.string.scroll_always_label,
        "生成超宽幅壁纸，适合原生 Pixel/AOSP 等多屏平移桌面",
        R.string.scroll_always_desc
    )
}
