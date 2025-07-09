package com.golfswing.vro.pixel.ui.camera

import android.content.Context
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfswing.vro.pixel.camera.GolfSwingCameraManager
import com.golfswing.vro.pixel.coaching.RealTimeCoachingEngine
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector
import com.golfswing.vro.pixel.performance.PerformanceMonitor
import com.golfswing.vro.pixel.resource.ResourcePool
import com.golfswing.vro.pixel.battery.BatteryOptimizationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraManager: GolfSwingCameraManager,
    private val poseDetector: GolfSwingPoseDetector,
    private val coachingEngine: RealTimeCoachingEngine,
    private val performanceMonitor: PerformanceMonitor,
    private val resourcePool: ResourcePool,
    private val batteryOptimizationManager: BatteryOptimizationManager
) : ViewModel() {

    // Camera state
    val cameraState = cameraManager.cameraState
    val isRecording = cameraManager.isRecording
    
    // Pose detection state
    val currentSwingPhase = poseDetector.swingPhase
    val poseResult = poseDetector.poseResult
    
    // Coaching state
    val coachingFeedback = coachingEngine.coachingFeedback
    val swingAnalysis = coachingEngine.swingAnalysis
    val practiceRecommendations = coachingEngine.practiceRecommendations
    
    // Session statistics
    private val _sessionStats = MutableStateFlow(RealTimeCoachingEngine.SwingSession())
    val sessionStats: StateFlow<RealTimeCoachingEngine.SwingSession> = _sessionStats.asStateFlow()
    
    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        initializePoseDetector()
        startCoachingEngine()
        observeSessionStats()
        initializePerformanceMonitoring()
    }

    /**
     * Initialize camera with lifecycle owner and preview view
     */
    fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                cameraManager.initializeCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onFrameAnalyzed = { imageProxy ->
                        processFrame(imageProxy)
                    }
                )
                _isInitialized.value = true
            } catch (e: Exception) {
                // Handle camera initialization error
                e.printStackTrace()
            }
        }
    }

    // Frame rate control
    private var lastFrameProcessTime = 0L
    private var frameProcessingInterval = 33L // 30 FPS default
    private var adaptiveProcessingEnabled = true
    
    /**
     * Process camera frame for pose detection with adaptive throttling
     */
    private fun processFrame(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Adaptive frame rate based on processing load
        if (adaptiveProcessingEnabled) {
            adaptFrameProcessingInterval()
        }
        
        // Check if we should process this frame
        if (currentTime - lastFrameProcessTime < frameProcessingInterval) {
            imageProxy.close()
            return
        }
        
        lastFrameProcessTime = currentTime
        
        try {
            // Process frame with pose detector
            poseDetector.processFrame(imageProxy)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Dynamically adjust frame processing interval based on system performance
     */
    private fun adaptFrameProcessingInterval() {
        val cameraMetrics = cameraManager.getPerformanceMetrics()
        val poseStats = poseDetector.getProcessingStats()
        val batteryOptimization = batteryOptimizationManager.getOptimizationSettings()
        val performanceRecommendation = performanceMonitor.getPerformanceRecommendation()
        
        // Adjust interval based on multiple factors
        frameProcessingInterval = when {
            performanceRecommendation == PerformanceMonitor.PerformanceRecommendation.REDUCE_PROCESSING_AGGRESSIVE -> 100L
            performanceRecommendation == PerformanceMonitor.PerformanceRecommendation.REDUCE_PROCESSING_MODERATE -> 60L
            batteryOptimizationManager.shouldEnterBatterySavingMode() -> batteryOptimization.poseDetectionInterval
            performanceMonitor.isUnderThermalStress() -> 80L
            cameraMetrics.processingTime > 50L -> 50L // Slower processing
            cameraMetrics.processingTime > 30L -> 40L // Medium processing
            poseStats.isProcessing -> 45L // Pose detector is busy
            else -> batteryOptimization.poseDetectionInterval // Use battery optimization settings
        }
    }

    /**
     * Start video recording
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
                val outputFile = createOutputFile()
                cameraManager.startRecording(outputFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stop video recording
     */
    fun stopRecording() {
        cameraManager.stopRecording()
    }

    /**
     * Reset current session
     */
    fun resetSession() {
        coachingEngine.resetSession()
        _sessionStats.value = RealTimeCoachingEngine.SwingSession()
    }

    /**
     * Switch between front and rear camera
     */
    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                cameraManager.switchCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onFrameAnalyzed = { imageProxy ->
                        processFrame(imageProxy)
                    },
                    useFrontCamera = useFrontCamera
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Capture single frame for analysis
     */
    fun captureFrame() {
        cameraManager.captureFrame { imageProxy ->
            processFrame(imageProxy)
        }
    }

    /**
     * Initialize pose detector
     */
    private fun initializePoseDetector() {
        viewModelScope.launch {
            try {
                poseDetector.initialize()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start real-time coaching engine
     */
    private fun startCoachingEngine() {
        viewModelScope.launch {
            coachingEngine.startRealTimeCoaching()
        }
    }
    
    /**
     * Initialize performance monitoring
     */
    private fun initializePerformanceMonitoring() {
        viewModelScope.launch {
            performanceMonitor.startMonitoring()
            batteryOptimizationManager.startBatteryMonitoring()
        }
    }

    /**
     * Observe session statistics
     */
    private fun observeSessionStats() {
        viewModelScope.launch {
            // Update session stats every 5 seconds
            while (true) {
                kotlinx.coroutines.delay(5000)
                _sessionStats.value = coachingEngine.getCurrentSessionStats()
            }
        }
    }

    /**
     * Create output file for video recording - using internal storage for privacy
     */
    private fun createOutputFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "golf_swing_$timestamp.mp4"
        
        // Create file in app's internal files directory for privacy
        val recordingsDir = File(context.filesDir, "recordings")
        recordingsDir.mkdirs()
        
        return File(recordingsDir, fileName).apply {
            // Set secure file permissions
            setReadable(false, false)
            setWritable(false, false)
            setExecutable(false, false)
            
            // Set permissions for owner only
            setReadable(true, true)
            setWritable(true, true)
        }
    }

    /**
     * Get camera capabilities for optimization
     */
    fun getCameraCapabilities() = cameraManager.getCameraCapabilities()

    /**
     * Get current pose detection result
     */
    fun getCurrentPoseResult() = poseResult.value

    /**
     * Get current coaching feedback
     */
    fun getCurrentCoachingFeedback() = coachingFeedback.value

    /**
     * Get current swing analysis
     */
    fun getCurrentSwingAnalysis() = swingAnalysis.value

    /**
     * Get practice recommendations
     */
    fun getPracticeRecommendations() = practiceRecommendations.value

    /**
     * Enable/disable real-time coaching
     */
    fun toggleRealTimeCoaching(enabled: Boolean) {
        if (enabled) {
            startCoachingEngine()
        } else {
            // Stop coaching engine (implementation depends on requirements)
        }
    }

    /**
     * Update coaching settings
     */
    fun updateCoachingSettings(
        enableRealTimeFeedback: Boolean = true,
        enableVoiceCoaching: Boolean = false,
        skillLevel: String = "intermediate"
    ) {
        // Update coaching engine settings
        // This would be implemented based on specific requirements
    }
    
    /**
     * Enable/disable adaptive frame processing
     */
    fun setAdaptiveProcessing(enabled: Boolean) {
        adaptiveProcessingEnabled = enabled
        if (!enabled) {
            frameProcessingInterval = 33L // Reset to 30 FPS
        }
    }
    
    /**
     * Set target frame processing rate
     */
    fun setTargetFrameRate(fps: Int) {
        frameProcessingInterval = 1000L / fps
    }
    
    /**
     * Get current performance metrics
     */
    fun getPerformanceMetrics(): PerformanceInfo {
        val cameraMetrics = cameraManager.getPerformanceMetrics()
        val poseStats = poseDetector.getProcessingStats()
        val performanceData = performanceMonitor.performanceData.value
        val batteryState = batteryOptimizationManager.batteryState.value
        val resourceStats = resourcePool.getPoolStats()
        
        return PerformanceInfo(
            cameraFps = cameraMetrics.fps,
            frameDrops = cameraMetrics.frameDrops,
            processingTime = cameraMetrics.processingTime,
            memoryUsage = cameraMetrics.memoryUsage,
            poseFramesProcessed = poseStats.processedFrames,
            currentFrameInterval = frameProcessingInterval,
            systemFps = performanceData.fps,
            cpuUsage = performanceData.cpuUsage,
            thermalState = performanceData.thermalState,
            batteryLevel = batteryState.level,
            optimizationMode = batteryOptimizationManager.optimizationMode.value,
            resourcePoolEfficiency = resourceStats.bitmapHitRate
        )
    }
    
    data class PerformanceInfo(
        val cameraFps: Float,
        val frameDrops: Long,
        val processingTime: Long,
        val memoryUsage: Long,
        val poseFramesProcessed: Long,
        val currentFrameInterval: Long,
        val systemFps: Float,
        val cpuUsage: Float,
        val thermalState: PerformanceMonitor.ThermalState,
        val batteryLevel: Int,
        val optimizationMode: BatteryOptimizationManager.OptimizationMode,
        val resourcePoolEfficiency: Float
    )
    
    /**
     * Get comprehensive performance summary
     */
    fun getPerformanceSummary(): String {
        val performanceInfo = getPerformanceMetrics()
        return """
            === PERFORMANCE SUMMARY ===
            Camera FPS: ${String.format("%.1f", performanceInfo.cameraFps)}
            System FPS: ${String.format("%.1f", performanceInfo.systemFps)}
            Frame Drops: ${performanceInfo.frameDrops}
            Processing Time: ${performanceInfo.processingTime}ms
            Frame Interval: ${performanceInfo.currentFrameInterval}ms
            CPU Usage: ${String.format("%.1f", performanceInfo.cpuUsage)}%
            Memory Usage: ${performanceInfo.memoryUsage / 1024 / 1024}MB
            Thermal State: ${performanceInfo.thermalState}
            Battery Level: ${performanceInfo.batteryLevel}%
            Optimization Mode: ${performanceInfo.optimizationMode}
            Resource Pool Efficiency: ${String.format("%.1f", performanceInfo.resourcePoolEfficiency)}%
            Pose Frames Processed: ${performanceInfo.poseFramesProcessed}
            
            ${performanceMonitor.getPerformanceSummary()}
            
            ${batteryOptimizationManager.getBatteryInfo()}
        """.trimIndent()
    }
    
    /**
     * Force performance optimization based on current conditions
     */
    fun optimizePerformance() {
        viewModelScope.launch {
            // Force update battery state
            batteryOptimizationManager.forceUpdateBatteryState()
            
            // Reset performance counters
            performanceMonitor.resetCounters()
            cameraManager.resetPerformanceMetrics()
            
            // Force garbage collection if under memory pressure
            if (performanceMonitor.isUnderMemoryPressure()) {
                resourcePool.forceGarbageCollection()
            }
            
            // Adapt frame processing interval
            adaptFrameProcessingInterval()
        }
    }

    /**
     * Get historical swing data
     */
    fun getSwingHistory(): List<String> {
        // Return historical swing data
        // This would be implemented with proper data storage
        return emptyList()
    }

    /**
     * Export swing data
     */
    fun exportSwingData(format: String = "json") {
        viewModelScope.launch {
            try {
                // Export swing data in specified format
                // This would be implemented based on requirements
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        
        // Release camera resources
        cameraManager.release()
        
        // Release pose detector resources
        poseDetector.release()
        
        // Reset coaching engine
        coachingEngine.resetSession()
        
        // Release performance monitoring resources
        performanceMonitor.release()
        batteryOptimizationManager.release()
        resourcePool.release()
    }
}