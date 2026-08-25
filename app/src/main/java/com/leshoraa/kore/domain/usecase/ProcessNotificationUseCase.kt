package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates business logic for a captured notification.
 */
class ProcessNotificationUseCase(
    private val bleRepository: BleRepository,
    private val notificationRepository: NotificationRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(event: NotificationEvent): Result<Unit> = withContext(defaultDispatcher) {
        runCatching {
            // 1. Mandatory Filters (Invariants)
            if (event.isGroupSummary) return@runCatching

            // 2. Persist for Audit
            notificationRepository.logEvent(event)

            // 3. Dispatch to BLE
            bleRepository.sendNotification(event).getOrThrow()
        }
    }
}
