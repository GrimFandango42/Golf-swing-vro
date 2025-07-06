package com.swingsync.ai.analysis

import android.util.Log
import com.swingsync.ai.data.model.*
import kotlin.math.*

/**
 * P-System Classification Algorithm for Golf Swing Analysis
 * Classifies golf swing into 10 distinct phases (P1-P10) based on pose keypoints
 */
class PSystemClassifier {

    companion object {
        private const val TAG = "PSystemClassifier"
        
        // Minimum frames required for each phase
        private const val MIN_PHASE_FRAMES = 3
        
        // Movement thresholds for phase detection
        private const val MOVEMENT_THRESHOLD = 0.05f
        private const val VELOCITY_THRESHOLD = 0.1f
        private const val ACCELERATION_THRESHOLD = 0.2f
        
        // Key body parts for golf swing analysis
        private val KEY_JOINTS = listOf(
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_ELBOW,
            MediaPipePoseLandmarks.RIGHT_ELBOW,
            MediaPipePoseLandmarks.LEFT_WRIST,
            MediaPipePoseLandmarks.RIGHT_WRIST,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP
        )
    }

    /**
     * Classify swing phases from pose detection results
     */
    fun classifySwingPhases(poseResults: List<PoseDetectionResult>): List<PSystemPhase> {
        if (poseResults.isEmpty()) {
            Log.w(TAG, "No pose results provided for classification")
            return emptyList()
        }

        try {
            Log.d(TAG, "Classifying swing phases from ${poseResults.size} frames")
            
            // Step 1: Extract key metrics from pose data
            val metrics = extractSwingMetrics(poseResults)
            
            // Step 2: Detect key events in the swing
            val keyEvents = detectKeyEvents(metrics)
            
            // Step 3: Classify phases based on events
            val phases = classifyPhases(keyEvents, poseResults.size)
            
            Log.d(TAG, "Classified ${phases.size} swing phases")
            return phases
            
        } catch (e: Exception) {
            Log.e(TAG, "Error classifying swing phases", e)
            return emptyList()
        }
    }

    /**
     * Extract key metrics from pose data for swing analysis
     */
    private fun extractSwingMetrics(poseResults: List<PoseDetectionResult>): SwingMetrics {
        val handPositions = mutableListOf<PointF>()
        val shoulderRotations = mutableListOf<Float>()
        val hipRotations = mutableListOf<Float>()
        val clubPositions = mutableListOf<PointF>()
        
        poseResults.forEach { result ->
            val keypoints = result.keypoints
            
            // Extract hand positions (average of both hands)
            val leftWrist = keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
            val rightWrist = keypoints[MediaPipePoseLandmarks.RIGHT_WRIST]
            
            if (leftWrist != null && rightWrist != null) {
                val handCenter = PointF(
                    (leftWrist.x + rightWrist.x) / 2f,
                    (leftWrist.y + rightWrist.y) / 2f
                )
                handPositions.add(handCenter)
            }
            
            // Extract shoulder rotation
            val shoulderRotation = calculateShoulderRotation(keypoints)
            shoulderRotations.add(shoulderRotation)
            
            // Extract hip rotation
            val hipRotation = calculateHipRotation(keypoints)
            hipRotations.add(hipRotation)
            
            // Estimate club position (extend from hands)
            val clubPosition = estimateClubPosition(keypoints)
            clubPositions.add(clubPosition)
        }
        
        return SwingMetrics(
            handPositions = handPositions,
            shoulderRotations = shoulderRotations,
            hipRotations = hipRotations,
            clubPositions = clubPositions
        )
    }

    /**
     * Calculate shoulder rotation angle
     */
    private fun calculateShoulderRotation(keypoints: FramePoseData): Float {
        val leftShoulder = keypoints[MediaPipePoseLandmarks.LEFT_SHOULDER]
        val rightShoulder = keypoints[MediaPipePoseLandmarks.RIGHT_SHOULDER]
        
        if (leftShoulder == null || rightShoulder == null) return 0f
        
        val deltaX = rightShoulder.x - leftShoulder.x
        val deltaY = rightShoulder.y - leftShoulder.y
        
        return atan2(deltaY, deltaX) * 180f / PI.toFloat()
    }

    /**
     * Calculate hip rotation angle
     */
    private fun calculateHipRotation(keypoints: FramePoseData): Float {
        val leftHip = keypoints[MediaPipePoseLandmarks.LEFT_HIP]
        val rightHip = keypoints[MediaPipePoseLandmarks.RIGHT_HIP]
        
        if (leftHip == null || rightHip == null) return 0f
        
        val deltaX = rightHip.x - leftHip.x
        val deltaY = rightHip.y - leftHip.y
        
        return atan2(deltaY, deltaX) * 180f / PI.toFloat()
    }

