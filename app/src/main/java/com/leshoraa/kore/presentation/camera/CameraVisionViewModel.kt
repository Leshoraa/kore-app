package com.leshoraa.kore.presentation.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.data.remote.MjpegStreamDecoder
import com.leshoraa.kore.domain.model.CameraSensorParams
import com.leshoraa.kore.domain.model.StreamConnectionState
import com.leshoraa.kore.domain.model.TelemetryData
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository
import com.leshoraa.kore.domain.usecase.GetCameraStreamUseCase
import com.leshoraa.kore.domain.usecase.GetTelemetryStreamUseCase
import com.leshoraa.kore.domain.usecase.UpdateCameraSensorUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel governing camera MJPEG streaming lifecycle, real-time AI telemetry aggregation,
 * sensor parameter calibration, network discovery, and network settings.
 */
class CameraVisionViewModel(
    private val getCameraStreamUseCase: GetCameraStreamUseCase,
    private val getTelemetryStreamUseCase: GetTelemetryStreamUseCase,
    private val updateCameraSensorUseCase: UpdateCameraSensorUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val cameraVisionRepository: CameraVisionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CameraVisionVM"
    }

    val connectionState: StateFlow<StreamConnectionState> = cameraVisionRepository.connectionState

    val hostIp: StateFlow<String> = userPreferencesRepository.cameraHost

    private val _isStreamActive = MutableStateFlow(false)
    val isStreamActive: StateFlow<Boolean> = _isStreamActive.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    private val _telemetry = MutableStateFlow<TelemetryData?>(null)
    val telemetry: StateFlow<TelemetryData?> = _telemetry.asStateFlow()

    private val _sensorParams = MutableStateFlow(CameraSensorParams())
    val sensorParams: StateFlow<CameraSensorParams> = _sensorParams.asStateFlow()

    private var streamJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        // If stored host is empty, set default KoRe IP
        if (hostIp.value.isBlank()) {
            userPreferencesRepository.setCameraHost("192.168.18.16")
        }
    }

    /**
     * Toggles live streaming on or off.
     */
    fun toggleStream() {
        if (_isStreamActive.value) {
            stopStream()
        } else {
            startStream()
        }
    }

    /**
     * Automatically sweeps the local network to find KoRe's current Wi-Fi IP.
     */
    fun autoDiscoverKoRe() {
        if (_isDiscovering.value) return
        _isDiscovering.value = true
        _lastErrorMessage.value = null

        viewModelScope.launch {
            try {
                val foundIp = cameraVisionRepository.autoDiscoverDevice()
                if (foundIp != null) {
                    Log.i(TAG, "KoRe discovered at IP: $foundIp")
                    userPreferencesRepository.setCameraHost(foundIp)
                    // Restart stream if active
                    if (_isStreamActive.value) {
                        stopStream()
                        startStream()
                    }
                } else {
                    _lastErrorMessage.value = "KoRe not found automatically. Check IP on device screen."
                }
            } catch (e: Exception) {
                _lastErrorMessage.value = "Discovery error: ${e.localizedMessage}"
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    /**
     * Starts concurrent MJPEG video stream and JSON telemetry polling.
     */
    fun startStream() {
        if (_isStreamActive.value) return

        _isStreamActive.value = true
        _lastErrorMessage.value = null
        val host = MjpegStreamDecoder.sanitizeHost(hostIp.value)

        Log.d(TAG, "Starting stream to sanitized host: $host")

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            getCameraStreamUseCase(host, port = 81)
                .catch { e ->
                    Log.e(TAG, "Video stream failed: ${e.message}", e)
                    _lastErrorMessage.value = "Stream unreachable at $host:81. Verify device IP address."
                }
                .collect { bitmap: Bitmap ->
                    _currentFrame.value = bitmap.asImageBitmap()
                    if (_lastErrorMessage.value != null) {
                        _lastErrorMessage.value = null
                    }
                }
        }

        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            getTelemetryStreamUseCase(host, port = 80, intervalMs = 75L)
                .catch { e ->
                    Log.w(TAG, "Telemetry stream failed: ${e.message}")
                }
                .collect { data: TelemetryData ->
                    _telemetry.value = data
                }
        }
    }

    /**
     * Halts video streaming and telemetry polling.
     */
    fun stopStream() {
        _isStreamActive.value = false
        streamJob?.cancel()
        streamJob = null
        telemetryJob?.cancel()
        telemetryJob = null
    }

    /**
     * Updates the target host IP for the ESP32 camera node.
     */
    fun setHostIp(newIp: String) {
        val sanitized = MjpegStreamDecoder.sanitizeHost(newIp)
        if (sanitized.isNotEmpty() && sanitized != hostIp.value) {
            userPreferencesRepository.setCameraHost(sanitized)
            if (_isStreamActive.value) {
                stopStream()
                startStream()
            }
        }
    }

    /**
     * Dispatches an SCCB sensor control adjustment to the camera hardware.
     */
    fun updateSensorParam(param: String, value: Int) {
        val current = _sensorParams.value
        val updated = when (param) {
            "brightness" -> current.copy(brightness = value)
            "contrast" -> current.copy(contrast = value)
            "saturation" -> current.copy(saturation = value)
            "vflip" -> current.copy(vflip = value != 0)
            "hmirror" -> current.copy(hmirror = value != 0)
            "aec" -> current.copy(aec = value != 0)
            "agc" -> current.copy(agc = value != 0)
            else -> current
        }
        _sensorParams.value = updated

        viewModelScope.launch {
            updateCameraSensorUseCase(hostIp.value, port = 80, param, value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStream()
    }
}
