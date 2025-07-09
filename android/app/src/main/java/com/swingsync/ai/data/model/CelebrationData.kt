package com.swingsync.ai.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * Data models for the celebration and achievement system
 * These classes handle the persistence and communication of celebration preferences,
 * achievement data, and user progress tracking.
 */

@Parcelize
data class UserProfile(
    val userId: String,
    val username: String,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val skillLevel: SkillLevel = SkillLevel.BEGINNER,
    val preferredClubs: List<String> = emptyList(),
    val celebrationPreferences: CelebrationPreferences = CelebrationPreferences(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class CelebrationPreferences(
    val personalityType: String = "dynamic", // dynamic, elegant, playful, minimalist, classic, futuristic
    val enableAnimations: Boolean = true,
    val enableSound: Boolean = true,
    val enableHaptics: Boolean = true,
    val enableVisualEffects: Boolean = true,
    val celebrationIntensity: Float = 1.0f, // 0.0 to 2.0
    val autoShare: Boolean = false,
    val preferredSocialPlatforms: List<String> = emptyList(),
    val celebrationDuration: CelebrationDuration = CelebrationDuration.NORMAL,
    val enableStreakNotifications: Boolean = true,
    val enableAchievementNotifications: Boolean = true,
    val enableImprovementNotifications: Boolean = true
) : Parcelable

@Parcelize
data class UserAchievementProgress(
    val userId: String,
    val achievementId: String,
    val progress: Float, // 0.0 to 1.0
    val currentValue: Int,
    val targetValue: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val milestones: List<AchievementMilestone> = emptyList()
) : Parcelable

@Parcelize
data class AchievementMilestone(
    val id: String,
    val name: String,
    val description: String,
    val threshold: Int,
    val rewardType: String,
    val rewardValue: String,
    val iconUrl: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
) : Parcelable

@Parcelize
data class UserStreak(
    val userId: String,
    val streakType: String, // "daily_practice", "good_swings", "great_swings", etc.
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActivityDate: Long,
    val streakStartDate: Long,
    val milestones: List<StreakMilestone> = emptyList()
) : Parcelable

@Parcelize
data class StreakMilestone(
    val id: String,
    val streakCount: Int,
    val name: String,
    val description: String,
    val rewardType: String,
    val rewardValue: String,
    val isAchieved: Boolean = false,
    val achievedAt: Long? = null
) : Parcelable

@Parcelize
data class CelebrationHistory(
    val id: String,
    val userId: String,
    val celebrationType: String, // "best_swing", "improvement", "achievement", "streak"
    val celebrationLevel: String, // "encouraging", "good", "great", "excellence", "epic", "legendary"
    val trigger: String, // What triggered the celebration
    val title: String,
    val description: String,
    val metadata: Map<String, String> = emptyMap(),
    val sharedToSocial: Boolean = false,
    val sharedPlatforms: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class SwingSessionData(
    val sessionId: String,
    val userId: String,
    val clubUsed: String,
    val sessionDate: Long,
    val totalSwings: Int,
    val goodSwings: Int,
    val greatSwings: Int,
    val excellentSwings: Int,
    val averageScore: Float,
    val bestScore: Float,
    val improvementAreas: List<String> = emptyList(),
    val achievements: List<String> = emptyList(), // Achievement IDs unlocked in this session
    val celebrations: List<String> = emptyList(), // Celebration IDs triggered in this session
    val notes: String? = null,
    val weatherConditions: String? = null,
    val location: String? = null
) : Parcelable

@Parcelize
data class PersonalBest(
    val userId: String,
    val category: String, // "overall", "biomechanics", "tempo", "balance", "power", "precision"
    val clubType: String,
    val score: Float,
    val sessionId: String,
    val achievedAt: Long,
    val previousBest: Float? = null,
    val improvementAmount: Float? = null,
    val isShared: Boolean = false
) : Parcelable

@Parcelize
data class SocialShareData(
    val id: String,
    val userId: String,
    val contentType: String, // "achievement", "best_swing", "improvement", "streak"
    val contentId: String, // ID of the content being shared
    val platform: String, // "instagram", "twitter", "facebook", etc.
    val imageUrl: String? = null,
    val title: String,
    val description: String,
    val hashtags: List<String> = emptyList(),
    val shareUrl: String? = null,
    val isPublic: Boolean = true,
    val engagementMetrics: EngagementMetrics? = null,
    val sharedAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class EngagementMetrics(
    val views: Int = 0,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val saves: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class NotificationSettings(
    val userId: String,
    val enablePushNotifications: Boolean = true,
    val enableEmailNotifications: Boolean = false,
    val achievementNotifications: Boolean = true,
    val improvementNotifications: Boolean = true,
    val streakNotifications: Boolean = true,
    val practiceReminders: Boolean = true,
    val socialNotifications: Boolean = true,
    val challengeNotifications: Boolean = true,
    val newsAndUpdates: Boolean = true,
    val quietHoursStart: String? = null, // "22:00"
    val quietHoursEnd: String? = null, // "08:00"
    val notificationFrequency: NotificationFrequency = NotificationFrequency.IMMEDIATE
) : Parcelable

@Parcelize
data class UserStatistics(
    val userId: String,
    val totalSwings: Int = 0,
    val totalSessions: Int = 0,
    val totalPracticeTime: Long = 0, // in milliseconds
    val averageScore: Float = 0f,
    val bestScore: Float = 0f,
    val improvementRate: Float = 0f,
    val consistencyRating: Float = 0f,
    val favoriteClub: String? = null,
    val strongestArea: String? = null,
    val improvementArea: String? = null,
    val totalAchievements: Int = 0,
    val totalCelebrations: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val lastPracticeDate: Long? = null,
    val memberSince: Long = System.currentTimeMillis(),
    val level: Int = 1,
    val experiencePoints: Int = 0,
    val nextLevelXP: Int = 100
) : Parcelable

@Parcelize
data class Challenge(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // "daily", "weekly", "monthly", "special"
    val difficulty: ChallengeDifficulty,
    val requirements: List<ChallengeRequirement>,
    val rewards: List<ChallengeReward>,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true,
    val participantCount: Int = 0,
    val completionRate: Float = 0f,
    val iconUrl: String? = null,
    val bannerUrl: String? = null
) : Parcelable

@Parcelize
data class ChallengeRequirement(
    val type: String, // "swing_count", "score_threshold", "streak_days", etc.
    val target: Int,
    val description: String
) : Parcelable

@Parcelize
data class ChallengeReward(
    val type: String, // "xp", "badge", "title", "unlock"
    val value: String,
    val description: String
) : Parcelable

@Parcelize
data class UserChallengeProgress(
    val userId: String,
    val challengeId: String,
    val progress: Map<String, Int> = emptyMap(), // requirement type -> current value
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val rewardsClaimed: Boolean = false,
    val startedAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class Leaderboard(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // "global", "friends", "local", "club"
    val metric: String, // "average_score", "total_swings", "streak", etc.
    val timeFrame: String, // "daily", "weekly", "monthly", "all_time"
    val entries: List<LeaderboardEntry> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val profileImageUrl: String? = null,
    val rank: Int,
    val score: Float,
    val change: Int = 0, // position change from last update
    val isCurrentUser: Boolean = false
) : Parcelable

// Enums for type safety
enum class SkillLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT,
    PROFESSIONAL
}

enum class CelebrationDuration {
    BRIEF,      // 1-2 seconds
    NORMAL,     // 3-4 seconds
    EXTENDED    // 5+ seconds
}

enum class NotificationFrequency {
    IMMEDIATE,
    HOURLY,
    DAILY,
    WEEKLY,
    NEVER
}

enum class ChallengeDifficulty {
    EASY,
    MEDIUM,
    HARD,
    EXPERT,
    LEGENDARY
}

// Helper extensions
fun UserProfile.isNewUser(): Boolean {
    return System.currentTimeMillis() - createdAt < 7 * 24 * 60 * 60 * 1000 // 7 days
}

fun UserStreak.isActive(): Boolean {
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000
    return now - lastActivityDate <= oneDayMs
}

fun UserStatistics.getLevel(): Int {
    return when {
        experiencePoints < 100 -> 1
        experiencePoints < 300 -> 2
        experiencePoints < 600 -> 3
        experiencePoints < 1000 -> 4
        experiencePoints < 1500 -> 5
        experiencePoints < 2100 -> 6
        experiencePoints < 2800 -> 7
        experiencePoints < 3600 -> 8
        experiencePoints < 4500 -> 9
        experiencePoints < 5500 -> 10
        else -> 10 + (experiencePoints - 5500) / 1000
    }
}

fun UserStatistics.getNextLevelXP(): Int {
    val currentLevel = getLevel()
    return when (currentLevel) {
        1 -> 100
        2 -> 300
        3 -> 600
        4 -> 1000
        5 -> 1500
        6 -> 2100
        7 -> 2800
        8 -> 3600
        9 -> 4500
        10 -> 5500
        else -> 5500 + (currentLevel - 10) * 1000
    }
}

fun Challenge.isExpired(): Boolean {
    return System.currentTimeMillis() > endDate
}

fun Challenge.isUpcoming(): Boolean {
    return System.currentTimeMillis() < startDate
}