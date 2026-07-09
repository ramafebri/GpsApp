package com.rama.gpsapp.level

import com.rama.gpsapp.sensor.SensorSample
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Converts accelerometer samples into pitch, roll, and slope angles with
 * exponential low-pass filtering and optional calibration offsets.
 */
class TiltCalculator(
    private var filterAlpha: Float = FILTER_ALPHA,
    private var toleranceDegrees: Float = LEVEL_TOLERANCE_DEGREES,
    private var pitchOffsetDegrees: Float = 0f,
    private var rollOffsetDegrees: Float = 0f
) {
    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private var filterInitialized = false
    private var frozen = false
    private var lastReading: LevelReading? = null

    fun onSample(sample: SensorSample): LevelReading? {
        if (frozen) {
            return lastReading
        }

        if (!filterInitialized) {
            filteredX = sample.x
            filteredY = sample.y
            filteredZ = sample.z
            filterInitialized = true
            return null
        }

        filteredX = filterAlpha * filteredX + (1f - filterAlpha) * sample.x
        filteredY = filterAlpha * filteredY + (1f - filterAlpha) * sample.y
        filteredZ = filterAlpha * filteredZ + (1f - filterAlpha) * sample.z

        val reading = buildReading()
        lastReading = reading
        return reading
    }

    fun calibrate(pitch: Float, roll: Float) {
        pitchOffsetDegrees = pitch
        rollOffsetDegrees = roll
        lastReading?.let { lastReading = buildReading() }
    }

    fun calibrateCurrentOrientation() {
        if (!filterInitialized) return
        val (pitch, roll) = rawAnglesDegrees()
        calibrate(pitch, roll)
    }

    fun resetCalibration() {
        pitchOffsetDegrees = 0f
        rollOffsetDegrees = 0f
        lastReading?.let { lastReading = buildReading() }
    }

    fun resetFilter() {
        filterInitialized = false
        lastReading = null
    }

    fun setTolerance(degrees: Float) {
        toleranceDegrees = degrees
        lastReading?.let { lastReading = buildReading() }
    }

    fun setFilterAlpha(alpha: Float) {
        filterAlpha = alpha
    }

    fun setOffsets(pitch: Float, roll: Float) {
        pitchOffsetDegrees = pitch
        rollOffsetDegrees = roll
        lastReading?.let { lastReading = buildReading() }
    }

    fun freeze(frozen: Boolean) {
        this.frozen = frozen
    }

    fun getOffsets(): Pair<Float, Float> = pitchOffsetDegrees to rollOffsetDegrees

    private fun buildReading(): LevelReading {
        val (rawPitch, rawRoll) = rawAnglesDegrees()
        val pitch = rawPitch - pitchOffsetDegrees
        val roll = rawRoll - rollOffsetDegrees
        val slope = slopeDegrees()
        val isLevel = abs(pitch) <= toleranceDegrees && abs(roll) <= toleranceDegrees
        val bubbleX = (pitch / BUBBLE_MAX_DEFLECTION_DEGREES).coerceIn(-1f, 1f)
        val bubbleY = (roll / BUBBLE_MAX_DEFLECTION_DEGREES).coerceIn(-1f, 1f)

        return LevelReading(
            pitchDegrees = pitch,
            rollDegrees = roll,
            slopeDegrees = slope,
            isLevel = isLevel,
            bubbleOffsetX = bubbleX,
            bubbleOffsetY = bubbleY
        )
    }

    private fun rawAnglesDegrees(): Pair<Float, Float> {
        val pitch = Math.toDegrees(
            atan2(-filteredX.toDouble(), sqrt((filteredY * filteredY + filteredZ * filteredZ).toDouble()))
        ).toFloat()
        val roll = Math.toDegrees(
            atan2(filteredY.toDouble(), filteredZ.toDouble())
        ).toFloat()
        return pitch to roll
    }

    private fun slopeDegrees(): Float {
        return Math.toDegrees(
            atan2(
                sqrt((filteredX * filteredX + filteredY * filteredY).toDouble()),
                abs(filteredZ).toDouble()
            )
        ).toFloat()
    }

    companion object {
        const val FILTER_ALPHA = 0.85f
        const val LEVEL_TOLERANCE_DEGREES = 1.0f
        const val BUBBLE_MAX_DEFLECTION_DEGREES = 15.0f
    }
}
