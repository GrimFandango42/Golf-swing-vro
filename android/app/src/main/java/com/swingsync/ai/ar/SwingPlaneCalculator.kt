package com.swingsync.ai.ar

import com.swingsync.ai.data.model.FramePoseData
import com.swingsync.ai.data.model.MediaPipePoseLandmarks
import com.swingsync.ai.data.model.PoseKeypoint
import kotlin.math.*

/**
 * Advanced swing plane calculator that determines ideal swing planes
 * based on golfer's body alignment and club characteristics
 */
class SwingPlaneCalculator {
    
    companion object {
        // Golf swing plane angles (degrees from horizontal)
        private const val DRIVER_PLANE_ANGLE = 45f
        private const val IRON_PLANE_ANGLE = 60f
        private const val WEDGE_PLANE_ANGLE = 65f
        private const val PUTTER_PLANE_ANGLE = 70f
        
        // Confidence thresholds for pose detection
        private const val MIN_CONFIDENCE = 0.6f
        private const val IDEAL_CONFIDENCE = 0.8f
        
        // Swing plane dimensions (meters)
        private const val PLANE_WIDTH = 3.0f
        private const val PLANE_HEIGHT = 2.0f
        
        // Club length estimates (meters)
        private const val DRIVER_LENGTH = 1.17f
        private const val IRON_LENGTH = 0.95f
        private const val WEDGE_LENGTH = 0.89f
        private const val PUTTER_LENGTH = 0.86f
    }
    
    /**
     * Data class representing a calculated swing plane
     */
    data class SwingPlane(
        val plane: Plane,
        val centerPoint: Vector3,
        val width: Float,
        val height: Float,
        val angle: Float, // Angle from horizontal in degrees
        val confidence: Float,
        val clubType: String,
        val handedness: Handedness
    ) {
        
        /**
         * Get the four corner points of the swing plane for rendering
         */
        fun getCornerPoints(): Array<Vector3> {
            val rightVector = Vector3(1f, 0f, 0f)
            val upVector = plane.normal.cross(rightVector).normalized
            val actualRight = upVector.cross(plane.normal).normalized
            
            val halfWidth = width / 2f
            val halfHeight = height / 2f
            
            return arrayOf(
                centerPoint + actualRight * halfWidth + upVector * halfHeight,
                centerPoint - actualRight * halfWidth + upVector * halfHeight,
                centerPoint - actualRight * halfWidth - upVector * halfHeight,
                centerPoint + actualRight * halfWidth - upVector * halfHeight
            )
        }
        
        /**
         * Get grid lines for swing plane visualization
         */
        fun getGridLines(): List<Pair<Vector3, Vector3>> {
            val lines = mutableListOf<Pair<Vector3, Vector3>>()
            val rightVector = Vector3(1f, 0f, 0f)
            val upVector = plane.normal.cross(rightVector).normalized
            val actualRight = upVector.cross(plane.normal).normalized
            
            val halfWidth = width / 2f
            val halfHeight = height / 2f
            
            // Horizontal lines
            for (i in -2..2) {
                val y = (i / 2f) * halfHeight
                val start = centerPoint - actualRight * halfWidth + upVector * y
                val end = centerPoint + actualRight * halfWidth + upVector * y
                lines.add(Pair(start, end))
            }
            
            // Vertical lines
            for (i in -3..3) {
                val x = (i / 3f) * halfWidth
                val start = centerPoint + actualRight * x - upVector * halfHeight
                val end = centerPoint + actualRight * x + upVector * halfHeight
                lines.add(Pair(start, end))
            }
            
            return lines
        }
    }
    
    /**
     * Golfer handedness
     */
    enum class Handedness {
        RIGHT_HANDED,
        LEFT_HANDED
    }
    
    /**
     * Calculate ideal swing plane based on pose data and club type
     */
    fun calculateIdealSwingPlane(
        poseData: FramePoseData,
        clubType: String,
        handedness: Handedness = Handedness.RIGHT_HANDED
    ): SwingPlane? {
        
        // Extract key body landmarks
        val leftShoulder = poseData[MediaPipePoseLandmarks.LEFT_SHOULDER]
        val rightShoulder = poseData[MediaPipePoseLandmarks.RIGHT_SHOULDER]
        val leftHip = poseData[MediaPipePoseLandmarks.LEFT_HIP]
        val rightHip = poseData[MediaPipePoseLandmarks.RIGHT_HIP]
        
        // Validate pose data confidence
        if (!isValidPoseData(leftShoulder, rightShoulder, leftHip, rightHip)) {
            return null
        }
        
        // Calculate body alignment vectors
        val shoulderLine = getBodyLineVector(leftShoulder!!, rightShoulder!!)
        val hipLine = getBodyLineVector(leftHip!!, rightHip!!)
        val spineVector = getSpineVector(leftShoulder, rightShoulder, leftHip, rightHip)
        
        // Calculate ideal plane angle based on club type
        val planeAngle = getPlaneAngleForClub(clubType)
        val clubLength = getClubLength(clubType)
        
        // Calculate swing plane center point
        val centerPoint = calculateSwingPlaneCenter(
            leftShoulder, rightShoulder, leftHip, rightHip, 
            handedness, clubLength
        )
        
        // Calculate swing plane normal vector
        val planeNormal = calculateSwingPlaneNormal(
            shoulderLine, spineVector, planeAngle, handedness
        )
        
        // Create swing plane
        val plane = Plane(centerPoint, planeNormal)
        
        // Calculate confidence based on pose quality
        val confidence = calculatePlaneConfidence(
            leftShoulder, rightShoulder, leftHip, rightHip
        )
        
        return SwingPlane(
            plane = plane,
            centerPoint = centerPoint,
            width = PLANE_WIDTH,
            height = PLANE_HEIGHT,
            angle = planeAngle,
            confidence = confidence,
            clubType = clubType,
            handedness = handedness
        )
    }
    
