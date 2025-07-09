package com.swingsync.ai.data.local.dao

import androidx.room.*
import com.swingsync.ai.data.local.entity.PoseDetectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for pose detection operations.
 */
@Dao
interface PoseDetectionDao {
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId ORDER BY frame_number ASC")
    fun getPoseDetectionsBySessionId(sessionId: String): Flow<List<PoseDetectionEntity>>
    
    @Query("SELECT * FROM pose_detections WHERE detection_id = :detectionId")
    suspend fun getPoseDetectionById(detectionId: String): PoseDetectionEntity?
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId AND frame_number = :frameNumber")
    suspend fun getPoseDetectionByFrame(sessionId: String, frameNumber: Int): PoseDetectionEntity?
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId AND frame_number >= :startFrame AND frame_number <= :endFrame ORDER BY frame_number ASC")
    fun getPoseDetectionsByFrameRange(sessionId: String, startFrame: Int, endFrame: Int): Flow<List<PoseDetectionEntity>>
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId AND confidence >= :minConfidence ORDER BY frame_number ASC")
    fun getHighConfidencePoseDetections(sessionId: String, minConfidence: Float): Flow<List<PoseDetectionEntity>>
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId ORDER BY frame_number ASC LIMIT :limit")
    fun getPoseDetectionsLimited(sessionId: String, limit: Int): Flow<List<PoseDetectionEntity>>
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getPoseDetectionsByTimeRange(sessionId: String, startTime: Long, endTime: Long): Flow<List<PoseDetectionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoseDetection(poseDetection: PoseDetectionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoseDetections(poseDetections: List<PoseDetectionEntity>)
    
    @Update
    suspend fun updatePoseDetection(poseDetection: PoseDetectionEntity)
    
    @Delete
    suspend fun deletePoseDetection(poseDetection: PoseDetectionEntity)
    
    @Query("DELETE FROM pose_detections WHERE detection_id = :detectionId")
    suspend fun deletePoseDetectionById(detectionId: String)
    
    @Query("DELETE FROM pose_detections WHERE session_id = :sessionId")
    suspend fun deletePoseDetectionsBySessionId(sessionId: String)
    
    @Query("DELETE FROM pose_detections WHERE session_id IN (SELECT session_id FROM swing_sessions WHERE user_id = :userId)")
    suspend fun deleteAllPoseDetectionsForUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM pose_detections WHERE session_id = :sessionId")
    suspend fun getPoseDetectionCount(sessionId: String): Int
    
    @Query("SELECT AVG(confidence) FROM pose_detections WHERE session_id = :sessionId")
    suspend fun getAverageConfidence(sessionId: String): Float?
    
    @Query("SELECT MAX(confidence) FROM pose_detections WHERE session_id = :sessionId")
    suspend fun getBestConfidence(sessionId: String): Float?
    
    @Query("SELECT MIN(confidence) FROM pose_detections WHERE session_id = :sessionId")
    suspend fun getWorstConfidence(sessionId: String): Float?
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId ORDER BY confidence DESC LIMIT 1")
    suspend fun getBestPoseDetection(sessionId: String): PoseDetectionEntity?
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId ORDER BY confidence ASC LIMIT 1")
    suspend fun getWorstPoseDetection(sessionId: String): PoseDetectionEntity?
    
    @Query("SELECT frame_number, confidence FROM pose_detections WHERE session_id = :sessionId ORDER BY frame_number ASC")
    suspend fun getConfidenceOverTime(sessionId: String): List<FrameConfidence>
    
    @Query("SELECT COUNT(*) FROM pose_detections WHERE session_id = :sessionId AND confidence >= :threshold")
    suspend fun getHighConfidenceFrameCount(sessionId: String, threshold: Float): Int
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId ORDER BY frame_number ASC LIMIT 1")
    suspend fun getFirstPoseDetection(sessionId: String): PoseDetectionEntity?
    
    @Query("SELECT * FROM pose_detections WHERE session_id = :sessionId ORDER BY frame_number DESC LIMIT 1")
    suspend fun getLastPoseDetection(sessionId: String): PoseDetectionEntity?
}

/**
 * Data class for frame confidence data.
 */
data class FrameConfidence(
    val frame_number: Int,
    val confidence: Float
)