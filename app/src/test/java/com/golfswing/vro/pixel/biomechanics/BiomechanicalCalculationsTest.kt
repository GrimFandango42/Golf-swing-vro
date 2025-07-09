package com.golfswing.vro.pixel.biomechanics

import com.golfswing.vro.pixel.metrics.*
import com.google.mediapipe.solutions.pose.PoseLandmark
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.*

/**
 * Comprehensive tests for biomechanical calculations
 * Tests professional golf swing biomechanics according to PGA standards
 */
@RunWith(JUnit4::class)
class BiomechanicalCalculationsTest {

    private lateinit var biomechanicsCalculator: BiomechanicalCalculations
    private lateinit var mockPoseLandmarks: List<PoseLandmark>
    private lateinit var mockPreviousLandmarks: List<List<PoseLandmark>>

    @Before
    fun setUp() {
        biomechanicsCalculator = BiomechanicalCalculations()
        mockPoseLandmarks = createMockPoseLandmarks()
        mockPreviousLandmarks = createMockPreviousLandmarks()
    }

    @Test
    fun testXFactorCalculation() {
        val leftShoulder = PoseLandmark.create(0.3f, 0.4f, 0.5f)
        val rightShoulder = PoseLandmark.create(0.7f, 0.4f, 0.5f)
        val leftHip = PoseLandmark.create(0.35f, 0.6f, 0.5f)
        val rightHip = PoseLandmark.create(0.65f, 0.6f, 0.5f)

        val xFactor = biomechanicsCalculator.calculateXFactor(
            leftShoulder, rightShoulder, leftHip, rightHip
        )

        // X-Factor should be within realistic range
        assertTrue("X-Factor should be positive", xFactor >= 0f)
        assertTrue("X-Factor should be within realistic range", xFactor <= 90f)
    }

    @Test
    fun testXFactorWithExtremeSeparation() {
        // Test with maximum shoulder-hip separation
        val leftShoulder = PoseLandmark.create(0.2f, 0.4f, 0.5f)
        val rightShoulder = PoseLandmark.create(0.8f, 0.4f, 0.5f)
        val leftHip = PoseLandmark.create(0.4f, 0.6f, 0.5f)
        val rightHip = PoseLandmark.create(0.6f, 0.6f, 0.5f)

        val xFactor = biomechanicsCalculator.calculateXFactor(
            leftShoulder, rightShoulder, leftHip, rightHip
        )

        // Should be constrained to maximum 90 degrees
        assertTrue("X-Factor should be constrained to maximum 90 degrees", xFactor <= 90f)
    }

    @Test
    fun testXFactorStretch() {
        val xFactorHistory = listOf(15f, 25f, 35f, 45f, 30f, 20f)
        val xFactorStretch = biomechanicsCalculator.calculateXFactorStretch(xFactorHistory)

        assertEquals("X-Factor stretch should be maximum value", 45f, xFactorStretch, 0.01f)
    }

    @Test
    fun testXFactorStretchWithEmptyHistory() {
        val xFactorStretch = biomechanicsCalculator.calculateXFactorStretch(emptyList())
        assertEquals("Empty history should return 0", 0f, xFactorStretch, 0.01f)
    }

    @Test
    fun testKinematicSequenceAnalysis() {
        val sequence = biomechanicsCalculator.analyzeKinematicSequence(
            mockPoseLandmarks,
            mockPreviousLandmarks,
            30f
        )

        assertNotNull("Kinematic sequence should not be null", sequence)
        assertEquals("Should have 4 segments in sequence", 4, sequence.sequenceOrder.size)
        assertTrue("Sequence efficiency should be between 0 and 1", 
            sequence.sequenceEfficiency >= 0f && sequence.sequenceEfficiency <= 1f)
    }

    @Test
    fun testOptimalKinematicSequence() {
        val optimalSequence = biomechanicsCalculator.analyzeKinematicSequence(
            mockPoseLandmarks,
            createOptimalSequenceLandmarks(),
            30f
        )

        assertTrue("Optimal sequence should be efficient", optimalSequence.sequenceEfficiency > 0.8f)
        assertTrue("Should recognize optimal sequence", optimalSequence.isOptimalSequence)
    }

    @Test
    fun testPowerMetricsCalculation() {
        val powerMetrics = biomechanicsCalculator.calculatePowerMetrics(
            mockPoseLandmarks,
            mockPreviousLandmarks,
            30f
        )

        assertNotNull("Power metrics should not be null", powerMetrics)
        assertTrue("Total power should be positive", powerMetrics.totalPower >= 0f)
        assertTrue("Peak power should be positive", powerMetrics.peakPower >= 0f)
        assertTrue("Transfer efficiency should be between 0 and 1", 
            powerMetrics.powerTransferEfficiency >= 0f && powerMetrics.powerTransferEfficiency <= 1f)
    }

