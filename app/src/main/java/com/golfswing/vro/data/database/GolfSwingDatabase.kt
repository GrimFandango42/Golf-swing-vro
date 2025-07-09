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
        
        fun getDatabase(context: Context): GolfSwingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GolfSwingDatabase::class.java,
                    "golf_swing_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
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