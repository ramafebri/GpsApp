package com.rama.gpsapp.gesture

import com.rama.gpsapp.data.GestureSettings
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class GestureEngine(
    private val sensorHub: SensorHub
) {
    fun detect(settings: GestureSettings): Flow<GestureType> {
        val flows = buildList {
            if (settings.shakeEnabled) {
                val detector = ShakeDetector(threshold = settings.shakeSensitivity)
                add(
                    sensorHub.accelerometer()
                        .map { sample ->
                            if (detector.onSample(sample)) GestureType.SHAKE else null
                        }
                        .filterNotNull()
                )
            }
            if (settings.flipEnabled) {
                val detector = FlipDetector()
                add(
                    sensorHub.accelerometer()
                        .map { sample ->
                            if (detector.onSample(sample)) GestureType.FLIP else null
                        }
                        .filterNotNull()
                )
            }
            if (settings.twistEnabled) {
                val detector = TwistDetector(angularThreshold = settings.twistSensitivity)
                add(
                    sensorHub.gyroscope()
                        .map { sample ->
                            if (detector.onSample(sample)) GestureType.TWIST else null
                        }
                        .filterNotNull()
                )
            }
        }

        if (flows.isEmpty()) {
            return emptyFlow()
        }

        return merge(*flows.toTypedArray())
    }
}