    @Test
    fun testAttackAngleCalculation() {
        val leftWrist = PoseLandmark.create(0.45f, 0.5f, 0.5f)
        val rightWrist = PoseLandmark.create(0.55f, 0.5f, 0.5f)
        val previousWrists = listOf(
            Pair(PoseLandmark.create(0.45f, 0.52f, 0.5f), PoseLandmark.create(0.55f, 0.52f, 0.5f)),
            Pair(PoseLandmark.create(0.45f, 0.54f, 0.5f), PoseLandmark.create(0.55f, 0.54f, 0.5f)),
            Pair(PoseLandmark.create(0.45f, 0.56f, 0.5f), PoseLandmark.create(0.55f, 0.56f, 0.5f))
        )

        val attackAngle = biomechanicsCalculator.calculateAttackAngle(
            leftWrist, rightWrist, previousWrists, 30f
        )

        // Attack angle should be within realistic range
        assertTrue("Attack angle should be within realistic range", 
            attackAngle >= -15f && attackAngle <= 15f)
    }

    @Test
    fun testSwingPlaneCalculation() {
        val leftWrist = PoseLandmark.create(0.45f, 0.6f, 0.5f)
        val rightWrist = PoseLandmark.create(0.55f, 0.6f, 0.5f)
        val leftShoulder = PoseLandmark.create(0.3f, 0.4f, 0.5f)
        val rightShoulder = PoseLandmark.create(0.7f, 0.4f, 0.5f)

        val swingPlane = biomechanicsCalculator.calculateSwingPlane(
            leftWrist, rightWrist, leftShoulder, rightShoulder
        )

        // Swing plane should be within normalized range
        assertTrue("Swing plane should be within normalized range", 
            swingPlane >= -90f && swingPlane <= 90f)
    }

    @Test
    fun testClubPathCalculation() {
        val leftWrist = PoseLandmark.create(0.45f, 0.5f, 0.5f)
        val rightWrist = PoseLandmark.create(0.55f, 0.5f, 0.5f)
        val previousWrists = listOf(
            Pair(PoseLandmark.create(0.43f, 0.5f, 0.48f), PoseLandmark.create(0.53f, 0.5f, 0.48f)),
            Pair(PoseLandmark.create(0.41f, 0.5f, 0.46f), PoseLandmark.create(0.51f, 0.5f, 0.46f)),
            Pair(PoseLandmark.create(0.39f, 0.5f, 0.44f), PoseLandmark.create(0.49f, 0.5f, 0.44f))
        )

        val clubPath = biomechanicsCalculator.calculateClubPath(
            leftWrist, rightWrist, previousWrists, 0f
        )

        // Club path should be calculable and within reasonable range
        assertTrue("Club path should be calculable", !clubPath.isNaN())
        assertTrue("Club path should be within reasonable range", 
            abs(clubPath) <= 45f)
    }

    @Test
    fun testGroundForceCalculation() {
        val groundForce = biomechanicsCalculator.calculateGroundForce(
            mockPoseLandmarks,
            mockPreviousLandmarks,
            30f
        )

        assertNotNull("Ground force should not be null", groundForce)
        assertTrue("Vertical force should be positive", groundForce.verticalForce > 0f)
        assertTrue("Ground force index should be between 0 and 1", 
            groundForce.groundForceIndex >= 0f && groundForce.groundForceIndex <= 1f)
    }

    @Test
    fun testEnergyTransferCalculation() {
        val energyTransfer = biomechanicsCalculator.calculateEnergyTransfer(
            mockPoseLandmarks,
            mockPreviousLandmarks,
            30f
        )

        assertNotNull("Energy transfer should not be null", energyTransfer)
        assertTrue("Kinetic energy should be positive", energyTransfer.kineticEnergy >= 0f)
        assertTrue("Potential energy should be positive", energyTransfer.potentialEnergy >= 0f)
        assertTrue("Transfer efficiency should be between 0 and 1", 
            energyTransfer.transferEfficiency >= 0f && energyTransfer.transferEfficiency <= 1f)
    }

    @Test
    fun testSwingConsistencyCalculation() {
        val currentMetrics = createMockEnhancedSwingMetrics()
        val historicalMetrics = createMockHistoricalMetrics()

        val consistency = biomechanicsCalculator.calculateSwingConsistency(
            currentMetrics,
            historicalMetrics
        )

        assertNotNull("Swing consistency should not be null", consistency)
        assertTrue("Overall consistency should be between 0 and 1", 
            consistency.overallConsistency >= 0f && consistency.overallConsistency <= 1f)
        assertTrue("Repeatability score should be between 0 and 1", 
            consistency.repeatabilityScore >= 0f && consistency.repeatabilityScore <= 1f)
    }

