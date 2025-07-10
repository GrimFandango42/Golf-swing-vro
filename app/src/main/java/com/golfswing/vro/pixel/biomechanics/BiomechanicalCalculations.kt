package com.golfswing.vro.pixel.biomechanics

import com.golfswing.vro.pixel.metrics.*
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

/**
 * Professional biomechanical calculations for golf swing analysis
 * Implements PGA teaching standards and sports science principles
 */
class BiomechanicalCalculations {

    companion object {
        // Anthropometric constants
        private const val SHOULDER_WIDTH_RATIO = 0.18f
        private const val HIP_WIDTH_RATIO = 0.15f
        private const val ARM_LENGTH_RATIO = 0.35f
        private const val TORSO_LENGTH_RATIO = 0.30f
        
        // Physics constants
        private const val GRAVITY = 9.81f
        private const val AVERAGE_BODY_MASS = 75f // kg
        private const val MOMENT_OF_INERTIA_FACTOR = 0.4f
        
        // Filtering constants
        private const val VELOCITY_SMOOTHING_FACTOR = 0.3f
        private const val ACCELERATION_THRESHOLD = 50f
    }

    /**
     * Calculate X-Factor (shoulder-hip separation)
     * Core metric in professional golf instruction
     */
    fun calculateXFactor(
        leftShoulder: PoseLandmark,
        rightShoulder: PoseLandmark,
        leftHip: PoseLandmark,
        rightHip: PoseLandmark
    ): Float {
        val shoulderAngle = calculateSegmentAngle(leftShoulder, rightShoulder)
        val hipAngle = calculateSegmentAngle(leftHip, rightHip)
        
        // X-Factor is the difference between shoulder and hip rotation
        val xFactor = abs(shoulderAngle - hipAngle)
        
        // Apply biomechanical constraints
        return xFactor.coerceIn(0f, 90f)
    }

    /**
     * Calculate X-Factor stretch (maximum separation achieved)
     */
    fun calculateXFactorStretch(xFactorHistory: List<Float>): Float {
        if (xFactorHistory.isEmpty()) return 0f
        return xFactorHistory.maxOrNull() ?: 0f
    }

    /**
     * Analyze kinematic sequence
     * Proper sequence: Pelvis -> Torso -> Lead Arm -> Club
     */
    fun analyzeKinematicSequence(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float = 30f
    ): KinematicSequence {
        if (previousLandmarks.size < 5) {
            return KinematicSequence(
                sequenceOrder = listOf(BodySegment.PELVIS, BodySegment.TORSO, BodySegment.LEAD_ARM, BodySegment.CLUB),
                peakVelocityOrder = emptyList(),
                sequenceEfficiency = 0f,
                isOptimalSequence = false,
                sequenceGaps = emptyList()
            )
        }
        
        val segmentVelocities = calculateSegmentVelocities(landmarks, previousLandmarks, frameRate)
        val peakVelocityOrder = determineVelocityPeakOrder(segmentVelocities)
        val sequenceEfficiency = calculateSequenceEfficiency(peakVelocityOrder)
        val isOptimal = isOptimalKinematicSequence(peakVelocityOrder)
        val sequenceGaps = calculateSequenceGaps(peakVelocityOrder)
        
        return KinematicSequence(
            sequenceOrder = peakVelocityOrder.map { it.segment },
            peakVelocityOrder = peakVelocityOrder,
            sequenceEfficiency = sequenceEfficiency,
            isOptimalSequence = isOptimal,
            sequenceGaps = sequenceGaps
        )
    }

