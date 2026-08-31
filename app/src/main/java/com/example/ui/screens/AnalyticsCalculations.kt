package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import com.example.data.local.CompletedActivity
import com.example.data.local.DailyActivity
import com.example.data.local.SavedRoute
import com.example.data.local.UserProfile
import com.example.data.sensor.SensorFusionDiagnostics
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet
import com.example.ui.theme.CyberWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class AnalyticsTimeRange(val label: String) {
    TODAY("Today"),
    SEVEN_DAYS("7 Days"),
    THIRTY_DAYS("30 Days")
}

enum class AnalyticsMetric(val label: String) {
    STEPS("Steps"),
    CALORIES("Calories"),
    DISTANCE("Distance")
}

data class FitnessScoreBreakdown(
    val score: Int,
    val rating: String,
    val stepFactor: Int,
    val activeFactor: Int,
    val waterFactor: Int,
    val calorieFactor: Int,
    val consistencyFactor: Int,
    val hasEnoughData: Boolean,
    val summary: String
)

data class MotionEfficiencyResult(
    val score: Int,
    val rating: String,
    val stabilityPercent: Int,
    val cadenceRhythmPercent: Int,
    val paceSmoothnessPercent: Int,
    val rejectedShakes: Int,
    val hasData: Boolean,
    val description: String
)

data class ConsistencyResult(
    val score: Int,
    val rating: String,
    val activeDaysCount: Int,
    val totalDaysCount: Int,
    val goalsAchievedDays: Int,
    val streakDays: Int,
    val hasData: Boolean,
    val description: String
)

data class PersonalBests(
    val maxSteps: Int = 0,
    val maxStepsDate: String = "",
    val maxDistanceMeters: Double = 0.0,
    val maxCalories: Double = 0.0,
    val maxDurationSeconds: Long = 0L,
    val maxSpeedKmh: Double = 0.0,
    val hasRecords: Boolean = false
)

data class ActiveVsInactiveData(
    val activeMinutes: Int,
    val inactiveMinutes: Int,
    val activePercentage: Float,
    val inactivePercentage: Float,
    val formattedActive: String,
    val formattedInactive: String,
    val hasData: Boolean,
    val insightSummary: String
)

data class GoalProgressItem(
    val title: String,
    val currentFormatted: String,
    val targetFormatted: String,
    val progress: Float,
    val percentageInt: Int,
    val color: Color
)

data class HeatmapDayData(
    val dayOfWeek: String,
    val dateStr: String,
    val displayDate: String,
    val steps: Int,
    val distanceMeters: Double,
    val calories: Double,
    val activeMinutes: Int,
    val intensityLevel: Int, // 0 = 0%, 1 = 1-35%, 2 = 36-70%, 3 = 71-99%, 4 = 100%+
    val isToday: Boolean
)

data class AnalyticsInsight(
    val title: String,
    val description: String,
    val category: String,
    val accentColor: Color
)

object AnalyticsEngine {

    private val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayOfWeekSdf = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthDaySdf = SimpleDateFormat("d MMM", Locale.getDefault())

    fun getTodayDateString(): String = isoSdf.format(Date())

    /**
     * Merge today's live steps into the daily activity list for consistent real-time computations.
     */
    fun getEffectiveDailyActivities(
        allActivities: List<DailyActivity>,
        todayActivity: DailyActivity?,
        liveSteps: Int,
        userProfile: UserProfile
    ): List<DailyActivity> {
        val todayStr = getTodayDateString()
        val activityMap = allActivities.associateBy { it.date }.toMutableMap()

        val strideMeters = if (userProfile.strideLengthMeters > 0) userProfile.strideLengthMeters else 0.75
        val weightKg = if (userProfile.weightKg > 0) userProfile.weightKg else 70.0

        val existingToday = activityMap[todayStr] ?: todayActivity
        val currentSteps = max(liveSteps, existingToday?.steps ?: 0)
        val currentWater = existingToday?.waterIntakeMl ?: 0
        val currentActiveMins = max(existingToday?.activeMinutes ?: 0, (currentSteps / 100))
        val currentDistMeters = max(existingToday?.distanceMeters ?: 0.0, currentSteps * strideMeters)
        val currentCalories = max(existingToday?.calories ?: 0.0, (currentDistMeters / 1000.0) * weightKg * 0.75)

        activityMap[todayStr] = DailyActivity(
            id = "${userProfile.id}_$todayStr",
            userId = userProfile.id,
            date = todayStr,
            steps = currentSteps,
            calories = currentCalories,
            distanceMeters = currentDistMeters,
            activeMinutes = currentActiveMins,
            waterIntakeMl = currentWater
        )

        return activityMap.values.sortedBy { it.date }
    }

