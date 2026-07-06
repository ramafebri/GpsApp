package com.rama.gpsapp.pdr

import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Fuses accelerometer-based step detection with rotation-vector heading to
 * produce a live Pedestrian Dead Reckoning position estimate, for use where
 * GPS is unavailable (tunnels, underground parking, etc.).
 */
class PdrEngine(
    private val sensorHub: SensorHub
) {
    private val lock = Any()
    private val stepDetector = StepDetector()
    private val tracker = PdrTracker()

    private var latestHeadingRadians = 0f
    private var stepLengthMeters = DEFAULT_STEP_LENGTH_METERS

    fun setStepLengthMeters(meters: Float) {
        synchronized(lock) { stepLengthMeters = meters }
    }

    fun reset() {
        synchronized(lock) {
            stepDetector.reset()
            tracker.reset()
        }
    }

    /**
     * Starts collecting sensors and emits a new [PdrPosition] every time a
     * step is detected. Cancelling collection of the returned flow stops all
     * sensor listeners.
     */
    fun track(): Flow<PdrPosition> = channelFlow {
        launch {
            sensorHub.rotationVector().collect { sample ->
                synchronized(lock) { latestHeadingRadians = sample.x }
            }
        }

        sensorHub.accelerometer().collect { sample ->
            val position = synchronized(lock) {
                if (stepDetector.onSample(sample)) {
                    tracker.onStep(latestHeadingRadians, stepLengthMeters)
                } else {
                    null
                }
            }
            if (position != null) {
                send(position)
            }
        }
    }

    companion object {
        const val DEFAULT_STEP_LENGTH_METERS = 0.75f
    }
}
