package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val modelTag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Generates a deep life simulation analysis using gemini-3.1-pro-preview with HIGH thinking level.
     */
    suspend fun generateDeepSimulation(prompt: String): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-pro-preview"
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            }
            put("contents", contentsArr)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are an elite Life & Financial Decision Simulator. You provide deep strategic life forecasting, quantitative net worth calculations, risk analysis, and psychological stress impact predictions. You evaluate multiple paths with rigorous critical thinking, avoiding generic advice. Return structured, detailed analysis.")
                    })
                })
            })
            // High thinking configuration for gemini-3.1-pro-preview - no maxOutputTokens set
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingLevel", "high")
                })
                put("temperature", 0.7)
            })
        }

        callGeminiApi(model, requestJson)
    }

    /**
     * Fast, low-latency response using gemini-3.1-flash-lite for instant teasers or mood tips.
     */
    suspend fun generateFastAdvice(prompt: String): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-flash-lite"
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            }
            put("contents", contentsArr)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        callGeminiApi(model, requestJson)
    }

    /**
     * Multi-turn chat generation supporting gemini-3.1-pro-preview, gemini-3.5-flash, or gemini-3.1-flash-lite,
     * with optional Search or Maps grounding.
     */
    suspend fun generateChatReply(
        modelName: String,
        messages: List<ChatMessage>,
        systemInstructionText: String,
        enableSearchGrounding: Boolean = false,
        enableMapsGrounding: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray()
            messages.forEach { msg ->
                val partObj = JSONObject().apply { put("text", msg.content) }
                val contentObj = JSONObject().apply {
                    put("role", msg.role)
                    put("parts", JSONArray().apply { put(partObj) })
                }
                contentsArr.put(contentObj)
            }
            put("contents", contentsArr)

            if (systemInstructionText.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionText) })
                    })
                })
            }

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                if (modelName == "gemini-3.1-pro-preview") {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "high")
                    })
                }
            }
            put("generationConfig", genConfig)

            // Grounding tools if requested (using gemini-3.5-flash)
            if (enableSearchGrounding || enableMapsGrounding) {
                val toolsArr = JSONArray()
                if (enableSearchGrounding) {
                    toolsArr.put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                if (enableMapsGrounding) {
                    toolsArr.put(JSONObject().apply {
                        put("googleMaps", JSONObject())
                    })
                }
                put("tools", toolsArr)
            }
        }

        callGeminiApi(modelName, requestJson)
    }

    private fun callGeminiApi(model: String, requestJson: JSONObject): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("API_KEY_NOT_CONFIGURED")
        }

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API failed code=${response.code}: $responseBody")
            throw RuntimeException("API error ${response.code}: $responseBody")
        }

        return parseCandidateText(responseBody)
    }

    private fun parseCandidateText(jsonStr: String): String {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    val t = p.optString("text", "")
                    sb.append(t)
                }
                return sb.toString().trim()
            }
        }
        return "No response generated."
    }
}
