package com.swingsync.ai.ar

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Performance optimization utilities for AR rendering
 * Ensures 30fps+ performance on mobile devices
 */
class PerformanceOptimizer(private val context: Context) {
    
    companion object {
        private const val TAG = "PerformanceOptimizer"
        
        // Performance targets
        private const val TARGET_FPS = 30f
        private const val MIN_FPS = 20f
        private const val FRAME_TIME_TARGET_MS = 33f // 30 FPS
        
        // Quality levels
        enum class QualityLevel {
            HIGH,
            MEDIUM,
            LOW,
            POTATO
        }
        
        // Memory thresholds (MB)
        private const val LOW_MEMORY_THRESHOLD = 200
        private const val CRITICAL_MEMORY_THRESHOLD = 100
        
        // GPU performance hints
        private const val MAX_VERTICES_HIGH = 10000
        private const val MAX_VERTICES_MEDIUM = 5000
        private const val MAX_VERTICES_LOW = 2000
    }
    
    // Performance monitoring
    private var frameTimeHistory = mutableListOf<Float>()
    private var currentQuality = QualityLevel.HIGH
    private var lastOptimizationTime = 0L
    private var optimizationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Device capabilities
    private val deviceInfo = DeviceCapabilities(context)
    private var adaptiveQuality = true
    
    data class DeviceCapabilities(
        val totalRAM: Long,
        val availableRAM: Long,
        val gpuRenderer: String,
        val gpuVendor: String,
        val maxTextureSize: Int,
        val maxVertexAttribs: Int,
        val extensionsSupported: Set<String>
    ) {
        companion object {
            operator fun invoke(context: Context): DeviceCapabilities {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                
                // Get OpenGL info (this would need to be called from GL thread)
                val gpuRenderer = "Unknown" // Would get from GLES20.glGetString(GLES20.GL_RENDERER)
                val gpuVendor = "Unknown" // Would get from GLES20.glGetString(GLES20.GL_VENDOR)
                val maxTextureSize = 2048 // Default conservative value
                val maxVertexAttribs = 16 // Default value
                val extensions = emptySet<String>()
                
                return DeviceCapabilities(
                    totalRAM = memInfo.totalMem,
                    availableRAM = memInfo.availMem,
                    gpuRenderer = gpuRenderer,
                    gpuVendor = gpuVendor,
                    maxTextureSize = maxTextureSize,
                    maxVertexAttribs = maxVertexAttribs,
                    extensionsSupported = extensions
                )
            }
        }
        
        val isLowEndDevice: Boolean
            get() = totalRAM < 3L * 1024 * 1024 * 1024 // Less than 3GB RAM
            
        val isMidRangeDevice: Boolean
            get() = totalRAM >= 3L * 1024 * 1024 * 1024 && totalRAM < 6L * 1024 * 1024 * 1024
            
        val isHighEndDevice: Boolean
            get() = totalRAM >= 6L * 1024 * 1024 * 1024
    }
    
    data class PerformanceSettings(
        val maxPathPoints: Int,
        val gridLineCount: Int,
        val textureResolution: Int,
        val antialiasingEnabled: Boolean,
        val shadowsEnabled: Boolean,
        val particleEffectsEnabled: Boolean,
        val updateFrequencyMs: Long,
        val useVBO: Boolean,
        val cullingEnabled: Boolean
    ) {
        companion object {
            fun forQuality(quality: QualityLevel, deviceCaps: DeviceCapabilities): PerformanceSettings {
                return when (quality) {
                    QualityLevel.HIGH -> PerformanceSettings(
                        maxPathPoints = if (deviceCaps.isHighEndDevice) 200 else 150,
                        gridLineCount = 20,
                        textureResolution = 512,
                        antialiasingEnabled = true,
                        shadowsEnabled = deviceCaps.isHighEndDevice,
                        particleEffectsEnabled = true,
                        updateFrequencyMs = 16,
                        useVBO = true,
                        cullingEnabled = true
                    )
                    
                    QualityLevel.MEDIUM -> PerformanceSettings(
                        maxPathPoints = 100,
                        gridLineCount = 15,
                        textureResolution = 256,
                        antialiasingEnabled = deviceCaps.isMidRangeDevice || deviceCaps.isHighEndDevice,
                        shadowsEnabled = false,
                        particleEffectsEnabled = deviceCaps.isMidRangeDevice || deviceCaps.isHighEndDevice,
                        updateFrequencyMs = 33,
                        useVBO = true,
                        cullingEnabled = true
                    )
                    
                    QualityLevel.LOW -> PerformanceSettings(
                        maxPathPoints = 50,
                        gridLineCount = 10,
                        textureResolution = 128,
                        antialiasingEnabled = false,
                        shadowsEnabled = false,
                        particleEffectsEnabled = false,
                        updateFrequencyMs = 50,
                        useVBO = true,
                        cullingEnabled = true
                    )
                    
                    QualityLevel.POTATO -> PerformanceSettings(
                        maxPathPoints = 25,
                        gridLineCount = 5,
                        textureResolution = 64,
                        antialiasingEnabled = false,
                        shadowsEnabled = false,
                        particleEffectsEnabled = false,
                        updateFrequencyMs = 100,
                        useVBO = false,
                        cullingEnabled = true
                    )
                }
            }
        }
    }
    
