package com.rama.gpsapp.pdr

import com.rama.gpsapp.sensor.SensorSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepDetectorTest {
    @Test
    fun `detects a step the sample after an acceleration peak`() {
        val detector = StepDetector(threshold = 1.1f, minStepIntervalNanos = 100_000_000L)

        val results = listOf(
            sample(0L, 0f, 0f, 9.8f),
            sample(100_000_000L, 0f, 0f, 9.8f),
            sample(200_000_000L, 0f, 0f, 12.8f),
            sample(300_000_000L, 0f, 0f, 9.8f)
        ).map(detector::onSample)

        assertEquals(listOf(false, false, false, true), results)
    }

    @Test
    fun `does not trigger on gentle jitter below threshold`() {
        val detector = StepDetector(threshold = 2.5f)

        val triggered = listOf(
            sample(0L, 0f, 0f, 9.8f),
            sample(100_000_000L, 0.1f, 0f, 9.8f),
            sample(200_000_000L, 0.2f, 0f, 9.9f),
            sample(300_000_000L, 0.1f, 0f, 9.8f),
            sample(400_000_000L, 0f, 0f, 9.8f)
        ).any(detector::onSample)

        assertFalse(triggered)
    }

    @Test
    fun `ignores a second peak that arrives before the minimum step interval`() {
        val detector = StepDetector(threshold = 1.1f, minStepIntervalNanos = 500_000_000L)

        val results = listOf(
            sample(0L, 0f, 0f, 9.8f),
            sample(100_000_000L, 0f, 0f, 9.8f),
            sample(200_000_000L, 0f, 0f, 12.8f),
            sample(300_000_000L, 0f, 0f, 9.8f), // first step confirmed here
            sample(400_000_000L, 0f, 0f, 12.8f),
            sample(500_000_000L, 0f, 0f, 9.8f) // too soon, should be suppressed
        ).map(detector::onSample)

        assertEquals(1, results.count { it })
        assertTrue(results[3])
        assertFalse(results[5])
    }

    @Test
    fun `reset clears gravity baseline and cooldown state`() {
        val detector = StepDetector(threshold = 1.1f, minStepIntervalNanos = 100_000_000L)
        listOf(
            sample(0L, 0f, 0f, 9.8f),
            sample(100_000_000L, 0f, 0f, 9.8f),
            sample(200_000_000L, 0f, 0f, 12.8f),
            sample(300_000_000L, 0f, 0f, 9.8f)
        ).forEach(detector::onSample)

        detector.reset()

        val results = listOf(
            sample(0L, 0f, 0f, 9.8f),
            sample(100_000_000L, 0f, 0f, 9.8f),
            sample(200_000_000L, 0f, 0f, 12.8f),
            sample(300_000_000L, 0f, 0f, 9.8f)
        ).map(detector::onSample)

        assertEquals(listOf(false, false, false, true), results)
    }

    private fun sample(tNanos: Long, x: Float, y: Float, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = x, y = y, z = z)
}
