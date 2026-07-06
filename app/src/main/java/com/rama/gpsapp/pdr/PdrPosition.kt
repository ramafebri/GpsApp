package com.rama.gpsapp.pdr

/**
 * A single point on the dead-reckoned path, relative to the origin (0, 0)
 * where tracking started. Units are meters for [x]/[y] and radians for
 * [headingRadians] (0 = north/forward, increasing clockwise).
 */
data class PdrPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val headingRadians: Float = 0f,
    val stepCount: Int = 0
)
