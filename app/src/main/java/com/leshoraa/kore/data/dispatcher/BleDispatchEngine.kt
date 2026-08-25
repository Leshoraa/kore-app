package com.leshoraa.kore.data.dispatcher

import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.ble.BlePacketFraming
import com.leshoraa.kore.domain.model.NotificationEvent
import android.util.Log

/**
 * Handles the logic of converting Domain Events into fragments and sending them via BleManager.
 */
class BleDispatchEngine(private val bleManager: BleManager) {

    companion object {
        private const val TAG = "BleDispatchEngine"
    }

    suspend fun dispatch(event: NotificationEvent): Result<Unit> {
        return runCatching {
            Log.d(TAG, "Packing event for ${event.packageName}")
            val fullFrame = BlePacketFraming.pack(event)
            
            val mtu = bleManager.negotiatedMtu.value
            val chunks = BlePacketFraming.fragment(fullFrame, mtu)
            
            Log.d(TAG, "Sending ${chunks.size} chunks with MTU $mtu")
            
            chunks.forEachIndexed { index, chunk ->
                bleManager.writeData(chunk).onFailure { error ->
                    Log.e(TAG, "Failed to send chunk $index", error)
                    throw error
                }
            }
        }
    }
}
