package com.miupiu.zveryata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val LabelColor = Color(0xFFF2F2F2)

private data class Slide(val assetName: String, val label: String)
private data class PointerTrack(val start: Offset, var triggered: Boolean = false)

private val labelsByAsset = mapOf(
    "01-arich.webp" to "Ари", "02-kitten.webp" to "котёнок", "03-irish-setter.webp" to "собака",
    "04-rabbit.webp" to "кролик", "05-calf.webp" to "телёнок", "06-goat.webp" to "коза",
    "07-lamb.webp" to "ягнёнок", "08-chick.webp" to "цыплёнок", "09-duckling.webp" to "утёнок",
    "10-hedgehog.webp" to "ёж", "11-squirrel.webp" to "белка", "12-ferret.webp" to "хорёк",
    "13-fox.webp" to "лиса", "14-raccoon.webp" to "енот", "15-fawn.webp" to "оленёнок",
    "16-bear.webp" to "медведь", "17-lynx.webp" to "рысь", "18-chipmunk.webp" to "бурундук",
    "19-amur-leopard.webp" to "леопард", "20-tiger.webp" to "тигр", "21-lion.webp" to "лев",
    "22-elephant.webp" to "слон", "23-giraffe.webp" to "жираф", "24-zebra.webp" to "зебра",
    "25-camel.webp" to "верблюд", "26-capybara.webp" to "капибара", "27-alpaca.webp" to "альпака",
    "28-panda.webp" to "панда", "29-hippo.webp" to "бегемот", "30-rhino.webp" to "носорог",
    "31-tit.webp" to "синица", "32-owl.webp" to "сова", "33-crow.webp" to "ворона",
    "34-magpie.webp" to "сорока", "35-pigeon.webp" to "голубь", "36-flamingo.webp" to "фламинго",
    "37-hummingbird.webp" to "колибри", "38-cuckoo.webp" to "кукушка", "39-peacock.webp" to "павлин",
    "40-swan.webp" to "лебедь", "41-whale.webp" to "кит", "42-shark.webp" to "акула",
    "43-penguin.webp" to "пингвин", "44-seal.webp" to "тюлень", "45-dolphin.webp" to "дельфин",
    "46-octopus.webp" to "осьминог", "47-seahorse.webp" to "морской конёк", "48-crab.webp" to "краб",
    "49-ray.webp" to "скат", "50-walrus.webp" to "морж", "51-jellyfish.webp" to "медуза",
    "52-snake.webp" to "змея", "53-lizard.webp" to "ящерица", "54-butterfly.webp" to "бабочка",
    "55-bee.webp" to "пчела", "56-snail.webp" to "улитка", "57-lemur.webp" to "лемур",
    "58-dragonfly.webp" to "стрекоза", "59-ladybird.webp" to "божья коровка"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = AppBlack, surface = AppBlack)) {
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
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    val tapSlopPx = with(density) { TAP_SLOP_DP.dp.toPx() }
    val displayMetrics = context.resources.displayMetrics
    val targetWidthPx = displayMetrics.widthPixels.coerceAtLeast(1)
    val targetHeightPx = displayMetrics.heightPixels.coerceAtLeast(1)
    val decodeMutex = remember { Mutex() }

    val slides = remember {
        context.assets.list("images")?.asSequence()
            ?.filter { it.endsWith(".webp", ignoreCase = true) }
            ?.sorted()
            ?.map { Slide(it, labelsByAsset[it] ?: it.substringAfter('-').substringBeforeLast('.')) }
            ?.toList().orEmpty()
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var timerGeneration by remember { mutableIntStateOf(0) }
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

    fun nextSlide() {
        if (slides.isEmpty()) return
        currentIndex = wrapIndex(currentIndex, 1, slides.size)
        timerGeneration++
    }

    LaunchedEffect(timerGeneration, isForeground, slides.size) {
        if (isForeground && slides.isNotEmpty()) {
            delay(AUTO_ADVANCE_MS)
            nextSlide()
        }
    }

    val bitmap by produceState<Bitmap?>(null, currentIndex, slides, targetWidthPx, targetHeightPx) {
        value = if (slides.isEmpty()) null else {
            val assetPath = "images/${slides[currentIndex].assetName}"
            withContext(Dispatchers.IO) {
                decodeMutex.withLock { decodeBitmapForScreen(context, assetPath, targetWidthPx, targetHeightPx) }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppBlack)
            .pointerInput(slides.size, swipeThresholdPx, tapSlopPx) {
                val pointers = mutableMapOf<PointerId, PointerTrack>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (!change.previousPressed && change.pressed) {
                                pointers[change.id] = PointerTrack(change.position)
                            }
                            val track = pointers[change.id]
                            if (track != null && change.pressed && !track.triggered) {
                                val delta = change.position - track.start
                                if (movementExceedsThreshold(delta.x, delta.y, swipeThresholdPx)) {
                                    track.triggered = true
                                    nextSlide()
                                    change.consume()
                                }
                            }
                            if (change.previousPressed && !change.pressed) {
                                if (track != null && !track.triggered) {
                                    val delta = change.position - track.start
                                    if (isTapMovement(delta.x, delta.y, tapSlopPx)) {
                                        track.triggered = true
                                        nextSlide()
                                        change.consume()
                                    }
                                }
                                pointers.remove(change.id)
                            }
                        }
                    }
                }
            }
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(), contentDescription = null,
                modifier = Modifier.fillMaxSize(), alignment = Alignment.BottomCenter,
                contentScale = ContentScale.Fit
            )

            val renderedHeightPx = min(
                targetHeightPx.toFloat(),
                targetWidthPx.toFloat() * image.height / image.width.coerceAtLeast(1).toFloat()
            )
            val freeHeightDp = with(density) { (targetHeightPx - renderedHeightPx).coerceAtLeast(0f).toDp() }
            val screenWidthDp = with(density) { targetWidthPx.toDp() }
            val labelSize = when {
                screenWidthDp >= 840.dp -> 36.sp
                screenWidthDp >= 600.dp -> 32.sp
                else -> 28.sp
            }

            if (freeHeightDp >= 36.dp && slides.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(freeHeightDp).align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slides[currentIndex].label,
                        color = LabelColor,
                        fontSize = labelSize,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
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
            val scale = min(reqWidth.toFloat() / sourceWidth, reqHeight.toFloat() / sourceHeight).coerceAtMost(1f)
            decoder.setTargetSize(max(1, (sourceWidth * scale).roundToInt()), max(1, (sourceHeight * scale).roundToInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(assetPath).use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= reqWidth && bounds.outHeight / (sampleSize * 2) >= reqHeight) sampleSize *= 2

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = context.assets.open(assetPath).use { stream ->
        BitmapFactory.decodeStream(stream, null, options) ?: error("Could not decode $assetPath")
    }
    val scale = min(reqWidth.toFloat() / decoded.width.coerceAtLeast(1), reqHeight.toFloat() / decoded.height.coerceAtLeast(1)).coerceAtMost(1f)
    if (scale >= 0.999f) return decoded

    val scaled = Bitmap.createScaledBitmap(decoded, max(1, (decoded.width * scale).roundToInt()), max(1, (decoded.height * scale).roundToInt()), true)
    if (scaled !== decoded) decoded.recycle()
    return scaled
}
