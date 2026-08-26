package com.leshoraa.kore.data.parser

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.leshoraa.kore.domain.model.NotificationEvent

/**
 * Utility for robustly extracting domain-relevant data from a StatusBarNotification.
 */
class NotificationParser(private val context: Context) {

    /**
     * Maps a StatusBarNotification to a NotificationEvent domain model.
     */
    fun parse(sbn: StatusBarNotification): NotificationEvent {
        val notification = sbn.notification
        val extras = notification.extras
        val pm = context.packageManager

        // Resolve app name
        val appName = try {
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName.substringAfterLast(".")
        }

        // Extract text safely using common notification extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        
        // Try multiple sources for text content
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: notification.tickerText?.toString()
            ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        return NotificationEvent(
            id = "${sbn.packageName}_${sbn.id}_${sbn.postTime}",
            packageName = sbn.packageName,
            appName = appName,
            postTimeMillis = sbn.postTime,
            title = title,
            text = text,
            subText = subText,
            isClearable = sbn.isClearable,
            isGroupSummary = isGroupSummary
        )
    }
}
