package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val STORE_NAME = "anti_theft_settings"

val Context.antiTheftDataStore by preferencesDataStore(name = STORE_NAME)

object AntiTheftPreferences {
    val armed: Preferences.Key<Boolean> = booleanPreferencesKey("anti_theft_armed")
    val movementSensitivity: Preferences.Key<Float> =
        floatPreferencesKey("anti_theft_movement_sensitivity")
    val rotationSensitivityDegrees: Preferences.Key<Float> =
        floatPreferencesKey("anti_theft_rotation_sensitivity_degrees")
    val armDelaySeconds: Preferences.Key<Int> = intPreferencesKey("anti_theft_arm_delay_seconds")
    val vibrateEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("anti_theft_vibrate_enabled")
}
