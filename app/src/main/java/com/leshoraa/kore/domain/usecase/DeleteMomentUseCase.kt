package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.MomentRepository

/**
 * Use case to delete a saved desk moment.
 */
class DeleteMomentUseCase(
    private val momentRepository: MomentRepository
) {
    suspend operator fun invoke(id: Long, filePath: String): Result<Unit> {
        return momentRepository.deleteMoment(id, filePath)
    }

    suspend fun clearAll(): Result<Unit> {
        return momentRepository.clearAllMoments()
    }
}
