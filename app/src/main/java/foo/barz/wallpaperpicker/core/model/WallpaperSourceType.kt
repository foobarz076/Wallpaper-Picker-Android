package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Supported wallpaper source types.
 */
enum class WallpaperSourceType(
    val displayName: String,
    @get:StringRes val displayNameRes: Int
) {
    LOCAL_FOLDER("本地文件夹", R.string.source_type_local_folder),
    MEDIA_STORE("系统相册", R.string.source_type_media_store),
    CUSTOM_PHOTOS("自选照片集", R.string.source_type_custom_photos),
    FAVORITES("我的收藏", R.string.source_type_favorites),
    IMMICH("自建相册 (Immich)", R.string.source_type_immich),
    HTTP_API("通用网络图源 (HTTP)", R.string.source_type_http_api),
    COMPOSITE("多源混合轮播", R.string.source_type_composite)
}
