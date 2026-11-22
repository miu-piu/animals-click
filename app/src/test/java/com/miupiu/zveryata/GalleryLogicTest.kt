package com.miupiu.zveryata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryLogicTest {
    @Test
    fun movementInAnyDirectionCanTriggerNavigation() {
        val threshold = 100f
        assertTrue(movementExceedsThreshold(0f, -101f, threshold))
        assertTrue(movementExceedsThreshold(0f, 101f, threshold))
        assertTrue(movementExceedsThreshold(-101f, 0f, threshold))
        assertTrue(movementExceedsThreshold(101f, 0f, threshold))
        assertTrue(movementExceedsThreshold(75f, 75f, threshold))
    }

    @Test
    fun smallRestingFingerMovementDoesNotTriggerNavigation() {
        assertFalse(movementExceedsThreshold(20f, 15f, 100f))
    }

    @Test
    fun shortMovementCountsAsTap() {
        assertTrue(isTapMovement(10f, 10f, 24f))
        assertFalse(isTapMovement(30f, 0f, 24f))
    }

    @Test
    fun wrapsForwardAfterLastSlide() {
        assertEquals(0, wrapIndex(current = 58, delta = 1, size = 59))
    }

    @Test
    fun repeatedNavigationRemainsInRange() {
        var index = 0
        repeat(10_000) { index = wrapIndex(index, 1, 59) }
        assertEquals(10_000 % 59, index)
    }

    @Test
    fun firstManualNavigationIsImmediate() {
        assertTrue(manualNavigationAllowed(0L, -1L))
        assertTrue(manualNavigationAllowed(10L, -1L))
    }

    @Test
    fun manualCooldownRejectsEarlyGesturesAndAcceptsBoundary() {
        assertFalse(manualNavigationAllowed(2999L, 0L))
        assertTrue(manualNavigationAllowed(3000L, 0L))
        assertTrue(manualNavigationAllowed(3001L, 0L))
        assertFalse(manualNavigationAllowed(5999L, 3000L))
        assertTrue(manualNavigationAllowed(6000L, 3000L))
    }

    @Test
    fun ignoredGestureDoesNotExtendCooldown() {
        val lastAcceptedAt = 1000L
        assertFalse(manualNavigationAllowed(2000L, lastAcceptedAt))
        assertFalse(manualNavigationAllowed(3500L, lastAcceptedAt))
        assertTrue(manualNavigationAllowed(4000L, lastAcceptedAt))
    }

    @Test
    fun clockRegressionIsRejected() {
        assertFalse(manualNavigationAllowed(1000L, 2000L))
    }

    @Test
    fun swipeThresholdBoundaryAndStationaryTouch() {
        assertFalse(movementExceedsThreshold(0f, 0f, 48f))
        assertFalse(movementExceedsThreshold(47f, 0f, 48f))
        assertTrue(movementExceedsThreshold(48f, 0f, 48f))
        assertTrue(movementExceedsThreshold(0f, -48f, 48f))
    }

    @Test
    fun autoAdvanceIntervalIsThirtySeconds() {
        assertEquals(30_000L, AUTO_ADVANCE_MS)
    }
}
