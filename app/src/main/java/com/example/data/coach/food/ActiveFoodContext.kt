package com.example.data.coach.food

import com.example.data.coach.FoodNutrientInfo

enum class FoodPortionSize(val label: String, val multiplier: Double) {
    SMALL("Small", 0.75),
    MEDIUM("Medium", 1.0),
    LARGE("Large", 1.5),
    EXTRA_LARGE("Extra Large", 2.0)
}

data class AnalyzedFoodItem(
    val food: FoodNutrientInfo,
    val portionSize: FoodPortionSize = FoodPortionSize.MEDIUM,
    val quantity: Double = 1.0,
    val confidencePercent: Int = 85,
    val isManuallySelected: Boolean = false
) {
    val totalMultiplier: Double get() = portionSize.multiplier * quantity

    val calculatedCalories: Int get() = (food.caloriesKcal * totalMultiplier).toInt()
    val calculatedProtein: Double get() = food.proteinG * totalMultiplier
    val calculatedCarbs: Double get() = food.carbsG * totalMultiplier
    val calculatedFat: Double get() = food.fatG * totalMultiplier
    val calculatedFiber: Double get() = food.fiberG * totalMultiplier
    val calculatedWeightGrams: Int get() = (food.baseServingGrams * totalMultiplier).toInt()
}

data class ActiveFoodContext(
    val items: List<AnalyzedFoodItem>,
    val imageUriString: String? = null,
    val overallStatus: RecognitionConfidenceLevel = RecognitionConfidenceLevel.HIGH_CONFIDENCE,
    val diagnosticMessage: String = "",
    val sessionId: String = java.util.UUID.randomUUID().toString()
) {
    val requestId: String get() = sessionId
    val totalCalories: Int get() = items.sumOf { it.calculatedCalories }
    val totalProtein: Double get() = items.sumOf { it.calculatedProtein }
    val totalCarbs: Double get() = items.sumOf { it.calculatedCarbs }
    val totalFat: Double get() = items.sumOf { it.calculatedFat }
    val totalFiber: Double get() = items.sumOf { it.calculatedFiber }

    val primaryItem: AnalyzedFoodItem? get() = items.firstOrNull()
}
