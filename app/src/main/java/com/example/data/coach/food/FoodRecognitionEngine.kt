package com.example.data.coach.food

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.coach.CoachFoodDatabase
import com.example.data.coach.FoodNutrientInfo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class RecognitionConfidenceLevel {
    HIGH_CONFIDENCE,
    POSSIBLE_MATCH,
    LOW_CONFIDENCE,
    UNCLEAR_OR_NON_FOOD,
    ERROR
}

data class RecognitionCandidate(
    val food: FoodNutrientInfo,
    val confidencePercent: Int,
    val matchReason: String
)

data class FoodRecognitionResult(
    val status: RecognitionConfidenceLevel,
    val primaryFood: FoodNutrientInfo?,
    val confidencePercent: Int,
    val possibleMatches: List<RecognitionCandidate>,
    val detectedMultiItems: List<FoodNutrientInfo>,
    val diagnosticMessage: String
)

object FoodRecognitionEngine {

    /**
     * Performs pure on-device, offline visual feature analysis and classification
     * on the provided food image Bitmap.
     */
    fun analyzeFoodImage(bitmap: Bitmap): FoodRecognitionResult {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 10 || height < 10) {
                return FoodRecognitionResult(
                    status = RecognitionConfidenceLevel.ERROR,
                    primaryFood = null,
                    confidencePercent = 0,
                    possibleMatches = emptyList(),
                    detectedMultiItems = emptyList(),
                    diagnosticMessage = "Image is too small or invalid."
                )
            }

            // 1. Primary Engine: On-Device ML Kit Neural Vision Model
            val mlKitResult = com.example.data.coach.vision.LocalMlKitVisionEngine.classifyImageSync(bitmap)
            if (mlKitResult.isConfident && mlKitResult.isFood && mlKitResult.matchedFood != null) {
                val candidate = RecognitionCandidate(
                    food = mlKitResult.matchedFood,
                    confidencePercent = mlKitResult.confidenceScore,
                    matchReason = "On-device ML Vision: ${mlKitResult.visualDescription}"
                )
                return FoodRecognitionResult(
                    status = if (mlKitResult.confidenceScore >= 75) RecognitionConfidenceLevel.HIGH_CONFIDENCE else RecognitionConfidenceLevel.POSSIBLE_MATCH,
                    primaryFood = mlKitResult.matchedFood,
                    confidencePercent = mlKitResult.confidenceScore,
                    possibleMatches = listOf(candidate),
                    detectedMultiItems = emptyList(),
                    diagnosticMessage = "Identified: **${mlKitResult.matchedFood.name}** with ${mlKitResult.confidenceScore}% confidence."
                )
            } else if (mlKitResult.isConfident && !mlKitResult.isFood) {
                return FoodRecognitionResult(
                    status = RecognitionConfidenceLevel.UNCLEAR_OR_NON_FOOD,
                    primaryFood = null,
                    confidencePercent = mlKitResult.confidenceScore,
                    possibleMatches = emptyList(),
                    detectedMultiItems = emptyList(),
                    diagnosticMessage = "Detected non-food object: **${mlKitResult.identifiedName ?: "Non-food item"}**. Please upload a photo of food."
                )
            } else if (mlKitResult.topLabels.isNotEmpty() && !mlKitResult.isConfident) {
                return FoodRecognitionResult(
                    status = RecognitionConfidenceLevel.UNCLEAR_OR_NON_FOOD,
                    primaryFood = null,
                    confidencePercent = mlKitResult.confidenceScore,
                    possibleMatches = emptyList(),
                    detectedMultiItems = emptyList(),
                    diagnosticMessage = mlKitResult.uncertaintyMessage ?: "I can't confidently identify the food in this image. Please upload a clearer photo or select manually."
                )
            }

            // 2. Secondary Heuristic Fallback Pipeline (for testing or unclassified items)
            // Downscale to 128x128 for efficient and consistent feature extraction
            val scaled = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
            val pixels = IntArray(128 * 128)
            scaled.getPixels(pixels, 0, 128, 0, 0, 128, 128)

