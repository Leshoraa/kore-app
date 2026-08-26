package com.leshoraa.kore.core.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

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
        
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        
        const val DEFAULT_MTU = 517
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName = _connectedDeviceName.asStateFlow()

    private val _negotiatedMtu = MutableStateFlow(23)
    val negotiatedMtu = _negotiatedMtu.asStateFlow()

    val isBluetoothEnabled: StateFlow<Boolean> = callbackFlow {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(bluetoothAdapter.isEnabled)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    trySend(state == BluetoothAdapter.STATE_ON)
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }.stateIn(scope, SharingStarted.Eagerly, BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false)

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
                scope.launch {
                    kotlinx.coroutines.delay(150)
                    operationQueue.enqueue { gatt.requestMtu(DEFAULT_MTU) }
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectedDeviceName.value = null
                gatt.close()
                bluetoothGatt = null
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "MTU negotiated to $mtu")
                _negotiatedMtu.value = mtu
            } else {
                Log.w(TAG, "MTU negotiation status $status, keeping ${_negotiatedMtu.value}")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "Services discovered with status $status")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d(TAG, "onCharacteristicWrite: UUID=${characteristic.uuid}, status=$status")
        }
    }

    fun connect(address: String, deviceName: String? = null) {
        _connectionState.value = BluetoothProfile.STATE_CONNECTING
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
        _connectedDeviceName.value = deviceName ?: device.name
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTING
        bluetoothGatt?.disconnect()
    }

    private fun handleGattError(gatt: BluetoothGatt) {
        Log.e(TAG, "GATT error detected. Closing handle to prevent status 133 leaks.")
        gatt.disconnect()
        gatt.close()
        bluetoothGatt = null
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        _connectedDeviceName.value = null
    }

    suspend fun writeData(data: ByteArray): Result<Unit> {
        val gatt = bluetoothGatt ?: return Result.failure(Exception("Not connected"))
        val service = gatt.getService(SERVICE_UUID) ?: return Result.failure(Exception("Service not found"))
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID_RX) ?: return Result.failure(Exception("Char not found"))

        return operationQueue.enqueue {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val res = gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                if (res != BluetoothStatusCodes.SUCCESS) {
                    val fallback = gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                    if (fallback != BluetoothStatusCodes.SUCCESS) {
                        Log.e(TAG, "GATT writeCharacteristic failed: code=$res, fallback=$fallback")
                        throw Exception("writeCharacteristic failed with code $res")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                val success = gatt.writeCharacteristic(characteristic)
                if (!success) {
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    @Suppress("DEPRECATION")
                    val fallback = gatt.writeCharacteristic(characteristic)
                    if (!fallback) {
                        Log.e(TAG, "GATT writeCharacteristic failed on legacy API")
                        throw Exception("writeCharacteristic failed")
                    }
                }
            }
        }
    }
}

