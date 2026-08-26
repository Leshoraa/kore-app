package com.leshoraa.kore.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.leshoraa.kore.data.remote.DeviceDiscoveryScanner
import com.leshoraa.kore.data.remote.MjpegStreamDecoder
import com.leshoraa.kore.data.remote.TelemetryHttpClient
import com.leshoraa.kore.domain.model.StreamConnectionState
import com.leshoraa.kore.domain.model.TelemetryData
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/**
 * Concrete implementation of [CameraVisionRepository] handling remote communication
 * with KoRe firmware on ESP32-S3.
 */
class CameraVisionRepositoryImpl(
    context: Context? = null,
    private val mjpegDecoder: MjpegStreamDecoder = MjpegStreamDecoder(),
    private val telemetryClient: TelemetryHttpClient = TelemetryHttpClient(),
    private val discoveryScanner: DeviceDiscoveryScanner? = context?.let { DeviceDiscoveryScanner(it) }
) : CameraVisionRepository {

    companion object {
        private const val TAG = "CameraVisionRepo"
    }

    private val _connectionState = MutableStateFlow(StreamConnectionState.IDLE)
    override val connectionState: StateFlow<StreamConnectionState> = _connectionState.asStateFlow()

    override fun getCameraStream(host: String, port: Int): Flow<Bitmap> {
        val cleanHost = MjpegStreamDecoder.sanitizeHost(host)
        val url = "http://$cleanHost:$port/stream"
        Log.d(TAG, "Requesting camera stream from: $url")

        return mjpegDecoder.decodeStream(url)
            .onStart {
                Log.d(TAG, "Stream connecting to: $url")
                _connectionState.value = StreamConnectionState.CONNECTING
            }
            .onEach {
                if (_connectionState.value != StreamConnectionState.STREAMING) {
                    _connectionState.value = StreamConnectionState.STREAMING
                }
            }
            .catch { e ->
                Log.e(TAG, "Stream error on $url: ${e.message}", e)
                _connectionState.value = StreamConnectionState.ERROR
                throw e
            }
            .onCompletion { cause ->
                Log.d(TAG, "Stream completed. Cause: ${cause?.message}")
                if (cause == null && _connectionState.value == StreamConnectionState.STREAMING) {
                    _connectionState.value = StreamConnectionState.IDLE
                }
            }
    }

    override fun getTelemetryStream(host: String, port: Int, intervalMs: Long): Flow<TelemetryData> {
        val baseUrl = TelemetryHttpClient.formatBaseUrl(host, port)
        return telemetryClient.pollTelemetry(baseUrl, intervalMs)
    }

    override suspend fun updateSensorParam(
        host: String,
        port: Int,
        param: String,
        value: Int
    ): Result<Unit> {
        val baseUrl = TelemetryHttpClient.formatBaseUrl(host, port)
        return telemetryClient.postCameraControl(baseUrl, param, value)
    }

    override suspend fun autoDiscoverDevice(): String? {
        return discoveryScanner?.discoverKoReDevice()
    }
}
