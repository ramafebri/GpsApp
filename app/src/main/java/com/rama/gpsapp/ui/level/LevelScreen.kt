package com.rama.gpsapp.ui.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rama.gpsapp.level.LevelDisplayMode

@Composable
fun LevelScreen(
    viewModel: LevelViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.startListening()
            try {
                awaitCancellation()
            } finally {
                viewModel.stopListening()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Digital Bubble Level & Clinometer",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Use the accelerometer to check if a surface is level, read pitch and " +
                "roll angles, and calibrate against your current resting orientation.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!state.sensorsAvailable) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This device is missing an accelerometer, so the bubble level " +
                        "and clinometer are unavailable.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Display mode", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LevelDisplayMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = LevelDisplayMode.entries.size
                            ),
                            onClick = { viewModel.setDisplayMode(mode) },
                            selected = state.settings.displayMode == mode,
                            enabled = state.sensorsAvailable,
                            label = { Text(displayModeLabel(mode)) }
                        )
                    }
                }
            }
        }

        LevelDisplayCard(
            viewModel = viewModel,
            displayMode = state.settings.displayMode,
            toleranceDegrees = state.settings.levelToleranceDegrees,
            sensorsAvailable = state.sensorsAvailable
        )

        LevelControlsCard(
            viewModel = viewModel,
            isFrozen = state.isFrozen,
            sensorsAvailable = state.sensorsAvailable
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.titleMedium)
                Text("Level tolerance: ${"%.1f".format(state.settings.levelToleranceDegrees)}°")
                Slider(
                    value = state.settings.levelToleranceDegrees,
                    valueRange = 0.5f..5.0f,
                    onValueChange = viewModel::setTolerance,
                    enabled = state.sensorsAvailable
                )
                Text(
                    text = "Surfaces within this angle of level are marked as LEVEL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Filter smoothing: ${"%.2f".format(state.settings.filterAlpha)}")
                Slider(
                    value = state.settings.filterAlpha,
                    valueRange = 0.7f..0.95f,
                    onValueChange = viewModel::setFilterAlpha,
                    enabled = state.sensorsAvailable
                )
                Text(
                    text = "Higher values smooth sensor jitter; lower values react faster to movement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LevelDisplayCard(
    viewModel: LevelViewModel,
    displayMode: LevelDisplayMode,
    toleranceDegrees: Float,
    sensorsAvailable: Boolean
) {
    val reading by viewModel.reading.collectAsState()
    val currentReading = reading

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Level reading", style = MaterialTheme.typography.titleMedium)

            if (!sensorsAvailable) {
                Text(
                    text = "Accelerometer not available on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (currentReading == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waiting for sensor readings…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                when (displayMode) {
                    LevelDisplayMode.BUBBLE -> {
                        BubbleLevelCanvas(
                            reading = currentReading,
                            modifier = Modifier.size(280.dp)
                        )
                    }
                    LevelDisplayMode.CLINOMETER -> {
                        ClinometerPanel(
                            reading = currentReading,
                            toleranceDegrees = toleranceDegrees,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    LevelDisplayMode.COMBINED -> {
                        BubbleLevelCanvas(
                            reading = currentReading,
                            modifier = Modifier.size(280.dp)
                        )
                        ClinometerPanel(
                            reading = currentReading,
                            toleranceDegrees = toleranceDegrees,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelControlsCard(
    viewModel: LevelViewModel,
    isFrozen: Boolean,
    sensorsAvailable: Boolean
) {
    val reading by viewModel.reading.collectAsState()
    val hasReading = reading != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Controls", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isFrozen) {
                    "Readings are held. Tap Resume to continue live updates."
                } else {
                    "Calibrate sets the current orientation as level. Hold freezes the display."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::calibrate,
                    enabled = sensorsAvailable && hasReading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Calibrate")
                }
                Button(
                    onClick = viewModel::toggleFreeze,
                    enabled = sensorsAvailable && hasReading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isFrozen) "Resume" else "Hold")
                }
                OutlinedButton(
                    onClick = viewModel::resetCalibration,
                    enabled = sensorsAvailable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

private fun displayModeLabel(mode: LevelDisplayMode): String = when (mode) {
    LevelDisplayMode.BUBBLE -> "Bubble"
    LevelDisplayMode.CLINOMETER -> "Clinometer"
    LevelDisplayMode.COMBINED -> "Combined"
}
