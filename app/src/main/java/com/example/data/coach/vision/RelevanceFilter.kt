package com.example.data.coach.vision

import com.example.data.coach.DetectedMediaCategory
import java.text.DecimalFormat

object RelevanceFilter {

    private val df1 = DecimalFormat("#.#")

    /**
     * Filters and shapes the AI response so that it answers strictly what the user asked,
     * grounded in the actual image analysis and OCR evidence.
     */
    fun filterAndRefineResponse(
        rawResponse: String,
        userPrompt: String?,
        visionDetection: StrictVisionDetection,
        ocrResult: OcrExtractionResult?
    ): String {
        val q = userPrompt?.trim()?.lowercase() ?: ""
        val isSimpleId = q == "what is this" || q == "what is this?" || q == "what is it" ||
                q == "what is it?" || q == "what food is this" || q == "what food" ||
                q == "identify this" || q == "what do you see" || q.isBlank()

        // 1. If vision analysis indicated uncertainty or image validation failed
        if (!visionDetection.isConfident && visionDetection.uncertaintyMessage != null && rawResponse.isBlank()) {
            return visionDetection.uncertaintyMessage
        }

        // 2. Multimodal API response priority: If the multimodal vision engine provided a response, use it directly
        if (rawResponse.isNotBlank()) {
            return if (isSimpleId) {
                sanitizeRawIdentification(rawResponse)
            } else {
                rawResponse.trim()
            }
        }

        // 3. OCR Nutrition Label Priority (Offline / On-Device)
        if (ocrResult?.hasNutritionLabel == true && ocrResult.nutritionFacts != null) {
            val facts = ocrResult.nutritionFacts
            if (q.contains("protein")) {
                return if (facts.proteinGrams != null) {
                    "The nutrition label shows **${df1.format(facts.proteinGrams)} g of protein** per serving (${facts.servingSize ?: "1 serving"})."
                } else {
                    "The visible nutrition label does not clearly specify the protein amount."
                }
            }

            if (q.contains("calorie") || q.contains("kcal") || q.contains("energy")) {
                return if (facts.caloriesKcal != null) {
                    "The nutrition label indicates **${facts.caloriesKcal} kcal** per serving (${facts.servingSize ?: "1 serving"})."
                } else {
                    "The visible nutrition label does not clearly display the calorie count."
                }
            }

            if (q.contains("carb") || q.contains("carbohydrate")) {
                return if (facts.carbsGrams != null) {
                    "The nutrition label shows **${df1.format(facts.carbsGrams)} g of total carbohydrates** per serving."
                } else {
                    "The visible nutrition label does not list total carbohydrates."
                }
            }

            if (q.contains("fat") || q.contains("oil")) {
                return if (facts.fatGrams != null) {
                    "The nutrition label shows **${df1.format(facts.fatGrams)} g of total fat**${if (facts.saturatedFatGrams != null) " (including ${df1.format(facts.saturatedFatGrams)}g saturated fat)" else ""} per serving."
                } else {
                    "The visible nutrition label does not clearly show total fat content."
                }
            }

            if (q.contains("label") || q.contains("read") || q.contains("what is this") || q.isBlank()) {
                val sb = StringBuilder()
                sb.append("### 📋 Nutrition Facts (Extracted from Label)\n\n")
                if (facts.servingSize != null) sb.append("• **Serving Size**: ${facts.servingSize}\n")
                if (facts.caloriesKcal != null) sb.append("• **Calories**: ${facts.caloriesKcal} kcal\n")
                if (facts.proteinGrams != null) sb.append("• **Protein**: ${facts.proteinGrams}g\n")
                if (facts.carbsGrams != null) sb.append("• **Carbohydrates**: ${facts.carbsGrams}g\n")
                if (facts.fatGrams != null) sb.append("• **Total Fat**: ${facts.fatGrams}g\n")
                if (facts.saturatedFatGrams != null) sb.append("• **Saturated Fat**: ${facts.saturatedFatGrams}g\n")
                if (facts.fiberGrams != null) sb.append("• **Dietary Fiber**: ${facts.fiberGrams}g\n")
                if (facts.sugarsGrams != null) sb.append("• **Sugars**: ${facts.sugarsGrams}g\n")
                if (facts.sodiumMg != null) sb.append("• **Sodium**: ${facts.sodiumMg} mg\n")
                return sb.toString().trim()
            }
        }

        // 4. User asked simple identification question: "What is this?" / "What food is this?"
        if (isSimpleId) {
            if (visionDetection.isConfident && visionDetection.identifiedName != null) {
                return if (visionDetection.isFood) {
                    "This appears to be a **${visionDetection.identifiedName}**."
                } else {
                    "This appears to be **${visionDetection.identifiedName}**."
                }
            }
            return visionDetection.uncertaintyMessage ?: "I can't confidently identify the object in this image. Please upload a clearer photo."
        }

        // 5. Non-food object question
        if (!visionDetection.isFood && visionDetection.category != DetectedMediaCategory.FOOD) {
            return if (visionDetection.isConfident && visionDetection.identifiedName != null) {
                "This appears to be **${visionDetection.identifiedName}**. This is not a food item."
            } else {
                visionDetection.uncertaintyMessage ?: "I can't confidently identify this object. Please upload a clearer photo."
            }
        }

        // 6. Specific nutritional questions for confirmed food items (On-Device)
        if (visionDetection.isConfident && visionDetection.isFood) {
            val food = visionDetection.matchedFood
            if (food != null) {
                if (q.contains("calorie") || q.contains("kcal")) {
                    return "A standard serving of **${food.name}** (${food.portion}) contains approximately **${food.caloriesKcal} kcal**."
                }
                if (q.contains("protein")) {
                    return "A standard serving of **${food.name}** (${food.portion}) provides approximately **${df1.format(food.proteinG)} g of protein**."
                }
                if (q.contains("carb") || q.contains("carbohydrate")) {
                    return "A standard serving of **${food.name}** (${food.portion}) provides approximately **${df1.format(food.carbsG)} g of carbohydrates**."
                }
                if (q.contains("fat")) {
                    return "A standard serving of **${food.name}** (${food.portion}) contains approximately **${df1.format(food.fatG)} g of fat**."
                }
                if (q.contains("weight loss") || q.contains("fat loss") || q.contains("lose weight")) {
                    return "### 🥗 Weight Loss Assessment: **${food.name}**\n\n" +
                            "• **Calorie Profile**: ~${food.caloriesKcal} kcal (${food.portion}).\n" +
                            "• **Fit**: ${food.weightLossAdvice}\n" +
                            "• **Tip**: Pair with a protein source to increase satiety."
                }
                if (q.contains("weight gain") || q.contains("gain weight") || q.contains("bulking")) {
                    return "### 🥩 Weight Gain Assessment: **${food.name}**\n\n" +
                            "• **Calorie Profile**: ~${food.caloriesKcal} kcal (${food.portion}).\n" +
                            "• **Fit**: ${food.weightGainAdvice}"
                }
                return "This appears to be **${food.name}** (~${food.caloriesKcal} kcal per ${food.portion})."
            }
        }

        return visionDetection.uncertaintyMessage
            ?: "I couldn't reliably analyze this image. Please upload a clearer image and try again."
    }

    private fun sanitizeRawIdentification(rawText: String): String {
        val lines = rawText.trim().lines()
        // If the model gave a concise response (<= 3 lines), keep it
        if (lines.size <= 3) return rawText.trim()

        // Otherwise take the first substantive identification paragraph
        val firstParagraph = lines.takeWhile { it.isNotBlank() }.joinToString("\n")
        return if (firstParagraph.isNotBlank()) firstParagraph else lines.first()
    }
}
