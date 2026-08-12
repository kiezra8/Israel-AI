package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ReminderEntity
import com.example.data.remote.ActionType
import com.example.data.remote.GeminiRepository
import com.example.data.remote.ParsedIntent
import com.example.device.PixelDeviceController
import com.example.service.JarvisForegroundService
import com.example.voice.IsraelSpeechManager
import com.example.voice.IsraelSpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class IsraelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val reminderDao = db.reminderDao()
    private val chatDao = db.chatMessageDao()
    private val geminiRepo = GeminiRepository()
    private val pixelController = PixelDeviceController(application)

    val reminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatHistory: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _israelResponse = MutableStateFlow("Israel Protocol initialized. At your service, Sir. Tap the core or say a command.")
    val israelResponse: StateFlow<String> = _israelResponse.asStateFlow()

    private val _speechState = MutableStateFlow(IsraelSpeechState.IDLE)
    val speechState: StateFlow<IsraelSpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _pitch = MutableStateFlow(0.9f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _rate = MutableStateFlow(1.05f)
    val rate: StateFlow<Float> = _rate.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private var speechManager: IsraelSpeechManager? = null

    init {
        // Start foreground service for reliable background execution
        try {
            JarvisForegroundService.startService(application)
        } catch (e: Exception) {
            Log.e("IsraelViewModel", "Failed to start foreground service", e)
        }

        speechManager = IsraelSpeechManager(
            context = application,
            onVoiceResult = { text -> processVoiceCommand(text) },
            onWakeWordDetected = {
                speakText("Yes, Sir? Israel listening.")
            }
        )

        viewModelScope.launch {
            speechManager?.speechState?.collect { state ->
                _speechState.value = state
            }
        }
        viewModelScope.launch {
            speechManager?.rmsDb?.collect { db ->
                _rmsDb.value = db
            }
        }
        viewModelScope.launch {
            speechManager?.partialTranscript?.collect { text ->
                if (text.isNotBlank()) {
                    _userTranscript.value = text
                }
            }
        }
    }

    fun onReactorClick() {
        if (_speechState.value == IsraelSpeechState.SPEAKING) {
            speechManager?.stopSpeaking()
        } else if (_speechState.value == IsraelSpeechState.LISTENING) {
            speechManager?.stopListening()
        } else {
            speechManager?.startListening()
        }
    }

    fun processVoiceCommand(userInput: String) {
        if (userInput.isBlank()) return

        _userTranscript.value = userInput
        _speechState.value = IsraelSpeechState.THINKING

        viewModelScope.launch {
            // Store user message in Room DB
            chatDao.insertMessage(
                ChatMessageEntity(
                    sender = "USER",
                    text = userInput,
                    timestamp = System.currentTimeMillis()
                )
            )

            val parsedIntent = geminiRepo.parseVoiceCommand(userInput)
            executeIntentActions(parsedIntent, userInput)
        }
    }

    private suspend fun executeIntentActions(parsedIntent: ParsedIntent, rawInput: String) {
        val params = parsedIntent.parameters
        val resultMessage: String

        when (parsedIntent.actionType) {
            ActionType.OPEN_APP -> {
                val appName = params["appName"] ?: rawInput.replace("open", "").trim()
                resultMessage = pixelController.openAppByName(appName)
            }

            ActionType.SET_ALARM -> {
                val hour = params["hour"]?.toIntOrNull() ?: 7
                val min = params["minute"]?.toIntOrNull() ?: 0
                val label = params["label"] ?: "Israel Alarm"
                resultMessage = pixelController.setAlarmAndReminder(hour, min, label)

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                }
                reminderDao.insertReminder(
                    ReminderEntity(
                        title = "Alarm: $label at $hour:${if (min < 10) "0$min" else min}",
                        description = "Scheduled via voice command",
                        timeInMillis = cal.timeInMillis,
                        category = "ALARM"
                    )
                )
            }

            ActionType.MAKE_CALL -> {
                val contact = params["contact"] ?: rawInput.replace("call", "").trim()
                resultMessage = pixelController.makePhoneCall(contact)
            }

            ActionType.SEND_SMS -> {
                val contact = params["contact"] ?: "Contact"
                val message = params["message"] ?: rawInput
                resultMessage = pixelController.sendSms(contact, message)
            }

            ActionType.READ_NOTIFICATIONS -> {
                resultMessage = pixelController.readNotificationsAloud()
            }

            ActionType.TOGGLE_SETTING -> {
                val setting = params["setting"] ?: "flashlight"
                val enable = params["enable"]?.toBooleanStrictOrNull() ?: true
                resultMessage = pixelController.toggleSystemSettings(setting, enable)
            }

            ActionType.WHATSAPP_REPLY -> {
                val msg = params["message"] ?: ""
                resultMessage = pixelController.readAndRespondWhatsApp(if (msg.isNotBlank()) msg else null)
            }

            ActionType.DEVICE_STATUS -> {
                resultMessage = pixelController.checkDeviceStatus()
            }

            ActionType.WEB_SEARCH -> {
                val query = params["query"] ?: rawInput
                resultMessage = pixelController.searchWebAndSummarize(query)
            }

            ActionType.GENERAL_CHAT -> {
                resultMessage = parsedIntent.spokenResponse
            }
        }

        _israelResponse.value = resultMessage

        // Save assistant response in Room DB
        chatDao.insertMessage(
            ChatMessageEntity(
                sender = "ISRAEL",
                text = resultMessage,
                timestamp = System.currentTimeMillis(),
                actionType = parsedIntent.actionType.name
            )
        )

        speakText(resultMessage)
    }

    fun speakText(text: String) {
        speechManager?.speak(text)
    }

    fun stopSpeaking() {
        speechManager?.stopSpeaking()
    }

    fun toggleReminderComplete(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderDao.deleteReminderById(id)
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            reminderDao.deleteAll()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
        }
    }

    fun updatePitch(newPitch: Float) {
        _pitch.value = newPitch
        speechManager?.speechPitch = newPitch
    }

    fun updateRate(newRate: Float) {
        _rate.value = newRate
        speechManager?.speechRate = newRate
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}
