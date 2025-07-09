package com.golfswing.vro.pixel.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfig @Inject constructor(
    private val context: Context,
    private val privacyUtils: PrivacyUtils,
    private val securePreferencesManager: SecurePreferencesManager,
    private val encryptionManager: DatabaseEncryptionManager
) {

    companion object {
        private const val TAG = "SecurityConfig"
        private const val FIRST_RUN_KEY = "first_run_completed"
        private const val SECURITY_INIT_KEY = "security_initialized"
    }

    /**
     * Initialize security configuration on first run
     */
    fun initializeSecurity(): Boolean {
        return try {
            Log.i(TAG, "Initializing security configuration...")
            
            // Check if already initialized
            if (securePreferencesManager.getSecureBoolean(SECURITY_INIT_KEY)) {
                Log.i(TAG, "Security already initialized")
                return true
            }
            
            // Verify secure environment
            if (!privacyUtils.isSecureEnvironment()) {
                Log.w(TAG, "App is not running in secure environment")
                // Continue but log warning
            }
            
            // Initialize database encryption
            initializeDatabaseEncryption()
            
            // Verify no network permissions
            verifyNetworkPermissions()
            
            // Setup secure file storage
            setupSecureFileStorage()
            
            // Mark as initialized
            securePreferencesManager.storeSecureBoolean(SECURITY_INIT_KEY, true)
            securePreferencesManager.storeSecureBoolean(FIRST_RUN_KEY, true)
            
            Log.i(TAG, "Security configuration completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize security", e)
            false
        }
    }

    /**
     * Initialize database encryption
     */
    private fun initializeDatabaseEncryption() {
        try {
            val key = encryptionManager.getDatabaseKey()
            if (key.isEmpty()) {
                throw SecurityException("Failed to generate database encryption key")
            }
            Log.i(TAG, "Database encryption initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize database encryption", e)
            throw e
        }
    }

    /**
     * Verify network permissions are removed
     */
    private fun verifyNetworkPermissions() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            
            val permissions = packageInfo.requestedPermissions ?: arrayOf()
            val networkPermissions = arrayOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE"
            )
            
            networkPermissions.forEach { permission ->
                if (permissions.contains(permission)) {
                    Log.w(TAG, "Network permission still present: $permission")
                }
            }
            
            Log.i(TAG, "Network permissions verification completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify network permissions", e)
        }
    }

    /**
     * Setup secure file storage directories
     */
    private fun setupSecureFileStorage() {
        try {
            // Create secure directories
            val secureDirectories = arrayOf(
                "recordings",
                "secure_prefs",
                "temp",
                "cache",
                "user_data"
            )
            
            secureDirectories.forEach { dirName ->
                val dir = java.io.File(context.filesDir, dirName)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                // Set secure permissions
                privacyUtils.sanitizeFileMetadata(dir)
            }
            
            Log.i(TAG, "Secure file storage setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup secure file storage", e)
            throw e
        }
    }

    /**
     * Validate current security configuration
     */
    fun validateSecurityConfig(): SecurityValidationResult {
        val issues = mutableListOf<String>()
        
        try {
            // Check environment security
            if (!privacyUtils.isSecureEnvironment()) {
                issues.add("App is not running in secure environment")
            }
            
            // Check database encryption
            if (!encryptionManager.getDatabaseKey().isNotEmpty()) {
                issues.add("Database encryption key not found")
            }
            
            // Check preferences integrity
            if (!securePreferencesManager.validateIntegrity()) {
                issues.add("Secure preferences integrity check failed")
            }
            
            // Check file permissions
            val recordingsDir = java.io.File(context.filesDir, "recordings")
            if (recordingsDir.exists() && recordingsDir.canRead() && recordingsDir.canWrite()) {
                // Check if readable by others
                if (recordingsDir.setReadable(false, false)) {
                    recordingsDir.setReadable(true, true)
                } else {
                    issues.add("Unable to set secure file permissions")
                }
            }
            
            return SecurityValidationResult(
                isSecure = issues.isEmpty(),
                issues = issues
            )
        } catch (e: Exception) {
            issues.add("Security validation failed: ${e.message}")
            return SecurityValidationResult(
                isSecure = false,
                issues = issues
            )
        }
    }

    /**
     * Perform security cleanup
     */
    fun performSecurityCleanup() {
        try {
            Log.i(TAG, "Starting security cleanup...")
            
            // Clear temporary files
            val tempDir = java.io.File(context.cacheDir, "temp")
            if (tempDir.exists()) {
                privacyUtils.secureWipeDirectory(tempDir)
            }
            
            // Clear sensitive cache
            val sensitiveCache = java.io.File(context.cacheDir, "sensitive")
            if (sensitiveCache.exists()) {
                privacyUtils.secureWipeDirectory(sensitiveCache)
            }
            
            // Cleanup old recordings (keep only last 30 days)
            cleanupOldRecordings()
            
            Log.i(TAG, "Security cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform security cleanup", e)
        }
    }

    /**
     * Clean up old recordings for privacy
     */
    private fun cleanupOldRecordings() {
        try {
            val recordingsDir = java.io.File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) return
            
            val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days
            
            recordingsDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    privacyUtils.secureWipeFile(file)
                    Log.i(TAG, "Cleaned up old recording: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old recordings", e)
        }
    }

    /**
     * Get security status
     */
    fun getSecurityStatus(): SecurityStatus {
        val validation = validateSecurityConfig()
        return SecurityStatus(
            isInitialized = securePreferencesManager.getSecureBoolean(SECURITY_INIT_KEY),
            isSecure = validation.isSecure,
            databaseEncrypted = encryptionManager.getDatabaseKey().isNotEmpty(),
            secureEnvironment = privacyUtils.isSecureEnvironment(),
            issues = validation.issues
        )
    }

    /**
     * Reset security configuration
     */
    fun resetSecurityConfig() {
        try {
            Log.i(TAG, "Resetting security configuration...")
            
            // Clear all secure preferences
            securePreferencesManager.clearAllEncryptedData()
            
            // Clear database encryption key
            encryptionManager.clearDatabaseKey()
            
            // Re-initialize
            initializeSecurity()
            
            Log.i(TAG, "Security configuration reset completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset security configuration", e)
            throw e
        }
    }

    data class SecurityValidationResult(
        val isSecure: Boolean,
        val issues: List<String>
    )

    data class SecurityStatus(
        val isInitialized: Boolean,
        val isSecure: Boolean,
        val databaseEncrypted: Boolean,
        val secureEnvironment: Boolean,
        val issues: List<String>
    )
}