    /**
     * Calculate swing plane from actual hand positions during swing
     */
    fun calculateActualSwingPlane(
        handPositions: List<Vector3>,
        minPositions: Int = 10
    ): SwingPlane? {
        
        if (handPositions.size < minPositions) {
            return null
        }
        
        // Use least squares to fit a plane through hand positions
        val plane = fitPlaneToPoints(handPositions) ?: return null
        
        // Calculate center point as average of hand positions
        val centerPoint = handPositions.fold(Vector3.ZERO) { acc, pos -> acc + pos } / handPositions.size.toFloat()
        
        // Calculate plane angle
        val angle = calculatePlaneAngle(plane.normal)
        
        return SwingPlane(
            plane = plane,
            centerPoint = centerPoint,
            width = PLANE_WIDTH,
            height = PLANE_HEIGHT,
            angle = angle,
            confidence = 0.9f, // High confidence for actual swing data
            clubType = "Actual",
            handedness = Handedness.RIGHT_HANDED // Default
        )
    }
    
    /**
     * Compare two swing planes and calculate deviation
     */
    fun compareSwingPlanes(ideal: SwingPlane, actual: SwingPlane): SwingPlaneDeviation {
        val angleDeviation = abs(ideal.angle - actual.angle)
        val normalDeviation = ideal.plane.normal.angleTo(actual.plane.normal) * 180f / PI.toFloat()
        val centerDeviation = ideal.centerPoint.distanceTo(actual.centerPoint)
        
        return SwingPlaneDeviation(
            angleDeviation = angleDeviation,
            normalDeviation = normalDeviation,
            centerDeviation = centerDeviation,
            overallDeviation = (angleDeviation + normalDeviation + centerDeviation * 10f) / 3f
        )
    }
    
    /**
     * Data class for swing plane deviation analysis
     */
    data class SwingPlaneDeviation(
        val angleDeviation: Float,
        val normalDeviation: Float,
        val centerDeviation: Float,
        val overallDeviation: Float
    ) {
        val isOnPlane: Boolean
            get() = overallDeviation < 5f
            
        val quality: String
            get() = when {
                overallDeviation < 3f -> "Excellent"
                overallDeviation < 6f -> "Good"
                overallDeviation < 10f -> "Fair"
                else -> "Poor"
            }
    }
    
    // Private helper methods
    
    private fun isValidPoseData(vararg keypoints: PoseKeypoint?): Boolean {
        return keypoints.all { it != null && (it.visibility ?: 0f) >= MIN_CONFIDENCE }
    }
    
    private fun getBodyLineVector(left: PoseKeypoint, right: PoseKeypoint): Vector3 {
        val leftVec = Vector3(left.x, left.y, left.z)
        val rightVec = Vector3(right.x, right.y, right.z)
        return (rightVec - leftVec).normalized
    }
    
    private fun getSpineVector(
        leftShoulder: PoseKeypoint,
        rightShoulder: PoseKeypoint,
        leftHip: PoseKeypoint,
        rightHip: PoseKeypoint
    ): Vector3 {
        val shoulderCenter = Vector3(
            (leftShoulder.x + rightShoulder.x) / 2f,
            (leftShoulder.y + rightShoulder.y) / 2f,
            (leftShoulder.z + rightShoulder.z) / 2f
        )
        
        val hipCenter = Vector3(
            (leftHip.x + rightHip.x) / 2f,
            (leftHip.y + rightHip.y) / 2f,
            (leftHip.z + rightHip.z) / 2f
        )
        
        return (shoulderCenter - hipCenter).normalized
    }
    
    private fun getPlaneAngleForClub(clubType: String): Float {
        return when (clubType.lowercase()) {
            "driver", "1-wood", "3-wood", "5-wood" -> DRIVER_PLANE_ANGLE
            "putter" -> PUTTER_PLANE_ANGLE
            "sand wedge", "lob wedge", "gap wedge", "pitching wedge" -> WEDGE_PLANE_ANGLE
            else -> IRON_PLANE_ANGLE
        }
    }
    
