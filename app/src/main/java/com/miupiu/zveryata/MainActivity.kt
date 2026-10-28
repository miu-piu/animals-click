package com.miupiu.zveryata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.input.pointer.consume
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
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val AUTO_ADVANCE_MS = 30_000L
private const val SWIPE_COOLDOWN_MS = 400L
private val AppBlack = Color(0xFF000000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppBlack,
                    surface = AppBlack
                )
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
    val swipeThresholdPx = with(density) { 64.dp.toPx() }

    val slides = remember {
        context.assets.list("images")
            ?.asSequence()
            ?.filter { it.endsWith(".webp", ignoreCase = true) }
            ?.sorted()
            ?.toList()
            .orEmpty()
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastSwipeAt by remember { mutableLongStateOf(0L) }
    var isForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun move(delta: Int) {
        if (slides.isEmpty()) return
        currentIndex = (currentIndex + delta + slides.size) % slides.size
    }

    LaunchedEffect(currentIndex, isForeground, slides.size) {
        if (isForeground && slides.isNotEmpty()) {
            delay(AUTO_ADVANCE_MS)
            move(1)
        }
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, currentIndex, slides) {
        value = if (slides.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                context.assets.open("images/${slides[currentIndex]}").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }
    }

    var dragX = 0f
    var dragY = 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .pointerInput(slides.size, swipeThresholdPx) {
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        val now = SystemClock.elapsedRealtime()
                        val absX = abs(dragX)
                        val absY = abs(dragY)
                        val enoughDistance = maxOf(absX, absY) >= swipeThresholdPx
                        val cooldownPassed = now - lastSwipeAt >= SWIPE_COOLDOWN_MS

                        if (enoughDistance && cooldownPassed && slides.isNotEmpty()) {
                            val horizontal = absX >= absY * 1.35f
                            val vertical = absY >= absX * 1.35f

                            when {
                                horizontal -> {
                                    lastSwipeAt = now
                                    move(if (dragX < 0f) 1 else -1)
                                }
                                vertical -> {
                                    lastSwipeAt = now
                                    move(if (dragY < 0f) 1 else -1)
                                }
                            }
                        }

                        dragX = 0f
                        dragY = 0f
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
