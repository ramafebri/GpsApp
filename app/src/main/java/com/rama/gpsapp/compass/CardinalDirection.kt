package com.rama.gpsapp.compass

object CardinalDirection {
    private val LABELS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    fun fromDegrees(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        val index = ((normalized + 22.5f) / 45f).toInt() % 8
        return LABELS[index]
    }
}
