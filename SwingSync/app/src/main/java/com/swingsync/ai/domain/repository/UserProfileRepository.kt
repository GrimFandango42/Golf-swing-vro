package com.swingsync.ai.domain.repository

import com.swingsync.ai.data.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    
    suspend fun getUserProfile(userId: String): UserProfile?
    
    suspend fun getCurrentUserProfile(): UserProfile?
    
    fun getCurrentUserProfileFlow(): Flow<UserProfile?>
    
    suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile>
    
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>
    
    suspend fun deleteUserProfile(userId: String): Result<Unit>
    
    suspend fun syncUserProfile(userId: String): Result<UserProfile>
    
    suspend fun refreshUserProfile(userId: String): Result<UserProfile>
}