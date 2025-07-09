package com.swingsync.ai.ar

import com.swingsync.ai.data.model.FramePoseData
import com.swingsync.ai.data.model.MediaPipePoseLandmarks
import com.swingsync.ai.data.model.PoseKeypoint
import kotlin.math.*

/**
 * Real-time club path tracking using hand positions and pose data
 * Estimates club head position and creates smooth 3D path visualization
 */
class ClubPathTracker {
    
    companion object {
        // Confidence thresholds
        private const val MIN_CONFIDENCE = 0.6f
        private const val IDEAL_CONFIDENCE = 0.8f
        
        // Smoothing parameters
        private const val SMOOTHING_FACTOR = 0.8f
        private const val MAX_VELOCITY = 10f // m/s
        private const val MAX_ACCELERATION = 50f // m/s²
        
        // Club parameters
        private const val CLUB_LENGTH_DRIVER = 1.17f
        private const val CLUB_LENGTH_IRON = 0.95f
        private const val CLUB_LENGTH_WEDGE = 0.89f
        private const val CLUB_LENGTH_PUTTER = 0.86f
        
        // Path tracking parameters
        private const val MAX_PATH_POINTS = 200
        private const val MIN_MOVEMENT_THRESHOLD = 0.01f // meters
        
        // Prediction parameters
        private const val PREDICTION_FRAMES = 5
        private const val PREDICTION_WEIGHT = 0.3f
    }
    
    /**
     * Represents a single point in the club path
     */
    data class ClubPathPoint(
        val position: Vector3,
        val velocity: Vector3,
        val timestamp: Long,
        val confidence: Float,
        val frameIndex: Int
    )
    
    /**
     * Represents the complete club path with metadata
     */
    data class ClubPath(
        val points: List<ClubPathPoint>,
        val clubType: String,
        val handedness: SwingPlaneCalculator.Handedness,
        val totalDistance: Float,
        val maxVelocity: Float,
        val avgVelocity: Float,
        val swingDuration: Long,
        val isComplete: Boolean
    ) {
        
        /**
         * Get path points as simple Vector3 array for rendering
         */
        fun getPositions(): Array<Vector3> {
            return points.map { it.position }.toTypedArray()
        }
        
        /**
         * Get path segments for line rendering
         */
        fun getSegments(): List<Pair<Vector3, Vector3>> {
            return points.zipWithNext { a, b -> Pair(a.position, b.position) }
        }
        
        /**
         * Get velocity-colored segments (for visualization)
         */
        fun getVelocitySegments(): List<Triple<Vector3, Vector3, Float>> {
            return points.zipWithNext { a, b -> 
                Triple(a.position, b.position, (a.velocity.magnitude + b.velocity.magnitude) / 2f)
            }
        }
        
        /**
         * Get the impact zone (around maximum velocity)
         */
        fun getImpactZone(): ClubPathPoint? {
            return points.maxByOrNull { it.velocity.magnitude }
        }
        
        /**
         * Calculate swing tempo (time ratios)
         */
        fun getSwingTempo(): Triple<Float, Float, Float> {
            if (points.size < 10) return Triple(0f, 0f, 0f)
            
            val impactPoint = getImpactZone()
            if (impactPoint == null) return Triple(0f, 0f, 0f)
            
            val impactIndex = points.indexOf(impactPoint)
            val backswingTime = impactPoint.timestamp - points.first().timestamp
            val downswingTime = points.last().timestamp - impactPoint.timestamp
            val totalTime = points.last().timestamp - points.first().timestamp
            
            val backswingRatio = backswingTime.toFloat() / totalTime
            val downswingRatio = downswingTime.toFloat() / totalTime
            val tempo = backswingTime.toFloat() / downswingTime.toFloat()
            
            return Triple(backswingRatio, downswingRatio, tempo)
        }
    }
    
    // Internal state
    private val pathPoints = mutableListOf<ClubPathPoint>()
    private var lastPosition: Vector3? = null
    private var lastVelocity: Vector3 = Vector3.ZERO
    private var lastTimestamp: Long = 0
    private var frameIndex = 0
    private var isTracking = false
    private var clubType = "7-Iron"
    private var handedness = SwingPlaneCalculator.Handedness.RIGHT_HANDED
    
    /**
     * Start tracking a new swing
     */
    fun startTracking(clubType: String, handedness: SwingPlaneCalculator.Handedness) {
        this.clubType = clubType
        this.handedness = handedness
        pathPoints.clear()
        lastPosition = null
        lastVelocity = Vector3.ZERO
        lastTimestamp = 0
        frameIndex = 0
        isTracking = true
    }
    
