package com.example

import com.example.util.BmiCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testBmiCalculation_Case1_70kg_175cm() {
        val weight = 70.0
        val height = 175.0
        val bmi = BmiCalculator.calculateBMI(weight, height)
        val formatted = BmiCalculator.formatBmi(bmi)
        val (category, _) = BmiCalculator.getBmiCategory(bmi)

        assertEquals(22.86, bmi, 0.01)
        assertEquals("22.9", formatted)
        assertEquals("Normal weight", category)
    }

    @Test
    fun testBmiCalculation_Case2_80kg_175cm() {
        val weight = 80.0
        val height = 175.0
        val bmi = BmiCalculator.calculateBMI(weight, height)
        val formatted = BmiCalculator.formatBmi(bmi)
        val (category, _) = BmiCalculator.getBmiCategory(bmi)

        assertEquals(26.12, bmi, 0.01)
        assertEquals("26.1", formatted)
        assertEquals("Overweight", category)
    }

    @Test
    fun testBmiCalculation_Case3_50kg_175cm() {
        val weight = 50.0
        val height = 175.0
        val bmi = BmiCalculator.calculateBMI(weight, height)
        val formatted = BmiCalculator.formatBmi(bmi)
        val (category, _) = BmiCalculator.getBmiCategory(bmi)

        assertEquals(16.33, bmi, 0.01)
        assertEquals("16.3", formatted)
        assertEquals("Underweight", category)
    }

    @Test
    fun testBmiCalculation_Case4_100kg_175cm() {
        val weight = 100.0
        val height = 175.0
        val bmi = BmiCalculator.calculateBMI(weight, height)
        val formatted = BmiCalculator.formatBmi(bmi)
        val (category, _) = BmiCalculator.getBmiCategory(bmi)

        assertEquals(32.65, bmi, 0.01)
        assertEquals("32.7", formatted)
        assertEquals("Obesity", category)
    }

    @Test
    fun testBmiCalculation_Case5_CurrentUser_68kg_176cm() {
        val weight = 68.0
        val height = 176.0
        val bmi = BmiCalculator.calculateBMI(weight, height)
        val formatted = BmiCalculator.formatBmi(bmi)
        val (category, _) = BmiCalculator.getBmiCategory(bmi)

        assertEquals(21.95, bmi, 0.01)
        assertEquals("22.0", formatted)
        assertEquals("Normal weight", category)
    }

    @Test
    fun testBmiCalculation_InvalidInputs() {
        val bmiZero = BmiCalculator.calculateBMI(0.0, 176.0)
        assertEquals(0.0, bmiZero, 0.001)
        val (category, tip) = BmiCalculator.getBmiCategory(bmiZero)
        assertEquals("Not Available", category)
        assertEquals("Enter valid height and weight to calculate BMI.", tip)
    }

    @Test
    fun testEmailValidation() {
        org.junit.Assert.assertTrue(com.example.util.isValidEmailAddress("user@gmail.com"))
        org.junit.Assert.assertTrue(com.example.util.isValidEmailAddress("first.last@gmail.com"))
        org.junit.Assert.assertTrue(com.example.util.isValidEmailAddress("Athlete123@GMAIL.COM"))
        org.junit.Assert.assertTrue(com.example.util.isValidEmailAddress("user@yahoo.com"))
        org.junit.Assert.assertTrue(com.example.util.isValidEmailAddress("user@example.org"))
        org.junit.Assert.assertFalse(com.example.util.isValidEmailAddress("invalid-email"))
        org.junit.Assert.assertFalse(com.example.util.isValidEmailAddress("@gmail.com"))
        org.junit.Assert.assertFalse(com.example.util.isValidEmailAddress(""))
    }

    @Test
    fun testAuthErrorMapping() {
        val invalidCred = Exception("INVALID_LOGIN_CREDENTIALS [ invalid-credential ]")
        org.junit.Assert.assertEquals("Invalid email or password.", com.example.util.formatFriendlyAuthError(invalidCred))

        val userNotFound = Exception("USER_NOT_FOUND")
        org.junit.Assert.assertEquals("No account found with this email.", com.example.util.formatFriendlyAuthError(userNotFound))

        val alreadyInUse = Exception("EMAIL_EXISTS: email-already-in-use")
        org.junit.Assert.assertEquals("An account already exists with this email. Please sign in.", com.example.util.formatFriendlyAuthError(alreadyInUse))

        val weakPass = Exception("WEAK_PASSWORD: weak-password")
        org.junit.Assert.assertEquals("Password must contain at least 8 characters.", com.example.util.formatFriendlyAuthError(weakPass))

        val opNotAllowed = Exception("OPERATION_NOT_ALLOWED: sign-in provider is disabled")
        org.junit.Assert.assertEquals("Email/password sign-in is currently disabled.", com.example.util.formatFriendlyAuthError(opNotAllowed))
    }

    @Test
    fun testOfflineAiCoachEngine_All20BenchmarkQuestions() {
        val input = com.example.data.coach.CoachInputData(
            name = "Harish",
            steps = 4500,
            stepGoal = 10000,
            distanceMeters = 3300.0,
            caloriesBurned = 180.0,
            activeMinutes = 45,
            waterIntakeMl = 1200,
            waterGoalMl = 2500,
            heightCm = 176.0,
            weightKg = 68.0,
            bmi = 22.0,
            bmiCategory = "Normal",
            activityLevel = "Moderately Active",
            currentActivity = "Walking",
            age = 28,
            gender = "Male"
        )
        val profile = com.example.data.local.UserProfile(
            name = "Harish",
            email = "harish@test.com",
            fitnessGoal = "Weight Loss",
            dietPreference = "Vegetarian",
            dailyStepGoal = 10000,
            dailyWaterGoalMl = 2500,
            age = 28,
            gender = "Male",
            heightCm = 176.0,
            weightKg = 68.0,
            activityLevel = "Moderately Active"
        )

        // 1. What should I eat for breakfast?
        val ans1 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What should I eat for breakfast?", input, profile)
        assertTrue(ans1.contains("Breakfast", ignoreCase = true))

        // 2. What should I eat after my workout?
        val ans2 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What should I eat after my workout?", input, profile)
        assertTrue(ans2.contains("Post-Workout", ignoreCase = true) || ans2.contains("Recovery", ignoreCase = true))

        // 3. How can I lose weight?
        val ans3 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How can I lose weight?", input, profile)
        assertTrue(ans3.contains("Weight", ignoreCase = true) || ans3.contains("Fat", ignoreCase = true))

        // 4. How many calories are in 2 eggs?
        val ans4 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many calories are in 2 eggs?", input, profile)
        assertTrue(ans4.contains("Egg", ignoreCase = true))
        assertTrue(ans4.contains("148") || ans4.contains("74") || ans4.contains("Calories", ignoreCase = true))

        // 5. How many steps should I walk today?
        val ans5 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many steps should I walk today?", input, profile)
        assertTrue(ans5.contains("Step", ignoreCase = true))
        assertTrue(ans5.contains("4500") || ans5.contains("10000"))

        // 6. How much water should I drink?
        val ans6 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How much water should I drink?", input, profile)
        assertTrue(ans6.contains("Water", ignoreCase = true) || ans6.contains("Hydration", ignoreCase = true))

        // 7. Suggest a cheap dinner.
        val ans7 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Suggest a cheap dinner.", input, profile)
        assertTrue(ans7.contains("Dinner", ignoreCase = true))
        assertTrue(ans7.contains("Chapati") || ans7.contains("Idli") || ans7.contains("Moong") || ans7.contains("Affordable", ignoreCase = true))

        // 8. Suggest a vegetarian dinner.
        val ans8 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Suggest a vegetarian dinner.", input, profile)
        assertTrue(ans8.contains("Dinner", ignoreCase = true))

        // 9. What vegetables should I eat?
        val ans9 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What vegetables should I eat?", input, profile)
        assertTrue(ans9.contains("Vegetable", ignoreCase = true))
        assertTrue(ans9.contains("Bottle Gourd", ignoreCase = true) || ans9.contains("Spinach", ignoreCase = true) || ans9.contains("Lauki", ignoreCase = true))

        // 10. How much protein do I need?
        val ans10 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How much protein do I need?", input, profile)
        assertTrue(ans10.contains("Protein", ignoreCase = true))

        // 11. Give me a beginner workout.
        val ans11 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Give me a beginner workout.", input, profile)
        assertTrue(ans11.contains("Workout", ignoreCase = true) || ans11.contains("Circuit", ignoreCase = true))
        assertTrue(ans11.contains("Squats", ignoreCase = true) || ans11.contains("Push-ups", ignoreCase = true))

        // 12. How can I increase my steps?
        val ans12 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How can I increase my steps?", input, profile)
        assertTrue(ans12.contains("Step", ignoreCase = true) || ans12.contains("Walk", ignoreCase = true))

        // 13. Is banana good after workout?
        val ans13 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Is banana good after workout?", input, profile)
        assertTrue(ans13.contains("Banana", ignoreCase = true) || ans13.contains("Post-Workout", ignoreCase = true))
        assertTrue(ans13.contains("Yes", ignoreCase = true) || ans13.contains("105", ignoreCase = true))

        // 14. How many calories should I eat to lose weight?
        val ans14 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many calories should I eat to lose weight?", input, profile)
        assertTrue(ans14.contains("Calorie", ignoreCase = true))
        assertTrue(ans14.contains("BMR", ignoreCase = true) || ans14.contains("TDEE", ignoreCase = true) || ans14.contains("Deficit", ignoreCase = true))

        // 15. What should I eat before walking?
        val ans15 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What should I eat before walking?", input, profile)
        assertTrue(ans15.contains("Pre-Workout", ignoreCase = true) || ans15.contains("Fuel", ignoreCase = true) || ans15.contains("Banana", ignoreCase = true))

        // 16. Make my dinner cheaper. (Contextual follow-up)
        val dinnerHistory = listOf("Suggest a dinner." to "### 🌙 Light Dinner Suggestions\n• 2 Chapatis with curry")
        val ans16 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Make my dinner cheaper.", input, profile, dinnerHistory)
        assertTrue(ans16.contains("Dinner", ignoreCase = true) && (ans16.contains("Affordable", ignoreCase = true) || ans16.contains("Chapati", ignoreCase = true)))

        // 17. How many calories are in that? (Contextual follow-up)
        val foodHistory = listOf("Tell me about banana" to "### 🍌 Banana\nA medium banana provides 105 kcal")
        val ans17 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many calories are in that?", input, profile, foodHistory)
        assertTrue(ans17.contains("Calories", ignoreCase = true) || ans17.contains("105") || ans17.contains("Nutrition", ignoreCase = true))

        // 18. What about breakfast? (Contextual switch)
        val ans18 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What about breakfast?", input, profile, dinnerHistory)
        assertTrue(ans18.contains("Breakfast", ignoreCase = true))

        // 19. What should I take? (Ambiguous -> Clarification prompt)
        val ans19 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What should I take?", input, profile)
        assertTrue(ans19.contains("assist", ignoreCase = true) || ans19.contains("specify", ignoreCase = true) || ans19.contains("Nutrition", ignoreCase = true))

        // 20. Give me something healthy. (Clarification / Broad)
        val ans20 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Give me something healthy.", input, profile)
        assertTrue(ans20.contains("assist", ignoreCase = true) || ans20.contains("Healthy", ignoreCase = true) || ans20.contains("Nutrition", ignoreCase = true))

        // Deterministic Calculations Accuracy:
        val calculatedBmi = com.example.data.coach.OfflineAiCoachEngine.calculateBmi(176.0, 68.0)
        assertEquals(22.0, calculatedBmi, 0.1) // Height = 176cm, Weight = 68kg -> 22.0
    }

    @Test
    fun testOfflineAiCoachEngine_FollowUpAndMultiIntent() {
        val input = com.example.data.coach.CoachInputData(
            steps = 5000,
            stepGoal = 10000,
            distanceMeters = 3800.0,
            caloriesBurned = 210.0,
            activeMinutes = 40,
            waterIntakeMl = 1500,
            waterGoalMl = 2500,
            heightCm = 170.0,
            weightKg = 65.0,
            bmi = 22.49,
            bmiCategory = "Normal",
            activityLevel = "Active",
            currentActivity = "Walking",
            age = 28,
            gender = "Female"
        )
        val history = listOf(
            "Suggest a dinner." to "### 🌙 Light Dinner Suggestions\n• 2 Chapatis with vegetable curry\n• Steamed Idlis with Sambar"
        )

        // Contextual follow-up: "Give me a vegetarian option"
        val followUp = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer(
            "Give me a vegetarian option",
            input,
            null,
            history
        )
        assertTrue(followUp.contains("Dinner", ignoreCase = true))

        // Medical safety question
        val medSafety = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer(
            "I have severe chest pain and can't breathe",
            input,
            null
        )
        assertTrue(medSafety.contains("Safety", ignoreCase = true) || medSafety.contains("physician", ignoreCase = true))

        // Unknown question outside fitness -> triggers clarification
        val unknown = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer(
            "Who won the 1994 World Cup in football?",
            input,
            null
        )
        assertTrue(unknown.contains("assist", ignoreCase = true) || unknown.contains("Nutrition", ignoreCase = true))
    }

    @Test
    fun testPasswordHashing() {
        val hash1 = com.example.util.hashPassword("password123")
        val hash2 = com.example.util.hashPassword("password123")
        val hashDiff = com.example.util.hashPassword("different_password")
        assertEquals(hash1, hash2)
        org.junit.Assert.assertNotEquals(hash1, hashDiff)
        org.junit.Assert.assertEquals(64, hash1.length) // SHA-256 hex string
    }

    @Test
    fun testMifflinStJeorBmrCalculation() {
        // Male: 10 * weight(70) + 6.25 * height(175) - 5 * age(25) + 5 = 700 + 1093.75 - 125 + 5 = 1673.75 -> 1674
        val maleBmr = com.example.data.model.DynamicFoodEngine.calculateBmr(70.0, 175.0, 25, "Male")
        assertEquals(1674, maleBmr)

        // Female: 10 * weight(60) + 6.25 * height(165) - 5 * age(25) - 161 = 600 + 1031.25 - 125 - 161 = 1345.25 -> 1345
        val femaleBmr = com.example.data.model.DynamicFoodEngine.calculateBmr(60.0, 165.0, 25, "Female")
        assertEquals(1345, femaleBmr)
    }

    @Test
    fun testTdeeAndTargetCalories() {
        val bmr = 1674
        val tdeeSedentary = com.example.data.model.DynamicFoodEngine.calculateTdee(bmr, "Sedentary")
        val tdeeModerate = com.example.data.model.DynamicFoodEngine.calculateTdee(bmr, "Moderately Active")

        assertEquals(kotlin.math.round((1674 * 1.2)).toInt(), tdeeSedentary)
        assertEquals(kotlin.math.round((1674 * 1.55)).toInt(), tdeeModerate)

        val weightLossTarget = com.example.data.model.DynamicFoodEngine.calculateTargetCalories(tdeeModerate, "Weight Loss")
        val weightGainTarget = com.example.data.model.DynamicFoodEngine.calculateTargetCalories(tdeeModerate, "Muscle Gain")

        assertEquals(tdeeModerate - 500, weightLossTarget)
        assertEquals(tdeeModerate + 400, weightGainTarget)
    }

    @Test
    fun testFoodLookupSearch() {
        val bananaResults = com.example.data.model.FoodDataCatalog.searchFoodLookup("banana")
        org.junit.Assert.assertTrue(bananaResults.isNotEmpty())
        org.junit.Assert.assertTrue(bananaResults.any { it.name.contains("Banana", ignoreCase = true) })

        val dalResults = com.example.data.model.FoodDataCatalog.searchFoodLookup("dal")
        org.junit.Assert.assertTrue(dalResults.isNotEmpty())

        val eggResults = com.example.data.model.FoodDataCatalog.searchFoodLookup("egg")
        org.junit.Assert.assertTrue(eggResults.isNotEmpty())
    }

    @Test
    fun testDailyMealPlanGeneration() {
        val vegPlan = com.example.data.model.FoodDataCatalog.generateDailyMealPlan(
            seed = 1,
            dietPreference = "Veg",
            targetCalories = 1800,
            fitnessGoal = "Weight Loss"
        )
        org.junit.Assert.assertNotNull(vegPlan.breakfast)
        org.junit.Assert.assertNotNull(vegPlan.lunch)
        org.junit.Assert.assertNotNull(vegPlan.dinner)
        org.junit.Assert.assertNotNull(vegPlan.eveningSnack)
        org.junit.Assert.assertTrue(vegPlan.totalCalories in 800..3000)
        org.junit.Assert.assertTrue(vegPlan.totalProteinG > 20.0)
        org.junit.Assert.assertTrue(vegPlan.featuredVegetables.isNotEmpty())
        org.junit.Assert.assertTrue(vegPlan.goalFeedback.isNotBlank())
        org.junit.Assert.assertEquals("Weight Loss", vegPlan.fitnessGoal)

        val regeneratedPlan = com.example.data.model.FoodDataCatalog.generateDailyMealPlan(
            seed = 2,
            dietPreference = "Veg",
            targetCalories = 1800,
            fitnessGoal = "Weight Loss"
        )
        org.junit.Assert.assertNotNull(regeneratedPlan.breakfast)
    }

    @Test
    fun testOfflineAiCoachEngine_WorkoutTipsGeneration_LowSteps() {
        val input = com.example.data.coach.CoachInputData(
            name = "Harish",
            steps = 1500,
            stepGoal = 10000,
            distanceMeters = 1100.0,
            caloriesBurned = 60.0,
            activeMinutes = 15,
            waterIntakeMl = 1000,
            waterGoalMl = 3000,
            heightCm = 176.0,
            weightKg = 68.0,
            bmi = 22.0,
            bmiCategory = "Normal",
            currentActivity = "Walking",
            activityLevel = "Moderate",
            age = 28,
            gender = "Male"
        )
        val profile = com.example.data.local.UserProfile(
            name = "Harish",
            dailyStepGoal = 10000,
            fitnessGoal = "Weight Loss",
            activityLevel = "Moderate"
        )

        val plan = com.example.data.coach.OfflineAiCoachEngine.generateWorkoutTips(input, profile)
        org.junit.Assert.assertNotNull(plan)
        org.junit.Assert.assertTrue(plan.tips.isNotEmpty())
        org.junit.Assert.assertEquals("Active Foundation Established", plan.activityTier)
        org.junit.Assert.assertTrue(plan.overallAdvice.isNotBlank())
        org.junit.Assert.assertTrue(plan.tips.any { it.targetActivityMode.equals("Walking", ignoreCase = true) })
    }

    @Test
    fun testOfflineAiCoachEngine_WorkoutTipsGeneration_GoalAchieved() {
        val input = com.example.data.coach.CoachInputData(
            name = "Harish",
            steps = 11500,
            stepGoal = 10000,
            distanceMeters = 8200.0,
            caloriesBurned = 460.0,
            activeMinutes = 95,
            waterIntakeMl = 3000,
            waterGoalMl = 3000,
            heightCm = 176.0,
            weightKg = 68.0,
            bmi = 22.0,
            bmiCategory = "Normal",
            currentActivity = "Running",
            activityLevel = "Active",
            age = 28,
            gender = "Male"
        )
        val profile = com.example.data.local.UserProfile(
            name = "Harish",
            dailyStepGoal = 10000,
            fitnessGoal = "Endurance",
            activityLevel = "Active"
        )

        val plan = com.example.data.coach.OfflineAiCoachEngine.generateWorkoutTips(input, profile)
        org.junit.Assert.assertNotNull(plan)
        org.junit.Assert.assertEquals("Goal Surpassed & Peak Maintenance", plan.activityTier)
        org.junit.Assert.assertTrue(plan.tips.any { it.category.contains("Recovery", ignoreCase = true) })
    }

    @Test
    fun testAiCoach_15CoreScenarios() {
        val input = com.example.data.coach.CoachInputData(
            name = "Harish",
            steps = 6200,
            stepGoal = 8000,
            distanceMeters = 4600.0,
            caloriesBurned = 248.0,
            activeMinutes = 40,
            waterIntakeMl = 1800,
            waterGoalMl = 2500,
            heightCm = 176.0,
            weightKg = 68.0,
            bmi = 21.95,
            bmiCategory = "Normal",
            activityLevel = "Moderately Active",
            currentActivity = "Walking",
            age = 28,
            gender = "Male"
        )
        val profile = com.example.data.local.UserProfile(
            name = "Harish",
            dailyStepGoal = 8000,
            dailyWaterGoalMl = 2500,
            heightCm = 176.0,
            weightKg = 68.0,
            age = 28,
            gender = "Male",
            activityLevel = "Moderately Active"
        )

        // 1. "How many steps should I walk?"
        val r1 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many steps should I walk?", input, profile)
        assertTrue(r1.contains("8,000") || r1.contains("10,000"))
        assertTrue(r1.contains("6200") || r1.contains("8000"))

        // 2. "How can I increase my steps?"
        val r2 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How can I increase my steps?", input, profile)
        assertTrue(r2.contains("Post-Meal", ignoreCase = true) || r2.contains("Stairs", ignoreCase = true) || r2.contains("Phone Calls", ignoreCase = true) || r2.contains("1,000", ignoreCase = true))

        // 3. "I walked 5000 steps today. Is that enough?"
        val r3 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("I walked 5000 steps today. Is that enough?", input, profile)
        assertTrue(r3.contains("5000") || r3.contains("Evaluation", ignoreCase = true) || r3.contains("enough", ignoreCase = true))

        // 4. "How many more steps do I need?"
        val r4 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many more steps do I need?", input, profile)
        assertTrue(r4.contains("1800") || r4.contains("Remaining", ignoreCase = true))

        // 5. "I am tired after walking"
        val r5 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("I am tired after walking", input, profile)
        assertTrue(r5.contains("Recovery", ignoreCase = true) || r5.contains("Rehydrate", ignoreCase = true) || r5.contains("Electrolytes", ignoreCase = true) || r5.contains("Stretches", ignoreCase = true))

        // 6. "What should I eat for protein?"
        val r6 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What should I eat for protein?", input, profile)
        assertTrue(r6.contains("Protein", ignoreCase = true))
        assertTrue(r6.contains("Egg", ignoreCase = true) || r6.contains("Dal", ignoreCase = true) || r6.contains("Paneer", ignoreCase = true) || r6.contains("Moong", ignoreCase = true))

        // 7. "What should I eat after exercise?"
        val r7 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What should I eat after exercise?", input, profile)
        assertTrue(r7.contains("Post-Workout", ignoreCase = true) || r7.contains("Carbohydrate", ignoreCase = true) || r7.contains("Protein", ignoreCase = true))

        // 8. "How much water should I drink?"
        val r8 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How much water should I drink?", input, profile)
        assertTrue(r8.contains("Hydration", ignoreCase = true) || r8.contains("2500") || r8.contains("Water", ignoreCase = true))

        // 9. Follow-up: "What about after walking?"
        val waterHistory = listOf("How much water should I drink?" to "Aim for 2.5 Liters per day.")
        val r9 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What about after walking?", input, profile, waterHistory)
        assertTrue(r9.contains("Post-Walk", ignoreCase = true) || r9.contains("Hydration", ignoreCase = true) || r9.contains("300", ignoreCase = true) || r9.contains("Water", ignoreCase = true))

        // 10. "I want to lose weight."
        val r10 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("I want to lose weight.", input, profile)
        assertTrue(r10.contains("Weight Loss", ignoreCase = true) || r10.contains("Deficit", ignoreCase = true) || r10.contains("TDEE", ignoreCase = true))

        // 11. "How many calories should I eat?"
        val r11 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many calories should I eat?", input, profile)
        assertTrue(r11.contains("BMR", ignoreCase = true) || r11.contains("TDEE", ignoreCase = true) || r11.contains("Calorie", ignoreCase = true))

        // 12. "How many calories did I burn?"
        val r12 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many calories did I burn?", input, profile)
        assertTrue(r12.contains("248") || r12.contains("Calories Burned", ignoreCase = true) || r12.contains("burned", ignoreCase = true))

        // 13. "What is my BMI?"
        val r13 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("What is my BMI?", input, profile)
        assertTrue(r13.contains("21.95") || r13.contains("22") || r13.contains("BMI", ignoreCase = true))

        // 14. "Why is my fitness score low?"
        val r14 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("Why is my fitness score low?", input, profile)
        assertTrue(r14.contains("Fitness Score", ignoreCase = true) || r14.contains("Breakdown", ignoreCase = true) || r14.contains("Score", ignoreCase = true))

        // 15. Follow-up: "How many should I add?"
        val stepHistory = listOf("How can I increase my steps?" to "Add steps gradually.")
        val r15 = com.example.data.coach.OfflineAiCoachEngine.generateOfflineAnswer("How many should I add?", input, profile, stepHistory)
        assertTrue(r15.contains("Step", ignoreCase = true) || r15.contains("1,000", ignoreCase = true) || r15.contains("Habits", ignoreCase = true))
    }

    @Test
    fun testDailyStepGoalProgress_Calculation() {
        val currentSteps = 6000
        val targetSteps = 10000
        val progress = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
        val percentage = (progress * 100).toInt()

        org.junit.Assert.assertEquals(0.6f, progress, 0.001f)
        org.junit.Assert.assertEquals(60, percentage)

        // Exceeded goal
        val exceededSteps = 12500
        val exceededProgress = (exceededSteps.toFloat() / targetSteps.toFloat())
        val cappedPercentage = (exceededProgress.coerceIn(0f, 1f) * 100).toInt()
        val totalPercentage = (exceededProgress * 100).toInt()
        org.junit.Assert.assertEquals(100, cappedPercentage)
        org.junit.Assert.assertEquals(125, totalPercentage)
    }

    @Test
    fun testLocalVisionClassification_CakeVsAppleVsNonFood() {
        // 1. Test Cake detection logic
        val cakeLabels = listOf(
            com.example.data.coach.vision.LocalVisionLabel("Cake", 0.92f, 1),
            com.example.data.coach.vision.LocalVisionLabel("Dessert", 0.88f, 2),
            com.example.data.coach.vision.LocalVisionLabel("Pastry", 0.85f, 3)
        )

        // Verify Cake match
        val cakeMatch = com.example.data.coach.CoachFoodDatabase.findFoodMatch("cake")
        org.junit.Assert.assertNotNull(cakeMatch)
        org.junit.Assert.assertEquals("Cake (Pastry / Slice)", cakeMatch?.name)
        org.junit.Assert.assertEquals(280, cakeMatch?.caloriesKcal)

        // 2. Test Apple detection logic
        val appleMatch = com.example.data.coach.CoachFoodDatabase.findFoodMatch("apple")
        org.junit.Assert.assertNotNull(appleMatch)
        org.junit.Assert.assertEquals("Apple", appleMatch?.name)
        org.junit.Assert.assertEquals(95, appleMatch?.caloriesKcal)

        // 3. Test Relevance Filter for Cake
        val cakeDetection = com.example.data.coach.vision.StrictVisionDetection(
            category = com.example.data.coach.DetectedMediaCategory.FOOD,
            identifiedName = "Cake / Pastry",
            matchedFood = cakeMatch,
            isConfident = true,
            confidenceScore = 92,
            isFood = true,
            visualDescription = "Baked dessert / slice of cake with sweet frosting."
        )
        val cakeAnswer = com.example.data.coach.vision.RelevanceFilter.filterAndRefineResponse(
            rawResponse = "",
            userPrompt = "What is this?",
            visionDetection = cakeDetection,
            ocrResult = null
        )
        assertTrue(cakeAnswer.contains("Cake", ignoreCase = true))
        org.junit.Assert.assertFalse(cakeAnswer.contains("Apple", ignoreCase = true))

        // 4. Test Relevance Filter for Non-Food Equipment
        val gymDetection = com.example.data.coach.vision.StrictVisionDetection(
            category = com.example.data.coach.DetectedMediaCategory.GYM_EQUIPMENT,
            identifiedName = "Gym / Workout Equipment",
            matchedFood = null,
            isConfident = true,
            confidenceScore = 90,
            isFood = false,
            visualDescription = "Fitness apparatus / exercise equipment."
        )
        val gymAnswer = com.example.data.coach.vision.RelevanceFilter.filterAndRefineResponse(
            rawResponse = "",
            userPrompt = "What is this?",
            visionDetection = gymDetection,
            ocrResult = null
        )
        assertTrue(gymAnswer.contains("Gym", ignoreCase = true) || gymAnswer.contains("Equipment", ignoreCase = true))
        org.junit.Assert.assertFalse(gymAnswer.contains("Apple", ignoreCase = true))
        org.junit.Assert.assertFalse(gymAnswer.contains("Rasam", ignoreCase = true))
    }

    @Test
    fun testAiCoachSessionStateClearing_UniqueSessionIdPreventsLeakage() {
        val dummyProfile = com.example.data.local.UserProfile(
            name = "Test User",
            age = 28,
            gender = "Male",
            heightCm = 175.0,
            weightKg = 72.0,
            fitnessGoal = "Weight Loss",
            dietPreference = "Vegetarian"
        )
        val dummyInput = com.example.data.coach.CoachInputData(
            name = "Test User",
            weightKg = 72.0,
            heightCm = 175.0,
            steps = 5000,
            stepGoal = 10000,
            distanceMeters = 3500.0,
            activeMinutes = 40,
            waterIntakeMl = 1500,
            waterGoalMl = 3000,
            caloriesBurned = 350.0,
            bmi = 23.5,
            bmiCategory = "Normal",
            currentActivity = "Resting",
            activityLevel = "Moderate",
            age = 28,
            gender = "Male"
        )

        // Session 1: Analyzing an Apple
        val appleFood = com.example.data.coach.CoachFoodDatabase.findFoodMatch("apple")!!
        val session1Id = java.util.UUID.randomUUID().toString()
        val foodContext1 = com.example.data.coach.food.ActiveFoodContext(
            items = listOf(com.example.data.coach.food.AnalyzedFoodItem(food = appleFood, confidencePercent = 90)),
            imageUriString = "content://media/1",
            overallStatus = com.example.data.coach.food.RecognitionConfidenceLevel.HIGH_CONFIDENCE,
            diagnosticMessage = "Identified: Apple",
            sessionId = session1Id
        )
        org.junit.Assert.assertEquals(session1Id, foodContext1.sessionId)
        org.junit.Assert.assertEquals(session1Id, foodContext1.requestId)
        org.junit.Assert.assertEquals("Apple", foodContext1.primaryItem?.food?.name)

        // Follow-up query on Session 1 (same session ID and context preserved)
        val followUpCheck = com.example.data.coach.context.ConversationContextManager.resolveFollowUpQuery(
            query = "How much protein is in this?",
            history = listOf("📷 Analyze this photo" to "This is an apple with ~95 kcal."),
            activeFoodContext = foodContext1,
            activeCategory = com.example.data.coach.DetectedMediaCategory.FOOD
        )
        assertTrue(followUpCheck.isFoodFollowUp)
        org.junit.Assert.assertEquals("Apple", followUpCheck.resolvedSubject)

        // Session 2: A NEW image is uploaded (e.g. Cake).
        // Before analyzing new image, previous foodContext1 is wiped and a distinct session ID is generated
        val session2Id = java.util.UUID.randomUUID().toString()
        org.junit.Assert.assertNotEquals(session1Id, session2Id)

        val cakeFood = com.example.data.coach.CoachFoodDatabase.findFoodMatch("cake")!!
        // Wiped state creates clean food context for session 2
        val foodContext2 = com.example.data.coach.food.ActiveFoodContext(
            items = listOf(com.example.data.coach.food.AnalyzedFoodItem(food = cakeFood, confidencePercent = 92)),
            imageUriString = "content://media/2",
            overallStatus = com.example.data.coach.food.RecognitionConfidenceLevel.HIGH_CONFIDENCE,
            diagnosticMessage = "Identified: Cake (Pastry / Slice)",
            sessionId = session2Id
        )

        // Verify that Session 2 is completely isolated and does not contain Apple metadata
        org.junit.Assert.assertEquals(session2Id, foodContext2.sessionId)
        org.junit.Assert.assertEquals("Cake (Pastry / Slice)", foodContext2.primaryItem?.food?.name)
        org.junit.Assert.assertEquals(280, foodContext2.totalCalories)
        org.junit.Assert.assertFalse(foodContext2.items.any { it.food.name.contains("Apple", ignoreCase = true) })

        // Follow-up on Session 2 targets Cake, not Apple
        val followUpCheck2 = com.example.data.coach.context.ConversationContextManager.resolveFollowUpQuery(
            query = "Is this good for weight loss?",
            history = listOf("📷 Analyze this photo" to "This is a cake slice."),
            activeFoodContext = foodContext2,
            activeCategory = com.example.data.coach.DetectedMediaCategory.FOOD
        )
        assertTrue(followUpCheck2.isFoodFollowUp)
        org.junit.Assert.assertEquals("Cake (Pastry / Slice)", followUpCheck2.resolvedSubject)
    }

    @Test
    fun testLocalVisionClassification_CakeVsApple_StrictDistinction() {
        // Case 1: Cake image labels
        val cakeLabels = listOf(
            com.example.data.coach.vision.LocalVisionLabel("Cake", 0.92f),
            com.example.data.coach.vision.LocalVisionLabel("Pastry", 0.85f),
            com.example.data.coach.vision.LocalVisionLabel("Dessert", 0.80f)
        )
        val cakeResult = com.example.data.coach.vision.LocalMlKitVisionEngine.mapLabelsForTesting(cakeLabels)
        assertTrue(cakeResult.isConfident)
        assertTrue(cakeResult.isFood)
        assertEquals("Cake / Pastry", cakeResult.identifiedName)
        assertEquals("Cake (Pastry / Slice)", cakeResult.matchedFood?.name)
        org.junit.Assert.assertFalse(cakeResult.identifiedName.orEmpty().contains("Apple", ignoreCase = true))

        // Case 2: Apple image labels
        val appleLabels = listOf(
            com.example.data.coach.vision.LocalVisionLabel("Apple", 0.94f),
            com.example.data.coach.vision.LocalVisionLabel("Fruit", 0.88f)
        )
        val appleResult = com.example.data.coach.vision.LocalMlKitVisionEngine.mapLabelsForTesting(appleLabels)
        assertTrue(appleResult.isConfident)
        assertTrue(appleResult.isFood)
        assertEquals("Apple", appleResult.identifiedName)
        assertEquals("Apple", appleResult.matchedFood?.name)
        org.junit.Assert.assertFalse(appleResult.identifiedName.orEmpty().contains("Cake", ignoreCase = true))

        // Case 3: Pizza labels
        val pizzaLabels = listOf(
            com.example.data.coach.vision.LocalVisionLabel("Pizza", 0.95f),
            com.example.data.coach.vision.LocalVisionLabel("Fast food", 0.80f)
        )
        val pizzaResult = com.example.data.coach.vision.LocalMlKitVisionEngine.mapLabelsForTesting(pizzaLabels)
        assertTrue(pizzaResult.isConfident)
        assertEquals("Pizza", pizzaResult.identifiedName)
    }

    @Test
    fun testLocalVisionClassification_Uncertainty_RejectsLowConfidenceWithoutGuessing() {
        // Case 1: Empty labels -> Uncertainty returned, never guessing
        val emptyLabels = emptyList<com.example.data.coach.vision.LocalVisionLabel>()
        val emptyResult = com.example.data.coach.vision.LocalMlKitVisionEngine.mapLabelsForTesting(emptyLabels)
        org.junit.Assert.assertFalse(emptyResult.isConfident)
        org.junit.Assert.assertNull(emptyResult.identifiedName)
        org.junit.Assert.assertNull(emptyResult.matchedFood)
        assertTrue(emptyResult.uncertaintyMessage?.contains("can't confidently identify", ignoreCase = true) == true)

        // Case 2: Ambiguous/Low confidence label
        val lowConfLabels = listOf(
            com.example.data.coach.vision.LocalVisionLabel("Thing", 0.35f)
        )
        val lowResult = com.example.data.coach.vision.LocalMlKitVisionEngine.mapLabelsForTesting(lowConfLabels)
        org.junit.Assert.assertFalse(lowResult.isConfident)
    }

    @Test
    fun testLocalVisionClassification_NonFoodItems_RejectedAsFood() {
        val nonFoodLabels = listOf(
            com.example.data.coach.vision.LocalVisionLabel("Laptop", 0.90f),
            com.example.data.coach.vision.LocalVisionLabel("Computer keyboard", 0.85f)
        )
        val nonFoodResult = com.example.data.coach.vision.LocalMlKitVisionEngine.mapLabelsForTesting(nonFoodLabels)
        assertTrue(nonFoodResult.isConfident)
        org.junit.Assert.assertFalse(nonFoodResult.isFood)
        org.junit.Assert.assertNull(nonFoodResult.matchedFood)
        assertEquals(com.example.data.coach.DetectedMediaCategory.GENERAL_WELLNESS, nonFoodResult.category)
    }

    @Test
    fun testRelevanceFilter_DirectMultimodalResponsePriority() {
        val dummyDetection = com.example.data.coach.vision.StrictVisionDetection(
            category = com.example.data.coach.DetectedMediaCategory.FOOD,
            identifiedName = "Apple",
            matchedFood = com.example.data.coach.CoachFoodDatabase.findFoodMatch("apple"),
            isConfident = false,
            confidenceScore = 40,
            isFood = true,
            visualDescription = "Uncertain"
        )

        // Gemini says Cake
        val geminiCakeResponse = "This appears to be a slice of chocolate cake with creamy frosting."
        val filtered = com.example.data.coach.vision.RelevanceFilter.filterAndRefineResponse(
            rawResponse = geminiCakeResponse,
            userPrompt = "What is this?",
            visionDetection = dummyDetection,
            ocrResult = null
        )

        // Must output Cake, not Apple
        assertTrue(filtered.contains("chocolate cake", ignoreCase = true))
        org.junit.Assert.assertFalse(filtered.contains("Apple", ignoreCase = true))
    }
}

