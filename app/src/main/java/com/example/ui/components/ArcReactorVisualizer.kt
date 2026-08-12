package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.ArcGold
import com.example.voice.IsraelSpeechState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    speechState: IsraelSpeechState,
    rmsDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

    // Rotation animation for HUD inner rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (speechState == IsraelSpeechState.THINKING) 2000 else 8000,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    // Pulse scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (speechState) {
                    IsraelSpeechState.LISTENING -> 800
                    IsraelSpeechState.SPEAKING -> 400
                    IsraelSpeechState.THINKING -> 300
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreColor = when (speechState) {
        IsraelSpeechState.LISTENING -> ArcGold
        IsraelSpeechState.SPEAKING -> ArcCyanGlow
        IsraelSpeechState.THINKING -> ArcGold
        IsraelSpeechState.ERROR -> Color(0xFFFF5252)
        IsraelSpeechState.IDLE -> ArcCyan
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("arc_reactor_visualizer")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val baseRadius = (size.toPx() / 2) * 0.85f
            val currentRadius = baseRadius * (if (speechState == IsraelSpeechState.LISTENING) 1f + (rmsDb / 40f) else pulseScale)

            // Outer Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.45f),
                        coreColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.25f
                ),
                center = center,
                radius = currentRadius * 1.25f
            )

            // Outer Concentric HUD Ring
            drawCircle(
                color = ArcCyan.copy(alpha = 0.4f),
                center = center,
                radius = currentRadius,
                style = Stroke(width = 3.dp.toPx())
            )

            // Rotating HUD Segment Ring
            rotate(degrees = rotationAngle, pivot = center) {
                val segments = 12
                val arcAngle = 360f / segments
                for (i in 0 until segments) {
                    drawArc(
                        color = if (i % 2 == 0) coreColor else ArcCyan.copy(alpha = 0.6f),
                        startAngle = i * arcAngle,
                        sweepAngle = arcAngle * 0.5f,
                        useCenter = false,
                        topLeft = Offset(center.x - currentRadius * 0.82f, center.y - currentRadius * 0.82f),
                        size = androidx.compose.ui.geometry.Size(currentRadius * 1.64f, currentRadius * 1.64f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Reverse Rotating Inner Ring
            rotate(degrees = -rotationAngle * 1.5f, pivot = center) {
                drawArc(
                    color = coreColor.copy(alpha = 0.8f),
                    startAngle = 0f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 0.62f, center.y - currentRadius * 0.62f),
                    size = androidx.compose.ui.geometry.Size(currentRadius * 1.24f, currentRadius * 1.24f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Triangular Arc Reactor Nodes (Iron Man style)
            rotate(degrees = rotationAngle * 0.5f, pivot = center) {
                val nodes = 3
                for (i in 0 until nodes) {
                    val angleRad = Math.toRadians((i * (360.0 / nodes)).toDouble())
                    val nodeX = center.x + (currentRadius * 0.45f * cos(angleRad)).toFloat()
                    val nodeY = center.y + (currentRadius * 0.45f * sin(angleRad)).toFloat()
                    drawCircle(
                        color = coreColor,
                        center = Offset(nodeX, nodeY),
                        radius = 6.dp.toPx()
                    )
                }
            }

            // Core Glowing Center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreColor,
                        coreColor.copy(alpha = 0.6f)
                    ),
                    center = center,
                    radius = currentRadius * 0.32f
                ),
                center = center,
                radius = currentRadius * 0.32f
            )

            // Dynamic Sound Frequency Rays when Speaking or Listening
            if (speechState == IsraelSpeechState.SPEAKING || speechState == IsraelSpeechState.LISTENING) {
                val rayCount = 18
                val angleStep = 360f / rayCount
                val factor = if (speechState == IsraelSpeechState.LISTENING) (rmsDb / 12f).coerceIn(0.2f, 1.5f) else 1f

                for (i in 0 until rayCount) {
                    val rayAngle = Math.toRadians((i * angleStep + rotationAngle).toDouble())
                    val startLength = currentRadius * 0.88f
                    val rayLength = startLength + (15.dp.toPx() * factor * (sin(i + rotationAngle * 0.1f) + 1.2f))

                    val startX = center.x + (startLength * cos(rayAngle)).toFloat()
                    val startY = center.y + (startLength * sin(rayAngle)).toFloat()
                    val endX = center.x + (rayLength * cos(rayAngle)).toFloat()
                    val endY = center.y + (rayLength * sin(rayAngle)).toFloat()

                    drawLine(
                        color = coreColor.copy(alpha = 0.85f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
