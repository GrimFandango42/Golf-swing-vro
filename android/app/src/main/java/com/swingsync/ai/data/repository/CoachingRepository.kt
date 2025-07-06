package com.swingsync.ai.data.repository

import com.swingsync.ai.ui.screens.coaching.CoachingPersonality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachingRepository @Inject constructor() {
    
    private val _currentPersonality = MutableStateFlow<CoachingPersonality?>(null)
    
    suspend fun getCurrentPersonality(): Flow<CoachingPersonality?> {
        // Load from preferences or return default
        if (_currentPersonality.value == null) {
            _currentPersonality.value = getDefaultPersonality()
        }
        return _currentPersonality.asStateFlow()
    }
    
    suspend fun setCurrentPersonality(personality: CoachingPersonality) {
        _currentPersonality.value = personality
        // Save to preferences
    }
    
    suspend fun processConversation(userInput: String): String {
        // This would integrate with the backend conversational coaching API
        // For now, return a mock response
        
        return when {
            userInput.contains("swing", ignoreCase = true) -> {
                "Great question about your swing! The key fundamentals are maintaining good posture, " +
                "keeping your head steady, and following through completely. Would you like me to " +
                "elaborate on any of these points?"
            }
            userInput.contains("tip", ignoreCase = true) -> {
                "Here's a helpful tip: Focus on your grip pressure. Many golfers grip too tightly, " +
                "which restricts the natural flow of the swing. Try to maintain a firm but relaxed grip, " +
                "about a 6 out of 10 in terms of pressure."
            }
            userInput.contains("improve", ignoreCase = true) -> {
                "To improve your golf game, I recommend focusing on these areas: " +
                "1) Consistent practice routine, 2) Proper fundamentals, 3) Mental game development. " +
                "Which area would you like to work on first?"
            }
            userInput.contains("thank", ignoreCase = true) -> {
                "You're very welcome! I'm here to help you improve your golf game. " +
                "Keep up the great work and don't hesitate to ask if you have any more questions!"
            }
            else -> {
                "That's an interesting point about golf technique. Let me help you with that. " +
                "Good golf fundamentals start with proper setup and posture. What specific aspect " +
                "of your game would you like to focus on?"
            }
        }
    }
    
    private fun getDefaultPersonality(): CoachingPersonality {
        return CoachingPersonality(
            name = "encouraging_mentor",
            displayName = "The Encouraging Mentor",
            style = "encouraging",
            voiceSettings = com.swingsync.ai.ui.screens.coaching.VoiceSettings(
                speed = 1.0f,
                pitch = 1.0f,
                volume = 1.0f
            )
        )
    }
}