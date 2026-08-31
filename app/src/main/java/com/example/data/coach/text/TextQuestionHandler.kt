package com.example.data.coach.text

import com.example.data.coach.CoachEntityExtractor
import com.example.data.coach.CoachInputData
import com.example.data.coach.CoachIntentDetector
import com.example.data.coach.CoachResponsePlanner
import com.example.data.coach.CoachTextNormalizer
import com.example.data.coach.CoachUserDataProvider
import com.example.data.coach.context.ConversationContextManager
import com.example.data.coach.food.ActiveFoodContext
import com.example.data.coach.gemini.GeminiMultimodalClient
import com.example.data.local.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TextQuestionHandler {

    /**
     * Answers a user text query with multi-turn context and personalization.
     */
    suspend fun answerTextQuestion(
        question: String,
        inputData: CoachInputData,
        userProfile: UserProfile?,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        activeFoodContext: ActiveFoodContext? = null
    ): String = withContext(Dispatchers.Default) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) {
            return@withContext "Please enter a question about your workouts, nutrition, hydration, or daily activity."
        }

        // 1. If Gemini API is configured and question is broad/conversational, query Gemini
        if (GeminiMultimodalClient.isGeminiConfigured() && trimmed.length > 5) {
            val name = userProfile?.name ?: inputData.name
            val goal = userProfile?.fitnessGoal ?: "General Fitness"
            val diet = userProfile?.dietPreference ?: "Balanced"
            val steps = inputData.steps
            val stepGoal = inputData.stepGoal

            val systemInstruction = """
                You are MotionIQ AI Coach, a supportive, knowledgeable, ChatGPT-like health, fitness, and nutrition assistant.
                User: $name | Fitness Goal: $goal | Diet: $diet | Today Steps: $steps / $stepGoal.
                Provide clear, concise, actionable advice using bullet points. Focus on sustainable healthy habits, Indian and global whole foods (dal, moong, eggs, chana, curd, vegetables), and safe workout guidance.
            """.trimIndent()

            val geminiRes = GeminiMultimodalClient.generateMultimodalContent(
                prompt = trimmed,
                bitmap = null,
                systemInstructionText = systemInstruction,
                conversationHistory = conversationHistory
            )

            if (geminiRes.isSuccess) {
                val text = geminiRes.getOrNull()?.trim()
                if (!text.isNullOrBlank()) {
                    return@withContext text
                }
            }
        }

        // 2. Local High-Intelligence Rule & NLP Engine Fallback
        val normalized = CoachTextNormalizer.normalize(trimmed)
        val entities = CoachEntityExtractor.extract(normalized)
        val contextResolution = ConversationContextManager.resolveFollowUpQuery(
            query = normalized,
            history = conversationHistory,
            activeFoodContext = activeFoodContext,
            activeCategory = null
        )

        val intentResolutionPair = if (contextResolution.targetIntent != null) {
            Pair(contextResolution.targetIntent, null)
        } else {
            Pair(null, null)
        }

        val intentResult = CoachIntentDetector.detectIntent(normalized, entities, intentResolutionPair)
        val userData = CoachUserDataProvider(inputData, userProfile)

        return@withContext CoachResponsePlanner.planResponse(
            intentResult = intentResult,
            entities = entities,
            userData = userData,
            history = conversationHistory,
            activeFoodContext = activeFoodContext
        )
    }
}