    /**
     * Calculate power metrics
     */
    fun calculatePowerMetrics(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float = 30f
    ): PowerMetrics {
        val velocities = calculateBodyVelocities(landmarks, previousLandmarks, frameRate)
        val accelerations = calculateBodyAccelerations(velocities, frameRate)
        
        val totalPower = calculateTotalPower(velocities, accelerations)
        val peakPower = calculatePeakPower(velocities)
        val transferEfficiency = calculatePowerTransferEfficiency(velocities)
        val groundForceContribution = calculateGroundForceContribution(landmarks)
        val rotationalPower = calculateRotationalPower(landmarks, previousLandmarks, frameRate)
        val linearPower = calculateLinearPower(velocities)
        
        return PowerMetrics(
            totalPower = totalPower,
            peakPower = peakPower,
            powerTransferEfficiency = transferEfficiency,
            groundForceContribution = groundForceContribution,
            rotationalPower = rotationalPower,
            linearPower = linearPower,
            powerSequence = calculatePowerSequence(velocities)
        )
    }

    /**
     * Calculate attack angle
     */
    fun calculateAttackAngle(
        leftWrist: PoseLandmark,
        rightWrist: PoseLandmark,
        previousWrists: List<Pair<PoseLandmark, PoseLandmark>>,
        frameRate: Float = 30f
    ): Float {
        if (previousWrists.size < 3) return 0f
        
        val wristCenter = calculateMidpoint(leftWrist, rightWrist)
        val previousWristCenter = calculateMidpoint(previousWrists.last().first, previousWrists.last().second)
        
        val deltaY = wristCenter.y - previousWristCenter.y
        val deltaX = wristCenter.x - previousWristCenter.x
        
        val angle = atan2(deltaY, deltaX) * 180f / PI.toFloat()
        
        // Constrain to realistic attack angle range
        return angle.coerceIn(-15f, 15f)
    }

    /**
     * Calculate swing plane
     */
    fun calculateSwingPlane(
        leftWrist: PoseLandmark,
        rightWrist: PoseLandmark,
        leftShoulder: PoseLandmark,
        rightShoulder: PoseLandmark
    ): Float {
        val wristCenter = calculateMidpoint(leftWrist, rightWrist)
        val shoulderCenter = calculateMidpoint(leftShoulder, rightShoulder)
        
        val deltaY = wristCenter.y - shoulderCenter.y
        val deltaZ = wristCenter.z - shoulderCenter.z
        
        val planeAngle = atan2(deltaY, deltaZ) * 180f / PI.toFloat()
        
        // Normalize to standard swing plane range
        return planeAngle.coerceIn(-90f, 90f)
    }

    /**
     * Calculate club path
     */
    fun calculateClubPath(
        leftWrist: PoseLandmark,
        rightWrist: PoseLandmark,
        previousWrists: List<Pair<PoseLandmark, PoseLandmark>>,
        targetLine: Float = 0f
    ): Float {
        if (previousWrists.size < 3) return 0f
        
        val wristCenter = calculateMidpoint(leftWrist, rightWrist)
        val previousWristCenter = calculateMidpoint(previousWrists.last().first, previousWrists.last().second)
        
        val deltaX = wristCenter.x - previousWristCenter.x
        val deltaZ = wristCenter.z - previousWristCenter.z
        
        val pathAngle = atan2(deltaX, deltaZ) * 180f / PI.toFloat()
        
        // Return relative to target line
        return pathAngle - targetLine
    }

    /**
     * Calculate ground force analysis
     */
    fun calculateGroundForce(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float = 30f
    ): GroundForce {
        val leftAnkle = landmarks[PoseLandmark.LEFT_ANKLE]
        val rightAnkle = landmarks[PoseLandmark.RIGHT_ANKLE]
        val leftKnee = landmarks[PoseLandmark.LEFT_KNEE]
        val rightKnee = landmarks[PoseLandmark.RIGHT_KNEE]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        
        val weightDistribution = calculateWeightDistribution(leftAnkle, rightAnkle, leftKnee, rightKnee, leftHip, rightHip)
        val verticalForce = calculateVerticalForce(landmarks, previousLandmarks, frameRate)
        val horizontalForce = calculateHorizontalForce(landmarks, previousLandmarks, frameRate)
        
        return GroundForce(
            verticalForce = verticalForce,
            horizontalForce = horizontalForce,
            forceDistribution = weightDistribution,
            forceSequence = calculateForceSequence(landmarks, previousLandmarks, frameRate),
            groundForceIndex = calculateGroundForceIndex(verticalForce, horizontalForce)
        )
    }

