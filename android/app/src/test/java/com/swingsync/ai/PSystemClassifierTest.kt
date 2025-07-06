package com.swingsync.ai

import com.swingsync.ai.analysis.PSystemClassifier
import com.swingsync.ai.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for P-System Classification Algorithm
 */
class PSystemClassifierTest {

    private lateinit var classifier: PSystemClassifier

    @Before
    fun setUp() {
        classifier = PSystemClassifier()
    }

    @Test
    fun testEmptyPoseResultsReturnsEmptyPhases() {
        val result = classifier.classifySwingPhases(emptyList())
        assertTrue("Empty pose results should return empty phases", result.isEmpty())
    }

    @Test
    fun testSingleFrameReturnsMinimalPhases() {
        val singleFrame = createMockPoseResult(0)
        val result = classifier.classifySwingPhases(listOf(singleFrame))
        
        // Should handle single frame gracefully
        assertTrue("Single frame should be handled", result.isNotEmpty())
    }

    @Test
    fun testFullSwingClassification() {
        // Create mock swing data
        val poseResults = createMockSwingSequence(100) // 100 frames
        val phases = classifier.classifySwingPhases(poseResults)
        
        // Should classify into P1-P10
        assertEquals("Should classify 10 phases", 10, phases.size)
        
        // Verify phase order
        val phaseNames = phases.map { it.phaseName }
        val expectedPhases = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10")
        assertEquals("Phases should be in correct order", expectedPhases, phaseNames)
        
        // Verify frame indices are sequential
        for (i in 0 until phases.size - 1) {
            assertTrue(
                "Phase end should be before next phase start",
                phases[i].endFrameIndex <= phases[i + 1].startFrameIndex
            )
        }
    }

    @Test
    fun testPhaseFrameIndicesAreValid() {
        val poseResults = createMockSwingSequence(60)
        val phases = classifier.classifySwingPhases(poseResults)
        
        phases.forEach { phase ->
            assertTrue(
                "Start frame should be non-negative",
                phase.startFrameIndex >= 0
            )
            assertTrue(
                "End frame should be greater than start",
                phase.endFrameIndex >= phase.startFrameIndex
            )
            assertTrue(
                "End frame should be within total frames",
                phase.endFrameIndex < poseResults.size
            )
        }
    }

    @Test
    fun testTopOfBackswingDetection() {
        val poseResults = createMockSwingWithClearTop(80)
        val phases = classifier.classifySwingPhases(poseResults)
        
        val p4Phase = phases.find { it.phaseName == "P4" }
        assertNotNull("P4 phase should exist", p4Phase)
        
        // P4 should be roughly in the middle of the swing
        val topFrame = p4Phase!!.endFrameIndex
        assertTrue(
            "Top of backswing should be in middle portion of swing",
            topFrame > poseResults.size / 4 && topFrame < poseResults.size * 3 / 4
        )
    }

    /**
     * Create a mock pose detection result
     */
    private fun createMockPoseResult(frameIndex: Int): PoseDetectionResult {
        // Create mock keypoints for a basic pose
        val keypoints = createMockKeypoints(frameIndex)
        
        return PoseDetectionResult(
            keypoints = keypoints,
            confidence = 0.8f,
            timestamp = System.currentTimeMillis() + frameIndex * 16, // ~60fps
            frameIndex = frameIndex
        )
    }

    /**
     * Create mock keypoints representing a golf pose
     */
    private fun createMockKeypoints(frameIndex: Int): FramePoseData {
        val keypoints = mutableMapOf<String, PoseKeypoint>()
        
        // Simulate hand movement during swing
        val swingProgress = frameIndex / 100f // Assuming 100 frame swing
        val handY = simulateHandMovement(swingProgress)
        val handX = 0.5f + (swingProgress - 0.5f) * 0.3f // Side to side movement
        
        // Add key golf swing keypoints
        keypoints[MediaPipePoseLandmarks.LEFT_WRIST] = PoseKeypoint(handX - 0.05f, handY, 0f, 0.9f)
        keypoints[MediaPipePoseLandmarks.RIGHT_WRIST] = PoseKeypoint(handX + 0.05f, handY, 0f, 0.9f)
        keypoints[MediaPipePoseLandmarks.LEFT_SHOULDER] = PoseKeypoint(0.3f, 0.3f, 0f, 0.95f)
        keypoints[MediaPipePoseLandmarks.RIGHT_SHOULDER] = PoseKeypoint(0.7f, 0.3f, 0f, 0.95f)
        keypoints[MediaPipePoseLandmarks.LEFT_HIP] = PoseKeypoint(0.35f, 0.6f, 0f, 0.9f)
        keypoints[MediaPipePoseLandmarks.RIGHT_HIP] = PoseKeypoint(0.65f, 0.6f, 0f, 0.9f)
        
        return keypoints
    }

    /**
     * Simulate hand movement during golf swing
     */
    private fun simulateHandMovement(progress: Float): Float {
        return when {
            progress < 0.1f -> 0.5f // Address
            progress < 0.4f -> 0.5f - (progress - 0.1f) * 0.8f // Backswing up
            progress < 0.5f -> 0.26f // Top of backswing
            progress < 0.7f -> 0.26f + (progress - 0.5f) * 1.2f // Downswing
            progress < 0.8f -> 0.5f // Impact
            else -> 0.5f - (progress - 0.8f) * 0.5f // Follow through
        }
    }

    /**
     * Create a mock swing sequence
     */
    private fun createMockSwingSequence(totalFrames: Int): List<PoseDetectionResult> {
        return (0 until totalFrames).map { createMockPoseResult(it) }
    }

    /**
     * Create a swing sequence with clear top of backswing
     */
    private fun createMockSwingWithClearTop(totalFrames: Int): List<PoseDetectionResult> {
        return (0 until totalFrames).map { frameIndex ->
            val result = createMockPoseResult(frameIndex)
            
            // Make frame at 1/3 position clearly the highest (top of backswing)
            if (frameIndex == totalFrames / 3) {
                val modifiedKeypoints = result.keypoints.toMutableMap()
                modifiedKeypoints[MediaPipePoseLandmarks.LEFT_WRIST] = 
                    result.keypoints[MediaPipePoseLandmarks.LEFT_WRIST]!!.copy(y = 0.1f)
                modifiedKeypoints[MediaPipePoseLandmarks.RIGHT_WRIST] = 
                    result.keypoints[MediaPipePoseLandmarks.RIGHT_WRIST]!!.copy(y = 0.1f)
                
                result.copy(keypoints = modifiedKeypoints)
            } else {
                result
            }
        }
    }
}