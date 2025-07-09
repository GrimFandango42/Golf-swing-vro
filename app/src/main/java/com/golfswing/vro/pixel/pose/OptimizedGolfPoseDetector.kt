package com.golfswing.vro.pixel.pose

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mediapipe.solutions.pose.Pose
import com.google.mediapipe.solutions.pose.PoseLandmark
import com.google.mediapipe.solutions.pose.PoseOptions
import com.google.mediapipe.solutions.pose.PoseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Optimized pose detector focused on golf swing accuracy and real-time performance
 * Prioritizes the essential biomechanics that make the biggest difference for golfers
 */
@Singleton
class OptimizedGolfPoseDetector @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Core pose detection
    private var mediapiPose: Pose? = null
    private val _poseResult = MutableStateFlow<GolfPoseResult?>(null)
    val poseResult: StateFlow<GolfPoseResult?> = _poseResult.asStateFlow()
    
    // Performance optimization
    private var lastProcessTime = 0L
    private val targetFPS = 30
    private val processingInterval = 1000L / targetFPS // 33ms for 30fps
    
    // Swing analysis state
    private val poseHistory = mutableListOf<GolfPoseResult>()
    private val maxHistorySize = 90 // 3 seconds at 30fps
    private var currentSwingPhase = SwingPhase.IDLE
    private var swingStartTime = 0L
    
    // Smoothing filters for stability
    private val shoulderAngleFilter = SimpleMovingAverage(5)
    private val hipAngleFilter = SimpleMovingAverage(5)
    private val balanceFilter = SimpleMovingAverage(3)
    
    data class GolfPoseResult(
        val landmarks: List<PoseLandmark>,
        val timestamp: Long,
        val swingPhase: SwingPhase,
        val coreMetrics: CoreGolfMetrics,
        val isValidPose: Boolean,
        val confidence: Float
    )
    
    data class CoreGolfMetrics(
        val xFactor: Float,           // Shoulder-hip separation (most important)
        val balance: Float,           // Weight distribution
        val posture: Float,           // Spine angle maintenance
        val tempo: Float,             // Swing rhythm
        val headStability: Float,     // Head movement
        val overallScore: Float       // Simple 0-10 score
    )
    
    enum class SwingPhase {
        IDLE,           // Not swinging
        ADDRESS,        // Setup position
        TAKEAWAY,       // First 18 inches of backswing
        BACKSWING,      // Main backswing
        TOP,            // Top of backswing
        TRANSITION,     // Start of downswing
        DOWNSWING,      // Main downswing
        IMPACT,         // Ball contact
        FOLLOW_THROUGH, // After impact
        FINISH          // End position
    }
    
    /**
     * Initialize the pose detector with optimized settings
     */
    fun initialize() {
        val poseOptions = PoseOptions.builder()
            .setModelComplexity(1) // Balance between speed and accuracy
            .setDetectionConfidence(0.5f) // Lower threshold for golf movements
            .setTrackingConfidence(0.5f)
            .setEnableSmoothing(true) // Enable temporal smoothing
            .build()
            
        mediapiPose = Pose(context, poseOptions)
        mediapiPose?.setResultListener { result -> processPoseResult(result) }
        mediapiPose?.setErrorListener { error -> 
            // Log error but don't crash
            println("Pose detection error: ${error.message}")
        }
    }
    
    /**
     * Process camera frame - optimized for 30fps
     */
    fun processFrame(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Frame rate limiting for consistent performance
        if (currentTime - lastProcessTime < processingInterval) {
            imageProxy.close()
            return
        }
        
        try {
            // Convert to bitmap efficiently
            val bitmap = imageProxyToBitmap(imageProxy)
            bitmap?.let { 
                mediapiPose?.send(it)
                lastProcessTime = currentTime
            }
        } catch (e: Exception) {
            println("Frame processing error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Process MediaPipe pose result and extract golf-specific insights
     */
    private fun processPoseResult(result: PoseResult) {
        if (result.poseLandmarks().isEmpty()) {
            _poseResult.value = null
            return
        }
        
        val landmarks = result.poseLandmarks()
        val timestamp = System.currentTimeMillis()
        
        // Validate pose quality for golf analysis
        val isValidPose = validateGolfPose(landmarks)
        if (!isValidPose) {
            _poseResult.value = null
            return
        }
        
        // Calculate core golf metrics
        val coreMetrics = calculateCoreMetrics(landmarks)
        
        // Detect swing phase
        val swingPhase = detectSwingPhase(landmarks, coreMetrics)
        
        // Calculate confidence based on landmark visibility
        val confidence = calculatePoseConfidence(landmarks)
        
        val golfPoseResult = GolfPoseResult(
            landmarks = landmarks,
            timestamp = timestamp,
            swingPhase = swingPhase,
            coreMetrics = coreMetrics,
            isValidPose = isValidPose,
            confidence = confidence
        )
        
        // Update history and emit result
        updatePoseHistory(golfPoseResult)
        _poseResult.value = golfPoseResult
    }
    
    /**
     * Validate if pose is suitable for golf swing analysis
     */
    private fun validateGolfPose(landmarks: List<PoseLandmark>): Boolean {
        // Check if key golf landmarks are visible
        val requiredLandmarks = listOf(
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.NOSE
        )
        
        return requiredLandmarks.all { landmarkType ->
            val landmark = landmarks.getOrNull(landmarkType.ordinal)
            landmark != null && landmark.visibility() > 0.3f
        }
    }
    
    /**
     * Calculate core golf metrics that matter most
     */
    private fun calculateCoreMetrics(landmarks: List<PoseLandmark>): CoreGolfMetrics {
        // X-Factor: Shoulder-hip separation (most important golf metric)
        val xFactor = calculateXFactor(landmarks)
        
        // Balance: Weight distribution and stability
        val balance = calculateBalance(landmarks)
        
        // Posture: Spine angle maintenance
        val posture = calculatePosture(landmarks)
        
        // Tempo: Based on movement velocity (if we have history)
        val tempo = calculateTempo(landmarks)
        
        // Head stability: Minimal head movement
        val headStability = calculateHeadStability(landmarks)
        
        // Overall score: Simple 0-10 based on core metrics
        val overallScore = calculateOverallScore(xFactor, balance, posture, headStability)
        
        return CoreGolfMetrics(
            xFactor = xFactor,
            balance = balance,
            posture = posture,
            tempo = tempo,
            headStability = headStability,
            overallScore = overallScore
        )
    }
    
    /**
     * Calculate X-Factor (shoulder-hip separation) - most important golf metric
     */
    private fun calculateXFactor(landmarks: List<PoseLandmark>): Float {
        val leftShoulder = landmarks.getOrNull(PoseLandmark.LEFT_SHOULDER.ordinal) ?: return 0f
        val rightShoulder = landmarks.getOrNull(PoseLandmark.RIGHT_SHOULDER.ordinal) ?: return 0f
        val leftHip = landmarks.getOrNull(PoseLandmark.LEFT_HIP.ordinal) ?: return 0f
        val rightHip = landmarks.getOrNull(PoseLandmark.RIGHT_HIP.ordinal) ?: return 0f
        
        // Calculate shoulder line angle
        val shoulderAngle = atan2(
            rightShoulder.y() - leftShoulder.y(),
            rightShoulder.x() - leftShoulder.x()
        ) * 180 / PI
        
        // Calculate hip line angle
        val hipAngle = atan2(
            rightHip.y() - leftHip.y(),
            rightHip.x() - leftHip.x()
        ) * 180 / PI
        
        // X-Factor is the difference (apply smoothing)
        val rawXFactor = abs(shoulderAngle - hipAngle).toFloat()
        
        // Apply smoothing filter
        shoulderAngleFilter.addValue(shoulderAngle.toFloat())
        hipAngleFilter.addValue(hipAngle.toFloat())
        
        val smoothedXFactor = abs(shoulderAngleFilter.getAverage() - hipAngleFilter.getAverage())
        
        // Clamp to reasonable range for golf (0-60 degrees)
        return smoothedXFactor.coerceIn(0f, 60f)
    }
    
    /**
     * Calculate balance/weight distribution
     */
    private fun calculateBalance(landmarks: List<PoseLandmark>): Float {
        val leftHip = landmarks.getOrNull(PoseLandmark.LEFT_HIP.ordinal) ?: return 0.5f
        val rightHip = landmarks.getOrNull(PoseLandmark.RIGHT_HIP.ordinal) ?: return 0.5f
        val leftAnkle = landmarks.getOrNull(PoseLandmark.LEFT_ANKLE.ordinal) ?: return 0.5f
        val rightAnkle = landmarks.getOrNull(PoseLandmark.RIGHT_ANKLE.ordinal) ?: return 0.5f
        
        // Calculate center of mass approximation
        val hipCenterX = (leftHip.x() + rightHip.x()) / 2
        val ankleCenterX = (leftAnkle.x() + rightAnkle.x()) / 2
        
        // Balance is how centered the hips are over the feet
        val balanceOffset = abs(hipCenterX - ankleCenterX)
        val rawBalance = 1f - (balanceOffset * 10f) // Scale factor
        
        // Apply smoothing
        val smoothedBalance = balanceFilter.addValue(rawBalance.coerceIn(0f, 1f))
        
        return smoothedBalance
    }
    
    /**
     * Calculate posture (spine angle maintenance)
     */
    private fun calculatePosture(landmarks: List<PoseLandmark>): Float {
        val nose = landmarks.getOrNull(PoseLandmark.NOSE.ordinal) ?: return 0.7f
        val leftShoulder = landmarks.getOrNull(PoseLandmark.LEFT_SHOULDER.ordinal) ?: return 0.7f
        val rightShoulder = landmarks.getOrNull(PoseLandmark.RIGHT_SHOULDER.ordinal) ?: return 0.7f
        val leftHip = landmarks.getOrNull(PoseLandmark.LEFT_HIP.ordinal) ?: return 0.7f
        val rightHip = landmarks.getOrNull(PoseLandmark.RIGHT_HIP.ordinal) ?: return 0.7f
        
        // Calculate approximate spine angle
        val shoulderMidpoint = Point(
            (leftShoulder.x() + rightShoulder.x()) / 2,
            (leftShoulder.y() + rightShoulder.y()) / 2
        )
        
        val hipMidpoint = Point(
            (leftHip.x() + rightHip.x()) / 2,
            (leftHip.y() + rightHip.y()) / 2
        )
        
        // Good golf posture maintains spine angle
        val spineAngle = atan2(
            shoulderMidpoint.y - hipMidpoint.y,
            shoulderMidpoint.x - hipMidpoint.x
        ) * 180 / PI
        
        // Good golf posture is around 30-45 degrees forward lean
        val idealSpineAngle = 37.5 // Degrees
        val angleDifference = abs(abs(spineAngle) - idealSpineAngle)
        
        // Convert to 0-1 score
        return (1f - (angleDifference / 45f)).coerceIn(0f, 1f)
    }
    
    /**
     * Calculate tempo based on movement velocity
     */
    private fun calculateTempo(landmarks: List<PoseLandmark>): Float {
        if (poseHistory.size < 2) return 0.7f
        
        // Use hand movement for tempo (simplified)
        val currentLeftHand = landmarks.getOrNull(PoseLandmark.LEFT_WRIST.ordinal)
        val previousResult = poseHistory.lastOrNull()
        val previousLeftHand = previousResult?.landmarks?.getOrNull(PoseLandmark.LEFT_WRIST.ordinal)
        
        if (currentLeftHand == null || previousLeftHand == null) return 0.7f
        
        // Calculate velocity
        val distance = sqrt(
            (currentLeftHand.x() - previousLeftHand.x()).pow(2) +
            (currentLeftHand.y() - previousLeftHand.y()).pow(2)
        )
        
        val timeInterval = (System.currentTimeMillis() - (previousResult?.timestamp ?: 0)) / 1000f
        val velocity = if (timeInterval > 0) distance / timeInterval else 0f
        
        // Good tempo is smooth and controlled (not too fast, not too slow)
        return when {
            velocity < 0.1f -> 0.3f  // Too slow
            velocity > 2.0f -> 0.3f  // Too fast
            else -> 1f - abs(velocity - 0.8f) / 0.8f // Optimal around 0.8
        }.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate head stability (minimal movement is good)
     */
    private fun calculateHeadStability(landmarks: List<PoseLandmark>): Float {
        val nose = landmarks.getOrNull(PoseLandmark.NOSE.ordinal) ?: return 0.7f
        
        if (poseHistory.size < 5) return 0.7f
        
        // Calculate head movement over recent frames
        val recentNosePositions = poseHistory.takeLast(5).mapNotNull { result ->
            result.landmarks.getOrNull(PoseLandmark.NOSE.ordinal)
        }
        
        if (recentNosePositions.size < 3) return 0.7f
        
        // Calculate variance in head position
        val avgX = recentNosePositions.map { it.x() }.average()
        val avgY = recentNosePositions.map { it.y() }.average()
        
        val variance = recentNosePositions.map { pos ->
            (pos.x() - avgX).pow(2) + (pos.y() - avgY).pow(2)
        }.average()
        
        // Lower variance = better stability
        val stability = 1f - (variance * 100f).toFloat() // Scale factor
        return stability.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate simple overall score (0-10)
     */
    private fun calculateOverallScore(
        xFactor: Float,
        balance: Float, 
        posture: Float,
        headStability: Float
    ): Float {
        // Weight the most important metrics for golf
        val xFactorScore = when {
            xFactor in 25f..45f -> 2.5f  // Optimal X-Factor range
            xFactor in 15f..55f -> 2.0f  // Good range
            else -> 1.0f                 // Needs work
        }
        
        val balanceScore = balance * 2.5f        // Balance is critical
        val postureScore = posture * 2.0f        // Posture important
        val headScore = headStability * 3.0f     // Head stability very important
        
        val totalScore = xFactorScore + balanceScore + postureScore + headScore
        return totalScore.coerceIn(0f, 10f)
    }
    
    /**
     * Detect current swing phase
     */
    private fun detectSwingPhase(landmarks: List<PoseLandmark>, metrics: CoreGolfMetrics): SwingPhase {
        // Simplified phase detection based on movement patterns
        if (poseHistory.size < 3) return SwingPhase.ADDRESS
        
        val leftWrist = landmarks.getOrNull(PoseLandmark.LEFT_WRIST.ordinal)
        val previousWrist = poseHistory.lastOrNull()?.landmarks?.getOrNull(PoseLandmark.LEFT_WRIST.ordinal)
        
        if (leftWrist == null || previousWrist == null) return currentSwingPhase
        
        // Calculate movement direction and speed
        val verticalMovement = leftWrist.y() - previousWrist.y()
        val movementSpeed = sqrt(
            (leftWrist.x() - previousWrist.x()).pow(2) +
            (leftWrist.y() - previousWrist.y()).pow(2)
        )
        
        // Simple state machine for swing phases
        return when (currentSwingPhase) {
            SwingPhase.IDLE, SwingPhase.ADDRESS -> {
                if (movementSpeed > 0.02f) SwingPhase.TAKEAWAY else SwingPhase.ADDRESS
            }
            SwingPhase.TAKEAWAY -> {
                if (verticalMovement < -0.01f) SwingPhase.BACKSWING else SwingPhase.TAKEAWAY
            }
            SwingPhase.BACKSWING -> {
                if (movementSpeed < 0.01f) SwingPhase.TOP else SwingPhase.BACKSWING
            }
            SwingPhase.TOP -> {
                if (verticalMovement > 0.01f) SwingPhase.DOWNSWING else SwingPhase.TOP
            }
            SwingPhase.DOWNSWING -> {
                if (movementSpeed > 0.05f) SwingPhase.IMPACT else SwingPhase.DOWNSWING
            }
            SwingPhase.IMPACT -> {
                if (verticalMovement > 0.02f) SwingPhase.FOLLOW_THROUGH else SwingPhase.IMPACT
            }
            SwingPhase.FOLLOW_THROUGH -> {
                if (movementSpeed < 0.02f) SwingPhase.FINISH else SwingPhase.FOLLOW_THROUGH
            }
            SwingPhase.FINISH -> {
                if (movementSpeed < 0.01f && System.currentTimeMillis() - swingStartTime > 3000) {
                    SwingPhase.IDLE
                } else SwingPhase.FINISH
            }
            else -> SwingPhase.IDLE
        }.also { newPhase ->
            if (newPhase != currentSwingPhase && newPhase == SwingPhase.TAKEAWAY) {
                swingStartTime = System.currentTimeMillis()
            }
            currentSwingPhase = newPhase
        }
    }
    
    /**
     * Calculate pose confidence based on landmark visibility
     */
    private fun calculatePoseConfidence(landmarks: List<PoseLandmark>): Float {
        val keyLandmarks = listOf(
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_WRIST
        )
        
        val visibilityScores = keyLandmarks.mapNotNull { landmarkType ->
            landmarks.getOrNull(landmarkType.ordinal)?.visibility()
        }
        
        return if (visibilityScores.isNotEmpty()) {
            visibilityScores.average().toFloat()
        } else 0f
    }
    
    /**
     * Update pose history for temporal analysis
     */
    private fun updatePoseHistory(result: GolfPoseResult) {
        poseHistory.add(result)
        if (poseHistory.size > maxHistorySize) {
            poseHistory.removeAt(0)
        }
    }
    
    /**
     * Convert ImageProxy to Bitmap efficiently
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            // This would be implemented with efficient YUV to RGB conversion
            // For now, returning null to avoid crashes in demo
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        mediapiPose?.close()
        mediapiPose = null
        poseHistory.clear()
    }
    
    // Helper classes
    private data class Point(val x: Float, val y: Float)
    
    /**
     * Simple moving average filter for smoothing
     */
    private class SimpleMovingAverage(private val windowSize: Int) {
        private val values = mutableListOf<Float>()
        
        fun addValue(value: Float): Float {
            values.add(value)
            if (values.size > windowSize) {
                values.removeAt(0)
            }
            return getAverage()
        }
        
        fun getAverage(): Float {
            return if (values.isNotEmpty()) {
                values.average().toFloat()
            } else 0f
        }
    }
}