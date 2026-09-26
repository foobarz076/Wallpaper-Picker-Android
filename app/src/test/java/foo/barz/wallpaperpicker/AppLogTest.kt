package foo.barz.wallpaperpicker

import android.util.Log
import foo.barz.wallpaperpicker.core.util.AppLog
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLogTest {

    private val loggedEntries = mutableListOf<LogEntry>()
    private var originalDebug: Boolean = false
    private var originalLogger: ((Int, String, String, Throwable?) -> Unit)? = null

    data class LogEntry(
        val priority: Int,
        val tag: String,
        val message: String,
        val throwable: Throwable?
    )

    @BeforeTest
    fun setUp() {
        loggedEntries.clear()
        originalDebug = AppLog.isDebug
        originalLogger = AppLog.logger

        AppLog.logger = { priority, tag, message, throwable ->
            loggedEntries.add(LogEntry(priority, tag, message, throwable))
        }
    }

    @AfterTest
    fun tearDown() {
        AppLog.isDebug = originalDebug
        originalLogger?.let { AppLog.logger = it }
    }

    @Test
    fun testDebugLogEvaluatedWhenDebugEnabled() {
        AppLog.isDebug = true
        var lambdaEvaluated = false

        AppLog.d("TestTag") {
            lambdaEvaluated = true
            "Debug message evaluated"
        }

        assertTrue(lambdaEvaluated, "Lambda must be evaluated when isDebug is true")
        assertEquals(1, loggedEntries.size)
        assertEquals(Log.DEBUG, loggedEntries[0].priority)
        assertEquals("TestTag", loggedEntries[0].tag)
        assertEquals("Debug message evaluated", loggedEntries[0].message)
    }

    @Test
    fun testDebugLogSkippedWhenDebugDisabled() {
        AppLog.isDebug = false
        var lambdaEvaluated = false

        AppLog.d("TestTag") {
            lambdaEvaluated = true
            "This should not be evaluated"
        }

        assertFalse(lambdaEvaluated, "Lambda must NEVER be evaluated when isDebug is false")
        assertTrue(loggedEntries.isEmpty(), "No logs should be recorded when isDebug is false")
    }

    @Test
    fun testVerboseLogSkippedWhenDebugDisabled() {
        AppLog.isDebug = false
        var lambdaEvaluated = false

        AppLog.v("TestTag") {
            lambdaEvaluated = true
            "Verbose message"
        }

        assertFalse(lambdaEvaluated)
        assertTrue(loggedEntries.isEmpty())
    }

    @Test
    fun testErrorLogAlwaysLogged() {
        AppLog.isDebug = false
        val exception = RuntimeException("Boom")

        AppLog.e("ErrorTag", "Fatal failure", exception)

        assertEquals(1, loggedEntries.size)
        assertEquals(Log.ERROR, loggedEntries[0].priority)
        assertEquals("ErrorTag", loggedEntries[0].tag)
        assertEquals("Fatal failure", loggedEntries[0].message)
        assertEquals(exception, loggedEntries[0].throwable)
    }

    @Test
    fun testInfoAndWarnAlwaysLogged() {
        AppLog.isDebug = false

        AppLog.i("InfoTag", "Normal info")
        AppLog.w("WarnTag", "Warning issue")

        assertEquals(2, loggedEntries.size)
        assertEquals(Log.INFO, loggedEntries[0].priority)
        assertEquals(Log.WARN, loggedEntries[1].priority)
    }

    @Test
    fun testRingBufferRecordsAndClearsLogs() {
        AppLog.clearLogs()
        assertEquals(0, AppLog.getLogCount())

        AppLog.i("BufferTag", "Message 1")
        AppLog.w("BufferTag", "Message 2")

        assertEquals(2, AppLog.getLogCount())
        val logs = AppLog.getLogs()
        assertEquals(2, logs.size)
        assertEquals("Message 1", logs[0].message)
        assertEquals("Message 2", logs[1].message)

        val formatted = AppLog.formatLogs(sanitize = false)
        assertTrue(formatted.contains("[I/BufferTag] Message 1"))
        assertTrue(formatted.contains("[W/BufferTag] Message 2"))

        AppLog.clearLogs()
        assertEquals(0, AppLog.getLogCount())
        assertTrue(AppLog.getLogs().isEmpty())
    }

    @Test
    fun testRingBufferEvictsOldestWhenFull() {
        AppLog.clearLogs()
        for (i in 1..AppLog.MAX_BUFFER_CAPACITY + 10) {
            AppLog.i("BufferTag", "Entry $i")
        }

        assertEquals(AppLog.MAX_BUFFER_CAPACITY, AppLog.getLogCount())
        val logs = AppLog.getLogs()
        // The first 10 entries should have been evicted
        assertEquals("Entry 11", logs.first().message)
        assertEquals("Entry ${AppLog.MAX_BUFFER_CAPACITY + 10}", logs.last().message)
        AppLog.clearLogs()
    }
}
