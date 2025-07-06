package com.swingsync.ai.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.swingsync.ai.data.local.dao.SwingAnalysisDao
import com.swingsync.ai.data.local.dao.UserProfileDao
import com.swingsync.ai.data.models.SwingAnalysis
import com.swingsync.ai.data.models.UserProfile

@Database(
    entities = [
        SwingAnalysis::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SwingSyncDatabase : RoomDatabase() {
    
    abstract fun swingAnalysisDao(): SwingAnalysisDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: SwingSyncDatabase? = null
        
        fun getDatabase(context: Context): SwingSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwingSyncDatabase::class.java,
                    "swing_sync_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}