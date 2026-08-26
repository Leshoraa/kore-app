package com.leshoraa.kore.data.remote

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Intelligent local network scanner that automatically discovers KoRe ESP32-S3 devices
 * using mDNS ("kore.local"), default AP endpoints, and high-speed parallel subnet sweeps.
 */
class DeviceDiscoveryScanner(
    private val context: Context
) {
    companion object {
        private val KNOWN_STATIC_IPS = listOf("192.168.18.16", "192.168.4.1")
        private const val PROBE_TIMEOUT_MS = 600
    }

    /**
     * Attempts to locate the KoRe device on the local network.
     *
     * @return Found IP address string (e.g. "192.168.1.105") or null if not detected.
     */
    suspend fun discoverKoReDevice(): String? = withContext(Dispatchers.IO) {
        // 1. Check mDNS hostname: "kore.local"
        val mdnsIp = tryResolveMdns("kore.local")
        if (mdnsIp != null && verifyKoReEndpoint(mdnsIp)) {
            return@withContext mdnsIp
        }

        // 2. Check known static/AP IPs
        for (ip in KNOWN_STATIC_IPS) {
            if (verifyKoReEndpoint(ip)) {
                return@withContext ip
            }
        }

        // 3. Perform fast parallel subnet scan on the connected Wi-Fi
        val subnetPrefix = getLocalSubnetPrefix()
        if (subnetPrefix != null) {
            val scannedIp = scanSubnetForKoRe(subnetPrefix)
            if (scannedIp != null) {
                return@withContext scannedIp
            }
        }

        null
    }

    /**
     * Verifies if an IP hosts the KoRe HTTP endpoint.
     */
    suspend fun verifyKoReEndpoint(ip: String): Boolean = withContext(Dispatchers.IO) {
        withTimeoutOrNull(PROBE_TIMEOUT_MS.toLong()) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("http://$ip:80/telemetry")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = PROBE_TIMEOUT_MS
                    readTimeout = PROBE_TIMEOUT_MS
                    requestMethod = "GET"
                }
                connection.responseCode == HttpURLConnection.HTTP_OK
            } catch (_: Exception) {
                false
            } finally {
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        } ?: false
    }

    private fun tryResolveMdns(host: String): String? {
        return try {
            val address = InetAddress.getByName(host)
            address.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun getLocalSubnetPrefix(): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: return null
            if (ipInt == 0) return null

            val ip = String.format(
                "%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff
            )
            return ip
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun scanSubnetForKoRe(prefix: String): String? = coroutineScope {
        val batchSize = 32
        for (start in 1..254 step batchSize) {
            val end = (start + batchSize - 1).coerceAtMost(254)
            val deferredList = (start..end).map { lastOctet ->
                async(Dispatchers.IO) {
                    val candidateIp = "$prefix.$lastOctet"
                    if (verifyKoReEndpoint(candidateIp)) candidateIp else null
                }
            }
            val results = deferredList.awaitAll()
            val found = results.firstOrNull { it != null }
            if (found != null) {
                return@coroutineScope found
            }
        }
        null
    }
}
