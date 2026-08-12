package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.4f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 1000,
    @Json(name = "topP") val topP: Float? = 0.95f
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

/**
 * Parsed Voice Command Intent produced by Gemini NLP parser
 */
data class ParsedIntent(
    val actionType: ActionType,
    val isRecognized: Boolean,
    val spokenResponse: String,
    val parameters: Map<String, String> = emptyMap()
)

enum class ActionType {
    SET_ALARM,
    SET_TIMER,
    SEND_EMAIL,
    OPEN_WHATSAPP,
    WEB_SEARCH,
    DEVICE_REPORT,
    SCHEDULE_APPOINTMENT,
    GENERAL_CHAT
}
