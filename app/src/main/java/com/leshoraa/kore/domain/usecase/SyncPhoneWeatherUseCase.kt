package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository

/**
 * UseCase to fetch latest weather on smartphone and push to KoRe over BLE.
 */
class SyncPhoneWeatherUseCase(
    private val preferencesRepository: UserPreferencesRepository,
    private val bleRepository: BleRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val config = preferencesRepository.getCachedWeatherConfig()
        return bleRepository.fetchAndPushWeatherFromPhone(config.city, config.latitude, config.longitude)
    }
}
