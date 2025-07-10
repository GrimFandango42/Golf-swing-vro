package com.swingsync.ai.analysis

import android.util.Log
import com.swingsync.ai.data.model.*
import kotlin.math.*

/**
 * Pose detection optimizer for performance and accuracy
 * Optimizes pose detection pipeline for golf swing analysis
 */
class PoseDetectionOptimizer {

    companion object {
        private const val TAG = "PoseDetectionOptimizer"
        private const val SMOOTHING_FACTOR = 0.3f
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val MAX_HISTORY_SIZE = 10
    }

    private val poseHistory = mutableListOf<FramePoseData>()
    private val performanceMetrics = mutableMapOf<String, Float>()
    private var frameCount = 0
    private var totalProcessingTime = 0L

    /**
     * Optimize pose detection result
     */
    fun optimizePoseData(rawPoseData: FramePoseData, processingTime: Long): FramePoseData {
        try {
            // Update performance metrics
            updatePerformanceMetrics(processingTime)
            
            // Filter low confidence keypoints
            val filteredData = filterLowConfidenceKeypoints(rawPoseData)
            
            // Apply smoothing
            val smoothedData = applySmoothingFilter(filteredData)
            
            // Update history
            updateHistory(smoothedData)
            
            return smoothedData

        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing pose data", e)
            return rawPoseData
        }
    }

    /**
     * Filter out keypoints with low confidence
     */
    private fun filterLowConfidenceKeypoints(poseData: FramePoseData): FramePoseData {
        return poseData.filterValues { keypoint ->
            keypoint.visibility?.let { it >= CONFIDENCE_THRESHOLD } ?: false
        }
    }

    /**
     * Apply smoothing filter to reduce jitter
     */
    private fun applySmoothingFilter(poseData: FramePoseData): FramePoseData {
        if (poseHistory.isEmpty()) {
            return poseData
        }

        val previousFrame = poseHistory.last()
        val smoothedData = mutableMapOf<String, PoseKeypoint>()

        poseData.forEach { (landmarkName, currentKeypoint) ->
            val previousKeypoint = previousFrame[landmarkName]
            
            if (previousKeypoint != null) {
                // Apply exponential smoothing
                val smoothedX = previousKeypoint.x * (1f - SMOOTHING_FACTOR) + currentKeypoint.x * SMOOTHING_FACTOR
                val smoothedY = previousKeypoint.y * (1f - SMOOTHING_FACTOR) + currentKeypoint.y * SMOOTHING_FACTOR
                val smoothedZ = previousKeypoint.z * (1f - SMOOTHING_FACTOR) + currentKeypoint.z * SMOOTHING_FACTOR
                
                smoothedData[landmarkName] = PoseKeypoint(
                    x = smoothedX,
                    y = smoothedY,
                    z = smoothedZ,
                    visibility = currentKeypoint.visibility
                )
            } else {
                smoothedData[landmarkName] = currentKeypoint
            }
        }

        return smoothedData
    }

    /**
     * Update pose history
     */
    private fun updateHistory(poseData: FramePoseData) {
        poseHistory.add(poseData)
        
        // Keep history size manageable
        if (poseHistory.size > MAX_HISTORY_SIZE) {
            poseHistory.removeAt(0)
        }
    }

    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics(processingTime: Long) {
        frameCount++
        totalProcessingTime += processingTime
        
        val averageProcessingTime = totalProcessingTime.toFloat() / frameCount
        val currentFPS = if (processingTime > 0) 1000f / processingTime else 0f
        
        performanceMetrics["average_processing_time"] = averageProcessingTime
        performanceMetrics["current_fps"] = currentFPS
        performanceMetrics["frame_count"] = frameCount.toFloat()
        
        // Log performance every 30 frames
        if (frameCount % 30 == 0) {
            Log.d(TAG, "Performance - Avg: ${averageProcessingTime.toInt()}ms, FPS: ${currentFPS.toInt()}")
        }
    }

    /**
     * Get current performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Float> {
        return performanceMetrics.toMap()
    }

    /**
     * Check if pose detection is stable
     */
    fun isPoseStable(): Boolean {
        if (poseHistory.size < 3) return false
        
        // Check if recent poses are similar
        val recentPoses = poseHistory.takeLast(3)
        val variations = mutableListOf<Float>()
        
        // Calculate variation in key landmarks
        val keyLandmarks = listOf(
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP
        )
        
        keyLandmarks.forEach { landmarkName ->
            val positions = recentPoses.mapNotNull { it[landmarkName] }
            if (positions.size >= 2) {
                val variation = calculatePositionVariation(positions)
                variations.add(variation)
            }
        }
        
        // Consider pose stable if variation is low
        val averageVariation = variations.average()
        return averageVariation < 0.02f // 2% variation threshold
    }

    /**
     * Calculate position variation for a set of keypoints
     */
    private fun calculatePositionVariation(positions: List<PoseKeypoint>): Float {
        if (positions.size < 2) return 0f
        
        val avgX = positions.map { it.x }.average()
        val avgY = positions.map { it.y }.average()
        
        val variations = positions.map { keypoint ->
            val dx = keypoint.x - avgX
            val dy = keypoint.y - avgY
            sqrt(dx * dx + dy * dy)
        }
        
        return variations.average().toFloat()
    }

    /**
     * Reset optimizer state
     */
    fun reset() {
        poseHistory.clear()
        performanceMetrics.clear()
        frameCount = 0
        totalProcessingTime = 0L
    }

    /**
     * Get optimization recommendations
     */
    fun getOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        val avgProcessingTime = performanceMetrics["average_processing_time"] ?: 0f
        val currentFPS = performanceMetrics["current_fps"] ?: 0f
        
        if (avgProcessingTime > 50f) {
            recommendations.add("Consider reducing input image resolution")
        }
        
        if (currentFPS < 15f) {
            recommendations.add("Frame rate is low - optimize processing pipeline")
        }
        
        if (poseHistory.size > 5 && !isPoseStable()) {
            recommendations.add("Pose detection is unstable - check lighting conditions")
        }
        
        return recommendations
    }
}