package com.swingsync.ai.di

import com.swingsync.ai.data.datasource.local.LocalDataSource
import com.swingsync.ai.data.datasource.local.LocalDataSourceImpl
import com.swingsync.ai.data.datasource.remote.RemoteDataSource
import com.swingsync.ai.data.datasource.remote.RemoteDataSourceImpl
import com.swingsync.ai.data.repository.SwingRepositoryImpl
import com.swingsync.ai.data.repository.UserRepositoryImpl
import com.swingsync.ai.data.repository.SocialRepositoryImpl
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.repository.UserRepository
import com.swingsync.ai.domain.repository.SocialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindLocalDataSource(
        localDataSourceImpl: LocalDataSourceImpl
    ): LocalDataSource
    
    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        remoteDataSourceImpl: RemoteDataSourceImpl
    ): RemoteDataSource
    
    @Binds
    @Singleton
    abstract fun bindSwingRepository(
        swingRepositoryImpl: SwingRepositoryImpl
    ): SwingRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindSocialRepository(
        socialRepositoryImpl: SocialRepositoryImpl
    ): SocialRepository
}