    /**
     * Calculate energy transfer
     */
    fun calculateEnergyTransfer(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float = 30f
    ): EnergyTransfer {
        val velocities = calculateBodyVelocities(landmarks, previousLandmarks, frameRate)
        val kineticEnergy = calculateKineticEnergy(velocities)
        val potentialEnergy = calculatePotentialEnergy(landmarks)
        val energyLoss = calculateEnergyLoss(kineticEnergy, potentialEnergy)
        val transferEfficiency = calculateEnergyTransferEfficiency(energyLoss, kineticEnergy)
        
        return EnergyTransfer(
            kineticEnergy = kineticEnergy,
            potentialEnergy = potentialEnergy,
            energyLoss = energyLoss,
            transferEfficiency = transferEfficiency,
            energySequence = calculateEnergySequence(velocities)
        )
    }

    /**
     * Calculate swing consistency
     */
    fun calculateSwingConsistency(
        currentMetrics: EnhancedSwingMetrics,
        historicalMetrics: List<EnhancedSwingMetrics>
    ): SwingConsistency {
        if (historicalMetrics.size < 5) {
            return SwingConsistency(
                overallConsistency = 0f,
                temporalConsistency = 0f,
                spatialConsistency = 0f,
                kinematicConsistency = 0f,
                metricVariations = emptyMap(),
                repeatabilityScore = 0f,
                consistencyTrend = ConsistencyTrend(TrendDirection.STABLE, 0f, 0f, 0f)
            )
        }
        
        val temporalConsistency = calculateTemporalConsistency(historicalMetrics)
        val spatialConsistency = calculateSpatialConsistency(historicalMetrics)
        val kinematicConsistency = calculateKinematicConsistency(historicalMetrics)
        val metricVariations = calculateMetricVariations(historicalMetrics)
        val repeatabilityScore = calculateRepeatabilityScore(historicalMetrics)
        val consistencyTrend = calculateConsistencyTrend(historicalMetrics)
        
        val overallConsistency = (temporalConsistency + spatialConsistency + kinematicConsistency) / 3f
        
        return SwingConsistency(
            overallConsistency = overallConsistency,
            temporalConsistency = temporalConsistency,
            spatialConsistency = spatialConsistency,
            kinematicConsistency = kinematicConsistency,
            metricVariations = metricVariations,
            repeatabilityScore = repeatabilityScore,
            consistencyTrend = consistencyTrend
        )
    }

    /**
     * Calculate swing timing
     */
    fun calculateSwingTiming(
        phaseHistory: List<String>,
        frameRate: Float = 30f
    ): SwingTiming {
        if (phaseHistory.isEmpty()) return SwingTiming(0f, 0f, 0f, 0f, 0f, 0f, emptyMap())
        
        val phaseDurations = calculatePhaseDurations(phaseHistory, frameRate)
        val totalSwingTime = phaseDurations.values.sum()
        val backswingTime = phaseDurations["BACKSWING"] ?: 0f
        val downswingTime = phaseDurations["DOWNSWING"] ?: 0f
        val transitionTime = phaseDurations["TRANSITION"] ?: 0f
        val tempoRatio = if (downswingTime > 0) backswingTime / downswingTime else 0f
        val timingEfficiency = calculateTimingEfficiency(phaseDurations)
        
        return SwingTiming(
            totalSwingTime = totalSwingTime,
            backswingTime = backswingTime,
            downswingTime = downswingTime,
            transitionTime = transitionTime,
            tempoRatio = tempoRatio,
            timingEfficiency = timingEfficiency,
            phaseDurations = phaseDurations
        )
    }

    // Private helper methods
    private fun calculateSegmentAngle(point1: PoseLandmark, point2: PoseLandmark): Float {
        val deltaY = point2.y - point1.y
        val deltaX = point2.x - point1.x
        return atan2(deltaY, deltaX) * 180f / PI.toFloat()
    }

