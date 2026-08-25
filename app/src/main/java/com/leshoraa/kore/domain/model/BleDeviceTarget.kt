package com.leshoraa.kore.domain.model

/**
 * Configuration for a target Bluetooth Low Energy device.
 */
data class BleDeviceTarget(
    val macAddress: String,
    val name: String? = null,
    val rssiThreshold: Int = -80
)
