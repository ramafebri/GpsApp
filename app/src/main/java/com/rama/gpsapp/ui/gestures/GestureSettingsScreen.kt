package com.rama.gpsapp.ui.gestures

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun GestureSettingsScreen(
    viewModel: GestureSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val readPhoneStateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionState()
    }
    val postNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Gesture-Based Shortcuts",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Trigger phone actions with physical gestures.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enable Background Detection", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Foreground service")
                    Switch(
                        checked = state.settings.serviceEnabled,
                        onCheckedChange = viewModel::setServiceEnabled
                    )
                }
                Text(
                    text = "A persistent notification appears while detection runs.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                PermissionRow(
                    label = "Phone state (for flip-to-mute calls)",
                    granted = state.permissions.phoneStateGranted,
                    onRequest = { readPhoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        label = "Notifications (for foreground service notice)",
                        granted = state.permissions.notificationsGranted,
                        onRequest = { postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
                PermissionRow(
                    label = "Do Not Disturb access (to mute ring stream)",
                    granted = state.permissions.dndAccessGranted,
                    onRequest = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                )
                PermissionRow(
                    label = "Battery optimization exemption",
                    granted = state.permissions.ignoringBatteryOptimizations,
                    onRequest = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                )
            }
        }

        GestureToggleCard(
            title = "Shake to toggle flashlight",
            enabled = state.settings.shakeEnabled,
            onEnabledChange = viewModel::setShakeEnabled,
            showSlider = state.settings.shakeEnabled,
            sliderValue = state.settings.shakeSensitivity,
            sliderRange = 8f..45f,
            onSliderValueChange = viewModel::setShakeSensitivity
        )

        GestureToggleCard(
            title = "Flip face down to mute incoming call",
            enabled = state.settings.flipEnabled,
            onEnabledChange = viewModel::setFlipEnabled
        )

        GestureToggleCard(
            title = "Twist wrist twice to open camera",
            enabled = state.settings.twistEnabled,
            onEnabledChange = viewModel::setTwistEnabled,
            showSlider = state.settings.twistEnabled,
            sliderValue = state.settings.twistSensitivity,
            sliderRange = 1.2f..7f,
            onSliderValueChange = viewModel::setTwistSensitivity
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: ${if (granted) "Granted" else "Missing"}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        if (!granted) {
            Button(onClick = onRequest) {
                Text("Grant")
            }
        }
    }
}

@Composable
private fun GestureToggleCard(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    showSlider: Boolean = false,
    sliderValue: Float = 0f,
    sliderRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onSliderValueChange: (Float) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            if (showSlider) {
                Text("Sensitivity: ${"%.1f".format(sliderValue)}")
                Slider(
                    value = sliderValue,
                    valueRange = sliderRange,
                    onValueChange = onSliderValueChange
                )
            }
        }
    }
}
