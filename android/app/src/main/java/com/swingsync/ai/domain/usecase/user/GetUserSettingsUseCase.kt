package com.swingsync.ai.domain.usecase.user

import com.swingsync.ai.domain.model.UserSettings
import com.swingsync.ai.domain.repository.UserRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for getting user settings.
 */
class GetUserSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<GetUserSettingsUseCase.Params, UserSettings>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<UserSettings> {
        // Validate input parameters
        if (parameters.userId.isBlank()) {
            return Result.Error(IllegalArgumentException("User ID cannot be blank"))
        }
        
        // Get user settings
        return when (val result = userRepository.getUserSettings(parameters.userId)) {
            is Result.Success -> {
                if (result.data != null) {
                    Result.Success(result.data)
                } else {
                    // Create default settings if none exist
                    val defaultResult = userRepository.createDefaultSettings(parameters.userId)
                    when (defaultResult) {
                        is Result.Success -> {
                            // Get the newly created settings
                            userRepository.getUserSettings(parameters.userId).let { newResult ->
                                when (newResult) {
                                    is Result.Success -> Result.Success(
                                        newResult.data ?: getDefaultSettings(parameters.userId)
                                    )
                                    is Result.Error -> newResult
                                    is Result.Loading -> newResult
                                }
                            }
                        }
                        is Result.Error -> defaultResult
                        is Result.Loading -> defaultResult
                    }
                }
            }
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    private fun getDefaultSettings(userId: String): UserSettings {
        return UserSettings(
            userId = userId,
            preferredClub = "Driver",
            difficultyLevel = "Beginner",
            unitsSystem = "Imperial",
            voiceCoachingEnabled = true,
            celebrationsEnabled = true,
            autoSaveEnabled = true,
            analysisNotificationsEnabled = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    data class Params(
        val userId: String
    )
}