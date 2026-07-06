package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AntiTheftSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.antiTheftDataStore

    val settings: Flow<AntiTheftSettings> = dataStore.data.map { preferences ->
        AntiTheftSettings(
            armed = preferences[AntiTheftPreferences.armed] ?: false,
            movementSensitivity = preferences[AntiTheftPreferences.movementSensitivity] ?: 1.9f,
            rotationSensitivityDegrees = preferences[AntiTheftPreferences.rotationSensitivityDegrees] ?: 35f,
            armDelaySeconds = preferences[AntiTheftPreferences.armDelaySeconds] ?: 8,
            vibrateEnabled = preferences[AntiTheftPreferences.vibrateEnabled] ?: true
        )
    }

    suspend fun setArmed(value: Boolean) {
        dataStore.edit { it[AntiTheftPreferences.armed] = value }
    }

    suspend fun setMovementSensitivity(value: Float) {
        dataStore.edit { it[AntiTheftPreferences.movementSensitivity] = value.coerceIn(0.8f, 4.5f) }
    }

    suspend fun setRotationSensitivityDegrees(value: Float) {
        dataStore.edit {
            it[AntiTheftPreferences.rotationSensitivityDegrees] = value.coerceIn(12f, 120f)
        }
    }

    suspend fun setArmDelaySeconds(value: Int) {
        dataStore.edit { it[AntiTheftPreferences.armDelaySeconds] = value.coerceIn(3, 30) }
    }

    suspend fun setVibrateEnabled(value: Boolean) {
        dataStore.edit { it[AntiTheftPreferences.vibrateEnabled] = value }
    }
}
