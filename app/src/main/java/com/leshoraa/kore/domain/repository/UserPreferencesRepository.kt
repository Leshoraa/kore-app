package com.leshoraa.kore.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Pure Kotlin repository interface for user preferences and persistent settings.
 */
interface UserPreferencesRepository {
    val brightness: StateFlow<Int>
    val autoSyncBrightness: StateFlow<Boolean>
    val selectedExpressionCode: StateFlow<Int?>
    val cameraHost: StateFlow<String>
    
    fun getBrightness(): Int
    fun setBrightness(value: Int)
    fun isAutoSyncEnabled(): Boolean
    fun setAutoSyncEnabled(enabled: Boolean)
    fun getSelectedExpressionCode(): Int?
    fun setSelectedExpressionCode(code: Int?)
    fun getCameraHost(): String
    fun setCameraHost(host: String)
    fun getCachedDeviceConfig(): com.leshoraa.kore.domain.model.DeviceNetworkConfig
    fun setCachedDeviceConfig(config: com.leshoraa.kore.domain.model.DeviceNetworkConfig)
    fun getLastConnectedBleAddress(): String?
    fun setLastConnectedBleAddress(address: String?)
    fun getLastConnectedBleName(): String?
    fun setLastConnectedBleName(name: String?)
    fun getCachedWeatherConfig(): com.leshoraa.kore.domain.model.WeatherLocationConfig
    fun setCachedWeatherConfig(config: com.leshoraa.kore.domain.model.WeatherLocationConfig)
}