    private fun calculateMidpoint(point1: PoseLandmark, point2: PoseLandmark): PoseLandmark {
        return PoseLandmark.create(
            (point1.x + point2.x) / 2f,
            (point1.y + point2.y) / 2f,
            (point1.z + point2.z) / 2f
        )
    }

    private fun calculateSegmentVelocities(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): List<BodySegmentVelocity> {
        val velocities = mutableListOf<BodySegmentVelocity>()
        
        // Calculate pelvis velocity
        val pelvisVelocity = calculatePelvisVelocity(landmarks, previousLandmarks, frameRate)
        velocities.add(BodySegmentVelocity(BodySegment.PELVIS, pelvisVelocity, 0f, emptyList()))
        
        // Calculate torso velocity
        val torsoVelocity = calculateTorsoVelocity(landmarks, previousLandmarks, frameRate)
        velocities.add(BodySegmentVelocity(BodySegment.TORSO, torsoVelocity, 0f, emptyList()))
        
        // Calculate arm velocity
        val armVelocity = calculateArmVelocity(landmarks, previousLandmarks, frameRate)
        velocities.add(BodySegmentVelocity(BodySegment.LEAD_ARM, armVelocity, 0f, emptyList()))
        
        // Calculate club velocity (estimated from wrist movement)
        val clubVelocity = calculateClubVelocity(landmarks, previousLandmarks, frameRate)
        velocities.add(BodySegmentVelocity(BodySegment.CLUB, clubVelocity, 0f, emptyList()))
        
        return velocities
    }

    private fun calculatePelvisVelocity(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        if (previousLandmarks.isEmpty()) return 0f
        
        val currentHipCenter = calculateMidpoint(
            landmarks[PoseLandmark.LEFT_HIP],
            landmarks[PoseLandmark.RIGHT_HIP]
        )
        val previousHipCenter = calculateMidpoint(
            previousLandmarks.last()[PoseLandmark.LEFT_HIP],
            previousLandmarks.last()[PoseLandmark.RIGHT_HIP]
        )
        
        val deltaX = currentHipCenter.x - previousHipCenter.x
        val deltaTime = 1f / frameRate
        
        return abs(deltaX) / deltaTime * 1000f // Convert to degrees per second
    }

    private fun calculateTorsoVelocity(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        if (previousLandmarks.isEmpty()) return 0f
        
        val currentShoulderCenter = calculateMidpoint(
            landmarks[PoseLandmark.LEFT_SHOULDER],
            landmarks[PoseLandmark.RIGHT_SHOULDER]
        )
        val previousShoulderCenter = calculateMidpoint(
            previousLandmarks.last()[PoseLandmark.LEFT_SHOULDER],
            previousLandmarks.last()[PoseLandmark.RIGHT_SHOULDER]
        )
        
        val deltaX = currentShoulderCenter.x - previousShoulderCenter.x
        val deltaTime = 1f / frameRate
        
        return abs(deltaX) / deltaTime * 1000f
    }

    private fun calculateArmVelocity(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        if (previousLandmarks.isEmpty()) return 0f
        
        val currentWristCenter = calculateMidpoint(
            landmarks[PoseLandmark.LEFT_WRIST],
            landmarks[PoseLandmark.RIGHT_WRIST]
        )
        val previousWristCenter = calculateMidpoint(
            previousLandmarks.last()[PoseLandmark.LEFT_WRIST],
            previousLandmarks.last()[PoseLandmark.RIGHT_WRIST]
        )
        
        val deltaX = currentWristCenter.x - previousWristCenter.x
        val deltaY = currentWristCenter.y - previousWristCenter.y
        val deltaTime = 1f / frameRate
        
        val velocity = sqrt(deltaX * deltaX + deltaY * deltaY) / deltaTime
        return velocity * 1000f
    }

    private fun calculateClubVelocity(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        // Estimate club velocity based on wrist movement
        return calculateArmVelocity(landmarks, previousLandmarks, frameRate) * 1.5f
    }

