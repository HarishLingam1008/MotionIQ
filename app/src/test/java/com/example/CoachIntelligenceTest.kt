package com.example

import com.example.data.coach.CoachConversationContext
import com.example.data.coach.CoachEntityExtractor
import com.example.data.coach.CoachFoodDatabase
import com.example.data.coach.CoachInputData
import com.example.data.coach.CoachIntent
import com.example.data.coach.CoachIntentDetector
import com.example.data.coach.CoachSecondaryIntent
import com.example.data.coach.CoachTextNormalizer
import com.example.data.coach.OfflineAiCoachEngine
import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachIntelligenceTest {

    private val sampleProfile = UserProfile(
        id = "user_1",
        name = "Harish",
        age = 28,
        gender = "Male",
        heightCm = 176.0,
        weightKg = 68.0,
        activityLevel = "Moderately Active",
        fitnessGoal = "General Fitness",
        dietPreference = "Balanced",
        dailyStepGoal = 8000,
        dailyWaterGoalMl = 2500
    )

    private val sampleWeeklyHistory = listOf(
        DailyActivity(id = "user_2026-08-22", userId = "user", date = "2026-08-22", steps = 7500, distanceMeters = 5400.0, calories = 280.0, activeMinutes = 40, waterIntakeMl = 2200),
        DailyActivity(id = "user_2026-08-23", userId = "user", date = "2026-08-23", steps = 8200, distanceMeters = 6100.0, calories = 310.0, activeMinutes = 45, waterIntakeMl = 2500),
        DailyActivity(id = "user_2026-08-24", userId = "user", date = "2026-08-24", steps = 9100, distanceMeters = 6800.0, calories = 340.0, activeMinutes = 50, waterIntakeMl = 2600),
        DailyActivity(id = "user_2026-08-25", userId = "user", date = "2026-08-25", steps = 6000, distanceMeters = 4500.0, calories = 240.0, activeMinutes = 30, waterIntakeMl = 2000),
        DailyActivity(id = "user_2026-08-26", userId = "user", date = "2026-08-26", steps = 8500, distanceMeters = 6300.0, calories = 320.0, activeMinutes = 48, waterIntakeMl = 2500),
        DailyActivity(id = "user_2026-08-27", userId = "user", date = "2026-08-27", steps = 7900, distanceMeters = 5900.0, calories = 300.0, activeMinutes = 42, waterIntakeMl = 2400),
        DailyActivity(id = "user_2026-08-28", userId = "user", date = "2026-08-28", steps = 8800, distanceMeters = 6500.0, calories = 330.0, activeMinutes = 46, waterIntakeMl = 2500)
    )

    private val sampleInput = CoachInputData(
        name = "Harish",
        steps = 6250,
        stepGoal = 8000,
        distanceMeters = 4700.0,
        caloriesBurned = 245.0,
        activeMinutes = 35,
        waterIntakeMl = 1800,
        waterGoalMl = 2500,
        heightCm = 176.0,
        weightKg = 68.0,
        bmi = 21.95,
        bmiCategory = "Normal weight",
        currentActivity = "Resting",
        activityLevel = "Moderately Active",
        age = 28,
        gender = "Male",
        weeklyActivities = sampleWeeklyHistory
    )

    // 1. Text Normalization Tests
    @Test
    fun testTextNormalization_AbbreviationsAndUnits() {
        val raw = "Today i did 5k steeps and 2L wter. wat shud i eat for protien?"
        val normalized = CoachTextNormalizer.normalize(raw)
        assertTrue(normalized.contains("5000"))
        assertTrue(normalized.contains("steps"))
        assertTrue(normalized.contains("water"))
        assertTrue(normalized.contains("what"))
        assertTrue(normalized.contains("should"))
        assertTrue(normalized.contains("protein"))
    }

    // 2. Entity Extraction Tests
    @Test
    fun testEntityExtraction() {
        val q = CoachTextNormalizer.normalize("I walked 5000 steps today, drank 2 litres of water and my weight is 68 kg")
        val entities = CoachEntityExtractor.extract(q)
        assertEquals(5000, entities.steps)
        assertEquals(2000, entities.waterMl)
        assertEquals(68.0, entities.weightKg ?: 0.0, 0.1)
    }

    // 3. Primary and Secondary Intent Detection Tests
    @Test
    fun testIntentDetection_Protein() {
        val q = CoachTextNormalizer.normalize("What should I eat for protein?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.PROTEIN, intent.primaryIntent)
    }

    @Test
    fun testIntentDetection_StepsTarget() {
        val q = CoachTextNormalizer.normalize("How many steps should I walk?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.STEPS, intent.primaryIntent)
    }

    @Test
    fun testIntentDetection_StepsIncrease() {
        val q = CoachTextNormalizer.normalize("How can I increase my steps?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.STEPS, intent.primaryIntent)
        assertEquals(CoachSecondaryIntent.HOW_TO, intent.secondaryIntent)
    }

    @Test
    fun testIntentDetection_StepsProgressCheck() {
        val q = CoachTextNormalizer.normalize("I walked 5000 steps today. Is that enough?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.STEPS, intent.primaryIntent)
        assertEquals(CoachSecondaryIntent.PROGRESS_CHECK, intent.secondaryIntent)
    }

    @Test
    fun testIntentDetection_Hydration() {
        val q = CoachTextNormalizer.normalize("How much water should I drink?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertTrue(intent.primaryIntent == CoachIntent.HYDRATION || intent.primaryIntent == CoachIntent.WATER)
    }

    @Test
    fun testIntentDetection_PostWorkoutFood() {
        val q = CoachTextNormalizer.normalize("What should I eat after exercise?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.FOOD, intent.primaryIntent)
        assertEquals(CoachSecondaryIntent.POST_WORKOUT, intent.secondaryIntent)
    }

    @Test
    fun testIntentDetection_Calories() {
        val q = CoachTextNormalizer.normalize("How many calories should I eat?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.CALORIES, intent.primaryIntent)
    }

    @Test
    fun testIntentDetection_BMI() {
        val q = CoachTextNormalizer.normalize("What is my BMI?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.BMI, intent.primaryIntent)
    }

    @Test
    fun testIntentDetection_FitnessScore() {
        val q = CoachTextNormalizer.normalize("Why is my fitness score low?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.FITNESS_SCORE, intent.primaryIntent)
    }

    @Test
    fun testIntentDetection_Analytics() {
        val q = CoachTextNormalizer.normalize("How did I perform this week?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.ANALYTICS, intent.primaryIntent)
    }

    @Test
    fun testIntentDetection_HomeWorkout() {
        val q = CoachTextNormalizer.normalize("Can I workout at home?")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.WORKOUT, intent.primaryIntent)
    }

    // 4. Tanglish / Tamil-English Tests
    @Test
    fun testTanglish_StepsIncrease() {
        val q = CoachTextNormalizer.normalize("steps increase panna enna panlam")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.STEPS, intent.primaryIntent)
    }

    @Test
    fun testTanglish_WeightLossFood() {
        val q = CoachTextNormalizer.normalize("weight loss ku enna sapdalam")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertTrue(intent.primaryIntent == CoachIntent.WEIGHT_LOSS || intent.primaryIntent == CoachIntent.FOOD)
    }

    @Test
    fun testTanglish_WaterIntake() {
        val q = CoachTextNormalizer.normalize("water evlo kudikanum")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertTrue(intent.primaryIntent == CoachIntent.HYDRATION || intent.primaryIntent == CoachIntent.WATER)
    }

    @Test
    fun testTanglish_ProteinFood() {
        val q = CoachTextNormalizer.normalize("protein ku enna sapdalam")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertTrue(intent.primaryIntent == CoachIntent.PROTEIN || intent.primaryIntent == CoachIntent.FOOD)
    }

    @Test
    fun testTanglish_TodayStepsProgress() {
        val q = CoachTextNormalizer.normalize("today 5000 steps nadanthiruken")
        val entities = CoachEntityExtractor.extract(q)
        val intent = CoachIntentDetector.detectIntent(q, entities, Pair(null, null))
        assertEquals(CoachIntent.STEPS, intent.primaryIntent)
        assertEquals(CoachSecondaryIntent.PROGRESS_CHECK, intent.secondaryIntent)
    }

    // 5. Conversation Memory Context Follow-up Tests
    @Test
    fun testContextFollowUp_HydrationAfterWalking() {
        val history = listOf(
            "How much water should I drink?" to "Your target is 2500 ml."
        )
        val response = OfflineAiCoachEngine.generateOfflineAnswer("What about after walking?", sampleInput, sampleProfile, history)
        assertTrue(response.contains("water") || response.contains("hydration") || response.contains("fluids") || response.contains("ml"))
    }

    @Test
    fun testContextFollowUp_HowManyShouldIAdd() {
        val history = listOf(
            "How can I increase my steps?" to "Add steps gradually."
        )
        val response = OfflineAiCoachEngine.generateOfflineAnswer("How many should I add?", sampleInput, sampleProfile, history)
        assertTrue(response.contains("step", ignoreCase = true) || response.contains("1,000", ignoreCase = true) || response.contains("habit", ignoreCase = true))
    }

    // 6. Data Consistency Test
    @Test
    fun testDataConsistency_WithDashboardMetrics() {
        // Steps = 6250, Goal = 8000, Remaining = 1750
        val answer = OfflineAiCoachEngine.generateOfflineAnswer("How many more steps?", sampleInput, sampleProfile, emptyList())
        assertTrue(answer.contains("6,250") || answer.contains("6250"))
        assertTrue(answer.contains("8,000") || answer.contains("8000"))
        assertTrue(answer.contains("1,750") || answer.contains("1750"))
    }

    // 7. BMI Calculation Test
    @Test
    fun testBmiCalculation_Accuracy() {
        // 68kg, 176cm -> BMI ~21.95
        val answer = OfflineAiCoachEngine.generateOfflineAnswer("What is my BMI?", sampleInput, sampleProfile, emptyList())
        assertTrue(answer.contains("21.95") || answer.contains("22"))
        assertTrue(answer.contains("Normal weight", ignoreCase = true))
    }

    // 8. Food Recommendation Database Test
    @Test
    fun testFoodDatabase_NutrientSearch() {
        val egg = CoachFoodDatabase.findFoodMatch("egg")
        assertNotNull(egg)
        assertEquals("PROTEIN", egg?.category)

        val idli = CoachFoodDatabase.findFoodMatch("idli")
        assertNotNull(idli)
        assertTrue(idli?.isVegetarian == true)

        val ragi = CoachFoodDatabase.findFoodMatch("ragi")
        assertNotNull(ragi)

        val potato = CoachFoodDatabase.findFoodMatch("potato")
        assertNotNull(potato)
    }

    // 9. Unknown Question Handling Test
    @Test
    fun testUnknownQuestion_Fallback() {
        val answer = OfflineAiCoachEngine.generateOfflineAnswer("Who won the 1998 world cup?", sampleInput, sampleProfile, emptyList())
        assertTrue(answer.contains("I'm designed mainly for fitness", ignoreCase = true))
    }

    // 10. Safety Filter Test
    @Test
    fun testSafetyFilter_Emergency() {
        val answer = OfflineAiCoachEngine.generateOfflineAnswer("I am having severe chest pain while running", sampleInput, sampleProfile, emptyList())
        assertTrue(answer.contains("Medical Advisory", ignoreCase = true) || answer.contains("physician", ignoreCase = true))
    }

    // 11. Anti-Repetition Test: 10 Distinct Fitness Questions
    @Test
    fun testAntiRepetition_TenDistinctFitnessQuestions() {
        val questions = listOf(
            "What should I eat for protein?",
            "How many steps should I walk?",
            "How can I increase my steps?",
            "How much water should I drink?",
            "What should I eat after exercise?",
            "How many calories should I eat?",
            "What is my BMI?",
            "Why is my fitness score low?",
            "How did I perform this week?",
            "Can I workout at home?"
        )

        val answers = mutableSetOf<String>()
        for (q in questions) {
            val ans = OfflineAiCoachEngine.generateOfflineAnswer(q, sampleInput, sampleProfile, emptyList())
            assertTrue(ans.isNotBlank())
            answers.add(ans)
        }
        // Verify all 10 answers are unique and distinct
        assertEquals(10, answers.size)
    }

    // 12. Strict Image-Grounded Multimodal Tests
    @Test
    fun testOcrNutritionLabelExtraction() {
        val labelSample = """
            Nutrition Facts
            Serving size 1 cup (240ml)
            Calories 150
            Total Fat 8g
            Saturated Fat 5g
            Total Carbohydrate 12g
            Dietary Fiber 0g
            Total Sugars 12g
            Protein 8g
            Sodium 120mg
            Ingredients: Grade A Pasteurized Milk, Vitamin D3
        """.trimIndent()

        val ocrResult = com.example.data.coach.vision.OcrTextExtractor.parseNutritionLabelText(labelSample)
        assertTrue(ocrResult.hasNutritionLabel)
        assertEquals(150, ocrResult.nutritionFacts?.caloriesKcal)
        assertEquals(8.0, ocrResult.nutritionFacts?.proteinGrams ?: 0.0, 0.1)
        assertEquals(12.0, ocrResult.nutritionFacts?.carbsGrams ?: 0.0, 0.1)
        assertEquals(8.0, ocrResult.nutritionFacts?.fatGrams ?: 0.0, 0.1)
    }

    @Test
    fun testRelevanceFilter_DirectWhatIsThisQuestion() {
        val detection = com.example.data.coach.vision.StrictVisionDetection(
            category = com.example.data.coach.DetectedMediaCategory.FOOD,
            identifiedName = "Banana",
            matchedFood = CoachFoodDatabase.findFoodMatch("banana"),
            isConfident = true,
            confidenceScore = 86,
            isFood = true,
            visualDescription = "Elongated yellow fruit"
        )

        val filtered = com.example.data.coach.vision.RelevanceFilter.filterAndRefineResponse(
            rawResponse = "",
            userPrompt = "What is this?",
            visionDetection = detection,
            ocrResult = null
        )

        assertEquals("This appears to be a **Banana**.", filtered)
    }

    @Test
    fun testRelevanceFilter_OCRProteinPriority() {
        val labelSample = "Nutrition Facts\nServing size 100g\nCalories 250\nProtein 24g\nTotal Carbohydrate 5g"
        val ocrResult = com.example.data.coach.vision.OcrTextExtractor.parseNutritionLabelText(labelSample)
        val detection = com.example.data.coach.vision.StrictVisionDetection(
            category = com.example.data.coach.DetectedMediaCategory.NUTRITION_LABEL,
            identifiedName = "Nutrition Label",
            matchedFood = null,
            isConfident = true,
            confidenceScore = 90,
            isFood = false,
            visualDescription = "Label"
        )

        val filtered = com.example.data.coach.vision.RelevanceFilter.filterAndRefineResponse(
            rawResponse = "",
            userPrompt = "How much protein is in this?",
            visionDetection = detection,
            ocrResult = ocrResult
        )

        assertTrue(filtered.contains("24 g of protein") || filtered.contains("24g"))
    }

    @Test
    fun testStrictGrounding_UnclearImageDoesNotDefaultToRasamOrDosa() {
        val unclearDetection = com.example.data.coach.vision.StrictVisionDetection(
            category = com.example.data.coach.DetectedMediaCategory.UNCLEAR,
            identifiedName = null,
            matchedFood = null,
            isConfident = false,
            confidenceScore = 20,
            isFood = false,
            visualDescription = "Unclear visual input",
            uncertaintyMessage = "I can't confidently identify the object in this image. Please upload a clearer photo."
        )

        val filtered = com.example.data.coach.vision.RelevanceFilter.filterAndRefineResponse(
            rawResponse = "",
            userPrompt = "What is this?",
            visionDetection = unclearDetection,
            ocrResult = null
        )

        assertTrue(filtered.contains("can't confidently identify") || filtered.contains("clearer photo"))
        assertTrue(!filtered.contains("Rasam", ignoreCase = true))
        assertTrue(!filtered.contains("Dosa", ignoreCase = true))
    }
}
