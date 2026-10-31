package com.miupiu.zveryata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryLogicTest {
    private val swipeThreshold = 100f
    private val tapSlop = 25f

    private fun swipe(
        x: Float,
        y: Float,
        elapsed: Long = 250L,
        cooldownPassed: Boolean = true
    ): GestureAction = classifyHorizontalSwipe(
        dragX = x,
        dragY = y,
        elapsedMs = elapsed,
        swipeThresholdPx = swipeThreshold,
        cooldownPassed = cooldownPassed
    )

    @Test
    fun swipeLeftGoesNext() {
        assertEquals(GestureAction.NEXT, swipe(x = -160f, y = 10f))
    }

    @Test
    fun swipeRightGoesPrevious() {
        assertEquals(GestureAction.PREVIOUS, swipe(x = 160f, y = 10f))
    }

    @Test
    fun verticalSwipeDoesNothing() {
        assertEquals(GestureAction.NONE, swipe(x = 10f, y = -160f))
        assertEquals(GestureAction.NONE, swipe(x = 10f, y = 160f))
    }

    @Test
    fun diagonalGestureDoesNothing() {
        assertEquals(GestureAction.NONE, swipe(x = 160f, y = 150f))
    }

    @Test
    fun shortMovementDoesNothing() {
        assertEquals(GestureAction.NONE, swipe(x = 20f, y = 5f))
    }

    @Test
    fun cooldownBlocksNavigation() {
        assertEquals(GestureAction.NONE, swipe(x = -160f, y = 0f, cooldownPassed = false))
    }

    @Test
    fun tapsNeverNavigateAndAreRecognizedForTimerReset() {
        assertTrue(isTap(0f, 0f, 120L, tapSlop))
        assertTrue(isTap(15f, 5f, 120L, tapSlop))
        assertFalse(isTap(40f, 0f, 120L, tapSlop))
        assertFalse(isTap(0f, 0f, 700L, tapSlop))
    }

    @Test
    fun wrapsForwardAfterLastSlide() {
        assertEquals(0, wrapIndex(current = 58, delta = 1, size = 59))
    }

    @Test
    fun wrapsBackwardBeforeFirstSlide() {
        assertEquals(58, wrapIndex(current = 0, delta = -1, size = 59))
    }

    @Test
    fun repeatedNavigationRemainsInRange() {
        var index = 0
        repeat(10_000) {
            index = wrapIndex(index, 1, 59)
        }
        assertEquals(10_000 % 59, index)

        repeat(10_000) {
            index = wrapIndex(index, -1, 59)
        }
        assertEquals(0, index)
    }

    @Test
    fun autoAdvanceIntervalIsFifteenSeconds() {
        assertEquals(15_000L, AUTO_ADVANCE_MS)
    }
}
