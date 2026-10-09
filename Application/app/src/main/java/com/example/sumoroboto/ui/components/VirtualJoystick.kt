package com.example.sumoroboto.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    onMove: (x: Float, y: Float) -> Unit,
    onRelease: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val knobOffsetX = remember { Animatable(0f) }
    val knobOffsetY = remember { Animatable(0f) }

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                            val maxRadius = (size.toPx() / 2f) - 35.dp.toPx()
                            val delta = offset - center
                            val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
                            val angle = atan2(delta.y, delta.x)

                            val clampedDist = dist.coerceAtMost(maxRadius)
                            val targetX = clampedDist * cos(angle)
                            val targetY = clampedDist * sin(angle)

                            coroutineScope.launch {
                                knobOffsetX.snapTo(targetX)
                                knobOffsetY.snapTo(targetY)
                            }

                            // Normalized: x from -1 to 1, y from -1 to 1 (up is positive forward)
                            val normX = (targetX / maxRadius).coerceIn(-1f, 1f)
                            val normY = (-targetY / maxRadius).coerceIn(-1f, 1f)
                            onMove(normX, normY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val maxRadius = (size.toPx() / 2f) - 35.dp.toPx()
                            val currentX = knobOffsetX.value + dragAmount.x
                            val currentY = knobOffsetY.value + dragAmount.y

                            val dist = sqrt(currentX * currentX + currentY * currentY)
                            val angle = atan2(currentY, currentX)

                            val (finalX, finalY) = if (dist > maxRadius) {
                                Pair(maxRadius * cos(angle), maxRadius * sin(angle))
                            } else {
                                Pair(currentX, currentY)
                            }

                            coroutineScope.launch {
                                knobOffsetX.snapTo(finalX)
                                knobOffsetY.snapTo(finalY)
                            }

                            // Normalized: up is +1, down is -1
                            val normX = (finalX / maxRadius).coerceIn(-1f, 1f)
                            val normY = (-finalY / maxRadius).coerceIn(-1f, 1f)
                            onMove(normX, normY)
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                launch {
                                    knobOffsetX.animateTo(
                                        0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                                launch {
                                    knobOffsetY.animateTo(
                                        0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                            onRelease()
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                knobOffsetX.animateTo(0f)
                                knobOffsetY.animateTo(0f)
                            }
                            onRelease()
                        }
                    )
                }
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = (this.size.width / 2f) - 10.dp.toPx()
            val maxRadius = (this.size.width / 2f) - 35.dp.toPx()
            val knobRadius = 32.dp.toPx()

            // 1. Base outer background circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.85f),
                        baseColor.copy(alpha = 0.40f)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer border ring
            drawCircle(
                color = outlineColor.copy(alpha = 0.7f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner boundary guide ring
            drawCircle(
                color = outlineColor.copy(alpha = 0.35f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Crosshair guidelines
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(center.x, center.y - outerRadius * 0.9f),
                end = Offset(center.x, center.y + outerRadius * 0.9f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(center.x - outerRadius * 0.9f, center.y),
                end = Offset(center.x + outerRadius * 0.9f, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Center deadzone circle
            drawCircle(
                color = outlineColor.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Movable Thumb Knob
            val knobCenter = Offset(center.x + knobOffsetX.value, center.y + knobOffsetY.value)

            // Knob subtle shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = knobRadius + 2.dp.toPx(),
                center = Offset(knobCenter.x, knobCenter.y + 3.dp.toPx())
            )

            // Knob core gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.8f)
                    ),
                    center = knobCenter,
                    radius = knobRadius
                ),
                radius = knobRadius,
                center = knobCenter
            )

            // Knob highlight rim
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = knobRadius - 2.dp.toPx(),
                center = knobCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Knob inner dot
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 5.dp.toPx(),
                center = knobCenter
            )
        }
    }
}
