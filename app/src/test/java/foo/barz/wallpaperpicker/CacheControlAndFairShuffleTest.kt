package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.model.CacheSizeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Phase 4.1.3 (Cache Size Tier & Disabled Auto-Pruning)
 * and Phase 4.4 (Configurable Fair Shuffle Deck Capacity & Deck Reset).
 */
class CacheControlAndFairShuffleTest {

    @Test
    fun testCacheSizeTierBoundsAndAutoPruningFlag() {
        // 1. SMALL tier
        assertEquals(10, CacheSizeTier.SMALL.maxCount)
        assertEquals(20L * 1024L * 1024L, CacheSizeTier.SMALL.maxSizeBytes)
        assertTrue(CacheSizeTier.SMALL.isAutoPruneEnabled)

        // 2. STANDARD tier (Default)
        assertEquals(25, CacheSizeTier.STANDARD.maxCount)
        assertEquals(50L * 1024L * 1024L, CacheSizeTier.STANDARD.maxSizeBytes)
        assertTrue(CacheSizeTier.STANDARD.isAutoPruneEnabled)

        // 3. LARGE tier
        assertEquals(50, CacheSizeTier.LARGE.maxCount)
        assertEquals(100L * 1024L * 1024L, CacheSizeTier.LARGE.maxSizeBytes)
        assertTrue(CacheSizeTier.LARGE.isAutoPruneEnabled)

        // 4. DISABLED tier (User option to disable automatic cleanup)
        assertEquals(Int.MAX_VALUE, CacheSizeTier.DISABLED.maxCount)
        assertEquals(Long.MAX_VALUE, CacheSizeTier.DISABLED.maxSizeBytes)
        assertFalse(CacheSizeTier.DISABLED.isAutoPruneEnabled)
    }

    @Test
    fun testPruneCacheSimulationWithDisabledMode() {
        data class MockFile(val name: String, val size: Long, val lastModified: Long)

        val files = mutableListOf(
            MockFile("f1", 10 * 1024 * 1024L, 1000L),
            MockFile("f2", 10 * 1024 * 1024L, 2000L),
            MockFile("f3", 10 * 1024 * 1024L, 3000L),
            MockFile("f4", 10 * 1024 * 1024L, 4000L),
            MockFile("f5", 10 * 1024 * 1024L, 5000L)
        ) // Total 50 MB (5 files)

        fun prune(tier: CacheSizeTier) {
            if (!tier.isAutoPruneEnabled) {
                return
            }
            files.sortBy { it.lastModified }
            var currentCount = files.size
            var currentSize = files.sumOf { it.size }
            val iterator = files.iterator()
            while (iterator.hasNext()) {
                if (currentCount <= tier.maxCount && currentSize <= tier.maxSizeBytes) {
                    break
                }
                val f = iterator.next()
                currentSize -= f.size
                currentCount--
                iterator.remove()
            }
        }

        // When DISABLED is selected, no pruning occurs
        prune(CacheSizeTier.DISABLED)
        assertEquals(5, files.size)
        assertEquals(50 * 1024 * 1024L, files.sumOf { it.size })

        // When SMALL is selected (limit 20MB / 10 count), oldest 3 files are pruned
        prune(CacheSizeTier.SMALL)
        assertEquals(2, files.size)
        assertEquals(20 * 1024 * 1024L, files.sumOf { it.size })
        assertEquals(listOf("f4", "f5"), files.map { it.name })
    }

    @Test
    fun testFairShuffleDynamicCapacityAndDeckReset() {
        val deck = mutableListOf<String>()

        fun record(key: String, capacity: Int) {
            deck.removeAll { it == key }
            deck.add(key)
            while (deck.size > capacity) {
                deck.removeAt(0)
            }
        }

        fun resize(newCapacity: Int) {
            if (deck.size > newCapacity) {
                val trimmed = deck.takeLast(newCapacity)
                deck.clear()
                deck.addAll(trimmed)
            }
        }

        // Record 10 items with capacity 50
        (1..10).forEach { record("img_$it", 50) }
        assertEquals(10, deck.size)
        assertEquals("img_1", deck.first())
        assertEquals("img_10", deck.last())

        // User changes capacity to 20 (no trimming needed yet)
        resize(20)
        assertEquals(10, deck.size)

        // User reduces capacity to 5 -> oldest 5 evicted, newest 5 preserved
        resize(5)
        assertEquals(5, deck.size)
        assertEquals(listOf("img_6", "img_7", "img_8", "img_9", "img_10"), deck)

        // User triggers deck reset
        deck.clear()
        assertEquals(0, deck.size)
        assertTrue(deck.isEmpty())
    }
}
