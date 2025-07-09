package com.swingsync.ai.ui.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.achievements.AchievementSystem
import com.swingsync.ai.celebration.CelebrationEngine
import com.swingsync.ai.data.model.PoseDetectionResult
import com.swingsync.ai.data.model.RecordingSession
import com.swingsync.ai.data.model.SwingAnalysisFeedback
import com.swingsync.ai.data.model.SwingVideoAnalysisInput
import com.swingsync.ai.detection.BestSwingDetector
import com.swingsync.ai.domain.model.*
import com.swingsync.ai.domain.usecase.swing.*
import com.swingsync.ai.domain.usecase.user.GetUserSettingsUseCase
import com.swingsync.ai.domain.util.Result
import com.swingsync.ai.domain.util.SwingAnalysisError
import com.swingsync.ai.domain.util.getUserFriendlyMessage
import com.swingsync.ai.domain.util.toSwingAnalysisError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for CameraActivity
 * Manages camera state, pose detection results, recording sessions, and celebration system integration
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val createSwingSessionUseCase: CreateSwingSessionUseCase,
    private val updateSwingSessionUseCase: UpdateSwingSessionUseCase,
    private val savePoseDetectionUseCase: SavePoseDetectionUseCase,
    private val analyzeSwingUseCase: AnalyzeSwingUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val bestSwingDetector: BestSwingDetector,
    private val celebrationEngine: CelebrationEngine,
    private val achievementSystem: AchievementSystem
) : ViewModel() {

    companion object {
        private const val TAG = "CameraViewModel"
    }

    // UI State
    private val _uiState = MutableLiveData<CameraUiState>()
    val uiState: LiveData<CameraUiState> = _uiState

    // Pose detection results
    private val _poseDetectionResult = MutableLiveData<PoseDetectionResult?>()
    val poseDetectionResult: LiveData<PoseDetectionResult?> = _poseDetectionResult

    // Current FPS
    private val _currentFPS = MutableLiveData<Float>()
    val currentFPS: LiveData<Float> = _currentFPS

    // Error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Processing state
    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    // Selected club
    private val _selectedClub = MutableLiveData<String>()
    val selectedClub: LiveData<String> = _selectedClub

    // Current swing session
    private val _currentSession = MutableLiveData<SwingSession?>()
    val currentSession: LiveData<SwingSession?> = _currentSession

    // Celebration events
    private val _celebrationTriggered = MutableLiveData<Boolean>()
    val celebrationTriggered: LiveData<Boolean> = _celebrationTriggered

    // Achievement progress
    private val _achievementUnlocked = MutableLiveData<String?>()
    val achievementUnlocked: LiveData<String?> = _achievementUnlocked

    // Analysis results
    private val _analysisResult = MutableLiveData<SwingAnalysis?>()
    val analysisResult: LiveData<SwingAnalysis?> = _analysisResult

    // FPS calculation
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var fpsUpdateTime = 0L

    // Current user ID (in a real app, this would come from authentication)
    private val currentUserId = "current_user"
    
    // Pose detection cache for current session
    private val poseDetectionCache = mutableListOf<PoseDetection>()

    init {
        // Initialize default values
        _selectedClub.value = "Driver"
        _isProcessing.value = false
        _currentFPS.value = 0f
        _uiState.value = CameraUiState.Idle
        
        // Load user settings
        loadUserSettings()
    }

    /**
     * Process pose detection result from MediaPipe
     */
    fun processPoseDetection(result: PoseDetectionResult) {
        viewModelScope.launch {
            try {
                // Update pose detection result
                _poseDetectionResult.value = result
                
                // Calculate FPS
                updateFPS()
                
                // Clear any previous errors
                _errorMessage.value = null
                
            } catch (e: Exception) {
                _errorMessage.value = "Pose detection processing failed: ${e.message}"
            }
        }
    }

    /**
     * Load user settings and set preferred club
     */
    private fun loadUserSettings() {
        viewModelScope.launch {
            try {
                when (val result = getUserSettingsUseCase(GetUserSettingsUseCase.Params(currentUserId))) {
                    is Result.Success -> {
                        _selectedClub.value = result.data.preferredClub
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Set the selected golf club
     */
    fun setSelectedClub(club: String) {
        _selectedClub.value = club
    }

    /**
     * Start a new recording session
     */
    fun startRecordingSession(clubUsed: String, fps: Float) {
        viewModelScope.launch {
            try {
                _uiState.value = CameraUiState.Starting
                _isProcessing.value = true
                
                val params = CreateSwingSessionUseCase.Params(
                    userId = currentUserId,
                    clubUsed = clubUsed,
                    fps = fps
                )
                
                when (val result = createSwingSessionUseCase(params)) {
                    is Result.Success -> {
                        _currentSession.value = result.data
                        _uiState.value = CameraUiState.Recording
                        poseDetectionCache.clear()
                        clearError()
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                        _uiState.value = CameraUiState.Error
                        _isProcessing.value = false
                    }
                    is Result.Loading -> {
                        _uiState.value = CameraUiState.Starting
                    }
                }
                
            } catch (e: Exception) {
                handleError(e)
                _uiState.value = CameraUiState.Error
                _isProcessing.value = false
            }
        }
    }

    /**
     * Stop the current recording session
     */
    fun stopRecordingSession() {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                    _uiState.value = CameraUiState.Stopping
                    
                    // Save pose detections first
                    if (poseDetectionCache.isNotEmpty()) {
                        savePoseDetections()
                    }
                    
                    // Update session to completed
                    val params = UpdateSwingSessionUseCase.Params(
                        sessionId = session.sessionId,
                        endTime = System.currentTimeMillis(),
                        totalFrames = poseDetectionCache.size,
                        isCompleted = true
                    )
                    
                    when (val result = updateSwingSessionUseCase(params)) {
                        is Result.Success -> {
                            _currentSession.value = result.data
                            _uiState.value = CameraUiState.Completed
                            _isProcessing.value = false
                            clearError()
                        }
                        is Result.Error -> {
                            handleError(result.exception)
                            _uiState.value = CameraUiState.Error
                            _isProcessing.value = false
                        }
                        is Result.Loading -> {
                            _uiState.value = CameraUiState.Stopping
                        }
                    }
                }
                
            } catch (e: Exception) {
                handleError(e)
                _uiState.value = CameraUiState.Error
                _isProcessing.value = false
            }
        }
    }

    /**
     * Add pose detection result to current recording session
     */
    fun addPoseResultToSession(result: PoseDetectionResult) {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                    if (!session.isCompleted) {
                        // Convert to domain model and cache
                        val poseDetection = PoseDetection(
                            detectionId = UUID.randomUUID().toString(),
                            sessionId = session.sessionId,
                            frameNumber = poseDetectionCache.size,
                            timestamp = System.currentTimeMillis(),
                            keypoints = result.keypoints.map { 
                                Keypoint(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z ?: 0f,
                                    visibility = it.visibility ?: 1f,
                                    confidence = it.confidence ?: 0f
                                )
                            },
                            confidence = result.confidence,
                            poseLandmarks = emptyList(), // Convert if needed
                            createdAt = System.currentTimeMillis()
                        )
                        
                        poseDetectionCache.add(poseDetection)
                        
                        // Save pose detections in batches to avoid overwhelming the database
                        if (poseDetectionCache.size % 30 == 0) {
                            savePoseDetections()
                        }
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Update FPS calculation
     */
    private fun updateFPS() {
        val currentTime = System.currentTimeMillis()
        
        if (lastFrameTime == 0L) {
            lastFrameTime = currentTime
            fpsUpdateTime = currentTime
            frameCount = 0
            return
        }
        
        frameCount++
        
        // Update FPS every second
        if (currentTime - fpsUpdateTime >= 1000) {
            val fps = frameCount.toFloat() / ((currentTime - fpsUpdateTime) / 1000f)
            _currentFPS.value = fps
            
            // Reset for next calculation
            frameCount = 0
            fpsUpdateTime = currentTime
        }
        
        lastFrameTime = currentTime
    }

    /**
     * Get current swing session data
     */
    fun getCurrentSwingSession(): SwingSession? {
        return _currentSession.value
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean {
        return _currentSession.value?.let { !it.isCompleted } == true
    }

    /**
     * Process swing analysis and trigger celebrations
     */
    fun processSwingAnalysis() {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                    if (!session.isCompleted) {
                        handleError(IllegalStateException("Session must be completed before analysis"))
                        return@launch
                    }
                    
                    _uiState.value = CameraUiState.Analyzing
                    
                    val params = AnalyzeSwingUseCase.Params(
                        sessionId = session.sessionId,
                        analysisType = "full"
                    )
                    
                    when (val result = analyzeSwingUseCase(params)) {
                        is Result.Success -> {
                            _analysisResult.value = result.data
                            _uiState.value = CameraUiState.AnalysisComplete
                            
                            // Check for celebrations based on analysis score
                            if (result.data.score >= 80f) {
                                _celebrationTriggered.value = true
                            }
                            
                            clearError()
                        }
                        is Result.Error -> {
                            handleError(result.exception)
                            _uiState.value = CameraUiState.Error
                        }
                        is Result.Loading -> {
                            _uiState.value = CameraUiState.Analyzing
                        }
                    }
                }
            } catch (e: Exception) {
                handleError(e)
                _uiState.value = CameraUiState.Error
            }
        }
    }

    /**
     * Clear celebration trigger
     */
    fun clearCelebrationTrigger() {
        _celebrationTriggered.value = false
    }

    /**
     * Clear achievement notification
     */
    fun clearAchievementNotification() {
        _achievementUnlocked.value = null
    }

    /**
     * Get current analysis result
     */
    fun getCurrentAnalysisResult(): SwingAnalysis? {
        return _analysisResult.value
    }
    
    /**
     * Save pose detections to database
     */
    private suspend fun savePoseDetections() {
        if (poseDetectionCache.isNotEmpty()) {
            val params = SavePoseDetectionUseCase.Params(poseDetectionCache.toList())
            when (val result = savePoseDetectionUseCase(params)) {
                is Result.Success -> {
                    poseDetectionCache.clear()
                }
                is Result.Error -> {
                    handleError(result.exception)
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }
    
    /**
     * Handle errors with proper error types
     */
    private fun handleError(exception: Exception) {
        val swingError = exception.toSwingAnalysisError()
        _errorMessage.value = swingError.getUserFriendlyMessage()
    }

    /**
     * Initialize celebration system for this session
     */
    fun initializeCelebrationSystem(lifecycleOwner: androidx.lifecycle.LifecycleOwner, rootView: android.view.ViewGroup) {
        // Initialize celebration engine with root view
        celebrationEngine.initialize(rootView, lifecycleOwner)
        
        // Initialize achievement system
        achievementSystem.initialize(lifecycleOwner)
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
        _poseDetectionResult.value = null
        _currentSession.value = null
        poseDetectionCache.clear()
    }
}

/**
 * UI State for the Camera screen
 */
sealed class CameraUiState {
    object Idle : CameraUiState()
    object Starting : CameraUiState()
    object Recording : CameraUiState()
    object Stopping : CameraUiState()
    object Completed : CameraUiState()
    object Analyzing : CameraUiState()
    object AnalysisComplete : CameraUiState()
    object Error : CameraUiState()
}
}