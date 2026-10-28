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
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
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
    val tapSlopPx = with(density) { 24.dp.toPx() }
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
        currentIndex = wrapIndex(currentIndex, delta, slides.size)
        lastChangeAt = SystemClock.elapsedRealtime()
    }

    // Every successful manual navigation and every automatic transition changes currentIndex,
    // so this coroutine restarts from 30 seconds. A center tap changes no state and therefore
    // does not reset the timer.
    LaunchedEffect(currentIndex, isForeground, slides.size) {
        if (isForeground && slides.isNotEmpty()) {
            delay(AUTO_ADVANCE_MS)
            move(1)
        }
    }

    // Decode only approximately as many pixels as the screen can display. The mutex prevents
    // rapid navigation from running multiple expensive decodes at the same time.
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
            .pointerInput(slides.size, swipeThresholdPx, tapSlopPx, targetWidthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startedAt = SystemClock.elapsedRealtime()
                    var dragX = 0f
                    var dragY = 0f
                    var completedNormally = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.positionChange()
                        dragX += delta.x
                        dragY += delta.y
                        if (delta.x != 0f || delta.y != 0f) {
                            change.consume()
                        }
                        if (!change.pressed) {
                            completedNormally = true
                            break
                        }
                    }

                    if (completedNormally && slides.isNotEmpty()) {
                        val now = SystemClock.elapsedRealtime()
                        val action = classifyGesture(
                            dragX = dragX,
                            dragY = dragY,
                            elapsedMs = now - startedAt,
                            startX = startX,
                            screenWidthPx = targetWidthPx,
                            swipeThresholdPx = swipeThresholdPx,
                            tapSlopPx = tapSlopPx,
                            cooldownPassed = now - lastChangeAt >= SWIPE_COOLDOWN_MS
                        )
                        when (action) {
                            GestureAction.NEXT -> move(1)
                            GestureAction.PREVIOUS -> move(-1)
                            GestureAction.NONE -> Unit
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
