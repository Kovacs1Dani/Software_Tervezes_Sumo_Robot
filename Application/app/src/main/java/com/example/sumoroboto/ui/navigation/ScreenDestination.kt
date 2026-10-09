package com.example.sumoroboto.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Radar
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenDestination(
    val title: String,
    val icon: ImageVector
) {
    CONTROL("Control", Icons.Default.Gamepad),
    TELEMETRY("Telemetry", Icons.Default.Analytics),
    DOHYO("Dohyo Map", Icons.Default.Radar)
}
