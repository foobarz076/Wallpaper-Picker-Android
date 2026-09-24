package foo.barz.wallpaperpicker

import android.app.Application
import androidx.work.Configuration

import org.conscrypt.Conscrypt
import java.security.Security

class WallpaperPickerApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Install Conscrypt as the primary security provider for modern TLS 1.3 & CA support on Android 6.0+
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }
}
