package com.miupiu.zveryata

import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color.Black
private val CaptionWhite = Color(0xFFF2F2F2)

private class TouchTrack(
    val start: Offset,
    val beganBlocked: Boolean,
    var handled: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Ink)) {
                AnimalGallery()
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
private fun AnimalGallery() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    val cards = remember(context) { loadPreviewCards(context) }
    val photoStore = remember(context) { BitmapStore(context.applicationContext) }

    var requestedIndex by rememberSaveable { mutableIntStateOf(0) }
    var shownIndex by remember { mutableIntStateOf(0) }
    var visibleImage by remember { mutableStateOf<Bitmap?>(null) }
    var photoWidth by remember { mutableIntStateOf(0) }
    var photoHeight by remember { mutableIntStateOf(0) }
    var transitionPending by remember { mutableStateOf(false) }
    var lastManualAt by remember { mutableLongStateOf(-1L) }
    var isForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

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

    fun requestMove(delta: Int, manual: Boolean): Boolean {
        if (!isForeground || transitionPending || cards.isEmpty()) return false
        val now = SystemClock.elapsedRealtime()
        if (manual && !manualNavigationAllowed(now, lastManualAt)) return false
        if (manual) lastManualAt = now
        transitionPending = true
        requestedIndex = wrapIndex(shownIndex, delta, cards.size)
        return true
    }

    val swipeHandler = rememberUpdatedState<(Int) -> Boolean> { step ->
        requestMove(step, manual = true)
    }
    val lastManualState = rememberUpdatedState(lastManualAt)
    val pendingState = rememberUpdatedState(transitionPending)
    val foregroundState = rememberUpdatedState(isForeground)

    // Keep the old image visible until the next bitmap has been decoded.
    LaunchedEffect(requestedIndex, photoWidth, photoHeight) {
        if (photoWidth <= 0 || photoHeight <= 0) return@LaunchedEffect
        try {
            val next = photoStore.load(cards[requestedIndex].asset, photoWidth, photoHeight)
            visibleImage = next
            shownIndex = requestedIndex
            transitionPending = false
            if (cards.size > 1) {
                launch { runCatching {
                    photoStore.load(cards[wrapIndex(shownIndex, 1, cards.size)].asset, photoWidth, photoHeight)
                } }
                launch { runCatching {
                    photoStore.load(cards[wrapIndex(shownIndex, -1, cards.size)].asset, photoWidth, photoHeight)
                } }
            }
        } catch (error: Exception) {
            Log.e("Zveryata", "Failed to decode slide", error)
            transitionPending = false
        }
    }

    // Only a completed transition or lifecycle return restarts the 30-second clock.
    LaunchedEffect(shownIndex, isForeground, transitionPending, visibleImage) {
        if (isForeground && visibleImage != null && !transitionPending && cards.size > 1) {
            delay(AUTO_ADVANCE_MS)
            requestMove(1, manual = false)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Ink)
            .pointerInput(swipeThresholdPx) {
                val fingers = mutableMapOf<PointerId, TouchTrack>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (!change.previousPressed && change.pressed) {
                                val beganBlocked = !foregroundState.value ||
                                    !manualNavigationAllowed(
                                        SystemClock.elapsedRealtime(),
                                        lastManualState.value
                                    ) || pendingState.value
                                fingers[change.id] = TouchTrack(change.position, beganBlocked)
                            }
                            val finger = fingers[change.id]
                            if (finger != null && !finger.handled &&
                                (change.pressed || change.previousPressed)) {
                                val displacement = change.position - finger.start
                                val step = swipeDelta(
                                    displacement.x, displacement.y, swipeThresholdPx
                                )
                                if (step != null) {
                                    finger.handled = true
                                    if (!finger.beganBlocked && swipeHandler.value(step)) {
                                        change.consume()
                                    }
                                }
                            }
                            if (!change.pressed) fingers.remove(change.id)
                        }
                    }
                }
            }
    ) {
        // On Galaxy S26 Ultra QHD+, available screen height minus 9:16 picture = 560 px.
        // For any other aspect ratio, keep the caption centered and let the image area adapt.
        val naturalHeader = maxHeight - maxWidth * (16f / 9f)
        val headerHeight = naturalHeader.coerceAtLeast(104.dp)
            .coerceAtMost(maxHeight * 0.27f)

        val screenWidth = maxWidth
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(headerHeight).background(Ink),
                contentAlignment = Alignment.Center
            ) {
                val caption = cards[shownIndex].label
                var fontSize by remember(caption, headerHeight, screenWidth) {
                    mutableFloatStateOf(32f)
                }
                Text(
                    text = caption,
                    color = CaptionWhite,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.17f).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true,
                    onTextLayout = { layout ->
                        if (layout.hasVisualOverflow && fontSize > 18f) {
                            fontSize = (fontSize - 2f).coerceAtLeast(18f)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Ink)
                    .onSizeChanged { size ->
                        photoWidth = size.width
                        photoHeight = size.height
                    },
                contentAlignment = Alignment.Center
            ) {
                val image = visibleImage
                if (image != null) {
                    // An enlarged natural backdrop fills unusual aspect ratios;
                    // the sharp fitted image preserves the entire animal silhouette.
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = cards[shownIndex].label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
