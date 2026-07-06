package com.rama.gpsapp.ui.gestures

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
import com.rama.gpsapp.data.GestureSettings
import com.rama.gpsapp.data.GestureSettingsRepository
import com.rama.gpsapp.service.GestureShortcutService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GesturePermissionState(
    val phoneStateGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val dndAccessGranted: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false
)

data class GestureSettingsUiState(
    val settings: GestureSettings = GestureSettings(),
    val permissions: GesturePermissionState = GesturePermissionState()
)

class GestureSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = GestureSettingsRepository(appContext)
    private val permissionState = MutableStateFlow(readPermissionState())

    val uiState: StateFlow<GestureSettingsUiState> = combine(
        repository.settings,
        permissionState
    ) { settings, permissions ->
        GestureSettingsUiState(settings = settings, permissions = permissions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = GestureSettingsUiState()
    )

    fun refreshPermissionState() {
        permissionState.value = readPermissionState()
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setServiceEnabled(enabled)
            if (enabled) {
                GestureShortcutService.start(appContext)
            } else {
                GestureShortcutService.stop(appContext)
            }
        }
    }

    fun setShakeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setShakeEnabled(enabled) }
    }

    fun setFlipEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setFlipEnabled(enabled) }
    }

    fun setTwistEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTwistEnabled(enabled) }
    }

    fun setShakeSensitivity(value: Float) {
        viewModelScope.launch { repository.setShakeSensitivity(value) }
    }

    fun setTwistSensitivity(value: Float) {
        viewModelScope.launch { repository.setTwistSensitivity(value) }
    }

    private fun readPermissionState(): GesturePermissionState {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

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

        return GesturePermissionState(
            phoneStateGranted = phoneStateGranted,
            notificationsGranted = notificationsGranted,
            dndAccessGranted = notificationManager.isNotificationPolicyAccessGranted,
            ignoringBatteryOptimizations = ignoringBatteryOptimizations
        )
    }
}
