package com.miupiu.zveryata

import org.junit.Assert.*
import org.junit.Test

class GalleryLogicTest {
    @Test fun directionsAreConsistent() {
        assertEquals(1, swipeDelta(-60f, 0f, 56f))
        assertEquals(-1, swipeDelta(60f, 0f, 56f))
        assertEquals(1, swipeDelta(0f, -60f, 56f))
        assertEquals(-1, swipeDelta(0f, 60f, 56f))
    }

    @Test fun thresholdBoundaryIsInclusive() {
        assertNull(swipeDelta(55.99f, 0f, 56f))
        assertEquals(-1, swipeDelta(56f, 0f, 56f))
        assertNull(swipeDelta(0f, 0f, 56f))
    }

    @Test fun ambiguousAndSmallDiagonalGesturesAreRejected() {
        assertNull(swipeDelta(60f, 60f, 56f))
        assertNull(swipeDelta(-100f, 95f, 56f))
        assertNull(swipeDelta(30f, 20f, 56f))
        assertEquals(1, swipeDelta(-100f, 40f, 56f))
    }

    @Test fun firstGestureHasNoArtificialDelay() {
        assertTrue(manualNavigationAllowed(0L, -1L))
        assertTrue(gestureCanNavigate(false, 0L, -1L, false))
    }

    @Test fun threeSecondCooldownAndExactBoundary() {
        assertFalse(manualNavigationAllowed(2999L, 0L))
        assertTrue(manualNavigationAllowed(3000L, 0L))
        assertFalse(manualNavigationAllowed(5999L, 3000L))
        assertTrue(manualNavigationAllowed(6000L, 3000L))
    }

    @Test fun cooldownStartsFromAcceptedGestureOnly() {
        val accepted = 1000L
        assertFalse(manualNavigationAllowed(2000L, accepted))
        assertFalse(manualNavigationAllowed(3500L, accepted))
        assertTrue(manualNavigationAllowed(4000L, accepted))
    }

    @Test fun gesturesStartedDuringCooldownNeverFireLater() {
        assertFalse(gestureCanNavigate(true, 5000L, 1000L, false))
        assertFalse(gestureCanNavigate(false, 5000L, 1000L, true))
        assertTrue(gestureCanNavigate(false, 5000L, 1000L, false))
    }

    @Test fun clockMovingBackwardIsIgnored() {
        assertFalse(manualNavigationAllowed(100L, 3000L))
    }

    @Test fun bothEndsWrapAndBulkNavigationStaysValid() {
        assertEquals(0, wrapIndex(141, 1, 142))
        assertEquals(141, wrapIndex(0, -1, 142))
        assertEquals(1, wrapIndex(0, 143, 142))
        var index = 0
        repeat(10_000) { index = wrapIndex(index, 1, 142) }
        assertEquals(10_000 % 142, index)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyGalleryIsRejected() {
        wrapIndex(0, 1, 0)
    }

    @Test fun automaticAdvanceIsThirtySeconds() {
        assertEquals(30_000L, AUTO_ADVANCE_MS)
    }
}
