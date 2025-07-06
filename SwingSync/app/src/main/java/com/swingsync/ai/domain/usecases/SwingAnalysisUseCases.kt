package com.swingsync.ai.domain.usecases

import com.swingsync.ai.data.models.SwingAnalysis
import com.swingsync.ai.domain.repository.SwingAnalysisRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAnalysesUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    operator fun invoke(userId: String): Flow<List<SwingAnalysis>> {
        return repository.getAllAnalyses(userId)
    }
}

class GetAnalysisByIdUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(id: String): SwingAnalysis? {
        return repository.getAnalysisById(id)
    }
}

class CreateAnalysisUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(analysis: SwingAnalysis): Result<SwingAnalysis> {
        return repository.createAnalysis(analysis)
    }
}

class UpdateAnalysisUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(analysis: SwingAnalysis): Result<SwingAnalysis> {
        return repository.updateAnalysis(analysis)
    }
}

class DeleteAnalysisUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.deleteAnalysis(id)
    }
}

class SyncAnalysesUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return repository.syncAnalyses(userId)
    }
}

class GetTopAnalysesUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(userId: String, limit: Int = 10): List<SwingAnalysis> {
        return repository.getTopAnalyses(userId, limit)
    }
}

class GetAverageScoreUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(userId: String): Float {
        return repository.getAverageScore(userId)
    }
}

class GetAnalysesByTypeUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    operator fun invoke(userId: String, swingType: String): Flow<List<SwingAnalysis>> {
        return repository.getAnalysesByType(userId, swingType)
    }
}

class GetAnalysisCountUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(userId: String): Int {
        return repository.getAnalysisCount(userId)
    }
}

class RefreshAnalysesUseCase @Inject constructor(
    private val repository: SwingAnalysisRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return repository.refreshAnalyses(userId)
    }
}