    /**
     * Estimate club position from hand positions
     */
    private fun estimateClubPosition(keypoints: FramePoseData): PointF {
        val leftWrist = keypoints[MediaPipePoseLandmarks.LEFT_WRIST]
        val rightWrist = keypoints[MediaPipePoseLandmarks.RIGHT_WRIST]
        
        if (leftWrist == null || rightWrist == null) {
            return PointF(0f, 0f)
        }
        
        // Estimate club extends downward from hands
        val handCenter = PointF(
            (leftWrist.x + rightWrist.x) / 2f,
            (leftWrist.y + rightWrist.y) / 2f
        )
        
        // Estimate club head position (approximate)
        val clubLength = 0.3f // Normalized club length
        return PointF(handCenter.x, handCenter.y + clubLength)
    }

    /**
     * Detect key events in the swing
     */
    private fun detectKeyEvents(metrics: SwingMetrics): SwingKeyEvents {
        val handPositions = metrics.handPositions
        val shoulderRotations = metrics.shoulderRotations
        
        // Detect swing start (setup complete)
        val setupCompleteFrame = detectSetupComplete(handPositions)
        
        // Detect takeaway start
        val takeawayStartFrame = detectTakeawayStart(handPositions, setupCompleteFrame)
        
        // Detect top of backswing
        val topOfBackswingFrame = detectTopOfBackswing(handPositions, shoulderRotations)
        
        // Detect transition start
        val transitionStartFrame = detectTransitionStart(handPositions, topOfBackswingFrame)
        
        // Detect impact
        val impactFrame = detectImpact(handPositions, metrics.clubPositions)
        
        // Detect finish
        val finishFrame = detectFinish(handPositions, shoulderRotations)
        
        return SwingKeyEvents(
            setupCompleteFrame = setupCompleteFrame,
            takeawayStartFrame = takeawayStartFrame,
            topOfBackswingFrame = topOfBackswingFrame,
            transitionStartFrame = transitionStartFrame,
            impactFrame = impactFrame,
            finishFrame = finishFrame
        )
    }

    /**
     * Detect setup complete frame
     */
    private fun detectSetupComplete(handPositions: List<PointF>): Int {
        if (handPositions.size < 10) return 0
        
        // Look for stability in hand position at the beginning
        for (i in 5 until minOf(handPositions.size - 5, 30)) {
            val avgMovement = calculateAverageMovement(handPositions, i - 5, i + 5)
            if (avgMovement < MOVEMENT_THRESHOLD) {
                return i
            }
        }
        
        return minOf(10, handPositions.size - 1)
    }

    /**
     * Detect takeaway start frame
     */
    private fun detectTakeawayStart(handPositions: List<PointF>, setupFrame: Int): Int {
        if (handPositions.size <= setupFrame + 5) return setupFrame + 1
        
        // Look for first significant movement after setup
        for (i in setupFrame + 1 until handPositions.size - 2) {
            val movement = calculateMovement(handPositions[i], handPositions[i + 1])
            if (movement > MOVEMENT_THRESHOLD) {
                return i
            }
        }
        
        return setupFrame + 5
    }

    /**
     * Detect top of backswing frame
     */
    private fun detectTopOfBackswing(handPositions: List<PointF>, shoulderRotations: List<Float>): Int {
        if (handPositions.size < 10) return handPositions.size / 3
        
        // Find highest hand position during backswing
        var highestY = Float.MAX_VALUE
        var topFrame = handPositions.size / 3
        
        val searchStart = handPositions.size / 6
        val searchEnd = handPositions.size * 2 / 3
        
        for (i in searchStart until minOf(searchEnd, handPositions.size)) {
            if (handPositions[i].y < highestY) {
                highestY = handPositions[i].y
                topFrame = i
            }
        }
        
        return topFrame
    }

    /**
     * Detect transition start frame
     */
    private fun detectTransitionStart(handPositions: List<PointF>, topFrame: Int): Int {
        if (handPositions.size <= topFrame + 3) return topFrame + 1
        
        // Look for direction change after top of backswing
        for (i in topFrame + 1 until minOf(handPositions.size - 2, topFrame + 10)) {
            val prevMovement = calculateMovement(handPositions[i - 1], handPositions[i])
            val nextMovement = calculateMovement(handPositions[i], handPositions[i + 1])
            
            if (prevMovement > MOVEMENT_THRESHOLD && nextMovement > MOVEMENT_THRESHOLD) {
                return i
            }
        }
        
        return topFrame + 3
    }

    /**
     * Detect impact frame
     */
    private fun detectImpact(handPositions: List<PointF>, clubPositions: List<PointF>): Int {
        if (handPositions.size < 10) return handPositions.size * 2 / 3
        
        // Look for lowest club position (impact zone)
        var lowestY = Float.MIN_VALUE
        var impactFrame = handPositions.size * 2 / 3
        
        val searchStart = handPositions.size / 2
        val searchEnd = handPositions.size * 4 / 5
        
        for (i in searchStart until minOf(searchEnd, clubPositions.size)) {
            if (clubPositions[i].y > lowestY) {
                lowestY = clubPositions[i].y
                impactFrame = i
            }
        }
        
        return impactFrame
    }

