package com.golfswing.vro.pixel.security

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.golfswing.vro.R
import java.security.GeneralSecurityException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityExceptionHandler @Inject constructor(
    private val context: Context,
    private val securityLogger: SecurityLogger
) {
    
    companion object {
        private const val TAG = "SecurityExceptionHandler"
    }
    
    /**
     * Handle security exceptions with appropriate logging and user messaging
     */
    fun handleSecurityException(
        exception: Exception,
        context: String,
        fallbackAction: (() -> Unit)? = null
    ): SecurityError {
        
        val securityError = when (exception) {
            is SecurityException -> handleSecurityException(exception, context)
            is GeneralSecurityException -> handleCryptoException(exception, context)
            is BadPaddingException -> handleCryptoException(exception, context)
            is IllegalBlockSizeException -> handleCryptoException(exception, context)
            is IllegalArgumentException -> handleValidationException(exception, context)
            is IllegalStateException -> handleStateException(exception, context)
            else -> handleGenericException(exception, context)
        }
        
        // Log the security error
        logSecurityError(securityError, exception)
        
        // Execute fallback action if provided
        fallbackAction?.invoke()
        
        return securityError
    }
    
    /**
     * Handle SecurityException
     */
    private fun handleSecurityException(
        exception: SecurityException,
        context: String
    ): SecurityError {
        return when {
            exception.message?.contains("encryption", ignoreCase = true) == true -> {
                SecurityError(
                    type = SecurityError.Type.ENCRYPTION_ERROR,
                    message = getString(R.string.error_encryption_failed),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.HIGH,
                    userAction = getString(R.string.action_try_again),
                    canRetry = true
                )
            }
            exception.message?.contains("authentication", ignoreCase = true) == true -> {
                SecurityError(
                    type = SecurityError.Type.AUTHENTICATION_ERROR,
                    message = getString(R.string.error_authentication_failed),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.HIGH,
                    userAction = getString(R.string.action_authenticate_again),
                    canRetry = true
                )
            }
            exception.message?.contains("permission", ignoreCase = true) == true -> {
                SecurityError(
                    type = SecurityError.Type.PERMISSION_DENIED,
                    message = getString(R.string.error_permission_denied),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.MEDIUM,
                    userAction = getString(R.string.action_check_permissions),
                    canRetry = false
                )
            }
            else -> {
                SecurityError(
                    type = SecurityError.Type.SECURITY_VIOLATION,
                    message = getString(R.string.error_security_violation),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.HIGH,
                    userAction = getString(R.string.action_contact_support),
                    canRetry = false
                )
            }
        }
    }
    
    /**
     * Handle cryptographic exceptions
     */
    private fun handleCryptoException(
        exception: GeneralSecurityException,
        context: String
    ): SecurityError {
        return when (exception) {
            is BadPaddingException -> {
                SecurityError(
                    type = SecurityError.Type.DECRYPTION_ERROR,
                    message = getString(R.string.error_data_corrupted),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.HIGH,
                    userAction = getString(R.string.action_reset_data),
                    canRetry = false
                )
            }
            is IllegalBlockSizeException -> {
                SecurityError(
                    type = SecurityError.Type.ENCRYPTION_ERROR,
                    message = getString(R.string.error_encryption_failed),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.HIGH,
                    userAction = getString(R.string.action_try_again),
                    canRetry = true
                )
            }
            else -> {
                SecurityError(
                    type = SecurityError.Type.CRYPTO_ERROR,
                    message = getString(R.string.error_crypto_operation_failed),
                    technicalMessage = exception.message,
                    context = context,
                    severity = SecurityError.Severity.HIGH,
                    userAction = getString(R.string.action_restart_app),
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * Handle validation exceptions
     */
    private fun handleValidationException(
        exception: IllegalArgumentException,
        context: String
    ): SecurityError {
        return SecurityError(
            type = SecurityError.Type.VALIDATION_ERROR,
            message = getString(R.string.error_invalid_input),
            technicalMessage = exception.message,
            context = context,
            severity = SecurityError.Severity.MEDIUM,
            userAction = getString(R.string.action_check_input),
            canRetry = true
        )
    }
    
    /**
     * Handle state exceptions
     */
    private fun handleStateException(
        exception: IllegalStateException,
        context: String
    ): SecurityError {
        return SecurityError(
            type = SecurityError.Type.STATE_ERROR,
            message = getString(R.string.error_invalid_state),
            technicalMessage = exception.message,
            context = context,
            severity = SecurityError.Severity.MEDIUM,
            userAction = getString(R.string.action_restart_operation),
            canRetry = true
        )
    }
    
    /**
     * Handle generic exceptions
     */
    private fun handleGenericException(
        exception: Exception,
        context: String
    ): SecurityError {
        return SecurityError(
            type = SecurityError.Type.UNKNOWN_ERROR,
            message = getString(R.string.error_unexpected),
            technicalMessage = exception.message,
            context = context,
            severity = SecurityError.Severity.MEDIUM,
            userAction = getString(R.string.action_try_again),
            canRetry = true
        )
    }
    
    /**
     * Log security error
     */
    private fun logSecurityError(securityError: SecurityError, exception: Exception) {
        val details = mapOf(
            "error_type" to securityError.type.name,
            "severity" to securityError.severity.name,
            "context" to securityError.context,
            "can_retry" to securityError.canRetry.toString(),
            "user_message" to securityError.message,
            "technical_message" to (securityError.technicalMessage ?: "")
        )
        
        when (securityError.severity) {
            SecurityError.Severity.CRITICAL -> {
                securityLogger.logCriticalSecurityEvent(
                    "Security Error: ${securityError.type.name}",
                    securityError.context,
                    exception
                )
            }
            SecurityError.Severity.HIGH -> {
                securityLogger.logSecurityEvent(
                    "SECURITY_ERROR",
                    "High severity security error",
                    details,
                    exception
                )
            }
            SecurityError.Severity.MEDIUM -> {
                securityLogger.logSecurityEvent(
                    "SECURITY_WARNING",
                    "Medium severity security error",
                    details,
                    exception
                )
            }
            SecurityError.Severity.LOW -> {
                securityLogger.logSecurityEvent(
                    "SECURITY_INFO",
                    "Low severity security error",
                    details,
                    exception
                )
            }
        }
    }
    
    /**
     * Get string resource safely
     */
    private fun getString(@StringRes resId: Int): String {
        return try {
            context.getString(resId)
        } catch (e: Exception) {
            "Error message not available"
        }
    }
    
    /**
     * Create security error for authentication failure
     */
    fun createAuthenticationError(reason: String): SecurityError {
        return SecurityError(
            type = SecurityError.Type.AUTHENTICATION_ERROR,
            message = getString(R.string.error_authentication_failed),
            technicalMessage = reason,
            context = "Authentication",
            severity = SecurityError.Severity.HIGH,
            userAction = getString(R.string.action_authenticate_again),
            canRetry = true
        )
    }
    
    /**
     * Create security error for permission denial
     */
    fun createPermissionDeniedError(permission: String): SecurityError {
        return SecurityError(
            type = SecurityError.Type.PERMISSION_DENIED,
            message = getString(R.string.error_permission_denied),
            technicalMessage = "Permission denied: $permission",
            context = "Permission Check",
            severity = SecurityError.Severity.MEDIUM,
            userAction = getString(R.string.action_check_permissions),
            canRetry = false
        )
    }
    
    /**
     * Create security error for session expiry
     */
    fun createSessionExpiredError(): SecurityError {
        return SecurityError(
            type = SecurityError.Type.SESSION_EXPIRED,
            message = getString(R.string.error_session_expired),
            technicalMessage = "Session has expired",
            context = "Session Management",
            severity = SecurityError.Severity.MEDIUM,
            userAction = getString(R.string.action_authenticate_again),
            canRetry = true
        )
    }
    
    /**
     * Create security error for data corruption
     */
    fun createDataCorruptionError(): SecurityError {
        return SecurityError(
            type = SecurityError.Type.DATA_CORRUPTION,
            message = getString(R.string.error_data_corrupted),
            technicalMessage = "Data integrity check failed",
            context = "Data Validation",
            severity = SecurityError.Severity.HIGH,
            userAction = getString(R.string.action_reset_data),
            canRetry = false
        )
    }
}

/**
 * Security error data class
 */
data class SecurityError(
    val type: Type,
    val message: String,
    val technicalMessage: String?,
    val context: String,
    val severity: Severity,
    val userAction: String,
    val canRetry: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Type {
        AUTHENTICATION_ERROR,
        PERMISSION_DENIED,
        ENCRYPTION_ERROR,
        DECRYPTION_ERROR,
        CRYPTO_ERROR,
        VALIDATION_ERROR,
        STATE_ERROR,
        SESSION_EXPIRED,
        DATA_CORRUPTION,
        SECURITY_VIOLATION,
        UNKNOWN_ERROR
    }
    
    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}