    @Test
    fun testSwingTimingCalculation() {
        val phaseHistory = listOf(
            "SETUP", "SETUP", "SETUP", "SETUP", "SETUP",
            "BACKSWING", "BACKSWING", "BACKSWING", "BACKSWING", "BACKSWING",
            "BACKSWING", "BACKSWING", "BACKSWING", "BACKSWING", "BACKSWING",
            "TRANSITION", "TRANSITION", "TRANSITION",
            "DOWNSWING", "DOWNSWING", "DOWNSWING", "DOWNSWING", "DOWNSWING",
            "IMPACT", "IMPACT",
            "FOLLOW_THROUGH", "FOLLOW_THROUGH", "FOLLOW_THROUGH", "FOLLOW_THROUGH"
        )

        val timing = biomechanicsCalculator.calculateSwingTiming(phaseHistory, 30f)

        assertNotNull("Swing timing should not be null", timing)
        assertTrue("Total swing time should be positive", timing.totalSwingTime > 0f)
        assertTrue("Backswing time should be positive", timing.backswingTime > 0f)
        assertTrue("Downswing time should be positive", timing.downswingTime > 0f)
        assertTrue("Tempo ratio should be positive", timing.tempoRatio > 0f)
    }

    @Test
    fun testBiomechanicalConstraints() {
        // Test that all calculations respect biomechanical constraints
        val leftShoulder = PoseLandmark.create(0.3f, 0.4f, 0.5f)
        val rightShoulder = PoseLandmark.create(0.7f, 0.4f, 0.5f)
        val leftHip = PoseLandmark.create(0.35f, 0.6f, 0.5f)
        val rightHip = PoseLandmark.create(0.65f, 0.6f, 0.5f)

        val xFactor = biomechanicsCalculator.calculateXFactor(
            leftShoulder, rightShoulder, leftHip, rightHip
        )

        // X-Factor should respect anatomical limits
        assertTrue("X-Factor should respect anatomical limits", xFactor <= 90f)
        assertTrue("X-Factor should be non-negative", xFactor >= 0f)
    }

    @Test
    fun testCalculationStability() {
        // Test that calculations are stable with similar inputs
        val leftShoulder1 = PoseLandmark.create(0.3f, 0.4f, 0.5f)
        val rightShoulder1 = PoseLandmark.create(0.7f, 0.4f, 0.5f)
        val leftHip1 = PoseLandmark.create(0.35f, 0.6f, 0.5f)
        val rightHip1 = PoseLandmark.create(0.65f, 0.6f, 0.5f)

        val leftShoulder2 = PoseLandmark.create(0.301f, 0.401f, 0.501f)
        val rightShoulder2 = PoseLandmark.create(0.701f, 0.401f, 0.501f)
        val leftHip2 = PoseLandmark.create(0.351f, 0.601f, 0.501f)
        val rightHip2 = PoseLandmark.create(0.651f, 0.601f, 0.501f)

        val xFactor1 = biomechanicsCalculator.calculateXFactor(
            leftShoulder1, rightShoulder1, leftHip1, rightHip1
        )
        val xFactor2 = biomechanicsCalculator.calculateXFactor(
            leftShoulder2, rightShoulder2, leftHip2, rightHip2
        )

        // Results should be very similar for similar inputs
        assertTrue("Calculations should be stable", abs(xFactor1 - xFactor2) < 0.1f)
    }

    // Helper methods for creating mock data
    private fun createMockPoseLandmarks(): List<PoseLandmark> {
        return listOf(
            PoseLandmark.create(0.5f, 0.2f, 0.5f),     // NOSE
            PoseLandmark.create(0.3f, 0.4f, 0.5f),     // LEFT_SHOULDER
            PoseLandmark.create(0.7f, 0.4f, 0.5f),     // RIGHT_SHOULDER
            PoseLandmark.create(0.25f, 0.55f, 0.5f),   // LEFT_ELBOW
            PoseLandmark.create(0.75f, 0.55f, 0.5f),   // RIGHT_ELBOW
            PoseLandmark.create(0.2f, 0.7f, 0.5f),     // LEFT_WRIST
            PoseLandmark.create(0.8f, 0.7f, 0.5f),     // RIGHT_WRIST
            PoseLandmark.create(0.35f, 0.6f, 0.5f),    // LEFT_HIP
            PoseLandmark.create(0.65f, 0.6f, 0.5f),    // RIGHT_HIP
            PoseLandmark.create(0.33f, 0.8f, 0.5f),    // LEFT_KNEE
            PoseLandmark.create(0.67f, 0.8f, 0.5f),    // RIGHT_KNEE
            PoseLandmark.create(0.31f, 0.95f, 0.5f),   // LEFT_ANKLE
            PoseLandmark.create(0.69f, 0.95f, 0.5f)    // RIGHT_ANKLE
        )
    }