    /**
     * Initialize optimizer with device-appropriate settings
     */
    fun initialize(): PerformanceSettings {
        Log.d(TAG, "Initializing performance optimizer")
        Log.d(TAG, "Device capabilities: RAM=${deviceInfo.totalRAM / (1024*1024)}MB, " +
                "GPU=${deviceInfo.gpuRenderer}")
        
        // Determine initial quality based on device
        currentQuality = when {
            deviceInfo.isHighEndDevice -> QualityLevel.HIGH
            deviceInfo.isMidRangeDevice -> QualityLevel.MEDIUM
            else -> QualityLevel.LOW
        }
        
        // Start performance monitoring
        startPerformanceMonitoring()
        
        val settings = PerformanceSettings.forQuality(currentQuality, deviceInfo)
        Log.d(TAG, "Initial quality: $currentQuality, Settings: $settings")
        
        return settings
    }
    
    /**
     * Report frame time for adaptive quality adjustment
     */
    fun reportFrameTime(frameTimeMs: Float) {
        frameTimeHistory.add(frameTimeMs)
        
        // Keep only recent history
        if (frameTimeHistory.size > 60) { // 2 seconds at 30fps
            frameTimeHistory.removeFirst()
        }
        
        // Check if we need to adjust quality
        if (adaptiveQuality && frameTimeHistory.size >= 30) {
            val avgFrameTime = frameTimeHistory.takeLast(30).average().toFloat()
            val currentFps = 1000f / avgFrameTime
            
            if (shouldAdjustQuality(currentFps)) {
                adjustQuality(currentFps)
            }
        }
    }
    
    /**
     * Get current performance settings
     */
    fun getCurrentSettings(): PerformanceSettings {
        return PerformanceSettings.forQuality(currentQuality, deviceInfo)
    }
    
    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        val avgFrameTime = if (frameTimeHistory.isNotEmpty()) {
            frameTimeHistory.average().toFloat()
        } else 0f
        
        val currentFps = if (avgFrameTime > 0) 1000f / avgFrameTime else 0f
        
