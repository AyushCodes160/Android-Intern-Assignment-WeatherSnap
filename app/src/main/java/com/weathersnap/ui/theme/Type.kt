package com.weathersnap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

private fun style(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = Sans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val AppTypography = Typography(
    displayLarge = style(FontWeight.Bold, 40, 44),
    displayMedium = style(FontWeight.Bold, 32, 36),
    headlineLarge = style(FontWeight.Bold, 28, 32),
    headlineMedium = style(FontWeight.SemiBold, 22, 26),
    headlineSmall = style(FontWeight.SemiBold, 18, 22),
    titleLarge = style(FontWeight.SemiBold, 20, 24),
    titleMedium = style(FontWeight.Medium, 16, 20),
    titleSmall = style(FontWeight.Medium, 14, 18),
    bodyLarge = style(FontWeight.Normal, 16, 22),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.SemiBold, 14, 18),
    labelMedium = style(FontWeight.Medium, 12, 16),
    labelSmall = style(FontWeight.Medium, 11, 14),
)
