package com.swingsync.ai.data.remote

import com.swingsync.ai.data.models.SwingAnalysis
import com.swingsync.ai.data.models.UserProfile
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // User Profile endpoints
    @GET("users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<UserProfile>
    
    @POST("users")
    suspend fun createUserProfile(@Body userProfile: UserProfile): Response<UserProfile>
    
    @PUT("users/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body userProfile: UserProfile
    ): Response<UserProfile>
    
    @DELETE("users/{userId}")
    suspend fun deleteUserProfile(@Path("userId") userId: String): Response<Unit>
    
    // Swing Analysis endpoints
    @GET("analyses/{userId}")
    suspend fun getUserAnalyses(@Path("userId") userId: String): Response<List<SwingAnalysis>>
    
    @GET("analyses/analysis/{analysisId}")
    suspend fun getAnalysis(@Path("analysisId") analysisId: String): Response<SwingAnalysis>
    
    @POST("analyses")
    suspend fun createAnalysis(@Body analysis: SwingAnalysis): Response<SwingAnalysis>
    
    @PUT("analyses/{analysisId}")
    suspend fun updateAnalysis(
        @Path("analysisId") analysisId: String,
        @Body analysis: SwingAnalysis
    ): Response<SwingAnalysis>
    
    @DELETE("analyses/{analysisId}")
    suspend fun deleteAnalysis(@Path("analysisId") analysisId: String): Response<Unit>
    
    // Video upload endpoint
    @Multipart
    @POST("videos/upload")
    suspend fun uploadVideo(
        @Part("userId") userId: RequestBody,
        @Part("swingType") swingType: RequestBody,
        @Part video: MultipartBody.Part
    ): Response<VideoUploadResponse>
    
    // Real-time analysis endpoint
    @POST("analyses/realtime")
    suspend fun startRealtimeAnalysis(@Body request: RealtimeAnalysisRequest): Response<RealtimeAnalysisResponse>
    
    // Pose data endpoint
    @POST("poses")
    suspend fun submitPoseData(@Body poseData: PoseDataRequest): Response<PoseAnalysisResponse>
    
    // Statistics endpoints
    @GET("stats/{userId}")
    suspend fun getUserStats(@Path("userId") userId: String): Response<UserStats>
    
    @GET("stats/{userId}/progress")
    suspend fun getUserProgress(
        @Path("userId") userId: String,
        @Query("days") days: Int = 30
    ): Response<ProgressData>
    
    // Coaching endpoints
    @GET("coaching/{userId}/recommendations")
    suspend fun getCoachingRecommendations(@Path("userId") userId: String): Response<List<CoachingTip>>
    
    @POST("coaching/feedback")
    suspend fun submitFeedback(@Body feedback: FeedbackRequest): Response<Unit>
}

// Response data classes
data class VideoUploadResponse(
    val videoId: String,
    val uploadUrl: String,
    val analysisId: String
)

data class RealtimeAnalysisRequest(
    val userId: String,
    val sessionId: String,
    val swingType: String
)

data class RealtimeAnalysisResponse(
    val sessionId: String,
    val websocketUrl: String,
    val token: String
)

data class PoseDataRequest(
    val userId: String,
    val sessionId: String,
    val timestamp: Long,
    val landmarks: List<PoseLandmarkData>
)

data class PoseLandmarkData(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
)

data class PoseAnalysisResponse(
    val feedback: String,
    val score: Float,
    val phase: String,
    val corrections: List<String>
)

data class UserStats(
    val totalSwings: Int,
    val averageScore: Float,
    val bestScore: Float,
    val improvementRate: Float,
    val favoriteClub: String,
    val totalPracticeTime: Long
)

data class ProgressData(
    val scores: List<ScorePoint>,
    val improvements: List<String>,
    val weakAreas: List<String>,
    val streaks: StreakData
)

data class ScorePoint(
    val date: String,
    val score: Float,
    val swingType: String
)

data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val practiceGoal: Int,
    val practiceCompleted: Int
)

data class CoachingTip(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: Int,
    val videoUrl: String?
)

data class FeedbackRequest(
    val analysisId: String,
    val rating: Int,
    val comments: String,
    val helpfulness: Int
)