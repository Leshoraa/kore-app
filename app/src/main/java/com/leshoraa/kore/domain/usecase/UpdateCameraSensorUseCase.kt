package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.CameraVisionRepository

/**
 * Use case to dispatch sensor parameter adjustments to the camera driver on the ESP32.
 */
class UpdateCameraSensorUseCase(
    private val cameraVisionRepository: CameraVisionRepository
) {
    /**
     * Updates a single sensor control register or flag.
     *
     * @param host Device IP address.
     * @param port HTTP port for control endpoint (default: 80).
     * @param param Parameter name (brightness, contrast, saturation, vflip, hmirror, aec, agc).
     * @param value Integer value to apply.
     * @return Result of the network operation.
     */
    suspend operator fun invoke(host: String, port: Int = 80, param: String, value: Int): Result<Unit> {
        return cameraVisionRepository.updateSensorParam(host, port, param, value)
    }
}
