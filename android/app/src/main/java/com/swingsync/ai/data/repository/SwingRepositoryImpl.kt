package com.swingsync.ai.data.repository

import com.swingsync.ai.data.datasource.local.LocalDataSource
import com.swingsync.ai.data.datasource.remote.RemoteDataSource
import com.swingsync.ai.data.mapper.SwingDataMapper
import com.swingsync.ai.data.remote.dto.SwingAnalysisRequestDto
import com.swingsync.ai.data.remote.dto.UserFeedbackDto
import com.swingsync.ai.domain.model.*
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.util.Result
import com.swingsync.ai.domain.util.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SwingRepository that coordinates between local and remote data sources.
 */
@Singleton
class SwingRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val mapper: SwingDataMapper
) : SwingRepository {
    
    // Swing Session operations
    override suspend fun createSwingSession(session: SwingSession): Result<Unit> {
        val entity = mapper.toEntity(session)
        return localDataSource.insertSwingSession(entity)
    }
    
    override suspend fun getSwingSession(sessionId: String): Result<SwingSession?> {
        return localDataSource.getSwingSession(sessionId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }
    
    override fun getSwingSessionsByUserId(userId: String): Flow<List<SwingSession>> {
        return localDataSource.getSwingSessionsByUserId(userId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }
    
    override suspend fun updateSwingSession(session: SwingSession): Result<Unit> {
        val entity = mapper.toEntity(session)
        return localDataSource.updateSwingSession(entity)
    }
    
    override suspend fun deleteSwingSession(sessionId: String): Result<Unit> {
        return localDataSource.deleteSwingSession(sessionId)
    }
    
    override fun getRecentSessions(userId: String, limit: Int): Flow<List<SwingSession>> {
        return localDataSource.getSwingSessionsByUserId(userId).map { entities ->
            entities.take(limit).map { mapper.toDomain(it) }
        }
    }
    
    override fun getSessionsByClub(userId: String, clubType: String): Flow<List<SwingSession>> {
        return localDataSource.getSwingSessionsByUserId(userId).map { entities ->
            entities.filter { it.clubUsed == clubType }.map { mapper.toDomain(it) }
        }
    }
    
    override suspend fun getSessionStats(userId: String): Result<SessionStats> {
        return try {
            val sessions = localDataSource.getSwingSessionsByUserId(userId)
            // This would be implemented with proper flow collection and stats calculation
            // For now, return mock data
            Result.Success(
                SessionStats(
                    totalSessions = 0,
                    completedSessions = 0,
                    averageFramesPerSession = 0f,
                    mostUsedClub = "Driver",
                    totalPracticeTime = 0L,
                    clubUsageStats = emptyMap()
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    // Swing Analysis operations
    override suspend fun analyzeSwing(sessionId: String, analysisType: String): Result<SwingAnalysis> {
        return try {
            // Get pose data from local database
            val poseDetections = localDataSource.getPoseDetectionsBySession(sessionId)
            
            // Convert to DTOs for remote analysis
            val poseDataDtos = mutableListOf<com.swingsync.ai.data.remote.dto.PoseDataDto>()
            // This would collect from the flow and convert to DTOs
            
            // Call remote analysis service
            val request = SwingAnalysisRequestDto(
                sessionId = sessionId,
                userId = "current_user", // This would come from user session
                clubType = "Driver", // This would come from session data
                poseData = poseDataDtos,
                videoMetadata = null,
                analysisType = analysisType
            )
            
            val remoteResult = remoteDataSource.analyzeSwing(request)
            
            when (remoteResult) {
                is Result.Success -> {
                    val analysis = mapper.toDomain(remoteResult.data)
                    // Save to local database
                    localDataSource.insertSwingAnalysis(mapper.toEntity(analysis))
                    Result.Success(analysis)
                }
                is Result.Error -> remoteResult
                is Result.Loading -> remoteResult
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getSwingAnalysis(analysisId: String): Result<SwingAnalysis?> {
        return localDataSource.getSwingAnalysis(analysisId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }
    
    override fun getSwingAnalysesBySession(sessionId: String): Flow<List<SwingAnalysis>> {
        return localDataSource.getSwingAnalysesBySessionId(sessionId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }
    
    override suspend fun saveSwingAnalysis(analysis: SwingAnalysis): Result<Unit> {
        val entity = mapper.toEntity(analysis)
        return localDataSource.insertSwingAnalysis(entity)
    }
    
    override suspend fun deleteSwingAnalysis(analysisId: String): Result<Unit> {
        return localDataSource.deleteSwingAnalysis(analysisId)
    }
    
    override fun getAnalysisHistory(userId: String, limit: Int): Flow<List<SwingAnalysis>> {
        // This would need to filter by userId through session joins
        // For now, return empty flow
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    override suspend fun getAnalysisStats(userId: String): Result<AnalysisStats> {
        return try {
            // This would calculate stats from local database
            Result.Success(
                AnalysisStats(
                    totalAnalyses = 0,
                    averageScore = 0f,
                    bestScore = 0f,
                    scoresByType = emptyMap(),
                    improvementTrend = ProgressTrend.STABLE,
                    recentPerformance = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    // Pose Detection operations
    override suspend fun savePoseDetection(detection: PoseDetection): Result<Unit> {
        val entity = mapper.toEntity(detection)
        return localDataSource.insertPoseDetection(entity)
    }
    
    override suspend fun savePoseDetections(detections: List<PoseDetection>): Result<Unit> {
        val entities = detections.map { mapper.toEntity(it) }
        return localDataSource.insertPoseDetections(entities)
    }
    
    override fun getPoseDetectionsBySession(sessionId: String): Flow<List<PoseDetection>> {
        return localDataSource.getPoseDetectionsBySessionId(sessionId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }
    
    override suspend fun getPoseDetection(detectionId: String): Result<PoseDetection?> {
        return localDataSource.getPoseDetection(detectionId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }
    
    override suspend fun deletePoseDetectionsBySession(sessionId: String): Result<Unit> {
        return localDataSource.deletePoseDetectionsBySessionId(sessionId)
    }
    
    // Feedback operations
    override suspend fun getSwingFeedback(analysisId: String): Result<SwingFeedback> {
        return remoteDataSource.getSwingFeedback(analysisId).map { dto ->
            mapper.toDomain(dto)
        }
    }
    
    override suspend fun submitUserFeedback(userId: String, analysisId: String, rating: Int, comment: String?): Result<Unit> {
        val feedbackDto = UserFeedbackDto(
            analysisId = analysisId,
            userId = userId,
            rating = rating,
            comment = comment,
            helpful = rating >= 3,
            feedbackType = "analysis_feedback",
            timestamp = System.currentTimeMillis()
        )
        return remoteDataSource.submitUserFeedback(feedbackDto)
    }
    
    // Coaching operations
    override suspend fun getCoachingTips(userLevel: String, clubType: String): Result<List<CoachingTip>> {
        return remoteDataSource.getCoachingTips(userLevel, clubType).map { dtos ->
            dtos.map { mapper.toDomain(it) }
        }
    }
    
    override suspend fun getPersonalizedTips(userId: String): Result<List<CoachingTip>> {
        // This would analyze user's performance and get personalized tips
        // For now, return general tips
        return getCoachingTips("beginner", "driver")
    }
}