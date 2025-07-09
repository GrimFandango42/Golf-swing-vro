package com.swingsync.ai.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.MediaPipeException
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.swingsync.ai.data.model.FramePoseData
import com.swingsync.ai.data.model.PoseKeypoint
import com.swingsync.ai.data.model.MediaPipePoseLandmarks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enhanced MediaPipe Manager for golf swing pose detection
 * Handles initialization, configuration, and pose detection operations
 * Optimized for golf-specific movements and real-time analysis
 */
class MediaPipeManager(private val context: Context) {

    companion object {
        private const val TAG = "MediaPipeManager"
        private const val POSE_LANDMARKER_TASK = "pose_landmarker_heavy.task"
        
        // Golf-optimized confidence thresholds
        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.7f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.7f
        private const val MIN_TRACKING_CONFIDENCE = 0.6f
        private const val NUM_POSES = 1 // Track single person
        
        // Golf-specific pose analysis constants
        private const val SWING_DETECTION_CONFIDENCE = 0.8f
        private const val BODY_ANGLE_THRESHOLD = 15.0f // degrees
        private const val FRAME_BUFFER_SIZE = 30 // frames for smoothing
    }

    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Golf-specific analysis components
    private val frameBuffer = mutableListOf<FramePoseData>()
    private val swingPhaseDetector = GolfSwingPhaseDetector()
    private val poseAnalyzer = GolfPoseAnalyzer()
    private val performanceOptimizer = PoseDetectionOptimizer()

    init {
        initializeMediaPipe()
    }

    /**
     * Initialize MediaPipe Pose Landmarker
     */
    private fun initializeMediaPipe() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Initializing MediaPipe Pose Landmarker")
                
                // Initialize Android assets
                AndroidAssetUtil.initializeNativeAssetManager(context)
                
                // Create base options
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath(POSE_LANDMARKER_TASK)
                    .build()
                
                // Create golf-optimized pose landmarker options
                val options = PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.VIDEO) // Changed to VIDEO for better tracking
                    .setNumPoses(NUM_POSES)
                    .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
                    .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
                    .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                    .setOutputSegmentationMasks(false)
                    .build()
                
                // Create pose landmarker
                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                isInitialized = true
                
                Log.d(TAG, "MediaPipe Pose Landmarker initialized successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MediaPipe", e)
                throw RuntimeException("MediaPipe initialization failed", e)
            }
        }
    }

    /**
     * Detect pose from bitmap image
     */
    suspend fun detectPose(bitmap: Bitmap): PoseLandmarkerResult? = withContext(Dispatchers.IO) {
        if (!isInitialized || poseLandmarker == null) {
            Log.w(TAG, "MediaPipe not initialized")
            return@withContext null
        }

        try {
            // Convert bitmap to MPImage
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // Detect pose
            val result = poseLandmarker?.detect(mpImage)
            
            Log.d(TAG, "Pose detection completed, landmarks: ${result?.landmarks()?.size}")
            return@withContext result
            
        } catch (e: MediaPipeException) {
            Log.e(TAG, "MediaPipe pose detection failed", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during pose detection", e)
            return@withContext null
        }
    }

    /**
     * Convert MediaPipe result to FramePoseData
     */
    fun convertResultToFramePoseData(result: PoseLandmarkerResult): FramePoseData {
        val framePoseData = mutableMapOf<String, PoseKeypoint>()
        
        try {
            // Get the first (and only) pose landmarks
            if (result.landmarks().isNotEmpty()) {
                val landmarks = result.landmarks()[0]
                
                // Map each landmark to our keypoint format
                MediaPipePoseLandmarks.ALL_LANDMARKS.forEachIndexed { index, landmarkName ->
                    if (index < landmarks.size) {
                        val landmark = landmarks[index]
                        framePoseData[landmarkName] = PoseKeypoint(
                            x = landmark.x(),
                            y = landmark.y(),
                            z = landmark.z(),
                            visibility = if (result.landmarksVisibility().isNotEmpty()) {
                                result.landmarksVisibility()[0][index]
                            } else {
                                1.0f // Default visibility
                            }
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting MediaPipe result", e)
        }
        
        return framePoseData
    }

    /**
     * Check if MediaPipe is initialized and ready
     */
    fun isReady(): Boolean = isInitialized && poseLandmarker != null

    /**
     * Get pose detection confidence for a frame
     */
    fun getPoseConfidence(framePoseData: FramePoseData): Float {
        if (framePoseData.isEmpty()) return 0f
        
        // Calculate average visibility/confidence
        val visibilityValues = framePoseData.values.mapNotNull { it.visibility }
        return if (visibilityValues.isNotEmpty()) {
            visibilityValues.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Filter keypoints by confidence threshold
     */
    fun filterKeypointsByConfidence(
        framePoseData: FramePoseData,
        minConfidence: Float = MIN_POSE_DETECTION_CONFIDENCE
    ): FramePoseData {
        return framePoseData.filterValues { keypoint ->
            (keypoint.visibility ?: 0f) >= minConfidence
        }
    }

    /**
     * Get key body joints for golf swing analysis
     */
    fun getKeyGolfJoints(framePoseData: FramePoseData): Map<String, PoseKeypoint> {
        val keyJoints = listOf(
            MediaPipePoseLandmarks.NOSE,
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_ELBOW,
            MediaPipePoseLandmarks.RIGHT_ELBOW,
            MediaPipePoseLandmarks.LEFT_WRIST,
            MediaPipePoseLandmarks.RIGHT_WRIST,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP,
            MediaPipePoseLandmarks.LEFT_KNEE,
            MediaPipePoseLandmarks.RIGHT_KNEE,
            MediaPipePoseLandmarks.LEFT_ANKLE,
            MediaPipePoseLandmarks.RIGHT_ANKLE
        )
        
        return framePoseData.filterKeys { it in keyJoints }
    }

    /**
     * Release MediaPipe resources
     */
    fun release() {
        try {
            poseLandmarker?.close()
            poseLandmarker = null
            isInitialized = false
            Log.d(TAG, "MediaPipe resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPipe resources", e)
        }
    }
}