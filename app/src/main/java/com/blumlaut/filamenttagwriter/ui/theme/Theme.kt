package com.blumlaut.filamenttagwriter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// M3 Expressive: spring-based motion scheme
private val expressiveMotion = MotionScheme.expressive()

// M3 Expressive typography: use default 15 baseline + 15 emphasized styles
// (no custom scale needed — MaterialExpressiveTheme provides them automatically)

// M3 Expressive color scheme: standard M3 tokens (unchanged)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CC4FF),
    secondary = Color(0xFFCFD7DF),
    tertiary = Color(0xFF576193),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFE3E2E6),
    surfaceContainerHigh = Color(0xFF2A2D36),
    surfaceContainerHighest = Color(0xFF353842),
    surfaceContainerLow = Color(0xFF17191F),
    surfaceContainerLowest = Color(0xFF0A0C11),
    surfaceContainer = Color(0xFF1F222A),
    primaryContainer = Color(0xFF3A5A8C),
    onPrimaryContainer = Color(0xFFD8E2F0),
    secondaryContainer = Color(0xFF3F444C),
    onSecondaryContainer = Color(0xFFD8E2EC),
    tertiaryContainer = Color(0xFF3A3E54),
    onTertiaryContainer = Color(0xFFE0E0F0),
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFF601420),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF8992A0),
    outlineVariant = Color(0xFF3F4450),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0064A4),
    secondary = Color(0xFF535F70),
    tertiary = Color(0xFF71568D),
    surface = Color(0xFFFDF8FF),
    onSurface = Color(0xFF1A1C1E),
    surfaceContainerHigh = Color(0xFFE4E1EC),
    surfaceContainerHighest = Color(0xFFDEDAE6),
    surfaceContainerLow = Color(0xFFF8F5FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF2F5),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF0F1C2B),
    tertiaryContainer = Color(0xFFE8DEFF),
    onTertiaryContainer = Color(0xFF2A1443),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
)

// M3 Expressive shape scale (10 corner radii)
@Immutable
object ExpressiveShapes {
    val cornerRadii = androidx.compose.material3.Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )
}

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color,
)

val unspecified_scheme = ColorFamily(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    // M3 Expressive: MaterialExpressiveTheme with spring-based motion
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = expressiveMotion,
        shapes = ExpressiveShapes.cornerRadii,
        content = content,
    )
}
