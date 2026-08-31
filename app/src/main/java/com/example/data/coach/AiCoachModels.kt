package com.example.data.coach

import android.net.Uri
import com.example.data.coach.food.ActiveFoodContext
import com.example.data.coach.food.RecognitionCandidate

enum class DetectedMediaCategory(val label: String) {
    FOOD("Food & Nutrition"),
    GYM_EQUIPMENT("Fitness & Gym Equipment"),
    FITNESS_DEVICE("Fitness Tracker & Wearable"),
    NUTRITION_LABEL("Nutrition Facts & Food Label"),
    MENU_OR_RESTAURANT("Restaurant & Menu"),
    GENERAL_WELLNESS("General Lifestyle & Wellness"),
    UNCLEAR("Unclear Image")
}

data class AiCoachResponse(
    val answerText: String,
    val detectedCategory: DetectedMediaCategory = DetectedMediaCategory.GENERAL_WELLNESS,
    val detectedFoodContext: ActiveFoodContext? = null,
    val possibleMatches: List<RecognitionCandidate> = emptyList(),
    val isAiGenerated: Boolean = true,
    val confidencePercent: Int = 85,
    val sessionId: String? = null,
    val requestId: String? = sessionId
)

data class CoachChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val messageText: String,
    val attachedImageUri: Uri? = null,
    val detectedCategory: DetectedMediaCategory? = null,
    val isError: Boolean = false,
    val followUpInquiries: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
    val sessionId: String? = null
)
