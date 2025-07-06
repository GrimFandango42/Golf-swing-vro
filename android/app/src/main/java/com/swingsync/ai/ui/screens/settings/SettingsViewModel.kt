package com.swingsync.ai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _availablePersonalities = MutableStateFlow(getAvailablePersonalities())
    val availablePersonalities: StateFlow<List<CoachingPersonality>> = _availablePersonalities.asStateFlow()
    
    private val _currentPersonality = MutableStateFlow<CoachingPersonality?>(null)
    val currentPersonality: StateFlow<CoachingPersonality?> = _currentPersonality.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load current settings
                val settings = settingsRepository.getSettings()
                _uiState.value = _uiState.value.copy(
                    voiceFeedbackEnabled = settings.voiceFeedbackEnabled,
                    autoListenEnabled = settings.autoListenEnabled,
                    practiceRemindersEnabled = settings.practiceRemindersEnabled,
                    progressUpdatesEnabled = settings.progressUpdatesEnabled,
                    analysisSensitivity = settings.analysisSensitivity,
                    realtimeFeedbackEnabled = settings.realtimeFeedbackEnabled,
                    voiceSettings = settings.voiceSettings
                )
                
                // Load current personality
                val personality = settingsRepository.getCurrentPersonality()
                _currentPersonality.value = personality
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(voiceFeedbackEnabled = enabled)
        saveSettings()
    }
    
    fun setAutoListenEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoListenEnabled = enabled)
        saveSettings()
    }
    
    fun setPracticeRemindersEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(practiceRemindersEnabled = enabled)
        saveSettings()
    }
    
    fun setProgressUpdatesEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(progressUpdatesEnabled = enabled)
        saveSettings()
    }
    
    fun setAnalysisSensitivity(sensitivity: Float) {
        _uiState.value = _uiState.value.copy(analysisSensitivity = sensitivity)
        saveSettings()
    }
    
    fun setRealtimeFeedbackEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(realtimeFeedbackEnabled = enabled)
        saveSettings()
    }
    
    fun updateVoiceSettings(settings: VoiceSettingsData) {
        _uiState.value = _uiState.value.copy(voiceSettings = settings)
        saveSettings()
    }
    
    fun setCoachingPersonality(personality: CoachingPersonality) {
        _currentPersonality.value = personality
        viewModelScope.launch {
            settingsRepository.setCurrentPersonality(personality)
        }
    }
    
    private fun saveSettings() {
        viewModelScope.launch {
            try {
                val settings = AppSettings(
                    voiceFeedbackEnabled = _uiState.value.voiceFeedbackEnabled,
                    autoListenEnabled = _uiState.value.autoListenEnabled,
                    practiceRemindersEnabled = _uiState.value.practiceRemindersEnabled,
                    progressUpdatesEnabled = _uiState.value.progressUpdatesEnabled,
                    analysisSensitivity = _uiState.value.analysisSensitivity,
                    realtimeFeedbackEnabled = _uiState.value.realtimeFeedbackEnabled,
                    voiceSettings = _uiState.value.voiceSettings
                )
                
                settingsRepository.saveSettings(settings)
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private fun getAvailablePersonalities(): List<CoachingPersonality> {
        return listOf(
            CoachingPersonality(
                name = "encouraging_mentor",
                displayName = "The Encouraging Mentor",
                description = "Supportive and patient, celebrates small wins",
                style = "encouraging"
            ),
            CoachingPersonality(
                name = "technical_expert",
                displayName = "The Technical Expert",
                description = "Detail-oriented, focuses on biomechanics",
                style = "technical"
            ),
            CoachingPersonality(
                name = "motivational_coach",
                displayName = "The Motivational Coach",
                description = "High energy, pushes for excellence",
                style = "motivational"
            ),
            CoachingPersonality(
                name = "patient_teacher",
                displayName = "The Patient Teacher",
                description = "Calm and methodical, takes time to explain",
                style = "patient"
            ),
            CoachingPersonality(
                name = "competitive_trainer",
                displayName = "The Competitive Trainer",
                description = "Results-focused, sets challenging goals",
                style = "competitive"
            ),
            CoachingPersonality(
                name = "holistic_guide",
                displayName = "The Holistic Guide",
                description = "Considers mental and physical aspects",
                style = "holistic"
            )
        )
    }
}

data class SettingsUiState(
    val voiceFeedbackEnabled: Boolean = true,
    val autoListenEnabled: Boolean = false,
    val practiceRemindersEnabled: Boolean = true,
    val progressUpdatesEnabled: Boolean = true,
    val analysisSensitivity: Float = 0.7f,
    val realtimeFeedbackEnabled: Boolean = true,
    val voiceSettings: VoiceSettingsData = VoiceSettingsData()
)

data class AppSettings(
    val voiceFeedbackEnabled: Boolean,
    val autoListenEnabled: Boolean,
    val practiceRemindersEnabled: Boolean,
    val progressUpdatesEnabled: Boolean,
    val analysisSensitivity: Float,
    val realtimeFeedbackEnabled: Boolean,
    val voiceSettings: VoiceSettingsData
)