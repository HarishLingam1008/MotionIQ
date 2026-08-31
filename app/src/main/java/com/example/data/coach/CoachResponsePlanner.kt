package com.example.data.coach

import com.example.data.coach.food.ActiveFoodContext
import java.text.DecimalFormat

object CoachResponsePlanner {

    private val df1 = DecimalFormat("#.#")
    private val df0 = DecimalFormat("#,###")

    fun planResponse(
        intentResult: IntentAnalysisResult,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider,
        history: List<Pair<String, String>>,
        activeFoodContext: ActiveFoodContext? = null
    ): String {
        // Handle Medical Safety Emergency First
        if (intentResult.primaryIntent == CoachIntent.MEDICAL_SAFETY) {
            val advisory = CoachSafetyFilter.checkEmergency(intentResult.normalizedQuery)
            return advisory ?: CoachResponseFormatter.format(
                directAnswer = "Please consult a healthcare professional for clinical or medication inquiries.",
                explanation = "MotionIQ provides fitness and lifestyle guidance for general wellness."
            )
        }

        return when (intentResult.primaryIntent) {
            CoachIntent.GREETING -> planGreeting(userData)
            CoachIntent.STEPS, CoachIntent.WALKING -> planStepsResponse(intentResult.secondaryIntent, entities, userData)
            CoachIntent.HYDRATION, CoachIntent.WATER -> planHydrationResponse(intentResult.secondaryIntent, entities, userData)
            CoachIntent.PROTEIN -> planProteinResponse(intentResult.secondaryIntent, entities, userData, activeFoodContext)
            CoachIntent.FOOD_IDENTIFICATION -> planFoodIdentificationResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_CALORIES -> planFoodCaloriesResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_PROTEIN -> planFoodProteinResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_CARBOHYDRATES -> planFoodCarbsResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_FAT -> planFoodFatResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_FIBER -> planFoodFiberResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_HEALTHINESS -> planFoodHealthinessResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_WEIGHT_LOSS -> planFoodWeightLossResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_WEIGHT_GAIN -> planFoodWeightGainResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_BREAKFAST -> planFoodBreakfastResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_LUNCH -> planFoodLunchResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_DINNER -> planFoodDinnerResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_PORTION -> planFoodPortionResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_ALTERNATIVE -> planFoodAlternativeResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_PREPARATION -> planFoodPreparationResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD_COMBINATION -> planFoodCombinationResponse(activeFoodContext, entities, userData)
            CoachIntent.FOOD, CoachIntent.BREAKFAST, CoachIntent.LUNCH, CoachIntent.DINNER, CoachIntent.SNACK,
            CoachIntent.PRE_WORKOUT, CoachIntent.POST_WORKOUT, CoachIntent.FRUITS, CoachIntent.VEGETABLES,
            CoachIntent.CARBOHYDRATES -> planFoodResponse(intentResult.primaryIntent, intentResult.secondaryIntent, entities, userData, activeFoodContext)
            CoachIntent.WEIGHT_LOSS -> planWeightLossResponse(entities, userData)
            CoachIntent.WEIGHT_GAIN -> planWeightGainResponse(entities, userData)
            CoachIntent.CALORIES -> planCaloriesResponse(intentResult.secondaryIntent, entities, userData)
            CoachIntent.BMI -> planBmiResponse(entities, userData)
            CoachIntent.FITNESS_SCORE -> planFitnessScoreResponse(userData)
            CoachIntent.ANALYTICS -> planAnalyticsResponse(intentResult.secondaryIntent, userData)
            CoachIntent.WORKOUT, CoachIntent.EXERCISE, CoachIntent.RUNNING -> planWorkoutResponse(intentResult.secondaryIntent, entities, userData)
            CoachIntent.SLEEP, CoachIntent.RECOVERY -> planRecoveryResponse(intentResult.secondaryIntent, userData)
            CoachIntent.GOALS -> planGoalsResponse(userData)
            CoachIntent.MOTIVATION -> planMotivationResponse(userData)
            CoachIntent.GENERAL_FITNESS -> planGeneralFitnessResponse(userData)
            CoachIntent.UNKNOWN -> CoachResponseFormatter.formatUnknown()
            else -> CoachResponseFormatter.formatUnknown()
        }
    }

    private fun planGreeting(userData: CoachUserDataProvider): String {
        return CoachResponseFormatter.format(
            directAnswer = "Hello ${userData.name}! I'm your MotionIQ AI Coach.",
            explanation = "Today you have completed ${df0.format(userData.steps)} of your ${df0.format(userData.stepGoal)} daily step goal (${df1.format(userData.stepProgressPercent)}%).",
            personalizedAction = if (userData.remainingSteps > 0) "A quick walk can help you cover the remaining ${df0.format(userData.remainingSteps)} steps." else "Great job hitting your step goal today!",
            followUp = "Ask me anything about your steps, workouts, calories, food, or hydration."
        )
    }

