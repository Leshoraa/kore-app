package com.leshoraa.kore.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.core.ble.BleScanner
import com.leshoraa.kore.domain.repository.BleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BleScannerViewModel(
    private val bleScanner: BleScanner,
    private val bleRepository: BleRepository
) : ViewModel() {

    val foundDevices = bleScanner.foundDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning = bleScanner.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBluetoothEnabled = bleRepository.isBluetoothEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val connectionState = bleRepository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startScan() = bleScanner.startScan()
    fun stopScan() = bleScanner.stopScan()

    fun connect(address: String, deviceName: String?) {
        bleScanner.stopScan()
        bleRepository.connect(address, deviceName)
    }

    fun disconnect() = bleRepository.disconnect()

    override fun onCleared() {
        stopScan()
    }
}
