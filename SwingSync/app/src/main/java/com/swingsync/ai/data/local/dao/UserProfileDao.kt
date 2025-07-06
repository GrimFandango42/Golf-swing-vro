package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.models.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getUserProfile(id: String): UserProfile?
    
    @Query("SELECT * FROM user_profile WHERE email = :email")
    suspend fun getUserProfileByEmail(email: String): UserProfile?
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getCurrentUserProfile(): UserProfile?
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getCurrentUserProfileFlow(): Flow<UserProfile?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)
    
    @Update
    suspend fun updateUserProfile(userProfile: UserProfile)
    
    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfile)
    
    @Query("DELETE FROM user_profile WHERE id = :id")
    suspend fun deleteUserProfileById(id: String)
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteAllUserProfiles()
    
    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun getUserProfileCount(): Int
}