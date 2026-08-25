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
            
            // Format as pure UTF-8 JSON as per requirements
            // {"app":"<AppName>","title":"<Title>","message":"<Content>"}
            val payload = """{"app":"${event.appName}","title":"${event.title}","message":"${event.text}"}"""
            val data = payload.toByteArray(Charsets.UTF_8)
            
            val mtu = bleManager.negotiatedMtu.value
            
            val chunks = fragment(data, mtu)
            
            Log.d(TAG, "Sending ${chunks.size} chunks with MTU $mtu: $payload")
            
            chunks.forEachIndexed { index, chunk ->
                bleManager.writeData(chunk).onFailure { error ->
                    Log.e(TAG, "Failed to send chunk $index", error)
                    throw error
                }
            }
        }
    }

    /**
     * Splits data into fragments that fit within the MTU.
     */
    private fun fragment(data: ByteArray, mtu: Int): List<ByteArray> {
        val payloadPerChunk = mtu - 3 // 3 bytes for opcode + attribute handle
        if (data.size <= payloadPerChunk) return listOf(data)
        
        val chunks = mutableListOf<ByteArray>()
        var start = 0
        while (start < data.size) {
            val end = (start + payloadPerChunk).coerceAtMost(data.size)
            chunks.add(data.copyOfRange(start, end))
            start = end
        }
        return chunks
    }
}
