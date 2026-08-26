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
        
        const val DEFAULT_BRIGHTNESS = 100
        const val MIN_BRIGHTNESS = 1
        const val MAX_BRIGHTNESS = 100
        const val DEFAULT_CAMERA_HOST = "192.168.18.16"
    }

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    private fun valToExpressionCode(value: Int): Int? {
        return if (value in 0..7) value else null
    }
}

