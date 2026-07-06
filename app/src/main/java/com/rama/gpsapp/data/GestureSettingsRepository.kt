package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GestureSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.gestureDataStore

    val settings: Flow<GestureSettings> = dataStore.data.map { preferences ->
        GestureSettings(
            serviceEnabled = preferences[GesturePreferences.serviceEnabled] ?: false,
            shakeEnabled = preferences[GesturePreferences.shakeEnabled] ?: true,
            flipEnabled = preferences[GesturePreferences.flipEnabled] ?: true,
            twistEnabled = preferences[GesturePreferences.twistEnabled] ?: true,
            shakeSensitivity = preferences[GesturePreferences.shakeSensitivity] ?: 20f,
            twistSensitivity = preferences[GesturePreferences.twistSensitivity] ?: 3.2f
        )
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        dataStore.edit { it[GesturePreferences.serviceEnabled] = enabled }
    }

    suspend fun setShakeEnabled(enabled: Boolean) {
        dataStore.edit { it[GesturePreferences.shakeEnabled] = enabled }
    }

    suspend fun setFlipEnabled(enabled: Boolean) {
        dataStore.edit { it[GesturePreferences.flipEnabled] = enabled }
    }

    suspend fun setTwistEnabled(enabled: Boolean) {
        dataStore.edit { it[GesturePreferences.twistEnabled] = enabled }
    }

    suspend fun setShakeSensitivity(value: Float) {
        dataStore.edit { it[GesturePreferences.shakeSensitivity] = value.coerceIn(8f, 45f) }
    }

    suspend fun setTwistSensitivity(value: Float) {
        dataStore.edit { it[GesturePreferences.twistSensitivity] = value.coerceIn(1.2f, 7f) }
    }
}
