package com.rama.gpsapp.sensor

data class RotationVectorSample(
    val timestampNanos: Long,
    val azimuthRadians: Float,
    val pitchRadians: Float,
    val rollRadians: Float,
    val accuracy: Int
)
