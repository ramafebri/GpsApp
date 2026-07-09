package com.rama.gpsapp.compass

import android.hardware.SensorManager
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Collects rotation-vector samples and converts them into live [CompassReading]
 * updates via [HeadingCalculator].
 */
class CompassEngine(
    private val sensorHub: SensorHub
) {
    private val lock = Any()
    private val calculator = HeadingCalculator()

    fun observe(): Flow<CompassReading> =
        sensorHub.rotationVectorWithAccuracy().mapNotNull { sample ->
            synchronized(lock) {
                val isCalibrated = sample.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
                calculator.onSample(sample.azimuthRadians)?.copy(isCalibrated = isCalibrated)
            }
        }

    fun setFilterAlpha(alpha: Float) {
        synchronized(lock) { calculator.setFilterAlpha(alpha) }
    }

    fun setDeclinationDegrees(degrees: Float) {
        synchronized(lock) { calculator.setDeclinationDegrees(degrees) }
    }

    fun setUseTrueNorth(enabled: Boolean) {
        synchronized(lock) { calculator.setUseTrueNorth(enabled) }
    }

    fun reset() {
        synchronized(lock) { calculator.reset() }
    }

    fun currentReading(): CompassReading? =
        synchronized(lock) { calculator.currentReading() }

    fun hasRequiredSensors(): Boolean = sensorHub.hasRotationVector()
}
