package com.leshoraa.kore.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val bleManager: BleManager,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val connectionState = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val logs = notificationRepository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun disconnect() = bleManager.disconnect()
}
