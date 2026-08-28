package com.leshoraa.kore.core.common

import android.content.Context
import android.content.SharedPreferences
import com.leshoraa.kore.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent user preferences (e.g. OLED brightness, theme, sync rules).
 */
class PreferencesManager(context: Context) : UserPreferencesRepository {

    companion object {
        private const val PREFS_NAME = "kore_preferences"
        private const val KEY_BRIGHTNESS = "key_oled_brightness"
        private const val KEY_AUTO_SYNC_BRIGHTNESS = "key_auto_sync_brightness"
        private const val KEY_SELECTED_EXPRESSION = "key_selected_expression"
        private const val KEY_CAMERA_HOST = "key_camera_host"
        
        private const val KEY_LAST_STA_SSID = "key_last_sta_ssid"
        private const val KEY_LAST_AP_SSID = "key_last_ap_ssid"
        private const val KEY_LAST_BLE_NAME = "key_last_ble_name"
        private const val KEY_LAST_BLE_ADDR = "key_last_ble_addr"
        private const val KEY_LAST_DEV_NAME = "key_last_dev_name"
        
        private const val KEY_WEATHER_CITY = "key_weather_city"
        private const val KEY_WEATHER_LAT = "key_weather_lat"
        private const val KEY_WEATHER_LON = "key_weather_lon"
        private const val KEY_WEATHER_ENABLED = "key_weather_enabled"
        private const val KEY_WEATHER_TZ = "key_weather_tz"
        private const val KEY_SYSTEM_ACCESS_TIP_DISMISSED = "key_system_access_tip_dismissed"
        private const val KEY_MOMENT_AUTO_CAPTURE_ENABLED = "key_moment_auto_capture_enabled"
        private const val KEY_MOMENT_CAPTURE_INTERVAL_MIN = "key_moment_capture_interval_min"
        
        const val DEFAULT_BRIGHTNESS = 100
        const val MIN_BRIGHTNESS = 1
        const val MAX_BRIGHTNESS = 100
        const val DEFAULT_CAMERA_HOST = "192.168.18.16"
        const val DEFAULT_MOMENT_INTERVAL_MIN = 60
    }

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _momentAutoCaptureEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_MOMENT_AUTO_CAPTURE_ENABLED, false)
    )
    override val momentAutoCaptureEnabled: StateFlow<Boolean> = _momentAutoCaptureEnabled.asStateFlow()

    private val _momentCaptureIntervalMinutes = MutableStateFlow(
        sharedPreferences.getInt(KEY_MOMENT_CAPTURE_INTERVAL_MIN, DEFAULT_MOMENT_INTERVAL_MIN)
    )
    override val momentCaptureIntervalMinutes: StateFlow<Int> = _momentCaptureIntervalMinutes.asStateFlow()

    private val _brightness = MutableStateFlow(
        sharedPreferences.getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    )
    override val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _autoSyncBrightness = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_AUTO_SYNC_BRIGHTNESS, true)
    )
    override val autoSyncBrightness: StateFlow<Boolean> = _autoSyncBrightness.asStateFlow()

    private val _selectedExpressionCode = MutableStateFlow(
        valToExpressionCode(sharedPreferences.getInt(KEY_SELECTED_EXPRESSION, -1))
    )
    override val selectedExpressionCode: StateFlow<Int?> = _selectedExpressionCode.asStateFlow()

    private val _cameraHost = MutableStateFlow(
        sharedPreferences.getString(KEY_CAMERA_HOST, DEFAULT_CAMERA_HOST) ?: DEFAULT_CAMERA_HOST
    )
    override val cameraHost: StateFlow<String> = _cameraHost.asStateFlow()

    override fun getBrightness(): Int = _brightness.value

    override fun setBrightness(value: Int) {
        val clamped = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        _brightness.value = clamped
        sharedPreferences.edit().putInt(KEY_BRIGHTNESS, clamped).apply()
    }

    override fun isAutoSyncEnabled(): Boolean = _autoSyncBrightness.value

    override fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncBrightness.value = enabled
        sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC_BRIGHTNESS, enabled).apply()
    }

    override fun getSelectedExpressionCode(): Int? = _selectedExpressionCode.value

    override fun setSelectedExpressionCode(code: Int?) {
        _selectedExpressionCode.value = code
        if (code == null || code < 0) {
            sharedPreferences.edit().putInt(KEY_SELECTED_EXPRESSION, -1).apply()
        } else {
            sharedPreferences.edit().putInt(KEY_SELECTED_EXPRESSION, code).apply()
        }
    }

    override fun getCameraHost(): String = _cameraHost.value

    override fun setCameraHost(host: String) {
        val cleaned = host.trim()
        _cameraHost.value = cleaned
        sharedPreferences.edit().putString(KEY_CAMERA_HOST, cleaned).apply()
    }

    override fun getCachedDeviceConfig(): com.leshoraa.kore.domain.model.DeviceNetworkConfig {
        return com.leshoraa.kore.domain.model.DeviceNetworkConfig(
            staSsid = sharedPreferences.getString(KEY_LAST_STA_SSID, "") ?: "",
            staPass = "",
            apSsid = sharedPreferences.getString(KEY_LAST_AP_SSID, "KoRe") ?: "KoRe",
            apPass = "",
            bleName = sharedPreferences.getString(KEY_LAST_BLE_NAME, "KoRe-Sense") ?: "KoRe-Sense"
        )
    }

    override fun setCachedDeviceConfig(config: com.leshoraa.kore.domain.model.DeviceNetworkConfig) {
        sharedPreferences.edit()
            .putString(KEY_LAST_STA_SSID, config.staSsid)
            .putString(KEY_LAST_AP_SSID, config.apSsid)
            .putString(KEY_LAST_BLE_NAME, config.bleName)
            .apply()
    }

    override fun getLastConnectedBleAddress(): String? {
        return sharedPreferences.getString(KEY_LAST_BLE_ADDR, null)
    }

    override fun setLastConnectedBleAddress(address: String?) {
        if (address.isNullOrBlank()) {
            sharedPreferences.edit().remove(KEY_LAST_BLE_ADDR).apply()
        } else {
            sharedPreferences.edit().putString(KEY_LAST_BLE_ADDR, address.trim()).apply()
        }
    }

    override fun getLastConnectedBleName(): String? {
        return sharedPreferences.getString(KEY_LAST_DEV_NAME, null)
    }

    override fun setLastConnectedBleName(name: String?) {
        if (name.isNullOrBlank()) {
            sharedPreferences.edit().remove(KEY_LAST_DEV_NAME).apply()
        } else {
            sharedPreferences.edit().putString(KEY_LAST_DEV_NAME, name.trim()).apply()
        }
    }

    override fun getCachedWeatherConfig(): com.leshoraa.kore.domain.model.WeatherLocationConfig {
        return com.leshoraa.kore.domain.model.WeatherLocationConfig(
            city = sharedPreferences.getString(KEY_WEATHER_CITY, "Jakarta") ?: "Jakarta",
            latitude = java.lang.Double.longBitsToDouble(sharedPreferences.getLong(KEY_WEATHER_LAT, java.lang.Double.doubleToRawLongBits(-6.2088))),
            longitude = java.lang.Double.longBitsToDouble(sharedPreferences.getLong(KEY_WEATHER_LON, java.lang.Double.doubleToRawLongBits(106.8456))),
            isEnabled = sharedPreferences.getBoolean(KEY_WEATHER_ENABLED, true),
            timezoneOffsetSec = sharedPreferences.getInt(KEY_WEATHER_TZ, 25200)
        )
    }

    override fun setCachedWeatherConfig(config: com.leshoraa.kore.domain.model.WeatherLocationConfig) {
        sharedPreferences.edit()
            .putString(KEY_WEATHER_CITY, config.city)
            .putLong(KEY_WEATHER_LAT, java.lang.Double.doubleToRawLongBits(config.latitude))
            .putLong(KEY_WEATHER_LON, java.lang.Double.doubleToRawLongBits(config.longitude))
            .putBoolean(KEY_WEATHER_ENABLED, config.isEnabled)
            .putInt(KEY_WEATHER_TZ, config.timezoneOffsetSec)
            .apply()
    }

    override fun isSystemAccessTipDismissed(): Boolean {
        return sharedPreferences.getBoolean(KEY_SYSTEM_ACCESS_TIP_DISMISSED, false)
    }

    override fun setSystemAccessTipDismissed(dismissed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SYSTEM_ACCESS_TIP_DISMISSED, dismissed).apply()
    }

    override fun isMomentAutoCaptureEnabled(): Boolean = _momentAutoCaptureEnabled.value

    override fun setMomentAutoCaptureEnabled(enabled: Boolean) {
        _momentAutoCaptureEnabled.value = enabled
        sharedPreferences.edit().putBoolean(KEY_MOMENT_AUTO_CAPTURE_ENABLED, enabled).apply()
    }

    override fun getMomentCaptureIntervalMinutes(): Int = _momentCaptureIntervalMinutes.value

    override fun setMomentCaptureIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(15, 360)
        _momentCaptureIntervalMinutes.value = clamped
        sharedPreferences.edit().putInt(KEY_MOMENT_CAPTURE_INTERVAL_MIN, clamped).apply()
    }

    private fun valToExpressionCode(value: Int): Int? {
        return if (value in 0..7) value else null
    }
}

