package com.leshoraa.kore.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * High-performance, memory-safe decoder for multipart MJPEG HTTP video streams.
 * Decodes sequential JPEG frames from the ESP32 camera endpoint and emits Android [Bitmap]s.
 */
class MjpegStreamDecoder {

    companion object {
        private const val TAG = "MjpegStreamDecoder"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 8000
        private const val BUFFER_SIZE = 8192
        private const val MAX_FRAME_SIZE = 512 * 1024 // 512 KB maximum frame size
        
        // JPEG SOI (Start Of Image) and EOI (End Of Image) markers
        private const val JPEG_SOI_1 = 0xFF.toByte()
        private const val JPEG_SOI_2 = 0xD8.toByte()
        private const val JPEG_EOI_1 = 0xFF.toByte()
        private const val JPEG_EOI_2 = 0xD9.toByte()

        fun sanitizeHost(raw: String): String {
            var s = raw.trim()
            if (s.startsWith("http://", ignoreCase = true)) s = s.substring(7)
            if (s.startsWith("https://", ignoreCase = true)) s = s.substring(8)
            if (s.contains("/")) s = s.substringBefore("/")
            if (s.contains(":")) s = s.substringBefore(":")
            return s.trim()
        }
    }

    /**
     * Decodes and streams incoming MJPEG frames from the given URL.
     *
     * @param streamUrl Complete HTTP stream URL (e.g. "http://192.168.18.16:81/stream").
     * @return Flow emitting decoded frame Bitmaps as they arrive.
     */
    fun decodeStream(streamUrl: String): Flow<Bitmap> = flow {
        var currentUrl = streamUrl
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null

        try {
            Log.d(TAG, "Opening MJPEG stream connection: $currentUrl")
            var url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                doInput = true
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("User-Agent", "KoRe-Android-Client/2.5.0")
            }

            connection.connect()

            var responseCode = connection.responseCode
            // Handle HTTP 307 / 302 / 301 Redirects (e.g. port 80 /stream -> port 81 /stream)
            if (responseCode in 300..399) {
                val redirectLocation = connection.getHeaderField("Location")
                if (!redirectLocation.isNullOrEmpty()) {
                    Log.d(TAG, "Following stream redirect to: $redirectLocation")
                    connection.disconnect()
                    currentUrl = redirectLocation
                    url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        requestMethod = "GET"
                        doInput = true
                        useCaches = false
                        setRequestProperty("Connection", "Keep-Alive")
                    }
                    connection.connect()
                    responseCode = connection.responseCode
                }
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP Stream response code: $responseCode for $currentUrl")
            }

            inputStream = BufferedInputStream(connection.inputStream, BUFFER_SIZE)
            val frameBuffer = ByteArrayOutputStream(32 * 1024)
            val chunk = ByteArray(BUFFER_SIZE)

            var prevByte: Byte = 0
            var insideJpeg = false

            while (currentCoroutineContext().isActive) {
                val bytesRead = inputStream.read(chunk)
                if (bytesRead == -1) break

                for (i in 0 until bytesRead) {
                    val currByte = chunk[i]

                    if (!insideJpeg) {
                        if (prevByte == JPEG_SOI_1 && currByte == JPEG_SOI_2) {
                            insideJpeg = true
                            frameBuffer.reset()
                            frameBuffer.write(JPEG_SOI_1.toInt())
                            frameBuffer.write(JPEG_SOI_2.toInt())
                        }
                    } else {
                        frameBuffer.write(currByte.toInt())

                        if (prevByte == JPEG_EOI_1 && currByte == JPEG_EOI_2) {
                            insideJpeg = false
                            val frameBytes = frameBuffer.toByteArray()

                            if (frameBytes.size in 100..MAX_FRAME_SIZE) {
                                val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                                if (bitmap != null) {
                                    emit(bitmap)
                                }
                            }
                            frameBuffer.reset()
                        }
                    }
                    prevByte = currByte
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream error on $currentUrl: ${e.localizedMessage}", e)
            throw e
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
