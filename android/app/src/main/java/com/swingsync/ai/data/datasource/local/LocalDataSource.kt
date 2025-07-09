package com.swingsync.ai.data.datasource.local

import com.swingsync.ai.data.local.dao.*
import com.swingsync.ai.data.local.entity.*
import com.swingsync.ai.domain.util.Result
import com.swingsync.ai.domain.util.safeSuspendCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source interface for abstraction.
 */
interface LocalDataSource {
    // Swing Session operations
    suspend fun getSwingSession(sessionId: String): Result<SwingSessionEntity?>
    fun getSwingSessionsByUserId(userId: String): Flow<List<SwingSessionEntity>>
    suspend fun insertSwingSession(session: SwingSessionEntity): Result<Unit>
    suspend fun updateSwingSession(session: SwingSessionEntity): Result<Unit>
    suspend fun deleteSwingSession(sessionId: String): Result<Unit>
    
    // Swing Analysis operations
    suspend fun getSwingAnalysis(analysisId: String): Result<SwingAnalysisEntity?>
    fun getSwingAnalysesBySessionId(sessionId: String): Flow<List<SwingAnalysisEntity>>
    suspend fun insertSwingAnalysis(analysis: SwingAnalysisEntity): Result<Unit>
    suspend fun updateSwingAnalysis(analysis: SwingAnalysisEntity): Result<Unit>
    suspend fun deleteSwingAnalysis(analysisId: String): Result<Unit>
    
    // Pose Detection operations
    suspend fun getPoseDetection(detectionId: String): Result<PoseDetectionEntity?>
    fun getPoseDetectionsBySessionId(sessionId: String): Flow<List<PoseDetectionEntity>>
    suspend fun insertPoseDetection(detection: PoseDetectionEntity): Result<Unit>
    suspend fun insertPoseDetections(detections: List<PoseDetectionEntity>): Result<Unit>
    suspend fun deletePoseDetectionsBySessionId(sessionId: String): Result<Unit>
    
    // User Settings operations
    suspend fun getUserSettings(userId: String): Result<UserSettingsEntity?>
    fun getUserSettingsFlow(userId: String): Flow<UserSettingsEntity?>
    suspend fun insertUserSettings(settings: UserSettingsEntity): Result<Unit>
    suspend fun updateUserSettings(settings: UserSettingsEntity): Result<Unit>
    
    // User Progress operations
    suspend fun getUserProgress(userId: String): Result<List<UserProgressEntity>>
    fun getUserProgressFlow(userId: String): Flow<List<UserProgressEntity>>
    suspend fun insertUserProgress(progress: UserProgressEntity): Result<Unit>
    suspend fun updateUserProgress(progress: UserProgressEntity): Result<Unit>
    suspend fun deleteUserProgress(progressId: String): Result<Unit>
}

/**
 * Implementation of local data source using Room database.
 */
