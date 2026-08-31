package com.example.data.model

import kotlin.math.roundToInt

data class TargetMacros(
    val dailyCalories: Int,
    val bmr: Int,
    val tdee: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val waterGoalMl: Int
)

object DynamicFoodEngine {

    fun calculateBmr(weightKg: Double, heightCm: Double, age: Int, gender: String): Int {
        if (weightKg <= 0.0 || heightCm <= 0.0 || age <= 0) return 0
        return when {
            gender.equals("Female", ignoreCase = true) -> (10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161.0).roundToInt()
            gender.equals("Male", ignoreCase = true) -> (10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5.0).roundToInt()
            else -> (10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 78.0).roundToInt()
        }
    }

    fun getActivityMultiplier(activityLevel: String): Double {
        return when (activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "lightly active", "light" -> 1.375
            "moderately active", "moderate" -> 1.55
            "very active", "active" -> 1.725
            "extra active", "super active" -> 1.9
            else -> 1.55
        }
    }

    fun calculateTdee(bmr: Int, activityLevel: String): Int {
        if (bmr <= 0) return 0
        val multiplier = getActivityMultiplier(activityLevel)
        return (bmr * multiplier).roundToInt()
    }

    fun calculateTargetCalories(tdee: Int, fitnessGoal: String): Int {
        if (tdee <= 0) return 0
        return when (fitnessGoal.lowercase()) {
            "weight loss", "loss", "lose weight" -> (tdee - 500).coerceAtLeast(1200)
            "muscle gain", "gain", "weight gain", "bulk" -> tdee + 400
            else -> tdee // Maintain Weight
        }
    }

    fun calculateTargetMacros(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: String,
        activityLevel: String,
        fitnessGoal: String
    ): TargetMacros {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val tdee = calculateTdee(bmr, activityLevel)
        val dailyCalories = calculateTargetCalories(tdee, fitnessGoal)

        val proteinPerKg = when (fitnessGoal.lowercase()) {
            "weight loss", "loss", "lose weight", "muscle gain", "gain" -> 2.0
            else -> 1.6
        }
        val proteinG = (weightKg * proteinPerKg).coerceAtLeast(50.0)
        val proteinCals = proteinG * 4.0

        val fatCals = dailyCalories * 0.28
        val fatG = (fatCals / 9.0).coerceAtLeast(30.0)

        val remainingCals = (dailyCalories - proteinCals - fatCals).coerceAtLeast(200.0)
        val carbsG = remainingCals / 4.0

        val waterMl = (weightKg * 35.0).roundToInt().coerceIn(2000, 4500)

        return TargetMacros(
            dailyCalories = dailyCalories,
            bmr = bmr,
            tdee = tdee,
            proteinG = (proteinG * 10).roundToInt() / 10.0,
            carbsG = (carbsG * 10).roundToInt() / 10.0,
            fatG = (fatG * 10).roundToInt() / 10.0,
            waterGoalMl = waterMl
        )
    }

