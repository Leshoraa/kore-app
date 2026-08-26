package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.TelemetryData
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to poll and stream real-time AI and affective telemetry data.
 */
class GetTelemetryStreamUseCase(
    private val cameraVisionRepository: CameraVisionRepository
) {
    /**
     * Observes real-time telemetry from the device.
     *
     * @param host Device IP address.
     * @param port HTTP port for telemetry (default: 80).
     * @param intervalMs Polling period in milliseconds.
     * @return Flow emitting [TelemetryData].
     */
    operator fun invoke(host: String, port: Int = 80, intervalMs: Long = 80L): Flow<TelemetryData> {
        return cameraVisionRepository.getTelemetryStream(host, port, intervalMs)
    }
}
