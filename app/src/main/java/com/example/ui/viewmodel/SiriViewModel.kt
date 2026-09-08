package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActionType
import com.example.data.model.ActiveTimer
import com.example.data.model.AssistantMessage
import com.example.data.model.AssistantStatus
import com.example.data.model.LanguageMode
import com.example.data.model.MessageSender
import com.example.engine.OfflineCommandEngine
import com.example.service.SiriWakeWordService
import com.example.voice.SiriVoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SiriViewModel(application: Application) : AndroidViewModel(application) {

    private val commandEngine = OfflineCommandEngine(application)

    private val _status = MutableStateFlow(AssistantStatus.IDLE)
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.1f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _messages = MutableStateFlow<List<AssistantMessage>>(
        listOf(
            AssistantMessage(
                sender = MessageSender.SIRI,
                text = "નમસ્તે! હું સિરી છું. ઑફલાઇન કમાન્ડ આપો અથવા 'હે સિરી' બોલો.",
                actionBadge = "✨ Ready"
            )
        )
    )
    val messages: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    private val _languageMode = MutableStateFlow(LanguageMode.AUTO)
    val languageMode: StateFlow<LanguageMode> = _languageMode.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(SiriWakeWordService.isRunning)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isScreenOffSimulated = MutableStateFlow(false)
    val isScreenOffSimulated: StateFlow<Boolean> = _isScreenOffSimulated.asStateFlow()

    private val _activeTimer = MutableStateFlow<ActiveTimer?>(null)
    val activeTimer: StateFlow<ActiveTimer?> = _activeTimer.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var timerJob: Job? = null

    val voiceManager: SiriVoiceManager = SiriVoiceManager(
        context = application,
        onSpeechResult = { recognizedText ->
            processUserQuery(recognizedText)
        },
        onRmsChanged = { level ->
            _audioLevel.value = level
        },
        onStatusChange = { isSpeaking, isListening ->
            if (isSpeaking) {
                _status.value = AssistantStatus.SPEAKING
            } else if (isListening) {
                _status.value = AssistantStatus.LISTENING
            } else if (_status.value != AssistantStatus.THINKING) {
                _status.value = AssistantStatus.IDLE
            }
        },
        onError = { err ->
            _errorMessage.value = err
            if (_status.value != AssistantStatus.SPEAKING) {
                _status.value = AssistantStatus.IDLE
            }
        }
    )

    fun onWakeWordDetected() {
        voiceManager.playSiriWakeChime()
        _isScreenOffSimulated.value = false
        _status.value = AssistantStatus.LISTENING

        val greeting = if (_languageMode.value == LanguageMode.GUJARATI) {
            "હા, હું સાંભળી રહ્યો છું."
        } else {
            "I'm listening."
        }

        addMessage(MessageSender.SIRI, greeting, actionBadge = "⚡ Hey Siri Activated")
        voiceManager.speak(greeting)

        // Begin listening for user command right after greeting
        viewModelScope.launch {
            delay(1400)
            voiceManager.startListening()
        }
    }

    fun startListening() {
        _errorMessage.value = null
        voiceManager.playSiriWakeChime()
        _status.value = AssistantStatus.LISTENING
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        _status.value = AssistantStatus.IDLE
    }

    fun toggleVoiceInteraction() {
        if (_status.value == AssistantStatus.LISTENING) {
            stopListening()
        } else if (_status.value == AssistantStatus.SPEAKING) {
            voiceManager.stopSpeaking()
            _status.value = AssistantStatus.IDLE
        } else {
            startListening()
        }
    }

    fun submitManualCommand(commandText: String) {
        processUserQuery(commandText)
    }

    private fun processUserQuery(text: String) {
        if (text.isBlank()) return

        addMessage(MessageSender.USER, text)
        _status.value = AssistantStatus.THINKING

        viewModelScope.launch {
            delay(250) // Quick smooth reaction
            val result = commandEngine.processCommand(text)

            _status.value = AssistantStatus.SPEAKING
            addMessage(
                sender = MessageSender.SIRI,
                text = result.displayText,
                actionBadge = result.actionBadge,
                isOfflineExecuted = true
            )

            // If timer action was executed, start in-app live timer countdown
            if (result.actionType == ActionType.TIMER && result.payload != null) {
                val seconds = result.payload.toIntOrNull() ?: 60
                startInAppCountdown(seconds)
            }

            voiceManager.speak(result.spokenText)
        }
    }

    private fun startInAppCountdown(seconds: Int) {
        timerJob?.cancel()
        val timer = ActiveTimer(initialSeconds = seconds, remainingSeconds = seconds)
        _activeTimer.value = timer

        timerJob = viewModelScope.launch {
            while (isActive && timer.remainingSeconds > 0) {
                delay(1000)
                timer.remainingSeconds -= 1
                _activeTimer.value = timer.copy(remainingSeconds = timer.remainingSeconds)
            }
            if (timer.remainingSeconds <= 0) {
                voiceManager.speak("Timer finished!")
                _activeTimer.value = null
            }
        }
    }

    fun cancelActiveTimer() {
        timerJob?.cancel()
        _activeTimer.value = null
    }

    private fun addMessage(
        sender: MessageSender,
        text: String,
        actionBadge: String? = null,
        isOfflineExecuted: Boolean = true
    ) {
        val newMsg = AssistantMessage(
            sender = sender,
            text = text,
            actionBadge = actionBadge,
            isOfflineExecuted = isOfflineExecuted
        )
        _messages.value = _messages.value + newMsg
    }

    fun setLanguageMode(mode: LanguageMode) {
        _languageMode.value = mode
        voiceManager.currentLanguageMode = mode
    }

    fun toggleForegroundService(enable: Boolean) {
        val app = getApplication<Application>()
        if (enable) {
            SiriWakeWordService.start(app)
            _isServiceRunning.value = true
        } else {
            SiriWakeWordService.stop(app)
            _isServiceRunning.value = false
        }
    }

    fun toggleScreenOffSimulation(active: Boolean) {
        _isScreenOffSimulated.value = active
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        voiceManager.release()
    }
}
