package com.rama.gpsapp.sensor

data class SensorSample(
    val timestampNanos: Long,
    val x: Float,
    val y: Float,
    val z: Float
)
