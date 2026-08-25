package com.leshoraa.kore.data.repository

import android.util.Log
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.NotificationRepository

/**
 * Repository implementation for managing notification logs (placeholder for Room DB).
 */
class NotificationRepositoryImpl : NotificationRepository {
    override suspend fun logEvent(event: NotificationEvent) {
        // Placeholder for Database persistence
        Log.i("NotificationRepo", "Logged to audit: ${event.packageName} | ${event.title}")
    }
}