    private fun planStepsResponse(
        secondary: CoachSecondaryIntent,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val querySteps = entities.steps
        val evaluatedSteps = querySteps ?: userData.steps
        val targetGoal = entities.stepGoal ?: userData.stepGoal
        val remaining = (targetGoal - evaluatedSteps).coerceAtLeast(0)
        val percent = (evaluatedSteps.toDouble() / targetGoal.toDouble()) * 100.0

        return when (secondary) {
            CoachSecondaryIntent.PROGRESS_CHECK -> {
                val direct = if (evaluatedSteps >= targetGoal) {
                    "Step Progress Evaluation: You've achieved ${df0.format(evaluatedSteps)} steps ($evaluatedSteps), which exceeds your ${df0.format(targetGoal)}-step goal ($targetGoal) (${df1.format(percent)}%)!"
                } else {
                    "Step Progress Evaluation: You're at ${df0.format(evaluatedSteps)} steps ($evaluatedSteps) today, which is ${df1.format(percent)}% of your ${df0.format(targetGoal)}-step goal ($targetGoal)."
                }
                val explanation = if (remaining > 0) {
                    "You need about ${df0.format(remaining)} more steps ($remaining) to complete your daily target."
                } else {
                    "Consistent daily movement above ${df0.format(targetGoal)} steps significantly improves cardiovascular endurance and metabolic rate."
                }
                val action = if (remaining > 0) {
                    val walkMins = (remaining / 100).coerceIn(10, 45)
                    "A brisk ${walkMins}-minute walk will comfortably cover your remaining ${df0.format(remaining)} steps."
                } else {
                    "Focus on light post-dinner stretching and adequate hydration for optimal recovery."
                }
                CoachResponseFormatter.format(direct, explanation, action, "Would you like tips on increasing your daily step pace?")
            }
            CoachSecondaryIntent.HOW_TO -> {
                val direct = "To increase your daily steps steadily, aim to add 1,000 to 1,500 steps per day each week."
                val explanation = "Sudden large jumps in walking volume can cause calf tightness. Build sustainable habits:\n1. **Post-Meal Walks**: 10-minute walk after meals (+1,200 steps)\n2. **Pacing Phone Calls**: Pace while on calls (+800 steps)\n3. **Stairs**: Choose stairs over elevators."
                val action = "Take a 10-minute walk after meals, pace during phone calls, and choose stairs over elevators."
                CoachResponseFormatter.format(direct, explanation, action, "Would you like a suggested morning vs evening walking schedule?")
            }
            CoachSecondaryIntent.DAILY_TARGET -> {
                val direct = "A healthy daily target for most active adults is between 8,000 and 10,000 steps (Your custom goal: ${df0.format(userData.stepGoal)} / ${userData.stepGoal} steps). Currently logged: ${df0.format(userData.steps)} (${userData.steps} steps)."
                val explanation = "Reaching 8,000–10,000 daily steps is clinically linked with improved insulin sensitivity, reduced resting blood pressure, and sustained energy balance."
                val action = "Currently you are at ${df0.format(userData.steps)} steps (${df1.format(userData.stepProgressPercent)}%). Try completing a 15-minute walk this afternoon."
                CoachResponseFormatter.format(direct, explanation, action)
            }
            CoachSecondaryIntent.CALCULATION -> {
                val direct = "You've completed ${df0.format(userData.steps)} (${userData.steps}) of your ${df0.format(userData.stepGoal)} (${userData.stepGoal})-step goal. Remaining: ${df0.format(userData.remainingSteps)} (${userData.remainingSteps}) steps needed today."
                val explanation = "At an average walking cadence of 100 steps/minute, ${df0.format(userData.remainingSteps)} steps takes approximately ${(userData.remainingSteps / 100)} minutes."
                val action = "A short evening walk will help close this gap."
                CoachResponseFormatter.format(direct, explanation, action)
            }
            else -> {
                val direct = "You have completed ${df0.format(userData.steps)} steps (${userData.steps}) today (${df1.format(userData.stepProgressPercent)}% of your ${df0.format(userData.stepGoal)} target)."
                val explanation = "Daily walking stimulates lymphatic circulation, burns active calories, and lowers post-meal glucose spikes."
                val action = if (userData.remainingSteps > 0) "Complete ${df0.format(userData.remainingSteps)} more steps to finish strong today." else "Great milestone reached today!"
                CoachResponseFormatter.format(direct, explanation, action)
            }
        }
    }

    private fun planHydrationResponse(
        secondary: CoachSecondaryIntent,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val queryWater = entities.waterMl
        val intake = queryWater ?: userData.waterIntakeMl
        val goal = userData.waterGoalMl
        val remaining = (goal - intake).coerceAtLeast(0)
        val percent = (intake.toDouble() / goal.toDouble()) * 100.0

        val direct = "Your daily hydration target is ${df0.format(goal)} ml (~${df1.format(goal / 1000.0)} L). You've logged ${df0.format(intake)} ml so far (${df1.format(percent)}%)."
        val explanation = if (remaining > 0) {
            "You need approximately ${df0.format(remaining)} ml more to reach full cellular hydration today. Drinking water consistently improves joint lubrication and muscle function."
        } else {
            "You have achieved your hydration goal for today! Maintaining fluid balance prevents cramping and mental fatigue."
        }
        val action = if (remaining > 0) {
            "Drink a glass of water (250–300 ml) now, and keep a bottle nearby for regular sips."
        } else {
            "Continue sipping water as needed, especially after any physical activity."
        }
        return CoachResponseFormatter.format(direct, explanation, action, "Did you do a workout or heavy walk today that requires extra fluids?")
    }

    private fun resolveActiveOrEntityFood(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities
    ): Pair<FoodNutrientInfo, Double>? {
        val activeItem = activeFoodContext?.primaryItem
        if (activeItem != null) {
            return Pair(activeItem.food, activeItem.totalMultiplier)
        }
        val entityFood = entities.foodItem
        if (entityFood != null) {
            return Pair(entityFood, entities.foodQuantity)
        }
        return null
    }

