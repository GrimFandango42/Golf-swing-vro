package com.swingsync.ai.celebration

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.achievements.AchievementSystem
import com.swingsync.ai.data.model.SwingAnalysisFeedback
import com.swingsync.ai.data.model.SwingVideoAnalysisInput
import com.swingsync.ai.detection.BestSwingDetector
import com.swingsync.ai.social.ShareGraphicsGenerator
import com.swingsync.ai.social.SocialMediaFormat
import com.swingsync.ai.ui.animations.CelebrationAnimations
import com.swingsync.ai.ui.animations.CelebrationPersonality
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CelebrationIntegrationExample - Demonstrates how all celebration components work together
 * 
 * This example shows the complete flow from swing analysis to magical celebrations:
 * 1. Swing is analyzed for quality
 * 2. Best swing detector evaluates performance
 * 3. Achievement system tracks progress
 * 4. Celebration engine orchestrates the celebration
 * 5. Share graphics are generated for social media
 * 6. Personalized animations are displayed
 * 
 * This creates a complete, delightful user experience that motivates continuous improvement.
 */
class CelebrationIntegrationExample @Inject constructor(
    private val bestSwingDetector: BestSwingDetector,
    private val celebrationEngine: CelebrationEngine,
    private val achievementSystem: AchievementSystem,
    private val shareGraphicsGenerator: ShareGraphicsGenerator,
    private val celebrationAnimations: CelebrationAnimations
) {
    
    companion object {
        private const val TAG = "CelebrationIntegration"
    }
    
    /**
     * Complete integration example - from swing analysis to celebration
     */
    fun demonstrateCompleteFlow(
        lifecycleOwner: LifecycleOwner,
        rootView: ViewGroup,
        swingData: SwingVideoAnalysisInput,
        analysisResult: SwingAnalysisFeedback,
        userPersonality: CelebrationPersonality = CelebrationPersonality.DYNAMIC
    ) {
        lifecycleOwner.lifecycleScope.launch {
            
            // Step 1: Initialize all systems
            initializeSystems(lifecycleOwner, rootView, userPersonality)
            
            // Step 2: Analyze swing quality
            val qualityAssessment = bestSwingDetector.analyzeSwing(swingData, analysisResult)
            
            // Step 3: The systems will automatically detect and react:
            // - BestSwingDetector publishes swing quality events
            // - CelebrationEngine observes and triggers celebrations
            // - AchievementSystem tracks progress and unlocks achievements
            // - ShareGraphicsGenerator creates beautiful graphics
            // - CelebrationAnimations displays personalized effects
            
            // Step 4: Optional manual celebration trigger for demo
            if (qualityAssessment.qualityLevel.name == "EXCELLENT") {
                demonstrateExcellentSwingCelebration(swingData.userId, qualityAssessment.overallScore)
            }
            
            android.util.Log.d(TAG, "Complete celebration flow demonstrated successfully!")
        }
    }
    
    /**
     * Initialize all celebration systems
     */
    private fun initializeSystems(
        lifecycleOwner: LifecycleOwner, 
        rootView: ViewGroup,
        userPersonality: CelebrationPersonality
    ) {
        // Initialize celebration engine
        celebrationEngine.initialize(rootView, lifecycleOwner)
        
        // Initialize achievement system
        achievementSystem.initialize(lifecycleOwner)
        
        // Initialize celebration animations with user personality
        celebrationAnimations.initialize(rootView)
        celebrationAnimations.updatePersonality(userPersonality)
        
        android.util.Log.d(TAG, "All celebration systems initialized")
    }
    
    /**
     * Demonstrate celebration for an excellent swing
     */
    private suspend fun demonstrateExcellentSwingCelebration(userId: String, score: Float) {
        android.util.Log.d(TAG, "Demonstrating excellent swing celebration for user: $userId, score: $score")
        
        // Create a sample best swing event
        val bestSwingEvent = com.swingsync.ai.detection.BestSwingEvent(
            sessionId = "demo_session_${System.currentTimeMillis()}",
            userId = userId,
            timestamp = System.currentTimeMillis(),
            score = score,
            qualityLevel = com.swingsync.ai.detection.SwingQuality.EXCELLENT,
            isPersonalBest = true,
            isTopPercentile = true,
            isConsistentExcellence = false,
            celebrationLevel = com.swingsync.ai.detection.CelebrationLevel.EPIC,
            strengths = listOf("Perfect tempo", "Excellent balance", "Great power generation"),
            message = "🏆 NEW PERSONAL BEST! Outstanding swing!"
        )
        
        // Trigger celebration through the engine
        celebrationEngine.triggerBestSwingCelebration(bestSwingEvent)
        
        // Generate share graphics for social media
        val shareGraphic = shareGraphicsGenerator.generateBestSwingGraphic(
            event = bestSwingEvent,
            format = SocialMediaFormat.INSTAGRAM_SQUARE
        )
        
        android.util.Log.d(TAG, "Share graphic generated: ${shareGraphic.filePath}")
    }
    
    /**
     * Demonstrate achievement unlock celebration
     */
    fun demonstrateAchievementUnlock(
        achievementId: String,
        achievementName: String,
        achievementDescription: String
    ) {
        val milestone = AchievementMilestone(
            id = achievementId,
            name = achievementName,
            description = achievementDescription,
            category = "skill",
            rarity = "epic",
            iconUrl = "ic_achievement_epic"
        )
        
        celebrationEngine.triggerMilestoneCelebration(milestone)
        
        android.util.Log.d(TAG, "Achievement celebration triggered: $achievementName")
    }
    
    /**
     * Demonstrate streak celebration
     */
    fun demonstrateStreakCelebration(streakCount: Int, streakType: String) {
        celebrationEngine.triggerStreakCelebration(streakCount, streakType)
        
        android.util.Log.d(TAG, "Streak celebration triggered: $streakCount $streakType")
    }
    
    /**
     * Demonstrate improvement celebration
     */
    fun demonstrateImprovementCelebration(userId: String, improvementArea: String, improvementPercentage: Float) {
        val improvementEvent = com.swingsync.ai.detection.ImprovementEvent(
            userId = userId,
            timestamp = System.currentTimeMillis(),
            improvementAmount = improvementPercentage / 100f,
            oldAverage = 0.6f,
            newAverage = 0.6f + (improvementPercentage / 100f),
            improvementArea = improvementArea,
            celebrationLevel = com.swingsync.ai.detection.CelebrationLevel.IMPROVEMENT,
            message = "🚀 AMAZING PROGRESS! ${improvementPercentage.toInt()}% improvement in $improvementArea!"
        )
        
        celebrationEngine.triggerImprovementCelebration(improvementEvent)
        
        android.util.Log.d(TAG, "Improvement celebration triggered: $improvementPercentage% in $improvementArea")
    }
    
    /**
     * Get celebration statistics
     */
    fun getCelebrationStatistics(): CelebrationStats {
        return celebrationEngine.getCelebrationStats()
    }
    
    /**
     * Get achievement statistics
     */
    fun getAchievementStatistics(): com.swingsync.ai.achievements.AchievementStats {
        return achievementSystem.getAchievementStats()
    }
    
    /**
     * Update user celebration preferences
     */
    fun updateCelebrationPreferences(preferences: CelebrationPreferences) {
        celebrationEngine.updatePreferences(preferences)
        
        // Update animation preferences
        celebrationAnimations.updatePreferences(
            intensity = preferences.celebrationIntensity,
            enableHaptics = preferences.enableHaptics,
            enableSound = preferences.enableSound
        )
        
        // Update personality if needed
        val personality = when (preferences.preferredStyle) {
            "elegant" -> CelebrationPersonality.ELEGANT
            "playful" -> CelebrationPersonality.PLAYFUL
            "minimalist" -> CelebrationPersonality.MINIMALIST
            "classic" -> CelebrationPersonality.CLASSIC
            "futuristic" -> CelebrationPersonality.FUTURISTIC
            else -> CelebrationPersonality.DYNAMIC
        }
        
        celebrationAnimations.updatePersonality(personality)
        
        android.util.Log.d(TAG, "Celebration preferences updated")
    }
    
    /**
     * Clean up resources when done
     */
    fun cleanup() {
        celebrationEngine.cleanup()
        achievementSystem.cleanup()
        celebrationAnimations.destroy()
        shareGraphicsGenerator.cleanupCache()
        
        android.util.Log.d(TAG, "All celebration systems cleaned up")
    }
}

/**
 * Usage Example in an Activity or Fragment:
 * 
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     
 *     @Inject
 *     lateinit var celebrationIntegration: CelebrationIntegrationExample
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.activity_main)
 *         
 *         val rootView = findViewById<ViewGroup>(R.id.root_container)
 *         
 *         // Demonstrate the complete celebration flow
 *         val swingData = createSampleSwingData()
 *         val analysisResult = createSampleAnalysisResult()
 *         
 *         celebrationIntegration.demonstrateCompleteFlow(
 *             lifecycleOwner = this,
 *             rootView = rootView,
 *             swingData = swingData,
 *             analysisResult = analysisResult,
 *             userPersonality = CelebrationPersonality.DYNAMIC
 *         )
 *     }
 *     
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         celebrationIntegration.cleanup()
 *     }
 * }
 * ```
 */