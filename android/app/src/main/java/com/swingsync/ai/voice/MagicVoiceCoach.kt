package com.swingsync.ai.voice

import android.content.Context
import android.util.Log
import com.swingsync.ai.auto.InstantFeedback
import com.swingsync.ai.auto.FeedbackType
import com.swingsync.ai.data.model.SwingAnalysisFeedback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MagicVoiceCoach - Intelligent voice feedback system for magical swing analysis
 * 
 * Features:
 * - Real-time streaming voice feedback during analysis
 * - Contextual coaching messages based on swing quality
 * - Adaptive voice speed and tone based on user performance
 * - Progressive feedback that builds excitement
 * - Smart interruption handling for seamless experience
 * - Personalized coaching style adaptation
 * - Motivational and encouraging tone
 * 
 * This system makes users feel like they have a professional golf coach
 * providing instant, intelligent feedback on their swing.
 */
@Singleton
class MagicVoiceCoach @Inject constructor(
    private val context: Context,
    private val voiceInterface: VoiceInterface
) {

    companion object {
        private const val TAG = "MagicVoiceCoach"
        
        // Voice feedback timing
        private const val INITIAL_FEEDBACK_DELAY = 500L
        private const val STREAMING_FEEDBACK_INTERVAL = 1500L
        private const val FINAL_FEEDBACK_DELAY = 1000L
        
        // Voice characteristics
        private const val EXCITED_SPEED = 1.1f
        private const val NORMAL_SPEED = 0.9f
        private const val CALM_SPEED = 0.8f
        
        private const val ENTHUSIASTIC_PITCH = 1.1f
        private const val NORMAL_PITCH = 1.0f
        private const val CALM_PITCH = 0.9f
    }

    // State management
    private val _coachingState = MutableStateFlow(CoachingState.READY)
    val coachingState: StateFlow<CoachingState> = _coachingState.asStateFlow()
    
    private val _currentMessage = MutableStateFlow<String?>(null)
    val currentMessage: StateFlow<String?> = _currentMessage.asStateFlow()
    
    // Coroutine scope for voice operations
    private val voiceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Feedback queue for smooth delivery
    private val feedbackQueue = mutableListOf<String>()
    private var isDelivering = false
    
    // User performance tracking for adaptive coaching
    private var userPerformanceHistory = mutableListOf<Float>()
    private var sessionSwingCount = 0
    private var currentCoachingStyle = CoachingStyle.ENCOURAGING

    /**
     * Start the magic voice coaching experience
     */
    fun startMagicCoaching() {
        _coachingState.value = CoachingState.ACTIVE
        sessionSwingCount++
        
        // Welcome message based on session count
        val welcomeMessage = when {
            sessionSwingCount == 1 -> getFirstTimeWelcome()
            sessionSwingCount <= 5 -> getRegularWelcome()
            else -> getExperiencedWelcome()
        }
        
        voiceScope.launch {
            speakWithStyle(welcomeMessage, CoachingStyle.ENCOURAGING)
        }
        
        Log.d(TAG, "Magic voice coaching started (session #$sessionSwingCount)")
    }

    /**
     * Handle instant feedback during analysis
     */
    fun handleInstantFeedback(feedback: InstantFeedback) {
        val message = createStreamingMessage(feedback)
        queueFeedback(message)
        
        // Update coaching style based on feedback
        updateCoachingStyle(feedback)
        
        Log.d(TAG, "Handling instant feedback: ${feedback.type}")
    }

    /**
     * Handle final analysis results
     */
    fun handleFinalAnalysis(analysis: SwingAnalysisFeedback) {
        voiceScope.launch {
            delay(FINAL_FEEDBACK_DELAY)
            
            val finalMessage = createFinalMessage(analysis)
            speakWithStyle(finalMessage, currentCoachingStyle)
            
            // Add to performance history
            val overallScore = calculateOverallScore(analysis)
            userPerformanceHistory.add(overallScore)
            
            // Limit history size
            if (userPerformanceHistory.size > 10) {
                userPerformanceHistory.removeAt(0)
            }
            
            _coachingState.value = CoachingState.COMPLETE
        }
        
        Log.d(TAG, "Final analysis feedback delivered")
    }

    /**
     * Stop voice coaching
     */
    fun stopCoaching() {
        _coachingState.value = CoachingState.STOPPED
        voiceInterface.stopSpeaking()
        feedbackQueue.clear()
        isDelivering = false
        
        Log.d(TAG, "Voice coaching stopped")
    }

    /**
     * Pause voice coaching temporarily
     */
    fun pauseCoaching() {
        _coachingState.value = CoachingState.PAUSED
        voiceInterface.stopSpeaking()
        
        Log.d(TAG, "Voice coaching paused")
    }

    /**
     * Resume voice coaching
     */
    fun resumeCoaching() {
        if (_coachingState.value == CoachingState.PAUSED) {
            _coachingState.value = CoachingState.ACTIVE
            
            voiceScope.launch {
                speakWithStyle("I'm back! Let's continue working on your swing.", CoachingStyle.ENCOURAGING)
            }
        }
        
        Log.d(TAG, "Voice coaching resumed")
    }

    /**
     * Create streaming message from instant feedback
     */
    private fun createStreamingMessage(feedback: InstantFeedback): String {
        return when (feedback.type) {
            FeedbackType.INITIAL -> createInitialMessage(feedback)
            FeedbackType.TEMPO -> createTempoMessage(feedback)
            FeedbackType.POSTURE -> createPostureMessage(feedback)
            FeedbackType.SEQUENCE -> createSequenceMessage(feedback)
            FeedbackType.SWING_PLANE -> createSwingPlaneMessage(feedback)
            FeedbackType.TECHNICAL -> createTechnicalMessage(feedback)
            FeedbackType.FALLBACK -> createFallbackMessage(feedback)
        }
    }

    /**
     * Create initial analysis message
     */
    private fun createInitialMessage(feedback: InstantFeedback): String {
        val enthusiasmLevel = when {
            feedback.confidence > 0.8f -> "Excellent! "
            feedback.confidence > 0.6f -> "Good! "
            else -> ""
        }
        
        return when {
            feedback.score > 0.8f -> "${enthusiasmLevel}I can see everything perfectly! Your setup looks fantastic."
            feedback.score > 0.6f -> "${enthusiasmLevel}Great capture! I'm analyzing your form now."
            else -> "${enthusiasmLevel}I've got your swing! Let me take a closer look."
        }
    }

    /**
     * Create tempo feedback message
     */
    private fun createTempoMessage(feedback: InstantFeedback): String {
        return when {
            feedback.score > 0.8f -> "Beautiful tempo! Your rhythm is spot on."
            feedback.score > 0.6f -> "Nice tempo! You're finding your rhythm."
            else -> "Let's work on smoothing out that tempo a bit."
        }
    }

    /**
     * Create posture feedback message
     */
    private fun createPostureMessage(feedback: InstantFeedback): String {
        return when {
            feedback.score > 0.8f -> "Excellent posture! Your balance is rock solid."
            feedback.score > 0.6f -> "Good posture! You're well-balanced."
            else -> "Let's focus on maintaining that balance throughout your swing."
        }
    }

    /**
     * Create sequence feedback message
     */
    private fun createSequenceMessage(feedback: InstantFeedback): String {
        return when {
            feedback.score > 0.8f -> "Perfect sequence! Your swing flow is beautiful."
            feedback.score > 0.6f -> "Good sequence! I can see the swing developing nicely."
            else -> "I'm tracking your swing sequence. Looking good so far!"
        }
    }

    /**
     * Create swing plane feedback message
     */
    private fun createSwingPlaneMessage(feedback: InstantFeedback): String {
        return when {
            feedback.score > 0.8f -> "Outstanding swing plane! You're right on track."
            feedback.score > 0.6f -> "Solid swing plane! Your club path looks good."
            else -> "I'm analyzing your swing plane. This will help improve consistency."
        }
    }

    /**
     * Create technical feedback message
     */
    private fun createTechnicalMessage(feedback: InstantFeedback): String {
        return when {
            feedback.score > 0.8f -> "Impressive power generation! You're creating great clubhead speed."
            feedback.score > 0.6f -> "Good technical metrics! You're generating solid power."
            else -> "Analyzing your swing mechanics. I see some good potential here!"
        }
    }

    /**
     * Create fallback message
     */
    private fun createFallbackMessage(feedback: InstantFeedback): String {
        return "I can see your swing! Let me analyze what I captured and give you some feedback."
    }

    /**
     * Create final comprehensive message
     */
    private fun createFinalMessage(analysis: SwingAnalysisFeedback): String {
        val overallScore = calculateOverallScore(analysis)
        val improvement = calculateImprovement()
        
        val baseMessage = when {
            overallScore > 0.85f -> "Outstanding swing! You're really dialing it in."
            overallScore > 0.7f -> "Great swing! You're showing solid fundamentals."
            overallScore > 0.55f -> "Good swing! I see some areas where we can make improvements."
            else -> "Nice try! Let's work on a few key areas to improve your swing."
        }
        
        val improvementMessage = when {
            improvement > 0.1f -> " You're really improving! Keep up the excellent work."
            improvement > 0.05f -> " I'm seeing some nice progress in your swing."
            improvement < -0.1f -> " Let's focus on consistency. You've got the potential!"
            else -> " Every swing is a learning opportunity!"
        }
        
        val encouragement = getEncouragingClosing()
        
        return "$baseMessage$improvementMessage $encouragement"
    }

    /**
     * Queue feedback for smooth delivery
     */
    private fun queueFeedback(message: String) {
        feedbackQueue.add(message)
        
        if (!isDelivering) {
            deliverQueuedFeedback()
        }
    }

    /**
     * Deliver queued feedback messages
     */
    private fun deliverQueuedFeedback() {
        if (feedbackQueue.isEmpty()) {
            isDelivering = false
            return
        }
        
        isDelivering = true
        val message = feedbackQueue.removeAt(0)
        
        voiceScope.launch {
            speakWithStyle(message, currentCoachingStyle)
            delay(STREAMING_FEEDBACK_INTERVAL)
            deliverQueuedFeedback()
        }
    }

    /**
     * Speak with adaptive style
     */
    private suspend fun speakWithStyle(message: String, style: CoachingStyle) {
        val settings = getVoiceSettings(style)
        voiceInterface.setVoiceSettings(settings)
        
        _currentMessage.value = message
        
        val success = voiceInterface.speak(message, settings)
        if (!success) {
            Log.w(TAG, "Failed to speak message: $message")
        }
        
        _currentMessage.value = null
    }

    /**
     * Get voice settings for coaching style
     */
    private fun getVoiceSettings(style: CoachingStyle): VoiceSettings {
        return when (style) {
            CoachingStyle.ENCOURAGING -> VoiceSettings(
                speed = NORMAL_SPEED,
                pitch = ENTHUSIASTIC_PITCH,
                volume = 1.0f
            )
            CoachingStyle.TECHNICAL -> VoiceSettings(
                speed = CALM_SPEED,
                pitch = NORMAL_PITCH,
                volume = 0.9f
            )
            CoachingStyle.MOTIVATIONAL -> VoiceSettings(
                speed = EXCITED_SPEED,
                pitch = ENTHUSIASTIC_PITCH,
                volume = 1.0f
            )
            CoachingStyle.ANALYTICAL -> VoiceSettings(
                speed = CALM_SPEED,
                pitch = CALM_PITCH,
                volume = 0.8f
            )
        }
    }

    /**
     * Update coaching style based on feedback
     */
    private fun updateCoachingStyle(feedback: InstantFeedback) {
        currentCoachingStyle = when {
            feedback.score > 0.8f -> CoachingStyle.ENCOURAGING
            feedback.score > 0.6f -> CoachingStyle.MOTIVATIONAL
            feedback.confidence < 0.5f -> CoachingStyle.ANALYTICAL
            else -> CoachingStyle.TECHNICAL
        }
    }

    /**
     * Calculate overall score from analysis
     */
    private fun calculateOverallScore(analysis: SwingAnalysisFeedback): Float {
        // Simple scoring based on feedback content
        val positiveWords = listOf("excellent", "great", "good", "solid", "outstanding", "perfect")
        val negativeWords = listOf("poor", "weak", "needs work", "improve", "fix", "problem")
        
        val summary = analysis.summaryOfFindings.lowercase()
        val positiveCount = positiveWords.count { summary.contains(it) }
        val negativeCount = negativeWords.count { summary.contains(it) }
        
        return when {
            positiveCount > negativeCount * 2 -> 0.9f
            positiveCount > negativeCount -> 0.75f
            positiveCount == negativeCount -> 0.6f
            else -> 0.45f
        }
    }

    /**
     * Calculate improvement over recent swings
     */
    private fun calculateImprovement(): Float {
        if (userPerformanceHistory.size < 3) return 0f
        
        val recent = userPerformanceHistory.takeLast(3).average()
        val previous = userPerformanceHistory.dropLast(3).takeLastWhile { true }.average()
        
        return (recent - previous).toFloat()
    }

    /**
     * Get welcome message for first-time users
     */
    private fun getFirstTimeWelcome(): String {
        return "Welcome to SwingSync! I'm your AI golf coach, and I'm excited to help you improve your swing. " +
                "Just hold steady and let me analyze your form!"
    }

    /**
     * Get welcome message for regular users
     */
    private fun getRegularWelcome(): String {
        return "Ready for another swing analysis! I'm here to help you dial in that form. " +
                "Let's see what we can improve today!"
    }

    /**
     * Get welcome message for experienced users
     */
    private fun getExperiencedWelcome(): String {
        val improvement = calculateImprovement()
        return when {
            improvement > 0.05f -> "You're on fire! Your swing is really coming together. Let's keep building on that progress!"
            improvement < -0.05f -> "Let's get back to fundamentals and find that consistency again. I'm here to help!"
            else -> "Consistency is key! Let's analyze this swing and keep refining your technique."
        }
    }

    /**
     * Get encouraging closing message
     */
    private fun getEncouragingClosing(): String {
        val closings = listOf(
            "Keep practicing, and you'll see amazing results!",
            "Your dedication to improvement really shows!",
            "I love working with golfers who are committed to getting better!",
            "Every swing gets you closer to your goals!",
            "You've got this! Trust the process and keep swinging!",
            "Great work today! I can't wait to see your next swing!",
            "Your swing is unique, and we're making it even better!",
            "Remember, even the pros are always working on their swing!"
        )
        
        return closings.random()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        voiceScope.cancel()
        feedbackQueue.clear()
        userPerformanceHistory.clear()
    }
}

/**
 * Coaching state enum
 */
enum class CoachingState {
    READY, ACTIVE, PAUSED, STOPPED, COMPLETE
}

/**
 * Coaching style enum for adaptive voice feedback
 */
enum class CoachingStyle {
    ENCOURAGING,    // Positive, upbeat, motivational
    TECHNICAL,      // Precise, analytical, instructional  
    MOTIVATIONAL,   // High energy, pump-up style
    ANALYTICAL      // Calm, detailed, thoughtful
}