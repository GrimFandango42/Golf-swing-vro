package com.swingsync.ai.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private val _swingHistory = MutableStateFlow<List<SwingRecord>>(emptyList())
    val swingHistory: StateFlow<List<SwingRecord>> = _swingHistory.asStateFlow()
    
    init {
        loadSwingHistory()
    }
    
    private fun loadSwingHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Load from repository
                val history = historyRepository.getSwingHistory()
                _swingHistory.value = history
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load swing history: ${e.message}"
                )
                
                // Load mock data for demo
                _swingHistory.value = generateMockHistory()
            }
        }
    }
    
    fun playbackSwing(swingId: String) {
        viewModelScope.launch {
            try {
                // Implement swing playback functionality
                // This would load and play the recorded swing video
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to play swing: ${e.message}"
                )
            }
        }
    }
    
    fun shareSwing(swingId: String) {
        viewModelScope.launch {
            try {
                // Implement swing sharing functionality
                // This would create a shareable link or export video
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to share swing: ${e.message}"
                )
            }
        }
    }
    
    fun deleteSwing(swingId: String) {
        viewModelScope.launch {
            try {
                // Delete from repository
                historyRepository.deleteSwing(swingId)
                
                // Update local state
                _swingHistory.value = _swingHistory.value.filter { it.id != swingId }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete swing: ${e.message}"
                )
            }
        }
    }
    
    fun refreshHistory() {
        loadSwingHistory()
    }
    
    private fun generateMockHistory(): List<SwingRecord> {
        return listOf(
            SwingRecord(
                id = "1",
                date = "Today",
                time = "2:30 PM",
                score = 85,
                clubType = "Driver",
                conditions = "Indoor",
                keyFeedback = listOf("Great follow through", "Good posture", "Steady head position")
            ),
            SwingRecord(
                id = "2", 
                date = "Yesterday",
                time = "10:15 AM",
                score = 78,
                clubType = "7 Iron",
                conditions = "Practice Range",
                keyFeedback = listOf("Improved tempo", "Better grip", "Good balance")
            ),
            SwingRecord(
                id = "3",
                date = "2 days ago",
                time = "4:45 PM", 
                score = 72,
                clubType = "Pitching Wedge",
                conditions = "Outdoor",
                keyFeedback = listOf("Consistent backswing", "Clean contact", "Nice rhythm")
            ),
            SwingRecord(
                id = "4",
                date = "3 days ago",
                time = "11:20 AM",
                score = 69,
                clubType = "Driver",
                conditions = "Windy",
                keyFeedback = listOf("Work on follow through", "Maintain posture", "Focus on timing")
            ),
            SwingRecord(
                id = "5",
                date = "1 week ago", 
                time = "3:10 PM",
                score = 81,
                clubType = "5 Iron",
                conditions = "Indoor",
                keyFeedback = listOf("Excellent form", "Great extension", "Perfect tempo")
            ),
            SwingRecord(
                id = "6",
                date = "1 week ago",
                time = "9:30 AM",
                score = 74,
                clubType = "9 Iron",
                conditions = "Practice Range", 
                keyFeedback = listOf("Good setup", "Smooth transition", "Clean finish")
            ),
            SwingRecord(
                id = "7",
                date = "2 weeks ago",
                time = "1:45 PM",
                score = 66,
                clubType = "Driver",
                conditions = "Outdoor",
                keyFeedback = listOf("Work on alignment", "Slower backswing", "Better weight shift")
            ),
            SwingRecord(
                id = "8",
                date = "2 weeks ago",
                time = "5:00 PM",
                score = 79,
                clubType = "Sand Wedge",
                conditions = "Bunker Practice",
                keyFeedback = listOf("Good technique", "Proper stance", "Nice acceleration")
            )
        )
    }
}

data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)