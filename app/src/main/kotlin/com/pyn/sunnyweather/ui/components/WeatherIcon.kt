package com.pyn.sunnyweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.cos
import kotlin.math.sin

// A single geometric icon family, also used in the hourly and daily lists.
@Composable
fun WeatherIcon(condition: String, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    val cloud = MaterialTheme.colorScheme.onSurfaceVariant
    val moon = remember { Path().apply {
        moveTo(49f, 13f); cubicTo(13f, 15f, 13f, 65f, 49f, 68f)
        cubicTo(35f, 50f, 34f, 32f, 49f, 13f); close()
    } }
    val outline = remember { Path().apply {
        moveTo(24f, 71f); cubicTo(4f, 70f, 7f, 44f, 27f, 43f)
        cubicTo(30f, 19f, 65f, 19f, 71f, 43f)
        cubicTo(96f, 39f, 100f, 73f, 77f, 73f); lineTo(24f, 73f); close()
    } }
    val rays = remember { List(8) { i ->
        val angle = i * Math.PI / 4
        Offset(cos(angle).toFloat(), sin(angle).toFloat())
    } }
    val precipitationPositions = remember { listOf(32f, 53f, 74f) }
    val cloudStroke = remember { Stroke(3f, cap = StrokeCap.Round) }
    val clear = condition.startsWith("CLEAR")
    val partly = condition.startsWith("PARTLY")
    val night = condition.contains("NIGHT")
    Canvas(modifier) {
        val unit = size.minDimension / 100f
        scale(unit, unit, pivot = Offset.Zero) {
            if (clear || partly) {
                if (night) {
                    drawPath(moon, ink)
                } else {
                    val center = if (partly) Offset(36f, 36f) else Offset(50f, 50f)
                    val radius = if (partly) 17f else 23f
                    for (v in rays) {
                        drawLine(Color(0xFFE6A120), center + v * (radius + 8), center + v * (radius + 15), 3f, StrokeCap.Round)
                    }
                    drawCircle(Color(0xFFF5BA43), radius, center)
                }
            }
            if (!clear) {
                drawPath(outline, cloud.copy(alpha = .13f))
                drawPath(outline, cloud, style = cloudStroke)
                when {
                    condition.contains("RAIN") || condition.contains("SHOWER") || condition == "SLEET" -> {
                        for (x in precipitationPositions) drawLine(ink, Offset(x, 81f), Offset(x - 4, 91f), 3f, StrokeCap.Round)
                    }
                    condition.contains("SNOW") || condition == "HAIL" -> {
                        for (x in precipitationPositions) drawCircle(ink, 2.5f, Offset(x, 86f))
                    }
                    condition.contains("HAZE") || condition == "FOG" || condition == "DUST" -> {
                        drawLine(cloud, Offset(25f, 84f), Offset(77f, 84f), 3f, StrokeCap.Round)
                        drawLine(cloud, Offset(34f, 92f), Offset(69f, 92f), 3f, StrokeCap.Round)
                    }
                }
            }
        }
    }
}
