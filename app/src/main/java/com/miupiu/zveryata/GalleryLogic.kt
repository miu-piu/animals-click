package com.miupiu.zveryata

import kotlin.math.abs

internal const val AUTO_ADVANCE_MS = 30_000L
internal const val SWIPE_COOLDOWN_MS = 350L
internal const val MIN_SWIPE_MS = 90L
internal const val MAX_SWIPE_MS = 900L
internal const val MAX_TAP_MS = 500L
internal const val AXIS_DOMINANCE = 1.8f
internal const val EDGE_TAP_FRACTION = 0.35f

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

internal fun classifyGesture(
    dragX: Float,
    dragY: Float,
    elapsedMs: Long,
    startX: Float,
    screenWidthPx: Int,
    swipeThresholdPx: Float,
    tapSlopPx: Float,
    cooldownPassed: Boolean
): GestureAction {
    if (!cooldownPassed || screenWidthPx <= 0) return GestureAction.NONE

    val absX = abs(dragX)
    val absY = abs(dragY)
    val horizontal = absX >= absY * AXIS_DOMINANCE
    val vertical = absY >= absX * AXIS_DOMINANCE
    val swipeDistance = when {
        horizontal -> absX
        vertical -> absY
        else -> 0f
    }

    if (
        elapsedMs in MIN_SWIPE_MS..MAX_SWIPE_MS &&
        swipeDistance >= swipeThresholdPx
    ) {
        return when {
            horizontal && dragX < 0f -> GestureAction.NEXT
            horizontal -> GestureAction.PREVIOUS
            vertical && dragY < 0f -> GestureAction.NEXT
            vertical -> GestureAction.PREVIOUS
            else -> GestureAction.NONE
        }
    }

    val tapDistanceSquared = dragX * dragX + dragY * dragY
    val tapSlopSquared = tapSlopPx * tapSlopPx
    if (elapsedMs in 0..MAX_TAP_MS && tapDistanceSquared <= tapSlopSquared) {
        val leftEdge = screenWidthPx * EDGE_TAP_FRACTION
        val rightEdge = screenWidthPx * (1f - EDGE_TAP_FRACTION)
        return when {
            startX < leftEdge -> GestureAction.PREVIOUS
            startX > rightEdge -> GestureAction.NEXT
            else -> GestureAction.NONE
        }
    }

    return GestureAction.NONE
}
