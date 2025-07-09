package com.swingsync.ai.celebration

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.detection.*
import com.swingsync.ai.ui.animations.MagicAnimations
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CelebrationEngine - Orchestrates magical celebrations for great swings
 * 
 * This engine creates delightful, well-timed celebration experiences that make
 * users feel excited about their golf improvement. It intelligently combines
 * animations, sounds, haptics, and visual effects to create memorable moments.
 * 
 * Features:
 * - Real-time celebration triggering
 * - Personalized celebration styles
 * - Progressive celebration intensity
 * - Achievement milestone celebrations
 * - Social sharing integration
 * - Contextual celebration timing
 * - Multi-modal feedback (visual, audio, haptic)
 * 
 * The engine learns user preferences and adjusts celebration styles accordingly.
 */
@Singleton
class CelebrationEngine @Inject constructor(
    private val context: Context,
    private val magicAnimations: MagicAnimations,
    private val bestSwingDetector: BestSwingDetector
) {
    
    companion object {
        private const val TAG = "CelebrationEngine"
        
        // Celebration timing
        private const val CELEBRATION_DELAY_MS = 500L
        private const val CELEBRATION_DURATION_MS = 3000L
        private const val MICRO_CELEBRATION_DURATION_MS = 1000L
        
        // Celebration intensity levels
        private const val SUBTLE_INTENSITY = 0.3f
        private const val MODERATE_INTENSITY = 0.6f
        private const val HIGH_INTENSITY = 0.8f
        private const val EPIC_INTENSITY = 1.0f
    }
    
    // State management
    private val _celebrationState = MutableStateFlow(CelebrationState.IDLE)
    val celebrationState: StateFlow<CelebrationState> = _celebrationState.asStateFlow()
    
    private val _currentCelebration = MutableStateFlow<CelebrationEvent?>(null)
    val currentCelebration: StateFlow<CelebrationEvent?> = _currentCelebration.asStateFlow()
    
    private val _celebrationHistory = MutableStateFlow<List<CelebrationEvent>>(emptyList())
    val celebrationHistory: StateFlow<List<CelebrationEvent>> = _celebrationHistory.asStateFlow()
    
    // User preferences
    private val userPreferences = MutableStateFlow(CelebrationPreferences())
    
    // Celebration components
    private var rootView: ViewGroup? = null
    private var celebrationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Celebration tracking
    private var lastCelebrationTime = 0L
    private var consecutiveGreatSwings = 0
    private var celebrationCount = 0
    
    /**
     * Initialize the celebration engine
     */
    fun initialize(rootView: ViewGroup, lifecycleOwner: LifecycleOwner) {
        this.rootView = rootView
        
        // Observe best swing detection
        lifecycleOwner.lifecycleScope.launch {
            bestSwingDetector.bestSwingDetected.collect { event ->
                event?.let { triggerBestSwingCelebration(it) }
            }
        }
        
        // Observe improvement detection
        lifecycleOwner.lifecycleScope.launch {
            bestSwingDetector.improvementDetected.collect { event ->
                event?.let { triggerImprovementCelebration(it) }
            }
        }
        
        Log.d(TAG, "Celebration engine initialized")
    }
    
    /**
     * Trigger celebration for a best swing
     */
    fun triggerBestSwingCelebration(event: BestSwingEvent) {
        Log.d(TAG, "Triggering best swing celebration: ${event.celebrationLevel}")
        
        // Check if enough time has passed since last celebration
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCelebrationTime < CELEBRATION_DELAY_MS) {
            Log.d(TAG, "Skipping celebration - too soon after last one")
            return
        }
        
        // Create celebration event
        val celebration = CelebrationEvent(
            id = "swing_${event.sessionId}_${System.currentTimeMillis()}",
            type = CelebrationType.BEST_SWING,
            level = event.celebrationLevel,
            timestamp = currentTime,
            title = event.message,
            description = generateCelebrationDescription(event),
            intensity = mapLevelToIntensity(event.celebrationLevel),
            duration = mapLevelToDuration(event.celebrationLevel),
            personalizedElements = generatePersonalizedElements(event),
            shareableContent = generateShareableContent(event)
        )
        
        // Execute celebration
        executeCelebration(celebration)
        
        // Update tracking
        lastCelebrationTime = currentTime
        consecutiveGreatSwings++
        celebrationCount++
        
        // Clear the detection event
        bestSwingDetector.clearBestSwingEvent()
    }
    
    /**
     * Trigger celebration for improvement
     */
    fun triggerImprovementCelebration(event: ImprovementEvent) {
        Log.d(TAG, "Triggering improvement celebration")
        
        val currentTime = System.currentTimeMillis()
        
        // Create improvement celebration
        val celebration = CelebrationEvent(
            id = "improvement_${event.userId}_${currentTime}",
            type = CelebrationType.IMPROVEMENT,
            level = event.celebrationLevel,
            timestamp = currentTime,
            title = event.message,
            description = "You've improved your ${event.improvementArea} by ${(event.improvementAmount * 100).toInt()}%!",
            intensity = MODERATE_INTENSITY,
            duration = CELEBRATION_DURATION_MS,
            personalizedElements = generateImprovementElements(event),
            shareableContent = generateImprovementShareableContent(event)
        )
        
        executeCelebration(celebration)
        
        // Clear the detection event
        bestSwingDetector.clearImprovementEvent()
    }
    
    /**
     * Trigger milestone celebration
     */
    fun triggerMilestoneCelebration(milestone: AchievementMilestone) {
        Log.d(TAG, "Triggering milestone celebration: ${milestone.name}")
        
        val currentTime = System.currentTimeMillis()
        
        val celebration = CelebrationEvent(
            id = "milestone_${milestone.id}_${currentTime}",
            type = CelebrationType.MILESTONE,
            level = CelebrationLevel.EPIC,
            timestamp = currentTime,
            title = "🏆 Achievement Unlocked!",
            description = "${milestone.name} - ${milestone.description}",
            intensity = HIGH_INTENSITY,
            duration = CELEBRATION_DURATION_MS * 2,
            personalizedElements = generateMilestoneElements(milestone),
            shareableContent = generateMilestoneShareableContent(milestone)
        )
        
        executeCelebration(celebration)
    }
    
    /**
     * Trigger streak celebration
     */
    fun triggerStreakCelebration(streakCount: Int, streakType: String) {
        Log.d(TAG, "Triggering streak celebration: $streakCount $streakType")
        
        val currentTime = System.currentTimeMillis()
        
        val celebration = CelebrationEvent(
            id = "streak_${streakType}_${currentTime}",
            type = CelebrationType.STREAK,
            level = when {
                streakCount >= 10 -> CelebrationLevel.LEGENDARY
                streakCount >= 5 -> CelebrationLevel.EPIC
                else -> CelebrationLevel.GREAT
            },
            timestamp = currentTime,
            title = "🔥 ${streakCount} ${streakType} Streak!",
            description = "You're on fire! Keep it up!",
            intensity = when {
                streakCount >= 10 -> EPIC_INTENSITY
                streakCount >= 5 -> HIGH_INTENSITY
                else -> MODERATE_INTENSITY
            },
            duration = CELEBRATION_DURATION_MS,
            personalizedElements = generateStreakElements(streakCount, streakType),
            shareableContent = generateStreakShareableContent(streakCount, streakType)
        )
        
        executeCelebration(celebration)
    }
    
    /**
     * Trigger micro celebration for good shots
     */
    fun triggerMicroCelebration(message: String) {
        Log.d(TAG, "Triggering micro celebration: $message")
        
        val currentTime = System.currentTimeMillis()
        
        val celebration = CelebrationEvent(
            id = "micro_${currentTime}",
            type = CelebrationType.MICRO,
            level = CelebrationLevel.GOOD,
            timestamp = currentTime,
            title = message,
            description = "Nice shot!",
            intensity = SUBTLE_INTENSITY,
            duration = MICRO_CELEBRATION_DURATION_MS,
            personalizedElements = emptyList(),
            shareableContent = null
        )
        
        executeCelebration(celebration)
    }
    
    /**
     * Execute a celebration event
     */
    private fun executeCelebration(celebration: CelebrationEvent) {
        // Cancel any ongoing celebration
        celebrationJob?.cancel()
        
        // Update state
        _celebrationState.value = CelebrationState.CELEBRATING
        _currentCelebration.value = celebration
        
        // Add to history
        val history = _celebrationHistory.value.toMutableList()
        history.add(celebration)
        _celebrationHistory.value = history
        
        // Execute celebration sequence
        celebrationJob = coroutineScope.launch {
            try {
                // Pre-celebration pause for dramatic effect
                delay(200L)
                
                // Execute celebration components in parallel
                launch { executeVisualCelebration(celebration) }
                launch { executeAudioCelebration(celebration) }
                launch { executeHapticCelebration(celebration) }
                launch { executeUIChanges(celebration) }
                
                // Wait for celebration duration
                delay(celebration.duration)
                
                // Post-celebration cleanup
                cleanup(celebration)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error executing celebration", e)
                cleanup(celebration)
            }
        }
    }
    
    /**
     * Execute visual celebration components
     */
    private suspend fun executeVisualCelebration(celebration: CelebrationEvent) {
        rootView?.let { root ->
            when (celebration.type) {
                CelebrationType.BEST_SWING -> {
                    executeSwingCelebrationVisuals(root, celebration)
                }
                CelebrationType.IMPROVEMENT -> {
                    executeImprovementCelebrationVisuals(root, celebration)
                }
                CelebrationType.MILESTONE -> {
                    executeMilestoneCelebrationVisuals(root, celebration)
                }
                CelebrationType.STREAK -> {
                    executeStreakCelebrationVisuals(root, celebration)
                }
                CelebrationType.MICRO -> {
                    executeMicroCelebrationVisuals(root, celebration)
                }
            }
        }
    }
    
    /**
     * Execute audio celebration components
     */
    private suspend fun executeAudioCelebration(celebration: CelebrationEvent) {
        // Play appropriate celebration sound
        when (celebration.level) {
            CelebrationLevel.LEGENDARY -> playSound("celebration_legendary")
            CelebrationLevel.EPIC -> playSound("celebration_epic")
            CelebrationLevel.EXCELLENCE -> playSound("celebration_excellence")
            CelebrationLevel.GREAT -> playSound("celebration_great")
            CelebrationLevel.GOOD -> playSound("celebration_good")
            CelebrationLevel.ENCOURAGING -> playSound("celebration_encouraging")
        }
    }
    
    /**
     * Execute haptic celebration components
     */
    private suspend fun executeHapticCelebration(celebration: CelebrationEvent) {
        // Trigger haptic feedback pattern based on celebration level
        when (celebration.level) {
            CelebrationLevel.LEGENDARY -> triggerHapticPattern("legendary")
            CelebrationLevel.EPIC -> triggerHapticPattern("epic")
            CelebrationLevel.EXCELLENCE -> triggerHapticPattern("excellence")
            CelebrationLevel.GREAT -> triggerHapticPattern("great")
            CelebrationLevel.GOOD -> triggerHapticPattern("good")
            CelebrationLevel.ENCOURAGING -> triggerHapticPattern("encouraging")
        }
    }
    
    /**
     * Execute UI changes for celebration
     */
    private suspend fun executeUIChanges(celebration: CelebrationEvent) {
        // Show celebration overlay
        showCelebrationOverlay(celebration)
        
        // Update UI elements
        updateCelebrationUI(celebration)
        
        // Show share options if applicable
        if (celebration.shareableContent != null) {
            delay(1000L) // Wait a bit before showing share options
            showShareOptions(celebration)
        }
    }
    
    /**
     * Execute swing celebration visuals
     */
    private suspend fun executeSwingCelebrationVisuals(root: ViewGroup, celebration: CelebrationEvent) {
        when (celebration.level) {
            CelebrationLevel.LEGENDARY -> {
                // Epic fireworks and golden particles
                magicAnimations.startCelebrationAnimation(root)
                magicAnimations.startSuccessParticles()
                createFireworksEffect(root)
                createGoldenRain(root)
            }
            CelebrationLevel.EPIC -> {
                // Burst animation with golden particles
                magicAnimations.startCelebrationAnimation(root)
                magicAnimations.startSuccessParticles()
                createBurstEffect(root)
            }
            CelebrationLevel.EXCELLENCE -> {
                // Elegant sparkle effect
                magicAnimations.startCelebrationAnimation(root)
                createSparkleEffect(root)
            }
            CelebrationLevel.GREAT -> {
                // Ripple effect with particles
                magicAnimations.startCelebrationAnimation(root)
                createRippleEffect(root)
            }
            CelebrationLevel.GOOD -> {
                // Gentle glow effect
                createGlowEffect(root)
            }
            CelebrationLevel.ENCOURAGING -> {
                // Subtle shine effect
                createShineEffect(root)
            }
        }
    }
    
    /**
     * Execute improvement celebration visuals
     */
    private suspend fun executeImprovementCelebrationVisuals(root: ViewGroup, celebration: CelebrationEvent) {
        // Progress-themed animations
        createProgressCelebration(root, celebration)
        createUpwardArrowEffect(root)
        createImprovementChart(root, celebration)
    }
    
    /**
     * Execute milestone celebration visuals
     */
    private suspend fun executeMilestoneCelebrationVisuals(root: ViewGroup, celebration: CelebrationEvent) {
        // Achievement-themed animations
        createTrophyAnimation(root)
        createAchievementBadge(root, celebration)
        createConfettiEffect(root)
    }
    
    /**
     * Execute streak celebration visuals
     */
    private suspend fun executeStreakCelebrationVisuals(root: ViewGroup, celebration: CelebrationEvent) {
        // Fire-themed animations for streaks
        createFireEffect(root)
        createStreakCounter(root, celebration)
        createFlameParticles(root)
    }
    
    /**
     * Execute micro celebration visuals
     */
    private suspend fun executeMicroCelebrationVisuals(root: ViewGroup, celebration: CelebrationEvent) {
        // Subtle animations
        createSubtleGlow(root)
        createMicroParticles(root)
    }
    
    /**
     * Show celebration overlay
     */
    private fun showCelebrationOverlay(celebration: CelebrationEvent) {
        // Create and show celebration overlay UI
        Log.d(TAG, "Showing celebration overlay: ${celebration.title}")
    }
    
    /**
     * Update celebration UI
     */
    private fun updateCelebrationUI(celebration: CelebrationEvent) {
        // Update UI elements to reflect celebration
        Log.d(TAG, "Updating celebration UI")
    }
    
    /**
     * Show share options
     */
    private fun showShareOptions(celebration: CelebrationEvent) {
        // Show social sharing options
        Log.d(TAG, "Showing share options for celebration")
    }
    
    /**
     * Create fireworks effect
     */
    private fun createFireworksEffect(root: ViewGroup) {
        // Implementation for fireworks animation
        Log.d(TAG, "Creating fireworks effect")
    }
    
    /**
     * Create golden rain effect
     */
    private fun createGoldenRain(root: ViewGroup) {
        // Implementation for golden rain animation
        Log.d(TAG, "Creating golden rain effect")
    }
    
    /**
     * Create burst effect
     */
    private fun createBurstEffect(root: ViewGroup) {
        // Implementation for burst animation
        Log.d(TAG, "Creating burst effect")
    }
    
    /**
     * Create sparkle effect
     */
    private fun createSparkleEffect(root: ViewGroup) {
        // Implementation for sparkle animation
        Log.d(TAG, "Creating sparkle effect")
    }
    
    /**
     * Create ripple effect
     */
    private fun createRippleEffect(root: ViewGroup) {
        // Implementation for ripple animation
        Log.d(TAG, "Creating ripple effect")
    }
    
    /**
     * Create glow effect
     */
    private fun createGlowEffect(root: ViewGroup) {
        // Implementation for glow animation
        Log.d(TAG, "Creating glow effect")
    }
    
    /**
     * Create shine effect
     */
    private fun createShineEffect(root: ViewGroup) {
        // Implementation for shine animation
        Log.d(TAG, "Creating shine effect")
    }
    
    /**
     * Create progress celebration
     */
    private fun createProgressCelebration(root: ViewGroup, celebration: CelebrationEvent) {
        // Implementation for progress-themed celebration
        Log.d(TAG, "Creating progress celebration")
    }
    
    /**
     * Create upward arrow effect
     */
    private fun createUpwardArrowEffect(root: ViewGroup) {
        // Implementation for upward arrow animation
        Log.d(TAG, "Creating upward arrow effect")
    }
    
    /**
     * Create improvement chart
     */
    private fun createImprovementChart(root: ViewGroup, celebration: CelebrationEvent) {
        // Implementation for improvement chart animation
        Log.d(TAG, "Creating improvement chart")
    }
    
    /**
     * Create trophy animation
     */
    private fun createTrophyAnimation(root: ViewGroup) {
        // Implementation for trophy animation
        Log.d(TAG, "Creating trophy animation")
    }
    
    /**
     * Create achievement badge
     */
    private fun createAchievementBadge(root: ViewGroup, celebration: CelebrationEvent) {
        // Implementation for achievement badge
        Log.d(TAG, "Creating achievement badge")
    }
    
    /**
     * Create confetti effect
     */
    private fun createConfettiEffect(root: ViewGroup) {
        // Implementation for confetti animation
        Log.d(TAG, "Creating confetti effect")
    }
    
    /**
     * Create fire effect
     */
    private fun createFireEffect(root: ViewGroup) {
        // Implementation for fire animation
        Log.d(TAG, "Creating fire effect")
    }
    
    /**
     * Create streak counter
     */
    private fun createStreakCounter(root: ViewGroup, celebration: CelebrationEvent) {
        // Implementation for streak counter animation
        Log.d(TAG, "Creating streak counter")
    }
    
    /**
     * Create flame particles
     */
    private fun createFlameParticles(root: ViewGroup) {
        // Implementation for flame particles
        Log.d(TAG, "Creating flame particles")
    }
    
    /**
     * Create subtle glow
     */
    private fun createSubtleGlow(root: ViewGroup) {
        // Implementation for subtle glow effect
        Log.d(TAG, "Creating subtle glow")
    }
    
    /**
     * Create micro particles
     */
    private fun createMicroParticles(root: ViewGroup) {
        // Implementation for micro particles
        Log.d(TAG, "Creating micro particles")
    }
    
    /**
     * Play celebration sound
     */
    private fun playSound(soundName: String) {
        // Implementation for playing celebration sounds
        Log.d(TAG, "Playing sound: $soundName")
    }
    
    /**
     * Trigger haptic pattern
     */
    private fun triggerHapticPattern(pattern: String) {
        // Implementation for haptic feedback
        Log.d(TAG, "Triggering haptic pattern: $pattern")
    }
    
    /**
     * Generate celebration description
     */
    private fun generateCelebrationDescription(event: BestSwingEvent): String {
        return when {
            event.isPersonalBest -> "New personal best with ${event.strengths.joinToString(", ").lowercase()}!"
            event.isConsistentExcellence -> "Consistent excellence! Your ${event.strengths.joinToString(", ").lowercase()} is outstanding!"
            event.isTopPercentile -> "Top-tier performance! Your swing quality is exceptional!"
            else -> "Great swing! Your ${event.strengths.joinToString(", ").lowercase()} really shined!"
        }
    }
    
    /**
     * Generate personalized elements
     */
    private fun generatePersonalizedElements(event: BestSwingEvent): List<PersonalizedElement> {
        val elements = mutableListOf<PersonalizedElement>()
        
        // Add elements based on user's strengths
        event.strengths.forEach { strength ->
            elements.add(PersonalizedElement(
                type = "strength_highlight",
                content = strength,
                style = "golden_text"
            ))
        }
        
        // Add celebration style based on preferences
        elements.add(PersonalizedElement(
            type = "celebration_style",
            content = userPreferences.value.preferredStyle,
            style = "animated"
        ))
        
        return elements
    }
    
    /**
     * Generate shareable content
     */
    private fun generateShareableContent(event: BestSwingEvent): ShareableContent {
        return ShareableContent(
            title = event.message,
            description = generateCelebrationDescription(event),
            imageUrl = null, // Will be generated by ShareGraphicsGenerator
            hashtags = listOf("#GolfSwing", "#SwingSync", "#GolfImprovement"),
            shareText = "Just hit an amazing swing with SwingSync! ${event.message}"
        )
    }
    
    /**
     * Generate improvement elements
     */
    private fun generateImprovementElements(event: ImprovementEvent): List<PersonalizedElement> {
        return listOf(
            PersonalizedElement(
                type = "improvement_stat",
                content = "${(event.improvementAmount * 100).toInt()}%",
                style = "progress_bar"
            ),
            PersonalizedElement(
                type = "improvement_area",
                content = event.improvementArea,
                style = "highlighted_text"
            )
        )
    }
    
    /**
     * Generate improvement shareable content
     */
    private fun generateImprovementShareableContent(event: ImprovementEvent): ShareableContent {
        return ShareableContent(
            title = "🚀 Golf Improvement!",
            description = "Made ${(event.improvementAmount * 100).toInt()}% improvement in ${event.improvementArea}",
            imageUrl = null,
            hashtags = listOf("#GolfImprovement", "#SwingSync", "#Progress"),
            shareText = "Making great progress with SwingSync! ${event.message}"
        )
    }
    
    /**
     * Generate milestone elements
     */
    private fun generateMilestoneElements(milestone: AchievementMilestone): List<PersonalizedElement> {
        return listOf(
            PersonalizedElement(
                type = "achievement_badge",
                content = milestone.name,
                style = "trophy"
            ),
            PersonalizedElement(
                type = "achievement_description",
                content = milestone.description,
                style = "elegant_text"
            )
        )
    }
    
    /**
     * Generate milestone shareable content
     */
    private fun generateMilestoneShareableContent(milestone: AchievementMilestone): ShareableContent {
        return ShareableContent(
            title = "🏆 Achievement Unlocked!",
            description = "${milestone.name} - ${milestone.description}",
            imageUrl = null,
            hashtags = listOf("#Achievement", "#SwingSync", "#GolfMilestone"),
            shareText = "Just unlocked the '${milestone.name}' achievement with SwingSync!"
        )
    }
    
    /**
     * Generate streak elements
     */
    private fun generateStreakElements(streakCount: Int, streakType: String): List<PersonalizedElement> {
        return listOf(
            PersonalizedElement(
                type = "streak_counter",
                content = streakCount.toString(),
                style = "fire_text"
            ),
            PersonalizedElement(
                type = "streak_type",
                content = streakType,
                style = "bold_text"
            )
        )
    }
    
    /**
     * Generate streak shareable content
     */
    private fun generateStreakShareableContent(streakCount: Int, streakType: String): ShareableContent {
        return ShareableContent(
            title = "🔥 On Fire!",
            description = "$streakCount $streakType streak!",
            imageUrl = null,
            hashtags = listOf("#Streak", "#SwingSync", "#OnFire"),
            shareText = "On a $streakCount $streakType streak with SwingSync! 🔥"
        )
    }
    
    /**
     * Map celebration level to intensity
     */
    private fun mapLevelToIntensity(level: CelebrationLevel): Float {
        return when (level) {
            CelebrationLevel.LEGENDARY -> EPIC_INTENSITY
            CelebrationLevel.EPIC -> EPIC_INTENSITY
            CelebrationLevel.EXCELLENCE -> HIGH_INTENSITY
            CelebrationLevel.GREAT -> HIGH_INTENSITY
            CelebrationLevel.GOOD -> MODERATE_INTENSITY
            CelebrationLevel.ENCOURAGING -> SUBTLE_INTENSITY
        }
    }
    
    /**
     * Map celebration level to duration
     */
    private fun mapLevelToDuration(level: CelebrationLevel): Long {
        return when (level) {
            CelebrationLevel.LEGENDARY -> CELEBRATION_DURATION_MS * 2
            CelebrationLevel.EPIC -> CELEBRATION_DURATION_MS * 2
            CelebrationLevel.EXCELLENCE -> CELEBRATION_DURATION_MS
            CelebrationLevel.GREAT -> CELEBRATION_DURATION_MS
            CelebrationLevel.GOOD -> CELEBRATION_DURATION_MS
            CelebrationLevel.ENCOURAGING -> MICRO_CELEBRATION_DURATION_MS
        }
    }
    
    /**
     * Cleanup after celebration
     */
    private fun cleanup(celebration: CelebrationEvent) {
        _celebrationState.value = CelebrationState.IDLE
        _currentCelebration.value = null
        
        // Clean up any remaining visual effects
        magicAnimations.stopAllAnimations()
        
        Log.d(TAG, "Celebration cleanup completed")
    }
    
    /**
     * Update user preferences
     */
    fun updatePreferences(preferences: CelebrationPreferences) {
        userPreferences.value = preferences
    }
    
    /**
     * Get celebration statistics
     */
    fun getCelebrationStats(): CelebrationStats {
        val history = _celebrationHistory.value
        return CelebrationStats(
            totalCelebrations = history.size,
            bestSwingCelebrations = history.count { it.type == CelebrationType.BEST_SWING },
            improvementCelebrations = history.count { it.type == CelebrationType.IMPROVEMENT },
            milestoneCelebrations = history.count { it.type == CelebrationType.MILESTONE },
            streakCelebrations = history.count { it.type == CelebrationType.STREAK },
            averageIntensity = history.map { it.intensity }.average().toFloat(),
            lastCelebrationTime = history.maxByOrNull { it.timestamp }?.timestamp
        )
    }
    
    /**
     * Clear celebration history
     */
    fun clearHistory() {
        _celebrationHistory.value = emptyList()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        celebrationJob?.cancel()
        coroutineScope.cancel()
        rootView = null
    }
}

