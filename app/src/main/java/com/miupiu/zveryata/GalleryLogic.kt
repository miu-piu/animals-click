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

/**
 * Pure touch-state tracker. Each pointer is independent, so a stationary finger
 * cannot prevent another finger from swiping. A movement may emit at most once.
 */
internal class TouchRouter(private val thresholdPx: Float) {
    private data class Start(
        val x: Float,
        val y: Float,
        val blocked: Boolean,
        var finished: Boolean = false
    )
    private val fingers = mutableMapOf<Long, Start>()

    fun press(pointer: Long, x: Float, y: Float, beganBlocked: Boolean) {
        fingers[pointer] = Start(x, y, beganBlocked)
    }

    fun move(pointer: Long, x: Float, y: Float): Int? {
        val start = fingers[pointer] ?: return null
        if (start.finished) return null
        val direction = swipeDelta(x - start.x, y - start.y, thresholdPx) ?: return null
        start.finished = true
        return if (start.blocked) null else direction
    }

    fun release(pointer: Long) {
        fingers.remove(pointer)
    }

    fun clear() {
        fingers.clear()
    }
}

/**
 * Reserve a caption band without hard-coded screen pixels.
 * Both dimensions must be measured within the immersive app viewport in dp.
 * At 1440x3120 and 1080x2340, this reserves exactly 560px and 420px.
 */
internal fun adaptiveHeaderHeightDp(
    widthDp: Float, heightDp: Float, fontScale: Float = 1f
): Float {
    require(widthDp > 0f && heightDp > 0f && fontScale > 0f)
    val preferredMaximum = heightDp * 0.27f
    val minimum = minOf(104f, preferredMaximum)
    val ideal = heightDp - widthDp * (16f / 9f)
    val defaultBand = ideal.coerceIn(minimum, preferredMaximum)
    // A two-line fallback at 18sp must still fit at accessibility font scales.
    val accessibilityFloor = 2f * 18f * 1.17f * fontScale + 24f
    return maxOf(defaultBand, accessibilityFloor).coerceAtMost(heightDp * 0.35f)
}
