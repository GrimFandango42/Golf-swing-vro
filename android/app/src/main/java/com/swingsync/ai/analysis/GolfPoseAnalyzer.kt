package com.swingsync.ai.analysis

import android.util.Log
import com.swingsync.ai.data.model.*
import kotlin.math.*

/**
 * Golf pose analyzer for biomechanical analysis
 * Analyzes pose data to extract golf-specific metrics
 */
class GolfPoseAnalyzer {

    companion object {
        private const val TAG = "GolfPoseAnalyzer"
        private const val MIN_CONFIDENCE = 0.5f
    }

    /**
     * Analyze pose data and extract golf metrics
     */
    fun analyzePose(poseData: FramePoseData): GolfPoseMetrics {
        try {
            Log.d(TAG, "Analyzing pose data with ${poseData.size} keypoints")

            // Extract key angles
            val shoulderAngle = calculateShoulderAngle(poseData)
            val hipAngle = calculateHipAngle(poseData)
            val spineAngle = calculateSpineAngle(poseData)
            val kneeFlex = calculateKneeFlex(poseData)
            
            // Calculate X-Factor
            val xFactor = calculateXFactor(shoulderAngle, hipAngle)
            
            // Calculate balance
            val balance = calculateBalance(poseData)
            
            // Calculate setup posture
            val setupQuality = calculateSetupQuality(poseData)

            return GolfPoseMetrics(
                shoulderAngle = shoulderAngle,
                hipAngle = hipAngle,
                spineAngle = spineAngle,
                kneeFlex = kneeFlex,
                xFactor = xFactor,
                balance = balance,
                setupQuality = setupQuality,
                confidence = calculateOverallConfidence(poseData)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing pose", e)
            return GolfPoseMetrics.empty()
        }
    }

    /**
     * Calculate shoulder line angle
     */
    private fun calculateShoulderAngle(poseData: FramePoseData): Float {
        val leftShoulder = poseData[MediaPipePoseLandmarks.LEFT_SHOULDER]
        val rightShoulder = poseData[MediaPipePoseLandmarks.RIGHT_SHOULDER]
        
        if (leftShoulder == null || rightShoulder == null) return 0f
        
        val dx = rightShoulder.x - leftShoulder.x
        val dy = rightShoulder.y - leftShoulder.y
        
        return atan2(dy, dx) * 180f / PI.toFloat()
    }

    /**
     * Calculate hip line angle
     */
    private fun calculateHipAngle(poseData: FramePoseData): Float {
        val leftHip = poseData[MediaPipePoseLandmarks.LEFT_HIP]
        val rightHip = poseData[MediaPipePoseLandmarks.RIGHT_HIP]
        
        if (leftHip == null || rightHip == null) return 0f
        
        val dx = rightHip.x - leftHip.x
        val dy = rightHip.y - leftHip.y
        
        return atan2(dy, dx) * 180f / PI.toFloat()
    }

    /**
     * Calculate spine angle
     */
    private fun calculateSpineAngle(poseData: FramePoseData): Float {
        val leftShoulder = poseData[MediaPipePoseLandmarks.LEFT_SHOULDER]
        val rightShoulder = poseData[MediaPipePoseLandmarks.RIGHT_SHOULDER]
        val leftHip = poseData[MediaPipePoseLandmarks.LEFT_HIP]
        val rightHip = poseData[MediaPipePoseLandmarks.RIGHT_HIP]
        
        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            return 0f
        }
        
        // Calculate midpoints
        val shoulderMidX = (leftShoulder.x + rightShoulder.x) / 2
        val shoulderMidY = (leftShoulder.y + rightShoulder.y) / 2
        val hipMidX = (leftHip.x + rightHip.x) / 2
        val hipMidY = (leftHip.y + rightHip.y) / 2
        
        // Calculate spine angle
        val dx = shoulderMidX - hipMidX
        val dy = shoulderMidY - hipMidY
        
        return atan2(dx, dy) * 180f / PI.toFloat()
    }

