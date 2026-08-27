package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.core.location.PhoneLocation
import com.leshoraa.kore.core.location.PhoneLocationProvider

/**
 * UseCase to acquire smartphone GPS location and reverse-geocode locality.
 */
class GetPhoneLocationUseCase(private val phoneLocationProvider: PhoneLocationProvider) {
    fun hasLocationPermission(): Boolean {
        return phoneLocationProvider.hasLocationPermission()
    }

    suspend fun checkSettings(): Result<Unit> {
        return phoneLocationProvider.checkLocationSettings()
    }

    suspend operator fun invoke(): Result<PhoneLocation> {
        return phoneLocationProvider.getCurrentLocation()
    }
}