    private fun planFoodIdentificationResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please upload a photo of the food or specify the food item you would like to analyze."
        val (food, mult) = resolved
        val conf = activeFoodContext?.primaryItem?.confidencePercent ?: 84
        val itemsCount = activeFoodContext?.items?.size ?: 1

        val direct = if (itemsCount > 1) {
            val names = activeFoodContext!!.items.joinToString(" + ") { it.food.name }
            "Identified Meal Composition: **$names** (~${activeFoodContext.totalCalories} kcal total)."
        } else {
            "Identified Food: **${food.name}** (Confidence: ${conf}%)."
        }

        val explanation = if (itemsCount > 1) {
            "Meal breakdown:\n" + activeFoodContext!!.items.joinToString("\n") {
                "• **${it.food.name}** (${it.portionSize.label} x ${df1.format(it.quantity)}): ${it.calculatedCalories} kcal | ${df1.format(it.calculatedProtein)}g protein | ${df1.format(it.calculatedCarbs)}g carbs"
            } + "\n\n${food.description}"
        } else {
            "**Description**: ${food.description}\n\n• **Standard Serving**: ${food.portion}\n• **Hydration**: ${food.hydrationNote}\n• **Health Profile**: ${food.healthNote}"
        }

        val action = "Estimated nutrition is calculated based on local database metrics. You can tap 'Choose food manually' if you wish to adjust the selection or portion."
        return CoachResponseFormatter.format(direct, explanation, action, "Would you like calorie details, weight loss tips, or healthy side recommendations?")
    }

    private fun planFoodCaloriesResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item (e.g., 'Calories in a banana') or upload a photo of your meal."
        val (food, mult) = resolved
        val cal = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalCalories else (food.caloriesKcal * mult).toInt()
        val prot = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalProtein else food.proteinG * mult)
        val carbs = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalCarbs else food.carbsG * mult)
        val fat = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFat else food.fatG * mult)
        val fiber = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFiber else food.fiberG * mult)

        val direct = "Estimated Energy: ~**${cal} kcal** (${food.name})."
        val explanation = """
            **Estimated Nutrition Breakdown**:
            • **Calories**: ~${cal} kcal
            • **Protein**: ${prot}g
            • **Carbohydrates**: ${carbs}g
            • **Fat**: ${fat}g
            • **Dietary Fiber**: ${fiber}g
            • **Hydration**: ${food.hydrationNote}
        """.trimIndent()

        val burnPercent = if (userData.caloriesBurned > 0) df1.format((cal.toDouble() / userData.caloriesBurned) * 100.0) else "0"
        val action = "Today you have actively burned ${df0.format(userData.caloriesBurned)} kcal (${userData.steps} steps). Pair this meal with vegetables to maintain stable blood sugar."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodProteinResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item (e.g., 'Protein in eggs') or upload a photo of your meal."
        val (food, mult) = resolved
        val totalProt = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalProtein else food.proteinG * mult
        val (protLow, protHigh) = userData.recommendedProteinGrams

        val direct = "Estimated Protein: ~**${df1.format(totalProt)}g protein** (${food.name})."
        val pairings = food.complementaryFoods.joinToString("\n") { "• **$it**" }
        val explanation = """
            ${food.proteinInfo}

            **Your Daily Protein Goal**: ${protLow}g–${protHigh}g (based on your ${df1.format(userData.weightKg)} kg body weight).
            This serving fulfills approx ${df1.format((totalProt / protLow.toDouble()) * 100.0)}% of your base daily protein requirement.
        """.trimIndent()

        val action = "Top affordable foods to pair with this for extra protein:\n$pairings"
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodCarbsResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check carbohydrates."
        val (food, mult) = resolved
        val totalCarbs = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalCarbs else food.carbsG * mult
        val totalFiber = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFiber else food.fiberG * mult

        val direct = "Carbohydrate Content: ~**${df1.format(totalCarbs)}g carbs** (~${df1.format(totalFiber)}g dietary fiber)."
        val explanation = "${food.healthNote}\n\n${food.nutritionalConsiderations}"
        val action = "Eating fiber-rich vegetables or drinking warm water before the meal slows glucose uptake and avoids sudden energy spikes."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodFatResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check fat content."
        val (food, mult) = resolved
        val totalFat = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFat else food.fatG * mult

        val direct = "Estimated Fat: ~**${df1.format(totalFat)}g fat** (${food.name})."
        val explanation = "${food.nutritionalConsiderations}\n\nCooking method (pan spray vs deep-frying or excess ghee) makes the biggest difference in total fat."
        val action = "Use a well-seasoned non-stick or cast-iron pan with 1/2 tsp oil to keep fat low without losing flavor."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodFiberResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check dietary fiber."
        val (food, mult) = resolved
        val totalFiber = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFiber else food.fiberG * mult

        val direct = "Dietary Fiber: ~**${df1.format(totalFiber)}g fiber** (${food.name})."
        val explanation = "Dietary fiber supports healthy bowel movements, fuels beneficial gut probiotics, and lowers blood LDL cholesterol."
        val action = "Ensure 1-2 glasses of water alongside this meal for comfortable fiber digestion."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodHealthinessResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check health benefits."
        val (food, _) = resolved
        val direct = "Health Profile: Yes, **${food.name}** (~${food.caloriesKcal} kcal) is an excellent, nourishing food choice."
        val tips = food.healthyCookingTips.joinToString("\n") { "• $it" }
        val explanation = """
            • **Health Benefits**: ${food.healthNote}
            • **Hydration**: ${food.hydrationNote}
            • **Protein Value**: ${food.proteinInfo}
            • **Considerations**: ${food.nutritionalConsiderations}

            **Healthy Cooking Tips**:
            $tips
        """.trimIndent()
        val action = "Pair with colorful vegetables or a protein side for complete macronutrient balance."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodWeightLossResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to evaluate its fit for weight loss."
        val (food, mult) = resolved
        val cal = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalCalories else (food.caloriesKcal * mult).toInt()

        val direct = "Weight Loss Guidance: ${food.weightLossAdvice}"
        val explanation = """
            • **Calorie Impact**: ~${cal} kcal.
            • **Satiety Strategy**: ${food.healthNote}
            • **Macronutrient Balance**: Combining this with high-protein and high-fiber sides (like sambar, egg whites, or salad) prolongs satiety and prevents evening sugar cravings.
        """.trimIndent()
        val action = "Keep serving size controlled (1-2 pieces) and fill half your plate with cucumber, greens, or dal."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodWeightGainResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to evaluate its fit for weight gain."
        val (food, _) = resolved
        val direct = "Weight Gain & Bulking Strategy: ${food.weightGainAdvice}"
        val explanation = """
            To gain lean muscle mass sustainably:
            • Combine ${food.name} with calorie-dense healthy sides like whole eggs, peanut butter, whole milk, curd, or paneer.
            • Maintain a slight daily calorie surplus (250–400 kcal above maintenance) with progressive strength workouts.
        """.trimIndent()
        val action = "Add 1 glass of milk or buttermilk with a handful of roasted peanuts to this meal."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodBreakfastResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check if it is suitable for breakfast."
        val (food, _) = resolved
        val isGood = food.mealSuitability.contains("BREAKFAST")
        val direct = if (isGood) {
            "Yes! **${food.name}** is an excellent, energizing choice for breakfast."
        } else {
            "**${food.name}** can be consumed for breakfast, though lighter portions are recommended."
        }
        val explanation = "${food.mealTimingAdvice}\n\n${food.healthNote}"
        val action = "Drink a glass of water or herbal tea before eating to awaken digestive peristalsis."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodLunchResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check if it is suitable for lunch."
        val (food, _) = resolved
        val direct = "Lunch Recommendation: **${food.name}** provides steady midday energy to power your afternoon activity."
        val explanation = "${food.description}\n\n${food.nutritionalConsiderations}"
        val action = "Include a vegetable salad or poriyal and 1 cup of curd or buttermilk."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodDinnerResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to check if it is suitable for dinner."
        val (food, _) = resolved
        val isGood = food.mealSuitability.contains("DINNER")
        val direct = if (isGood) {
            "Yes! **${food.name}** is well-suited for dinner."
        } else {
            "For dinner, keep **${food.name}** to a moderate portion to ensure smooth nighttime digestion."
        }
        val explanation = "${food.mealTimingAdvice}\n\nFinish dinner at least 2 hours before bedtime so gastrointestinal motility doesn't interfere with restorative deep sleep."
        val action = "Take a light 10-minute post-dinner walk (${df0.format(userData.steps)} steps currently logged today)."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodPortionResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to get portion guidance."
        val (food, _) = resolved
        val direct = "Recommended Serving Size: Standard baseline portion is **${food.portion}** (~${food.caloriesKcal} kcal)."
        val explanation = """
            • **For Weight Loss / Cutting**: 1 serving (${food.portion}) paired with generous low-calorie vegetables.
            • **For Maintenance**: 1.5–2 servings.
            • **For Weight / Muscle Gain**: 2–3 servings accompanied by a high-protein side.
        """.trimIndent()
        val action = "Use the portion selector chips in the Food Analysis Card to adjust between Small (0.75x), Medium (1.0x), and Large (1.5x)."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodAlternativeResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to find healthy alternatives."
        val (food, _) = resolved
        val direct = "Healthy & Budget-Friendly Alternatives to ${food.name}:"
        val altList = CoachFoodDatabase.getAffordableProteinFoods(isVeg = food.isVegetarian).take(4)
        val explanation = altList.joinToString("\n") {
            "• **${it.name}**: ${it.caloriesKcal} kcal | ${df1.format(it.proteinG)}g protein | ${df1.format(it.fiberG)}g fiber per ${it.portion}"
        }
        val action = "Tap 'Choose food manually' to search and pick any alternative from our 45+ item database."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodPreparationResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to get healthy cooking tips."
        val (food, _) = resolved
        val direct = "Healthy Preparation Guide for **${food.name}**:"
        val tips = food.healthyCookingTips.joinToString("\n") { "• $it" }
        val explanation = """
            $tips
            • **Oil Control**: Each tablespoon of oil adds 120 kcal. Use an oil sprayer or brush.
            • **Spice Fortification**: Turmeric, cumin, black pepper, and ginger provide anti-inflammatory antioxidants.
        """.trimIndent()
        val action = "Try these cooking adjustments to cut calories while keeping full taste and satisfaction."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFoodCombinationResponse(
        activeFoodContext: ActiveFoodContext?,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
            ?: return "Please specify the food item or attach a photo to view recommended pairings."
        val (food, _) = resolved
        val direct = "Top Recommended Affordable Pairings for **${food.name}**:"
        val pairs = food.complementaryFoods.joinToString("\n") { "• **$it**" }
        val explanation = """
            $pairs

            **Why Pairings Matter**:
            Combining carbohydrate sources with protein and fiber lowers the overall glycemic index of the meal, resulting in longer satiety and steady all-day energy.
        """.trimIndent()
        val action = "You can tap '+ Add another food item' in the Food Card to track combined meal nutrition."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planProteinResponse(
        secondary: CoachSecondaryIntent,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider,
        activeFoodContext: ActiveFoodContext? = null
    ): String {
        if (activeFoodContext?.primaryItem != null || entities.foodItem != null) {
            return planFoodProteinResponse(activeFoodContext, entities, userData)
        }
        val isVeg = entities.isVegetarian
        val isAffordable = entities.isAffordable
        val (protLow, protHigh) = userData.recommendedProteinGrams

        val direct = "For your weight (${df1.format(userData.weightKg)} kg), your recommended daily protein intake is ${protLow}g to ${protHigh}g (1.2–1.8g per kg)."
        val foodList = if (isVeg || isAffordable) {
            "• **Soya Chunks**: 26g protein per 50g dry (Budget superfood)\n" +
            "• **Boiled Eggs**: 12.6g protein per 2 eggs (Complete amino profile)\n" +
            "• **Green Gram / Moong Sprouts**: 10g protein per cup (Easy to digest)\n" +
            "• **Roasted Gram (Pottukadalai)**: 6g protein per small bowl (Instant snack)\n" +
            "• **Cooked Dal / Sambar**: 8.5g protein per bowl\n" +
            "• **Paneer / Curd**: 18g per 100g paneer / 4.5g per cup curd"
        } else {
            "• **Chicken Breast**: 31g protein per 100g\n" +
            "• **Boiled Whole Eggs**: 13g protein (2 eggs)\n" +
            "• **Fish**: 20g protein per 100g (Plus Omega-3s)\n" +
            "• **Soya Chunks / Paneer**: 26g / 18g protein\n" +
            "• **Lentils & Chickpeas**: 9–14g protein per cup"
        }

        val explanation = "Spreading your protein intake evenly across 3 to 4 meals maximizes muscle protein synthesis and keeps you full."
        val action = "Include at least one high-protein source in your next meal (e.g. 2 boiled eggs or 1 cup of boiled sundal/dal)."
        return CoachResponseFormatter.format(direct, "$explanation\n\n**Top Recommended Protein Sources:**\n$foodList", action, "Would you like a vegetarian high-protein meal breakdown?")
    }

    private fun planFoodResponse(
        primary: CoachIntent,
        secondary: CoachSecondaryIntent,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider,
        activeFoodContext: ActiveFoodContext? = null
    ): String {
        // 1. Check Meal / Workout Timing Recommendations first if not a specific food analysis
        val mealType = when (primary) {
            CoachIntent.BREAKFAST -> "BREAKFAST"
            CoachIntent.LUNCH -> "LUNCH"
            CoachIntent.DINNER -> "DINNER"
            CoachIntent.SNACK -> "SNACK"
            CoachIntent.PRE_WORKOUT -> "PRE_WORKOUT"
            CoachIntent.POST_WORKOUT -> "POST_WORKOUT"
            else -> if (secondary == CoachSecondaryIntent.POST_WORKOUT) "POST_WORKOUT" else if (secondary == CoachSecondaryIntent.PRE_WORKOUT) "PRE_WORKOUT" else null
        }

        if (mealType != null && activeFoodContext?.primaryItem == null) {
            return when (mealType) {
                "BREAKFAST" -> {
                    val direct = "Breakfast Options: A nourishing breakfast should combine complex carbohydrates, dietary fiber, and 15–20g of protein."
                    val options = "• **Option 1**: 3 Idlis + 1 bowl Vegetable Sambar + 1 Boiled Egg (or 1 small cup Curd) (~280 kcal)\n" +
                                  "• **Option 2**: 2 Ragi Dosas + Mint/Tomato Chutney + Sprouted Moong (~310 kcal)\n" +
                                  "• **Option 3**: 1 bowl Vegetable Oats + 2 Egg Whites (~220 kcal)"
                    val action = "Avoid sugary beverages; pair your breakfast with water, green tea, or warm buttermilk."
                    CoachResponseFormatter.format(direct, options, action)
                }
                "LUNCH" -> {
                    val direct = "Lunch Options: Structure your plate with 50% fiber-rich vegetables, 25% complex carbs, and 25% protein."
                    val options = "• **Plate Setup**: 1 cup Cooked Rice or 2 Phulkas (Chapati) + 1 bowl Dal/Sambar + 1 cup Cabbage/Spinach/Beans Poriyal + 1 cup Curd (~420 kcal)\n" +
                                  "• **Protein Boost**: Add grilled chicken breast (100g), boiled soya chunks (50g), or boiled chana sundal."
                    val action = "Eat your vegetables and protein first to lower the post-meal glycemic spike."
                    CoachResponseFormatter.format(direct, options, action)
                }
                "DINNER" -> {
                    val direct = "Dinner Recommendations: Keep dinner light, affordable, and easily digestible (consumed at least 2 hours before bedtime)."
                    val options = "• **Option 1 (Affordable & Light)**: 2 Whole Wheat Chapatis with 1 bowl Mixed Vegetable Dal & Bottle Gourd Sabzi (~280 kcal)\n" +
                                  "• **Option 2 (Vegetarian)**: 2 Ragi Rotis/Dosas + Drumstick Sambar + Sprouted Moong Sundal (~270 kcal)\n" +
                                  "• **Option 3**: Warm bowl of Vegetable Soup + 100g lightly sauteed Paneer or Tofu (~290 kcal)"
                    val action = "Take a gentle 10-minute stroll after dinner to assist digestion."
                    CoachResponseFormatter.format(direct, options, action)
                }
                "POST_WORKOUT" -> {
                    val direct = "Post-Workout Nutrition & Recovery: After exercise, consume 15–25g of fast-acting protein with carbohydrates within 45 minutes to restore muscle glycogen and accelerate recovery."
                    val options = "• 2 Boiled Eggs + 1 Medium Banana (~250 kcal, 14g protein)\n" +
                                  "• 1 cup Sprouted Moong Sundal or Boiled Chickpeas (~190 kcal, 11g protein)\n" +
                                  "• 1 glass Toned Milk / Buttermilk with a handful of Roasted Peanuts (~200 kcal, 11g protein)"
                    val action = "Rehydrate with 350–500 ml of water immediately following your workout."
                    CoachResponseFormatter.format(direct, options, action)
                }
                "PRE_WORKOUT" -> {
                    val direct = "Pre-Workout Nutrition: 30–45 minutes before a workout, eat easily digestible light carbohydrates for sustained stamina."
                    val options = "• 1 Medium Banana (~105 kcal)\n" +
                                  "• 1 Boiled Sweet Potato (~112 kcal)\n" +
                                  "• 1 small handful of Roasted Gram (Pottukadalai) or 1 slice whole wheat toast with peanut butter"
                    val action = "Drink 200–300 ml of water to start your workout properly hydrated."
                    CoachResponseFormatter.format(direct, options, action)
                }
                "SNACK" -> {
                    val direct = "Healthy Snacks: Choose nutrient-dense, high-fiber snacks that suppress hunger between main meals."
                    val options = "• 1 Medium Guava or Papaya slices (High Vitamin C & fiber, < 70 kcal)\n" +
                                  "• 1 small cup Boiled Kala Chana or Sundal (~160 kcal, 8.5g protein)\n" +
                                  "• 1 handful Roasted Peanuts or Bhuna Chana (~140 kcal)\n" +
                                  "• 1 Whole Cucumber with chili & lemon (< 30 kcal)"
                    val action = "Drink a glass of water before snacking to distinguish thirst from true hunger."
                    CoachResponseFormatter.format(direct, options, action)
                }
                else -> {
                    val direct = "Wholesome Nutrition: Focus on whole, minimally processed regional foods with balanced macronutrients."
                    val options = "Staples like ragi, brown rice, dal, fresh vegetables (spinach, bottle gourd, cabbage, carrots), curd, and eggs provide sustained vital energy."
                    val action = "Aim for colorful plates containing at least 2 vegetable varieties daily."
                    CoachResponseFormatter.format(direct, options, action)
                }
            }
        }

        // Specific Food Nutrient Query
        if (activeFoodContext?.primaryItem != null || entities.foodItem != null) {
            val resolved = resolveActiveOrEntityFood(activeFoodContext, entities)
                ?: return "Please specify the food item or upload a clear photo of the food."
            val (food, mult) = resolved
            val cal = if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalCalories else (food.caloriesKcal * mult).toInt()
            val prot = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalProtein else food.proteinG * mult)
            val carbs = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalCarbs else food.carbsG * mult)
            val fat = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFat else food.fatG * mult)
            val fiber = df1.format(if (activeFoodContext != null && activeFoodContext.items.isNotEmpty()) activeFoodContext.totalFiber else food.fiberG * mult)

            val qtyStr = if (mult == 1.0) food.portion else "${df1.format(mult)}x portion (${food.portion})"
            val direct = "**${food.name}** ($qtyStr):\n• **Calories**: $cal kcal\n• **Protein**: ${prot}g\n• **Carbohydrates**: ${carbs}g\n• **Fat**: ${fat}g\n• **Fiber**: ${fiber}g"
            val explanation = "${food.description}\n\n• **Hydration**: ${food.hydrationNote}\n• **Health Profile**: ${food.healthNote}"
            val action = "Pair ${food.name} with fresh vegetables or a lean protein source for a balanced macronutrient profile."
            return CoachResponseFormatter.format(direct, explanation, action)
        }

        // Specific Fruit & Vegetable Responses
        if (primary == CoachIntent.VEGETABLES) {
            val direct = "Vegetables: Incorporating 2–3 cups of nutrient-dense vegetables daily delivers essential fiber, vitamins, and minerals."
            val options = "• **Spinach (Palak / Keerai)**: Rich in plant iron, magnesium, and folate (~41 kcal/cup)\n" +
                          "• **Bottle Gourd (Lauki / Sorakkai)**: 96% water content, highly cooling and prevents water retention (~25 kcal/cup)\n" +
                          "• **Green Beans**: High volume dietary fiber with minimal calories (~44 kcal/cup)\n" +
                          "• **Carrot & Cucumber**: Beta-carotene and hydration powerhouse (~30 kcal)"
            val action = "Add a generous bowl of vegetable poriyal or salad to both lunch and dinner."
            return CoachResponseFormatter.format(direct, options, action)
        }

        if (primary == CoachIntent.FRUITS) {
            val direct = "Fruits: Whole fresh fruits provide antioxidants, dietary fiber, and natural hydration."
            val options = "• **Guava**: 68 kcal, 5.4g fiber, rich in Vitamin C and low glycemic index\n" +
                          "• **Banana**: 105 kcal, 27g clean carbohydrates, ideal for workout energy\n" +
                          "• **Papaya**: 62 kcal/cup, contains digestive papain enzymes\n" +
                          "• **Apple**: 95 kcal, 4.4g soluble pectin fiber"
            val action = "Eat whole fresh fruits rather than fruit juice to retain intact dietary fiber."
            return CoachResponseFormatter.format(direct, options, action)
        }

        val direct = "Wholesome Nutrition: Focus on whole, minimally processed regional foods with balanced macronutrients."
        val options = "Staples like ragi, brown rice, dal, fresh vegetables (spinach, bottle gourd, cabbage, carrots), curd, and eggs provide sustained vital energy."
        val action = "Aim for colorful plates containing at least 2 vegetable varieties daily."
        return CoachResponseFormatter.format(direct, options, action)
    }

    private fun planWeightLossResponse(entities: ExtractedEntities, userData: CoachUserDataProvider): String {
        val targetKcal = userData.weightLossTargetKcal
        val tdee = userData.tdeeKcal
        val direct = "Sustainable fat loss requires a moderate calorie deficit of 400–500 kcal below your TDEE (Total Daily Energy Expenditure: ${df0.format(tdee)} kcal)."
        val explanation = "Your recommended daily intake for healthy weight loss is **${df0.format(targetKcal)} kcal/day**. Crash diets slow down thyroid function and cause muscle loss."
        val action = "Combine this calorie target with ${df0.format(userData.stepGoal)} daily steps and 20g of protein per meal to preserve lean muscle."
        return CoachResponseFormatter.format(direct, explanation, action, "Would you like a sample 1,600 kcal daily South Indian meal plan?")
    }

    private fun planWeightGainResponse(entities: ExtractedEntities, userData: CoachUserDataProvider): String {
        val targetKcal = userData.weightGainTargetKcal
        val direct = "To build lean muscle and gain weight sustainably, aim for a clean calorie surplus of 300–400 kcal above maintenance (**${df0.format(targetKcal)} kcal/day**)."
        val explanation = "Focus on nutrient-dense calorie sources: whole eggs, paneer, peanuts, bananas, oats, curd, and milk rather than refined junk foods."
        val action = "Add 2 high-calorie snacks daily (e.g. banana with peanut butter or a glass of milk with roasted gram)."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planCaloriesResponse(
        secondary: CoachSecondaryIntent,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val burned = userData.caloriesBurned
        val bmr = userData.bmrKcal
        val tdee = userData.tdeeKcal
        val deficitLoss = userData.weightLossTargetKcal

        val direct = "You've burned approximately **${df0.format(burned)} active kcal** today from movement and exercise."
        val explanation = "• **BMR (Basal Metabolic Rate)**: ${df0.format(bmr)} kcal (Calories burned at complete rest)\n" +
                          "• **TDEE (Total Daily Expenditure)**: ${df0.format(tdee)} kcal (Maintenance calories)\n" +
                          "• **Fat Loss Target**: ${df0.format(deficitLoss)} kcal/day (Safe 500 kcal deficit)\n" +
                          "• **Muscle Gain Target**: ${df0.format(userData.weightGainTargetKcal)} kcal/day"
        val action = "Ensure your daily nutrition supports your energy expenditure without extreme calorie cutting."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planBmiResponse(entities: ExtractedEntities, userData: CoachUserDataProvider): String {
        val weight = entities.weightKg ?: userData.weightKg
        val height = entities.heightCm ?: userData.heightCm
        val heightM = height / 100.0
        val calculatedBmi = (weight / (heightM * heightM) * 100.0).toInt() / 100.0
        val category = when {
            calculatedBmi < 18.5 -> "Underweight"
            calculatedBmi in 18.5..24.99 -> "Normal weight"
            calculatedBmi in 25.0..29.99 -> "Overweight"
            else -> "Obese"
        }

        val direct = "For weight **${df1.format(weight)} kg** and height **${df1.format(height)} cm**, your calculated BMI is **$calculatedBmi** (${df1.format(calculatedBmi)}) (**$category**)."
        val explanation = "BMI = Weight (kg) / [Height (m)]². Normal range is 18.5 to 24.9. Note that BMI is a general population screening metric and does not differentiate between skeletal muscle and adipose body fat."
        val action = if (calculatedBmi >= 25.0) {
            "Aim for a consistent 500 kcal deficit and 8,000+ daily steps to gradually optimize body composition."
        } else {
            "Maintain your current balanced nutrition and consistent daily activity levels."
        }
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planFitnessScoreResponse(userData: CoachUserDataProvider): String {
        val direct = "Your MotionIQ Fitness Score evaluates four core pillars: Daily Steps, Active vs Sedentary Minutes, Hydration Progress, and 7-Day Consistency."
        val explanation = "• **Steps**: ${df0.format(userData.steps)} / ${df0.format(userData.stepGoal)} (${df1.format(userData.stepProgressPercent)}%)\n" +
                          "• **Active Time**: ${userData.activeMinutes} mins active | ${userData.inactiveMinutes} mins sedentary\n" +
                          "• **Hydration**: ${df0.format(userData.waterIntakeMl)} / ${df0.format(userData.waterGoalMl)} ml\n" +
                          "• **Consistency**: ${userData.getWeeklyConsistencyPercent()}% weekly goal adherence"
        val action = if (userData.stepProgressPercent < 70) {
            "Taking a 20-minute walk now and drinking a glass of water will directly boost your score."
        } else {
            "Keep up your consistent activity to maintain a high performance rating."
        }
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planAnalyticsResponse(secondary: CoachSecondaryIntent, userData: CoachUserDataProvider): String {
        val history = userData.weeklyActivities
        if (history.isEmpty()) {
            return CoachResponseFormatter.format(
                directAnswer = "Not enough historical data is available yet.",
                explanation = "MotionIQ records your daily activity automatically. As you log steps and workouts over the coming days, your weekly performance trends and personal bests will appear here.",
                personalizedAction = "Complete your step goal today to record your first performance baseline."
            )
        }

        val avgSteps = userData.getWeeklyStepAverage()
        val bestDay = userData.getBestStepDay()
        val consistency = userData.getWeeklyConsistencyPercent()

        val direct = "Over the past week, your daily average is **${df0.format(avgSteps)} steps**, with a **$consistency% goal consistency rate**."
        val explanation = if (bestDay != null) {
            "Your peak performance day recorded **${df0.format(bestDay.steps)} steps** (${bestDay.date})."
        } else {
            "Consistent daily movement is key to long-term aerobic conditioning."
        }
        val action = "Aim to maintain at least a 5-day streak above your ${df0.format(userData.stepGoal)}-step goal."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planWorkoutResponse(
        secondary: CoachSecondaryIntent,
        entities: ExtractedEntities,
        userData: CoachUserDataProvider
    ): String {
        val isHome = entities.isAtHome || entities.rawNormalizedText.contains("home")
        val direct = if (isHome) {
            "Yes! You can get an effective, fat-burning full-body workout at home without any equipment in just 20 minutes."
        } else {
            "A balanced weekly workout routine combines cardiovascular aerobic training with progressive resistance exercises."
        }

        val circuit = """
            **20-Minute Home Circuit (3 Rounds, 45s work / 15s rest):**
            1. **Bodyweight Squats**: 15–20 reps (Quads & Glutes)
            2. **Push-ups or Knee Push-ups**: 10–12 reps (Chest, Shoulders & Triceps)
            3. **Reverse Lunges**: 10 reps each leg (Leg stability & balance)
            4. **Glute Bridges**: 15 reps (Posterior chain)
            5. **Plank Hold**: 30–45 seconds (Core stability)
        """.trimIndent()

        val action = "Do a 3-minute dynamic warm-up (arm circles, high knees) before beginning, and stretch your hamstrings and quads afterward."
        return CoachResponseFormatter.format(direct, circuit, action, "Would you like a beginner or intermediate variation of this routine?")
    }

    private fun planRecoveryResponse(secondary: CoachSecondaryIntent, userData: CoachUserDataProvider): String {
        val direct = "Optimal physical recovery requires adequate sleep (7–9 hours), muscle rehydration, and gentle active recovery."
        val explanation = """
            • **Active Mobility**: 10 minutes of gentle walking or foam rolling increases blood flow and flushes metabolic waste.
            • **Hydration & Electrolytes**: Drink water with a pinch of salt or fresh coconut water/buttermilk to replenish potassium and magnesium.
            • **Muscle Rest**: Ensure at least 48 hours of recovery between intense training sessions for the same muscle group.
        """.trimIndent()
        val action = "Perform 5 minutes of static hamstring, calf, and hip flexor stretches tonight before bed."
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planGoalsResponse(userData: CoachUserDataProvider): String {
        val direct = "Your current daily targets are **${df0.format(userData.stepGoal)} steps** and **${df0.format(userData.waterGoalMl)} ml of water**."
        val explanation = "You are currently at ${df0.format(userData.steps)} steps (${df1.format(userData.stepProgressPercent)}%) and ${df0.format(userData.waterIntakeMl)} ml of water (${df1.format(userData.waterProgressPercent)}%)."
        val action = if (userData.remainingSteps > 0) "You need ${df0.format(userData.remainingSteps)} more steps to finish your daily goal." else "Daily goal achieved!"
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planMotivationResponse(userData: CoachUserDataProvider): String {
        val direct = "Every step you take today compounds into long-term cardiovascular health, vitality, and metabolic strength."
        val explanation = "Consistency beats intensity every time. You've already taken ${df0.format(userData.steps)} steps today."
        val action = if (userData.remainingSteps > 0) "Take a 10-minute walking break now to make progress toward your ${df0.format(userData.stepGoal)}-step goal." else "Celebrate your milestone today and rest well!"
        return CoachResponseFormatter.format(direct, explanation, action)
    }

    private fun planGeneralFitnessResponse(userData: CoachUserDataProvider): String {
        val direct = "A sustainable fitness lifestyle rests on 4 pillars: Daily Steps (7,000–10,000), Balanced Whole Foods (Adequate Protein & Fiber), Daily Hydration (2.5L+), and 7–9 Hours of Sleep."
        val explanation = "Tracking your daily habits with MotionIQ helps build consistent, lifelong healthy routines without extreme burnout."
        val action = "Pick one pillar to focus on today—for example, completing your remaining ${df0.format(userData.remainingSteps)} steps."
        return CoachResponseFormatter.format(direct, explanation, action, "What area would you like to improve first?")
    }
}
