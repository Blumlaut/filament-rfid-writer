package com.blumlaut.filamenttagwriter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * M3 Expressive typography.
 *
 * Uses the default M3 type scale which includes both baseline
 * and emphasized styles (headlineLarge, titleMediumEmphasized, etc.).
 *
 * Tokens: md.sys.typescale.* and md.sys.typescale.emphasized.*
 *
 * Custom overrides for brand feel — slightly bolder headlines
 * for the filament catalog context.
 */
val FilamentTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
        ),
        headlineMedium = headlineMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        titleLarge = titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
}
