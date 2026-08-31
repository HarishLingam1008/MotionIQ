package com.example.data.coach.vision

import android.graphics.Bitmap
import com.example.data.coach.CoachFoodDatabase
import com.example.data.coach.DetectedMediaCategory
import com.example.data.coach.FoodNutrientInfo
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class LocalVisionLabel(
    val text: String,
    val confidence: Float,
    val index: Int = 0
)

data class LocalVisionClassification(
    val topLabels: List<LocalVisionLabel>,
    val category: DetectedMediaCategory,
    val identifiedName: String?,
    val matchedFood: FoodNutrientInfo?,
    val isFood: Boolean,
    val isConfident: Boolean,
    val confidenceScore: Int,
    val visualDescription: String,
    val uncertaintyMessage: String? = null
)

/**
 * Real On-Device Computer Vision Engine powered by Google ML Kit Image Labeling.
 * Runs on-device quantized neural vision models offline without cloud/API requirements.
 */
object LocalMlKitVisionEngine {

    private val labelerOptions = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.35f)
        .build()

    /**
     * Executes real on-device ML Kit image classification on a Bitmap.
     */
    suspend fun classifyImage(bitmap: Bitmap): LocalVisionClassification = withContext(Dispatchers.Default) {
        val detectedLabels = try {
            val labeler = ImageLabeling.getClient(labelerOptions)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val task = labeler.process(inputImage)
            val mlKitLabels: List<ImageLabel> = Tasks.await(task, 4, TimeUnit.SECONDS)
            mlKitLabels.map { LocalVisionLabel(it.text, it.confidence, it.index) }
        } catch (e: Throwable) {
            emptyList()
        }

        // Map ML Kit output labels into strictly grounded domain categories
        return@withContext mapLabelsToDomain(detectedLabels, bitmap)
    }

    /**
     * Synchronous classification wrapper for blocking contexts or fallback pipelines.
     */
    fun classifyImageSync(bitmap: Bitmap): LocalVisionClassification {
        val detectedLabels = try {
            val labeler = ImageLabeling.getClient(labelerOptions)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val task = labeler.process(inputImage)
            val mlKitLabels: List<ImageLabel> = Tasks.await(task, 4, TimeUnit.SECONDS)
            mlKitLabels.map { LocalVisionLabel(it.text, it.confidence, it.index) }
        } catch (e: Throwable) {
            emptyList()
        }

        return mapLabelsToDomain(detectedLabels, bitmap)
    }

    fun mapLabelsForTesting(labels: List<LocalVisionLabel>, bitmap: Bitmap? = null): LocalVisionClassification {
        return mapLabelsToDomain(labels, bitmap)
    }

    private fun mapLabelsToDomain(
        labels: List<LocalVisionLabel>,
        bitmap: Bitmap?
    ): LocalVisionClassification {
        if (labels.isEmpty()) {
            // If ML Kit produced no confident labels or was unavailable in testing environment:
            return evaluateFallbackVisuals(bitmap)
        }

        val topLabel = labels.maxByOrNull { it.confidence } ?: labels.first()
        val topConfidence = topLabel.confidence

        // Look for Cake / Dessert / Pastry / Baked goods
        val isCakeOrDessert = labels.any {
            it.text.equals("Cake", ignoreCase = true) ||
            it.text.equals("Torte", ignoreCase = true) ||
            it.text.equals("Pastry", ignoreCase = true) ||
            it.text.equals("Dessert", ignoreCase = true) ||
            it.text.equals("Tart", ignoreCase = true) ||
            it.text.equals("Cupcake", ignoreCase = true) ||
            it.text.equals("Icing", ignoreCase = true) ||
            it.text.equals("Sweetness", ignoreCase = true)
        }
        if (isCakeOrDessert) {
            val cakeFood = CoachFoodDatabase.findFoodMatch("cake")
            val confInt = ((labels.firstOrNull { it.text.contains("Cake", true) }?.confidence ?: topConfidence) * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Cake / Pastry",
                matchedFood = cakeFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Baked dessert / slice of cake with sweet frosting."
            )
        }

        // Look for Apple / Fruit
        val isApple = labels.any {
            it.text.equals("Apple", ignoreCase = true) ||
            it.text.equals("Granny smith", ignoreCase = true) ||
            it.text.equals("Pome", ignoreCase = true)
        }
        if (isApple) {
            val appleFood = CoachFoodDatabase.findFoodMatch("apple")
            val confInt = ((labels.firstOrNull { it.text.contains("Apple", true) }?.confidence ?: topConfidence) * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Apple",
                matchedFood = appleFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Fresh round apple fruit."
            )
        }

        // Look for Banana
        val isBanana = labels.any {
            it.text.equals("Banana", ignoreCase = true) ||
            it.text.equals("Plantain", ignoreCase = true)
        }
        if (isBanana) {
            val bananaFood = CoachFoodDatabase.findFoodMatch("banana")
            val confInt = ((labels.firstOrNull { it.text.contains("Banana", true) }?.confidence ?: topConfidence) * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Banana",
                matchedFood = bananaFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Fresh yellow banana."
            )
        }

        // Look for Pizza
        val isPizza = labels.any { it.text.equals("Pizza", ignoreCase = true) }
        if (isPizza) {
            val pizzaFood = CoachFoodDatabase.findFoodMatch("pizza")
            val confInt = ((labels.firstOrNull { it.text.contains("Pizza", true) }?.confidence ?: topConfidence) * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Pizza",
                matchedFood = pizzaFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Baked pizza with crust and cheese toppings."
            )
        }

        // Look for Salad / Vegetables
        val isSalad = labels.any {
            it.text.equals("Salad", ignoreCase = true) ||
            it.text.equals("Leaf vegetable", ignoreCase = true)
        }
        if (isSalad) {
            val saladFood = CoachFoodDatabase.findFoodMatch("salad")
            val confInt = (topConfidence * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Fresh Garden Salad",
                matchedFood = saladFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Fresh bowl of mixed vegetable greens."
            )
        }

        // Look for Egg
        val isEgg = labels.any {
            it.text.equals("Egg", ignoreCase = true) ||
            it.text.equals("Boiled egg", ignoreCase = true) ||
            it.text.equals("Poached egg", ignoreCase = true)
        }
        if (isEgg) {
            val eggFood = CoachFoodDatabase.findFoodMatch("egg")
            val confInt = (topConfidence * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Boiled Egg",
                matchedFood = eggFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Boiled whole egg."
            )
        }

        // Look for Bread / Roti / Chapati
        val isBreadOrRoti = labels.any {
            it.text.equals("Bread", ignoreCase = true) ||
            it.text.equals("Flatbread", ignoreCase = true) ||
            it.text.equals("Roti", ignoreCase = true) ||
            it.text.equals("Chapati", ignoreCase = true) ||
            it.text.equals("Toast", ignoreCase = true)
        }
        if (isBreadOrRoti) {
            val rotiFood = CoachFoodDatabase.findFoodMatch("chapati")
            val confInt = (topConfidence * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Chapati / Whole Wheat Roti",
                matchedFood = rotiFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Whole wheat flatbread / roti."
            )
        }

        // Look for Rice / Grain Dish
        val isRice = labels.any {
            it.text.equals("Rice", ignoreCase = true) ||
            it.text.equals("Fried rice", ignoreCase = true) ||
            it.text.equals("Steamed rice", ignoreCase = true)
        }
        if (isRice) {
            val riceFood = CoachFoodDatabase.findFoodMatch("rice")
            val confInt = (topConfidence * 100).toInt().coerceIn(75, 98)
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = "Cooked Rice",
                matchedFood = riceFood,
                isFood = true,
                isConfident = true,
                confidenceScore = confInt,
                visualDescription = "Steamed cooked rice."
            )
        }

        // Match generic food in database by label text
        for (label in labels) {
            val match = CoachFoodDatabase.findFoodMatch(label.text)
            if (match != null && label.confidence >= 0.50f) {
                return LocalVisionClassification(
                    topLabels = labels,
                    category = DetectedMediaCategory.FOOD,
                    identifiedName = match.name,
                    matchedFood = match,
                    isFood = true,
                    isConfident = true,
                    confidenceScore = (label.confidence * 100).toInt().coerceIn(70, 95),
                    visualDescription = "Identified food item: ${match.name}."
                )
            }
        }

        // Look for Gym / Exercise Equipment
        val isGymEquipment = labels.any {
            it.text.contains("Dumbbell", true) ||
            it.text.contains("Barbell", true) ||
            it.text.contains("Exercise equipment", true) ||
            it.text.contains("Weight training", true) ||
            it.text.contains("Gym", true) ||
            it.text.contains("Treadmill", true) ||
            it.text.contains("Physical fitness", true) ||
            it.text.contains("Sports equipment", true)
        }
        if (isGymEquipment) {
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.GYM_EQUIPMENT,
                identifiedName = "Gym / Workout Equipment",
                matchedFood = null,
                isFood = false,
                isConfident = true,
                confidenceScore = 90,
                visualDescription = "Fitness apparatus / exercise equipment."
            )
        }

        // Look for Water Bottle / Drinkware
        val isBottle = labels.any {
            it.text.contains("Bottle", true) ||
            it.text.contains("Drinkware", true) ||
            it.text.contains("Water bottle", true)
        }
        if (isBottle) {
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.GENERAL_WELLNESS,
                identifiedName = "Water Bottle / Drinkware",
                matchedFood = null,
                isFood = false,
                isConfident = true,
                confidenceScore = 88,
                visualDescription = "Hydration water bottle or container."
            )
        }

        // Look for Smartwatch / Fitness Tracker
        val isWatch = labels.any {
            it.text.contains("Watch", true) ||
            it.text.contains("Smartwatch", true) ||
            it.text.contains("Clock", true)
        }
        if (isWatch) {
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FITNESS_DEVICE,
                identifiedName = "Smartwatch / Fitness Tracker",
                matchedFood = null,
                isFood = false,
                isConfident = true,
                confidenceScore = 88,
                visualDescription = "Wearable fitness tracker or watch."
            )
        }

        // Look for other Non-Food Objects
        val isNonFoodSubject = labels.any {
            it.text.contains("Dog", true) ||
            it.text.contains("Cat", true) ||
            it.text.contains("Animal", true) ||
            it.text.contains("Car", true) ||
            it.text.contains("Vehicle", true) ||
            it.text.contains("Shoe", true) ||
            it.text.contains("Footwear", true) ||
            it.text.contains("Computer", true) ||
            it.text.contains("Laptop", true) ||
            it.text.contains("Electronics", true) ||
            it.text.contains("Mobile phone", true) ||
            it.text.contains("Gadget", true) ||
            it.text.contains("Furniture", true) ||
            it.text.contains("Chair", true) ||
            it.text.contains("Tableware", true)
        }
        if (isNonFoodSubject) {
            val nonFoodName = labels.firstOrNull {
                !it.text.equals("Food", true) && !it.text.equals("Dish", true)
            }?.text ?: "Non-Food Object"

            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.GENERAL_WELLNESS,
                identifiedName = nonFoodName,
                matchedFood = null,
                isFood = false,
                isConfident = true,
                confidenceScore = (topConfidence * 100).toInt().coerceIn(75, 95),
                visualDescription = "Non-food item ($nonFoodName)."
            )
        }

        // Check if top label is food/dish without specific subtype
        val isGenericFood = labels.any {
            it.text.equals("Food", true) ||
            it.text.equals("Dish", true) ||
            it.text.equals("Cuisine", true) ||
            it.text.equals("Meal", true)
        }
        if (isGenericFood) {
            return LocalVisionClassification(
                topLabels = labels,
                category = DetectedMediaCategory.FOOD,
                identifiedName = null,
                matchedFood = null,
                isFood = true,
                isConfident = false,
                confidenceScore = 40,
                visualDescription = "Meal or food dish without specific classification.",
                uncertaintyMessage = "I see a food dish, but cannot determine the exact recipe with high certainty. Please select the food manually or specify what it is."
            )
        }

        // If top confidence is low or unclear:
        return LocalVisionClassification(
            topLabels = labels,
            category = DetectedMediaCategory.UNCLEAR,
            identifiedName = null,
            matchedFood = null,
            isFood = false,
            isConfident = false,
            confidenceScore = (topConfidence * 100).toInt().coerceAtMost(45),
            visualDescription = "Unconfirmed visual subject.",
            uncertaintyMessage = "I can't confidently identify the object in this image. Please upload a clearer photo."
        )
    }

    private fun evaluateFallbackVisuals(bitmap: Bitmap? = null): LocalVisionClassification {
        // Fallback to strict validation if ML Kit returned empty labels
        return LocalVisionClassification(
            topLabels = emptyList(),
            category = DetectedMediaCategory.UNCLEAR,
            identifiedName = null,
            matchedFood = null,
            isFood = false,
            isConfident = false,
            confidenceScore = 20,
            visualDescription = "Unconfirmed visual subject.",
            uncertaintyMessage = "I can't confidently identify the object in this image. Please upload a clearer photo."
        )
    }
}
