package com.swingsync.ai.di

import com.swingsync.ai.domain.usecase.analysis.GetSwingAnalysisUseCase
import com.swingsync.ai.domain.usecase.analysis.GetSwingFeedbackUseCase
import com.swingsync.ai.domain.usecase.swing.*
import com.swingsync.ai.domain.usecase.user.GetUserSettingsUseCase
import com.swingsync.ai.domain.usecase.user.UpdateUserSettingsUseCase
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Hilt module for use case dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    // Swing-related use cases
    @Provides
    @Singleton
    fun provideCreateSwingSessionUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CreateSwingSessionUseCase {
        return CreateSwingSessionUseCase(swingRepository, dispatcher)
    }
    
    @Provides
    @Singleton
    fun provideAnalyzeSwingUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): AnalyzeSwingUseCase {
        return AnalyzeSwingUseCase(swingRepository, dispatcher)
    }
    
    @Provides
    @Singleton
    fun provideGetSwingSessionsUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetSwingSessionsUseCase {
        return GetSwingSessionsUseCase(swingRepository, dispatcher)
    }
    
    @Provides
    @Singleton
    fun provideSavePoseDetectionUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): SavePoseDetectionUseCase {
        return SavePoseDetectionUseCase(swingRepository, dispatcher)
    }
    
    @Provides
    @Singleton
    fun provideUpdateSwingSessionUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): UpdateSwingSessionUseCase {
        return UpdateSwingSessionUseCase(swingRepository, dispatcher)
    }
    
    // Analysis-related use cases
    @Provides
    @Singleton
    fun provideGetSwingAnalysisUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetSwingAnalysisUseCase {
        return GetSwingAnalysisUseCase(swingRepository, dispatcher)
    }
    
    @Provides
    @Singleton
    fun provideGetSwingFeedbackUseCase(
        swingRepository: SwingRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetSwingFeedbackUseCase {
        return GetSwingFeedbackUseCase(swingRepository, dispatcher)
    }
    
    // User-related use cases
    @Provides
    @Singleton
    fun provideGetUserSettingsUseCase(
        userRepository: UserRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetUserSettingsUseCase {
        return GetUserSettingsUseCase(userRepository, dispatcher)
    }
    
    @Provides
    @Singleton
    fun provideUpdateUserSettingsUseCase(
        userRepository: UserRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): UpdateUserSettingsUseCase {
        return UpdateUserSettingsUseCase(userRepository, dispatcher)
    }
}