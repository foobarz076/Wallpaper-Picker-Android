package foo.barz.wallpaperpicker.core.model

/**
 * Download quality options for Immich assets.
 */
enum class ImmichQuality(val label: String) {
    PREVIEW("高清预览 (推荐，加载快且省流)"),
    ORIGINAL("原始全尺寸 (画质最高)")
}

/**
 * Representation of an Immich album.
 */
data class ImmichAlbum(
    val id: String,
    val name: String,
    val assetCount: Int = 0,
    val thumbnailAssetId: String? = null
)

/**
 * Configuration parameters for an Immich wallpaper source.
 */
data class ImmichConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val albumId: String? = null,
    val albumName: String? = null,
    val albumIds: Set<String> = emptySet(),
    val downloadQuality: ImmichQuality = ImmichQuality.PREVIEW,
    val ignoreSslErrors: Boolean = false,
    val wifiOnly: Boolean = true
)
