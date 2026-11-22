package com.miupiu.zveryata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Keep a memory-bounded cache; no bitmap is recycled while Compose may draw it.
 * All disk reads/decoding happen off the UI thread.
 */
internal class BitmapStore(private val context: Context) {
    private val mutex = Mutex()
    private val cacheBudget = (Runtime.getRuntime().maxMemory() / 8L)
        .coerceIn(12L * 1024 * 1024, 64L * 1024 * 1024).toInt()
    private val cache = object : LruCache<String, Bitmap>(cacheBudget) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(asset: String, width: Int, height: Int): Bitmap = withContext(Dispatchers.IO) {
        val boundedWidth = width.coerceAtLeast(1)
        val boundedHeight = height.coerceAtLeast(1)
        val key = asset + "@" + boundedWidth + "x" + boundedHeight
        mutex.withLock {
            cache.get(key) ?: decode(asset, boundedWidth, boundedHeight).also {
                cache.put(key, it)
            }
        }
    }

    private fun decode(asset: String, width: Int, height: Int): Bitmap {
        val path = "images/" + asset
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.assets, path)
            return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val ratio = min(1f, min(
                    width.toFloat() / info.size.width.coerceAtLeast(1),
                    height.toFloat() / info.size.height.coerceAtLeast(1)
                ))
                decoder.setTargetSize(
                    max(1, (info.size.width * ratio).roundToInt()),
                    max(1, (info.size.height * ratio).roundToInt())
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= width &&
            bounds.outHeight / (sample * 2) >= height) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.assets.open(path).use {
            BitmapFactory.decodeStream(it, null, options) ?: error("Cannot decode image " + asset)
        }
        val scale = min(1f, min(
            width.toFloat() / decoded.width.coerceAtLeast(1),
            height.toFloat() / decoded.height.coerceAtLeast(1)
        ))
        if (scale >= 0.999f) return decoded
        return Bitmap.createScaledBitmap(
            decoded,
            max(1, (decoded.width * scale).roundToInt()),
            max(1, (decoded.height * scale).roundToInt()), true
        ).also { if (it !== decoded) decoded.recycle() }
    }
}
