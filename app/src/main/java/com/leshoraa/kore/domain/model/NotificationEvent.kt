package com.leshoraa.kore.domain.model

/**
 * Immutable domain entity representing a sanitized notification captured by the system.
 */
data class NotificationEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val postTimeMillis: Long,
    val title: String,
    val text: String,
    val subText: String? = null,
    val isClearable: Boolean,
    val isGroupSummary: Boolean = false
)
