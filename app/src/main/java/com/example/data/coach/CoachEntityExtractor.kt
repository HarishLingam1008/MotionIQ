package com.example.data.coach

data class ExtractedEntities(
    val steps: Int? = null,
    val distanceKm: Double? = null,
    val waterMl: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val stepGoal: Int? = null,
    val durationMinutes: Int? = null,
    val foodItem: FoodNutrientInfo? = null,
    val foodQuantity: Double = 1.0,
    val isVegetarian: Boolean = false,
    val isAffordable: Boolean = false,
    val isAtHome: Boolean = false,
    val rawNormalizedText: String = ""
)

object CoachEntityExtractor {

    fun extract(normalizedText: String): ExtractedEntities {
        val q = normalizedText.lowercase()

        // 1. Extract Steps
        var steps: Int? = null
        val stepRegexes = listOf(
            Regex("(\\d{1,6})\\s*(?:steps|step|walked|nadanthiruken)"),
            Regex("(?:walked|did|done|steps|today)\\s*(\\d{1,6})\\s*(?:steps|step)?")
        )
        for (reg in stepRegexes) {
            val match = reg.find(q)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null && num in 50..150000) {
                    steps = num
                    break
                }
            }
        }

        // 2. Extract Step Goal
        var stepGoal: Int? = null
        val goalRegex = Regex("(?:goal|target)\\s*(?:is|=|of)?\\s*(\\d{1,6})")
        val goalMatch = goalRegex.find(q)
        if (goalMatch != null) {
            val num = goalMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 1000..100000) {
                stepGoal = num
            }
        }

        // 3. Extract Distance (km or meters)
        var distanceKm: Double? = null
        val distKmRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:km|kms|kilometer|kilometres|kilometers)")
        val distKmMatch = distKmRegex.find(q)
        if (distKmMatch != null) {
            distanceKm = distKmMatch.groupValues[1].toDoubleOrNull()
        } else {
            val distMetersRegex = Regex("(\\d+)\\s*(?:m|meters|metres)\\b")
            val metersMatch = distMetersRegex.find(q)
            if (metersMatch != null) {
                val m = metersMatch.groupValues[1].toDoubleOrNull()
                if (m != null && m > 50.0) distanceKm = m / 1000.0
            }
        }

        // 4. Extract Water (Litres or ml)
        var waterMl: Int? = null
        val waterLitreRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:l|litre|litres|liter|liters)\\b")
        val litreMatch = waterLitreRegex.find(q)
        if (litreMatch != null) {
            val litres = litreMatch.groupValues[1].toDoubleOrNull()
            if (litres != null) waterMl = (litres * 1000).toInt()
        } else {
            val waterMlRegex = Regex("(\\d{2,5})\\s*(?:ml|milliliter|millilitres)")
            val mlMatch = waterMlRegex.find(q)
            if (mlMatch != null) {
                waterMl = mlMatch.groupValues[1].toIntOrNull()
            }
        }

        // 5. Extract Weight (kg)
        var weightKg: Double? = null
        val weightRegex = Regex("(\\d{2,3}(?:\\.\\d+)?)\\s*(?:kg|kgs|kilos|kilo)")
        val weightMatch = weightRegex.find(q)
        if (weightMatch != null) {
            weightKg = weightMatch.groupValues[1].toDoubleOrNull()
        }

        // 6. Extract Height (cm)
        var heightCm: Double? = null
        val heightRegex = Regex("(\\d{2,3}(?:\\.\\d+)?)\\s*(?:cm|cms|centimeter|centimeters)")
        val heightMatch = heightRegex.find(q)
        if (heightMatch != null) {
            heightCm = heightMatch.groupValues[1].toDoubleOrNull()
        }

        // 7. Extract Duration (minutes)
        var durationMinutes: Int? = null
        val durationRegex = Regex("(\\d{1,4})\\s*(?:min|mins|minute|minutes)")
        val durationMatch = durationRegex.find(q)
        if (durationMatch != null) {
            durationMinutes = durationMatch.groupValues[1].toIntOrNull()
        } else {
            val hourRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:hr|hrs|hour|hours)")
            val hourMatch = hourRegex.find(q)
            if (hourMatch != null) {
                val h = hourMatch.groupValues[1].toDoubleOrNull()
                if (h != null) durationMinutes = (h * 60).toInt()
            }
        }

        // 8. Extract Food and Quantity
        val foodMatch = CoachFoodDatabase.findFoodWithQuantity(q)
        val foodItem = foodMatch?.first
        val foodQuantity = foodMatch?.second ?: 1.0

        // 9. Dietary & Context Preferences
        val isVegetarian = q.contains("veg") || q.contains("vegetarian") || q.contains("plant based") || q.contains("non meat")
        val isAffordable = q.contains("cheap") || q.contains("affordable") || q.contains("budget") || q.contains("low cost") || q.contains("kammi")
        val isAtHome = q.contains("home") || q.contains("living room") || q.contains("without equipment") || q.contains("no equipment") || q.contains("bodyweight")

        return ExtractedEntities(
            steps = steps,
            distanceKm = distanceKm,
            waterMl = waterMl,
            weightKg = weightKg,
            heightCm = heightCm,
            stepGoal = stepGoal,
            durationMinutes = durationMinutes,
            foodItem = foodItem,
            foodQuantity = foodQuantity,
            isVegetarian = isVegetarian,
            isAffordable = isAffordable,
            isAtHome = isAtHome,
            rawNormalizedText = normalizedText
        )
    }
}
