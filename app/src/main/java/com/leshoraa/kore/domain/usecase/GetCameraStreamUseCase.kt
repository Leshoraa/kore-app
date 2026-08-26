package com.leshoraa.kore.domain.usecase

import android.graphics.Bitmap
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to initiate and observe the live video frame stream from the ESP32 camera.
 */
class GetCameraStreamUseCase(
    private val cameraVisionRepository: CameraVisionRepository
) {
    /**
     * Executes the live video stream acquisition.
     *
     * @param host Target camera host IP.
     * @param port HTTP port for the MJPEG video stream (default: 81).
     * @return Flow emitting live decoded Bitmap frames.
     */
    operator fun invoke(host: String, port: Int = 81): Flow<Bitmap> {
        return cameraVisionRepository.getCameraStream(host, port)
    }
}
