package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Supported presets for HTTP API sources.
 */
enum class HttpPresetType(
    val label: String,
    @get:StringRes val labelRes: Int
) {
    BING("Bing 每日壁纸", R.string.http_preset_bing),
    PICSUM("Picsum 随机摄影", R.string.http_preset_picsum),
    CUSTOM("自定义 HTTP API", R.string.http_preset_custom)
}

/**
 * Configuration for HTTP API wallpaper sources.
 */
data class HttpApiConfig(
    val presetType: HttpPresetType = HttpPresetType.BING,
    val customUrl: String = "",
    val customJsonPath: String = "",
    val wifiOnly: Boolean = true
)
