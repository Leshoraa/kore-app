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
    private val filterAppRuleUseCase: FilterAppRuleUseCase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(event: NotificationEvent): Result<Unit> = withContext(defaultDispatcher) {
        runCatching {
            // 1. Mandatory Filters (Invariants)
            if (event.isGroupSummary) return@runCatching
            if (event.title.isBlank() && event.text.isBlank()) return@runCatching

            // 2. User-defined App Filter
            val isAllowed = filterAppRuleUseCase(event.packageName)
            if (!isAllowed) return@runCatching

            // 3. Persist for Audit
            notificationRepository.logEvent(event)

            // 4. Dispatch to BLE
            bleRepository.sendNotification(event).getOrThrow()
        }
    }
}
