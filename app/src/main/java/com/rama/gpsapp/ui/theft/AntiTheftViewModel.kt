package com.rama.gpsapp.ui.theft

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rama.gpsapp.data.AntiTheftSettings
import com.rama.gpsapp.data.AntiTheftSettingsRepository
import com.rama.gpsapp.theft.AlarmSoundPlayer
import com.rama.gpsapp.theft.TheftAlarmService
import com.rama.gpsapp.theft.TheftAlarmState
import com.rama.gpsapp.theft.TheftGuardStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AntiTheftPermissionState(
    val notificationsGranted: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
    val fullScreenIntentAllowed: Boolean = true
)

data class AntiTheftUiState(
    val settings: AntiTheftSettings = AntiTheftSettings(),
    val permissions: AntiTheftPermissionState = AntiTheftPermissionState(),
    val stage: TheftGuardStage = TheftGuardStage.DISARMED,
    val alarmActive: Boolean = false
)

class AntiTheftViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = AntiTheftSettingsRepository(appContext)
    private val permissionState = MutableStateFlow(readPermissionState())
    private val alarmSoundPlayer = AlarmSoundPlayer(appContext)

    val uiState: StateFlow<AntiTheftUiState> = combine(
        repository.settings,
        permissionState,
        TheftAlarmState.stage,
        TheftAlarmState.isActive
    ) { settings, permissions, stage, alarmActive ->
        AntiTheftUiState(
            settings = settings,
            permissions = permissions,
            stage = stage,
            alarmActive = alarmActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AntiTheftUiState()
    )

    fun refreshPermissionState() {
        permissionState.value = readPermissionState()
    }

    fun setArmed(value: Boolean) {
        viewModelScope.launch {
            repository.setArmed(value)
            if (value) {
                TheftAlarmService.start(appContext)
            } else {
                TheftAlarmService.stop(appContext)
            }
        }
    }

    fun setMovementSensitivity(value: Float) {
        viewModelScope.launch { repository.setMovementSensitivity(value) }
    }

    fun setRotationSensitivityDegrees(value: Float) {
        viewModelScope.launch { repository.setRotationSensitivityDegrees(value) }
    }

    fun setArmDelaySeconds(value: Int) {
        viewModelScope.launch { repository.setArmDelaySeconds(value) }
    }

    fun setVibrateEnabled(value: Boolean) {
        viewModelScope.launch { repository.setVibrateEnabled(value) }
    }

    fun testAlarm() {
        viewModelScope.launch {
            val settings = uiState.value.settings
            val started = alarmSoundPlayer.start(vibrateEnabled = settings.vibrateEnabled)
            if (!started) return@launch
            delay(4_000L)
            alarmSoundPlayer.stop()
        }
    }

    private fun readPermissionState(): AntiTheftPermissionState {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val ignoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
        } else {
            true
        }

        val fullScreenIntentAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }

        return AntiTheftPermissionState(
            notificationsGranted = notificationsGranted,
            ignoringBatteryOptimizations = ignoringBatteryOptimizations,
            fullScreenIntentAllowed = fullScreenIntentAllowed
        )
    }
}
