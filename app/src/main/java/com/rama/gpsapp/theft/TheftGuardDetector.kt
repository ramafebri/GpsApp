package com.rama.gpsapp.theft

import com.rama.gpsapp.sensor.SensorSample
import kotlin.math.sqrt

enum class TheftGuardStage {
    DISARMED,
    ARMING,
    CALIBRATING,
    MONITORING,
    TRIGGERED
}

class TheftGuardDetector(
    private val movementThreshold: Float,
    private val rotationThresholdDegrees: Float,
    private val armDelayNanos: Long,
    private val calibrationWindowNanos: Long = 1_200_000_000L,
    private val calibrationMotionThreshold: Float = 0.8f,
    private val movementSustainNanos: Long = 250_000_000L
) {
    private var stageValue: TheftGuardStage = TheftGuardStage.DISARMED
    private var armTransitionNanos: Long = 0L

    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = 0f
    private var calibrationStartedAt = 0L

    private var movementExceededAt = 0L
    private var lastGyroTimestamp = 0L
    private var accumulatedRotationDegrees = 0f

    val stage: TheftGuardStage
        get() = stageValue

    fun arm(nowNanos: Long) {
        stageValue = TheftGuardStage.ARMING
        armTransitionNanos = nowNanos + armDelayNanos
        resetCalibration()
        resetMonitoringState()
    }

    fun disarm() {
        stageValue = TheftGuardStage.DISARMED
        armTransitionNanos = 0L
        resetCalibration()
        resetMonitoringState()
    }

    fun onAccelerometerSample(sample: SensorSample): TheftTrigger? {
        when (stageValue) {
            TheftGuardStage.DISARMED,
            TheftGuardStage.TRIGGERED -> return null

            TheftGuardStage.ARMING -> {
                if (sample.timestampNanos < armTransitionNanos) {
                    return null
                }
                enterCalibrating(sample)
                return null
            }

            TheftGuardStage.CALIBRATING -> {
                updateCalibration(sample)
                return null
            }

            TheftGuardStage.MONITORING -> {
                return detectMovement(sample)
            }
        }
    }

    fun onGyroscopeSample(sample: SensorSample): TheftTrigger? {
        if (stageValue != TheftGuardStage.MONITORING) return null

        if (lastGyroTimestamp == 0L) {
            lastGyroTimestamp = sample.timestampNanos
            return null
        }

        val dtSeconds = (sample.timestampNanos - lastGyroTimestamp) / 1_000_000_000f
        lastGyroTimestamp = sample.timestampNanos
        if (dtSeconds <= 0f) return null

        val angularMagnitude = magnitude(sample.x, sample.y, sample.z)
        accumulatedRotationDegrees += angularMagnitude * dtSeconds * RAD_TO_DEG
        if (accumulatedRotationDegrees >= rotationThresholdDegrees) {
            stageValue = TheftGuardStage.TRIGGERED
            return TheftTrigger.ROTATION
        }
        return null
    }

    private fun enterCalibrating(sample: SensorSample) {
        stageValue = TheftGuardStage.CALIBRATING
        calibrationStartedAt = sample.timestampNanos
        baselineX = sample.x
        baselineY = sample.y
        baselineZ = sample.z
        resetMonitoringState()
    }

    private fun updateCalibration(sample: SensorSample) {
        val deltaFromBaseline = magnitude(
            sample.x - baselineX,
            sample.y - baselineY,
            sample.z - baselineZ
        )

        if (deltaFromBaseline > calibrationMotionThreshold) {
            // If the desk is still moving, restart the stillness window.
            calibrationStartedAt = sample.timestampNanos
            baselineX = sample.x
            baselineY = sample.y
            baselineZ = sample.z
            return
        }

        baselineX = (baselineX * CALIBRATION_ALPHA) + (sample.x * (1f - CALIBRATION_ALPHA))
        baselineY = (baselineY * CALIBRATION_ALPHA) + (sample.y * (1f - CALIBRATION_ALPHA))
        baselineZ = (baselineZ * CALIBRATION_ALPHA) + (sample.z * (1f - CALIBRATION_ALPHA))

        if (sample.timestampNanos - calibrationStartedAt >= calibrationWindowNanos) {
            stageValue = TheftGuardStage.MONITORING
            resetMonitoringState()
        }
    }

    private fun detectMovement(sample: SensorSample): TheftTrigger? {
        val deltaFromBaseline = magnitude(
            sample.x - baselineX,
            sample.y - baselineY,
            sample.z - baselineZ
        )

        if (deltaFromBaseline >= movementThreshold) {
            if (movementExceededAt == 0L) {
                movementExceededAt = sample.timestampNanos
                return null
            }

            if (sample.timestampNanos - movementExceededAt >= movementSustainNanos) {
                stageValue = TheftGuardStage.TRIGGERED
                return TheftTrigger.MOVEMENT
            }
            return null
        }

        movementExceededAt = 0L
        return null
    }

    private fun resetCalibration() {
        baselineX = 0f
        baselineY = 0f
        baselineZ = 0f
        calibrationStartedAt = 0L
    }

    private fun resetMonitoringState() {
        movementExceededAt = 0L
        lastGyroTimestamp = 0L
        accumulatedRotationDegrees = 0f
    }

    private fun magnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

    companion object {
        private const val CALIBRATION_ALPHA = 0.85f
        private const val RAD_TO_DEG = (180f / kotlin.math.PI).toFloat()
    }
}
