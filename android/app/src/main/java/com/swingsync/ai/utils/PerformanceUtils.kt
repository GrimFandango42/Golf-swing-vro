package com.swingsync.ai.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Utility class for performance monitoring and optimization
 */
object PerformanceUtils {

    private const val TAG = "PerformanceUtils"
    
    // Frame rate monitoring
    private val frameCounter = AtomicLong(0)
    private val lastFpsUpdate = AtomicLong(0)
    private var currentFps = 0f
    
    // Memory monitoring
    private val runtime = Runtime.getRuntime()
    
    /**
     * Monitor frame processing performance
     */
    fun recordFrameProcessed() {
        val currentTime = System.currentTimeMillis()
        val frameCount = frameCounter.incrementAndGet()
        val lastUpdate = lastFpsUpdate.get()
        
        // Update FPS every second
        if (currentTime - lastUpdate >= 1000) {
            if (lastFpsUpdate.compareAndSet(lastUpdate, currentTime)) {
                val timeDiff = currentTime - lastUpdate
                val framesDiff = frameCount - (frameCount - (timeDiff * currentFps / 1000).toLong())
                currentFps = (framesDiff * 1000f) / timeDiff
                
                Log.d(TAG, "Current FPS: $currentFps")
            }
        }
    }
    
    /**
     * Get current FPS
     */
    fun getCurrentFps(): Float = currentFps
    
    /**
     * Monitor memory usage
     */
    fun logMemoryUsage() {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val usedMB = usedMemory / (1024 * 1024)
        val totalMB = totalMemory / (1024 * 1024)
        val maxMB = maxMemory / (1024 * 1024)
        
        Log.d(TAG, "Memory Usage: ${usedMB}MB / ${totalMB}MB (Max: ${maxMB}MB)")
        
        // Warn if memory usage is high
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        if (memoryUsagePercent > 80) {
            Log.w(TAG, "High memory usage: ${memoryUsagePercent.toInt()}%")
        }
    }
    
    /**
     * Suggest garbage collection if memory is low
     */
    fun suggestGCIfNeeded() {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        val memoryUsagePercent = ((totalMemory - freeMemory).toFloat() / maxMemory.toFloat()) * 100
        
        if (memoryUsagePercent > 85) {
            Log.d(TAG, "Suggesting garbage collection due to high memory usage")
            System.gc()
        }
    }
    
    /**
     * Get device performance class
     */
    fun getDevicePerformanceClass(): DevicePerformanceClass {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Use MediaPerformanceClassManager for Android 12+
                DevicePerformanceClass.HIGH
            }
            hasHighPerformanceCPU() && hasEnoughRAM() -> DevicePerformanceClass.HIGH
            hasMediumPerformanceCPU() && hasModerateRAM() -> DevicePerformanceClass.MEDIUM
            else -> DevicePerformanceClass.LOW
        }
    }
    
    /**
     * Check if device has high performance CPU
     */
    private fun hasHighPerformanceCPU(): Boolean {
        val cores = runtime.availableProcessors()
        return cores >= 6 // Assuming 6+ cores indicates high performance
    }
    
    /**
     * Check if device has medium performance CPU
     */
    private fun hasMediumPerformanceCPU(): Boolean {
        val cores = runtime.availableProcessors()
        return cores >= 4 // 4-5 cores indicates medium performance
    }
    
    /**
     * Check if device has enough RAM
     */
    private fun hasEnoughRAM(): Boolean {
        val maxMemory = runtime.maxMemory()
        val maxMemoryMB = maxMemory / (1024 * 1024)
        return maxMemoryMB >= 2048 // 2GB+ indicates sufficient RAM
    }
    
    /**
     * Check if device has moderate RAM
     */
    private fun hasModerateRAM(): Boolean {
        val maxMemory = runtime.maxMemory()
        val maxMemoryMB = maxMemory / (1024 * 1024)
        return maxMemoryMB >= 1024 // 1GB+ indicates moderate RAM
    }
    
    /**
     * Get optimal camera resolution based on device performance
     */
    fun getOptimalCameraResolution(performanceClass: DevicePerformanceClass): Pair<Int, Int> {
        return when (performanceClass) {
            DevicePerformanceClass.HIGH -> Pair(1920, 1080) // Full HD
            DevicePerformanceClass.MEDIUM -> Pair(1280, 720) // HD
            DevicePerformanceClass.LOW -> Pair(854, 480) // 480p
        }
    }
    
    /**
     * Get optimal frame rate based on device performance
     */
    fun getOptimalFrameRate(performanceClass: DevicePerformanceClass): Int {
        return when (performanceClass) {
            DevicePerformanceClass.HIGH -> 60
            DevicePerformanceClass.MEDIUM -> 30
            DevicePerformanceClass.LOW -> 24
        }
    }
    
    /**
     * Get optimal MediaPipe model complexity
     */
    fun getOptimalModelComplexity(performanceClass: DevicePerformanceClass): Int {
        return when (performanceClass) {
            DevicePerformanceClass.HIGH -> 2 // Heavy model
            DevicePerformanceClass.MEDIUM -> 1 // Medium model
            DevicePerformanceClass.LOW -> 0 // Light model
        }
    }
    
    /**
     * Get processing thread count based on device
     */
    fun getOptimalThreadCount(): Int {
        val cores = runtime.availableProcessors()
        return maxOf(2, cores - 1) // Leave one core for UI thread
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformanceMetrics(context: Context) {
        val performanceClass = getDevicePerformanceClass()
        val optimalResolution = getOptimalCameraResolution(performanceClass)
        val optimalFrameRate = getOptimalFrameRate(performanceClass)
        
        Log.d(TAG, "=== Performance Metrics ===")
        Log.d(TAG, "Device Performance Class: $performanceClass")
        Log.d(TAG, "CPU Cores: ${runtime.availableProcessors()}")
        Log.d(TAG, "Max Memory: ${runtime.maxMemory() / (1024 * 1024)}MB")
        Log.d(TAG, "Optimal Resolution: ${optimalResolution.first}x${optimalResolution.second}")
        Log.d(TAG, "Optimal Frame Rate: ${optimalFrameRate}fps")
        Log.d(TAG, "Optimal Thread Count: ${getOptimalThreadCount()}")
        Log.d(TAG, "=========================")
        
        logMemoryUsage()
    }
    
    /**
     * Start performance monitoring
     */
    fun startPerformanceMonitoring() {
        Log.d(TAG, "Performance monitoring started")
        frameCounter.set(0)
        lastFpsUpdate.set(System.currentTimeMillis())
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopPerformanceMonitoring() {
        Log.d(TAG, "Performance monitoring stopped")
        Log.d(TAG, "Final FPS: $currentFps")
        logMemoryUsage()
    }
    
    /**
     * Device performance classification
     */
    enum class DevicePerformanceClass {
        LOW,    // Low-end devices - reduced quality/features
        MEDIUM, // Mid-range devices - balanced quality/performance
        HIGH    // High-end devices - maximum quality/features
    }
}