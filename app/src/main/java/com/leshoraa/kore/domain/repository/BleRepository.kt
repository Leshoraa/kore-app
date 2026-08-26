package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.model.NotificationEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for controlling BLE connection and dispatching event frames.
 */
interface BleRepository {
    val connectionState: StateFlow<Int>
    val isBluetoothEnabled: StateFlow<Boolean>
    fun connect(address: String, deviceName: String? = null)
    fun disconnect()
    suspend fun sendNotification(event: NotificationEvent): Result<Unit>
    suspend fun sendBrightness(brightness: Int, save: Boolean = true): Result<Unit>
    suspend fun sendNavigation(event: NavEvent): Result<Unit>
}
