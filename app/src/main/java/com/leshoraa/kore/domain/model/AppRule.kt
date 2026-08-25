package com.leshoraa.kore.domain.model

/**
 * Domain model for an application filter rule.
 *
 * @property packageName The unique Android package identifier.
 * @property appName The user-friendly name of the application.
 * @property isEnabled Whether notifications from this app are allowed to be dispatched.
 */
data class AppRule(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean
)
