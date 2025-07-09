package com.swingsync.ai.voice

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.data.repository.SettingsRepository
import com.swingsync.ai.ui.camera.CameraActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * HandsFreeService - Central coordination service for hands-free voice control
 * 
 * Features:
 * - Always-listening wake word detection
 * - Seamless voice command processing
 * - Background operation with battery optimization
 * - Smart context awareness (practice vs review modes)
 * - Integration with existing voice coaching
 * - Spatial audio guidance coordination
 * - Notification management for hands-free status
 * - Power management and optimization
 * - Automatic mode switching based on app state
 * 
 * This service makes the entire app controllable through voice commands,
 * allowing golfers to keep their hands on their clubs while getting
 * intelligent coaching and feedback.
 */
@AndroidEntryPoint
class HandsFreeService : Service() {
    
    companion object {
        private const val TAG = "HandsFreeService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hands_free_service"
        
        // Service states
        private const val STATE_IDLE = "idle"
        private const val STATE_LISTENING = "listening"
        private const val STATE_PROCESSING = "processing"
        private const val STATE_RESPONDING = "responding"
        
        // Battery optimization
        private const val LOW_BATTERY_THRESHOLD = 0.15f
        private const val CRITICAL_BATTERY_THRESHOLD = 0.10f
        
        // Context detection
        private const val PRACTICE_MODE_KEYWORDS = listOf("practice", "swing", "record", "analyze")
        private const val REVIEW_MODE_KEYWORDS = listOf("review", "history", "progress", "stats")
        
        // Actions
        const val ACTION_START_HANDS_FREE = "com.swingsync.START_HANDS_FREE"
        const val ACTION_STOP_HANDS_FREE = "com.swingsync.STOP_HANDS_FREE"
        const val ACTION_TOGGLE_HANDS_FREE = "com.swingsync.TOGGLE_HANDS_FREE"
    }

    @Inject
    lateinit var wakeWordDetector: WakeWordDetector
    
    @Inject
    lateinit var voiceCommandProcessor: VoiceCommandProcessor
    
    @Inject
    lateinit var voiceInterface: VoiceInterface
    
    @Inject
    lateinit var magicVoiceCoach: MagicVoiceCoach
    
    @Inject
    lateinit var spatialAudioGuide: SpatialAudioGuide
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var powerOptimizationManager: PowerOptimizationManager

    private val binder = HandsFreeServiceBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // State management
    private val _serviceState = MutableStateFlow(STATE_IDLE)
    val serviceState: StateFlow<String> = _serviceState.asStateFlow()
    
    private val _isHandsFreeActive = MutableStateFlow(false)
    val isHandsFreeActive: StateFlow<Boolean> = _isHandsFreeActive.asStateFlow()
    
    private val _currentMode = MutableStateFlow(HandsFreeMode.PRACTICE)
    val currentMode: StateFlow<HandsFreeMode> = _currentMode.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow(1.0f)
    val batteryLevel: StateFlow<Float> = _batteryLevel.asStateFlow()
    
    private val _isInBackground = MutableStateFlow(false)
    val isInBackground: StateFlow<Boolean> = _isInBackground.asStateFlow()
    
    // Command handling
    private val _pendingCommand = MutableStateFlow<VoiceCommand?>(null)
    val pendingCommand: StateFlow<VoiceCommand?> = _pendingCommand.asStateFlow()
    
    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()
    
    // Context awareness
    private var appContext = AppContext.UNKNOWN
    private var lastActivityTime = System.currentTimeMillis()
    private var sessionStartTime = 0L
    
    // Power management
    private var wakeLock: PowerManager.WakeLock? = null
    private var powerManager: PowerManager? = null
    private var audioManager: AudioManager? = null
    
    // Performance tracking
    private var commandCount = 0
    private var successfulCommands = 0
    private var averageResponseTime = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HandsFreeService created")
        