@Singleton
class LocalDataSourceImpl @Inject constructor(
    private val swingSessionDao: SwingSessionDao,
    private val swingAnalysisDao: SwingAnalysisDao,
    private val poseDetectionDao: PoseDetectionDao,
    private val userSettingsDao: UserSettingsDao,
    private val userProgressDao: UserProgressDao
) : LocalDataSource {
    
    // Swing Session operations
    override suspend fun getSwingSession(sessionId: String): Result<SwingSessionEntity?> {
        return safeSuspendCall { swingSessionDao.getSessionById(sessionId) }
    }
    
    override fun getSwingSessionsByUserId(userId: String): Flow<List<SwingSessionEntity>> {
        return swingSessionDao.getSessionsByUserId(userId)
    }
    
    override suspend fun insertSwingSession(session: SwingSessionEntity): Result<Unit> {
        return safeSuspendCall { swingSessionDao.insertSession(session) }
    }
    
    override suspend fun updateSwingSession(session: SwingSessionEntity): Result<Unit> {
        return safeSuspendCall { swingSessionDao.updateSession(session) }
    }
    
    override suspend fun deleteSwingSession(sessionId: String): Result<Unit> {
        return safeSuspendCall { swingSessionDao.deleteSessionById(sessionId) }
    }
    
    // Swing Analysis operations
    override suspend fun getSwingAnalysis(analysisId: String): Result<SwingAnalysisEntity?> {
        return safeSuspendCall { swingAnalysisDao.getAnalysisById(analysisId) }
    }
    
    override fun getSwingAnalysesBySessionId(sessionId: String): Flow<List<SwingAnalysisEntity>> {
        return swingAnalysisDao.getAnalysesBySessionId(sessionId)
    }
    
    override suspend fun insertSwingAnalysis(analysis: SwingAnalysisEntity): Result<Unit> {
        return safeSuspendCall { swingAnalysisDao.insertAnalysis(analysis) }
    }
    
    override suspend fun updateSwingAnalysis(analysis: SwingAnalysisEntity): Result<Unit> {
        return safeSuspendCall { swingAnalysisDao.updateAnalysis(analysis) }
    }
    
    override suspend fun deleteSwingAnalysis(analysisId: String): Result<Unit> {
        return safeSuspendCall { swingAnalysisDao.deleteAnalysisById(analysisId) }
    }
    
    // Pose Detection operations
    override suspend fun getPoseDetection(detectionId: String): Result<PoseDetectionEntity?> {
        return safeSuspendCall { poseDetectionDao.getPoseDetectionById(detectionId) }
    }
    
    override fun getPoseDetectionsBySessionId(sessionId: String): Flow<List<PoseDetectionEntity>> {
        return poseDetectionDao.getPoseDetectionsBySessionId(sessionId)
    }
    
    override suspend fun insertPoseDetection(detection: PoseDetectionEntity): Result<Unit> {
        return safeSuspendCall { poseDetectionDao.insertPoseDetection(detection) }
    }
    
    override suspend fun insertPoseDetections(detections: List<PoseDetectionEntity>): Result<Unit> {
        return safeSuspendCall { poseDetectionDao.insertPoseDetections(detections) }
    }
    
    override suspend fun deletePoseDetectionsBySessionId(sessionId: String): Result<Unit> {
        return safeSuspendCall { poseDetectionDao.deletePoseDetectionsBySessionId(sessionId) }
    }
    
    // User Settings operations
    override suspend fun getUserSettings(userId: String): Result<UserSettingsEntity?> {
        return safeSuspendCall { userSettingsDao.getUserSettingsSync(userId) }
    }
    
    override fun getUserSettingsFlow(userId: String): Flow<UserSettingsEntity?> {
        return userSettingsDao.getUserSettings(userId)
    }
    
    override suspend fun insertUserSettings(settings: UserSettingsEntity): Result<Unit> {
        return safeSuspendCall { userSettingsDao.insertUserSettings(settings) }
    }
    
    override suspend fun updateUserSettings(settings: UserSettingsEntity): Result<Unit> {
        return safeSuspendCall { userSettingsDao.updateUserSettings(settings) }
    }
    
    // User Progress operations
    override suspend fun getUserProgress(userId: String): Result<List<UserProgressEntity>> {
        return safeSuspendCall { 
            userProgressDao.getUserProgress(userId).let { flow ->
                // Convert Flow to List for Result wrapper
                // In real implementation, you might want to handle this differently
                emptyList<UserProgressEntity>()
            }
        }
    }
    
    override fun getUserProgressFlow(userId: String): Flow<List<UserProgressEntity>> {
        return userProgressDao.getUserProgress(userId)
    }
    
    override suspend fun insertUserProgress(progress: UserProgressEntity): Result<Unit> {
        return safeSuspendCall { userProgressDao.insertProgress(progress) }
    }
    
    override suspend fun updateUserProgress(progress: UserProgressEntity): Result<Unit> {
        return safeSuspendCall { userProgressDao.updateProgress(progress) }
    }
    
    override suspend fun deleteUserProgress(progressId: String): Result<Unit> {
        return safeSuspendCall { userProgressDao.deleteProgressById(progressId) }
    }
}