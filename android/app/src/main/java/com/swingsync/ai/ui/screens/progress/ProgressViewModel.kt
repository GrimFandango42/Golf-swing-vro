package com.swingsync.ai.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    
    private val _progressStats = MutableStateFlow(generateMockProgressStats())
    val progressStats: StateFlow<ProgressStats> = _progressStats.asStateFlow()
    
    private val _recentSessions = MutableStateFlow(generateMockSessions())
    val recentSessions: StateFlow<List<PracticeSession>> = _recentSessions.asStateFlow()
    
    init {
        loadProgressData()
    }
    
    private fun loadProgressData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Load progress data from repository
                // For now, using mock data
                _progressStats.value = generateMockProgressStats()
                _recentSessions.value = generateMockSessions()
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load progress data: ${e.message}"
                )
            }
        }
    }
    
    private fun generateMockProgressStats(): ProgressStats {
        return ProgressStats(
            totalSessions = 24,
            averageScore = 78,
            improvement = 12,
            metrics = listOf(
                PerformanceMetric("Posture", 85, 100, 0.85f),
                PerformanceMetric("Swing Tempo", 72, 100, 0.72f),
                PerformanceMetric("Follow Through", 90, 100, 0.90f),
                PerformanceMetric("Club Face Control", 68, 100, 0.68f),
                PerformanceMetric("Body Rotation", 76, 100, 0.76f)
            ),
            goals = listOf(
                Goal(
                    title = "Practice 5 times this week",
                    description = "Complete 5 swing analysis sessions",
                    isCompleted = true
                ),
                Goal(
                    title = "Improve posture score to 90%",
                    description = "Focus on maintaining proper spine angle",
                    isCompleted = false
                ),
                Goal(
                    title = "Master the driver swing",
                    description = "Achieve consistent 80%+ scores with driver",
                    isCompleted = false
                ),
                Goal(
                    title = "Complete beginner course",
                    description = "Finish all basic swing technique lessons",
                    isCompleted = true
                )
            )
        )
    }
    
    private fun generateMockSessions(): List<PracticeSession> {
        return listOf(
            PracticeSession(
                date = "Today",
                score = 82,
                swingsAnalyzed = 15,
                duration = "25 min",
                improvements = listOf(
                    "Better follow through",
                    "Improved posture stability"
                )
            ),
            PracticeSession(
                date = "Yesterday",
                score = 76,
                swingsAnalyzed = 12,
                duration = "20 min",
                improvements = listOf(
                    "Consistent tempo",
                    "Better grip position"
                )
            ),
            PracticeSession(
                date = "2 days ago",
                score = 71,
                swingsAnalyzed = 18,
                duration = "30 min",
                improvements = listOf(
                    "Reduced head movement",
                    "Better shoulder turn"
                )
            ),
            PracticeSession(
                date = "4 days ago",
                score = 68,
                swingsAnalyzed = 10,
                duration = "15 min",
                improvements = listOf(
                    "Improved balance",
                    "Better weight transfer"
                )
            ),
            PracticeSession(
                date = "1 week ago",
                score = 74,
                swingsAnalyzed = 14,
                duration = "22 min",
                improvements = listOf(
                    "Consistent backswing",
                    "Better timing"
                )
            )
        )
    }
    
    fun refreshData() {
        loadProgressData()
    }
}

data class ProgressUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)