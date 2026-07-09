package com.rama.gpsapp.ui.compass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rama.gpsapp.compass.CompassNorthMode
import com.rama.gpsapp.data.CompassSettings
import kotlinx.coroutines.awaitCancellation

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
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
            text = "Digital Compass",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Uses the rotation vector sensor to show live heading in degrees and " +
                "cardinal direction. Sensors run only while this tab is visible.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!state.sensorsAvailable) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This device is missing a rotation vector sensor, so the compass " +
                        "is unavailable.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        CompassCalibrationBanner(viewModel = viewModel)

        CompassDisplayCard(
            viewModel = viewModel,
            northMode = state.settings.northMode,
            sensorsAvailable = state.sensorsAvailable
        )

        CompassSettingsCard(
            viewModel = viewModel,
            settings = state.settings,
            sensorsAvailable = state.sensorsAvailable
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CompassCalibrationBanner(viewModel: CompassViewModel) {
    val reading by viewModel.reading.collectAsState()

    if (reading?.isCalibrated == false) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Magnetometer accuracy is low. Move the phone in a figure-eight " +
                    "pattern to improve calibration.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun CompassDisplayCard(
    viewModel: CompassViewModel,
    northMode: CompassNorthMode,
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
            Text("Heading", style = MaterialTheme.typography.titleMedium)

            if (!sensorsAvailable) {
                Text(
                    text = "Rotation vector sensor not available on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (currentReading == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waiting for sensor readings…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                CompassRoseCanvas(
                    headingDegrees = currentReading.headingDegrees,
                    modifier = Modifier.size(280.dp)
                )
                Text(
                    text = "${currentReading.headingDegrees.toInt()}°",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentReading.cardinalLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = northModeLabel(northMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompassSettingsCard(
    viewModel: CompassViewModel,
    settings: CompassSettings,
    sensorsAvailable: Boolean
) {
    var filterAlpha by remember(settings.filterAlpha) {
        mutableFloatStateOf(settings.filterAlpha)
    }
    var declinationDegrees by remember(settings.declinationDegrees) {
        mutableFloatStateOf(settings.declinationDegrees)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            Text("North reference", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CompassNorthMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CompassNorthMode.entries.size
                        ),
                        onClick = { viewModel.setNorthMode(mode) },
                        selected = settings.northMode == mode,
                        enabled = sensorsAvailable,
                        label = { Text(northModeShortLabel(mode)) }
                    )
                }
            }
            Text(
                text = if (settings.northMode == CompassNorthMode.TRUE_NORTH) {
                    "True north applies your declination offset to magnetic heading. " +
                        "Without GPS, set declination manually for your region."
                } else {
                    "Magnetic north matches the onboard magnetometer reference."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (settings.northMode == CompassNorthMode.TRUE_NORTH) {
                Text(
                    text = "Declination: ${"%.1f".format(declinationDegrees)}°"
                )
                Slider(
                    value = declinationDegrees,
                    valueRange = -30f..30f,
                    onValueChange = { value ->
                        declinationDegrees = value
                        viewModel.previewDeclinationDegrees(value)
                    },
                    onValueChangeFinished = {
                        viewModel.setDeclinationDegrees(declinationDegrees)
                    },
                    enabled = sensorsAvailable
                )
            }

            Text("Filter smoothing: ${"%.2f".format(filterAlpha)}")
            Slider(
                value = filterAlpha,
                valueRange = 0.7f..0.95f,
                onValueChange = { value ->
                    filterAlpha = value
                    viewModel.previewFilterAlpha(value)
                },
                onValueChangeFinished = {
                    viewModel.setFilterAlpha(filterAlpha)
                },
                enabled = sensorsAvailable
            )
            Text(
                text = "Higher values smooth sensor jitter; lower values react faster to rotation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun northModeShortLabel(mode: CompassNorthMode): String = when (mode) {
    CompassNorthMode.MAGNETIC -> "Magnetic"
    CompassNorthMode.TRUE_NORTH -> "True North"
}

private fun northModeLabel(mode: CompassNorthMode): String = when (mode) {
    CompassNorthMode.MAGNETIC -> "Magnetic north"
    CompassNorthMode.TRUE_NORTH -> "True north (declination applied)"
}
