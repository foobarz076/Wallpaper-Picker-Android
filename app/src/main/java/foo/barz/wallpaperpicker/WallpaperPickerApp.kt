package foo.barz.wallpaperpicker

import android.app.Application
import androidx.work.Configuration

class WallpaperPickerApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
    }
}
