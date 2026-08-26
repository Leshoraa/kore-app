package com.leshoraa.kore.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun getLightColorScheme(palette: AppPalette): ColorScheme = when (palette) {
    AppPalette.FOREST -> lightColorScheme(
        primary = ForestPrimaryLight,
        onPrimary = ForestOnPrimaryLight,
        primaryContainer = ForestPrimaryContainerLight,
        onPrimaryContainer = ForestOnPrimaryContainerLight,
        secondary = ForestSecondaryLight,
        onSecondary = ForestOnSecondaryLight,
        secondaryContainer = ForestSecondaryContainerLight,
        onSecondaryContainer = ForestOnSecondaryContainerLight,
        background = ForestBackgroundLight,
        surface = ForestSurfaceLight,
        surfaceContainer = ForestSurfaceContainerLight,
        error = ErrorRedLight,
        onError = OnErrorRedLight,
    )
    AppPalette.PEACH -> lightColorScheme(
        primary = PeachPrimaryLight,
        onPrimary = PeachOnPrimaryLight,
        primaryContainer = PeachPrimaryContainerLight,
        onPrimaryContainer = PeachOnPrimaryContainerLight,
        secondary = PeachSecondaryLight,
        onSecondary = PeachOnSecondaryLight,
        secondaryContainer = PeachSecondaryContainerLight,
        onSecondaryContainer = PeachOnSecondaryContainerLight,
        background = PeachBackgroundLight,
        surface = PeachSurfaceLight,
        surfaceContainer = PeachSurfaceContainerLight,
        error = ErrorRedLight,
        onError = OnErrorRedLight,
    )
    AppPalette.LAVENDER -> lightColorScheme(
        primary = LavenderPrimaryLight,
        onPrimary = LavenderOnPrimaryLight,
        primaryContainer = LavenderPrimaryContainerLight,
        onPrimaryContainer = LavenderOnPrimaryContainerLight,
        secondary = LavenderSecondaryLight,
        onSecondary = LavenderOnSecondaryLight,
        secondaryContainer = LavenderSecondaryContainerLight,
        onSecondaryContainer = LavenderOnSecondaryContainerLight,
        background = LavenderBackgroundLight,
        surface = LavenderSurfaceLight,
        surfaceContainer = LavenderSurfaceContainerLight,
        error = ErrorRedLight,
        onError = OnErrorRedLight,
    )
    AppPalette.MIDNIGHT -> lightColorScheme(
        primary = MidnightPrimaryLight,
        onPrimary = MidnightOnPrimaryLight,
        primaryContainer = MidnightPrimaryContainerLight,
        onPrimaryContainer = MidnightOnPrimaryContainerLight,
        secondary = MidnightSecondaryLight,
        onSecondary = MidnightOnSecondaryLight,
        secondaryContainer = MidnightSecondaryContainerLight,
        onSecondaryContainer = MidnightOnSecondaryContainerLight,
        background = MidnightBackgroundLight,
        surface = MidnightSurfaceLight,
        surfaceContainer = MidnightSurfaceContainerLight,
        error = ErrorRedLight,
        onError = OnErrorRedLight,
    )
}

private fun getDarkColorScheme(palette: AppPalette): ColorScheme = when (palette) {
    AppPalette.FOREST -> darkColorScheme(
        primary = ForestPrimaryDark,
        onPrimary = ForestOnPrimaryDark,
        primaryContainer = ForestPrimaryContainerDark,
        onPrimaryContainer = ForestOnPrimaryContainerDark,
        secondary = ForestSecondaryDark,
        onSecondary = ForestOnSecondaryDark,
        secondaryContainer = ForestSecondaryContainerDark,
        onSecondaryContainer = ForestOnSecondaryContainerDark,
        background = ForestBackgroundDark,
        surface = ForestSurfaceDark,
        surfaceContainer = ForestSurfaceContainerDark,
        error = ErrorRedDark,
        onError = OnErrorRedDark,
    )
    AppPalette.PEACH -> darkColorScheme(
        primary = PeachPrimaryDark,
        onPrimary = PeachOnPrimaryDark,
        primaryContainer = PeachPrimaryContainerDark,
        onPrimaryContainer = PeachOnPrimaryContainerDark,
        secondary = PeachSecondaryDark,
        onSecondary = PeachOnSecondaryDark,
        secondaryContainer = PeachSecondaryContainerDark,
        onSecondaryContainer = PeachOnSecondaryContainerDark,
        background = PeachBackgroundDark,
        surface = PeachSurfaceDark,
        surfaceContainer = PeachSurfaceContainerDark,
        error = ErrorRedDark,
        onError = OnErrorRedDark,
    )
    AppPalette.LAVENDER -> darkColorScheme(
        primary = LavenderPrimaryDark,
        onPrimary = LavenderOnPrimaryDark,
        primaryContainer = LavenderPrimaryContainerDark,
        onPrimaryContainer = LavenderOnPrimaryContainerDark,
        secondary = LavenderSecondaryDark,
        onSecondary = LavenderOnSecondaryDark,
        secondaryContainer = LavenderSecondaryContainerDark,
        onSecondaryContainer = LavenderOnSecondaryContainerDark,
        background = LavenderBackgroundDark,
        surface = LavenderSurfaceDark,
        surfaceContainer = LavenderSurfaceContainerDark,
        error = ErrorRedDark,
        onError = OnErrorRedDark,
    )
    AppPalette.MIDNIGHT -> darkColorScheme(
        primary = MidnightPrimaryDark,
        onPrimary = MidnightOnPrimaryDark,
        primaryContainer = MidnightPrimaryContainerDark,
        onPrimaryContainer = MidnightOnPrimaryContainerDark,
        secondary = MidnightSecondaryDark,
        onSecondary = MidnightOnSecondaryDark,
        secondaryContainer = MidnightSecondaryContainerDark,
        onSecondaryContainer = MidnightOnSecondaryContainerDark,
        background = MidnightBackgroundDark,
        surface = MidnightSurfaceDark,
        surfaceContainer = MidnightSurfaceContainerDark,
        error = ErrorRedDark,
        onError = OnErrorRedDark,
    )
}

@Composable
fun KoReTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.FOREST,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) getDarkColorScheme(palette) else getLightColorScheme(palette)
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
