package com.golfswing.vro.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.golfswing.vro.data.entity.Converters
import com.golfswing.vro.data.entity.SwingAnalysisEntity
import com.golfswing.vro.data.entity.UserProfileEntity
import com.golfswing.vro.data.entity.SwingSessionEntity
import com.golfswing.vro.data.entity.PracticeGoalEntity
import com.golfswing.vro.pixel.security.DatabaseEncryptionManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Log

@Database(
    entities = [
        SwingAnalysisEntity::class,
        UserProfileEntity::class,
        SwingSessionEntity::class,
        PracticeGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GolfSwingDatabase : RoomDatabase() {
    
    abstract fun swingAnalysisDao(): SwingAnalysisDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun swingSessionDao(): SwingSessionDao
    abstract fun practiceGoalDao(): PracticeGoalDao
    
    companion object {
        @Volatile
        private var INSTANCE: GolfSwingDatabase? = null
        private const val DATABASE_NAME = "golf_swing_database_encrypted"
        
        fun getDatabase(
            context: Context,
            encryptionManager: DatabaseEncryptionManager
        ): GolfSwingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = createEncryptedDatabase(context, encryptionManager)
                INSTANCE = instance
                instance
            }
        }
        
        private fun createEncryptedDatabase(
            context: Context,
            encryptionManager: DatabaseEncryptionManager
        ): GolfSwingDatabase {
            try {
                val passphrase = encryptionManager.getDatabaseKey()
                val factory = SupportOpenHelperFactory(passphrase)
                
                return Room.databaseBuilder(
                    context.applicationContext,
                    GolfSwingDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            configureDatabaseSecurity(db)
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            validateDatabaseEncryption(db, encryptionManager)
                        }
                    })
                    .build()
            } catch (e: Exception) {
                Log.e("GolfSwingDatabase", "Failed to create encrypted database", e)
                throw SecurityException("Database encryption initialization failed", e)
            }
        }
        
        private fun configureDatabaseSecurity(db: SupportSQLiteDatabase) {
            try {
                // Enable secure delete to prevent data recovery
                db.execSQL("PRAGMA secure_delete = ON")
                
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys = ON")
                
                // Set journal mode to WAL for better performance
                db.execSQL("PRAGMA journal_mode = WAL")
                
                // Enable query optimization
                db.execSQL("PRAGMA optimize")
                
                // Set cipher page size for better performance
                db.execSQL("PRAGMA cipher_page_size = 4096")
                
                // Enable memory security
                db.execSQL("PRAGMA cipher_memory_security = ON")
                
                Log.d("GolfSwingDatabase", "Database security configured successfully")
            } catch (e: Exception) {
                Log.e("GolfSwingDatabase", "Failed to configure database security", e)
                throw SecurityException("Database security configuration failed", e)
            }
        }
        
        private fun validateDatabaseEncryption(
            db: SupportSQLiteDatabase,
            encryptionManager: DatabaseEncryptionManager
        ) {
            try {
                val isEncrypted = encryptionManager.isDatabaseEncrypted(db)
                if (!isEncrypted) {
                    throw SecurityException("Database encryption validation failed")
                }
                
                val isIntegrityValid = encryptionManager.verifyDatabaseIntegrity(db)
                if (!isIntegrityValid) {
                    Log.w("GolfSwingDatabase", "Database integrity check failed")
                }
                
                Log.d("GolfSwingDatabase", "Database encryption validated successfully")
            } catch (e: Exception) {
                Log.e("GolfSwingDatabase", "Database encryption validation failed", e)
                throw SecurityException("Database encryption validation failed", e)
            }
        }
        
        /**
         * Securely close database and clear from memory
         */
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
        
        /**
         * Re-encrypt database with new key
         */
        fun reEncryptDatabase(
            context: Context,
            encryptionManager: DatabaseEncryptionManager,
            newKey: ByteArray
        ) {
            synchronized(this) {
                INSTANCE?.let { database ->
                    try {
                        val db = database.openHelper.writableDatabase
                        encryptionManager.reEncryptDatabase(db, newKey)
                        Log.d("GolfSwingDatabase", "Database re-encrypted successfully")
                    } catch (e: Exception) {
                        Log.e("GolfSwingDatabase", "Database re-encryption failed", e)
                        throw SecurityException("Database re-encryption failed", e)
                    }
                }
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add new columns if needed
                // database.execSQL("ALTER TABLE swing_analyses ADD COLUMN new_column TEXT")
            }
        }
    }
}