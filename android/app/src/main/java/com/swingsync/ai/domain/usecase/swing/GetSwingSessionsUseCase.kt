package com.swingsync.ai.domain.usecase.swing

import com.swingsync.ai.domain.model.SwingSession
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.usecase.BaseFlowUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting swing sessions for a user.
 */
class GetSwingSessionsUseCase @Inject constructor(
    private val swingRepository: SwingRepository,
    dispatcher: CoroutineDispatcher
) : BaseFlowUseCase<GetSwingSessionsUseCase.Params, List<SwingSession>>(dispatcher) {
    
    override suspend fun execute(parameters: Params): Flow<List<SwingSession>> {
        return when (parameters.filterType) {
            FilterType.ALL -> swingRepository.getSwingSessionsByUserId(parameters.userId)
            FilterType.RECENT -> swingRepository.getRecentSessions(parameters.userId, parameters.limit ?: 10)
            FilterType.BY_CLUB -> swingRepository.getSessionsByClub(parameters.userId, parameters.clubType!!)
        }
    }
    
    data class Params(
        val userId: String,
        val filterType: FilterType = FilterType.ALL,
        val limit: Int? = null,
        val clubType: String? = null
    )
    
    enum class FilterType {
        ALL,
        RECENT,
        BY_CLUB
    }
}