package com.swingsync.ai.data.repository

import com.swingsync.ai.data.local.dao.SwingAnalysisDao
import com.swingsync.ai.data.models.SwingAnalysis
import com.swingsync.ai.data.remote.ApiService
import com.swingsync.ai.data.remote.WebSocketService
import com.swingsync.ai.domain.repository.SwingAnalysisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class SwingAnalysisRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val swingAnalysisDao: SwingAnalysisDao,
    private val webSocketService: WebSocketService
) : SwingAnalysisRepository {
    
    override fun getAllAnalyses(userId: String): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getAllAnalyses(userId)
    }
    
    override suspend fun getAnalysisById(id: String): SwingAnalysis? {
        return swingAnalysisDao.getAnalysisById(id)
    }
    
    override suspend fun createAnalysis(analysis: SwingAnalysis): Result<SwingAnalysis> {
        return try {
            // Save locally first
            swingAnalysisDao.insertAnalysis(analysis)
            
            // Try to sync with server
            val response = apiService.createAnalysis(analysis)
            if (response.isSuccessful) {
                response.body()?.let { serverAnalysis ->
                    swingAnalysisDao.insertAnalysis(serverAnalysis.copy(isSynced = true))
                    Result.success(serverAnalysis)
                } ?: Result.failure(Exception("Server response body is null"))
            } else {
                Timber.w("Failed to sync analysis with server: ${response.code()}")
                Result.success(analysis) // Return local analysis if server sync fails
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating analysis")
            Result.failure(e)
        }
    }
    
    override suspend fun updateAnalysis(analysis: SwingAnalysis): Result<SwingAnalysis> {
        return try {
            swingAnalysisDao.updateAnalysis(analysis)
            
            // Try to sync with server
            val response = apiService.updateAnalysis(analysis.id, analysis)
            if (response.isSuccessful) {
                response.body()?.let { serverAnalysis ->
                    swingAnalysisDao.insertAnalysis(serverAnalysis.copy(isSynced = true))
                    Result.success(serverAnalysis)
                } ?: Result.failure(Exception("Server response body is null"))
            } else {
                Timber.w("Failed to sync updated analysis with server: ${response.code()}")
                Result.success(analysis)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating analysis")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAnalysis(id: String): Result<Unit> {
        return try {
            swingAnalysisDao.deleteAnalysisById(id)
            
            // Try to delete from server
            val response = apiService.deleteAnalysis(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Timber.w("Failed to delete analysis from server: ${response.code()}")
                Result.success(Unit) // Local deletion succeeded
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting analysis")
            Result.failure(e)
        }
    }
    
    override suspend fun syncAnalyses(userId: String): Result<Unit> {
        return try {
            // Get server analyses
            val response = apiService.getUserAnalyses(userId)
            if (response.isSuccessful) {
                response.body()?.let { serverAnalyses ->
                    swingAnalysisDao.insertAnalyses(serverAnalyses.map { it.copy(isSynced = true) })
                }
            }
            
            // Upload unsynced local analyses
            val unsyncedAnalyses = swingAnalysisDao.getUnsyncedAnalyses()
            unsyncedAnalyses.forEach { analysis ->
                try {
                    val syncResponse = apiService.createAnalysis(analysis)
                    if (syncResponse.isSuccessful) {
                        swingAnalysisDao.markAsSynced(analysis.id)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync analysis ${analysis.id}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error syncing analyses")
            Result.failure(e)
        }
    }
    
    override suspend fun getTopAnalyses(userId: String, limit: Int): List<SwingAnalysis> {
        return swingAnalysisDao.getTopAnalyses(userId, limit)
    }
    
    override suspend fun getAverageScore(userId: String): Float {
        return swingAnalysisDao.getAverageScore(userId) ?: 0.0f
    }
    
    override fun getAnalysesByType(userId: String, swingType: String): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getAnalysesByType(userId, swingType)
    }
    
    override suspend fun getAnalysisCount(userId: String): Int {
        return swingAnalysisDao.getAnalysisCount(userId)
    }
    
    override suspend fun refreshAnalyses(userId: String): Result<Unit> {
        return try {
            val response = apiService.getUserAnalyses(userId)
            if (response.isSuccessful) {
                response.body()?.let { analyses ->
                    swingAnalysisDao.insertAnalyses(analyses.map { it.copy(isSynced = true) })
                    Result.success(Unit)
                } ?: Result.failure(Exception("Server response body is null"))
            } else {
                Result.failure(Exception("Failed to refresh analyses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing analyses")
            Result.failure(e)
        }
    }
}