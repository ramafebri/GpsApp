package com.rama.gpsapp.gesture

import com.rama.gpsapp.sensor.SensorSample
import java.util.ArrayDeque
import kotlin.math.abs

class TwistDetector(
    private val angularThreshold: Float,
    private val requiredPeaks: Int = 2,
    private val windowNanos: Long = 1_200_000_000L,
    private val cooldownNanos: Long = 1_600_000_000L
) {
    private val peakTimestamps = ArrayDeque<Long>()
    private var lastTriggerTimestamp = 0L

    fun onSample(sample: SensorSample): Boolean {
        if (abs(sample.z) >= angularThreshold) {
            peakTimestamps.addLast(sample.timestampNanos)
        }

        while (peakTimestamps.isNotEmpty() && sample.timestampNanos - peakTimestamps.first() > windowNanos) {
            peakTimestamps.removeFirst()
        }

        val canTrigger = sample.timestampNanos - lastTriggerTimestamp >= cooldownNanos
        if (canTrigger && peakTimestamps.size >= requiredPeaks) {
            lastTriggerTimestamp = sample.timestampNanos
            peakTimestamps.clear()
            return true
        }
        return false
    }
}
