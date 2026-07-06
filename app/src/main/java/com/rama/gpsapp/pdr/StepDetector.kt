package com.rama.gpsapp.pdr

import com.rama.gpsapp.sensor.SensorSample
import kotlin.math.sqrt

/**
 * Detects individual walking steps from a stream of accelerometer samples.
 *
 * Gravity is estimated with a low-pass filter and subtracted from the raw
 * reading to obtain linear (dynamic) acceleration. A step is reported the
 * moment the magnitude of that linear acceleration peaks above [threshold],
 * which corresponds to the heel-strike impulse of a walking gait. A minimum
 * interval between steps ([minStepIntervalNanos]) guards against double
 * counting a single footfall.
 */
class StepDetector(
    private val threshold: Float = 1.1f,
    private val minStepIntervalNanos: Long = 250_000_000L,
    private val gravityFilterAlpha: Float = 0.9f
) {
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var gravityInitialized = false

    private var previousMagnitude = 0f
    private var wasRising = false
    private var lastStepTimestampNanos = 0L

    fun onSample(sample: SensorSample): Boolean {
        if (!gravityInitialized) {
            gravityX = sample.x
            gravityY = sample.y
            gravityZ = sample.z
            gravityInitialized = true
            return false
        }

        gravityX = gravityFilterAlpha * gravityX + (1f - gravityFilterAlpha) * sample.x
        gravityY = gravityFilterAlpha * gravityY + (1f - gravityFilterAlpha) * sample.y
        gravityZ = gravityFilterAlpha * gravityZ + (1f - gravityFilterAlpha) * sample.z

        val linearX = sample.x - gravityX
        val linearY = sample.y - gravityY
        val linearZ = sample.z - gravityZ
        val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        val isRising = magnitude > previousMagnitude
        var stepDetected = false

        // A peak is confirmed the sample after the magnitude stops rising, using the
        // value it rose to (previousMagnitude) so the comparison happens at the apex.
        if (wasRising && !isRising && previousMagnitude >= threshold) {
            val elapsedSinceLastStep = sample.timestampNanos - lastStepTimestampNanos
            if (lastStepTimestampNanos == 0L || elapsedSinceLastStep >= minStepIntervalNanos) {
                stepDetected = true
                lastStepTimestampNanos = sample.timestampNanos
            }
        }

        wasRising = isRising
        previousMagnitude = magnitude
        return stepDetected
    }

    fun reset() {
        gravityInitialized = false
        previousMagnitude = 0f
        wasRising = false
        lastStepTimestampNanos = 0L
    }
}
