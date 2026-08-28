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
        tertiary = ForestTertiaryLight,
        onTertiary = ForestOnTertiaryLight,
        tertiaryContainer = ForestTertiaryContainerLight,
        onTertiaryContainer = ForestOnTertiaryContainerLight,
        background = ForestBackgroundLight,
        onBackground = ForestOnBackgroundLight,
        surface = ForestSurfaceLight,
        onSurface = ForestOnSurfaceLight,
        surfaceContainerLow = ForestSurfaceContainerLowLight,
        surfaceContainer = ForestSurfaceContainerLight,
        surfaceContainerHigh = ForestSurfaceContainerHighLight,
        surfaceContainerHighest = ForestSurfaceContainerHighestLight,
        surfaceVariant = ForestSecondaryContainerLight,
        onSurfaceVariant = ForestOnSecondaryContainerLight,
        outline = ForestSecondaryLight,
        outlineVariant = ForestSecondaryLight.copy(alpha = 0.2f),
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
        tertiary = PeachTertiaryLight,
        onTertiary = PeachOnTertiaryLight,
        tertiaryContainer = PeachTertiaryContainerLight,
        onTertiaryContainer = PeachOnTertiaryContainerLight,
        background = PeachBackgroundLight,
        onBackground = PeachOnBackgroundLight,
        surface = PeachSurfaceLight,
        onSurface = PeachOnSurfaceLight,
        surfaceContainerLow = PeachSurfaceContainerLowLight,
        surfaceContainer = PeachSurfaceContainerLight,
        surfaceContainerHigh = PeachSurfaceContainerHighLight,
        surfaceContainerHighest = PeachSurfaceContainerHighestLight,
        surfaceVariant = PeachSecondaryContainerLight,
        onSurfaceVariant = PeachOnSecondaryContainerLight,
        outline = PeachSecondaryLight,
        outlineVariant = PeachSecondaryLight.copy(alpha = 0.2f),
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
        tertiary = LavenderTertiaryLight,
        onTertiary = LavenderOnTertiaryLight,
        tertiaryContainer = LavenderTertiaryContainerLight,
        onTertiaryContainer = LavenderOnTertiaryContainerLight,
        background = LavenderBackgroundLight,
        onBackground = LavenderOnBackgroundLight,
        surface = LavenderSurfaceLight,
        onSurface = LavenderOnSurfaceLight,
        surfaceContainerLow = LavenderSurfaceContainerLowLight,
        surfaceContainer = LavenderSurfaceContainerLight,
        surfaceContainerHigh = LavenderSurfaceContainerHighLight,
        surfaceContainerHighest = LavenderSurfaceContainerHighestLight,
        surfaceVariant = LavenderSecondaryContainerLight,
        onSurfaceVariant = LavenderOnSecondaryContainerLight,
        outline = LavenderSecondaryLight,
        outlineVariant = LavenderSecondaryLight.copy(alpha = 0.2f),
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
        tertiary = MidnightTertiaryLight,
        onTertiary = MidnightOnTertiaryLight,
        tertiaryContainer = MidnightTertiaryContainerLight,
        onTertiaryContainer = MidnightOnTertiaryContainerLight,
        background = MidnightBackgroundLight,
        onBackground = MidnightOnBackgroundLight,
        surface = MidnightSurfaceLight,
        onSurface = MidnightOnSurfaceLight,
        surfaceContainerLow = MidnightSurfaceContainerLowLight,
        surfaceContainer = MidnightSurfaceContainerLight,
        surfaceContainerHigh = MidnightSurfaceContainerHighLight,
        surfaceContainerHighest = MidnightSurfaceContainerHighestLight,
        surfaceVariant = MidnightSecondaryContainerLight,
        onSurfaceVariant = MidnightOnSecondaryContainerLight,
        outline = MidnightSecondaryLight,
        outlineVariant = MidnightSecondaryLight.copy(alpha = 0.2f),
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
        tertiary = ForestTertiaryDark,
        onTertiary = ForestOnTertiaryDark,
        tertiaryContainer = ForestTertiaryContainerDark,
        onTertiaryContainer = ForestOnTertiaryContainerDark,
        background = ForestBackgroundDark,
        onBackground = ForestOnBackgroundDark,
        surface = ForestSurfaceDark,
        onSurface = ForestOnSurfaceDark,
        surfaceContainerLow = ForestSurfaceContainerLowDark,
        surfaceContainer = ForestSurfaceContainerDark,
        surfaceContainerHigh = ForestSurfaceContainerHighDark,
        surfaceContainerHighest = ForestSurfaceContainerHighestDark,
        surfaceVariant = ForestSecondaryContainerDark,
        onSurfaceVariant = ForestOnSecondaryContainerDark,
        outline = ForestSecondaryDark,
        outlineVariant = ForestSecondaryDark.copy(alpha = 0.2f),
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
        tertiary = PeachTertiaryDark,
        onTertiary = PeachOnTertiaryDark,
        tertiaryContainer = PeachTertiaryContainerDark,
        onTertiaryContainer = PeachOnTertiaryContainerDark,
        background = PeachBackgroundDark,
        onBackground = PeachOnBackgroundDark,
        surface = PeachSurfaceDark,
        onSurface = PeachOnSurfaceDark,
        surfaceContainerLow = PeachSurfaceContainerLowDark,
        surfaceContainer = PeachSurfaceContainerDark,
        surfaceContainerHigh = PeachSurfaceContainerHighDark,
        surfaceContainerHighest = PeachSurfaceContainerHighestDark,
        surfaceVariant = PeachSecondaryContainerDark,
        onSurfaceVariant = PeachOnSecondaryContainerDark,
        outline = PeachSecondaryDark,
        outlineVariant = PeachSecondaryDark.copy(alpha = 0.2f),
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
        tertiary = LavenderTertiaryDark,
        onTertiary = LavenderOnTertiaryDark,
        tertiaryContainer = LavenderTertiaryContainerDark,
        onTertiaryContainer = LavenderOnTertiaryContainerDark,
        background = LavenderBackgroundDark,
        onBackground = LavenderOnBackgroundDark,
        surface = LavenderSurfaceDark,
        onSurface = LavenderOnSurfaceDark,
        surfaceContainerLow = LavenderSurfaceContainerLowDark,
        surfaceContainer = LavenderSurfaceContainerDark,
        surfaceContainerHigh = LavenderSurfaceContainerHighDark,
        surfaceContainerHighest = LavenderSurfaceContainerHighestDark,
        surfaceVariant = LavenderSecondaryContainerDark,
        onSurfaceVariant = LavenderOnSecondaryContainerDark,
        outline = LavenderSecondaryDark,
        outlineVariant = LavenderSecondaryDark.copy(alpha = 0.2f),
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
        tertiary = MidnightTertiaryDark,
        onTertiary = MidnightOnTertiaryDark,
        tertiaryContainer = MidnightTertiaryContainerDark,
        onTertiaryContainer = MidnightOnTertiaryContainerDark,
        background = MidnightBackgroundDark,
        onBackground = MidnightOnBackgroundDark,
        surface = MidnightSurfaceDark,
        onSurface = MidnightOnSurfaceDark,
        surfaceContainerLow = MidnightSurfaceContainerLowDark,
        surfaceContainer = MidnightSurfaceContainerDark,
        surfaceContainerHigh = MidnightSurfaceContainerHighDark,
        surfaceContainerHighest = MidnightSurfaceContainerHighestDark,
        surfaceVariant = MidnightSecondaryContainerDark,
        onSurfaceVariant = MidnightOnSecondaryContainerDark,
        outline = MidnightSecondaryDark,
        outlineVariant = MidnightSecondaryDark.copy(alpha = 0.2f),
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
