package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.model.WallpaperCropMode
import foo.barz.wallpaperpicker.core.model.WallpaperHistoryItem
import foo.barz.wallpaperpicker.core.model.WallpaperScrollMode
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import foo.barz.wallpaperpicker.ui.MainUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerImageAdjustmentTest {

    @Test
    fun testWallpaperHistoryItemWithPerImagePreferences() {
        val defaultItem = WallpaperHistoryItem(
            id = 1L,
            sourceUri = "file:///storage/emulated/0/Pictures/landscape.jpg",
            title = "Landscape.jpg",
            sourceType = WallpaperSourceType.LOCAL_FOLDER,
            appliedTimestamp = 1700000000000L
        )

        // Default state: no override, flip false, lockCropFocus null
        assertNull(defaultItem.customScrollMode)
        assertNull(defaultItem.cropFocusX)
        assertNull(defaultItem.cropFocusY)
        assertNull(defaultItem.lockCropFocusX)
        assertNull(defaultItem.lockCropFocusY)
        assertFalse(defaultItem.flipHorizontal)

        // Custom adjusted state with independent lock crop focus
        val adjustedItem = defaultItem.copy(
            customScrollMode = WallpaperScrollMode.NEVER,
            cropFocusX = 0.25f,
            cropFocusY = 0.15f,
            flipHorizontal = true,
            lockCropFocusX = 0.75f,
            lockCropFocusY = 0.85f
        )

        assertEquals(WallpaperScrollMode.NEVER, adjustedItem.customScrollMode)
        assertEquals(0.25f, adjustedItem.cropFocusX)
        assertEquals(0.15f, adjustedItem.cropFocusY)
        assertEquals(0.75f, adjustedItem.lockCropFocusX)
        assertEquals(0.85f, adjustedItem.lockCropFocusY)
        assertTrue(adjustedItem.flipHorizontal)
    }

    @Test
    fun testDatabaseConstantsIncludeFlipHorizontalAndLockCropFocus() {
        assertEquals("flip_horizontal", WallpaperHistoryDatabase.COLUMN_FLIP_HORIZONTAL)
        assertEquals("custom_scroll_mode", WallpaperHistoryDatabase.COLUMN_CUSTOM_SCROLL_MODE)
        assertEquals("crop_focus_x", WallpaperHistoryDatabase.COLUMN_CROP_FOCUS_X)
        assertEquals("crop_focus_y", WallpaperHistoryDatabase.COLUMN_CROP_FOCUS_Y)
        assertEquals("lock_crop_focus_x", WallpaperHistoryDatabase.COLUMN_LOCK_CROP_FOCUS_X)
        assertEquals("lock_crop_focus_y", WallpaperHistoryDatabase.COLUMN_LOCK_CROP_FOCUS_Y)
    }

    @Test
    fun testCropOffsetCalculationMath() {
        val targetWidth = 1080
        val targetHeight = 1920

        val scaledWidth = 1440
        val scaledHeight = 1920

        // Excess width = 360
        val excessWidth = scaledWidth - targetWidth

        // Focus X = 0.0 (leftmost)
        val focusLeft = 0.0f
        val offsetLeft = -(excessWidth * focusLeft.coerceIn(0f, 1f)).toInt()
        assertEquals(0, offsetLeft)

        // Focus X = 0.5 (center)
        val focusCenter = 0.5f
        val offsetCenter = -(excessWidth * focusCenter.coerceIn(0f, 1f)).toInt()
        assertEquals(-180, offsetCenter)
        assertEquals((targetWidth - scaledWidth) / 2, offsetCenter)

        // Focus X = 1.0 (rightmost)
        val focusRight = 1.0f
        val offsetRight = -(excessWidth * focusRight.coerceIn(0f, 1f)).toInt()
        assertEquals(-360, offsetRight)
        assertEquals(targetWidth - scaledWidth, offsetRight)
    }

    @Test
    fun testVerticalCropFocusOffsetMath() {
        val targetWidth = 1080
        val targetHeight = 1920

        // Wide image where scaled height exceeds target height (e.g., center crop)
        val scaledWidth = 1080
        val scaledHeight = 2400
        val excessHeight = scaledHeight - targetHeight // 480

        // Focus Y = 0.0 (top priority, head intact)
        val focusTop = 0.0f
        val offsetTop = -(excessHeight * focusTop.coerceIn(0f, 1f)).toInt()
        assertEquals(0, offsetTop)

        // Focus Y = 0.5 (standard center)
        val focusCenter = 0.5f
        val offsetCenter = -(excessHeight * focusCenter.coerceIn(0f, 1f)).toInt()
        assertEquals(-240, offsetCenter)
        assertEquals((targetHeight - scaledHeight) / 2, offsetCenter)

        // Focus Y = 1.0 (bottom priority)
        val focusBottom = 1.0f
        val offsetBottom = -(excessHeight * focusBottom.coerceIn(0f, 1f)).toInt()
        assertEquals(-480, offsetBottom)
        assertEquals(targetHeight - scaledHeight, offsetBottom)
    }

    @Test
    fun testBiasAlignmentMapping() {
        // BiasAlignment expects -1.0f for 0% and +1.0f for 100%
        val focus0 = 0.0f
        val bias0 = (focus0 - 0.5f) * 2f
        assertEquals(-1.0f, bias0)

        val focus50 = 0.5f
        val bias50 = (focus50 - 0.5f) * 2f
        assertEquals(0.0f, bias50)

        val focus100 = 1.0f
        val bias100 = (focus100 - 0.5f) * 2f
        assertEquals(1.0f, bias100)
    }

    @Test
    fun testMainUiStateWithCurrentWallpaperItem() {
        val state = MainUiState()
        assertNull(state.currentWallpaperItem)

        val item = WallpaperHistoryItem(
            id = 42L,
            sourceUri = "content://media/external/images/media/42",
            title = "Current.jpg",
            sourceType = WallpaperSourceType.MEDIA_STORE,
            appliedTimestamp = 1700000000000L,
            customScrollMode = WallpaperScrollMode.ALWAYS,
            cropFocusX = 0.7f,
            cropFocusY = 0.2f,
            flipHorizontal = true
        )

        val updated = state.copy(currentWallpaperItem = item)
        assertEquals(42L, updated.currentWallpaperItem?.id)
        assertEquals(WallpaperScrollMode.ALWAYS, updated.currentWallpaperItem?.customScrollMode)
        assertEquals(0.7f, updated.currentWallpaperItem?.cropFocusX)
        assertEquals(0.2f, updated.currentWallpaperItem?.cropFocusY)
        assertTrue(updated.currentWallpaperItem?.flipHorizontal == true)
    }

    @Test
    fun testIndependentLockAndHomeCropOffsets() {
        val targetWidth = 1080
        val targetHeight = 1920

        val scaledWidth = 1440
        val scaledHeight = 1920
        val excessWidth = scaledWidth - targetWidth // 360

        // Home screen focus at 0.2 (left-skewed)
        val homeFocusX = 0.2f
        val homeOffsetX = -(excessWidth * homeFocusX.coerceIn(0f, 1f)).toInt()
        assertEquals(-72, homeOffsetX)

        // Lock screen focus at 0.8 (right-skewed)
        val lockFocusX = 0.8f
        val lockOffsetX = -(excessWidth * lockFocusX.coerceIn(0f, 1f)).toInt()
        assertEquals(-288, lockOffsetX)

        // Ensure offsets are independent and distinct
        kotlin.test.assertNotEquals(homeOffsetX, lockOffsetX)
    }

    @Test
    fun testLockCropFocusLinkingFallback() {
        // When lockCropFocusX is null (linked mode), fallback to cropFocusX
        val linkedItem = WallpaperHistoryItem(
            id = 10L,
            sourceUri = "file:///dummy.jpg",
            title = "Dummy",
            sourceType = WallpaperSourceType.LOCAL_FOLDER,
            appliedTimestamp = 1000L,
            cropFocusX = 0.35f,
            cropFocusY = 0.45f,
            lockCropFocusX = null,
            lockCropFocusY = null
        )

        val effectiveLockFocusX = linkedItem.lockCropFocusX ?: linkedItem.cropFocusX ?: 0.5f
        val effectiveLockFocusY = linkedItem.lockCropFocusY ?: linkedItem.cropFocusY ?: 0.5f
        assertEquals(0.35f, effectiveLockFocusX)
        assertEquals(0.45f, effectiveLockFocusY)

        // When lockCropFocusX is specified (unlinked mode), use the custom lock crop focus
        val unlinkedItem = linkedItem.copy(
            lockCropFocusX = 0.90f,
            lockCropFocusY = 0.10f
        )
        val customLockFocusX = unlinkedItem.lockCropFocusX ?: unlinkedItem.cropFocusX ?: 0.5f
        val customLockFocusY = unlinkedItem.lockCropFocusY ?: unlinkedItem.cropFocusY ?: 0.5f
        assertEquals(0.90f, customLockFocusX)
        assertEquals(0.10f, customLockFocusY)
    }
}
