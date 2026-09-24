package foo.barz.wallpaperpicker.core.source

import android.content.Context
import android.net.Uri
import foo.barz.wallpaperpicker.core.cache.WallpaperCacheManager
import foo.barz.wallpaperpicker.core.model.ImmichAlbum
import foo.barz.wallpaperpicker.core.model.ImmichConfig
import foo.barz.wallpaperpicker.core.model.ImmichQuality
import foo.barz.wallpaperpicker.core.model.WallpaperData
import foo.barz.wallpaperpicker.core.network.HttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.IOException

/**
 * Wallpaper source that fetches random images from an Immich self-hosted instance.
 * Supports whole-library random queries, specific album filtering, self-signed TLS, and local LRU caching.
 */
class ImmichSource(
    private val context: Context,
    private val config: ImmichConfig,
    private val cacheManager: WallpaperCacheManager,
    private val bypassNetworkConstraints: Boolean = false
) : WallpaperSource {

    override val id: String = "immich"

    override val displayName: String
        get() = if (config.albumName.isNullOrEmpty()) "Immich (全部相册)" else "Immich (${config.albumName})"

    override suspend fun getNextWallpaper(): Result<WallpaperData> = withContext(Dispatchers.IO) {
        val networkAllowed = bypassNetworkConstraints ||
                HttpApiSource.isNetworkAvailableAndAllowed(context, config.wifiOnly)

        if (!networkAllowed) {
            val cached = cacheManager.getRandomCachedWallpaper()
            if (cached != null) {
                return@withContext Result.success(cached)
            }
            return@withContext Result.failure(IllegalStateException("当前处于移动网络或离线状态，且本地缓存池为空"))
        }

        try {
            val wallpaper = fetchFromImmich()
            Result.success(wallpaper)
        } catch (e: Exception) {
            val cached = cacheManager.getRandomCachedWallpaper()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun fetchFromImmich(): WallpaperData {
        val baseUrl = config.serverUrl.trim().trimEnd('/')
        if (baseUrl.isEmpty()) {
            throw IllegalArgumentException("Immich 服务器地址未填写")
        }

        val apiKey = config.apiKey.trim()
        if (apiKey.isEmpty()) {
            throw IllegalArgumentException("Immich API Key 未填写")
        }

        val client = HttpClientProvider.getClient(config.ignoreSslErrors)

        // Step 1: Query for target asset ID and file name
        val (assetId, originalName) = if (config.albumId.isNullOrEmpty()) {
            queryRandomAsset(baseUrl, apiKey, client)
        } else {
            queryAlbumRandomAsset(baseUrl, apiKey, config.albumId, client)
        }

        // Step 2: Download asset stream with fallback support (preview -> original)
        val downloadResponse = downloadAssetStream(client, baseUrl, apiKey, assetId, config.downloadQuality)

        val responseBody = downloadResponse.body
            ?: throw IOException("下载 Immich 照片响应体为空")

        val displayTitle = originalName.ifEmpty { "Immich 照片 ($assetId)" }

        // Step 3: Stream directly into the local LRU cache
        val savedFile = cacheManager.saveStream(
            inputStream = responseBody.byteStream(),
            preferredTitle = displayTitle
        )

        return WallpaperData(
            openStream = { FileInputStream(savedFile) },
            title = displayTitle,
            sourceUri = Uri.fromFile(savedFile)
        )
    }

    /**
     * Downloads an asset stream from Immich.
     * When PREVIEW is requested, attempts to load the preview thumbnail first; if missing (e.g. 404 when
     * microservices transcode job has not finished yet) or rejected, automatically falls back to ORIGINAL
     * so wallpaper rotation remains uninterrupted. Also supports legacy API routes for backwards compatibility.
     */
    private fun downloadAssetStream(
        client: okhttp3.OkHttpClient,
        baseUrl: String,
        apiKey: String,
        assetId: String,
        quality: ImmichQuality
    ): okhttp3.Response {
        fun buildRequest(url: String): Request {
            return Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .header("Accept", "image/*,*/*")
                .header("User-Agent", "WallpaperPicker/1.0 (Android)")
                .build()
        }

        if (quality == ImmichQuality.ORIGINAL) {
            val originalUrl = "$baseUrl/api/assets/$assetId/original"
            val response = client.newCall(buildRequest(originalUrl)).execute()
            if (response.isSuccessful) return response

            if (response.code == 404) {
                response.close()
                val legacyUrl = "$baseUrl/api/asset/file/$assetId"
                val legacyResp = client.newCall(buildRequest(legacyUrl)).execute()
                if (legacyResp.isSuccessful) return legacyResp
                val legacyError = legacyResp.body?.string()?.take(200)
                legacyResp.close()
                throw IOException("下载原图失败: HTTP ${legacyResp.code} ($legacyError)")
            }

            val errorMsg = response.body?.string()?.take(200)
            response.close()
            throw IOException("下载原图失败: HTTP ${response.code} ($errorMsg)")
        }

        // When quality is PREVIEW:
        // Attempt 1: Modern preview thumbnail endpoint
        val previewUrl = "$baseUrl/api/assets/$assetId/thumbnail?size=preview"
        val previewResponse = client.newCall(buildRequest(previewUrl)).execute()
        if (previewResponse.isSuccessful) {
            return previewResponse
        }
        val previewCode = previewResponse.code
        previewResponse.close()

        // Attempt 2: Legacy preview thumbnail endpoint (Immich < v1.106)
        val legacyPreviewUrl = "$baseUrl/api/asset/thumbnail/$assetId?size=preview"
        val legacyPreviewResp = client.newCall(buildRequest(legacyPreviewUrl)).execute()
        if (legacyPreviewResp.isSuccessful) {
            return legacyPreviewResp
        }
        legacyPreviewResp.close()

        // Attempt 3: Automatic fallback to ORIGINAL if preview is not generated or available
        val fallbackUrl = "$baseUrl/api/assets/$assetId/original"
        val fallbackResp = client.newCall(buildRequest(fallbackUrl)).execute()
        if (fallbackResp.isSuccessful) {
            return fallbackResp
        }
        fallbackResp.close()

        // Attempt 4: Fallback to legacy file route
        val legacyFallbackUrl = "$baseUrl/api/asset/file/$assetId"
        val legacyFallbackResp = client.newCall(buildRequest(legacyFallbackUrl)).execute()
        if (legacyFallbackResp.isSuccessful) {
            return legacyFallbackResp
        }

        val finalError = legacyFallbackResp.body?.string()?.take(200)
        val finalCode = legacyFallbackResp.code
        legacyFallbackResp.close()

        throw IOException("下载 Immich 照片失败 (预览图 HTTP $previewCode，原图保底 HTTP $finalCode: $finalError)")
    }

    private fun queryRandomAsset(
        baseUrl: String,
        apiKey: String,
        client: okhttp3.OkHttpClient
    ): Pair<String, String> {
        val randomEndpoint = "$baseUrl/api/search/random"
        val payload = JSONObject().apply {
            put("type", "IMAGE")
            put("size", 1)
            put("withDeleted", false)
        }

        val request = Request.Builder()
            .url(randomEndpoint)
            .header("x-api-key", apiKey)
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Immich 随机照片接口请求失败: HTTP ${response.code}")
        }

        val bodyString = response.body?.string()
            ?: throw IOException("Immich 随机接口返回为空")

        val rootArray = JSONArray(bodyString)
        if (rootArray.length() == 0) {
            throw NoSuchElementException("Immich 图库中未找到图片")
        }

        val assetObj = rootArray.getJSONObject(0)
        val id = assetObj.getString("id")
        val fileName = assetObj.optString("originalFileName", "Immich_$id.jpg")
        return Pair(id, fileName)
    }

    private fun queryAlbumRandomAsset(
        baseUrl: String,
        apiKey: String,
        albumId: String,
        client: okhttp3.OkHttpClient
    ): Pair<String, String> {
        // Attempt 1: Search metadata with albumIds
        try {
            val metadataEndpoint = "$baseUrl/api/search/metadata"
            val payload = JSONObject().apply {
                put("albumIds", JSONArray().put(albumId))
                put("type", "IMAGE")
                put("withDeleted", false)
            }

            val request = Request.Builder()
                .url(metadataEndpoint)
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val root = JSONObject(body)
                    val assetsObj = root.optJSONObject("assets")
                    val itemsArray = assetsObj?.optJSONArray("items")
                    if (itemsArray != null && itemsArray.length() > 0) {
                        val randomIndex = (0 until itemsArray.length()).random()
                        val assetObj = itemsArray.getJSONObject(randomIndex)
                        val id = assetObj.getString("id")
                        val fileName = assetObj.optString("originalFileName", "Immich_$id.jpg")
                        return Pair(id, fileName)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to GET /api/albums/{id}
        }

        // Attempt 2: GET /api/albums/{id} for older Immich servers
        val albumEndpoint = "$baseUrl/api/albums/$albumId"
        val albumRequest = Request.Builder()
            .url(albumEndpoint)
            .header("x-api-key", apiKey)
            .header("Accept", "application/json")
            .build()

        val albumResponse = client.newCall(albumRequest).execute()
        if (!albumResponse.isSuccessful) {
            throw IOException("获取相册详情失败: HTTP ${albumResponse.code}")
        }

        val albumBody = albumResponse.body?.string()
            ?: throw IOException("相册详情返回为空")

        val albumJson = JSONObject(albumBody)
        val assetsArray = albumJson.optJSONArray("assets")
        if (assetsArray == null || assetsArray.length() == 0) {
            throw NoSuchElementException("所选相册中没有任何图片")
        }

        val randomIndex = (0 until assetsArray.length()).random()
        val assetObj = assetsArray.getJSONObject(randomIndex)
        val id = assetObj.getString("id")
        val fileName = assetObj.optString("originalFileName", "Immich_$id.jpg")
        return Pair(id, fileName)
    }

    companion object {
        /**
         * Fetches all accessible albums from Immich to populate the UI dropdown/picker.
         */
        suspend fun fetchAlbums(
            serverUrl: String,
            apiKey: String,
            ignoreSsl: Boolean
        ): Result<List<ImmichAlbum>> = withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = serverUrl.trim().trimEnd('/')
                if (baseUrl.isEmpty()) throw IllegalArgumentException("服务器地址不能为空")
                if (apiKey.trim().isEmpty()) throw IllegalArgumentException("API Key 不能为空")

                val client = HttpClientProvider.getClient(ignoreSsl)
                val request = Request.Builder()
                    .url("$baseUrl/api/albums")
                    .header("x-api-key", apiKey.trim())
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("获取相册失败: HTTP ${response.code}")
                }

                val body = response.body?.string() ?: throw IOException("返回数据为空")
                val rootArray = JSONArray(body)
                val albums = mutableListOf<ImmichAlbum>()

                for (i in 0 until rootArray.length()) {
                    val obj = rootArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.optString("albumName", "未命名相册")
                    val assetCount = obj.optInt("assetCount", 0)
                    albums.add(ImmichAlbum(id, name, assetCount))
                }

                albums.sortedBy { it.name }
            }
        }
    }
}
