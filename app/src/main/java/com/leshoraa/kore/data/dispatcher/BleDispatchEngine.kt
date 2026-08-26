package com.leshoraa.kore.data.dispatcher

import android.util.Log
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.model.NotificationEvent

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Handles the logic of converting Domain Events into fragments and sending them via BleManager.
 */
class BleDispatchEngine(private val bleManager: BleManager) {

    companion object {
        private const val TAG = "BleDispatchEngine"
    }

    private val sendMutex = Mutex()

    suspend fun dispatch(event: NotificationEvent): Result<Unit> = sendMutex.withLock {
        return@withLock runCatching {
            Log.d(TAG, "Packing event for ${event.packageName}")
            
            // Format as pure UTF-8 JSON with escaping to prevent invalid JSON
            val escapedApp = escapeJson(event.appName)
            val escapedTitle = escapeJson(event.title)
            val escapedMsg = escapeJson(event.text)
            
            val payload = """{"app":"$escapedApp","title":"$escapedTitle","message":"$escapedMsg"}"""
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

    suspend fun dispatchBrightness(brightness: Int, save: Boolean = true): Result<Unit> = sendMutex.withLock {
        return@withLock runCatching {
            val clamped = brightness.coerceIn(0, 255)
            val payload = """{"cmd":"set_brightness","brightness":$clamped,"save":$save}"""
            val data = payload.toByteArray(Charsets.UTF_8)
            val mtu = bleManager.negotiatedMtu.value
            val chunks = fragment(data, mtu)

            Log.d(TAG, "Dispatching brightness $clamped (save=$save) in ${chunks.size} chunks")

            chunks.forEachIndexed { index, chunk ->
                bleManager.writeData(chunk).onFailure { error ->
                    Log.e(TAG, "Failed to send brightness chunk $index", error)
                    throw error
                }
            }
        }
    }

    suspend fun dispatchNavigation(event: NavEvent): Result<Unit> = sendMutex.withLock {
        return@withLock runCatching {
            val escapedIcon = escapeJson(event.icon)
            val escapedDist = escapeJson(event.distance)
            val escapedInst = escapeJson(event.instruction)
            val escapedStreet = escapeJson(event.street)
            val escapedEta = escapeJson(event.eta)
            val escapedDur = escapeJson(event.duration)
            val escapedTotDist = escapeJson(event.totalDistance)
            val isActive = event.isActive

            val payload = """{"cmd":"nav","active":$isActive,"icon":"$escapedIcon","dist":"$escapedDist","inst":"$escapedInst","street":"$escapedStreet","eta":"$escapedEta","dur":"$escapedDur","tot_dist":"$escapedTotDist"}"""
            val data = payload.toByteArray(Charsets.UTF_8)
            val mtu = bleManager.negotiatedMtu.value
            val chunks = fragment(data, mtu)

            Log.d(TAG, "Dispatching navigation [active=$isActive, icon=$escapedIcon, dist=$escapedDist, eta=$escapedEta, dur=$escapedDur] in ${chunks.size} chunks")

            chunks.forEachIndexed { index, chunk ->
                bleManager.writeData(chunk).onFailure { error ->
                    Log.e(TAG, "Failed to send navigation chunk $index", error)
                    throw error
                }
            }
        }
    }

    /**
     * Serializes and transmits an expression selection command to the companion device.
     *
     * @param expression Target [Expression] to set, or null to restore autonomous Auto Mood.
     */
    suspend fun dispatchExpression(expression: Expression?): Result<Unit> = sendMutex.withLock {
        return@withLock runCatching {
            val exprVal = expression?.code?.toString() ?: "\"auto\""
            val payload = """{"cmd":"set_expression","expr":$exprVal}"""
            val data = payload.toByteArray(Charsets.UTF_8)
            val mtu = bleManager.negotiatedMtu.value
            val chunks = fragment(data, mtu)

            Log.d(TAG, "Dispatching expression [$exprVal] in ${chunks.size} chunks: $payload")

            chunks.forEachIndexed { index, chunk ->
                bleManager.writeData(chunk).onFailure { error ->
                    Log.e(TAG, "Failed to send expression chunk $index", error)
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

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
