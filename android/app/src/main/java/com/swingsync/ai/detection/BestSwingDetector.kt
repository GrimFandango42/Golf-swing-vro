package com.swingsync.ai.detection

import android.util.Log
import com.swingsync.ai.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * BestSwingDetector - Intelligent system for detecting exceptional swings
 * 
 * This system uses multiple criteria to assess swing quality and automatically
 * detects when users hit great shots or show significant improvement.
 * 
 * Detection Criteria:
 * - Biomechanical Excellence: Perfect angles, timing, and sequence
 * - Consistency Score: Repeatability across multiple swings
 * - Improvement Rate: Progress over time
 * - Tempo and Rhythm: Smooth, professional timing
 * - Balance and Stability: Stable foundation throughout swing
 * - Power Generation: Efficient energy transfer
 * - Precision Metrics: Accuracy and control
 * 
 * The system learns from each swing to provide personalized quality assessment.
 */
@Singleton
class BestSwingDetector @Inject constructor() {
    
    companion object {
        private const val TAG = "BestSwingDetector"
        
        // Quality thresholds
        private const val EXCELLENT_THRESHOLD = 0.85f
        private const val GREAT_THRESHOLD = 0.75f
        private const val GOOD_THRESHOLD = 0.65f
        private const val IMPROVEMENT_THRESHOLD = 0.10f
        private const val CONSISTENCY_THRESHOLD = 0.80f
        
        // Weighting factors for different criteria
        private const val BIOMECHANICS_WEIGHT = 0.30f
        private const val TEMPO_WEIGHT = 0.20f
        private const val BALANCE_WEIGHT = 0.20f
        private const val POWER_WEIGHT = 0.15f
        private const val PRECISION_WEIGHT = 0.15f
        
        // Temporal analysis windows
        private const val RECENT_SWINGS_WINDOW = 10
        private const val IMPROVEMENT_WINDOW = 20
        private const val CONSISTENCY_WINDOW = 5
    }
    
    // State management
    private val _swingQuality = MutableStateFlow(SwingQuality.UNKNOWN)
    val swingQuality: StateFlow<SwingQuality> = _swingQuality.asStateFlow()
    
    private val _bestSwingDetected = MutableStateFlow<BestSwingEvent?>(null)
    val bestSwingDetected: StateFlow<BestSwingEvent?> = _bestSwingDetected.asStateFlow()
    
    private val _improvementDetected = MutableStateFlow<ImprovementEvent?>(null)
    val improvementDetected: StateFlow<ImprovementEvent?> = _improvementDetected.asStateFlow()
    
    // Historical data
    private val swingHistory = mutableListOf<SwingAnalysisResult>()
    private val userBaselines = mutableMapOf<String, UserBaseline>()
    
    /**
     * Analyze a swing and determine if it's worthy of celebration
     */
    fun analyzeSwing(
        swingData: SwingVideoAnalysisInput,
        analysisResult: SwingAnalysisFeedback
    ): SwingQualityAssessment {
        Log.d(TAG, "Analyzing swing for quality assessment")
        
        // Extract biomechanical data
        val biomechanicalScore = calculateBiomechanicalScore(swingData, analysisResult)
        val tempoScore = calculateTempoScore(swingData)
        val balanceScore = calculateBalanceScore(swingData)
        val powerScore = calculatePowerScore(swingData)
        val precisionScore = calculatePrecisionScore(swingData, analysisResult)
        
        // Calculate weighted overall score
        val overallScore = (
            biomechanicalScore * BIOMECHANICS_WEIGHT +
            tempoScore * TEMPO_WEIGHT +
            balanceScore * BALANCE_WEIGHT +
            powerScore * POWER_WEIGHT +
            precisionScore * PRECISION_WEIGHT
        )
        
        // Determine quality level
        val qualityLevel = when {
            overallScore >= EXCELLENT_THRESHOLD -> SwingQuality.EXCELLENT
            overallScore >= GREAT_THRESHOLD -> SwingQuality.GREAT
            overallScore >= GOOD_THRESHOLD -> SwingQuality.GOOD
            else -> SwingQuality.NEEDS_WORK
        }
        
        // Create detailed assessment
        val assessment = SwingQualityAssessment(
            sessionId = swingData.sessionId,
            userId = swingData.userId,
            timestamp = System.currentTimeMillis(),
            overallScore = overallScore,
            qualityLevel = qualityLevel,
            biomechanicalScore = biomechanicalScore,
            tempoScore = tempoScore,
            balanceScore = balanceScore,
            powerScore = powerScore,
            precisionScore = precisionScore,
            strengths = identifyStrengths(biomechanicalScore, tempoScore, balanceScore, powerScore, precisionScore),
            improvementAreas = identifyImprovementAreas(biomechanicalScore, tempoScore, balanceScore, powerScore, precisionScore),
            celebrationTrigger = determineCelebrationTrigger(qualityLevel, overallScore)
        )
        
        // Add to history
        val analysisResult = SwingAnalysisResult(
            sessionId = swingData.sessionId,
            userId = swingData.userId,
            timestamp = System.currentTimeMillis(),
            score = overallScore,
            qualityLevel = qualityLevel,
            clubUsed = swingData.clubUsed
        )
        swingHistory.add(analysisResult)
        
        // Check for best swing
        checkForBestSwing(assessment, analysisResult)
        
        // Check for improvement
        checkForImprovement(assessment, analysisResult)
        
        // Update quality state
        _swingQuality.value = qualityLevel
        
        return assessment
    }
    
