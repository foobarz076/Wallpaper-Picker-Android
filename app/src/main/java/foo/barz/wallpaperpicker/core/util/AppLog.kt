package foo.barz.wallpaperpicker.core.util

import android.util.Log
import foo.barz.wallpaperpicker.BuildConfig

/**
 * Lightweight logging utility that conditionally executes debug and verbose logs only in debug builds.
 * Uses inline lambdas to prevent unnecessary string evaluations and allocations in release builds.
 */
object AppLog {

    @Volatile
    var isDebug: Boolean = BuildConfig.DEBUG

    /**
     * Delegate for dispatching log messages. Can be replaced in tests or for custom logging sinks.
     */
    @Volatile
    var logger: (priority: Int, tag: String, message: String, throwable: Throwable?) -> Unit =
        { priority, tag, message, throwable ->
            runCatching {
                when (priority) {
                    Log.VERBOSE -> Log.v(tag, message, throwable)
                    Log.DEBUG -> Log.d(tag, message, throwable)
                    Log.INFO -> Log.i(tag, message, throwable)
                    Log.WARN -> Log.w(tag, message, throwable)
                    Log.ERROR -> Log.e(tag, message, throwable)
                    else -> Log.println(priority, tag, message)
                }
            }
        }

    inline fun v(tag: String, message: () -> String) {
        if (isDebug) {
            logger(Log.VERBOSE, tag, message(), null)
        }
    }

    fun v(tag: String, message: String) {
        if (isDebug) {
            logger(Log.VERBOSE, tag, message, null)
        }
    }

    inline fun d(tag: String, message: () -> String) {
        if (isDebug) {
            logger(Log.DEBUG, tag, message(), null)
        }
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            logger(Log.DEBUG, tag, message, null)
        }
    }

    inline fun i(tag: String, message: () -> String) {
        logger(Log.INFO, tag, message(), null)
    }

    fun i(tag: String, message: String) {
        logger(Log.INFO, tag, message, null)
    }

    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        logger(Log.WARN, tag, message(), throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        logger(Log.WARN, tag, message, throwable)
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        logger(Log.ERROR, tag, message(), throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logger(Log.ERROR, tag, message, throwable)
    }
}
