package com.miupiu.zveryata

internal const val AUTO_ADVANCE_MS = 30_000L
internal const val SWIPE_THRESHOLD_DP = 48
internal const val TAP_SLOP_DP = 24

internal fun wrapIndex(current: Int, delta: Int, size: Int): Int {
    require(size > 0)
    val raw = (current + delta) % size
    return if (raw < 0) raw + size else raw
}

internal fun movementExceedsThreshold(
    deltaX: Float,
    deltaY: Float,
    thresholdPx: Float
): Boolean = deltaX * deltaX + deltaY * deltaY >= thresholdPx * thresholdPx

internal fun isTapMovement(
    deltaX: Float,
    deltaY: Float,
    tapSlopPx: Float
): Boolean = deltaX * deltaX + deltaY * deltaY <= tapSlopPx * tapSlopPx
