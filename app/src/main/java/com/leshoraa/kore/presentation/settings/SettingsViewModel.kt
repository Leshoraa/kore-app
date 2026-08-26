package com.leshoraa.kore.presentation.settings

import android.bluetooth.BluetoothProfile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.domain.model.DeviceNetworkConfig
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.usecase.GetDeviceConfigUseCase
import com.leshoraa.kore.domain.usecase.SaveDeviceConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceConfigUiState(
    val staSsid: String = "",
    val staPass: String = "",
    val apSsid: String = "KoRe",
    val apPass: String = "",
    val bleName: String = "KoRe-Sense",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isBleConnected: Boolean = false,
    val isDialogOpen: Boolean = false,
    val staPassVisible: Boolean = false,
    val apPassVisible: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel(
    private val getDeviceConfigUseCase: GetDeviceConfigUseCase,
    private val saveDeviceConfigUseCase: SaveDeviceConfigUseCase,
    private val bleRepository: BleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceConfigUiState())
    val uiState: StateFlow<DeviceConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bleRepository.connectionState.collect { state ->
                _uiState.update { it.copy(isBleConnected = state == BluetoothProfile.STATE_CONNECTED) }
            }
        }
        viewModelScope.launch {
            bleRepository.deviceConfigFlow.collect { remote ->
                if (remote != null) {
                    _uiState.update {
                        it.copy(
                            staSsid = remote.staSsid.ifBlank { it.staSsid },
                            staPass = remote.staPass.ifBlank { it.staPass },
                            apSsid = remote.apSsid.ifBlank { it.apSsid },
                            apPass = remote.apPass.ifBlank { it.apPass },
                            bleName = remote.bleName.ifBlank { it.bleName },
                            isLoading = false
                        )
                    }
                }
            }
        }
        loadDeviceConfig()
    }

    fun openDialog() {
        refreshFromDevice()
        _uiState.update { it.copy(isDialogOpen = true, errorMessage = null, successMessage = null) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(isDialogOpen = false) }
    }

    fun refreshFromDevice() {
        loadDeviceConfig()
    }

    fun loadDeviceConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val config = getDeviceConfigUseCase()
            _uiState.update {
                it.copy(
                    staSsid = config.staSsid,
                    staPass = config.staPass,
                    apSsid = config.apSsid.ifBlank { "KoRe" },
                    apPass = config.apPass,
                    bleName = config.bleName.ifBlank { "KoRe-Sense" },
                    isLoading = false
                )
            }
        }
    }

    fun onStaSsidChanged(value: String) = _uiState.update { it.copy(staSsid = value) }
    fun onStaPassChanged(value: String) = _uiState.update { it.copy(staPass = value) }
    fun onApSsidChanged(value: String) = _uiState.update { it.copy(apSsid = value) }
    fun onApPassChanged(value: String) = _uiState.update { it.copy(apPass = value) }
    fun onBleNameChanged(value: String) = _uiState.update { it.copy(bleName = value) }

    fun toggleStaPassVisibility() = _uiState.update { it.copy(staPassVisible = !it.staPassVisible) }
    fun toggleApPassVisibility() = _uiState.update { it.copy(apPassVisible = !it.apPassVisible) }

    fun saveConfig() {
        val state = _uiState.value

        // Validate AP password (min 8 chars or empty for open AP)
        if (state.apPass.isNotBlank() && state.apPass.length < 8) {
            _uiState.update { it.copy(errorMessage = "AP Password must be at least 8 characters or empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            val config = DeviceNetworkConfig(
                staSsid = state.staSsid.trim(),
                staPass = state.staPass,
                apSsid = state.apSsid.trim().ifBlank { "KoRe" },
                apPass = state.apPass,
                bleName = state.bleName.trim().ifBlank { "KoRe-Sense" }
            )

            saveDeviceConfigUseCase(config).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Settings saved! KoRe is rebooting...",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to send configuration to KoRe",
                            successMessage = null
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
