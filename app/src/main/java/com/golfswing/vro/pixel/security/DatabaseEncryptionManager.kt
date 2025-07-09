package com.golfswing.vro.pixel.security

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseEncryptionManager @Inject constructor(
    private val context: Context,
    private val securePreferences: SecurePreferencesManager
) {

    companion object {
        private const val DATABASE_KEY_ALIAS = "golf_swing_db_key"
        private const val KEY_LENGTH = 256
        private const val ALGORITHM = "AES"
    }

    /**
     * Generate or retrieve database encryption key
     */
    fun getDatabaseKey(): ByteArray {
        return try {
            // Try to get existing key
            val existingKey = securePreferences.getEncryptedData(DATABASE_KEY_ALIAS)
            if (existingKey != null) {
                existingKey
            } else {
                // Generate new key
                generateDatabaseKey()
            }
        } catch (e: Exception) {
            // Fallback: generate new key
            generateDatabaseKey()
        }
    }

    /**
     * Generate new database encryption key
     */
    private fun generateDatabaseKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_LENGTH)
        val secretKey = keyGenerator.generateKey()
        val keyBytes = secretKey.encoded
        
        // Store key securely
        securePreferences.storeEncryptedData(DATABASE_KEY_ALIAS, keyBytes)
        
        return keyBytes
    }

    /**
     * Create encrypted database helper factory
     */
    fun createEncryptedDatabaseFactory(): SupportSQLiteOpenHelper.Factory {
        val passphrase = getDatabaseKey()
        return SupportOpenHelperFactory(passphrase)
    }

    /**
     * Get database passphrase as string (SQLCipher requirement)
     */
    fun getDatabasePassphrase(): String {
        return String(getDatabaseKey(), Charsets.UTF_8)
    }

    /**
     * Securely clear database key from memory
     */
    fun clearDatabaseKey() {
        securePreferences.removeEncryptedData(DATABASE_KEY_ALIAS)
    }

    /**
     * Validate database encryption status
     */
    fun isDatabaseEncrypted(database: SupportSQLiteDatabase): Boolean {
        return try {
            // Try to query SQLCipher pragma
            val cursor = database.query("PRAGMA cipher_version")
            cursor.moveToFirst()
            val version = cursor.getString(0)
            cursor.close()
            !version.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Re-encrypt database with new key
     */
    fun reEncryptDatabase(
        database: SupportSQLiteDatabase,
        newKey: ByteArray
    ) {
        try {
            val newPassphrase = String(newKey, Charsets.UTF_8)
            database.execSQL("PRAGMA rekey = '$newPassphrase'")
            
            // Update stored key
            securePreferences.storeEncryptedData(DATABASE_KEY_ALIAS, newKey)
        } catch (e: Exception) {
            throw SecurityException("Failed to re-encrypt database", e)
        }
    }

    /**
     * Verify database integrity
     */
    fun verifyDatabaseIntegrity(database: SupportSQLiteDatabase): Boolean {
        return try {
            val cursor = database.query("PRAGMA integrity_check")
            cursor.moveToFirst()
            val result = cursor.getString(0)
            cursor.close()
            result == "ok"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Secure database maintenance
     */
    fun performSecureMaintenance(database: SupportSQLiteDatabase) {
        try {
            // Vacuum to remove deleted data
            database.execSQL("PRAGMA secure_delete = ON")
            database.execSQL("VACUUM")
            
            // Analyze for performance
            database.execSQL("ANALYZE")
            
            // Clear temporary data
            database.execSQL("PRAGMA temp_store = MEMORY")
        } catch (e: Exception) {
            // Log error but don't throw - maintenance is optional
        }
    }
}