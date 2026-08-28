package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.DeskMoment
import com.leshoraa.kore.domain.repository.MomentRepository

/**
 * Use case to capture and persist a new desk moment snapshot.
 */
class CaptureMomentUseCase(
    private val momentRepository: MomentRepository
) {
    suspend operator fun invoke(host: String): Result<DeskMoment> {
        return momentRepository.captureAndSaveMoment(host)
    }
}
