package com.swingsync.ai.achievements

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.celebration.AchievementMilestone
import com.swingsync.ai.celebration.CelebrationEngine
import com.swingsync.ai.detection.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AchievementSystem - Progressive milestone tracking and achievement management
 * 
 * This system creates a comprehensive achievement framework that motivates users
 * through progressive milestones, streaks, and skill-based challenges.
 * 
 * Features:
 * - Progressive skill-based achievements
 * - Streak tracking (consistency rewards)
 * - Milestone celebrations
 * - Rare achievement unlocks
 * - Social sharing integration
 * - Personalized achievement paths
 * - Performance analytics integration
 * 
 * Achievement Categories:
 * - Skill Mastery (technique improvements)
 * - Consistency (streak-based)
 * - Improvement (progress-based)
 * - Dedication (practice-based)
 * - Exploration (feature usage)
 * - Social (sharing and engagement)
 * 
 * The system adapts to user skill level and provides meaningful progression.
 */
@Singleton
class AchievementSystem @Inject constructor(
    private val context: Context,
    private val bestSwingDetector: BestSwingDetector,
    private val celebrationEngine: CelebrationEngine
) {
    
    companion object {
        private const val TAG = "AchievementSystem"
        
        // Achievement categories
        const val CATEGORY_SKILL = "skill_mastery"
        const val CATEGORY_CONSISTENCY = "consistency"
        const val CATEGORY_IMPROVEMENT = "improvement"
        const val CATEGORY_DEDICATION = "dedication"
        const val CATEGORY_EXPLORATION = "exploration"
        const val CATEGORY_SOCIAL = "social"
        
        // Achievement rarities
        const val RARITY_COMMON = "common"
        const val RARITY_UNCOMMON = "uncommon"
        const val RARITY_RARE = "rare"
        const val RARITY_EPIC = "epic"
        const val RARITY_LEGENDARY = "legendary"
        
        // Streak thresholds
        const val STREAK_BRONZE = 3
        const val STREAK_SILVER = 5
        const val STREAK_GOLD = 7
        const val STREAK_PLATINUM = 10
        const val STREAK_DIAMOND = 15
        
        // Score thresholds
        const val SCORE_GOOD = 0.65f
        const val SCORE_GREAT = 0.75f
        const val SCORE_EXCELLENT = 0.85f
        const val SCORE_PERFECT = 0.95f
    }
    
    // State management
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()
    
    private val _unlockedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val unlockedAchievements: StateFlow<List<Achievement>> = _unlockedAchievements.asStateFlow()
    
    private val _achievementProgress = MutableStateFlow<Map<String, AchievementProgress>>(emptyMap())
    val achievementProgress: StateFlow<Map<String, AchievementProgress>> = _achievementProgress.asStateFlow()
    
    private val _streakData = MutableStateFlow<Map<String, StreakData>>(emptyMap())
    val streakData: StateFlow<Map<String, StreakData>> = _streakData.asStateFlow()
    
    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()
    
    // Tracking data
    private val userProgress = mutableMapOf<String, Any>()
    private val streakCounters = mutableMapOf<String, Int>()
    private var lastSwingDate = 0L
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Initialize the achievement system
     */
    fun initialize(lifecycleOwner: LifecycleOwner) {
        // Load existing achievements
        loadAchievements()
        
        // Observe swing quality for achievement tracking
        lifecycleOwner.lifecycleScope.launch {
            bestSwingDetector.swingQuality.collect { quality ->
                trackSwingQuality(quality)
            }
        }
        
        // Observe best swings for achievement tracking
        lifecycleOwner.lifecycleScope.launch {
            bestSwingDetector.bestSwingDetected.collect { event ->
                event?.let { trackBestSwing(it) }
            }
        }
        
        // Observe improvement for achievement tracking
        lifecycleOwner.lifecycleScope.launch {
            bestSwingDetector.improvementDetected.collect { event ->
                event?.let { trackImprovement(it) }
            }
        }
        
        Log.d(TAG, "Achievement system initialized")
    }
    
    /**
     * Load all available achievements
     */
    private fun loadAchievements() {
        val allAchievements = mutableListOf<Achievement>()
        
        // Skill mastery achievements
        allAchievements.addAll(createSkillMasteryAchievements())
        
        // Consistency achievements
        allAchievements.addAll(createConsistencyAchievements())
        
        // Improvement achievements
        allAchievements.addAll(createImprovementAchievements())
        
        // Dedication achievements
        allAchievements.addAll(createDedicationAchievements())
        
        // Exploration achievements
        allAchievements.addAll(createExplorationAchievements())
        
        // Social achievements
        allAchievements.addAll(createSocialAchievements())
        
        _achievements.value = allAchievements
        
        Log.d(TAG, "Loaded ${allAchievements.size} achievements")
    }
    
    /**
     * Create skill mastery achievements
     */
    private fun createSkillMasteryAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_great_swing",
                name = "First Great Swing",
                description = "Hit your first great swing",
                category = CATEGORY_SKILL,
                rarity = RARITY_COMMON,
                iconUrl = "ic_first_great_swing",
                requirements = listOf(
                    AchievementRequirement("swing_quality", "great", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 50),
                    AchievementReward("badge", "great_swing_badge")
                )
            ),
            Achievement(
                id = "swing_master",
                name = "Swing Master",
                description = "Hit 10 excellent swings",
                category = CATEGORY_SKILL,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_swing_master",
                requirements = listOf(
                    AchievementRequirement("swing_quality", "excellent", 10)
                ),
                rewards = listOf(
                    AchievementReward("xp", 200),
                    AchievementReward("title", "Swing Master")
                )
            ),
            Achievement(
                id = "perfect_swing",
                name = "Perfect Swing",
                description = "Achieve a perfect swing (95%+ score)",
                category = CATEGORY_SKILL,
                rarity = RARITY_RARE,
                iconUrl = "ic_perfect_swing",
                requirements = listOf(
                    AchievementRequirement("swing_score", "0.95", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 500),
                    AchievementReward("trophy", "perfect_swing_trophy")
                )
            ),
            Achievement(
                id = "biomechanics_expert",
                name = "Biomechanics Expert",
                description = "Master biomechanical excellence",
                category = CATEGORY_SKILL,
                rarity = RARITY_EPIC,
                iconUrl = "ic_biomechanics_expert",
                requirements = listOf(
                    AchievementRequirement("biomechanics_score", "0.90", 5)
                ),
                rewards = listOf(
                    AchievementReward("xp", 1000),
                    AchievementReward("certification", "biomechanics_expert")
                )
            ),
            Achievement(
                id = "swing_legend",
                name = "Swing Legend",
                description = "Achieve legendary status with 100 excellent swings",
                category = CATEGORY_SKILL,
                rarity = RARITY_LEGENDARY,
                iconUrl = "ic_swing_legend",
                requirements = listOf(
                    AchievementRequirement("swing_quality", "excellent", 100)
                ),
                rewards = listOf(
                    AchievementReward("xp", 5000),
                    AchievementReward("legend_status", "swing_legend")
                )
            )
        )
    }
    
    /**
     * Create consistency achievements
     */
    private fun createConsistencyAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "consistent_performer",
                name = "Consistent Performer",
                description = "Hit 3 good swings in a row",
                category = CATEGORY_CONSISTENCY,
                rarity = RARITY_COMMON,
                iconUrl = "ic_consistent_performer",
                requirements = listOf(
                    AchievementRequirement("good_swing_streak", "3", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 100),
                    AchievementReward("badge", "consistency_badge")
                )
            ),
            Achievement(
                id = "streak_master",
                name = "Streak Master",
                description = "Maintain a 7-swing streak",
                category = CATEGORY_CONSISTENCY,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_streak_master",
                requirements = listOf(
                    AchievementRequirement("good_swing_streak", "7", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 300),
                    AchievementReward("title", "Streak Master")
                )
            ),
            Achievement(
                id = "unstoppable",
                name = "Unstoppable",
                description = "Achieve a 15-swing streak",
                category = CATEGORY_CONSISTENCY,
                rarity = RARITY_RARE,
                iconUrl = "ic_unstoppable",
                requirements = listOf(
                    AchievementRequirement("good_swing_streak", "15", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 750),
                    AchievementReward("special_effect", "unstoppable_aura")
                )
            ),
            Achievement(
                id = "daily_dedication",
                name = "Daily Dedication",
                description = "Practice 7 days in a row",
                category = CATEGORY_CONSISTENCY,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_daily_dedication",
                requirements = listOf(
                    AchievementRequirement("daily_streak", "7", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 400),
                    AchievementReward("badge", "daily_dedication_badge")
                )
            )
        )
    }
    
    /**
     * Create improvement achievements
     */
    private fun createImprovementAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "getting_better",
                name = "Getting Better",
                description = "Show 10% improvement in any area",
                category = CATEGORY_IMPROVEMENT,
                rarity = RARITY_COMMON,
                iconUrl = "ic_getting_better",
                requirements = listOf(
                    AchievementRequirement("improvement_percentage", "10", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 150),
                    AchievementReward("badge", "improvement_badge")
                )
            ),
            Achievement(
                id = "rapid_improvement",
                name = "Rapid Improvement",
                description = "Show 25% improvement in one session",
                category = CATEGORY_IMPROVEMENT,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_rapid_improvement",
                requirements = listOf(
                    AchievementRequirement("session_improvement", "25", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 400),
                    AchievementReward("title", "Fast Learner")
                )
            ),
            Achievement(
                id = "technique_master",
                name = "Technique Master",
                description = "Master all swing fundamentals",
                category = CATEGORY_IMPROVEMENT,
                rarity = RARITY_EPIC,
                iconUrl = "ic_technique_master",
                requirements = listOf(
                    AchievementRequirement("biomechanics_mastery", "true", 1),
                    AchievementRequirement("tempo_mastery", "true", 1),
                    AchievementRequirement("balance_mastery", "true", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 2000),
                    AchievementReward("master_title", "Technique Master")
                )
            )
        )
    }
    
    /**
     * Create dedication achievements
     */
    private fun createDedicationAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "committed_golfer",
                name = "Committed Golfer",
                description = "Complete 50 swing sessions",
                category = CATEGORY_DEDICATION,
                rarity = RARITY_COMMON,
                iconUrl = "ic_committed_golfer",
                requirements = listOf(
                    AchievementRequirement("swing_sessions", "50", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 250),
                    AchievementReward("badge", "dedication_badge")
                )
            ),
            Achievement(
                id = "swing_enthusiast",
                name = "Swing Enthusiast",
                description = "Record 500 swings",
                category = CATEGORY_DEDICATION,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_swing_enthusiast",
                requirements = listOf(
                    AchievementRequirement("total_swings", "500", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 600),
                    AchievementReward("title", "Swing Enthusiast")
                )
            ),
            Achievement(
                id = "practice_warrior",
                name = "Practice Warrior",
                description = "Practice for 30 days",
                category = CATEGORY_DEDICATION,
                rarity = RARITY_RARE,
                iconUrl = "ic_practice_warrior",
                requirements = listOf(
                    AchievementRequirement("practice_days", "30", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 1500),
                    AchievementReward("warrior_title", "Practice Warrior")
                )
            )
        )
    }
    
    /**
     * Create exploration achievements
     */
    private fun createExplorationAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "feature_explorer",
                name = "Feature Explorer",
                description = "Try all analysis features",
                category = CATEGORY_EXPLORATION,
                rarity = RARITY_COMMON,
                iconUrl = "ic_feature_explorer",
                requirements = listOf(
                    AchievementRequirement("features_used", "5", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 100),
                    AchievementReward("badge", "explorer_badge")
                )
            ),
            Achievement(
                id = "tech_savvy",
                name = "Tech Savvy",
                description = "Use AR and voice features",
                category = CATEGORY_EXPLORATION,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_tech_savvy",
                requirements = listOf(
                    AchievementRequirement("ar_usage", "true", 1),
                    AchievementRequirement("voice_usage", "true", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 300),
                    AchievementReward("title", "Tech Savvy")
                )
            )
        )
    }
    
    /**
     * Create social achievements
     */
    private fun createSocialAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "social_sharer",
                name = "Social Sharer",
                description = "Share your first swing",
                category = CATEGORY_SOCIAL,
                rarity = RARITY_COMMON,
                iconUrl = "ic_social_sharer",
                requirements = listOf(
                    AchievementRequirement("shares", "1", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 50),
                    AchievementReward("badge", "social_badge")
                )
            ),
            Achievement(
                id = "viral_swinger",
                name = "Viral Swinger",
                description = "Share 10 achievements",
                category = CATEGORY_SOCIAL,
                rarity = RARITY_UNCOMMON,
                iconUrl = "ic_viral_swinger",
                requirements = listOf(
                    AchievementRequirement("shares", "10", 1)
                ),
                rewards = listOf(
                    AchievementReward("xp", 500),
                    AchievementReward("title", "Viral Swinger")
                )
            )
        )
    }
    
    /**
     * Track swing quality for achievements
     */
    private fun trackSwingQuality(quality: SwingQuality) {
        Log.d(TAG, "Tracking swing quality: $quality")
        
        when (quality) {
            SwingQuality.GOOD -> {
                updateProgress("good_swings", 1)
                updateStreak("good_swing_streak", true)
            }
            SwingQuality.GREAT -> {
                updateProgress("great_swings", 1)
                updateProgress("good_swings", 1)
                updateStreak("good_swing_streak", true)
                updateStreak("great_swing_streak", true)
            }
            SwingQuality.EXCELLENT -> {
                updateProgress("excellent_swings", 1)
                updateProgress("great_swings", 1)
                updateProgress("good_swings", 1)
                updateStreak("good_swing_streak", true)
                updateStreak("great_swing_streak", true)
                updateStreak("excellent_swing_streak", true)
            }
            else -> {
                // Reset streaks for poor swings
                updateStreak("good_swing_streak", false)
                updateStreak("great_swing_streak", false)
                updateStreak("excellent_swing_streak", false)
            }
        }
        
        // Update total swings
        updateProgress("total_swings", 1)
        
        // Update daily streak
        updateDailyStreak()
        
        // Check for achievements
        checkAchievements()
    }
    
    /**
     * Track best swing events
     */
    private fun trackBestSwing(event: BestSwingEvent) {
        Log.d(TAG, "Tracking best swing event")
        
        // Track specific achievements
        if (event.isPersonalBest) {
            updateProgress("personal_bests", 1)
        }
        
        if (event.score >= SCORE_PERFECT) {
            updateProgress("perfect_swings", 1)
        }
        
        // Track score-based achievements
        updateProgress("swing_score_${event.score}", 1)
        
        checkAchievements()
    }
    
    /**
     * Track improvement events
     */
    private fun trackImprovement(event: ImprovementEvent) {
        Log.d(TAG, "Tracking improvement event")
        
        val improvementPercent = (event.improvementAmount * 100).toInt()
        
        updateProgress("improvement_events", 1)
        updateProgress("max_improvement", improvementPercent)
        
        // Track area-specific improvements
        updateProgress("${event.improvementArea}_improvement", improvementPercent)
        
        checkAchievements()
    }
    
    /**
     * Update progress for a specific metric
     */
    private fun updateProgress(metric: String, value: Int) {
        val currentValue = userProgress[metric] as? Int ?: 0
        val newValue = currentValue + value
        userProgress[metric] = newValue
        
        Log.d(TAG, "Updated $metric: $currentValue -> $newValue")
    }
    
    /**
     * Update streak counters
     */
    private fun updateStreak(streakType: String, success: Boolean) {
        val currentStreak = streakCounters[streakType] ?: 0
        val newStreak = if (success) currentStreak + 1 else 0
        
        streakCounters[streakType] = newStreak
        
        // Update max streak
        val maxStreakKey = "${streakType}_max"
        val maxStreak = userProgress[maxStreakKey] as? Int ?: 0
        if (newStreak > maxStreak) {
            userProgress[maxStreakKey] = newStreak
        }
        
        // Trigger streak celebrations
        if (newStreak > 0 && newStreak % 5 == 0) {
            celebrationEngine.triggerStreakCelebration(newStreak, streakType)
        }
        
        Log.d(TAG, "Updated streak $streakType: $currentStreak -> $newStreak")
    }
    
    /**
     * Update daily streak
     */
    private fun updateDailyStreak() {
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        val lastDay = lastSwingDate / (24 * 60 * 60 * 1000)
        
        if (today != lastDay) {
            if (today == lastDay + 1) {
                // Consecutive day
                val currentStreak = streakCounters["daily_streak"] ?: 0
                streakCounters["daily_streak"] = currentStreak + 1
            } else {
                // Streak broken
                streakCounters["daily_streak"] = 1
            }
            
            lastSwingDate = System.currentTimeMillis()
            
            // Update progress
            val dailyStreak = streakCounters["daily_streak"] ?: 0
            val maxDailyStreak = userProgress["daily_streak_max"] as? Int ?: 0
            if (dailyStreak > maxDailyStreak) {
                userProgress["daily_streak_max"] = dailyStreak
            }
        }
    }
    
    /**
     * Check for newly unlocked achievements
     */
    private fun checkAchievements() {
        val currentlyUnlocked = _unlockedAchievements.value
        val newlyUnlocked = mutableListOf<Achievement>()
        
        _achievements.value.forEach { achievement ->
            if (!currentlyUnlocked.contains(achievement)) {
                if (checkAchievementRequirements(achievement)) {
                    newlyUnlocked.add(achievement)
                    Log.d(TAG, "Achievement unlocked: ${achievement.name}")
                }
            }
        }
        
        if (newlyUnlocked.isNotEmpty()) {
            // Update unlocked achievements
            _unlockedAchievements.value = currentlyUnlocked + newlyUnlocked
            
            // Trigger celebrations for new achievements
            newlyUnlocked.forEach { achievement ->
                val milestone = AchievementMilestone(
                    id = achievement.id,
                    name = achievement.name,
                    description = achievement.description,
                    category = achievement.category,
                    rarity = achievement.rarity,
                    iconUrl = achievement.iconUrl
                )
                celebrationEngine.triggerMilestoneCelebration(milestone)
            }
        }
        
        // Update progress tracking
        updateAchievementProgress()
    }
    
    /**
     * Check if achievement requirements are met
     */
    private fun checkAchievementRequirements(achievement: Achievement): Boolean {
        return achievement.requirements.all { requirement ->
            when (requirement.type) {
                "swing_quality" -> {
                    val count = userProgress["${requirement.value}_swings"] as? Int ?: 0
                    count >= requirement.target.toInt()
                }
                "swing_score" -> {
                    val count = userProgress["swing_score_${requirement.value}"] as? Int ?: 0
                    count >= requirement.target.toInt()
                }
                "good_swing_streak" -> {
                    val maxStreak = userProgress["good_swing_streak_max"] as? Int ?: 0
                    maxStreak >= requirement.target.toInt()
                }
                "daily_streak" -> {
                    val maxStreak = userProgress["daily_streak_max"] as? Int ?: 0
                    maxStreak >= requirement.target.toInt()
                }
                "improvement_percentage" -> {
                    val maxImprovement = userProgress["max_improvement"] as? Int ?: 0
                    maxImprovement >= requirement.target.toInt()
                }
                "total_swings" -> {
                    val totalSwings = userProgress["total_swings"] as? Int ?: 0
                    totalSwings >= requirement.target.toInt()
                }
                "biomechanics_score" -> {
                    // This would require more complex tracking
                    false // Placeholder
                }
                else -> false
            }
        }
    }
    
    /**
     * Update achievement progress tracking
     */
    private fun updateAchievementProgress() {
        val progressMap = mutableMapOf<String, AchievementProgress>()
        
        _achievements.value.forEach { achievement ->
            val isUnlocked = _unlockedAchievements.value.contains(achievement)
            val progress = if (isUnlocked) {
                1.0f
            } else {
                calculateAchievementProgress(achievement)
            }
            
            progressMap[achievement.id] = AchievementProgress(
                achievementId = achievement.id,
                progress = progress,
                isUnlocked = isUnlocked,
                currentValue = getCurrentValueForAchievement(achievement),
                targetValue = getTargetValueForAchievement(achievement),
                lastUpdated = System.currentTimeMillis()
            )
        }
        
        _achievementProgress.value = progressMap
    }
    
    /**
     * Calculate progress for an achievement
     */
    private fun calculateAchievementProgress(achievement: Achievement): Float {
        val requirements = achievement.requirements
        if (requirements.isEmpty()) return 0f
        
        val progressValues = requirements.map { requirement ->
            val currentValue = getCurrentValueForRequirement(requirement)
            val targetValue = requirement.target.toFloat()
            (currentValue / targetValue).coerceIn(0f, 1f)
        }
        
        return progressValues.average().toFloat()
    }
    
    /**
     * Get current value for achievement requirement
     */
    private fun getCurrentValueForRequirement(requirement: AchievementRequirement): Float {
        return when (requirement.type) {
            "swing_quality" -> {
                (userProgress["${requirement.value}_swings"] as? Int ?: 0).toFloat()
            }
            "swing_score" -> {
                (userProgress["swing_score_${requirement.value}"] as? Int ?: 0).toFloat()
            }
            "good_swing_streak" -> {
                (userProgress["good_swing_streak_max"] as? Int ?: 0).toFloat()
            }
            "daily_streak" -> {
                (userProgress["daily_streak_max"] as? Int ?: 0).toFloat()
            }
            "total_swings" -> {
                (userProgress["total_swings"] as? Int ?: 0).toFloat()
            }
            else -> 0f
        }
    }
    
    /**
     * Get current value for achievement
     */
    private fun getCurrentValueForAchievement(achievement: Achievement): Int {
        // Return the current value for the first requirement (simplified)
        if (achievement.requirements.isEmpty()) return 0
        return getCurrentValueForRequirement(achievement.requirements.first()).toInt()
    }
    
    /**
     * Get target value for achievement
     */
    private fun getTargetValueForAchievement(achievement: Achievement): Int {
        // Return the target value for the first requirement (simplified)
        if (achievement.requirements.isEmpty()) return 0
        return achievement.requirements.first().target.toInt()
    }
    
    /**
     * Get achievement statistics
     */
    fun getAchievementStats(): AchievementStats {
        val unlocked = _unlockedAchievements.value
        val total = _achievements.value.size
        
        return AchievementStats(
            totalAchievements = total,
            unlockedAchievements = unlocked.size,
            completionPercentage = if (total > 0) (unlocked.size.toFloat() / total * 100) else 0f,
            rarityBreakdown = unlocked.groupBy { it.rarity }.mapValues { it.value.size },
            categoryBreakdown = unlocked.groupBy { it.category }.mapValues { it.value.size },
            totalXP = unlocked.sumOf { achievement ->
                achievement.rewards.filter { it.type == "xp" }.sumOf { it.value.toInt() }
            },
            currentStreaks = streakCounters.toMap(),
            maxStreaks = userProgress.filterKeys { it.endsWith("_max") }.mapValues { it.value as Int }
        )
    }
    
    /**
     * Get user progress summary
     */
    fun getUserProgress(): UserProgressSummary {
        return UserProgressSummary(
            totalSwings = userProgress["total_swings"] as? Int ?: 0,
            goodSwings = userProgress["good_swings"] as? Int ?: 0,
            greatSwings = userProgress["great_swings"] as? Int ?: 0,
            excellentSwings = userProgress["excellent_swings"] as? Int ?: 0,
            personalBests = userProgress["personal_bests"] as? Int ?: 0,
            currentStreaks = streakCounters.toMap(),
            maxStreaks = userProgress.filterKeys { it.endsWith("_max") }.mapValues { it.value as Int },
            improvementEvents = userProgress["improvement_events"] as? Int ?: 0,
            practiceSessionsCompleted = userProgress["swing_sessions"] as? Int ?: 0
        )
    }
    
    /**
     * Reset user progress (for testing or new users)
     */
    fun resetProgress() {
        userProgress.clear()
        streakCounters.clear()
        _unlockedAchievements.value = emptyList()
        _achievementProgress.value = emptyMap()
        lastSwingDate = 0L
        
        Log.d(TAG, "User progress reset")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        coroutineScope.cancel()
    }
}

