package com.leshoraa.kore.presentation.dashboard

import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.common.PreferencesManager
import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.NotificationRepository
import com.leshoraa.kore.domain.usecase.SetBrightnessUseCase
import com.leshoraa.kore.domain.usecase.SetExpressionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(
    private val bleManager: BleManager,
    private val bleRepository: BleRepository,
    private val notificationRepository: NotificationRepository,
    private val preferencesManager: PreferencesManager,
    private val setBrightnessUseCase: SetBrightnessUseCase,
    private val setExpressionUseCase: SetExpressionUseCase
) : ViewModel() {

    val connectionState = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothProfile.STATE_DISCONNECTED)

    val isBluetoothEnabled = bleManager.isBluetoothEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val connectedDeviceName = bleManager.connectedDeviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs = notificationRepository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telemetry = bleRepository.telemetryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _brightness = MutableStateFlow(preferencesManager.getBrightness())
    val brightness = _brightness.asStateFlow()

    private val _selectedExpression = MutableStateFlow(
        preferencesManager.getSelectedExpressionCode()?.let { Expression.fromCode(it) }
    )
    val selectedExpression = _selectedExpression.asStateFlow()

    private var liveBrightnessJob: Job? = null

    private val _testTitle = MutableStateFlow("")
    val testTitle = _testTitle.asStateFlow()

    private val _testMessage = MutableStateFlow("")
    val testMessage = _testMessage.asStateFlow()

    private val _isSystemAccessTipDismissed = MutableStateFlow(preferencesManager.isSystemAccessTipDismissed())
    val isSystemAccessTipDismissed = _isSystemAccessTipDismissed.asStateFlow()

    private val _isBluetoothTipDismissed = MutableStateFlow(false)
    val isBluetoothTipDismissed = _isBluetoothTipDismissed.asStateFlow()

    private val _isNotificationAccessGranted = MutableStateFlow(true)
    private val _isBatteryOptimizationIgnored = MutableStateFlow(true)

    fun updatePermissionStatus(notificationAccess: Boolean, batteryOptimization: Boolean) {
        _isNotificationAccessGranted.value = notificationAccess
        _isBatteryOptimizationIgnored.value = batteryOptimization
    }

    fun dismissSystemAccessTip() {
        _isSystemAccessTipDismissed.value = true
        preferencesManager.setSystemAccessTipDismissed(true)
    }

    fun dismissBluetoothTip() {
        _isBluetoothTipDismissed.value = true
    }

    init {
        // Synchronize persisted KoRe brightness, expression, RTC time, weather, and start telemetry stream on connection
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    kotlinx.coroutines.delay(500) // Allow service discovery and MTU negotiation to settle
                    if (preferencesManager.isAutoSyncEnabled()) {
                        setBrightnessUseCase(_brightness.value, save = false)
                    }
                    _selectedExpression.value?.let { expr ->
                        setExpressionUseCase(expr)
                    }
                    // Auto-sync hardware RTC clock with phone timestamp & timezone!
                    bleRepository.syncTime()
                    // Auto-fetch latest weather on phone and push to KoRe!
                    val wConfig = preferencesManager.getCachedWeatherConfig()
                    if (wConfig.isEnabled) {
                        bleRepository.fetchAndPushWeatherFromPhone(wConfig.city, wConfig.latitude, wConfig.longitude)
                    }
                    // Start live real-time telemetry streaming over BLE!
                    bleRepository.startTelemetryStreaming(500L)
                    bleRepository.queryTelemetry()
                }
            }
        }
    }


    fun onBrightnessChange(newBrightness: Int) {
        val clamped = newBrightness.coerceIn(PreferencesManager.MIN_BRIGHTNESS, PreferencesManager.MAX_BRIGHTNESS)
        _brightness.value = clamped
        
        liveBrightnessJob?.cancel()
        liveBrightnessJob = viewModelScope.launch {
            if (connectionState.value == BluetoothProfile.STATE_CONNECTED) {
                setBrightnessUseCase(clamped, save = false)
            }
        }
    }

    fun onBrightnessChangeFinished() {
        viewModelScope.launch {
            setBrightnessUseCase(_brightness.value, save = true)
        }
    }

    fun setBrightnessPreset(presetValue: Int) {
        val clamped = presetValue.coerceIn(PreferencesManager.MIN_BRIGHTNESS, PreferencesManager.MAX_BRIGHTNESS)
        _brightness.value = clamped
        viewModelScope.launch {
            setBrightnessUseCase(clamped, save = true)
        }
    }

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

    fun selectExpression(expression: Expression?) {
        _selectedExpression.value = expression
        viewModelScope.launch {
            Log.d("DashboardViewModel", "Selecting expression: ${expression?.displayName ?: "Auto Mood"}")
            val result = setExpressionUseCase(expression)
            result.onSuccess {
                Log.i("DashboardViewModel", "Expression successfully dispatched to BLE: ${expression?.displayName ?: "Auto Mood"}")
            }.onFailure { error ->
                Log.e("DashboardViewModel", "Failed to dispatch expression: ${error.message}", error)
            }
        }
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun disconnect() = bleManager.disconnect()
}



