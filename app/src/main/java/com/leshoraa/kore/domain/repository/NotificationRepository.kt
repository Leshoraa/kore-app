package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.NotificationEvent
import kotlinx.coroutines.flow.Flow

/**
 * Interface for persisting or logging notification event history.
 */
interface NotificationRepository {
    val recentEvents: Flow<List<NotificationEvent>>
    suspend fun logEvent(event: NotificationEvent)
}
