package foo.barz.wallpaperpicker.core.network

import okhttp3.OkHttpClient
import java.io.IOException
import java.net.UnknownServiceException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Singleton and factory provider for OkHttpClient instances.
 * Supports standard TLS verification as well as an insecure client for self-signed certificates.
 */
object HttpClientProvider {

    val client: OkHttpClient by lazy {
        createBaseBuilder().build()
    }

    val insecureClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        createBaseBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun getClient(ignoreSsl: Boolean): OkHttpClient {
        return if (ignoreSsl) insecureClient else client
    }

    private fun createBaseBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                try {
                    chain.proceed(chain.request())
                } catch (e: Exception) {
                    if (e is UnknownServiceException ||
                        e.message?.contains("CLEARTEXT", ignoreCase = true) == true ||
                        e.cause?.message?.contains("CLEARTEXT", ignoreCase = true) == true
                    ) {
                        throw IOException(
                            "系统网络安全策略已拦截未加密的 HTTP 明文请求。请优先使用 HTTPS 协议，或使用以 .local / .lan 结尾的局域网主机名。",
                            e
                        )
                    }
                    throw e
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
    }
}
