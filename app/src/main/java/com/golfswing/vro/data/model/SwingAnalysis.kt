package com.golfswing.vro.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
@Parcelize
data class SwingAnalysis(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val videoPath: String,
    val duration: Long,
    val phases: List<SwingPhase>,
    val biomechanics: BiomechanicsData,
    val score: SwingScore,
    val recommendations: List<String>,
    val isProcessed: Boolean = false
) : Parcelable

@Serializable
@Parcelize
data class SwingPhase(
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val keyPoints: List<KeyPoint>,
    val analysis: String
) : Parcelable

@Serializable
@Parcelize
data class KeyPoint(
    val x: Float,
    val y: Float,
    val z: Float? = null,
    val confidence: Float,
    val timestamp: Long
) : Parcelable

@Serializable
@Parcelize
data class BiomechanicsData(
    val clubheadSpeed: Float,
    val ballSpeed: Float,
    val launchAngle: Float,
    val spinRate: Float,
    val tempo: Float,
    val rhythm: Float,
    val balance: Float,
    val rotation: RotationData,
    val posture: PostureData
) : Parcelable

@Serializable
@Parcelize
data class RotationData(
    val shoulderTurn: Float,
    val hipTurn: Float,
    val xFactor: Float,
    val sequencing: Float
) : Parcelable

@Serializable
@Parcelize
data class PostureData(
    val spineAngle: Float,
    val kneeFlexion: Float,
    val weightShift: Float,
    val headMovement: Float
) : Parcelable

@Serializable
@Parcelize
data class SwingScore(
    val overall: Float,
    val setup: Float,
    val backswing: Float,
    val downswing: Float,
    val impact: Float,
    val followThrough: Float,
    val breakdown: ScoreBreakdown
) : Parcelable

@Serializable
@Parcelize
data class ScoreBreakdown(
    val tempo: Float,
    val balance: Float,
    val clubpath: Float,
    val faceAngle: Float,
    val sequencing: Float
) : Parcelable

enum class SwingPhaseType {
    SETUP,
    TAKEAWAY,
    BACKSWING,
    TRANSITION,
    DOWNSWING,
    IMPACT,
    FOLLOW_THROUGH,
    FINISH
}

enum class AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}