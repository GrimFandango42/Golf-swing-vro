package com.golfswing.vro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.golfswing.vro.pixel.security.DatabaseEncryptionManager
import com.golfswing.vro.pixel.security.PrivacyUtils
import com.golfswing.vro.pixel.security.SecurePreferencesManager
import com.golfswing.vro.pixel.security.SecurityConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Security integration tests to ensure all security components work together
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SecurityIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var securityConfig: SecurityConfig

    @Inject
    lateinit var databaseEncryptionManager: DatabaseEncryptionManager

    @Inject
    lateinit var securePreferencesManager: SecurePreferencesManager

    @Inject
    lateinit var privacyUtils: PrivacyUtils

    private lateinit var appContext: android.content.Context

    @Before
    fun setUp() {
        hiltRule.inject()
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testSecurityConfigurationInitialization() {
        // Test security configuration is properly initialized
        assert(securityConfig.isSecurityEnabled())
        assert(securityConfig.isDatabaseEncryptionEnabled())
        assert(securityConfig.isPrivacyProtectionEnabled())
    }

    @Test
    fun testDatabaseEncryptionIntegration() {
        // Test database encryption manager is working
        val testData = "test encryption data"
        val encryptedData = databaseEncryptionManager.encryptData(testData)
        val decryptedData = databaseEncryptionManager.decryptData(encryptedData)
        
        assert(encryptedData != testData)
        assert(decryptedData == testData)
    }

    @Test
    fun testSecurePreferencesIntegration() {
        // Test secure preferences manager
        val testKey = "test_key"
        val testValue = "test_value"
        
        securePreferencesManager.storeSecureString(testKey, testValue)
        val retrievedValue = securePreferencesManager.getSecureString(testKey)
        
        assert(retrievedValue == testValue)
    }

    @Test
    fun testPrivacyUtilsIntegration() {
        // Test privacy utilities
        val testPii = "John Doe, john@example.com, 123-456-7890"
        val sanitizedData = privacyUtils.sanitizeData(testPii)
        
        assert(sanitizedData != testPii)
        assert(!sanitizedData.contains("John Doe"))
        assert(!sanitizedData.contains("john@example.com"))
        assert(!sanitizedData.contains("123-456-7890"))
    }

    @Test
    fun testSecurityValidation() {
        // Test comprehensive security validation
        val validationResult = securityConfig.validateSecurityConfiguration(appContext)
        assert(validationResult.isValid)
        assert(validationResult.errors.isEmpty())
    }

    @Test
    fun testNoNetworkPermissions() {
        // Test that no network permissions are present
        val packageManager = appContext.packageManager
        val packageInfo = packageManager.getPackageInfo(appContext.packageName, 
            android.content.pm.PackageManager.GET_PERMISSIONS)
        
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        
        assert(!permissions.contains(android.Manifest.permission.INTERNET))
        assert(!permissions.contains(android.Manifest.permission.ACCESS_NETWORK_STATE))
        assert(!permissions.contains(android.Manifest.permission.ACCESS_WIFI_STATE))
    }

    @Test
    fun testInternalStorageOnlyAccess() {
        // Test that only internal storage is accessible
        val packageManager = appContext.packageManager
        val packageInfo = packageManager.getPackageInfo(appContext.packageName, 
            android.content.pm.PackageManager.GET_PERMISSIONS)
        
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        
        assert(!permissions.contains(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assert(!permissions.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    @Test
    fun testSecureFileOperations() {
        // Test secure file operations
        val testFile = java.io.File(appContext.filesDir, "test_secure_file.txt")
        val testContent = "Test content for security"
        
        // Write file
        testFile.writeText(testContent)
        
        // Verify file exists and is readable
        assert(testFile.exists())
        assert(testFile.canRead())
        assert(testFile.readText() == testContent)
        
        // Test secure deletion
        privacyUtils.secureDeleteFile(testFile)
        assert(!testFile.exists())
    }

    @Test
    fun testBackupPrevention() {
        // Test that sensitive data is excluded from backups
        val applicationInfo = appContext.applicationInfo
        assert(!applicationInfo.flags.and(android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0)
    }

    @Test
    fun testSecurityIntegrityCheck() {
        // Test security integrity check
        val integrityResult = securityConfig.performSecurityIntegrityCheck(appContext)
        assert(integrityResult.isIntegrityMaintained)
        assert(integrityResult.vulnerabilities.isEmpty())
    }
}