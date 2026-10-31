package com.miupiu.zveryata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val AppBlack = Color.Black

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(background = AppBlack, surface = AppBlack)
            ) {
                ZveryataGallery()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

private data class PointerTrack(
    val startedAt: Long,
    var drag: Offset = Offset.Zero,
    var navigated: Boolean = false
)

@Composable
private fun ZveryataGallery() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }
    val tapSlopPx = with(density) { 24.dp.toPx() }
    val exclusionWidthPx = with(density) { 24.dp.roundToPx() }
    val exclusionHeightPx = with(density) { 180.dp.roundToPx() }
    val displayMetrics = context.resources.displayMetrics
    val targetWidthPx = displayMetrics.widthPixels.coerceAtLeast(1)
    val targetHeightPx = displayMetrics.heightPixels.coerceAtLeast(1)
    val decodeMutex = remember { Mutex() }
    val bitmapCache = remember { LruCache<String, Bitmap>(3) }

    val slides = remember {
        context.assets.list("images")
            ?.asSequence()
            ?.filter { it.endsWith(".webp", ignoreCase = true) }
            ?.sorted()
            ?.toList()
            .orEmpty()
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastChangeAt by remember { mutableLongStateOf(0L) }
    var interactionEpoch by remember { mutableLongStateOf(0L) }
    var isForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isForeground = true
                    interactionEpoch++
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Android deliberately keeps system navigation recoverable. Exclude only two small central
    // edge strips so ordinary gallery swipes near the sides are less likely to become Back,
    // without pretending that the app can disable system gestures or screen-unpin gestures.
    DisposableEffect(view, exclusionWidthPx, exclusionHeightPx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val width = view.width.takeIf { it > 0 } ?: targetWidthPx
            val height = view.height.takeIf { it > 0 } ?: targetHeightPx
            val top = ((height - exclusionHeightPx) / 2).coerceAtLeast(0)
            val bottom = (top + exclusionHeightPx).coerceAtMost(height)
            view.systemGestureExclusionRects = listOf(
                Rect(0, top, exclusionWidthPx.coerceAtMost(width), bottom),
                Rect((width - exclusionWidthPx).coerceAtLeast(0), top, width, bottom)
            )
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.systemGestureExclusionRects = emptyList()
            }
        }
    }

    fun registerInteraction() {
        interactionEpoch++
    }

    fun move(delta: Int) {
        if (slides.isEmpty()) return
        currentIndex = wrapIndex(currentIndex, delta, slides.size)
        lastChangeAt = SystemClock.elapsedRealtime()
        registerInteraction()
    }

    // Exactly one auto-advance coroutine exists for the current interaction epoch. A tap, swipe,
    // successful navigation, resume, or index change cancels the previous delay and starts a fresh
    // 15-second interval. Pausing removes the active timer entirely.
    LaunchedEffect(currentIndex, interactionEpoch, isForeground, slides.size) {
        if (isForeground && slides.isNotEmpty()) {
            delay(AUTO_ADVANCE_MS)
            move(1)
        }
    }

    // Keep only the current and nearby decoded images hot. Decoding happens off the main thread,
    // is serialized to avoid memory spikes, and is never performed from a pointer MOVE event.
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        currentIndex,
        slides,
        targetWidthPx,
        targetHeightPx
    ) {
        if (slides.isEmpty()) {
            value = null
            return@produceState
        }

        suspend fun load(index: Int): Bitmap {
            val name = slides[index]
            bitmapCache.get(name)?.let { return it }
            return withContext(Dispatchers.IO) {
                decodeMutex.withLock {
                    bitmapCache.get(name) ?: decodeBitmapForScreen(
                        context,
                        "images/$name",
                        targetWidthPx,
                        targetHeightPx
                    ).also { bitmapCache.put(name, it) }
                }
            }
        }

        value = load(currentIndex)
        if (slides.size > 1) {
            load(wrapIndex(currentIndex, 1, slides.size))
            load(wrapIndex(currentIndex, -1, slides.size))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .pointerInput(slides.size, swipeThresholdPx, tapSlopPx) {
                awaitPointerEventScope {
                    val tracks = mutableMapOf<PointerId, PointerTrack>()

                    while (true) {
                        val event = awaitPointerEvent()
                        val now = SystemClock.elapsedRealtime()

                        for (change in event.changes) {
                            val track = tracks[change.id]

                            if (change.pressed && track == null) {
                                tracks[change.id] = PointerTrack(startedAt = now)
                                continue
                            }

                            if (track != null) {
                                val delta = change.positionChange()
                                track.drag += delta

                                if (change.pressed && !track.navigated && slides.isNotEmpty()) {
                                    val action = classifyHorizontalSwipe(
                                        dragX = track.drag.x,
                                        dragY = track.drag.y,
                                        elapsedMs = now - track.startedAt,
                                        swipeThresholdPx = swipeThresholdPx,
                                        cooldownPassed = now - lastChangeAt >= SWIPE_COOLDOWN_MS
                                    )
                                    when (action) {
                                        GestureAction.NEXT -> {
                                            track.navigated = true
                                            move(1)
                                            change.consume()
                                        }
                                        GestureAction.PREVIOUS -> {
                                            track.navigated = true
                                            move(-1)
                                            change.consume()
                                        }
                                        GestureAction.NONE -> Unit
                                    }
                                }

                                if (!change.pressed) {
                                    if (
                                        !track.navigated &&
                                        isTap(
                                            dragX = track.drag.x,
                                            dragY = track.drag.y,
                                            elapsedMs = now - track.startedAt,
                                            tapSlopPx = tapSlopPx
                                        )
                                    ) {
                                        registerInteraction()
                                    }
                                    tracks.remove(change.id)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun decodeBitmapForScreen(
    context: android.content.Context,
    assetPath: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.assets, assetPath)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val sourceWidth = info.size.width.coerceAtLeast(1)
            val sourceHeight = info.size.height.coerceAtLeast(1)
            val scale = min(
                reqWidth.toFloat() / sourceWidth,
                reqHeight.toFloat() / sourceHeight
            ).coerceAtMost(1f)
            val targetWidth = max(1, (sourceWidth * scale).roundToInt())
            val targetHeight = max(1, (sourceHeight * scale).roundToInt())
            decoder.setTargetSize(targetWidth, targetHeight)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(assetPath).use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }

    var sampleSize = 1
    while (
        bounds.outWidth / (sampleSize * 2) >= reqWidth &&
        bounds.outHeight / (sampleSize * 2) >= reqHeight
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = context.assets.open(assetPath).use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
            ?: error("Could not decode $assetPath")
    }

    val scale = min(
        reqWidth.toFloat() / decoded.width.coerceAtLeast(1),
        reqHeight.toFloat() / decoded.height.coerceAtLeast(1)
    ).coerceAtMost(1f)
    if (scale >= 0.999f) return decoded

    val scaled = Bitmap.createScaledBitmap(
        decoded,
        max(1, (decoded.width * scale).roundToInt()),
        max(1, (decoded.height * scale).roundToInt()),
        true
    )
    if (scaled !== decoded) decoded.recycle()
    return scaled
}
