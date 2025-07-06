package com.swingsync.ai.domain.repository

import com.swingsync.ai.data.models.SwingAnalysis
import kotlinx.coroutines.flow.Flow

interface SwingAnalysisRepository {
    
    fun getAllAnalyses(userId: String): Flow<List<SwingAnalysis>>
    
    suspend fun getAnalysisById(id: String): SwingAnalysis?
    
    suspend fun createAnalysis(analysis: SwingAnalysis): Result<SwingAnalysis>
    
    suspend fun updateAnalysis(analysis: SwingAnalysis): Result<SwingAnalysis>
    
    suspend fun deleteAnalysis(id: String): Result<Unit>
    
    suspend fun syncAnalyses(userId: String): Result<Unit>
    
    suspend fun getTopAnalyses(userId: String, limit: Int): List<SwingAnalysis>
    
    suspend fun getAverageScore(userId: String): Float
    
    fun getAnalysesByType(userId: String, swingType: String): Flow<List<SwingAnalysis>>
    
    suspend fun getAnalysisCount(userId: String): Int
    
    suspend fun refreshAnalyses(userId: String): Result<Unit>
}