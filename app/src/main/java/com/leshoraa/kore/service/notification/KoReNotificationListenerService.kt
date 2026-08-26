package com.leshoraa.kore.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.common.ServiceLocator
import com.leshoraa.kore.data.parser.MapsNavigationParser
import com.leshoraa.kore.data.parser.NotificationParser
import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.usecase.ProcessNavigationUseCase
import com.leshoraa.kore.domain.usecase.ProcessNotificationUseCase
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * High-efficiency notification listener with support for both standard phone notifications
 * and real-time Google Maps turn-by-turn navigation HUD streaming.
 */
class KoReNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "KoReNotifListener"
        private const val DEBOUNCE_MS = 500L
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    
    // In-memory cache for deduplication: [Key -> LastProcessedTimestamp]
    private val seenEvents = ConcurrentHashMap<Int, Long>()

    private lateinit var processNotificationUseCase: ProcessNotificationUseCase
    private lateinit var processNavigationUseCase: ProcessNavigationUseCase
    private lateinit var bleManager: BleManager
    private lateinit var notificationParser: NotificationParser
    private val mapsNavigationParser = MapsNavigationParser()

    override fun onCreate() {
        super.onCreate()
        
        // Setup via ServiceLocator
        bleManager = ServiceLocator.provideBleManager(applicationContext)
        processNotificationUseCase = ServiceLocator.provideProcessNotificationUseCase(applicationContext)
        processNavigationUseCase = ServiceLocator.provideProcessNavigationUseCase(applicationContext)
        notificationParser = NotificationParser(applicationContext)
        
        startCleanupWatchdog()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener connected to Android OS")
        
        serviceScope.launch {
            bleManager.connectionState.collect { state ->
                if (state == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Bluetooth connected, pushing active notifications...")
                    delay(2000) // Stabilize even more to ensure MTU/Services are ready
                    
                    // Send connection success notification automatically
                    processEvent(NotificationEvent(
                        id = "CONN_SUCCESS",
                        packageName = packageName,
                        appName = "KoRe",
                        postTimeMillis = System.currentTimeMillis(),
                        title = "Success",
                        text = "haloo",
                        isClearable = true
                    ))
                    
                    pushActiveNotifications()
                }
            }
        }
    }

    private suspend fun pushActiveNotifications() {
        try {
            val activeNotifications = getActiveNotifications() ?: return
            // Reverse to send oldest first
            activeNotifications.reversed().forEach { sbn ->
                // Basic filtering
                if (sbn.packageName == packageName) return@forEach
                if (!sbn.isClearable) return@forEach
                if (mapsNavigationParser.isNavigationNotification(sbn)) return@forEach

                val event = notificationParser.parse(sbn)
                if (event.title.isNotBlank() || event.text.isNotBlank()) {
                    processEvent(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch active notifications", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Ignore own notifications to prevent infinite loops/self-triggering
        if (sbn.packageName == packageName) return

        // 2. Specialized Handler: Google Maps Turn-by-Turn Navigation Stream
        if (mapsNavigationParser.isNavigationNotification(sbn)) {
            val navEvent = mapsNavigationParser.parse(sbn)
            if (navEvent != null) {
                val navKey = ("NAV_" + navEvent.icon + navEvent.distance + navEvent.instruction + navEvent.street + navEvent.isActive).hashCode()
                val currentTime = System.currentTimeMillis()
                val lastTime = seenEvents[navKey] ?: 0L
                if (currentTime - lastTime < DEBOUNCE_MS) {
                    return
                }
                seenEvents[navKey] = currentTime

                serviceScope.launch {
                    Log.d(TAG, "Dispatching Turn-by-Turn Navigation: [$navEvent]")
                    processNavigationUseCase(navEvent).onFailure { error ->
                        Log.e(TAG, "Navigation dispatch failed", error)
                    }
                }
                return
            }
        }

        // 3. Delegate standard notification parsing
        val event = notificationParser.parse(sbn)

        // 4. Ignore ongoing/system notifications that aren't clearable
        if (!sbn.isClearable) return

        // 5. Create deduplication key to avoid progress bar spam
        val eventKey = (event.packageName + event.id + event.title + event.text).hashCode()
        val currentTime = System.currentTimeMillis()
        
        val lastTime = seenEvents[eventKey] ?: 0L
        if (currentTime - lastTime < DEBOUNCE_MS) {
            return
        }
        seenEvents[eventKey] = currentTime

        serviceScope.launch {
            processEvent(event)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // When Google Maps navigation is dismissed/exited, deactivate HUD on KoRe OLED
        if (mapsNavigationParser.isNavigationNotification(sbn)) {
            serviceScope.launch {
                Log.d(TAG, "Navigation notification removed -> returning KoRe to face")
                processNavigationUseCase(NavEvent(isActive = false)).onFailure { error ->
                    Log.e(TAG, "Navigation deactivation dispatch failed", error)
                }
            }
        }
    }

    private suspend fun processEvent(event: NotificationEvent) {
        Log.d(TAG, "Processing notification from ${event.packageName}")
        processNotificationUseCase(event).onFailure { error ->
            Log.e(TAG, "Dispatch failed for ${event.packageName}", error)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bleManager.isInitialized) {
            bleManager.disconnect()
        }
        serviceJob.cancel()
    }
    
    // Periodic cleanup of seenEvents to prevent memory growth
    private fun startCleanupWatchdog() {
        serviceScope.launch {
            while (isActive) {
                delay(60000) // Every minute
                val now = System.currentTimeMillis()
                seenEvents.entries.removeIf { now - it.value > 5000 } // Remove keys older than 5s
            }
        }
    }
}
