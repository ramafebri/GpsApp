package com.rama.gpsapp.ui.compass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompassRoseCanvas(
    headingDegrees: Float,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.outline
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val needleNorthColor = MaterialTheme.colorScheme.error
    val needleSouthColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val pivotColor = MaterialTheme.colorScheme.primary
    val faceColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f * 0.88f

            drawCircle(color = faceColor, radius = radius, center = center)
            drawCircle(
                color = ringColor,
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            drawCardinalTicks(center, radius, tickColor, needleNorthColor)
            drawNeedle(center, radius, headingDegrees, needleNorthColor, needleSouthColor)
            drawCircle(color = pivotColor, radius = 6.dp.toPx(), center = center)
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = center
            )
        }

        Text(
            text = "N",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 12.dp)
        )
        Text(
            text = "E",
            color = labelColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp)
        )
        Text(
            text = "S",
            color = labelColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-12).dp)
        )
        Text(
            text = "W",
            color = labelColor,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 12.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardinalTicks(
    center: Offset,
    radius: Float,
    tickColor: Color,
    northColor: Color
) {
    for (angle in 0 until 360 step 30) {
        val isCardinal = angle % 90 == 0
        val radians = Math.toRadians((angle - 90f).toDouble())
        val outer = Offset(
            x = center.x + cos(radians).toFloat() * radius,
            y = center.y + sin(radians).toFloat() * radius
        )
        val innerScale = if (isCardinal) 0.82f else 0.9f
        val inner = Offset(
            x = center.x + cos(radians).toFloat() * (radius * innerScale),
            y = center.y + sin(radians).toFloat() * (radius * innerScale)
        )
        drawLine(
            color = if (angle == 0) northColor else tickColor,
            start = inner,
            end = outer,
            strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeedle(
    center: Offset,
    radius: Float,
    headingDegrees: Float,
    northColor: Color,
    southColor: Color
) {
    rotate(-headingDegrees, center) {
        val northTip = Offset(center.x, center.y - radius * 0.72f)
        val southTip = Offset(center.x, center.y + radius * 0.42f)
        val halfWidth = radius * 0.07f

        val northPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(center.x - halfWidth, center.y)
            lineTo(northTip.x, northTip.y)
            lineTo(center.x + halfWidth, center.y)
            close()
        }
        drawPath(northPath, color = northColor)

        val southPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(center.x - halfWidth * 0.7f, center.y)
            lineTo(southTip.x, southTip.y)
            lineTo(center.x + halfWidth * 0.7f, center.y)
            close()
        }
        drawPath(southPath, color = southColor)
    }
}
