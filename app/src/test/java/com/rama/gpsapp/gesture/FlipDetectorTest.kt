package com.rama.gpsapp.gesture

import com.rama.gpsapp.sensor.SensorSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipDetectorTest {
    @Test
    fun `triggers when phone goes face down and is held`() {
        val detector = FlipDetector()
        val samples = listOf(
            sample(0L, z = 8f),
            sample(100_000_000L, z = -8f),
            sample(360_000_000L, z = -8f)
        )

        val triggered = samples.any { detector.onSample(it) }
        assertTrue(triggered)
    }

    @Test
    fun `does not trigger when briefly tilted`() {
        val detector = FlipDetector()
        val samples = listOf(
            sample(0L, z = 8f),
            sample(100_000_000L, z = -7f),
            sample(220_000_000L, z = -2f),
            sample(340_000_000L, z = 7f)
        )

        val triggered = samples.any { detector.onSample(it) }
        assertFalse(triggered)
    }

    private fun sample(tNanos: Long, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = 0f, y = 0f, z = z)
}