    /**
     * Section 1: Dynamic weighted Fitness Score based on REAL data.
     */
    fun calculateFitnessScore(
        todayActivity: DailyActivity?,
        allActivities: List<DailyActivity>,
        userProfile: UserProfile,
        consistencyScore: Int,
        motionEfficiencyScore: Int
    ): FitnessScoreBreakdown {
        val stepGoal = userProfile.dailyStepGoal.coerceAtLeast(1)
        val waterGoal = userProfile.dailyWaterGoalMl.coerceAtLeast(1)
        val activeMinsGoal = 30
        val calorieGoal = 400.0

        val todaySteps = todayActivity?.steps ?: 0
        val todayWater = todayActivity?.waterIntakeMl ?: 0
        val todayActive = todayActivity?.activeMinutes ?: 0
        val todayCalories = todayActivity?.calories ?: 0.0

        val totalHistoricalSteps = allActivities.sumOf { it.steps }

        if (totalHistoricalSteps == 0 && todaySteps == 0 && todayWater == 0) {
            return FitnessScoreBreakdown(
                score = 0,
                rating = "No Data Yet",
                stepFactor = 0,
                activeFactor = 0,
                waterFactor = 0,
                calorieFactor = 0,
                consistencyFactor = 0,
                hasEnoughData = false,
                summary = "Start moving today to compute your live fitness index."
            )
        }

        // Weighted calculations (Total = 100)
        // 1. Step factor: 25 pts max
        val stepRatio = (todaySteps.toFloat() / stepGoal).coerceIn(0f, 1.2f)
        val stepFactor = (stepRatio * 25f).roundToInt().coerceAtMost(25)

        // 2. Active minutes factor: 20 pts max
        val activeRatio = (todayActive.toFloat() / activeMinsGoal).coerceIn(0f, 1.2f)
        val activeFactor = (activeRatio * 20f).roundToInt().coerceAtMost(20)

        // 3. Water intake factor: 15 pts max
        val waterRatio = (todayWater.toFloat() / waterGoal).coerceIn(0f, 1.0f)
        val waterFactor = (waterRatio * 15f).roundToInt().coerceAtMost(15)

        // 4. Calorie burn factor: 15 pts max
        val calorieRatio = (todayCalories / calorieGoal).toFloat().coerceIn(0f, 1.2f)
        val calorieFactor = (calorieRatio * 15f).roundToInt().coerceAtMost(15)

        // 5. Consistency factor: 15 pts max
        val consistencyFactor = ((consistencyScore.toFloat() / 100f) * 15f).roundToInt().coerceIn(0, 15)

        // 6. Motion Efficiency factor: 10 pts max
        val motionFactor = if (motionEfficiencyScore > 0) {
            ((motionEfficiencyScore.toFloat() / 100f) * 10f).roundToInt().coerceIn(0, 10)
        } else {
            5 // Baseline if telemetry warming up
        }

        val totalScore = (stepFactor + activeFactor + waterFactor + calorieFactor + consistencyFactor + motionFactor)
            .coerceIn(5, 100)

        val rating = when {
            totalScore >= 85 -> "Excellent"
            totalScore >= 70 -> "Good"
            totalScore >= 50 -> "Moderate"
            else -> "Needs Activity"
        }

        val summary = when {
            totalScore >= 85 -> "Outstanding performance! You are exceeding your daily movement & wellness targets."
            totalScore >= 70 -> "Great pace! You are consistently meeting key daily activity milestones."
            totalScore >= 50 -> "Moderate activity recorded. A short evening walk will boost your score."
            else -> "Activity is below target. Take a walk to build momentum."
        }

        return FitnessScoreBreakdown(
            score = totalScore,
            rating = rating,
            stepFactor = stepFactor,
            activeFactor = activeFactor,
            waterFactor = waterFactor,
            calorieFactor = calorieFactor,
            consistencyFactor = consistencyFactor,
            hasEnoughData = true,
            summary = summary
        )
    }

