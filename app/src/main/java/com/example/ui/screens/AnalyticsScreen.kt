package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CompletedActivity
import com.example.data.local.DailyActivity
import com.example.data.local.SavedRoute
import com.example.data.local.UserProfile
import com.example.data.sensor.SensorFusionDiagnostics
import com.example.ui.components.AnimatedCountUpText
import com.example.ui.components.AnimatedHealthScoreRing
import com.example.ui.components.ChartBarData
import com.example.ui.components.CustomBarChart
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet
import com.example.ui.theme.CyberWarning
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
    todayActivity: DailyActivity? = null,
    allActivities: List<DailyActivity>,
    completedActivities: List<CompletedActivity> = emptyList(),
    savedRoutes: List<SavedRoute> = emptyList(),
    userProfile: UserProfile,
    formatDistance: (Double, Boolean) -> String,
    fusionDiagnostics: SensorFusionDiagnostics? = null,
    liveSteps: Int = 0,
    onDeleteCompletedActivity: (CompletedActivity) -> Unit = {}
) {
    var selectedTimeRange by remember { mutableStateOf(AnalyticsTimeRange.SEVEN_DAYS) }
    var selectedMetric by remember { mutableStateOf(AnalyticsMetric.STEPS) }
    var selectedHeatmapDay by remember { mutableStateOf<HeatmapDayData?>(null) }

    val isImperial = userProfile.unitSystem == "Imperial"

    // Real merged daily activities including live steps for today
    val effectiveActivities = remember(allActivities, todayActivity, liveSteps, userProfile) {
        AnalyticsEngine.getEffectiveDailyActivities(allActivities, todayActivity, liveSteps, userProfile)
    }

    val currentTodayActivity = remember(effectiveActivities) {
        val todayStr = AnalyticsEngine.getTodayDateString()
        effectiveActivities.find { it.date == todayStr } ?: todayActivity
    }

    // Section 4: Motion Efficiency calculation
    val motionEfficiency = remember(fusionDiagnostics, completedActivities, savedRoutes, currentTodayActivity?.steps) {
        AnalyticsEngine.calculateMotionEfficiency(
            diagnostics = fusionDiagnostics,
            completedActivities = completedActivities,
            savedRoutes = savedRoutes,
            todaySteps = currentTodayActivity?.steps ?: liveSteps
        )
    }

    // Section 5: Consistency Score calculation
    val consistencyResult = remember(effectiveActivities, userProfile.dailyStepGoal, selectedTimeRange) {
        AnalyticsEngine.calculateConsistency(
            activities = effectiveActivities,
            stepGoal = userProfile.dailyStepGoal,
            timeRange = selectedTimeRange
        )
    }

    // Section 1: Fitness Score calculation
    val fitnessScoreBreakdown = remember(currentTodayActivity, effectiveActivities, userProfile, consistencyResult.score, motionEfficiency.score) {
        AnalyticsEngine.calculateFitnessScore(
            todayActivity = currentTodayActivity,
            allActivities = effectiveActivities,
            userProfile = userProfile,
            consistencyScore = consistencyResult.score,
            motionEfficiencyScore = motionEfficiency.score
        )
    }

    // Section 6: Personal Best records
    val personalBests = remember(effectiveActivities, completedActivities, savedRoutes) {
        AnalyticsEngine.calculatePersonalBests(
            allActivities = effectiveActivities,
            completedActivities = completedActivities,
            savedRoutes = savedRoutes
        )
    }

    // Section 7: Active vs Inactive time
    val activeVsInactive = remember(effectiveActivities, completedActivities, selectedTimeRange) {
        AnalyticsEngine.calculateActiveVsInactive(
            activities = effectiveActivities,
            completedActivities = completedActivities,
            timeRange = selectedTimeRange
        )
    }

    // Section 8: Goal achievement progress items
    val goalAchievements = remember(currentTodayActivity, userProfile, isImperial) {
        AnalyticsEngine.calculateGoalAchievements(
            todayActivity = currentTodayActivity,
            userProfile = userProfile,
            isImperial = isImperial,
            formatDistance = formatDistance
        )
    }

    // Section 9: Weekly Activity Heatmap
    val weeklyHeatmap = remember(effectiveActivities, userProfile.dailyStepGoal) {
        AnalyticsEngine.buildWeeklyHeatmap(
            activities = effectiveActivities,
            stepGoal = userProfile.dailyStepGoal
        )
    }

    // Section 10: Rule-based intelligent AI insights
    val aiInsights = remember(effectiveActivities, currentTodayActivity, userProfile, motionEfficiency, consistencyResult) {
        AnalyticsEngine.generateAiInsights(
            activities = effectiveActivities,
            todayActivity = currentTodayActivity,
            userProfile = userProfile,
            motionEfficiency = motionEfficiency,
            consistency = consistencyResult
        )
    }

    // Section 3: Interactive Bar Chart data
    val chartData = remember(effectiveActivities, selectedTimeRange, selectedMetric, isImperial) {
        val sorted = effectiveActivities.sortedBy { it.date }
        val filtered = when (selectedTimeRange) {
            AnalyticsTimeRange.TODAY -> sorted.takeLast(7) // Shows recent days leading to today
            AnalyticsTimeRange.SEVEN_DAYS -> sorted.takeLast(7)
            AnalyticsTimeRange.THIRTY_DAYS -> sorted.takeLast(30)
        }

        val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outDaySdf = SimpleDateFormat("EEE", Locale.getDefault())
        val outDateSdf = SimpleDateFormat("d MMM", Locale.getDefault())

        filtered.map { activity ->
            val parsedDate = try { inSdf.parse(activity.date) } catch (e: Exception) { null }
            val label = when (selectedTimeRange) {
                AnalyticsTimeRange.THIRTY_DAYS -> parsedDate?.let { outDateSdf.format(it) } ?: activity.date
                else -> parsedDate?.let { outDaySdf.format(it) } ?: activity.date
            }

            val rawVal: Float
            val formattedVal: String

            when (selectedMetric) {
                AnalyticsMetric.STEPS -> {
                    rawVal = activity.steps.toFloat()
                    formattedVal = "%,d steps".format(activity.steps)
                }
                AnalyticsMetric.CALORIES -> {
                    rawVal = activity.calories.toFloat()
                    formattedVal = "%.0f kcal".format(activity.calories)
                }
                AnalyticsMetric.DISTANCE -> {
                    rawVal = if (isImperial) (activity.distanceMeters / 1609.34).toFloat() else (activity.distanceMeters / 1000.0).toFloat()
                    formattedVal = formatDistance(activity.distanceMeters, isImperial)
                }
            }

            ChartBarData(label = label, value = rawVal, formattedValue = formattedVal)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("analytics_screen")
    ) {
        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Analytics & Insights 📊",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Real-time Biomechanics & Historical Performance",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberPrimaryCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: FITNESS SCORE
        FitnessScoreSection(fitnessScoreBreakdown)

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: TIME RANGE FILTER
        TimeRangeFilterSection(
            selectedTimeRange = selectedTimeRange,
            onTimeRangeSelected = { selectedTimeRange = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: WEEKLY ACTIVITY (Bar Chart)
        WeeklyActivitySection(
            selectedMetric = selectedMetric,
            onMetricSelected = { selectedMetric = it },
            timeRange = selectedTimeRange,
            chartData = chartData
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 4: MOTION EFFICIENCY (⚡ XX/100)
        MotionEfficiencySection(motionEfficiency)

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 5: CONSISTENCY SCORE (🎯 XX/100)
        ConsistencyScoreSection(consistencyResult)

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 6: PERSONAL BEST RECORDS (🏆)
        PersonalBestsSection(
            personalBests = personalBests,
            isImperial = isImperial,
            formatDistance = formatDistance
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 7: ACTIVE VS INACTIVE
        ActiveVsInactiveSection(activeVsInactive)

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 8: GOAL ACHIEVEMENT
        GoalAchievementSection(goalAchievements)

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 9: WEEKLY HEATMAP MATRIX
        WeeklyHeatmapSection(
            heatmapData = weeklyHeatmap,
            selectedDay = selectedHeatmapDay,
            isImperial = isImperial,
            formatDistance = formatDistance,
            onDaySelected = { day ->
                selectedHeatmapDay = if (selectedHeatmapDay?.dateStr == day.dateStr) null else day
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 10: RULE-BASED AI INSIGHTS
        AiInsightsSection(insights = aiInsights)

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 11: COMPLETED WORKOUT SESSION LOGS
        ActivitySessionLogSection(
            completedActivities = completedActivities,
            isImperial = isImperial,
            formatDistance = formatDistance,
            onDeleteClick = onDeleteCompletedActivity
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Section 1: Fitness Score
 */
@Composable
private fun FitnessScoreSection(breakdown: FitnessScoreBreakdown) {
    GlassCard(
        glowColor = if (breakdown.hasEnoughData) CyberAccentMint else CyberPrimaryCyan,
        shape = RoundedCornerShape(28.dp),
        testTag = "fitness_score_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedHealthScoreRing(
                    score = breakdown.score,
                    size = 135.dp,
                    strokeWidth = 13.dp
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FITNESS SCORE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (breakdown.rating) {
                            "Excellent" -> CyberAccentMint.copy(alpha = 0.15f)
                            "Good" -> CyberPrimaryCyan.copy(alpha = 0.15f)
                            "Moderate" -> CyberWarning.copy(alpha = 0.15f)
                            else -> CyberSecondaryViolet.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = breakdown.rating.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = when (breakdown.rating) {
                                "Excellent" -> CyberAccentMint
                                "Good" -> CyberPrimaryCyan
                                "Moderate" -> CyberWarning
                                else -> CyberSecondaryViolet
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = breakdown.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (breakdown.hasEnoughData) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CyberPrimaryCyan.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScoreFactorBadge("Steps", "${breakdown.stepFactor}/25", CyberPrimaryCyan, Modifier.weight(1f))
                    ScoreFactorBadge("Active", "${breakdown.activeFactor}/20", CyberSecondaryViolet, Modifier.weight(1f))
                    ScoreFactorBadge("Water", "${breakdown.waterFactor}/15", CyberAccentMint, Modifier.weight(1f))
                    ScoreFactorBadge("Calories", "${breakdown.calorieFactor}/15", CyberWarning, Modifier.weight(1f))
                    ScoreFactorBadge("Consistency", "${breakdown.consistencyFactor}/15", CyberPinkGlow, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScoreFactorBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

/**
 * Section 2: Time Range Filter
 */
@Composable
private fun TimeRangeFilterSection(
    selectedTimeRange: AnalyticsTimeRange,
    onTimeRangeSelected: (AnalyticsTimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_range_filters"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsTimeRange.values().forEach { timeRange ->
            val isSelected = selectedTimeRange == timeRange
            FilterChip(
                selected = isSelected,
                onClick = { onTimeRangeSelected(timeRange) },
                label = {
                    Text(
                        text = timeRange.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberPrimaryCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Section 3: Weekly Activity (Interactive Bar Chart)
 */
@Composable
private fun WeeklyActivitySection(
    selectedMetric: AnalyticsMetric,
    onMetricSelected: (AnalyticsMetric) -> Unit,
    timeRange: AnalyticsTimeRange,
    chartData: List<ChartBarData>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMetric.values().forEach { metric ->
                val isSelected = selectedMetric == metric
                FilterChip(
                    selected = isSelected,
                    onClick = { onMetricSelected(metric) },
                    label = {
                        Text(
                            text = metric.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberSecondaryViolet,
                        selectedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CustomBarChart(
            title = "${selectedMetric.label} Trend (${timeRange.label})",
            data = chartData,
            barHeight = 190
        )
    }
}

/**
 * Section 4: Motion Efficiency
 */
@Composable
private fun MotionEfficiencySection(motionEfficiency: MotionEfficiencyResult) {
    val ratingColor = when (motionEfficiency.rating) {
        "Excellent" -> CyberAccentMint
        "Good" -> CyberPrimaryCyan
        "Moderate" -> CyberWarning
        else -> CyberDanger
    }

    GlassCard(
        glowColor = CyberWarning,
        shape = RoundedCornerShape(26.dp),
        testTag = "motion_efficiency_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Motion Efficiency",
                            tint = CyberWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "MOTION EFFICIENCY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (motionEfficiency.hasData) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ratingColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${motionEfficiency.score}/100 • ${motionEfficiency.rating.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ratingColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!motionEfficiency.hasData) {
                Text(
                    text = motionEfficiency.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = motionEfficiency.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EfficiencyMiniMetric(
                        title = "Stability",
                        value = "${motionEfficiency.stabilityPercent}%",
                        color = CyberAccentMint,
                        modifier = Modifier.weight(1f)
                    )
                    EfficiencyMiniMetric(
                        title = "Cadence",
                        value = "${motionEfficiency.cadenceRhythmPercent}%",
                        color = CyberPrimaryCyan,
                        modifier = Modifier.weight(1f)
                    )
                    EfficiencyMiniMetric(
                        title = "Pacing",
                        value = "${motionEfficiency.paceSmoothnessPercent}%",
                        color = CyberSecondaryViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EfficiencyMiniMetric(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

/**
 * Section 5: Consistency Score
 */
@Composable
private fun ConsistencyScoreSection(consistency: ConsistencyResult) {
    GlassCard(
        glowColor = CyberSecondaryViolet,
        shape = RoundedCornerShape(26.dp),
        testTag = "consistency_score_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberSecondaryViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Consistency Score",
                            tint = CyberSecondaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "CONSISTENCY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (consistency.hasData) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberSecondaryViolet.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${consistency.score}/100",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = CyberSecondaryViolet,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!consistency.hasData) {
                Text(
                    text = consistency.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = consistency.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Active Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text(
                            text = "${consistency.activeDaysCount} of ${consistency.totalDaysCount} days",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberPrimaryCyan,
                            maxLines = 1
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Active Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text(
                            text = "${consistency.streakDays} Days 🔥",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberWarning,
                            maxLines = 1
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = "Goals Hit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text(
                            text = "${consistency.goalsAchievedDays} Days",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberAccentMint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section 6: Personal Best Records
 */
@Composable
private fun PersonalBestsSection(
    personalBests: PersonalBests,
    isImperial: Boolean,
    formatDistance: (Double, Boolean) -> String
) {
    GlassCard(
        glowColor = CyberPinkGlow,
        shape = RoundedCornerShape(26.dp),
        testTag = "personal_bests_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberPinkGlow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Personal Bests",
                        tint = CyberPinkGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "PERSONAL BESTS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!personalBests.hasRecords) {
                Text(
                    text = "No personal best yet. Track your walks, runs, and rides to set historical records!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val durHrs = personalBests.maxDurationSeconds / 3600
                val durMins = (personalBests.maxDurationSeconds % 3600) / 60
                val formattedDuration = when {
                    durHrs > 0 -> "${durHrs}h ${durMins}m"
                    durMins > 0 -> "${durMins}m"
                    else -> "${personalBests.maxDurationSeconds}s"
                }

                val speedUnit = if (isImperial) "mph" else "km/h"
                val convertedSpeed = if (isImperial) personalBests.maxSpeedKmh * 0.621371 else personalBests.maxSpeedKmh

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PersonalBestRow(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = "Highest Steps",
                        value = "%,d steps".format(personalBests.maxSteps),
                        subtext = if (personalBests.maxStepsDate.isNotBlank()) "Set on ${personalBests.maxStepsDate}" else null,
                        color = CyberPrimaryCyan
                    )
                    PersonalBestRow(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        label = "Longest Distance",
                        value = formatDistance(personalBests.maxDistanceMeters, isImperial),
                        color = CyberAccentMint
                    )
                    PersonalBestRow(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Highest Calories Burned",
                        value = "%.0f kcal".format(personalBests.maxCalories),
                        color = CyberWarning
                    )
                    PersonalBestRow(
                        icon = Icons.Default.Timer,
                        label = "Longest Active Session",
                        value = formattedDuration,
                        color = CyberSecondaryViolet
                    )
                    if (personalBests.maxSpeedKmh > 0.1) {
                        PersonalBestRow(
                            icon = Icons.Default.Speed,
                            label = "Peak Speed",
                            value = "%.1f %s".format(convertedSpeed, speedUnit),
                            color = CyberPinkGlow
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalBestRow(
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String? = null,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (subtext != null) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

/**
 * Section 7: Active vs Inactive
 */
@Composable
private fun ActiveVsInactiveSection(data: ActiveVsInactiveData) {
    GlassCard(
        glowColor = CyberAccentMint,
        shape = RoundedCornerShape(26.dp),
        testTag = "active_vs_inactive_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ACTIVE VS INACTIVE TIME",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = data.insightSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (data.hasData) {
                Spacer(modifier = Modifier.height(16.dp))

                // Dual Color Segmented Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(data.activePercentage.coerceAtLeast(0.05f))
                            .fillMaxSize()
                            .background(CyberAccentMint)
                    )
                    Box(
                        modifier = Modifier
                            .weight(data.inactivePercentage.coerceAtLeast(0.05f))
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyberAccentMint)
                        )
                        Column {
                            Text(text = "Active Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${data.formattedActive} (${(data.activePercentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Resting / Inactive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${data.formattedInactive} (${(data.inactivePercentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 8: Goal Achievement
 */
@Composable
private fun GoalAchievementSection(goals: List<GoalProgressItem>) {
    GlassCard(
        glowColor = CyberPrimaryCyan,
        shape = RoundedCornerShape(26.dp),
        testTag = "goal_achievement_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "GOAL ACHIEVEMENT",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                goals.forEach { goal ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${goal.currentFormatted} / ${goal.targetFormatted} (${goal.percentageInt}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = goal.color,
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val animatedProgress by animateFloatAsState(
                            targetValue = goal.progress,
                            animationSpec = tween(1000),
                            label = "goal_progress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = goal.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section 9: Weekly Activity Heatmap
 */
@Composable
private fun WeeklyHeatmapSection(
    heatmapData: List<HeatmapDayData>,
    selectedDay: HeatmapDayData?,
    isImperial: Boolean,
    formatDistance: (Double, Boolean) -> String,
    onDaySelected: (HeatmapDayData) -> Unit
) {
    GlassCard(
        glowColor = CyberAccentMint,
        shape = RoundedCornerShape(26.dp),
        testTag = "weekly_heatmap_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Weekly Heatmap",
                        tint = CyberAccentMint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "WEEKLY ACTIVITY HEATMAP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any day to inspect full telemetry",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7 Columns Grid for MON-SUN with equal weight & responsive sizing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                heatmapData.forEach { day ->
                    val isSelected = selectedDay?.dateStr == day.dateStr
                    val cellColor = when (day.intensityLevel) {
                        4 -> CyberAccentMint
                        3 -> CyberPrimaryCyan
                        2 -> CyberPrimaryCyan.copy(alpha = 0.65f)
                        1 -> CyberPrimaryCyan.copy(alpha = 0.30f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDaySelected(day) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = day.dayOfWeek,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (day.isToday) CyberPrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cellColor)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, CyberSecondaryViolet, RoundedCornerShape(10.dp))
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.intensityLevel >= 4) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Goal Achieved",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expanded day details inspector card
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (selectedDay != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${selectedDay.dayOfWeek}, ${selectedDay.displayDate}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberPrimaryCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(text = "Steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text(text = "%,d".format(selectedDay.steps), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text(text = formatDistance(selectedDay.distanceMeters, isImperial), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Calories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text(text = "%.0f kcal".format(selectedDay.calories), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(text = "Active Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text(text = "${selectedDay.activeMinutes}m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 10: AI Insights
 */
@Composable
private fun AiInsightsSection(insights: List<AnalyticsInsight>) {
    GlassCard(
        glowColor = CyberPrimaryCyan,
        shape = RoundedCornerShape(26.dp),
        testTag = "ai_insights_card"
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberPrimaryCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "AI Insight",
                        tint = CyberPrimaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "AI INSIGHTS 💡",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights.forEach { insight ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = insight.accentColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, insight.accentColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (insight.category) {
                                    "Trend" -> Icons.Default.TrendingUp
                                    "Consistency" -> Icons.Default.TrackChanges
                                    "Hydration" -> Icons.Default.WaterDrop
                                    "Motion Quality" -> Icons.Default.Bolt
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = insight.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = insight.accentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = insight.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 11: Completed Workout Activity Session Log
 */
@Composable
fun ActivityHistoryList(
    completedActivities: List<CompletedActivity>,
    isImperial: Boolean,
    formatDistance: (Double, Boolean) -> String,
    onDeleteClick: (CompletedActivity) -> Unit
) {
    ActivitySessionLogSection(
        completedActivities = completedActivities,
        isImperial = isImperial,
        formatDistance = formatDistance,
        onDeleteClick = onDeleteClick
    )
}

@Composable
fun ActivitySessionLogSection(
    completedActivities: List<CompletedActivity>,
    isImperial: Boolean,
    formatDistance: (Double, Boolean) -> String,
    onDeleteClick: (CompletedActivity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = CyberPrimaryCyan
                )
                Text(
                    text = "Activity Session Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${completedActivities.size} Sessions",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CyberSecondaryViolet
            )
        }

        if (completedActivities.isEmpty()) {
            GlassCard(shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No recorded workout sessions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start tracking in the Motion tab to generate workout history!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            completedActivities.forEach { activity ->
                CompletedActivityHistoryCard(
                    activity = activity,
                    isImperial = isImperial,
                    formatDistance = formatDistance,
                    onDeleteClick = { onDeleteClick(activity) }
                )
            }
        }
    }
}

@Composable
private fun CompletedActivityHistoryCard(
    activity: CompletedActivity,
    isImperial: Boolean,
    formatDistance: (Double, Boolean) -> String,
    onDeleteClick: () -> Unit
) {
    val (icon, color) = when (activity.activityType) {
        "Cycling" -> Pair(Icons.Default.DirectionsBike, CyberSecondaryViolet)
        "Running" -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, CyberPinkGlow)
        "Jogging" -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, CyberWarning)
        else -> Pair(Icons.AutoMirrored.Filled.DirectionsWalk, CyberPrimaryCyan)
    }

    val hrs = activity.durationSeconds / 3600
    val mins = (activity.durationSeconds % 3600) / 60
    val secs = activity.durationSeconds % 60
    val durationFormatted = when {
        hrs > 0 -> "%dh %02dm %02ds".format(hrs, mins, secs)
        mins > 0 -> "%02dm %02ds".format(mins, secs)
        else -> "%02ds".format(secs)
    }

    GlassCard(
        glowColor = color,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = activity.activityType,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = activity.activityType,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activity.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Record",
                        tint = CyberDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = color.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(text = durationFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(
                        text = formatDistance(activity.distanceMeters, isImperial),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CyberPrimaryCyan),
                        maxLines = 1
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Calories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(
                        text = "%.0f kcal".format(activity.calories),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CyberWarning),
                        maxLines = 1
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = "Steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(
                        text = if (activity.steps != null && activity.activityType != "Cycling") "%,d".format(activity.steps) else "N/A",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (activity.activityType == "Cycling") MaterialTheme.colorScheme.onSurfaceVariant else CyberAccentMint
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
