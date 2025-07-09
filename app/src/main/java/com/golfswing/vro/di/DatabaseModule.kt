package com.golfswing.vro.di

import android.content.Context
import androidx.room.Room
import com.golfswing.vro.data.database.GolfSwingDatabase
import com.golfswing.vro.data.database.SwingAnalysisDao
import com.golfswing.vro.data.database.UserProfileDao
import com.golfswing.vro.data.database.SwingSessionDao
import com.golfswing.vro.data.database.PracticeGoalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideGolfSwingDatabase(@ApplicationContext context: Context): GolfSwingDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            GolfSwingDatabase::class.java,
            "golf_swing_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideSwingAnalysisDao(database: GolfSwingDatabase): SwingAnalysisDao {
        return database.swingAnalysisDao()
    }
    
    @Provides
    fun provideUserProfileDao(database: GolfSwingDatabase): UserProfileDao {
        return database.userProfileDao()
    }
    
    @Provides
    fun provideSwingSessionDao(database: GolfSwingDatabase): SwingSessionDao {
        return database.swingSessionDao()
    }
    
    @Provides
    fun providePracticeGoalDao(database: GolfSwingDatabase): PracticeGoalDao {
        return database.practiceGoalDao()
    }
}