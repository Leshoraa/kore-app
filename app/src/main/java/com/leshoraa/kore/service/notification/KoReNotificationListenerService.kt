package com.leshoraa.kore.service.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.ble.BleOperationQueue
import com.leshoraa.kore.data.repository.BleRepositoryImpl
import com.leshoraa.kore.data.repository.NotificationRepositoryImpl
import com.leshoraa.kore.core.common.ServiceLocator
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
    
    // Hash-ring ring for deduplication: [Key -> LastProcessedTimestamp]
    private val seenEvents = ConcurrentHashMap<Int, Long>()

    // Simple manual DI for Stage 3
    private lateinit var processNotificationUseCase: ProcessNotificationUseCase
    private lateinit var bleManager: BleManager

    override fun onCreate() {
        super.onCreate()
        
        // Dependency Graph setup via ServiceLocator
        bleManager = ServiceLocator.provideBleManager(applicationContext)
        processNotificationUseCase = ServiceLocator.provideProcessNotificationUseCase(applicationContext)
        
        startCleanupWatchdog()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        // Defensive extraction
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        // Create deduplication key
        val eventKey = (packageName + sbn.id + sbn.tag + title + text).hashCode()
        val currentTime = System.currentTimeMillis()
        
        val lastTime = seenEvents[eventKey] ?: 0L
        if (currentTime - lastTime < DEBOUNCE_MS) {
            // Discard spam/duplicate (common with progress updates)
            return
        }
        seenEvents[eventKey] = currentTime

        serviceScope.launch {
            val event = NotificationEvent(
                id = sbn.id.toString(),
                packageName = packageName,
                postTimeMillis = sbn.postTime,
                title = title,
                text = text,
                subText = subText,
                isClearable = sbn.isClearable,
                isGroupSummary = isGroupSummary
            )
            
            processEvent(event)
        }
    }

    private suspend fun processEvent(event: NotificationEvent) {
        Log.d(TAG, "Invoking ProcessNotificationUseCase for ${event.packageName}")
        processNotificationUseCase(event).onFailure { error ->
            Log.e(TAG, "Processing failed for ${event.packageName}", error)
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
