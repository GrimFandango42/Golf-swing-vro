package com.swingsync.ai.domain.usecase.analysis

import com.swingsync.ai.domain.model.SwingAnalysis
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseFlowUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting swing analysis results.
 */
class GetSwingAnalysisUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseFlowUseCase<GetSwingAnalysisUseCase.Params, List<SwingAnalysis>>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Flow<List<SwingAnalysis>> {
        return when (parameters.filterType) {
            FilterType.BY_SESSION -> swingRepository.getSwingAnalysesBySession(parameters.sessionId!!)
            FilterType.HISTORY -> swingRepository.getAnalysisHistory(parameters.userId!!, parameters.limit ?: 20)
        }
    }
    
    data class Params(
        val filterType: FilterType,
        val sessionId: String? = null,
        val userId: String? = null,
        val limit: Int? = null
    )
    
    enum class FilterType {
        BY_SESSION,
        HISTORY
    }
}