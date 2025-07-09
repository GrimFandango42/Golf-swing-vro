package com.golfswing.vro.pixel.security

import android.content.Context
import androidx.room.Room
import com.golfswing.vro.pixel.database.GolfSwingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurePreferencesManager(
        @ApplicationContext context: Context
    ): SecurePreferencesManager {
        return SecurePreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabaseEncryptionManager(
        @ApplicationContext context: Context,
        securePreferencesManager: SecurePreferencesManager
    ): DatabaseEncryptionManager {
        return DatabaseEncryptionManager(context, securePreferencesManager)
    }

    @Provides
    @Singleton
    fun providePrivacyUtils(
        @ApplicationContext context: Context
    ): PrivacyUtils {
        return PrivacyUtils(context)
    }

    @Provides
    @Singleton
    fun provideEncryptedDatabase(
        @ApplicationContext context: Context,
        encryptionManager: DatabaseEncryptionManager
    ): GolfSwingDatabase {
        return Room.databaseBuilder(
            context,
            GolfSwingDatabase::class.java,
            "golf_swing_database"
        )
        .openHelperFactory(encryptionManager.createEncryptedDatabaseFactory())
        .fallbackToDestructiveMigration()
        .build()
    }
}