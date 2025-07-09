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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Arrays
import android.util.Log
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec

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
        private const val KEYSTORE_ALIAS = "golf_swing_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_SIZE = 256
        private const val TAG = "SecurePreferencesManager"
    }

    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build()
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "StrongBox not available, falling back to TEE", e)
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(false)
                .build()
        }
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
            Log.e(TAG, "Failed to create encrypted preferences", e)
            throw SecurityException("Failed to create encrypted preferences", e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error creating encrypted preferences", e)
            throw SecurityException("IO error creating encrypted preferences", e)
        }
    }
    
    private val keyStore: KeyStore by lazy {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize KeyStore", e)
            throw SecurityException("Failed to initialize KeyStore", e)
        }
    }

    /**
     * Store encrypted data with additional layer of encryption using Android Keystore
     */
    fun storeEncryptedData(key: String, data: ByteArray) {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        require(data.isNotEmpty()) { "Data cannot be empty" }
        
        try {
            val encryptedData = encryptDataWithKeystore(data)
            val base64Data = android.util.Base64.encodeToString(encryptedData, android.util.Base64.NO_WRAP)
            
            val success = encryptedSharedPreferences.edit()
                .putString(key, base64Data)
                .commit()
                
            if (!success) {
                throw SecurityException("Failed to commit encrypted data to preferences")
            }
            
            Log.d(TAG, "Successfully stored encrypted data for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted data", e)
            throw SecurityException("Failed to store encrypted data for key: $key", e)
        }
    }

    /**
     * Retrieve and decrypt data using Android Keystore
     */
    fun getEncryptedData(key: String): ByteArray? {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        
        return try {
            val base64Data = encryptedSharedPreferences.getString(key, null)
            if (base64Data.isNullOrEmpty()) {
                Log.d(TAG, "No data found for key: $key")
                return null
            }
            
            val encryptedData = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
            val decryptedData = decryptDataWithKeystore(encryptedData)
            
            Log.d(TAG, "Successfully retrieved encrypted data for key: $key")
            decryptedData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve encrypted data for key: $key", e)
            null
        }
    }

    /**
     * Store secure string with validation
     */
    fun storeSecureString(key: String, value: String) {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        require(value.isNotEmpty()) { "Value cannot be empty" }
        
        try {
            val success = encryptedSharedPreferences.edit().putString(key, value).commit()
            if (!success) {
                throw SecurityException("Failed to commit secure string")
            }
            Log.d(TAG, "Successfully stored secure string for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store secure string for key: $key", e)
            throw SecurityException("Failed to store secure string for key: $key", e)
        }
    }

    /**
     * Retrieve secure string
     */
    fun getSecureString(key: String, defaultValue: String? = null): String? {
        return encryptedSharedPreferences.getString(key, defaultValue)
    }

    /**
     * Store secure boolean with validation
     */
    fun storeSecureBoolean(key: String, value: Boolean) {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        
        try {
            val success = encryptedSharedPreferences.edit().putBoolean(key, value).commit()
            if (!success) {
                throw SecurityException("Failed to commit secure boolean")
            }
            Log.d(TAG, "Successfully stored secure boolean for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store secure boolean for key: $key", e)
            throw SecurityException("Failed to store secure boolean for key: $key", e)
        }
    }

    /**
     * Retrieve secure boolean
     */
    fun getSecureBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return encryptedSharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Store secure integer with validation
     */
    fun storeSecureInt(key: String, value: Int) {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        
        try {
            val success = encryptedSharedPreferences.edit().putInt(key, value).commit()
            if (!success) {
                throw SecurityException("Failed to commit secure integer")
            }
            Log.d(TAG, "Successfully stored secure integer for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store secure integer for key: $key", e)
            throw SecurityException("Failed to store secure integer for key: $key", e)
        }
    }

    /**
     * Retrieve secure integer
     */
    fun getSecureInt(key: String, defaultValue: Int = 0): Int {
        return encryptedSharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Store secure long with validation
     */
    fun storeSecureLong(key: String, value: Long) {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        
        try {
            val success = encryptedSharedPreferences.edit().putLong(key, value).commit()
            if (!success) {
                throw SecurityException("Failed to commit secure long")
            }
            Log.d(TAG, "Successfully stored secure long for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store secure long for key: $key", e)
            throw SecurityException("Failed to store secure long for key: $key", e)
        }
    }

    /**
     * Retrieve secure long
     */
    fun getSecureLong(key: String, defaultValue: Long = 0L): Long {
        return encryptedSharedPreferences.getLong(key, defaultValue)
    }

    /**
     * Remove encrypted data with validation
     */
    fun removeEncryptedData(key: String) {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        
        try {
            val success = encryptedSharedPreferences.edit().remove(key).commit()
            if (!success) {
                Log.w(TAG, "Failed to remove data for key: $key")
            } else {
                Log.d(TAG, "Successfully removed data for key: $key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove data for key: $key", e)
            throw SecurityException("Failed to remove data for key: $key", e)
        }
    }

    /**
     * Clear all encrypted data and keystore keys
     */
    fun clearAllEncryptedData() {
        try {
            val success = encryptedSharedPreferences.edit().clear().commit()
            if (!success) {
                Log.w(TAG, "Failed to clear encrypted preferences")
            }
            
            // Clear keystore key
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
                Log.d(TAG, "Keystore key cleared")
            }
            
            Log.d(TAG, "All encrypted data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all encrypted data", e)
            throw SecurityException("Failed to clear all encrypted data", e)
        }
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
     * Encrypt data using Android Keystore with AES-GCM
     */
    private fun encryptDataWithKeystore(data: ByteArray): ByteArray {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)
            
            // Combine IV + encrypted data
            val result = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt data with keystore", e)
            throw SecurityException("Failed to encrypt data with keystore", e)
        }
    }

    /**
     * Decrypt data using Android Keystore with AES-GCM
     */
    private fun decryptDataWithKeystore(encryptedData: ByteArray): ByteArray {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Extract IV and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherData = ByteArray(encryptedData.size - GCM_IV_LENGTH)
            
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherData, 0, cipherData.size)
            
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            return cipher.doFinal(cipherData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt data with keystore", e)
            throw SecurityException("Failed to decrypt data with keystore", e)
        }
    }

    /**
     * Get or create secret key in Android Keystore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            } else {
                createSecretKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get or create secret key", e)
            throw SecurityException("Failed to get or create secret key", e)
        }
    }
    
    /**
     * Create new secret key in Android Keystore
     */
    private fun createSecretKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(false)
                .build()
                
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create secret key", e)
            throw SecurityException("Failed to create secret key", e)
        }
    }

    /**
     * Validate preferences integrity and keystore health
     */
    fun validateIntegrity(): Boolean {
        return try {
            // Test keystore access
            val testKey = "integrity_test"
            val testValue = "test_value"
            val testData = testValue.toByteArray(Charsets.UTF_8)
            
            // Test encrypted data operations
            storeEncryptedData(testKey, testData)
            val retrievedData = getEncryptedData(testKey)
            removeEncryptedData(testKey)
            
            // Test regular string operations
            storeSecureString(testKey, testValue)
            val retrievedValue = getSecureString(testKey)
            removeEncryptedData(testKey)
            
            val dataMatch = retrievedData != null && Arrays.equals(retrievedData, testData)
            val stringMatch = retrievedValue == testValue
            
            Log.d(TAG, "Integrity validation: dataMatch=$dataMatch, stringMatch=$stringMatch")
            dataMatch && stringMatch
        } catch (e: Exception) {
            Log.e(TAG, "Integrity validation failed", e)
            false
        }
    }

    /**
     * Backup preferences to internal storage with keystore encryption
     */
    fun backupPreferences(): ByteArray? {
        return try {
            val allData = encryptedSharedPreferences.all
            val jsonString = com.google.gson.Gson().toJson(allData)
            encryptDataWithKeystore(jsonString.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup preferences", e)
            null
        }
    }

    /**
     * Restore preferences from backup with keystore decryption
     */
    fun restorePreferences(backupData: ByteArray): Boolean {
        return try {
            val jsonString = String(decryptDataWithKeystore(backupData))
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
            val success = editor.commit()
            
            if (!success) {
                Log.e(TAG, "Failed to commit restored preferences")
                return false
            }
            
            Log.d(TAG, "Successfully restored preferences from backup")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore preferences from backup", e)
            false
        }
    }
    
    /**
     * Securely wipe memory containing sensitive data
     */
    fun secureWipeMemory(data: ByteArray) {
        Arrays.fill(data, 0.toByte())
    }
    
    /**
     * Check if Android Keystore is available and healthy
     */
    fun isKeystoreHealthy(): Boolean {
        return try {
            val testAlias = "health_check_key"
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                testAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build()
                
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            // Clean up test key
            keyStore.deleteEntry(testAlias)
            
            Log.d(TAG, "Keystore health check passed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Keystore health check failed", e)
            false
        }
    }
}