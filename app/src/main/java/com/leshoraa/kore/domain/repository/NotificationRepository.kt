package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.NotificationEvent

/**
 * Interface for persisting or logging notification event history.
 */
interface NotificationRepository {
    suspend fun logEvent(event: NotificationEvent)
}
