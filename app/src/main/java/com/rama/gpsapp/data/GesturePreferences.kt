package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val STORE_NAME = "gesture_settings"

val Context.gestureDataStore by preferencesDataStore(name = STORE_NAME)

object GesturePreferences {
    val serviceEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("service_enabled")
    val shakeEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("shake_enabled")
    val flipEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("flip_enabled")
    val twistEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("twist_enabled")
    val shakeSensitivity: Preferences.Key<Float> = floatPreferencesKey("shake_sensitivity")
    val twistSensitivity: Preferences.Key<Float> = floatPreferencesKey("twist_sensitivity")
}
