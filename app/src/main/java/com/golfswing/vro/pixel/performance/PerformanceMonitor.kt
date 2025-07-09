package com.golfswing.vro.pixel.performance

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    private val monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    
    private val _performanceData = MutableStateFlow(PerformanceData())
    val performanceData: StateFlow<PerformanceData> = _performanceData.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    // Performance counters
    private val frameCount = AtomicLong(0)
    private val frameDropCount = AtomicLong(0)
    private val processingTimeSum = AtomicLong(0)
    private val processingTimeCount = AtomicLong(0)
    
    // System monitoring
    private var lastCpuTime = 0L
    private var lastAppCpuTime = 0L
    private var monitoringJob: Job? = null
    
    data class PerformanceData(
        val fps: Float = 0f,
        val frameDrops: Long = 0L,
        val averageProcessingTime: Long = 0L,
        val cpuUsage: Float = 0f,
        val memoryUsage: Long = 0L,
        val memoryTotal: Long = 0L,
        val thermalState: ThermalState = ThermalState.NORMAL,
        val batteryLevel: Float = 0f,
        val powerConsumption: Float = 0f,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ThermalState {
        NORMAL, LIGHT, MODERATE, SEVERE, CRITICAL
    }
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        _isMonitoring.value = true
        
        monitoringJob = monitoringScope.launch {
            while (isActive) {
                try {
                    updatePerformanceData()
                    delay(1000) // Update every second
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Record frame processing
     */
    fun recordFrameProcessed(processingTime: Long) {
        frameCount.incrementAndGet()
        processingTimeSum.addAndGet(processingTime)
        processingTimeCount.incrementAndGet()
    }
    
    /**
     * Record frame drop
     */
    fun recordFrameDropped() {
        frameDropCount.incrementAndGet()
    }
    
    /**
     * Update performance data
     */
    private suspend fun updatePerformanceData() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val cpuUsage = getCpuUsage()
        val thermalState = getThermalState()
        val batteryLevel = getBatteryLevel()
        val powerConsumption = getPowerConsumption()
        
        val fps = calculateFps()
        val averageProcessingTime = calculateAverageProcessingTime()
        
        _performanceData.value = PerformanceData(
            fps = fps,
            frameDrops = frameDropCount.get(),
            averageProcessingTime = averageProcessingTime,
            cpuUsage = cpuUsage,
            memoryUsage = usedMemory,
            memoryTotal = totalMemory,
            thermalState = thermalState,
            batteryLevel = batteryLevel,
            powerConsumption = powerConsumption
        )
    }
    
    /**
     * Calculate current FPS
     */
    private fun calculateFps(): Float {
        val currentFrameCount = frameCount.get()
        val currentTime = System.currentTimeMillis()
        
        // Reset counters every second for accurate FPS calculation
        if (currentTime % 1000 < 100) {
            frameCount.set(0)
            frameDropCount.set(0)
            return currentFrameCount.toFloat()
        }
        
        return 0f
    }
    
    /**
     * Calculate average processing time
     */
    private fun calculateAverageProcessingTime(): Long {
        val count = processingTimeCount.get()
        if (count == 0L) return 0L
        
        val average = processingTimeSum.get() / count
        
        // Reset counters periodically
        if (count > 100) {
            processingTimeSum.set(0)
            processingTimeCount.set(0)
        }
        
        return average
    }
    
    /**
     * Get CPU usage percentage
     */
    private fun getCpuUsage(): Float {
        return try {
            val cpuInfoFile = File("/proc/stat")
            val appCpuInfoFile = File("/proc/${Process.myPid()}/stat")
            
            val cpuInfo = cpuInfoFile.readText().lines().firstOrNull()
            val appCpuInfo = appCpuInfoFile.readText().split(" ")
            
            if (cpuInfo != null && appCpuInfo.size > 15) {
                val cpuTimes = cpuInfo.split("\\s+".toRegex()).drop(1).take(4).map { it.toLong() }
                val currentCpuTime = cpuTimes.sum()
                
                val appUserTime = appCpuInfo[13].toLong()
                val appSystemTime = appCpuInfo[14].toLong()
                val currentAppCpuTime = appUserTime + appSystemTime
                
                val cpuTimeDiff = currentCpuTime - lastCpuTime
                val appCpuTimeDiff = currentAppCpuTime - lastAppCpuTime
                
                lastCpuTime = currentCpuTime
                lastAppCpuTime = currentAppCpuTime
                
                if (cpuTimeDiff > 0) {
                    (appCpuTimeDiff.toFloat() / cpuTimeDiff.toFloat()) * 100f
                } else {
                    0f
                }
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Get thermal state
     */
    private fun getThermalState(): ThermalState {
        return try {
            val thermalFile = File("/sys/class/thermal/thermal_zone0/temp")
            if (thermalFile.exists()) {
                val temp = thermalFile.readText().trim().toInt() / 1000 // Convert to Celsius
                when {
                    temp < 60 -> ThermalState.NORMAL
                    temp < 70 -> ThermalState.LIGHT
                    temp < 80 -> ThermalState.MODERATE
                    temp < 90 -> ThermalState.SEVERE
                    else -> ThermalState.CRITICAL
                }
            } else {
                ThermalState.NORMAL
            }
        } catch (e: Exception) {
            ThermalState.NORMAL
        }
    }
    
    /**
     * Get battery level
     */
    private fun getBatteryLevel(): Float {
        return try {
            val batteryFile = File("/sys/class/power_supply/battery/capacity")
            if (batteryFile.exists()) {
                batteryFile.readText().trim().toFloat()
            } else {
                100f
            }
        } catch (e: Exception) {
            100f
        }
    }
    
    /**
     * Get power consumption estimate
     */
    private fun getPowerConsumption(): Float {
        return try {
            val powerFile = File("/sys/class/power_supply/battery/power_now")
            if (powerFile.exists()) {
                powerFile.readText().trim().toFloat() / 1000000f // Convert to watts
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Check if system is under thermal stress
     */
    fun isUnderThermalStress(): Boolean {
        val thermalState = _performanceData.value.thermalState
        return thermalState == ThermalState.SEVERE || thermalState == ThermalState.CRITICAL
    }
    
    /**
     * Check if system is under memory pressure
     */
    fun isUnderMemoryPressure(): Boolean {
        val data = _performanceData.value
        val memoryUsagePercent = (data.memoryUsage.toFloat() / data.memoryTotal.toFloat()) * 100f
        return memoryUsagePercent > 80f
    }
    
    /**
     * Check if system is under CPU pressure
     */
    fun isUnderCpuPressure(): Boolean {
        return _performanceData.value.cpuUsage > 80f
    }
    
    /**
     * Get performance recommendation
     */
    fun getPerformanceRecommendation(): PerformanceRecommendation {
        val data = _performanceData.value
        
        return when {
            isUnderThermalStress() -> PerformanceRecommendation.REDUCE_PROCESSING_AGGRESSIVE
            isUnderMemoryPressure() -> PerformanceRecommendation.REDUCE_MEMORY_USAGE
            isUnderCpuPressure() -> PerformanceRecommendation.REDUCE_CPU_USAGE
            data.fps < 20f -> PerformanceRecommendation.REDUCE_PROCESSING_MODERATE
            data.batteryLevel < 20f -> PerformanceRecommendation.BATTERY_SAVING_MODE
            else -> PerformanceRecommendation.MAINTAIN_CURRENT
        }
    }
    
    enum class PerformanceRecommendation {
        MAINTAIN_CURRENT,
        REDUCE_PROCESSING_MODERATE,
        REDUCE_PROCESSING_AGGRESSIVE,
        REDUCE_MEMORY_USAGE,
        REDUCE_CPU_USAGE,
        BATTERY_SAVING_MODE
    }
    
    /**
     * Reset performance counters
     */
    fun resetCounters() {
        frameCount.set(0)
        frameDropCount.set(0)
        processingTimeSum.set(0)
        processingTimeCount.set(0)
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): String {
        val data = _performanceData.value
        return """
            FPS: ${String.format("%.1f", data.fps)}
            Frame Drops: ${data.frameDrops}
            Avg Processing Time: ${data.averageProcessingTime}ms
            CPU Usage: ${String.format("%.1f", data.cpuUsage)}%
            Memory Usage: ${data.memoryUsage / 1024 / 1024}MB / ${data.memoryTotal / 1024 / 1024}MB
            Thermal State: ${data.thermalState}
            Battery Level: ${String.format("%.1f", data.batteryLevel)}%
            Power Consumption: ${String.format("%.2f", data.powerConsumption)}W
        """.trimIndent()
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopMonitoring()
        monitoringScope.cancel()
    }
}