package com.leshoraa.kore.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.ble.BleScanner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BleScannerViewModel(
    private val bleScanner: BleScanner,
    private val bleManager: BleManager
) : ViewModel() {

    val foundDevices = bleScanner.foundDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning = bleScanner.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val connectionState = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startScan() = bleScanner.startScan()
    fun stopScan() = bleScanner.stopScan()

    fun connect(address: String) {
        bleScanner.stopScan()
        bleManager.connect(address)
    }

    fun disconnect() = bleManager.disconnect()
}
