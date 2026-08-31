package com.example.data.coach.gemini

import android.graphics.Bitmap
import com.example.BuildConfig
import com.example.data.coach.food.FoodImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiMultimodalClient {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks if a valid non-placeholder Gemini API Key is available in BuildConfig.
     */
    fun isGeminiConfigured(): Boolean {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return key.isNotBlank() && !key.contains("YOUR_") && !key.equals("placeholder", ignoreCase = true)
    }

    /**
     * Executes a multimodal generateContent request to the Gemini REST API.
     */
    suspend fun generateMultimodalContent(
        prompt: String,
        bitmap: Bitmap? = null,
        mimeType: String = "image/jpeg",
        systemInstructionText: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey.contains("YOUR_")) {
                return@withContext Result.failure(IllegalStateException("Gemini API key is not configured"))
            }

            // Build request payload
            val rootJson = JSONObject()

            // System Instruction
            if (!systemInstructionText.isNullOrBlank()) {
                val sysInstObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", systemInstructionText))
                sysInstObj.put("parts", partsArray)
                rootJson.put("systemInstruction", sysInstObj)
            }

            val contentsArray = JSONArray()

            // Append recent conversation turns for memory context (last 3 turns)
            val recentTurns = conversationHistory.takeLast(3)
            for (turn in recentTurns) {
                if (turn.first.isNotBlank()) {
                    val userTurn = JSONObject()
                    val uParts = JSONArray()
                    uParts.put(JSONObject().put("text", turn.first))
                    userTurn.put("role", "user")
                    userTurn.put("parts", uParts)
                    contentsArray.put(userTurn)
                }
                if (turn.second.isNotBlank()) {
                    val modelTurn = JSONObject()
                    val mParts = JSONArray()
                    mParts.put(JSONObject().put("text", turn.second))
                    modelTurn.put("role", "model")
                    modelTurn.put("parts", mParts)
                    contentsArray.put(modelTurn)
                }
            }

            // Current user turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            // Add text prompt
            if (prompt.isNotBlank()) {
                currentParts.put(JSONObject().put("text", prompt))
            }

            // Add image inline data if bitmap is provided
            if (bitmap != null) {
                val base64Data = FoodImageProcessor.bitmapToBase64(bitmap, quality = 85)
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", mimeType)
                inlineDataObj.put("data", base64Data)

                val imagePart = JSONObject()
                imagePart.put("inlineData", inlineDataObj)
                currentParts.put(imagePart)
            }

            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)
            rootJson.put("contents", contentsArray)

            // Generation config - strictly grounded with low temperature
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.1)
            genConfig.put("topP", 0.95)
            genConfig.put("maxOutputTokens", 1024)
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            // Modern recommended gemini-3.5-flash endpoint for high-speed grounded multimodal vision
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    RuntimeException("API Error (${response.code}): $responseBodyString")
                )
            }

            val responseJson = JSONObject(responseBodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text", "")
                        textBuilder.append(text)
                    }
                    val fullText = textBuilder.toString().trim()
                    if (fullText.isNotBlank()) {
                        return@withContext Result.success(fullText)
                    }
                }
            }

            Result.failure(RuntimeException("No valid content found in API response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
