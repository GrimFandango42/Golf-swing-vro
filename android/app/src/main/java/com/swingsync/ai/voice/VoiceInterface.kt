package com.swingsync.ai.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class VoiceInterface @Inject constructor(
    private val context: Context
) {
    
    // Hands-free system integration
    private var handsFreeService: HandsFreeService? = null
    private var spatialAudioGuide: SpatialAudioGuide? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    
    init {
        initializeTTS()
        initializeSpeechRecognizer()
    }
    
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    tts.language = Locale.US
                    tts.setSpeechRate(0.9f)
                    tts.setPitch(1.0f)
                    
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                        
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            _error.value = "TTS Error: Failed to speak"
                        }
                    })
                }
                isInitialized = true
            } else {
                _error.value = "TTS initialization failed"
            }
        }
    }
    
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Speech recognition not available"
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
            }
            
            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.SPEAKING
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Update voice activity level if needed
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Handle audio buffer if needed
            }
            
            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.PROCESSING
            }
            
            override fun onError(error: Int) {
                _voiceState.value = VoiceState.ERROR
                _error.value = getErrorMessage(error)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                    _voiceState.value = VoiceState.IDLE
                } else {
                    _error.value = "No speech recognized"
                    _voiceState.value = VoiceState.ERROR
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle recognition events
            }
        })
    }
    
    suspend fun speak(text: String, voiceSettings: VoiceSettings = VoiceSettings()): Boolean {
        if (!isInitialized) {
            _error.value = "TTS not initialized"
            return false
        }
        
        return suspendCancellableCoroutine { continuation ->
            textToSpeech?.let { tts ->
                // Apply voice settings
                tts.setSpeechRate(voiceSettings.speed)
                tts.setPitch(voiceSettings.pitch)
                
                val utteranceId = System.currentTimeMillis().toString()
                val bundle = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceSettings.volume)
                }
                
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        continuation.resume(true)
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _error.value = "Speech synthesis failed"
                        continuation.resume(false)
                    }
                })
                
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    _error.value = "Failed to start speech synthesis"
                    continuation.resume(false)
                }
            } ?: run {
                _error.value = "TTS not available"
                continuation.resume(false)
            }
        }
    }
    
    fun startListening() {
        if (_voiceState.value == VoiceState.LISTENING) {
            return
        }
        
        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            recognizer.startListening(intent)
        } ?: run {
            _error.value = "Speech recognizer not available"
        }
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.IDLE
    }
    
    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }
    
    fun setVoiceSettings(settings: VoiceSettings) {
        textToSpeech?.let { tts ->
            tts.setSpeechRate(settings.speed)
            tts.setPitch(settings.pitch)
        }
    }
    
    fun processVoiceCommand(text: String): VoiceCommand {
        return VoiceCommandProcessor.processCommand(text)
    }
    
    /**
     * Integration with hands-free system
     */
    fun setHandsFreeService(service: HandsFreeService) {
        handsFreeService = service
    }
    
    fun setSpatialAudioGuide(guide: SpatialAudioGuide) {
        spatialAudioGuide = guide
    }
    
    /**
     * Enhanced speak with spatial audio support
     */
    suspend fun speakWithSpatialAudio(text: String, position: Position3D? = null, voiceSettings: VoiceSettings = VoiceSettings()): Boolean {
        // First speak normally
        val success = speak(text, voiceSettings)
        
        // If spatial audio is available and position is provided, enhance with spatial cues
        if (success && position != null) {
            spatialAudioGuide?.playPositioningGuidance("", position.magnitude())
        }
        
        return success
    }
    
    /**
     * Check if hands-free mode is compatible
     */
    fun isHandsFreeCompatible(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context) && isInitialized
    }
    
    /**
     * Enhanced listening with wake word integration
     */
    fun startHandsFreeListening() {
        if (isHandsFreeCompatible()) {
            startListening()
        }
    }
    
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech input"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
    
    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        speechRecognizer = null
        textToSpeech = null
    }
}

enum class VoiceState {
    IDLE, LISTENING, SPEAKING, PROCESSING, ERROR
}

data class VoiceSettings(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val language: String = "en-US"
)

data class VoiceCommand(
    val type: VoiceCommandType,
    val confidence: Float,
    val originalText: String,
    val parameters: Map<String, Any> = emptyMap()
)

enum class VoiceCommandType {
    START_PRACTICE,
    END_PRACTICE,
    ANALYZE_SWING,
    GET_TIPS,
    REPEAT,
    SLOW_DOWN,
    BE_QUIET,
    CHANGE_VOICE,
    HELP,
    CONVERSATION
}

object VoiceCommandProcessor {
    private val commands = mapOf(
        VoiceCommandType.START_PRACTICE to listOf(
            "start practice", "begin session", "let's practice", "start coaching"
        ),
        VoiceCommandType.END_PRACTICE to listOf(
            "end practice", "stop session", "finish up", "that's enough"
        ),
        VoiceCommandType.ANALYZE_SWING to listOf(
            "analyze my swing", "check my form", "how did I do", "review my swing"
        ),
        VoiceCommandType.GET_TIPS to listOf(
            "give me tips", "what should I work on", "help me improve", "any advice"
        ),
        VoiceCommandType.REPEAT to listOf(
            "repeat that", "say again", "what did you say", "come again"
        ),
        VoiceCommandType.SLOW_DOWN to listOf(
            "slow down", "speak slower", "too fast", "slower please"
        ),
        VoiceCommandType.BE_QUIET to listOf(
            "be quiet", "stop talking", "less feedback", "quiet mode"
        ),
        VoiceCommandType.CHANGE_VOICE to listOf(
            "change voice", "different voice", "voice settings", "new voice"
        ),
        VoiceCommandType.HELP to listOf(
            "help", "what can you do", "commands", "instructions"
        )
    )
    
    fun processCommand(text: String): VoiceCommand {
        val textLower = text.lowercase().trim()
        
        var bestMatch = VoiceCommand(
            type = VoiceCommandType.CONVERSATION,
            confidence = 0.0f,
            originalText = text
        )
        
        for ((commandType, phrases) in commands) {
            for (phrase in phrases) {
                if (textLower.contains(phrase)) {
                    val confidence = phrase.length.toFloat() / textLower.length
                    if (confidence > bestMatch.confidence && confidence >= 0.7f) {
                        bestMatch = VoiceCommand(
                            type = commandType,
                            confidence = confidence,
                            originalText = text
                        )
                    }
                }
            }
        }
        
        return bestMatch
    }
}