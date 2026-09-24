package foo.barz.wallpaperpicker.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Singleton provider for OkHttpClient with standard timeout and connection settings.
 */
object HttpClientProvider {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