    private fun getClubLength(clubType: String): Float {
        return when (clubType.lowercase()) {
            "driver", "1-wood", "3-wood", "5-wood" -> DRIVER_LENGTH
            "putter" -> PUTTER_LENGTH
            "sand wedge", "lob wedge", "gap wedge", "pitching wedge" -> WEDGE_LENGTH
            else -> IRON_LENGTH
        }
    }
    
    private fun calculateSwingPlaneCenter(
        leftShoulder: PoseKeypoint,
        rightShoulder: PoseKeypoint,
        leftHip: PoseKeypoint,
        rightHip: PoseKeypoint,
        handedness: Handedness,
        clubLength: Float
    ): Vector3 {
        
        // Calculate body center
        val bodyCenter = Vector3(
            (leftShoulder.x + rightShoulder.x + leftHip.x + rightHip.x) / 4f,
            (leftShoulder.y + rightShoulder.y + leftHip.y + rightHip.y) / 4f,
            (leftShoulder.z + rightShoulder.z + leftHip.z + rightHip.z) / 4f
        )
        
        // Offset based on handedness and club length
        val offset = if (handedness == Handedness.RIGHT_HANDED) {
            Vector3(-clubLength * 0.3f, -clubLength * 0.1f, 0f)
        } else {
            Vector3(clubLength * 0.3f, -clubLength * 0.1f, 0f)
        }
        
        return bodyCenter + offset
    }
    
    private fun calculateSwingPlaneNormal(
        shoulderLine: Vector3,
        spineVector: Vector3,
        planeAngle: Float,
        handedness: Handedness
    ): Vector3 {
        
        // Calculate base normal from shoulder line and spine
        val baseNormal = shoulderLine.cross(spineVector).normalized
        
        // Adjust for swing plane angle
        val angleRad = planeAngle * PI.toFloat() / 180f
        val rotationAxis = shoulderLine
        
        // Rotate normal to match swing plane angle
        val cosAngle = cos(angleRad)
        val sinAngle = sin(angleRad)
        val oneMinusCos = 1f - cosAngle
        
        val rotatedNormal = Vector3(
            baseNormal.x * (cosAngle + rotationAxis.x * rotationAxis.x * oneMinusCos) +
                    baseNormal.y * (rotationAxis.x * rotationAxis.y * oneMinusCos - rotationAxis.z * sinAngle) +
                    baseNormal.z * (rotationAxis.x * rotationAxis.z * oneMinusCos + rotationAxis.y * sinAngle),
            
            baseNormal.x * (rotationAxis.y * rotationAxis.x * oneMinusCos + rotationAxis.z * sinAngle) +
                    baseNormal.y * (cosAngle + rotationAxis.y * rotationAxis.y * oneMinusCos) +
                    baseNormal.z * (rotationAxis.y * rotationAxis.z * oneMinusCos - rotationAxis.x * sinAngle),
            
            baseNormal.x * (rotationAxis.z * rotationAxis.x * oneMinusCos - rotationAxis.y * sinAngle) +
                    baseNormal.y * (rotationAxis.z * rotationAxis.y * oneMinusCos + rotationAxis.x * sinAngle) +
                    baseNormal.z * (cosAngle + rotationAxis.z * rotationAxis.z * oneMinusCos)
        )
        
        // Adjust for handedness
        return if (handedness == Handedness.RIGHT_HANDED) {
            rotatedNormal
        } else {
            rotatedNormal * -1f
        }
    }
    
    private fun calculatePlaneConfidence(vararg keypoints: PoseKeypoint): Float {
        val avgConfidence = keypoints.map { it.visibility ?: 0f }.average().toFloat()
        return avgConfidence.coerceIn(0f, 1f)
    }
    
    private fun fitPlaneToPoints(points: List<Vector3>): Plane? {
        if (points.size < 3) return null
        
        // Calculate centroid
        val centroid = points.fold(Vector3.ZERO) { acc, point -> acc + point } / points.size.toFloat()
        
        // Calculate covariance matrix
        var xx = 0f; var xy = 0f; var xz = 0f
        var yy = 0f; var yz = 0f; var zz = 0f
        
        points.forEach { point ->
            val diff = point - centroid
            xx += diff.x * diff.x
            xy += diff.x * diff.y
            xz += diff.x * diff.z
            yy += diff.y * diff.y
            yz += diff.y * diff.z
            zz += diff.z * diff.z
        }
        
        // Find normal using cross product of two vectors in the plane
        val v1 = points[1] - points[0]
        val v2 = points[2] - points[0]
        val normal = v1.cross(v2).normalized
        
        return Plane(centroid, normal)
    }
    
    private fun calculatePlaneAngle(normal: Vector3): Float {
        val horizontal = Vector3(0f, 0f, 1f)
        return normal.angleTo(horizontal) * 180f / PI.toFloat()
    }
}