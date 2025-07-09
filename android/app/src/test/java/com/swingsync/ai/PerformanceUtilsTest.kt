package com.swingsync.ai

import com.swingsync.ai.utils.PerformanceUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Performance Utilities
 */
class PerformanceUtilsTest {

    @Test
    fun testDevicePerformanceClassification() {
        val performanceClass = PerformanceUtils.getDevicePerformanceClass()
        
        // Should return a valid performance class
        assertTrue(
            "Performance class should be valid",
            performanceClass in listOf(
                PerformanceUtils.DevicePerformanceClass.LOW,
                PerformanceUtils.DevicePerformanceClass.MEDIUM,
                PerformanceUtils.DevicePerformanceClass.HIGH
            )
        )
    }

    @Test
    fun testOptimalCameraResolution() {
        // Test all performance classes
        val highRes = PerformanceUtils.getOptimalCameraResolution(
            PerformanceUtils.DevicePerformanceClass.HIGH
        )
        val mediumRes = PerformanceUtils.getOptimalCameraResolution(
            PerformanceUtils.DevicePerformanceClass.MEDIUM
        )
        val lowRes = PerformanceUtils.getOptimalCameraResolution(
            PerformanceUtils.DevicePerformanceClass.LOW
        )

        // High performance should have highest resolution
        assertTrue("High performance should have HD+ resolution", 
            highRes.first >= 1920 && highRes.second >= 1080)
        
        // Medium should be less than high
        assertTrue("Medium performance should have moderate resolution",
            mediumRes.first <= highRes.first && mediumRes.second <= highRes.second)
        
        // Low should be least
        assertTrue("Low performance should have lowest resolution",
            lowRes.first <= mediumRes.first && lowRes.second <= mediumRes.second)
    }

    @Test
    fun testOptimalFrameRate() {
        val highFps = PerformanceUtils.getOptimalFrameRate(
            PerformanceUtils.DevicePerformanceClass.HIGH
        )
        val mediumFps = PerformanceUtils.getOptimalFrameRate(
            PerformanceUtils.DevicePerformanceClass.MEDIUM
        )
        val lowFps = PerformanceUtils.getOptimalFrameRate(
            PerformanceUtils.DevicePerformanceClass.LOW
        )

        // Frame rates should be reasonable
        assertTrue("High performance FPS should be reasonable", highFps >= 30 && highFps <= 120)
        assertTrue("Medium performance FPS should be reasonable", mediumFps >= 24 && mediumFps <= 60)
        assertTrue("Low performance FPS should be reasonable", lowFps >= 15 && lowFps <= 30)
        
        // Higher performance should have higher or equal FPS
        assertTrue("Higher performance should have higher FPS", highFps >= mediumFps)
        assertTrue("Medium performance should have higher FPS than low", mediumFps >= lowFps)
    }

    @Test
    fun testOptimalThreadCount() {
        val threadCount = PerformanceUtils.getOptimalThreadCount()
        
        // Should return reasonable thread count
        assertTrue("Thread count should be at least 2", threadCount >= 2)
        assertTrue("Thread count should not exceed available processors", 
            threadCount <= Runtime.getRuntime().availableProcessors())
    }

    @Test
    fun testFpsMonitoring() {
        // Start monitoring
        PerformanceUtils.startPerformanceMonitoring()
        
        // Simulate some frame processing
        for (i in 1..10) {
            PerformanceUtils.recordFrameProcessed()
            Thread.sleep(16) // Simulate ~60fps
        }
        
        // FPS should be calculated
        val fps = PerformanceUtils.getCurrentFps()
        
        // Should be a reasonable FPS (allowing for test timing variations)
        assertTrue("FPS should be reasonable", fps >= 0 && fps <= 120)
        
        // Stop monitoring
        PerformanceUtils.stopPerformanceMonitoring()
    }

    @Test
    fun testModelComplexityMapping() {
        val highComplexity = PerformanceUtils.getOptimalModelComplexity(
            PerformanceUtils.DevicePerformanceClass.HIGH
        )
        val mediumComplexity = PerformanceUtils.getOptimalModelComplexity(
            PerformanceUtils.DevicePerformanceClass.MEDIUM
        )
        val lowComplexity = PerformanceUtils.getOptimalModelComplexity(
            PerformanceUtils.DevicePerformanceClass.LOW
        )

        // Should return valid complexity values
        assertTrue("High complexity should be valid", highComplexity >= 0 && highComplexity <= 2)
        assertTrue("Medium complexity should be valid", mediumComplexity >= 0 && mediumComplexity <= 2)
        assertTrue("Low complexity should be valid", lowComplexity >= 0 && lowComplexity <= 2)
        
        // Higher performance should allow higher complexity
        assertTrue("Higher performance should allow higher complexity", 
            highComplexity >= mediumComplexity)
        assertTrue("Medium performance should allow higher complexity than low", 
            mediumComplexity >= lowComplexity)
    }
}