    fun generateDynamicMeals(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: String,
        activityLevel: String,
        fitnessGoal: String,
        dietPreference: String
    ): List<FoodRecommendation> {
        val macros = calculateTargetMacros(weightKg, heightCm, age, gender, activityLevel, fitnessGoal)
        val totalCals = if (macros.dailyCalories > 0) macros.dailyCalories else 2000
        val isVeg = dietPreference.equals("Veg", ignoreCase = true) || dietPreference.equals("Vegetarian", ignoreCase = true)

        val bCals = (totalCals * 0.25).roundToInt()
        val lCals = (totalCals * 0.35).roundToInt()
        val dCals = (totalCals * 0.25).roundToInt()
        val sCals = (totalCals * 0.15).roundToInt()

        val foods = mutableListOf<FoodRecommendation>()

        // Breakfast (25% cals)
        if (isVeg) {
            foods.add(
                FoodRecommendation(
                    id = "dyn_b1",
                    name = "Protein Oatmeal Bowl",
                    category = "Breakfast",
                    calories = (bCals * 0.6).roundToInt(),
                    proteinG = ((macros.proteinG * 0.22) * 10).roundToInt() / 10.0,
                    carbsG = ((bCals * 0.6 * 0.6) / 4.0 * 10).roundToInt() / 10.0,
                    fatG = 6.0,
                    benefits = "Soluble beta-glucan fiber, chia seeds & plant protein to power your morning steps."
                )
            )
            foods.add(
                FoodRecommendation(
                    id = "dyn_b2",
                    name = "Paneer & Spinach Stuffed Paratha / Toast",
                    category = "Breakfast",
                    calories = (bCals * 0.4).roundToInt(),
                    proteinG = 12.0,
                    carbsG = 25.0,
                    fatG = 8.0,
                    benefits = "Rich in calcium, casein protein & iron for steady muscle recovery."
                )
            )
        } else {
            foods.add(
                FoodRecommendation(
                    id = "dyn_b1",
                    name = "Whole Eggs & Avocado Toast",
                    category = "Breakfast",
                    calories = (bCals * 0.65).roundToInt(),
                    proteinG = ((macros.proteinG * 0.25) * 10).roundToInt() / 10.0,
                    carbsG = 28.0,
                    fatG = 14.0,
                    benefits = "Bioavailable complete egg protein, healthy omega fats & choline for high focus."
                )
            )
            foods.add(
                FoodRecommendation(
                    id = "dyn_b2",
                    name = "Banana Berry Protein Shake",
                    category = "Breakfast",
                    calories = (bCals * 0.35).roundToInt(),
                    proteinG = 18.0,
                    carbsG = 24.0,
                    fatG = 3.0,
                    benefits = "Rapid electrolyte potassium replenishment & glycogen restoration."
                )
            )
        }

        // Lunch (35% cals)
        if (isVeg) {
            foods.add(
                FoodRecommendation(
                    id = "dyn_l1",
                    name = "Quinoa & Lentil (Dal) Buddha Bowl",
                    category = "Lunch",
                    calories = (lCals * 0.65).roundToInt(),
                    proteinG = ((macros.proteinG * 0.30) * 10).roundToInt() / 10.0,
                    carbsG = ((lCals * 0.65 * 0.55) / 4.0 * 10).roundToInt() / 10.0,
                    fatG = 9.0,
                    benefits = "High-fiber complex grain bowl providing complete amino acids & steady satiety."
                )
            )
            foods.add(
                FoodRecommendation(
                    id = "dyn_l2",
                    name = "Tofu/Paneer Tikka with Grilled Veggies",
                    category = "Lunch",
                    calories = (lCals * 0.35).roundToInt(),
                    proteinG = 18.0,
                    carbsG = 14.0,
                    fatG = 10.0,
                    benefits = "Antioxidant-rich vegetables & lean soy/dairy protein for muscle mass maintenance."
                )
            )
        } else {
            foods.add(
                FoodRecommendation(
                    id = "dyn_l1",
                    name = "Herb-Grilled Chicken Breast with Brown Rice",
                    category = "Lunch",
                    calories = (lCals * 0.7).roundToInt(),
                    proteinG = ((macros.proteinG * 0.38) * 10).roundToInt() / 10.0,
                    carbsG = ((lCals * 0.7 * 0.5) / 4.0 * 10).roundToInt() / 10.0,
                    fatG = 7.0,
                    benefits = "Lean poultry protein paired with whole grains to optimize your TDEE target."
                )
            )
            foods.add(
                FoodRecommendation(
                    id = "dyn_l2",
                    name = "Crunchy Garden Salad with Olive Oil Dressing",
                    category = "Lunch",
                    calories = (lCals * 0.3).roundToInt(),
                    proteinG = 4.0,
                    carbsG = 12.0,
                    fatG = 8.0,
                    benefits = "Loaded with vitamins A, C, folate & essential digestive roughage."
                )
            )
        }

        // Dinner (25% cals)
        if (isVeg) {
            foods.add(
                FoodRecommendation(
                    id = "dyn_d1",
                    name = "Whole Wheat Roti & Mixed Veg Korma",
                    category = "Dinner",
                    calories = (dCals * 0.65).roundToInt(),
                    proteinG = 12.0,
                    carbsG = 42.0,
                    fatG = 8.0,
                    benefits = "Sustained fiber release that supports overnight metabolic rest without spike."
                )
            )
            foods.add(
                FoodRecommendation(
                    id = "dyn_d2",
                    name = "Warm Clear Vegetable & Moong Soup",
                    category = "Dinner",
                    calories = (dCals * 0.35).roundToInt(),
                    proteinG = 9.0,
                    carbsG = 18.0,
                    fatG = 2.0,
                    benefits = "Hydrating & easy to digest to promote deep REM sleep recovery."
                )
            )
        } else {
            foods.add(
                FoodRecommendation(
                    id = "dyn_d1",
                    name = "Pan-Seared Salmon / Fish Fillet with Asparagus",
                    category = "Dinner",
                    calories = (dCals * 0.7).roundToInt(),
                    proteinG = ((macros.proteinG * 0.32) * 10).roundToInt() / 10.0,
                    carbsG = 14.0,
                    fatG = 12.0,
                    benefits = "Rich in Omega-3 fatty acids to reduce muscle inflammation post-workout."
                )
            )
            foods.add(
                FoodRecommendation(
                    id = "dyn_d2",
                    name = "Steamed Sweet Potato & Broccoli Mash",
                    category = "Dinner",
                    calories = (dCals * 0.3).roundToInt(),
                    proteinG = 4.0,
                    carbsG = 26.0,
                    fatG = 1.0,
                    benefits = "High in micronutrients, potassium & slow-digesting complex carbs."
                )
            )
        }

        // Snacks (15% cals)
        foods.add(
            FoodRecommendation(
                id = "dyn_s1",
                name = "Raw Almonds, Walnuts & Pumpkin Seeds",
                category = "Snack",
                calories = (sCals * 0.55).roundToInt(),
                proteinG = 7.0,
                carbsG = 6.0,
                fatG = 12.0,
                benefits = "Magnesium & healthy monounsaturated fats for sustained cognitive endurance."
            )
        )
        foods.add(
            FoodRecommendation(
                id = "dyn_s2",
                name = "Low-Fat Greek Yogurt / Cottage Cheese",
                category = "Snack",
                calories = (sCals * 0.45).roundToInt(),
                proteinG = 12.0,
                carbsG = 8.0,
                fatG = 3.0,
                benefits = "Probiotics for gut health and slow-release casein protein."
            )
        )

        return foods
    }
}
