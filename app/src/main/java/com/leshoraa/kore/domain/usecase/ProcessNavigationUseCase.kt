package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.repository.BleRepository

/**
 * Pure Kotlin domain usecase to process and dispatch turn-by-turn navigation events over BLE.
 */
class ProcessNavigationUseCase(
    private val bleRepository: BleRepository
) {
    suspend operator fun invoke(event: NavEvent): Result<Unit> {
        return bleRepository.sendNavigation(event)
    }
}
