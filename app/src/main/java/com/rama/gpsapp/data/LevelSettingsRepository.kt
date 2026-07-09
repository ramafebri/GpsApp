package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.rama.gpsapp.level.LevelDisplayMode
import com.rama.gpsapp.level.TiltCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LevelSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.levelDataStore

    val settings: Flow<LevelSettings> = dataStore.data.map { preferences ->
        LevelSettings(
            displayMode = LevelDisplayMode.fromPersistedString(
                preferences[LevelPreferences.displayMode] ?: LevelDisplayMode.BUBBLE.name
            ),
            levelToleranceDegrees = preferences[LevelPreferences.levelToleranceDegrees]
                ?: TiltCalculator.LEVEL_TOLERANCE_DEGREES,
            filterAlpha = preferences[LevelPreferences.filterAlpha] ?: TiltCalculator.FILTER_ALPHA,
            calibrationPitchOffsetDegrees = preferences[LevelPreferences.calibrationPitchOffsetDegrees]
                ?: 0f,
            calibrationRollOffsetDegrees = preferences[LevelPreferences.calibrationRollOffsetDegrees]
                ?: 0f
        )
    }

    suspend fun setDisplayMode(mode: LevelDisplayMode) {
        dataStore.edit { it[LevelPreferences.displayMode] = mode.toPersistedString() }
    }

    suspend fun setLevelToleranceDegrees(value: Float) {
        dataStore.edit {
            it[LevelPreferences.levelToleranceDegrees] = value.coerceIn(0.5f, 5.0f)
        }
    }

    suspend fun setFilterAlpha(value: Float) {
        dataStore.edit {
            it[LevelPreferences.filterAlpha] = value.coerceIn(0.7f, 0.95f)
        }
    }

    suspend fun setCalibrationOffsets(pitchDegrees: Float, rollDegrees: Float) {
        dataStore.edit {
            it[LevelPreferences.calibrationPitchOffsetDegrees] = pitchDegrees
            it[LevelPreferences.calibrationRollOffsetDegrees] = rollDegrees
        }
    }

    suspend fun clearCalibrationOffsets() {
        setCalibrationOffsets(pitchDegrees = 0f, rollDegrees = 0f)
    }
}
