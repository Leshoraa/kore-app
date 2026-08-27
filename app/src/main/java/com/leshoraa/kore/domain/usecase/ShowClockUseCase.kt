package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.BleRepository

/**
 * UseCase to trigger the Clock display glance on KoRe's OLED screen.
 */
class ShowClockUseCase(private val bleRepository: BleRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return bleRepository.showClock()
    }
}
