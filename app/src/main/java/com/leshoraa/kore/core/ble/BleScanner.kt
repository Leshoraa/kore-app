package com.leshoraa.kore.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper to discover nearby BLE devices.
 */
@SuppressLint("MissingPermission")
class BleScanner {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner = adapter?.bluetoothLeScanner
    
    private val _foundDevices = MutableStateFlow<List<ScanResult>>(emptyList())
    val foundDevices = _foundDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val currentList = _foundDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.device.address == result.device.address }
            if (existingIndex != -1) {
                currentList[existingIndex] = result
            } else {
                currentList.add(result)
            }
            _foundDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScanner", "Scan failed with error: $errorCode")
            _isScanning.value = false
        }
    }

    fun startScan() {
        if (scanner == null || _isScanning.value) return
        
        _foundDevices.value = emptyList()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        scanner.startScan(null, settings, scanCallback)
        _isScanning.value = true
    }

    fun stopScan() {
        if (scanner == null || !_isScanning.value) return
        scanner.stopScan(scanCallback)
        _isScanning.value = false
    }
}
