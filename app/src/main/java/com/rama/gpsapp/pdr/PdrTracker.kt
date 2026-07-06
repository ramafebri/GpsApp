package com.rama.gpsapp.pdr

import kotlin.math.cos
import kotlin.math.sin

/**
 * Maintains the running (X, Y) estimate for pedestrian dead reckoning.
 *
 * Each detected step advances the position by [stepLengthMeters] in the
 * direction of the current heading:
 *
 *   X_new = X_old + (stepLength * sin(heading))
 *   Y_new = Y_old + (stepLength * cos(heading))
 *
 * Heading is measured in radians from north (0 = straight ahead / +Y,
 * increasing clockwise), matching the rotation vector sensor's azimuth.
 */
class PdrTracker {
    private var x = 0f
    private var y = 0f
    private var stepCount = 0

    fun onStep(headingRadians: Float, stepLengthMeters: Float): PdrPosition {
        x += stepLengthMeters * sin(headingRadians)
        y += stepLengthMeters * cos(headingRadians)
        stepCount += 1
        return PdrPosition(x = x, y = y, headingRadians = headingRadians, stepCount = stepCount)
    }

    fun reset() {
        x = 0f
        y = 0f
        stepCount = 0
    }
}
