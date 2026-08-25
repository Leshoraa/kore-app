package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.NotificationEvent

/**
 * Interface for controlling BLE connection and dispatching event frames.
 */
interface BleRepository {
    fun connect(address: String)
    fun disconnect()
    suspend fun sendNotification(event: NotificationEvent): Result<Unit>
}
