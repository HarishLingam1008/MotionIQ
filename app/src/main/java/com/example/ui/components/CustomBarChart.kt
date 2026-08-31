package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet

data class ChartBarData(
    val label: String,
    val value: Float,
    val formattedValue: String
)

@Composable
fun CustomBarChart(
    title: String,
    data: List<ChartBarData>,
    modifier: Modifier = Modifier,
    barHeight: Int = 180
) {
    val hasNoData = data.isEmpty() || data.all { it.value <= 0f }
    val maxVal = if (!hasNoData) (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(10f) else 10f
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    val primaryCyan = CyberPrimaryCyan
    val secondaryViolet = CyberSecondaryViolet
    val accentMint = CyberAccentMint
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textColorArgb = MaterialTheme.colorScheme.onSurface.toArgb()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        glowColor = CyberSecondaryViolet,
        testTag = "custom_bar_chart"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (hasNoData) {
                Text(
                    text = "No activity recorded yet.",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (selectedIndex in data.indices) {
                val selectedItem = data[selectedIndex]
                Text(
                    text = "${selectedItem.label}: ${selectedItem.formattedValue}",
                    style = MaterialTheme.typography.labelLarge.copy(color = primaryCyan, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "TAP BAR TO INSPECT METRICS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasNoData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(
                            text = "No activity recorded yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val barWidthTotal = size.width / data.size
                                val index = (offset.x / barWidthTotal).toInt().coerceIn(0, data.size - 1)
                                selectedIndex = index
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 30.dp.toPx()
                    val barCount = data.size
                    val totalBarSlot = canvasWidth / barCount
                    val barWidth = (totalBarSlot * 0.55f).coerceAtMost(28.dp.toPx())

                    // Grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = (canvasHeight / gridLines) * i
                        drawLine(
                            color = surfaceVariant,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Bars
                    data.forEachIndexed { index, item ->
                        val barHeightCalculated = (item.value / maxVal) * canvasHeight * animationProgress.value
                        val xCenter = totalBarSlot * index + (totalBarSlot / 2f)
                        val xLeft = xCenter - (barWidth / 2f)
                        val yTop = canvasHeight - barHeightCalculated

                        val isSelected = index == selectedIndex
                        val brush = if (isSelected) {
                            Brush.verticalGradient(colors = listOf(accentMint, primaryCyan))
                        } else {
                            Brush.verticalGradient(colors = listOf(primaryCyan, secondaryViolet))
                        }

                        // Background track
                        drawRoundRect(
                            color = surfaceVariant,
                            topLeft = Offset(xLeft, 0f),
                            size = Size(barWidth, canvasHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        // Active Neon Pillar
                        if (barHeightCalculated > 0) {
                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(xLeft, yTop),
                                size = Size(barWidth, barHeightCalculated),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }

                        // X-axis label
                        drawContext.canvas.nativeCanvas.drawText(
                            item.label,
                            xCenter,
                            canvasHeight + 22.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textColorArgb
                                textSize = 11.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}
}

