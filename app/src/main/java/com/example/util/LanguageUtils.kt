package com.example.util

object LanguageUtils {
    val SUPPORTED_LANGUAGES = listOf(
        "English",
        "Tamil (தமிழ்)",
        "Hindi (हिंदी)",
        "Telugu (తెలుగు)",
        "Malayalam (മലയാളം)",
        "Kannada (ಕನ್ನಡ)"
    )

    fun getTranslation(key: String, language: String): String {
        val langKey = when {
            language.contains("Tamil", ignoreCase = true) || language.contains("தமிழ்") -> "ta"
            language.contains("Hindi", ignoreCase = true) || language.contains("हिंदी") -> "hi"
            language.contains("Telugu", ignoreCase = true) || language.contains("తెలుగు") -> "te"
            language.contains("Malayalam", ignoreCase = true) || language.contains("മലയാളം") -> "ml"
            language.contains("Kannada", ignoreCase = true) || language.contains("കನ್ನಡ") -> "kn"
            else -> "en"
        }

        return translations[langKey]?.get(key) ?: translations["en"]?.get(key) ?: key
    }

    private val translations = mapOf(
        "en" to mapOf(
            "home" to "Home",
            "activity" to "Activity",
            "map" to "Map",
            "ai_coach" to "AI Coach",
            "analytics" to "Analytics",
            "goals" to "Goals",
            "health" to "Health",
            "profile" to "Profile",
            "settings" to "Settings",
            "sign_out" to "Sign Out",
            "steps" to "Steps",
            "calories" to "Calories",
            "distance" to "Distance",
            "water" to "Water",
            "language" to "Language",
            "system_settings" to "System Settings"
        ),
        "ta" to mapOf(
            "home" to "முகப்பு",
            "activity" to "செயல்பாடு",
            "map" to "வரைபடம்",
            "ai_coach" to "AI பயிற்றுவிப்பாளர்",
            "analytics" to "பகுப்பாய்வு",
            "goals" to "இலக்குகள்",
            "health" to "சுகாதாரம்",
            "profile" to "சுயவிவரம்",
            "settings" to "அமைப்புகள்",
            "sign_out" to "வெளியேறு",
            "steps" to "அடிகள்",
            "calories" to "கலோரிகள்",
            "distance" to "தூரம்",
            "water" to "தண்ணீர்",
            "language" to "மொழி",
            "system_settings" to "அமைப்புகள்"
        ),
        "hi" to mapOf(
            "home" to "मुख्य पृष्ठ",
            "activity" to "गतिविधि",
            "map" to "मानचित्र",
            "ai_coach" to "एआई कोच",
            "analytics" to "विश्लेषण",
            "goals" to "लक्ष्य",
            "health" to "स्वास्थ्य",
            "profile" to "प्रोफ़ाइल",
            "settings" to "सेटिंग्स",
            "sign_out" to "साइन आउट",
            "steps" to "कदम",
            "calories" to "कैलोरी",
            "distance" to "दूरी",
            "water" to "पानी",
            "language" to "भाषा",
            "system_settings" to "सिस्टम सेटिंग्स"
        ),
        "te" to mapOf(
            "home" to "హోమ్",
            "activity" to "కార్యాచరణ",
            "map" to "మ్యాప్",
            "ai_coach" to "AI కోచ్",
            "analytics" to "విశ్లేషణ",
            "goals" to "లక్ష్యాలు",
            "health" to "ఆరోగ్యం",
            "profile" to "ప్రొఫైల్",
            "settings" to "సెట్టింగ్‌లు",
            "sign_out" to "సైన్ అవుట్",
            "steps" to "అడుగులు",
            "calories" to "కేలరీలు",
            "distance" to "దూరం",
            "water" to "నీరు",
            "language" to "భాష",
            "system_settings" to "సిస్టమ్ సెట్టింగ్‌లు"
        ),
        "ml" to mapOf(
            "home" to "ഹോം",
            "activity" to "പ്രവർത്തനം",
            "map" to "മാപ്പ്",
            "ai_coach" to "എഐ കോച്ച്",
            "analytics" to "അനലിറ്റിക്സ്",
            "goals" to "ലക്ഷ്യങ്ങൾ",
            "health" to "ആരോഗ്യം",
            "profile" to "പ്രൊഫൈൽ",
            "settings" to "ക്രമീകരണങ്ങൾ",
            "sign_out" to "സൈൻ ഔട്ട്",
            "steps" to "സ്റ്റെപ്പുകൾ",
            "calories" to "കലോറി",
            "distance" to "ദൂരം",
            "water" to "വെള്ളം",
            "language" to "ഭാഷ",
            "system_settings" to "സിസ്റ്റം ക്രമീകരണങ്ങൾ"
        ),
        "kn" to mapOf(
            "home" to "ಮುಖ್ಯ ಪುಟ",
            "activity" to "ಚಟುವಟಿಕೆ",
            "map" to "ನಕ್ಷೆ",
            "ai_coach" to "AI ಕೋಚ್",
            "analytics" to "ವಿಶ್ಲೇಷಣೆ",
            "goals" to "ಗುರಿಗಳು",
            "health" to "ಆರೋಗ್ಯ",
            "profile" to "ಪ್ರೊಫೈಲ್",
            "settings" to "ಸೆಟ್ಟಿಂಗ್‌ಗಳು",
            "sign_out" to "ಸೈನ್ ಔಟ್",
            "steps" to "ಹೆಜ್ಜೆಗಳು",
            "calories" to "ಕ್ಯಾಲೋರಿಗಳು",
            "distance" to "ದೂರ",
            "water" to "నీరు",
            "language" to "ಭಾಷೆ",
            "system_settings" to "ಸಿಸ್ಟಮ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳು"
        )
    )
}
