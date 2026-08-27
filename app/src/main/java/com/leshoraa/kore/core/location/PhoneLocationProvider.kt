package com.leshoraa.kore.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

data class PhoneLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val timezoneOffsetSec: Int
)

/**
 * Clean location provider extracting smartphone GPS/Network location
 * and reverse-geocoding the locality name for KoRe OLED display.
 */
class PhoneLocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "PhoneLocationProvider"
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun checkLocationSettings(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(context)
            client.checkLocationSettings(builder.build()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<PhoneLocation> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("Location permission not granted"))
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            // Try to get fresh current location instead of just last known
            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            
            val bestLocation = fusedLocationClient.getCurrentLocation(locationRequest, null).await()

            if (bestLocation == null) {
                return@withContext Result.failure(IllegalStateException("Unable to acquire fresh GPS location. Ensure GPS is active and has clear sky view."))
            }

            val lat = bestLocation.latitude
            val lon = bestLocation.longitude
            val tzOffsetSec = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000

            var detectedCity = "GPS Loc"
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            detectedCity = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            detectedCity = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reverse geocoding error: ${e.message}")
            }

            Log.i(TAG, "Detected location: $detectedCity ($lat, $lon), tz=$tzOffsetSec")
            Result.success(
                PhoneLocation(
                    latitude = lat,
                    longitude = lon,
                    cityName = detectedCity,
                    timezoneOffsetSec = tzOffsetSec
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring location", e)
            Result.failure(e)
        }
    }
}
