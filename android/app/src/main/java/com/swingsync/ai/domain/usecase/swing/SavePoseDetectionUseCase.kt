package com.swingsync.ai.domain.usecase.swing

import com.swingsync.ai.domain.model.PoseDetection
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for saving pose detection results.
 */
class SavePoseDetectionUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<SavePoseDetectionUseCase.Params, Unit>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<Unit> {
        // Validate input parameters
        if (parameters.poseDetections.isEmpty()) {
            return Result.Error(IllegalArgumentException("Pose detections cannot be empty"))
        }
        
        // Validate all pose detections have the same session ID
        val sessionId = parameters.poseDetections.first().sessionId
        if (parameters.poseDetections.any { it.sessionId != sessionId }) {
            return Result.Error(IllegalArgumentException("All pose detections must belong to the same session"))
        }
        
        // Validate session exists
        val sessionResult = swingRepository.getSwingSession(sessionId)
        when (sessionResult) {
            is Result.Success -> {
                if (sessionResult.data == null) {
                    return Result.Error(IllegalArgumentException("Session not found"))
                }
            }
            is Result.Error -> return sessionResult
            is Result.Loading -> return Result.Loading()
        }
        
        // Save pose detections
        return if (parameters.poseDetections.size == 1) {
            swingRepository.savePoseDetection(parameters.poseDetections.first())
        } else {
            swingRepository.savePoseDetections(parameters.poseDetections)
        }
    }
    
    data class Params(
        val poseDetections: List<PoseDetection>
    )
}