package com.golfswing.vro.pixel.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfswing.vro.pixel.coaching.CelebrationSystem
import com.golfswing.vro.pixel.coaching.ConversationalCoachingEngine
import com.golfswing.vro.pixel.pose.OptimizedGolfPoseDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * ViewModel for conversational golf coaching camera
 * Focuses on core functionality: real-time analysis and natural coaching feedback
 */
@HiltViewModel
class ConversationalCameraViewModel @Inject constructor(
    private val poseDetector: OptimizedGolfPoseDetector,
    private val coachingEngine: ConversationalCoachingEngine,
    private val celebrationSystem: CelebrationSystem
) : ViewModel() {
    
    // Camera execution
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // Session state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _swingCount = MutableStateFlow(0)
    val swingCount: StateFlow<Int> = _swingCount.asStateFlow()
    
    // Forward coaching engine states
    val coachMessage = coachingEngine.coachMessage
    val sessionMood = coachingEngine.sessionMood
    val celebration = celebrationSystem.currentCelebration
    
    // Session tracking
    private var sessionStartTime = 0L
    private var lastSwingTime = 0L
    private val swingScores = mutableListOf<Float>()
    
    init {
        // Initialize pose detector
        poseDetector.initialize()
        
        // Listen to pose results and trigger coaching
        viewModelScope.launch {
            poseDetector.poseResult
                .filterNotNull()
                .collect { poseResult ->
                    handlePoseResult(poseResult)
                }
        }
    }
    
    /**
     * Initialize camera for golf swing analysis
     */
    fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // Image analysis use case for pose detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (_isRecording.value) {
                            poseDetector.processFrame(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            
            // Select camera (prefer back camera for golf)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                
            } catch (exc: Exception) {
                // Handle camera initialization error
                println("Camera initialization failed: ${exc.message}")
            }
            
        }, ContextCompat.getMainExecutor(previewView.context))
    }
    
    /**
     * Start practice session
     */
    fun startSession() {
        if (!_isRecording.value) {
            _isRecording.value = true
            sessionStartTime = System.currentTimeMillis()
            
            // Start conversational coaching
            coachingEngine.startCoachingSession()
            
            // Reset session stats
            _swingCount.value = 0
            swingScores.clear()
        }
    }
    
    /**
     * Pause practice session
     */
    fun pauseSession() {
        _isRecording.value = false
        
        // Give encouraging feedback about the session
        if (_swingCount.value > 0) {
            val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60
            coachingEngine.endSession()
            
            // Update practice time for achievements
            celebrationSystem.updatePracticeTime(System.currentTimeMillis() - sessionStartTime)
        }
    }
    
    /**
     * Handle pose detection results and trigger coaching
     */
    private fun handlePoseResult(poseResult: OptimizedGolfPoseDetector.GolfPoseResult) {
        // Track swing completion
        if (poseResult.swingPhase == OptimizedGolfPoseDetector.SwingPhase.FINISH) {
            handleSwingCompletion(poseResult)
        }
        
        // Update swing phase transitions for coaching
        handleSwingPhaseChange(poseResult)
        
        // Check for real-time corrections
        checkForRealTimeCorrections(poseResult)
    }
    
    /**
     * Handle completed swing
     */
    private fun handleSwingCompletion(poseResult: OptimizedGolfPoseDetector.GolfPoseResult) {
        val currentTime = System.currentTimeMillis()
        
        // Avoid duplicate swing counting
        if (currentTime - lastSwingTime < 2000L) return
        
        lastSwingTime = currentTime
        _swingCount.value += 1
        
        val score = poseResult.coreMetrics.overallScore
        swingScores.add(score)
        
        // Update celebration system
        val isGoodSwing = score > 7f
        celebrationSystem.updateStreak(isGoodSwing)
        
        // Check for celebrations
        val context = CelebrationSystem.CoachingContext(
            isFirstSwing = _swingCount.value == 1,
            wasStruggling = getRecentAverageScore() < 6f,
            averageScore = getSessionAverageScore()
        )
        
        val celebration = celebrationSystem.getSuggestedCelebration(score, context)
        celebration?.let { 
            celebrationSystem.triggerCelebration(it)
        }
        
        // Special handling for first swing
        if (_swingCount.value == 1) {
            celebrationSystem.triggerCelebration(CelebrationSystem.CelebrationEvent.FIRST_SWING)
        }
    }
    
    /**
     * Handle swing phase changes for coaching
     */
    private fun handleSwingPhaseChange(poseResult: OptimizedGolfPoseDetector.GolfPoseResult) {
        // Provide phase-specific coaching tips
        when (poseResult.swingPhase) {
            OptimizedGolfPoseDetector.SwingPhase.ADDRESS -> {
                // Gentle setup reminders
                if (poseResult.coreMetrics.posture < 0.6f) {
                    // Let coaching engine handle this naturally
                }
            }
            
            OptimizedGolfPoseDetector.SwingPhase.BACKSWING -> {
                // X-Factor coaching during backswing
                if (poseResult.coreMetrics.xFactor < 20f) {
                    // Coaching engine will provide natural feedback
                }
            }
            
            OptimizedGolfPoseDetector.SwingPhase.IMPACT -> {
                // Quick positive reinforcement for good impact
                if (poseResult.coreMetrics.balance > 0.8f && 
                    poseResult.coreMetrics.headStability > 0.8f) {
                    // Natural coaching reaction
                }
            }
            
            else -> {
                // Let coaching engine handle other phases naturally
            }
        }
    }
    
    /**
     * Check for real-time corrections needed
     */
    private fun checkForRealTimeCorrections(poseResult: OptimizedGolfPoseDetector.GolfPoseResult) {
        val metrics = poseResult.coreMetrics
        
        // Only provide critical real-time feedback to avoid overwhelming
        when {
            metrics.headStability < 0.3f -> {
                // Head moving too much - critical feedback
            }
            
            metrics.balance < 0.4f -> {
                // Severe balance issue
            }
            
            poseResult.swingPhase == OptimizedGolfPoseDetector.SwingPhase.BACKSWING && 
            metrics.xFactor < 15f -> {
                // X-Factor too low during backswing
            }
            
            else -> {
                // Let normal coaching flow handle feedback
            }
        }
    }
    
    /**
     * Get recent average score (last 5 swings)
     */
    private fun getRecentAverageScore(): Float {
        return if (swingScores.size >= 3) {
            swingScores.takeLast(5).average().toFloat()
        } else 7f // Default neutral score
    }
    
    /**
     * Get session average score
     */
    private fun getSessionAverageScore(): Float {
        return if (swingScores.isNotEmpty()) {
            swingScores.average().toFloat()
        } else 7f // Default neutral score
    }
    
    /**
     * Handle voice commands from user
     */
    fun handleVoiceCommand(command: String) {
        coachingEngine.handleVoiceCommand(command)
    }
    
    /**
     * Get session summary for user
     */
    fun getSessionSummary(): SessionSummary {
        val duration = if (sessionStartTime > 0) {
            (System.currentTimeMillis() - sessionStartTime) / 1000 / 60
        } else 0L
        
        return SessionSummary(
            swingCount = _swingCount.value,
            averageScore = getSessionAverageScore(),
            bestScore = swingScores.maxOrNull() ?: 0f,
            durationMinutes = duration.toInt(),
            improvementTrend = calculateImprovementTrend()
        )
    }
    
    /**
     * Calculate if golfer is improving during session
     */
    private fun calculateImprovementTrend(): ImprovementTrend {
        if (swingScores.size < 6) return ImprovementTrend.NEUTRAL
        
        val firstHalf = swingScores.take(swingScores.size / 2).average()
        val secondHalf = swingScores.drop(swingScores.size / 2).average()
        
        return when {
            secondHalf > firstHalf + 0.5 -> ImprovementTrend.IMPROVING
            secondHalf < firstHalf - 0.5 -> ImprovementTrend.DECLINING
            else -> ImprovementTrend.STEADY
        }
    }
    
    /**
     * Reset current session
     */
    fun resetSession() {
        pauseSession()
        _swingCount.value = 0
        swingScores.clear()
        sessionStartTime = 0L
        lastSwingTime = 0L
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        poseDetector.release()
    }
    
    // Data classes for session tracking
    data class SessionSummary(
        val swingCount: Int,
        val averageScore: Float,
        val bestScore: Float,
        val durationMinutes: Int,
        val improvementTrend: ImprovementTrend
    )
    
    enum class ImprovementTrend {
        IMPROVING, DECLINING, STEADY, NEUTRAL
    }
}