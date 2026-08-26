package com.leshoraa.kore.data.remote

import android.util.Log
import com.leshoraa.kore.domain.model.TargetCandidate
import com.leshoraa.kore.domain.model.TelemetryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight HTTP client for JSON telemetry polling and camera sensor control on KoRe.
 */
class TelemetryHttpClient {

    companion object {
        private const val TAG = "TelemetryHttpClient"
        private const val TIMEOUT_MS = 3500

        fun formatBaseUrl(rawHost: String, port: Int = 80): String {
            val sanitized = MjpegStreamDecoder.sanitizeHost(rawHost)
            return "http://$sanitized:$port"
        }
    }

    /**
     * Periodically polls `/telemetry` endpoint and emits [TelemetryData] snapshots.
     *
     * @param baseUrl Base HTTP URL (e.g. "http://192.168.18.16:80").
     * @param intervalMs Polling period.
     */
    fun pollTelemetry(baseUrl: String, intervalMs: Long = 80L): Flow<TelemetryData> = flow {
        val endpointUrl = "$baseUrl/telemetry"

        while (currentCoroutineContext().isActive) {
            try {
                val data = fetchTelemetry(endpointUrl)
                if (data != null) {
                    emit(data)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry poll drop from $endpointUrl: ${e.message}")
            }
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Performs a single GET request to `/telemetry` and parses the JSON response.
     */
    suspend fun fetchTelemetry(endpointUrl: String): TelemetryData? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.use { it.readText() }
                return@withContext parseTelemetryJson(responseText)
            }
            Log.w(TAG, "GET $endpointUrl HTTP response: ${connection.responseCode}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed connecting to $endpointUrl: ${e.message}")
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Sends a POST request to `/camera_control` to adjust sensor parameters.
     *
     * @param baseUrl Base URL (e.g. "http://192.168.18.16:80").
     * @param param Parameter key (e.g. "brightness", "contrast", "saturation", "vflip", "hmirror", "aec", "agc").
     * @param value Integer value to set.
     */
    suspend fun postCameraControl(baseUrl: String, param: String, value: Int): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/camera_control")
            val payload = JSONObject().apply {
                put("param", param)
                put("val", value)
            }.toString()

            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Sensor control HTTP error $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    private fun parseTelemetryJson(jsonString: String): TelemetryData {
        val json = JSONObject(jsonString)

        val numCands = json.optInt("num_cands", 0)
        val candidatesList = mutableListOf<TargetCandidate>()

        for (i in 0 until 3) {
            val cx = json.optDouble("c${i}_cx", 0.0).toFloat()
            val cy = json.optDouble("c${i}_cy", 0.0).toFloat()
            val w = json.optDouble("c${i}_w", 0.0).toFloat()
            val h = json.optDouble("c${i}_h", 0.0).toFloat()
            val p = json.optDouble("c${i}_p", 0.0).toFloat()

            if (cx > 0f || cy > 0f) {
                candidatesList.add(TargetCandidate(i, cx, cy, w, h, p))
            }
        }

        return TelemetryData(
            detected = json.optBoolean("detected", false),
            cx = json.optDouble("cx", json.optDouble("c0_cx", 0.0)).toFloat(),
            cy = json.optDouble("cy", json.optDouble("c0_cy", 0.0)).toFloat(),
            w = json.optDouble("w", json.optDouble("c0_w", 0.0)).toFloat(),
            h = json.optDouble("h", json.optDouble("c0_h", 0.0)).toFloat(),
            errX = json.optDouble("err_x", 0.0).toFloat(),
            errY = json.optDouble("err_y", 0.0).toFloat(),
            conf = json.optDouble("conf", 0.0).toFloat(),
            fpsAi = json.optDouble("fps_ai", 0.0).toFloat(),
            humanLikelihood = json.optDouble("human_likelihood", 0.0).toFloat(),
            prox = json.optDouble("prox", 0.0).toFloat(),
            fw = json.optInt("fw", 640),
            fh = json.optInt("fh", 480),
            vx = json.optDouble("vx", 0.0).toFloat(),
            vy = json.optDouble("vy", 0.0).toFloat(),
            numCands = numCands,
            inspIdx = json.optInt("insp_idx", 0),
            candidates = candidatesList,
            expression = json.optInt("expr", 0),
            expressionName = json.optString("expr_name", "IDLE"),
            isManual = json.optBoolean("is_manual", false),
            valence = json.optDouble("valence", 0.0).toFloat(),
            arousal = json.optDouble("arousal", 0.0).toFloat(),
            heapFree = json.optLong("heap_free", 0L),
            psramFree = json.optLong("psram_free", 0L),
            uptimeSeconds = json.optLong("uptime_s", 0L),
            cpuMhz = json.optInt("cpu_mhz", 240)
        )
    }
}
