package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.NotificationEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for persisting or logging notification event history.
 */
interface NotificationRepository {
    val recentEvents: StateFlow<List<NotificationEvent>>
    suspend fun logEvent(event: NotificationEvent)
}
