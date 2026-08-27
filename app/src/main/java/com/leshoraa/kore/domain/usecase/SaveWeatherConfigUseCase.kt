package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.WeatherLocationConfig
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository

/**
 * UseCase to save Weather location settings locally and dispatch to KoRe hardware over BLE.
 */
class SaveWeatherConfigUseCase(
    private val preferencesRepository: UserPreferencesRepository,
    private val bleRepository: BleRepository
) {
    suspend operator fun invoke(config: WeatherLocationConfig): Result<Unit> {
        preferencesRepository.setCachedWeatherConfig(config)
        return bleRepository.sendWeatherConfig(config)
    }
}
