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
    val successMessage: String? = null,
    val resolvableSettingsException: com.google.android.gms.common.api.ResolvableApiException? = null
)

data class WeatherConfigUiState(
    val city: String = "Jakarta",
    val latitude: String = "-6.2088",
    val longitude: String = "106.8456",
    val isEnabled: Boolean = true,
    val timezoneOffsetSec: Int = 25200,
    val isDialogOpen: Boolean = false,
    val isSaving: Boolean = false,
    val isAcquiringLocation: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val resolvableSettingsException: com.google.android.gms.common.api.ResolvableApiException? = null
)

class SettingsViewModel(
    private val getDeviceConfigUseCase: GetDeviceConfigUseCase,
    private val saveDeviceConfigUseCase: SaveDeviceConfigUseCase,
    private val getWeatherConfigUseCase: com.leshoraa.kore.domain.usecase.GetWeatherConfigUseCase,
    private val saveWeatherConfigUseCase: com.leshoraa.kore.domain.usecase.SaveWeatherConfigUseCase,
    private val getPhoneLocationUseCase: com.leshoraa.kore.domain.usecase.GetPhoneLocationUseCase,
    private val syncPhoneWeatherUseCase: com.leshoraa.kore.domain.usecase.SyncPhoneWeatherUseCase,
    private val showClockUseCase: com.leshoraa.kore.domain.usecase.ShowClockUseCase,
    private val showWeatherUseCase: com.leshoraa.kore.domain.usecase.ShowWeatherUseCase,
    private val bleRepository: BleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceConfigUiState())
    val uiState: StateFlow<DeviceConfigUiState> = _uiState.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherConfigUiState())
    val weatherState: StateFlow<WeatherConfigUiState> = _weatherState.asStateFlow()

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
        viewModelScope.launch {
            bleRepository.weatherConfigFlow.collect { remote ->
                if (remote != null) {
                    _weatherState.update {
                        it.copy(
                            city = remote.city.ifBlank { it.city },
                            latitude = remote.latitude.toString(),
                            longitude = remote.longitude.toString(),
                            isEnabled = remote.isEnabled,
                            timezoneOffsetSec = remote.timezoneOffsetSec
                        )
                    }
                }
            }
        }
        loadDeviceConfig()
        loadWeatherConfig()
    }

    fun openWeatherDialog() {
        loadWeatherConfig()
        _weatherState.update { it.copy(isDialogOpen = true, errorMessage = null, successMessage = null) }
    }

    fun closeWeatherDialog() {
        _weatherState.update { it.copy(isDialogOpen = false) }
    }

    fun loadWeatherConfig() {
        val cached = getWeatherConfigUseCase()
        _weatherState.update {
            it.copy(
                city = cached.city,
                latitude = cached.latitude.toString(),
                longitude = cached.longitude.toString(),
                isEnabled = cached.isEnabled,
                timezoneOffsetSec = cached.timezoneOffsetSec
            )
        }
    }

    fun onWeatherCityChanged(value: String) = _weatherState.update { it.copy(city = value) }
    fun onWeatherLatChanged(value: String) = _weatherState.update { it.copy(latitude = value) }
    fun onWeatherLonChanged(value: String) = _weatherState.update { it.copy(longitude = value) }
    fun onWeatherEnabledChanged(value: Boolean) = _weatherState.update { it.copy(isEnabled = value) }
    fun onWeatherTzChanged(value: Int) = _weatherState.update { it.copy(timezoneOffsetSec = value) }

    fun applyWeatherPreset(preset: com.leshoraa.kore.domain.model.WeatherLocationConfig) {
        _weatherState.update {
            it.copy(
                city = preset.city,
                latitude = preset.latitude.toString(),
                longitude = preset.longitude.toString(),
                timezoneOffsetSec = preset.timezoneOffsetSec,
                isEnabled = preset.isEnabled
            )
        }
    }

    fun acquireLocationFromPhone() {
        viewModelScope.launch {
            _weatherState.update { it.copy(isAcquiringLocation = true, errorMessage = null, successMessage = null, resolvableSettingsException = null) }
            
            getPhoneLocationUseCase.checkSettings().fold(
                onSuccess = {
                    fetchLocationInternal()
                },
                onFailure = { error ->
                    if (error is com.google.android.gms.common.api.ResolvableApiException) {
                        _weatherState.update {
                            it.copy(
                                isAcquiringLocation = false,
                                resolvableSettingsException = error
                            )
                        }
                    } else {
                        // Fallback to fetch location anyway (maybe standard LocationManager works)
                        fetchLocationInternal()
                    }
                }
            )
        }
    }

    private fun fetchLocationInternal() {
        viewModelScope.launch {
            getPhoneLocationUseCase().fold(
                onSuccess = { loc ->
                    _weatherState.update {
                        it.copy(
                            city = loc.cityName,
                            latitude = String.format(java.util.Locale.US, "%.4f", loc.latitude),
                            longitude = String.format(java.util.Locale.US, "%.4f", loc.longitude),
                            timezoneOffsetSec = loc.timezoneOffsetSec,
                            isAcquiringLocation = false,
                            successMessage = "GPS location detected: ${loc.cityName}"
                        )
                    }
                },
                onFailure = { error ->
                    _weatherState.update {
                        it.copy(
                            isAcquiringLocation = false,
                            errorMessage = error.message ?: "Failed to acquire GPS location from phone."
                        )
                    }
                }
            )
        }
    }

    fun clearResolvableException() {
        _weatherState.update { it.copy(resolvableSettingsException = null) }
    }

    fun syncTimeAndWeatherNow() {
        val state = _weatherState.value
        val lat = state.latitude.toDoubleOrNull()
        val lon = state.longitude.toDoubleOrNull()

        if (state.city.isBlank() || lat == null || lon == null) {
            _weatherState.update { it.copy(errorMessage = "Please enter valid City and Coordinates before syncing.") }
            return
        }

        viewModelScope.launch {
            _weatherState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val config = com.leshoraa.kore.domain.model.WeatherLocationConfig(
                city = state.city.trim(),
                latitude = lat,
                longitude = lon,
                isEnabled = state.isEnabled,
                timezoneOffsetSec = state.timezoneOffsetSec
            )
            saveWeatherConfigUseCase(config)
            bleRepository.syncTime()
            syncPhoneWeatherUseCase().fold(
                onSuccess = {
                    _weatherState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "RTC Time & Live Weather pushed to KoRe!",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _weatherState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Time synced, but weather push failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun showClock() {
        viewModelScope.launch {
            showClockUseCase()
            kotlinx.coroutines.delay(100)
            bleRepository.syncTime()
            _weatherState.update { it.copy(successMessage = "Clock displayed on KoRe") }
        }
    }

    fun showWeather() {
        viewModelScope.launch {
            showWeatherUseCase()
            kotlinx.coroutines.delay(100)
            syncPhoneWeatherUseCase()
            _weatherState.update { it.copy(successMessage = "Weather displayed on KoRe") }
        }
    }

    fun saveWeatherConfig() {
        val state = _weatherState.value
        val lat = state.latitude.toDoubleOrNull()
        val lon = state.longitude.toDoubleOrNull()

        if (state.city.isBlank()) {
            _weatherState.update { it.copy(errorMessage = "City name cannot be empty.") }
            return
        }
        if (lat == null || lat !in -90.0..90.0) {
            _weatherState.update { it.copy(errorMessage = "Latitude must be a valid number between -90 and 90.") }
            return
        }
        if (lon == null || lon !in -180.0..180.0) {
            _weatherState.update { it.copy(errorMessage = "Longitude must be a valid number between -180 and 180.") }
            return
        }

        viewModelScope.launch {
            _weatherState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val config = com.leshoraa.kore.domain.model.WeatherLocationConfig(
                city = state.city.trim(),
                latitude = lat,
                longitude = lon,
                isEnabled = state.isEnabled,
                timezoneOffsetSec = state.timezoneOffsetSec
            )

            saveWeatherConfigUseCase(config).fold(
                onSuccess = {
                    _weatherState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Weather settings synced to KoRe!",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _weatherState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to sync weather settings to KoRe.",
                            successMessage = null
                        )
                    }
                }
            )
        }
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
