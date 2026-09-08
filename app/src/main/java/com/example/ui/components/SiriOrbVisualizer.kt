package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantStatus
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SiriOrbVisualizer(
    status: AssistantStatus,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "siri_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.THINKING -> 2000
                    AssistantStatus.LISTENING -> 4000
                    AssistantStatus.SPEAKING -> 3000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.6f) * breathingScale
            val dynamicRadius = when (status) {
                AssistantStatus.LISTENING -> baseRadius * (1f + (audioLevel * 0.35f))
                AssistantStatus.SPEAKING -> baseRadius * (1f + (sin(pulsePhase) * 0.08f) + 0.05f)
                AssistantStatus.THINKING -> baseRadius * 1.08f
                else -> baseRadius
            }

            // 1. Ambient Outer Halo
            val ambientHaloBrush = Brush.radialGradient(
                colors = listOf(
                    SiriMagenta.copy(alpha = if (status == AssistantStatus.LISTENING) 0.5f else 0.3f),
                    SiriCyan.copy(alpha = if (status == AssistantStatus.SPEAKING) 0.4f else 0.2f),
                    SiriPurple.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = dynamicRadius * 1.55f
            )
            drawCircle(brush = ambientHaloBrush, radius = dynamicRadius * 1.55f, center = center)

            // 2. Swirling Nebula Layer 1 (Cyan & Blue Wave)
            rotate(rotation, pivot = center) {
                val wavePath1 = Path().apply {
                    val points = 8
                    for (i in 0..points) {
                        val angle = (i * 2 * PI / points).toFloat()
                        val r = dynamicRadius + (sin(angle * 3 + pulsePhase) * 16f)
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }

                drawPath(
                    path = wavePath1,
                    brush = Brush.linearGradient(
                        colors = listOf(SiriCyan.copy(alpha = 0.7f), SiriBlue.copy(alpha = 0.5f), SiriPurple.copy(alpha = 0.6f)),
                        start = Offset(center.x - dynamicRadius, center.y - dynamicRadius),
                        end = Offset(center.x + dynamicRadius, center.y + dynamicRadius)
                    )
                )
            }

            // 3. Counter-swirling Nebula Layer 2 (Magenta & Violet)
            rotate(-rotation * 1.3f, pivot = center) {
                val wavePath2 = Path().apply {
                    val points = 7
                    for (i in 0..points) {
                        val angle = (i * 2 * PI / points).toFloat()
                        val r = dynamicRadius * 0.88f + (cos(angle * 4 - pulsePhase) * 14f)
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }

                drawPath(
                    path = wavePath2,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SiriMagenta.copy(alpha = 0.85f),
                            SiriPurple.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius
                    )
                )
            }

            // 4. Luminous Spherical Core
            val coreBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    SiriCyan.copy(alpha = 0.85f),
                    SiriMagenta.copy(alpha = 0.7f),
                    SiriPurple.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = dynamicRadius * 0.65f
            )
            drawCircle(brush = coreBrush, radius = dynamicRadius * 0.65f, center = center)

            // 5. Soundwave Frequency Bars across center during speech
            if (status == AssistantStatus.LISTENING || status == AssistantStatus.SPEAKING) {
                val barCount = 7
                val barWidth = 4.dp.toPx()
                val totalWidth = barCount * barWidth * 2.5f
                val startX = center.x - (totalWidth / 2f)

                for (i in 0 until barCount) {
                    val barX = startX + (i * barWidth * 2.5f)
                    val factor = if (status == AssistantStatus.LISTENING) {
                        (audioLevel * 1.4f * sin((i + 1) * 0.8f + pulsePhase)).coerceIn(0.15f, 1f)
                    } else {
                        (0.6f * sin((i + 1) * 0.9f + pulsePhase * 1.5f).coerceIn(0.2f, 1f))
                    }
                    val barHeight = (dynamicRadius * 0.5f * factor).coerceAtLeast(10f)

                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = Offset(barX, center.y - (barHeight / 2f)),
                        end = Offset(barX, center.y + (barHeight / 2f)),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                // Sleek Apple Siri inner ring stroke
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = dynamicRadius * 0.45f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
