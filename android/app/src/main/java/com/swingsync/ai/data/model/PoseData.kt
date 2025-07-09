package com.swingsync.ai.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data classes that mirror the backend Python data structures
 * These are used for communication between the Android app and SwingSync AI backend
 */

@Parcelize
data class PoseKeypoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float? = null
) : Parcelable

/**
 * Represents pose data for a single frame
 * Key: keypoint name (e.g., "nose", "left_shoulder", "right_elbow")
 * Value: PoseKeypoint with 3D coordinates and confidence
 */
typealias FramePoseData = Map<String, PoseKeypoint>

@Parcelize
data class PSystemPhase(
    val phaseName: String, // e.g., "P1", "P2", ..., "P10"
    val startFrameIndex: Int,
    val endFrameIndex: Int
) : Parcelable

@Parcelize
data class SwingVideoAnalysisInput(
    val sessionId: String,
    val userId: String,
    val clubUsed: String, // e.g., "Driver", "7-Iron", "Putter"
    val frames: List<FramePoseData>,
    val pSystemClassification: List<PSystemPhase>,
    val videoFps: Float
) : Parcelable

@Parcelize
data class BiomechanicalKPI(
    val pPosition: String, // P-System position (e.g., "P1", "P4")
    val kpiName: String, // Name of the KPI (e.g., "Hip Hinge Angle")
    val value: String, // Calculated value as string (can be any type)
    val unit: String, // Unit (e.g., "degrees", "radians", "meters")
    val idealRange: Pair<Float, Float>? = null, // Optional ideal range
    val notes: String? = null // Optional notes
) : Parcelable

@Parcelize
data class KPIDeviation(
    val kpiName: String,
    val observedValue: String,
    val idealValueOrRange: String,
    val pPosition: String
) : Parcelable

@Parcelize
data class DetectedFault(
    val faultId: String, // Unique identifier (e.g., "F001", "OVER_THE_TOP")
    val faultName: String, // User-friendly name
    val pPositionsImplicated: List<String>, // P-System phases where fault is evident
    val description: String, // General description of the fault
    val kpiDeviations: List<KPIDeviation>, // KPI measurements that led to this diagnosis
    val llmPromptTemplateKey: String, // Key for LLM prompt template
    val severity: Float? = null // Severity score 0.0 to 1.0
) : Parcelable

@Parcelize
data class LLMGeneratedTip(
    val explanation: String,
    val tip: String,
    val drillSuggestion: String? = null
) : Parcelable

@Parcelize
data class SwingAnalysisFeedback(
    val sessionId: String,
    val summaryOfFindings: String, // Overall summary
    val detailedFeedback: List<LLMGeneratedTip>, // Tips for each major fault
    val rawDetectedFaults: List<DetectedFault>, // Raw detected faults for debugging
    val visualisationAnnotations: List<Map<String, Any>>? = null // Optional visualization hints
) : Parcelable

/**
 * Real-time pose detection result
 * Used internally by the app during live pose detection
 */
@Parcelize
data class PoseDetectionResult(
    val keypoints: FramePoseData,
    val confidence: Float,
    val timestamp: Long,
    val frameIndex: Int
) : Parcelable

/**
 * Recording session data
 * Tracks the current recording session state
 */
@Parcelize
data class RecordingSession(
    val sessionId: String,
    val userId: String,
    val clubUsed: String,
    val startTime: Long,
    val endTime: Long? = null,
    val fps: Float,
    val totalFrames: Int = 0,
    val poseDetectionResults: List<PoseDetectionResult> = emptyList(),
    val isRecording: Boolean = false
) : Parcelable

/**
 * Golf swing phases for P-System classification
 */
enum class GolfSwingPhase(val displayName: String) {
    P1("Address"),
    P2("Takeaway"),
    P3("Backswing"),
    P4("Top of Backswing"),
    P5("Transition"),
    P6("Downswing"),
    P7("Impact"),
    P8("Release"),
    P9("Follow Through"),
    P10("Finish");
    
    companion object {
        fun fromPhaseName(phaseName: String): GolfSwingPhase? {
            return values().find { it.name == phaseName }
        }
    }
}

/**
 * MediaPipe pose landmark names
 * These correspond to the 33 keypoints detected by MediaPipe Pose
 */
object MediaPipePoseLandmarks {
    const val NOSE = "nose"
    const val LEFT_EYE_INNER = "left_eye_inner"
    const val LEFT_EYE = "left_eye"
    const val LEFT_EYE_OUTER = "left_eye_outer"
    const val RIGHT_EYE_INNER = "right_eye_inner"
    const val RIGHT_EYE = "right_eye"
    const val RIGHT_EYE_OUTER = "right_eye_outer"
    const val LEFT_EAR = "left_ear"
    const val RIGHT_EAR = "right_ear"
    const val MOUTH_LEFT = "mouth_left"
    const val MOUTH_RIGHT = "mouth_right"
    const val LEFT_SHOULDER = "left_shoulder"
    const val RIGHT_SHOULDER = "right_shoulder"
    const val LEFT_ELBOW = "left_elbow"
    const val RIGHT_ELBOW = "right_elbow"
    const val LEFT_WRIST = "left_wrist"
    const val RIGHT_WRIST = "right_wrist"
    const val LEFT_PINKY = "left_pinky"
    const val RIGHT_PINKY = "right_pinky"
    const val LEFT_INDEX = "left_index"
    const val RIGHT_INDEX = "right_index"
    const val LEFT_THUMB = "left_thumb"
    const val RIGHT_THUMB = "right_thumb"
    const val LEFT_HIP = "left_hip"
    const val RIGHT_HIP = "right_hip"
    const val LEFT_KNEE = "left_knee"
    const val RIGHT_KNEE = "right_knee"
    const val LEFT_ANKLE = "left_ankle"
    const val RIGHT_ANKLE = "right_ankle"
    const val LEFT_HEEL = "left_heel"
    const val RIGHT_HEEL = "right_heel"
    const val LEFT_FOOT_INDEX = "left_foot_index"
    const val RIGHT_FOOT_INDEX = "right_foot_index"
    
    val ALL_LANDMARKS = listOf(
        NOSE, LEFT_EYE_INNER, LEFT_EYE, LEFT_EYE_OUTER, RIGHT_EYE_INNER, RIGHT_EYE, RIGHT_EYE_OUTER,
        LEFT_EAR, RIGHT_EAR, MOUTH_LEFT, MOUTH_RIGHT, LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW,
        RIGHT_ELBOW, LEFT_WRIST, RIGHT_WRIST, LEFT_PINKY, RIGHT_PINKY, LEFT_INDEX, RIGHT_INDEX,
        LEFT_THUMB, RIGHT_THUMB, LEFT_HIP, RIGHT_HIP, LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE,
        LEFT_HEEL, RIGHT_HEEL, LEFT_FOOT_INDEX, RIGHT_FOOT_INDEX
    )
}