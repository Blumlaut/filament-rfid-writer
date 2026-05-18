package com.blumlaut.filamenttagwriter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Convert an RGB888 int (0xRRGGBB) to a Compose Color with full alpha.
 */
fun Int.toArgbColor(): Color = Color(0xFF000000L or (this and 0xFFFFFF).toLong())

/**
 * Parse a hex color string (with or without leading '#') to an RGB888 int.
 */
fun String.parseHexColor(default: Int = 0xFFFFFF): Int = try {
    this.removePrefix("#").toInt(16)
} catch (_: Exception) {
    default
}

/**
 * Check if a color is "light" using perceived luminance.
 * Used to decide text color on top of a color swatch.
 */
fun isLightColor(rgb: Int): Boolean {
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return luminance > 128
}

/**
 * Circular color swatch with optional outline border.
 *
 * M3 Expressive: CircleShape with semantic outline border.
 */
@Composable
fun ColorSwatch(
    rgb: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderColor: Color? = null,
    borderStroke: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(rgb.toArgbColor())
            .then(
                if (borderColor != null) {
                    Modifier.border(borderStroke, borderColor, CircleShape)
                } else {
                    Modifier
                },
            ),
    )
}
