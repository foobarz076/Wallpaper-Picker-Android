package foo.barz.wallpaperpicker.core.model

/**
 * Supported presets for HTTP API sources.
 */
enum class HttpPresetType(val label: String) {
    BING("Bing 每日壁纸"),
    PICSUM("Picsum 随机摄影"),
    CUSTOM("自定义 HTTP API")
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
