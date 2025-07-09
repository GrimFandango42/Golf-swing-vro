package com.golfswing.vro.pixel.coaching

import android.content.Context
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System for celebrating achievements and milestones with joy!
 * Makes practice sessions fun and rewarding
 */
@Singleton
class CelebrationSystem @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val soundPool = SoundPool.Builder().setMaxStreams(3).build()
    
    private val _currentCelebration = MutableStateFlow<CelebrationEvent?>(null)
    val currentCelebration: StateFlow<CelebrationEvent?> = _currentCelebration.asStateFlow()
    
    // Track achievements
    private val achievements = mutableSetOf<Achievement>()
    private var totalGreatSwings = 0
    private var bestScore = 0f
    private var currentStreak = 0
    private var longestStreak = 0
    private var totalPracticeTime = 0L
    
    // Celebration sounds (would be loaded from resources)
    private val soundEffects = mapOf(
        CelebrationEvent.GREAT_SWING to "cheer_short",
        CelebrationEvent.PERFECT_SWING to "cheer_big",
        CelebrationEvent.CONSISTENCY_STREAK to "applause",
        CelebrationEvent.PERSONAL_BEST to "fanfare",
        CelebrationEvent.MILESTONE_REACHED to "achievement",
        CelebrationEvent.IMPROVEMENT_DETECTED to "success"
    )
    
    enum class CelebrationEvent {
        GREAT_SWING,           // Single great swing (8+ score)
        PERFECT_SWING,         // Near perfect swing (9.5+ score)
        CONSISTENCY_STREAK,    // Multiple good swings in a row
        PERSONAL_BEST,         // New high score
        MILESTONE_REACHED,     // 10, 25, 50, 100 swings etc
        IMPROVEMENT_DETECTED,  // Better than average
        FIRST_SWING,          // Very first swing ever
        COMEBACK,             // Good swing after struggling
        PRACTICE_MILESTONE,    // Time-based achievements
        TECHNIQUE_MASTERY     // Specific technique improvement
    }
    
    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val unlockedAt: Long = System.currentTimeMillis()
    )
    
    data class CelebrationConfig(
        val event: CelebrationEvent,
        val title: String,
        val subtitle: String? = null,
        val duration: Long = 3000L,
        val hapticPattern: LongArray? = null,
        val soundEnabled: Boolean = true,
        val particles: ParticleEffect? = null,
        val animation: AnimationType = AnimationType.BURST
    )
    
    enum class AnimationType {
        BURST,      // Explosion of particles
        RISE,       // Floating up
        FIREWORKS,  // Fireworks effect
        CONFETTI,   // Falling confetti
        SPARKLE,    // Twinkling stars
        PULSE       // Pulsing glow
    }
    
    data class ParticleEffect(
        val type: ParticleType,
        val count: Int = 20,
        val colors: List<String> = listOf("#FFD700", "#FFA500", "#FF6347"),
        val duration: Long = 2000L
    )
    
    enum class ParticleType {
        STARS, CIRCLES, HEARTS, GOLF_BALLS, PLUS_SIGNS, CHECKMARKS
    }
    
    /**
     * Trigger a celebration for an achievement
     */
    fun triggerCelebration(event: CelebrationEvent, customMessage: String? = null) {
        val config = getCelebrationConfig(event, customMessage)
        
        scope.launch {
            // Update celebration state for UI
            _currentCelebration.value = event
            
            // Haptic feedback
            if (config.hapticPattern != null) {
                playHapticFeedback(config.hapticPattern)
            }
            
            // Sound effect
            if (config.soundEnabled) {
                playCelebrationSound(event)
            }
            
            // Track achievement
            trackAchievement(event)
            
            // Clear celebration after duration
            kotlinx.coroutines.delay(config.duration)
            _currentCelebration.value = null
        }
    }
    
    /**
     * Get celebration configuration for each event type
     */
    private fun getCelebrationConfig(event: CelebrationEvent, customMessage: String?): CelebrationConfig {
        return when (event) {
            CelebrationEvent.GREAT_SWING -> CelebrationConfig(
                event = event,
                title = customMessage ?: "Great Swing! 🎯",
                subtitle = "Keep it up!",
                hapticPattern = longArrayOf(0, 50, 50, 50),
                particles = ParticleEffect(ParticleType.STARS, count = 15),
                animation = AnimationType.BURST
            )
            
            CelebrationEvent.PERFECT_SWING -> CelebrationConfig(
                event = event,
                title = customMessage ?: "PERFECT! 🏆",
                subtitle = "That was tour quality!",
                hapticPattern = longArrayOf(0, 100, 50, 100, 50, 100),
                particles = ParticleEffect(ParticleType.STARS, count = 30),
                animation = AnimationType.FIREWORKS
            )
            
            CelebrationEvent.CONSISTENCY_STREAK -> CelebrationConfig(
                event = event,
                title = customMessage ?: "On Fire! 🔥",
                subtitle = "${currentStreak} great swings in a row!",
                hapticPattern = longArrayOf(0, 200, 100, 200),
                particles = ParticleEffect(ParticleType.CIRCLES, count = 25),
                animation = AnimationType.CONFETTI
            )
            
            CelebrationEvent.PERSONAL_BEST -> CelebrationConfig(
                event = event,
                title = customMessage ?: "NEW PERSONAL BEST! 🌟",
                subtitle = "You're improving!",
                hapticPattern = longArrayOf(0, 300, 100, 100, 100, 100),
                particles = ParticleEffect(
                    type = ParticleType.GOLF_BALLS,
                    count = 20,
                    colors = listOf("#FFD700", "#C0C0C0", "#CD7F32")
                ),
                animation = AnimationType.FIREWORKS,
                duration = 4000L
            )
            
            CelebrationEvent.MILESTONE_REACHED -> CelebrationConfig(
                event = event,
                title = customMessage ?: "Milestone! 🎉",
                subtitle = getLatestMilestone(),
                hapticPattern = longArrayOf(0, 150, 75, 150, 75, 150),
                particles = ParticleEffect(ParticleType.CHECKMARKS, count = 20),
                animation = AnimationType.RISE
            )
            
            CelebrationEvent.IMPROVEMENT_DETECTED -> CelebrationConfig(
                event = event,
                title = customMessage ?: "Improving! 📈",
                subtitle = "Better than your average!",
                hapticPattern = longArrayOf(0, 100, 50, 100),
                particles = ParticleEffect(ParticleType.PLUS_SIGNS, count = 10),
                animation = AnimationType.PULSE
            )
            
            CelebrationEvent.FIRST_SWING -> CelebrationConfig(
                event = event,
                title = "First Swing! 🎊",
                subtitle = "Welcome to your golf journey!",
                hapticPattern = longArrayOf(0, 200, 100, 200, 100, 200),
                particles = ParticleEffect(ParticleType.HEARTS, count = 20),
                animation = AnimationType.BURST,
                duration = 5000L
            )
            
            CelebrationEvent.COMEBACK -> CelebrationConfig(
                event = event,
                title = customMessage ?: "Great Comeback! 💪",
                subtitle = "That's the spirit!",
                hapticPattern = longArrayOf(0, 150, 100, 150),
                particles = ParticleEffect(ParticleType.STARS, count = 15),
                animation = AnimationType.SPARKLE
            )
            
            CelebrationEvent.PRACTICE_MILESTONE -> CelebrationConfig(
                event = event,
                title = customMessage ?: "Dedication! ⏱️",
                subtitle = getPracticeMilestone(),
                hapticPattern = longArrayOf(0, 100, 100, 100, 100, 100),
                particles = ParticleEffect(ParticleType.CIRCLES, count = 20),
                animation = AnimationType.CONFETTI
            )
            
            CelebrationEvent.TECHNIQUE_MASTERY -> CelebrationConfig(
                event = event,
                title = customMessage ?: "Technique Mastered! 🎓",
                subtitle = "You've nailed it!",
                hapticPattern = longArrayOf(0, 250, 100, 250),
                particles = ParticleEffect(ParticleType.CHECKMARKS, count = 25),
                animation = AnimationType.FIREWORKS
            )
        }
    }
    
    /**
     * Play haptic feedback pattern
     */
    private fun playHapticFeedback(pattern: LongArray) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }
    
    /**
     * Play celebration sound effect
     */
    private fun playCelebrationSound(event: CelebrationEvent) {
        // In real implementation, would load and play sound files
        // soundPool.play(soundIds[event], 1f, 1f, 1, 0, 1f)
    }
    
    /**
     * Track achievements for gamification
     */
    private fun trackAchievement(event: CelebrationEvent) {
        when (event) {
            CelebrationEvent.GREAT_SWING -> {
                totalGreatSwings++
                checkSwingAchievements()
            }
            CelebrationEvent.CONSISTENCY_STREAK -> {
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                    unlockAchievement(
                        Achievement(
                            id = "streak_${longestStreak}",
                            title = "Hot Streak!",
                            description = "$longestStreak swings in a row!",
                            icon = "🔥"
                        )
                    )
                }
            }
            CelebrationEvent.PERSONAL_BEST -> {
                unlockAchievement(
                    Achievement(
                        id = "pb_${System.currentTimeMillis()}",
                        title = "Personal Best!",
                        description = "New high score achieved!",
                        icon = "🏆"
                    )
                )
            }
            else -> {}
        }
    }
    
    /**
     * Check for swing count achievements
     */
    private fun checkSwingAchievements() {
        val milestones = listOf(10, 25, 50, 100, 250, 500, 1000)
        
        milestones.forEach { milestone ->
            if (totalGreatSwings == milestone) {
                unlockAchievement(
                    Achievement(
                        id = "great_swings_$milestone",
                        title = "$milestone Great Swings!",
                        description = "You've made $milestone great swings!",
                        icon = when (milestone) {
                            10 -> "🌟"
                            25 -> "⭐"
                            50 -> "🎯"
                            100 -> "💎"
                            250 -> "🏅"
                            500 -> "🥇"
                            1000 -> "👑"
                            else -> "🎉"
                        }
                    )
                )
                triggerCelebration(CelebrationEvent.MILESTONE_REACHED)
            }
        }
    }
    
    /**
     * Unlock a new achievement
     */
    private fun unlockAchievement(achievement: Achievement) {
        if (!achievements.any { it.id == achievement.id }) {
            achievements.add(achievement)
            // Could trigger additional celebration or notification
        }
    }
    
    /**
     * Update current streak
     */
    fun updateStreak(isGoodSwing: Boolean) {
        if (isGoodSwing) {
            currentStreak++
            if (currentStreak == 3 || currentStreak == 5 || currentStreak == 10) {
                triggerCelebration(CelebrationEvent.CONSISTENCY_STREAK)
            }
        } else {
            currentStreak = 0
        }
    }
    
    /**
     * Update practice time
     */
    fun updatePracticeTime(sessionDuration: Long) {
        totalPracticeTime += sessionDuration
        
        val milestoneMinutes = listOf(10, 30, 60, 120, 300, 600)
        val totalMinutes = totalPracticeTime / 60000
        
        milestoneMinutes.forEach { milestone ->
            if (totalMinutes.toInt() == milestone) {
                triggerCelebration(
                    CelebrationEvent.PRACTICE_MILESTONE,
                    "$milestone minutes of practice!"
                )
            }
        }
    }
    
    /**
     * Get latest milestone description
     */
    private fun getLatestMilestone(): String {
        return when (totalGreatSwings) {
            10 -> "10 great swings!"
            25 -> "25 great swings!"
            50 -> "50 great swings!"
            100 -> "100 great swings! Century club!"
            else -> "$totalGreatSwings great swings!"
        }
    }
    
    /**
     * Get practice milestone description
     */
    private fun getPracticeMilestone(): String {
        val minutes = totalPracticeTime / 60000
        return when {
            minutes >= 600 -> "10 hours of practice! Dedication!"
            minutes >= 300 -> "5 hours of practice!"
            minutes >= 120 -> "2 hours of practice!"
            minutes >= 60 -> "1 hour of practice!"
            minutes >= 30 -> "30 minutes of practice!"
            else -> "$minutes minutes of practice!"
        }
    }
    
    /**
     * Check if this is a comeback swing
     */
    fun checkForComeback(previousScores: List<Float>, currentScore: Float): Boolean {
        if (previousScores.size < 3) return false
        
        val recentAvg = previousScores.takeLast(3).average()
        return recentAvg < 5f && currentScore > 7f
    }
    
    /**
     * Get all unlocked achievements
     */
    fun getAchievements(): List<Achievement> = achievements.toList()
    
    /**
     * Get celebration suggestions based on context
     */
    fun getSuggestedCelebration(score: Float, context: CoachingContext): CelebrationEvent? {
        return when {
            score > 9.5f -> CelebrationEvent.PERFECT_SWING
            score > 8f && score > bestScore -> {
                bestScore = score
                CelebrationEvent.PERSONAL_BEST
            }
            score > 8f -> CelebrationEvent.GREAT_SWING
            context.isFirstSwing -> CelebrationEvent.FIRST_SWING
            context.wasStruggling && score > 7f -> CelebrationEvent.COMEBACK
            context.hasImprovedTechnique -> CelebrationEvent.TECHNIQUE_MASTERY
            score > context.averageScore + 1f -> CelebrationEvent.IMPROVEMENT_DETECTED
            else -> null
        }
    }
    
    data class CoachingContext(
        val isFirstSwing: Boolean = false,
        val wasStruggling: Boolean = false,
        val hasImprovedTechnique: Boolean = false,
        val averageScore: Float = 7f
    )
}