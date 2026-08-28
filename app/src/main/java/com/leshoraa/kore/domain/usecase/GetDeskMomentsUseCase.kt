package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.DeskMoment
import com.leshoraa.kore.domain.repository.MomentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the stream of captured desk moments.
 */
class GetDeskMomentsUseCase(
    private val momentRepository: MomentRepository
) {
    operator fun invoke(): Flow<List<DeskMoment>> {
        return momentRepository.momentsFlow
    }
}
