package com.rama.gpsapp.gesture

import com.rama.gpsapp.sensor.SensorSample
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.sqrt

class ShakeDetector(
    private val threshold: Float,
    private val requiredPeaks: Int = 3,
    private val windowNanos: Long = 700_000_000L,
    private val cooldownNanos: Long = 1_500_000_000L
) {
    private var previousMagnitude = 0f
    private var hasPreviousSample = false
    private var previousTimestamp = 0L
    private var lastTriggerTimestamp = 0L
    private val peakTimestamps = ArrayDeque<Long>()

    fun onSample(sample: SensorSample): Boolean {
        val magnitude = sqrt(sample.x * sample.x + sample.y * sample.y + sample.z * sample.z)

        if (!hasPreviousSample) {
            hasPreviousSample = true
            previousTimestamp = sample.timestampNanos
            previousMagnitude = magnitude
            return false
        }

        val deltaTimeSeconds = (sample.timestampNanos - previousTimestamp) / 1_000_000_000f
        previousTimestamp = sample.timestampNanos
        if (deltaTimeSeconds <= 0f) {
            previousMagnitude = magnitude
            return false
        }

        val jerk = abs(magnitude - previousMagnitude) / deltaTimeSeconds
        previousMagnitude = magnitude

        if (jerk >= threshold) {
            peakTimestamps.addLast(sample.timestampNanos)
        }

        while (peakTimestamps.isNotEmpty() && sample.timestampNanos - peakTimestamps.first() > windowNanos) {
            peakTimestamps.removeFirst()
        }

        val canTrigger = lastTriggerTimestamp == 0L ||
            sample.timestampNanos - lastTriggerTimestamp >= cooldownNanos
        if (canTrigger && peakTimestamps.size >= requiredPeaks) {
            lastTriggerTimestamp = sample.timestampNanos
            peakTimestamps.clear()
            return true
        }

        return false
    }
}
