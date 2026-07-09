package com.rama.gpsapp.level

import com.rama.gpsapp.sensor.SensorSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class TiltCalculatorTest {

    @Test
    fun `level device after filter init reports near zero pitch and roll`() {
        val calculator = TiltCalculator()
        val levelVector = sample(0L, 0f, 0f, 9.8f)

        assertNull(calculator.onSample(levelVector))

        val reading = calculator.onSample(levelVector)
        assertNotNull(reading)
        assertEquals(0f, reading!!.pitchDegrees, ANGLE_TOLERANCE)
        assertEquals(0f, reading.rollDegrees, ANGLE_TOLERANCE)
        assertTrue(reading.isLevel)
    }

    @Test
    fun `x dominated tilt reports non level pitch`() {
        val calculator = TiltCalculator()
        val tiltedVector = sample(0L, 3f, 0f, 9f)

        assertNull(calculator.onSample(tiltedVector))

        val reading = feedUntilStable(calculator, tiltedVector)
        val expectedPitch = Math.toDegrees(
            atan2(-3.0, sqrt(81.0))
        ).toFloat()

        assertNotNull(reading)
        assertEquals(expectedPitch, reading!!.pitchDegrees, ANGLE_TOLERANCE)
        assertEquals(0f, reading.rollDegrees, ANGLE_TOLERANCE)
        assertFalse(reading.isLevel)
    }

    @Test
    fun `calibration zeroes current reading`() {
        val calculator = TiltCalculator()
        val tiltedVector = sample(0L, 3f, 0f, 9f)

        assertNull(calculator.onSample(tiltedVector))
        val beforeCalibration = feedUntilStable(calculator, tiltedVector)
        assertNotNull(beforeCalibration)
        assertFalse(beforeCalibration!!.isLevel)

        calculator.calibrate(
            pitch = beforeCalibration.pitchDegrees,
            roll = beforeCalibration.rollDegrees
        )

        val afterCalibration = calculator.onSample(tiltedVector)
        assertNotNull(afterCalibration)
        assertEquals(0f, afterCalibration!!.pitchDegrees, ANGLE_TOLERANCE)
        assertEquals(0f, afterCalibration.rollDegrees, ANGLE_TOLERANCE)
        assertTrue(afterCalibration.isLevel)
    }

    @Test
    fun `freeze holds last reading across new samples`() {
        val calculator = TiltCalculator()
        val levelVector = sample(0L, 0f, 0f, 9.8f)
        val tiltedVector = sample(100_000_000L, 3f, 0f, 9f)

        assertNull(calculator.onSample(levelVector))
        val levelReading = feedUntilStable(calculator, levelVector)
        assertNotNull(levelReading)

        calculator.freeze(frozen = true)

        val frozenReading = calculator.onSample(tiltedVector)
        assertNotNull(frozenReading)
        assertEquals(levelReading!!.pitchDegrees, frozenReading!!.pitchDegrees, ANGLE_TOLERANCE)
        assertEquals(levelReading.rollDegrees, frozenReading.rollDegrees, ANGLE_TOLERANCE)
        assertEquals(levelReading.isLevel, frozenReading.isLevel)
    }

    @Test
    fun `filter smooths abrupt step change`() {
        val calculator = TiltCalculator(filterAlpha = TiltCalculator.FILTER_ALPHA)
        val levelVector = sample(0L, 0f, 0f, 9.8f)
        val tiltedVector = sample(100_000_000L, 3f, 0f, 9f)

        assertNull(calculator.onSample(levelVector))
        feedUntilStable(calculator, levelVector)

        val firstAfterStep = calculator.onSample(tiltedVector)
        assertNotNull(firstAfterStep)

        val fullTiltPitch = Math.toDegrees(atan2(-3.0, 9.0)).toFloat()

        assertTrue(abs(firstAfterStep!!.pitchDegrees) > 0.5f)
        assertTrue(abs(firstAfterStep.pitchDegrees) < abs(fullTiltPitch) - 1f)
    }

    private fun feedUntilStable(
        calculator: TiltCalculator,
        vector: SensorSample,
        iterations: Int = 30
    ): LevelReading? {
        var reading: LevelReading? = null
        repeat(iterations) { index ->
            reading = calculator.onSample(
                vector.copy(timestampNanos = vector.timestampNanos + index * 100_000_000L)
            )
        }
        return reading
    }

    private fun sample(tNanos: Long, x: Float, y: Float, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = x, y = y, z = z)

    companion object {
        private const val ANGLE_TOLERANCE = 0.5f
    }
}