    /**
     * Calculate knee flex angle
     */
    private fun calculateKneeFlex(poseData: FramePoseData): Float {
        val leftHip = poseData[MediaPipePoseLandmarks.LEFT_HIP]
        val leftKnee = poseData[MediaPipePoseLandmarks.LEFT_KNEE]
        val leftAnkle = poseData[MediaPipePoseLandmarks.LEFT_ANKLE]
        
        if (leftHip == null || leftKnee == null || leftAnkle == null) return 0f
        
        // Calculate vectors
        val hipKneeX = leftKnee.x - leftHip.x
        val hipKneeY = leftKnee.y - leftHip.y
        val kneeAnkleX = leftAnkle.x - leftKnee.x
        val kneeAnkleY = leftAnkle.y - leftKnee.y
        
        // Calculate angle
        val dot = hipKneeX * kneeAnkleX + hipKneeY * kneeAnkleY
        val mag1 = sqrt(hipKneeX * hipKneeX + hipKneeY * hipKneeY)
        val mag2 = sqrt(kneeAnkleX * kneeAnkleX + kneeAnkleY * kneeAnkleY)
        
        if (mag1 == 0f || mag2 == 0f) return 0f
        
        val cosAngle = dot / (mag1 * mag2)
        return acos(cosAngle.coerceIn(-1f, 1f)) * 180f / PI.toFloat()
    }

    /**
     * Calculate X-Factor (shoulder-hip separation)
     */
    private fun calculateXFactor(shoulderAngle: Float, hipAngle: Float): Float {
        return abs(shoulderAngle - hipAngle)
    }

    /**
     * Calculate balance score
     */
    private fun calculateBalance(poseData: FramePoseData): Float {
        val leftAnkle = poseData[MediaPipePoseLandmarks.LEFT_ANKLE]
        val rightAnkle = poseData[MediaPipePoseLandmarks.RIGHT_ANKLE]
        val leftHip = poseData[MediaPipePoseLandmarks.LEFT_HIP]
        val rightHip = poseData[MediaPipePoseLandmarks.RIGHT_HIP]
        
        if (leftAnkle == null || rightAnkle == null || leftHip == null || rightHip == null) {
            return 0.5f
        }
        
        // Calculate center of mass
        val centerX = (leftHip.x + rightHip.x) / 2
        val baseX = (leftAnkle.x + rightAnkle.x) / 2
        
        // Balance is better when center of mass is over base of support
        val deviation = abs(centerX - baseX)
        return (1f - deviation).coerceIn(0f, 1f)
    }

    /**
     * Calculate setup quality score
     */
    private fun calculateSetupQuality(poseData: FramePoseData): Float {
        // Simple setup quality based on posture
        val spineAngle = calculateSpineAngle(poseData)
        val balance = calculateBalance(poseData)
        val kneeFlex = calculateKneeFlex(poseData)
        
        // Ideal setup: spine tilted forward, good balance, slight knee flex
        val spineScore = when {
            spineAngle > 10f && spineAngle < 30f -> 1f
            spineAngle > 5f && spineAngle < 40f -> 0.8f
            else -> 0.5f
        }
        
        val kneeScore = when {
            kneeFlex > 150f && kneeFlex < 170f -> 1f
            kneeFlex > 140f && kneeFlex < 180f -> 0.8f
            else -> 0.5f
        }
        
        return (spineScore + balance + kneeScore) / 3f
    }

    /**
     * Calculate overall confidence of pose detection
     */
    private fun calculateOverallConfidence(poseData: FramePoseData): Float {
        val confidences = poseData.values.mapNotNull { it.visibility }
        return if (confidences.isEmpty()) 0f else confidences.average().toFloat()
    }
}

/**
 * Data class for golf pose metrics
 */
data class GolfPoseMetrics(
    val shoulderAngle: Float,
    val hipAngle: Float,
    val spineAngle: Float,
    val kneeFlex: Float,
    val xFactor: Float,
    val balance: Float,
    val setupQuality: Float,
    val confidence: Float
) {
    companion object {
        fun empty() = GolfPoseMetrics(
            shoulderAngle = 0f,
            hipAngle = 0f,
            spineAngle = 0f,
            kneeFlex = 0f,
            xFactor = 0f,
            balance = 0f,
            setupQuality = 0f,
            confidence = 0f
        )
    }
}