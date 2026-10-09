package com.example.sumoroboto.data.bluetooth

import com.example.sumoroboto.data.model.RobotMode
import com.example.sumoroboto.data.model.TelemetryData

object ProtocolParser {

    /**
     * Formats differential motor command: M,<pwmL>,<pwmR>\n
     * Range: -255 to 255
     */
    fun createMotorCommand(pwmL: Int, pwmR: Int): String {
        val clampedL = pwmL.coerceIn(-255, 255)
        val clampedR = pwmR.coerceIn(-255, 255)
        return "M,$clampedL,$clampedR\n"
    }

    /**
     * Formats mode command: MODE,<STATE>\n
     */
    fun createModeCommand(mode: RobotMode): String {
        return "MODE,${mode.name}\n"
    }

    /**
     * Formats emergency stop command: STOP\n
     */
    fun createStopCommand(): String {
        return "STOP\n"
    }

    /**
     * Parses incoming telemetry packet: TEL,<distL>,<distR>,<lineL>,<lineR>,<vBat>
     * Returns null if line is malformed, ensuring crashes never occur.
     */
    fun parseTelemetry(line: String): TelemetryData? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("TEL,")) {
            return null
        }

        val parts = trimmed.split(",")
        if (parts.size < 6) {
            return null
        }

        return try {
            val distL = parts[1].trim().toInt()
            val distR = parts[2].trim().toInt()
            val lineL = parts[3].trim().toInt() != 0
            val lineR = parts[4].trim().toInt() != 0
            val vBat = parts[5].trim().toFloat()

            TelemetryData(
                distL = distL,
                distR = distR,
                lineL = lineL,
                lineR = lineR,
                vBat = vBat,
                timestamp = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            // Gracefully ignore malformed tokens
            null
        }
    }
}
