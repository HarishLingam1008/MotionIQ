package com.example.data.coach.nutrition

import com.example.data.coach.CoachFoodDatabase
import com.example.data.coach.FoodNutrientInfo
import com.example.data.coach.food.ActiveFoodContext
import com.example.data.coach.food.AnalyzedFoodItem
import com.example.data.coach.food.FoodPortionSize
import com.example.data.local.UserProfile
import java.text.DecimalFormat

object NutritionContextManager {

    private val df1 = DecimalFormat("#.#")

    /**
     * Affordable, accessible everyday staple food recommendations (Vegetarian & Non-Vegetarian).
     */
    val affordableStapleFoods = listOf(
        "Cooked Rice (White / Brown)",
        "Idli",
        "Dosa (Plain / Roast)",
        "Boiled Egg",
        "Moong Dal",
        "Chana / Chickpeas (Sundal / Boiled)",
        "Green Gram / Whole Moong Sprouts",
        "Roasted Peanuts / Groundnuts",
        "Curd / Yogurt (Thayir)",
        "Cow's Milk",
        "Banana",
        "Spinach / Palak",
        "Carrot Poriyal",
        "Beans Poriyal",
        "Tomato Rasam / Soup",
        "Onion & Cabbage Stir Fry",
        "Boiled Sweet Potato"
    )

    /**
     * Builds a detailed nutrition summary for active food items.
     */
    fun buildFoodNutritionBreakdown(
        foodContext: ActiveFoodContext,
        userProfile: UserProfile?
    ): String {
        val sb = StringBuilder()
        val items = foodContext.items

        if (items.isEmpty()) {
            return "No food items currently selected."
        }

        val primary = items.first()
        sb.append("### 🍽️ Meal Breakdown: **${primary.food.name}**\n\n")

        if (items.size > 1) {
            sb.append("**Detected Items:**\n")
            items.forEachIndexed { idx, item ->
                sb.append("• **${item.food.name}** (${item.portionSize.label}, ${df1.format(item.quantity)} serving) — *~${item.calculatedCalories} kcal, ${df1.format(item.calculatedProtein)}g protein*\n")
            }
            sb.append("\n")
        }

        sb.append("**Nutritional Estimation (Based on visible portion):**\n")
        sb.append("• **Calories:** ~${foodContext.totalCalories} kcal\n")
        sb.append("• **Protein:** ~${df1.format(foodContext.totalProtein)} g\n")
        sb.append("• **Carbohydrates:** ~${df1.format(foodContext.totalCarbs)} g\n")
        sb.append("• **Healthy Fats:** ~${df1.format(foodContext.totalFat)} g\n")
        sb.append("• **Dietary Fiber:** ~${df1.format(foodContext.totalFiber)} g\n")
        sb.append("• **Estimated Weight:** ~${items.sumOf { it.calculatedWeightGrams }} g\n\n")

        // Goal Fit Evaluation
        val goal = userProfile?.fitnessGoal ?: "General Fitness"
        sb.append("**Fitness Goal Alignment (${goal}):**\n")

        when {
            goal.contains("Loss", ignoreCase = true) || goal.contains("Fat", ignoreCase = true) -> {
                if (foodContext.totalCalories > 550) {
                    sb.append("• *Calorie Note:* This is a moderately calorie-dense meal (~${foodContext.totalCalories} kcal). If you are in a caloric deficit, consider pairing a smaller portion with high-fiber vegetables (cucumber, spinach, or cabbage) or light rasam/dal.\n")
                } else {
                    sb.append("• *Calorie Note:* Great fit for a calorie-conscious diet (~${foodContext.totalCalories} kcal) with satisfying satiety.\n")
                }
                if (foodContext.totalProtein < 15.0) {
                    sb.append("• *Protein Boost:* Adding 1-2 boiled eggs, a cup of curd, or 50g boiled moong sprouts can boost protein for better satiety and muscle retention.\n")
                }
            }
            goal.contains("Muscle", ignoreCase = true) || goal.contains("Gain", ignoreCase = true) -> {
                sb.append("• *Muscle Building:* Delivers ~${df1.format(foodContext.totalProtein)}g of protein. For optimal muscle synthesis, aim for 25-35g protein per meal. Pair this with 2 boiled eggs, roasted peanuts, curd, or paneer/soya chunks.\n")
            }
            else -> {
                sb.append("• *Balanced Living:* Provides a good mix of clean energy and macro nutrients. Ensure you stay well hydrated with water throughout the day.\n")
            }
        }

        // Affordable Alternatives & Upgrades
        sb.append("\n**Budget-Friendly & Nutrient-Dense Additions:**\n")
        val isVeg = userProfile?.dietPreference?.contains("Veg", ignoreCase = true) == true
        if (isVeg) {
            sb.append("• **Moong Sprouts / Boiled Chana:** High fiber and plant protein (~7-8g protein/100g, very low cost).\n")
            sb.append("• **Fresh Curd (Thayir):** Natural probiotics and easy protein (~4g protein/100g).\n")
            sb.append("• **Roasted Peanuts:** Energy-dense healthy fats and protein (~25g protein/100g).\n")
        } else {
            sb.append("• **Boiled Eggs:** High biological value protein (~6g protein & only ~70 kcal per whole egg).\n")
            sb.append("• **Moong Dal / Green Gram:** Rich in soluble fiber and clean amino acids.\n")
            sb.append("• **Curd or Buttermilk (Moru):** Excellent for digestion and hydration.\n")
        }

        sb.append("\n*Note: Nutritional values are approximate estimates based on standard recipe preparations and visual serving size.*")
        return sb.toString()
    }

    /**
     * Suggests affordable, high-protein meal alternatives.
     */
    fun getAffordableAlternatives(currentFoodName: String, isVeg: Boolean): List<String> {
        val alternatives = mutableListOf<String>()
        if (isVeg) {
            alternatives.add("2 Idlis + Sambar + 1 Cup Boiled Moong Sprouts (~15g protein)")
            alternatives.add("2 Wheat Chapatis + Thick Yellow Dal + Cucumber Salad (~14g protein)")
            alternatives.add("Curd Rice + Roasted Peanuts + Steamed Spinach Poriyal (~13g protein)")
            alternatives.add("Oats with Milk, Banana & 1 Spoon Groundnuts (~12g protein)")
        } else {
            alternatives.add("2 Boiled Eggs + 1 Plate Cooked Rice with Rasam (~16g protein)")
            alternatives.add("2 Idlis + Egg Bhurji / Omelette (~16g protein)")
            alternatives.add("2 Chapatis + Chicken Curry (Low Oil) + Green Salad (~22g protein)")
            alternatives.add("Curd Rice + 2 Boiled Eggs (~16g protein)")
        }
        return alternatives
    }
}
