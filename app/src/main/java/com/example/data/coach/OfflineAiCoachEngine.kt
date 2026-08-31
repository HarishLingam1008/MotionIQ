package com.example.data.coach

import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import kotlin.math.max
import kotlin.math.roundToInt

data class CoachInputData(
    val name: String = "",
    val steps: Int,
    val stepGoal: Int,
    val distanceMeters: Double,
    val caloriesBurned: Double,
    val activeMinutes: Int,
    val waterIntakeMl: Int,
    val waterGoalMl: Int,
    val heightCm: Double,
    val weightKg: Double,
    val bmi: Double,
    val bmiCategory: String,
    val currentActivity: String, // "Walking", "Running", "Jogging", "Cycling", "Idle", "Still"
    val activityLevel: String,
    val age: Int,
    val gender: String,
    val caloriesConsumed: Int = 0,
    val weeklyActivities: List<DailyActivity> = emptyList()
)

data class HealthScoreBreakdown(
    val stepScore: Int,      // Max 35
    val waterScore: Int,     // Max 25
    val activeScore: Int,    // Max 25
    val bmiScore: Int        // Max 15
)

data class WorkoutSuggestion(
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val intensity: String, // "Low", "Moderate", "High"
    val exerciseType: String
)

data class FoodSuggestion(
    val title: String,
    val description: String,
    val macroFocus: String,
    val mealIdeas: List<String>
)

data class HydrationAdvice(
    val status: String,
    val remainingMl: Int,
    val actionTip: String
)

data class RecoveryAdvice(
    val status: String,
    val primaryTip: String,
    val stretches: List<String>
)

data class GoalProgressAnalysis(
    val overallStatus: String,
    val stepPercent: Int,
    val waterPercent: Int,
    val calorieBurnPercent: Int,
    val actionableInsights: List<String>
)

data class PersonalizedWorkoutTip(
    val id: String,
    val headline: String,
    val category: String, // "Step Boost", "Cardio Tempo", "Strength & Core", "Active Recovery", "HIIT"
    val tip: String,
    val recommendedExercise: String,
    val suggestedDurationMinutes: Int,
    val intensity: String, // "Low", "Moderate", "High"
    val stepImpact: String, // e.g. "+1,800 steps"
    val estimatedBurnKcal: Int,
    val targetActivityMode: String, // "Walking", "Running", "Jogging", "Cycling", "Strength"
    val difficulty: String = "All Levels"
)

data class PersonalizedWorkoutPlan(
    val overallAdvice: String,
    val currentStepProgressPercent: Int,
    val activityTier: String,
    val activityLevelSummary: String,
    val tips: List<PersonalizedWorkoutTip>,
    val generatedAt: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false
)

data class AiCoachAnalysis(
    val healthScore: Int,
    val scoreBreakdown: HealthScoreBreakdown,
    val dailyMotivation: String,
    val primaryInsights: List<String>,
    val workoutSuggestion: WorkoutSuggestion,
    val foodSuggestion: FoodSuggestion,
    val hydrationAdvice: HydrationAdvice,
    val recoveryAdvice: RecoveryAdvice,
    val goalProgress: GoalProgressAnalysis
)

object OfflineAiCoachEngine {

    fun calculateBmi(heightCm: Double, weightKg: Double): Double {
        if (heightCm <= 0.0) return 22.0
        val heightMeters = heightCm / 100.0
        return (weightKg / (heightMeters * heightMeters) * 100.0).roundToInt() / 100.0
    }

    fun calculateBmr(heightCm: Double, weightKg: Double, age: Int, gender: String): Double {
        val safeWeight = if (weightKg > 20.0) weightKg else 65.0
        val safeHeight = if (heightCm > 50.0) heightCm else 170.0
        val safeAge = if (age in 10..110) age else 25
        val base = (10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge)
        return if (gender.equals("Female", ignoreCase = true)) base - 161 else base + 5
    }

    fun calculateTdee(bmr: Double, activityLevel: String): Double {
        val multiplier = when {
            activityLevel.contains("sedentary", ignoreCase = true) -> 1.2
            activityLevel.contains("light", ignoreCase = true) -> 1.375
            activityLevel.contains("moderate", ignoreCase = true) -> 1.55
            activityLevel.contains("very active", ignoreCase = true) || activityLevel.contains("extra", ignoreCase = true) -> 1.725
            else -> 1.45
        }
        return bmr * multiplier
    }

    fun calculateWaterNeeds(weightKg: Double, activeMinutes: Int): Int {
        val safeWeight = if (weightKg > 20.0) weightKg else 65.0
        val baseWater = safeWeight * 35.0
        val exerciseWater = (activeMinutes / 30.0) * 250.0
        return (baseWater + exerciseWater).roundToInt()
    }

