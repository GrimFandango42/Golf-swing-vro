package com.golfswing.vro.pixel.coaching

import com.golfswing.vro.pixel.ai.GeminiNanoManager
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Conversational coaching engine that provides natural, encouraging feedback
 * like a real golf coach standing beside you
 */
@Singleton
class ConversationalCoachingEngine @Inject constructor(
    private val geminiNanoManager: GeminiNanoManager,
    private val poseDetector: GolfSwingPoseDetector,
    private val celebrationSystem: CelebrationSystem
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    private val _coachMessage = MutableStateFlow<CoachMessage?>(null)
    val coachMessage: StateFlow<CoachMessage?> = _coachMessage.asStateFlow()
    
    private val _sessionMood = MutableStateFlow(SessionMood.NEUTRAL)
    val sessionMood: StateFlow<SessionMood> = _sessionMood.asStateFlow()
    
    // Track coaching context for natural conversation
    private var lastFeedbackTime = 0L
    private var consecutiveGoodSwings = 0
    private var currentFocusArea: String? = null
    private var swingCount = 0
    private var sessionStartTime = System.currentTimeMillis()
    private var lastEncouragementTime = 0L
    private var isFirstSwing = true
    
    data class CoachMessage(
        val message: String,
        val emotion: CoachEmotion,
        val priority: MessagePriority,
        val duration: Long = 3000L, // How long to show message
        val voiceEnabled: Boolean = true,
        val celebration: CelebrationSystem.CelebrationEvent? = null
    )
    
    enum class CoachEmotion {
        ENCOURAGING,    // "Great job!"
        EXCITED,       // "Wow, that was amazing!"
        HELPFUL,       // "Try this..."
        SUPPORTIVE,    // "Keep going, you're improving"
        PROUD,         // "I'm proud of your progress"
        THOUGHTFUL,    // "Let me see what's happening..."
        CELEBRATING    // "YES! You nailed it!"
    }
    
    enum class MessagePriority {
        HIGH,      // Critical feedback
        MEDIUM,    // General coaching
        LOW,       // Encouragement
        AMBIENT    // Background encouragement
    }
    
    enum class SessionMood {
        WARMING_UP,
        IN_THE_ZONE,
        STRUGGLING,
        IMPROVING,
        CRUSHING_IT,
        NEUTRAL
    }
    
    /**
     * Start conversational coaching session
     */
    fun startCoachingSession() {
        isFirstSwing = true
        swingCount = 0
        sessionStartTime = System.currentTimeMillis()
        consecutiveGoodSwings = 0
        
        // Welcome message
        sendCoachMessage(
            message = getWelcomeMessage(),
            emotion = CoachEmotion.ENCOURAGING,
            priority = MessagePriority.MEDIUM
        )
        
        // Start observing swings
        scope.launch {
            poseDetector.poseResult
                .filterNotNull()
                .collect { poseResult ->
                    analyzeAndCoach(poseResult)
                }
        }
    }
    
    /**
     * Analyze swing and provide conversational feedback
     */
    private suspend fun analyzeAndCoach(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult) {
        val currentTime = System.currentTimeMillis()
        
        // Don't overwhelm with feedback
        if (currentTime - lastFeedbackTime < 2000L && 
            poseResult.swingPhase != SwingPhase.FINISH) {
            return
        }
        
        when (poseResult.swingPhase) {
            SwingPhase.ADDRESS -> handleAddressPosition(poseResult)
            SwingPhase.BACKSWING -> handleBackswing(poseResult)
            SwingPhase.TOP -> handleTopOfSwing(poseResult)
            SwingPhase.DOWNSWING -> handleDownswing(poseResult)
            SwingPhase.IMPACT -> handleImpact(poseResult)
            SwingPhase.FOLLOW_THROUGH -> handleFollowThrough(poseResult)
            SwingPhase.FINISH -> handleFinish(poseResult)
            else -> {} // IDLE, SETUP
        }
        
        // Periodic encouragement
        checkForEncouragement(currentTime)
    }
    
    /**
     * Handle address position with conversational feedback
     */
    private fun handleAddressPosition(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult) {
        if (isFirstSwing) {
            sendCoachMessage(
                message = "Looking good at address! Take your time...",
                emotion = CoachEmotion.SUPPORTIVE,
                priority = MessagePriority.LOW
            )
            isFirstSwing = false
        }
    }
    
    /**
     * Handle backswing phase
     */
    private fun handleBackswing(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult) {
        val xFactor = poseResult.enhancedMetrics.xFactor
        
        when {
            xFactor < 25f -> {
                currentFocusArea = "shoulder turn"
                sendCoachMessage(
                    message = "Turn those shoulders a bit more... feel the stretch!",
                    emotion = CoachEmotion.HELPFUL,
                    priority = MessagePriority.MEDIUM
                )
            }
            xFactor > 35f && xFactor < 45f -> {
                if (Random.nextFloat() > 0.7f) { // Don't always comment on good form
                    sendCoachMessage(
                        message = "Nice turn! That's the feeling we want.",
                        emotion = CoachEmotion.ENCOURAGING,
                        priority = MessagePriority.LOW
                    )
                }
            }
        }
    }
    
    /**
     * Handle impact position
     */
    private fun handleImpact(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult) {
        val metrics = poseResult.swingMetrics
        
        // Quick reaction to impact
        when {
            metrics.balance > 0.85f && metrics.clubPlane in 55f..65f -> {
                sendCoachMessage(
                    message = listOf(
                        "Solid contact!",
                        "That's it!",
                        "Nice strike!",
                        "Yes!"
                    ).random(),
                    emotion = CoachEmotion.EXCITED,
                    priority = MessagePriority.LOW,
                    duration = 1500L
                )
            }
        }
    }
    
    /**
     * Handle finish position and provide comprehensive feedback
     */
    private suspend fun handleFinish(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult) {
        swingCount++
        val overallScore = calculateSimpleScore(poseResult)
        
        // Update session mood
        updateSessionMood(overallScore)
        
        // Generate conversational feedback
        val feedback = generateConversationalFeedback(poseResult, overallScore)
        
        // Check for celebrations
        if (overallScore > 8.5f) {
            consecutiveGoodSwings++
            if (consecutiveGoodSwings >= 3) {
                celebrationSystem.triggerCelebration(
                    CelebrationSystem.CelebrationEvent.CONSISTENCY_STREAK
                )
                sendCoachMessage(
                    message = "You're on fire! 🔥 Three great swings in a row!",
                    emotion = CoachEmotion.CELEBRATING,
                    priority = MessagePriority.HIGH,
                    celebration = CelebrationSystem.CelebrationEvent.CONSISTENCY_STREAK
                )
                consecutiveGoodSwings = 0
                return
            }
        } else {
            consecutiveGoodSwings = 0
        }
        
        // Send the main coaching message
        sendCoachMessage(
            message = feedback.message,
            emotion = feedback.emotion,
            priority = MessagePriority.MEDIUM
        )
        
        lastFeedbackTime = System.currentTimeMillis()
    }
    
    /**
     * Generate natural conversational feedback
     */
    private suspend fun generateConversationalFeedback(
        poseResult: GolfSwingPoseDetector.GolfSwingPoseResult,
        score: Float
    ): CoachMessage {
        val prompt = buildConversationalPrompt(poseResult, score)
        
        var aiResponse = ""
        geminiNanoManager.analyzeSwingPose(
            poseData = prompt,
            swingPhase = poseResult.swingPhase.name,
            previousAnalysis = currentFocusArea
        ).collect { response ->
            aiResponse = response
        }
        
        // Fallback to template-based feedback if AI is slow
        if (aiResponse.isEmpty()) {
            aiResponse = getTemplateFeedback(poseResult, score)
        }
        
        val emotion = when {
            score > 8.5f -> CoachEmotion.EXCITED
            score > 7.0f -> CoachEmotion.ENCOURAGING
            score > 5.0f -> CoachEmotion.HELPFUL
            else -> CoachEmotion.SUPPORTIVE
        }
        
        return CoachMessage(
            message = aiResponse,
            emotion = emotion,
            priority = MessagePriority.MEDIUM
        )
    }
    
    /**
     * Build conversational prompt for AI
     */
    private fun buildConversationalPrompt(
        poseResult: GolfSwingPoseDetector.GolfSwingPoseResult,
        score: Float
    ): String {
        val metrics = poseResult.enhancedMetrics
        val mood = _sessionMood.value
        
        return """
        You are a friendly, encouraging golf coach having a real-time conversation with your student.
        
        CURRENT SWING:
        - Overall quality: ${score}/10
        - X-Factor: ${metrics.xFactor}°
        - Balance: ${(poseResult.swingMetrics.balance * 100).toInt()}%
        - Tempo: ${if (poseResult.swingMetrics.tempo < 0.7f) "smooth" else "quick"}
        - This is swing #$swingCount of the session
        - Session mood: $mood
        ${currentFocusArea?.let { "- We've been working on: $it" } ?: ""}
        
        COACHING STYLE:
        - Be conversational and natural, like you're standing right there
        - Use simple, friendly language (avoid too much technical jargon)
        - Be encouraging but honest
        - React to the swing like a real coach would
        - Keep it brief (1-2 sentences max)
        - Mix in some personality and humor when appropriate
        - If they're doing well, get excited!
        - If they're struggling, be supportive
        
        EXAMPLES OF GOOD RESPONSES:
        - "That's more like it! Feel how your shoulders really turned there?"
        - "Ooh, rushed that one a bit. Take a breath and try again."
        - "YES! That's the swing I've been waiting to see!"
        - "Getting closer! Just need a touch more balance at the finish."
        - "Beautiful tempo on that one - bottle that feeling!"
        
        Provide natural coaching feedback for this swing:
        """.trimIndent()
    }
    
    /**
     * Get template-based feedback for quick responses
     */
    private fun getTemplateFeedback(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult, score: Float): String {
        val templates = when {
            score > 8.5f -> listOf(
                "Beautiful swing! That felt good, didn't it?",
                "Now THAT'S what I'm talking about!",
                "Boom! Great contact on that one.",
                "Perfect! Do that again!",
                "That's the one! Remember that feeling."
            )
            score > 7.0f -> listOf(
                "Nice swing! Just a small adjustment away from perfect.",
                "Good stuff! You're getting really close.",
                "That's better! Keep that rhythm going.",
                "I like what I'm seeing! One more with that same feel.",
                "Solid contact. Let's build on that."
            )
            score > 5.0f -> listOf(
                "Getting there! Focus on ${getCurrentFocusArea(poseResult)}.",
                "Not bad. Let's slow it down just a touch.",
                "I see what happened. Try this next time...",
                "Almost! Just need to ${getSimpleCorrection(poseResult)}.",
                "Good effort. One thing to work on..."
            )
            else -> listOf(
                "No worries, let's reset and try again.",
                "Shake that one off. You've got this!",
                "Been there! Let's get back to basics.",
                "It happens. Take a breath and let's go again.",
                "Forget that one. Fresh start!"
            )
        }
        
        return templates.random()
    }
    
    /**
     * Simple scoring for quick feedback
     */
    private fun calculateSimpleScore(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult): Float {
        val metrics = poseResult.enhancedMetrics
        val swingMetrics = poseResult.swingMetrics
        
        var score = 5f // Base score
        
        // Bonus points for good metrics
        if (metrics.xFactor in 30f..50f) score += 1.5f
        if (swingMetrics.balance > 0.8f) score += 1.5f
        if (swingMetrics.tempo in 0.3f..0.4f) score += 1f
        if (metrics.kinematicSequence.isOptimalSequence) score += 1f
        
        // Deductions for issues
        if (swingMetrics.headPosition > 0.1f) score -= 0.5f
        if (metrics.xFactor < 20f) score -= 1f
        
        return score.coerceIn(0f, 10f)
    }
    
    /**
     * Update session mood based on performance
     */
    private fun updateSessionMood(score: Float) {
        val currentMood = _sessionMood.value
        val avgScore = 7f // Would track real average
        
        _sessionMood.value = when {
            consecutiveGoodSwings >= 3 -> SessionMood.CRUSHING_IT
            score > avgScore + 1f -> SessionMood.IN_THE_ZONE
            score < avgScore - 1f && currentMood == SessionMood.STRUGGLING -> SessionMood.STRUGGLING
            score > avgScore -> SessionMood.IMPROVING
            swingCount < 5 -> SessionMood.WARMING_UP
            else -> SessionMood.NEUTRAL
        }
    }
    
    /**
     * Check if it's time for ambient encouragement
     */
    private fun checkForEncouragement(currentTime: Long) {
        if (currentTime - lastEncouragementTime < 30000L) return // Every 30 seconds max
        
        val mood = _sessionMood.value
        val timePracticing = (currentTime - sessionStartTime) / 1000 / 60 // minutes
        
        val encouragement = when {
            mood == SessionMood.STRUGGLING && Random.nextFloat() > 0.5f -> {
                listOf(
                    "You're doing great. Every swing is progress!",
                    "Stay patient. Good things are happening.",
                    "I can see you're working hard. It'll click!",
                    "Trust the process. You're improving!"
                ).random()
            }
            timePracticing > 10 && Random.nextFloat() > 0.7f -> {
                listOf(
                    "Nice session! Your dedication is showing.",
                    "Love the focus today!",
                    "You're putting in the work. Respect!",
                    "This is how champions are made!"
                ).random()
            }
            mood == SessionMood.IN_THE_ZONE && Random.nextFloat() > 0.6f -> {
                listOf(
                    "You're in the zone! Ride this wave!",
                    "This is beautiful golf!",
                    "Watching you swing is a joy right now!",
                    "You're locked in! Keep going!"
                ).random()
            }
            else -> null
        }
        
        encouragement?.let {
            sendCoachMessage(
                message = it,
                emotion = CoachEmotion.SUPPORTIVE,
                priority = MessagePriority.AMBIENT,
                duration = 2000L
            )
            lastEncouragementTime = currentTime
        }
    }
    
    /**
     * Get current focus area in simple terms
     */
    private fun getCurrentFocusArea(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult): String {
        val metrics = poseResult.enhancedMetrics
        
        return when {
            metrics.xFactor < 25f -> "turning your shoulders more"
            poseResult.swingMetrics.balance < 0.7f -> "staying balanced"
            poseResult.swingMetrics.tempo > 0.5f -> "slowing down your tempo"
            else -> "maintaining that good form"
        }
    }
    
    /**
     * Get simple correction suggestion
     */
    private fun getSimpleCorrection(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult): String {
        val metrics = poseResult.enhancedMetrics
        
        return when {
            metrics.xFactor < 25f -> "turn your shoulders more on the backswing"
            poseResult.swingMetrics.balance < 0.7f -> "keep your weight centered"
            poseResult.swingMetrics.headPosition > 0.15f -> "keep your head still"
            else -> "trust your swing"
        }
    }
    
    /**
     * Send coach message to UI
     */
    private fun sendCoachMessage(
        message: String,
        emotion: CoachEmotion,
        priority: MessagePriority,
        duration: Long = 3000L,
        celebration: CelebrationSystem.CelebrationEvent? = null
    ) {
        _coachMessage.value = CoachMessage(
            message = message,
            emotion = emotion,
            priority = priority,
            duration = duration,
            celebration = celebration
        )
    }
    
    /**
     * Get personalized welcome message
     */
    private fun getWelcomeMessage(): String {
        val timeOfDay = java.time.LocalTime.now().hour
        
        val greeting = when (timeOfDay) {
            in 5..11 -> "Good morning!"
            in 12..16 -> "Good afternoon!"
            in 17..20 -> "Good evening!"
            else -> "Hey there!"
        }
        
        val messages = listOf(
            "$greeting Ready to work on that swing?",
            "$greeting Let's make some magic happen today!",
            "$greeting Time to dial in that swing!",
            "$greeting Let's see what you've got!",
            "$greeting Ready to have some fun?"
        )
        
        return messages.random()
    }
    
    /**
     * End session with summary
     */
    fun endSession() {
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60
        
        val summary = when {
            swingCount == 0 -> "Come back when you're ready to practice!"
            swingCount < 10 -> "Short but sweet session! Every swing counts."
            _sessionMood.value == SessionMood.CRUSHING_IT -> "What a session! You were on fire today! 🔥"
            _sessionMood.value == SessionMood.STRUGGLING -> "Tough session, but you stuck with it. That's what matters!"
            duration > 30 -> "Great practice session! $swingCount swings in $duration minutes. Well done!"
            else -> "Nice work today! Keep building on this."
        }
        
        sendCoachMessage(
            message = summary,
            emotion = CoachEmotion.PROUD,
            priority = MessagePriority.HIGH,
            duration = 5000L
        )
    }
    
    /**
     * Handle voice commands naturally
     */
    fun handleVoiceCommand(command: String) {
        val response = when {
            command.contains("how", ignoreCase = true) && command.contains("doing", ignoreCase = true) -> {
                val mood = _sessionMood.value
                when (mood) {
                    SessionMood.CRUSHING_IT -> "You're absolutely crushing it! Keep going!"
                    SessionMood.IN_THE_ZONE -> "You're doing great! Really in the zone right now."
                    SessionMood.STRUGGLING -> "Hanging in there! We all have tough days. You're doing fine."
                    else -> "You're doing well! Just keep focusing on the basics."
                }
            }
            command.contains("what", ignoreCase = true) && command.contains("work on", ignoreCase = true) -> {
                currentFocusArea?.let {
                    "Right now, let's focus on $it. You're getting closer!"
                } ?: "Just focus on making a smooth, balanced swing."
            }
            command.contains("tired", ignoreCase = true) -> {
                "No problem! Take a break whenever you need. Quality over quantity!"
            }
            else -> "I'm here to help! Just keep swinging and I'll guide you."
        }
        
        sendCoachMessage(
            message = response,
            emotion = CoachEmotion.SUPPORTIVE,
            priority = MessagePriority.MEDIUM
        )
    }
}