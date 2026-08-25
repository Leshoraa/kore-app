package com.leshoraa.kore.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an application filter rule.
 */
@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true
)

/**
 * Entity representing a logged notification event for auditing.
 */
@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)
