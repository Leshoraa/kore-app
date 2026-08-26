package com.leshoraa.kore.domain.usecase

import android.bluetooth.BluetoothProfile
import android.util.Log
import com.leshoraa.kore.domain.model.DeviceNetworkConfig
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository

/**
 * Saves and applies Wi-Fi STA, AP Hotspot, and Bluetooth configurations to the KoRe device.
 * Automatically tries active transport:
 * 1. BLE characteristic write (if BLE is connected).
 * 2. HTTP `/save_wifi` (if Wi-Fi / Hotspot cameraHost is reachable).
 * Also caches the applied configuration locally in preferences.
 */
class SaveDeviceConfigUseCase(
    private val bleRepository: BleRepository,
    private val cameraVisionRepository: CameraVisionRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "SaveDeviceConfigUseCase"
    }

    suspend operator fun invoke(config: DeviceNetworkConfig): Result<Unit> {
        val isBleConnected = bleRepository.connectionState.value == BluetoothProfile.STATE_CONNECTED

        var sentViaBle = false
        var sentViaHttp = false
        var lastError: Throwable? = null

        // 1. Try BLE dispatch if connected
        if (isBleConnected) {
            Log.d(TAG, "Attempting config dispatch via BLE...")
            bleRepository.sendDeviceConfig(config).fold(
                onSuccess = {
                    Log.i(TAG, "Device config successfully dispatched via BLE")
                    sentViaBle = true
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed dispatching via BLE: ${error.message}", error)
                    lastError = error
                }
            )
        }

        // 2. Try HTTP dispatch
        val hostsToTry = mutableListOf<String>()
        val configuredHost = preferencesRepository.getCameraHost()
        if (configuredHost.isNotBlank()) {
            hostsToTry.add(configuredHost)
        }
        if (!hostsToTry.contains("192.168.18.16")) hostsToTry.add("192.168.18.16")
        if (!hostsToTry.contains("192.168.4.1")) hostsToTry.add("192.168.4.1")

        if (!sentViaBle) {
            for (targetHost in hostsToTry) {
                Log.d(TAG, "Attempting config dispatch via HTTP to $targetHost:80...")
                cameraVisionRepository.saveDeviceConfig(targetHost, 80, config).fold(
                    onSuccess = {
                        Log.i(TAG, "Device config successfully dispatched via HTTP to $targetHost")
                        sentViaHttp = true
                        preferencesRepository.setCameraHost(targetHost)
                        return@fold
                    },
                    onFailure = { error ->
                        Log.w(TAG, "Failed dispatching via HTTP to $targetHost: ${error.message}")
                        if (lastError == null) lastError = error
                    }
                )
                if (sentViaHttp) break
            }
        }

        // Always cache preference locally
        preferencesRepository.setCachedDeviceConfig(config)

        return if (sentViaBle || sentViaHttp) {
            Result.success(Unit)
        } else {
            Result.failure(lastError ?: IllegalStateException("KoRe is not connected via BLE or Wi-Fi. Preferences cached locally."))
        }
    }
}
