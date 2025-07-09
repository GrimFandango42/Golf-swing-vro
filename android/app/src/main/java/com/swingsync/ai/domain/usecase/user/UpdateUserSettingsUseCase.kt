package com.swingsync.ai.domain.usecase.user

import com.swingsync.ai.domain.model.UserSettings
import com.swingsync.ai.domain.repository.UserRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for updating user settings.
 */
class UpdateUserSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<UpdateUserSettingsUseCase.Params, UserSettings>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<UserSettings> {
        // Validate input parameters
        if (parameters.userId.isBlank()) {
            return Result.Error(IllegalArgumentException("User ID cannot be blank"))
        }
        
        // Get existing settings
        val existingSettingsResult = userRepository.getUserSettings(parameters.userId)
        val existingSettings = when (existingSettingsResult) {
            is Result.Success -> {
                existingSettingsResult.data
                    ?: return Result.Error(IllegalArgumentException("User settings not found"))
            }
            is Result.Error -> return existingSettingsResult
            is Result.Loading -> return Result.Loading()
        }
        
        // Validate new settings
        parameters.preferredClub?.let { club ->
            if (club.isBlank()) {
                return Result.Error(IllegalArgumentException("Preferred club cannot be blank"))
            }
        }
        
        parameters.difficultyLevel?.let { level ->
            if (level.isBlank()) {
                return Result.Error(IllegalArgumentException("Difficulty level cannot be blank"))
            }
            if (level !in listOf("Beginner", "Intermediate", "Advanced", "Professional")) {
                return Result.Error(IllegalArgumentException("Invalid difficulty level"))
            }
        }
        
        parameters.unitsSystem?.let { units ->
            if (units !in listOf("Imperial", "Metric")) {
                return Result.Error(IllegalArgumentException("Invalid units system"))
            }
        }
        
        // Build updated settings
        val updatedSettings = existingSettings.copy(
            preferredClub = parameters.preferredClub ?: existingSettings.preferredClub,
            difficultyLevel = parameters.difficultyLevel ?: existingSettings.difficultyLevel,
            unitsSystem = parameters.unitsSystem ?: existingSettings.unitsSystem,
            voiceCoachingEnabled = parameters.voiceCoachingEnabled ?: existingSettings.voiceCoachingEnabled,
            celebrationsEnabled = parameters.celebrationsEnabled ?: existingSettings.celebrationsEnabled,
            autoSaveEnabled = parameters.autoSaveEnabled ?: existingSettings.autoSaveEnabled,
            analysisNotificationsEnabled = parameters.analysisNotificationsEnabled ?: existingSettings.analysisNotificationsEnabled,
            updatedAt = System.currentTimeMillis()
        )
        
        // Update settings
        return when (val result = userRepository.updateUserSettings(updatedSettings)) {
            is Result.Success -> Result.Success(updatedSettings)
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    data class Params(
        val userId: String,
        val preferredClub: String? = null,
        val difficultyLevel: String? = null,
        val unitsSystem: String? = null,
        val voiceCoachingEnabled: Boolean? = null,
        val celebrationsEnabled: Boolean? = null,
        val autoSaveEnabled: Boolean? = null,
        val analysisNotificationsEnabled: Boolean? = null
    )
}