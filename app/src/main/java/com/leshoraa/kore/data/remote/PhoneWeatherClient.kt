package com.leshoraa.kore.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class PhoneWeatherData(
    val temperature: Float,
    val humidity: Int,
    val weatherCode: Int,
    val condition: String
)

/**
 * Android client that queries Open-Meteo forecast API using smartphone's Internet
 * (Cellular 4G/5G or Wi-Fi), allowing KoRe to receive weather updates even when KoRe has no Wi-Fi.
 */
class PhoneWeatherClient {

    companion object {
        private const val TAG = "PhoneWeatherClient"
        private const val TIMEOUT_MS = 5000

        fun mapWmoCodeToCondition(code: Int): String {
            return when {
                code == 0 -> "CLEAR"
                code == 1 -> "MAINLY CLEAR"
                code == 2 -> "PARTLY CLOUDY"
                code == 3 -> "OVERCAST"
                code == 45 || code == 48 -> "FOG"
                code in 51..55 -> "DRIZZLE"
                code in 56..57 -> "FREEZING DRIZZLE"
                code in 61..65 -> "RAIN"
                code in 66..67 -> "FREEZING RAIN"
                code in 71..77 -> "SNOW"
                code in 80..82 -> "SHOWERS"
                code in 85..86 -> "SNOW SHOWERS"
                code in 95..99 -> "THUNDERSTORM"
                else -> "CLOUDY"
            }
        }
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): Result<PhoneWeatherData> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlStr = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,weather_code",
                latitude, longitude
            )
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.use { it.readText() }
                val root = JSONObject(responseText)
                val current = root.getJSONObject("current")

                val temp = current.getDouble("temperature_2m").toFloat()
                val hum = current.getInt("relative_humidity_2m")
                val code = current.getInt("weather_code")
                val cond = mapWmoCodeToCondition(code)

                Log.i(TAG, "Fetched weather from phone: $temp°C, $hum%, $cond (code $code)")
                return@withContext Result.success(PhoneWeatherData(temp, hum, code, cond))
            }
            Result.failure(IllegalStateException("Open-Meteo HTTP ${connection.responseCode}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather on phone", e)
            Result.failure(e)
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }
}
