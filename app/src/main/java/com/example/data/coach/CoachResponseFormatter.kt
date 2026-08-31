package com.example.data.coach

object CoachResponseFormatter {

    fun format(
        directAnswer: String,
        explanation: String? = null,
        personalizedAction: String? = null,
        followUp: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append(directAnswer.trim())

        if (!explanation.isNullOrBlank()) {
            sb.append("\n\n")
            sb.append(explanation.trim())
        }

        if (!personalizedAction.isNullOrBlank()) {
            sb.append("\n\n")
            sb.append("💡 **Actionable Step**: ").append(personalizedAction.trim())
        }

        if (!followUp.isNullOrBlank()) {
            sb.append("\n\n")
            sb.append("💬 ").append(followUp.trim())
        }

        return sb.toString()
    }

    fun formatUnknown(): String {
        return "I'm designed mainly for fitness, activity, nutrition, hydration and MotionIQ data. How can MotionIQ AI Coach assist you? Try asking me about your steps, workouts, calories, food, water or fitness progress."
    }
}
