package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

/**
 * Premium 3D Glassmorphic Card Container
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    glowColor: Color = CyberPrimaryCyan,
    onClick: (() -> Unit)? = null,
    testTag: String = "glass_card",
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = springAnimationSpec(),
        label = "glassCardScale"
    )

    val borderGradient = Brush.linearGradient(
        colors = listOf(
            SoftBorderHighlight.copy(alpha = 0.50f),
            Color.White.copy(alpha = 0.90f),
            SoftBorder
        )
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF), // pure white top
            Color(0xFDF8FAFC), // soft white frosted middle glass
            Color(0xFFF1F5F9)  // subtle gradient base
        )
    )

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .testTag(testTag),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, borderGradient),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundGradient)
                .padding(1.dp),
            content = content
        )
    }
}

/**
 * Animated Integer Counter for Steps, Calories, Distance
 */
@Composable
fun AnimatedCountUpText(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    formatPattern: String = "%,d"
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "countUpText"
    )

    Text(
        text = formatPattern.format(animatedValue),
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * Futuristic Glassmorphic Button
 */
@Composable
fun FuturisticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryColor: Color = CyberPrimaryCyan,
    secondaryColor: Color = CyberSecondaryViolet,
    enabled: Boolean = true,
    testTag: String = "futuristic_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = springAnimationSpec(),
        label = "futuristicBtnScale"
    )

    val gradient = Brush.horizontalGradient(
        colors = if (enabled) listOf(primaryColor, secondaryColor) else listOf(Color.Gray, Color.DarkGray)
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .heightIn(min = 54.dp)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3D Health Score Indicator Ring
 */
@Composable
fun AnimatedHealthScoreRing(
    score: Int, // 0 to 100
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 12.dp
) {
    val progress = (score.coerceIn(0, 100) / 100f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "healthScore"
    )

    val scoreColor = when {
        score >= 80 -> CyberAccentMint
        score >= 60 -> CyberPrimaryCyan
        score >= 40 -> CyberWarning
        else -> CyberDanger
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()

            // Outer subtle track
            drawCircle(
                color = scoreColor.copy(alpha = 0.15f),
                style = Stroke(width = strokePx)
            )

            // Progress Arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(scoreColor.copy(alpha = 0.4f), scoreColor, CyberSecondaryViolet)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedCountUpText(
                targetValue = score,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = scoreColor
            )
            Text(
                text = "HEALTH INDEX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Confetti Celebration Canvas Overlay
 */
private data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1f
)

@Composable
fun ConfettiOverlay(
    trigger: Boolean,
    onFinished: () -> Unit = {}
) {
    if (!trigger) return

    val particles = remember { mutableStateListOf<Particle>() }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        particles.clear()
        val colors = listOf(CyberPrimaryCyan, CyberSecondaryViolet, CyberAccentMint, CyberPinkGlow, CyberWarning)
        for (i in 0..60) {
            particles.add(
                Particle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * 0.3f,
                    vx = (Random.nextFloat() - 0.5f) * 0.015f,
                    vy = Random.nextFloat() * 0.02f + 0.005f,
                    color = colors.random(),
                    size = Random.nextFloat() * 16f + 8f
                )
            )
        }

        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = LinearEasing)
        )
        onFinished()
    }

    if (animProgress.value < 1f) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("confetti_overlay")) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                p.x += p.vx
                p.y += p.vy
                p.alpha = (1f - animProgress.value).coerceIn(0f, 1f)

                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(p.x * width, p.y * height)
                )
            }
        }
    }
}

private fun springAnimationSpec() = androidx.compose.animation.core.spring<Float>(
    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
)
