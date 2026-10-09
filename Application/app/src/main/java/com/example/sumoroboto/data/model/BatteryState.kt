package com.example.sumoroboto.data.model

data class BatteryState(
    val voltage: Float = 0f,
    val percentage: Int = 0,
    val isCritical: Boolean = false
) {
    companion object {
        const val FULL_VOLTAGE = 8.4f
        const val NOMINAL_VOLTAGE = 7.4f
        const val CRITICAL_VOLTAGE = 6.8f
        const val MIN_VOLTAGE = 6.0f

        fun fromVoltage(vBat: Float): BatteryState {
            if (vBat <= 0.1f) {
                return BatteryState(voltage = 0f, percentage = 0, isCritical = false)
            }
            val percentage = calculatePercentage(vBat)
            val isCritical = vBat < CRITICAL_VOLTAGE
            return BatteryState(
                voltage = vBat,
                percentage = percentage,
                isCritical = isCritical
            )
        }

        private fun calculatePercentage(v: Float): Int {
            return when {
                v >= FULL_VOLTAGE -> 100
                v <= MIN_VOLTAGE -> 0
                v >= NOMINAL_VOLTAGE -> {
                    // 7.4V to 8.4V -> 50% to 100%
                    val ratio = (v - NOMINAL_VOLTAGE) / (FULL_VOLTAGE - NOMINAL_VOLTAGE)
                    (50 + (ratio * 50)).toInt().coerceIn(0, 100)
                }
                else -> {
                    // 6.0V to 7.4V -> 0% to 50%
                    val ratio = (v - MIN_VOLTAGE) / (NOMINAL_VOLTAGE - MIN_VOLTAGE)
                    (ratio * 50).toInt().coerceIn(0, 100)
                }
            }
        }
    }
}
