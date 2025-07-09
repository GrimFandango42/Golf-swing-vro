package com.swingsync.ai.domain.model

/**
 * Domain models for the swing analysis application.
 * These models represent the core business entities without any framework dependencies.
 */

data class SwingSession(
    val sessionId: String,
    val userId: String,
    val clubUsed: String,
    val startTime: Long,
    val endTime: Long?,
    val totalFrames: Int,
    val fps: Float,
    val videoPath: String?,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class SwingAnalysis(
    val analysisId: String,
    val sessionId: String,
    val analysisType: String,
    val score: Float,
    val strengths: List<String>,
    val improvements: List<String>,
    val feedback: List<String>,
    val metrics: Map<String, Float>,
    val swingPhases: List<SwingPhase>,
    val analysisTimestamp: Long,
    val createdAt: Long
)

data class SwingPhase(
    val phaseName: String,
    val startFrame: Int,
    val endFrame: Int,
    val durationMs: Long,
    val score: Float,
    val feedback: String?
)

data class PoseDetection(
    val detectionId: String,
    val sessionId: String,
    val frameNumber: Int,
    val timestamp: Long,
    val keypoints: List<Keypoint>,
    val confidence: Float,
    val poseLandmarks: List<Landmark>,
    val createdAt: Long
)

data class Keypoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
    val confidence: Float
)

data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
)

data class UserSettings(
    val userId: String,
    val preferredClub: String,
    val difficultyLevel: String,
    val unitsSystem: String,
    val voiceCoachingEnabled: Boolean,
    val celebrationsEnabled: Boolean,
    val autoSaveEnabled: Boolean,
    val analysisNotificationsEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class UserProgress(
    val progressId: String,
    val userId: String,
    val metricName: String,
    val currentValue: Float,
    val bestValue: Float,
    val targetValue: Float?,
    val trend: ProgressTrend,
    val lastUpdated: Long,
    val createdAt: Long
)

enum class ProgressTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

data class CoachingTip(
    val tipId: String,
    val title: String,
    val content: String,
    val category: String,
    val difficultyLevel: String,
    val clubType: String?,
    val imageUrl: String?,
    val videoUrl: String?
)

data class Achievement(
    val achievementId: String,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val unlockedAt: Long?,
    val progress: Float,
    val maxProgress: Float
)

data class SwingFeedback(
    val analysisId: String,
    val feedbackText: String,
    val tips: List<String>,
    val drills: List<Drill>,
    val nextSteps: List<String>,
    val updatedAt: Long
)

data class Drill(
    val drillId: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val durationMinutes: Int,
    val videoUrl: String?
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val score: Float,
    val sessionsCount: Int
)

data class Leaderboard(
    val category: String,
    val timeframe: String,
    val entries: List<LeaderboardEntry>,
    val userRank: Int?,
    val totalParticipants: Int
)