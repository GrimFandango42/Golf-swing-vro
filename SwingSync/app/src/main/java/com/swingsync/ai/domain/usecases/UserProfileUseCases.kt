package com.swingsync.ai.domain.usecases

import com.swingsync.ai.data.models.UserProfile
import com.swingsync.ai.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userId: String): UserProfile? {
        return repository.getUserProfile(userId)
    }
}

class GetCurrentUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(): UserProfile? {
        return repository.getCurrentUserProfile()
    }
}

class GetCurrentUserProfileFlowUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile?> {
        return repository.getCurrentUserProfileFlow()
    }
}

class CreateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userProfile: UserProfile): Result<UserProfile> {
        return repository.createUserProfile(userProfile)
    }
}

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userProfile: UserProfile): Result<UserProfile> {
        return repository.updateUserProfile(userProfile)
    }
}

class DeleteUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return repository.deleteUserProfile(userId)
    }
}

class SyncUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userId: String): Result<UserProfile> {
        return repository.syncUserProfile(userId)
    }
}

class RefreshUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userId: String): Result<UserProfile> {
        return repository.refreshUserProfile(userId)
    }
}