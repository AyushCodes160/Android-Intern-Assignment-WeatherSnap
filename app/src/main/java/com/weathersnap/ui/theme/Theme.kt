package com.weathersnap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppDarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnLime,
    primaryContainer = LimeDim,
    onPrimaryContainer = OnLime,
    secondary = Lime,
    onSecondary = OnLime,
    tertiary = LimeDim,
    background = BgBlack,
    onBackground = TextHigh,
    surface = SurfaceDark,
    onSurface = TextHigh,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextMedium,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCardElevated,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = ErrorRed,
    onError = OnErrorBg,
)

@Composable
fun WeatherSnapTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = AppDarkColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
