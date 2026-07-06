package com.rama.gpsapp.ui.theft

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import com.rama.gpsapp.theft.TheftGuardStage

@Composable
fun AntiTheftSettingsScreen(
    viewModel: AntiTheftViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
            text = "Anti-Theft Alarm",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Arm your phone on a desk. If it is moved or rotated, a loud alarm starts.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Armed", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.settings.armed,
                        onCheckedChange = viewModel::setArmed
                    )
                }
                Text(
                    text = "Status: ${statusText(state.stage, state.alarmActive)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Sensitivity", style = MaterialTheme.typography.titleMedium)
                Text("Movement threshold: ${"%.2f".format(state.settings.movementSensitivity)}")
                Slider(
                    value = state.settings.movementSensitivity,
                    valueRange = 0.8f..4.5f,
                    onValueChange = viewModel::setMovementSensitivity
                )

                Text("Rotation threshold: ${"%.0f".format(state.settings.rotationSensitivityDegrees)} deg")
                Slider(
                    value = state.settings.rotationSensitivityDegrees,
                    valueRange = 12f..120f,
                    onValueChange = viewModel::setRotationSensitivityDegrees
                )

                val armDelay = state.settings.armDelaySeconds.toFloat()
                Text("Arm delay: ${state.settings.armDelaySeconds}s")
                Slider(
                    value = armDelay,
                    valueRange = 3f..30f,
                    steps = 26,
                    onValueChange = { viewModel.setArmDelaySeconds(it.toInt()) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vibrate with alarm", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.settings.vibrateEnabled,
                        onCheckedChange = viewModel::setVibrateEnabled
                    )
                }

                Button(onClick = viewModel::testAlarm) {
                    Text("Test alarm (4s)")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        label = "Notifications (status + alarm)",
                        granted = state.permissions.notificationsGranted,
                        onRequest = { postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
                PermissionRow(
                    label = "Battery optimization exemption",
                    granted = state.permissions.ignoringBatteryOptimizations,
                    onRequest = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    PermissionRow(
                        label = "Full-screen intent access",
                        granted = state.permissions.fullScreenIntentAllowed,
                        onRequest = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

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

private fun statusText(stage: TheftGuardStage, alarmActive: Boolean): String {
    if (alarmActive) return "Alarm triggered"
    return when (stage) {
        TheftGuardStage.DISARMED -> "Disarmed"
        TheftGuardStage.ARMING -> "Arming..."
        TheftGuardStage.CALIBRATING -> "Calibrating..."
        TheftGuardStage.MONITORING -> "Monitoring"
        TheftGuardStage.TRIGGERED -> "Alarm triggered"
    }
}