    /**
     * Calculate biomechanical score based on P-System positions and KPI values
     */
    private fun calculateBiomechanicalScore(
        swingData: SwingVideoAnalysisInput,
        analysisResult: SwingAnalysisFeedback
    ): Float {
        var score = 1.0f
        
        // Analyze detected faults
        val faultSeverity = analysisResult.rawDetectedFaults.sumOf { 
            (it.severity ?: 0.5f).toDouble() 
        } / max(1, analysisResult.rawDetectedFaults.size)
        
        // Reduce score based on fault severity
        score -= faultSeverity.toFloat() * 0.3f
        
        // Analyze P-System phases
        val phaseQuality = analyzePSystemPhases(swingData.pSystemClassification)
        score *= phaseQuality
        
        // Analyze pose consistency
        val poseConsistency = analyzePoseConsistency(swingData.frames)
        score *= poseConsistency
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate tempo score based on swing timing
     */
    private fun calculateTempoScore(swingData: SwingVideoAnalysisInput): Float {
        val phases = swingData.pSystemClassification
        if (phases.size < 2) return 0.5f
        
        // Calculate phase durations
        val phaseDurations = phases.zipWithNext { current, next ->
            (next.startFrameIndex - current.endFrameIndex) / swingData.videoFps
        }
        
        // Ideal tempo ratios (backswing:downswing should be about 3:1)
        val backswingDuration = phaseDurations.take(3).sum() // P1-P4
        val downswingDuration = phaseDurations.drop(3).take(3).sum() // P4-P7
        
        if (backswingDuration == 0f || downswingDuration == 0f) return 0.5f
        
        val tempoRatio = backswingDuration / downswingDuration
        val idealRatio = 3.0f
        
        // Score based on how close to ideal ratio
        val ratioScore = 1f - min(1f, abs(tempoRatio - idealRatio) / idealRatio)
        
        // Also consider overall swing tempo
        val totalDuration = phaseDurations.sum()
        val idealTotalDuration = 2.5f // seconds
        val durationScore = 1f - min(1f, abs(totalDuration - idealTotalDuration) / idealTotalDuration)
        
        return (ratioScore + durationScore) / 2f
    }
    
    /**
     * Calculate balance score based on pose stability
     */
    private fun calculateBalanceScore(swingData: SwingVideoAnalysisInput): Float {
        val frames = swingData.frames
        if (frames.isEmpty()) return 0.5f
        
        // Track center of mass movement
        val centerOfMassPoints = frames.mapNotNull { frame ->
            calculateCenterOfMass(frame)
        }
        
        if (centerOfMassPoints.size < 2) return 0.5f
        
        // Calculate stability metrics
        val lateralMovement = calculateLateralMovement(centerOfMassPoints)
        val verticalStability = calculateVerticalStability(centerOfMassPoints)
        val weightTransfer = calculateWeightTransfer(frames)
        
        // Combine metrics
        val stabilityScore = 1f - min(1f, lateralMovement / 0.2f) // 20cm max lateral movement
        val verticalScore = 1f - min(1f, verticalStability / 0.1f) // 10cm max vertical variation
        val transferScore = weightTransfer // Already normalized
        
        return (stabilityScore + verticalScore + transferScore) / 3f
    }
    
    /**
     * Calculate power score based on clubhead speed and sequence
     */
    private fun calculatePowerScore(swingData: SwingVideoAnalysisInput): Float {
        val frames = swingData.frames
        if (frames.isEmpty()) return 0.5f
        
        // Calculate kinetic chain sequence
        val sequenceScore = calculateKineticChainSequence(frames)
        
        // Calculate speed buildup
        val speedScore = calculateSpeedBuildup(frames)
        
        // Calculate energy transfer efficiency
        val efficiencyScore = calculateEnergyTransferEfficiency(frames)
        
        return (sequenceScore + speedScore + efficiencyScore) / 3f
    }
    
    /**
     * Calculate precision score based on swing plane consistency
     */
    private fun calculatePrecisionScore(
        swingData: SwingVideoAnalysisInput,
        analysisResult: SwingAnalysisFeedback
    ): Float {
        val frames = swingData.frames
        if (frames.isEmpty()) return 0.5f
        
        // Calculate swing plane consistency
        val planeConsistency = calculateSwingPlaneConsistency(frames)
        
        // Calculate club path accuracy
        val pathAccuracy = calculateClubPathAccuracy(frames)
        
        // Calculate face angle control
        val faceControl = calculateFaceAngleControl(frames)
        
        // Factor in detected faults related to precision
        val precisionFaults = analysisResult.rawDetectedFaults.filter { fault ->
            fault.faultId.contains("PLANE") || fault.faultId.contains("PATH") || fault.faultId.contains("FACE")
        }
        val faultPenalty = precisionFaults.size * 0.1f
        
        val baseScore = (planeConsistency + pathAccuracy + faceControl) / 3f
        return max(0f, baseScore - faultPenalty)
    }
    
    /**
     * Check if this swing qualifies as a "best swing"
     */
    private fun checkForBestSwing(assessment: SwingQualityAssessment, result: SwingAnalysisResult) {
        val userHistory = swingHistory.filter { it.userId == result.userId }
        
        if (userHistory.isEmpty()) return
        
        // Check if this is a new personal best
        val previousBest = userHistory.maxByOrNull { it.score }?.score ?: 0f
        val isNewBest = result.score > previousBest
        
        // Check if this is in top percentile
        val recentSwings = userHistory.takeLast(RECENT_SWINGS_WINDOW)
        val topPercentile = recentSwings.sortedByDescending { it.score }.take(2)
        val isTopPercentile = topPercentile.any { it.sessionId == result.sessionId }
        
        // Check consistency with recent good swings
        val consistentQuality = checkConsistentQuality(recentSwings, result)
        
        if (isNewBest || isTopPercentile || consistentQuality) {
            val event = BestSwingEvent(
                sessionId = result.sessionId,
                userId = result.userId,
                timestamp = result.timestamp,
                score = result.score,
                qualityLevel = result.qualityLevel,
                isPersonalBest = isNewBest,
                isTopPercentile = isTopPercentile,
                isConsistentExcellence = consistentQuality,
                celebrationLevel = determineCelebrationLevel(result.score, isNewBest, isTopPercentile, consistentQuality),
                strengths = assessment.strengths,
                message = generateBestSwingMessage(result.score, isNewBest, isTopPercentile, consistentQuality)
            )
            
            _bestSwingDetected.value = event
        }
    }
    
    /**
     * Check for improvement trends
     */
    private fun checkForImprovement(assessment: SwingQualityAssessment, result: SwingAnalysisResult) {
        val userHistory = swingHistory.filter { it.userId == result.userId }
        
        if (userHistory.size < IMPROVEMENT_WINDOW) return
        
        val recentSwings = userHistory.takeLast(IMPROVEMENT_WINDOW)
        val oldAverage = recentSwings.take(IMPROVEMENT_WINDOW / 2).map { it.score }.average()
        val newAverage = recentSwings.drop(IMPROVEMENT_WINDOW / 2).map { it.score }.average()
        
        val improvement = newAverage - oldAverage
        
        if (improvement >= IMPROVEMENT_THRESHOLD) {
            val event = ImprovementEvent(
                userId = result.userId,
                timestamp = result.timestamp,
                improvementAmount = improvement.toFloat(),
                oldAverage = oldAverage.toFloat(),
                newAverage = newAverage.toFloat(),
                improvementArea = identifyMainImprovementArea(recentSwings),
                celebrationLevel = CelebrationLevel.IMPROVEMENT,
                message = generateImprovementMessage(improvement.toFloat(), identifyMainImprovementArea(recentSwings))
            )
            
            _improvementDetected.value = event
        }
    }
    
    /**
     * Analyze P-System phases for quality
     */
    private fun analyzePSystemPhases(phases: List<PSystemPhase>): Float {
        if (phases.isEmpty()) return 0.5f
        
        // Check if all major phases are present
        val expectedPhases = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10")
        val presentPhases = phases.map { it.phaseName }
        val completeness = presentPhases.intersect(expectedPhases).size.toFloat() / expectedPhases.size
        
        // Check phase timing ratios
        val phaseDurations = phases.map { it.endFrameIndex - it.startFrameIndex }
        val timingConsistency = if (phaseDurations.isNotEmpty()) {
            1f - (phaseDurations.standardDeviation() / phaseDurations.average()).toFloat()
        } else 0.5f
        
        return (completeness + timingConsistency.coerceIn(0f, 1f)) / 2f
    }
    
    /**
     * Analyze pose consistency across frames
     */
    private fun analyzePoseConsistency(frames: List<FramePoseData>): Float {
        if (frames.size < 2) return 0.5f
        
        // Calculate pose stability for key points
        val keyPoints = listOf(
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP
        )
        
        val consistencyScores = keyPoints.map { keyPoint ->
            val positions = frames.mapNotNull { frame ->
                frame[keyPoint]?.let { Point3D(it.x, it.y, it.z) }
            }
            
            if (positions.size < 2) return@map 0.5f
            
            val movements = positions.zipWithNext { p1, p2 ->
                sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2) + (p1.z - p2.z).pow(2))
            }
            
            // Lower movement variance = higher consistency
            1f - min(1f, movements.standardDeviation() / movements.average()).toFloat()
        }
        
