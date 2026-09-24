package foo.barz.wallpaperpicker.core.source

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.model.HttpApiConfig
import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.core.network.HttpClientProvider
import foo.barz.wallpaperpicker.core.network.JsonPathExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.FileInputStream
import java.io.IOException
import java.net.URI

/**
 * Wallpaper source that fetches wallpapers from HTTP endpoints (Presets, Direct Images, or JSON APIs).
 * Integrates LRU caching and graceful offline / metered network fallback.
 */
class HttpApiSource(
    private val context: Context,
    private val config: HttpApiConfig,
    private val cacheManager: WallpaperCacheManager,
    private val bypassNetworkConstraints: Boolean = false
) : WallpaperSource {

    override val id: String = "http_api"

    override val displayName: String
        get() = when (config.presetType) {
            HttpPresetType.BING -> "Bing 每日壁纸"
            HttpPresetType.PICSUM -> "Picsum 随机摄影"
            HttpPresetType.CUSTOM -> "自定义 HTTP API"
        }

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        runCatching {
            // Check network availability and constraints (unless bypassed, e.g., manual trigger)
            val networkAllowed = bypassNetworkConstraints || isNetworkAvailableAndAllowed(context, config.wifiOnly)

            val prefs = foo.barz.wallpaperpicker.data.PreferencesManager(context)
            val excluded = if (prefs.fairShuffle) prefs.getRecentWallpaperKeys().toSet() else emptySet()

            if (!networkAllowed) {
                // Attempt fallback to local LRU cache pool
                val cached = cacheManager.getRandomCachedWallpaper(excluded)
                if (cached != null) {
                    return@runCatching cached
                }
                throw IllegalStateException("当前处于移动网络或离线状态，且本地缓存池为空")
            }

            try {
                fetchFromNetwork()
            } catch (e: Exception) {
                // Network error fallback to cached wallpaper
                val cached = cacheManager.getRandomCachedWallpaper(excluded)
                if (cached != null) {
                    return@runCatching cached
                }
                throw e
            }
        }
    }

    private fun fetchFromNetwork(): WallpaperData {
        val client = HttpClientProvider.client

        val (requestUrl, jsonPath, title) = when (config.presetType) {
            HttpPresetType.BING -> Triple(
                "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1",
                "images[0].url",
                "Bing 每日壁纸"
            )
            HttpPresetType.PICSUM -> Triple(
                "https://picsum.photos/1080/1920",
                null,
                "Picsum 随机壁纸"
            )
            HttpPresetType.CUSTOM -> {
                val url = config.customUrl.trim()
                if (url.isEmpty()) {
                    throw IllegalArgumentException("自定义 API 网址不能为空")
                }
                val path = config.customJsonPath.trim().takeIf { it.isNotEmpty() }
                Triple(url, path, "自定义网络壁纸")
            }
        }

        var directImageUrl = requestUrl

        // Step 1: If a JSON path is specified, query the JSON API first to extract the image URL
        if (jsonPath != null) {
            val jsonRequest = Request.Builder()
                .url(requestUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .build()

            val jsonResponse = client.newCall(jsonRequest).execute()
            if (!jsonResponse.isSuccessful) {
                throw IOException("JSON API 请求失败: HTTP ${jsonResponse.code}")
            }

            val bodyString = jsonResponse.body?.string()
                ?: throw IOException("JSON API 返回空数据")

            val extractedUrl = JsonPathExtractor.extractString(bodyString, jsonPath)
                ?: throw IOException("未能从响应中提取图片路径 ($jsonPath)")

            directImageUrl = resolveUrl(requestUrl, extractedUrl)
        }

        // Step 2: Retrieve from cache if already downloaded, otherwise download from directImageUrl
        val savedFile = cacheManager.findCachedFile(directImageUrl) ?: run {
            val imageRequest = Request.Builder()
                .url(directImageUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val imageResponse: Response = client.newCall(imageRequest).execute()
            if (!imageResponse.isSuccessful) {
                throw IOException("图片下载失败: HTTP ${imageResponse.code}")
            }

            val responseBody = imageResponse.body
                ?: throw IOException("图片响应体为空")

            // Step 3: Stream directly into the cache file (avoids storing the entire byte array in RAM)
            cacheManager.saveStream(
                inputStream = responseBody.byteStream(),
                preferredTitle = title,
                cacheKey = directImageUrl
            )
        }

        return WallpaperData(
            openStream = { FileInputStream(savedFile) },
            title = title,
            sourceUri = Uri.fromFile(savedFile)
        )
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String {
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            return candidate
        }
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(candidate).toString()
        } catch (_: Exception) {
            if (candidate.startsWith("/")) {
                val origin = baseUrl.substringBefore("/", "").ifEmpty { baseUrl }
                origin + candidate
            } else {
                "$baseUrl/$candidate"
            }
        }
    }

    companion object {
        private const val USER_AGENT = "WallpaperPicker/1.0 (Android)"

        fun isNetworkAvailableAndAllowed(context: Context, wifiOnly: Boolean): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (!hasInternet) return false

                if (wifiOnly) {
                    val isUnmetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    return isUnmetered || isWifi
                }
                return true
            } else {
                @Suppress("DEPRECATION")
                val activeInfo = cm.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                if (!activeInfo.isConnected) return false

                if (wifiOnly) {
                    @Suppress("DEPRECATION")
                    return activeInfo.type == ConnectivityManager.TYPE_WIFI ||
                            activeInfo.type == ConnectivityManager.TYPE_ETHERNET
                }
                return true
            }
        }
    }
}
