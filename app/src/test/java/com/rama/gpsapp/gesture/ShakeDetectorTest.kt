package com.rama.gpsapp.gesture

import com.rama.gpsapp.sensor.SensorSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {
    @Test
    fun `triggers when repeated high jerk peaks happen inside window`() {
        val detector = ShakeDetector(threshold = 15f, requiredPeaks = 3)
        val samples = listOf(
            sample(0, 0f, 0f, 9.8f),
            sample(100_000_000L, 30f, 0f, 0f),
            sample(200_000_000L, 2f, 0f, 0f),
            sample(300_000_000L, 28f, 0f, 0f)
        )

        val triggered = samples.any { detector.onSample(it) }
        assertTrue(triggered)
    }

    @Test
    fun `does not trigger on gentle motion`() {
        val detector = ShakeDetector(threshold = 25f, requiredPeaks = 3)
        val samples = listOf(
            sample(0, 0f, 0f, 9.8f),
            sample(100_000_000L, 0.4f, 0.5f, 9.6f),
            sample(200_000_000L, 0.8f, 0.4f, 9.4f),
            sample(300_000_000L, 1.1f, 0.7f, 9.1f)
        )

        val triggered = samples.any { detector.onSample(it) }
        assertFalse(triggered)
    }

    private fun sample(tNanos: Long, x: Float, y: Float, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = x, y = y, z = z)
}
