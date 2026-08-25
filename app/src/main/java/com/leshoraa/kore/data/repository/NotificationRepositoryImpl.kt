package com.leshoraa.kore.data.repository

import android.util.Log
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository implementation for managing notification logs (placeholder for Room DB).
 */
class NotificationRepositoryImpl : NotificationRepository {
    
    private val _recentEvents = MutableStateFlow<List<NotificationEvent>>(emptyList())
    override val recentEvents = _recentEvents.asStateFlow()

    override suspend fun logEvent(event: NotificationEvent) {
        Log.i("NotificationRepo", "Logged to audit: ${event.packageName} | ${event.title}")
        
        val current = _recentEvents.value.toMutableList()
        current.add(0, event)
        if (current.size > 50) current.removeAt(current.size - 1)
        _recentEvents.value = current
    }
}
