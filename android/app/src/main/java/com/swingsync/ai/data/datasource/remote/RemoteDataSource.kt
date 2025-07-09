package com.swingsync.ai.data.datasource.remote

import com.swingsync.ai.data.remote.api.SwingAnalysisApiService
import com.swingsync.ai.data.remote.dto.*
import com.swingsync.ai.domain.util.Result
import com.swingsync.ai.domain.util.safeSuspendCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source interface for abstraction.
 */
interface RemoteDataSource {
    suspend fun analyzeSwing(request: SwingAnalysisRequestDto): Result<SwingAnalysisResponseDto>
    suspend fun uploadSwingVideo(videoData: ByteArray, sessionId: String): Result<VideoUploadResponseDto>
    suspend fun getSwingFeedback(analysisId: String): Result<SwingFeedbackResponseDto>
    suspend fun submitUserFeedback(feedback: UserFeedbackDto): Result<Unit>
    suspend fun getCoachingTips(userLevel: String, clubType: String): Result<List<CoachingTipDto>>
    suspend fun syncUserProgress(userId: String, progressData: UserProgressDto): Result<Unit>
    suspend fun getLeaderboard(category: String, timeframe: String): Result<LeaderboardResponseDto>
}

/**
 * Implementation of remote data source using REST API.
 */
@Singleton
class RemoteDataSourceImpl @Inject constructor(
    private val apiService: SwingAnalysisApiService
) : RemoteDataSource {
    
    override suspend fun analyzeSwing(request: SwingAnalysisRequestDto): Result<SwingAnalysisResponseDto> {
        return safeSuspendCall { apiService.analyzeSwing(request) }
    }
    
    override suspend fun uploadSwingVideo(videoData: ByteArray, sessionId: String): Result<VideoUploadResponseDto> {
        return safeSuspendCall { apiService.uploadSwingVideo(videoData, sessionId) }
    }
    
    override suspend fun getSwingFeedback(analysisId: String): Result<SwingFeedbackResponseDto> {
        return safeSuspendCall { apiService.getSwingFeedback(analysisId) }
    }
    
    override suspend fun submitUserFeedback(feedback: UserFeedbackDto): Result<Unit> {
        return safeSuspendCall { apiService.submitUserFeedback(feedback) }
    }
    
    override suspend fun getCoachingTips(userLevel: String, clubType: String): Result<List<CoachingTipDto>> {
        return safeSuspendCall { apiService.getCoachingTips(userLevel, clubType) }
    }
    
    override suspend fun syncUserProgress(userId: String, progressData: UserProgressDto): Result<Unit> {
        return safeSuspendCall { apiService.syncUserProgress(userId, progressData) }
    }
    
    override suspend fun getLeaderboard(category: String, timeframe: String): Result<LeaderboardResponseDto> {
        return safeSuspendCall { apiService.getLeaderboard(category, timeframe) }
    }
}