    private fun determineVelocityPeakOrder(velocities: List<BodySegmentVelocity>): List<BodySegmentVelocity> {
        return velocities.sortedBy { it.peakTime }
    }

    private fun calculateSequenceEfficiency(peakVelocityOrder: List<BodySegmentVelocity>): Float {
        val optimalOrder = listOf(BodySegment.PELVIS, BodySegment.TORSO, BodySegment.LEAD_ARM, BodySegment.CLUB)
        val actualOrder = peakVelocityOrder.map { it.segment }
        
        var correctSequence = 0
        for (i in actualOrder.indices) {
            if (i < optimalOrder.size && actualOrder[i] == optimalOrder[i]) {
                correctSequence++
            }
        }
        
        return correctSequence.toFloat() / optimalOrder.size
    }

    private fun isOptimalKinematicSequence(peakVelocityOrder: List<BodySegmentVelocity>): Boolean {
        val optimalOrder = listOf(BodySegment.PELVIS, BodySegment.TORSO, BodySegment.LEAD_ARM, BodySegment.CLUB)
        val actualOrder = peakVelocityOrder.map { it.segment }
        return actualOrder == optimalOrder
    }

    private fun calculateSequenceGaps(peakVelocityOrder: List<BodySegmentVelocity>): List<Float> {
        val gaps = mutableListOf<Float>()
        for (i in 1 until peakVelocityOrder.size) {
            gaps.add(peakVelocityOrder[i].peakTime - peakVelocityOrder[i-1].peakTime)
        }
        return gaps
    }

