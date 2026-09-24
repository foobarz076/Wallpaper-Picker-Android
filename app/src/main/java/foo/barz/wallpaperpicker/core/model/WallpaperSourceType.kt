package foo.barz.wallpaperpicker.core.model

/**
 * Supported wallpaper source types.
 */
enum class WallpaperSourceType(val displayName: String) {
    LOCAL_FOLDER("本地文件夹"),
    HTTP_API("网络图源 (HTTP)")
}
