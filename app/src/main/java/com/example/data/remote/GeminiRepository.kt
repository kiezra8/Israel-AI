package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiRepository {

    private val systemInstructionText = """
        You are 'Israel', an advanced AI voice assistant inspired by JARVIS from Iron Man.
        Your tone is polite, concise, highly intelligent, crisp, and professional.
        You control the user's Pixel device using native tools.

        CRITICAL OUTPUT FORMATTING RULE:
        You must analyze the user's speech input and return a strict JSON object with this exact structure:
        {
          "action": "OPEN_APP | SET_ALARM | MAKE_CALL | SEND_SMS | READ_NOTIFICATIONS | TOGGLE_SETTING | WHATSAPP_REPLY | DEVICE_STATUS | WEB_SEARCH | GENERAL_CHAT",
          "spokenResponse": "Crisp JARVIS-style spoken response (max 25 words).",
          "params": {
             "appName": "YouTube",
             "hour": "7",
             "minute": "30",
             "contact": "John",
             "message": "I am on my way",
             "setting": "flashlight",
             "enable": "true",
             "query": "quantum computing advances"
          }
        }

        Examples:
        - "Open YouTube" -> action: OPEN_APP, params: {"appName": "YouTube"}
        - "Set an alarm for 6:30 AM" -> action: SET_ALARM, params: {"hour": "6", "minute": "30", "label": "Alarm"}
        - "Call Sarah" -> action: MAKE_CALL, params: {"contact": "Sarah"}
        - "Send SMS to Mom saying I will be home soon" -> action: SEND_SMS, params: {"contact": "Mom", "message": "I will be home soon"}
        - "Read my notifications" -> action: READ_NOTIFICATIONS, params: {}
        - "Turn on flashlight" -> action: TOGGLE_SETTING, params: {"setting": "flashlight", "enable": "true"}
        - "Reply on WhatsApp saying got it" -> action: WHATSAPP_REPLY, params: {"message": "got it"}
        - "Check battery and device status" -> action: DEVICE_STATUS, params: {}
        - "Search Google for latest tech news" -> action: WEB_SEARCH, params: {"query": "latest tech news"}
        - "Who was Albert Einstein?" -> action: GENERAL_CHAT, params: {}
    """.trimIndent()

    suspend fun parseVoiceCommand(userInput: String): ParsedIntent = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
            Log.w("IsraelAI", "Gemini API key is not set. Falling back to local pattern parser.")
            return@withContext parseLocalFallback(userInput)
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = userInput))
                    )
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
                generationConfig = GenerationConfig(temperature = 0.2f)
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (responseText.isNullOrBlank()) {
                return@withContext parseLocalFallback(userInput)
            }

            val cleanedJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonObject = JSONObject(cleanedJson)
            val actionStr = jsonObject.optString("action", "GENERAL_CHAT")
            val spokenResponse = jsonObject.optString("spokenResponse", "At your service, Sir.")
            val paramsObj = jsonObject.optJSONObject("params")
            val paramsMap = mutableMapOf<String, String>()

            if (paramsObj != null) {
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    paramsMap[k] = paramsObj.optString(k, "")
                }
            }

            val actionType = try {
                ActionType.valueOf(actionStr)
            } catch (e: Exception) {
                ActionType.GENERAL_CHAT
            }

            ParsedIntent(
                actionType = actionType,
                isRecognized = true,
                spokenResponse = spokenResponse,
                parameters = paramsMap
            )
        } catch (e: Exception) {
            Log.e("IsraelAI", "Error calling Gemini API: ${e.message}", e)
            parseLocalFallback(userInput)
        }
    }

    private fun parseLocalFallback(input: String): ParsedIntent {
        val lower = input.lowercase().trim()

        return when {
            lower.startsWith("open ") || lower.contains("launch ") -> {
                val appName = lower.replace("open", "").replace("launch", "").replace("app", "").trim()
                ParsedIntent(
                    actionType = ActionType.OPEN_APP,
                    isRecognized = true,
                    spokenResponse = "Opening $appName, Sir.",
                    parameters = mapOf("appName" to appName)
                )
            }
            lower.contains("alarm") || lower.contains("wake me up") -> {
                val numbers = Regex("\\d+").findAll(lower).map { it.value.toInt() }.toList()
                val hour = if (numbers.isNotEmpty()) numbers[0].toString() else "7"
                val minute = if (numbers.size > 1) numbers[1].toString() else "0"
                ParsedIntent(
                    actionType = ActionType.SET_ALARM,
                    isRecognized = true,
                    spokenResponse = "Setting an alarm for $hour:${if (minute.length == 1) "0$minute" else minute}, Sir.",
                    parameters = mapOf("hour" to hour, "minute" to minute, "label" to "Israel Alarm")
                )
            }
            lower.startsWith("call ") || lower.contains("phone call") || lower.contains("dial ") -> {
                val contact = lower.replace("call", "").replace("dial", "").replace("phone", "").replace("to", "").trim()
                ParsedIntent(
                    actionType = ActionType.MAKE_CALL,
                    isRecognized = true,
                    spokenResponse = "Initiating call to $contact, Sir.",
                    parameters = mapOf("contact" to contact)
                )
            }
            lower.contains("sms") || lower.contains("text message") || lower.contains("send message") -> {
                val words = lower.split(" ")
                val contact = if (words.contains("to")) words.getOrElse(words.indexOf("to") + 1) { "contact" } else "contact"
                ParsedIntent(
                    actionType = ActionType.SEND_SMS,
                    isRecognized = true,
                    spokenResponse = "Preparing SMS to $contact, Sir.",
                    parameters = mapOf("contact" to contact, "message" to input)
                )
            }
            lower.contains("notification") || lower.contains("read messages") || lower.contains("what did i miss") -> {
                ParsedIntent(
                    actionType = ActionType.READ_NOTIFICATIONS,
                    isRecognized = true,
                    spokenResponse = "Surfacing recent notifications, Sir.",
                    parameters = emptyMap()
                )
            }
            lower.contains("flashlight") || lower.contains("torch") || lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("volume") || lower.contains("brightness") -> {
                val setting = when {
                    lower.contains("flashlight") || lower.contains("torch") -> "flashlight"
                    lower.contains("wifi") -> "wifi"
                    lower.contains("bluetooth") -> "bluetooth"
                    lower.contains("volume") -> "volume"
                    else -> "brightness"
                }
                val enable = if (lower.contains("off") || lower.contains("disable")) "false" else "true"
                ParsedIntent(
                    actionType = ActionType.TOGGLE_SETTING,
                    isRecognized = true,
                    spokenResponse = "Adjusting $setting, Sir.",
                    parameters = mapOf("setting" to setting, "enable" to enable)
                )
            }
            lower.contains("whatsapp") -> {
                val msg = if (lower.contains("saying")) lower.substringAfter("saying").trim() else ""
                ParsedIntent(
                    actionType = ActionType.WHATSAPP_REPLY,
                    isRecognized = true,
                    spokenResponse = "Interacting with WhatsApp, Sir.",
                    parameters = mapOf("message" to msg)
                )
            }
            lower.contains("status") || lower.contains("battery") || lower.contains("diagnostics") || lower.contains("time") -> {
                ParsedIntent(
                    actionType = ActionType.DEVICE_STATUS,
                    isRecognized = true,
                    spokenResponse = "Retrieving Pixel device status report, Sir.",
                    parameters = emptyMap()
                )
            }
            lower.contains("search") || lower.contains("google") || lower.contains("look up") -> {
                val query = lower.replace("search", "").replace("google", "").replace("for", "").trim()
                ParsedIntent(
                    actionType = ActionType.WEB_SEARCH,
                    isRecognized = true,
                    spokenResponse = "Searching the global network for $query, Sir.",
                    parameters = mapOf("query" to if (query.isNotBlank()) query else input)
                )
            }
            else -> {
                ParsedIntent(
                    actionType = ActionType.GENERAL_CHAT,
                    isRecognized = true,
                    spokenResponse = "At your service, Sir. I am listening.",
                    parameters = emptyMap()
                )
            }
        }
    }
}
