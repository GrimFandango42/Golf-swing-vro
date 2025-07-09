package com.swingsync.ai.domain.usecase.swing

import com.swingsync.ai.domain.model.SwingAnalysis
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for analyzing a golf swing.
 */
class AnalyzeSwingUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<AnalyzeSwingUseCase.Params, SwingAnalysis>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<SwingAnalysis> {
        // Validate input parameters
        if (parameters.sessionId.isBlank()) {
            return Result.Error(IllegalArgumentException("Session ID cannot be blank"))
        }
        
        if (parameters.analysisType.isBlank()) {
            return Result.Error(IllegalArgumentException("Analysis type cannot be blank"))
        }
        
        // Validate session exists
        val sessionResult = swingRepository.getSwingSession(parameters.sessionId)
        when (sessionResult) {
            is Result.Success -> {
                if (sessionResult.data == null) {
                    return Result.Error(IllegalArgumentException("Session not found"))
                }
                
                if (!sessionResult.data.isCompleted) {
                    return Result.Error(IllegalStateException("Session is not completed"))
                }
            }
            is Result.Error -> return sessionResult
            is Result.Loading -> return Result.Loading()
        }
        
        // Perform swing analysis
        return swingRepository.analyzeSwing(parameters.sessionId, parameters.analysisType)
    }
    
    data class Params(
        val sessionId: String,
        val analysisType: String = "full"
    )
}