    /**
     * Section 4: Motion Efficiency Score (0-100) from real sensors & session telemetry.
     */
    fun calculateMotionEfficiency(
        diagnostics: SensorFusionDiagnostics?,
        completedActivities: List<CompletedActivity>,
        savedRoutes: List<SavedRoute>,
        todaySteps: Int
    ): MotionEfficiencyResult {
        val totalValidated = diagnostics?.totalStepsValidated ?: todaySteps
        val rejectedShakes = diagnostics?.rejectedShakesCount ?: 0

        // If no movement recorded at all:
        if (totalValidated == 0 && completedActivities.isEmpty() && savedRoutes.isEmpty()) {
            return MotionEfficiencyResult(
                score = 0,
                rating = "Needs Data",
                stabilityPercent = 0,
                cadenceRhythmPercent = 0,
                paceSmoothnessPercent = 0,
                rejectedShakes = 0,
                hasData = false,
                description = "Collect more movement data to calculate Motion Efficiency."
            )
        }

        // 1. Stability Factor (Low parasitic jerk / high step purity)
        val stepPurityRatio = if (totalValidated + rejectedShakes > 0) {
            totalValidated.toFloat() / (totalValidated + rejectedShakes).toFloat()
        } else 1.0f
        val stabilityScore = (stepPurityRatio * 100f).roundToInt().coerceIn(60, 98)

        // 2. Cadence Rhythm (Walking confidence & cadence smoothness)
        val walkingConf = diagnostics?.confidence?.walkingConfidence ?: 0.85f
        val runningConf = diagnostics?.confidence?.runningConfidence ?: 0.0f
        val activeConf = max(walkingConf, runningConf)
        val cadenceScore = if (activeConf > 0.1f) {
            (activeConf * 100f).roundToInt().coerceIn(65, 96)
        } else {
            82 // Standard baseline walking cadence
        }

        // 3. Pace Smoothness (From saved routes / completed activities speed variance)
        val paceSmoothness = if (savedRoutes.isNotEmpty()) {
            val avgRatio = savedRoutes.map {
                if (it.maxSpeedKmh > 0.1) (it.avgSpeedKmh / it.maxSpeedKmh).toFloat().coerceIn(0.4f, 0.95f)
                else 0.80f
            }.average().toFloat()
            (avgRatio * 100f).roundToInt().coerceIn(60, 95)
        } else if (completedActivities.isNotEmpty()) {
            85
        } else {
            80
        }

        // Combined Motion Efficiency Score
        val weightedScore = (stabilityScore * 0.40f + cadenceScore * 0.35f + paceSmoothness * 0.25f)
            .roundToInt()
            .coerceIn(30, 99)

        val rating = when {
            weightedScore >= 90 -> "Excellent"
            weightedScore >= 75 -> "Good"
            weightedScore >= 60 -> "Moderate"
            else -> "Needs Improvement"
        }

        val description = when {
            weightedScore >= 90 -> "Optimal biomechanical stride cadence with minimal lateral jitter."
            weightedScore >= 75 -> "Stable walking posture and smooth stride consistency."
            weightedScore >= 60 -> "Moderate motion stability with slight cadence variation."
            else -> "Irregular movement rhythms or sudden stopping detected."
        }

        return MotionEfficiencyResult(
            score = weightedScore,
            rating = rating,
            stabilityPercent = stabilityScore,
            cadenceRhythmPercent = cadenceScore,
            paceSmoothnessPercent = paceSmoothness,
            rejectedShakes = rejectedShakes,
            hasData = true,
            description = description
        )
    }

