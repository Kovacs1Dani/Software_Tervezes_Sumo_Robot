package com.example.sumoroboto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.sumoroboto.data.settings.AppAccentColor
import com.example.sumoroboto.data.settings.AppThemeMode

private fun createDarkColorScheme(accentColor: AppAccentColor) = darkColorScheme(
    primary = accentColor.primaryColor,
    onPrimary = if (accentColor.primaryColor.luminance() > 0.5f) Color.Black else Color.White,
    primaryContainer = accentColor.primaryColor.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = SumoOrange,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5D3600),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = TechBlue,
    background = DarkCharcoal,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFA0AAB5),
    outline = Color(0xFF455A64),
    outlineVariant = Color(0xFF2C3440),
    error = SumoRed
)

private fun createLightColorScheme(accentColor: AppAccentColor) = lightColorScheme(
    primary = accentColor.primaryColor,
    onPrimary = if (accentColor.primaryColor.luminance() > 0.5f) Color.Black else Color.White,
    primaryContainer = accentColor.primaryColor.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    tertiary = Color(0xFF2563EB),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF94A3B8),
    error = SumoRed
)

@Composable
fun SumoRobotoTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColor: AppAccentColor = AppAccentColor.CYAN,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        createDarkColorScheme(accentColor)
    } else {
        createLightColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}