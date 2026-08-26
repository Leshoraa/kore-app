package com.leshoraa.kore.domain.repository

import android.graphics.Bitmap
import com.leshoraa.kore.domain.model.CameraSensorParams
import com.leshoraa.kore.domain.model.StreamConnectionState
import com.leshoraa.kore.domain.model.TelemetryData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract for camera video stream acquisition, real-time telemetry polling,
 * and SCCB camera sensor adjustment over HTTP.
 */
interface CameraVisionRepository {

    /**
     * Observable stream connection status.
     */
    val connectionState: StateFlow<StreamConnectionState>

    /**
     * Start capturing MJPEG multipart stream from the ESP32 camera endpoint.
     *
     * @param host Target device IP or hostname (e.g. 192.168.4.1).
     * @param port HTTP port for the MJPEG stream (default: 81).
     * @return Flow emitting decoded video frame Bitmaps.
     */
    fun getCameraStream(host: String, port: Int = 81): Flow<Bitmap>

    /**
     * Start polling real-time JSON telemetry from the embedded AI tracking pipeline.
     *
     * @param host Target device IP or hostname.
     * @param port HTTP port for the telemetry endpoint (default: 80).
     * @param intervalMs Polling interval in milliseconds.
     * @return Flow emitting parsed [TelemetryData] snapshots.
     */
    fun getTelemetryStream(host: String, port: Int = 80, intervalMs: Long = 80L): Flow<TelemetryData>

    /**
     * Sends a sensor parameter update command to `/camera_control`.
     *
     * @param host Target device IP.
     * @param port HTTP port (default: 80).
     * @param param Parameter identifier (brightness, contrast, saturation, vflip, hmirror, aec, agc).
     * @param value Integer value or flag to set.
     * @return Result indicating success or error.
     */
    suspend fun updateSensorParam(host: String, port: Int = 80, param: String, value: Int): Result<Unit>

    /**
     * Scans local network and mDNS to automatically detect the KoRe ESP32-S3 IP.
     *
     * @return Discovered IP address string, or null if not found.
     */
    suspend fun autoDiscoverDevice(): String?

    /**
     * Fetches current network and hardware device configurations via HTTP.
     */
    suspend fun fetchDeviceConfig(host: String, port: Int = 80): Result<com.leshoraa.kore.domain.model.DeviceNetworkConfig>

    /**
     * Saves network and hardware device configurations via HTTP.
     */
    suspend fun saveDeviceConfig(host: String, port: Int = 80, config: com.leshoraa.kore.domain.model.DeviceNetworkConfig): Result<Unit>
}
