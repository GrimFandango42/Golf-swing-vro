package com.swingsync.ai.ui.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.swingsync.ai.SwingSyncApplication
import com.swingsync.ai.data.model.FramePoseData
import com.swingsync.ai.data.model.PoseDetectionResult
import com.swingsync.ai.data.model.PoseKeypoint
import com.swingsync.ai.data.model.MediaPipePoseLandmarks
import com.swingsync.ai.utils.ImageUtils
import java.nio.ByteBuffer

/**
 * Analyzer for real-time pose detection using MediaPipe
 * Processes camera frames and extracts pose keypoints
 */
class PoseAnalyzer(
    private val onPoseDetected: (PoseDetectionResult) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
    }

    private var frameIndex = 0
    private var lastAnalysisTime = 0L
    private val mediaPipeManager = SwingSyncApplication.instance.mediaPipeManager

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(image)
            
            // Run pose detection asynchronously
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Detect pose using MediaPipe
                    val poseResult = mediaPipeManager.detectPose(bitmap)
                    
                    poseResult?.let { result ->
                        // Convert MediaPipe result to our data format
                        val framePoseData = mediaPipeManager.convertResultToFramePoseData(result)
                        
                        // Calculate overall confidence
                        val avgConfidence = mediaPipeManager.getPoseConfidence(framePoseData)
                        
                        // Create pose detection result
                        val poseDetectionResult = PoseDetectionResult(
                            keypoints = framePoseData,
                            confidence = avgConfidence,
                            timestamp = currentTime,
                            frameIndex = frameIndex++
                        )
                        
                        // Callback with result on main thread
                        kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onPoseDetected(poseDetectionResult)
                        }
                        
                        Log.d(TAG, "Pose detected with confidence: $avgConfidence, keypoints: ${framePoseData.size}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pose detection coroutine", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
        } finally {
            image.close()
        }
        
        lastAnalysisTime = currentTime
    }

    /**
     * Convert ImageProxy to Bitmap for MediaPipe processing
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        // Convert YUV420 to RGB bitmap
        val bitmap = ImageUtils.yuv420ToBitmap(
            bytes,
            image.width,
            image.height,
            image.planes[0].rowStride
        )
        
        // Rotate bitmap to match display orientation
        val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
        
        return rotatedBitmap
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Convert MediaPipe pose result to our FramePoseData format
     */
    private fun convertMediaPipeResultToFramePoseData(result: Any): FramePoseData {
        val framePoseData = mutableMapOf<String, PoseKeypoint>()
        
        try {
            // This is a simplified conversion - actual implementation depends on MediaPipe API
            // MediaPipe returns pose landmarks as a list of normalized coordinates
            val landmarks = extractLandmarksFromResult(result)
            
            MediaPipePoseLandmarks.ALL_LANDMARKS.forEachIndexed { index, landmarkName ->
                if (index < landmarks.size) {
                    val landmark = landmarks[index]
                    framePoseData[landmarkName] = PoseKeypoint(
                        x = landmark.x,
                        y = landmark.y,
                        z = landmark.z,
                        visibility = landmark.visibility
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting MediaPipe result", e)
        }
        
        return framePoseData
    }

    /**
     * Extract landmarks from MediaPipe result
     * This is a placeholder - actual implementation depends on MediaPipe API
     */
    private fun extractLandmarksFromResult(result: Any): List<MediaPipeLandmark> {
        // TODO: Implement actual MediaPipe result parsing
        // This is a mock implementation
        return emptyList()
    }

    /**
     * Calculate average confidence across all keypoints
     */
    private fun calculateAverageConfidence(framePoseData: FramePoseData): Float {
        if (framePoseData.isEmpty()) return 0f
        
        val confidences = framePoseData.values.mapNotNull { it.visibility }
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Check if pose has minimum required confidence
     */
    private fun hasMinimumConfidence(framePoseData: FramePoseData): Boolean {
        val avgConfidence = calculateAverageConfidence(framePoseData)
        return avgConfidence >= MIN_CONFIDENCE_THRESHOLD
    }

    /**
     * Data class for MediaPipe landmark
     */
    private data class MediaPipeLandmark(
        val x: Float,
        val y: Float,
        val z: Float,
        val visibility: Float
    )
}