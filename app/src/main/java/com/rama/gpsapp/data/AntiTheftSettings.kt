package com.rama.gpsapp.data

data class AntiTheftSettings(
    val armed: Boolean = false,
    val movementSensitivity: Float = 1.9f,
    val rotationSensitivityDegrees: Float = 35f,
    val armDelaySeconds: Int = 8,
    val vibrateEnabled: Boolean = true
)
