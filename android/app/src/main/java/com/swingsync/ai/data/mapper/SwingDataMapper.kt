package com.swingsync.ai.data.mapper

import com.swingsync.ai.data.local.entity.*
import com.swingsync.ai.data.remote.dto.*
import com.swingsync.ai.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper class to convert between different data layer representations.
 * This class handles the mapping between domain models, entities, and DTOs.
 */
@Singleton
class SwingDataMapper @Inject constructor() {
    
    // SwingSession mappings
    fun toDomain(entity: SwingSessionEntity): SwingSession {
        return SwingSession(
            sessionId = entity.sessionId,
            userId = entity.userId,
            clubUsed = entity.clubUsed,
            startTime = entity.startTime,
            endTime = entity.endTime,
            totalFrames = entity.totalFrames,
            fps = entity.fps,
            videoPath = entity.videoPath,
            isCompleted = entity.isCompleted,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    fun toEntity(domain: SwingSession): SwingSessionEntity {
        return SwingSessionEntity(
            sessionId = domain.sessionId,
            userId = domain.userId,
            clubUsed = domain.clubUsed,
            startTime = domain.startTime,
            endTime = domain.endTime,
            totalFrames = domain.totalFrames,
            fps = domain.fps,
            videoPath = domain.videoPath,
            isCompleted = domain.isCompleted,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    // SwingAnalysis mappings
    fun toDomain(entity: SwingAnalysisEntity): SwingAnalysis {
        return SwingAnalysis(
            analysisId = entity.analysisId,
            sessionId = entity.sessionId,
            analysisType = entity.analysisType,
            score = entity.score,
            strengths = entity.strengths,
            improvements = entity.improvements,
            feedback = entity.feedback,
            metrics = entity.metrics,
            swingPhases = emptyList(), // Would be mapped from separate entity/field
            analysisTimestamp = entity.analysisTimestamp,
            createdAt = entity.createdAt
        )
    }
    
    fun toEntity(domain: SwingAnalysis): SwingAnalysisEntity {
        return SwingAnalysisEntity(
            analysisId = domain.analysisId,
            sessionId = domain.sessionId,
            analysisType = domain.analysisType,
            score = domain.score,
            strengths = domain.strengths,
            improvements = domain.improvements,
            feedback = domain.feedback,
            metrics = domain.metrics,
            analysisTimestamp = domain.analysisTimestamp,
            createdAt = domain.createdAt
        )
    }
    
    fun toDomain(dto: SwingAnalysisResponseDto): SwingAnalysis {
        return SwingAnalysis(
            analysisId = dto.analysisId,
            sessionId = dto.sessionId,
            analysisType = "full", // Default type
            score = dto.overallScore,
            strengths = dto.strengths,
            improvements = dto.improvements,
            feedback = dto.feedback,
            metrics = dto.detailedMetrics,
            swingPhases = dto.swingPhases.map { toDomain(it) },
            analysisTimestamp = dto.analysisTimestamp,
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun toDomain(dto: SwingPhaseDto): SwingPhase {
        return SwingPhase(
            phaseName = dto.phaseName,
            startFrame = dto.startFrame,
            endFrame = dto.endFrame,
            durationMs = dto.durationMs,
            score = dto.score,
            feedback = dto.feedback
        )
    }
    
    // PoseDetection mappings
    fun toDomain(entity: PoseDetectionEntity): PoseDetection {
        return PoseDetection(
            detectionId = entity.detectionId,
            sessionId = entity.sessionId,
            frameNumber = entity.frameNumber,
            timestamp = entity.timestamp,
            keypoints = entity.keypoints.map { toDomain(it) },
            confidence = entity.confidence,
            poseLandmarks = entity.poseLandmarks.map { toDomain(it) },
            createdAt = entity.createdAt
        )
    }
    
    fun toEntity(domain: PoseDetection): PoseDetectionEntity {
        return PoseDetectionEntity(
            detectionId = domain.detectionId,
            sessionId = domain.sessionId,
            frameNumber = domain.frameNumber,
            timestamp = domain.timestamp,
            keypoints = domain.keypoints.map { toEntity(it) },
            confidence = domain.confidence,
            poseLandmarks = domain.poseLandmarks.map { toEntity(it) },
            createdAt = domain.createdAt
        )
    }
    
    fun toDomain(entity: KeypointEntity): Keypoint {
        return Keypoint(
            x = entity.x,
            y = entity.y,
            z = entity.z,
            visibility = entity.visibility,
            confidence = entity.confidence
        )
    }
    
    fun toEntity(domain: Keypoint): KeypointEntity {
        return KeypointEntity(
            x = domain.x,
            y = domain.y,
            z = domain.z,
            visibility = domain.visibility,
            confidence = domain.confidence
        )
    }
    
    fun toDomain(entity: LandmarkEntity): Landmark {
        return Landmark(
            x = entity.x,
            y = entity.y,
            z = entity.z,
            visibility = entity.visibility
        )
    }
    
    fun toEntity(domain: Landmark): LandmarkEntity {
        return LandmarkEntity(
            x = domain.x,
            y = domain.y,
            z = domain.z,
            visibility = domain.visibility
        )
    }
    
    // UserSettings mappings
    fun toDomain(entity: UserSettingsEntity): UserSettings {
        return UserSettings(
            userId = entity.userId,
            preferredClub = entity.preferredClub,
            difficultyLevel = entity.difficultyLevel,
            unitsSystem = entity.unitsSystem,
            voiceCoachingEnabled = entity.voiceCoachingEnabled,
            celebrationsEnabled = entity.celebrationsEnabled,
            autoSaveEnabled = entity.autoSaveEnabled,
            analysisNotificationsEnabled = entity.analysisNotificationsEnabled,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    fun toEntity(domain: UserSettings): UserSettingsEntity {
        return UserSettingsEntity(
            userId = domain.userId,
            preferredClub = domain.preferredClub,
            difficultyLevel = domain.difficultyLevel,
            unitsSystem = domain.unitsSystem,
            voiceCoachingEnabled = domain.voiceCoachingEnabled,
            celebrationsEnabled = domain.celebrationsEnabled,
            autoSaveEnabled = domain.autoSaveEnabled,
            analysisNotificationsEnabled = domain.analysisNotificationsEnabled,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    // UserProgress mappings
    fun toDomain(entity: UserProgressEntity): UserProgress {
        return UserProgress(
            progressId = entity.progressId,
            userId = entity.userId,
            metricName = entity.metricName,
            currentValue = entity.currentValue,
            bestValue = entity.bestValue,
            targetValue = entity.targetValue,
            trend = when (entity.trend) {
                "improving" -> ProgressTrend.IMPROVING
                "stable" -> ProgressTrend.STABLE
                "declining" -> ProgressTrend.DECLINING
                else -> ProgressTrend.STABLE
            },
            lastUpdated = entity.lastUpdated,
            createdAt = entity.createdAt
        )
    }
    
    fun toEntity(domain: UserProgress): UserProgressEntity {
        return UserProgressEntity(
            progressId = domain.progressId,
            userId = domain.userId,
            metricName = domain.metricName,
            currentValue = domain.currentValue,
            bestValue = domain.bestValue,
            targetValue = domain.targetValue,
            trend = when (domain.trend) {
                ProgressTrend.IMPROVING -> "improving"
                ProgressTrend.STABLE -> "stable"
                ProgressTrend.DECLINING -> "declining"
            },
            lastUpdated = domain.lastUpdated,
            createdAt = domain.createdAt
        )
    }
    
    // CoachingTip mappings
    fun toDomain(dto: CoachingTipDto): CoachingTip {
        return CoachingTip(
            tipId = dto.tipId,
            title = dto.title,
            content = dto.content,
            category = dto.category,
            difficultyLevel = dto.difficultyLevel,
            clubType = dto.clubType,
            imageUrl = dto.imageUrl,
            videoUrl = dto.videoUrl
        )
    }
    
    // SwingFeedback mappings
    fun toDomain(dto: SwingFeedbackResponseDto): SwingFeedback {
        return SwingFeedback(
            analysisId = dto.analysisId,
            feedbackText = dto.feedbackText,
            tips = dto.tips,
            drills = dto.drills.map { toDomain(it) },
            nextSteps = dto.nextSteps,
            updatedAt = dto.updatedAt
        )
    }
    
    fun toDomain(dto: DrillDto): Drill {
        return Drill(
            drillId = dto.drillId,
            name = dto.name,
            description = dto.description,
            difficulty = dto.difficulty,
            durationMinutes = dto.durationMinutes,
            videoUrl = dto.videoUrl
        )
    }
    
    // Achievement mappings
    fun toDomain(dto: AchievementDto): Achievement {
        return Achievement(
            achievementId = dto.achievementId,
            name = dto.name,
            description = dto.description,
            iconUrl = dto.iconUrl,
            unlockedAt = dto.unlockedAt,
            progress = dto.progress,
            maxProgress = dto.maxProgress
        )
    }
    
    // Leaderboard mappings
    fun toDomain(dto: LeaderboardResponseDto): Leaderboard {
        return Leaderboard(
            category = dto.category,
            timeframe = dto.timeframe,
            entries = dto.entries.map { toDomain(it) },
            userRank = dto.userRank,
            totalParticipants = dto.totalParticipants
        )
    }
    
    fun toDomain(dto: LeaderboardEntryDto): LeaderboardEntry {
        return LeaderboardEntry(
            rank = dto.rank,
            userId = dto.userId,
            username = dto.username,
            score = dto.score,
            sessionsCount = dto.sessionsCount
        )
    }
}