package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Circular Progress Indicator for Daily Step Goal.
 * Animates smoothly from 0 to the current percentage completed when loaded.
 */
@Composable
fun ProgressRing(
    progress: Float, // 0.0 to 1.0+
    currentSteps: Int,
    targetSteps: Int,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    strokeWidth: Dp = 18.dp
) {
    // Animates from 0f to target progress when the screen loads
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1.5f),
            animationSpec = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Subtle glow pulse when goal is completed
    val infiniteTransition = rememberInfiniteTransition(label = "goalGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val primaryCyan = CyberPrimaryCyan
    val secondaryViolet = CyberSecondaryViolet
    val accentMint = CyberAccentMint

    val isGoalAchieved = currentSteps >= targetSteps && targetSteps > 0
    val animatedPercent = (animatedProgress.value * 100).toInt().coerceAtLeast(0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("progress_ring")
            .testTag("step_circular_progress_indicator")
    ) {
        // Outer Ambient Glass Halo Circle
        Box(
            modifier = Modifier
                .size(size - 6.dp)
                .clip(CircleShape)
                .background(
                    if (isGoalAchieved) {
                        accentMint.copy(alpha = 0.06f * glowPulse)
                    } else {
                        primaryCyan.copy(alpha = 0.04f)
                    }
                )
        )

        // Canvas for Circular Progress Track & Dynamic Gradient Arc
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // 1. Full 360-degree Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 2. Animated Circular Progress Arc (from top / -90 degrees)
            val currentAnimatedValue = animatedProgress.value
            if (currentAnimatedValue > 0.001f) {
                val sweepAngle = (360f * currentAnimatedValue.coerceIn(0f, 1f)).coerceAtMost(360f)

                // Multi-Stop Fitness Gradient Brush
                val progressBrush = Brush.sweepGradient(
                    colors = listOf(
                        primaryCyan,
                        secondaryViolet,
                        accentMint,
                        if (isGoalAchieved) accentMint else primaryCyan
                    ),
                    center = Offset(this.size.width / 2f, this.size.height / 2f)
                )

                drawArc(
                    brush = progressBrush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // 3. Glowing Leading Edge Indicator Dot
                if (sweepAngle in 5f..355f) {
                    val angleRad = Math.toRadians(((-90f + sweepAngle)).toDouble())
                    val radius = (this.size.width - strokePx) / 2f
                    val dotCenterX = (this.size.width / 2f) + (radius * cos(angleRad)).toFloat()
                    val dotCenterY = (this.size.height / 2f) + (radius * sin(angleRad)).toFloat()

                    // Glow aura
                    drawCircle(
                        color = (if (isGoalAchieved) accentMint else primaryCyan).copy(alpha = 0.45f),
                        radius = strokePx * 0.75f,
                        center = Offset(dotCenterX, dotCenterY)
                    )

                    // Core bright dot
                    drawCircle(
                        color = Color.White,
                        radius = strokePx * 0.35f,
                        center = Offset(dotCenterX, dotCenterY)
                    )
                }
            }
        }

        // Inner Content Hierarchy
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Label with Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = if (isGoalAchieved) accentMint else primaryCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DAILY STEPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = if (isGoalAchieved) accentMint else primaryCyan
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Animated Step Counter
            AnimatedCountUpText(
                targetValue = currentSteps,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Target Goal Label
            Text(
                text = "GOAL: %,d".format(targetSteps),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Percentage Badge (animates from 0% to current percentage)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGoalAchieved) {
                    accentMint.copy(alpha = 0.16f)
                } else {
                    secondaryViolet.copy(alpha = 0.12f)
                },
                modifier = Modifier.testTag("step_percentage_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGoalAchieved) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Goal Met",
                            tint = accentMint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "$animatedPercent% COMPLETED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = if (isGoalAchieved) accentMint else secondaryViolet,
                        modifier = Modifier.testTag("step_percentage_text")
                    )
                }
            }
        }
    }
}


