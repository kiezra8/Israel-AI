package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class IsraelSpeechState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

class IsraelSpeechManager(
    private val context: Context,
    private val onVoiceResult: (String) -> Unit,
    private val onWakeWordDetected: () -> Unit
) : TextToSpeech.OnInitListener {

    private val _speechState = MutableStateFlow(IsraelSpeechState.IDLE)
    val speechState: StateFlow<IsraelSpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    var speechPitch: Float = 0.9f // Slightly lower robotic pitch
    var speechRate: Float = 1.05f // Slightly faster crisp delivery

    init {
        initSpeechRecognizer()
        initTts()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechState.value = IsraelSpeechState.LISTENING
                        _partialTranscript.value = "Listening for commands..."
                    }

                    override fun onBeginningOfSpeech() {
                        _speechState.value = IsraelSpeechState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDb.value = rmsdB.coerceAtLeast(0f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _speechState.value = IsraelSpeechState.THINKING
                    }

                    override fun onError(error: Int) {
                        Log.e("IsraelSpeechManager", "Speech recognition error code: $error")
                        _speechState.value = IsraelSpeechState.IDLE
                        _rmsDb.value = 0f
                        _partialTranscript.value = ""
                    }

                    override fun onResults(results: Bundle?) {
                        _speechState.value = IsraelSpeechState.THINKING
                        _rmsDb.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        _partialTranscript.value = spokenText

                        if (spokenText.isNotBlank()) {
                            // Check for wake word trigger
                            if (spokenText.lowercase().contains("hey israel") || spokenText.lowercase().contains("israel")) {
                                onWakeWordDetected()
                            }
                            onVoiceResult(spokenText)
                        } else {
                            _speechState.value = IsraelSpeechState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _partialTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            Log.w("IsraelSpeechManager", "SpeechRecognizer not available on this device.")
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                textToSpeech?.setPitch(speechPitch)
                textToSpeech?.setSpeechRate(speechRate)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _speechState.value = IsraelSpeechState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _speechState.value = IsraelSpeechState.IDLE
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _speechState.value = IsraelSpeechState.IDLE
                    }
                })
            }
        }
    }

    /**
     * Starts voice recognition listening
     */
    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Hey Israel' or tell Israel what to do...")
            }
            try {
                stopSpeaking()
                speechRecognizer?.startListening(intent)
                _speechState.value = IsraelSpeechState.LISTENING
            } catch (e: Exception) {
                Log.e("IsraelSpeechManager", "Start listening failed", e)
                _speechState.value = IsraelSpeechState.IDLE
            }
        }
    }

    /**
     * Stops listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _speechState.value = IsraelSpeechState.IDLE
        _rmsDb.value = 0f
    }

    /**
     * Speaks the JARVIS response aloud
     */
    fun speak(text: String) {
        if (isTtsReady && text.isNotBlank()) {
            stopListening()
            _speechState.value = IsraelSpeechState.SPEAKING
            textToSpeech?.setPitch(speechPitch)
            textToSpeech?.setSpeechRate(speechRate)
            val utteranceId = "ISRAEL_SPEECH_${System.currentTimeMillis()}"
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    /**
     * Stops TTS
     */
    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        if (_speechState.value == IsraelSpeechState.SPEAKING) {
            _speechState.value = IsraelSpeechState.IDLE
        }
    }

    /**
     * Sets current speech state externally (e.g. while Gemini is thinking)
     */
    fun setState(state: IsraelSpeechState) {
        _speechState.value = state
    }

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