    private fun calculateBodyVelocities(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Map<String, Float> {
        val velocities = mutableMapOf<String, Float>()
        
        if (previousLandmarks.isEmpty()) return velocities
        
        val keyPoints = listOf(
            "head" to PoseLandmark.NOSE,
            "shoulder" to PoseLandmark.LEFT_SHOULDER,
            "hip" to PoseLandmark.LEFT_HIP,
            "wrist" to PoseLandmark.LEFT_WRIST
        )
        
        for ((name, landmark) in keyPoints) {
            val current = landmarks[landmark]
            val previous = previousLandmarks.last()[landmark]
            
            val deltaX = current.x - previous.x
            val deltaY = current.y - previous.y
            val deltaTime = 1f / frameRate
            
            val velocity = sqrt(deltaX * deltaX + deltaY * deltaY) / deltaTime
            velocities[name] = velocity
        }
        
        return velocities
    }

    private fun calculateBodyAccelerations(
        velocities: Map<String, Float>,
        frameRate: Float
    ): Map<String, Float> {
        // This would require velocity history for proper calculation
        // For now, return zero accelerations
        return velocities.mapValues { 0f }
    }

    private fun calculateTotalPower(
        velocities: Map<String, Float>,
        accelerations: Map<String, Float>
    ): Float {
        // P = F * v, where F = m * a
        var totalPower = 0f
        for ((key, velocity) in velocities) {
            val acceleration = accelerations[key] ?: 0f
            val force = AVERAGE_BODY_MASS * acceleration
            totalPower += force * velocity
        }
        return totalPower
    }

    private fun calculatePeakPower(velocities: Map<String, Float>): Float {
        return velocities.values.maxOrNull() ?: 0f
    }

    private fun calculatePowerTransferEfficiency(velocities: Map<String, Float>): Float {
        val wristVelocity = velocities["wrist"] ?: 0f
        val totalVelocity = velocities.values.sum()
        
        return if (totalVelocity > 0) wristVelocity / totalVelocity else 0f
    }

    private fun calculateGroundForceContribution(landmarks: List<PoseLandmark>): Float {
        // Estimate ground force contribution based on leg positioning
        val leftKnee = landmarks[PoseLandmark.LEFT_KNEE]
        val rightKnee = landmarks[PoseLandmark.RIGHT_KNEE]
        val leftAnkle = landmarks[PoseLandmark.LEFT_ANKLE]
        val rightAnkle = landmarks[PoseLandmark.RIGHT_ANKLE]
        
        val leftLegAngle = calculateLegAngle(leftKnee, leftAnkle)
        val rightLegAngle = calculateLegAngle(rightKnee, rightAnkle)
        
        // More vertical leg = better ground force utilization
        val groundForceUtilization = (cos(leftLegAngle) + cos(rightLegAngle)) / 2f
        return groundForceUtilization.coerceIn(0f, 1f)
    }

    private fun calculateLegAngle(knee: PoseLandmark, ankle: PoseLandmark): Float {
        val deltaY = knee.y - ankle.y
        val deltaX = knee.x - ankle.x
        return atan2(deltaY, deltaX)
    }

    private fun calculateRotationalPower(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        if (previousLandmarks.isEmpty()) return 0f
        
        val currentShoulderAngle = calculateSegmentAngle(
            landmarks[PoseLandmark.LEFT_SHOULDER],
            landmarks[PoseLandmark.RIGHT_SHOULDER]
        )
        val previousShoulderAngle = calculateSegmentAngle(
            previousLandmarks.last()[PoseLandmark.LEFT_SHOULDER],
            previousLandmarks.last()[PoseLandmark.RIGHT_SHOULDER]
        )
        
        val angularVelocity = abs(currentShoulderAngle - previousShoulderAngle) * frameRate
        val momentOfInertia = AVERAGE_BODY_MASS * MOMENT_OF_INERTIA_FACTOR
        
        return 0.5f * momentOfInertia * angularVelocity * angularVelocity
    }

    private fun calculateLinearPower(velocities: Map<String, Float>): Float {
        val wristVelocity = velocities["wrist"] ?: 0f
        return 0.5f * AVERAGE_BODY_MASS * wristVelocity * wristVelocity
    }

    private fun calculatePowerSequence(velocities: Map<String, Float>): List<PowerPhase> {
        return velocities.map { (key, velocity) ->
            PowerPhase(
                phase = key,
                powerGenerated = velocity * 10f, // Simplified calculation
                duration = 100f // Simplified duration
            )
        }
    }

    private fun calculateWeightDistribution(
        leftAnkle: PoseLandmark,
        rightAnkle: PoseLandmark,
        leftKnee: PoseLandmark,
        rightKnee: PoseLandmark,
        leftHip: PoseLandmark,
        rightHip: PoseLandmark
    ): WeightDistribution {
        val leftLegStability = calculateLegStability(leftAnkle, leftKnee, leftHip)
        val rightLegStability = calculateLegStability(rightAnkle, rightKnee, rightHip)
        
        val totalStability = leftLegStability + rightLegStability
        val leftWeight = if (totalStability > 0) leftLegStability / totalStability else 0.5f
        val rightWeight = 1f - leftWeight
        
        return WeightDistribution(
            leftFoot = leftWeight,
            rightFoot = rightWeight,
            centerOfPressure = leftWeight - 0.5f,
            weightTransferTiming = 0f, // Would need historical data
            weightTransferEfficiency = calculateWeightTransferEfficiency(leftWeight, rightWeight)
        )
    }

    private fun calculateLegStability(ankle: PoseLandmark, knee: PoseLandmark, hip: PoseLandmark): Float {
        val kneeAnkleDistance = calculateDistance(knee, ankle)
        val hipKneeDistance = calculateDistance(hip, knee)
        
        return (kneeAnkleDistance + hipKneeDistance) / 2f
    }

    private fun calculateDistance(point1: PoseLandmark, point2: PoseLandmark): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        val dz = point1.z - point2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun calculateWeightTransferEfficiency(leftWeight: Float, rightWeight: Float): Float {
        val balance = 1f - abs(leftWeight - rightWeight)
        return balance.coerceIn(0f, 1f)
    }

    private fun calculateVerticalForce(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        if (previousLandmarks.isEmpty()) return AVERAGE_BODY_MASS * GRAVITY
        
        val currentCenterOfMass = calculateCenterOfMass(landmarks)
        val previousCenterOfMass = calculateCenterOfMass(previousLandmarks.last())
        
        val verticalVelocity = (currentCenterOfMass.y - previousCenterOfMass.y) * frameRate
        val verticalAcceleration = verticalVelocity * frameRate
        
        return AVERAGE_BODY_MASS * (GRAVITY + verticalAcceleration)
    }

    private fun calculateHorizontalForce(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): Float {
        if (previousLandmarks.isEmpty()) return 0f
        
        val currentCenterOfMass = calculateCenterOfMass(landmarks)
        val previousCenterOfMass = calculateCenterOfMass(previousLandmarks.last())
        
        val horizontalVelocity = (currentCenterOfMass.x - previousCenterOfMass.x) * frameRate
        val horizontalAcceleration = horizontalVelocity * frameRate
        
        return AVERAGE_BODY_MASS * horizontalAcceleration
    }

    private fun calculateCenterOfMass(landmarks: List<PoseLandmark>): PoseLandmark {
        val keyPoints = listOf(
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP
        )
        
        var totalX = 0f
        var totalY = 0f
        var totalZ = 0f
        
        for (pointIndex in keyPoints) {
            val point = landmarks[pointIndex]
            totalX += point.x
            totalY += point.y
            totalZ += point.z
        }
        
        val count = keyPoints.size
        return PoseLandmark.create(totalX / count, totalY / count, totalZ / count)
    }

    private fun calculateForceSequence(
        landmarks: List<PoseLandmark>,
        previousLandmarks: List<List<PoseLandmark>>,
        frameRate: Float
    ): List<ForcePhase> {
        return listOf(
            ForcePhase("setup", 100f, 500f),
            ForcePhase("backswing", 200f, 800f),
            ForcePhase("downswing", 800f, 400f),
            ForcePhase("impact", 1200f, 100f)
        )
    }

    private fun calculateGroundForceIndex(verticalForce: Float, horizontalForce: Float): Float {
        val totalForce = sqrt(verticalForce * verticalForce + horizontalForce * horizontalForce)
        val normalizedForce = totalForce / (AVERAGE_BODY_MASS * GRAVITY)
        return normalizedForce.coerceIn(0f, 3f) / 3f
    }

    private fun calculateKineticEnergy(velocities: Map<String, Float>): Float {
        var totalKineticEnergy = 0f
        for ((_, velocity) in velocities) {
            totalKineticEnergy += 0.5f * AVERAGE_BODY_MASS * velocity * velocity
        }
        return totalKineticEnergy
    }

    private fun calculatePotentialEnergy(landmarks: List<PoseLandmark>): Float {
        val centerOfMass = calculateCenterOfMass(landmarks)
        return AVERAGE_BODY_MASS * GRAVITY * centerOfMass.y
    }

    private fun calculateEnergyLoss(kineticEnergy: Float, potentialEnergy: Float): Float {
        // Simplified energy loss calculation
        return (kineticEnergy + potentialEnergy) * 0.1f
    }

    private fun calculateEnergyTransferEfficiency(energyLoss: Float, kineticEnergy: Float): Float {
        return if (kineticEnergy > 0) 1f - (energyLoss / kineticEnergy) else 0f
    }

    private fun calculateEnergySequence(velocities: Map<String, Float>): List<EnergyPhase> {
        return velocities.map { (key, velocity) ->
            EnergyPhase(
                segment = when (key) {
                    "hip" -> BodySegment.PELVIS
                    "shoulder" -> BodySegment.TORSO
                    "wrist" -> BodySegment.LEAD_ARM
                    else -> BodySegment.CLUB
                },
                energy = 0.5f * AVERAGE_BODY_MASS * velocity * velocity,
                transferRate = velocity * 10f
            )
        }
    }

    private fun calculateTemporalConsistency(historicalMetrics: List<EnhancedSwingMetrics>): Float {
        val tempos = historicalMetrics.map { it.tempo }
        return 1f - calculateStandardDeviation(tempos) / tempos.average().toFloat()
    }

    private fun calculateSpatialConsistency(historicalMetrics: List<EnhancedSwingMetrics>): Float {
        val headPositions = historicalMetrics.map { it.headPosition }
        return 1f - calculateStandardDeviation(headPositions) / 0.1f
    }

    private fun calculateKinematicConsistency(historicalMetrics: List<EnhancedSwingMetrics>): Float {
        val xFactors = historicalMetrics.map { it.xFactor }
        return 1f - calculateStandardDeviation(xFactors) / 10f
    }

    private fun calculateMetricVariations(historicalMetrics: List<EnhancedSwingMetrics>): Map<String, Float> {
        val variations = mutableMapOf<String, Float>()
        
        variations["xFactor"] = calculateStandardDeviation(historicalMetrics.map { it.xFactor })
        variations["tempo"] = calculateStandardDeviation(historicalMetrics.map { it.tempo })
        variations["balance"] = calculateStandardDeviation(historicalMetrics.map { it.balance })
        variations["swingPlane"] = calculateStandardDeviation(historicalMetrics.map { it.swingPlane })
        
        return variations
    }

    private fun calculateRepeatabilityScore(historicalMetrics: List<EnhancedSwingMetrics>): Float {
        val variations = calculateMetricVariations(historicalMetrics)
        val avgVariation = variations.values.average().toFloat()
        return 1f - (avgVariation / 10f).coerceIn(0f, 1f)
    }

    private fun calculateConsistencyTrend(historicalMetrics: List<EnhancedSwingMetrics>): ConsistencyTrend {
        if (historicalMetrics.size < 10) {
            return ConsistencyTrend(TrendDirection.STABLE, 0f, 0f, 0f)
        }
        
        val recentMetrics = historicalMetrics.takeLast(5)
        val historicalAverage = historicalMetrics.take(historicalMetrics.size - 5)
        
        val recentConsistency = calculateTemporalConsistency(recentMetrics)
        val historicalConsistency = calculateTemporalConsistency(historicalAverage)
        
        val trend = when {
            recentConsistency > historicalConsistency + 0.05f -> TrendDirection.IMPROVING
            recentConsistency < historicalConsistency - 0.05f -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
        
        val improvementRate = (recentConsistency - historicalConsistency) / 5f
        
        return ConsistencyTrend(trend, improvementRate, recentConsistency, historicalConsistency)
    }

    private fun calculatePhaseDurations(phaseHistory: List<String>, frameRate: Float): Map<String, Float> {
        val durations = mutableMapOf<String, Float>()
        var currentPhase = ""
        var currentDuration = 0
        
        for (phase in phaseHistory) {
            if (phase != currentPhase) {
                if (currentPhase.isNotEmpty()) {
                    durations[currentPhase] = currentDuration / frameRate * 1000f // Convert to milliseconds
                }
                currentPhase = phase
                currentDuration = 1
            } else {
                currentDuration++
            }
        }
        
        if (currentPhase.isNotEmpty()) {
            durations[currentPhase] = currentDuration / frameRate * 1000f
        }
        
        return durations
    }

    private fun calculateTimingEfficiency(phaseDurations: Map<String, Float>): Float {
        val backswingTime = phaseDurations["BACKSWING"] ?: 0f
        val downswingTime = phaseDurations["DOWNSWING"] ?: 0f
        val transitionTime = phaseDurations["TRANSITION"] ?: 0f
        
        // Optimal timing ratios
        val optimalBackswing = 800f // ms
        val optimalDownswing = 400f // ms
        val optimalTransition = 200f // ms
        
        val backswingEfficiency = 1f - abs(backswingTime - optimalBackswing) / optimalBackswing
        val downswingEfficiency = 1f - abs(downswingTime - optimalDownswing) / optimalDownswing
        val transitionEfficiency = 1f - abs(transitionTime - optimalTransition) / optimalTransition
        
        return (backswingEfficiency + downswingEfficiency + transitionEfficiency) / 3f
    }

    private fun calculateStandardDeviation(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance)
    }
}