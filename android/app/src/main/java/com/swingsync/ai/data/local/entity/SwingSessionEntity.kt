package com.swingsync.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.swingsync.ai.data.local.converter.Converters

/**
 * Entity representing a golf swing session in the local database.
 */
@Entity(tableName = "swing_sessions")
@TypeConverters(Converters::class)
data class SwingSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "club_used")
    val clubUsed: String,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    
    @ColumnInfo(name = "end_time")
    val endTime: Long?,
    
    @ColumnInfo(name = "total_frames")
    val totalFrames: Int,
    
    @ColumnInfo(name = "fps")
    val fps: Float,
    
    @ColumnInfo(name = "video_path")
    val videoPath: String?,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing swing analysis results.
 */
@Entity(
    tableName = "swing_analyses",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SwingSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converters::class)
data class SwingAnalysisEntity(
    @PrimaryKey
    @ColumnInfo(name = "analysis_id")
    val analysisId: String,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "analysis_type")
    val analysisType: String, // "pose", "swing_plane", "tempo", etc.
    
    @ColumnInfo(name = "score")
    val score: Float,
    
    @ColumnInfo(name = "strengths")
    val strengths: List<String>,
    
    @ColumnInfo(name = "improvements")
    val improvements: List<String>,
    
    @ColumnInfo(name = "feedback")
    val feedback: List<String>,
    
    @ColumnInfo(name = "metrics")
    val metrics: Map<String, Float>,
    
    @ColumnInfo(name = "analysis_timestamp")
    val analysisTimestamp: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing pose detection results for a frame.
 */
@Entity(
    tableName = "pose_detections",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SwingSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converters::class)
data class PoseDetectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "detection_id")
    val detectionId: String,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "frame_number")
    val frameNumber: Int,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "keypoints")
    val keypoints: List<KeypointEntity>,
    
    @ColumnInfo(name = "confidence")
    val confidence: Float,
    
    @ColumnInfo(name = "pose_landmarks")
    val poseLandmarks: List<LandmarkEntity>,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a keypoint in pose detection.
 */
data class KeypointEntity(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
    val confidence: Float
)

/**
 * Entity representing a landmark in pose detection.
 */
data class LandmarkEntity(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
)

/**
 * Entity representing user settings and preferences.
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "preferred_club")
    val preferredClub: String,
    
    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String,
    
    @ColumnInfo(name = "units_system")
    val unitsSystem: String, // "metric" or "imperial"
    
    @ColumnInfo(name = "voice_coaching_enabled")
    val voiceCoachingEnabled: Boolean,
    
    @ColumnInfo(name = "celebrations_enabled")
    val celebrationsEnabled: Boolean,
    
    @ColumnInfo(name = "auto_save_enabled")
    val autoSaveEnabled: Boolean,
    
    @ColumnInfo(name = "analysis_notifications_enabled")
    val analysisNotificationsEnabled: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing user progress and achievements.
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "progress_id")
    val progressId: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "metric_name")
    val metricName: String,
    
    @ColumnInfo(name = "current_value")
    val currentValue: Float,
    
    @ColumnInfo(name = "best_value")
    val bestValue: Float,
    
    @ColumnInfo(name = "target_value")
    val targetValue: Float?,
    
    @ColumnInfo(name = "trend")
    val trend: String, // "improving", "stable", "declining"
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)