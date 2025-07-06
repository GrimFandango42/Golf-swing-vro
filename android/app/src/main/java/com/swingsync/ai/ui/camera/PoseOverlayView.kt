package com.swingsync.ai.ui.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.swingsync.ai.R
import com.swingsync.ai.data.model.FramePoseData
import com.swingsync.ai.data.model.PoseKeypoint
import com.swingsync.ai.data.model.MediaPipePoseLandmarks

/**
 * Custom view for overlaying pose detection results on camera preview
 * Renders pose skeleton and keypoints in real-time
 */
class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val KEYPOINT_RADIUS = 8f
        private const val SKELETON_STROKE_WIDTH = 4f
        private const val CONFIDENCE_THRESHOLD = 0.5f
    }

    private val skeletonPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_skeleton_color)
        strokeWidth = SKELETON_STROKE_WIDTH
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val keypointPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_joint_color)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val lowConfidencePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_confidence_low)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val mediumConfidencePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_confidence_medium)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val highConfidencePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_confidence_high)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var currentPoseKeypoints: FramePoseData? = null
    private var viewWidth = 0
    private var viewHeight = 0

    // Define skeleton connections for drawing lines between keypoints
    private val skeletonConnections = listOf(
        // Face
        Pair(MediaPipePoseLandmarks.LEFT_EYE, MediaPipePoseLandmarks.NOSE),
        Pair(MediaPipePoseLandmarks.RIGHT_EYE, MediaPipePoseLandmarks.NOSE),
        Pair(MediaPipePoseLandmarks.LEFT_EYE, MediaPipePoseLandmarks.LEFT_EAR),
        Pair(MediaPipePoseLandmarks.RIGHT_EYE, MediaPipePoseLandmarks.RIGHT_EAR),
        
        // Upper body
        Pair(MediaPipePoseLandmarks.LEFT_SHOULDER, MediaPipePoseLandmarks.RIGHT_SHOULDER),
        Pair(MediaPipePoseLandmarks.LEFT_SHOULDER, MediaPipePoseLandmarks.LEFT_ELBOW),
        Pair(MediaPipePoseLandmarks.RIGHT_SHOULDER, MediaPipePoseLandmarks.RIGHT_ELBOW),
        Pair(MediaPipePoseLandmarks.LEFT_ELBOW, MediaPipePoseLandmarks.LEFT_WRIST),
        Pair(MediaPipePoseLandmarks.RIGHT_ELBOW, MediaPipePoseLandmarks.RIGHT_WRIST),
        
        // Torso
        Pair(MediaPipePoseLandmarks.LEFT_SHOULDER, MediaPipePoseLandmarks.LEFT_HIP),
        Pair(MediaPipePoseLandmarks.RIGHT_SHOULDER, MediaPipePoseLandmarks.RIGHT_HIP),
        Pair(MediaPipePoseLandmarks.LEFT_HIP, MediaPipePoseLandmarks.RIGHT_HIP),
        
        // Lower body
        Pair(MediaPipePoseLandmarks.LEFT_HIP, MediaPipePoseLandmarks.LEFT_KNEE),
        Pair(MediaPipePoseLandmarks.RIGHT_HIP, MediaPipePoseLandmarks.RIGHT_KNEE),
        Pair(MediaPipePoseLandmarks.LEFT_KNEE, MediaPipePoseLandmarks.LEFT_ANKLE),
        Pair(MediaPipePoseLandmarks.RIGHT_KNEE, MediaPipePoseLandmarks.RIGHT_ANKLE),
        
        // Hands
        Pair(MediaPipePoseLandmarks.LEFT_WRIST, MediaPipePoseLandmarks.LEFT_PINKY),
        Pair(MediaPipePoseLandmarks.LEFT_WRIST, MediaPipePoseLandmarks.LEFT_INDEX),
        Pair(MediaPipePoseLandmarks.LEFT_WRIST, MediaPipePoseLandmarks.LEFT_THUMB),
        Pair(MediaPipePoseLandmarks.RIGHT_WRIST, MediaPipePoseLandmarks.RIGHT_PINKY),
        Pair(MediaPipePoseLandmarks.RIGHT_WRIST, MediaPipePoseLandmarks.RIGHT_INDEX),
        Pair(MediaPipePoseLandmarks.RIGHT_WRIST, MediaPipePoseLandmarks.RIGHT_THUMB),
        
        // Feet
        Pair(MediaPipePoseLandmarks.LEFT_ANKLE, MediaPipePoseLandmarks.LEFT_HEEL),
        Pair(MediaPipePoseLandmarks.LEFT_ANKLE, MediaPipePoseLandmarks.LEFT_FOOT_INDEX),
        Pair(MediaPipePoseLandmarks.RIGHT_ANKLE, MediaPipePoseLandmarks.RIGHT_HEEL),
        Pair(MediaPipePoseLandmarks.RIGHT_ANKLE, MediaPipePoseLandmarks.RIGHT_FOOT_INDEX)
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        currentPoseKeypoints?.let { keypoints ->
            // Draw skeleton connections
            drawSkeleton(canvas, keypoints)
            
            // Draw keypoints
            drawKeypoints(canvas, keypoints)
        }
    }

    /**
     * Update pose keypoints and trigger redraw
     */
    fun updatePoseKeypoints(keypoints: FramePoseData) {
        currentPoseKeypoints = keypoints
        invalidate() // Trigger onDraw
    }

    /**
     * Clear pose overlay
     */
    fun clearPose() {
        currentPoseKeypoints = null
        invalidate()
    }

    /**
     * Draw skeleton connections between keypoints
     */
    private fun drawSkeleton(canvas: Canvas, keypoints: FramePoseData) {
        skeletonConnections.forEach { connection ->
            val startKeypoint = keypoints[connection.first]
            val endKeypoint = keypoints[connection.second]
            
            if (startKeypoint != null && endKeypoint != null) {
                val startConfidence = startKeypoint.visibility ?: 0f
                val endConfidence = endKeypoint.visibility ?: 0f
                
                // Only draw if both keypoints have sufficient confidence
                if (startConfidence >= CONFIDENCE_THRESHOLD && endConfidence >= CONFIDENCE_THRESHOLD) {
                    val startPoint = normalizedToViewCoordinates(startKeypoint)
                    val endPoint = normalizedToViewCoordinates(endKeypoint)
                    
                    // Adjust paint alpha based on confidence
                    val avgConfidence = (startConfidence + endConfidence) / 2f
                    skeletonPaint.alpha = (avgConfidence * 255).toInt()
                    
                    canvas.drawLine(
                        startPoint.x, startPoint.y,
                        endPoint.x, endPoint.y,
                        skeletonPaint
                    )
                }
            }
        }
    }

    /**
     * Draw individual keypoints
     */
    private fun drawKeypoints(canvas: Canvas, keypoints: FramePoseData) {
        keypoints.forEach { (name, keypoint) ->
            val confidence = keypoint.visibility ?: 0f
            
            if (confidence >= CONFIDENCE_THRESHOLD) {
                val point = normalizedToViewCoordinates(keypoint)
                val paint = getPaintForConfidence(confidence)
                
                // Draw keypoint circle
                canvas.drawCircle(point.x, point.y, KEYPOINT_RADIUS, paint)
                
                // Draw keypoint name for debugging (optional)
                if (isImportantKeypoint(name)) {
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 24f
                        isAntiAlias = true
                    }
                    canvas.drawText(
                        name.substringAfterLast("_").uppercase(),
                        point.x + KEYPOINT_RADIUS + 4,
                        point.y,
                        textPaint
                    )
                }
            }
        }
    }

    /**
     * Convert normalized coordinates to view coordinates
     */
    private fun normalizedToViewCoordinates(keypoint: PoseKeypoint): PointF {
        // MediaPipe returns normalized coordinates (0.0 to 1.0)
        val x = keypoint.x * viewWidth
        val y = keypoint.y * viewHeight
        return PointF(x, y)
    }

    /**
     * Get paint based on confidence level
     */
    private fun getPaintForConfidence(confidence: Float): Paint {
        return when {
            confidence >= 0.8f -> highConfidencePaint
            confidence >= 0.6f -> mediumConfidencePaint
            else -> lowConfidencePaint
        }
    }

    /**
     * Check if keypoint is important for golf swing analysis
     */
    private fun isImportantKeypoint(name: String): Boolean {
        val importantKeypoints = listOf(
            MediaPipePoseLandmarks.LEFT_SHOULDER,
            MediaPipePoseLandmarks.RIGHT_SHOULDER,
            MediaPipePoseLandmarks.LEFT_ELBOW,
            MediaPipePoseLandmarks.RIGHT_ELBOW,
            MediaPipePoseLandmarks.LEFT_WRIST,
            MediaPipePoseLandmarks.RIGHT_WRIST,
            MediaPipePoseLandmarks.LEFT_HIP,
            MediaPipePoseLandmarks.RIGHT_HIP,
            MediaPipePoseLandmarks.LEFT_KNEE,
            MediaPipePoseLandmarks.RIGHT_KNEE
        )
        return name in importantKeypoints
    }

    /**
     * Set overlay transparency
     */
    fun setOverlayAlpha(alpha: Float) {
        this.alpha = alpha
    }

    /**
     * Toggle skeleton visibility
     */
    fun setSkeletonVisible(visible: Boolean) {
        skeletonPaint.alpha = if (visible) 255 else 0
        invalidate()
    }

    /**
     * Toggle keypoint visibility
     */
    fun setKeypointsVisible(visible: Boolean) {
        val alpha = if (visible) 255 else 0
        keypointPaint.alpha = alpha
        lowConfidencePaint.alpha = alpha
        mediumConfidencePaint.alpha = alpha
        highConfidencePaint.alpha = alpha
        invalidate()
    }
}