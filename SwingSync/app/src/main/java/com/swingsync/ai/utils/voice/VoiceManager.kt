package com.swingsync.ai.utils.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceManager @Inject constructor(
    private val context: Context
) : TextToSpeech.OnInitListener {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    
    private val _speechResults = MutableSharedFlow<SpeechResult>()
    val speechResults: SharedFlow<SpeechResult> = _speechResults.asSharedFlow()
    
    private val _voiceState = MutableSharedFlow<VoiceState>()
    val voiceState: SharedFlow<VoiceState> = _voiceState.asSharedFlow()
    
    fun initialize(): Result<Unit> {
        return try {
            // Initialize Text-to-Speech
            textToSpeech = TextToSpeech(context, this)
            
            // Initialize Speech Recognition
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(speechRecognitionListener)
                _voiceState.tryEmit(VoiceState.INITIALIZED)
                Result.success(Unit)
            } else {
                _voiceState.tryEmit(VoiceState.ERROR)
                Result.failure(Exception("Speech recognition not available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing voice manager")
            _voiceState.tryEmit(VoiceState.ERROR)
            Result.failure(e)
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.w("Language not supported for TTS")
            } else {
                isTtsInitialized = true
                Timber.d("TTS initialized successfully")
            }
        } else {
            Timber.e("TTS initialization failed")
        }
    }
    
    fun startListening(): Result<Unit> {
        return try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            speechRecognizer?.startListening(intent)
            _voiceState.tryEmit(VoiceState.LISTENING)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error starting speech recognition")
            _voiceState.tryEmit(VoiceState.ERROR)
            Result.failure(e)
        }
    }
    
    fun stopListening(): Result<Unit> {
        return try {
            speechRecognizer?.stopListening()
            _voiceState.tryEmit(VoiceState.IDLE)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping speech recognition")
            Result.failure(e)
        }
    }
    
    fun speak(text: String, priority: SpeechPriority = SpeechPriority.NORMAL): Result<Unit> {
        return try {
            if (!isTtsInitialized) {
                return Result.failure(Exception("TTS not initialized"))
            }
            
            val queueMode = when (priority) {
                SpeechPriority.IMMEDIATE -> TextToSpeech.QUEUE_FLUSH
                SpeechPriority.NORMAL -> TextToSpeech.QUEUE_ADD
            }
            
            textToSpeech?.speak(text, queueMode, null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error speaking text")
            Result.failure(e)
        }
    }
    
    fun speakCoachingFeedback(feedback: String) {
        speak("Coach says: $feedback", SpeechPriority.IMMEDIATE)
    }
    
    fun speakSwingPhase(phase: String) {
        speak(phase, SpeechPriority.NORMAL)
    }
    
    fun speakScore(score: Float) {
        val scoreText = "Your swing score is ${score.toInt()} out of 100"
        speak(scoreText, SpeechPriority.NORMAL)
    }
    
    fun speakInstruction(instruction: String) {
        speak(instruction, SpeechPriority.IMMEDIATE)
    }
    
    private val speechRecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.tryEmit(VoiceState.LISTENING)
        }
        
        override fun onBeginningOfSpeech() {
            _voiceState.tryEmit(VoiceState.SPEAKING)
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }
        
        override fun onEndOfSpeech() {
            _voiceState.tryEmit(VoiceState.PROCESSING)
        }
        
        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
            }
            
            Timber.e("Speech recognition error: $errorMessage")
            _speechResults.tryEmit(SpeechResult.Error(errorMessage))
            _voiceState.tryEmit(VoiceState.ERROR)
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                _speechResults.tryEmit(SpeechResult.Success(recognizedText))
                processVoiceCommand(recognizedText)
            }
            _voiceState.tryEmit(VoiceState.IDLE)
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val partialText = matches[0]
                _speechResults.tryEmit(SpeechResult.Partial(partialText))
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            // Additional events
        }
    }
    
    private fun processVoiceCommand(command: String) {
        val lowerCommand = command.lowercase()
        
        when {
            lowerCommand.contains("start recording") || lowerCommand.contains("record") -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.START_RECORDING))
            }
            lowerCommand.contains("stop recording") || lowerCommand.contains("stop") -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.STOP_RECORDING))
            }
            lowerCommand.contains("analyze") || lowerCommand.contains("analysis") -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.ANALYZE_SWING))
            }
            lowerCommand.contains("help") || lowerCommand.contains("instructions") -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.SHOW_HELP))
            }
            lowerCommand.contains("repeat") || lowerCommand.contains("again") -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.REPEAT_FEEDBACK))
            }
            lowerCommand.contains("settings") || lowerCommand.contains("preferences") -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.OPEN_SETTINGS))
            }
            else -> {
                _speechResults.tryEmit(SpeechResult.Command(VoiceCommand.UNKNOWN))
            }
        }
    }
    
    fun setLanguage(locale: Locale): Result<Unit> {
        return try {
            val result = textToSpeech?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Result.failure(Exception("Language not supported"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting language")
            Result.failure(e)
        }
    }
    
    fun setSpeechRate(rate: Float): Result<Unit> {
        return try {
            textToSpeech?.setSpeechRate(rate)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error setting speech rate")
            Result.failure(e)
        }
    }
    
    fun release() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _voiceState.tryEmit(VoiceState.IDLE)
    }
}

sealed class SpeechResult {
    data class Success(val text: String) : SpeechResult()
    data class Partial(val text: String) : SpeechResult()
    data class Error(val message: String) : SpeechResult()
    data class Command(val command: VoiceCommand) : SpeechResult()
}

enum class VoiceCommand {
    START_RECORDING,
    STOP_RECORDING,
    ANALYZE_SWING,
    SHOW_HELP,
    REPEAT_FEEDBACK,
    OPEN_SETTINGS,
    UNKNOWN
}

enum class VoiceState {
    IDLE,
    INITIALIZED,
    LISTENING,
    SPEAKING,
    PROCESSING,
    ERROR
}

enum class SpeechPriority {
    IMMEDIATE,
    NORMAL
}