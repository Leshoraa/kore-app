package com.leshoraa.kore.core.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageOptimizer {
    private const val TAG = "ImageOptimizer"

    // LRU in-memory bitmap cache (keyed by filePath, stores up to 60 decoded bitmaps)
    private val memoryCache: LruCache<String, ImageBitmap> = object : LruCache<String, ImageBitmap>(60) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return 1
        }
    }

    /**
     * Compresses raw bytes received from KoRe on the phone's CPU.
     * Saves both an optimized full-resolution JPEG and a compact thumbnail for instant lazy loading.
     *
     * @return Pair(fullFilePath, thumbFilePath)
     */
    suspend fun compressAndSaveMoment(
        rawBytes: ByteArray,
        destinationDir: File,
        timestamp: Long
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val fullFile = File(destinationDir, "moment_${timestamp}.jpg")
        val thumbFile = File(destinationDir, "moment_${timestamp}_thumb.jpg")

        // 1. Decode raw bytes
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565 // 50% RAM reduction
        }
        val originalBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
            ?: throw IllegalStateException("Failed to decode snapshot bytes into Bitmap")

        try {
            // 2. Compress and save optimized full-resolution image (quality 82 - crisp & lightweight)
            FileOutputStream(fullFile).use { fos ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
                fos.flush()
            }

            // 3. Generate compact downscaled thumbnail (max dimension 320px for instant carousel/grid rendering)
            val maxThumbDim = 320
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = (maxThumbDim.toFloat() / maxOf(width, height)).coerceAtMost(1f)

            val thumbWidth = (width * scale).toInt()
            val thumbHeight = (height * scale).toInt()
            val thumbBitmap = Bitmap.createScaledBitmap(originalBitmap, thumbWidth, thumbHeight, true)

            try {
                FileOutputStream(thumbFile).use { fos ->
                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos)
                    fos.flush()
                }
                // Pre-cache thumbnail into memory
                memoryCache.put(thumbFile.absolutePath, thumbBitmap.asImageBitmap())
            } finally {
                if (thumbBitmap != originalBitmap) {
                    // Do not recycle originalBitmap yet
                }
            }

            Log.i(TAG, "Compressed snapshot: original ${rawBytes.size / 1024}KB -> full ${fullFile.length() / 1024}KB, thumb ${thumbFile.length() / 1024}KB")
            return@withContext Pair(fullFile.absolutePath, thumbFile.absolutePath)
        } finally {
            originalBitmap.recycle()
        }
    }

    /**
     * Loads a bitmap asynchronously with memory caching and sample downscaling.
     */
    suspend fun loadOptimizedBitmap(filePath: String, isThumbnail: Boolean = true): ImageBitmap? = withContext(Dispatchers.IO) {
        val targetPath = if (isThumbnail) {
            val potentialThumb = filePath.replace(".jpg", "_thumb.jpg")
            if (File(potentialThumb).exists()) potentialThumb else filePath
        } else {
            filePath
        }

        // 1. Check memory cache first
        memoryCache[targetPath]?.let { return@withContext it }

        // 2. Load from disk with efficient decoding
        val file = File(targetPath)
        if (!file.exists()) return@withContext null

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            if (isThumbnail && !targetPath.endsWith("_thumb.jpg")) {
                inSampleSize = 2 // Downscale if thumbnail file is not available
            }
        }

        val decoded = BitmapFactory.decodeFile(targetPath, options) ?: return@withContext null
        val imageBitmap = decoded.asImageBitmap()
        memoryCache.put(targetPath, imageBitmap)
        return@withContext imageBitmap
    }

    fun evictFromCache(filePath: String) {
        memoryCache.remove(filePath)
        memoryCache.remove(filePath.replace(".jpg", "_thumb.jpg"))
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}

/**
 * Lazy loading image composable with smooth crossfade and async IO decoding.
 */
@Composable
fun LazyDeskMomentImage(
    filePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isThumbnail: Boolean = true
) {
    var imageBitmap by remember(filePath, isThumbnail) { mutableStateOf<ImageBitmap?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(filePath, isThumbnail) {
        imageBitmap = ImageOptimizer.loadOptimizedBitmap(filePath, isThumbnail)
    }

    Crossfade(
        targetState = imageBitmap,
        animationSpec = tween(durationMillis = 180),
        label = "LazyImageCrossfade",
        modifier = modifier
    ) { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = null,
                    tint = colorScheme.outlineVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
