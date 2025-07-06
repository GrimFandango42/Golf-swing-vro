package com.swingsync.ai.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("handicap")
    val handicap: Float?,
    
    @SerializedName("dominant_hand")
    val dominantHand: HandType,
    
    @SerializedName("height_cm")
    val heightCm: Int?,
    
    @SerializedName("weight_kg")
    val weightKg: Float?,
    
    @SerializedName("experience_level")
    val experienceLevel: ExperienceLevel,
    
    @SerializedName("goals")
    val goals: List<String>,
    
    @SerializedName("preferred_clubs")
    val preferredClubs: List<String>,
    
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    
    @SerializedName("created_at")
    val createdAt: Date,
    
    @SerializedName("updated_at")
    val updatedAt: Date,
    
    @SerializedName("settings")
    val settings: UserSettings
)

enum class HandType {
    @SerializedName("right")
    RIGHT,
    
    @SerializedName("left")
    LEFT
}

enum class ExperienceLevel {
    @SerializedName("beginner")
    BEGINNER,
    
    @SerializedName("intermediate")
    INTERMEDIATE,
    
    @SerializedName("advanced")
    ADVANCED,
    
    @SerializedName("professional")
    PROFESSIONAL
}

data class UserSettings(
    @SerializedName("voice_coaching_enabled")
    val voiceCoachingEnabled: Boolean = true,
    
    @SerializedName("auto_record_enabled")
    val autoRecordEnabled: Boolean = false,
    
    @SerializedName("video_quality")
    val videoQuality: VideoQuality = VideoQuality.HIGH,
    
    @SerializedName("analysis_sensitivity")
    val analysisSensitivity: AnalysisSensitivity = AnalysisSensitivity.MEDIUM,
    
    @SerializedName("notification_enabled")
    val notificationEnabled: Boolean = true,
    
    @SerializedName("data_sync_enabled")
    val dataSyncEnabled: Boolean = true,
    
    @SerializedName("offline_mode")
    val offlineMode: Boolean = false
)

enum class VideoQuality {
    @SerializedName("low")
    LOW,
    
    @SerializedName("medium")
    MEDIUM,
    
    @SerializedName("high")
    HIGH
}

enum class AnalysisSensitivity {
    @SerializedName("low")
    LOW,
    
    @SerializedName("medium")
    MEDIUM,
    
    @SerializedName("high")
    HIGH
}