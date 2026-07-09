package com.rama.gpsapp.level

data class LevelReading(
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val slopeDegrees: Float,
    val isLevel: Boolean,
    val bubbleOffsetX: Float,
    val bubbleOffsetY: Float
)
