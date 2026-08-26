package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.DeviceNetworkConfig
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository

/**
 * Retrieves the current device network configuration.
 * Fetches live configuration from the hardware over HTTP if available,
 * falling back to locally cached preferences.
 */
class GetDeviceConfigUseCase(
    private val bleRepository: com.leshoraa.kore.domain.repository.BleRepository,
    private val cameraVisionRepository: CameraVisionRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(): DeviceNetworkConfig {
        val cached = preferencesRepository.getCachedDeviceConfig()

        // 1. If BLE is connected, send query command
        if (bleRepository.connectionState.value == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
            bleRepository.queryDeviceConfig()
        }

        // 2. If HTTP is available, fetch live config
        val hostsToTry = mutableListOf<String>()
        val configuredHost = preferencesRepository.getCameraHost()
        if (configuredHost.isNotBlank()) hostsToTry.add(configuredHost)
        if (!hostsToTry.contains("192.168.18.16")) hostsToTry.add("192.168.18.16")
        if (!hostsToTry.contains("192.168.4.1")) hostsToTry.add("192.168.4.1")

        for (targetHost in hostsToTry) {
            cameraVisionRepository.fetchDeviceConfig(targetHost, 80).onSuccess { remoteConfig ->
                val merged = DeviceNetworkConfig(
                    staSsid = remoteConfig.staSsid.ifBlank { cached.staSsid },
                    staPass = remoteConfig.staPass.ifBlank { cached.staPass },
                    apSsid = remoteConfig.apSsid.ifBlank { cached.apSsid },
                    apPass = remoteConfig.apPass.ifBlank { cached.apPass },
                    bleName = remoteConfig.bleName.ifBlank { cached.bleName }
                )
                preferencesRepository.setCachedDeviceConfig(merged)
                preferencesRepository.setCameraHost(targetHost)
                return merged
            }
        }
        return cached
    }
}
