package com.leshoraa.kore.core.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.*

/**
 * Core BLE service for managing GATT connections, auto-reconnection, and communications.
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
        private const val AUTO_RECONNECT_DELAY_MS = 3500L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName = _connectedDeviceName.asStateFlow()

    private val _negotiatedMtu = MutableStateFlow(23)
    val negotiatedMtu = _negotiatedMtu.asStateFlow()

    private val _deviceConfigFlow = MutableStateFlow<com.leshoraa.kore.domain.model.DeviceNetworkConfig?>(null)
    val deviceConfigFlow: StateFlow<com.leshoraa.kore.domain.model.DeviceNetworkConfig?> = _deviceConfigFlow.asStateFlow()

    private val _weatherConfigFlow = MutableStateFlow<com.leshoraa.kore.domain.model.WeatherLocationConfig?>(null)
    val weatherConfigFlow: StateFlow<com.leshoraa.kore.domain.model.WeatherLocationConfig?> = _weatherConfigFlow.asStateFlow()

    private var userRequestedDisconnect = false
    private var autoReconnectJob: Job? = null

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
                    val isEnabled = (state == BluetoothAdapter.STATE_ON)
                    trySend(isEnabled)
                    if (isEnabled) {
                        reconnectLastDevice()
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }.stateIn(scope, SharingStarted.Eagerly, BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false)

    init {
        // Automatically attempt to reconnect to last known device if Bluetooth is on
        scope.launch {
            delay(1000)
            if (isBluetoothEnabled.value) {
                reconnectLastDevice()
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT connection status error: $status, closing connection handle.")
                handleGattError(gatt)
                return
            }

            _connectionState.value = newState
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                autoReconnectJob?.cancel()
                autoReconnectJob = null
                scope.launch {
                    // Allow connection handshake to stabilize before service discovery
                    delay(350)
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectedDeviceName.value = null
                handleGattError(gatt)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "MTU negotiated to $mtu")
                _negotiatedMtu.value = mtu
            } else {
                Log.w(TAG, "MTU negotiation status $status, keeping ${_negotiatedMtu.value}")
            }
            // Request current config from KoRe once MTU is set
            scope.launch {
                delay(100)
                writeData("""{"cmd":"get_config"}""".toByteArray(Charsets.UTF_8))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "Services discovered with status $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Request balanced connection priority to prevent OS Doze dormancy
                try {
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request connection priority: ${e.message}")
                }

                val service = gatt.getService(SERVICE_UUID)
                val txChar = service?.getCharacteristic(CHARACTERISTIC_UUID_TX)
                if (txChar != null) {
                    gatt.setCharacteristicNotification(txChar, true)
                    val cccd = txChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    if (cccd != null) {
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(cccd)
                    }
                }

                // Request MTU after services are discovered
                scope.launch {
                    delay(200)
                    operationQueue.enqueue { gatt.requestMtu(DEFAULT_MTU) }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBlePacket(value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val v = characteristic.value ?: return
            handleIncomingBlePacket(v)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d(TAG, "onCharacteristicWrite: UUID=${characteristic.uuid}, status=$status")
        }
    }

    private fun handleIncomingBlePacket(bytes: ByteArray) {
        val text = String(bytes)
        Log.d(TAG, "BLE Notification received: $text")
        if (text.contains("\"ip\":")) {
            val ip = text.substringAfter("\"ip\":\"").substringBefore("\"")
            if (ip.isNotBlank() && ip.contains(".")) {
                Log.i(TAG, "Auto-configured Camera Host from BLE: $ip")
                com.leshoraa.kore.core.common.ServiceLocator.providePreferencesManager(context).setCameraHost(ip)
            }
        }

        if (text.contains("\"sta_ssid\":") || text.contains("\"ap_ssid\":") || text.contains("\"ble_name\":")) {
            val prefs = com.leshoraa.kore.core.common.ServiceLocator.providePreferencesManager(context)
            val current = prefs.getCachedDeviceConfig()
            val staSsid = if (text.contains("\"sta_ssid\":\"")) text.substringAfter("\"sta_ssid\":\"").substringBefore("\"") else current.staSsid
            val staPass = if (text.contains("\"sta_pass\":\"")) text.substringAfter("\"sta_pass\":\"").substringBefore("\"") else current.staPass
            val apSsid = if (text.contains("\"ap_ssid\":\"")) text.substringAfter("\"ap_ssid\":\"").substringBefore("\"").ifBlank { current.apSsid } else current.apSsid
            val apPass = if (text.contains("\"ap_pass\":\"")) text.substringAfter("\"ap_pass\":\"").substringBefore("\"") else current.apPass
            val bleName = if (text.contains("\"ble_name\":\"")) text.substringAfter("\"ble_name\":\"").substringBefore("\"").ifBlank { current.bleName } else current.bleName

            val updated = com.leshoraa.kore.domain.model.DeviceNetworkConfig(
                staSsid = staSsid,
                staPass = staPass,
                apSsid = apSsid,
                apPass = apPass,
                bleName = bleName
            )
            prefs.setCachedDeviceConfig(updated)
            _deviceConfigFlow.value = updated
            Log.i(TAG, "Auto-synced Device Setup from BLE: STA=$staSsid, AP=$apSsid, BLE=$bleName")
        }

        if (text.contains("\"city\":") && (text.contains("\"lat\":") || text.contains("\"temp\":"))) {
            val prefs = com.leshoraa.kore.core.common.ServiceLocator.providePreferencesManager(context)
            val current = prefs.getCachedWeatherConfig()
            val city = if (text.contains("\"city\":\"")) text.substringAfter("\"city\":\"").substringBefore("\"") else current.city
            val lat = if (text.contains("\"lat\":")) text.substringAfter("\"lat\":").substringBefore(",").substringBefore("}").toDoubleOrNull() ?: current.latitude else current.latitude
            val lon = if (text.contains("\"lon\":")) text.substringAfter("\"lon\":").substringBefore(",").substringBefore("}").toDoubleOrNull() ?: current.longitude else current.longitude
            val en = if (text.contains("\"enabled\":")) text.substringAfter("\"enabled\":").substringBefore(",").substringBefore("}").toBooleanStrictOrNull() ?: current.isEnabled else current.isEnabled
            val tz = if (text.contains("\"tz\":")) text.substringAfter("\"tz\":").substringBefore(",").substringBefore("}").toIntOrNull() ?: current.timezoneOffsetSec else current.timezoneOffsetSec

            val updated = com.leshoraa.kore.domain.model.WeatherLocationConfig(
                city = city,
                latitude = lat,
                longitude = lon,
                isEnabled = en,
                timezoneOffsetSec = tz
            )
            prefs.setCachedWeatherConfig(updated)
            _weatherConfigFlow.value = updated
            Log.i(TAG, "Auto-synced Weather Setup from BLE: City=$city, Lat=$lat, Lon=$lon, TZ=$tz")
        }
    }

    suspend fun queryDeviceConfig(): Result<Unit> {
        return writeData("""{"cmd":"get_config"}""".toByteArray(Charsets.UTF_8))
    }

    fun connect(address: String, deviceName: String? = null) {
        userRequestedDisconnect = false
        autoReconnectJob?.cancel()
        autoReconnectJob = null

        // Save last connected address and device name for seamless auto-reconnect
        val prefs = com.leshoraa.kore.core.common.ServiceLocator.providePreferencesManager(context)
        prefs.setLastConnectedBleAddress(address)
        if (deviceName != null) {
            prefs.setLastConnectedBleName(deviceName)
        }

        connectInternal(address, deviceName)
    }

    fun reconnectLastDevice() {
        val prefs = com.leshoraa.kore.core.common.ServiceLocator.providePreferencesManager(context)
        val lastAddr = prefs.getLastConnectedBleAddress()
        val lastName = prefs.getLastConnectedBleName()

        if (!lastAddr.isNullOrBlank() && !userRequestedDisconnect && _connectionState.value == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Attempting auto-reconnect to last device: $lastName ($lastAddr)")
            connectInternal(lastAddr, lastName)
        }
    }

    private fun connectInternal(address: String, deviceName: String?) {
        _connectionState.value = BluetoothProfile.STATE_CONNECTING
        mainHandler.post {
            try {
                bluetoothGatt?.let {
                    try {
                        it.disconnect()
                        it.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error closing previous gatt: ${e.message}")
                    }
                }
                bluetoothGatt = null

                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                val device = adapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                    triggerAutoReconnect()
                    return@post
                }
                _connectedDeviceName.value = deviceName ?: device.name

                bluetoothGatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    device.connectGatt(context, false, gattCallback)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during connectGatt: ${e.message}", e)
                _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                triggerAutoReconnect()
            }
        }
    }

    fun disconnect() {
        userRequestedDisconnect = true
        autoReconnectJob?.cancel()
        autoReconnectJob = null

        _connectionState.value = BluetoothProfile.STATE_DISCONNECTING
        mainHandler.post {
            try {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
            } catch (e: Exception) {
                Log.e(TAG, "Exception during disconnect: ${e.message}", e)
            } finally {
                _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                _connectedDeviceName.value = null
            }
        }
    }

    private fun handleGattError(gatt: BluetoothGatt) {
        Log.e(TAG, "GATT error / disconnection detected. Closing handle.")
        mainHandler.post {
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing gatt: ${e.message}")
            }
            if (bluetoothGatt == gatt) {
                bluetoothGatt = null
            }
            _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
            _connectedDeviceName.value = null

            if (!userRequestedDisconnect) {
                triggerAutoReconnect()
            }
        }
    }

    private fun triggerAutoReconnect() {
        if (userRequestedDisconnect) return
        val prefs = com.leshoraa.kore.core.common.ServiceLocator.providePreferencesManager(context)
        val lastAddr = prefs.getLastConnectedBleAddress() ?: return
        val lastName = prefs.getLastConnectedBleName()

        if (autoReconnectJob?.isActive == true) return

        autoReconnectJob = scope.launch {
            Log.i(TAG, "Scheduling auto-reconnect to $lastAddr in ${AUTO_RECONNECT_DELAY_MS}ms...")
            delay(AUTO_RECONNECT_DELAY_MS)
            if (!userRequestedDisconnect && _connectionState.value == BluetoothProfile.STATE_DISCONNECTED && isBluetoothEnabled.value) {
                Log.i(TAG, "Auto-reconnect executing for $lastAddr")
                connectInternal(lastAddr, lastName)
            }
        }
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
