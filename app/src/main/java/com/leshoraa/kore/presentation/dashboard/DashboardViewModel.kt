package com.leshoraa.kore.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(
    private val bleManager: BleManager,
    private val bleRepository: BleRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val connectionState = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val connectedDeviceName = bleManager.connectedDeviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs = notificationRepository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testTitle = MutableStateFlow("")
    val testTitle = _testTitle.asStateFlow()

    private val _testMessage = MutableStateFlow("")
    val testMessage = _testMessage.asStateFlow()

    fun onTestTitleChange(newValue: String) {
        _testTitle.value = newValue
    }

    fun onTestMessageChange(newValue: String) {
        _testMessage.value = newValue
    }

    fun sendTestMessage() {
        viewModelScope.launch {
            val event = NotificationEvent(
                id = UUID.randomUUID().toString(),
                packageName = "com.leshoraa.kore.test",
                appName = "Test",
                postTimeMillis = System.currentTimeMillis(),
                title = _testTitle.value,
                text = _testMessage.value,
                isClearable = true
            )
            bleRepository.sendNotification(event)
            // Clear fields after sending
            _testTitle.value = ""
            _testMessage.value = ""
        }
    }

    fun disconnect() = bleManager.disconnect()
}
