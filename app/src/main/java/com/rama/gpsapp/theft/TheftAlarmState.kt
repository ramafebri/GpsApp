package com.rama.gpsapp.theft

import kotlinx.coroutines.flow.MutableStateFlow

object TheftAlarmState {
    val isActive = MutableStateFlow(false)
    val stage = MutableStateFlow(TheftGuardStage.DISARMED)
}