            // Extract Color & Texture Metrics
            var totalBrightness = 0.0
            var whiteRatio = 0.0
            var yellowGoldenRatio = 0.0
            var redOrangeRatio = 0.0
            var greenRatio = 0.0
            var brownEarthyRatio = 0.0
            var purpleRatio = 0.0
            var unnaturalColorRatio = 0.0

            val hsv = FloatArray(3)
            val totalPixels = 128 * 128

            for (p in pixels) {
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0] // 0..360
                val sat = hsv[1] // 0..1
                val value = hsv[2] // 0..1

                totalBrightness += value

                // Classify color bucket
                when {
                    // White / Cream / Light Gray
                    sat < 0.20f && value > 0.55f -> whiteRatio++

                    // Green / Leafy
                    hue in 65f..165f && sat > 0.22f -> greenRatio++

                    // Yellow / Golden / Turmeric
                    hue in 33f..65f && sat > 0.25f && value > 0.35f -> yellowGoldenRatio++

                    // Red / Tomato / Carrot / Orange
                    ((hue in 0f..33f) || (hue in 345f..360f)) && sat > 0.30f && value > 0.30f -> redOrangeRatio++

                    // Deep Purple / Beetroot
                    hue in 280f..345f && sat > 0.35f -> purpleRatio++

                    // Brown / Roasted / Earthy / Baked
                    hue in 15f..45f && sat in 0.20f..0.75f && value in 0.20f..0.65f -> brownEarthyRatio++

                    // Unnatural Blues / Cyan (Rare in natural food)
                    hue in 175f..275f && sat > 0.35f -> unnaturalColorRatio++
                }
            }

            val avgBrightness = totalBrightness / totalPixels
            whiteRatio /= totalPixels
            yellowGoldenRatio /= totalPixels
            redOrangeRatio /= totalPixels
            greenRatio /= totalPixels
            brownEarthyRatio /= totalPixels
            purpleRatio /= totalPixels
            unnaturalColorRatio /= totalPixels

            // Calculate Texture Roughness using Gradient Variation
            var gradientSum = 0.0
            for (y in 0 until 127) {
                for (x in 0 until 127) {
                    val p = pixels[y * 128 + x]
                    val pRight = pixels[y * 128 + (x + 1)]
                    val pDown = pixels[(y + 1) * 128 + x]

                    val l1 = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3.0
                    val l2 = (Color.red(pRight) + Color.green(pRight) + Color.blue(pRight)) / 3.0
                    val l3 = (Color.red(pDown) + Color.green(pDown) + Color.blue(pDown)) / 3.0

                    gradientSum += abs(l1 - l2) + abs(l1 - l3)
                }
            }
            val roughness = (gradientSum / (127 * 127 * 255.0)).coerceIn(0.0, 1.0).toFloat()

            // Calculate Center vs Edge Circularity
            var centerColorDiff = 0.0
            var centerCount = 0
            var edgeCount = 0
            var centerBrightness = 0.0
            var edgeBrightness = 0.0

            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    val dx = x - 64
                    val dy = y - 64
                    val dist = sqrt((dx * dx + dy * dy).toDouble())
                    val p = pixels[y * 128 + x]
                    val b = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3.0 / 255.0

                    if (dist < 40) {
                        centerBrightness += b
                        centerCount++
                    } else if (dist > 50) {
                        edgeBrightness += b
                        edgeCount++
                    }
                }
            }
            val avgCenterB = if (centerCount > 0) centerBrightness / centerCount else 0.5
            val avgEdgeB = if (edgeCount > 0) edgeBrightness / edgeCount else 0.5
            val circularityContrast = abs(avgCenterB - avgEdgeB).toFloat()

            // Quality & Anomaly Checks
            if (avgBrightness < 0.12) {
                return FoodRecognitionResult(
                    status = RecognitionConfidenceLevel.UNCLEAR_OR_NON_FOOD,
                    primaryFood = null,
                    confidencePercent = 15,
                    possibleMatches = emptyList(),
                    detectedMultiItems = emptyList(),
                    diagnosticMessage = "The image is too dark. Please take a clearer photo with good lighting or select the food manually."
                )
            }

            if (unnaturalColorRatio > 0.45) {
                return FoodRecognitionResult(
                    status = RecognitionConfidenceLevel.UNCLEAR_OR_NON_FOOD,
                    primaryFood = null,
                    confidencePercent = 20,
                    possibleMatches = emptyList(),
                    detectedMultiItems = emptyList(),
                    diagnosticMessage = "I couldn't identify this as a known food item. Please choose the food manually from our database."
                )
            }

            // Score against each food in CoachFoodDatabase
            val candidateScores = mutableListOf<RecognitionCandidate>()
            val allFoods = CoachFoodDatabase.foods

            for (food in allFoods) {
                var score = 0.0
                var matchDetail = ""

                // Color alignment
                when (food.dominantColorGroup) {
                    "WHITE" -> {
                        score += whiteRatio * 45.0
                        if (whiteRatio > 0.25) matchDetail = "White/creamy tone detected"
                    }
                    "YELLOW" -> {
                        score += (yellowGoldenRatio * 45.0) + (whiteRatio * 10.0)
                        if (yellowGoldenRatio > 0.20) matchDetail = "Golden/yellow color spectrum detected"
                    }
                    "RED_ORANGE" -> {
                        score += redOrangeRatio * 50.0
                        if (redOrangeRatio > 0.20) matchDetail = "Rich tomato/red-orange spectrum detected"
                    }
                    "GREEN" -> {
                        score += greenRatio * 55.0
                        if (greenRatio > 0.20) matchDetail = "Vibrant green chlorophyll tone detected"
                    }
                    "BROWN" -> {
                        score += (brownEarthyRatio * 40.0) + (yellowGoldenRatio * 15.0)
                        if (brownEarthyRatio > 0.20) matchDetail = "Earthy roasted brown color detected"
                    }
                    "PURPLE" -> {
                        score += purpleRatio * 60.0
                        if (purpleRatio > 0.15) matchDetail = "Deep beetroot/purple pigment detected"
                    }
                    else -> score += 15.0
                }

                // Roughness alignment (difference penalty)
                val roughnessDiff = abs(roughness - food.visualRoughness)
                val roughnessBonus = (1.0 - (roughnessDiff * 1.5)).coerceIn(0.0, 1.0) * 25.0
                score += roughnessBonus

                // Circularity alignment
                val circDiff = abs(circularityContrast - (food.visualCircularity * 0.4f))
                val circBonus = (1.0 - (circDiff * 2.0)).coerceIn(0.0, 1.0) * 15.0
                score += circBonus

                // Specific Food Fine-Tuning heuristics
                when (food.name) {
                    "Dosa (Plain / Roast)" -> {
                        if (yellowGoldenRatio > 0.25 && brownEarthyRatio > 0.15) score += 15.0
                        if (circularityContrast > 0.15) score += 10.0
                    }
                    "Idli" -> {
                        if (whiteRatio > 0.35 && roughness < 0.30) score += 20.0
                        if (circularityContrast > 0.15) score += 10.0
                    }
                    "Chapati / Whole Wheat Roti" -> {
                        if (brownEarthyRatio > 0.25 && circularityContrast > 0.15) score += 15.0
                    }
                    "Cooked Rice (White / Brown)" -> {
                        if (whiteRatio > 0.35 && roughness > 0.35) score += 20.0
                    }
                    "Curd Rice (Thayir Sadam)" -> {
                        if (whiteRatio > 0.40 && roughness in 0.20..0.45) score += 15.0
                    }
                    "Lemon Rice (Elumichai Sadam)" -> {
                        if (yellowGoldenRatio > 0.35 && roughness > 0.35) score += 20.0
                    }
                    "Tomato Rice (Thakkali Sadam)" -> {
                        if (redOrangeRatio > 0.30 && roughness > 0.35) score += 20.0
                    }
                    "Sambar" -> {
                        if (redOrangeRatio > 0.20 && brownEarthyRatio > 0.20 && roughness < 0.35) score += 18.0
                    }
                    "Spinach / Palak" -> {
                        if (greenRatio > 0.40) score += 25.0
                    }
                    "Boiled Egg" -> {
                        if (whiteRatio > 0.35 && circularityContrast > 0.20 && roughness < 0.25) score += 20.0
                    }
                    "Omelette / Egg Bhurji" -> {
                        if (yellowGoldenRatio > 0.30 && roughness in 0.25..0.55) score += 18.0
                    }
                    "Beetroot" -> {
                        if (purpleRatio > 0.20) score += 30.0
                    }
                    "Banana" -> {
                        if (yellowGoldenRatio > 0.40 && roughness < 0.30) score += 20.0
                    }
                    "Watermelon" -> {
                        if (redOrangeRatio > 0.30 && greenRatio > 0.10) score += 25.0
                    }
                }

                val finalConfidence = score.coerceIn(10.0, 94.0).toInt()
                candidateScores.add(
                    RecognitionCandidate(
                        food = food,
                        confidencePercent = finalConfidence,
                        matchReason = if (matchDetail.isNotBlank()) matchDetail else "Visual feature pattern matching"
                    )
                )
            }

            candidateScores.sortByDescending { it.confidencePercent }
            val topCandidate = candidateScores.firstOrNull()

            // Detect Multi-Food Items (e.g. Rice + Sambar/Dal or Dosa + Sambar)
            val detectedMultiItems = mutableListOf<FoodNutrientInfo>()
            if (whiteRatio > 0.30 && (redOrangeRatio > 0.20 || brownEarthyRatio > 0.20)) {
                val rice = allFoods.firstOrNull { it.name.contains("Rice", ignoreCase = true) && !it.name.contains("Lemon") && !it.name.contains("Tomato") }
                val sambar = allFoods.firstOrNull { it.name.equals("Sambar", ignoreCase = true) }
                if (rice != null && sambar != null) {
                    detectedMultiItems.add(rice)
                    detectedMultiItems.add(sambar)
                }
            } else if (yellowGoldenRatio > 0.30 && redOrangeRatio > 0.18) {
                val dosa = allFoods.firstOrNull { it.name.contains("Dosa", ignoreCase = true) }
                val sambar = allFoods.firstOrNull { it.name.equals("Sambar", ignoreCase = true) }
                if (dosa != null && sambar != null) {
                    detectedMultiItems.add(dosa)
                    detectedMultiItems.add(sambar)
                }
            }

            val topConfidence = topCandidate?.confidencePercent ?: 0
            val status = when {
                topConfidence >= 75 -> RecognitionConfidenceLevel.HIGH_CONFIDENCE
                topConfidence >= 60 -> RecognitionConfidenceLevel.POSSIBLE_MATCH
                topConfidence >= 30 -> RecognitionConfidenceLevel.LOW_CONFIDENCE
                else -> RecognitionConfidenceLevel.UNCLEAR_OR_NON_FOOD
            }

            val isConfirmed = status == RecognitionConfidenceLevel.HIGH_CONFIDENCE || status == RecognitionConfidenceLevel.POSSIBLE_MATCH

            val message = when (status) {
                RecognitionConfidenceLevel.HIGH_CONFIDENCE ->
                    "Identified: **${topCandidate?.food?.name}** with ${topConfidence}% confidence."
                RecognitionConfidenceLevel.POSSIBLE_MATCH ->
                    "Possible match: **${topCandidate?.food?.name}** (Confidence: ${topConfidence}%)."
                RecognitionConfidenceLevel.LOW_CONFIDENCE, RecognitionConfidenceLevel.UNCLEAR_OR_NON_FOOD ->
                    "I can't confidently identify the food in this image. Please upload a clearer photo or select manually."
                RecognitionConfidenceLevel.ERROR ->
                    "Error analyzing image."
            }

            FoodRecognitionResult(
                status = status,
                primaryFood = if (isConfirmed) topCandidate?.food else null,
                confidencePercent = if (isConfirmed) topConfidence else 0,
                possibleMatches = if (isConfirmed) candidateScores.take(5) else emptyList(),
                detectedMultiItems = if (isConfirmed) detectedMultiItems else emptyList(),
                diagnosticMessage = message
            )
        } catch (e: Exception) {
            e.printStackTrace()
            FoodRecognitionResult(
                status = RecognitionConfidenceLevel.ERROR,
                primaryFood = null,
                confidencePercent = 0,
                possibleMatches = emptyList(),
                detectedMultiItems = emptyList(),
                diagnosticMessage = "Unexpected error during local image processing: ${e.localizedMessage}"
            )
        }
    }
}
