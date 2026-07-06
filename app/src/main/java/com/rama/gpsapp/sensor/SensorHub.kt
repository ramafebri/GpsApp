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
