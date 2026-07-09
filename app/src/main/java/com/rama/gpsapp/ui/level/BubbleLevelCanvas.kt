package com.rama.gpsapp.ui.level

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.rama.gpsapp.level.LevelReading
import kotlin.math.min

@Composable
fun BubbleLevelCanvas(
    reading: LevelReading,
    modifier: Modifier = Modifier
) {
    BubbleLevelCanvas(
        bubbleOffsetX = reading.bubbleOffsetX,
        bubbleOffsetY = reading.bubbleOffsetY,
        isLevel = reading.isLevel,
        modifier = modifier
    )
}

@Composable
fun BubbleLevelCanvas(
    bubbleOffsetX: Float,
    bubbleOffsetY: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val crosshairColor = MaterialTheme.colorScheme.outlineVariant
    val vialBorderColor = if (isLevel) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val targetRingColor = if (isLevel) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val bubbleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val bubbleHighlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Canvas(modifier = modifier) {
        drawRect(color = backgroundColor)

        val center = Offset(size.width / 2f, size.height / 2f)
        val vialRadius = min(size.width, size.height) / 2f * 0.88f
        val bubbleRadius = vialRadius * 0.14f
        val maxBubbleTravel = vialRadius - bubbleRadius - 8f
        val clampedOffsetX = bubbleOffsetX.coerceIn(-1f, 1f)
        val clampedOffsetY = bubbleOffsetY.coerceIn(-1f, 1f)
        val bubbleCenter = Offset(
            x = center.x + clampedOffsetX * maxBubbleTravel,
            y = center.y + clampedOffsetY * maxBubbleTravel
        )

        drawCircle(
            color = backgroundColor,
            radius = vialRadius,
            center = center
        )
        drawCircle(
            color = vialBorderColor,
            radius = vialRadius,
            center = center,
            style = Stroke(width = if (isLevel) 4.dp.toPx() else 2.dp.toPx())
        )

        val crosshairExtent = vialRadius * 0.92f
        drawLine(
            color = crosshairColor,
            start = Offset(center.x - crosshairExtent, center.y),
            end = Offset(center.x + crosshairExtent, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = crosshairColor,
            start = Offset(center.x, center.y - crosshairExtent),
            end = Offset(center.x, center.y + crosshairExtent),
            strokeWidth = 1.5f
        )

        val targetRingRadius = vialRadius * 0.12f
        drawCircle(
            color = targetRingColor,
            radius = targetRingRadius,
            center = center,
            style = Stroke(width = if (isLevel) 3.dp.toPx() else 2.dp.toPx())
        )

        drawCircle(
            color = bubbleColor,
            radius = bubbleRadius,
            center = bubbleCenter
        )
        drawCircle(
            color = bubbleHighlightColor,
            radius = bubbleRadius * 0.35f,
            center = Offset(
                x = bubbleCenter.x - bubbleRadius * 0.25f,
                y = bubbleCenter.y - bubbleRadius * 0.25f
            )
        )
    }
}
