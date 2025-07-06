package com.swingsync.ai.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.domain.usecases.GetCurrentUserProfileUseCase
import com.swingsync.ai.utils.camera.CameraManager
import com.swingsync.ai.utils.mediapipe.PoseEstimationManager
import com.swingsync.ai.utils.network.NetworkManager
import com.swingsync.ai.utils.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val cameraManager: CameraManager,
    private val poseEstimationManager: PoseEstimationManager,
    private val voiceManager: VoiceManager,
    private val networkManager: NetworkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        checkUserProfile()
        startNetworkMonitoring()
    }
    
    private fun checkUserProfile() {
        viewModelScope.launch {
            try {
                val userProfile = getCurrentUserProfileUseCase()
                _uiState.value = _uiState.value.copy(
                    isFirstLaunch = userProfile == null,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Error checking user profile")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load user profile"
                )
            }
        }
    }
    
    private fun startNetworkMonitoring() {
        networkManager.startNetworkMonitoring()
    }
    
    fun initializeServices() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isInitializing = true)
                
                // Initialize camera
                cameraManager.initializeCamera().getOrThrow()
                
                // Initialize pose estimation
                poseEstimationManager.initialize().getOrThrow()
                
                // Initialize voice manager
                voiceManager.initialize().getOrThrow()
                
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    servicesInitialized = true
                )
                
                Timber.d("All services initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error initializing services")
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    errorMessage = "Failed to initialize services: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        networkManager.stopNetworkMonitoring()
        cameraManager.release()
        poseEstimationManager.release()
        voiceManager.release()
    }
}

data class MainUiState(
    val isLoading: Boolean = true,
    val isInitializing: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val servicesInitialized: Boolean = false,
    val errorMessage: String? = null
)