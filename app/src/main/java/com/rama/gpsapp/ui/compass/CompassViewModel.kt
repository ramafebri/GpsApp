package com.rama.gpsapp.ui.compass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rama.gpsapp.compass.CompassEngine
import com.rama.gpsapp.compass.CompassNorthMode
import com.rama.gpsapp.compass.CompassReading
import com.rama.gpsapp.data.CompassSettings
import com.rama.gpsapp.data.CompassSettingsRepository
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompassUiState(
    val settings: CompassSettings = CompassSettings(),
    val isListening: Boolean = false,
    val sensorsAvailable: Boolean = true
)

class CompassViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = CompassSettingsRepository(appContext)
    private val sensorHub = SensorHub(appContext)
    private val engine = CompassEngine(sensorHub)

    private val _uiState = MutableStateFlow(
        CompassUiState(sensorsAvailable = engine.hasRequiredSensors())
    )
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private val _reading = MutableStateFlow<CompassReading?>(null)
    val reading: StateFlow<CompassReading?> = _reading.asStateFlow()

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
        _reading.value = null
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun setNorthMode(mode: CompassNorthMode) {
        viewModelScope.launch { repository.setNorthMode(mode) }
    }

    fun previewFilterAlpha(alpha: Float) {
        engine.setFilterAlpha(alpha)
        engine.currentReading()?.let { _reading.value = it }
    }

    fun previewDeclinationDegrees(degrees: Float) {
        engine.setDeclinationDegrees(degrees)
        engine.currentReading()?.let { _reading.value = it }
    }

    fun setFilterAlpha(alpha: Float) {
        viewModelScope.launch { repository.setFilterAlpha(alpha) }
    }

    fun setDeclinationDegrees(degrees: Float) {
        viewModelScope.launch { repository.setDeclinationDegrees(degrees) }
    }

    override fun onCleared() {
        super.onCleared()
        listeningJob?.cancel()
    }

    private fun syncSettingsToEngine(settings: CompassSettings) {
        engine.setFilterAlpha(settings.filterAlpha)
        engine.setDeclinationDegrees(settings.declinationDegrees)
        engine.setUseTrueNorth(settings.northMode == CompassNorthMode.TRUE_NORTH)
        engine.currentReading()?.let { _reading.value = it }
    }
}