        return mapOf(
            "current_fps" to currentFps,
            "target_fps" to TARGET_FPS,
            "avg_frame_time_ms" to avgFrameTime,
            "quality_level" to currentQuality.name,
            "device_category" to when {
                deviceInfo.isHighEndDevice -> "HIGH_END"
                deviceInfo.isMidRangeDevice -> "MID_RANGE"
                else -> "LOW_END"
            },
            "adaptive_quality_enabled" to adaptiveQuality,
            "available_memory_mb" to (deviceInfo.availableRAM / (1024 * 1024))
        )
    }
    
    /**
     * Enable/disable adaptive quality
     */
    fun setAdaptiveQuality(enabled: Boolean) {
        adaptiveQuality = enabled
        Log.d(TAG, "Adaptive quality ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Manually set quality level
     */
    fun setQualityLevel(quality: QualityLevel) {
        if (currentQuality != quality) {
            currentQuality = quality
            Log.d(TAG, "Quality level manually set to: $quality")
        }
    }
    
    /**
     * Optimize vertex buffer for current settings
     */
    fun optimizeVertexBuffer(vertices: FloatArray): FloatArray {
        val settings = getCurrentSettings()
        
        return if (vertices.size > settings.maxPathPoints * 3) {
            // Reduce vertex count by sampling
            val step = vertices.size / (settings.maxPathPoints * 3)
            vertices.filterIndexed { index, _ -> index % step == 0 }.toFloatArray()
        } else {
            vertices
        }
    }
    
    /**
     * Check if memory pressure is high
     */
    fun isMemoryPressureHigh(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableMB = memInfo.availMem / (1024 * 1024)
        return availableMB < LOW_MEMORY_THRESHOLD
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        optimizationScope.cancel()
        frameTimeHistory.clear()
    }
    
    // Private methods
    
    private fun startPerformanceMonitoring() {
        optimizationScope.launch {
            while (isActive) {
                delay(5000) // Check every 5 seconds
                
                // Check memory pressure
                if (isMemoryPressureHigh()) {
                    Log.w(TAG, "High memory pressure detected")
                    if (currentQuality != QualityLevel.POTATO) {
                        val newQuality = when (currentQuality) {
                            QualityLevel.HIGH -> QualityLevel.MEDIUM
                            QualityLevel.MEDIUM -> QualityLevel.LOW
                            QualityLevel.LOW -> QualityLevel.POTATO
                            QualityLevel.POTATO -> QualityLevel.POTATO
                        }
                        adjustQualityTo(newQuality, "Memory pressure")
                    }
                }
                
                // Log performance metrics
                val metrics = getPerformanceMetrics()
                Log.d(TAG, "Performance: FPS=${metrics["current_fps"]}, " +
                        "Quality=${metrics["quality_level"]}, " +
                        "Memory=${metrics["available_memory_mb"]}MB")
            }
        }
    }
    
    private fun shouldAdjustQuality(currentFps: Float): Boolean {
        val timeSinceLastAdjustment = System.currentTimeMillis() - lastOptimizationTime
        return timeSinceLastAdjustment > 3000 && // At least 3 seconds between adjustments
                (currentFps < MIN_FPS || currentFps > TARGET_FPS + 10f)
    }
    
    private fun adjustQuality(currentFps: Float) {
        val newQuality = when {
            currentFps < MIN_FPS -> {
                // Performance is poor, reduce quality
                when (currentQuality) {
                    QualityLevel.HIGH -> QualityLevel.MEDIUM
                    QualityLevel.MEDIUM -> QualityLevel.LOW
                    QualityLevel.LOW -> QualityLevel.POTATO
                    QualityLevel.POTATO -> QualityLevel.POTATO
                }
            }
            currentFps > TARGET_FPS + 15f && !isMemoryPressureHigh() -> {
                // Performance is good, can increase quality
                when (currentQuality) {
                    QualityLevel.POTATO -> QualityLevel.LOW
                    QualityLevel.LOW -> QualityLevel.MEDIUM
                    QualityLevel.MEDIUM -> if (deviceInfo.isHighEndDevice) QualityLevel.HIGH else QualityLevel.MEDIUM
                    QualityLevel.HIGH -> QualityLevel.HIGH
                }
            }
            else -> currentQuality
        }
        
        if (newQuality != currentQuality) {
            adjustQualityTo(newQuality, "FPS=$currentFps")
        }
    }
    
    private fun adjustQualityTo(newQuality: QualityLevel, reason: String) {
        val oldQuality = currentQuality
        currentQuality = newQuality
        lastOptimizationTime = System.currentTimeMillis()
        
        Log.i(TAG, "Quality adjusted: $oldQuality -> $newQuality (Reason: $reason)")
    }
}

/**
 * OpenGL ES optimization utilities
 */
object GLOptimizer {
    
    /**
     * Check and optimize OpenGL state
     */
    fun optimizeGLState() {
        // Disable unnecessary OpenGL features for better performance
        GLES20.glDisable(GLES20.GL_DITHER)
        
        // Enable depth testing with optimized function
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        
        // Optimize blending
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // Enable culling for better performance
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
    }
    
    /**
     * Check for OpenGL errors
     */
    fun checkGLError(operation: String): Boolean {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("GLOptimizer", "$operation: glError $error")
            return false
        }
        return true
    }
    
    /**
     * Get GPU capabilities
     */
    fun getGPUCapabilities(): Map<String, Any> {
        return mapOf(
            "renderer" to (GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"),
            "vendor" to (GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"),
            "version" to (GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"),
            "extensions" to (GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: "").split(" ")
        )
    }
}