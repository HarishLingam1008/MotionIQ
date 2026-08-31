package com.example.data.coach

import android.content.Context
import com.example.data.coach.context.ConversationContextManager
import com.example.data.coach.text.TextQuestionHandler
import com.example.data.local.UserProfile

object AiCoachEngine {

    /**
     * Text-only AI Coach Query Entrypoint.
     * Supports:
     * 1. Pure text questions
     * 2. Personalized health, fitness, nutrition, hydration, and activity advice.
     * 3. Multi-turn follow-up queries with conversational memory.
     */
    suspend fun askCoach(
        context: Context,
        question: String?,
        inputData: CoachInputData,
        userProfile: UserProfile?,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): AiCoachResponse {
        val trimmedQuery = question?.trim() ?: ""

        val textAnswer = TextQuestionHandler.answerTextQuestion(
            question = trimmedQuery,
            inputData = inputData,
            userProfile = userProfile,
            conversationHistory = conversationHistory
        )

        return AiCoachResponse(
            answerText = textAnswer,
            detectedCategory = DetectedMediaCategory.GENERAL_WELLNESS,
            detectedFoodContext = null,
            isAiGenerated = true
        )
    }

    /**
     * Daily health analytics calculation.
     */
    fun analyze(input: CoachInputData): AiCoachAnalysis {
        return OfflineAiCoachEngine.analyze(input)
    }

    /**
     * Personalized workout plan generation based on live step count and goals.
     */
    fun generateWorkoutTips(input: CoachInputData, userProfile: UserProfile?): PersonalizedWorkoutPlan {
        return OfflineAiCoachEngine.generateWorkoutTips(input, userProfile)
    }

    /**
     * Synchronous / offline answer helper for fast UI previews and test cases.
     */
    fun generateOfflineAnswer(
        question: String,
        input: CoachInputData,
        userProfile: UserProfile?,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String {
        return OfflineAiCoachEngine.generateOfflineAnswer(
            question = question,
            input = input,
            userProfile = userProfile,
            conversationHistory = conversationHistory,
            activeFoodContext = null
        )
    }
}
