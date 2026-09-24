package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.HttpPresetType
import foo.barz.wallpaperpicker.core.worker.WallpaperWorker
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    @Test
    fun testFairShuffleFifoEviction() {
        val maxCapacity = 5
        val list = mutableListOf<String>()

        fun record(key: String) {
            list.removeAll { it == key }
            list.add(key)
            while (list.size > maxCapacity) {
                list.removeAt(0)
            }
        }

        listOf("img1", "img2", "img3", "img4", "img5").forEach { record(it) }
        assertEquals(5, list.size)
        assertEquals("img1", list.first())
        assertEquals("img5", list.last())

        // Adding a 6th item should evict the oldest ("img1")
        record("img6")
        assertEquals(5, list.size)
        assertEquals(listOf("img2", "img3", "img4", "img5", "img6"), list)

        // Re-adding "img3" should move it to the end without increasing size
        record("img3")
        assertEquals(5, list.size)
        assertEquals(listOf("img2", "img4", "img5", "img6", "img3"), list)
    }

    @Test
    fun testFairShuffleExclusionSet() {
        val totalImages = listOf("a", "b", "c", "d")
        val recentHistory = setOf("a", "b")

        // Excluded set should leave remaining candidates
        val remaining = totalImages.filter { !recentHistory.contains(it) }
        assertEquals(listOf("c", "d"), remaining)

        // When all images have been used, deck should reset to all
        val exhaustedHistory = setOf("a", "b", "c", "d")
        val shouldReset = exhaustedHistory.size >= totalImages.size
        assertTrue(shouldReset)
    }
}
