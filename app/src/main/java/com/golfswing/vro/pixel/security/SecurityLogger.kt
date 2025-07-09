package com.golfswing.vro.pixel.security

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityLogger @Inject constructor(
    private val context: Context,
    private val securePreferences: SecurePreferencesManager
) {
    
    companion object {
        private const val TAG = "SecurityLogger"
        private const val LOG_FILE_NAME = "security_log.txt"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
        private const val MAX_LOG_ENTRIES = 1000
        private const val FLUSH_INTERVAL = 30_000L // 30 seconds
        
        // Log levels
        private const val LEVEL_DEBUG = 1
        private const val LEVEL_INFO = 2
        private const val LEVEL_WARN = 3
        private const val LEVEL_ERROR = 4
        private const val LEVEL_CRITICAL = 5
        
        // Security event types
        const val EVENT_AUTHENTICATION_SUCCESS = "AUTH_SUCCESS"
        const val EVENT_AUTHENTICATION_FAILED = "AUTH_FAILED"
        const val EVENT_AUTHENTICATION_LOCKED = "AUTH_LOCKED"
        const val EVENT_SESSION_CREATED = "SESSION_CREATED"
        const val EVENT_SESSION_EXPIRED = "SESSION_EXPIRED"
        const val EVENT_PERMISSION_DENIED = "PERMISSION_DENIED"
        const val EVENT_ENCRYPTION_ERROR = "ENCRYPTION_ERROR"
        const val EVENT_DECRYPTION_ERROR = "DECRYPTION_ERROR"
        const val EVENT_DATABASE_ERROR = "DATABASE_ERROR"
        const val EVENT_FILE_ACCESS_DENIED = "FILE_ACCESS_DENIED"
        const val EVENT_MEMORY_SECURITY_ISSUE = "MEMORY_SECURITY_ISSUE"
        const val EVENT_DEEP_LINK_VALIDATION = "DEEP_LINK_VALIDATION"
        const val EVENT_BIOMETRIC_AUTH = "BIOMETRIC_AUTH"
        const val EVENT_SECURITY_CONFIG_CHANGED = "SECURITY_CONFIG_CHANGED"
        const val EVENT_SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY"
    }
    
    private val logQueue = ConcurrentLinkedQueue<SecurityLogEntry>()
    private val flushThread = Thread(::flushLoop)
    private val isRunning = AtomicBoolean(true)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    data class SecurityLogEntry(
        val timestamp: Long,
        val level: Int,
        val eventType: String,
        val message: String,
        val details: Map<String, String>? = null,
        val exception: Throwable? = null
    )
    
    init {
        flushThread.isDaemon = true
        flushThread.start()
        logSecurityEvent(EVENT_SECURITY_CONFIG_CHANGED, "SecurityLogger initialized")
    }
    
    /**
     * Log security event
     */
    fun logSecurityEvent(
        eventType: String,
        message: String,
        details: Map<String, String>? = null,
        exception: Throwable? = null
    ) {
        val entry = SecurityLogEntry(
            timestamp = System.currentTimeMillis(),
            level = LEVEL_INFO,
            eventType = eventType,
            message = message,
            details = details,
            exception = exception
        )
        
        logQueue.offer(entry)
        
        // Also log to system log for debugging
        if (exception != null) {
            Log.i(TAG, "[$eventType] $message", exception)
        } else {
            Log.i(TAG, "[$eventType] $message")
        }
    }
    
    /**
     * Log authentication success
     */
    fun logAuthenticationSuccess(userId: String, method: String) {
        logSecurityEvent(
            EVENT_AUTHENTICATION_SUCCESS,
            "User authenticated successfully",
            mapOf(
                "user_id" to userId,
                "method" to method,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log authentication failure
     */
    fun logAuthenticationFailure(userId: String, method: String, reason: String) {
        logSecurityEvent(
            EVENT_AUTHENTICATION_FAILED,
            "Authentication failed",
            mapOf(
                "user_id" to userId,
                "method" to method,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log account lockout
     */
    fun logAccountLockout(userId: String, failedAttempts: Int) {
        logSecurityEvent(
            EVENT_AUTHENTICATION_LOCKED,
            "Account locked due to failed attempts",
            mapOf(
                "user_id" to userId,
                "failed_attempts" to failedAttempts.toString(),
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log session creation
     */
    fun logSessionCreated(sessionId: String, userId: String) {
        logSecurityEvent(
            EVENT_SESSION_CREATED,
            "Session created",
            mapOf(
                "session_id" to sessionId,
                "user_id" to userId,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log session expiration
     */
    fun logSessionExpired(sessionId: String, reason: String) {
        logSecurityEvent(
            EVENT_SESSION_EXPIRED,
            "Session expired",
            mapOf(
                "session_id" to sessionId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log permission denied
     */
    fun logPermissionDenied(sessionId: String, permission: String, resource: String) {
        logSecurityEvent(
            EVENT_PERMISSION_DENIED,
            "Permission denied",
            mapOf(
                "session_id" to sessionId,
                "permission" to permission,
                "resource" to resource,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log encryption error
     */
    fun logEncryptionError(context: String, exception: Throwable) {
        logSecurityEvent(
            EVENT_ENCRYPTION_ERROR,
            "Encryption error occurred",
            mapOf(
                "context" to context,
                "error" to exception.message.orEmpty(),
                "timestamp" to System.currentTimeMillis().toString()
            ),
            exception
        )
    }
    
    /**
     * Log decryption error
     */
    fun logDecryptionError(context: String, exception: Throwable) {
        logSecurityEvent(
            EVENT_DECRYPTION_ERROR,
            "Decryption error occurred",
            mapOf(
                "context" to context,
                "error" to exception.message.orEmpty(),
                "timestamp" to System.currentTimeMillis().toString()
            ),
            exception
        )
    }
    
    /**
     * Log database error
     */
    fun logDatabaseError(operation: String, exception: Throwable) {
        logSecurityEvent(
            EVENT_DATABASE_ERROR,
            "Database error occurred",
            mapOf(
                "operation" to operation,
                "error" to exception.message.orEmpty(),
                "timestamp" to System.currentTimeMillis().toString()
            ),
            exception
        )
    }
    
    /**
     * Log file access denied
     */
    fun logFileAccessDenied(filePath: String, operation: String) {
        logSecurityEvent(
            EVENT_FILE_ACCESS_DENIED,
            "File access denied",
            mapOf(
                "file_path" to filePath,
                "operation" to operation,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log memory security issue
     */
    fun logMemorySecurityIssue(issue: String, details: String) {
        logSecurityEvent(
            EVENT_MEMORY_SECURITY_ISSUE,
            "Memory security issue detected",
            mapOf(
                "issue" to issue,
                "details" to details,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log deep link validation
     */
    fun logDeepLinkValidation(url: String, isValid: Boolean, reason: String?) {
        logSecurityEvent(
            EVENT_DEEP_LINK_VALIDATION,
            "Deep link validation",
            mapOf(
                "url" to url,
                "is_valid" to isValid.toString(),
                "reason" to (reason ?: ""),
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log biometric authentication
     */
    fun logBiometricAuth(result: String, error: String?) {
        logSecurityEvent(
            EVENT_BIOMETRIC_AUTH,
            "Biometric authentication attempt",
            mapOf(
                "result" to result,
                "error" to (error ?: ""),
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log suspicious activity
     */
    fun logSuspiciousActivity(activity: String, severity: String, details: Map<String, String>) {
        val allDetails = details.toMutableMap()
        allDetails["severity"] = severity
        allDetails["timestamp"] = System.currentTimeMillis().toString()
        
        logSecurityEvent(
            EVENT_SUSPICIOUS_ACTIVITY,
            "Suspicious activity detected: $activity",
            allDetails
        )
    }
    
    /**
     * Log critical security event
     */
    fun logCriticalSecurityEvent(event: String, details: String, exception: Throwable?) {
        val entry = SecurityLogEntry(
            timestamp = System.currentTimeMillis(),
            level = LEVEL_CRITICAL,
            eventType = "CRITICAL_SECURITY_EVENT",
            message = event,
            details = mapOf(
                "details" to details,
                "timestamp" to System.currentTimeMillis().toString()
            ),
            exception = exception
        )
        
        logQueue.offer(entry)
        
        // Immediately flush critical events
        flushLogs()
        
        // Also log to system log
        if (exception != null) {
            Log.e(TAG, "[CRITICAL] $event: $details", exception)
        } else {
            Log.e(TAG, "[CRITICAL] $event: $details")
        }
    }
    
    /**
     * Flush logs to persistent storage
     */
    private fun flushLogs() {
        if (logQueue.isEmpty()) return
        
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            val entries = mutableListOf<SecurityLogEntry>()
            
            // Drain queue
            while (logQueue.isNotEmpty()) {
                val entry = logQueue.poll()
                if (entry != null) {
                    entries.add(entry)
                }
            }
            
            if (entries.isEmpty()) return
            
            // Check file size and rotate if necessary
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile(logFile)
            }
            
            // Write entries to file
            FileWriter(logFile, true).use { writer ->
                entries.forEach { entry ->
                    val logLine = formatLogEntry(entry)
                    writer.write(logLine)
                    writer.write("\n")
                }
            }
            
            // Set secure permissions
            logFile.setReadable(true, true)
            logFile.setWritable(true, true)
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to flush security logs", e)
        }
    }
    
    /**
     * Format log entry for writing
     */
    private fun formatLogEntry(entry: SecurityLogEntry): String {
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val level = getLevelString(entry.level)
        
        val sb = StringBuilder()
        sb.append("[$timestamp] [$level] [${entry.eventType}] ${entry.message}")
        
        entry.details?.forEach { (key, value) ->
            sb.append(" | $key=$value")
        }
        
        entry.exception?.let { ex ->
            sb.append(" | Exception: ${ex.javaClass.simpleName}: ${ex.message}")
        }
        
        return sb.toString()
    }
    
    /**
     * Get level string
     */
    private fun getLevelString(level: Int): String {
        return when (level) {
            LEVEL_DEBUG -> "DEBUG"
            LEVEL_INFO -> "INFO"
            LEVEL_WARN -> "WARN"
            LEVEL_ERROR -> "ERROR"
            LEVEL_CRITICAL -> "CRITICAL"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Rotate log file
     */
    private fun rotateLogFile(logFile: File) {
        try {
            val backupFile = File(context.filesDir, "${LOG_FILE_NAME}.backup")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            logFile.renameTo(backupFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    /**
     * Flush loop
     */
    private fun flushLoop() {
        while (isRunning.get()) {
            try {
                Thread.sleep(FLUSH_INTERVAL)
                flushLogs()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in flush loop", e)
            }
        }
    }
    
    /**
     * Get log entries for analysis
     */
    fun getLogEntries(limit: Int = 100): List<String> {
        return try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (!logFile.exists()) {
                return emptyList()
            }
            
            logFile.readLines().takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log entries", e)
            emptyList()
        }
    }
    
    /**
     * Clear logs
     */
    fun clearLogs() {
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.delete()
            }
            
            val backupFile = File(context.filesDir, "${LOG_FILE_NAME}.backup")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            logQueue.clear()
            
            logSecurityEvent(EVENT_SECURITY_CONFIG_CHANGED, "Security logs cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    /**
     * Export logs
     */
    fun exportLogs(): File? {
        return try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (!logFile.exists()) {
                return null
            }
            
            val exportFile = File(context.filesDir, "security_log_export_${System.currentTimeMillis()}.txt")
            logFile.copyTo(exportFile, true)
            
            logSecurityEvent(EVENT_SECURITY_CONFIG_CHANGED, "Security logs exported")
            
            exportFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            null
        }
    }
    
    /**
     * Shutdown logger
     */
    fun shutdown() {
        isRunning.set(false)
        flushThread.interrupt()
        
        // Final flush
        flushLogs()
        
        logSecurityEvent(EVENT_SECURITY_CONFIG_CHANGED, "SecurityLogger shutdown")
    }
}