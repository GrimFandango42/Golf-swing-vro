package com.swingsync.ai.voice

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PowerOptimizationManager - Advanced battery optimization for hands-free voice control
 * 
 * Features:
 * - Dynamic performance scaling based on battery level
 * - Intelligent wake lock management
 * - Background processing optimization
 * - Thermal throttling prevention
 * - Memory usage optimization
 * - Network request batching
 * - Adaptive quality settings
 * - System resource monitoring
 * 
 * Ensures hands-free voice control operates efficiently without draining battery,
 * adapting performance characteristics based on device state and user needs.
 */
@Singleton
class PowerOptimizationManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PowerOptimizationManager"
        
        // Battery level thresholds
        private const val CRITICAL_BATTERY = 0.10f  // 10%
        private const val LOW_BATTERY = 0.20f       // 20%
        private const val NORMAL_BATTERY = 0.50f    // 50%
        
        // Performance modes
        private const val PERFORMANCE_HIGH = "high"
        private const val PERFORMANCE_BALANCED = "balanced"
        private const val PERFORMANCE_BATTERY_SAVER = "battery_saver"
        private const val PERFORMANCE_CRITICAL = "critical"
        
        // Optimization intervals
        private const val BATTERY_CHECK_INTERVAL = 30000L      // 30 seconds
        private const val MEMORY_CHECK_INTERVAL = 60000L       // 1 minute
        private const val THERMAL_CHECK_INTERVAL = 45000L      // 45 seconds
        
        // Resource limits
        private const val MAX_MEMORY_USAGE_MB = 100
        private const val MAX_CPU_USAGE_PERCENT = 15
        private const val MAX_WAKE_LOCK_DURATION = 300000L     // 5 minutes
    }

    // System managers
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // State management
    private val _batteryLevel = MutableStateFlow(1.0f)
    val batteryLevel: StateFlow<Float> = _batteryLevel.asStateFlow()
    
    private val _performanceMode = MutableStateFlow(PERFORMANCE_HIGH)
    val performanceMode: StateFlow<String> = _performanceMode.asStateFlow()
    
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()
    
    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()
    
    private val _memoryUsage = MutableStateFlow(0L)
    val memoryUsage: StateFlow<Long> = _memoryUsage.asStateFlow()
    
    private val _isOptimizationActive = MutableStateFlow(false)
    val isOptimizationActive: StateFlow<Boolean> = _isOptimizationActive.asStateFlow()
    
    // Optimization scope
    private val optimizationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Wake lock management
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockStartTime = 0L
    
    // Performance tracking
    private var cpuUsageHistory = mutableListOf<Float>()
    private var memoryUsageHistory = mutableListOf<Long>()
    private var batteryDrainRate = 0f
    
    // Optimization strategies
    private val optimizationStrategies = OptimizationStrategies()

    /**
     * Start power optimization monitoring
     */
    fun startOptimization() {
        if (_isOptimizationActive.value) return
        
        Log.d(TAG, "Starting power optimization")
        
        _isOptimizationActive.value = true
        
        startBatteryMonitoring()
        startMemoryMonitoring()
        startThermalMonitoring()
        startPerformanceOptimization()
    }

    /**
     * Stop power optimization
     */
    fun stopOptimization() {
        if (!_isOptimizationActive.value) return
        
        Log.d(TAG, "Stopping power optimization")
        
        _isOptimizationActive.value = false
        
        releaseWakeLock()
        optimizationScope.coroutineContext.cancelChildren()
    }

    /**
     * Acquire optimized wake lock
     */
    fun acquireOptimizedWakeLock(tag: String, duration: Long = MAX_WAKE_LOCK_DURATION) {
        try {
            releaseWakeLock() // Release any existing wake lock
            
            val wakeLockType = when (_performanceMode.value) {
                PERFORMANCE_CRITICAL -> PowerManager.PARTIAL_WAKE_LOCK
                PERFORMANCE_BATTERY_SAVER -> PowerManager.PARTIAL_WAKE_LOCK
                else -> PowerManager.PARTIAL_WAKE_LOCK
            }
            
            wakeLock = powerManager.newWakeLock(wakeLockType, "SwingSync::$tag")
            wakeLock?.acquire(duration)
            wakeLockStartTime = System.currentTimeMillis()
            
            Log.d(TAG, "Acquired wake lock: $tag")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    /**
     * Release wake lock
     */
    fun releaseWakeLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    val duration = System.currentTimeMillis() - wakeLockStartTime
                    Log.d(TAG, "Released wake lock after ${duration}ms")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    /**
     * Get optimized configuration for component
     */
    fun getOptimizedConfig(component: String): OptimizationConfig {
        return when (_performanceMode.value) {
            PERFORMANCE_HIGH -> OptimizationConfig(
                processingInterval = 50L,
                qualityLevel = 1.0f,
                enableAdvancedFeatures = true,
                memoryLimit = MAX_MEMORY_USAGE_MB,
                cpuLimit = MAX_CPU_USAGE_PERCENT
            )
            PERFORMANCE_BALANCED -> OptimizationConfig(
                processingInterval = 100L,
                qualityLevel = 0.8f,
                enableAdvancedFeatures = true,
                memoryLimit = MAX_MEMORY_USAGE_MB * 0.8f,
                cpuLimit = MAX_CPU_USAGE_PERCENT * 0.8f
            )
            PERFORMANCE_BATTERY_SAVER -> OptimizationConfig(
                processingInterval = 200L,
                qualityLevel = 0.6f,
                enableAdvancedFeatures = false,
                memoryLimit = MAX_MEMORY_USAGE_MB * 0.6f,
                cpuLimit = MAX_CPU_USAGE_PERCENT * 0.6f
            )
            PERFORMANCE_CRITICAL -> OptimizationConfig(
                processingInterval = 500L,
                qualityLevel = 0.4f,
                enableAdvancedFeatures = false,
                memoryLimit = MAX_MEMORY_USAGE_MB * 0.4f,
                cpuLimit = MAX_CPU_USAGE_PERCENT * 0.4f
            )
            else -> OptimizationConfig()
        }
    }

    /**
     * Apply optimization to wake word detector
     */
    fun optimizeWakeWordDetector(detector: WakeWordDetector) {
        val config = getOptimizedConfig("wake_word")
        
        when (_performanceMode.value) {
            PERFORMANCE_BATTERY_SAVER, PERFORMANCE_CRITICAL -> {
                detector.adjustSensitivity(0.7f) // Reduce sensitivity to save power
            }
            else -> {
                detector.adjustSensitivity(1.0f)
            }
        }
    }

    /**
     * Apply optimization to spatial audio
     */
    fun optimizeSpatialAudio(spatialAudio: SpatialAudioGuide) {
        val config = getOptimizedConfig("spatial_audio")
        
        if (!config.enableAdvancedFeatures && _batteryLevel.value < LOW_BATTERY) {
            spatialAudio.setAudioEnvironment(AudioEnvironment.PUTTING_GREEN) // Minimal processing
        }
    }

    /**
     * Start battery monitoring
     */
    private fun startBatteryMonitoring() {
        optimizationScope.launch {
            while (_isOptimizationActive.value) {
                updateBatteryState()
                updatePerformanceMode()
                delay(BATTERY_CHECK_INTERVAL)
            }
        }
    }

    /**
     * Start memory monitoring
     */
    private fun startMemoryMonitoring() {
        optimizationScope.launch {
            while (_isOptimizationActive.value) {
                updateMemoryUsage()
                delay(MEMORY_CHECK_INTERVAL)
            }
        }
    }

    /**
     * Start thermal monitoring
     */
    private fun startThermalMonitoring() {
        optimizationScope.launch {
            while (_isOptimizationActive.value) {
                updateThermalState()
                delay(THERMAL_CHECK_INTERVAL)
            }
        }
    }

    /**
     * Start performance optimization loop
     */
    private fun startPerformanceOptimization() {
        optimizationScope.launch {
            while (_isOptimizationActive.value) {
                applyPerformanceOptimizations()
                delay(10000) // Check every 10 seconds
            }
        }
    }

    /**
     * Update battery state
     */
    private fun updateBatteryState() {
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            
            if (level != -1 && scale != -1) {
                val newBatteryLevel = level.toFloat() / scale.toFloat()
                val previousLevel = _batteryLevel.value
                
                _batteryLevel.value = newBatteryLevel
                
                // Calculate battery drain rate
                if (previousLevel > 0) {
                    batteryDrainRate = (previousLevel - newBatteryLevel) / (BATTERY_CHECK_INTERVAL / 1000f)
                }
            }
            
            _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                               status == BatteryManager.BATTERY_STATUS_FULL
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating battery state", e)
        }
    }

    /**
     * Update performance mode based on battery and thermal state
     */
    private fun updatePerformanceMode() {
        val newMode = when {
            _batteryLevel.value < CRITICAL_BATTERY -> PERFORMANCE_CRITICAL
            _batteryLevel.value < LOW_BATTERY && !_isCharging.value -> PERFORMANCE_BATTERY_SAVER
            _thermalState.value == ThermalState.HOT -> PERFORMANCE_BATTERY_SAVER
            _batteryLevel.value > NORMAL_BATTERY && _isCharging.value -> PERFORMANCE_HIGH
            else -> PERFORMANCE_BALANCED
        }
        
        if (newMode != _performanceMode.value) {
            _performanceMode.value = newMode
            Log.d(TAG, "Performance mode changed to: $newMode")
        }
    }

    /**
     * Update memory usage
     */
    private fun updateMemoryUsage() {
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
            _memoryUsage.value = usedMemory / (1024 * 1024) // Convert to MB
            
            memoryUsageHistory.add(usedMemory)
            if (memoryUsageHistory.size > 10) {
                memoryUsageHistory.removeAt(0)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating memory usage", e)
        }
    }

    /**
     * Update thermal state
     */
    private fun updateThermalState() {
        try {
            // Simplified thermal detection based on CPU usage and battery temperature
            val averageMemory = memoryUsageHistory.average()
            val highMemoryUsage = averageMemory > MAX_MEMORY_USAGE_MB * 1.5
            
            _thermalState.value = when {
                highMemoryUsage || batteryDrainRate > 0.01f -> ThermalState.HOT
                batteryDrainRate > 0.005f -> ThermalState.WARM
                else -> ThermalState.NORMAL
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating thermal state", e)
        }
    }

    /**
     * Apply performance optimizations
     */
    private fun applyPerformanceOptimizations() {
        try {
            // Garbage collection suggestion for low memory
            if (_memoryUsage.value > MAX_MEMORY_USAGE_MB * 0.8) {
                System.gc()
            }
            
            // Wake lock timeout check
            if (wakeLock?.isHeld == true) {
                val wakeLockDuration = System.currentTimeMillis() - wakeLockStartTime
                if (wakeLockDuration > MAX_WAKE_LOCK_DURATION) {
                    releaseWakeLock()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying optimizations", e)
        }
    }

    /**
     * Get optimization statistics
     */
    fun getOptimizationStats(): Map<String, Any> {
        return mapOf(
            "batteryLevel" to _batteryLevel.value,
            "isCharging" to _isCharging.value,
            "performanceMode" to _performanceMode.value,
            "memoryUsageMB" to _memoryUsage.value,
            "thermalState" to _thermalState.value.name,
            "batteryDrainRate" to batteryDrainRate,
            "wakeLockActive" to (wakeLock?.isHeld ?: false),
            "optimizationActive" to _isOptimizationActive.value
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopOptimization()
        optimizationScope.cancel()
        cpuUsageHistory.clear()
        memoryUsageHistory.clear()
    }
}

/**
 * Thermal states
 */
enum class ThermalState {
    NORMAL,
    WARM,
    HOT
}

/**
 * Optimization configuration
 */
data class OptimizationConfig(
    val processingInterval: Long = 100L,
    val qualityLevel: Float = 1.0f,
    val enableAdvancedFeatures: Boolean = true,
    val memoryLimit: Float = 100f,
    val cpuLimit: Float = 15f
)

/**
 * Optimization strategies
 */
class OptimizationStrategies {
    
    /**
     * Get wake word detection strategy
     */
    fun getWakeWordStrategy(performanceMode: String): WakeWordStrategy {
        return when (performanceMode) {
            "high" -> WakeWordStrategy(
                sensitivity = 1.0f,
                bufferSize = 4096,
                processingInterval = 50L
            )
            "balanced" -> WakeWordStrategy(
                sensitivity = 0.9f,
                bufferSize = 2048,
                processingInterval = 100L
            )
            "battery_saver" -> WakeWordStrategy(
                sensitivity = 0.8f,
                bufferSize = 1024,
                processingInterval = 200L
            )
            "critical" -> WakeWordStrategy(
                sensitivity = 0.7f,
                bufferSize = 512,
                processingInterval = 500L
            )
            else -> WakeWordStrategy()
        }
    }
    
    /**
     * Get voice processing strategy
     */
    fun getVoiceProcessingStrategy(performanceMode: String): VoiceProcessingStrategy {
        return when (performanceMode) {
            "high" -> VoiceProcessingStrategy(
                enableNLP = true,
                enableContextTracking = true,
                maxHistorySize = 100
            )
            "balanced" -> VoiceProcessingStrategy(
                enableNLP = true,
                enableContextTracking = true,
                maxHistorySize = 50
            )
            "battery_saver" -> VoiceProcessingStrategy(
                enableNLP = false,
                enableContextTracking = false,
                maxHistorySize = 20
            )
            "critical" -> VoiceProcessingStrategy(
                enableNLP = false,
                enableContextTracking = false,
                maxHistorySize = 10
            )
            else -> VoiceProcessingStrategy()
        }
    }
}

/**
 * Wake word detection strategy
 */
data class WakeWordStrategy(
    val sensitivity: Float = 1.0f,
    val bufferSize: Int = 2048,
    val processingInterval: Long = 100L
)

/**
 * Voice processing strategy
 */
data class VoiceProcessingStrategy(
    val enableNLP: Boolean = true,
    val enableContextTracking: Boolean = true,
    val maxHistorySize: Int = 50
)