package com.example.data.coach.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.coach.CoachFoodDatabase
import com.example.data.coach.DetectedMediaCategory
import com.example.data.coach.FoodNutrientInfo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class StrictVisionDetection(
    val category: DetectedMediaCategory,
    val identifiedName: String?,
    val matchedFood: FoodNutrientInfo?,
    val isConfident: Boolean,
    val confidenceScore: Int,
    val isFood: Boolean,
    val visualDescription: String,
    val uncertaintyMessage: String? = null
)

object StrictVisionAnalyzer {

    /**
     * Performs strict, on-device visual analysis without guessing or assuming.
     * Uses Google ML Kit on-device image labeling as the primary neural vision engine,
     * followed by strict physical image heuristics as a fallback.
     */
    fun analyzeVisualImage(bitmap: Bitmap): StrictVisionDetection {
        // 1. PRIMARY ENGINE: On-Device ML Kit Neural Vision Model
        val mlKitResult = LocalMlKitVisionEngine.classifyImageSync(bitmap)
        if (mlKitResult.isConfident) {
            return StrictVisionDetection(
                category = mlKitResult.category,
                identifiedName = mlKitResult.identifiedName,
                matchedFood = mlKitResult.matchedFood,
                isConfident = mlKitResult.isConfident,
                confidenceScore = mlKitResult.confidenceScore,
                isFood = mlKitResult.isFood,
                visualDescription = mlKitResult.visualDescription,
                uncertaintyMessage = mlKitResult.uncertaintyMessage
            )
        }

        // 2. OCR Visual Check: Printed Nutrition Label / Document
        if (OcrTextExtractor.detectVisualTextLabel(bitmap)) {
            return StrictVisionDetection(
                category = DetectedMediaCategory.NUTRITION_LABEL,
                identifiedName = "Nutrition Facts / Product Label",
                matchedFood = null,
                isConfident = true,
                confidenceScore = 88,
                isFood = false,
                visualDescription = "Contains visible printed text and nutrition facts formatting."
            )
        }

        // 3. If ML Kit processed the image but confidence is below strict threshold:
        if (mlKitResult.topLabels.isNotEmpty()) {
            return StrictVisionDetection(
                category = mlKitResult.category,
                identifiedName = mlKitResult.identifiedName,
                matchedFood = mlKitResult.matchedFood,
                isConfident = mlKitResult.isConfident,
                confidenceScore = mlKitResult.confidenceScore,
                isFood = mlKitResult.isFood,
                visualDescription = mlKitResult.visualDescription,
                uncertaintyMessage = mlKitResult.uncertaintyMessage ?: "I can't confidently identify the item in this image. Please upload a clearer photo."
            )
        }

        // 4. STRICT GROUNDING: Zero-guessing fallback when no vision model or label is available
        return StrictVisionDetection(
            category = DetectedMediaCategory.UNCLEAR,
            identifiedName = null,
            matchedFood = null,
            isConfident = false,
            confidenceScore = 0,
            isFood = false,
            visualDescription = "Unconfirmed visual subject.",
            uncertaintyMessage = "I can't confidently identify the item in this image. Please upload a clearer photo or specify what it is."
        )
    }
}
