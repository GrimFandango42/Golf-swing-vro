package com.golfswing.vro.pixel.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager as AndroidXBiometricManager
import androidx.biometric.BiometricPrompt as AndroidXBiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationManager @Inject constructor(
    private val context: Context,
    private val securePreferences: SecurePreferencesManager,
    private val memoryManager: SecureMemoryManager
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "AuthenticationManager"
        private const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes
        private const val MAX_FAILED_ATTEMPTS = 3
        private const val LOCKOUT_DURATION = 5 * 60 * 1000L // 5 minutes
        private const val PIN_LENGTH = 6
        private const val SALT_LENGTH = 32
        private const val HASH_ITERATIONS = 100000
        
        // Preference keys
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIME = "lockout_time"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_LAST_ACTIVITY = "last_activity"
    }
    
    private val activeSessions = ConcurrentHashMap<String, Session>()
    private val sessionTimeoutChecker = Thread(::sessionTimeoutLoop)
    private val isRunning = AtomicBoolean(true)
    private val sessionCounter = AtomicLong(1)
    
    data class Session(
        val id: String,
        val userId: String,
        val createdAt: Long,
        val lastActivity: AtomicLong,
        val permissions: Set<Permission>,
        val isActive: AtomicBoolean = AtomicBoolean(true)
    )
    
    enum class Permission {
        CAMERA_ACCESS,
        MEDIA_ACCESS,
        SETTINGS_ACCESS,
        EXPORT_DATA,
        DELETE_DATA,
        ADMIN_ACCESS
    }
    
    enum class AuthenticationResult {
        SUCCESS,
        FAILED,
        LOCKED_OUT,
        BIOMETRIC_NOT_AVAILABLE,
        BIOMETRIC_NOT_ENROLLED,
        CANCELLED,
        SESSION_EXPIRED
    }
    
    data class AuthenticationResponse(
        val result: AuthenticationResult,
        val sessionId: String? = null,
        val message: String? = null
    )
    
    init {
        sessionTimeoutChecker.isDaemon = true
        sessionTimeoutChecker.start()
        Log.d(TAG, "AuthenticationManager initialized")
    }
    
    /**
     * Set up PIN-based authentication
     */
    fun setupPinAuthentication(pin: String): Boolean {
        require(pin.length == PIN_LENGTH) { "PIN must be exactly $PIN_LENGTH digits" }
        require(pin.all { it.isDigit() }) { "PIN must contain only digits" }
        
        try {
            val salt = memoryManager.generateSecureRandom(SALT_LENGTH)
            val pinHash = hashPin(pin, salt)
            
            securePreferences.storeEncryptedData(KEY_PIN_SALT, salt)
            securePreferences.storeEncryptedData(KEY_PIN_HASH, pinHash)
            
            // Clear failed attempts
            securePreferences.storeSecureInt(KEY_FAILED_ATTEMPTS, 0)
            securePreferences.storeSecureLong(KEY_LOCKOUT_TIME, 0)
            
            // Securely wipe PIN from memory
            val pinArray = pin.toCharArray()
            memoryManager.secureWipeCharArray(pinArray)
            
            Log.d(TAG, "PIN authentication setup successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup PIN authentication", e)
            return false
        }
    }
    
    /**
     * Authenticate with PIN
     */
    fun authenticateWithPin(pin: String): AuthenticationResponse {
        try {
            // Check if locked out
            if (isLockedOut()) {
                val lockoutTime = securePreferences.getSecureLong(KEY_LOCKOUT_TIME)
                val remainingTime = (lockoutTime + LOCKOUT_DURATION - System.currentTimeMillis()) / 1000
                return AuthenticationResponse(
                    result = AuthenticationResult.LOCKED_OUT,
                    message = "Account locked for $remainingTime seconds"
                )
            }
            
            val storedSalt = securePreferences.getEncryptedData(KEY_PIN_SALT)
            val storedHash = securePreferences.getEncryptedData(KEY_PIN_HASH)
            
            if (storedSalt == null || storedHash == null) {
                return AuthenticationResponse(
                    result = AuthenticationResult.FAILED,
                    message = "PIN not set up"
                )
            }
            
            val pinHash = hashPin(pin, storedSalt)
            val isValid = memoryManager.secureEquals(pinHash, storedHash)
            
            // Securely wipe PIN from memory
            val pinArray = pin.toCharArray()
            memoryManager.secureWipeCharArray(pinArray)
            memoryManager.secureWipeArray(pinHash)
            
            if (isValid) {
                // Reset failed attempts
                securePreferences.storeSecureInt(KEY_FAILED_ATTEMPTS, 0)
                securePreferences.storeSecureLong(KEY_LOCKOUT_TIME, 0)
                
                // Create session
                val sessionId = createSession("default_user", getDefaultPermissions())
                
                return AuthenticationResponse(
                    result = AuthenticationResult.SUCCESS,
                    sessionId = sessionId
                )
            } else {
                // Increment failed attempts
                val failedAttempts = securePreferences.getSecureInt(KEY_FAILED_ATTEMPTS) + 1
                securePreferences.storeSecureInt(KEY_FAILED_ATTEMPTS, failedAttempts)
                
                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    securePreferences.storeSecureLong(KEY_LOCKOUT_TIME, System.currentTimeMillis())
                }
                
                return AuthenticationResponse(
                    result = AuthenticationResult.FAILED,
                    message = "Invalid PIN. ${MAX_FAILED_ATTEMPTS - failedAttempts} attempts remaining"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "PIN authentication failed", e)
            return AuthenticationResponse(
                result = AuthenticationResult.FAILED,
                message = "Authentication error"
            )
        }
    }
    
    /**
     * Enable biometric authentication
     */
    fun enableBiometricAuthentication(): Boolean {
        val biometricManager = AndroidXBiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(AndroidXBiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            AndroidXBiometricManager.BIOMETRIC_SUCCESS -> {
                securePreferences.storeSecureBoolean(KEY_BIOMETRIC_ENABLED, true)
                Log.d(TAG, "Biometric authentication enabled")
                true
            }
            AndroidXBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "Biometric hardware not available")
                false
            }
            AndroidXBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware unavailable")
                false
            }
            AndroidXBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No biometric enrolled")
                false
            }
            else -> {
                Log.w(TAG, "Biometric authentication not available")
                false
            }
        }
    }
    
    /**
     * Authenticate with biometric
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        callback: (AuthenticationResponse) -> Unit
    ) {
        if (!securePreferences.getSecureBoolean(KEY_BIOMETRIC_ENABLED)) {
            callback(AuthenticationResponse(
                result = AuthenticationResult.BIOMETRIC_NOT_AVAILABLE,
                message = "Biometric authentication not enabled"
            ))
            return
        }
        
        val biometricManager = AndroidXBiometricManager.from(context)
        
        when (biometricManager.canAuthenticate(AndroidXBiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            AndroidXBiometricManager.BIOMETRIC_SUCCESS -> {
                val biometricPrompt = AndroidXBiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(context),
                    object : AndroidXBiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: AndroidXBiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            
                            val sessionId = createSession("default_user", getDefaultPermissions())
                            callback(AuthenticationResponse(
                                result = AuthenticationResult.SUCCESS,
                                sessionId = sessionId
                            ))
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            callback(AuthenticationResponse(
                                result = AuthenticationResult.FAILED,
                                message = "Biometric authentication failed"
                            ))
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            callback(AuthenticationResponse(
                                result = AuthenticationResult.CANCELLED,
                                message = errString.toString()
                            ))
                        }
                    }
                )
                
                val promptInfo = AndroidXBiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Use your biometric credential to access the app")
                    .setNegativeButtonText("Cancel")
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
            }
            AndroidXBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                callback(AuthenticationResponse(
                    result = AuthenticationResult.BIOMETRIC_NOT_ENROLLED,
                    message = "No biometric enrolled"
                ))
            }
            else -> {
                callback(AuthenticationResponse(
                    result = AuthenticationResult.BIOMETRIC_NOT_AVAILABLE,
                    message = "Biometric authentication not available"
                ))
            }
        }
    }
    
    /**
     * Create authentication session
     */
    private fun createSession(userId: String, permissions: Set<Permission>): String {
        val sessionId = "session_${sessionCounter.getAndIncrement()}_${System.currentTimeMillis()}"
        val currentTime = System.currentTimeMillis()
        
        val session = Session(
            id = sessionId,
            userId = userId,
            createdAt = currentTime,
            lastActivity = AtomicLong(currentTime),
            permissions = permissions
        )
        
        activeSessions[sessionId] = session
        
        // Store session info
        securePreferences.storeSecureString(KEY_SESSION_TOKEN, sessionId)
        securePreferences.storeSecureLong(KEY_LAST_ACTIVITY, currentTime)
        
        Log.d(TAG, "Created session: $sessionId")
        return sessionId
    }
    
    /**
     * Validate session
     */
    fun validateSession(sessionId: String): Boolean {
        val session = activeSessions[sessionId] ?: return false
        
        if (!session.isActive.get()) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val lastActivity = session.lastActivity.get()
        
        if (currentTime - lastActivity > SESSION_TIMEOUT) {
            invalidateSession(sessionId)
            return false
        }
        
        // Update last activity
        session.lastActivity.set(currentTime)
        securePreferences.storeSecureLong(KEY_LAST_ACTIVITY, currentTime)
        
        return true
    }
    
    /**
     * Check if user has permission
     */
    fun hasPermission(sessionId: String, permission: Permission): Boolean {
        val session = activeSessions[sessionId] ?: return false
        
        if (!validateSession(sessionId)) {
            return false
        }
        
        return session.permissions.contains(permission)
    }
    
    /**
     * Invalidate session
     */
    fun invalidateSession(sessionId: String) {
        val session = activeSessions.remove(sessionId)
        if (session != null) {
            session.isActive.set(false)
            Log.d(TAG, "Invalidated session: $sessionId")
        }
        
        // Clear session info
        securePreferences.removeEncryptedData(KEY_SESSION_TOKEN)
        securePreferences.removeEncryptedData(KEY_LAST_ACTIVITY)
    }
    
    /**
     * Invalidate all sessions
     */
    fun invalidateAllSessions() {
        activeSessions.values.forEach { session ->
            session.isActive.set(false)
        }
        activeSessions.clear()
        
        securePreferences.removeEncryptedData(KEY_SESSION_TOKEN)
        securePreferences.removeEncryptedData(KEY_LAST_ACTIVITY)
        
        Log.d(TAG, "Invalidated all sessions")
    }
    
    /**
     * Check if currently locked out
     */
    private fun isLockedOut(): Boolean {
        val lockoutTime = securePreferences.getSecureLong(KEY_LOCKOUT_TIME)
        if (lockoutTime == 0L) return false
        
        val currentTime = System.currentTimeMillis()
        return currentTime - lockoutTime < LOCKOUT_DURATION
    }
    
    /**
     * Hash PIN with salt
     */
    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        
        var result = pin.toByteArray(Charsets.UTF_8)
        
        repeat(HASH_ITERATIONS) {
            digest.reset()
            digest.update(salt)
            result = digest.digest(result)
        }
        
        return result
    }
    
    /**
     * Get default permissions
     */
    private fun getDefaultPermissions(): Set<Permission> {
        return setOf(
            Permission.CAMERA_ACCESS,
            Permission.MEDIA_ACCESS,
            Permission.SETTINGS_ACCESS,
            Permission.EXPORT_DATA
        )
    }
    
    /**
     * Session timeout checker loop
     */
    private fun sessionTimeoutLoop() {
        while (isRunning.get()) {
            try {
                Thread.sleep(60_000) // Check every minute
                checkSessionTimeouts()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in session timeout checker", e)
            }
        }
    }
    
    /**
     * Check for expired sessions
     */
    private fun checkSessionTimeouts() {
        val currentTime = System.currentTimeMillis()
        val expiredSessions = mutableListOf<String>()
        
        activeSessions.forEach { (sessionId, session) ->
            if (currentTime - session.lastActivity.get() > SESSION_TIMEOUT) {
                expiredSessions.add(sessionId)
            }
        }
        
        expiredSessions.forEach { sessionId ->
            invalidateSession(sessionId)
        }
        
        if (expiredSessions.isNotEmpty()) {
            Log.d(TAG, "Expired ${expiredSessions.size} sessions")
        }
    }
    
    /**
     * Get active session count
     */
    fun getActiveSessionCount(): Int {
        return activeSessions.size
    }
    
    /**
     * Check if PIN is set up
     */
    fun isPinSetup(): Boolean {
        return securePreferences.containsKey(KEY_PIN_HASH) && 
               securePreferences.containsKey(KEY_PIN_SALT)
    }
    
    /**
     * Check if biometric is enabled
     */
    fun isBiometricEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_BIOMETRIC_ENABLED)
    }
    
    /**
     * Reset authentication (admin function)
     */
    fun resetAuthentication() {
        securePreferences.removeEncryptedData(KEY_PIN_HASH)
        securePreferences.removeEncryptedData(KEY_PIN_SALT)
        securePreferences.storeSecureInt(KEY_FAILED_ATTEMPTS, 0)
        securePreferences.storeSecureLong(KEY_LOCKOUT_TIME, 0)
        securePreferences.storeSecureBoolean(KEY_BIOMETRIC_ENABLED, false)
        
        invalidateAllSessions()
        
        Log.d(TAG, "Authentication reset")
    }
    
    /**
     * Lifecycle observer methods
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Clear sessions when app goes to background
        invalidateAllSessions()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        shutdown()
    }
    
    /**
     * Shutdown authentication manager
     */
    fun shutdown() {
        isRunning.set(false)
        sessionTimeoutChecker.interrupt()
        invalidateAllSessions()
        Log.d(TAG, "AuthenticationManager shutdown")
    }
}