    /**
     * Section 5: Consistency Score (0-100) from real historical activity days & goal met ratio.
     */
    fun calculateConsistency(
        activities: List<DailyActivity>,
        stepGoal: Int,
        timeRange: AnalyticsTimeRange
    ): ConsistencyResult {
        if (activities.isEmpty()) {
            return ConsistencyResult(
                score = 0,
                rating = "No History",
                activeDaysCount = 0,
                totalDaysCount = 0,
                goalsAchievedDays = 0,
                streakDays = 0,
                hasData = false,
                description = "No consistency history yet."
            )
        }

        val sorted = activities.sortedBy { it.date }
        val window = when (timeRange) {
            AnalyticsTimeRange.TODAY -> sorted.takeLast(7) // Today uses last 7 days for meaningful habit score
            AnalyticsTimeRange.SEVEN_DAYS -> sorted.takeLast(7)
            AnalyticsTimeRange.THIRTY_DAYS -> sorted.takeLast(30)
        }

        val targetStepGoal = stepGoal.coerceAtLeast(1)
        val activeDays = window.count { it.steps >= 500 }
        val goalMetDays = window.count { it.steps >= (targetStepGoal * 0.8) }
        val totalDays = window.size.coerceAtLeast(1)

        // Calculate consecutive active day streak up to today
        var streak = 0
        for (i in sorted.indices.reversed()) {
            if (sorted[i].steps >= 500) {
                streak++
            } else {
                break
            }
        }

        val activeRatio = activeDays.toFloat() / totalDays.toFloat()
        val goalRatio = goalMetDays.toFloat() / totalDays.toFloat()
        val streakBonus = (streak.toFloat() / 7f).coerceIn(0f, 1f) * 15f

        val calculatedScore = ((activeRatio * 50f) + (goalRatio * 35f) + streakBonus)
            .roundToInt()
            .coerceIn(0, 100)

        val rating = when {
            calculatedScore >= 85 -> "Consistent Athlete"
            calculatedScore >= 65 -> "Building Habit"
            calculatedScore >= 40 -> "Developing Routine"
            else -> "Irregular Pace"
        }

        val desc = when {
            calculatedScore >= 85 -> "Outstanding discipline! You consistently reach your movement targets."
            calculatedScore >= 65 -> "Solid momentum. You're active on most tracked days."
            calculatedScore >= 40 -> "Building consistency. Strive for consecutive daily walking streaks."
            else -> "Activity is intermittent. Try setting a small daily reminder to walk."
        }

        return ConsistencyResult(
            score = calculatedScore,
            rating = rating,
            activeDaysCount = activeDays,
            totalDaysCount = totalDays,
            goalsAchievedDays = goalMetDays,
            streakDays = streak,
            hasData = activeDays > 0,
            description = desc
        )
    }

    /**
     * Section 6: Personal Best Records from real stored data.
     */
    fun calculatePersonalBests(
        allActivities: List<DailyActivity>,
        completedActivities: List<CompletedActivity>,
        savedRoutes: List<SavedRoute>
    ): PersonalBests {
        val maxStepDay = allActivities.maxByOrNull { it.steps }
        val maxSteps = maxStepDay?.steps ?: 0
        val maxStepsDate = maxStepDay?.let {
            try {
                val d = isoSdf.parse(it.date)
                d?.let { date -> monthDaySdf.format(date) } ?: it.date
            } catch (e: Exception) { it.date }
        } ?: ""

        val maxDailyDist = allActivities.maxOfOrNull { it.distanceMeters } ?: 0.0
        val maxWorkoutDist = completedActivities.maxOfOrNull { it.distanceMeters } ?: 0.0
        val maxRouteDist = savedRoutes.maxOfOrNull { it.distanceMeters } ?: 0.0
        val maxDistance = maxOf(maxDailyDist, maxWorkoutDist, maxRouteDist)

        val maxDailyCal = allActivities.maxOfOrNull { it.calories } ?: 0.0
        val maxWorkoutCal = completedActivities.maxOfOrNull { it.calories } ?: 0.0
        val maxCalories = maxOf(maxDailyCal, maxWorkoutCal)

        val maxDailyDuration = (allActivities.maxOfOrNull { it.activeMinutes } ?: 0) * 60L
        val maxWorkoutDuration = completedActivities.maxOfOrNull { it.durationSeconds } ?: 0L
        val maxDuration = maxOf(maxDailyDuration, maxWorkoutDuration)

        val maxRouteSpeed = savedRoutes.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
        val maxWorkoutSpeed = completedActivities.mapNotNull { act ->
            if (act.durationSeconds > 60 && act.distanceMeters > 50) {
                (act.distanceMeters / 1000.0) / (act.durationSeconds / 3600.0)
            } else null
        }.maxOrNull() ?: 0.0
        val maxSpeed = maxOf(maxRouteSpeed, maxWorkoutSpeed)

        val hasRecords = maxSteps > 0 || maxDistance > 0.0 || maxCalories > 0.0 || maxDuration > 0L

        return PersonalBests(
            maxSteps = maxSteps,
            maxStepsDate = maxStepsDate,
            maxDistanceMeters = maxDistance,
            maxCalories = maxCalories,
            maxDurationSeconds = maxDuration,
            maxSpeedKmh = maxSpeed,
            hasRecords = hasRecords
        )
    }

