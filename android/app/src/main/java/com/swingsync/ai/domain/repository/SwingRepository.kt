package com.swingsync.ai.domain.repository

import com.swingsync.ai.domain.model.*
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for swing-related operations.
 * This interface defines the contract for swing data operations without exposing implementation details.
 */
interface SwingRepository {
    
    // Swing Session operations
    suspend fun createSwingSession(session: SwingSession): Result<Unit>
    suspend fun getSwingSession(sessionId: String): Result<SwingSession?>
    fun getSwingSessionsByUserId(userId: String): Flow<List<SwingSession>>
    suspend fun updateSwingSession(session: SwingSession): Result<Unit>
    suspend fun deleteSwingSession(sessionId: String): Result<Unit>
    fun getRecentSessions(userId: String, limit: Int): Flow<List<SwingSession>>
    fun getSessionsByClub(userId: String, clubType: String): Flow<List<SwingSession>>
    suspend fun getSessionStats(userId: String): Result<SessionStats>
    
    // Swing Analysis operations
    suspend fun analyzeSwing(sessionId: String, analysisType: String): Result<SwingAnalysis>
    suspend fun getSwingAnalysis(analysisId: String): Result<SwingAnalysis?>
    fun getSwingAnalysesBySession(sessionId: String): Flow<List<SwingAnalysis>>
    suspend fun saveSwingAnalysis(analysis: SwingAnalysis): Result<Unit>
    suspend fun deleteSwingAnalysis(analysisId: String): Result<Unit>
    fun getAnalysisHistory(userId: String, limit: Int): Flow<List<SwingAnalysis>>
    suspend fun getAnalysisStats(userId: String): Result<AnalysisStats>
    
    // Pose Detection operations
    suspend fun savePoseDetection(detection: PoseDetection): Result<Unit>
    suspend fun savePoseDetections(detections: List<PoseDetection>): Result<Unit>
    fun getPoseDetectionsBySession(sessionId: String): Flow<List<PoseDetection>>
    suspend fun getPoseDetection(detectionId: String): Result<PoseDetection?>
    suspend fun deletePoseDetectionsBySession(sessionId: String): Result<Unit>
    
    // Feedback operations
    suspend fun getSwingFeedback(analysisId: String): Result<SwingFeedback>
    suspend fun submitUserFeedback(userId: String, analysisId: String, rating: Int, comment: String?): Result<Unit>
    
    // Coaching operations
    suspend fun getCoachingTips(userLevel: String, clubType: String): Result<List<CoachingTip>>
    suspend fun getPersonalizedTips(userId: String): Result<List<CoachingTip>>
}

/**
 * Repository interface for user-related operations.
 */
interface UserRepository {
    
    // User Settings operations
    suspend fun getUserSettings(userId: String): Result<UserSettings?>
    fun getUserSettingsFlow(userId: String): Flow<UserSettings?>
    suspend fun updateUserSettings(settings: UserSettings): Result<Unit>
    suspend fun createDefaultSettings(userId: String): Result<Unit>
    
    // User Progress operations
    suspend fun getUserProgress(userId: String): Result<List<UserProgress>>
    fun getUserProgressFlow(userId: String): Flow<List<UserProgress>>
    suspend fun updateUserProgress(progress: UserProgress): Result<Unit>
    suspend fun getProgressByMetric(userId: String, metricName: String): Result<UserProgress?>
    suspend fun updateProgressValue(userId: String, metricName: String, value: Float): Result<Unit>
    suspend fun deleteUserProgress(progressId: String): Result<Unit>
    
    // Achievement operations
    suspend fun getUserAchievements(userId: String): Result<List<Achievement>>
    suspend fun unlockAchievement(userId: String, achievementId: String): Result<Unit>
    suspend fun getAchievementProgress(userId: String, achievementId: String): Result<Float>
    
    // Sync operations
    suspend fun syncUserData(userId: String): Result<Unit>
    suspend fun syncUserProgress(userId: String): Result<Unit>
}

/**
 * Repository interface for social and competitive features.
 */
interface SocialRepository {
    
    // Leaderboard operations
    suspend fun getLeaderboard(category: String, timeframe: String): Result<Leaderboard>
    suspend fun getUserRank(userId: String, category: String): Result<Int?>
    
    // Favorites operations
    suspend fun markSwingAsFavorite(userId: String, sessionId: String): Result<Unit>
    suspend fun removeSwingFromFavorites(userId: String, sessionId: String): Result<Unit>
    fun getFavoriteSwings(userId: String): Flow<List<SwingSession>>
    
    // Sharing operations
    suspend fun shareSwingAnalysis(userId: String, analysisId: String): Result<String>
    suspend fun getSharedAnalysis(shareId: String): Result<SwingAnalysis?>
}

/**
 * Data classes for statistics and summaries.
 */
data class SessionStats(
    val totalSessions: Int,
    val completedSessions: Int,
    val averageFramesPerSession: Float,
    val mostUsedClub: String,
    val totalPracticeTime: Long,
    val clubUsageStats: Map<String, Int>
)

data class AnalysisStats(
    val totalAnalyses: Int,
    val averageScore: Float,
    val bestScore: Float,
    val scoresByType: Map<String, Float>,
    val improvementTrend: ProgressTrend,
    val recentPerformance: List<Float>
)