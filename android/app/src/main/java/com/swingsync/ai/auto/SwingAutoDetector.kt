package com.swingsync.ai.auto

import android.util.Log
import com.swingsync.ai.data.model.GolfSwingPhase
import com.swingsync.ai.data.model.MediaPipePoseLandmarks
import com.swingsync.ai.data.model.PoseDetectionResult
import com.swingsync.ai.data.model.PoseKeypoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * SwingAutoDetector - Intelligent swing detection and recording optimization
 * 
 * Features:
 * - Automatic swing motion detection using pose analysis
 * - Intelligent recording duration optimization (3-8 seconds based on tempo)
 * - Real-time swing phase classification
 * - Motion intensity tracking
 * - Club-specific detection parameters
 * - Smart noise filtering and false positive reduction
 * 
 * This class makes the magic happen by automatically detecting when to start/stop recording
 * and determining the optimal duration for each swing type.
 */
@Singleton
class SwingAutoDetector @Inject constructor() {

    companion object {
        private const val TAG = "SwingAutoDetector"
        
        // Detection thresholds
        private const val MOTION_THRESHOLD = 0.15f // Minimum motion to trigger detection
        private const val SWING_START_THRESHOLD = 0.25f // Motion threshold for swing start
        private const val SWING_CONFIDENCE_THRESHOLD = 0.7f // Confidence threshold for swing detection
        
        // Timing parameters
        private const val MIN_RECORDING_DURATION = 3000L // 3 seconds minimum
        private const val MAX_RECORDING_DURATION = 8000L // 8 seconds maximum
        private const val OPTIMAL_RECORDING_DURATION = 5000L // 5 seconds ideal
        
        // Frame history for analysis
        private const val FRAME_HISTORY_SIZE = 90 // 3 seconds at 30fps
        private const val MOTION_ANALYSIS_WINDOW = 30 // 1 second for motion analysis
        
        // Club-specific parameters
        private val CLUB_PARAMETERS = mapOf(
            "Driver" to ClubParameters(
                motionThreshold = 0.20f,
                optimalDuration = 6000L,
                swingSpeedMultiplier = 1.2f
            ),
            "3-Wood" to ClubParameters(
                motionThreshold = 0.18f,
                optimalDuration = 5500L,
                swingSpeedMultiplier = 1.1f
            ),
            "7-Iron" to ClubParameters(
                motionThreshold = 0.15f,
                optimalDuration = 5000L,
                swingSpeedMultiplier = 1.0f
            ),
            "Pitching Wedge" to ClubParameters(
                motionThreshold = 0.12f,
                optimalDuration = 4500L,
                swingSpeedMultiplier = 0.9f
            ),
            "Putter" to ClubParameters(
                motionThreshold = 0.08f,
                optimalDuration = 3500L,
                swingSpeedMultiplier = 0.6f
            )
        )
    }

    // State management
    private var isDetecting = false
    private var selectedClub = "Driver"
    private var recordingStartTime = 0L
    private var swingDetectedTime = 0L
    private var currentSwingPhase = GolfSwingPhase.P1
    
    // Frame history for analysis
    private val frameHistory = ConcurrentLinkedQueue<PoseDetectionResult>()
    private val motionHistory = ConcurrentLinkedQueue<Float>()
    
    // Coroutine scope for background processing
    private val detectionScope = CoroutineScope(Dispatchers.Default)
    private var detectionJob: Job? = null
    
    // Listeners for callbacks
    private var motionDetectedListener: ((Float) -> Unit)? = null
    private var swingDetectedListener: ((GolfSwingPhase) -> Unit)? = null
    private var optimalDurationReachedListener: ((Float) -> Unit)? = null
    private var swingPhaseChangedListener: ((GolfSwingPhase) -> Unit)? = null
    
    // Previous frame for motion calculation
    private var previousFrame: PoseDetectionResult? = null

    /**
     * Start automatic swing detection
     */
    fun startDetection() {
        if (isDetecting) return
        
        isDetecting = true
        frameHistory.clear()
        motionHistory.clear()
        recordingStartTime = 0L
        swingDetectedTime = 0L
        currentSwingPhase = GolfSwingPhase.P1
        
        Log.d(TAG, "Auto detection started for club: $selectedClub")
        
        // Start background detection processing
        detectionJob = detectionScope.launch {
            runDetectionLoop()
        }
    }

    /**
     * Stop automatic swing detection
     */
    fun stopDetection() {
        isDetecting = false
        detectionJob?.cancel()
        detectionJob = null
        
        Log.d(TAG, "Auto detection stopped")
    }

