package com.example.util

import android.util.Log
import kotlin.math.roundToInt

data class BmiResult(
    val bmi: Double,
    val formattedBmi: String,
    val category: String,
    val tip: String,
    val isValid: Boolean
)

object BmiCalculator {

    private const val TAG = "MotionIQ_BMI"

    private fun logDebug(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
            println("[$TAG] $message")
        }
    }

    private fun logWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: Throwable) {
            System.err.println("[$TAG WARN] $message")
        }
    }

    /**
     * Calculates BMI from weight in kg and height in cm.
     * Converts height from centimeters to meters:
     *   heightMeters = heightCm / 100.0
     *   bmi = weightKg / (heightMeters * heightMeters)
     *
     * @param weightKg User's weight in kilograms
     * @param heightCm User's height in centimeters
     * @return Calculated BMI (raw double), or 0.0 if inputs are invalid.
     */
    fun calculateBMI(weightKg: Double, heightCm: Double): Double {
        if (weightKg <= 0.0 || heightCm <= 0.0) {
            logWarn("Invalid inputs for BMI calculation: Weight = $weightKg kg, Height = $heightCm cm")
            return 0.0
        }

        val heightMeters = heightCm / 100.0
        val bmi = weightKg / (heightMeters * heightMeters)
        val formattedBmi = "%.1f".format(bmi)
        val (category, _) = getBmiCategory(bmi)

        logDebug(
            """
            BMI Calculation:
            Weight = $weightKg kg
            Height = $heightCm cm
            Height = $heightMeters m
            BMI = $bmi
            Displayed BMI = $formattedBmi
            Category = $category
            """.trimIndent()
        )

        return bmi
    }

    /**
     * Adult BMI Categories:
     *   BMI < 18.5       → Underweight
     *   BMI 18.5 – 24.9  → Normal weight
     *   BMI 25.0 – 29.9  → Overweight
     *   BMI >= 30.0      → Obesity
     */
    fun getBmiCategory(bmi: Double): Pair<String, String> {
        return when {
            bmi <= 0.0 -> "Not Available" to "Enter valid height and weight to calculate BMI."
            bmi < 18.5 -> "Underweight" to "Below ideal range. Focus on nutrient-rich caloric intake and strength exercises."
            bmi < 25.0 -> "Normal weight" to "Optimal healthy range. Maintain your balanced nutrition and consistent activity."
            bmi < 30.0 -> "Overweight" to "Slightly above ideal range. Incorporate daily walking steps and moderate portion sizes."
            else -> "Obesity" to "Higher risk range. Focus on steady-state activity and sustainable dietary habits."
        }
    }

    /**
     * Helper to get full evaluated BmiResult including validation and tips.
     */
    fun getFullBmiResult(weightKg: Double, heightCm: Double): BmiResult {
        if (weightKg <= 0.0 || heightCm <= 0.0) {
            return BmiResult(
                bmi = 0.0,
                formattedBmi = "Not Available",
                category = "Not Available",
                tip = "Enter valid height and weight to calculate BMI.",
                isValid = false
            )
        }

        val bmi = calculateBMI(weightKg, heightCm)
        val formatted = "%.1f".format(bmi)
        val (category, tip) = getBmiCategory(bmi)

        return BmiResult(
            bmi = bmi,
            formattedBmi = formatted,
            category = category,
            tip = tip,
            isValid = true
        )
    }

    /**
     * Formats BMI to 1 decimal place or returns fallback message if invalid.
     */
    fun formatBmi(bmi: Double): String {
        return if (bmi > 0.0) "%.1f".format(bmi) else "Not Available"
    }
}