    /**
     * Section 7: Active vs Inactive Time.
     */
    fun calculateActiveVsInactive(
        activities: List<DailyActivity>,
        completedActivities: List<CompletedActivity>,
        timeRange: AnalyticsTimeRange
    ): ActiveVsInactiveData {
        val filtered = when (timeRange) {
            AnalyticsTimeRange.TODAY -> activities.takeLast(1)
            AnalyticsTimeRange.SEVEN_DAYS -> activities.takeLast(7)
            AnalyticsTimeRange.THIRTY_DAYS -> activities.takeLast(30)
        }

        val totalActiveMins = filtered.sumOf { it.activeMinutes }
        val numDays = filtered.size.coerceAtLeast(1)

        // Estimated tracking window (e.g. 14 waking hours per day = 840 mins)
        val wakingMinsPerDay = 14 * 60
        val totalWindowMins = numDays * wakingMinsPerDay
        val inactiveMins = max(0, totalWindowMins - totalActiveMins)

        val totalMins = (totalActiveMins + inactiveMins).coerceAtLeast(1)
        val activePct = (totalActiveMins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f)
        val inactivePct = (1f - activePct).coerceIn(0f, 1f)

        fun formatMins(mins: Int): String {
            val hrs = mins / 60
            val remainingMins = mins % 60
            return if (hrs > 0) "${hrs}h ${remainingMins}m" else "${remainingMins}m"
        }

        val summary = if (totalActiveMins > 0) {
            when (timeRange) {
                AnalyticsTimeRange.TODAY -> "You were active for ${formatMins(totalActiveMins)} and resting for ${formatMins(inactiveMins)} today."
                else -> "Averaging ${formatMins(totalActiveMins / numDays)} active time per day over the last $numDays days."
            }
        } else {
            "No active time recorded for this period."
        }

        return ActiveVsInactiveData(
            activeMinutes = totalActiveMins,
            inactiveMinutes = inactiveMins,
            activePercentage = activePct,
            inactivePercentage = inactivePct,
            formattedActive = formatMins(totalActiveMins),
            formattedInactive = formatMins(inactiveMins),
            hasData = totalActiveMins > 0,
            insightSummary = summary
        )
    }

