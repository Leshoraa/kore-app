package com.leshoraa.kore.core.ble

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Serialized FIFO queue for Bluetooth GATT operations.
 * Android's BLE stack is not thread-safe and cannot handle concurrent GATT requests.
 */
class BleOperationQueue(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val operationTimeoutMs: Long = 2000L
) {
    private val mutex = Mutex()

    /**
     * Enqueues a GATT operation and waits for its completion.
     * 
     * @param operation The lambda that initiates the GATT call.
     * @return Result of the operation.
     */
    suspend fun <T> enqueue(operation: suspend () -> T): Result<T> = withContext(dispatcher) {
        mutex.withLock {
            runCatching {
                withTimeout(operationTimeoutMs) {
                    operation()
                }
            }
        }
    }
}
