package com.swingsync.ai.domain.usecase.swing

import com.swingsync.ai.domain.model.SwingSession
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for updating a swing session.
 */
class UpdateSwingSessionUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<UpdateSwingSessionUseCase.Params, SwingSession>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<SwingSession> {
        // Validate session exists
        val existingSessionResult = swingRepository.getSwingSession(parameters.sessionId)
        val existingSession = when (existingSessionResult) {
            is Result.Success -> {
                existingSessionResult.data
                    ?: return Result.Error(IllegalArgumentException("Session not found"))
            }
            is Result.Error -> return existingSessionResult
            is Result.Loading -> return Result.Loading()
        }
        
        // Build updated session
        val updatedSession = existingSession.copy(
            endTime = parameters.endTime ?: existingSession.endTime,
            totalFrames = parameters.totalFrames ?: existingSession.totalFrames,
            videoPath = parameters.videoPath ?: existingSession.videoPath,
            isCompleted = parameters.isCompleted ?: existingSession.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        
        // Validate updates
        if (parameters.isCompleted == true && updatedSession.endTime == null) {
            return Result.Error(IllegalArgumentException("End time must be set when completing session"))
        }
        
        if (parameters.totalFrames != null && parameters.totalFrames < 0) {
            return Result.Error(IllegalArgumentException("Total frames cannot be negative"))
        }
        
        if (updatedSession.endTime != null && updatedSession.endTime < updatedSession.startTime) {
            return Result.Error(IllegalArgumentException("End time cannot be before start time"))
        }
        
        // Update session
        return when (val result = swingRepository.updateSwingSession(updatedSession)) {
            is Result.Success -> Result.Success(updatedSession)
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    data class Params(
        val sessionId: String,
        val endTime: Long? = null,
        val totalFrames: Int? = null,
        val videoPath: String? = null,
        val isCompleted: Boolean? = null
    )
}