    /**
     * Process incoming pose data for swing detection
     */
    fun processPoseData(poseResult: PoseDetectionResult) {
        if (!isDetecting) return
        
        // Add to frame history
        frameHistory.offer(poseResult)
        if (frameHistory.size > FRAME_HISTORY_SIZE) {
            frameHistory.poll()
        }
        
        // Calculate motion intensity
        val motionIntensity = calculateMotionIntensity(poseResult)
        motionHistory.offer(motionIntensity)
        if (motionHistory.size > MOTION_ANALYSIS_WINDOW) {
            motionHistory.poll()
        }
        
        // Notify motion detected
        motionDetectedListener?.invoke(motionIntensity)
        
        // Store for next frame comparison
        previousFrame = poseResult
    }

    /**
     * Set the selected golf club for detection optimization
     */
    fun setSelectedClub(club: String) {
        selectedClub = club
        Log.d(TAG, "Selected club changed to: $club")
    }

    /**
     * Set motion detection listener
     */
    fun setOnMotionDetectedListener(listener: (Float) -> Unit) {
        motionDetectedListener = listener
    }

    /**
     * Set swing detection listener
     */
    fun setOnSwingDetectedListener(listener: (GolfSwingPhase) -> Unit) {
        swingDetectedListener = listener
    }

    /**
     * Set optimal duration reached listener
     */
    fun setOnOptimalDurationReachedListener(listener: (Float) -> Unit) {
        optimalDurationReachedListener = listener
    }

    /**
     * Set swing phase changed listener
     */
    fun setOnSwingPhaseChangedListener(listener: (GolfSwingPhase) -> Unit) {
        swingPhaseChangedListener = listener
    }

    /**
     * Main detection loop that runs in background
     */
    private suspend fun runDetectionLoop() {
        while (isDetecting) {
            try {
                // Analyze current motion state
                analyzeMotionState()
                
                // Check for swing detection
                checkForSwingDetection()
                
                // Monitor recording duration
                monitorRecordingDuration()
                
                // Brief delay to prevent excessive CPU usage
                delay(33) // ~30fps analysis rate
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in detection loop", e)
                delay(100) // Wait a bit before retrying
            }
        }
    }

    /**
     * Analyze current motion state and classify swing phase
     */
    private fun analyzeMotionState() {
        if (frameHistory.size < 10) return
        
        val recentFrames = frameHistory.takeLast(10)
        val avgMotion = motionHistory.takeLast(10).average().toFloat()
        
        // Classify swing phase based on motion patterns
        val detectedPhase = classifySwingPhase(recentFrames, avgMotion)
        
        if (detectedPhase != currentSwingPhase) {
            currentSwingPhase = detectedPhase
            swingPhaseChangedListener?.invoke(detectedPhase)
            Log.d(TAG, "Swing phase changed to: ${detectedPhase.displayName}")
        }
    }

    /**
     * Check if we've detected a swing start
     */
    private fun checkForSwingDetection() {
        if (swingDetectedTime > 0) return // Already detected
        
        val clubParams = CLUB_PARAMETERS[selectedClub] ?: CLUB_PARAMETERS["Driver"]!!
        val avgMotion = motionHistory.takeLast(5).average().toFloat()
        
        // Check if motion exceeds threshold for swing start
        if (avgMotion > clubParams.motionThreshold && 
            currentSwingPhase == GolfSwingPhase.P2) { // Takeaway phase
            
            swingDetectedTime = System.currentTimeMillis()
            recordingStartTime = swingDetectedTime
            
            Log.d(TAG, "Swing detected! Phase: ${currentSwingPhase.displayName}, Motion: $avgMotion")
            swingDetectedListener?.invoke(currentSwingPhase)
        }
    }

    /**
     * Monitor recording duration and trigger optimal duration callback
     */
    private fun monitorRecordingDuration() {
        if (recordingStartTime == 0L) return
        
        val elapsed = System.currentTimeMillis() - recordingStartTime
        val clubParams = CLUB_PARAMETERS[selectedClub] ?: CLUB_PARAMETERS["Driver"]!!
        
        // Check if we've reached optimal duration
        if (elapsed >= clubParams.optimalDuration) {
            val durationSeconds = elapsed / 1000f
            
            Log.d(TAG, "Optimal recording duration reached: ${durationSeconds}s")
            optimalDurationReachedListener?.invoke(durationSeconds)
            
            // Reset for next swing
            recordingStartTime = 0L
            swingDetectedTime = 0L
        }
    }

    /**
     * Calculate motion intensity between current and previous frame
     */
    private fun calculateMotionIntensity(currentFrame: PoseDetectionResult): Float {
        val prevFrame = previousFrame ?: return 0f
        
        // Key points to track for swing motion
        val keyPoints = listOf(
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_ELBOW,
            MediaPipePoseLandmarks.RIGHT_ELBOW,
            MediaPipePoseLandmarks.LEFT_WRIST,
            MediaPipePoseLandmarks.RIGHT_WRIST,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP
        )
        
        var totalMotion = 0f
        var pointCount = 0
        
        for (point in keyPoints) {
            val currentPoint = currentFrame.keypoints[point]
            val prevPoint = prevFrame.keypoints[point]
            
            if (currentPoint != null && prevPoint != null) {
                val distance = calculateDistance(currentPoint, prevPoint)
                totalMotion += distance
                pointCount++
            }
        }
        
        return if (pointCount > 0) totalMotion / pointCount else 0f
    }

