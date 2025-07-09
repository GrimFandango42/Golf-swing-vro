package com.swingsync.ai.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.swingsync.ai.data.model.SwingAnalysisFeedback
import com.swingsync.ai.data.model.SwingVideoAnalysisInput
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * API client for SwingSync AI backend communication
 * Handles analysis requests and responses
 */
class ApiClient {

    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = "http://localhost:8000/" // TODO: Replace with actual backend URL
        private const val TIMEOUT_SECONDS = 30L
    }

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val apiService: SwingSyncApiService by lazy {
        retrofit.create(SwingSyncApiService::class.java)
    }

    /**
     * Submit swing analysis request to backend
     */
    suspend fun analyzeSwing(analysisInput: SwingVideoAnalysisInput): Result<SwingAnalysisFeedback> {
        return try {
            Log.d(TAG, "Submitting swing analysis for session: ${analysisInput.sessionId}")
            
            val response = apiService.analyzeSwing(analysisInput)
            
            if (response.isSuccessful) {
                val feedback = response.body()
                if (feedback != null) {
                    Log.d(TAG, "Analysis completed successfully")
                    Result.success(feedback)
                } else {
                    Log.e(TAG, "Analysis response body is null")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorMsg = "Analysis failed: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Network error during analysis", e)
            Result.failure(e)
        }
    }

    /**
     * Test connection to backend
     */
    suspend fun testConnection(): Result<String> {
        return try {
            Log.d(TAG, "Testing connection to backend")
            
            val response = apiService.healthCheck()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Backend connection successful")
                Result.success("Connection successful")
            } else {
                val errorMsg = "Health check failed: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }

    /**
     * Update base URL for backend
     */
    fun updateBaseUrl(newBaseUrl: String) {
        // Note: This requires recreating the retrofit instance
        // For simplicity, we'll log this for now
        Log.d(TAG, "Base URL update requested: $newBaseUrl")
        // TODO: Implement dynamic URL updates if needed
    }
}

/**
 * Retrofit API service interface
 */
interface SwingSyncApiService {
    
    @POST("analyze_swing/")
    suspend fun analyzeSwing(@Body analysisInput: SwingVideoAnalysisInput): Response<SwingAnalysisFeedback>
    
    @POST("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}

/**
 * Network configuration
 */
object NetworkConfig {
    const val DEFAULT_BASE_URL = "http://localhost:8000/"
    const val PRODUCTION_BASE_URL = "https://api.swingsync.ai/"
    const val STAGING_BASE_URL = "https://staging-api.swingsync.ai/"
    
    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    
    // Retry configuration
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_MS = 1000L
}