    /**
     * Stop tracking current swing
     */
    fun stopTracking(): ClubPath? {
        isTracking = false
        
        if (pathPoints.isEmpty()) return null
        
        return ClubPath(
            points = pathPoints.toList(),
            clubType = clubType,
            handedness = handedness,
            totalDistance = calculateTotalDistance(),
            maxVelocity = pathPoints.maxOfOrNull { it.velocity.magnitude } ?: 0f,
            avgVelocity = pathPoints.map { it.velocity.magnitude }.average().toFloat(),
            swingDuration = pathPoints.last().timestamp - pathPoints.first().timestamp,
            isComplete = pathPoints.size >= 20
        )
    }
    
    /**
     * Process new pose data and update club path
     */
    fun updatePath(poseData: FramePoseData, timestamp: Long): ClubPathPoint? {
        if (!isTracking) return null
        
        // Extract hand positions
        val leftWrist = poseData[MediaPipePoseLandmarks.LEFT_WRIST]
        val rightWrist = poseData[MediaPipePoseLandmarks.RIGHT_WRIST]
        val leftHand = poseData[MediaPipePoseLandmarks.LEFT_INDEX]
        val rightHand = poseData[MediaPipePoseLandmarks.RIGHT_INDEX]
        
        // Validate hand positions
        if (!isValidHandData(leftWrist, rightWrist, leftHand, rightHand)) {
            return null
        }
        
        // Calculate club head position
        val clubHeadPosition = calculateClubHeadPosition(
            leftWrist!!, rightWrist!!, leftHand, rightHand
        ) ?: return null
        
        // Calculate velocity
        val velocity = calculateVelocity(clubHeadPosition, timestamp)
        
        // Apply smoothing
        val smoothedPosition = applySmoothingToPosition(clubHeadPosition)
        val smoothedVelocity = applySmoothingToVelocity(velocity)
        
        // Calculate confidence
        val confidence = calculatePositionConfidence(leftWrist, rightWrist, leftHand, rightHand)
        
        // Check if this is a significant movement
        if (lastPosition != null && 
            smoothedPosition.distanceTo(lastPosition!!) < MIN_MOVEMENT_THRESHOLD) {
            return null
        }
        
        // Create new path point
        val pathPoint = ClubPathPoint(
            position = smoothedPosition,
            velocity = smoothedVelocity,
            timestamp = timestamp,
            confidence = confidence,
            frameIndex = frameIndex++
        )
        
        // Add to path
        pathPoints.add(pathPoint)
        
        // Limit path length
        if (pathPoints.size > MAX_PATH_POINTS) {
            pathPoints.removeFirst()
        }
        
        // Update tracking state
        lastPosition = smoothedPosition
        lastVelocity = smoothedVelocity
        lastTimestamp = timestamp
        
        return pathPoint
    }
    
    /**
     * Get current club path
     */
    fun getCurrentPath(): ClubPath? {
        if (pathPoints.isEmpty()) return null
        
        return ClubPath(
            points = pathPoints.toList(),
            clubType = clubType,
            handedness = handedness,
            totalDistance = calculateTotalDistance(),
            maxVelocity = pathPoints.maxOfOrNull { it.velocity.magnitude } ?: 0f,
            avgVelocity = pathPoints.map { it.velocity.magnitude }.average().toFloat(),
            swingDuration = if (pathPoints.size > 1) pathPoints.last().timestamp - pathPoints.first().timestamp else 0,
            isComplete = false
        )
    }
    
    /**
     * Predict next club position based on current trajectory
     */
    fun predictNextPosition(frames: Int = PREDICTION_FRAMES): Vector3? {
        if (pathPoints.size < 3) return null
        
        val recent = pathPoints.takeLast(3)
        val acceleration = calculateAcceleration(recent)
        val currentVelocity = recent.last().velocity
        val currentPosition = recent.last().position
        
        // Predict position using physics
        val deltaTime = frames * (1f / 30f) // Assuming 30 FPS
        val predictedPosition = currentPosition + 
                                currentVelocity * deltaTime + 
                                acceleration * (deltaTime * deltaTime * 0.5f)
        
        return predictedPosition
    }
    
    /**
     * Clear current path
     */
    fun clearPath() {
        pathPoints.clear()
        lastPosition = null
        lastVelocity = Vector3.ZERO
        lastTimestamp = 0
        frameIndex = 0
    }
    
    /**
     * Get path statistics
     */
    fun getPathStatistics(): Map<String, Any> {
        if (pathPoints.isEmpty()) return emptyMap()
        
        return mapOf(
            "total_points" to pathPoints.size,
            "total_distance" to calculateTotalDistance(),
            "max_velocity" to (pathPoints.maxOfOrNull { it.velocity.magnitude } ?: 0f),
            "avg_velocity" to pathPoints.map { it.velocity.magnitude }.average(),
            "avg_confidence" to pathPoints.map { it.confidence }.average(),
            "duration_ms" to if (pathPoints.size > 1) pathPoints.last().timestamp - pathPoints.first().timestamp else 0
        )
    }
    