    /**
     * Calculate 3D distance between two pose keypoints
     */
    private fun calculateDistance(point1: PoseKeypoint, point2: PoseKeypoint): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        val dz = point1.z - point2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Classify swing phase based on pose analysis
     */
    private fun classifySwingPhase(frames: List<PoseDetectionResult>, avgMotion: Float): GolfSwingPhase {
        if (frames.size < 5) return GolfSwingPhase.P1
        
        // Simple classification based on motion intensity and patterns
        // This is a simplified version - real implementation would use more sophisticated ML
        return when {
            avgMotion < 0.05f -> GolfSwingPhase.P1 // Address
            avgMotion < 0.15f && isBackswingMotion(frames) -> GolfSwingPhase.P2 // Takeaway
            avgMotion < 0.25f && isBackswingMotion(frames) -> GolfSwingPhase.P3 // Backswing
            avgMotion < 0.10f && isTopOfBackswing(frames) -> GolfSwingPhase.P4 // Top
            avgMotion > 0.30f && isDownswingMotion(frames) -> GolfSwingPhase.P6 // Downswing
            avgMotion > 0.40f && isImpactMotion(frames) -> GolfSwingPhase.P7 // Impact
            avgMotion > 0.20f && isFollowThroughMotion(frames) -> GolfSwingPhase.P8 // Release
            avgMotion < 0.15f && isFinishMotion(frames) -> GolfSwingPhase.P10 // Finish
            else -> currentSwingPhase // Keep current if unclear
        }
    }

    /**
     * Detect backswing motion pattern
     */
    private fun isBackswingMotion(frames: List<PoseDetectionResult>): Boolean {
        if (frames.size < 3) return false
        
        // Check if hands are moving up and back
        val firstFrame = frames.first()
        val lastFrame = frames.last()
        
        val leftWrist1 = firstFrame.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        val leftWrist2 = lastFrame.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        
        return if (leftWrist1 != null && leftWrist2 != null) {
            leftWrist2.y < leftWrist1.y && abs(leftWrist2.x - leftWrist1.x) > 0.1f
        } else false
    }

    /**
     * Detect top of backswing (minimal motion)
     */
    private fun isTopOfBackswing(frames: List<PoseDetectionResult>): Boolean {
        // Check for minimal motion at the top
        val recentMotion = motionHistory.takeLast(3)
        return recentMotion.average() < 0.05f && recentMotion.size >= 3
    }

    /**
     * Detect downswing motion pattern
     */
    private fun isDownswingMotion(frames: List<PoseDetectionResult>): Boolean {
        if (frames.size < 3) return false
        
        // Check if hands are moving down and forward rapidly
        val firstFrame = frames.first()
        val lastFrame = frames.last()
        
        val leftWrist1 = firstFrame.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        val leftWrist2 = lastFrame.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        
        return if (leftWrist1 != null && leftWrist2 != null) {
            leftWrist2.y > leftWrist1.y && leftWrist2.x > leftWrist1.x
        } else false
    }

    /**
     * Detect impact motion pattern
     */
    private fun isImpactMotion(frames: List<PoseDetectionResult>): Boolean {
        // Impact typically has highest motion intensity
        val recentMotion = motionHistory.takeLast(3)
        return recentMotion.maxOrNull() ?: 0f > 0.4f
    }

    /**
     * Detect follow-through motion pattern
     */
    private fun isFollowThroughMotion(frames: List<PoseDetectionResult>): Boolean {
        if (frames.size < 3) return false
        
        // Check if hands are moving up and around
        val firstFrame = frames.first()
        val lastFrame = frames.last()
        
        val leftWrist1 = firstFrame.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        val leftWrist2 = lastFrame.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        
        return if (leftWrist1 != null && leftWrist2 != null) {
            leftWrist2.y < leftWrist1.y && leftWrist2.x > leftWrist1.x
        } else false
    }

    /**
     * Detect finish motion pattern
     */
    private fun isFinishMotion(frames: List<PoseDetectionResult>): Boolean {
        // Finish typically has decreasing motion
        val recentMotion = motionHistory.takeLast(5)
        if (recentMotion.size < 5) return false
        
        val firstHalf = recentMotion.take(2).average()
        val secondHalf = recentMotion.takeLast(2).average()
        
        return secondHalf < firstHalf && secondHalf < 0.15f
    }

    /**
     * Data class for club-specific parameters
     */
    private data class ClubParameters(
        val motionThreshold: Float,
        val optimalDuration: Long,
        val swingSpeedMultiplier: Float
    )
}