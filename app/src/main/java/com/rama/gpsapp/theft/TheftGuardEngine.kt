package com.rama.gpsapp.theft

import android.os.SystemClock
import com.rama.gpsapp.data.AntiTheftSettings
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class TheftGuardEngine(
    private val sensorHub: SensorHub
) {
    private val lock = Any()
    private var activeDetector: TheftGuardDetector? = null

    private val _stage = MutableStateFlow(TheftGuardStage.DISARMED)
    val stage: Flow<TheftGuardStage> = _stage

    fun detect(settings: AntiTheftSettings): Flow<TheftTrigger> {
        val detector = TheftGuardDetector(
            movementThreshold = settings.movementSensitivity,
            rotationThresholdDegrees = settings.rotationSensitivityDegrees,
            armDelayNanos = settings.armDelaySeconds * 1_000_000_000L
        )

        synchronized(lock) {
            activeDetector = detector
            if (settings.armed) {
                detector.arm(SystemClock.elapsedRealtimeNanos())
            } else {
                detector.disarm()
            }
            _stage.value = detector.stage
        }

        val movementFlow = sensorHub.accelerometer()
            .map { sample ->
                synchronized(lock) {
                    val trigger = detector.onAccelerometerSample(sample)
                    _stage.value = detector.stage
                    trigger
                }
            }
            .filterNotNull()

        val rotationFlow = sensorHub.gyroscope()
            .map { sample ->
                synchronized(lock) {
                    val trigger = detector.onGyroscopeSample(sample)
                    _stage.value = detector.stage
                    trigger
                }
            }
            .filterNotNull()

        return merge(movementFlow, rotationFlow)
    }

    fun arm() {
        synchronized(lock) {
            activeDetector?.arm(SystemClock.elapsedRealtimeNanos())
            _stage.value = activeDetector?.stage ?: TheftGuardStage.DISARMED
        }
    }

    fun disarm() {
        synchronized(lock) {
            activeDetector?.disarm()
            _stage.value = activeDetector?.stage ?: TheftGuardStage.DISARMED
        }
    }
}