    // Private helper methods
    
    private fun isValidHandData(vararg keypoints: PoseKeypoint?): Boolean {
        return keypoints.all { it != null && (it.visibility ?: 0f) >= MIN_CONFIDENCE }
    }
    
    private fun calculateClubHeadPosition(
        leftWrist: PoseKeypoint,
        rightWrist: PoseKeypoint,
        leftHand: PoseKeypoint?,
        rightHand: PoseKeypoint?
    ): Vector3? {
        
        // Calculate grip center (between wrists)
        val leftWristPos = Vector3(leftWrist.x, leftWrist.y, leftWrist.z)
        val rightWristPos = Vector3(rightWrist.x, rightWrist.y, rightWrist.z)
        val gripCenter = (leftWristPos + rightWristPos) / 2f
        
        // Calculate grip direction
        val gripDirection = if (handedness == SwingPlaneCalculator.Handedness.RIGHT_HANDED) {
            (rightWristPos - leftWristPos).normalized
        } else {
            (leftWristPos - rightWristPos).normalized
        }
        
        // Use hand positions to refine direction if available
        val refinedDirection = if (leftHand != null && rightHand != null) {
            val leftHandPos = Vector3(leftHand.x, leftHand.y, leftHand.z)
            val rightHandPos = Vector3(rightHand.x, rightHand.y, rightHand.z)
            val handDirection = (rightHandPos - leftHandPos).normalized
            (gripDirection + handDirection).normalized
        } else {
            gripDirection
        }
        
        // Calculate club length
        val clubLength = getClubLength(clubType)
        
        // Estimate club head position
        // This is a simplified model - in reality, club shaft angle changes during swing
        val clubHeadOffset = refinedDirection * clubLength
        
        return gripCenter + clubHeadOffset
    }
    
    private fun calculateVelocity(position: Vector3, timestamp: Long): Vector3 {
        if (lastPosition == null || lastTimestamp == 0) {
            return Vector3.ZERO
        }
        
        val deltaTime = (timestamp - lastTimestamp) / 1000f // Convert to seconds
        if (deltaTime <= 0) return Vector3.ZERO
        
        val deltaPosition = position - lastPosition!!
        val velocity = deltaPosition / deltaTime
        
        // Clamp velocity to reasonable limits
        val speed = velocity.magnitude
        return if (speed > MAX_VELOCITY) {
            velocity.normalized * MAX_VELOCITY
        } else {
            velocity
        }
    }
    
    private fun applySmoothingToPosition(newPosition: Vector3): Vector3 {
        return if (lastPosition == null) {
            newPosition
        } else {
            lastPosition!! * SMOOTHING_FACTOR + newPosition * (1f - SMOOTHING_FACTOR)
        }
    }
    
    private fun applySmoothingToVelocity(newVelocity: Vector3): Vector3 {
        return lastVelocity * SMOOTHING_FACTOR + newVelocity * (1f - SMOOTHING_FACTOR)
    }
    
    private fun calculatePositionConfidence(vararg keypoints: PoseKeypoint): Float {
        val confidences = keypoints.mapNotNull { it.visibility }
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            0f
        }
    }
    
    private fun calculateTotalDistance(): Float {
        if (pathPoints.size < 2) return 0f
        
        return pathPoints.zipWithNext { a, b ->
            a.position.distanceTo(b.position)
        }.sum()
    }
    
    private fun calculateAcceleration(recentPoints: List<ClubPathPoint>): Vector3 {
        if (recentPoints.size < 2) return Vector3.ZERO
        
        val deltaTime = (recentPoints.last().timestamp - recentPoints.first().timestamp) / 1000f
        if (deltaTime <= 0) return Vector3.ZERO
        
        val deltaVelocity = recentPoints.last().velocity - recentPoints.first().velocity
        val acceleration = deltaVelocity / deltaTime
        
        // Clamp acceleration
        val accelerationMagnitude = acceleration.magnitude
        return if (accelerationMagnitude > MAX_ACCELERATION) {
            acceleration.normalized * MAX_ACCELERATION
        } else {
            acceleration
        }
    }
    
    private fun getClubLength(clubType: String): Float {
        return when (clubType.lowercase()) {
            "driver", "1-wood", "3-wood", "5-wood" -> CLUB_LENGTH_DRIVER
            "putter" -> CLUB_LENGTH_PUTTER
            "sand wedge", "lob wedge", "gap wedge", "pitching wedge" -> CLUB_LENGTH_WEDGE
            else -> CLUB_LENGTH_IRON
        }
    }
}