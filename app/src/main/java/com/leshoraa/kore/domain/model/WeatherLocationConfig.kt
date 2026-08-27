package com.leshoraa.kore.domain.model

/**
 * Domain model representing Weather and Location configuration on KoRe.
 *
 * @property city Location / City name displayed on KoRe OLED screen.
 * @property latitude Geographic latitude for Open-Meteo API.
 * @property longitude Geographic longitude for Open-Meteo API.
 * @property isEnabled Whether periodic spontaneous weather glances are enabled.
 * @property timezoneOffsetSec Timezone offset in seconds (e.g. 25200 for UTC+7 / WIB).
 */
data class WeatherLocationConfig(
    val city: String = "Jakarta",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val isEnabled: Boolean = true,
    val timezoneOffsetSec: Int = 25200
) {
    companion object {
        val PRESETS = listOf(
            WeatherLocationConfig("Jakarta", -6.2088, 106.8456, true, 25200),
            WeatherLocationConfig("Bandung", -6.9175, 107.6191, true, 25200),
            WeatherLocationConfig("Surabaya", -7.2575, 112.7521, true, 25200),
            WeatherLocationConfig("Yogyakarta", -7.7956, 110.3695, true, 25200),
            WeatherLocationConfig("Semarang", -6.9667, 110.4167, true, 25200),
            WeatherLocationConfig("Denpasar (Bali)", -8.6705, 115.2126, true, 28800),
            WeatherLocationConfig("Medan", 3.5952, 98.6722, true, 25200),
            WeatherLocationConfig("Makassar", -5.1477, 119.4327, true, 28800),
            WeatherLocationConfig("Singapore", 1.3521, 103.8198, true, 28800),
            WeatherLocationConfig("Tokyo", 35.6762, 139.6503, true, 32400),
            WeatherLocationConfig("London", 51.5074, -0.1278, true, 0),
            WeatherLocationConfig("New York", 40.7128, -74.0060, true, -18000)
        )
    }
}
