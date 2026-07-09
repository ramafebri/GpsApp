package com.rama.gpsapp.level

import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Collects accelerometer samples and converts them into live [LevelReading]
 * updates via [TiltCalculator].
 */
class LevelEngine(
    private val sensorHub: SensorHub
) {
    private val lock = Any()
    private val calculator = TiltCalculator()

    fun observe(): Flow<LevelReading> =
        sensorHub.accelerometer().mapNotNull { sample ->
            synchronized(lock) { calculator.onSample(sample) }
        }

    fun calibrate() {
        synchronized(lock) { calculator.calibrateCurrentOrientation() }
    }

    fun resetCalibration() {
        synchronized(lock) { calculator.resetCalibration() }
    }

    fun setTolerance(degrees: Float) {
        synchronized(lock) { calculator.setTolerance(degrees) }
    }

    fun setFilterAlpha(alpha: Float) {
        synchronized(lock) { calculator.setFilterAlpha(alpha) }
    }

    fun setOffsets(pitch: Float, roll: Float) {
        synchronized(lock) { calculator.setOffsets(pitch, roll) }
    }

    fun setFrozen(frozen: Boolean) {
        synchronized(lock) { calculator.freeze(frozen) }
    }

    fun getCalibrationOffsets(): Pair<Float, Float> =
        synchronized(lock) { calculator.getOffsets() }

    fun hasRequiredSensors(): Boolean = sensorHub.hasAccelerometer()
}
