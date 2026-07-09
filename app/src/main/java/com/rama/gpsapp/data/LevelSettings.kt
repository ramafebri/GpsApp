package com.rama.gpsapp.data

import com.rama.gpsapp.level.LevelDisplayMode
import com.rama.gpsapp.level.TiltCalculator

data class LevelSettings(
    val displayMode: LevelDisplayMode = LevelDisplayMode.BUBBLE,
    val levelToleranceDegrees: Float = TiltCalculator.LEVEL_TOLERANCE_DEGREES,
    val filterAlpha: Float = TiltCalculator.FILTER_ALPHA,
    val calibrationPitchOffsetDegrees: Float = 0f,
    val calibrationRollOffsetDegrees: Float = 0f
)
