package com.golfswing.vro.pixel.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryOptimizationManager @Inject constructor(
    private val context: Context
) {
    private val optimizationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    private val _optimizationMode = MutableStateFlow(OptimizationMode.BALANCED)
    val optimizationMode: StateFlow<OptimizationMode> = _optimizationMode.asStateFlow()
    
    private val _isOptimizationEnabled = MutableStateFlow(true)
    val isOptimizationEnabled: StateFlow<Boolean> = _isOptimizationEnabled.asStateFlow()
    
    private var monitoringJob: Job? = null
    
    data class BatteryState(
        val level: Int = 100,
        val isCharging: Boolean = false,
        val temperature: Float = 0f,
        val voltage: Float = 0f,
        val health: BatteryHealth = BatteryHealth.GOOD,
        val powerSaveMode: Boolean = false,
        val thermalState: ThermalState = ThermalState.NORMAL,
        val estimatedTimeLeft: Long = 0L // in minutes
    )
    
    enum class BatteryHealth {
        UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD
    }
    
    enum class ThermalState {
        NORMAL, LIGHT, MODERATE, SEVERE, CRITICAL
    }
    
    enum class OptimizationMode {
        PERFORMANCE,    // Maximum performance, minimal optimization
        BALANCED,       // Balance between performance and battery life
        BATTERY_SAVER,  // Aggressive battery saving
        CRITICAL        // Emergency battery saving
    }
    
    data class OptimizationSettings(
        val frameRate: Int,
        val processingQuality: ProcessingQuality,
        val backgroundProcessing: Boolean,
        val cameraResolution: CameraResolution,
        val poseDetectionInterval: Long,
        val enableHapticFeedback: Boolean,
        val enableSoundFeedback: Boolean,
        val displayBrightness: Float
    )
    
    enum class ProcessingQuality {
        HIGH, MEDIUM, LOW
    }
    
    enum class CameraResolution {
        HIGH_4K, FULL_HD, HD, LOW
    }
    
    init {
        startBatteryMonitoring()
    }
    
    /**
     * Start battery monitoring
     */
    fun startBatteryMonitoring() {
        monitoringJob = optimizationScope.launch {
            while (isActive) {
                try {
                    updateBatteryState()
                    adaptOptimizationMode()
                    delay(5000) // Update every 5 seconds
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Stop battery monitoring
     */
    fun stopBatteryMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Update battery state
     */
    private fun updateBatteryState() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
            
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000f
            
            val healthStatus = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val health = when (healthStatus) {
                BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
                BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
                else -> BatteryHealth.UNKNOWN
            }
            
            val powerSaveMode = powerManager.isPowerSaveMode
            val thermalState = getThermalState()
            val estimatedTimeLeft = calculateEstimatedTimeLeft(batteryPct, isCharging)
            
            _batteryState.value = BatteryState(
                level = batteryPct,
                isCharging = isCharging,
                temperature = temperature,
                voltage = voltage,
                health = health,
                powerSaveMode = powerSaveMode,
                thermalState = thermalState,
                estimatedTimeLeft = estimatedTimeLeft
            )
        }
    }
    
    /**
     * Get thermal state
     */
    private fun getThermalState(): ThermalState {
        return try {
            val thermalFile = File("/sys/class/thermal/thermal_zone0/temp")
            if (thermalFile.exists()) {
                val temp = thermalFile.readText().trim().toInt() / 1000
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
     * Calculate estimated time left
     */
    private fun calculateEstimatedTimeLeft(batteryLevel: Int, isCharging: Boolean): Long {
        if (isCharging) return Long.MAX_VALUE
        
        // Simple estimation based on battery level and typical usage
        val baseTimePerPercent = 5L // 5 minutes per percent under normal usage
        val currentMode = _optimizationMode.value
        
        val multiplier = when (currentMode) {
            OptimizationMode.PERFORMANCE -> 0.7f
            OptimizationMode.BALANCED -> 1.0f
            OptimizationMode.BATTERY_SAVER -> 1.5f
            OptimizationMode.CRITICAL -> 2.0f
        }
        
        return (batteryLevel * baseTimePerPercent * multiplier).toLong()
    }
    
    /**
     * Adapt optimization mode based on battery state
     */
    private fun adaptOptimizationMode() {
        if (!_isOptimizationEnabled.value) return
        
        val state = _batteryState.value
        val newMode = when {
            state.level <= 10 && !state.isCharging -> OptimizationMode.CRITICAL
            state.level <= 20 && !state.isCharging -> OptimizationMode.BATTERY_SAVER
            state.powerSaveMode -> OptimizationMode.BATTERY_SAVER
            state.thermalState == ThermalState.SEVERE || state.thermalState == ThermalState.CRITICAL -> OptimizationMode.BATTERY_SAVER
            state.health == BatteryHealth.OVERHEAT -> OptimizationMode.BATTERY_SAVER
            state.temperature > 40f -> OptimizationMode.BALANCED
            state.isCharging && state.level > 80 -> OptimizationMode.PERFORMANCE
            else -> OptimizationMode.BALANCED
        }
        
        if (newMode != _optimizationMode.value) {
            _optimizationMode.value = newMode
        }
    }
    
    /**
     * Get optimization settings for current mode
     */
    fun getOptimizationSettings(): OptimizationSettings {
        return when (_optimizationMode.value) {
            OptimizationMode.PERFORMANCE -> OptimizationSettings(
                frameRate = 30,
                processingQuality = ProcessingQuality.HIGH,
                backgroundProcessing = true,
                cameraResolution = CameraResolution.FULL_HD,
                poseDetectionInterval = 33L,
                enableHapticFeedback = true,
                enableSoundFeedback = true,
                displayBrightness = 1.0f
            )
            
            OptimizationMode.BALANCED -> OptimizationSettings(
                frameRate = 24,
                processingQuality = ProcessingQuality.MEDIUM,
                backgroundProcessing = true,
                cameraResolution = CameraResolution.HD,
                poseDetectionInterval = 50L,
                enableHapticFeedback = true,
                enableSoundFeedback = true,
                displayBrightness = 0.8f
            )
            
            OptimizationMode.BATTERY_SAVER -> OptimizationSettings(
                frameRate = 15,
                processingQuality = ProcessingQuality.LOW,
                backgroundProcessing = false,
                cameraResolution = CameraResolution.LOW,
                poseDetectionInterval = 100L,
                enableHapticFeedback = false,
                enableSoundFeedback = false,
                displayBrightness = 0.5f
            )
            
            OptimizationMode.CRITICAL -> OptimizationSettings(
                frameRate = 10,
                processingQuality = ProcessingQuality.LOW,
                backgroundProcessing = false,
                cameraResolution = CameraResolution.LOW,
                poseDetectionInterval = 200L,
                enableHapticFeedback = false,
                enableSoundFeedback = false,
                displayBrightness = 0.3f
            )
        }
    }
    
    /**
     * Manually set optimization mode
     */
    fun setOptimizationMode(mode: OptimizationMode) {
        _optimizationMode.value = mode
    }
    
    /**
     * Enable/disable automatic optimization
     */
    fun setOptimizationEnabled(enabled: Boolean) {
        _isOptimizationEnabled.value = enabled
        if (!enabled) {
            _optimizationMode.value = OptimizationMode.PERFORMANCE
        }
    }
    
    /**
     * Check if device should enter battery saving mode
     */
    fun shouldEnterBatterySavingMode(): Boolean {
        val state = _batteryState.value
        return state.level <= 20 && !state.isCharging
    }
    
    /**
     * Check if device should reduce performance due to thermal state
     */
    fun shouldReducePerformanceForThermal(): Boolean {
        val state = _batteryState.value
        return state.thermalState == ThermalState.SEVERE || 
               state.thermalState == ThermalState.CRITICAL ||
               state.temperature > 45f
    }
    
    /**
     * Get battery optimization recommendations
     */
    fun getBatteryOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val state = _batteryState.value
        
        when {
            state.level <= 10 -> {
                recommendations.add("Critical battery level - Switch to emergency mode")
                recommendations.add("Reduce frame rate to 10 FPS")
                recommendations.add("Disable background processing")
                recommendations.add("Lower camera resolution")
            }
            state.level <= 20 -> {
                recommendations.add("Low battery - Enable battery saver mode")
                recommendations.add("Reduce frame rate to 15 FPS")
                recommendations.add("Disable haptic feedback")
                recommendations.add("Lower display brightness")
            }
            state.thermalState == ThermalState.SEVERE -> {
                recommendations.add("Device overheating - Reduce processing load")
                recommendations.add("Lower frame rate and resolution")
                recommendations.add("Pause processing temporarily")
            }
            state.temperature > 40f -> {
                recommendations.add("Device warming up - Consider reducing performance")
                recommendations.add("Enable adaptive frame rate")
            }
        }
        
        if (state.powerSaveMode) {
            recommendations.add("System power save mode active - Optimize for battery life")
        }
        
        return recommendations
    }
    
    /**
     * Get battery health status
     */
    fun getBatteryHealthStatus(): String {
        val state = _batteryState.value
        return when (state.health) {
            BatteryHealth.GOOD -> "Good"
            BatteryHealth.OVERHEAT -> "Overheating"
            BatteryHealth.DEAD -> "Dead"
            BatteryHealth.OVER_VOLTAGE -> "Over Voltage"
            BatteryHealth.UNSPECIFIED_FAILURE -> "Failure"
            BatteryHealth.COLD -> "Cold"
            BatteryHealth.UNKNOWN -> "Unknown"
        }
    }
    
    /**
     * Get formatted battery info
     */
    fun getBatteryInfo(): String {
        val state = _batteryState.value
        return """
            Battery Level: ${state.level}%
            Status: ${if (state.isCharging) "Charging" else "Discharging"}
            Temperature: ${String.format("%.1f", state.temperature)}°C
            Voltage: ${String.format("%.2f", state.voltage)}V
            Health: ${getBatteryHealthStatus()}
            Power Save Mode: ${if (state.powerSaveMode) "Enabled" else "Disabled"}
            Thermal State: ${state.thermalState}
            Estimated Time Left: ${if (state.estimatedTimeLeft == Long.MAX_VALUE) "∞" else "${state.estimatedTimeLeft} min"}
            Current Mode: ${_optimizationMode.value}
        """.trimIndent()
    }
    
    /**
     * Force update battery state
     */
    fun forceUpdateBatteryState() {
        optimizationScope.launch {
            updateBatteryState()
            adaptOptimizationMode()
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopBatteryMonitoring()
        optimizationScope.cancel()
    }
}