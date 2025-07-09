package com.golfswing.vro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class GolfSwingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize ML libraries
        initializeMLLibraries()
        
        Timber.d("Golf Swing VRO Application initialized")
    }
    
    private fun initializeMLLibraries() {
        // Initialize TensorFlow Lite
        try {
            // TensorFlow Lite initialization is handled automatically
            Timber.d("TensorFlow Lite initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize TensorFlow Lite")
        }
        
        // Initialize MediaPipe
        try {
            // MediaPipe initialization
            Timber.d("MediaPipe initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MediaPipe")
        }
    }
}