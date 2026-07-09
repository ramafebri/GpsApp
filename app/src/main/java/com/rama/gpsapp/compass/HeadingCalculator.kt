package com.rama.gpsapp.compass

/**
 * Converts rotation-vector azimuth (magnetic north) into a smoothed 0–360° heading
 * with optional true-north declination offset.
 */
class HeadingCalculator(
    private var filterAlpha: Float = FILTER_ALPHA,
    private var declinationDegrees: Float = 0f,
    private var useTrueNorth: Boolean = false
) {
    private var filteredHeadingDegrees = 0f
    private var filterInitialized = false
    private var lastAzimuthRadians = 0f

    fun onSample(azimuthRadians: Float): CompassReading? {
        lastAzimuthRadians = azimuthRadians

        val sampleDegrees = azimuthRadiansToDegrees(azimuthRadians)

        if (!filterInitialized) {
            filteredHeadingDegrees = sampleDegrees
            filterInitialized = true
            return null
        }

        filteredHeadingDegrees = smoothDegrees(filteredHeadingDegrees, sampleDegrees, filterAlpha)
        return buildReading()
    }

    fun setFilterAlpha(alpha: Float) {
        filterAlpha = alpha
    }

    fun setDeclinationDegrees(degrees: Float) {
        declinationDegrees = degrees
    }

    fun setUseTrueNorth(enabled: Boolean) {
        useTrueNorth = enabled
    }

    fun currentReading(): CompassReading? =
        if (filterInitialized) buildReading() else null

    fun reset() {
        filterInitialized = false
    }

    private fun buildReading(): CompassReading {
        val displayHeading = applyNorthMode(filteredHeadingDegrees)
        return CompassReading(
            headingDegrees = displayHeading,
            azimuthRadians = lastAzimuthRadians,
            cardinalLabel = CardinalDirection.fromDegrees(displayHeading),
            isCalibrated = false
        )
    }

    private fun azimuthRadiansToDegrees(azimuthRadians: Float): Float {
        val degrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        return ((degrees % 360f) + 360f) % 360f
    }

    private fun applyNorthMode(magneticHeadingDegrees: Float): Float {
        val offset = if (useTrueNorth) declinationDegrees else 0f
        return ((magneticHeadingDegrees + offset) % 360f + 360f) % 360f
    }

    private fun smoothDegrees(current: Float, sample: Float, alpha: Float): Float {
        var delta = sample - current
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        var result = current + (1f - alpha) * delta
        result = ((result % 360f) + 360f) % 360f
        return result
    }

    companion object {
        const val FILTER_ALPHA = 0.85f
    }
}
