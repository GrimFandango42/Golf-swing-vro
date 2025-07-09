package com.golfswing.vro.pixel.security

import android.content.Context
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyUtils @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
        private const val SECURE_WIPE_PASSES = 3
        private const val ANONYMIZED_PREFIX = "anon_"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss"
    }

    /**
     * Sanitize user data by removing personally identifiable information
     */
    fun sanitizeUserData(data: Map<String, Any>): Map<String, Any> {
        val sanitized = mutableMapOf<String, Any>()
        
        data.forEach { (key, value) ->
            when {
                isPersonallyIdentifiable(key) -> {
                    sanitized[key] = anonymizeValue(value)
                }
                isLocationData(key) -> {
                    sanitized[key] = generalizeLocation(value)
                }
                isTimestampData(key) -> {
                    sanitized[key] = generalizeTimestamp(value)
                }
                else -> {
                    sanitized[key] = value
                }
            }
        }
        
        return sanitized
    }

    /**
     * Anonymize sensitive values
     */
    private fun anonymizeValue(value: Any): String {
        return when (value) {
            is String -> {
                if (value.contains("@")) {
                    // Email anonymization
                    val parts = value.split("@")
                    "${ANONYMIZED_PREFIX}${hashString(parts[0])}@${parts[1]}"
                } else {
                    // Generic string anonymization
                    "${ANONYMIZED_PREFIX}${hashString(value)}"
                }
            }
            else -> "${ANONYMIZED_PREFIX}${hashString(value.toString())}"
        }
    }

    /**
     * Check if field contains personally identifiable information
     */
    private fun isPersonallyIdentifiable(key: String): Boolean {
        val piiKeys = setOf(
            "name", "email", "phone", "address", "username", "user_id",
            "device_id", "imei", "mac_address", "ip_address", "user_name",
            "first_name", "last_name", "full_name", "contact", "identifier"
        )
        return piiKeys.any { key.toLowerCase().contains(it) }
    }

    /**
     * Check if field contains location data
     */
    private fun isLocationData(key: String): Boolean {
        val locationKeys = setOf(
            "latitude", "longitude", "location", "coords", "gps", "position",
            "address", "city", "country", "postal_code", "zip_code"
        )
        return locationKeys.any { key.toLowerCase().contains(it) }
    }

    /**
     * Check if field contains timestamp data
     */
    private fun isTimestampData(key: String): Boolean {
        val timestampKeys = setOf(
            "timestamp", "created_at", "updated_at", "date", "time",
            "created", "modified", "accessed", "last_seen"
        )
        return timestampKeys.any { key.toLowerCase().contains(it) }
    }

    /**
     * Generalize location data for privacy
     */
    private fun generalizeLocation(value: Any): String {
        return when (value) {
            is Double -> {
                // Round to 2 decimal places (roughly 1km accuracy)
                String.format("%.2f", value)
            }
            is String -> {
                // Remove specific address details
                value.split(",").firstOrNull()?.trim() ?: "Location"
            }
            else -> "Location"
        }
    }

    /**
     * Generalize timestamp data for privacy
     */
    private fun generalizeTimestamp(value: Any): String {
        return when (value) {
            is Long -> {
                // Round to nearest hour
                val date = Date(value)
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(calendar.time)
            }
            is String -> {
                // Extract date part only
                value.split(" ").firstOrNull() ?: "Date"
            }
            else -> "Date"
        }
    }

    /**
     * Hash string for anonymization
     */
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(8)
    }

    /**
     * Securely wipe file from storage
     */
    fun secureWipeFile(file: File): Boolean {
        return try {
            if (!file.exists()) return true
            
            val fileLength = file.length()
            val secureRandom = SecureRandom()
            
            // Multiple pass wipe
            repeat(SECURE_WIPE_PASSES) {
                file.outputStream().use { output ->
                    val buffer = ByteArray(1024)
                    var bytesRemaining = fileLength
                    
                    while (bytesRemaining > 0) {
                        val bytesToWrite = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                        secureRandom.nextBytes(buffer)
                        output.write(buffer, 0, bytesToWrite)
                        bytesRemaining -= bytesToWrite
                    }
                    output.flush()
                }
            }
            
            // Final deletion
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Securely wipe directory and contents
     */
    fun secureWipeDirectory(directory: File): Boolean {
        return try {
            if (!directory.exists()) return true
            
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    secureWipeDirectory(file)
                } else {
                    secureWipeFile(file)
                }
            }
            
            directory.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate secure random filename
     */
    fun generateSecureFilename(extension: String = ""): String {
        val timestamp = System.currentTimeMillis()
        val random = SecureRandom().nextInt(100000)
        val suffix = if (extension.isNotEmpty()) ".$extension" else ""
        return "secure_${timestamp}_${random}$suffix"
    }

    /**
     * Sanitize file metadata
     */
    fun sanitizeFileMetadata(file: File): Boolean {
        return try {
            // Set file permissions to restrict access
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setExecutable(false, false)
            
            // Set permissions for owner only
            file.setReadable(true, true)
            file.setWritable(true, true)
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear sensitive data from memory
     */
    fun clearSensitiveData(data: ByteArray) {
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(data)
        data.fill(0)
    }

    /**
     * Clear sensitive data from char array
     */
    fun clearSensitiveData(data: CharArray) {
        val secureRandom = SecureRandom()
        for (i in data.indices) {
            data[i] = secureRandom.nextInt(65536).toChar()
        }
        data.fill('\u0000')
    }

    /**
     * Create secure temporary file
     */
    fun createSecureTempFile(prefix: String, suffix: String): File {
        val tempDir = File(context.cacheDir, "temp_secure")
        tempDir.mkdirs()
        
        val tempFile = File.createTempFile(prefix, suffix, tempDir)
        sanitizeFileMetadata(tempFile)
        
        return tempFile
    }

    /**
     * Generate device fingerprint for analytics (privacy-preserving)
     */
    fun generatePrivacyPreservingFingerprint(): String {
        val deviceInfo = buildString {
            append(Build.MODEL)
            append(Build.MANUFACTURER)
            append(Build.VERSION.SDK_INT)
            append(context.packageName)
            // Note: Not including unique identifiers like IMEI, MAC, etc.
        }
        
        return hashString(deviceInfo)
    }

    /**
     * Validate data before processing
     */
    fun isDataSafe(data: String): Boolean {
        // Check for potential security issues
        val dangerousPatterns = listOf(
            "<script", "javascript:", "data:", "vbscript:",
            "onload=", "onerror=", "onclick=", "eval(",
            "document.cookie", "window.location", "innerHTML"
        )
        
        val lowerData = data.toLowerCase()
        return !dangerousPatterns.any { lowerData.contains(it) }
    }

    /**
     * Redact sensitive information from logs
     */
    fun redactSensitiveInfo(logMessage: String): String {
        var redacted = logMessage
        
        // Redact email addresses
        redacted = redacted.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL_REDACTED]")
        
        // Redact phone numbers
        redacted = redacted.replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "[PHONE_REDACTED]")
        
        // Redact credit card numbers
        redacted = redacted.replace(Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"), "[CARD_REDACTED]")
        
        // Redact IP addresses
        redacted = redacted.replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), "[IP_REDACTED]")
        
        // Redact API keys and tokens
        redacted = redacted.replace(Regex("\\b[A-Za-z0-9]{32,}\\b"), "[TOKEN_REDACTED]")
        
        return redacted
    }

    /**
     * Check if app is running in secure environment
     */
    fun isSecureEnvironment(): Boolean {
        return try {
            // Check for debugging
            val isDebugging = Build.TYPE == "eng" || Build.TYPE == "userdebug"
            
            // Check for emulator
            val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                    Build.FINGERPRINT.startsWith("unknown") ||
                    Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86")
            
            // Check for root
            val isRooted = checkRootAccess()
            
            !isDebugging && !isEmulator && !isRooted
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for root access
     */
    private fun checkRootAccess(): Boolean {
        return try {
            val suPaths = arrayOf(
                "/system/bin/su", "/system/xbin/su", "/sbin/su",
                "/system/su", "/vendor/bin/su"
            )
            
            suPaths.any { File(it).exists() }
        } catch (e: Exception) {
            false
        }
    }
}