package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user progress operations.
 */
@Dao
interface UserProgressDao {
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId ORDER BY last_updated DESC")
    fun getUserProgress(userId: String): Flow<List<UserProgressEntity>>
    
    @Query("SELECT * FROM user_progress WHERE progress_id = :progressId")
    suspend fun getProgressById(progressId: String): UserProgressEntity?
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun getProgressByMetric(userId: String, metricName: String): UserProgressEntity?
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND metric_name = :metricName")
    fun getProgressByMetricFlow(userId: String, metricName: String): Flow<UserProgressEntity?>
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND trend = :trend ORDER BY last_updated DESC")
    fun getProgressByTrend(userId: String, trend: String): Flow<List<UserProgressEntity>>
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND target_value IS NOT NULL ORDER BY last_updated DESC")
    fun getProgressWithTargets(userId: String): Flow<List<UserProgressEntity>>
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND current_value >= best_value ORDER BY last_updated DESC")
    fun getProgressAtBest(userId: String): Flow<List<UserProgressEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgressEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<UserProgressEntity>)
    
    @Update
    suspend fun updateProgress(progress: UserProgressEntity)
    
    @Delete
    suspend fun deleteProgress(progress: UserProgressEntity)
    
    @Query("DELETE FROM user_progress WHERE progress_id = :progressId")
    suspend fun deleteProgressById(progressId: String)
    
    @Query("DELETE FROM user_progress WHERE user_id = :userId")
    suspend fun deleteAllProgressForUser(userId: String)
    
    @Query("DELETE FROM user_progress WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun deleteProgressByMetric(userId: String, metricName: String)
    
    @Query("UPDATE user_progress SET current_value = :value, last_updated = :timestamp WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun updateCurrentValue(userId: String, metricName: String, value: Float, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_progress SET best_value = :value, last_updated = :timestamp WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun updateBestValue(userId: String, metricName: String, value: Float, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_progress SET target_value = :value, last_updated = :timestamp WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun updateTargetValue(userId: String, metricName: String, value: Float?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_progress SET trend = :trend, last_updated = :timestamp WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun updateTrend(userId: String, metricName: String, trend: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE user_id = :userId")
    suspend fun getProgressCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE user_id = :userId AND trend = 'improving'")
    suspend fun getImprovingMetricsCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE user_id = :userId AND current_value >= best_value")
    suspend fun getMetricsAtBestCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE user_id = :userId AND target_value IS NOT NULL AND current_value >= target_value")
    suspend fun getTargetsAchievedCount(userId: String): Int
    
    @Query("SELECT metric_name, current_value, best_value, trend FROM user_progress WHERE user_id = :userId ORDER BY last_updated DESC")
    suspend fun getProgressSummary(userId: String): List<ProgressSummary>
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId ORDER BY (current_value - best_value) DESC LIMIT :limit")
    fun getTopPerformingMetrics(userId: String, limit: Int): Flow<List<UserProgressEntity>>
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND last_updated >= :since ORDER BY last_updated DESC")
    fun getRecentProgress(userId: String, since: Long): Flow<List<UserProgressEntity>>
    
    @Query("SELECT AVG(current_value) FROM user_progress WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun getAverageValue(userId: String, metricName: String): Float?
    
    @Query("SELECT MAX(current_value) FROM user_progress WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun getMaxValue(userId: String, metricName: String): Float?
    
    @Query("SELECT MIN(current_value) FROM user_progress WHERE user_id = :userId AND metric_name = :metricName")
    suspend fun getMinValue(userId: String, metricName: String): Float?
}

/**
 * Data class for progress summary.
 */
data class ProgressSummary(
    val metric_name: String,
    val current_value: Float,
    val best_value: Float,
    val trend: String
)