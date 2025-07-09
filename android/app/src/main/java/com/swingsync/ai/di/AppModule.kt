package com.swingsync.ai.di

import android.content.Context
import com.swingsync.ai.data.repository.*
import com.swingsync.ai.voice.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideVoiceInterface(
        @ApplicationContext context: Context
    ): VoiceInterface {
        return VoiceInterface(context)
    }
    
    @Provides
    @Singleton
    fun provideWakeWordDetector(
        @ApplicationContext context: Context
    ): WakeWordDetector {
        return WakeWordDetector(context)
    }
    
    @Provides
    @Singleton
    fun provideVoiceCommandProcessor(
        @ApplicationContext context: Context
    ): VoiceCommandProcessor {
        return VoiceCommandProcessor(context)
    }
    
    @Provides
    @Singleton
    fun provideSpatialAudioGuide(
        @ApplicationContext context: Context
    ): SpatialAudioGuide {
        return SpatialAudioGuide(context)
    }
    
    @Provides
    @Singleton
    fun provideMagicVoiceCoach(
        @ApplicationContext context: Context,
        voiceInterface: VoiceInterface
    ): MagicVoiceCoach {
        return MagicVoiceCoach(context, voiceInterface)
    }
    
    @Provides
    @Singleton
    fun providePowerOptimizationManager(
        @ApplicationContext context: Context
    ): PowerOptimizationManager {
        return PowerOptimizationManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAnalysisRepository(): AnalysisRepository {
        return AnalysisRepository()
    }
    
    @Provides
    @Singleton
    fun provideCoachingRepository(): CoachingRepository {
        return CoachingRepository()
    }
    
    @Provides
    @Singleton
    fun provideProgressRepository(): ProgressRepository {
        return ProgressRepository()
    }
    
    @Provides
    @Singleton
    fun provideHistoryRepository(): HistoryRepository {
        return HistoryRepository()
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(): SettingsRepository {
        return SettingsRepository()
    }
}