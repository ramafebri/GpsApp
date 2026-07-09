package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val STORE_NAME = "compass_settings"

val Context.compassDataStore by preferencesDataStore(name = STORE_NAME)

object CompassPreferences {
    val northMode: Preferences.Key<String> = stringPreferencesKey("north_mode")
    val filterAlpha: Preferences.Key<Float> = floatPreferencesKey("filter_alpha")
    val declinationDegrees: Preferences.Key<Float> = floatPreferencesKey("declination_degrees")
}
