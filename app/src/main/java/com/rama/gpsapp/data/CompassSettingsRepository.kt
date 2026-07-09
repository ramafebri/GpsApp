package com.rama.gpsapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.rama.gpsapp.compass.CompassNorthMode
import com.rama.gpsapp.compass.HeadingCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CompassSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.compassDataStore

    val settings: Flow<CompassSettings> = dataStore.data.map { preferences ->
        CompassSettings(
            northMode = CompassNorthMode.fromPersistedString(
                preferences[CompassPreferences.northMode] ?: CompassNorthMode.MAGNETIC.name
            ),
            filterAlpha = preferences[CompassPreferences.filterAlpha] ?: HeadingCalculator.FILTER_ALPHA,
            declinationDegrees = preferences[CompassPreferences.declinationDegrees] ?: 0f
        )
    }

    suspend fun setNorthMode(mode: CompassNorthMode) {
        dataStore.edit { it[CompassPreferences.northMode] = mode.toPersistedString() }
    }

    suspend fun setFilterAlpha(value: Float) {
        dataStore.edit {
            it[CompassPreferences.filterAlpha] = value.coerceIn(0.7f, 0.95f)
        }
    }

    suspend fun setDeclinationDegrees(value: Float) {
        dataStore.edit {
            it[CompassPreferences.declinationDegrees] = value.coerceIn(-30f, 30f)
        }
    }
}