    fun calculateProteinNeeds(weightKg: Double, isMuscleBuilding: Boolean = false): Pair<Double, Double> {
        val safeWeight = if (weightKg > 20.0) weightKg else 65.0
        return if (isMuscleBuilding) {
            Pair(safeWeight * 1.6, safeWeight * 2.0)
        } else {
            Pair(safeWeight * 1.0, safeWeight * 1.4)
        }
    }

    /**
     * Evaluates daily health score out of 100 based on verified deterministic metrics.
     */
    fun analyze(input: CoachInputData): AiCoachAnalysis {
        val safeStepGoal = input.stepGoal.coerceAtLeast(1)
        val safeWaterGoal = if (input.waterGoalMl > 0) input.waterGoalMl else 2500

        // 1. Step score (Max 35)
        val stepRatio = (input.steps.toDouble() / safeStepGoal).coerceIn(0.0, 1.2)
        val stepScore = (stepRatio * 35.0).coerceAtMost(35.0).roundToInt()

        // 2. Water score (Max 25)
        val waterRatio = (input.waterIntakeMl.toDouble() / safeWaterGoal).coerceIn(0.0, 1.2)
        val waterScore = (waterRatio * 25.0).coerceAtMost(25.0).roundToInt()

        // 3. Active Minutes score (Max 25, 45 mins target)
        val activeRatio = (input.activeMinutes.toDouble() / 45.0).coerceIn(0.0, 1.2)
        val activeScore = (activeRatio * 25.0).coerceAtMost(25.0).roundToInt()

        // 4. BMI / Body Composition Score (Max 15)
        val bmiScore = when {
            input.bmi in 18.5..24.9 -> 15
            input.bmi in 25.0..27.5 -> 12
            input.bmi in 27.6..29.9 -> 9
            input.bmi > 0.0 -> 7
            else -> 10
        }

        val totalScore = (stepScore + waterScore + activeScore + bmiScore).coerceIn(0, 100)
        val scoreBreakdown = HealthScoreBreakdown(
            stepScore = stepScore,
            waterScore = waterScore,
            activeScore = activeScore,
            bmiScore = bmiScore
        )

        val remainingSteps = max(0, safeStepGoal - input.steps)
        val remainingWater = max(0, safeWaterGoal - input.waterIntakeMl)

        val insights = mutableListOf<String>()
        if (remainingSteps > 0) {
            insights.add("Walk $remainingSteps more steps to reach your daily goal of $safeStepGoal.")
        } else {
            insights.add("Daily step goal reached! Excellent consistency.")
        }

        if (remainingWater > 0) {
            insights.add("Drink $remainingWater ml more water to fulfill your hydration target.")
        } else {
            insights.add("Hydration goal achieved for today.")
        }

        if (input.activeMinutes < 30) {
            insights.add("Aim for at least ${30 - input.activeMinutes} more active minutes.")
        }

        val workoutSuggestion = WorkoutSuggestion(
            title = if (remainingSteps > 0) "Evening Brisk Walk" else "Active Recovery & Mobility",
            description = if (remainingSteps > 0) "A 15-20 min brisk walk will help bank ~$remainingSteps steps." else "Perform gentle leg & spine stretches.",
            durationMinutes = if (remainingSteps > 0) 20 else 10,
            intensity = if (remainingSteps > 0) "Moderate" else "Low",
            exerciseType = if (remainingSteps > 0) "Walking" else "Stretching"
        )

        val foodSuggestion = FoodSuggestion(
            title = "Balanced Whole-Food Fuel",
            description = "Pair complex carbohydrates with clean protein and green vegetables.",
            macroFocus = "High Protein & Fiber",
            mealIdeas = listOf("Boiled Moong Sundal", "Chapati with Dal & Poriyal", "2 Boiled Eggs & Fruit")
        )

        val hydrationAdvice = HydrationAdvice(
            status = if (remainingWater == 0) "Optimal" else "Needs Attention",
            remainingMl = remainingWater,
            actionTip = if (remainingWater > 0) "Drink 250 ml every 60 minutes until target is reached." else "Maintain regular sips."
        )

        val recoveryAdvice = RecoveryAdvice(
            status = if (input.activeMinutes > 60) "High Recovery Needed" else "Standard Recovery",
            primaryTip = "Perform 5 minutes of hamstring & quad stretches before sleeping.",
            stretches = listOf("Standing Quad Stretch", "Seated Forward Bend", "Child's Pose")
        )

        val goalProgress = GoalProgressAnalysis(
            overallStatus = if (totalScore >= 75) "On Track" else "Action Recommended",
            stepPercent = ((input.steps.toDouble() / safeStepGoal) * 100).roundToInt(),
            waterPercent = ((input.waterIntakeMl.toDouble() / safeWaterGoal) * 100).roundToInt(),
            calorieBurnPercent = ((input.caloriesBurned / 400.0) * 100).roundToInt(),
            actionableInsights = insights
        )

        return AiCoachAnalysis(
            healthScore = totalScore,
            scoreBreakdown = scoreBreakdown,
            dailyMotivation = "Small daily habits repeated consistently build extraordinary health.",
            primaryInsights = insights,
            workoutSuggestion = workoutSuggestion,
            foodSuggestion = foodSuggestion,
            hydrationAdvice = hydrationAdvice,
            recoveryAdvice = recoveryAdvice,
            goalProgress = goalProgress
        )
    }