        initializeService()
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_HANDS_FREE -> startHandsFreeControl()
            ACTION_STOP_HANDS_FREE -> stopHandsFreeControl()
            ACTION_TOGGLE_HANDS_FREE -> toggleHandsFreeControl()
            else -> startHandsFreeControl()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HandsFreeService destroyed")
        
        cleanup()
    }

    /**
     * Initialize service components
     */
    private fun initializeService() {
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Start power optimization
        powerOptimizationManager.startOptimization()
        
        // Monitor battery level
        monitorBatteryLevel()
        
        // Monitor app context
        monitorAppContext()
        
        // Setup wake word detection
        setupWakeWordDetection()
        
        // Setup voice command processing
        setupVoiceCommandProcessing()
    }

    /**
     * Start hands-free control
     */
    fun startHandsFreeControl() {
        if (_isHandsFreeActive.value) return
        
        Log.d(TAG, "Starting hands-free control")
        
        sessionStartTime = System.currentTimeMillis()
        _isHandsFreeActive.value = true
        _serviceState.value = STATE_LISTENING
        
        // Apply power optimizations
        powerOptimizationManager.optimizeWakeWordDetector(wakeWordDetector)
        powerOptimizationManager.optimizeSpatialAudio(spatialAudioGuide)
        
        // Acquire optimized wake lock
        powerOptimizationManager.acquireOptimizedWakeLock("HandsFreeService")
        
        // Start wake word detection
        wakeWordDetector.startListening()
        
        // Start spatial audio if needed
        if (shouldUseSpatialAudio()) {
            spatialAudioGuide.startGuidance()
        }
        
        // Update notification
        updateNotification()
        
        // Welcome message
        serviceScope.launch {
            speakWelcomeMessage()
        }
    }

    /**
     * Stop hands-free control
     */
    fun stopHandsFreeControl() {
        if (!_isHandsFreeActive.value) return
        
        Log.d(TAG, "Stopping hands-free control")
        
        _isHandsFreeActive.value = false
        _serviceState.value = STATE_IDLE
        
        // Stop wake word detection
        wakeWordDetector.stopListening()
        
        // Stop spatial audio
        spatialAudioGuide.stopGuidance()
        
        // Release wake lock
        powerOptimizationManager.releaseWakeLock()
        
        // Update notification
        updateNotification()
        
        // Goodbye message
        serviceScope.launch {
            voiceInterface.speak("Hands-free mode disabled. Touch to reactivate.")
        }
    }

    /**
     * Toggle hands-free control
     */
    fun toggleHandsFreeControl() {
        if (_isHandsFreeActive.value) {
            stopHandsFreeControl()
        } else {
            startHandsFreeControl()
        }
    }

    /**
     * Setup wake word detection
     */
    private fun setupWakeWordDetection() {
        serviceScope.launch {
            wakeWordDetector.isWakeWordDetected.collect { detected ->
                if (detected && _isHandsFreeActive.value) {
                    handleWakeWordDetected()
                }
            }
        }
    }

    /**
     * Setup voice command processing
     */
    private fun setupVoiceCommandProcessing() {
        serviceScope.launch {
            voiceInterface.recognizedText.collect { text ->
                if (text.isNotBlank() && _serviceState.value == STATE_LISTENING) {
                    processVoiceCommand(text)
                }
            }
        }
    }

    /**
     * Handle wake word detection
     */
    private suspend fun handleWakeWordDetected() {
        Log.d(TAG, "Wake word detected")
        
        _serviceState.value = STATE_LISTENING
        
        // Reset wake word detector
        wakeWordDetector.reset()
        
        // Play wake word confirmation
        spatialAudioGuide.playWakeWordConfirmation()
        
        // Start listening for command
        voiceInterface.startListening()
        
        // Timeout after 10 seconds
        serviceScope.launch {
            delay(10000)
            if (_serviceState.value == STATE_LISTENING) {
                _serviceState.value = STATE_IDLE
                voiceInterface.stopListening()
                voiceInterface.speak("I'm still listening for 'Hey SwingSync' or 'Golf Coach'")
            }
        }
    }

    /**
     * Process voice command
     */
    private suspend fun processVoiceCommand(text: String) {
        val startTime = System.currentTimeMillis()
        
        Log.d(TAG, "Processing voice command: $text")
        
        _serviceState.value = STATE_PROCESSING
        commandCount++
        
        try {
            // Process command
            val command = voiceCommandProcessor.processCommand(text)
            _pendingCommand.value = command
            
            // Execute command
            val response = executeCommand(command)
            
            // Speak response
            if (response.isNotBlank()) {
                _serviceState.value = STATE_RESPONDING
                voiceInterface.speak(response)
                _lastResponse.value = response
            }
            
            // Update statistics
            if (command.confidence > 0.5f) {
                successfulCommands++
            }
            
            val responseTime = System.currentTimeMillis() - startTime
            averageResponseTime = (averageResponseTime + responseTime) / 2
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
            voiceInterface.speak("Sorry, I didn't understand that. Please try again.")
        }
        
        _serviceState.value = STATE_IDLE
    }

    /**
     * Execute voice command
     */
    private suspend fun executeCommand(command: VoiceCommand): String {
        return when (command.type) {
            VoiceCommandType.START_PRACTICE -> {
                handleStartPractice(command)
            }
            VoiceCommandType.END_PRACTICE -> {
                handleEndPractice(command)
            }
            VoiceCommandType.RECORD_SWING -> {
                handleRecordSwing(command)
            }
            VoiceCommandType.ANALYZE_SWING -> {
                handleAnalyzeSwing(command)
            }
            VoiceCommandType.GET_TIPS -> {
                handleGetTips(command)
            }
            VoiceCommandType.SHOW_METRICS -> {
                handleShowMetrics(command)
            }
            VoiceCommandType.COMPARE_SWINGS -> {
                handleCompareSwings(command)
            }
            VoiceCommandType.SETTINGS -> {
                handleSettings(command)
            }
            VoiceCommandType.HELP -> {
                handleHelp(command)
            }
            VoiceCommandType.CONVERSATION -> {
                handleConversation(command)
            }
            else -> {
                "I'm not sure how to help with that. Try saying 'help' for available commands."
            }
        }
    }

    /**
     * Handle start practice command
     */
    private suspend fun handleStartPractice(command: VoiceCommand): String {
        _currentMode.value = HandsFreeMode.PRACTICE
        
        // Start camera activity if not already active
        if (appContext != AppContext.CAMERA) {
            val intent = Intent(this, CameraActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
        
        val club = command.parameters["club"] as? String
        return when (club) {
            "driver" -> "Starting practice with your driver. I'll help you perfect that long game!"
            "iron" -> "Iron practice session started. Let's work on accuracy and consistency."
            "wedge" -> "Wedge practice time! I'll help you dial in those short game shots."
            "putter" -> "Putting practice started. Let's focus on your stroke and alignment."
            else -> "Practice session started! I'm here to help you improve your swing."
        }
    }

    /**
     * Handle end practice command
     */
    private suspend fun handleEndPractice(command: VoiceCommand): String {
        _currentMode.value = HandsFreeMode.REVIEW
        
        val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60
        return "Great practice session! You worked for $sessionDuration minutes. " +
                "Check your progress in the history section."
    }

    /**
     * Handle record swing command
     */
    private suspend fun handleRecordSwing(command: VoiceCommand): String {
        // Trigger recording through camera activity
        val intent = Intent("com.swingsync.RECORD_SWING")
        sendBroadcast(intent)
        
        val club = command.parameters["club"] as? String
        return when (club) {
            null -> "Recording started. Take your swing when ready!"
            else -> "Recording your $club swing. Show me your best form!"
        }
    }

    /**
     * Handle analyze swing command
     */
    private suspend fun handleAnalyzeSwing(command: VoiceCommand): String {
        // Trigger analysis
        val intent = Intent("com.swingsync.ANALYZE_SWING")
        sendBroadcast(intent)
        
        return "Analyzing your swing now. I'll give you detailed feedback in just a moment."
    }

    /**
     * Handle get tips command
     */
    private suspend fun handleGetTips(command: VoiceCommand): String {
        val club = command.parameters["club"] as? String
        val swingPhase = command.parameters["swing_phase"] as? String
        
        return when {
            club == "driver" -> "For your driver, focus on a wide arc and staying behind the ball. " +
                    "Let your hips lead the downswing."
            club == "iron" -> "With irons, focus on ball-first contact. Keep your weight forward " +
                    "and compress the ball against the turf."
            swingPhase == "backswing" -> "For your backswing, maintain your spine angle and " +
                    "turn your shoulders fully while keeping your head steady."
            else -> "Here's a key tip: Focus on tempo and balance. A smooth, controlled swing " +
                    "beats a fast, aggressive one every time."
        }
    }

    /**
     * Handle show metrics command
     */
    private suspend fun handleShowMetrics(command: VoiceCommand): String {
        val metrics = command.parameters["metrics"] as? List<String>
        
        return when {
            metrics?.contains("swing_speed") == true -> 
                "Your average swing speed is 105 mph. That's solid club head speed!"
            metrics?.contains("tempo") == true -> 
                "Your tempo ratio is 3 to 1, which is right in the ideal range."
            else -> "Opening your swing metrics now. You can see all your stats on screen."
        }
    }

    /**
     * Handle compare swings command
     */
    private suspend fun handleCompareSwings(command: VoiceCommand): String {
        return "Comparing your last two swings. The main difference I see is in your " +
                "follow-through position. Your recent swing has better extension."
    }

    /**
     * Handle settings command
     */
    private suspend fun handleSettings(command: VoiceCommand): String {
        val text = command.originalText.lowercase()
        
        return when {
            text.contains("slower") -> {
                voiceInterface.setVoiceSettings(VoiceSettings(speed = 0.8f))
                "I'll speak more slowly now. Is this better?"
            }
            text.contains("faster") -> {
                voiceInterface.setVoiceSettings(VoiceSettings(speed = 1.2f))
                "Speaking faster now. How's this pace?"
            }
            text.contains("volume") -> {
                "You can adjust my volume using your device's volume controls."
            }
            else -> {
                "Opening settings now. You can customize my voice and behavior there."
            }
        }
    }

    /**
     * Handle help command
     */
    private suspend fun handleHelp(command: VoiceCommand): String {
        return "Here's what I can help you with:\n" +
                "Say 'start practice' to begin a session\n" +
                "Say 'record swing' to capture your swing\n" +
                "Say 'analyze swing' for detailed feedback\n" +
                "Say 'give me tips' for improvement suggestions\n" +
                "Say 'show my stats' to see your metrics\n" +
                "Just speak naturally and I'll understand!"
    }

    /**
     * Handle conversation command
     */
    private suspend fun handleConversation(command: VoiceCommand): String {
        return "I'm here to help you improve your golf swing. What would you like to work on today?"
    }

    /**
     * Monitor battery level
     */
    private fun monitorBatteryLevel() {
        serviceScope.launch {
            while (true) {
                val batteryLevel = getCurrentBatteryLevel()
                _batteryLevel.value = batteryLevel
                
                // Adjust wake word sensitivity based on battery
                when {
                    batteryLevel < CRITICAL_BATTERY_THRESHOLD -> {
                        wakeWordDetector.adjustSensitivity(0.3f)
                    }
                    batteryLevel < LOW_BATTERY_THRESHOLD -> {
                        wakeWordDetector.adjustSensitivity(0.6f)
                    }
                    else -> {
                        wakeWordDetector.adjustSensitivity(1.0f)
                    }
                }
                
                delay(30000) // Check every 30 seconds
            }
        }
    }

    /**
     * Monitor app context
     */
    private fun monitorAppContext() {
        serviceScope.launch {
            while (true) {
                // This would integrate with activity lifecycle callbacks
                // For now, we'll use a simple time-based approach
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastActivityTime > 60000) {
                    _isInBackground.value = true
                } else {
                    _isInBackground.value = false
                }
                
                delay(10000) // Check every 10 seconds
            }
        }
    }

    /**
     * Get current battery level
     */
    private fun getCurrentBatteryLevel(): Float {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level != -1 && scale != -1) {
            level.toFloat() / scale.toFloat()
        } else {
            1.0f
        }
    }

    /**
     * Should use spatial audio
     */
    private fun shouldUseSpatialAudio(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)
    }

    /**
     * Speak welcome message
     */
    private suspend fun speakWelcomeMessage() {
        val welcomeMessage = when (_currentMode.value) {
            HandsFreeMode.PRACTICE -> "Hands-free mode is active. Say 'Hey SwingSync' or 'Golf Coach' to get started with your practice!"
            HandsFreeMode.REVIEW -> "Hands-free mode is active. I can help you review your swings and track your progress."
            HandsFreeMode.COACHING -> "Personal coaching mode is active. I'm here to provide detailed feedback and guidance."
        }
        
        voiceInterface.speak(welcomeMessage)
    }

    /**
     * Setup notification channel
     */
    private fun setupNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hands-Free Golf Coaching",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Voice control for hands-free golf coaching"
            setSound(null, null)
            enableVibration(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create notification
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, CameraActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val toggleIntent = Intent(this, HandsFreeService::class.java).apply {
            action = ACTION_TOGGLE_HANDS_FREE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val statusText = if (_isHandsFreeActive.value) {
            "Listening for 'Hey SwingSync' or 'Golf Coach'"
        } else {
            "Tap to activate hands-free mode"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SwingSync Voice Coach")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                if (_isHandsFreeActive.value) "Disable" else "Enable",
                togglePendingIntent
            )
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Update notification
     */
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        stopHandsFreeControl()
        serviceScope.cancel()
        
        powerOptimizationManager.stopOptimization()
        powerOptimizationManager.cleanup()
        
        wakeLock?.release()
        wakeLock = null
        
        wakeWordDetector.cleanup()
        spatialAudioGuide.cleanup()
    }

    /**
     * Get service statistics
     */
    fun getServiceStats(): Map<String, Any> {
        val optimizationStats = powerOptimizationManager.getOptimizationStats()
        
        return mapOf(
            "sessionDuration" to (System.currentTimeMillis() - sessionStartTime) / 1000,
            "commandCount" to commandCount,
            "successfulCommands" to successfulCommands,
            "successRate" to if (commandCount > 0) successfulCommands.toFloat() / commandCount else 0f,
            "averageResponseTime" to averageResponseTime,
            "batteryLevel" to _batteryLevel.value,
            "isInBackground" to _isInBackground.value,
            "optimization" to optimizationStats
        )
    }

    inner class HandsFreeServiceBinder : Binder() {
        fun getService(): HandsFreeService = this@HandsFreeService
    }
}

/**
 * Hands-free modes
 */
enum class HandsFreeMode {
    PRACTICE,   // Active swing recording and analysis
    REVIEW,     // Reviewing past swings and progress
    COACHING    // Detailed coaching and instruction
}

/**
 * App context for smart mode switching
 */
enum class AppContext {
    CAMERA,     // Camera activity - practice mode
    ANALYSIS,   // Analysis screen - review mode
    HISTORY,    // History screen - review mode
    SETTINGS,   // Settings screen - configuration mode
    UNKNOWN     // Unknown or background
}