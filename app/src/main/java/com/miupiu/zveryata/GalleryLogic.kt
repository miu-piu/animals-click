package com.miupiu.zveryata

import kotlin.math.abs

internal const val AUTO_ADVANCE_MS = 15_000L
internal const val SWIPE_COOLDOWN_MS = 400L
internal const val MIN_SWIPE_MS = 70L
internal const val MAX_SWIPE_MS = 1_500L
internal const val MAX_TAP_MS = 500L
internal const val AXIS_DOMINANCE = 1.6f

internal enum class GestureAction {
    NEXT,
    PREVIOUS,
    NONE
}

internal fun wrapIndex(current: Int, delta: Int, size: Int): Int {
    require(size > 0)
    val raw = (current + delta) % size
    return if (raw < 0) raw + size else raw
}

internal fun classifyHorizontalSwipe(
    dragX: Float,
    dragY: Float,
    elapsedMs: Long,
    swipeThresholdPx: Float,
    cooldownPassed: Boolean
): GestureAction {
    if (!cooldownPassed) return GestureAction.NONE

    val absX = abs(dragX)
    val absY = abs(dragY)
    val horizontal = absX >= absY * AXIS_DOMINANCE

    if (
        elapsedMs in MIN_SWIPE_MS..MAX_SWIPE_MS &&
        horizontal &&
        absX >= swipeThresholdPx
    ) {
        return if (dragX < 0f) GestureAction.NEXT else GestureAction.PREVIOUS
    }

    return GestureAction.NONE
}

internal fun isTap(
    dragX: Float,
    dragY: Float,
    elapsedMs: Long,
    tapSlopPx: Float
): Boolean {
    if (elapsedMs !in 0..MAX_TAP_MS) return false
    return dragX * dragX + dragY * dragY <= tapSlopPx * tapSlopPx
}
