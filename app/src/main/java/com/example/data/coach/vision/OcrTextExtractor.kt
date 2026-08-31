package com.example.data.coach.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ExtractedNutritionFacts(
    val servingSize: String? = null,
    val caloriesKcal: Int? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val saturatedFatGrams: Double? = null,
    val fiberGrams: Double? = null,
    val sugarsGrams: Double? = null,
    val sodiumMg: Double? = null
)

data class OcrExtractionResult(
    val hasVisibleText: Boolean,
    val rawExtractedText: String,
    val hasNutritionLabel: Boolean,
    val nutritionFacts: ExtractedNutritionFacts?,
    val detectedProductName: String? = null,
    val ingredientsList: List<String> = emptyList()
)

object OcrTextExtractor {

    /**
     * Executes real on-device ML Kit Text Recognition on the bitmap.
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognized = visionText.text.trim()
                    continuation.resume(recognized)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        } catch (e: Throwable) {
            continuation.resume("")
        }
    }

    /**
     * Parses raw OCR / Multimodal text into structured nutrition label metrics,
     * ensuring exact numbers are extracted without inventing values.
     */
    fun parseNutritionLabelText(text: String): OcrExtractionResult {
        if (text.isBlank()) {
            return OcrExtractionResult(
                hasVisibleText = false,
                rawExtractedText = "",
                hasNutritionLabel = false,
                nutritionFacts = null
            )
        }

        val lower = text.lowercase()
        val hasLabelKeyword = lower.contains("nutrition") ||
                lower.contains("nutrition facts") ||
                lower.contains("calories") ||
                lower.contains("serving size") ||
                lower.contains("per 100g") ||
                lower.contains("per serving") ||
                (lower.contains("protein") && lower.contains("fat") && lower.contains("carbohydrate"))

        var cal: Int? = null
        var protein: Double? = null
        var carbs: Double? = null
        var fat: Double? = null
        var satFat: Double? = null
        var fiber: Double? = null
        var sugars: Double? = null
        var sodium: Double? = null
        var servingSize: String? = null

        // 1. Calories / Energy Regex
        val calPattern = Pattern.compile("(?i)(?:calories|energy|kcal)[^0-9]{0,10}(\\d{1,4})")
        val calMatcher = calPattern.matcher(text)
        if (calMatcher.find()) {
            cal = calMatcher.group(1)?.toIntOrNull()
        }

        // 2. Protein Regex
        val protPattern = Pattern.compile("(?i)protein[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*g")
        val protMatcher = protPattern.matcher(text)
        if (protMatcher.find()) {
            protein = protMatcher.group(1)?.toDoubleOrNull()
        }

        // 3. Carbohydrate Regex
        val carbsPattern = Pattern.compile("(?i)(?:total carbohydrate|carbohydrate|carbs)[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*g")
        val carbsMatcher = carbsPattern.matcher(text)
        if (carbsMatcher.find()) {
            carbs = carbsMatcher.group(1)?.toDoubleOrNull()
        }

        // 4. Total Fat Regex
        val fatPattern = Pattern.compile("(?i)(?:total fat|fat)[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*g")
        val fatMatcher = fatPattern.matcher(text)
        if (fatMatcher.find()) {
            fat = fatMatcher.group(1)?.toDoubleOrNull()
        }

        // 5. Saturated Fat Regex
        val satFatPattern = Pattern.compile("(?i)(?:saturated fat|sat fat)[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*g")
        val satFatMatcher = satFatPattern.matcher(text)
        if (satFatMatcher.find()) {
            satFat = satFatMatcher.group(1)?.toDoubleOrNull()
        }

        // 6. Dietary Fiber Regex
        val fiberPattern = Pattern.compile("(?i)(?:dietary fiber|fiber|fibre)[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*g")
        val fiberMatcher = fiberPattern.matcher(text)
        if (fiberMatcher.find()) {
            fiber = fiberMatcher.group(1)?.toDoubleOrNull()
        }

        // 7. Total Sugars Regex
        val sugarPattern = Pattern.compile("(?i)(?:total sugars|sugars|sugar)[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*g")
        val sugarMatcher = sugarPattern.matcher(text)
        if (sugarMatcher.find()) {
            sugars = sugarMatcher.group(1)?.toDoubleOrNull()
        }

        // 8. Sodium Regex
        val sodiumPattern = Pattern.compile("(?i)sodium[^0-9]{0,10}(\\d+(?:\\.\\d+)?)\\s*(?:mg|g)")
        val sodiumMatcher = sodiumPattern.matcher(text)
        if (sodiumMatcher.find()) {
            sodium = sodiumMatcher.group(1)?.toDoubleOrNull()
        }

        // 9. Serving Size Regex
        val servPattern = Pattern.compile("(?i)serving size[^\\n:]{0,10}[:\\s]+([^\\n,]+)")
        val servMatcher = servPattern.matcher(text)
        if (servMatcher.find()) {
            servingSize = servMatcher.group(1)?.trim()
        }

        // 10. Ingredients Extraction
        val ingredientsList = mutableListOf<String>()
        val ingIndex = lower.indexOf("ingredients:")
        if (ingIndex != -1) {
            val ingSubstring = text.substring(ingIndex + "ingredients:".length)
            val endIdx = ingSubstring.indexOf("\n\n").let { if (it == -1) ingSubstring.length else it }
            val cleanIng = ingSubstring.substring(0, endIdx).replace("\n", " ").trim()
            ingredientsList.addAll(cleanIng.split(",").map { it.trim() }.filter { it.isNotBlank() })
        }

        val facts = if (cal != null || protein != null || carbs != null || fat != null) {
            ExtractedNutritionFacts(
                servingSize = servingSize,
                caloriesKcal = cal,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                saturatedFatGrams = satFat,
                fiberGrams = fiber,
                sugarsGrams = sugars,
                sodiumMg = sodium
            )
        } else null

        return OcrExtractionResult(
            hasVisibleText = text.isNotBlank(),
            rawExtractedText = text,
            hasNutritionLabel = hasLabelKeyword && facts != null,
            nutritionFacts = facts,
            ingredientsList = ingredientsList
        )
    }

    /**
     * Checks if bitmap contains strong visual indicators of printed text / nutrition label
     * (high horizontal edge frequency, high contrast black-white rectangular zones).
     */
    fun detectVisualTextLabel(bitmap: Bitmap): Boolean {
        return try {
            val sample = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
            var highContrastTextPixels = 0
            var horizontalLineTransitions = 0

            for (y in 5 until 59) {
                var rowTransitions = 0
                for (x in 5 until 58) {
                    val p1 = sample.getPixel(x, y)
                    val p2 = sample.getPixel(x + 1, y)
                    val b1 = (Color.red(p1) + Color.green(p1) + Color.blue(p1)) / 3
                    val b2 = (Color.red(p2) + Color.green(p2) + Color.blue(p2)) / 3

                    if (Math.abs(b1 - b2) > 70) {
                        rowTransitions++
                        highContrastTextPixels++
                    }
                }
                if (rowTransitions > 6) {
                    horizontalLineTransitions++
                }
            }

            // High frequency horizontal contrast lines indicate printed lines of text/labels
            horizontalLineTransitions > 18 && highContrastTextPixels > 250
        } catch (e: Exception) {
            false
        }
    }
}
