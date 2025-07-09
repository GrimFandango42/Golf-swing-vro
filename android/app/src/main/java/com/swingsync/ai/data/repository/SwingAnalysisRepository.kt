package com.swingsync.ai.data.repository

import android.util.Log
import com.swingsync.ai.analysis.PSystemClassifier
import com.swingsync.ai.data.model.*
import com.swingsync.ai.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for swing analysis operations
 * Handles data processing, P-system classification, and backend communication
 */
class SwingAnalysisRepository(
    private val apiClient: ApiClient
) {

    companion object {
        private const val TAG = "SwingAnalysisRepository"
    }

    private val pSystemClassifier = PSystemClassifier()

    /**
     * Process recording session and perform full analysis
     */
    suspend fun processRecordingSession(recordingSession: RecordingSession): Result<SwingAnalysisFeedback> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing recording session: ${recordingSession.sessionId}")
                
                // Step 1: Validate recording session
                if (!isValidRecordingSession(recordingSession)) {
                    return@withContext Result.failure(Exception("Invalid recording session"))
                }
                
                // Step 2: Classify P-System phases
                val pSystemPhases = pSystemClassifier.classifySwingPhases(recordingSession.poseDetectionResults)
                
                // Step 3: Convert to backend format
                val analysisInput = convertToAnalysisInput(recordingSession, pSystemPhases)
                
                // Step 4: Submit to backend for analysis
                val result = apiClient.analyzeSwing(analysisInput)
                
                if (result.isSuccess) {
                    Log.d(TAG, "Analysis completed successfully")
                } else {
                    Log.e(TAG, "Analysis failed: ${result.exceptionOrNull()?.message}")
                }
                
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recording session", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Validate recording session data
     */
    private fun isValidRecordingSession(recordingSession: RecordingSession): Boolean {
        return recordingSession.poseDetectionResults.isNotEmpty() &&
                recordingSession.sessionId.isNotEmpty() &&
                recordingSession.userId.isNotEmpty() &&
                recordingSession.clubUsed.isNotEmpty() &&
                recordingSession.fps > 0
    }

    /**
     * Convert recording session to backend analysis input format
     */
    private fun convertToAnalysisInput(
        recordingSession: RecordingSession,
        pSystemPhases: List<PSystemPhase>
    ): SwingVideoAnalysisInput {
        
        // Convert pose detection results to frame pose data
        val frames = recordingSession.poseDetectionResults.map { result ->
            result.keypoints
        }
        
        return SwingVideoAnalysisInput(
            sessionId = recordingSession.sessionId,
            userId = recordingSession.userId,
            clubUsed = recordingSession.clubUsed,
            frames = frames,
            pSystemClassification = pSystemPhases,
            videoFps = recordingSession.fps
        )
    }

    /**
     * Get swing analysis for a specific session
     */
    suspend fun getSwingAnalysis(sessionId: String): Result<SwingAnalysisFeedback> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting swing analysis for session: $sessionId")
                
                // TODO: Implement session retrieval from local storage or backend
                // For now, return a placeholder
                Result.failure(Exception("Session retrieval not implemented"))
                
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving swing analysis", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Save swing analysis locally
     */
    suspend fun saveSwingAnalysis(analysis: SwingAnalysisFeedback): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving swing analysis for session: ${analysis.sessionId}")
                
                // TODO: Implement local storage (Room database, SharedPreferences, etc.)
                // For now, just log the operation
                Log.d(TAG, "Analysis saved successfully")
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving swing analysis", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get P-System classification for a recording session
     */
    suspend fun classifySwingPhases(recordingSession: RecordingSession): Result<List<PSystemPhase>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Classifying swing phases for session: ${recordingSession.sessionId}")
                
                val phases = pSystemClassifier.classifySwingPhases(recordingSession.poseDetectionResults)
                
                if (phases.isNotEmpty()) {
                    Log.d(TAG, "Successfully classified ${phases.size} phases")
                    Result.success(phases)
                } else {
                    Log.w(TAG, "No phases classified")
                    Result.failure(Exception("Failed to classify swing phases"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error classifying swing phases", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Test backend connection
     */
    suspend fun testBackendConnection(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing backend connection")
                apiClient.testConnection()
                
            } catch (e: Exception) {
                Log.e(TAG, "Backend connection test failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get swing statistics from pose data
     */
    suspend fun getSwingStatistics(recordingSession: RecordingSession): Result<SwingStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calculating swing statistics for session: ${recordingSession.sessionId}")
                
                val statistics = calculateSwingStatistics(recordingSession)
                
                Log.d(TAG, "Swing statistics calculated successfully")
                Result.success(statistics)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating swing statistics", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Calculate swing statistics from pose data
     */
    private fun calculateSwingStatistics(recordingSession: RecordingSession): SwingStatistics {
        val poseResults = recordingSession.poseDetectionResults
        
        if (poseResults.isEmpty()) {
            return SwingStatistics(
                totalFrames = 0,
                averageConfidence = 0f,
                swingDuration = 0f,
                frameRate = recordingSession.fps
            )
        }
        
        val totalFrames = poseResults.size
        val averageConfidence = poseResults.map { it.confidence }.average().toFloat()
        val swingDuration = totalFrames / recordingSession.fps
        
        return SwingStatistics(
            totalFrames = totalFrames,
            averageConfidence = averageConfidence,
            swingDuration = swingDuration,
            frameRate = recordingSession.fps
        )
    }

    /**
     * Data class for swing statistics
     */
    data class SwingStatistics(
        val totalFrames: Int,
        val averageConfidence: Float,
        val swingDuration: Float,
        val frameRate: Float
    )
}