package com.example

import com.example.model.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedUnitTest {

    @Test
    fun testMbpsFormatting() {
        val bytesPerSec = 1_250_000L // 10 Mbps (1.25 MB/s * 8 = 10 Mbps)
        val formatted = SpeedUnit.MBPS.formatSpeed(bytesPerSec)
        assertTrue(formatted.contains("Мбит/с"))
        assertTrue(formatted.contains("10"))
    }

    @Test
    fun testKbpsFormatting() {
        val bytesPerSec = 125_000L // 1,000 kbps (125 KB/s * 8 = 1000 kbps)
        val formatted = SpeedUnit.KBPS.formatSpeed(bytesPerSec)
        assertTrue(formatted.contains("Кбит/с"))
        assertTrue(formatted.contains("1 000") || formatted.contains("1000"))
    }

    @Test
    fun testZeroSpeedFormatting() {
        val zeroBytes = 0L
        val mbpsZero = SpeedUnit.MBPS.formatSpeed(zeroBytes)
        val kbpsZero = SpeedUnit.KBPS.formatSpeed(zeroBytes)
        assertEquals("0.0 Мбит/с", mbpsZero)
        assertEquals("0 Кбит/с", kbpsZero)
    }

    @Test
    fun testToUnitValue() {
        val bytes = 2_500_000L // 20 Mbps
        val mbpsVal = SpeedUnit.MBPS.toUnitValue(bytes)
        assertEquals(20f, mbpsVal, 0.01f)

        val kbpsVal = SpeedUnit.KBPS.toUnitValue(bytes)
        assertEquals(20_000f, kbpsVal, 0.01f)
    }
}
