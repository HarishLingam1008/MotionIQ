package com.example.data.coach

object CoachSafetyFilter {

    /**
     * Checks if the query indicates acute medical danger or emergency symptoms.
     */
    fun checkEmergency(normalizedQuery: String): String? {
        val q = normalizedQuery.lowercase()
        if (q.contains("chest pain") || q.contains("heart pain") || q.contains("severe breathing") ||
            q.contains("shortness of breath") || q.contains("fainted") || q.contains("passed out") ||
            q.contains("severe dizziness") || q.contains("paralysis") || q.contains("coughing blood")) {
            return """
                ⚠️ **Medical & Safety Notice**
                
                If you are experiencing chest pain, severe shortness of breath, sudden dizziness, or fainting, please stop physical activity immediately and consult an emergency medical physician or local healthcare facility.
                
                MotionIQ is a fitness coaching assistant and cannot provide emergency medical diagnosis or treatment.
            """.trimIndent()
        }

        if (q.contains("prescribe") || q.contains("insulin dose") || q.contains("blood pressure medicine") ||
            q.contains("cure diabetes") || q.contains("cure cancer") || q.contains("steroids")) {
            return """
                ⚠️ **Clinical Guidance Notice**
                
                MotionIQ provides lifestyle, exercise, and nutritional guidance for general wellness. We cannot prescribe medications, adjust clinical dosages, or provide medical diagnoses.
                
                Please consult your physician or registered dietitian for clinical treatment plans.
            """.trimIndent()
        }

        return null
    }

    /**
     * Ensures calculated calorie floors are safe and sustainable.
     */
    fun applyCalorieSafetyFloor(targetKcal: Int, isFemale: Boolean): Int {
        val minFloor = if (isFemale) 1200 else 1500
        return targetKcal.coerceAtLeast(minFloor)
    }
}
