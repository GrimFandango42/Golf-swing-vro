package com.swingsync.ai.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.swingsync.ai.data.local.converter.Converters
import com.swingsync.ai.data.local.dao.*
import com.swingsync.ai.data.local.entity.*

/**
 * Room database for SwingSync AI application.
 * Contains all entities and provides access to DAOs.
 */
@Database(
    entities = [
        SwingSessionEntity::class,
        SwingAnalysisEntity::class,
        PoseDetectionEntity::class,
        UserSettingsEntity::class,
        UserProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SwingSyncDatabase : RoomDatabase() {
    
    abstract fun swingSessionDao(): SwingSessionDao
    abstract fun swingAnalysisDao(): SwingAnalysisDao
    abstract fun poseDetectionDao(): PoseDetectionDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun userProgressDao(): UserProgressDao
    
    companion object {
        private const val DATABASE_NAME = "swing_sync_database"
        
        @Volatile
        private var INSTANCE: SwingSyncDatabase? = null
        
        fun getDatabase(context: Context): SwingSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwingSyncDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Migration from version 1 to 2.
         * This is an example migration - adjust as needed for actual schema changes.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration SQL - adjust based on actual schema changes
                // database.execSQL("ALTER TABLE swing_sessions ADD COLUMN new_column TEXT")
            }
        }
    }
}

/**
 * Database callback for initialization tasks.
 */
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Database creation logic if needed
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Database opening logic if needed
    }
}