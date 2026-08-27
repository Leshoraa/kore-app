package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.WeatherLocationConfig
import com.leshoraa.kore.domain.repository.UserPreferencesRepository

/**
 * UseCase to fetch locally cached Weather location and timezone configuration.
 */
class GetWeatherConfigUseCase(private val preferencesRepository: UserPreferencesRepository) {
    operator fun invoke(): WeatherLocationConfig {
        return preferencesRepository.getCachedWeatherConfig()
    }
}
