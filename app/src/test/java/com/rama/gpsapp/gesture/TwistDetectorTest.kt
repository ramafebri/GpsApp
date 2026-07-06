package com.rama.gpsapp.gesture

import com.rama.gpsapp.sensor.SensorSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwistDetectorTest {
    @Test
    fun `triggers when two strong twists happen quickly`() {
        val detector = TwistDetector(angularThreshold = 2f, requiredPeaks = 2)
        val samples = listOf(
            sample(0L, z = 0.5f),
            sample(150_000_000L, z = 3.2f),
            sample(450_000_000L, z = -3.1f)
        )

        val triggered = samples.any { detector.onSample(it) }
        assertTrue(triggered)
    }

    @Test
    fun `does not trigger on low angular velocity`() {
        val detector = TwistDetector(angularThreshold = 2f, requiredPeaks = 2)
        val samples = listOf(
            sample(0L, z = 0.4f),
            sample(300_000_000L, z = 0.7f),
            sample(600_000_000L, z = 1.1f),
            sample(900_000_000L, z = 0.6f)
        )

        val triggered = samples.any { detector.onSample(it) }
        assertFalse(triggered)
    }

    private fun sample(tNanos: Long, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = 0f, y = 0f, z = z)
}
