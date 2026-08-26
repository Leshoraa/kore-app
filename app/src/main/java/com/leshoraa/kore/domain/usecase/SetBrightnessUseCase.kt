package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pure Kotlin UseCase to adjust OLED brightness on the KoRe companion device and persist user preference.
 */
class SetBrightnessUseCase(
    private val bleRepository: BleRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(brightness: Int, save: Boolean = true): Result<Unit> = withContext(defaultDispatcher) {
        val clamped = brightness.coerceIn(0, 255)
        if (save) {
            preferencesRepository.setBrightness(clamped)
        }
        bleRepository.sendBrightness(clamped, save)
    }
}
