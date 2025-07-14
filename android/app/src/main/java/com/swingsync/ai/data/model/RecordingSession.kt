package com.swingsync.ai.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing a golf swing recording session
 */
@Parcelize
data class RecordingSession(
    val sessionId: String,
    val userId: String,
    val clubUsed: String,
    val startTime: Long,
    val endTime: Long? = null,
    val fps: Float = 30f,
    val totalFrames: Int = 0,
    val isRecording: Boolean = false,
    val poseDetectionResults: List<PoseDetectionResult> = emptyList(),
    val analysisResults: SwingAnalysisResult? = null
) : Parcelable

/**
 * Data model for pose detection results
 */
@Parcelize
data class PoseDetectionResult(
    val timestamp: Long,
    val confidence: Float,
    val keypoints: List<PoseKeypoint>
) : Parcelable

/**
 * Data model for individual pose keypoints
 */
@Parcelize
data class PoseKeypoint(
    val type: KeypointType,
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val confidence: Float
) : Parcelable

/**
 * Enum defining different pose keypoint types
 */
enum class KeypointType {
    NOSE,
    LEFT_EYE,
    RIGHT_EYE,
    LEFT_EAR,
    RIGHT_EAR,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_ELBOW,
    RIGHT_ELBOW,
    LEFT_WRIST,
    RIGHT_WRIST,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_ANKLE,
    RIGHT_ANKLE
}

/**
 * Data model for swing analysis results
 */
@Parcelize
data class SwingAnalysisResult(
    val sessionId: String,
    val overallScore: Float,
    val swingPlaneAngle: Float,
    val tempo: Float,
    val balance: Float,
    val clubheadSpeed: Float,
    val feedback: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
) : Parcelable