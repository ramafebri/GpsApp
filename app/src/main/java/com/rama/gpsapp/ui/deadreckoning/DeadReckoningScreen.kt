package com.rama.gpsapp.ui.deadreckoning

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.rama.gpsapp.pdr.PdrPosition
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun DeadReckoningScreen(
    viewModel: DeadReckoningViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Dead Reckoning Navigation",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "When GPS is lost in tunnels or underground parking, steps and the " +
                "gyroscope compass estimate your position on this local map instead.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!state.sensorsAvailable) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This device is missing an accelerometer or rotation vector " +
                        "sensor, so dead reckoning is unavailable.",
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
                Text("Tracking", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (state.isTracking) "Status: Tracking your steps" else "Status: Stopped",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (state.isTracking) viewModel.stopTracking() else viewModel.startTracking()
                        },
                        enabled = state.sensorsAvailable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.isTracking) "Stop" else "Start")
                    }
                    OutlinedButton(
                        onClick = viewModel::reset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Local position map", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                PathCanvas(
                    path = state.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Live estimate", style = MaterialTheme.typography.titleMedium)
                StatRow("Steps", state.position.stepCount.toString())
                StatRow("Heading", "${"%.0f".format(Math.toDegrees(state.position.headingRadians.toDouble()))} deg")
                StatRow("X", "${"%.2f".format(state.position.x)} m")
                StatRow("Y", "${"%.2f".format(state.position.y)} m")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Calibration", style = MaterialTheme.typography.titleMedium)
                Text("Step length: ${"%.2f".format(state.stepLengthMeters)} m")
                Slider(
                    value = state.stepLengthMeters,
                    valueRange = 0.4f..1.1f,
                    onValueChange = viewModel::setStepLengthMeters
                )
                Text(
                    text = "Tune this to your stride so distance estimates stay accurate. " +
                        "New (X, Y) = Old (X, Y) + Step Length x (sin, cos) of heading.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PathCanvas(
    path: List<PdrPosition>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trailColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.secondary
    val currentColor = MaterialTheme.colorScheme.error
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
    ) {
        drawRect(color = backgroundColor)
        drawGrid(gridColor)

        val maxExtent = path.fold(1f) { acc, position ->
            max(acc, max(kotlin.math.abs(position.x), kotlin.math.abs(position.y)))
        }
        // Leave 20% margin around the furthest point from the origin.
        val metersToEdge = maxExtent * 1.2f
        val pixelsPerMeter = (max(size.width, 1f).coerceAtMost(size.height)) / (2f * metersToEdge)
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        fun toOffset(position: PdrPosition): Offset = Offset(
            x = centerX + position.x * pixelsPerMeter,
            y = centerY - position.y * pixelsPerMeter
        )

        if (path.size > 1) {
            for (i in 1 until path.size) {
                drawLine(
                    color = trailColor,
                    start = toOffset(path[i - 1]),
                    end = toOffset(path[i]),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }

        val start = toOffset(path.first())
        drawCircle(color = startColor, radius = 10f, center = start)

        val current = toOffset(path.last())
        drawCircle(color = currentColor, radius = 12f, center = current)

        val heading = path.last().headingRadians
        val headingEnd = Offset(
            x = current.x + sin(heading) * 26f,
            y = current.y - cos(heading) * 26f
        )
        drawLine(
            color = currentColor,
            start = current,
            end = headingEnd,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawGrid(color: Color) {
    val step = 40f
    var x = 0f
    while (x < size.width) {
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}
