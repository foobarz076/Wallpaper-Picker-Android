package foo.barz.wallpaperpicker.core.util

import android.util.Log
import foo.barz.wallpaperpicker.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representation of an individual diagnostic log event.
 */
data class LogEntry(
    val timestamp: Long,
    val priority: Int,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

/**
 * Lightweight logging utility that conditionally executes debug and verbose logs only in debug builds,
 * and maintains an in-memory ring buffer for runtime diagnostic inspection and export.
 */
object AppLog {

    const val MAX_BUFFER_CAPACITY = 500

    @Volatile
    var isDebug: Boolean = BuildConfig.DEBUG

    private val bufferLock = Any()
    private val ringBuffer = ArrayDeque<LogEntry>(MAX_BUFFER_CAPACITY)

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

    /**
     * Records a log entry into the internal circular buffer.
     */
    fun recordEntry(entry: LogEntry) {
        synchronized(bufferLock) {
            if (ringBuffer.size >= MAX_BUFFER_CAPACITY) {
                ringBuffer.removeFirst()
            }
            ringBuffer.addLast(entry)
        }
    }

    /**
     * Retrieves an immutable snapshot of all buffered log entries in chronological order.
     */
    fun getLogs(): List<LogEntry> = synchronized(bufferLock) {
        ringBuffer.toList()
    }

    /**
     * Returns the current number of buffered log entries.
     */
    fun getLogCount(): Int = synchronized(bufferLock) {
        ringBuffer.size
    }

    /**
     * Clears all in-memory diagnostic logs.
     */
    fun clearLogs() {
        synchronized(bufferLock) {
            ringBuffer.clear()
        }
    }

    /**
     * Formats a single log entry into a human-readable string.
     */
    fun formatEntry(entry: LogEntry, sanitize: Boolean = true): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val dateStr = dateFormat.format(Date(entry.timestamp))
        val levelChar = when (entry.priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "?"
        }

        val rawMessage = if (entry.throwable != null) {
            "${entry.message}\n${entry.throwable.stackTraceToString()}"
        } else {
            entry.message
        }

        val finalMessage = if (sanitize) LogSanitizer.sanitize(rawMessage) else rawMessage
        return "$dateStr [$levelChar/${entry.tag}] $finalMessage"
    }

    /**
     * Formats all buffered logs into a single newline-separated text block.
     */
    fun formatLogs(sanitize: Boolean = true): String {
        val entries = getLogs()
        if (entries.isEmpty()) return "暂无日志记录 (No logs recorded)"
        return entries.joinToString("\n") { formatEntry(it, sanitize) }
    }

    @PublishedApi
    internal fun dispatch(priority: Int, tag: String, message: String, throwable: Throwable? = null) {
        recordEntry(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                priority = priority,
                tag = tag,
                message = message,
                throwable = throwable
            )
        )
        logger(priority, tag, message, throwable)
    }

    inline fun v(tag: String, message: () -> String) {
        if (isDebug) {
            dispatch(Log.VERBOSE, tag, message(), null)
        }
    }

    fun v(tag: String, message: String) {
        if (isDebug) {
            dispatch(Log.VERBOSE, tag, message, null)
        }
    }

    inline fun d(tag: String, message: () -> String) {
        if (isDebug) {
            dispatch(Log.DEBUG, tag, message(), null)
        }
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            dispatch(Log.DEBUG, tag, message, null)
        }
    }

    inline fun i(tag: String, message: () -> String) {
        dispatch(Log.INFO, tag, message(), null)
    }

    fun i(tag: String, message: String) {
        dispatch(Log.INFO, tag, message, null)
    }

    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        dispatch(Log.WARN, tag, message(), throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        dispatch(Log.WARN, tag, message, throwable)
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        dispatch(Log.ERROR, tag, message(), throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        dispatch(Log.ERROR, tag, message, throwable)
    }
}
