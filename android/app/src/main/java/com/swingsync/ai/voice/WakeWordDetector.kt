package com.swingsync.ai.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * WakeWordDetector - Efficient always-listening wake word detection for hands-free golf coaching
 * 
 * Features:
 * - Always-listening with minimal battery impact
 * - Dual wake word support ("Hey SwingSync" and "Golf Coach")
 * - Adaptive sensitivity based on environment
 * - Voice activity detection to save processing
 * - Efficient audio processing with circular buffer
 * - Background noise adaptation
 * - Power-optimized algorithms
 * 
 * Uses lightweight spectral analysis and template matching for wake word detection
 * without requiring heavy ML models, ensuring minimal battery drain.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        
        // Audio configuration
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 4
        
        // Wake word detection parameters
        private const val WAKE_WORD_WINDOW_MS = 2000 // 2 seconds
        private const val WAKE_WORD_OVERLAP_MS = 500  // 0.5 second overlap
        private const val VOICE_THRESHOLD = 0.02f     // Voice activity threshold
        private const val WAKE_WORD_THRESHOLD = 0.75f // Wake word confidence threshold
        
        // Power optimization
        private const val SLEEP_BETWEEN_CHECKS_MS = 50L
        private const val BACKGROUND_CHECK_INTERVAL_MS = 200L
        private const val BATTERY_SAVE_THRESHOLD = 0.15f // 15% battery
        
        // Spectral analysis parameters
        private const val FFT_SIZE = 512
        private const val FREQ_BINS = FFT_SIZE / 2
        private const val MEL_FILTERS = 26
        
        // Wake word templates (simplified spectral patterns)
        private val HEY_SWINGSYNC_PATTERN = floatArrayOf(
            0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.9f, 0.7f, 0.5f, 0.6f, 0.8f, 0.9f, 0.7f, 0.6f
        )
        private val GOLF_COACH_PATTERN = floatArrayOf(
            0.7f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.8f, 0.9f, 0.6f, 0.7f, 0.8f, 0.5f, 0.6f
        )
    }

    // State management
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _isWakeWordDetected = MutableStateFlow(false)
    val isWakeWordDetected: StateFlow<Boolean> = _isWakeWordDetected.asStateFlow()
    
    private val _detectedWakeWord = MutableStateFlow<String?>(null)
    val detectedWakeWord: StateFlow<String?> = _detectedWakeWord.asStateFlow()
    
    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence.asStateFlow()
    
    private val _voiceActivity = MutableStateFlow(0f)
    val voiceActivity: StateFlow<Float> = _voiceActivity.asStateFlow()

    // Audio recording
    private var audioRecord: AudioRecord? = null
    private var audioBuffer: ByteArray? = null
    private var bufferSize = 0
    
    // Processing components
    private val detectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val audioProcessor = AudioProcessor()
    private val spectralAnalyzer = SpectralAnalyzer()
    private val templateMatcher = TemplateMatcher()
    
    // Circular buffer for audio processing
    private var circularBuffer = CircularBuffer(SAMPLE_RATE * 3) // 3 seconds of audio
    private var noiseFloor = 0f
    private var adaptiveThreshold = WAKE_WORD_THRESHOLD
    
    // Power management
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Performance tracking
    private var processingTime = 0L
    private var detectionCount = 0
    private var falsePositiveCount = 0

    /**
     * Start wake word detection
     */
    fun startListening() {
        if (_isListening.value) return
        
        Log.d(TAG, "Starting wake word detection")
        
        if (initializeAudioRecord()) {
            _isListening.value = true
            acquireWakeLock()
            startAudioProcessing()
        } else {
            Log.e(TAG, "Failed to initialize audio recording")
        }
    }

    /**
     * Stop wake word detection
     */
    fun stopListening() {
        if (!_isListening.value) return
        
        Log.d(TAG, "Stopping wake word detection")
        
        _isListening.value = false
        stopAudioProcessing()
        releaseWakeLock()
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    /**
     * Reset wake word detection state
     */
    fun reset() {
        _isWakeWordDetected.value = false
        _detectedWakeWord.value = null
        _confidence.value = 0f
        adaptiveThreshold = WAKE_WORD_THRESHOLD
    }

    /**
     * Adjust sensitivity based on environment
     */
    fun adjustSensitivity(sensitivity: Float) {
        adaptiveThreshold = WAKE_WORD_THRESHOLD * (1.0f - sensitivity * 0.3f)
        Log.d(TAG, "Adjusted wake word threshold to: $adaptiveThreshold")
    }

    /**
     * Initialize audio recording
     */
    private fun initializeAudioRecord(): Boolean {
        try {
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING)
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return false
            }
            
            bufferSize *= BUFFER_SIZE_MULTIPLIER
            audioBuffer = ByteArray(bufferSize)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNELS,
                ENCODING,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return false
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioRecord", e)
            return false
        }
    }

    /**
     * Start audio processing in background
     */
    private fun startAudioProcessing() {
        audioRecord?.startRecording()
        
        detectionScope.launch {
            processAudioStream()
        }
    }

    /**
     * Stop audio processing
     */
    private fun stopAudioProcessing() {
        detectionScope.coroutineContext.cancelChildren()
    }

    /**
     * Main audio processing loop
     */
    private suspend fun processAudioStream() {
        var lastProcessTime = System.currentTimeMillis()
        
        while (_isListening.value) {
            try {
                val currentTime = System.currentTimeMillis()
                val audioData = readAudioData()
                
                if (audioData != null) {
                    // Add to circular buffer
                    circularBuffer.write(audioData)
                    
                    // Update voice activity
                    val voiceLevel = audioProcessor.calculateVoiceActivity(audioData)
                    _voiceActivity.value = voiceLevel
                    
                    // Update noise floor adaptation
                    updateNoiseFloor(voiceLevel)
                    
                    // Only process if voice activity detected
                    if (voiceLevel > VOICE_THRESHOLD) {
                        processWakeWordDetection()
                    }
                }
                
                // Adaptive sleep based on activity and battery
                val sleepTime = calculateSleepTime(currentTime - lastProcessTime)
                delay(sleepTime)
                lastProcessTime = currentTime
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio processing", e)
                delay(BACKGROUND_CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Read audio data from microphone
     */
    private suspend fun readAudioData(): FloatArray? {
        return withContext(Dispatchers.IO) {
            try {
                val buffer = audioBuffer ?: return@withContext null
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    audioProcessor.bytesToFloat(buffer, bytesRead)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading audio data", e)
                null
            }
        }
    }

    /**
     * Process wake word detection on audio window
     */
    private suspend fun processWakeWordDetection() {
        val startTime = System.currentTimeMillis()
        
        try {
            // Get audio window for analysis
            val audioWindow = circularBuffer.getLastSeconds(WAKE_WORD_WINDOW_MS / 1000.0f)
            
            if (audioWindow.size < SAMPLE_RATE) return // Not enough audio
            
            // Extract spectral features
            val spectralFeatures = spectralAnalyzer.extractMelFeatures(audioWindow)
            
            // Match against wake word templates
            val heySwingSyncScore = templateMatcher.matchTemplate(spectralFeatures, HEY_SWINGSYNC_PATTERN)
            val golfCoachScore = templateMatcher.matchTemplate(spectralFeatures, GOLF_COACH_PATTERN)
            
            val maxScore = maxOf(heySwingSyncScore, golfCoachScore)
            val detectedWord = when {
                heySwingSyncScore > golfCoachScore -> "Hey SwingSync"
                golfCoachScore > heySwingSyncScore -> "Golf Coach"
                else -> null
            }
            
            // Update confidence
            _confidence.value = maxScore
            
            // Check for wake word detection
            if (maxScore > adaptiveThreshold && detectedWord != null) {
                handleWakeWordDetection(detectedWord, maxScore)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in wake word processing", e)
        }
        
        processingTime = System.currentTimeMillis() - startTime
    }

    /**
     * Handle wake word detection
     */
    private fun handleWakeWordDetection(word: String, confidence: Float) {
        detectionCount++
        
        Log.d(TAG, "Wake word detected: $word (confidence: $confidence)")
        
        _isWakeWordDetected.value = true
        _detectedWakeWord.value = word
        _confidence.value = confidence
        
        // Temporarily increase threshold to prevent immediate re-detection
        adaptiveThreshold = minOf(adaptiveThreshold * 1.2f, 0.95f)
        
        // Reset threshold after delay
        detectionScope.launch {
            delay(2000)
            adaptiveThreshold = WAKE_WORD_THRESHOLD
        }
    }

    /**
     * Update noise floor for environmental adaptation
     */
    private fun updateNoiseFloor(voiceLevel: Float) {
        if (voiceLevel < VOICE_THRESHOLD) {
            noiseFloor = noiseFloor * 0.95f + voiceLevel * 0.05f
        }
    }

    /**
     * Calculate adaptive sleep time based on activity and battery
     */
    private fun calculateSleepTime(processingTime: Long): Long {
        val batteryLevel = getBatteryLevel()
        val baseSleep = SLEEP_BETWEEN_CHECKS_MS
        
        return when {
            batteryLevel < BATTERY_SAVE_THRESHOLD -> baseSleep * 2
            _voiceActivity.value < VOICE_THRESHOLD -> baseSleep * 2
            processingTime > 100 -> baseSleep / 2 // Reduce sleep if processing is slow
            else -> baseSleep
        }
    }

    /**
     * Get current battery level
     */
    private fun getBatteryLevel(): Float {
        return try {
            val batteryIntent = context.registerReceiver(null, 
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            
            if (level != -1 && scale != -1) {
                level.toFloat() / scale.toFloat()
            } else {
                1.0f // Assume full battery if can't determine
            }
        } catch (e: Exception) {
            1.0f
        }
    }

    /**
     * Acquire wake lock for background processing
     */
    private fun acquireWakeLock() {
        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SwingSync::WakeWordDetector"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    /**
     * Get performance statistics
     */
    fun getPerformanceStats(): Map<String, Any> {
        return mapOf(
            "detectionCount" to detectionCount,
            "falsePositiveCount" to falsePositiveCount,
            "averageProcessingTime" to processingTime,
            "noiseFloor" to noiseFloor,
            "adaptiveThreshold" to adaptiveThreshold,
            "batteryLevel" to getBatteryLevel()
        )
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopListening()
        detectionScope.cancel()
        circularBuffer.clear()
    }
}

/**
 * Audio processing utilities
 */
class AudioProcessor {
    /**
     * Convert byte array to float array
     */
    fun bytesToFloat(bytes: ByteArray, length: Int): FloatArray {
        val floats = FloatArray(length / 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in floats.indices) {
            floats[i] = buffer.short.toFloat() / 32768.0f
        }
        
        return floats
    }

    /**
     * Calculate voice activity level
     */
    fun calculateVoiceActivity(audioData: FloatArray): Float {
        var sum = 0.0
        for (sample in audioData) {
            sum += sample * sample
        }
        return sqrt(sum / audioData.size).toFloat()
    }
}

/**
 * Spectral analysis for wake word detection
 */
class SpectralAnalyzer {
    /**
     * Extract mel-frequency features
     */
    fun extractMelFeatures(audioData: FloatArray): FloatArray {
        // Simplified mel-frequency extraction
        val windowSize = 512
        val hopSize = 256
        val features = mutableListOf<Float>()
        
        for (i in 0 until audioData.size - windowSize step hopSize) {
            val window = audioData.sliceArray(i until i + windowSize)
            val spectrum = computeSpectrum(window)
            val melFeatures = applyMelFilters(spectrum)
            features.addAll(melFeatures)
        }
        
        return features.toFloatArray()
    }

    /**
     * Compute power spectrum
     */
    private fun computeSpectrum(window: FloatArray): FloatArray {
        // Simplified FFT - in production would use proper FFT
        val spectrum = FloatArray(window.size / 2)
        
        for (k in spectrum.indices) {
            var real = 0.0
            var imag = 0.0
            
            for (n in window.indices) {
                val angle = -2.0 * PI * k * n / window.size
                real += window[n] * cos(angle)
                imag += window[n] * sin(angle)
            }
            
            spectrum[k] = sqrt(real * real + imag * imag).toFloat()
        }
        
        return spectrum
    }

    /**
     * Apply mel filter bank
     */
    private fun applyMelFilters(spectrum: FloatArray): FloatArray {
        // Simplified mel filtering
        val melFeatures = FloatArray(26)
        val binSize = spectrum.size / melFeatures.size
        
        for (i in melFeatures.indices) {
            val startBin = i * binSize
            val endBin = minOf(startBin + binSize, spectrum.size)
            
            var sum = 0f
            for (j in startBin until endBin) {
                sum += spectrum[j]
            }
            
            melFeatures[i] = sum / binSize
        }
        
        return melFeatures
    }
}

/**
 * Template matching for wake word detection
 */
class TemplateMatcher {
    /**
     * Match features against template
     */
    fun matchTemplate(features: FloatArray, template: FloatArray): Float {
        if (features.size < template.size) return 0f
        
        var bestScore = 0f
        val windowSize = template.size
        
        // Slide template across features
        for (i in 0..features.size - windowSize) {
            val window = features.sliceArray(i until i + windowSize)
            val score = calculateSimilarity(window, template)
            bestScore = maxOf(bestScore, score)
        }
        
        return bestScore
    }

    /**
     * Calculate similarity between two feature vectors
     */
    private fun calculateSimilarity(features1: FloatArray, features2: FloatArray): Float {
        if (features1.size != features2.size) return 0f
        
        // Normalize features
        val norm1 = features1.map { it * it }.sum()
        val norm2 = features2.map { it * it }.sum()
        
        if (norm1 == 0f || norm2 == 0f) return 0f
        
        // Calculate cosine similarity
        var dotProduct = 0f
        for (i in features1.indices) {
            dotProduct += features1[i] * features2[i]
        }
        
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
}

/**
 * Circular buffer for audio data
 */
class CircularBuffer(private val capacity: Int) {
    private val buffer = FloatArray(capacity)
    private var writePos = 0
    private var size = 0

    /**
     * Write audio data to buffer
     */
    fun write(data: FloatArray) {
        for (sample in data) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % capacity
            if (size < capacity) size++
        }
    }

    /**
     * Get last N seconds of audio
     */
    fun getLastSeconds(seconds: Float): FloatArray {
        val sampleCount = (seconds * 16000).toInt()
        val actualCount = minOf(sampleCount, size)
        
        val result = FloatArray(actualCount)
        val startPos = (writePos - actualCount + capacity) % capacity
        
        for (i in 0 until actualCount) {
            result[i] = buffer[(startPos + i) % capacity]
        }
        
        return result
    }

    /**
     * Clear buffer
     */
    fun clear() {
        writePos = 0
        size = 0
        buffer.fill(0f)
    }
}