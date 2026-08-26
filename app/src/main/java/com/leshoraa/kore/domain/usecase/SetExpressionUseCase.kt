package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pure Kotlin UseCase to trigger expression changes on the KoRe companion device
 * and persist the user's expression preference.
 *
 * Passing null resets the companion hardware to its autonomous Auto Mood mode.
 */
class SetExpressionUseCase(
    private val bleRepository: BleRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(expression: Expression?): Result<Unit> = withContext(defaultDispatcher) {
        preferencesRepository.setSelectedExpressionCode(expression?.code)
        bleRepository.sendExpression(expression)
    }
}
