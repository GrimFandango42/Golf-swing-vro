package com.swingsync.ai.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.models.SwingAnalysis
import com.swingsync.ai.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val getAnalysisCountUseCase: GetAnalysisCountUseCase,
    private val getAverageScoreUseCase: GetAverageScoreUseCase,
    private val getTopAnalysesUseCase: GetTopAnalysesUseCase,
    private val getAllAnalysesUseCase: GetAllAnalysesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val userProfile = getCurrentUserProfileUseCase()
                val userId = userProfile?.id ?: return@launch
                
                // Load statistics
                val totalSwings = getAnalysisCountUseCase(userId)
                val averageScore = getAverageScoreUseCase(userId)
                val topAnalyses = getTopAnalysesUseCase(userId, 1)
                val bestScore = topAnalyses.firstOrNull()?.score ?: 0f
                
                // Load recent analyses
                val recentAnalyses = getTopAnalysesUseCase(userId, 5)
                
                // Calculate weekly swings (placeholder - would need date filtering)
                val weeklySwings = minOf(totalSwings, 7)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalSwings = totalSwings,
                    averageScore = averageScore,
                    bestScore = bestScore,
                    weeklySwings = weeklySwings,
                    recentAnalyses = recentAnalyses
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading home data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load data"
                )
            }
        }
    }
    
    fun refreshData() {
        loadData()
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val totalSwings: Int = 0,
    val averageScore: Float = 0f,
    val bestScore: Float = 0f,
    val weeklySwings: Int = 0,
    val recentAnalyses: List<SwingAnalysis> = emptyList(),
    val errorMessage: String? = null
)