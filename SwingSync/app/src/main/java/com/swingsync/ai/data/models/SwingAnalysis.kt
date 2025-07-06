package com.swingsync.ai.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

@Entity(tableName = "swing_analysis")
data class SwingAnalysis(
    @PrimaryKey
    @SerializedName("id")
    val id: String,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("video_path")
    val videoPath: String,
    
    @SerializedName("timestamp")
    val timestamp: Date,
    
    @SerializedName("pose_data")
    val poseData: String, // JSON string of pose landmarks
    
    @SerializedName("analysis_results")
    val analysisResults: AnalysisResults,
    
    @SerializedName("feedback")
    val feedback: String,
    
    @SerializedName("score")
    val score: Float,
    
    @SerializedName("swing_type")
    val swingType: SwingType,
    
    @SerializedName("is_synced")
    val isSynced: Boolean = false
)

data class AnalysisResults(
    @SerializedName("backswing_score")
    val backswingScore: Float,
    
    @SerializedName("downswing_score")
    val downswingScore: Float,
    
    @SerializedName("impact_score")
    val impactScore: Float,
    
    @SerializedName("follow_through_score")
    val followThroughScore: Float,
    
    @SerializedName("tempo_score")
    val tempoScore: Float,
    
    @SerializedName("balance_score")
    val balanceScore: Float,
    
    @SerializedName("plane_score")
    val planeScore: Float,
    
    @SerializedName("key_issues")
    val keyIssues: List<String>,
    
    @SerializedName("improvements")
    val improvements: List<String>
)

enum class SwingType {
    @SerializedName("driver")
    DRIVER,
    
    @SerializedName("iron")
    IRON,
    
    @SerializedName("wedge")
    WEDGE,
    
    @SerializedName("putter")
    PUTTER
}

data class PoseLandmark(
    @SerializedName("x")
    val x: Float,
    
    @SerializedName("y")
    val y: Float,
    
    @SerializedName("z")
    val z: Float,
    
    @SerializedName("visibility")
    val visibility: Float
)

data class SwingPhase(
    @SerializedName("phase_name")
    val phaseName: String,
    
    @SerializedName("start_frame")
    val startFrame: Int,
    
    @SerializedName("end_frame")
    val endFrame: Int,
    
    @SerializedName("duration_ms")
    val durationMs: Long
)