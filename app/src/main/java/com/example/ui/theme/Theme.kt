package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkMode = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isAppInDarkTheme(): Boolean = LocalIsDarkMode.current

private val DarkColorScheme = darkColorScheme(
    primary = GlowCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0F2D4A),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = GlowEmerald,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0D3B2E),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = GlowOrange,
    background = HealthDarkBackground,
    surface = HealthDarkSurface,
    surfaceVariant = HealthDarkSurfaceVariant,
    onBackground = HealthDarkTextPrimary,
    onSurface = HealthDarkTextPrimary,
    onSurfaceVariant = HealthDarkTextSecondary,
    outline = Color(0xFF38BDF8).copy(alpha = 0.35f),
    error = HealthRose
)

private val LightColorScheme = lightColorScheme(
    primary = HealthEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = HealthEmeraldDark,
    secondary = HealthBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = HealthBlueDark,
    tertiary = HealthOrange,
    background = HealthLightBackground,
    surface = HealthLightSurface,
    surfaceVariant = HealthLightSurfaceVariant,
    onBackground = HealthLightTextPrimary,
    onSurface = HealthLightTextPrimary,
    onSurfaceVariant = HealthLightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = HealthRose
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkMode provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
