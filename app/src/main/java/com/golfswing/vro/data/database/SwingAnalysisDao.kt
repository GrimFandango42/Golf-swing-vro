package com.golfswing.vro.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.golfswing.vro.data.entity.SwingAnalysisEntity
import com.golfswing.vro.data.entity.UserProfileEntity
import com.golfswing.vro.data.entity.SwingSessionEntity
import com.golfswing.vro.data.entity.PracticeGoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SwingAnalysisDao {
    
    @Query("SELECT * FROM swing_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE id = :id")
    suspend fun getAnalysisById(id: String): SwingAnalysisEntity?
    
    @Query("SELECT * FROM swing_analyses WHERE isProcessed = :isProcessed ORDER BY timestamp DESC")
    fun getAnalysesByProcessingStatus(isProcessed: Boolean): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getAnalysesByDateRange(startTime: Long, endTime: Long): Flow<List<SwingAnalysisEntity>>
    
    @Query("SELECT * FROM swing_analyses ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAnalyses(limit: Int): Flow<List<SwingAnalysisEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: SwingAnalysisEntity)
    
    @Update
    suspend fun updateAnalysis(analysis: SwingAnalysisEntity)
    
    @Delete
    suspend fun deleteAnalysis(analysis: SwingAnalysisEntity)
    
    @Query("DELETE FROM swing_analyses WHERE id = :id")
    suspend fun deleteAnalysisById(id: String)
    
    @Query("DELETE FROM swing_analyses")
    suspend fun deleteAllAnalyses()
    
    @Query("SELECT COUNT(*) FROM swing_analyses")
    suspend fun getAnalysisCount(): Int
    
    @Query("SELECT AVG(overallScore) FROM swing_analyses WHERE isProcessed = 1")
    suspend fun getAverageScore(): Float?
    
    @Query("SELECT * FROM swing_analyses WHERE overallScore >= :minScore ORDER BY overallScore DESC")
    fun getAnalysesWithScoreAbove(minScore: Float): Flow<List<SwingAnalysisEntity>>
}

@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfileEntity?
    
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
    
    @Update
    suspend fun updateProfile(profile: UserProfileEntity)
    
    @Delete
    suspend fun deleteProfile(profile: UserProfileEntity)
    
    @Query("UPDATE user_profiles SET handicap = :handicap, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateHandicap(userId: String, handicap: Float, updatedAt: Date)
}

@Dao
interface SwingSessionDao {
    
    @Query("SELECT * FROM swing_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getSessionsByUser(userId: String): Flow<List<SwingSessionEntity>>
    
    @Query("SELECT * FROM swing_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): SwingSessionEntity?
    
    @Query("SELECT * FROM swing_sessions WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getSessionsByDateRange(startTime: Date, endTime: Date): Flow<List<SwingSessionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SwingSessionEntity)
    
    @Update
    suspend fun updateSession(session: SwingSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: SwingSessionEntity)
    
    @Query("UPDATE swing_sessions SET swingCount = :swingCount WHERE sessionId = :sessionId")
    suspend fun updateSwingCount(sessionId: String, swingCount: Int)
    
    @Query("SELECT COUNT(*) FROM swing_sessions WHERE userId = :userId")
    suspend fun getSessionCountByUser(userId: String): Int
    
    @Query("SELECT SUM(swingCount) FROM swing_sessions WHERE userId = :userId")
    suspend fun getTotalSwingsByUser(userId: String): Int?
}

@Dao
interface PracticeGoalDao {
    
    @Query("SELECT * FROM practice_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsByUser(userId: String): Flow<List<PracticeGoalEntity>>
    
    @Query("SELECT * FROM practice_goals WHERE goalId = :goalId")
    suspend fun getGoalById(goalId: String): PracticeGoalEntity?
    
    @Query("SELECT * FROM practice_goals WHERE userId = :userId AND isCompleted = :isCompleted ORDER BY createdAt DESC")
    fun getGoalsByCompletionStatus(userId: String, isCompleted: Boolean): Flow<List<PracticeGoalEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: PracticeGoalEntity)
    
    @Update
    suspend fun updateGoal(goal: PracticeGoalEntity)
    
    @Delete
    suspend fun deleteGoal(goal: PracticeGoalEntity)
    
    @Query("UPDATE practice_goals SET currentValue = :currentValue WHERE goalId = :goalId")
    suspend fun updateGoalProgress(goalId: String, currentValue: Float)
    
    @Query("UPDATE practice_goals SET isCompleted = :isCompleted WHERE goalId = :goalId")
    suspend fun updateGoalCompletion(goalId: String, isCompleted: Boolean)
}