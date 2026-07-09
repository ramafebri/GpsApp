package com.rama.gpsapp.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingCalculatorTest {

    @Test
    fun `first sample returns null for filter init`() {
        val calculator = HeadingCalculator()

        assertNull(calculator.onSample(azimuthDegrees(0f)))
    }

    @Test
    fun `second sample at zero azimuth reports north heading`() {
        val calculator = HeadingCalculator()

        assertNull(calculator.onSample(azimuthDegrees(0f)))

        val reading = calculator.onSample(azimuthDegrees(0f))
        assertNotNull(reading)
        assertEquals(0f, reading!!.headingDegrees, ANGLE_TOLERANCE)
        assertEquals("N", reading.cardinalLabel)
    }

    @Test
    fun `smoothing takes shortest path across north wrap`() {
        val calculator = HeadingCalculator(filterAlpha = HeadingCalculator.FILTER_ALPHA)

        assertNull(calculator.onSample(azimuthDegrees(350f)))
        val at350 = calculator.onSample(azimuthDegrees(350f))
        assertNotNull(at350)
        assertEquals(350f, at350!!.headingDegrees, ANGLE_TOLERANCE)

        val afterJump = calculator.onSample(azimuthDegrees(10f))
        assertNotNull(afterJump)

        // Shortest path: 350° → 10° moves forward (+20°), not backward through 340°.
        // With alpha=0.85: 350 + 0.15 * 20 = 353°
        assertTrue(afterJump!!.headingDegrees > 340f)
        assertTrue(afterJump.headingDegrees < 360f)
    }

    @Test
    fun `true north mode applies declination offset`() {
        val calculator = HeadingCalculator(
            filterAlpha = 0f,
            declinationDegrees = 10f,
            useTrueNorth = true
        )

        assertNull(calculator.onSample(azimuthDegrees(0f)))

        val reading = calculator.onSample(azimuthDegrees(0f))
        assertNotNull(reading)
        assertEquals(10f, reading!!.headingDegrees, ANGLE_TOLERANCE)
    }

    @Test
    fun `cardinal direction labels match heading degrees`() {
        assertEquals("N", CardinalDirection.fromDegrees(0f))
        assertEquals("E", CardinalDirection.fromDegrees(90f))
        assertEquals("S", CardinalDirection.fromDegrees(180f))
        assertEquals("W", CardinalDirection.fromDegrees(270f))
        assertEquals("NE", CardinalDirection.fromDegrees(45f))
    }

    private fun azimuthDegrees(degrees: Float): Float =
        Math.toRadians(degrees.toDouble()).toFloat()

    companion object {
        private const val ANGLE_TOLERANCE = 0.5f
    }
}
