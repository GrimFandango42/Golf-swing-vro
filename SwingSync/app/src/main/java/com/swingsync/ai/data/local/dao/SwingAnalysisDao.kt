package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.models.SwingAnalysis
import kotlinx.coroutines.flow.Flow

@Dao
interface SwingAnalysisDao {
    
    @Query("SELECT * FROM swing_analysis WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllAnalyses(userId: String): Flow<List<SwingAnalysis>>
    
    @Query("SELECT * FROM swing_analysis WHERE id = :id")
    suspend fun getAnalysisById(id: String): SwingAnalysis?
    
    @Query("SELECT * FROM swing_analysis WHERE user_id = :userId AND swing_type = :swingType ORDER BY timestamp DESC")
    fun getAnalysesByType(userId: String, swingType: String): Flow<List<SwingAnalysis>>
    
    @Query("SELECT * FROM swing_analysis WHERE is_synced = 0")
    suspend fun getUnsyncedAnalyses(): List<SwingAnalysis>
    
    @Query("SELECT * FROM swing_analysis WHERE user_id = :userId ORDER BY score DESC LIMIT :limit")
    suspend fun getTopAnalyses(userId: String, limit: Int): List<SwingAnalysis>
    
    @Query("SELECT AVG(score) FROM swing_analysis WHERE user_id = :userId")
    suspend fun getAverageScore(userId: String): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: SwingAnalysis)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyses(analyses: List<SwingAnalysis>)
    
    @Update
    suspend fun updateAnalysis(analysis: SwingAnalysis)
    
    @Query("UPDATE swing_analysis SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Delete
    suspend fun deleteAnalysis(analysis: SwingAnalysis)
    
    @Query("DELETE FROM swing_analysis WHERE id = :id")
    suspend fun deleteAnalysisById(id: String)
    
    @Query("DELETE FROM swing_analysis WHERE user_id = :userId")
    suspend fun deleteAllUserAnalyses(userId: String)
    
    @Query("SELECT COUNT(*) FROM swing_analysis WHERE user_id = :userId")
    suspend fun getAnalysisCount(userId: String): Int
    
    @Query("SELECT * FROM swing_analysis WHERE user_id = :userId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getAnalysesInDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<SwingAnalysis>
}