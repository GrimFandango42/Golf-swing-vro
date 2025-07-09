package com.golfswing.vro.pixel.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferencesManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val SECURE_PREFS_NAME = "golf_swing_secure_prefs"
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedSharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            throw SecurityException("Failed to create encrypted preferences", e)
        }
    }

    /**
     * Store encrypted data with additional layer of encryption
     */
    fun storeEncryptedData(key: String, data: ByteArray) {
        try {
            val encryptedData = encryptData(data)
            val base64Data = android.util.Base64.encodeToString(encryptedData, android.util.Base64.DEFAULT)
            encryptedSharedPreferences.edit().putString(key, base64Data).apply()
        } catch (e: Exception) {
            throw SecurityException("Failed to store encrypted data", e)
        }
    }

    /**
     * Retrieve and decrypt data
     */
    fun getEncryptedData(key: String): ByteArray? {
        return try {
            val base64Data = encryptedSharedPreferences.getString(key, null) ?: return null
            val encryptedData = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            decryptData(encryptedData)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Store secure string
     */
    fun storeSecureString(key: String, value: String) {
        encryptedSharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Retrieve secure string
     */
    fun getSecureString(key: String, defaultValue: String? = null): String? {
        return encryptedSharedPreferences.getString(key, defaultValue)
    }

    /**
     * Store secure boolean
     */
    fun storeSecureBoolean(key: String, value: Boolean) {
        encryptedSharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Retrieve secure boolean
     */
    fun getSecureBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return encryptedSharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Store secure integer
     */
    fun storeSecureInt(key: String, value: Int) {
        encryptedSharedPreferences.edit().putInt(key, value).apply()
    }

    /**
     * Retrieve secure integer
     */
    fun getSecureInt(key: String, defaultValue: Int = 0): Int {
        return encryptedSharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Store secure long
     */
    fun storeSecureLong(key: String, value: Long) {
        encryptedSharedPreferences.edit().putLong(key, value).apply()
    }

    /**
     * Retrieve secure long
     */
    fun getSecureLong(key: String, defaultValue: Long = 0L): Long {
        return encryptedSharedPreferences.getLong(key, defaultValue)
    }

    /**
     * Remove encrypted data
     */
    fun removeEncryptedData(key: String) {
        encryptedSharedPreferences.edit().remove(key).apply()
    }

    /**
     * Clear all encrypted data
     */
    fun clearAllEncryptedData() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    /**
     * Check if key exists
     */
    fun containsKey(key: String): Boolean {
        return encryptedSharedPreferences.contains(key)
    }

    /**
     * Get all keys
     */
    fun getAllKeys(): Set<String> {
        return encryptedSharedPreferences.all.keys
    }

    /**
     * Encrypt data using AES-GCM
     */
    private fun encryptData(data: ByteArray): ByteArray {
        try {
            val secretKey = generateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            
            val encryptedData = cipher.doFinal(data)
            val keyBytes = secretKey.encoded
            
            // Combine IV + key + encrypted data
            val result = ByteArray(iv.size + keyBytes.size + encryptedData.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(keyBytes, 0, result, iv.size, keyBytes.size)
            System.arraycopy(encryptedData, 0, result, iv.size + keyBytes.size, encryptedData.size)
            
            return result
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt data using AES-GCM
     */
    private fun decryptData(encryptedData: ByteArray): ByteArray {
        try {
            // Extract IV, key, and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val keyBytes = ByteArray(32) // 256-bit key
            val cipherData = ByteArray(encryptedData.size - GCM_IV_LENGTH - 32)
            
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(encryptedData, GCM_IV_LENGTH, keyBytes, 0, 32)
            System.arraycopy(encryptedData, GCM_IV_LENGTH + 32, cipherData, 0, cipherData.size)
            
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            return cipher.doFinal(cipherData)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    /**
     * Generate secret key for additional encryption
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Validate preferences integrity
     */
    fun validateIntegrity(): Boolean {
        return try {
            // Test basic operations
            val testKey = "integrity_test"
            val testValue = "test_value"
            
            storeSecureString(testKey, testValue)
            val retrievedValue = getSecureString(testKey)
            removeEncryptedData(testKey)
            
            retrievedValue == testValue
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Backup preferences to internal storage
     */
    fun backupPreferences(): ByteArray? {
        return try {
            val allData = encryptedSharedPreferences.all
            val jsonString = com.google.gson.Gson().toJson(allData)
            encryptData(jsonString.toByteArray())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Restore preferences from backup
     */
    fun restorePreferences(backupData: ByteArray): Boolean {
        return try {
            val jsonString = String(decryptData(backupData))
            val data = com.google.gson.Gson().fromJson(jsonString, Map::class.java)
            
            val editor = encryptedSharedPreferences.edit()
            data.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key as String, value)
                    is Boolean -> editor.putBoolean(key as String, value)
                    is Int -> editor.putInt(key as String, value)
                    is Long -> editor.putLong(key as String, value)
                    is Float -> editor.putFloat(key as String, value)
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}