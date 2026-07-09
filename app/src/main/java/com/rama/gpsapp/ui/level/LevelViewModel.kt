package com.rama.gpsapp.ui.level

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rama.gpsapp.data.LevelSettings
import com.rama.gpsapp.data.LevelSettingsRepository
import com.rama.gpsapp.level.LevelDisplayMode
import com.rama.gpsapp.level.LevelEngine
import com.rama.gpsapp.level.LevelReading
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LevelUiState(
    val settings: LevelSettings = LevelSettings(),
    val isListening: Boolean = false,
    val isFrozen: Boolean = false,
    val sensorsAvailable: Boolean = true
)

class LevelViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = LevelSettingsRepository(appContext)
    private val sensorHub = SensorHub(appContext)
    private val engine = LevelEngine(sensorHub)

    private val _uiState = MutableStateFlow(
        LevelUiState(sensorsAvailable = engine.hasRequiredSensors())
    )
    val uiState: StateFlow<LevelUiState> = _uiState.asStateFlow()

    private val _reading = MutableStateFlow<LevelReading?>(null)
    val reading: StateFlow<LevelReading?> = _reading.asStateFlow()

    private var listeningJob: Job? = null

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                syncSettingsToEngine(settings)
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun startListening() {
        if (listeningJob != null || !_uiState.value.sensorsAvailable) return
        _uiState.value = _uiState.value.copy(isListening = true)
        listeningJob = viewModelScope.launch {
            engine.observe().collect { sample ->
                _reading.value = sample
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun calibrate() {
        viewModelScope.launch {
            engine.calibrate()
            val (pitchOffset, rollOffset) = engine.getCalibrationOffsets()
            repository.setCalibrationOffsets(pitchOffset, rollOffset)
        }
    }

    fun toggleFreeze() {
        val frozen = !_uiState.value.isFrozen
        engine.setFrozen(frozen)
        _uiState.value = _uiState.value.copy(isFrozen = frozen)
    }

    fun setDisplayMode(mode: LevelDisplayMode) {
        viewModelScope.launch { repository.setDisplayMode(mode) }
    }

    fun setTolerance(degrees: Float) {
        viewModelScope.launch { repository.setLevelToleranceDegrees(degrees) }
    }

    fun setFilterAlpha(alpha: Float) {
        viewModelScope.launch { repository.setFilterAlpha(alpha) }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            engine.resetCalibration()
            repository.clearCalibrationOffsets()
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeningJob?.cancel()
    }

    private fun syncSettingsToEngine(settings: LevelSettings) {
        engine.setTolerance(settings.levelToleranceDegrees)
        engine.setFilterAlpha(settings.filterAlpha)
        engine.setOffsets(
            settings.calibrationPitchOffsetDegrees,
            settings.calibrationRollOffsetDegrees
        )
    }
}
