package com.swingsync.ai.data.repository

import com.swingsync.ai.data.local.dao.UserProfileDao
import com.swingsync.ai.data.models.UserProfile
import com.swingsync.ai.data.remote.ApiService
import com.swingsync.ai.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {
    
    override suspend fun getUserProfile(userId: String): UserProfile? {
        return userProfileDao.getUserProfile(userId)
    }
    
    override suspend fun getCurrentUserProfile(): UserProfile? {
        return userProfileDao.getCurrentUserProfile()
    }
    
    override fun getCurrentUserProfileFlow(): Flow<UserProfile?> {
        return userProfileDao.getCurrentUserProfileFlow()
    }
    
    override suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            // Save locally first
            userProfileDao.insertUserProfile(userProfile)
            
            // Try to sync with server
            val response = apiService.createUserProfile(userProfile)
            if (response.isSuccessful) {
                response.body()?.let { serverProfile ->
                    userProfileDao.insertUserProfile(serverProfile)
                    Result.success(serverProfile)
                } ?: Result.failure(Exception("Server response body is null"))
            } else {
                Timber.w("Failed to sync user profile with server: ${response.code()}")
                Result.success(userProfile) // Return local profile if server sync fails
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating user profile")
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            userProfileDao.updateUserProfile(userProfile)
            
            // Try to sync with server
            val response = apiService.updateUserProfile(userProfile.id, userProfile)
            if (response.isSuccessful) {
                response.body()?.let { serverProfile ->
                    userProfileDao.insertUserProfile(serverProfile)
                    Result.success(serverProfile)
                } ?: Result.failure(Exception("Server response body is null"))
            } else {
                Timber.w("Failed to sync updated user profile with server: ${response.code()}")
                Result.success(userProfile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating user profile")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return try {
            userProfileDao.deleteUserProfileById(userId)
            
            // Try to delete from server
            val response = apiService.deleteUserProfile(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Timber.w("Failed to delete user profile from server: ${response.code()}")
                Result.success(Unit) // Local deletion succeeded
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting user profile")
            Result.failure(e)
        }
    }
    
    override suspend fun syncUserProfile(userId: String): Result<UserProfile> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.isSuccessful) {
                response.body()?.let { serverProfile ->
                    userProfileDao.insertUserProfile(serverProfile)
                    Result.success(serverProfile)
                } ?: Result.failure(Exception("Server response body is null"))
            } else {
                Result.failure(Exception("Failed to sync user profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing user profile")
            Result.failure(e)
        }
    }
    
    override suspend fun refreshUserProfile(userId: String): Result<UserProfile> {
        return syncUserProfile(userId)
    }
}