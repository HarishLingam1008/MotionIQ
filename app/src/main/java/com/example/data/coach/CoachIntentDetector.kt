package com.example.data.coach

data class IntentAnalysisResult(
    val primaryIntent: CoachIntent,
    val secondaryIntent: CoachSecondaryIntent,
    val confidence: Double = 1.0,
    val isAmbiguous: Boolean = false,
    val normalizedQuery: String = ""
)

object CoachIntentDetector {

    fun detectIntent(
        normalizedQuery: String,
        entities: ExtractedEntities,
        conversationContext: Pair<CoachIntent?, CoachSecondaryIntent?>
    ): IntentAnalysisResult {
        val q = normalizedQuery.lowercase().trim()

        // 0. Check Contextual Follow-up Resolution first if query is short / ambiguous
        if ((conversationContext.first != null || conversationContext.second != null) &&
            (q.split(" ").size <= 5 || q.startsWith("what about") || q.startsWith("how many should i") || q.contains("cheaper") || q.contains("enough"))) {
            return IntentAnalysisResult(
                primaryIntent = conversationContext.first ?: CoachIntent.GENERAL_FITNESS,
                secondaryIntent = conversationContext.second ?: CoachSecondaryIntent.GENERAL,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 1. Safety Check
        if (CoachSafetyFilter.checkEmergency(q) != null) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.MEDICAL_SAFETY,
                secondaryIntent = CoachSecondaryIntent.TROUBLESHOOTING,
                confidence = 1.0,
                normalizedQuery = normalizedQuery
            )
        }

        // 2. Greetings
        if (q in listOf("hi", "hello", "hey", "vanakkam", "namaste", "good morning", "good evening", "hi coach", "hello coach")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.GREETING,
                secondaryIntent = CoachSecondaryIntent.GENERAL,
                confidence = 1.0,
                normalizedQuery = normalizedQuery
            )
        }

