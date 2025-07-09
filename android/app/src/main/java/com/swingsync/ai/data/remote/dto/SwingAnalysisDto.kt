package com.swingsync.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for swing analysis API communication.
 */

data class SwingAnalysisRequestDto(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("club_type")
    val clubType: String,
    @SerializedName("pose_data")
    val poseData: List<PoseDataDto>,
    @SerializedName("video_metadata")
    val videoMetadata: VideoMetadataDto?,
    @SerializedName("analysis_type")
    val analysisType: String = "full"
)

data class SwingAnalysisResponseDto(
    @SerializedName("analysis_id")
    val analysisId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("overall_score")
    val overallScore: Float,
    @SerializedName("strengths")
    val strengths: List<String>,
    @SerializedName("improvements")
    val improvements: List<String>,
    @SerializedName("feedback")
    val feedback: List<String>,
    @SerializedName("detailed_metrics")
    val detailedMetrics: Map<String, Float>,
    @SerializedName("swing_phases")
    val swingPhases: List<SwingPhaseDto>,
    @SerializedName("analysis_timestamp")
    val analysisTimestamp: Long,
    @SerializedName("processing_time_ms")
    val processingTimeMs: Long
)

data class PoseDataDto(
    @SerializedName("frame_number")
    val frameNumber: Int,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("keypoints")
    val keypoints: List<KeypointDto>,
    @SerializedName("confidence")
    val confidence: Float
)

data class KeypointDto(
    @SerializedName("x")
    val x: Float,
    @SerializedName("y")
    val y: Float,
    @SerializedName("z")
    val z: Float,
    @SerializedName("visibility")
    val visibility: Float,
    @SerializedName("confidence")
    val confidence: Float
)

data class VideoMetadataDto(
    @SerializedName("duration_ms")
    val durationMs: Long,
    @SerializedName("fps")
    val fps: Float,
    @SerializedName("resolution")
    val resolution: String,
    @SerializedName("file_size_bytes")
    val fileSizeBytes: Long
)

data class SwingPhaseDto(
    @SerializedName("phase_name")
    val phaseName: String,
    @SerializedName("start_frame")
    val startFrame: Int,
    @SerializedName("end_frame")
    val endFrame: Int,
    @SerializedName("duration_ms")
    val durationMs: Long,
    @SerializedName("score")
    val score: Float,
    @SerializedName("feedback")
    val feedback: String?
)

data class VideoUploadResponseDto(
    @SerializedName("upload_id")
    val uploadId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("video_url")
    val videoUrl: String?,
    @SerializedName("processing_status")
    val processingStatus: String
)

data class SwingFeedbackResponseDto(
    @SerializedName("analysis_id")
    val analysisId: String,
    @SerializedName("feedback_text")
    val feedbackText: String,
    @SerializedName("tips")
    val tips: List<String>,
    @SerializedName("drills")
    val drills: List<DrillDto>,
    @SerializedName("next_steps")
    val nextSteps: List<String>,
    @SerializedName("updated_at")
    val updatedAt: Long
)

data class DrillDto(
    @SerializedName("drill_id")
    val drillId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("difficulty")
    val difficulty: String,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    @SerializedName("video_url")
    val videoUrl: String?
)

data class UserFeedbackDto(
    @SerializedName("analysis_id")
    val analysisId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("comment")
    val comment: String?,
    @SerializedName("helpful")
    val helpful: Boolean,
    @SerializedName("feedback_type")
    val feedbackType: String,
    @SerializedName("timestamp")
    val timestamp: Long
)

data class CoachingTipDto(
    @SerializedName("tip_id")
    val tipId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("difficulty_level")
    val difficultyLevel: String,
    @SerializedName("club_type")
    val clubType: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("video_url")
    val videoUrl: String?
)

data class UserProgressDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("metrics")
    val metrics: Map<String, Float>,
    @SerializedName("achievements")
    val achievements: List<String>,
    @SerializedName("total_sessions")
    val totalSessions: Int,
    @SerializedName("average_score")
    val averageScore: Float,
    @SerializedName("last_updated")
    val lastUpdated: Long
)

data class LeaderboardResponseDto(
    @SerializedName("category")
    val category: String,
    @SerializedName("timeframe")
    val timeframe: String,
    @SerializedName("entries")
    val entries: List<LeaderboardEntryDto>,
    @SerializedName("user_rank")
    val userRank: Int?,
    @SerializedName("total_participants")
    val totalParticipants: Int
)

data class LeaderboardEntryDto(
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("score")
    val score: Float,
    @SerializedName("sessions_count")
    val sessionsCount: Int
)

data class AchievementDto(
    @SerializedName("achievement_id")
    val achievementId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("icon_url")
    val iconUrl: String?,
    @SerializedName("unlocked_at")
    val unlockedAt: Long?,
    @SerializedName("progress")
    val progress: Float,
    @SerializedName("max_progress")
    val maxProgress: Float
)

data class SwingHistoryResponseDto(
    @SerializedName("swings")
    val swings: List<SwingHistoryEntryDto>,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)

data class SwingHistoryEntryDto(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("analysis_id")
    val analysisId: String?,
    @SerializedName("club_type")
    val clubType: String,
    @SerializedName("score")
    val score: Float,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("is_favorite")
    val isFavorite: Boolean,
    @SerializedName("video_thumbnail")
    val videoThumbnail: String?
)

data class FavoriteSwingDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("analysis_id")
    val analysisId: String?
)

data class UserSettingsDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("preferred_club")
    val preferredClub: String,
    @SerializedName("difficulty_level")
    val difficultyLevel: String,
    @SerializedName("units_system")
    val unitsSystem: String,
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean,
    @SerializedName("voice_coaching_enabled")
    val voiceCoachingEnabled: Boolean,
    @SerializedName("celebrations_enabled")
    val celebrationsEnabled: Boolean,
    @SerializedName("auto_save_enabled")
    val autoSaveEnabled: Boolean
)