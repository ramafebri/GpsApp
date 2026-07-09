package com.rama.gpsapp.compass

data class CompassReading(
    val headingDegrees: Float,
    val azimuthRadians: Float,
    val cardinalLabel: String,
    val isCalibrated: Boolean
)