    /**
     * Detect finish frame
     */
    private fun detectFinish(handPositions: List<PointF>, shoulderRotations: List<Float>): Int {
        if (handPositions.size < 10) return handPositions.size - 1
        
        // Look for stability at the end
        for (i in handPositions.size - 10 downTo handPositions.size / 2) {
            val avgMovement = calculateAverageMovement(handPositions, i, handPositions.size - 1)
            if (avgMovement < MOVEMENT_THRESHOLD) {
                return i
            }
        }
        
        return handPositions.size - 1
    }

    /**
     * Classify phases based on key events
     */
    private fun classifyPhases(keyEvents: SwingKeyEvents, totalFrames: Int): List<PSystemPhase> {
        val phases = mutableListOf<PSystemPhase>()
        
        // P1: Address (start to setup complete)
        phases.add(PSystemPhase(
            phaseName = "P1",
            startFrameIndex = 0,
            endFrameIndex = keyEvents.setupCompleteFrame
        ))
        
        // P2: Takeaway (setup complete to early backswing)
        val p2End = keyEvents.takeawayStartFrame + (keyEvents.topOfBackswingFrame - keyEvents.takeawayStartFrame) / 4
        phases.add(PSystemPhase(
            phaseName = "P2",
            startFrameIndex = keyEvents.setupCompleteFrame,
            endFrameIndex = p2End
        ))
        
        // P3: Backswing (early to mid backswing)
        val p3End = keyEvents.takeawayStartFrame + (keyEvents.topOfBackswingFrame - keyEvents.takeawayStartFrame) / 2
        phases.add(PSystemPhase(
            phaseName = "P3",
            startFrameIndex = p2End,
            endFrameIndex = p3End
        ))
        
        // P4: Top of Backswing
        phases.add(PSystemPhase(
            phaseName = "P4",
            startFrameIndex = p3End,
            endFrameIndex = keyEvents.topOfBackswingFrame
        ))
        
        // P5: Transition
        phases.add(PSystemPhase(
            phaseName = "P5",
            startFrameIndex = keyEvents.topOfBackswingFrame,
            endFrameIndex = keyEvents.transitionStartFrame
        ))
        
        // P6: Downswing (transition to pre-impact)
        val p6End = keyEvents.transitionStartFrame + (keyEvents.impactFrame - keyEvents.transitionStartFrame) * 2 / 3
        phases.add(PSystemPhase(
            phaseName = "P6",
            startFrameIndex = keyEvents.transitionStartFrame,
            endFrameIndex = p6End
        ))
        
        // P7: Impact
        phases.add(PSystemPhase(
            phaseName = "P7",
            startFrameIndex = p6End,
            endFrameIndex = keyEvents.impactFrame
        ))
        
        // P8: Release (impact to early follow-through)
        val p8End = keyEvents.impactFrame + (keyEvents.finishFrame - keyEvents.impactFrame) / 3
        phases.add(PSystemPhase(
            phaseName = "P8",
            startFrameIndex = keyEvents.impactFrame,
            endFrameIndex = p8End
        ))
        
        // P9: Follow Through
        val p9End = keyEvents.impactFrame + (keyEvents.finishFrame - keyEvents.impactFrame) * 2 / 3
        phases.add(PSystemPhase(
            phaseName = "P9",
            startFrameIndex = p8End,
            endFrameIndex = p9End
        ))
        
        // P10: Finish
        phases.add(PSystemPhase(
            phaseName = "P10",
            startFrameIndex = p9End,
            endFrameIndex = totalFrames - 1
        ))
        
        return phases
    }

    /**
     * Calculate movement between two points
     */
    private fun calculateMovement(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculate average movement in a range
     */
    private fun calculateAverageMovement(positions: List<PointF>, startIndex: Int, endIndex: Int): Float {
        if (startIndex >= endIndex || endIndex >= positions.size) return 0f
        
        var totalMovement = 0f
        var count = 0
        
        for (i in startIndex until endIndex - 1) {
            totalMovement += calculateMovement(positions[i], positions[i + 1])
            count++
        }
        
        return if (count > 0) totalMovement / count else 0f
    }

    /**
     * Data class for swing metrics
     */
    private data class SwingMetrics(
        val handPositions: List<PointF>,
        val shoulderRotations: List<Float>,
        val hipRotations: List<Float>,
        val clubPositions: List<PointF>
    )

    /**
     * Data class for key swing events
     */
    private data class SwingKeyEvents(
        val setupCompleteFrame: Int,
        val takeawayStartFrame: Int,
        val topOfBackswingFrame: Int,
        val transitionStartFrame: Int,
        val impactFrame: Int,
        val finishFrame: Int
    )

    /**
     * Simple PointF implementation
     */
    private data class PointF(val x: Float, val y: Float)
}