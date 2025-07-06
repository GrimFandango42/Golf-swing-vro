package com.swingsync.ai.ui.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.model.PoseDetectionResult
import com.swingsync.ai.data.model.RecordingSession
import kotlinx.coroutines.launch

/**
 * ViewModel for CameraActivity
 * Manages camera state, pose detection results, and recording sessions
 */
class CameraViewModel : ViewModel() {

    companion object {
        private const val TAG = "CameraViewModel"
    }

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

    // Recording session
    private val _recordingSession = MutableLiveData<RecordingSession?>()
    val recordingSession: LiveData<RecordingSession?> = _recordingSession

    // FPS calculation
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var fpsUpdateTime = 0L

    init {
        // Initialize default values
        _selectedClub.value = "Driver"
        _isProcessing.value = false
        _currentFPS.value = 0f
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
     * Set the selected golf club
     */
    fun setSelectedClub(club: String) {
        _selectedClub.value = club
    }

    /**
     * Start a new recording session
     */
    fun startRecordingSession(sessionId: String, userId: String, clubUsed: String, fps: Float) {
        viewModelScope.launch {
            try {
                val session = RecordingSession(
                    sessionId = sessionId,
                    userId = userId,
                    clubUsed = clubUsed,
                    startTime = System.currentTimeMillis(),
                    fps = fps,
                    isRecording = true
                )
                
                _recordingSession.value = session
                _isProcessing.value = true
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start recording session: ${e.message}"
            }
        }
    }

    /**
     * Stop the current recording session
     */
    fun stopRecordingSession() {
        viewModelScope.launch {
            try {
                _recordingSession.value?.let { session ->
                    val updatedSession = session.copy(
                        endTime = System.currentTimeMillis(),
                        isRecording = false
                    )
                    _recordingSession.value = updatedSession
                }
                
                _isProcessing.value = false
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop recording session: ${e.message}"
            }
        }
    }

    /**
     * Add pose detection result to current recording session
     */
    fun addPoseResultToSession(result: PoseDetectionResult) {
        viewModelScope.launch {
            try {
                _recordingSession.value?.let { session ->
                    if (session.isRecording) {
                        val updatedSession = session.copy(
                            totalFrames = session.totalFrames + 1,
                            poseDetectionResults = session.poseDetectionResults + result
                        )
                        _recordingSession.value = updatedSession
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add pose result to session: ${e.message}"
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
     * Get current recording session data
     */
    fun getCurrentRecordingSession(): RecordingSession? {
        return _recordingSession.value
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean {
        return _recordingSession.value?.isRecording == true
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
        _poseDetectionResult.value = null
        _recordingSession.value = null
    }
}