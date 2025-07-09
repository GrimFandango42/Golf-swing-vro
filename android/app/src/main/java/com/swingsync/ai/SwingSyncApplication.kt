package com.swingsync.ai

import android.app.Application
import android.util.Log
import com.swingsync.ai.data.repository.SwingAnalysisRepository
import com.swingsync.ai.mediapipe.MediaPipeManager
import com.swingsync.ai.network.ApiClient

/**
 * Application class for SwingSync AI
 * Handles global initialization and dependency injection
 */
class SwingSyncApplication : Application() {
    
    companion object {
        private const val TAG = "SwingSyncApplication"
        lateinit var instance: SwingSyncApplication
            private set
    }
    
    // Global dependencies
    lateinit var mediaPipeManager: MediaPipeManager
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var swingAnalysisRepository: SwingAnalysisRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "Initializing SwingSync AI Application")
        
        // Initialize core components
        initializeComponents()
        
        Log.d(TAG, "SwingSync AI Application initialized successfully")
    }
    
    private fun initializeComponents() {
        try {
            // Initialize MediaPipe for pose detection
            mediaPipeManager = MediaPipeManager(this)
            Log.d(TAG, "MediaPipe Manager initialized")
            
            // Initialize API client for backend communication
            apiClient = ApiClient()
            Log.d(TAG, "API Client initialized")
            
            // Initialize repository
            swingAnalysisRepository = SwingAnalysisRepository(apiClient)
            Log.d(TAG, "Swing Analysis Repository initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
            throw RuntimeException("Failed to initialize SwingSync AI Application", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Terminating SwingSync AI Application")
        
        // Clean up resources
        try {
            mediaPipeManager.release()
            Log.d(TAG, "MediaPipe Manager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPipe Manager", e)
        }
    }
}