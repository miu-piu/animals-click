package com.miupiu.zveryata

import kotlin.math.abs

internal const val AUTO_ADVANCE_MS = 30_000L
internal const val MANUAL_COOLDOWN_MS = 3_000L
internal const val SWIPE_THRESHOLD_DP = 56
internal const val AXIS_DOMINANCE = 1.6f

/** Swipe left/up advances; right/down returns. Ambiguous diagonals and jitters are ignored. */
internal fun swipeDelta(dx: Float, dy: Float, thresholdPx: Float): Int? {
    val ax = abs(dx)
    val ay = abs(dy)
    if (ax < thresholdPx && ay < thresholdPx) return null
    return when {
        ax >= thresholdPx && ax >= ay * AXIS_DOMINANCE -> if (dx < 0f) 1 else -1
        ay >= thresholdPx && ay >= ax * AXIS_DOMINANCE -> if (dy < 0f) 1 else -1
        else -> null
    }
}

internal fun wrapIndex(current: Int, delta: Int, size: Int): Int {
    require(size > 0)
    return Math.floorMod(current + delta, size)
}

/** A rejected swipe never moves the timestamp, so it cannot extend the cooldown. */
internal fun manualNavigationAllowed(nowMs: Long, lastAcceptedAtMs: Long): Boolean =
    lastAcceptedAtMs < 0L ||
        (nowMs >= lastAcceptedAtMs && nowMs - lastAcceptedAtMs >= MANUAL_COOLDOWN_MS)

/** Refuse a gesture which began during the cooldown even if it finishes after it. */
internal fun gestureCanNavigate(
    beganBlocked: Boolean,
    nowMs: Long,
    lastAcceptedAtMs: Long,
    transitionInProgress: Boolean
): Boolean = !beganBlocked && !transitionInProgress && manualNavigationAllowed(nowMs, lastAcceptedAtMs)
