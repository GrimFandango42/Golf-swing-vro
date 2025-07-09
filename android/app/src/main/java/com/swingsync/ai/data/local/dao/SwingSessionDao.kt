package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.local.entity.SwingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for swing session operations.
 */
@Dao
interface SwingSessionDao {
    
    @Query("SELECT * FROM swing_sessions WHERE user_id = :userId ORDER BY created_at DESC")
    fun getSessionsByUserId(userId: String): Flow<List<SwingSessionEntity>>
    
    @Query("SELECT * FROM swing_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): SwingSessionEntity?
    
    @Query("SELECT * FROM swing_sessions WHERE user_id = :userId AND is_completed = 1 ORDER BY created_at DESC LIMIT :limit")
    fun getCompletedSessions(userId: String, limit: Int): Flow<List<SwingSessionEntity>>
    
    @Query("SELECT * FROM swing_sessions WHERE user_id = :userId AND is_completed = 0 ORDER BY created_at DESC LIMIT 1")
    suspend fun getActiveSession(userId: String): SwingSessionEntity?
    
    @Query("SELECT * FROM swing_sessions WHERE user_id = :userId AND club_used = :clubUsed ORDER BY created_at DESC")
    fun getSessionsByClub(userId: String, clubUsed: String): Flow<List<SwingSessionEntity>>
    
    @Query("SELECT * FROM swing_sessions WHERE user_id = :userId AND created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    fun getSessionsByDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<SwingSessionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SwingSessionEntity)
    
    @Update
    suspend fun updateSession(session: SwingSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: SwingSessionEntity)
    
    @Query("DELETE FROM swing_sessions WHERE session_id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
    
    @Query("DELETE FROM swing_sessions WHERE user_id = :userId")
    suspend fun deleteAllSessionsForUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM swing_sessions WHERE user_id = :userId")
    suspend fun getTotalSessionCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM swing_sessions WHERE user_id = :userId AND is_completed = 1")
    suspend fun getCompletedSessionCount(userId: String): Int
    
    @Query("SELECT AVG(total_frames) FROM swing_sessions WHERE user_id = :userId AND is_completed = 1")
    suspend fun getAverageFramesPerSession(userId: String): Float?
    
    @Query("SELECT club_used, COUNT(*) as count FROM swing_sessions WHERE user_id = :userId AND is_completed = 1 GROUP BY club_used ORDER BY count DESC")
    suspend fun getClubUsageStats(userId: String): List<ClubUsageStats>
    
    @Query("SELECT * FROM swing_sessions WHERE user_id = :userId AND created_at >= :since ORDER BY created_at DESC")
    fun getRecentSessions(userId: String, since: Long): Flow<List<SwingSessionEntity>>
}

/**
 * Data class for club usage statistics.
 */
data class ClubUsageStats(
    val club_used: String,
    val count: Int
)