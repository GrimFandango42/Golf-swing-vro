package com.swingsync.ai.data.repository

import com.swingsync.ai.ui.screens.settings.AppSettings
import com.swingsync.ai.ui.screens.settings.CoachingPersonality
import com.swingsync.ai.ui.screens.settings.VoiceSettingsData
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor() {
    
    // Mock in-memory storage - would be replaced with DataStore preferences
    private var currentSettings = AppSettings(
        voiceFeedbackEnabled = true,
        autoListenEnabled = false,
        practiceRemindersEnabled = true,
        progressUpdatesEnabled = true,
        analysisSensitivity = 0.7f,
        realtimeFeedbackEnabled = true,
        voiceSettings = VoiceSettingsData()
    )
    
    private var currentPersonality: CoachingPersonality? = null
    
    suspend fun getSettings(): AppSettings {
        // Simulate loading delay
        delay(200)
        return currentSettings
    }
    
    suspend fun saveSettings(settings: AppSettings) {
        // Simulate save delay
        delay(200)
        currentSettings = settings
        // Would save to DataStore preferences
    }
    
    suspend fun getCurrentPersonality(): CoachingPersonality? {
        delay(200)
        return currentPersonality ?: getDefaultPersonality()
    }
    
    suspend fun setCurrentPersonality(personality: CoachingPersonality) {
        delay(200)
        currentPersonality = personality
        // Would save to DataStore preferences
    }
    
    private fun getDefaultPersonality(): CoachingPersonality {
        return CoachingPersonality(
            name = "encouraging_mentor",
            displayName = "The Encouraging Mentor",
            description = "Supportive and patient, celebrates small wins",
            style = "encouraging"
        )
    }
}