    private fun createMockPreviousLandmarks(): List<List<PoseLandmark>> {
        return listOf(
            createMockPoseLandmarks(),
            createMockPoseLandmarks(),
            createMockPoseLandmarks(),
            createMockPoseLandmarks(),
            createMockPoseLandmarks()
        )
    }

    private fun createOptimalSequenceLandmarks(): List<List<PoseLandmark>> {
        val landmarks = mutableListOf<List<PoseLandmark>>()
        
        // Create sequence showing optimal kinematic chain
        for (i in 0..10) {
            val progress = i / 10f
            val poseList = mutableListOf<PoseLandmark>()
            
            // Simulate optimal sequence: pelvis -> torso -> arm -> club
            val pelvisRotation = sin(progress * PI).toFloat() * 0.1f
            val torsoRotation = sin((progress - 0.1f) * PI).toFloat() * 0.15f
            val armRotation = sin((progress - 0.2f) * PI).toFloat() * 0.2f
            
            poseList.add(PoseLandmark.create(0.5f, 0.2f, 0.5f))                    // NOSE
            poseList.add(PoseLandmark.create(0.3f + torsoRotation, 0.4f, 0.5f))    // LEFT_SHOULDER
            poseList.add(PoseLandmark.create(0.7f - torsoRotation, 0.4f, 0.5f))    // RIGHT_SHOULDER
            poseList.add(PoseLandmark.create(0.25f + armRotation, 0.55f, 0.5f))    // LEFT_ELBOW
            poseList.add(PoseLandmark.create(0.75f - armRotation, 0.55f, 0.5f))    // RIGHT_ELBOW
            poseList.add(PoseLandmark.create(0.2f + armRotation, 0.7f, 0.5f))      // LEFT_WRIST
            poseList.add(PoseLandmark.create(0.8f - armRotation, 0.7f, 0.5f))      // RIGHT_WRIST
            poseList.add(PoseLandmark.create(0.35f + pelvisRotation, 0.6f, 0.5f))  // LEFT_HIP
            poseList.add(PoseLandmark.create(0.65f - pelvisRotation, 0.6f, 0.5f))  // RIGHT_HIP
            poseList.add(PoseLandmark.create(0.33f, 0.8f, 0.5f))                   // LEFT_KNEE
            poseList.add(PoseLandmark.create(0.67f, 0.8f, 0.5f))                   // RIGHT_KNEE
            poseList.add(PoseLandmark.create(0.31f, 0.95f, 0.5f))                  // LEFT_ANKLE
            poseList.add(PoseLandmark.create(0.69f, 0.95f, 0.5f))                  // RIGHT_ANKLE
            
            landmarks.add(poseList)
        }
        
        return landmarks
    }

    private fun createMockEnhancedSwingMetrics(): EnhancedSwingMetrics {
        return EnhancedSwingMetrics(
            xFactor = 45f,
            tempo = 3.0f,
            balance = 0.8f,
            swingPlane = 60f,
            clubPath = -2f,
            attackAngle = 5f,
            headPosition = 0.5f,
            shoulderTilt = 15f,
            hipSlide = 0.1f,
            weightTransfer = 0.7f,
            sequenceScore = 0.85f,
            powerGeneration = 85f,
            consistency = 0.75f,
            efficiency = 0.9f,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createMockHistoricalMetrics(): List<EnhancedSwingMetrics> {
        return listOf(
            createMockEnhancedSwingMetrics(),
            createMockEnhancedSwingMetrics().copy(xFactor = 42f, tempo = 2.8f),
            createMockEnhancedSwingMetrics().copy(xFactor = 47f, tempo = 3.2f),
            createMockEnhancedSwingMetrics().copy(xFactor = 44f, tempo = 3.1f),
            createMockEnhancedSwingMetrics().copy(xFactor = 46f, tempo = 2.9f),
            createMockEnhancedSwingMetrics().copy(xFactor = 43f, tempo = 3.0f),
            createMockEnhancedSwingMetrics().copy(xFactor = 48f, tempo = 3.3f),
            createMockEnhancedSwingMetrics().copy(xFactor = 45f, tempo = 3.0f)
        )
    }
}