package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.BleRepository

/**
 * UseCase to trigger the Weather display glance on KoRe's OLED screen.
 */
class ShowWeatherUseCase(private val bleRepository: BleRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return bleRepository.showWeather()
    }
}
