package com.swingsync.ai.di

import android.content.Context
import androidx.room.Room
import com.swingsync.ai.data.local.dao.*
import com.swingsync.ai.data.local.database.SwingSyncDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSwingSyncDatabase(
        @ApplicationContext context: Context
    ): SwingSyncDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SwingSyncDatabase::class.java,
            "swing_sync_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideSwingSessionDao(database: SwingSyncDatabase): SwingSessionDao {
        return database.swingSessionDao()
    }
    
    @Provides
    fun provideSwingAnalysisDao(database: SwingSyncDatabase): SwingAnalysisDao {
        return database.swingAnalysisDao()
    }
    
    @Provides
    fun providePoseDetectionDao(database: SwingSyncDatabase): PoseDetectionDao {
        return database.poseDetectionDao()
    }
    
    @Provides
    fun provideUserSettingsDao(database: SwingSyncDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }
    
    @Provides
    fun provideUserProgressDao(database: SwingSyncDatabase): UserProgressDao {
        return database.userProgressDao()
    }
}