package com.rama.gpsapp.gesture

import com.rama.gpsapp.sensor.SensorSample

class FlipDetector(
    private val faceDownThreshold: Float = -6.5f,
    private val faceUpThreshold: Float = 6.5f,
    private val holdNanos: Long = 220_000_000L,
    private val cooldownNanos: Long = 1_500_000_000L
) {
    private var armed = true
    private var faceDownStart = 0L
    private var lastTriggerTimestamp = 0L

    fun onSample(sample: SensorSample): Boolean {
        if (sample.z >= faceUpThreshold) {
            armed = true
            faceDownStart = 0L
            return false
        }

        if (sample.z <= faceDownThreshold) {
            if (faceDownStart == 0L) {
                faceDownStart = sample.timestampNanos
                return false
            }
            val heldLongEnough = sample.timestampNanos - faceDownStart >= holdNanos
            val canTrigger = sample.timestampNanos - lastTriggerTimestamp >= cooldownNanos
            if (armed && heldLongEnough && canTrigger) {
                armed = false
                lastTriggerTimestamp = sample.timestampNanos
                faceDownStart = 0L
                return true
            }
            return false
        }

        faceDownStart = 0L
        return false
    }
}
