package com.golfswing.vro.pixel

import android.app.Application
import android.util.Log
import com.golfswing.vro.pixel.security.SecurityConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GolfSwingApplication : Application() {

    @Inject
    lateinit var securityConfig: SecurityConfig

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "GolfSwingApp"
    }

    override fun onCreate() {
        super.onCreate()
        
        Log.i(TAG, "Golf Swing VRO App starting...")
        
        // Initialize security in background
        applicationScope.launch {
            try {
                Log.i(TAG, "Initializing security configuration...")
                val securityInitialized = securityConfig.initializeSecurity()
                
                if (securityInitialized) {
                    Log.i(TAG, "Security configuration completed successfully")
                    
                    // Perform security validation
                    val securityStatus = securityConfig.getSecurityStatus()
                    Log.i(TAG, "Security Status: ${securityStatus}")
                    
                    if (!securityStatus.isSecure) {
                        Log.w(TAG, "Security issues detected: ${securityStatus.issues}")
                    }
                } else {
                    Log.e(TAG, "Failed to initialize security configuration")
                }
                
                // Perform security cleanup
                securityConfig.performSecurityCleanup()
                
            } catch (e: Exception) {
                Log.e(TAG, "Security initialization failed", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        
        // Perform final security cleanup
        try {
            securityConfig.performSecurityCleanup()
            Log.i(TAG, "Final security cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Final security cleanup failed", e)
        }
    }
}