        // 2.1 Food Specific Questions (Image Context or Specific Food Queries)
        val isFoodIdentification = q.contains("what is this") || q.contains("what food") || q.contains("identify this") ||
                q.contains("which food") || q.contains("identify food") || q == "what is it" || q == "what is this?"
        if (isFoodIdentification) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_IDENTIFICATION,
                secondaryIntent = CoachSecondaryIntent.EXPLANATION,
                confidence = 0.99,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodCalories = (q.contains("calorie") || q.contains("calories") || q.contains("kcal") || q.contains("energy")) &&
                (q.contains("this") || q.contains("in this") || q.contains("in that") || (q.contains("in") && entities.foodItem != null))
        if (isFoodCalories) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_CALORIES,
                secondaryIntent = CoachSecondaryIntent.CALCULATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodProtein = (q.contains("protein") || q.contains("amino")) &&
                (q.contains("this") || q.contains("in this") || q.contains("in that") || (q.contains("in") && entities.foodItem != null))
        if (isFoodProtein) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_PROTEIN,
                secondaryIntent = CoachSecondaryIntent.CALCULATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodCarbs = (q.contains("carb") || q.contains("carbs") || q.contains("carbohydrate") || q.contains("carbohydrates")) &&
                (q.contains("this") || q.contains("in this") || q.contains("in that") || (q.contains("in") && entities.foodItem != null))
        if (isFoodCarbs) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_CARBOHYDRATES,
                secondaryIntent = CoachSecondaryIntent.CALCULATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodFat = (q.contains("fat") || q.contains("oil") || q.contains("oily") || q.contains("lipid")) &&
                (q.contains("this") || q.contains("in this") || q.contains("in that") || (q.contains("in") && entities.foodItem != null))
        if (isFoodFat) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_FAT,
                secondaryIntent = CoachSecondaryIntent.CALCULATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodFiber = (q.contains("fiber") || q.contains("fibre") || q.contains("roughage")) &&
                (q.contains("this") || q.contains("in this") || q.contains("in that") || (q.contains("in") && entities.foodItem != null))
        if (isFoodFiber) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_FIBER,
                secondaryIntent = CoachSecondaryIntent.CALCULATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodWeightLoss = (q.contains("weight loss") || q.contains("fat loss") || q.contains("lose weight") || q.contains("cutting")) &&
                (q.contains("this") || q.contains("in this") || q.contains("eat this") || (q.contains("is this good") && entities.foodItem != null))
        if (isFoodWeightLoss) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_WEIGHT_LOSS,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodWeightGain = (q.contains("weight gain") || q.contains("gain weight") || q.contains("bulking") || q.contains("mass")) &&
                (q.contains("this") || q.contains("in this") || q.contains("eat this") || (q.contains("is this good") && entities.foodItem != null))
        if (isFoodWeightGain) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_WEIGHT_GAIN,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodCombination = (q.contains("what can i eat with") || q.contains("eat with this") || q.contains("pair with") ||
                q.contains("side dish") || q.contains("complement") || q.contains("what to eat along with") || q.contains("healthy side"))
        if (isFoodCombination) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_COMBINATION,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodPreparation = (q.contains("make this healthier") || q.contains("cook this healthy") || q.contains("healthy way to make") ||
                q.contains("healthy preparation") || q.contains("cook healthy") || q.contains("healthy version"))
        if (isFoodPreparation) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_PREPARATION,
                secondaryIntent = CoachSecondaryIntent.HOW_TO,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodAlternative = (q.contains("alternative") || q.contains("replace this") || q.contains("substitute") || q.contains("healthier option"))
        if (isFoodAlternative) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_ALTERNATIVE,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodPortion = (q.contains("how much should i eat") || q.contains("portion size") || q.contains("serving size") || q.contains("how many pieces"))
        if (isFoodPortion) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_PORTION,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodBreakfast = (q.contains("breakfast") || q.contains("morning meal")) && (q.contains("this") || q.contains("can i eat") || entities.foodItem != null)
        if (isFoodBreakfast) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_BREAKFAST,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodDinner = (q.contains("dinner") || q.contains("night") || q.contains("evening meal")) && (q.contains("this") || q.contains("can i eat") || entities.foodItem != null)
        if (isFoodDinner) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_DINNER,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isFoodHealthiness = (q.contains("is this healthy") || q.contains("is it good") || q.contains("good for health") ||
                q.contains("health benefits") || q.contains("nutritional value") || q.contains("nutrition value") ||
                (q.contains("good") && (q.contains("workout") || q.contains("exercise") || entities.foodItem != null)))
        if (isFoodHealthiness) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD_HEALTHINESS,
                secondaryIntent = CoachSecondaryIntent.EXPLANATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 3. BMI Questions
        if (q.contains("bmi") || q.contains("body mass index") || q.contains("my weight and height status")) {
            val sec = if (q.contains("calculate") || q.contains("what is") || q.contains("my bmi")) CoachSecondaryIntent.CALCULATION else CoachSecondaryIntent.EXPLANATION
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.BMI,
                secondaryIntent = sec,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 4. Sleep, Fatigue & Recovery (Evaluated before general walking/exercise)
        if (q.contains("tired") || q.contains("fatigue") || q.contains("exhausted") || q.contains("sore") || q.contains("soreness") || q.contains("leg pain") || q.contains("muscle pain") || q.contains("stretch") || q.contains("recovery")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.RECOVERY,
                secondaryIntent = CoachSecondaryIntent.TROUBLESHOOTING,
                confidence = 0.96,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("sleep") || q.contains("insomnia") || q.contains("bedtime") || q.contains("rest hours")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.SLEEP,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 5. Pre/Post Workout Nutrition
        val isPostWorkout = q.contains("post workout") || q.contains("post-workout") ||
                ((q.contains("after") || q.contains("following")) && (q.contains("workout") || q.contains("exercise") || q.contains("running") || q.contains("walk") || q.contains("gym") || q.contains("run") || q.contains("training")))
        if (isPostWorkout) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD,
                secondaryIntent = CoachSecondaryIntent.POST_WORKOUT,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        val isPreWorkout = q.contains("pre workout") || q.contains("pre-workout") ||
                ((q.contains("before") || q.contains("prior to")) && (q.contains("workout") || q.contains("exercise") || q.contains("running") || q.contains("walk") || q.contains("gym") || q.contains("run") || q.contains("training")))
        if (isPreWorkout) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD,
                secondaryIntent = CoachSecondaryIntent.PRE_WORKOUT,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 6. Fitness Score & Health Score
        if (q.contains("fitness score") || q.contains("health score") || q.contains("score low") || q.contains("why is my score") || q.contains("score breakdown")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FITNESS_SCORE,
                secondaryIntent = if (q.contains("why") || q.contains("low")) CoachSecondaryIntent.EXPLANATION else CoachSecondaryIntent.PROGRESS_CHECK,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 7. Analytics & History ("How did I perform this week?", "Best day", "Did I improve?")
        if (q.contains("this week") || q.contains("weekly") || q.contains("perform this week") ||
            q.contains("improve") || q.contains("improvement") || q.contains("best day") ||
            q.contains("consistency") || q.contains("past days") || q.contains("history")) {
            val sec = when {
                q.contains("best day") -> CoachSecondaryIntent.CALCULATION
                q.contains("improve") -> CoachSecondaryIntent.PROGRESS_CHECK
                else -> CoachSecondaryIntent.COMPARISON
            }
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.ANALYTICS,
                secondaryIntent = sec,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 7. Specific Meal Types (Breakfast, Lunch, Dinner, Snack)
        if (q.contains("breakfast") || q.contains("morning food") || q.contains("morning meal") || q.contains("tiffin")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.BREAKFAST,
                secondaryIntent = CoachSecondaryIntent.MEAL_PLAN,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("lunch") || q.contains("afternoon meal")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.LUNCH,
                secondaryIntent = CoachSecondaryIntent.MEAL_PLAN,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("dinner") || q.contains("night food") || q.contains("night meal")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.DINNER,
                secondaryIntent = CoachSecondaryIntent.MEAL_PLAN,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("snack") || q.contains("evening snack") || q.contains("healthy snack")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.SNACK,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 8. Protein & Macronutrients
        if (q.contains("protein") || q.contains("high protein") || q.contains("cheap protein") || q.contains("veg protein") || q.contains("protein food")) {
            val sec = if (q.contains("how much") || q.contains("how many")) CoachSecondaryIntent.CALCULATION else CoachSecondaryIntent.RECOMMENDATION
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.PROTEIN,
                secondaryIntent = sec,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("carbohydrate") || q.contains("carbs") || q.contains("complex carbs")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.CARBOHYDRATES,
                secondaryIntent = CoachSecondaryIntent.EXPLANATION,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 9. Fruits & Vegetables
        if (q.contains("fruit") || q.contains("fruits")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FRUITS,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("vegetable") || q.contains("vegetables") || q.contains("veggies") || q.contains("greens")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.VEGETABLES,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 10. Weight Loss & Weight Gain Nutrition / Strategy
        if (q.contains("weight loss") || q.contains("lose weight") || q.contains("fat loss") || q.contains("burn fat") || q.contains("belly fat")) {
            val sec = if (q.contains("eat") || q.contains("food") || q.contains("diet")) CoachSecondaryIntent.MEAL_PLAN else CoachSecondaryIntent.HOW_TO
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.WEIGHT_LOSS,
                secondaryIntent = sec,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("weight gain") || q.contains("gain weight") || q.contains("muscle gain") || q.contains("bulk")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.WEIGHT_GAIN,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 11. Specific Food Item Entity Queries (e.g. "How many calories are in 2 eggs?")
        if (entities.foodItem != null) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD,
                secondaryIntent = CoachSecondaryIntent.CALCULATION,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 12. Calorie Intelligence (Burned vs Consumed vs Deficit vs BMR/TDEE vs Target)
        if (q.contains("calorie") || q.contains("calories") || q.contains("kcal") || q.contains("bmr") || q.contains("tdee") || q.contains("energy burn")) {
            val sec = when {
                q.contains("burn") || q.contains("burned") || q.contains("burnt") -> CoachSecondaryIntent.CALCULATION
                q.contains("how many should i eat") || q.contains("intake") || q.contains("target") -> CoachSecondaryIntent.DAILY_TARGET
                q.contains("bmr") || q.contains("tdee") -> CoachSecondaryIntent.EXPLANATION
                else -> CoachSecondaryIntent.CALCULATION
            }
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.CALORIES,
                secondaryIntent = sec,
                confidence = 0.96,
                normalizedQuery = normalizedQuery
            )
        }

        // 13. Water & Hydration
        if (q.contains("water") || q.contains("hydration") || q.contains("drink") || q.contains("thirsty") || q.contains("dehydrated")) {
            val sec = when {
                q.contains("how much") || q.contains("target") || q.contains("should i drink") -> CoachSecondaryIntent.DAILY_TARGET
                q.contains("remaining") || q.contains("more") -> CoachSecondaryIntent.PROGRESS_CHECK
                else -> CoachSecondaryIntent.RECOMMENDATION
            }
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.HYDRATION,
                secondaryIntent = sec,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 14. General Food & Diet Inquiries
        if (q.contains("food") || q.contains("eat") || q.contains("diet") || q.contains("nutrition")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.FOOD,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.92,
                normalizedQuery = normalizedQuery
            )
        }

        // 15. Steps & Walking Inquiries
        if (q.contains("step") || q.contains("walk") || q.contains("walking") || q.contains("nadanthiruken") || entities.steps != null) {
            val sec = when {
                q.contains("how many more") || q.contains("remaining") || q.contains("left") || q.contains("how many more steps") || (q.contains("more") && q.contains("need")) || q.contains("more steps") || q.contains("how many steps need") -> CoachSecondaryIntent.CALCULATION
                q.contains("how to increase") || q.contains("increase") || q.contains("boost") || q.contains("how can i increase") || q.contains("steps increase") -> CoachSecondaryIntent.HOW_TO
                q.contains("how many should i") || q.contains("how many steps should i") || q.contains("target") || q.contains("daily goal") -> CoachSecondaryIntent.DAILY_TARGET
                q.contains("enough") || q.contains("today") || q.contains("completed") || entities.steps != null -> CoachSecondaryIntent.PROGRESS_CHECK
                else -> CoachSecondaryIntent.PROGRESS_CHECK
            }
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.STEPS,
                secondaryIntent = sec,
                confidence = 0.98,
                normalizedQuery = normalizedQuery
            )
        }

        // 16. Workouts & Exercise (Home workout, cardio, strength, running)
        if (q.contains("workout") || q.contains("exercise") || q.contains("training") || q.contains("gym") || q.contains("home") || q.contains("pushup") || q.contains("squat")) {
            val sec = if (q.contains("home")) CoachSecondaryIntent.RECOMMENDATION else CoachSecondaryIntent.HOW_TO
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.WORKOUT,
                secondaryIntent = sec,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("run") || q.contains("running") || q.contains("jog") || q.contains("jogging")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.RUNNING,
                secondaryIntent = CoachSecondaryIntent.HOW_TO,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 17. Goals & Motivation
        if (q.contains("goal") || q.contains("target")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.GOALS,
                secondaryIntent = CoachSecondaryIntent.PROGRESS_CHECK,
                confidence = 0.92,
                normalizedQuery = normalizedQuery
            )
        }
        if (q.contains("motivation") || q.contains("motivate") || q.contains("quote") || q.contains("inspire")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.MOTIVATION,
                secondaryIntent = CoachSecondaryIntent.GENERAL,
                confidence = 0.95,
                normalizedQuery = normalizedQuery
            )
        }

        // 18. General Fitness or Unknown
        if (q.contains("fitness") || q.contains("health") || q.contains("healthy") || q.contains("routine")) {
            return IntentAnalysisResult(
                primaryIntent = CoachIntent.GENERAL_FITNESS,
                secondaryIntent = CoachSecondaryIntent.RECOMMENDATION,
                confidence = 0.85,
                normalizedQuery = normalizedQuery
            )
        }

        return IntentAnalysisResult(
            primaryIntent = CoachIntent.UNKNOWN,
            secondaryIntent = CoachSecondaryIntent.GENERAL,
            confidence = 0.0,
            isAmbiguous = true,
            normalizedQuery = normalizedQuery
        )
    }
}
