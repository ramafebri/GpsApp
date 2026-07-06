package com.rama.gpsapp.theft

import com.rama.gpsapp.sensor.SensorSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TheftGuardDetectorTest {
    @Test
    fun `does not trigger during arm delay`() {
        val detector = TheftGuardDetector(
            movementThreshold = 1.5f,
            rotationThresholdDegrees = 25f,
            armDelayNanos = 300_000_000L
        )

        detector.arm(nowNanos = 0L)
        val trigger = detector.onAccelerometerSample(accel(100_000_000L, 0f, 0f, 9.8f))

        assertNull(trigger)
        assertEquals(TheftGuardStage.ARMING, detector.stage)
    }

    @Test
    fun `calibration restarts when motion occurs`() {
        val detector = TheftGuardDetector(
            movementThreshold = 1.5f,
            rotationThresholdDegrees = 25f,
            armDelayNanos = 100_000_000L,
            calibrationWindowNanos = 120_000_000L
        )

        detector.arm(nowNanos = 0L)
        detector.onAccelerometerSample(accel(110_000_000L, 0f, 0f, 9.8f))
        detector.onAccelerometerSample(accel(150_000_000L, 5.5f, 0f, 9.8f))
        detector.onAccelerometerSample(accel(230_000_000L, 5.4f, 0f, 9.8f))
        assertEquals(TheftGuardStage.CALIBRATING, detector.stage)

        detector.onAccelerometerSample(accel(290_000_000L, 5.45f, 0f, 9.8f))
        assertEquals(TheftGuardStage.MONITORING, detector.stage)
    }

    @Test
    fun `triggers movement once sustained deviation occurs while monitoring`() {
        val detector = TheftGuardDetector(
            movementThreshold = 1.2f,
            rotationThresholdDegrees = 40f,
            armDelayNanos = 100_000_000L,
            calibrationWindowNanos = 100_000_000L,
            movementSustainNanos = 60_000_000L
        )

        detector.arm(nowNanos = 0L)
        detector.onAccelerometerSample(accel(110_000_000L, 0f, 0f, 9.8f))
        detector.onAccelerometerSample(accel(220_000_000L, 0f, 0f, 9.8f))
        assertEquals(TheftGuardStage.MONITORING, detector.stage)

        val first = detector.onAccelerometerSample(accel(300_000_000L, 2f, 0f, 9.8f))
        val second = detector.onAccelerometerSample(accel(380_000_000L, 2.2f, 0f, 9.7f))

        assertNull(first)
        assertEquals(TheftTrigger.MOVEMENT, second)
        assertEquals(TheftGuardStage.TRIGGERED, detector.stage)
    }

    @Test
    fun `triggers rotation from gyro integration`() {
        val detector = TheftGuardDetector(
            movementThreshold = 2.5f,
            rotationThresholdDegrees = 20f,
            armDelayNanos = 100_000_000L,
            calibrationWindowNanos = 100_000_000L
        )

        detector.arm(nowNanos = 0L)
        detector.onAccelerometerSample(accel(110_000_000L, 0f, 0f, 9.8f))
        detector.onAccelerometerSample(accel(220_000_000L, 0f, 0f, 9.8f))
        assertEquals(TheftGuardStage.MONITORING, detector.stage)

        assertNull(detector.onGyroscopeSample(gyro(300_000_000L, 0f, 0f, 1f)))
        val trigger = detector.onGyroscopeSample(gyro(900_000_000L, 0f, 0f, 1f))

        assertEquals(TheftTrigger.ROTATION, trigger)
        assertEquals(TheftGuardStage.TRIGGERED, detector.stage)
    }

    @Test
    fun `disarm resets state and suppresses triggers`() {
        val detector = TheftGuardDetector(
            movementThreshold = 1f,
            rotationThresholdDegrees = 15f,
            armDelayNanos = 50_000_000L,
            calibrationWindowNanos = 80_000_000L
        )

        detector.arm(nowNanos = 0L)
        detector.onAccelerometerSample(accel(60_000_000L, 0f, 0f, 9.8f))
        detector.onAccelerometerSample(accel(160_000_000L, 0f, 0f, 9.8f))
        assertTrue(detector.stage == TheftGuardStage.MONITORING || detector.stage == TheftGuardStage.CALIBRATING)

        detector.disarm()
        val trigger = detector.onAccelerometerSample(accel(300_000_000L, 4f, 0f, 8f))

        assertNull(trigger)
        assertEquals(TheftGuardStage.DISARMED, detector.stage)
    }

    private fun accel(tNanos: Long, x: Float, y: Float, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = x, y = y, z = z)

    private fun gyro(tNanos: Long, x: Float, y: Float, z: Float): SensorSample =
        SensorSample(timestampNanos = tNanos, x = x, y = y, z = z)
}
