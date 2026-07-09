package com.rama.gpsapp.data

import com.rama.gpsapp.compass.CompassNorthMode
import com.rama.gpsapp.compass.HeadingCalculator

data class CompassSettings(
    val northMode: CompassNorthMode = CompassNorthMode.MAGNETIC,
    val filterAlpha: Float = HeadingCalculator.FILTER_ALPHA,
    val declinationDegrees: Float = 0f
)
