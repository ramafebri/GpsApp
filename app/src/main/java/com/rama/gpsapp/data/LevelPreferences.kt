package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val STORE_NAME = "level_settings"

val Context.levelDataStore by preferencesDataStore(name = STORE_NAME)

object LevelPreferences {
    val displayMode: Preferences.Key<String> = stringPreferencesKey("display_mode")
    val levelToleranceDegrees: Preferences.Key<Float> = floatPreferencesKey("level_tolerance_degrees")
    val filterAlpha: Preferences.Key<Float> = floatPreferencesKey("filter_alpha")
    val calibrationPitchOffsetDegrees: Preferences.Key<Float> =
        floatPreferencesKey("calibration_pitch_offset_degrees")
    val calibrationRollOffsetDegrees: Preferences.Key<Float> =
        floatPreferencesKey("calibration_roll_offset_degrees")
}
