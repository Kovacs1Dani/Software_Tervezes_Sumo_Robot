package com.example.sumoroboto.data.model

data class TelemetryData(
    val distL: Int = 0,
    val distR: Int = 0,
    val lineL: Boolean = false,
    val lineR: Boolean = false,
    val vBat: Float = 0f,
    val battery: BatteryState = BatteryState.fromVoltage(vBat),
    val timestamp: Long = System.currentTimeMillis()
)
