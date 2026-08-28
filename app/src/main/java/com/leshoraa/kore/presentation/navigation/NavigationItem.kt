package com.leshoraa.kore.presentation.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represent a navigation destination in the app.
 * Using standard KDoc for better documentation.
 */
data class NavigationItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
