package com.example.data.coach

data class ConversationTurn(
    val userQuestion: String,
    val assistantAnswer: String,
    val primaryIntent: CoachIntent? = null,
    val secondaryIntent: CoachSecondaryIntent? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

object CoachConversationContext {

    /**
     * Inspects recent conversation turns to resolve ambiguous follow-ups or pronouns.
     */
    fun resolveContextualIntent(
        currentNormalizedQuery: String,
        history: List<Pair<String, String>>
    ): Pair<CoachIntent?, CoachSecondaryIntent?> {
        val q = currentNormalizedQuery.lowercase().trim()
        val lastTurn = history.lastOrNull() ?: return Pair(null, null)
        val lastUserQ = CoachTextNormalizer.normalize(lastTurn.first)
        val lastAnswer = lastTurn.second.lowercase()

        // 1. Follow-up: "What about after walking?" or "What about after workout?"
        if (q.contains("what about after") || q.contains("after walking") || q.contains("after exercise") || q.contains("after run")) {
            if (lastUserQ.contains("water") || lastUserQ.contains("drink") || lastUserQ.contains("hydration") || lastAnswer.contains("hydration") || lastAnswer.contains("water")) {
                return Pair(CoachIntent.HYDRATION, CoachSecondaryIntent.RECOMMENDATION)
            }
            if (lastUserQ.contains("food") || lastUserQ.contains("eat") || lastUserQ.contains("protein") || lastAnswer.contains("meal") || lastAnswer.contains("protein")) {
                return Pair(CoachIntent.POST_WORKOUT, CoachSecondaryIntent.RECOMMENDATION)
            }
            return Pair(CoachIntent.RECOVERY, CoachSecondaryIntent.HOW_TO)
        }

        // 2. Follow-up: "How many should I add?" or "How much should I increase?"
        if (q.contains("how many should i add") || q.contains("how much should i increase") || q.contains("how many to add") || q.contains("how much to add")) {
            if (lastUserQ.contains("step") || lastUserQ.contains("walk") || lastAnswer.contains("step")) {
                return Pair(CoachIntent.STEPS, CoachSecondaryIntent.HOW_TO)
            }
            if (lastUserQ.contains("water") || lastAnswer.contains("water")) {
                return Pair(CoachIntent.WATER, CoachSecondaryIntent.DAILY_TARGET)
            }
            if (lastUserQ.contains("calorie") || lastAnswer.contains("calorie")) {
                return Pair(CoachIntent.CALORIES, CoachSecondaryIntent.CALCULATION)
            }
        }

        // 3. Follow-up: "Make it cheaper" or "Any cheaper options?"
        if (q.contains("cheaper") || q.contains("budget") || q.contains("low cost") || q.contains("cheap options")) {
            if (q.contains("dinner") || lastUserQ.contains("dinner") || lastAnswer.contains("dinner")) {
                return Pair(CoachIntent.DINNER, CoachSecondaryIntent.MEAL_PLAN)
            }
            if (q.contains("breakfast") || lastUserQ.contains("breakfast") || lastAnswer.contains("breakfast")) {
                return Pair(CoachIntent.BREAKFAST, CoachSecondaryIntent.MEAL_PLAN)
            }
            if (q.contains("lunch") || lastUserQ.contains("lunch") || lastAnswer.contains("lunch")) {
                return Pair(CoachIntent.LUNCH, CoachSecondaryIntent.MEAL_PLAN)
            }
            if (lastUserQ.contains("protein") || lastAnswer.contains("protein")) {
                return Pair(CoachIntent.PROTEIN, CoachSecondaryIntent.RECOMMENDATION)
            }
            return Pair(CoachIntent.FOOD, CoachSecondaryIntent.RECOMMENDATION)
        }

        // 4. Follow-up: "Give me a vegetarian option" or "Vegetarian options"
        if (q.contains("vegetarian") || q.contains("veg") || q.contains("veg option")) {
            if (lastUserQ.contains("dinner") || lastAnswer.contains("dinner")) {
                return Pair(CoachIntent.DINNER, CoachSecondaryIntent.MEAL_PLAN)
            }
            if (lastUserQ.contains("breakfast") || lastAnswer.contains("breakfast")) {
                return Pair(CoachIntent.BREAKFAST, CoachSecondaryIntent.MEAL_PLAN)
            }
            if (lastUserQ.contains("lunch") || lastAnswer.contains("lunch")) {
                return Pair(CoachIntent.LUNCH, CoachSecondaryIntent.MEAL_PLAN)
            }
            if (lastUserQ.contains("protein") || lastAnswer.contains("protein")) {
                return Pair(CoachIntent.PROTEIN, CoachSecondaryIntent.RECOMMENDATION)
            }
            return Pair(CoachIntent.FOOD, CoachSecondaryIntent.RECOMMENDATION)
        }

        // 5. Follow-up: "Is that good?" / "Is it enough?"
        if (q.contains("is that good") || q.contains("is it enough") || q.contains("is that enough") || q.contains("enough ah") || q.contains("podhuma")) {
            if (lastUserQ.contains("step") || lastAnswer.contains("step")) {
                return Pair(CoachIntent.STEPS, CoachSecondaryIntent.PROGRESS_CHECK)
            }
            if (lastUserQ.contains("water") || lastAnswer.contains("water")) {
                return Pair(CoachIntent.WATER, CoachSecondaryIntent.PROGRESS_CHECK)
            }
            if (lastUserQ.contains("calorie") || lastAnswer.contains("calorie")) {
                return Pair(CoachIntent.CALORIES, CoachSecondaryIntent.PROGRESS_CHECK)
            }
        }

        // 5. Follow-up: "What about vegetarian?" / "Veg options?"
        if (q.contains("what about veg") || q.contains("vegetarian") || q.contains("veg options")) {
            if (lastUserQ.contains("protein") || lastAnswer.contains("protein")) {
                return Pair(CoachIntent.PROTEIN, CoachSecondaryIntent.RECOMMENDATION)
            }
            return Pair(CoachIntent.FOOD, CoachSecondaryIntent.RECOMMENDATION)
        }

        return Pair(null, null)
    }
}
