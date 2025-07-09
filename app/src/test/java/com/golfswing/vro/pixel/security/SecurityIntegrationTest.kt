package com.golfswing.vro.pixel.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom
import java.util.Arrays

@RunWith(AndroidJUnit4::class)
class SecurityIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var securePreferences: SecurePreferencesManager
    private lateinit var memoryManager: SecureMemoryManager
    private lateinit var securityLogger: SecurityLogger
    private lateinit var authManager: AuthenticationManager
    private lateinit var mediaManager: SecureMediaManager
    private lateinit var securityConfig: SecurityConfig
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        securePreferences = SecurePreferencesManager(context)
        memoryManager = SecureMemoryManager()
        securityLogger = SecurityLogger(context, securePreferences)
        authManager = AuthenticationManager(context, securePreferences, memoryManager)
        mediaManager = SecureMediaManager(context, securePreferences)
        securityConfig = SecurityConfig(context, securePreferences, securityLogger)
    }
    
    @After
    fun tearDown() {
        // Clean up test data
        securePreferences.clearAllEncryptedData()
        memoryManager.clearAllSensitiveData()
        memoryManager.shutdown()
        authManager.shutdown()
        securityLogger.shutdown()
    }
    
    @Test
    fun testSecurePreferencesIntegration() {
        // Test basic string storage
        val testKey = "test_key"
        val testValue = "test_value"
        
        securePreferences.storeSecureString(testKey, testValue)
        val retrievedValue = securePreferences.getSecureString(testKey)
        
        assertEquals(testValue, retrievedValue)
        
        // Test encrypted data storage
        val testData = "sensitive_data".toByteArray()
        securePreferences.storeEncryptedData("encrypted_key", testData)
        val retrievedData = securePreferences.getEncryptedData("encrypted_key")
        
        assertNotNull(retrievedData)
        assertTrue(Arrays.equals(testData, retrievedData))
        
        // Test integrity validation
        assertTrue(securePreferences.validateIntegrity())
        
        // Test keystore health
        assertTrue(securePreferences.isKeystoreHealthy())
    }
    
    @Test
    fun testMemorySecurityIntegration() {
        // Test sensitive data registration and cleanup
        val sensitiveData = "password123".toByteArray()
        val dataId = memoryManager.registerSensitiveData(sensitiveData, "test_password")
        
        assertTrue(dataId > 0)
        
        // Test secure memory operations
        val secureArray = memoryManager.getSecureByteArray(32)
        assertNotNull(secureArray)
        assertEquals(32, secureArray.size)
        
        // Test secure random generation
        val randomData = memoryManager.generateSecureRandom(16)
        assertNotNull(randomData)
        assertEquals(16, randomData.size)
        
        // Test secure comparison
        val data1 = "test".toByteArray()
        val data2 = "test".toByteArray()
        val data3 = "different".toByteArray()
        
        assertTrue(memoryManager.secureEquals(data1, data2))
        assertFalse(memoryManager.secureEquals(data1, data3))
        
        // Test memory cleanup
        memoryManager.clearSensitiveData(dataId)
        memoryManager.secureWipeArray(sensitiveData)
        
        // All bytes should be zero after wiping
        assertTrue(sensitiveData.all { it == 0.toByte() })
    }
    
    @Test
    fun testAuthenticationIntegration() {
        // Test PIN setup
        val testPin = "123456"
        val setupSuccess = authManager.setupPinAuthentication(testPin)
        assertTrue(setupSuccess)
        assertTrue(authManager.isPinSetup())
        
        // Test PIN authentication
        val authResponse = authManager.authenticateWithPin(testPin)
        assertEquals(AuthenticationManager.AuthenticationResult.SUCCESS, authResponse.result)
        assertNotNull(authResponse.sessionId)
        
        // Test session validation
        val sessionId = authResponse.sessionId!!
        assertTrue(authManager.validateSession(sessionId))
        
        // Test permission checking
        assertTrue(authManager.hasPermission(sessionId, AuthenticationManager.Permission.CAMERA_ACCESS))
        
        // Test wrong PIN
        val wrongAuthResponse = authManager.authenticateWithPin("wrong123")
        assertEquals(AuthenticationManager.AuthenticationResult.FAILED, wrongAuthResponse.result)
        
        // Test session invalidation
        authManager.invalidateSession(sessionId)
        assertFalse(authManager.validateSession(sessionId))
    }
    
    @Test
    fun testMediaSecurityIntegration() {
        // Create test file
        val testFile = File(context.cacheDir, "test_video.mp4")
        val testData = "test video content".toByteArray()
        testFile.writeBytes(testData)
        
        // Test file encryption
        val encryptedFile = mediaManager.encryptMediaFile(testFile, "encrypted_video")
        assertNotNull(encryptedFile)
        assertTrue(encryptedFile.exists())
        
        // Test file decryption
        val decryptedFile = File(context.cacheDir, "decrypted_video.mp4")
        val decryptSuccess = mediaManager.decryptMediaFile("encrypted_video", decryptedFile)
        assertTrue(decryptSuccess)
        assertTrue(decryptedFile.exists())
        
        // Verify content integrity
        val decryptedData = decryptedFile.readBytes()
        assertTrue(Arrays.equals(testData, decryptedData))
        
        // Test metadata
        val metadata = mediaManager.getMediaMetadata("encrypted_video")
        assertNotNull(metadata)
        assertEquals("encrypted_video", metadata?.originalName)
        
        // Test file listing
        val encryptedFiles = mediaManager.listEncryptedFiles()
        assertTrue(encryptedFiles.contains("encrypted_video"))
        
        // Test file deletion
        val deleteSuccess = mediaManager.deleteEncryptedFile("encrypted_video")
        assertTrue(deleteSuccess)
        
        // Clean up
        testFile.delete()
        decryptedFile.delete()
    }
    
    @Test
    fun testSecurityConfiguration() {
        // Test default configuration
        val config = securityConfig.getSecurityConfiguration()
        assertNotNull(config)
        assertTrue(config.encryptionEnabled)
        assertTrue(config.databaseEncryption)
        
        // Test security level changes
        securityConfig.setSecurityLevel(SecurityConfig.SecurityLevel.MAXIMUM)
        assertEquals(SecurityConfig.SecurityLevel.MAXIMUM, securityConfig.getSecurityLevel())
        
        // Test feature enabling/disabling
        securityConfig.setMemoryProtectionEnabled(false)
        assertFalse(securityConfig.isMemoryProtectionEnabled())
        
        securityConfig.setMemoryProtectionEnabled(true)
        assertTrue(securityConfig.isMemoryProtectionEnabled())
        
        // Test configuration validation
        assertTrue(securityConfig.validateConfiguration())
        
        // Test device capabilities
        val capabilities = securityConfig.getDeviceSecurityCapabilities()
        assertNotNull(capabilities)
    }
    
    @Test
    fun testSecurityLogging() {
        // Test basic logging
        securityLogger.logSecurityEvent(
            SecurityLogger.EVENT_AUTHENTICATION_SUCCESS,
            "Test authentication success"
        )
        
        // Test authentication logging
        securityLogger.logAuthenticationSuccess("test_user", "PIN")
        securityLogger.logAuthenticationFailure("test_user", "PIN", "Invalid PIN")
        
        // Test session logging
        securityLogger.logSessionCreated("session_123", "test_user")
        securityLogger.logSessionExpired("session_123", "Timeout")
        
        // Test critical event logging
        securityLogger.logCriticalSecurityEvent(
            "Test critical event",
            "This is a test critical security event",
            null
        )
        
        // Test log retrieval
        val logEntries = securityLogger.getLogEntries(10)
        assertNotNull(logEntries)
        assertTrue(logEntries.isNotEmpty())
        
        // Test log export
        val exportedLog = securityLogger.exportLogs()
        assertNotNull(exportedLog)
        assertTrue(exportedLog.exists())
        
        // Clean up
        exportedLog.delete()
    }
    
    @Test
    fun testCryptoOperations() {
        // Test encryption/decryption cycle
        val originalData = "This is sensitive data that needs to be encrypted"
        val dataBytes = originalData.toByteArray()
        
        // Store encrypted data
        securePreferences.storeEncryptedData("crypto_test", dataBytes)
        
        // Retrieve and verify
        val retrievedData = securePreferences.getEncryptedData("crypto_test")
        assertNotNull(retrievedData)
        assertTrue(Arrays.equals(dataBytes, retrievedData))
        
        // Test secure memory wiping
        memoryManager.secureWipeArray(dataBytes)
        
        // Verify data is wiped
        assertTrue(dataBytes.all { it == 0.toByte() })
    }
    
    @Test
    fun testSecurityResilience() {
        // Test multiple authentication failures
        authManager.setupPinAuthentication("123456")
        
        // Test lockout after multiple failures
        repeat(3) {
            val response = authManager.authenticateWithPin("wrong")
            assertEquals(AuthenticationManager.AuthenticationResult.FAILED, response.result)
        }
        
        // Should be locked out now
        val lockedResponse = authManager.authenticateWithPin("wrong")
        assertEquals(AuthenticationManager.AuthenticationResult.LOCKED_OUT, lockedResponse.result)
        
        // Test data corruption handling
        val corruptData = ByteArray(100)
        SecureRandom().nextBytes(corruptData)
        
        // This should fail gracefully
        val retrievedCorruptData = securePreferences.getEncryptedData("non_existent_key")
        assertNull(retrievedCorruptData)
    }
    
    @Test
    fun testPerformanceAndMemoryUsage() {
        // Test memory statistics
        val memoryStats = memoryManager.getMemoryStats()
        assertNotNull(memoryStats)
        assertTrue(memoryStats.totalMemory > 0)
        
        // Test memory pressure handling
        memoryManager.handleMemoryPressure()
        
        // Test cleanup performance
        val startTime = System.currentTimeMillis()
        memoryManager.forceCleanup()
        val endTime = System.currentTimeMillis()
        
        // Cleanup should be fast (less than 1 second)
        assertTrue(endTime - startTime < 1000)
    }
    
    @Test
    fun testEndToEndSecurityFlow() {
        // Complete security flow test
        
        // 1. Setup authentication
        val setupSuccess = authManager.setupPinAuthentication("123456")
        assertTrue(setupSuccess)
        
        // 2. Authenticate
        val authResponse = authManager.authenticateWithPin("123456")
        assertEquals(AuthenticationManager.AuthenticationResult.SUCCESS, authResponse.result)
        val sessionId = authResponse.sessionId!!
        
        // 3. Store sensitive data
        val sensitiveData = "Very sensitive user data".toByteArray()
        securePreferences.storeEncryptedData("user_data", sensitiveData)
        
        // 4. Create and encrypt media file
        val mediaFile = File(context.cacheDir, "test_media.mp4")
        mediaFile.writeBytes("media content".toByteArray())
        val encryptedMedia = mediaManager.encryptMediaFile(mediaFile, "user_media")
        
        // 5. Verify permissions
        assertTrue(authManager.hasPermission(sessionId, AuthenticationManager.Permission.MEDIA_ACCESS))
        
        // 6. Retrieve and verify data
        val retrievedData = securePreferences.getEncryptedData("user_data")
        assertNotNull(retrievedData)
        assertTrue(Arrays.equals(sensitiveData, retrievedData))
        
        // 7. Decrypt media
        val decryptedMedia = File(context.cacheDir, "decrypted_media.mp4")
        val decryptSuccess = mediaManager.decryptMediaFile("user_media", decryptedMedia)
        assertTrue(decryptSuccess)
        
        // 8. Cleanup
        authManager.invalidateSession(sessionId)
        mediaManager.deleteEncryptedFile("user_media")
        securePreferences.removeEncryptedData("user_data")
        
        // 9. Verify cleanup
        assertFalse(authManager.validateSession(sessionId))
        assertNull(securePreferences.getEncryptedData("user_data"))
        
        // Clean up test files
        mediaFile.delete()
        decryptedMedia.delete()
    }
}