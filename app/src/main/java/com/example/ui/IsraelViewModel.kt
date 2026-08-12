package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderEntity
import com.example.data.remote.ActionType
import com.example.data.remote.GeminiRepository
import com.example.data.remote.ParsedIntent
import com.example.device.PixelDeviceController
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
    private val geminiRepo = GeminiRepository()
    private val pixelController = PixelDeviceController(application)

    val reminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _israelResponse = MutableStateFlow("Israel Protocol initialized. At your service, Sir. Say 'Hey Israel' or tap the core to command.")
    val israelResponse: StateFlow<String> = _israelResponse.asStateFlow()

    private val _speechState = MutableStateFlow(IsraelSpeechState.IDLE)
    val speechState: StateFlow<IsraelSpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _pitch = MutableStateFlow(0.9f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _rate = MutableStateFlow(1.05f)
    val rate: StateFlow<Float> = _rate.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(true)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private var speechManager: IsraelSpeechManager? = null

    init {
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

    /**
     * Called when user taps the Arc Reactor or speaks a command
     */
    fun onReactorClick() {
        if (_speechState.value == IsraelSpeechState.SPEAKING) {
            speechManager?.stopSpeaking()
        } else if (_speechState.value == IsraelSpeechState.LISTENING) {
            speechManager?.stopListening()
        } else {
            speechManager?.startListening()
        }
    }

    /**
     * Core NLP & Command Execution Flow
     */
    fun processVoiceCommand(userInput: String) {
        _userTranscript.value = userInput
        _speechState.value = IsraelSpeechState.THINKING

        viewModelScope.launch {
            val parsedIntent = geminiRepo.parseVoiceCommand(userInput)
            executeIntentActions(parsedIntent, userInput)
        }
    }

    private suspend fun executeIntentActions(parsedIntent: ParsedIntent, rawInput: String) {
        val params = parsedIntent.parameters
        val pendingCount = reminderDao.getPendingCount()

        when (parsedIntent.actionType) {
            ActionType.SET_ALARM -> {
                val hour = params["hour"]?.toIntOrNull() ?: 7
                val min = params["minute"]?.toIntOrNull() ?: 0
                val label = params["label"] ?: "Israel Alarm"

                pixelController.setAlarm(hour, min, label)

                // Save to Room DB as appointment/alarm record
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                }
                reminderDao.insertReminder(
                    ReminderEntity(
                        title = "Alarm: $label at $hour:${if (min < 10) "0$min" else min}",
                        description = "Set via voice command",
                        timeInMillis = cal.timeInMillis,
                        category = "ALARM"
                    )
                )

                val reply = "Alarm set for $hour:${if (min < 10) "0$min" else min}, Sir."
                _israelResponse.value = reply
                speakText(reply)
            }

            ActionType.SET_TIMER -> {
                val secs = params["seconds"]?.toIntOrNull() ?: 300
                pixelController.setTimer(secs)

                val reply = "Timer initiated for ${secs / 60} minutes, Sir."
                _israelResponse.value = reply
                speakText(reply)
            }

            ActionType.SEND_EMAIL -> {
                val recipient = params["recipient"] ?: ""
                val subject = params["subject"] ?: "Note from Israel AI"
                val body = params["body"] ?: rawInput

                pixelController.sendEmail(recipient, subject, body)

                reminderDao.insertReminder(
                    ReminderEntity(
                        title = "Email to $recipient",
                        description = body,
                        timeInMillis = System.currentTimeMillis(),
                        category = "EMAIL"
                    )
                )

                val reply = "Opening email app to send your message to $recipient, Sir."
                _israelResponse.value = reply
                speakText(reply)
            }

            ActionType.OPEN_WHATSAPP -> {
                val phone = params["phone"] ?: ""
                val msg = params["message"] ?: ""

                pixelController.openWhatsApp(phone, msg)

                val reply = "Opening WhatsApp, Sir."
                _israelResponse.value = reply
                speakText(reply)
            }

            ActionType.WEB_SEARCH -> {
                val query = params["query"] ?: rawInput
                pixelController.performWebSearch(query)

                val reply = "Searching Google for '$query', Sir."
                _israelResponse.value = reply
                speakText(reply)
            }

            ActionType.DEVICE_REPORT -> {
                val report = pixelController.getDeviceDiagnosticsReport(pendingCount)
                _israelResponse.value = report
                speakText("Pixel diagnostic report complete, Sir. All systems functioning normally.")
            }

            ActionType.SCHEDULE_APPOINTMENT -> {
                val title = params["title"] ?: rawInput
                val startMillis = System.currentTimeMillis() + 3600000L // default 1 hr from now

                pixelController.scheduleCalendarEvent(title, "Scheduled via Israel AI", startMillis)

                reminderDao.insertReminder(
                    ReminderEntity(
                        title = title,
                        description = "Calendar Appointment",
                        timeInMillis = startMillis,
                        category = "APPOINTMENT"
                    )
                )

                val reply = "Appointment '$title' scheduled on your calendar and agenda, Sir."
                _israelResponse.value = reply
                speakText(reply)
            }

            ActionType.GENERAL_CHAT -> {
                val reply = parsedIntent.spokenResponse
                _israelResponse.value = reply
                speakText(reply)
            }
        }
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

    fun updatePitch(newPitch: Float) {
        _pitch.value = newPitch
        speechManager?.speechPitch = newPitch
    }

    fun updateRate(newRate: Float) {
        _rate.value = newRate
        speechManager?.speechRate = newRate
    }

    fun toggleWakeWord(enabled: Boolean) {
        _wakeWordEnabled.value = enabled
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}
