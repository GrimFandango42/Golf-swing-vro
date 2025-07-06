package com.swingsync.ai.ui.screens.coaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.repository.CoachingRepository
import com.swingsync.ai.voice.VoiceInterface
import com.swingsync.ai.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CoachingViewModel @Inject constructor(
    private val coachingRepository: CoachingRepository,
    private val voiceInterface: VoiceInterface
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CoachingUiState())
    val uiState: StateFlow<CoachingUiState> = _uiState.asStateFlow()
    
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _currentPersonality = MutableStateFlow<CoachingPersonality?>(null)
    val currentPersonality: StateFlow<CoachingPersonality?> = _currentPersonality.asStateFlow()
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    init {
        // Initialize with welcome message
        initializeChat()
        
        // Observe voice interface state
        viewModelScope.launch {
            voiceInterface.voiceState.collect { state ->
                _voiceState.value = state
            }
        }
        
        // Observe speaking state
        viewModelScope.launch {
            voiceInterface.isSpeaking.collect { speaking ->
                _isSpeaking.value = speaking
            }
        }
        
        // Observe recognized text
        viewModelScope.launch {
            voiceInterface.recognizedText.collect { text ->
                if (text.isNotEmpty()) {
                    processUserMessage(text)
                }
            }
        }
        
        // Load coaching personality
        loadCoachingPersonality()
    }
    
    private fun initializeChat() {
        val welcomeMessage = ChatMessage(
            content = "Hello! I'm your AI golf coach. I'm here to help you improve your swing and answer any questions you have about golf technique. How can I assist you today?",
            isFromUser = false,
            timestamp = timeFormat.format(Date())
        )
        
        _messages.value = listOf(welcomeMessage)
        
        // Speak welcome message
        viewModelScope.launch {
            if (!_uiState.value.isMuted) {
                voiceInterface.speak(welcomeMessage.content)
            }
        }
    }
    
    private fun loadCoachingPersonality() {
        viewModelScope.launch {
            coachingRepository.getCurrentPersonality().collect { personality ->
                _currentPersonality.value = personality
                
                // Update voice settings based on personality
                personality?.let {
                    voiceInterface.setVoiceSettings(
                        com.swingsync.ai.voice.VoiceSettings(
                            speed = it.voiceSettings.speed,
                            pitch = it.voiceSettings.pitch,
                            volume = it.voiceSettings.volume
                        )
                    )
                }
            }
        }
    }
    
    fun startListening() {
        voiceInterface.startListening()
    }
    
    fun stopListening() {
        voiceInterface.stopListening()
    }
    
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        
        _uiState.value = _uiState.value.copy(currentInput = "")
        processUserMessage(text)
    }
    
    fun sendQuickMessage(message: String) {
        processUserMessage(message)
    }
    
    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }
    
    fun toggleMute() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
        
        if (_uiState.value.isMuted) {
            voiceInterface.stopSpeaking()
        }
    }
    
    private fun processUserMessage(text: String) {
        // Add user message to chat
        val userMessage = ChatMessage(
            content = text,
            isFromUser = true,
            timestamp = timeFormat.format(Date())
        )
        
        _messages.value = _messages.value + userMessage
        
        // Process the message and generate response
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCoachTyping = true)
            
            try {
                // Generate AI response
                val response = generateCoachResponse(text)
                
                // Add coach response to chat
                val coachMessage = ChatMessage(
                    content = response,
                    isFromUser = false,
                    timestamp = timeFormat.format(Date())
                )
                
                _messages.value = _messages.value + coachMessage
                
                // Speak response if not muted
                if (!_uiState.value.isMuted) {
                    voiceInterface.speak(response)
                }
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "I apologize, but I'm having trouble responding right now. Please try again.",
                    isFromUser = false,
                    timestamp = timeFormat.format(Date())
                )
                
                _messages.value = _messages.value + errorMessage
                
            } finally {
                _uiState.value = _uiState.value.copy(isCoachTyping = false)
            }
        }
    }
    
    private suspend fun generateCoachResponse(userInput: String): String {
        // Simulate processing delay
        kotlinx.coroutines.delay(1000)
        
        // Get current personality
        val personality = _currentPersonality.value
        
        // Process with coaching repository
        val baseResponse = coachingRepository.processConversation(userInput)
        
        // Adapt response based on personality
        return personality?.let { 
            adaptResponseToPersonality(baseResponse, it) 
        } ?: baseResponse
    }
    
    private fun adaptResponseToPersonality(response: String, personality: CoachingPersonality): String {
        return when (personality.style.lowercase()) {
            "encouraging" -> {
                "Great question! $response You're doing fantastic by asking about this!"
            }
            "technical" -> {
                "From a technical standpoint, $response This relates to the biomechanics of your swing."
            }
            "motivational" -> {
                "Excellent! $response This is exactly the kind of thinking that will take your game to the next level!"
            }
            "patient" -> {
                "That's a wonderful question. $response Take your time to practice this concept."
            }
            "competitive" -> {
                "Good focus! $response This is what separates good players from great ones."
            }
            else -> response
        }
    }
}

data class CoachingUiState(
    val currentInput: String = "",
    val isCoachTyping: Boolean = false,
    val isMuted: Boolean = false
)