package com.swingsync.ai.domain.usecase.swing

import com.swingsync.ai.domain.model.SwingSession
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for creating a new swing session.
 */
class CreateSwingSessionUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<CreateSwingSessionUseCase.Params, SwingSession>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<SwingSession> {
        // Validate input parameters
        if (parameters.userId.isBlank()) {
            return Result.Error(IllegalArgumentException("User ID cannot be blank"))
        }
        
        if (parameters.clubUsed.isBlank()) {
            return Result.Error(IllegalArgumentException("Club type cannot be blank"))
        }
        
        if (parameters.fps <= 0) {
            return Result.Error(IllegalArgumentException("FPS must be positive"))
        }
        
        // Create new swing session
        val session = SwingSession(
            sessionId = UUID.randomUUID().toString(),
            userId = parameters.userId,
            clubUsed = parameters.clubUsed,
            startTime = System.currentTimeMillis(),
            endTime = null,
            totalFrames = 0,
            fps = parameters.fps,
            videoPath = null,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        return when (val result = swingRepository.createSwingSession(session)) {
            is Result.Success -> Result.Success(session)
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    data class Params(
        val userId: String,
        val clubUsed: String,
        val fps: Float
    )
}