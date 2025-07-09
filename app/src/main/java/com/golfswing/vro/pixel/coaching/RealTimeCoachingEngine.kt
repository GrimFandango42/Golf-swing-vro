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
import com.golfswing.vro.pixel.metrics.*
import com.golfswing.vro.pixel.benchmarking.ProfessionalBenchmarking
import kotlin.math.abs

@Singleton
class RealTimeCoachingEngine @Inject constructor(
    private val geminiNanoManager: GeminiNanoManager,
    private val poseDetector: GolfSwingPoseDetector,
    private val professionalCoachingPrompts: ProfessionalCoachingPrompts = ProfessionalCoachingPrompts,
    private val skillLevelAdaptationSystem: SkillLevelAdaptationSystem,
    private val contextualFeedbackSystem: ContextualFeedbackSystem,
    private val progressTrackingSystem: ProgressTrackingSystem,
    private val practiceDrillEngine: PracticeDrillEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val professionalBenchmarking = ProfessionalBenchmarking()
    
    private val _coachingFeedback = MutableStateFlow<CoachingFeedback?>(null)
    val coachingFeedback: StateFlow<CoachingFeedback?> = _coachingFeedback.asStateFlow()
    
    private val _swingAnalysis = MutableStateFlow<SwingAnalysis?>(null)
    val swingAnalysis: StateFlow<SwingAnalysis?> = _swingAnalysis.asStateFlow()
    
    private val _practiceRecommendations = MutableStateFlow<List<PracticeRecommendation>>(emptyList())
    val practiceRecommendations: StateFlow<List<PracticeRecommendation>> = _practiceRecommendations.asStateFlow()
    
    private var lastAnalysisTime = 0L
    private val analysisIntervalMs = 500L // Analyze every 500ms to balance performance and feedback
    private var swingHistory = mutableListOf<GolfSwingPoseDetector.GolfSwingPoseResult>()
    private var currentSwingSession = SwingSession()
    private var currentSkillAssessment: SkillLevelAdaptationSystem.SkillLevelAssessment? = null
    private var currentClubType: GeminiNanoManager.ClubType = GeminiNanoManager.ClubType.MID_IRON
    private var currentPlayingSituation: ContextualFeedbackSystem.PlayingSituation = ContextualFeedbackSystem.PlayingSituation.DRIVING_RANGE

    data class CoachingFeedback(
        val message: String,
        val severity: FeedbackSeverity,
        val swingPhase: SwingPhase,
        val timestamp: Long = System.currentTimeMillis(),
        val isRealTime: Boolean = true
    )

    data class SwingAnalysis(
        val overallScore: Float,
        val strengths: List<String>,
        val areasForImprovement: List<String>,
        val keyMetrics: Map<String, Float>,
        val swingPhase: SwingPhase,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class PracticeRecommendation(
        val title: String,
        val description: String,
        val difficulty: PracticeDifficulty,
        val estimatedTimeMinutes: Int,
        val targetArea: String
    )

    data class SwingSession(
        val startTime: Long = System.currentTimeMillis(),
        val swingCount: Int = 0,
        val avgTempo: Float = 0f,
        val avgBalance: Float = 0f,
        val consistencyScore: Float = 0f
    )

    enum class FeedbackSeverity {
        INFO, WARNING, CRITICAL, POSITIVE
    }

    enum class PracticeDifficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
    
    /**
     * Enhanced coaching feedback with professional insights
     */
    data class EnhancedCoachingFeedback(
        val basicFeedback: CoachingFeedback,
        val xFactorFeedback: String,
        val kinematicFeedback: String,
        val powerFeedback: String,
        val consistencyFeedback: String,
        val professionalComparison: ProfessionalComparison,
        val improvementRecommendations: List<String>,
        val priorityFocus: String
    )
    
    /**
     * Professional swing analysis
     */
    data class ProfessionalAnalysis(
        val overallScore: Float,
        val strengths: List<String>,
        val weaknesses: List<String>,
        val xFactorAnalysis: String,
        val kinematicAnalysis: String,
        val powerAnalysis: String,
        val consistencyAnalysis: String,
        val benchmarkComparison: String,
        val nextSteps: List<String>
    )

    /**
     * Start real-time coaching analysis
     */
    fun startRealTimeCoaching() {
        scope.launch {
            poseDetector.poseResult
                .filterNotNull()
                .collect { poseResult ->
                    processRealTimeAnalysis(poseResult)
                }
        }
    }

    /**
     * Process real-time swing analysis
     */
    private suspend fun processRealTimeAnalysis(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult) {
        val currentTime = System.currentTimeMillis()
        
        // Store swing data for historical analysis
        swingHistory.add(poseResult)
        if (swingHistory.size > 100) { // Keep last 100 frames
            swingHistory.removeAt(0)
        }
        
        // Throttle analysis to prevent overwhelming the system
        if (currentTime - lastAnalysisTime < analysisIntervalMs) {
            return
        }
        lastAnalysisTime = currentTime
        
        // Generate pose description for Gemini Nano
        val poseDescription = poseDetector.generatePoseDescription(poseResult)
        
        // Get real-time feedback
        generateRealTimeFeedback(poseResult, poseDescription)
        
        // Analyze overall swing if we have enough data
        if (shouldAnalyzeSwing(poseResult)) {
            analyzeCompleteSwing()
        }
        
        // Update practice recommendations periodically
        if (swingHistory.size % 20 == 0) {
            updatePracticeRecommendations()
        }
    }

    /**
     * Generate immediate coaching feedback using professional methodology
     */
    private suspend fun generateRealTimeFeedback(
        poseResult: GolfSwingPoseDetector.GolfSwingPoseResult,
        poseDescription: String
    ) {
        // Analyze critical form issues first
        val criticalIssues = analyzeCriticalFormIssues(poseResult)
        
        if (criticalIssues.isNotEmpty()) {
            _coachingFeedback.value = CoachingFeedback(
                message = criticalIssues.first(),
                severity = FeedbackSeverity.CRITICAL,
                swingPhase = poseResult.swingPhase
            )
            return
        }
        
        // Get current skill assessment
        val skillAssessment = currentSkillAssessment ?: assessCurrentSkillLevel()
        
        // Generate professional coaching prompt
        val professionalPrompt = professionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            poseDescription,
            poseResult.swingPhase,
            mapToPromptSkillLevel(skillAssessment.skillLevel),
            mapToPromptClubType(currentClubType),
            _coachingFeedback.value?.message
        )
        
        // Get AI-powered feedback
        geminiNanoManager.generateCoachingTips(
            professionalPrompt,
            mapToGeminiSkillLevel(skillAssessment.skillLevel),
            currentClubType,
            mapToGeminiSituation(currentPlayingSituation)
        ).collect { aiResponse ->
            // Apply skill level adaptation
            val adaptedMessage = skillLevelAdaptationSystem.adaptCoachingMessage(
                aiResponse,
                skillAssessment,
                poseResult.swingPhase,
                currentClubType
            )
            
            // Generate contextual feedback
            val contextualFeedback = generateContextualFeedback(
                adaptedMessage,
                poseResult,
                skillAssessment
            )
            
            val feedback = parseAIResponse(contextualFeedback, poseResult.swingPhase)
            _coachingFeedback.value = feedback
        }
    }

    /**
     * Generate enhanced coaching feedback with professional insights
     */
    private suspend fun generateEnhancedCoachingFeedback(
        poseResult: GolfSwingPoseDetector.GolfSwingPoseResult
    ): EnhancedCoachingFeedback {
        val enhancedMetrics = poseResult.enhancedMetrics
        val professionalComparison = poseResult.professionalComparison
        
        // Generate basic feedback
        val basicFeedback = CoachingFeedback(
            message = "Analyzing swing with professional standards...",
            severity = FeedbackSeverity.INFO,
            swingPhase = poseResult.swingPhase
        )
        
        // Generate specific feedback areas
        val xFactorFeedback = generateXFactorFeedback(enhancedMetrics.xFactor, enhancedMetrics.xFactorStretch)
        val kinematicFeedback = generateKinematicFeedback(enhancedMetrics.kinematicSequence)
        val powerFeedback = generatePowerFeedback(enhancedMetrics.powerMetrics)
        val consistencyFeedback = generateConsistencyFeedback(enhancedMetrics.swingConsistency)
        
        // Generate improvement recommendations
        val improvementRecommendations = professionalBenchmarking.generateImprovementRecommendations(
            enhancedMetrics,
            professionalComparison
        ).take(3).map { it.message }
        
        // Determine priority focus
        val priorityFocus = determinePriorityFocus(professionalComparison)
        
        return EnhancedCoachingFeedback(
            basicFeedback = basicFeedback,
            xFactorFeedback = xFactorFeedback,
            kinematicFeedback = kinematicFeedback,
            powerFeedback = powerFeedback,
            consistencyFeedback = consistencyFeedback,
            professionalComparison = professionalComparison,
            improvementRecommendations = improvementRecommendations,
            priorityFocus = priorityFocus
        )
    }
    
    // Enhanced feedback generation methods
    private fun generateXFactorFeedback(xFactor: Float, xFactorStretch: Float): String {
        return when {
            xFactor < 25f -> "Increase shoulder turn while keeping hips stable for better X-Factor (Current: ${xFactor.toInt()}°, Target: 35-55°)"
            xFactor > 60f -> "Reduce excessive shoulder turn for better control (Current: ${xFactor.toInt()}°, Target: 35-55°)"
            else -> "Good X-Factor generation! (${xFactor.toInt()}° - within optimal range)"
        }
    }
    
    private fun generateKinematicFeedback(kinematicSequence: KinematicSequence): String {
        return if (kinematicSequence.isOptimalSequence) {
            "Excellent kinematic sequence! You're firing body segments in optimal order."
        } else {
            "Focus on proper sequence: Pelvis → Torso → Arms → Club. Current efficiency: ${(kinematicSequence.sequenceEfficiency * 100).toInt()}%"
        }
    }
    
    private fun generatePowerFeedback(powerMetrics: PowerMetrics): String {
        return when {
            powerMetrics.totalPower < 1500f -> "Focus on generating more power through ground force and rotation (Current: ${powerMetrics.totalPower.toInt()}W)"
            powerMetrics.powerTransferEfficiency < 0.6f -> "Improve power transfer efficiency through better sequence (Current: ${(powerMetrics.powerTransferEfficiency * 100).toInt()}%)"
            else -> "Good power generation! (${powerMetrics.totalPower.toInt()}W with ${(powerMetrics.powerTransferEfficiency * 100).toInt()}% efficiency)"
        }
    }
    
    private fun generateConsistencyFeedback(swingConsistency: SwingConsistency): String {
        return when {
            swingConsistency.overallConsistency < 0.5f -> "Work on swing consistency through focused practice (Current: ${(swingConsistency.overallConsistency * 100).toInt()}%)"
            swingConsistency.consistencyTrend.trend == TrendDirection.DECLINING -> "Consistency declining - review fundamentals"
            swingConsistency.consistencyTrend.trend == TrendDirection.IMPROVING -> "Great job! Consistency is improving"
            else -> "Maintain current consistency level through regular practice"
        }
    }
    
    private fun determinePriorityFocus(comparison: ProfessionalComparison): String {
        return when {
            comparison.xFactorScore < 5f -> "X-Factor Development"
            comparison.kinematicScore < 5f -> "Kinematic Sequence"
            comparison.powerScore < 5f -> "Power Generation"
            comparison.consistencyScore < 5f -> "Swing Consistency"
            else -> "Fine-tuning"
        }
    }
    
    /**
     * Get enhanced coaching feedback for current swing
     */
    suspend fun getEnhancedCoachingFeedback(): EnhancedCoachingFeedback? {
        val currentResult = poseDetector.poseResult.value ?: return null
        return generateEnhancedCoachingFeedback(currentResult)
    }
    
    /**
     * Analyze critical form issues that need immediate attention
     */
    private fun analyzeCriticalFormIssues(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult): List<String> {
        val issues = mutableListOf<String>()
        val metrics = poseResult.swingMetrics
        val enhancedMetrics = poseResult.enhancedMetrics
        
        // Enhanced critical issue detection
        
        // X-Factor critical check
        if (enhancedMetrics.xFactor < 15f) {
            issues.add("CRITICAL: Insufficient shoulder turn - increase X-Factor")
        }
        
        // Kinematic sequence critical check
        if (enhancedMetrics.kinematicSequence.sequenceEfficiency < 0.3f) {
            issues.add("CRITICAL: Poor kinematic sequence - focus on proper body firing order")
        }
        
        // Power generation critical check
        if (enhancedMetrics.powerMetrics.totalPower < 1000f) {
            issues.add("CRITICAL: Very low power generation - work on ground force and rotation")
        }
        
        // Traditional critical checks"
        
        // Head movement check
        if (metrics.headPosition > 0.1f) {
            issues.add("Keep your head still - excessive head movement detected")
        }
        
        // Balance check
        if (metrics.balance < 0.6f) {
            issues.add("Maintain balance - weight distribution is off")
        }
        
        // Posture check
        if (abs(metrics.shoulderAngle) > 45f) {
            issues.add("Check shoulder alignment - excessive tilt detected")
        }
        
        // Knee stability check
        if (metrics.kneeFlexion < 0.2f || metrics.kneeFlexion > 0.8f) {
            issues.add("Maintain proper knee flexion throughout swing")
        }
        
        return issues
    }

    /**
     * Parse AI response and create appropriate feedback
     */
    private fun parseAIResponse(aiResponse: String, swingPhase: SwingPhase): CoachingFeedback {
        val severity = when {
            aiResponse.contains("excellent", ignoreCase = true) || 
            aiResponse.contains("good", ignoreCase = true) -> FeedbackSeverity.POSITIVE
            aiResponse.contains("critical", ignoreCase = true) || 
            aiResponse.contains("urgent", ignoreCase = true) -> FeedbackSeverity.CRITICAL
            aiResponse.contains("improve", ignoreCase = true) || 
            aiResponse.contains("adjust", ignoreCase = true) -> FeedbackSeverity.WARNING
            else -> FeedbackSeverity.INFO
        }
        
        return CoachingFeedback(
            message = aiResponse,
            severity = severity,
            swingPhase = swingPhase
        )
    }

    /**
     * Determine if we should analyze the complete swing
     */
    private fun shouldAnalyzeSwing(poseResult: GolfSwingPoseDetector.GolfSwingPoseResult): Boolean {
        return poseResult.swingPhase == SwingPhase.FINISH || 
               (swingHistory.size >= 30 && hasCompletedSwingCycle())
    }

    /**
     * Check if we have a complete swing cycle in history
     */
    private fun hasCompletedSwingCycle(): Boolean {
        val recentPhases = swingHistory.takeLast(30).map { it.swingPhase }
        return recentPhases.contains(SwingPhase.ADDRESS) && 
               recentPhases.contains(SwingPhase.BACKSWING) &&
               recentPhases.contains(SwingPhase.DOWNSWING)
    }

    /**
     * Analyze complete swing and provide comprehensive professional feedback
     */
    private suspend fun analyzeCompleteSwing() {
        if (swingHistory.isEmpty()) return
        
        val recentSwing = swingHistory.takeLast(30)
        val swingMetrics = calculateSwingMetrics(recentSwing)
        
        // Get current skill assessment
        val skillAssessment = currentSkillAssessment ?: assessCurrentSkillLevel()
        
        // Calculate overall score
        val overallScore = calculateOverallScore(swingMetrics)
        
        // Identify strengths and areas for improvement
        val strengths = identifyStrengths(swingMetrics)
        val areasForImprovement = identifyAreasForImprovement(swingMetrics)
        
        // Generate professional coaching analysis
        val analysisPrompt = professionalCoachingPrompts.buildSkillAdaptedCoachingPrompt(
            "Swing analysis: Score ${overallScore}/10, Strengths: ${strengths.joinToString()}, Areas for improvement: ${areasForImprovement.joinToString()}",
            mapToPromptSkillLevel(skillAssessment.skillLevel),
            mapToPromptClubType(currentClubType),
            mapToPromptSituation(currentPlayingSituation)
        )
        
        // Get AI-powered coaching tips
        geminiNanoManager.generateCoachingTips(
            analysisPrompt,
            mapToGeminiSkillLevel(skillAssessment.skillLevel),
            currentClubType,
            mapToGeminiSituation(currentPlayingSituation)
        ).collect { coachingTips ->
            _swingAnalysis.value = SwingAnalysis(
                overallScore = overallScore,
                strengths = strengths,
                areasForImprovement = areasForImprovement,
                keyMetrics = swingMetrics,
                swingPhase = SwingPhase.FINISH
            )
        }
        
        // Update progress tracking
        updateProgressTracking(swingMetrics)
    }

    /**
     * Calculate key swing metrics from swing history
     */
    private fun calculateSwingMetrics(swingData: List<GolfSwingPoseDetector.GolfSwingPoseResult>): Map<String, Float> {
        if (swingData.isEmpty()) return emptyMap()
        
        val metrics = mutableMapOf<String, Float>()
        
        // Calculate averages
        metrics["avgTempo"] = swingData.map { it.swingMetrics.tempo }.average().toFloat()
        metrics["avgBalance"] = swingData.map { it.swingMetrics.balance }.average().toFloat()
        metrics["avgHeadStability"] = swingData.map { 1f - it.swingMetrics.headPosition }.average().toFloat()
        metrics["avgClubPlane"] = swingData.map { it.swingMetrics.clubPlane }.average().toFloat()
        
        // Calculate consistency scores
        val tempoVariation = calculateVariation(swingData.map { it.swingMetrics.tempo })
        metrics["tempoConsistency"] = 1f - tempoVariation
        
        val balanceVariation = calculateVariation(swingData.map { it.swingMetrics.balance })
        metrics["balanceConsistency"] = 1f - balanceVariation
        
        return metrics
    }

    /**
     * Calculate variation in a list of values
     */
    private fun calculateVariation(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        return kotlin.math.sqrt(variance)
    }

    /**
     * Calculate overall swing score
     */
    private fun calculateOverallScore(metrics: Map<String, Float>): Float {
        val weights = mapOf(
            "avgTempo" to 0.2f,
            "avgBalance" to 0.25f,
            "avgHeadStability" to 0.2f,
            "tempoConsistency" to 0.15f,
            "balanceConsistency" to 0.2f
        )
        
        var score = 0f
        weights.forEach { (metric, weight) ->
            score += (metrics[metric] ?: 0f) * weight
        }
        
        return (score * 10f).coerceIn(0f, 10f)
    }

    /**
     * Identify swing strengths
     */
    private fun identifyStrengths(metrics: Map<String, Float>): List<String> {
        val strengths = mutableListOf<String>()
        
        if ((metrics["avgBalance"] ?: 0f) > 0.8f) {
            strengths.add("Excellent balance throughout swing")
        }
        
        if ((metrics["avgHeadStability"] ?: 0f) > 0.8f) {
            strengths.add("Good head stability")
        }
        
        if ((metrics["tempoConsistency"] ?: 0f) > 0.7f) {
            strengths.add("Consistent swing tempo")
        }
        
        if ((metrics["balanceConsistency"] ?: 0f) > 0.7f) {
            strengths.add("Consistent balance")
        }
        
        return strengths
    }

    /**
     * Identify areas for improvement
     */
    private fun identifyAreasForImprovement(metrics: Map<String, Float>): List<String> {
        val improvements = mutableListOf<String>()
        
        if ((metrics["avgBalance"] ?: 0f) < 0.6f) {
            improvements.add("Work on balance and weight distribution")
        }
        
        if ((metrics["avgHeadStability"] ?: 0f) < 0.6f) {
            improvements.add("Focus on keeping head still")
        }
        
        if ((metrics["tempoConsistency"] ?: 0f) < 0.5f) {
            improvements.add("Practice consistent swing tempo")
        }
        
        if ((metrics["avgTempo"] ?: 0f) > 0.8f) {
            improvements.add("Slow down swing tempo")
        }
        
        return improvements
    }

    /**
     * Update practice recommendations using professional drill methodology
     */
    private suspend fun updatePracticeRecommendations() {
        if (swingHistory.isEmpty()) return
        
        val recentMetrics = calculateSwingMetrics(swingHistory.takeLast(50))
        val areasForImprovement = identifyAreasForImprovement(recentMetrics)
        val skillAssessment = currentSkillAssessment ?: assessCurrentSkillLevel()
        
        // Analyze swing faults from metrics
        val detectedFaults = practiceDrillEngine.analyzeFaultsFromMetrics(recentMetrics)
        
        // Generate drill recommendations
        val drillInput = PracticeDrillEngine.SwingAnalysisInput(
            faults = detectedFaults,
            skillLevel = mapToPracticeDrillSkillLevel(skillAssessment.skillLevel),
            clubType = mapToPracticeDrillClubType(currentClubType),
            availableTime = 30, // Default practice time
            practiceEnvironment = "Driving Range"
        )
        
        val professionalDrills = practiceDrillEngine.generateDrillRecommendations(drillInput)
        
        // Convert to practice recommendations
        val recommendations = professionalDrills.map { drill ->
            PracticeRecommendation(
                title = drill.name,
                description = drill.description,
                difficulty = mapToPracticeDifficulty(drill.difficulty),
                estimatedTimeMinutes = drill.duration,
                targetArea = drill.purpose
            )
        }
        
        _practiceRecommendations.value = recommendations
    }

    /**
     * Parse practice drills from AI response
     */
    private fun parsePracticeDrills(drillsResponse: String): List<PracticeRecommendation> {
        // Parse the AI response to extract structured practice recommendations
        // This would be more sophisticated in a real implementation
        return listOf(
            PracticeRecommendation(
                title = "Balance Practice",
                description = "Practice swings with feet together to improve balance",
                difficulty = PracticeDifficulty.BEGINNER,
                estimatedTimeMinutes = 10,
                targetArea = "Balance"
            ),
            PracticeRecommendation(
                title = "Head Stability Drill",
                description = "Practice keeping head still during swing",
                difficulty = PracticeDifficulty.INTERMEDIATE,
                estimatedTimeMinutes = 15,
                targetArea = "Head Position"
            ),
            PracticeRecommendation(
                title = "Tempo Training",
                description = "Use metronome to practice consistent swing tempo",
                difficulty = PracticeDifficulty.INTERMEDIATE,
                estimatedTimeMinutes = 20,
                targetArea = "Tempo"
            )
        )
    }

    /**
     * Reset coaching session
     */
    fun resetSession() {
        swingHistory.clear()
        currentSwingSession = SwingSession()
        _coachingFeedback.value = null
        _swingAnalysis.value = null
        _practiceRecommendations.value = emptyList()
    }

    /**
     * Get current session statistics with professional insights
     */
    fun getCurrentSessionStats(): SwingSession {
        return currentSwingSession.copy(
            swingCount = swingHistory.size,
            avgTempo = swingHistory.map { it.swingMetrics.tempo }.average().toFloat(),
            avgBalance = swingHistory.map { it.swingMetrics.balance }.average().toFloat(),
            consistencyScore = calculateConsistencyScore()
        )
    }
    
    /**
     * Set current club type for contextual coaching
     */
    fun setCurrentClubType(clubType: GeminiNanoManager.ClubType) {
        currentClubType = clubType
    }
    
    /**
     * Set current playing situation for contextual coaching
     */
    fun setPlayingSituation(situation: ContextualFeedbackSystem.PlayingSituation) {
        currentPlayingSituation = situation
    }
    
    /**
     * Get comprehensive progress insights
     */
    suspend fun getProgressInsights(): String {
        if (swingHistory.isEmpty()) return "No swing data available for analysis"
        
        val recentMetrics = calculateSwingMetrics(swingHistory.takeLast(20))
        val skillAssessment = currentSkillAssessment ?: assessCurrentSkillLevel()
        
        // Capture progress snapshot
        val progressSnapshot = progressTrackingSystem.captureProgressSnapshot(
            recentMetrics,
            skillAssessment.skillLevel,
            mapOf(
                "totalPracticeTime" to (swingHistory.size * 30L), // Approximate
                "sessionsCompleted" to 1,
                "improvementRate" to calculateConsistencyScore()
            )
        )
        
        // Generate AI insights
        var insights = ""
        progressTrackingSystem.generateProgressInsights(
            progressSnapshot,
            null,
            "Current session"
        ).collect { insight ->
            insights = insight
        }
        
        return insights
    }

    /**
     * Calculate consistency score for current session
     */
    private fun calculateConsistencyScore(): Float {
        if (swingHistory.size < 5) return 0f
        
        val tempoVariation = calculateVariation(swingHistory.map { it.swingMetrics.tempo })
        val balanceVariation = calculateVariation(swingHistory.map { it.swingMetrics.balance })
        
        return (1f - (tempoVariation + balanceVariation) / 2f).coerceIn(0f, 1f)
    }
    
    // Professional coaching integration methods
    
    private fun assessCurrentSkillLevel(): SkillLevelAdaptationSystem.SkillLevelAssessment {
        val recentMetrics = if (swingHistory.isNotEmpty()) {
            calculateSwingMetrics(swingHistory.takeLast(10))
        } else {
            emptyMap()
        }
        
        val consistencyMetrics = mapOf(
            "tempoConsistency" to calculateConsistencyScore(),
            "balanceConsistency" to calculateConsistencyScore()
        )
        
        val assessment = skillLevelAdaptationSystem.assessSkillLevel(
            recentMetrics,
            consistencyMetrics,
            emptyList() // No history for new assessment
        )
        
        currentSkillAssessment = assessment
        return assessment
    }
    
    private fun generateContextualFeedback(
        baseMessage: String,
        poseResult: GolfSwingPoseDetector.GolfSwingPoseResult,
        skillAssessment: SkillLevelAdaptationSystem.SkillLevelAssessment
    ): String {
        val context = ContextualFeedbackSystem.FeedbackContext(
            clubType = currentClubType,
            swingPhase = poseResult.swingPhase,
            playingSituation = currentPlayingSituation,
            weatherConditions = null, // Would be provided by app
            lieCondition = ContextualFeedbackSystem.LieCondition.PERFECT_LIE,
            targetDistance = 150f, // Default target
            pressureLevel = ContextualFeedbackSystem.PressureLevel.LOW,
            playerConfidence = 0.7f, // Default confidence
            recentPerformance = ContextualFeedbackSystem.RecentPerformance(
                lastFiveShots = emptyList(),
                overallTrend = ContextualFeedbackSystem.TrendDirection.STABLE,
                confidenceLevel = 0.7f,
                strugglingAreas = emptyList()
            )
        )
        
        val contextualFeedback = contextualFeedbackSystem.generateContextualFeedback(
            baseMessage,
            context,
            skillAssessment.skillLevel
        )
        
        return contextualFeedback.adaptiveCoaching
    }
    
    private fun updateProgressTracking(swingMetrics: Map<String, Float>) {
        val skillAssessment = currentSkillAssessment ?: return
        
        progressTrackingSystem.captureProgressSnapshot(
            swingMetrics,
            skillAssessment.skillLevel,
            mapOf(
                "totalPracticeTime" to (swingHistory.size * 30L),
                "sessionsCompleted" to 1,
                "improvementRate" to calculateConsistencyScore()
            )
        )
    }
    
    // Mapping functions for different skill level enums
    
    private fun mapToPromptSkillLevel(skillLevel: SkillLevelAdaptationSystem.SkillLevel): ProfessionalCoachingPrompts.SkillLevel {
        return when (skillLevel) {
            SkillLevelAdaptationSystem.SkillLevel.BEGINNER -> ProfessionalCoachingPrompts.SkillLevel.BEGINNER
            SkillLevelAdaptationSystem.SkillLevel.INTERMEDIATE -> ProfessionalCoachingPrompts.SkillLevel.INTERMEDIATE
            SkillLevelAdaptationSystem.SkillLevel.ADVANCED -> ProfessionalCoachingPrompts.SkillLevel.ADVANCED
            SkillLevelAdaptationSystem.SkillLevel.PROFESSIONAL -> ProfessionalCoachingPrompts.SkillLevel.PROFESSIONAL
        }
    }
    
    private fun mapToGeminiSkillLevel(skillLevel: SkillLevelAdaptationSystem.SkillLevel): GeminiNanoManager.SkillLevel {
        return when (skillLevel) {
            SkillLevelAdaptationSystem.SkillLevel.BEGINNER -> GeminiNanoManager.SkillLevel.BEGINNER
            SkillLevelAdaptationSystem.SkillLevel.INTERMEDIATE -> GeminiNanoManager.SkillLevel.INTERMEDIATE
            SkillLevelAdaptationSystem.SkillLevel.ADVANCED -> GeminiNanoManager.SkillLevel.ADVANCED
            SkillLevelAdaptationSystem.SkillLevel.PROFESSIONAL -> GeminiNanoManager.SkillLevel.PROFESSIONAL
        }
    }
    
    private fun mapToPromptClubType(clubType: GeminiNanoManager.ClubType): ProfessionalCoachingPrompts.ClubType {
        return when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> ProfessionalCoachingPrompts.ClubType.DRIVER
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> ProfessionalCoachingPrompts.ClubType.FAIRWAY_WOOD
            GeminiNanoManager.ClubType.HYBRID -> ProfessionalCoachingPrompts.ClubType.HYBRID
            GeminiNanoManager.ClubType.LONG_IRON -> ProfessionalCoachingPrompts.ClubType.LONG_IRON
            GeminiNanoManager.ClubType.MID_IRON -> ProfessionalCoachingPrompts.ClubType.MID_IRON
            GeminiNanoManager.ClubType.SHORT_IRON -> ProfessionalCoachingPrompts.ClubType.SHORT_IRON
            GeminiNanoManager.ClubType.WEDGE -> ProfessionalCoachingPrompts.ClubType.WEDGE
            GeminiNanoManager.ClubType.PUTTER -> ProfessionalCoachingPrompts.ClubType.PUTTER
        }
    }
    
    private fun mapToPromptSituation(situation: ContextualFeedbackSystem.PlayingSituation): ProfessionalCoachingPrompts.SituationalContext {
        return when (situation) {
            ContextualFeedbackSystem.PlayingSituation.DRIVING_RANGE -> ProfessionalCoachingPrompts.SituationalContext.DRIVING_RANGE
            ContextualFeedbackSystem.PlayingSituation.COURSE_PLAY -> ProfessionalCoachingPrompts.SituationalContext.COURSE_PLAY
            ContextualFeedbackSystem.PlayingSituation.PRACTICE_GREEN -> ProfessionalCoachingPrompts.SituationalContext.PRACTICE_SESSION
            ContextualFeedbackSystem.PlayingSituation.TOURNAMENT_PLAY -> ProfessionalCoachingPrompts.SituationalContext.TOURNAMENT_PREP
            else -> ProfessionalCoachingPrompts.SituationalContext.PRACTICE_SESSION
        }
    }
    
    private fun mapToGeminiSituation(situation: ContextualFeedbackSystem.PlayingSituation): GeminiNanoManager.SwingSituation {
        return when (situation) {
            ContextualFeedbackSystem.PlayingSituation.DRIVING_RANGE -> GeminiNanoManager.SwingSituation.DRIVING_RANGE
            ContextualFeedbackSystem.PlayingSituation.COURSE_PLAY -> GeminiNanoManager.SwingSituation.COURSE_PLAY
            ContextualFeedbackSystem.PlayingSituation.PRACTICE_GREEN -> GeminiNanoManager.SwingSituation.PRACTICE_SESSION
            ContextualFeedbackSystem.PlayingSituation.TOURNAMENT_PLAY -> GeminiNanoManager.SwingSituation.COMPETITION
            else -> GeminiNanoManager.SwingSituation.PRACTICE_SESSION
        }
    }
    
    private fun mapToPracticeDrillSkillLevel(skillLevel: SkillLevelAdaptationSystem.SkillLevel): ProfessionalCoachingPrompts.SkillLevel {
        return when (skillLevel) {
            SkillLevelAdaptationSystem.SkillLevel.BEGINNER -> ProfessionalCoachingPrompts.SkillLevel.BEGINNER
            SkillLevelAdaptationSystem.SkillLevel.INTERMEDIATE -> ProfessionalCoachingPrompts.SkillLevel.INTERMEDIATE
            SkillLevelAdaptationSystem.SkillLevel.ADVANCED -> ProfessionalCoachingPrompts.SkillLevel.ADVANCED
            SkillLevelAdaptationSystem.SkillLevel.PROFESSIONAL -> ProfessionalCoachingPrompts.SkillLevel.PROFESSIONAL
        }
    }
    
    private fun mapToPracticeDrillClubType(clubType: GeminiNanoManager.ClubType): ProfessionalCoachingPrompts.ClubType {
        return when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> ProfessionalCoachingPrompts.ClubType.DRIVER
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> ProfessionalCoachingPrompts.ClubType.FAIRWAY_WOOD
            GeminiNanoManager.ClubType.HYBRID -> ProfessionalCoachingPrompts.ClubType.HYBRID
            GeminiNanoManager.ClubType.LONG_IRON -> ProfessionalCoachingPrompts.ClubType.LONG_IRON
            GeminiNanoManager.ClubType.MID_IRON -> ProfessionalCoachingPrompts.ClubType.MID_IRON
            GeminiNanoManager.ClubType.SHORT_IRON -> ProfessionalCoachingPrompts.ClubType.SHORT_IRON
            GeminiNanoManager.ClubType.WEDGE -> ProfessionalCoachingPrompts.ClubType.WEDGE
            GeminiNanoManager.ClubType.PUTTER -> ProfessionalCoachingPrompts.ClubType.PUTTER
        }
    }
    
    private fun mapToPracticeDifficulty(skillLevel: ProfessionalCoachingPrompts.SkillLevel): PracticeDifficulty {
        return when (skillLevel) {
            ProfessionalCoachingPrompts.SkillLevel.BEGINNER -> PracticeDifficulty.BEGINNER
            ProfessionalCoachingPrompts.SkillLevel.INTERMEDIATE -> PracticeDifficulty.INTERMEDIATE
            ProfessionalCoachingPrompts.SkillLevel.ADVANCED -> PracticeDifficulty.ADVANCED
            ProfessionalCoachingPrompts.SkillLevel.PROFESSIONAL -> PracticeDifficulty.ADVANCED
        }
    }
}