        return consistencyScores.average().toFloat()
    }
    
    /**
     * Calculate center of mass for a frame
     */
    private fun calculateCenterOfMass(frame: FramePoseData): Point3D? {
        val bodyPoints = listOf(
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP
        )
        
        val points = bodyPoints.mapNotNull { frame[it] }
        if (points.isEmpty()) return null
        
        val avgX = points.map { it.x }.average().toFloat()
        val avgY = points.map { it.y }.average().toFloat()
        val avgZ = points.map { it.z }.average().toFloat()
        
        return Point3D(avgX, avgY, avgZ)
    }
    
    /**
     * Calculate lateral movement of center of mass
     */
    private fun calculateLateralMovement(centerOfMassPoints: List<Point3D>): Float {
        if (centerOfMassPoints.size < 2) return 0f
        
        val lateralPositions = centerOfMassPoints.map { it.x }
        val range = lateralPositions.maxOrNull()!! - lateralPositions.minOrNull()!!
        
        return range
    }
    
    /**
     * Calculate vertical stability
     */
    private fun calculateVerticalStability(centerOfMassPoints: List<Point3D>): Float {
        if (centerOfMassPoints.size < 2) return 0f
        
        val verticalPositions = centerOfMassPoints.map { it.y }
        return verticalPositions.standardDeviation().toFloat()
    }
    
    /**
     * Calculate weight transfer quality
     */
    private fun calculateWeightTransfer(frames: List<FramePoseData>): Float {
        // Simplified weight transfer analysis based on hip movement
        val hipMovements = frames.mapNotNull { frame ->
            val leftHip = frame[MediaPipePoseLandmarks.LEFT_HIP]
            val rightHip = frame[MediaPipePoseLandmarks.RIGHT_HIP]
            
            if (leftHip != null && rightHip != null) {
                Point3D((leftHip.x + rightHip.x) / 2, (leftHip.y + rightHip.y) / 2, (leftHip.z + rightHip.z) / 2)
            } else null
        }
        
        if (hipMovements.size < 3) return 0.5f
        
        // Good weight transfer shows controlled hip movement
        val hipMovementRange = hipMovements.map { it.x }.let { positions ->
            positions.maxOrNull()!! - positions.minOrNull()!!
        }
        
        // Optimal hip movement is neither too little nor too much
        val optimalRange = 0.15f // 15cm lateral hip movement
        return 1f - min(1f, abs(hipMovementRange - optimalRange) / optimalRange)
    }
    
    /**
     * Calculate kinetic chain sequence quality
     */
    private fun calculateKineticChainSequence(frames: List<FramePoseData>): Float {
        // Simplified sequence analysis - proper sequence is hips -> shoulders -> arms
        // This would require more sophisticated analysis in a real implementation
        return 0.8f // Placeholder
    }
    
    /**
     * Calculate speed buildup through the swing
     */
    private fun calculateSpeedBuildup(frames: List<FramePoseData>): Float {
        // Analyze wrist speed as proxy for clubhead speed
        val wristSpeeds = frames.zipWithNext { frame1, frame2 ->
            val wrist1 = frame1[MediaPipePoseLandmarks.LEFT_WRIST]
            val wrist2 = frame2[MediaPipePoseLandmarks.LEFT_WRIST]
            
            if (wrist1 != null && wrist2 != null) {
                sqrt((wrist1.x - wrist2.x).pow(2) + (wrist1.y - wrist2.y).pow(2) + (wrist1.z - wrist2.z).pow(2))
            } else 0f
        }
        
        if (wristSpeeds.isEmpty()) return 0.5f
        
        // Good swing shows increasing speed toward impact
        val midPoint = wristSpeeds.size / 2
        val backswingSpeed = wristSpeeds.take(midPoint).average()
        val downswingSpeed = wristSpeeds.drop(midPoint).average()
        
        return min(1f, (downswingSpeed / max(0.001, backswingSpeed)).toFloat() / 3f)
    }
    
    /**
     * Calculate energy transfer efficiency
     */
    private fun calculateEnergyTransferEfficiency(frames: List<FramePoseData>): Float {
        // Simplified efficiency calculation
        // Real implementation would analyze the sequential activation of body segments
        return 0.75f // Placeholder
    }
    
    /**
     * Calculate swing plane consistency
     */
    private fun calculateSwingPlaneConsistency(frames: List<FramePoseData>): Float {
        // Analyze shoulder and wrist positions to determine swing plane
        val swingPlanePoints = frames.mapNotNull { frame ->
            val leftShoulder = frame[MediaPipePoseLandmarks.LEFT_SHOULDER]
            val rightShoulder = frame[MediaPipePoseLandmarks.RIGHT_SHOULDER]
            val leftWrist = frame[MediaPipePoseLandmarks.LEFT_WRIST]
            
            if (leftShoulder != null && rightShoulder != null && leftWrist != null) {
                // Calculate plane deviation
                listOf(leftShoulder, rightShoulder, leftWrist)
            } else null
        }
        
        if (swingPlanePoints.isEmpty()) return 0.5f
        
        // Calculate plane consistency (simplified)
        return 0.8f // Placeholder - would need 3D plane analysis
    }
    
    /**
     * Calculate club path accuracy
     */
    private fun calculateClubPathAccuracy(frames: List<FramePoseData>): Float {
        // Analyze wrist path as proxy for club path
        val wristPath = frames.mapNotNull { frame ->
            frame[MediaPipePoseLandmarks.LEFT_WRIST]?.let { Point3D(it.x, it.y, it.z) }
        }
        
        if (wristPath.size < 3) return 0.5f
        
        // Calculate path smoothness
        val pathDeviations = wristPath.windowed(3).map { triplet ->
            // Calculate deviation from straight line between first and last point
            val start = triplet.first()
            val end = triplet.last()
            val middle = triplet[1]
            
            // Distance from middle point to line between start and end
            distancePointToLine(middle, start, end)
        }
        
        val avgDeviation = pathDeviations.average().toFloat()
        return 1f - min(1f, avgDeviation / 0.1f) // 10cm max deviation
    }
    
    /**
     * Calculate face angle control
     */
    private fun calculateFaceAngleControl(frames: List<FramePoseData>): Float {
        // Simplified face angle analysis based on wrist rotation
        // Real implementation would need more sophisticated analysis
        return 0.7f // Placeholder
    }
    
    /**
     * Identify swing strengths
     */
    private fun identifyStrengths(
        biomechanical: Float,
        tempo: Float,
        balance: Float,
        power: Float,
        precision: Float
    ): List<String> {
        val strengths = mutableListOf<String>()
        
        if (biomechanical >= 0.8f) strengths.add("Excellent biomechanics")
        if (tempo >= 0.8f) strengths.add("Perfect tempo")
        if (balance >= 0.8f) strengths.add("Great balance")
        if (power >= 0.8f) strengths.add("Powerful swing")
        if (precision >= 0.8f) strengths.add("Precise control")
        
        return strengths
    }
    
    /**
     * Identify improvement areas
     */
    private fun identifyImprovementAreas(
        biomechanical: Float,
        tempo: Float,
        balance: Float,
        power: Float,
        precision: Float
    ): List<String> {
        val areas = mutableListOf<String>()
        
        if (biomechanical < 0.6f) areas.add("Biomechanics")
        if (tempo < 0.6f) areas.add("Tempo")
        if (balance < 0.6f) areas.add("Balance")
        if (power < 0.6f) areas.add("Power generation")
        if (precision < 0.6f) areas.add("Precision")
        
        return areas
    }
    
    /**
     * Determine celebration trigger type
     */
    private fun determineCelebrationTrigger(quality: SwingQuality, score: Float): CelebrationTrigger {
        return when {
            quality == SwingQuality.EXCELLENT -> CelebrationTrigger.EXCELLENCE
            quality == SwingQuality.GREAT -> CelebrationTrigger.GREAT_SHOT
            score > 0.8f -> CelebrationTrigger.IMPROVEMENT
            else -> CelebrationTrigger.NONE
        }
    }
    
    /**
     * Check for consistent quality
     */
    private fun checkConsistentQuality(recentSwings: List<SwingAnalysisResult>, currentSwing: SwingAnalysisResult): Boolean {
        if (recentSwings.size < CONSISTENCY_WINDOW) return false
        
        val lastFiveSwings = recentSwings.takeLast(CONSISTENCY_WINDOW)
        val avgScore = lastFiveSwings.map { it.score }.average().toFloat()
        
        return avgScore >= CONSISTENCY_THRESHOLD && currentSwing.score >= CONSISTENCY_THRESHOLD
    }
    
    /**
     * Determine celebration level
     */
    private fun determineCelebrationLevel(
        score: Float,
        isNewBest: Boolean,
        isTopPercentile: Boolean,
        isConsistentExcellence: Boolean
    ): CelebrationLevel {
        return when {
            isNewBest && score >= 0.95f -> CelebrationLevel.LEGENDARY
            isNewBest && score >= 0.85f -> CelebrationLevel.EPIC
            isTopPercentile && isConsistentExcellence -> CelebrationLevel.EXCELLENCE
            isTopPercentile -> CelebrationLevel.GREAT
            score >= 0.8f -> CelebrationLevel.GOOD
            else -> CelebrationLevel.ENCOURAGING
        }
    }
    
    /**
     * Generate best swing message
     */
    private fun generateBestSwingMessage(
        score: Float,
        isNewBest: Boolean,
        isTopPercentile: Boolean,
        isConsistentExcellence: Boolean
    ): String {
        return when {
            isNewBest && score >= 0.95f -> "🏆 LEGENDARY SWING! New personal best!"
            isNewBest -> "🌟 NEW PERSONAL BEST! Outstanding swing!"
            isConsistentExcellence -> "🎯 CONSISTENT EXCELLENCE! You're on fire!"
            isTopPercentile -> "💎 EXCEPTIONAL SWING! Top-tier performance!"
            else -> "⭐ GREAT SWING! Beautiful execution!"
        }
    }
    
    /**
     * Identify main improvement area
     */
    private fun identifyMainImprovementArea(swings: List<SwingAnalysisResult>): String {
        // This would analyze which aspect improved most
        return "Overall technique"
    }
    
    /**
     * Generate improvement message
     */
    private fun generateImprovementMessage(improvement: Float, area: String): String {
        val percent = (improvement * 100).toInt()
        return "🚀 AMAZING PROGRESS! $percent% improvement in $area!"
    }
    
    /**
     * Calculate distance from point to line
     */
    private fun distancePointToLine(point: Point3D, lineStart: Point3D, lineEnd: Point3D): Float {
        val lineVec = Point3D(lineEnd.x - lineStart.x, lineEnd.y - lineStart.y, lineEnd.z - lineStart.z)
        val pointVec = Point3D(point.x - lineStart.x, point.y - lineStart.y, point.z - lineStart.z)
        
        val lineLength = sqrt(lineVec.x.pow(2) + lineVec.y.pow(2) + lineVec.z.pow(2))
        if (lineLength == 0f) return 0f
        
        val crossProduct = sqrt(
            (pointVec.y * lineVec.z - pointVec.z * lineVec.y).pow(2) +
            (pointVec.z * lineVec.x - pointVec.x * lineVec.z).pow(2) +
            (pointVec.x * lineVec.y - pointVec.y * lineVec.x).pow(2)
        )
        
        return crossProduct / lineLength
    }
    
    /**
     * Clear best swing event
     */
    fun clearBestSwingEvent() {
        _bestSwingDetected.value = null
    }
    
    /**
     * Clear improvement event
     */
    fun clearImprovementEvent() {
        _improvementDetected.value = null
    }
}

