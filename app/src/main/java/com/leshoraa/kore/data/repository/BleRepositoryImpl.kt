package com.leshoraa.kore.data.repository

import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.data.dispatcher.BleDispatchEngine
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.BleRepository

/**
 * Concrete implementation of BleRepository connecting Domain logic to BleManager and Dispatcher.
 */
class BleRepositoryImpl(
    private val bleManager: BleManager,
    private val dispatchEngine: BleDispatchEngine = BleDispatchEngine(bleManager)
) : BleRepository {

    override val connectionState = bleManager.connectionState
    override val isBluetoothEnabled = bleManager.isBluetoothEnabled

    override fun connect(address: String, deviceName: String?) {
        bleManager.connect(address, deviceName)
    }

    override fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun sendNotification(event: NotificationEvent): Result<Unit> {
        return dispatchEngine.dispatch(event)
    }

    override suspend fun sendBrightness(brightness: Int, save: Boolean): Result<Unit> {
        return dispatchEngine.dispatchBrightness(brightness, save)
    }

    override suspend fun sendNavigation(event: com.leshoraa.kore.domain.model.NavEvent): Result<Unit> {
        return dispatchEngine.dispatchNavigation(event)
    }
}
