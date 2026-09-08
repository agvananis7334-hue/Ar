package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.LanguageMode
import java.util.Locale

class SiriVoiceManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onStatusChange: (isSpeaking: Boolean, isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isListeningNow = false
    private var toneGenerator: ToneGenerator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var currentLanguageMode: LanguageMode = LanguageMode.AUTO

    init {
        initTts()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                applyTtsLanguage()
                textToSpeech?.setPitch(1.08f)
                textToSpeech?.setSpeechRate(1.02f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post {
                            onStatusChange(true, isListeningNow)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            onStatusChange(false, isListeningNow)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            onStatusChange(false, isListeningNow)
                        }
                    }
                })
            }
        }
    }

    private fun applyTtsLanguage() {
        val tts = textToSpeech ?: return
        when (currentLanguageMode) {
            LanguageMode.GUJARATI -> {
                val guLocale = Locale("gu", "IN")
                val res = tts.setLanguage(guLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.ENGLISH
                }
            }
            LanguageMode.ENGLISH -> {
                tts.language = Locale.US
            }
            LanguageMode.AUTO -> {
                tts.language = Locale.getDefault()
            }
        }
    }

    fun playSiriWakeChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 140)
            mainHandler.postDelayed({
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            }, 120)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        stopSpeaking()

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@SiriVoiceManager)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    when (currentLanguageMode) {
                        LanguageMode.GUJARATI -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN")
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("gu-IN", "en-US"))
                        }
                        LanguageMode.ENGLISH -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        }
                        LanguageMode.AUTO -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("gu-IN", "en-US"))
                        }
                    }
                }

                speechRecognizer?.startListening(intent)
                isListeningNow = true
                onStatusChange(false, true)
            } catch (e: Exception) {
                isListeningNow = false
                onStatusChange(false, false)
                onError("Failed to start voice listener: ${e.message}")
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListeningNow = false
            onStatusChange(false, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speak(text: String) {
        if (!isTtsReady) return
        stopListening()
        val tts = textToSpeech ?: return

        // Auto-adapt language based on text content
        val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
        if (hasGujarati) {
            val guResult = tts.setLanguage(Locale("gu", "IN"))
            if (guResult == TextToSpeech.LANG_MISSING_DATA || guResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.ENGLISH
            }
        } else {
            tts.language = Locale.US
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "siri_msg_${System.currentTimeMillis()}")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "siri_msg_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
                onStatusChange(false, isListeningNow)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.shutdown()
            textToSpeech = null
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        isListeningNow = true
        onStatusChange(false, true)
    }

    override fun onBeginningOfSpeech() {
        isListeningNow = true
        onStatusChange(false, true)
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize rmsdB (-2 to 10 dB typically) to 0.0f - 1.0f range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
        onRmsChanged(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        isListeningNow = false
        onStatusChange(false, false)
    }

    override fun onError(error: Int) {
        isListeningNow = false
        onStatusChange(false, false)
        val msg = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap to try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_NETWORK -> "Network issue (offline mode active)."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            else -> "Listening paused."
        }
        onError(msg)
    }

    override fun onResults(results: Bundle?) {
        isListeningNow = false
        onStatusChange(false, false)
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognized = matches?.firstOrNull() ?: return
        onSpeechResult(recognized)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: return
        // Could be used for live preview
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