    /**
     * Section 8: Goal Achievement progress list.
     */
    fun calculateGoalAchievements(
        todayActivity: DailyActivity?,
        userProfile: UserProfile,
        isImperial: Boolean,
        formatDistance: (Double, Boolean) -> String
    ): List<GoalProgressItem> {
        val steps = todayActivity?.steps ?: 0
        val stepGoal = userProfile.dailyStepGoal.coerceAtLeast(1)
        val stepPct = ((steps.toFloat() / stepGoal) * 100).roundToInt()

        val water = todayActivity?.waterIntakeMl ?: 0
        val waterGoal = userProfile.dailyWaterGoalMl.coerceAtLeast(1)
        val waterPct = ((water.toFloat() / waterGoal) * 100).roundToInt()

        val activeMins = todayActivity?.activeMinutes ?: 0
        val activeGoal = 30
        val activePct = ((activeMins.toFloat() / activeGoal) * 100).roundToInt()

        val calories = todayActivity?.calories ?: 0.0
        val calorieGoal = 400.0
        val caloriePct = ((calories / calorieGoal) * 100).roundToInt()

        return listOf(
            GoalProgressItem(
                title = "Daily Steps",
                currentFormatted = "%,d".format(steps),
                targetFormatted = "%,d steps".format(stepGoal),
                progress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f),
                percentageInt = stepPct,
                color = CyberPrimaryCyan
            ),
            GoalProgressItem(
                title = "Hydration",
                currentFormatted = "%.1f L".format(water / 1000.0),
                targetFormatted = "%.1f L".format(waterGoal / 1000.0),
                progress = (water.toFloat() / waterGoal).coerceIn(0f, 1f),
                percentageInt = waterPct,
                color = CyberAccentMint
            ),
            GoalProgressItem(
                title = "Active Minutes",
                currentFormatted = "$activeMins min",
                targetFormatted = "$activeGoal min",
                progress = (activeMins.toFloat() / activeGoal).coerceIn(0f, 1f),
                percentageInt = activePct,
                color = CyberSecondaryViolet
            ),
            GoalProgressItem(
                title = "Active Calories",
                currentFormatted = "%.0f kcal".format(calories),
                targetFormatted = "%.0f kcal".format(calorieGoal),
                progress = (calories / calorieGoal).toFloat().coerceIn(0f, 1f),
                percentageInt = caloriePct,
                color = CyberWarning
            )
        )
    }

    /**
     * Section 9: 7-Day Heatmap Matrix for MON-SUN.
     */
    fun buildWeeklyHeatmap(
        activities: List<DailyActivity>,
        stepGoal: Int
    ): List<HeatmapDayData> {
        val calendar = Calendar.getInstance()
        val todayStr = getTodayDateString()

        // Align with Monday of current week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (currentDayOfWeek - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val activityByDate = activities.associateBy { it.date }
        val result = mutableListOf<HeatmapDayData>()
        val targetGoal = stepGoal.coerceAtLeast(1)

        val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        for (i in 0 until 7) {
            val date = calendar.time
            val dateStr = isoSdf.format(date)
            val displayDate = monthDaySdf.format(date)
            val dayName = dayNames[i]

            val act = activityByDate[dateStr]
            val steps = act?.steps ?: 0
            val dist = act?.distanceMeters ?: 0.0
            val cal = act?.calories ?: 0.0
            val mins = act?.activeMinutes ?: 0

            val ratio = steps.toFloat() / targetGoal.toFloat()
            val level = when {
                steps <= 0 -> 0
                ratio < 0.35f -> 1
                ratio < 0.70f -> 2
                ratio < 1.00f -> 3
                else -> 4
            }

            result.add(
                HeatmapDayData(
                    dayOfWeek = dayName,
                    dateStr = dateStr,
                    displayDate = displayDate,
                    steps = steps,
                    distanceMeters = dist,
                    calories = cal,
                    activeMinutes = mins,
                    intensityLevel = level,
                    isToday = (dateStr == todayStr)
                )
            )

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return result
    }

    /**
     * Section 10: Rule-based intelligent diagnostic AI insights from real data.
     */
    fun generateAiInsights(
        activities: List<DailyActivity>,
        todayActivity: DailyActivity?,
        userProfile: UserProfile,
        motionEfficiency: MotionEfficiencyResult,
        consistency: ConsistencyResult
    ): List<AnalyticsInsight> {
        val insights = mutableListOf<AnalyticsInsight>()
        val sorted = activities.sortedBy { it.date }

        if (sorted.isEmpty() || (sorted.size == 1 && (todayActivity?.steps ?: 0) == 0)) {
            return listOf(
                AnalyticsInsight(
                    title = "Getting Started",
                    description = "Keep using MotionIQ throughout your day to generate personalized biomechanical and performance insights.",
                    category = "System",
                    accentColor = CyberPrimaryCyan
                )
            )
        }

        // 1. Weekly Comparison Trend Insight
        if (sorted.size >= 7) {
            val currentWeek = sorted.takeLast(7)
            val previousWeek = sorted.dropLast(7).takeLast(7)

            val curSteps = currentWeek.sumOf { it.steps }
            val prevSteps = previousWeek.sumOf { it.steps }

            if (prevSteps > 0) {
                val diffPct = (((curSteps - prevSteps).toFloat() / prevSteps.toFloat()) * 100).roundToInt()
                if (diffPct >= 5) {
                    insights.add(
                        AnalyticsInsight(
                            title = "Activity Surge",
                            description = "Your weekly activity increased by $diffPct% compared to the previous week. Excellent stamina progression!",
                            category = "Trend",
                            accentColor = CyberAccentMint
                        )
                    )
                } else if (diffPct <= -5) {
                    insights.add(
                        AnalyticsInsight(
                            title = "Pacing Opportunity",
                            description = "Your activity is $diffPct% lower than last week. A 15-minute brisk walk today will help close the gap.",
                            category = "Trend",
                            accentColor = CyberWarning
                        )
                    )
                }
            }
        }

        // 2. Consistency Insight
        if (consistency.activeDaysCount > 0) {
            if (consistency.streakDays >= 3) {
                insights.add(
                    AnalyticsInsight(
                        title = "Active Streak",
                        description = "Impressive consistency! You have an active streak of ${consistency.streakDays} consecutive days.",
                        category = "Consistency",
                        accentColor = CyberSecondaryViolet
                    )
                )
            } else if (consistency.goalsAchievedDays >= 3) {
                insights.add(
                    AnalyticsInsight(
                        title = "Goal Rhythm",
                        description = "Great discipline! You achieved your step goal on ${consistency.goalsAchievedDays} of the tracked days.",
                        category = "Consistency",
                        accentColor = CyberPrimaryCyan
                    )
                )
            }
        }

        // 3. Motion Efficiency Feedback
        if (motionEfficiency.hasData) {
            val effScore = motionEfficiency.score
            val effMsg = when {
                effScore >= 90 -> "Your Motion Efficiency is $effScore% (Excellent). High rhythm cadence with minimal lateral acceleration jitter."
                effScore >= 75 -> "Your Motion Efficiency is $effScore% (Good). Smooth stride consistency detected during movement."
                else -> "Motion stability is $effScore%. Maintaining a steady pace helps optimize joint impact and calorie burn."
            }
            insights.add(
                AnalyticsInsight(
                    title = "Biomechanical Quality",
                    description = effMsg,
                    category = "Motion Quality",
                    accentColor = CyberAccentMint
                )
            )
        }

        // 4. Hydration & Recovery Insight
        val todayWater = todayActivity?.waterIntakeMl ?: 0
        val waterGoal = userProfile.dailyWaterGoalMl.coerceAtLeast(1)
        val waterRatio = todayWater.toFloat() / waterGoal.toFloat()

        if (waterRatio < 0.4f && (todayActivity?.steps ?: 0) > 2000) {
            insights.add(
                AnalyticsInsight(
                    title = "Hydration Needed",
                    description = "Your hydration is at ${(waterRatio * 100).roundToInt()}% of target. Drink 350ml before your next active session.",
                    category = "Hydration",
                    accentColor = CyberPrimaryCyan
                )
            )
        } else if (waterRatio >= 0.8f) {
            insights.add(
                AnalyticsInsight(
                    title = "Optimal Hydration",
                    description = "Well hydrated! Meeting your daily fluid intake accelerates muscle recovery and metabolic efficiency.",
                    category = "Hydration",
                    accentColor = CyberAccentMint
                )
            )
        }

        // 5. Sedentary Alert
        val todayActive = todayActivity?.activeMinutes ?: 0
        val todaySteps = todayActivity?.steps ?: 0
        if (todaySteps < 1500 && todayActive < 15) {
            insights.add(
                AnalyticsInsight(
                    title = "Movement Break",
                    description = "Extended sedentary period detected today. Stand up and take a 250-step movement break to reset posture.",
                    category = "Recovery",
                    accentColor = CyberPinkGlow
                )
            )
        }

        return if (insights.isNotEmpty()) insights else listOf(
            AnalyticsInsight(
                title = "Steady Progress",
                description = "Keep moving throughout your day to build comprehensive fitness trends and personalized insights.",
                category = "Insight",
                accentColor = CyberPrimaryCyan
            )
        )
    }
}
