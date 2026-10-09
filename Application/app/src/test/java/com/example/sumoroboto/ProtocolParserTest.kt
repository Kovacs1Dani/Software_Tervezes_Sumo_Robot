package com.example.sumoroboto

import com.example.sumoroboto.data.bluetooth.ProtocolParser
import com.example.sumoroboto.data.model.BatteryState
import com.example.sumoroboto.data.model.RobotMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolParserTest {

    @Test
    fun testMotorCommandFormatting() {
        assertEquals("M,150,150\n", ProtocolParser.createMotorCommand(150, 150))
        assertEquals("M,-100,100\n", ProtocolParser.createMotorCommand(-100, 100))
        // Verify clamping to [-255, 255]
        assertEquals("M,255,-255\n", ProtocolParser.createMotorCommand(400, -300))
    }

    @Test
    fun testModeAndStopCommands() {
        assertEquals("MODE,MANUAL\n", ProtocolParser.createModeCommand(RobotMode.MANUAL))
        assertEquals("MODE,AUTO\n", ProtocolParser.createModeCommand(RobotMode.AUTO))
        assertEquals("STOP\n", ProtocolParser.createStopCommand())
    }

    @Test
    fun testParseTelemetryValidPacket() {
        val line = "TEL,25,32,0,1,7.84\n"
        val data = ProtocolParser.parseTelemetry(line)

        assertNotNull(data)
        assertEquals(25, data!!.distL)
        assertEquals(32, data.distR)
        assertFalse(data.lineL)
        assertTrue(data.lineR)
        assertEquals(7.84f, data.vBat, 0.01f)
    }

    @Test
    fun testParseTelemetryGracefulHandlingOfMalformedPackets() {
        // Not starting with TEL
        assertNull(ProtocolParser.parseTelemetry("HELLO,1,2,3"))
        // Incomplete fields
        assertNull(ProtocolParser.parseTelemetry("TEL,25,32,0"))
        // Non-numeric tokens
        assertNull(ProtocolParser.parseTelemetry("TEL,abc,def,0,0,7.0"))
        // Empty string
        assertNull(ProtocolParser.parseTelemetry(""))
    }

    @Test
    fun testBatteryStateCalculation() {
        val full = BatteryState.fromVoltage(8.4f)
        assertEquals(100, full.percentage)
        assertFalse(full.isCritical)

        val nominal = BatteryState.fromVoltage(7.4f)
        assertEquals(50, nominal.percentage)
        assertFalse(nominal.isCritical)

        val critical = BatteryState.fromVoltage(6.7f)
        assertTrue(critical.isCritical)

        val empty = BatteryState.fromVoltage(6.0f)
        assertEquals(0, empty.percentage)
        assertTrue(empty.isCritical)
    }
}
