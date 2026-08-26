package com.leshoraa.kore.domain.model

/**
 * Immutable domain model representing a turn-by-turn navigation event.
 *
 * @property icon Directional maneuver icon identifier (e.g., "turn_right", "uturn", "straight").
 * @property distance Formatted distance to the next maneuver step (e.g., "200 m", "1.5 km").
 * @property instruction Maneuver instruction text (e.g., "Turn right onto Grand Ave").
 * @property street Target roadway or destination name.
 * @property eta Estimated arrival time timestamp (e.g., "14:49").
 * @property duration Estimated remaining route duration (e.g., "9 min").
 * @property totalDistance Total remaining route distance (e.g., "2.9 km").
 * @property isActive Flag indicating whether active navigation mode is currently engaged.
 */
data class NavEvent(
    val icon: String = "straight",
    val distance: String = "",
    val instruction: String = "",
    val street: String = "",
    val eta: String = "",
    val duration: String = "",
    val totalDistance: String = "",
    val isActive: Boolean = true
)
