package com.blumlaut.filamenttagwriter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * M3 Expressive shape scale.
 *
 * Tokens: md.sys.shape.corner-value.*
 * - none: 0dp
 * - extraSmall: 4dp
 * - small: 8dp
 * - medium: 12dp
 * - large: 16dp
 * - largeIncreased: 20dp
 * - extraLarge: 28dp
 * - extraLargeIncreased: 32dp
 * - extraExtraLarge: 48dp
 * - full: fully rounded
 *
 * Intensity: Foundational — clarity and familiarity first,
 * with bolder corner contrast on hero surfaces.
 */
private val ExpressiveShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = CutCornerShape(topStart = 16.dp, topEnd = 16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private val LightColorScheme = lightColorScheme(
    primary = FilamentPrimaryLight,
    onPrimary = FilamentOnPrimaryLight,
    primaryContainer = FilamentPrimaryContainerLight,
    onPrimaryContainer = FilamentOnPrimaryContainerLight,
    secondary = FilamentSecondaryLight,
    onSecondary = FilamentOnSecondaryLight,
    secondaryContainer = FilamentSecondaryContainerLight,
    onSecondaryContainer = FilamentOnSecondaryContainerLight,
    tertiary = FilamentTertiaryLight,
    onTertiary = FilamentOnTertiaryLight,
    tertiaryContainer = FilamentTertiaryContainerLight,
    onTertiaryContainer = FilamentOnTertiaryContainerLight,
    error = FilamentErrorLight,
    onError = FilamentOnErrorLight,
    errorContainer = FilamentErrorContainerLight,
    onErrorContainer = FilamentOnErrorContainerLight,
    surface = FilamentSurfaceLight,
    onSurface = FilamentOnSurfaceLight,
    surfaceVariant = FilamentSurfaceVariantLight,
    onSurfaceVariant = FilamentOnSurfaceVariantLight,
    surfaceContainerLowest = FilamentSurfaceContainerLowestLight,
    surfaceContainerLow = FilamentSurfaceContainerLowLight,
    surfaceContainer = FilamentSurfaceContainerLight,
    surfaceContainerHigh = FilamentSurfaceContainerHighLight,
    surfaceContainerHighest = FilamentSurfaceContainerHighestLight,
    outline = FilamentOutlineLight,
    outlineVariant = FilamentOutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = FilamentPrimaryDark,
    onPrimary = FilamentOnPrimaryDark,
    primaryContainer = FilamentPrimaryContainerDark,
    onPrimaryContainer = FilamentOnPrimaryContainerDark,
    secondary = FilamentSecondaryDark,
    onSecondary = FilamentOnSecondaryDark,
    secondaryContainer = FilamentSecondaryContainerDark,
    onSecondaryContainer = FilamentOnSecondaryContainerDark,
    tertiary = FilamentTertiaryDark,
    onTertiary = FilamentOnTertiaryDark,
    tertiaryContainer = FilamentTertiaryContainerDark,
    onTertiaryContainer = FilamentOnTertiaryContainerDark,
    error = FilamentErrorDark,
    onError = FilamentOnErrorDark,
    errorContainer = FilamentErrorContainerDark,
    onErrorContainer = FilamentOnErrorContainerDark,
    surface = FilamentSurfaceDark,
    onSurface = FilamentOnSurfaceDark,
    surfaceVariant = FilamentSurfaceVariantDark,
    onSurfaceVariant = FilamentOnSurfaceVariantDark,
    surfaceContainerLowest = FilamentSurfaceContainerLowestDark,
    surfaceContainerLow = FilamentSurfaceContainerLowDark,
    surfaceContainer = FilamentSurfaceContainerDark,
    surfaceContainerHigh = FilamentSurfaceContainerHighDark,
    surfaceContainerHighest = FilamentSurfaceContainerHighestDark,
    outline = FilamentOutlineDark,
    outlineVariant = FilamentOutlineVariantDark,
)

@Composable
fun FilamentTagWriterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FilamentTypography,
        shapes = ExpressiveShapes,
        content = content,
    )
}
