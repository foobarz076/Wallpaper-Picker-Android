package foo.barz.wallpaperpicker.core.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persistent entity representing a configured wallpaper source instance.
 * Supports multiple instances of the same source type with independent configurations.
 */
data class WallpaperSourceEntity(
    val id: String = UUID.randomUUID().toString(),
    val type: WallpaperSourceType,
    val title: String,
    val isEnabled: Boolean = true,
    val configJson: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Configuration payload for a Local Folder wallpaper source.
 */
data class LocalFolderSourceConfig(
    val folderUri: String,
    val folderName: String,
    val imageCount: Int = 0
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("folderUri", folderUri)
            put("folderName", folderName)
            put("imageCount", imageCount)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): LocalFolderSourceConfig {
            return runCatching {
                val json = JSONObject(jsonStr)
                LocalFolderSourceConfig(
                    folderUri = json.optString("folderUri", ""),
                    folderName = json.optString("folderName", "未命名文件夹"),
                    imageCount = json.optInt("imageCount", 0)
                )
            }.getOrDefault(LocalFolderSourceConfig("", ""))
        }
    }
}

/**
 * Configuration payload for a MediaStore system gallery wallpaper source.
 */
data class MediaStoreSourceConfig(
    val albumIds: Set<String> = emptySet(),
    val albumNames: String = "全部照片 (全库随机)"
) {
    fun toJson(): String {
        return JSONObject().apply {
            val array = JSONArray()
            albumIds.forEach { array.put(it) }
            put("albumIds", array)
            put("albumNames", albumNames)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): MediaStoreSourceConfig {
            return runCatching {
                val json = JSONObject(jsonStr)
                val albumIds = mutableSetOf<String>()
                val array = json.optJSONArray("albumIds")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        albumIds.add(array.getString(i))
                    }
                }
                MediaStoreSourceConfig(
                    albumIds = albumIds,
                    albumNames = json.optString("albumNames", "全部照片 (全库随机)")
                )
            }.getOrDefault(MediaStoreSourceConfig())
        }
    }
}

/**
 * Configuration payload for an Immich self-hosted wallpaper source.
 */
data class ImmichSourceConfig(
    val serverUrl: String,
    val apiKey: String,
    val albumIds: Set<String> = emptySet(),
    val albumNames: String = "全部相册 (全库随机)",
    val quality: ImmichQuality = ImmichQuality.PREVIEW,
    val ignoreSsl: Boolean = false,
    val wifiOnly: Boolean = true
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("serverUrl", serverUrl)
            put("apiKey", apiKey)
            val array = JSONArray()
            albumIds.forEach { array.put(it) }
            put("albumIds", array)
            put("albumNames", albumNames)
            put("quality", quality.name)
            put("ignoreSsl", ignoreSsl)
            put("wifiOnly", wifiOnly)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): ImmichSourceConfig {
            return runCatching {
                val json = JSONObject(jsonStr)
                val albumIds = mutableSetOf<String>()
                val array = json.optJSONArray("albumIds")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        albumIds.add(array.getString(i))
                    }
                }
                val qualityName = json.optString("quality", ImmichQuality.PREVIEW.name)
                val quality = runCatching { ImmichQuality.valueOf(qualityName) }.getOrDefault(ImmichQuality.PREVIEW)
                ImmichSourceConfig(
                    serverUrl = json.optString("serverUrl", ""),
                    apiKey = json.optString("apiKey", ""),
                    albumIds = albumIds,
                    albumNames = json.optString("albumNames", "全部相册 (全库随机)"),
                    quality = quality,
                    ignoreSsl = json.optBoolean("ignoreSsl", false),
                    wifiOnly = json.optBoolean("wifiOnly", true)
                )
            }.getOrDefault(ImmichSourceConfig("", ""))
        }
    }
}

/**
 * Configuration payload for a generic HTTP API wallpaper source.
 */
data class HttpApiSourceConfig(
    val preset: HttpPresetType = HttpPresetType.BING,
    val customUrl: String = "",
    val customJsonPath: String = "",
    val wifiOnly: Boolean = true
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("preset", preset.name)
            put("customUrl", customUrl)
            put("customJsonPath", customJsonPath)
            put("wifiOnly", wifiOnly)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): HttpApiSourceConfig {
            return runCatching {
                val json = JSONObject(jsonStr)
                val presetName = json.optString("preset", HttpPresetType.BING.name)
                val preset = runCatching { HttpPresetType.valueOf(presetName) }.getOrDefault(HttpPresetType.BING)
                HttpApiSourceConfig(
                    preset = preset,
                    customUrl = json.optString("customUrl", ""),
                    customJsonPath = json.optString("customJsonPath", ""),
                    wifiOnly = json.optBoolean("wifiOnly", true)
                )
            }.getOrDefault(HttpApiSourceConfig())
        }
    }
}

/**
 * Configuration payload for a Favorites wallpaper source.
 */
data class FavoritesSourceConfig(
    val note: String = ""
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("note", note)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): FavoritesSourceConfig {
            return runCatching {
                val json = JSONObject(jsonStr)
                FavoritesSourceConfig(
                    note = json.optString("note", "")
                )
            }.getOrDefault(FavoritesSourceConfig())
        }
    }
}
