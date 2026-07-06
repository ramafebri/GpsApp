package com.rama.gpsapp.data

data class GestureSettings(
    val serviceEnabled: Boolean = false,
    val shakeEnabled: Boolean = true,
    val flipEnabled: Boolean = true,
    val twistEnabled: Boolean = true,
    val shakeSensitivity: Float = 20f,
    val twistSensitivity: Float = 3.2f
)
