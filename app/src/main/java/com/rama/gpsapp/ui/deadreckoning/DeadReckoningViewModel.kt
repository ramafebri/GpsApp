package com.rama.gpsapp.ui.deadreckoning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rama.gpsapp.pdr.PdrEngine
import com.rama.gpsapp.pdr.PdrPosition
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeadReckoningUiState(
    val isTracking: Boolean = false,
    val position: PdrPosition = PdrPosition(),
    val path: List<PdrPosition> = listOf(PdrPosition()),
    val stepLengthMeters: Float = PdrEngine.DEFAULT_STEP_LENGTH_METERS,
    val sensorsAvailable: Boolean = true
)

class DeadReckoningViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorHub = SensorHub(application.applicationContext)
    private val engine = PdrEngine(sensorHub)

    private val _uiState = MutableStateFlow(
        DeadReckoningUiState(
            sensorsAvailable = sensorHub.hasAccelerometer() && sensorHub.hasRotationVector()
        )
    )
    val uiState: StateFlow<DeadReckoningUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null

    fun startTracking() {
        if (trackingJob != null || !_uiState.value.sensorsAvailable) return
        _uiState.value = _uiState.value.copy(isTracking = true)
        trackingJob = viewModelScope.launch {
            engine.track().collect { position ->
                _uiState.value = _uiState.value.copy(
                    position = position,
                    path = _uiState.value.path + position
                )
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.value = _uiState.value.copy(isTracking = false)
    }

    fun reset() {
        engine.reset()
        _uiState.value = _uiState.value.copy(
            position = PdrPosition(),
            path = listOf(PdrPosition())
        )
    }

    fun setStepLengthMeters(meters: Float) {
        engine.setStepLengthMeters(meters)
        _uiState.value = _uiState.value.copy(stepLengthMeters = meters)
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}
