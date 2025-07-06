package com.swingsync.ai.di

import android.content.Context
import com.swingsync.ai.data.repository.*
import com.swingsync.ai.voice.VoiceInterface
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