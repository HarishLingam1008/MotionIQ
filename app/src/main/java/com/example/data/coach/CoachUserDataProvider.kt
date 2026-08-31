package com.example.data.coach

import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import kotlin.math.roundToInt

class CoachUserDataProvider(
    private val input: CoachInputData?,
    private val profile: UserProfile?
) {
    val name: String = input?.name ?: profile?.name ?: "Athlete"
    val steps: Int = input?.steps ?: 0
    val stepGoal: Int = input?.stepGoal ?: profile?.dailyStepGoal ?: 8000
    val remainingSteps: Int = (stepGoal - steps).coerceAtLeast(0)
    val stepProgressPercent: Double = if (stepGoal > 0) (steps * 100.0) / stepGoal else 0.0

    val distanceMeters: Double = input?.distanceMeters ?: 0.0
    val distanceKm: Double = distanceMeters / 1000.0

    val caloriesBurned: Double = input?.caloriesBurned ?: 0.0
    val activeMinutes: Int = input?.activeMinutes ?: 0
    val inactiveMinutes: Int = (480 - activeMinutes).coerceAtLeast(0)

    val waterIntakeMl: Int = input?.waterIntakeMl ?: 0
    val waterGoalMl: Int = input?.waterGoalMl ?: profile?.dailyWaterGoalMl ?: 2500
    val remainingWaterMl: Int = (waterGoalMl - waterIntakeMl).coerceAtLeast(0)
    val waterProgressPercent: Double = if (waterGoalMl > 0) (waterIntakeMl * 100.0) / waterGoalMl else 0.0

    val weightKg: Double = input?.weightKg ?: profile?.weightKg ?: 68.0
    val heightCm: Double = input?.heightCm ?: profile?.heightCm ?: 176.0
    val age: Int = input?.age ?: profile?.age ?: 28
    val gender: String = input?.gender ?: profile?.gender ?: "Male"
    val activityLevel: String = input?.activityLevel ?: profile?.activityLevel ?: "Moderately Active"
    val currentActivity: String = input?.currentActivity ?: "Resting"

    val weeklyActivities: List<DailyActivity> = input?.weeklyActivities ?: emptyList()

    // Accurate BMI Calculation
    val bmi: Double by lazy {
        if (heightCm > 0.0) {
            val heightM = heightCm / 100.0
            (weightKg / (heightM * heightM) * 100.0).roundToInt() / 100.0
        } else {
            22.0
        }
    }

    val bmiCategory: String by lazy {
        when {
            bmi < 18.5 -> "Underweight"
            bmi in 18.5..24.99 -> "Normal weight"
            bmi in 25.0..29.99 -> "Overweight"
            else -> "Obese"
        }
    }

    // BMR (Mifflin-St Jeor equation)
    val bmrKcal: Int by lazy {
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        if (gender.equals("Female", ignoreCase = true)) {
            (base - 161).roundToInt()
        } else {
            (base + 5).roundToInt()
        }
    }

    // TDEE (Total Daily Energy Expenditure)
    val tdeeKcal: Int by lazy {
        val multiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "lightly active" -> 1.375
            "moderately active" -> 1.55
            "very active" -> 1.725
            "extra active" -> 1.9
            else -> 1.45
        }
        (bmrKcal * multiplier).roundToInt()
    }

    val weightLossTargetKcal: Int by lazy {
        (tdeeKcal - 450).coerceAtLeast(if (gender.equals("Female", ignoreCase = true)) 1250 else 1500)
    }

    val weightGainTargetKcal: Int by lazy {
        tdeeKcal + 350
    }

    val recommendedProteinGrams: Pair<Int, Int> by lazy {
        val low = (weightKg * 1.2).roundToInt()
        val high = (weightKg * 1.8).roundToInt()
        Pair(low, high)
    }

    // Historical Analytics Evaluation
    fun getWeeklyStepAverage(): Int {
        if (weeklyActivities.isEmpty()) return steps
        val total = weeklyActivities.sumOf { it.steps }
        return (total / weeklyActivities.size).coerceAtLeast(steps)
    }

    fun getBestStepDay(): DailyActivity? {
        return weeklyActivities.maxByOrNull { it.steps }
    }

    fun getWeeklyConsistencyPercent(): Int {
        if (weeklyActivities.isEmpty()) return 75
        val daysMetGoal = weeklyActivities.count { it.steps >= stepGoal }
        return ((daysMetGoal.toDouble() / weeklyActivities.size.toDouble()) * 100).roundToInt()
    }
}
