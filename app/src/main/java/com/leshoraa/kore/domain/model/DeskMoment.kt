package com.leshoraa.kore.domain.model

/**
 * Domain model representing a captured desk moment snapshot.
 */
data class DeskMoment(
    val id: Long = 0,
    val timestamp: Long,
    val filePath: String,
    val expressionName: String = "IDLE",
    val valence: Float = 0f,
    val arousal: Float = 0f,
    val note: String = ""
)