// Data classes for achievement system
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String,
    val iconUrl: String,
    val requirements: List<AchievementRequirement>,
    val rewards: List<AchievementReward>
)

data class AchievementRequirement(
    val type: String,
    val value: String,
    val target: String
)

data class AchievementReward(
    val type: String,
    val value: String
)

data class AchievementProgress(
    val achievementId: String,
    val progress: Float,
    val isUnlocked: Boolean,
    val currentValue: Int,
    val targetValue: Int,
    val lastUpdated: Long
)

data class StreakData(
    val streakType: String,
    val currentStreak: Int,
    val maxStreak: Int,
    val lastUpdated: Long
)

data class UserStats(
    val totalSwings: Int = 0,
    val totalSessions: Int = 0,
    val totalPracticeTime: Long = 0,
    val averageScore: Float = 0f,
    val bestScore: Float = 0f,
    val improvementRate: Float = 0f,
    val consistencyRating: Float = 0f
)

data class AchievementStats(
    val totalAchievements: Int,
    val unlockedAchievements: Int,
    val completionPercentage: Float,
    val rarityBreakdown: Map<String, Int>,
    val categoryBreakdown: Map<String, Int>,
    val totalXP: Int,
    val currentStreaks: Map<String, Int>,
    val maxStreaks: Map<String, Int>
)

data class UserProgressSummary(
    val totalSwings: Int,
    val goodSwings: Int,
    val greatSwings: Int,
    val excellentSwings: Int,
    val personalBests: Int,
    val currentStreaks: Map<String, Int>,
    val maxStreaks: Map<String, Int>,
    val improvementEvents: Int,
    val practiceSessionsCompleted: Int
)