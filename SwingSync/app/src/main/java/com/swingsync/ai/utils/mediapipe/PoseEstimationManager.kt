package com.swingsync.ai.utils.mediapipe

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoseEstimationManager @Inject constructor(
    private val context: Context
) {
    private var poseLandmarker: PoseLandmarker? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private val _poseResults = MutableSharedFlow<PoseEstimationResult>()
    val poseResults: SharedFlow<PoseEstimationResult> = _poseResults.asSharedFlow()
    
    private val _estimationState = MutableSharedFlow<EstimationState>()
    val estimationState: SharedFlow<EstimationState> = _estimationState.asSharedFlow()
    
    fun initialize(): Result<Unit> {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker.task")
                .build()
            
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener(this::onPoseDetectionResult)
                .setErrorListener(this::onPoseDetectionError)
                .build()
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            _estimationState.tryEmit(EstimationState.INITIALIZED)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing pose estimation")
            _estimationState.tryEmit(EstimationState.ERROR)
            Result.failure(e)
        }
    }
    
    fun processFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            processFrame(bitmap, imageProxy.imageInfo.timestamp)
        }
        imageProxy.close()
    }
    
    fun processFrame(bitmap: Bitmap, timestamp: Long) {
        backgroundExecutor.execute {
            try {
                val mpImage = BitmapImageBuilder(bitmap).build()
                poseLandmarker?.detectAsync(mpImage, timestamp)
            } catch (e: Exception) {
                Timber.e(e, "Error processing frame")
                _estimationState.tryEmit(EstimationState.ERROR)
            }
        }
    }
    
    private fun onPoseDetectionResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        if (result.landmarks().isNotEmpty()) {
            val landmarks = result.landmarks()[0]
            val worldLandmarks = result.worldLandmarks().getOrNull(0)
            
            val poseResult = PoseEstimationResult(
                landmarks = landmarks.map { landmark ->
                    PoseLandmarkData(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z().orElse(0f),
                        visibility = landmark.visibility().orElse(0f)
                    )
                },
                worldLandmarks = worldLandmarks?.map { landmark ->
                    PoseLandmarkData(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        visibility = landmark.visibility().orElse(0f)
                    )
                } ?: emptyList(),
                timestamp = System.currentTimeMillis(),
                confidence = calculateOverallConfidence(landmarks)
            )
            
            _poseResults.tryEmit(poseResult)
        }
    }
    
    private fun onPoseDetectionError(error: RuntimeException) {
        Timber.e(error, "Pose detection error")
        _estimationState.tryEmit(EstimationState.ERROR)
    }
    
    private fun calculateOverallConfidence(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val visibilityScores = landmarks.mapNotNull { it.visibility().orElse(null) }
        return if (visibilityScores.isNotEmpty()) {
            visibilityScores.average().toFloat()
        } else {
            0f
        }
    }
    
    fun analyzeSwingPhase(poseResults: List<PoseEstimationResult>): SwingPhaseAnalysis {
        if (poseResults.isEmpty()) {
            return SwingPhaseAnalysis(
                phase = SwingPhase.UNKNOWN,
                confidence = 0f,
                keyPoints = emptyMap()
            )
        }
        
        val latestPose = poseResults.last()
        val phase = detectSwingPhase(latestPose)
        
        return SwingPhaseAnalysis(
            phase = phase,
            confidence = latestPose.confidence,
            keyPoints = extractKeyPoints(latestPose),
            analysis = generatePhaseAnalysis(phase, latestPose)
        )
    }
    
    private fun detectSwingPhase(poseResult: PoseEstimationResult): SwingPhase {
        val landmarks = poseResult.landmarks
        if (landmarks.size < 33) return SwingPhase.UNKNOWN
        
        // Key landmarks for golf swing analysis
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftElbow = landmarks[13]
        val rightElbow = landmarks[14]
        val leftWrist = landmarks[15]
        val rightWrist = landmarks[16]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        
        // Analyze arm positions and body rotation
        val armAngle = calculateArmAngle(leftShoulder, rightShoulder, leftElbow, rightElbow)
        val bodyRotation = calculateBodyRotation(leftShoulder, rightShoulder, leftHip, rightHip)
        val wristPosition = calculateWristPosition(leftWrist, rightWrist)
        
        return when {
            armAngle < -30 && bodyRotation > 20 -> SwingPhase.BACKSWING
            armAngle > 30 && wristPosition < 0.3 -> SwingPhase.DOWNSWING
            abs(armAngle) < 15 && wristPosition < 0.2 -> SwingPhase.IMPACT
            armAngle > 45 && bodyRotation < -20 -> SwingPhase.FOLLOW_THROUGH
            else -> SwingPhase.ADDRESS
        }
    }
    
    private fun calculateArmAngle(
        leftShoulder: PoseLandmarkData,
        rightShoulder: PoseLandmarkData,
        leftElbow: PoseLandmarkData,
        rightElbow: PoseLandmarkData
    ): Float {
        val shoulderCenter = (leftShoulder.x + rightShoulder.x) / 2
        val elbowCenter = (leftElbow.x + rightElbow.x) / 2
        return Math.toDegrees(Math.atan2(
            (elbowCenter - shoulderCenter).toDouble(),
            (leftElbow.y - leftShoulder.y).toDouble()
        )).toFloat()
    }
    
    private fun calculateBodyRotation(
        leftShoulder: PoseLandmarkData,
        rightShoulder: PoseLandmarkData,
        leftHip: PoseLandmarkData,
        rightHip: PoseLandmarkData
    ): Float {
        val shoulderAngle = Math.toDegrees(Math.atan2(
            (rightShoulder.y - leftShoulder.y).toDouble(),
            (rightShoulder.x - leftShoulder.x).toDouble()
        ))
        val hipAngle = Math.toDegrees(Math.atan2(
            (rightHip.y - leftHip.y).toDouble(),
            (rightHip.x - leftHip.x).toDouble()
        ))
        return (shoulderAngle - hipAngle).toFloat()
    }
    
    private fun calculateWristPosition(leftWrist: PoseLandmarkData, rightWrist: PoseLandmarkData): Float {
        return (leftWrist.y + rightWrist.y) / 2
    }
    
    private fun extractKeyPoints(poseResult: PoseEstimationResult): Map<String, PoseLandmarkData> {
        val landmarks = poseResult.landmarks
        if (landmarks.size < 33) return emptyMap()
        
        return mapOf(
            "left_shoulder" to landmarks[11],
            "right_shoulder" to landmarks[12],
            "left_elbow" to landmarks[13],
            "right_elbow" to landmarks[14],
            "left_wrist" to landmarks[15],
            "right_wrist" to landmarks[16],
            "left_hip" to landmarks[23],
            "right_hip" to landmarks[24],
            "left_knee" to landmarks[25],
            "right_knee" to landmarks[26],
            "left_ankle" to landmarks[27],
            "right_ankle" to landmarks[28]
        )
    }
    
    private fun generatePhaseAnalysis(phase: SwingPhase, poseResult: PoseEstimationResult): String {
        return when (phase) {
            SwingPhase.ADDRESS -> "Good setup position. Keep your stance balanced."
            SwingPhase.BACKSWING -> "Rotate your shoulders while keeping your lower body stable."
            SwingPhase.DOWNSWING -> "Maintain your spine angle and lead with your hips."
            SwingPhase.IMPACT -> "Keep your head steady and extend through the ball."
            SwingPhase.FOLLOW_THROUGH -> "Complete your rotation with good balance."
            SwingPhase.UNKNOWN -> "Position not recognized. Please ensure you're in the camera view."
        }
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )
            
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
            val imageBytes = out.toByteArray()
            
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Timber.e(e, "Error converting ImageProxy to Bitmap")
            null
        }
    }
    
    fun release() {
        backgroundExecutor.shutdown()
        poseLandmarker?.close()
    }
    
    private fun abs(value: Float): Float = kotlin.math.abs(value)
}

data class PoseEstimationResult(
    val landmarks: List<PoseLandmarkData>,
    val worldLandmarks: List<PoseLandmarkData>,
    val timestamp: Long,
    val confidence: Float
)

data class PoseLandmarkData(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
)

data class SwingPhaseAnalysis(
    val phase: SwingPhase,
    val confidence: Float,
    val keyPoints: Map<String, PoseLandmarkData>,
    val analysis: String = ""
)

enum class SwingPhase {
    ADDRESS,
    BACKSWING,
    DOWNSWING,
    IMPACT,
    FOLLOW_THROUGH,
    UNKNOWN
}

enum class EstimationState {
    IDLE,
    INITIALIZED,
    PROCESSING,
    ERROR
}