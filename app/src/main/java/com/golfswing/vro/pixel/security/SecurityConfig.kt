package com.golfswing.vro.pixel.security

import android.content.Context
import android.os.Build
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfig @Inject constructor(
    private val context: Context,
    private val securePreferences: SecurePreferencesManager,
    private val securityLogger: SecurityLogger
) {
    
    companion object {
        private const val TAG = "SecurityConfig"
        
        // Security feature flags
        private const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val KEY_MEMORY_PROTECTION = "memory_protection"
        private const val KEY_FILE_ENCRYPTION = "file_encryption"
        private const val KEY_DATABASE_ENCRYPTION = "database_encryption"
        private const val KEY_SECURE_COMMUNICATIONS = "secure_communications"
        private const val KEY_INTEGRITY_CHECKING = "integrity_checking"
        private const val KEY_ANTI_TAMPERING = "anti_tampering"
        
        // Security levels
        private const val KEY_SECURITY_LEVEL = "security_level"
        
        // Default values
        private const val DEFAULT_ENCRYPTION_ENABLED = true
        private const val DEFAULT_LOGGING_ENABLED = true
        private const val DEFAULT_MEMORY_PROTECTION = true
        private const val DEFAULT_FILE_ENCRYPTION = true
        private const val DEFAULT_DATABASE_ENCRYPTION = true
        private const val DEFAULT_SECURE_COMMUNICATIONS = true
        private const val DEFAULT_INTEGRITY_CHECKING = true
        private const val DEFAULT_ANTI_TAMPERING = true
    }
    
    enum class SecurityLevel {
        BASIC,
        ENHANCED,
        MAXIMUM
    }
    
    data class SecurityConfiguration(
        val encryptionEnabled: Boolean,
        val biometricEnabled: Boolean,
        val pinEnabled: Boolean,
        val loggingEnabled: Boolean,
        val memoryProtection: Boolean,
        val fileEncryption: Boolean,
        val databaseEncryption: Boolean,
        val secureCommunications: Boolean,
        val integrityChecking: Boolean,
        val antiTampering: Boolean,
        val securityLevel: SecurityLevel
    )
    
    init {
        initializeDefaultConfiguration()
    }
    
    /**
     * Initialize default security configuration
     */
    private fun initializeDefaultConfiguration() {
        try {
            // Set default values if not already configured
            if (!securePreferences.containsKey(KEY_ENCRYPTION_ENABLED)) {
                securePreferences.storeSecureBoolean(KEY_ENCRYPTION_ENABLED, DEFAULT_ENCRYPTION_ENABLED)
            }
            
            if (!securePreferences.containsKey(KEY_LOGGING_ENABLED)) {
                securePreferences.storeSecureBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED)
            }
            
            if (!securePreferences.containsKey(KEY_MEMORY_PROTECTION)) {
                securePreferences.storeSecureBoolean(KEY_MEMORY_PROTECTION, DEFAULT_MEMORY_PROTECTION)
            }
            
            if (!securePreferences.containsKey(KEY_FILE_ENCRYPTION)) {
                securePreferences.storeSecureBoolean(KEY_FILE_ENCRYPTION, DEFAULT_FILE_ENCRYPTION)
            }
            
            if (!securePreferences.containsKey(KEY_DATABASE_ENCRYPTION)) {
                securePreferences.storeSecureBoolean(KEY_DATABASE_ENCRYPTION, DEFAULT_DATABASE_ENCRYPTION)
            }
            
            if (!securePreferences.containsKey(KEY_SECURE_COMMUNICATIONS)) {
                securePreferences.storeSecureBoolean(KEY_SECURE_COMMUNICATIONS, DEFAULT_SECURE_COMMUNICATIONS)
            }
            
            if (!securePreferences.containsKey(KEY_INTEGRITY_CHECKING)) {
                securePreferences.storeSecureBoolean(KEY_INTEGRITY_CHECKING, DEFAULT_INTEGRITY_CHECKING)
            }
            
            if (!securePreferences.containsKey(KEY_ANTI_TAMPERING)) {
                securePreferences.storeSecureBoolean(KEY_ANTI_TAMPERING, DEFAULT_ANTI_TAMPERING)
            }
            
            if (!securePreferences.containsKey(KEY_SECURITY_LEVEL)) {
                securePreferences.storeSecureString(KEY_SECURITY_LEVEL, SecurityLevel.ENHANCED.name)
            }
            
            securityLogger.logSecurityEvent(
                SecurityLogger.EVENT_SECURITY_CONFIG_CHANGED,
                "Default security configuration initialized"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default configuration", e)
            securityLogger.logCriticalSecurityEvent(
                "Configuration initialization failed",
                "Failed to initialize default security configuration",
                e
            )
        }
    }
    
    /**
     * Get current security configuration
     */
    fun getSecurityConfiguration(): SecurityConfiguration {
        return SecurityConfiguration(
            encryptionEnabled = securePreferences.getSecureBoolean(KEY_ENCRYPTION_ENABLED, DEFAULT_ENCRYPTION_ENABLED),
            biometricEnabled = securePreferences.getSecureBoolean(KEY_BIOMETRIC_ENABLED, false),
            pinEnabled = securePreferences.getSecureBoolean(KEY_PIN_ENABLED, false),
            loggingEnabled = securePreferences.getSecureBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED),
            memoryProtection = securePreferences.getSecureBoolean(KEY_MEMORY_PROTECTION, DEFAULT_MEMORY_PROTECTION),
            fileEncryption = securePreferences.getSecureBoolean(KEY_FILE_ENCRYPTION, DEFAULT_FILE_ENCRYPTION),
            databaseEncryption = securePreferences.getSecureBoolean(KEY_DATABASE_ENCRYPTION, DEFAULT_DATABASE_ENCRYPTION),
            secureCommunications = securePreferences.getSecureBoolean(KEY_SECURE_COMMUNICATIONS, DEFAULT_SECURE_COMMUNICATIONS),
            integrityChecking = securePreferences.getSecureBoolean(KEY_INTEGRITY_CHECKING, DEFAULT_INTEGRITY_CHECKING),
            antiTampering = securePreferences.getSecureBoolean(KEY_ANTI_TAMPERING, DEFAULT_ANTI_TAMPERING),
            securityLevel = getSecurityLevel()
        )
    }
    
    /**
     * Update security configuration
     */
    fun updateSecurityConfiguration(config: SecurityConfiguration) {
        try {
            securePreferences.storeSecureBoolean(KEY_ENCRYPTION_ENABLED, config.encryptionEnabled)
            securePreferences.storeSecureBoolean(KEY_BIOMETRIC_ENABLED, config.biometricEnabled)
            securePreferences.storeSecureBoolean(KEY_PIN_ENABLED, config.pinEnabled)
            securePreferences.storeSecureBoolean(KEY_LOGGING_ENABLED, config.loggingEnabled)
            securePreferences.storeSecureBoolean(KEY_MEMORY_PROTECTION, config.memoryProtection)
            securePreferences.storeSecureBoolean(KEY_FILE_ENCRYPTION, config.fileEncryption)
            securePreferences.storeSecureBoolean(KEY_DATABASE_ENCRYPTION, config.databaseEncryption)
            securePreferences.storeSecureBoolean(KEY_SECURE_COMMUNICATIONS, config.secureCommunications)
            securePreferences.storeSecureBoolean(KEY_INTEGRITY_CHECKING, config.integrityChecking)
            securePreferences.storeSecureBoolean(KEY_ANTI_TAMPERING, config.antiTampering)
            securePreferences.storeSecureString(KEY_SECURITY_LEVEL, config.securityLevel.name)
            
            securityLogger.logSecurityEvent(
                SecurityLogger.EVENT_SECURITY_CONFIG_CHANGED,
                "Security configuration updated",
                mapOf(
                    "encryption_enabled" to config.encryptionEnabled.toString(),
                    "biometric_enabled" to config.biometricEnabled.toString(),
                    "pin_enabled" to config.pinEnabled.toString(),
                    "security_level" to config.securityLevel.name
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update security configuration", e)
            securityLogger.logCriticalSecurityEvent(
                "Configuration update failed",
                "Failed to update security configuration",
                e
            )
        }
    }
    
    /**
     * Get security level
     */
    fun getSecurityLevel(): SecurityLevel {
        val levelString = securePreferences.getSecureString(KEY_SECURITY_LEVEL, SecurityLevel.ENHANCED.name)
        return try {
            SecurityLevel.valueOf(levelString ?: SecurityLevel.ENHANCED.name)
        } catch (e: IllegalArgumentException) {
            SecurityLevel.ENHANCED
        }
    }
    
    /**
     * Set security level
     */
    fun setSecurityLevel(level: SecurityLevel) {
        try {
            securePreferences.storeSecureString(KEY_SECURITY_LEVEL, level.name)
            
            // Apply security level-specific configurations
            when (level) {
                SecurityLevel.BASIC -> applyBasicSecuritySettings()
                SecurityLevel.ENHANCED -> applyEnhancedSecuritySettings()
                SecurityLevel.MAXIMUM -> applyMaximumSecuritySettings()
            }
            
            securityLogger.logSecurityEvent(
                SecurityLogger.EVENT_SECURITY_CONFIG_CHANGED,
                "Security level changed to ${level.name}"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set security level", e)
            securityLogger.logCriticalSecurityEvent(
                "Security level change failed",
                "Failed to change security level to ${level.name}",
                e
            )
        }
    }
    
    /**
     * Apply basic security settings
     */
    private fun applyBasicSecuritySettings() {
        val config = SecurityConfiguration(
            encryptionEnabled = true,
            biometricEnabled = false,
            pinEnabled = false,
            loggingEnabled = true,
            memoryProtection = false,
            fileEncryption = true,
            databaseEncryption = true,
            secureCommunications = true,
            integrityChecking = false,
            antiTampering = false,
            securityLevel = SecurityLevel.BASIC
        )
        updateSecurityConfiguration(config)
    }
    
    /**
     * Apply enhanced security settings
     */
    private fun applyEnhancedSecuritySettings() {
        val config = SecurityConfiguration(
            encryptionEnabled = true,
            biometricEnabled = true,
            pinEnabled = true,
            loggingEnabled = true,
            memoryProtection = true,
            fileEncryption = true,
            databaseEncryption = true,
            secureCommunications = true,
            integrityChecking = true,
            antiTampering = true,
            securityLevel = SecurityLevel.ENHANCED
        )
        updateSecurityConfiguration(config)
    }
    
    /**
     * Apply maximum security settings
     */
    private fun applyMaximumSecuritySettings() {
        val config = SecurityConfiguration(
            encryptionEnabled = true,
            biometricEnabled = true,
            pinEnabled = true,
            loggingEnabled = true,
            memoryProtection = true,
            fileEncryption = true,
            databaseEncryption = true,
            secureCommunications = true,
            integrityChecking = true,
            antiTampering = true,
            securityLevel = SecurityLevel.MAXIMUM
        )
        updateSecurityConfiguration(config)
    }
    
    /**
     * Check if feature is enabled
     */
    fun isEncryptionEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_ENCRYPTION_ENABLED, DEFAULT_ENCRYPTION_ENABLED)
    }
    
    fun isBiometricEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    fun isPinEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_PIN_ENABLED, false)
    }
    
    fun isLoggingEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED)
    }
    
    fun isMemoryProtectionEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_MEMORY_PROTECTION, DEFAULT_MEMORY_PROTECTION)
    }
    
    fun isFileEncryptionEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_FILE_ENCRYPTION, DEFAULT_FILE_ENCRYPTION)
    }
    
    fun isDatabaseEncryptionEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_DATABASE_ENCRYPTION, DEFAULT_DATABASE_ENCRYPTION)
    }
    
    fun isSecureCommunicationsEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_SECURE_COMMUNICATIONS, DEFAULT_SECURE_COMMUNICATIONS)
    }
    
    fun isIntegrityCheckingEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_INTEGRITY_CHECKING, DEFAULT_INTEGRITY_CHECKING)
    }
    
    fun isAntiTamperingEnabled(): Boolean {
        return securePreferences.getSecureBoolean(KEY_ANTI_TAMPERING, DEFAULT_ANTI_TAMPERING)
    }
    
    /**
     * Enable/disable specific security features
     */
    fun setEncryptionEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_ENCRYPTION_ENABLED, enabled)
        logFeatureChange("encryption", enabled)
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_BIOMETRIC_ENABLED, enabled)
        logFeatureChange("biometric", enabled)
    }
    
    fun setPinEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_PIN_ENABLED, enabled)
        logFeatureChange("pin", enabled)
    }
    
    fun setLoggingEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_LOGGING_ENABLED, enabled)
        logFeatureChange("logging", enabled)
    }
    
    fun setMemoryProtectionEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_MEMORY_PROTECTION, enabled)
        logFeatureChange("memory_protection", enabled)
    }
    
    fun setFileEncryptionEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_FILE_ENCRYPTION, enabled)
        logFeatureChange("file_encryption", enabled)
    }
    
    fun setDatabaseEncryptionEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_DATABASE_ENCRYPTION, enabled)
        logFeatureChange("database_encryption", enabled)
    }
    
    fun setSecureCommunicationsEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_SECURE_COMMUNICATIONS, enabled)
        logFeatureChange("secure_communications", enabled)
    }
    
    fun setIntegrityCheckingEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_INTEGRITY_CHECKING, enabled)
        logFeatureChange("integrity_checking", enabled)
    }
    
    fun setAntiTamperingEnabled(enabled: Boolean) {
        securePreferences.storeSecureBoolean(KEY_ANTI_TAMPERING, enabled)
        logFeatureChange("anti_tampering", enabled)
    }
    
    /**
     * Log feature change
     */
    private fun logFeatureChange(feature: String, enabled: Boolean) {
        securityLogger.logSecurityEvent(
            SecurityLogger.EVENT_SECURITY_CONFIG_CHANGED,
            "Security feature ${if (enabled) "enabled" else "disabled"}: $feature"
        )
    }
    
    /**
     * Validate security configuration
     */
    fun validateConfiguration(): Boolean {
        return try {
            val config = getSecurityConfiguration()
            
            // Basic validation rules
            if (config.securityLevel == SecurityLevel.MAXIMUM) {
                if (!config.encryptionEnabled || !config.databaseEncryption || !config.fileEncryption) {
                    return false
                }
            }
            
            if (config.securityLevel == SecurityLevel.ENHANCED) {
                if (!config.encryptionEnabled || !config.databaseEncryption) {
                    return false
                }
            }
            
            // Check if critical features are properly configured
            if (config.encryptionEnabled && !securePreferences.isKeystoreHealthy()) {
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Configuration validation failed", e)
            false
        }
    }
    
    /**
     * Get device security capabilities
     */
    fun getDeviceSecurityCapabilities(): DeviceSecurityCapabilities {
        return DeviceSecurityCapabilities(
            hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            hasTee = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            hasBiometric = context.packageManager.hasSystemFeature("android.hardware.fingerprint"),
            hasSecureScreen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            hasHardwareKeystore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            supportsFileBasedEncryption = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            supportsNetworkSecurityConfig = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        )
    }
    
    data class DeviceSecurityCapabilities(
        val hasStrongBox: Boolean,
        val hasTee: Boolean,
        val hasBiometric: Boolean,
        val hasSecureScreen: Boolean,
        val hasHardwareKeystore: Boolean,
        val supportsFileBasedEncryption: Boolean,
        val supportsNetworkSecurityConfig: Boolean
    )
    
    /**
     * Reset security configuration to defaults
     */
    fun resetToDefaults() {
        try {
            securePreferences.clearAllEncryptedData()
            initializeDefaultConfiguration()
            
            securityLogger.logSecurityEvent(
                SecurityLogger.EVENT_SECURITY_CONFIG_CHANGED,
                "Security configuration reset to defaults"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset configuration", e)
            securityLogger.logCriticalSecurityEvent(
                "Configuration reset failed",
                "Failed to reset security configuration to defaults",
                e
            )
        }
    }
}