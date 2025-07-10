package com.swingsync.ai.analysis

import android.util.Log
import com.swingsync.ai.data.model.*
import kotlin.math.*

/**
 * Golf swing phase detector for real-time analysis
 * Detects swing phases based on pose keypoint movements
 */
class GolfSwingPhaseDetector {

    companion object {
        private const val TAG = "GolfSwingPhaseDetector"
        private const val MOVEMENT_THRESHOLD = 0.02f
        private const val VELOCITY_THRESHOLD = 0.05f
    }

    private val poseHistory = mutableListOf<FramePoseData>()
    private var currentPhase = GolfSwingPhase.P1
    private var lastPhaseChangeTime = 0L

    /**
     * Detect current swing phase from pose data
     */
    fun detectPhase(poseData: FramePoseData, timestamp: Long): GolfSwingPhase {
        try {
            // Add to history
            poseHistory.add(poseData)
            if (poseHistory.size > 30) { // Keep last 30 frames
                poseHistory.removeAt(0)
            }

            // Detect phase based on movement patterns
            val detectedPhase = analyzeMovementPattern(poseData, timestamp)
            
            // Update current phase if changed
            if (detectedPhase != currentPhase) {
                Log.d(TAG, "Phase change detected: ${currentPhase.displayName} -> ${detectedPhase.displayName}")
                currentPhase = detectedPhase
                lastPhaseChangeTime = timestamp
            }

            return currentPhase

        } catch (e: Exception) {
            Log.e(TAG, "Error detecting swing phase", e)
            return currentPhase
        }
    }

    /**
     * Analyze movement pattern to determine swing phase
     */
    private fun analyzeMovementPattern(poseData: FramePoseData, timestamp: Long): GolfSwingPhase {
        if (poseHistory.size < 5) {
            return GolfSwingPhase.P1 // Not enough data
        }

        // Simple phase detection based on hand movement
        val leftWrist = poseData[MediaPipePoseLandmarks.LEFT_WRIST]
        val rightWrist = poseData[MediaPipePoseLandmarks.RIGHT_WRIST]
        
        if (leftWrist == null || rightWrist == null) {
            return currentPhase
        }

        // Calculate hand velocity
        val handVelocity = calculateHandVelocity()
        
        // Basic phase logic
        return when {
            handVelocity < MOVEMENT_THRESHOLD -> GolfSwingPhase.P1 // Address/Setup
            handVelocity > VELOCITY_THRESHOLD && isMovingUp() -> GolfSwingPhase.P3 // Backswing
            handVelocity > VELOCITY_THRESHOLD && isMovingDown() -> GolfSwingPhase.P6 // Downswing
            else -> currentPhase
        }
    }

    /**
     * Calculate hand velocity from recent frames
     */
    private fun calculateHandVelocity(): Float {
        if (poseHistory.size < 2) return 0f
        
        val current = poseHistory.last()
        val previous = poseHistory[poseHistory.size - 2]
        
        val currentWrist = current[MediaPipePoseLandmarks.LEFT_WRIST] ?: return 0f
        val previousWrist = previous[MediaPipePoseLandmarks.LEFT_WRIST] ?: return 0f
        
        val dx = currentWrist.x - previousWrist.x
        val dy = currentWrist.y - previousWrist.y
        
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Check if hands are moving upward
     */
    private fun isMovingUp(): Boolean {
        if (poseHistory.size < 2) return false
        
        val current = poseHistory.last()
        val previous = poseHistory[poseHistory.size - 2]
        
        val currentWrist = current[MediaPipePoseLandmarks.LEFT_WRIST] ?: return false
        val previousWrist = previous[MediaPipePoseLandmarks.LEFT_WRIST] ?: return false
        
        return currentWrist.y < previousWrist.y // Y decreases as we go up
    }

    /**
     * Check if hands are moving downward
     */
    private fun isMovingDown(): Boolean {
        if (poseHistory.size < 2) return false
        
        val current = poseHistory.last()
        val previous = poseHistory[poseHistory.size - 2]
        
        val currentWrist = current[MediaPipePoseLandmarks.LEFT_WRIST] ?: return false
        val previousWrist = previous[MediaPipePoseLandmarks.LEFT_WRIST] ?: return false
        
        return currentWrist.y > previousWrist.y // Y increases as we go down
    }

    /**
     * Reset detector state
     */
    fun reset() {
        poseHistory.clear()
        currentPhase = GolfSwingPhase.P1
        lastPhaseChangeTime = 0L
    }
}