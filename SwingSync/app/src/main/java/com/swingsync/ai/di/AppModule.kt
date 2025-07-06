package com.swingsync.ai.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.swingsync.ai.data.local.SwingSyncDatabase
import com.swingsync.ai.data.local.dao.SwingAnalysisDao
import com.swingsync.ai.data.local.dao.UserProfileDao
import com.swingsync.ai.data.remote.ApiService
import com.swingsync.ai.data.remote.WebSocketService
import com.swingsync.ai.data.repository.SwingAnalysisRepositoryImpl
import com.swingsync.ai.data.repository.UserProfileRepositoryImpl
import com.swingsync.ai.domain.repository.SwingAnalysisRepository
import com.swingsync.ai.domain.repository.UserProfileRepository
import com.swingsync.ai.utils.camera.CameraManager
import com.swingsync.ai.utils.mediapipe.PoseEstimationManager
import com.swingsync.ai.utils.network.NetworkManager
import com.swingsync.ai.utils.voice.VoiceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.swingsync.ai/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSocketService(okHttpClient: OkHttpClient): WebSocketService {
        return WebSocketService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideSwingSyncDatabase(@ApplicationContext context: Context): SwingSyncDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SwingSyncDatabase::class.java,
            "swing_sync_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSwingAnalysisDao(database: SwingSyncDatabase): SwingAnalysisDao {
        return database.swingAnalysisDao()
    }

    @Provides
    fun provideUserProfileDao(database: SwingSyncDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun provideSwingAnalysisRepository(
        apiService: ApiService,
        swingAnalysisDao: SwingAnalysisDao,
        webSocketService: WebSocketService
    ): SwingAnalysisRepository {
        return SwingAnalysisRepositoryImpl(apiService, swingAnalysisDao, webSocketService)
    }

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        apiService: ApiService,
        userProfileDao: UserProfileDao
    ): UserProfileRepository {
        return UserProfileRepositoryImpl(apiService, userProfileDao)
    }

    @Provides
    @Singleton
    fun provideCameraManager(@ApplicationContext context: Context): CameraManager {
        return CameraManager(context)
    }

    @Provides
    @Singleton
    fun providePoseEstimationManager(@ApplicationContext context: Context): PoseEstimationManager {
        return PoseEstimationManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceManager(@ApplicationContext context: Context): VoiceManager {
        return VoiceManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager {
        return NetworkManager(context)
    }
}