package com.swingsync.ai.domain.usecase.analysis

import com.swingsync.ai.domain.model.SwingFeedback
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseUseCase
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for getting detailed swing feedback.
 */
class GetSwingFeedbackUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<GetSwingFeedbackUseCase.Params, SwingFeedback>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Result<SwingFeedback> {
        // Validate input parameters
        if (parameters.analysisId.isBlank()) {
            return Result.Error(IllegalArgumentException("Analysis ID cannot be blank"))
        }
        
        // Validate analysis exists
        val analysisResult = swingRepository.getSwingAnalysis(parameters.analysisId)
        when (analysisResult) {
            is Result.Success -> {
                if (analysisResult.data == null) {
                    return Result.Error(IllegalArgumentException("Analysis not found"))
                }
            }
            is Result.Error -> return analysisResult
            is Result.Loading -> return Result.Loading()
        }
        
        // Get swing feedback
        return swingRepository.getSwingFeedback(parameters.analysisId)
    }
    
    data class Params(
        val analysisId: String
    )
}