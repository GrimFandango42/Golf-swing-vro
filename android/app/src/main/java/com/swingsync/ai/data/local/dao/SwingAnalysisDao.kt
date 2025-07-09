package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.local.entity.SwingAnalysisEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for swing analysis operations.
 */
@Dao
interface SwingAnalysisDao {
    
    @Query("SELECT * FROM swing_analyses WHERE session_id = :sessionId ORDER BY analysis_timestamp DESC")
    fun getAnalysesBySessionId(sessionId: String): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE analysis_id = :analysisId")
    suspend fun getAnalysisById(analysisId: String): SwingAnalysisEntity?
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) ORDER BY analysis_timestamp DESC")
    fun getAnalysesByUserId(userId: String): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE analysis_type = :analysisType AND session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) ORDER BY analysis_timestamp DESC")
    fun getAnalysesByType(userId: String, analysisType: String): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) ORDER BY analysis_timestamp DESC LIMIT :limit")
    fun getRecentAnalyses(userId: String, limit: Int): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId AND club_used = :clubUsed) ORDER BY analysis_timestamp DESC")
    fun getAnalysesByClub(userId: String, clubUsed: String): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) AND analysis_timestamp >= :startTime AND analysis_timestamp <= :endTime ORDER BY analysis_timestamp DESC")
    fun getAnalysesByDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<SwingAnalysisEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: SwingAnalysisEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyses(analyses: List<SwingAnalysisEntity>)
    
    @Update
    suspend fun updateAnalysis(analysis: SwingAnalysisEntity)
    
    @Delete
    suspend fun deleteAnalysis(analysis: SwingAnalysisEntity)
    
    @Query("DELETE FROM swing_analyses WHERE analysis_id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: String)
    
    @Query("DELETE FROM swing_analyses WHERE session_id = :sessionId")
    suspend fun deleteAnalysesBySessionId(sessionId: String)
    
    @Query("DELETE FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId)")
    suspend fun deleteAllAnalysesForUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId)")
    suspend fun getTotalAnalysisCount(userId: String): Int
    
    @Query("SELECT AVG(score) FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId)")
    suspend fun getAverageScore(userId: String): Float?
    
    @Query("SELECT AVG(score) FROM swing_analyses WHERE analysis_type = :analysisType AND session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId)")
    suspend fun getAverageScoreByType(userId: String, analysisType: String): Float?
    
    @Query("SELECT MAX(score) FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId)")
    suspend fun getBestScore(userId: String): Float?
    
    @Query("SELECT analysis_type, AVG(score) as average_score FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) GROUP BY analysis_type")
    suspend fun getScoresByAnalysisType(userId: String): List<AnalysisTypeScore>
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) AND score >= :minScore ORDER BY score DESC")
    fun getHighScoreAnalyses(userId: String, minScore: Float): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) ORDER BY score DESC LIMIT 1")
    suspend fun getBestAnalysis(userId: String): SwingAnalysisEntity?
    
    @Query("SELECT * FROM swing_analyses WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId) ORDER BY score ASC LIMIT 1")
    suspend fun getWorstAnalysis(userId: String): SwingAnalysisEntity?
}

/**
 * Data class for analysis type scores.
 */
data class AnalysisTypeScore(
    val analysis_type: String,
    val average_score: Float
)