// Data classes for swing analysis
data class SwingQualityAssessment(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,
    val overallScore: Float,
    val qualityLevel: SwingQuality,
    val biomechanicalScore: Float,
    val tempoScore: Float,
    val balanceScore: Float,
    val powerScore: Float,
    val precisionScore: Float,
    val strengths: List<String>,
    val improvementAreas: List<String>,
    val celebrationTrigger: CelebrationTrigger
)

data class SwingAnalysisResult(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,
    val score: Float,
    val qualityLevel: SwingQuality,
    val clubUsed: String
)

data class BestSwingEvent(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,
    val score: Float,
    val qualityLevel: SwingQuality,
    val isPersonalBest: Boolean,
    val isTopPercentile: Boolean,
    val isConsistentExcellence: Boolean,
    val celebrationLevel: CelebrationLevel,
    val strengths: List<String>,
    val message: String
)

data class ImprovementEvent(
    val userId: String,
    val timestamp: Long,
    val improvementAmount: Float,
    val oldAverage: Float,
    val newAverage: Float,
    val improvementArea: String,
    val celebrationLevel: CelebrationLevel,
    val message: String
)

data class UserBaseline(
    val userId: String,
    val averageScore: Float,
    val bestScore: Float,
    val swingCount: Int,
    val lastUpdated: Long
)

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
)

enum class SwingQuality {
    UNKNOWN,
    NEEDS_WORK,
    GOOD,
    GREAT,
    EXCELLENT
}

enum class CelebrationTrigger {
    NONE,
    IMPROVEMENT,
    GREAT_SHOT,
    EXCELLENCE,
    PERSONAL_BEST
}

enum class CelebrationLevel {
    ENCOURAGING,
    GOOD,
    GREAT,
    EXCELLENCE,
    EPIC,
    LEGENDARY
}

// Extension functions
fun List<Float>.standardDeviation(): Double {
    if (isEmpty()) return 0.0
    val mean = average()
    return sqrt(map { (it - mean).pow(2) }.average())
}

fun List<Double>.standardDeviation(): Double {
    if (isEmpty()) return 0.0
    val mean = average()
    return sqrt(map { (it - mean).pow(2) }.average())
}