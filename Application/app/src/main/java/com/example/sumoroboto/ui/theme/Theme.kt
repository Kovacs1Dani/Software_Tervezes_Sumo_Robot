package com.example.sumoroboto.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TechCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFE0F7FA),
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
    error = SumoRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006877),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA1EFFF),
    secondary = Color(0xFF8B5000),
    tertiary = Color(0xFF005AC1),
    background = Color(0xFFF9F9FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8ECEF),
    error = SumoRed
)

@Composable
fun SumoRobotoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Robotics dark theme preferred
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}