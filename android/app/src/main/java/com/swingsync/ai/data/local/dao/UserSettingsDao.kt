package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user settings operations.
 */
@Dao
interface UserSettingsDao {
    
    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    fun getUserSettings(userId: String): Flow<UserSettingsEntity?>
    
    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun getUserSettingsSync(userId: String): UserSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(userSettings: UserSettingsEntity)
    
    @Update
    suspend fun updateUserSettings(userSettings: UserSettingsEntity)
    
    @Delete
    suspend fun deleteUserSettings(userSettings: UserSettingsEntity)
    
    @Query("DELETE FROM user_settings WHERE user_id = :userId")
    suspend fun deleteUserSettingsById(userId: String)
    
    @Query("UPDATE user_settings SET preferred_club = :club, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updatePreferredClub(userId: String, club: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_settings SET difficulty_level = :level, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updateDifficultyLevel(userId: String, level: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_settings SET units_system = :unitsSystem, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updateUnitsSystem(userId: String, unitsSystem: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_settings SET voice_coaching_enabled = :enabled, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updateVoiceCoachingEnabled(userId: String, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_settings SET celebrations_enabled = :enabled, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updateCelebrationsEnabled(userId: String, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_settings SET auto_save_enabled = :enabled, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updateAutoSaveEnabled(userId: String, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_settings SET analysis_notifications_enabled = :enabled, updated_at = :timestamp WHERE user_id = :userId")
    suspend fun updateAnalysisNotificationsEnabled(userId: String, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT preferred_club FROM user_settings WHERE user_id = :userId")
    suspend fun getPreferredClub(userId: String): String?
    
    @Query("SELECT difficulty_level FROM user_settings WHERE user_id = :userId")
    suspend fun getDifficultyLevel(userId: String): String?
    
    @Query("SELECT units_system FROM user_settings WHERE user_id = :userId")
    suspend fun getUnitsSystem(userId: String): String?
    
    @Query("SELECT voice_coaching_enabled FROM user_settings WHERE user_id = :userId")
    suspend fun isVoiceCoachingEnabled(userId: String): Boolean?
    
    @Query("SELECT celebrations_enabled FROM user_settings WHERE user_id = :userId")
    suspend fun isCelebrationsEnabled(userId: String): Boolean?
    
    @Query("SELECT auto_save_enabled FROM user_settings WHERE user_id = :userId")
    suspend fun isAutoSaveEnabled(userId: String): Boolean?
    
    @Query("SELECT analysis_notifications_enabled FROM user_settings WHERE user_id = :userId")
    suspend fun isAnalysisNotificationsEnabled(userId: String): Boolean?
    
    @Query("SELECT COUNT(*) FROM user_settings WHERE user_id = :userId")
    suspend fun hasUserSettings(userId: String): Int
}