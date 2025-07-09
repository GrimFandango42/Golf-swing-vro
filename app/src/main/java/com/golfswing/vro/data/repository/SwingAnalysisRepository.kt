package com.golfswing.vro.data.repository

import com.golfswing.vro.data.database.SwingAnalysisDao
import com.golfswing.vro.data.entity.SwingAnalysisEntity
import com.golfswing.vro.data.model.SwingAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwingAnalysisRepository @Inject constructor(
    private val swingAnalysisDao: SwingAnalysisDao
) {
    
    fun getAllAnalyses(): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getAllAnalyses().map { entities ->
            entities.map { it.toSwingAnalysis() }
        }
    }
    
    suspend fun getAnalysisById(id: String): SwingAnalysis? {
        return swingAnalysisDao.getAnalysisById(id)?.toSwingAnalysis()
    }
    
    fun getAnalysesByProcessingStatus(isProcessed: Boolean): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getAnalysesByProcessingStatus(isProcessed).map { entities ->
            entities.map { it.toSwingAnalysis() }
        }
    }
    
    fun getAnalysesByDateRange(startTime: Long, endTime: Long): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getAnalysesByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toSwingAnalysis() }
        }
    }
    
    fun getRecentAnalyses(limit: Int): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getRecentAnalyses(limit).map { entities ->
            entities.map { it.toSwingAnalysis() }
        }
    }
    
    suspend fun insertAnalysis(analysis: SwingAnalysis) {
        swingAnalysisDao.insertAnalysis(analysis.toEntity())
    }
    
    suspend fun updateAnalysis(analysis: SwingAnalysis) {
        swingAnalysisDao.updateAnalysis(analysis.toEntity())
    }
    
    suspend fun deleteAnalysis(analysis: SwingAnalysis) {
        swingAnalysisDao.deleteAnalysisById(analysis.id)
    }
    
    suspend fun deleteAnalysisById(id: String) {
        swingAnalysisDao.deleteAnalysisById(id)
    }
    
    suspend fun deleteAllAnalyses() {
        swingAnalysisDao.deleteAllAnalyses()
    }
    
    suspend fun getAnalysisCount(): Int {
        return swingAnalysisDao.getAnalysisCount()
    }
    
    suspend fun getAverageScore(): Float? {
        return swingAnalysisDao.getAverageScore()
    }
    
    fun getAnalysesWithScoreAbove(minScore: Float): Flow<List<SwingAnalysis>> {
        return swingAnalysisDao.getAnalysesWithScoreAbove(minScore).map { entities ->
            entities.map { it.toSwingAnalysis() }
        }
    }
    
    private fun SwingAnalysisEntity.toSwingAnalysis(): SwingAnalysis {
        return SwingAnalysis(
            id = id,
            timestamp = timestamp,
            videoPath = videoPath,
            duration = duration,
            phases = phases,
            biomechanics = biomechanics,
            score = score,
            recommendations = recommendations,
            isProcessed = isProcessed
        )
    }
    
    private fun SwingAnalysis.toEntity(): SwingAnalysisEntity {
        return SwingAnalysisEntity(
            id = id,
            timestamp = timestamp,
            videoPath = videoPath,
            duration = duration,
            phases = phases,
            biomechanics = biomechanics,
            score = score,
            recommendations = recommendations,
            isProcessed = isProcessed
        )
    }
}