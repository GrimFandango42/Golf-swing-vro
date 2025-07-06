package com.swingsync.ai.voice

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.data.model.CoachingPersonality
import com.swingsync.ai.data.repository.CoachingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VoiceRecognitionService : Service() {
    
    @Inject
    lateinit var voiceInterface: VoiceInterface
    
    @Inject
    lateinit var coachingRepository: CoachingRepository
    
    private val binder = VoiceServiceBinder()
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val _currentPersonality = MutableStateFlow<CoachingPersonality?>(null)
    val currentPersonality: StateFlow<CoachingPersonality?> = _currentPersonality.asStateFlow()
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        // Initialize voice interface
        initializeVoiceInterface()
    }
    
    private fun initializeVoiceInterface() {
        // Load current coaching personality
        lifecycleScope.launch {
            coachingRepository.getCurrentPersonality().collect { personality ->
                _currentPersonality.value = personality
                personality?.let {
                    voiceInterface.setVoiceSettings(
                        VoiceSettings(
                            speed = it.voiceSettings.speed,
                            pitch = it.voiceSettings.pitch,
                            volume = it.voiceSettings.volume
                        )
                    )
                }
            }
        }
    }
    
    suspend fun processVoiceInput(input: String): String {
        val command = voiceInterface.processVoiceCommand(input)
        
        return when (command.type) {
            VoiceCommandType.START_PRACTICE -> {
                startPracticeSession()
                "Great! Let's start your practice session. I'm here to help you improve your swing."
            }
            VoiceCommandType.END_PRACTICE -> {
                endPracticeSession()
                "Good work today! Your practice session is complete. Keep up the great progress!"
            }
            VoiceCommandType.ANALYZE_SWING -> {
                "I'm analyzing your swing now. Let me give you some feedback on your technique."
            }
            VoiceCommandType.GET_TIPS -> {
                "Here are some tips to improve your swing: Focus on your posture, keep your head steady, and follow through completely."
            }
            VoiceCommandType.REPEAT -> {
                "Let me repeat that for you..."
            }
            VoiceCommandType.SLOW_DOWN -> {
                voiceInterface.setVoiceSettings(
                    VoiceSettings(speed = 0.8f, pitch = 1.0f, volume = 1.0f)
                )
                "I'll speak more slowly now. Is this better?"
            }
            VoiceCommandType.BE_QUIET -> {
                "I'll reduce my feedback. Just let me know when you'd like me to speak up again."
            }
            VoiceCommandType.CHANGE_VOICE -> {
                "You can change my voice in the settings. Would you like me to guide you there?"
            }
            VoiceCommandType.HELP -> {
                getHelpMessage()
            }
            VoiceCommandType.CONVERSATION -> {
                // Process as conversational input
                processConversationalInput(input)
            }
        }
    }
    
    private fun startPracticeSession() {
        _isActive.value = true
        // Start practice session logic
    }
    
    private fun endPracticeSession() {
        _isActive.value = false
        // End practice session logic
    }
    
    private suspend fun processConversationalInput(input: String): String {
        // This would integrate with the conversational coaching backend
        val personality = _currentPersonality.value
        return personality?.let { 
            // Apply personality-specific response adaptation
            adaptResponseToPersonality(generateCoachingResponse(input), it)
        } ?: "I understand. Let me help you with your golf swing."
    }
    
    private fun generateCoachingResponse(input: String): String {
        // This would call the backend conversational coaching API
        // For now, returning a placeholder response
        return "That's a great question about your swing technique. Let me help you improve that aspect."
    }
    
    private fun adaptResponseToPersonality(response: String, personality: CoachingPersonality): String {
        // Apply personality-specific adaptations
        return when (personality.style) {
            "encouraging" -> "Great work! $response You're doing fantastic!"
            "technical" -> "From a technical perspective, $response The biomechanics show..."
            "motivational" -> "Let's go! $response You've got this!"
            "patient" -> "Take your time. $response We'll work through this together."
            "competitive" -> "Champions focus on this: $response Push yourself!"
            else -> response
        }
    }
    
    private fun getHelpMessage(): String {
        return """
            Here are some things you can say:
            • "Start practice" - Begin a practice session
            • "Analyze my swing" - Get swing analysis
            • "Give me tips" - Get improvement suggestions
            • "End practice" - Finish your session
            • "Slow down" - I'll speak more slowly
            • "Change voice" - Modify voice settings
            
            Just speak naturally and I'll understand what you need!
        """.trimIndent()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceInterface.destroy()
    }
    
    inner class VoiceServiceBinder : Binder() {
        fun getService(): VoiceRecognitionService = this@VoiceRecognitionService
    }
}