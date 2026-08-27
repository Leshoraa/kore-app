package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.model.NotificationEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for controlling BLE connection and dispatching event frames.
 */
interface BleRepository {
    val connectionState: StateFlow<Int>
    val isBluetoothEnabled: StateFlow<Boolean>
    val deviceConfigFlow: StateFlow<com.leshoraa.kore.domain.model.DeviceNetworkConfig?>
    val weatherConfigFlow: StateFlow<com.leshoraa.kore.domain.model.WeatherLocationConfig?>
    fun connect(address: String, deviceName: String? = null)
    fun disconnect()
    suspend fun sendNotification(event: NotificationEvent): Result<Unit>
    suspend fun sendBrightness(brightness: Int, save: Boolean = true): Result<Unit>
    suspend fun sendNavigation(event: NavEvent): Result<Unit>
    suspend fun sendExpression(expression: Expression?): Result<Unit>
    suspend fun sendDeviceConfig(config: com.leshoraa.kore.domain.model.DeviceNetworkConfig): Result<Unit>
    suspend fun queryDeviceConfig(): Result<Unit>
    suspend fun showClock(): Result<Unit>
    suspend fun showWeather(): Result<Unit>
    suspend fun sendWeatherConfig(config: com.leshoraa.kore.domain.model.WeatherLocationConfig): Result<Unit>
    suspend fun syncTime(epochSec: Long = System.currentTimeMillis() / 1000L, tzOffsetSec: Int = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000): Result<Unit>
    suspend fun pushWeatherData(city: String, temp: Float, hum: Int, code: Int, cond: String): Result<Unit>
    suspend fun fetchAndPushWeatherFromPhone(city: String, lat: Double, lon: Double): Result<Unit>
}

