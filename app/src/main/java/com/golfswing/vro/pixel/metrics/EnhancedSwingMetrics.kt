package com.golfswing.vro.pixel.metrics

/**
 * Enhanced swing metrics data class for comprehensive golf swing analysis
 */
data class EnhancedSwingMetrics(
    // Core biomechanical metrics
    val xFactor: Float,
    val xFactorStretch: Float,
    val kinematicSequence: KinematicSequence,
    val powerMetrics: PowerMetrics,
    
    // Traditional swing metrics
    val shoulderAngle: Float,
    val hipAngle: Float,
    val kneeFlexion: Float,
    val armExtension: Float,
    val headPosition: Float,
    val weightDistribution: Float,
    val clubPlane: Float,
    val tempo: Float,
    val balance: Float,
    
    // Professional golf metrics
    val attackAngle: Float,
    val swingPlane: Float,
    val clubPath: Float,
    val faceAngle: Float,
    val dynamicLoft: Float,
    
    // Advanced biomechanical analysis
    val groundForce: GroundForce,
    val energyTransfer: EnergyTransfer,
    val swingConsistency: SwingConsistency,
    val swingTiming: SwingTiming,
    val professionalComparison: ProfessionalComparison,
    
    // Metadata
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Professional comparison data class
 */
data class ProfessionalComparison(
    val overallScore: Float,
    val xFactorScore: Float,
    val kinematicScore: Float,
    val powerScore: Float,
    val consistencyScore: Float,
    val benchmarkCategory: SkillLevel,
    val improvementPotential: Float,
    val tourAverageComparison: Map<String, Float>
)

/**
 * Skill level enumeration
 */
enum class SkillLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    SCRATCH,
    PROFESSIONAL,
    TOUR_LEVEL
}