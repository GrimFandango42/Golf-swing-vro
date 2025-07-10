package com.golfswing.vro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.golfswing.vro.data.model.BiomechanicsData
import com.golfswing.vro.data.model.SwingPhase
import com.golfswing.vro.data.model.SwingScore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "swing_analyses")
@TypeConverters(Converters::class)
data class SwingAnalysisEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val videoPath: String,
    val duration: Long,
    val phases: List<SwingPhase>,
    val biomechanics: BiomechanicsData,
    val score: SwingScore,
    val overallScore: Float, // Denormalized for easy querying
    val recommendations: List<String>,
    val isProcessed: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val handicap: Float?,
    val dominantHand: String, // "LEFT" or "RIGHT"
    val height: Float?, // in cm
    val weight: Float?, // in kg
    val experienceLevel: String, // "BEGINNER", "INTERMEDIATE", "ADVANCED", "PROFESSIONAL"
    val preferredClubs: List<String>,
    val goals: List<String>,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

@Entity(tableName = "swing_sessions")
data class SwingSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val location: String?,
    val weather: String?,
    val temperature: Float?,
    val humidity: Float?,
    val windSpeed: Float?,
    val clubUsed: String?,
    val notes: String?,
    val startTime: Date,
    val endTime: Date?,
    val swingCount: Int = 0
)

@Entity(tableName = "practice_goals")
data class PracticeGoalEntity(
    @PrimaryKey val goalId: String,
    val userId: String,
    val title: String,
    val description: String,
    val targetMetric: String,
    val targetValue: Float,
    val currentValue: Float,
    val isCompleted: Boolean = false,
    val createdAt: Date = Date(),
    val targetDate: Date?
)

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromSwingPhaseList(value: List<SwingPhase>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toSwingPhaseList(value: String): List<SwingPhase> {
        return gson.fromJson(value, object : TypeToken<List<SwingPhase>>() {}.type)
    }
    
    @TypeConverter
    fun fromBiomechanicsData(value: BiomechanicsData): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toBiomechanicsData(value: String): BiomechanicsData {
        return gson.fromJson(value, BiomechanicsData::class.java)
    }
    
    @TypeConverter
    fun fromSwingScore(value: SwingScore): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toSwingScore(value: String): SwingScore {
        return gson.fromJson(value, SwingScore::class.java)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
    }
    
    @TypeConverter
    fun fromDate(value: Date): Long {
        return value.time
    }
    
    @TypeConverter
    fun toDate(value: Long): Date {
        return Date(value)
    }
}