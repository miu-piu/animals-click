package com.miupiu.zveryata

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryLogicTest {
    private val screenWidth = 1000
    private val swipeThreshold = 100f
    private val tapSlop = 25f

    private fun gesture(
        x: Float,
        y: Float,
        elapsed: Long = 250L,
        startX: Float = 500f,
        cooldownPassed: Boolean = true
    ): GestureAction = classifyGesture(
        dragX = x,
        dragY = y,
        elapsedMs = elapsed,
        startX = startX,
        screenWidthPx = screenWidth,
        swipeThresholdPx = swipeThreshold,
        tapSlopPx = tapSlop,
        cooldownPassed = cooldownPassed
    )

    @Test
    fun swipeLeftGoesNext() {
        assertEquals(GestureAction.NEXT, gesture(x = -160f, y = 10f))
    }

    @Test
    fun swipeRightGoesPrevious() {
        assertEquals(GestureAction.PREVIOUS, gesture(x = 160f, y = 10f))
    }

    @Test
    fun swipeUpGoesNext() {
        assertEquals(GestureAction.NEXT, gesture(x = 10f, y = -160f))
    }

    @Test
    fun swipeDownGoesPrevious() {
        assertEquals(GestureAction.PREVIOUS, gesture(x = 10f, y = 160f))
    }

    @Test
    fun diagonalGestureDoesNothing() {
        assertEquals(GestureAction.NONE, gesture(x = 160f, y = 150f))
    }

    @Test
    fun shortMovementInCenterDoesNothing() {
        assertEquals(GestureAction.NONE, gesture(x = 20f, y = 5f, startX = 500f))
    }

    @Test
    fun leftEdgeTapGoesPrevious() {
        assertEquals(GestureAction.PREVIOUS, gesture(x = 0f, y = 0f, elapsed = 120L, startX = 100f))
    }

    @Test
    fun rightEdgeTapGoesNext() {
        assertEquals(GestureAction.NEXT, gesture(x = 0f, y = 0f, elapsed = 120L, startX = 900f))
    }

    @Test
    fun centerTapDoesNothing() {
        assertEquals(GestureAction.NONE, gesture(x = 0f, y = 0f, elapsed = 120L, startX = 500f))
    }

    @Test
    fun longPressDoesNothing() {
        assertEquals(GestureAction.NONE, gesture(x = 0f, y = 0f, elapsed = 700L, startX = 900f))
    }

    @Test
    fun cooldownBlocksNavigation() {
        assertEquals(
            GestureAction.NONE,
            gesture(x = -160f, y = 0f, cooldownPassed = false)
        )
        assertEquals(
            GestureAction.NONE,
            gesture(x = 0f, y = 0f, elapsed = 120L, startX = 900f, cooldownPassed = false)
        )
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
    fun autoAdvanceIntervalIsThirtySeconds() {
        assertEquals(30_000L, AUTO_ADVANCE_MS)
    }
}
