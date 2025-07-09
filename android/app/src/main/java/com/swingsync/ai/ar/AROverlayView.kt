package com.swingsync.ai.ar

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.swingsync.ai.data.model.FramePoseData
import com.swingsync.ai.data.model.MediaPipePoseLandmarks
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * AR overlay view that combines camera feed with 3D swing plane visualization
 * Features:
 * - Real-time 3D swing plane rendering
 * - Club path tracking and visualization
 * - Interactive camera controls (zoom, rotate)
 * - Performance optimizations for mobile
 * - Smooth animations and transitions
 */
class AROverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    
    companion object {
        private const val TAG = "AROverlayView"
        
        // Update frequencies (ms)
        private const val SWING_PLANE_UPDATE_INTERVAL = 100L
        private const val CLUB_PATH_UPDATE_INTERVAL = 33L // ~30 FPS
        private const val CAMERA_UPDATE_INTERVAL = 16L // ~60 FPS
        
        // Camera control parameters
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 3.0f
        private const val ZOOM_SENSITIVITY = 0.01f
        private const val ROTATION_SENSITIVITY = 0.5f
        
        // Animation parameters
        private const val TRANSITION_DURATION = 500L
        private const val FADE_DURATION = 300L
    }
    
    // Core components
    private val renderer = SwingPlaneRenderer()
    private val swingPlaneCalculator = SwingPlaneCalculator()
    private val clubPathTracker = ClubPathTracker()
    
    // State management
    private var isARActive = false
    private var isCalibrated = false
    private var currentClubType = "7-Iron"
    private var currentHandedness = SwingPlaneCalculator.Handedness.RIGHT_HANDED
    
    // Data processing
    private var latestPoseData: FramePoseData? = null
    private var idealSwingPlane: SwingPlaneCalculator.SwingPlane? = null
    private var actualSwingPlane: SwingPlaneCalculator.SwingPlane? = null
    private var currentClubPath: ClubPathTracker.ClubPath? = null
    
    // Camera control
    private var cameraDistance = 5f
    private var cameraAngleX = 0f
    private var cameraAngleY = 0f
    private var cameraTarget = Vector3(0f, 0f, 0f)
    private var scaleGestureDetector: ScaleGestureDetector
    
    // Animation
    private var fadeAlpha = 0f
    private var transitionProgress = 0f
    private val animationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Performance monitoring
    private var lastUpdateTime = 0L
    private var frameCount = 0
    private var processingTime = 0L
    
    // Callbacks
    private var onSwingPlaneCalculatedListener: ((SwingPlaneCalculator.SwingPlane?, SwingPlaneCalculator.SwingPlane?) -> Unit)? = null
    private var onClubPathUpdatedListener: ((ClubPathTracker.ClubPath?) -> Unit)? = null
    private var onARStatusChangedListener: ((Boolean) -> Unit)? = null
    
    init {
        // Set up OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        
        // Set renderer
        setRenderer(renderer)
        
        // Only render when dirty (for performance)
        renderMode = RENDERMODE_WHEN_DIRTY
        
        // Initialize gesture detector
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        
        // Set initial camera position
        updateCameraPosition()
        
        // Start update loops
        startUpdateCoroutines()
        
        Log.d(TAG, "AROverlayView initialized")
    }
    
    /**
     * Activate AR mode
     */
    fun activateAR(clubType: String, handedness: SwingPlaneCalculator.Handedness) {
        if (isARActive) return
        
        currentClubType = clubType
        currentHandedness = handedness
        
        // Start with fade-in animation
        animateTransition(true) {
            isARActive = true
            onARStatusChangedListener?.invoke(true)
            
            // Start club path tracking
            clubPathTracker.startTracking(clubType, handedness)
            
            Log.d(TAG, "AR mode activated for $clubType")
        }
    }
    
    /**
     * Deactivate AR mode
     */
    fun deactivateAR() {
        if (!isARActive) return
        
        // Stop club path tracking
        clubPathTracker.stopTracking()
        
        // Fade out animation
        animateTransition(false) {
            isARActive = false
            onARStatusChangedListener?.invoke(false)
            
            // Clear all data
            clearSwingData()
            
            Log.d(TAG, "AR mode deactivated")
        }
    }
    
    /**
     * Update with new pose data
     */
    fun updatePoseData(poseData: FramePoseData) {
        if (!isARActive) return
        
        latestPoseData = poseData
        
        // Update club path tracking
        val currentTime = System.currentTimeMillis()
        clubPathTracker.updatePath(poseData, currentTime)?.let { pathPoint ->
            currentClubPath = clubPathTracker.getCurrentPath()
            onClubPathUpdatedListener?.invoke(currentClubPath)
        }
        
        // Check if we need to recalculate swing plane
        if (shouldRecalculateSwingPlane()) {
            recalculateSwingPlane()
        }
        
        // Update camera target based on pose
        updateCameraTarget(poseData)
        
        // Request render
        requestRender()
    }
    
    /**
     * Set swing plane visibility
     */
    fun setSwingPlaneVisibility(showIdeal: Boolean, showActual: Boolean, showPath: Boolean) {
        renderer.setPlaneVisibility(showIdeal, showActual, showPath)
        requestRender()
    }
    
    /**
     * Set visual parameters
     */
    fun setVisualParameters(
        planeAlpha: Float = 0.6f,
        gridSpacing: Float = 0.5f,
        glowIntensity: Float = 0.3f
    ) {
        renderer.setVisualParameters(planeAlpha, gridSpacing, glowIntensity)
        requestRender()
    }
    
    /**
     * Reset camera to default position
     */
    fun resetCamera() {
        cameraDistance = 5f
        cameraAngleX = 0f
        cameraAngleY = 0f
        cameraTarget = Vector3(0f, 0f, 0f)
        updateCameraPosition()
        requestRender()
    }
    
    /**
     * Get current performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        return mapOf(
            "fps" to renderer.getCurrentFPS(),
            "is_ar_active" to isARActive,
            "is_calibrated" to isCalibrated,
            "frame_count" to frameCount,
            "processing_time_ms" to processingTime,
            "club_path_points" to (currentClubPath?.points?.size ?: 0),
            "camera_distance" to cameraDistance
        )
    }
    
    /**
     * Set callbacks
     */
    fun setOnSwingPlaneCalculatedListener(listener: (SwingPlaneCalculator.SwingPlane?, SwingPlaneCalculator.SwingPlane?) -> Unit) {
        onSwingPlaneCalculatedListener = listener
    }
    
    fun setOnClubPathUpdatedListener(listener: (ClubPathTracker.ClubPath?) -> Unit) {
        onClubPathUpdatedListener = listener
    }
    
    fun setOnARStatusChangedListener(listener: (Boolean) -> Unit) {
        onARStatusChangedListener = listener
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isARActive) return false
        
        scaleGestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    handleCameraRotation(event)
                }
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationScope.cancel()
    }
    
    // Private methods
    
    private fun startUpdateCoroutines() {
        // Swing plane calculation loop
        animationScope.launch {
            while (isActive) {
                if (isARActive && latestPoseData != null) {
                    val startTime = System.currentTimeMillis()
                    
                    // Calculate ideal swing plane
                    idealSwingPlane = swingPlaneCalculator.calculateIdealSwingPlane(
                        latestPoseData!!,
                        currentClubType,
                        currentHandedness
                    )
                    
                    // Update renderer
                    renderer.updateSwingPlanes(idealSwingPlane, actualSwingPlane)
                    
                    processingTime = System.currentTimeMillis() - startTime
                    requestRender()
                }
                
                delay(SWING_PLANE_UPDATE_INTERVAL)
            }
        }
        
        // Club path update loop
        animationScope.launch {
            while (isActive) {
                if (isARActive) {
                    currentClubPath?.let { path ->
                        renderer.updateClubPath(path)
                        requestRender()
                    }
                }
                
                delay(CLUB_PATH_UPDATE_INTERVAL)
            }
        }
        
        // Camera update loop
        animationScope.launch {
            while (isActive) {
                if (isARActive) {
                    updateCameraPosition()
                    requestRender()
                }
                
                delay(CAMERA_UPDATE_INTERVAL)
            }
        }
    }
    
    private fun shouldRecalculateSwingPlane(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastUpdateTime > SWING_PLANE_UPDATE_INTERVAL
    }
    
    private fun recalculateSwingPlane() {
        latestPoseData?.let { poseData ->
            idealSwingPlane = swingPlaneCalculator.calculateIdealSwingPlane(
                poseData,
                currentClubType,
                currentHandedness
            )
            
            // Calculate actual swing plane from club path if available
            currentClubPath?.let { path ->
                if (path.points.size > 20) {
                    val handPositions = path.points.map { it.position }
                    actualSwingPlane = swingPlaneCalculator.calculateActualSwingPlane(handPositions)
                }
            }
            
            // Update renderer
            renderer.updateSwingPlanes(idealSwingPlane, actualSwingPlane)
            
            // Notify listeners
            onSwingPlaneCalculatedListener?.invoke(idealSwingPlane, actualSwingPlane)
            
            lastUpdateTime = System.currentTimeMillis()
        }
    }
    
    private fun updateCameraTarget(poseData: FramePoseData) {
        val leftShoulder = poseData[MediaPipePoseLandmarks.LEFT_SHOULDER]
        val rightShoulder = poseData[MediaPipePoseLandmarks.RIGHT_SHOULDER]
        val leftHip = poseData[MediaPipePoseLandmarks.LEFT_HIP]
        val rightHip = poseData[MediaPipePoseLandmarks.RIGHT_HIP]
        
        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            // Calculate body center
            val bodyCenter = Vector3(
                (leftShoulder.x + rightShoulder.x + leftHip.x + rightHip.x) / 4f,
                (leftShoulder.y + rightShoulder.y + leftHip.y + rightHip.y) / 4f,
                (leftShoulder.z + rightShoulder.z + leftHip.z + rightHip.z) / 4f
            )
            
            // Smooth camera target transition
            val smoothing = 0.9f
            cameraTarget = cameraTarget * smoothing + bodyCenter * (1f - smoothing)
        }
    }
    
    private fun updateCameraPosition() {
        val x = cameraDistance * cos(cameraAngleY * PI / 180f) * cos(cameraAngleX * PI / 180f)
        val y = cameraDistance * sin(cameraAngleX * PI / 180f)
        val z = cameraDistance * sin(cameraAngleY * PI / 180f) * cos(cameraAngleX * PI / 180f)
        
        val cameraPosition = Vector3(x.toFloat(), y.toFloat(), z.toFloat()) + cameraTarget
        
        renderer.setCamera(cameraPosition, cameraTarget)
    }
    
    private fun handleCameraRotation(event: MotionEvent) {
        // Simple rotation based on touch movement
        val deltaX = event.x - (width / 2f)
        val deltaY = event.y - (height / 2f)
        
        cameraAngleY += deltaX * ROTATION_SENSITIVITY
        cameraAngleX += deltaY * ROTATION_SENSITIVITY
        
        // Clamp angles
        cameraAngleX = cameraAngleX.coerceIn(-90f, 90f)
        cameraAngleY = cameraAngleY % 360f
        
        updateCameraPosition()
    }
    
    private fun animateTransition(fadeIn: Boolean, onComplete: () -> Unit) {
        animationScope.launch {
            val startAlpha = if (fadeIn) 0f else 1f
            val endAlpha = if (fadeIn) 1f else 0f
            val duration = if (fadeIn) FADE_DURATION else FADE_DURATION
            
            val startTime = System.currentTimeMillis()
            
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                
                fadeAlpha = startAlpha + (endAlpha - startAlpha) * progress
                
                if (progress >= 1f) {
                    onComplete()
                    break
                }
                
                delay(16) // ~60 FPS
            }
        }
    }
    
    private fun clearSwingData() {
        idealSwingPlane = null
        actualSwingPlane = null
        currentClubPath = null
        latestPoseData = null
        clubPathTracker.clearPath()
        
        renderer.updateSwingPlanes(null, null)
        renderer.updateClubPath(null)
        
        requestRender()
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            cameraDistance *= scaleFactor
            cameraDistance = cameraDistance.coerceIn(MIN_ZOOM, MAX_ZOOM)
            updateCameraPosition()
            return true
        }
    }
}