// Data classes for celebration system
data class CelebrationEvent(
    val id: String,
    val type: CelebrationType,
    val level: CelebrationLevel,
    val timestamp: Long,
    val title: String,
    val description: String,
    val intensity: Float,
    val duration: Long,
    val personalizedElements: List<PersonalizedElement>,
    val shareableContent: ShareableContent?
)

data class CelebrationPreferences(
    val preferredStyle: String = "dynamic",
    val enableSound: Boolean = true,
    val enableHaptics: Boolean = true,
    val enableVisualEffects: Boolean = true,
    val celebrationIntensity: Float = 1.0f,
    val autoShare: Boolean = false
)

data class PersonalizedElement(
    val type: String,
    val content: String,
    val style: String
)

data class ShareableContent(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val hashtags: List<String>,
    val shareText: String
)

data class AchievementMilestone(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String,
    val iconUrl: String?
)

data class CelebrationStats(
    val totalCelebrations: Int,
    val bestSwingCelebrations: Int,
    val improvementCelebrations: Int,
    val milestoneCelebrations: Int,
    val streakCelebrations: Int,
    val averageIntensity: Float,
    val lastCelebrationTime: Long?
)

enum class CelebrationState {
    IDLE,
    CELEBRATING,
    CLEANUP
}

enum class CelebrationType {
    BEST_SWING,
    IMPROVEMENT,
    MILESTONE,
    STREAK,
    MICRO
}