    fun generateWorkoutTips(input: CoachInputData, userProfile: UserProfile?): PersonalizedWorkoutPlan {
        val safeStepGoal = input.stepGoal.coerceAtLeast(1)
        val stepPercent = ((input.steps.toDouble() / safeStepGoal) * 100).roundToInt()
        val remainingSteps = max(0, safeStepGoal - input.steps)

        val tipsList = mutableListOf<PersonalizedWorkoutTip>()
        if (remainingSteps > 0) {
            tipsList.add(
                PersonalizedWorkoutTip(
                    id = "step_boost_1",
                    headline = "15-Minute Brisk Walk",
                    category = "Step Boost",
                    tip = "Add ~1,800 steps with a steady 15-minute brisk walk after lunch or in the evening.",
                    recommendedExercise = "Brisk Walk",
                    suggestedDurationMinutes = 15,
                    intensity = "Moderate",
                    stepImpact = "+1,800 steps",
                    estimatedBurnKcal = 75,
                    targetActivityMode = "Walking"
                )
            )
        } else {
            tipsList.add(
                PersonalizedWorkoutTip(
                    id = "recovery_mobility_1",
                    headline = "Active Recovery & Mobility",
                    category = "Active Recovery",
                    tip = "You reached your step goal! Perform gentle hamstring and hip flexor stretches to relieve muscle tension.",
                    recommendedExercise = "Lower Body Stretches",
                    suggestedDurationMinutes = 10,
                    intensity = "Low",
                    stepImpact = "0 steps",
                    estimatedBurnKcal = 30,
                    targetActivityMode = "Walking"
                )
            )
        }

        tipsList.add(
            PersonalizedWorkoutTip(
                id = "core_bodyweight_2",
                headline = "12-Min Core & Posture Circuit",
                category = "Strength & Core",
                tip = "Strengthen core stability with 3 rounds of planks (30s), glute bridges (15 reps), and bird-dogs (10 reps).",
                recommendedExercise = "Bodyweight Circuit",
                suggestedDurationMinutes = 12,
                intensity = "Moderate",
                stepImpact = "+250 steps",
                estimatedBurnKcal = 60,
                targetActivityMode = "Walking"
            )
        )

        val tier = when {
            stepPercent >= 100 -> "Goal Surpassed & Peak Maintenance"
            stepPercent >= 60 -> "On Track & Consistency"
            else -> "Active Foundation Established"
        }

        val advice = when {
            stepPercent >= 100 -> "Goal accomplished! Focus on hydration and gentle mobility."
            stepPercent >= 60 -> "You're close! A short walk will complete your goal."
            else -> "A brisk 20-minute walk can jumpstart your progress today."
        }

        return PersonalizedWorkoutPlan(
            overallAdvice = advice,
            currentStepProgressPercent = stepPercent,
            activityTier = tier,
            activityLevelSummary = "${input.activeMinutes} mins active | ${input.caloriesBurned.roundToInt()} kcal",
            tips = tipsList,
            isAiGenerated = false
        )
    }

    // =========================================================================
    // MODULAR NLP & OFFLINE INTELLIGENCE PIPELINE
    // =========================================================================

    /**
     * Primary entry point for interactive questions asked to MotionIQ AI Coach.
     * Executes:
     * 1. Text Normalization (English & Tanglish)
     * 2. Entity & Value Extraction (steps, water, distance, weight, duration, food)
     * 3. Conversation Context & Pronoun/Follow-up Resolution
     * 4. Hierarchical Intent Detection (Primary & Secondary Intents)
     * 5. Real User Data Provider Integration
     * 6. Safety Filter Inspection
     * 7. Response Planning & Formatting
     */
    fun generateOfflineAnswer(
        question: String,
        input: CoachInputData,
        userProfile: UserProfile?,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        activeFoodContext: com.example.data.coach.food.ActiveFoodContext? = null
    ): String {
        // 1. Text Normalization
        val normalizedQuery = CoachTextNormalizer.normalize(question)

        // 2. Entity Extraction
        val entities = CoachEntityExtractor.extract(normalizedQuery)

        // 3. Conversation Context Resolution
        val contextResolution = CoachConversationContext.resolveContextualIntent(normalizedQuery, conversationHistory)

        // 4. Intent Detection
        val intentResult = CoachIntentDetector.detectIntent(normalizedQuery, entities, contextResolution)

        // 5. User Data Provider
        val userData = CoachUserDataProvider(input, userProfile)

        // 6. Response Planning (incorporates Knowledge Base, Safety Filter, and Formatting)
        return CoachResponsePlanner.planResponse(intentResult, entities, userData, conversationHistory, activeFoodContext)
    }
}
