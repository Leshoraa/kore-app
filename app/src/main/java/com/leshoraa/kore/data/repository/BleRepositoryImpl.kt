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

    override fun connect(address: String) {
        bleManager.connect(address)
    }

    override fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun sendNotification(event: NotificationEvent): Result<Unit> {
        return dispatchEngine.dispatch(event)
    }
}
