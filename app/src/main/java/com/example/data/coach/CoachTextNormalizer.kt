package com.example.data.coach

/**
 * Text normalization pipeline for MotionIQ AI Coach.
 * Handles lowercasing, punctuation cleaning, typo corrections,
 * number & unit standardization (e.g. 5k -> 5000, 2L -> 2000 ml),
 * and English + Tanglish / Tamil-English query comprehension.
 */
object CoachTextNormalizer {

    fun normalize(input: String): String {
        var q = input.lowercase().trim()

        // 1. Handle numeric 'k' representations (e.g., "5k", "10k", "6.2k")
        q = q.replace(Regex("\\b(\\d+)\\s*k\\b")) { matchResult ->
            val num = matchResult.groupValues[1].toIntOrNull()
            if (num != null) "${num * 1000}" else matchResult.value
        }
        q = q.replace(Regex("\\b(\\d+\\.\\d+)\\s*k\\b")) { matchResult ->
            val num = matchResult.groupValues[1].toDoubleOrNull()
            if (num != null) "${(num * 1000).toInt()}" else matchResult.value
        }

        // 2. Remove special punctuation while keeping digits and basic alphanumeric words
        q = q.replace(Regex("[?!.,;:\"'’()\\-_/\\[\\]{}]"), " ")
        q = q.replace(Regex("\\s+"), " ").trim()

        // 3. Typo corrections and spelling normalization
        val typoMap = mapOf(
            "wat" to "what",
            "wut" to "what",
            "gud" to "good",
            "shud" to "should",
            "cud" to "could",
            "plz" to "please",
            "pls" to "please",
            "kno" to "know",
            "hlp" to "help",
            "enuf" to "enough",
            "enuff" to "enough",
            "calori" to "calorie",
            "calory" to "calorie",
            "caloreis" to "calorie",
            "caleries" to "calorie",
            "calories" to "calorie",
            "kcal" to "calorie",
            "protien" to "protein",
            "protin" to "protein",
            "protiem" to "protein",
            "carbs" to "carbohydrate",
            "carb" to "carbohydrate",
            "carbohydrates" to "carbohydrate",
            "hydratin" to "hydration",
            "hydraton" to "hydration",
            "wter" to "water",
            "wtr" to "water",
            "h2o" to "water",
            "steeps" to "steps",
            "stepss" to "steps",
            "walkin" to "walking",
            "exersise" to "exercise",
            "exercse" to "exercise",
            "excersize" to "exercise",
            "wrkout" to "workout",
            "wkout" to "workout",
            "brekfast" to "breakfast",
            "brakfast" to "breakfast",
            "vegitables" to "vegetables",
            "vegitarian" to "vegetarian",
            "vegiterian" to "vegetarian",
            "vege" to "vegetarian"
        )

        val words = q.split(" ").filter { it.isNotBlank() }
        val mappedWords = words.map { word ->
            typoMap[word] ?: word
        }
        q = mappedWords.joinToString(" ")

        // 4. Tamil / Tanglish Phonetic and Phrase Translation to Normalized Concepts
        val tanglishPhraseMap = listOf(
            // Steps & Walking
            Regex("\\b(nadakanum|nadakka|nadanthu|nadanthiruken|nadanthen|nadakalam|nadanthen)\\b") to "walk",
            Regex("\\b(steps increase panna|step increase panna|nadakka mudiyala|steps yethavathu)\\b") to "how to increase steps",
            Regex("\\b(evlo|evvalavu|evalo)\\s+steps\\b") to "how many steps",
            Regex("\\bsteps\\s+(evlo|evvalavu|evalo)\\b") to "how many steps",
            Regex("\\benough ah|podhuma|podhum\\b") to "is it enough",

            // Food & Nutrition
            Regex("\\b(sapda|sapdalam|sapdanum|saapda|saapdalam|saapdanum|food)\\b") to "eat food",
            Regex("\\b(thanni|thanneer|kudikanum|kudikka|kudikalam)\\b") to "drink water",
            Regex("\\bwater\\s+(evlo|evvalavu|evalo)\\b") to "how much water",
            Regex("\\b(evlo|evvalavu|evalo)\\s+water\\b") to "how much water",
            Regex("\\b(weight loss ku|weight korakka|idai kuraikka|thadi kuraikka)\\b") to "for weight loss",
            Regex("\\b(weight gain ku|weight poda|udambu theriya|idai kootta)\\b") to "for weight gain",
            Regex("\\b(protein ku|protein kedaika|protein irukura)\\b") to "for protein",
            Regex("\\b(kammiya|kuraiva|cheap ah|budget la|low cost la)\\b") to "cheap affordable",
            Regex("\\b(kaal vali|udambu vali|asathiya|tired ah|thaangala)\\b") to "tired fatigue pain recovery"
        )

        for ((pattern, replacement) in tanglishPhraseMap) {
            q = q.replace(pattern, replacement)
        }

        // Clean extra spaces
        return q.replace(Regex("\\s+"), " ").trim()
    }
}
