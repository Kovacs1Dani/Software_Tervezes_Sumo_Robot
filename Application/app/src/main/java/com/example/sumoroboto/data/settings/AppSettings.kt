package com.example.sumoroboto.data.settings

import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    AUTO("System Auto")
}

enum class AppAccentColor(
    val displayName: String,
    val primaryColor: Color,
    val hexCode: String
) {
    CYAN("Turquoise", Color(0xFF00E5FF), "#00E5FF"),
    BLUE("Electric Blue", Color(0xFF2979FF), "#2979FF"),
    GREEN("Emerald Green", Color(0xFF00E676), "#00E676"),
    ORANGE("Neon Orange", Color(0xFFFF9100), "#FF9100"),
    RED("Crimson Red", Color(0xFFFF1744), "#FF1744"),
    PURPLE("Royal Purple", Color(0xFFD500F9), "#D500F9"),
    AMBER("Sun Amber", Color(0xFFFFD600), "#FFD600"),
    PINK("Hot Pink", Color(0xFFFF4081), "#FF4081")
}
