package com.leshoraa.kore.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.NotificationRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for viewing historical notification logs.
 */
class LogsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val logs: StateFlow<List<NotificationEvent>> = notificationRepository.recentEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearLogs() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                notificationRepository.clearLogs()
            }
        }
    }
}
