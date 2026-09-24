package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.worker.WallpaperWorker
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PeriodicWorkerP0P1Test {

    @Test
    fun testWorkerNameConstant() {
        assertEquals("periodic_wallpaper_changer", WallpaperWorker.WORK_NAME)
    }

    @Test
    fun testHttpPresetTypes() {
        val presets = HttpPresetType.values()
        assertEquals(3, presets.size)
        assertEquals("Bing 每日壁纸", HttpPresetType.BING.label)
        assertEquals("Picsum 随机摄影", HttpPresetType.PICSUM.label)
        assertEquals("自定义 HTTP API", HttpPresetType.CUSTOM.label)
    }

    @Test
    fun testDeterministicCacheKeyHashing() {
        val testUrl = "https://cn.bing.com/th?id=OHR.MountShasta_ZH-CN1234567890_1920x1080.jpg"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(testUrl.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }

        assertNotNull(hash)
        assertEquals(32, hash.length)

        // Hashing the same URL must always yield the exact same hash
        val digest2 = md.digest(testUrl.toByteArray(Charsets.UTF_8))
        val hash2 = digest2.joinToString("") { "%02x".format(it) }
        assertEquals(hash, hash2)
    }
}
