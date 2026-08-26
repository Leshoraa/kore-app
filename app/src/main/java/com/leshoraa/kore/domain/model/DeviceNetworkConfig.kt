package com.leshoraa.kore.domain.model

/**
 * Encapsulates network and hardware identity configurations for the KoRe device:
 * 1. Wi-Fi STA: Router connection credentials.
 * 2. Access Point (AP): KoRe standalone hotspot SSID and password.
 * 3. Bluetooth (BLE): Broadcast device name.
 */
data class DeviceNetworkConfig(
    val staSsid: String = "",
    val staPass: String = "",
    val apSsid: String = "KoRe",
    val apPass: String = "12345678",
    val bleName: String = "KoRe-Sense"
)
