package com.leshoraa.kore.core.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Core BLE engine for managing GATT connections and communications.
 */
@SuppressLint("MissingPermission")
class BleManager(
    private val context: Context,
    private val operationQueue: BleOperationQueue = BleOperationQueue()
) {
    companion object {
        private const val TAG = "BleManager"
        
        // ESP32-S3 Target UUIDs (Change as needed to match firmware)
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // RX from ESP side
        
        const val DEFAULT_MTU = 517
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _negotiatedMtu = MutableStateFlow(23)
    val negotiatedMtu = _negotiatedMtu.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                // Catastrophic status 133 or other error: close and cleanup
                handleGattError(gatt)
                return
            }

            _connectionState.value = newState
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
                scope.launch {
                    operationQueue.enqueue { gatt.requestMtu(DEFAULT_MTU) }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
                bluetoothGatt = null
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "MTU changed to $mtu")
                _negotiatedMtu.value = mtu
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "Services discovered with status $status")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            // Log write status
        }
    }

    fun connect(address: String) {
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    private fun handleGattError(gatt: BluetoothGatt) {
        Log.e(TAG, "GATT error detected. Closing handle to prevent status 133 leaks.")
        gatt.disconnect()
        gatt.close()
        bluetoothGatt = null
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
    }

    suspend fun writeData(data: ByteArray): Result<Unit> {
        val gatt = bluetoothGatt ?: return Result.failure(Exception("Not connected"))
        val service = gatt.getService(SERVICE_UUID) ?: return Result.failure(Exception("Service not found"))
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID_TX) ?: return Result.failure(Exception("Char not found"))

        return operationQueue.enqueue {
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val success = gatt.writeCharacteristic(characteristic)
            if (!success) throw Exception("writeCharacteristic failed")
        }
    }
}
