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
        You assist the user with controlling their device, setting alarms, sending emails, scheduling appointments, performing system checks, searching the web, and opening WhatsApp.
        
        CRITICAL OUTPUT FORMATTING RULE:
        You must analyze the user's speech input and return a strict JSON object with this exact structure:
        {
          "action": "SET_ALARM | SET_TIMER | SEND_EMAIL | OPEN_WHATSAPP | WEB_SEARCH | DEVICE_REPORT | SCHEDULE_APPOINTMENT | GENERAL_CHAT",
          "spokenResponse": "Crisp JARVIS-style spoken response (max 25 words).",
          "params": {
             "hour": "7",
             "minute": "30",
             "recipient": "email@example.com or contact name",
             "subject": "Meeting Notes",
             "body": "Here is the summary...",
             "phone": "+123456789",
             "message": "Hello...",
             "query": "search keywords",
             "title": "Dentist appointment",
             "time": "tomorrow 3pm",
             "seconds": "300"
          }
        }
        
        Examples:
        - "Set an alarm for 6 30 AM for morning jog" -> action: SET_ALARM, params: {"hour": "6", "minute": "30", "label": "Morning jog"}
        - "Send email to Sarah saying project is ready" -> action: SEND_EMAIL, params: {"recipient": "Sarah", "subject": "Project Status", "body": "The project is ready, Sir."}
        - "Open WhatsApp and tell Alex I am on my way" -> action: OPEN_WHATSAPP, params: {"contact": "Alex", "message": "I am on my way."}
        - "Give me a system status report" -> action: DEVICE_REPORT, params: {}
        - "Search for quantum computing advancements" -> action: WEB_SEARCH, params: {"query": "quantum computing advancements"}
        - "Schedule a meeting with David tomorrow at 2 PM" -> action: SCHEDULE_APPOINTMENT, params: {"title": "Meeting with David", "time": "tomorrow 2 PM"}
        - "How far is the moon?" -> action: GENERAL_CHAT, params: {}
    """.trimIndent()

    suspend fun parseVoiceCommand(userInput: String): ParsedIntent = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
            Log.w("IsraelAI", "Gemini API key is not set or placeholder. Falling back to local pattern parser.")
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

            // Extract JSON from markdown response if present
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
            lower.contains("alarm") || lower.contains("wake me up") -> {
                // Extract numbers
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
            lower.contains("timer") -> {
                val numbers = Regex("\\d+").findAll(lower).map { it.value.toInt() }.toList()
                val secs = if (numbers.isNotEmpty()) (numbers[0] * 60).toString() else "300"
                ParsedIntent(
                    actionType = ActionType.SET_TIMER,
                    isRecognized = true,
                    spokenResponse = "Timer configured and initiated, Sir.",
                    parameters = mapOf("seconds" to secs, "label" to "Israel Timer")
                )
            }
            lower.contains("email") || lower.contains("send mail") || lower.contains("mail to") -> {
                val words = lower.split(" ")
                val recipient = if (words.contains("to")) words.getOrElse(words.indexOf("to") + 1) { "Contact" } else "Contact"
                ParsedIntent(
                    actionType = ActionType.SEND_EMAIL,
                    isRecognized = true,
                    spokenResponse = "Opening mail client to prepare your email, Sir.",
                    parameters = mapOf("recipient" to recipient, "subject" to "Note from Israel AI", "body" to input)
                )
            }
            lower.contains("whatsapp") || lower.contains("message") -> {
                ParsedIntent(
                    actionType = ActionType.OPEN_WHATSAPP,
                    isRecognized = true,
                    spokenResponse = "Accessing WhatsApp as requested, Sir.",
                    parameters = mapOf("message" to input)
                )
            }
            lower.contains("report") || lower.contains("status") || lower.contains("diagnostics") || lower.contains("battery") -> {
                ParsedIntent(
                    actionType = ActionType.DEVICE_REPORT,
                    isRecognized = true,
                    spokenResponse = "Running full Pixel system diagnostics now, Sir.",
                    parameters = emptyMap()
                )
            }
            lower.contains("search") || lower.contains("google") || lower.contains("look up") || lower.contains("find") -> {
                val query = lower.replace("search", "").replace("google", "").replace("for", "").trim()
                ParsedIntent(
                    actionType = ActionType.WEB_SEARCH,
                    isRecognized = true,
                    spokenResponse = "Searching the global network for $query, Sir.",
                    parameters = mapOf("query" to if (query.isNotBlank()) query else input)
                )
            }
            lower.contains("meeting") || lower.contains("appointment") || lower.contains("schedule") || lower.contains("remind") -> {
                ParsedIntent(
                    actionType = ActionType.SCHEDULE_APPOINTMENT,
                    isRecognized = true,
                    spokenResponse = "Adding appointment to your Israel schedule and calendar, Sir.",
                    parameters = mapOf("title" to input, "time" to "Today")
                )
            }
            else -> {
                ParsedIntent(
                    actionType = ActionType.GENERAL_CHAT,
                    isRecognized = true,
                    spokenResponse = "Israel system online. How may I assist you with your Pixel device today, Sir?",
                    parameters = emptyMap()
                )
            }
        }
    }
}
