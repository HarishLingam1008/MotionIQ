package com.example.data.coach.context

import com.example.data.coach.CoachChatMessage
import com.example.data.coach.CoachIntent
import com.example.data.coach.CoachSecondaryIntent
import com.example.data.coach.CoachTextNormalizer
import com.example.data.coach.DetectedMediaCategory
import com.example.data.coach.food.ActiveFoodContext

object ConversationContextManager {

    /**
     * Inspects conversation history and active visual context to identify follow-up intent.
     */
    fun resolveFollowUpQuery(
        query: String,
        history: List<Pair<String, String>>,
        activeFoodContext: ActiveFoodContext?,
        activeCategory: DetectedMediaCategory?
    ): FollowUpContextResult {
        val q = query.lowercase().trim()
        val normalized = CoachTextNormalizer.normalize(q)
        val hasActiveFood = activeFoodContext != null && activeFoodContext.items.isNotEmpty()
        val lastTurn = history.lastOrNull()

        // 1. Follow-up: "Is this / is it good for weight loss / muscle gain?"
        if (q.contains("is this good for") || q.contains("is it good for") || q.contains("can i eat this") || q.contains("good for weight") || q.contains("good for fat loss")) {
            if (hasActiveFood) {
                return FollowUpContextResult(
                    isFoodFollowUp = true,
                    targetIntent = CoachIntent.FOOD_WEIGHT_LOSS,
                    resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
                )
            }
        }

        // 2. Follow-up: "How much protein is in this?" / "How many calories?" / "Is it healthy?"
        if (q.contains("how much protein") || q.contains("protein in this") || q.contains("how much protien")) {
            if (hasActiveFood) {
                return FollowUpContextResult(
                    isFoodFollowUp = true,
                    targetIntent = CoachIntent.FOOD_PROTEIN,
                    resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
                )
            }
        }

        if (q.contains("how many calories") || q.contains("calories in this") || q.contains("how much calories")) {
            if (hasActiveFood) {
                return FollowUpContextResult(
                    isFoodFollowUp = true,
                    targetIntent = CoachIntent.FOOD_CALORIES,
                    resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
                )
            }
        }

        if (q.contains("is this healthy") || q.contains("is it healthy") || q.contains("is this good")) {
            if (hasActiveFood) {
                return FollowUpContextResult(
                    isFoodFollowUp = true,
                    targetIntent = CoachIntent.FOOD_HEALTHINESS,
                    resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
                )
            }
        }

        // 3. Follow-up: "How can I make it healthier?" / "Healthier alternatives?" / "Make it cheaper"
        if (q.contains("healthier") || q.contains("make this healthier") || q.contains("alternatives") || q.contains("better option") || q.contains("cheaper")) {
            if (hasActiveFood) {
                return FollowUpContextResult(
                    isFoodFollowUp = true,
                    targetIntent = CoachIntent.FOOD_ALTERNATIVE,
                    resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
                )
            }
        }

        // 4. Follow-up: "Can I eat this for breakfast / dinner / lunch?"
        if (q.contains("breakfast") && hasActiveFood) {
            return FollowUpContextResult(
                isFoodFollowUp = true,
                targetIntent = CoachIntent.FOOD_BREAKFAST,
                resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
            )
        }
        if (q.contains("dinner") && hasActiveFood) {
            return FollowUpContextResult(
                isFoodFollowUp = true,
                targetIntent = CoachIntent.FOOD_DINNER,
                resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
            )
        }
        if (q.contains("lunch") && hasActiveFood) {
            return FollowUpContextResult(
                isFoodFollowUp = true,
                targetIntent = CoachIntent.FOOD_LUNCH,
                resolvedSubject = activeFoodContext?.items?.firstOrNull()?.food?.name ?: "this meal"
            )
        }

        // 5. Follow-up: "How to use this?" / "How many sets?" / "What muscles does it work?" (Gym equipment)
        if (q.contains("how many sets") || q.contains("how to use") || q.contains("target muscle") || q.contains("which muscle")) {
            if (activeCategory == DetectedMediaCategory.GYM_EQUIPMENT) {
                return FollowUpContextResult(
                    isGymEquipmentFollowUp = true,
                    targetIntent = CoachIntent.WORKOUT,
                    resolvedSubject = "gym equipment"
                )
            }
        }

        return FollowUpContextResult(
            isFoodFollowUp = false,
            isGymEquipmentFollowUp = false,
            targetIntent = null,
            resolvedSubject = null
        )
    }
}

data class FollowUpContextResult(
    val isFoodFollowUp: Boolean = false,
    val isGymEquipmentFollowUp: Boolean = false,
    val targetIntent: CoachIntent? = null,
    val resolvedSubject: String? = null
)
