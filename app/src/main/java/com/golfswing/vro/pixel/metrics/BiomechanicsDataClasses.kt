package com.golfswing.vro.pixel.metrics

/**
 * Data classes for biomechanical calculations and metrics
 */

// Kinematic Sequence Data Classes
data class KinematicSequence(
    val sequenceOrder: List<BodySegment>,
    val peakVelocityOrder: List<BodySegmentVelocity>,
    val sequenceEfficiency: Float,
    val isOptimalSequence: Boolean,
    val sequenceGaps: List<Float>
)

data class BodySegmentVelocity(
    val segment: BodySegment,
    val velocity: Float,
    val peakTime: Float,
    val velocityProfile: List<Float>
)

enum class BodySegment {
    PELVIS,
    TORSO,
    LEAD_ARM,
    TRAIL_ARM,
    CLUB
}

// Power Metrics Data Classes
data class PowerMetrics(
    val totalPower: Float,
    val peakPower: Float,
    val powerTransferEfficiency: Float,
    val groundForceContribution: Float,
    val rotationalPower: Float,
    val linearPower: Float,
    val powerSequence: List<PowerPhase>
)

data class PowerPhase(
    val phase: String,
    val powerGenerated: Float,
    val duration: Float
)

// Ground Force Data Classes
data class GroundForce(
    val verticalForce: Float,
    val horizontalForce: Float,
    val forceDistribution: WeightDistribution,
    val forceSequence: List<ForcePhase>,
    val groundForceIndex: Float
)

data class WeightDistribution(
    val leftFoot: Float,
    val rightFoot: Float,
    val centerOfPressure: Float,
    val weightTransferTiming: Float,
    val weightTransferEfficiency: Float
)

data class ForcePhase(
    val phase: String,
    val force: Float,
    val duration: Float
)

// Energy Transfer Data Classes
data class EnergyTransfer(
    val kineticEnergy: Float,
    val potentialEnergy: Float,
    val energyLoss: Float,
    val transferEfficiency: Float,
    val energySequence: List<EnergyPhase>
)

data class EnergyPhase(
    val segment: BodySegment,
    val energy: Float,
    val transferRate: Float
)

// Swing Consistency Data Classes
data class SwingConsistency(
    val overallConsistency: Float,
    val temporalConsistency: Float,
    val spatialConsistency: Float,
    val kinematicConsistency: Float,
    val metricVariations: Map<String, Float>,
    val repeatabilityScore: Float,
    val consistencyTrend: ConsistencyTrend
)

data class ConsistencyTrend(
    val direction: TrendDirection,
    val improvementRate: Float,
    val recentConsistency: Float,
    val historicalConsistency: Float
)

enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
}

// Swing Timing Data Classes
data class SwingTiming(
    val totalSwingTime: Float,
    val backswingTime: Float,
    val downswingTime: Float,
    val transitionTime: Float,
    val tempoRatio: Float,
    val timingEfficiency: Float,
    val phaseDurations: Map<String, Float>
)

// Enhanced Swing Metrics Data Class
data class EnhancedSwingMetrics(
    val xFactor: Float,
    val tempo: Float,
    val balance: Float,
    val swingPlane: Float,
    val clubPath: Float,
    val attackAngle: Float,
    val headPosition: Float,
    val shoulderTilt: Float,
    val hipSlide: Float,
    val weightTransfer: Float,
    val sequenceScore: Float,
    val powerGeneration: Float,
    val consistency: Float,
    val efficiency: Float,
    val timestamp: Long
)