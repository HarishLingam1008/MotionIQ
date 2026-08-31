package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.coach.CoachInputData
import com.example.data.coach.OfflineAiCoachEngine
import com.example.data.coach.PersonalizedWorkoutPlan
import com.example.data.coach.PersonalizedWorkoutTip
import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet
import com.example.ui.theme.CyberWarning
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiWorkoutTipsCard(
    liveSteps: Int,
    activityState: String,
    userProfile: UserProfile,
    todayActivity: DailyActivity? = null,
    onStartWorkoutMode: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var workoutPlan by remember { mutableStateOf<PersonalizedWorkoutPlan?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var refreshTrigger by remember { mutableStateOf(0) }

    val safeStepGoal = userProfile.dailyStepGoal.coerceAtLeast(1)
    val effectiveSteps = max(liveSteps, todayActivity?.steps ?: 0)
    val stepPercent = ((effectiveSteps.toDouble() / safeStepGoal) * 100).roundToInt().coerceIn(0, 300)
    val remainingSteps = max(0, safeStepGoal - effectiveSteps)
    val activeMins = todayActivity?.activeMinutes ?: (effectiveSteps / 100)
    val cals = todayActivity?.calories ?: (effectiveSteps * 0.04)
    val dist = todayActivity?.distanceMeters ?: (effectiveSteps * userProfile.strideLengthMeters)

    // Generate workout tips locally based on step progress & profile
    LaunchedEffect(effectiveSteps, activityState, refreshTrigger) {
        isLoading = true
        val input = CoachInputData(
            name = userProfile.name,
            steps = effectiveSteps,
            stepGoal = safeStepGoal,
            distanceMeters = dist,
            caloriesBurned = cals,
            activeMinutes = activeMins,
            waterIntakeMl = todayActivity?.waterIntakeMl ?: 0,
            waterGoalMl = userProfile.dailyWaterGoalMl,
            heightCm = userProfile.heightCm,
            weightKg = userProfile.weightKg,
            bmi = 22.0,
            bmiCategory = "Normal",
            age = userProfile.age,
            gender = userProfile.gender,
            activityLevel = userProfile.activityLevel,
            currentActivity = activityState.ifBlank { "Walking" },
            caloriesConsumed = 0
        )

        workoutPlan = OfflineAiCoachEngine.generateWorkoutTips(input, userProfile)
        isLoading = false
    }

    val plan = workoutPlan ?: return

    val categories = remember(plan.tips) {
        listOf("All") + plan.tips.map { it.category }.distinct()
    }

    val filteredTips = remember(plan.tips, selectedCategory) {
        if (selectedCategory == "All") plan.tips else plan.tips.filter { it.category == selectedCategory }
    }

    // Glow pulse animation for AI card
    val infiniteTransition = rememberInfiniteTransition(label = "AiGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        CyberPrimaryCyan.copy(alpha = glowAlpha),
                        CyberSecondaryViolet.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("ai_workout_tips_card"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & AI Model Badge & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CyberPrimaryCyan.copy(alpha = glowAlpha),
                                        CyberSecondaryViolet.copy(alpha = glowAlpha)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Coach",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI WORKOUT TIPS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (plan.isAiGenerated) "Personalized by MotionIQ AI Coach" else "Smart Rule-Based Athletic Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (plan.isAiGenerated) CyberPrimaryCyan else CyberAccentMint
                        )
                    }
                }

                IconButton(
                    onClick = { refreshTrigger++ },
                    enabled = !isLoading,
                    modifier = Modifier.testTag("regenerate_workout_tips_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = CyberPrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Workout Tips",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Progress Bar with Tier Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${String.format("%,d", effectiveSteps)} / ${String.format("%,d", safeStepGoal)} steps",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Tier Tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (stepPercent >= 100) CyberAccentMint.copy(alpha = 0.2f) else CyberPrimaryCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (stepPercent >= 100) CyberAccentMint else CyberPrimaryCyan
                            )
                        ) {
                            Text(
                                text = "$stepPercent% • ${plan.activityTier}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (stepPercent >= 100) CyberAccentMint else CyberPrimaryCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (stepPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (stepPercent >= 100) CyberAccentMint else CyberPrimaryCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (remainingSteps > 0) "${String.format("%,d", remainingSteps)} steps to reach goal" else "🎉 Daily goal accomplished!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${cals.toInt()} kcal burned",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = CyberWarning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overall AI Advice Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = CyberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = plan.overallAdvice,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chips
            if (categories.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPrimaryCyan.copy(alpha = 0.25f),
                                selectedLabelColor = CyberPrimaryCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == cat,
                                borderColor = if (selectedCategory == cat) CyberPrimaryCyan else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Workout Tips List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredTips.forEachIndexed { index, tip ->
                    WorkoutTipItemCard(
                        tip = tip,
                        onStartWorkout = { mode ->
                            onStartWorkoutMode(mode)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutTipItemCard(
    tip: PersonalizedWorkoutTip,
    onStartWorkout: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val categoryColor = when (tip.category) {
        "Step Boost" -> CyberPrimaryCyan
        "Cardio Tempo" -> CyberPinkGlow
        "Strength & Core" -> CyberSecondaryViolet
        "Active Recovery" -> CyberAccentMint
        "HIIT" -> CyberDanger
        else -> CyberWarning
    }

    val icon: ImageVector = when (tip.targetActivityMode.lowercase()) {
        "running", "jogging" -> Icons.AutoMirrored.Filled.DirectionsRun
        "cycling" -> Icons.Default.DirectionsBike
        "strength" -> Icons.Default.FitnessCenter
        "recovery" -> Icons.Default.SelfImprovement
        else -> Icons.AutoMirrored.Filled.DirectionsWalk
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("workout_tip_item_${tip.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = tip.headline,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tip.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = categoryColor
                            )
                            Text(
                                text = " • ${tip.intensity} Intensity",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short Description / Tip
            Text(
                text = tip.tip,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Badges (Duration, Step Impact, Burn)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricChip(
                    icon = Icons.Default.Timer,
                    label = "${tip.suggestedDurationMinutes} mins",
                    tint = CyberPrimaryCyan
                )
                MetricChip(
                    icon = Icons.Default.Speed,
                    label = tip.stepImpact,
                    tint = CyberAccentMint
                )
                MetricChip(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "~${tip.estimatedBurnKcal} kcal",
                    tint = CyberWarning
                )
            }

            // Expanded Actionable Workout Breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "RECOMMENDED ROUTINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = CyberPrimaryCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tip.recommendedExercise,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Start Workout Action Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberPrimaryCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimaryCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartWorkout(tip.targetActivityMode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Workout",
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start ${tip.targetActivityMode} Session",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = CyberPrimaryCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, tint.copy(alpha = 0.4f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
