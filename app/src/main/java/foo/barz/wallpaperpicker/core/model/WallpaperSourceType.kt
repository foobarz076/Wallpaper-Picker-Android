package foo.barz.wallpaperpicker.core.model

/**
 * Supported wallpaper source types.
 */
enum class WallpaperSourceType(val displayName: String) {
    LOCAL_FOLDER("本地文件夹"),
    MEDIA_STORE("系统相册"),
    IMMICH("自建相册 (Immich)"),
    HTTP_API("通用网络图源 (HTTP)")
}
