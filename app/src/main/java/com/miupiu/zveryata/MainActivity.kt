package com.miupiu.zveryata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val AUTO_ADVANCE_MS = 30_000L
private const val SWIPE_COOLDOWN_MS = 350L
private const val MIN_SWIPE_MS = 90L
private const val MAX_SWIPE_MS = 900L
private const val AXIS_DOMINANCE = 1.8f
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

@Composable
private fun ZveryataGallery() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }
    val displayMetrics = context.resources.displayMetrics
    val targetWidthPx = displayMetrics.widthPixels.coerceAtLeast(1)
    val targetHeightPx = displayMetrics.heightPixels.coerceAtLeast(1)
    val decodeMutex = remember { Mutex() }

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
    var isForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun move(delta: Int) {
        if (slides.isEmpty()) return
        currentIndex = (currentIndex + delta + slides.size) % slides.size
        lastChangeAt = SystemClock.elapsedRealtime()
    }

    // currentIndex changes after every valid manual or automatic transition, so this
    // coroutine is cancelled/restarted. Taps do not change state and therefore do not
    // reset the 30-second timer.
    LaunchedEffect(currentIndex, isForeground, slides.size) {
        if (isForeground && slides.isNotEmpty()) {
            delay(AUTO_ADVANCE_MS)
            move(1)
        }
    }

    // Decode only approximately as many pixels as the screen can display. The previous
    // version decoded every original WebP at full resolution; repeated swipes could leave
    // several very large bitmaps waiting for GC and exhaust the app heap. The mutex also
    // guarantees that rapid swipes cannot run several expensive decodes simultaneously.
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        currentIndex,
        slides,
        targetWidthPx,
        targetHeightPx
    ) {
        value = if (slides.isEmpty()) {
            null
        } else {
            val assetPath = "images/${slides[currentIndex]}"
            withContext(Dispatchers.IO) {
                decodeMutex.withLock {
                    decodeBitmapForScreen(context, assetPath, targetWidthPx, targetHeightPx)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .pointerInput(slides.size, swipeThresholdPx) {
                var dragX = 0f
                var dragY = 0f
                var dragStartedAt = 0L

                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                        dragStartedAt = SystemClock.elapsedRealtime()
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                        dragStartedAt = 0L
                    },
                    onDragEnd = {
                        val now = SystemClock.elapsedRealtime()
                        val elapsed = now - dragStartedAt
                        val absX = abs(dragX)
                        val absY = abs(dragY)
                        val horizontal = absX >= absY * AXIS_DOMINANCE
                        val vertical = absY >= absX * AXIS_DOMINANCE
                        val distance = if (horizontal) absX else if (vertical) absY else 0f
                        val validDuration = elapsed in MIN_SWIPE_MS..MAX_SWIPE_MS
                        val cooldownPassed = now - lastChangeAt >= SWIPE_COOLDOWN_MS

                        if (
                            slides.isNotEmpty() &&
                            validDuration &&
                            cooldownPassed &&
                            distance >= swipeThresholdPx
                        ) {
                            when {
                                horizontal -> move(if (dragX < 0f) 1 else -1)
                                vertical -> move(if (dragY < 0f) 1 else -1)
                            }
                        }

                        dragX = 0f
                        dragY = 0f
                        dragStartedAt = 0L
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    }
                )
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
