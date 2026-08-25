package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.NotificationEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for controlling BLE connection and dispatching event frames.
 */
interface BleRepository {
    val connectionState: StateFlow<Int>
    fun connect(address: String, deviceName: String? = null)
    fun disconnect()
    suspend fun sendNotification(event: NotificationEvent): Result<Unit>
}
