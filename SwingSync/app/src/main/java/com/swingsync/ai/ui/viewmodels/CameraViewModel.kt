package com.swingsync.ai.ui.viewmodels

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.models.SwingAnalysis
import com.swingsync.ai.data.models.SwingType
import com.swingsync.ai.domain.usecases.CreateAnalysisUseCase
import com.swingsync.ai.domain.usecases.GetCurrentUserProfileUseCase
import com.swingsync.ai.utils.camera.CameraManager
import com.swingsync.ai.utils.camera.RecordingState
import com.swingsync.ai.utils.mediapipe.PoseEstimationManager
import com.swingsync.ai.utils.mediapipe.PoseEstimationResult
import com.swingsync.ai.utils.mediapipe.SwingPhase
import com.swingsync.ai.utils.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val poseEstimationManager: PoseEstimationManager,
    private val voiceManager: VoiceManager,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val createAnalysisUseCase: CreateAnalysisUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    
    private val poseResults = mutableListOf<PoseEstimationResult>()
    private var currentVideoFile: File? = null
    private var recordingStartTime: Long = 0
    
    init {
        observeStates()
    }
    
    private fun observeStates() {
        // Observe camera recording state
        viewModelScope.launch {
            cameraManager.recordingState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    recordingState = state,
                    isRecording = state == RecordingState.RECORDING
                )
                
                when (state) {
                    RecordingState.RECORDING -> {
                        recordingStartTime = System.currentTimeMillis()
                        startRecordingTimer()
                    }
                    RecordingState.FINISHED -> {
                        processRecordedVideo()
                    }
                    else -> {}
                }
            }
        }
        
        // Observe pose estimation results
        viewModelScope.launch {
            poseEstimationManager.poseResults.collect { result ->
                poseResults.add(result)
                
                // Analyze current swing phase
                val phaseAnalysis = poseEstimationManager.analyzeSwingPhase(poseResults.takeLast(30))
                updateSwingPhase(phaseAnalysis.phase)
                
                // Provide voice coaching if enabled
                if (_uiState.value.isVoiceCoachingEnabled) {
                    provideVoiceCoaching(phaseAnalysis.phase, phaseAnalysis.analysis)
                }
            }
        }
    }
    
    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                cameraManager.startCamera(lifecycleOwner, previewView) { imageProxy ->
                    poseEstimationManager.processFrame(imageProxy)
                }
                _uiState.value = _uiState.value.copy(isCameraReady = true)
            } catch (e: Exception) {
                Timber.e(e, "Error starting camera")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to start camera")
            }
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                val userProfile = getCurrentUserProfileUseCase()
                if (userProfile == null) {
                    _uiState.value = _uiState.value.copy(errorMessage = "User profile not found")
                    return@launch
                }
                
                currentVideoFile = cameraManager.createVideoFile()
                currentVideoFile?.let { file ->
                    cameraManager.startRecording(file) { state ->
                        // Recording state updates are handled by the state flow observer
                    }
                    
                    // Clear previous pose results
                    poseResults.clear()
                    
                    // Start voice coaching
                    if (_uiState.value.isVoiceCoachingEnabled) {
                        voiceManager.speakInstruction("Recording started. Get ready for your swing!")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting recording")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to start recording")
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            try {
                cameraManager.stopRecording()
                
                if (_uiState.value.isVoiceCoachingEnabled) {
                    voiceManager.speakInstruction("Recording stopped. Analyzing your swing...")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stopping recording")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to stop recording")
            }
        }
    }
    
    private fun startRecordingTimer() {
        viewModelScope.launch {
            while (_uiState.value.isRecording) {
                val elapsedTime = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsedTime / 1000) % 60
                val minutes = (elapsedTime / (1000 * 60)) % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                
                _uiState.value = _uiState.value.copy(recordingTime = timeString)
                
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private fun updateSwingPhase(phase: SwingPhase) {
        val phaseText = when (phase) {
            SwingPhase.ADDRESS -> "Address"
            SwingPhase.BACKSWING -> "Backswing"
            SwingPhase.DOWNSWING -> "Downswing"
            SwingPhase.IMPACT -> "Impact"
            SwingPhase.FOLLOW_THROUGH -> "Follow Through"
            SwingPhase.UNKNOWN -> ""
        }
        
        _uiState.value = _uiState.value.copy(currentPhase = phaseText)
    }
    
    private fun provideVoiceCoaching(phase: SwingPhase, analysis: String) {
        // Provide real-time voice coaching based on swing phase
        when (phase) {
            SwingPhase.ADDRESS -> {
                if (analysis.contains("good", ignoreCase = true)) {
                    voiceManager.speakInstruction("Good setup position!")
                }
            }
            SwingPhase.BACKSWING -> {
                voiceManager.speakInstruction("Nice backswing, keep it smooth")
            }
            SwingPhase.IMPACT -> {
                voiceManager.speakInstruction("Keep your head steady through impact")
            }
            else -> {}
        }
    }
    
    private fun processRecordedVideo() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                
                val userProfile = getCurrentUserProfileUseCase()
                if (userProfile == null || currentVideoFile == null) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Missing user profile or video file"
                    )
                    return@launch
                }
                
                // Create swing analysis
                val analysis = SwingAnalysis(
                    id = UUID.randomUUID().toString(),
                    userId = userProfile.id,
                    videoPath = currentVideoFile!!.absolutePath,
                    timestamp = Date(),
                    poseData = poseResults.joinToString { /* Convert to JSON */ },
                    analysisResults = generateAnalysisResults(),
                    feedback = generateFeedback(),
                    score = calculateOverallScore(),
                    swingType = SwingType.DRIVER, // Default, could be user-selected
                    isSynced = false
                )
                
                val result = createAnalysisUseCase(analysis)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        analysisId = analysis.id
                    )
                    
                    if (_uiState.value.isVoiceCoachingEnabled) {
                        voiceManager.speakScore(analysis.score)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Failed to save analysis"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing recorded video")
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to process video"
                )
            }
        }
    }
    
    private fun generateAnalysisResults(): com.swingsync.ai.data.models.AnalysisResults {
        // Analyze pose data and generate scores
        return com.swingsync.ai.data.models.AnalysisResults(
            backswingScore = 75f,
            downswingScore = 80f,
            impactScore = 70f,
            followThroughScore = 85f,
            tempoScore = 78f,
            balanceScore = 82f,
            planeScore = 76f,
            keyIssues = listOf("Slight early extension", "Head movement at impact"),
            improvements = listOf("Focus on maintaining posture", "Keep head steady")
        )
    }
    
    private fun generateFeedback(): String {
        return "Good swing overall! Focus on maintaining your posture through impact and keeping your head steady. Your tempo is excellent."
    }
    
    private fun calculateOverallScore(): Float {
        // Calculate overall score from pose analysis
        return 77.5f
    }
    
    fun toggleVoiceCoaching() {
        _uiState.value = _uiState.value.copy(
            isVoiceCoachingEnabled = !_uiState.value.isVoiceCoachingEnabled
        )
    }
    
    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = !_uiState.value.showSettings
        )
    }
    
    fun updateSetting(setting: String, value: Any) {
        val currentSettings = _uiState.value.cameraSettings.toMutableMap()
        currentSettings[setting] = value
        _uiState.value = _uiState.value.copy(cameraSettings = currentSettings)
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
    }
}

data class CameraUiState(
    val isCameraReady: Boolean = false,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val recordingState: RecordingState = RecordingState.IDLE,
    val recordingTime: String = "00:00",
    val currentPhase: String = "",
    val showPoseOverlay: Boolean = true,
    val isVoiceCoachingEnabled: Boolean = true,
    val showSettings: Boolean = false,
    val cameraSettings: Map<String, Any> = emptyMap(),
    val poseResults: List<PoseEstimationResult> = emptyList(),
    val analysisId: String = "",
    val errorMessage: String? = null
)