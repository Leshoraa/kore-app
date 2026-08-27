package com.leshoraa.kore.data.repository

import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.data.dispatcher.BleDispatcher
import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.BleRepository

/**
 * Concrete implementation of BleRepository connecting Domain logic to BleManager and Dispatcher.
 */
class BleRepositoryImpl(
    private val bleManager: BleManager,
    private val bleDispatcher: BleDispatcher = BleDispatcher(bleManager),
    private val phoneWeatherClient: com.leshoraa.kore.data.remote.PhoneWeatherClient = com.leshoraa.kore.data.remote.PhoneWeatherClient()
) : BleRepository {

    override val connectionState = bleManager.connectionState
    override val isBluetoothEnabled = bleManager.isBluetoothEnabled
    override val deviceConfigFlow = bleManager.deviceConfigFlow
    override val weatherConfigFlow = bleManager.weatherConfigFlow

    override fun connect(address: String, deviceName: String?) {
        bleManager.connect(address, deviceName)
    }

    override fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun sendNotification(event: NotificationEvent): Result<Unit> {
        return bleDispatcher.dispatch(event)
    }

    override suspend fun sendBrightness(brightness: Int, save: Boolean): Result<Unit> {
        return bleDispatcher.dispatchBrightness(brightness, save)
    }

    override suspend fun sendNavigation(event: NavEvent): Result<Unit> {
        return bleDispatcher.dispatchNavigation(event)
    }

    override suspend fun sendExpression(expression: Expression?): Result<Unit> {
        return bleDispatcher.dispatchExpression(expression)
    }

    override suspend fun sendDeviceConfig(config: com.leshoraa.kore.domain.model.DeviceNetworkConfig): Result<Unit> {
        return bleDispatcher.dispatchDeviceConfig(config)
    }

    override suspend fun queryDeviceConfig(): Result<Unit> {
        return bleManager.queryDeviceConfig()
    }

    override suspend fun showClock(): Result<Unit> {
        return bleDispatcher.dispatchShowClock()
    }

    override suspend fun showWeather(): Result<Unit> {
        return bleDispatcher.dispatchShowWeather()
    }

    override suspend fun sendWeatherConfig(config: com.leshoraa.kore.domain.model.WeatherLocationConfig): Result<Unit> {
        return bleDispatcher.dispatchWeatherConfig(config)
    }

    override suspend fun syncTime(epochSec: Long, tzOffsetSec: Int): Result<Unit> {
        return bleDispatcher.dispatchSyncTime(epochSec, tzOffsetSec)
    }

    override suspend fun pushWeatherData(city: String, temp: Float, hum: Int, code: Int, cond: String): Result<Unit> {
        return bleDispatcher.dispatchPushWeatherData(city, temp, hum, code, cond)
    }

    override suspend fun fetchAndPushWeatherFromPhone(city: String, lat: Double, lon: Double): Result<Unit> {
        return phoneWeatherClient.fetchWeather(lat, lon).fold(
            onSuccess = { data ->
                bleDispatcher.dispatchPushWeatherData(city, data.temperature, data.humidity, data.weatherCode, data.condition)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}

