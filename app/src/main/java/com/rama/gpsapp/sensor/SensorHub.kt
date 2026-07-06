package com.rama.gpsapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class SensorHub(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun accelerometer(): Flow<SensorSample> = sensorFlow(Sensor.TYPE_ACCELEROMETER)

    fun gyroscope(): Flow<SensorSample> = sensorFlow(Sensor.TYPE_GYROSCOPE)

    /**
     * Emits device orientation derived from the rotation vector sensor (a fused
     * accelerometer + gyroscope + magnetometer estimate). The [SensorSample] fields
     * are repurposed to carry orientation angles in radians rather than raw axis
     * readings: [SensorSample.x] = azimuth/heading, [SensorSample.y] = pitch,
     * [SensorSample.z] = roll.
     */
    fun rotationVector(): Flow<SensorSample> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            close(IllegalStateException("Sensor type ${Sensor.TYPE_ROTATION_VECTOR} not available"))
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                trySend(
                    SensorSample(
                        timestampNanos = event.timestamp,
                        x = orientation[0],
                        y = orientation[1],
                        z = orientation[2]
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener, sensor) }
    }.conflate()

    fun hasAccelerometer(): Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    fun hasRotationVector(): Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    private fun sensorFlow(sensorType: Int): Flow<SensorSample> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            close(IllegalStateException("Sensor type $sensorType not available"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values
                if (values.size < 3) return
                trySend(
                    SensorSample(
                        timestampNanos = event.timestamp,
                        x = values[0],
                        y = values[1],
                        z = values[2]
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener, sensor) }
    }.conflate()
}
