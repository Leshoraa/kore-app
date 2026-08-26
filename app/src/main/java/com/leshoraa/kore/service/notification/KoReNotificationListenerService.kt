package com.leshoraa.kore.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.common.ServiceLocator
import com.leshoraa.kore.data.parser.NotificationParser
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.usecase.ProcessNotificationUseCase
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * High-efficiency notification listener with in-memory deduplication and safe payload extraction.
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
    private lateinit var bleManager: BleManager
    private lateinit var notificationParser: NotificationParser

    override fun onCreate() {
        super.onCreate()
        
        // Setup via ServiceLocator
        bleManager = ServiceLocator.provideBleManager(applicationContext)
        processNotificationUseCase = ServiceLocator.provideProcessNotificationUseCase(applicationContext)
        notificationParser = NotificationParser(applicationContext)
        
        startCleanupWatchdog()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Ignore own notifications to prevent infinite loops/self-triggering
        if (sbn.packageName == packageName) return

        // 2. Delegate parsing
        val event = notificationParser.parse(sbn)

        // 3. Ignore ongoing/system notifications that aren't clearable
        if (!sbn.isClearable) return

        // 4. Create deduplication key to avoid progress bar spam
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
