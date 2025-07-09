package com.swingsync.ai.data.remote.api

import com.swingsync.ai.data.remote.dto.*
import retrofit2.http.*

/**
 * REST API service interface for swing analysis operations.
 */
interface SwingAnalysisApiService {
    
    @POST("api/v1/swing/analyze")
    suspend fun analyzeSwing(@Body request: SwingAnalysisRequestDto): SwingAnalysisResponseDto
    
    @Multipart
    @POST("api/v1/swing/upload")
    suspend fun uploadSwingVideo(
        @Part("video") videoData: ByteArray,
        @Part("session_id") sessionId: String
    ): VideoUploadResponseDto
    
    @GET("api/v1/swing/feedback/{analysisId}")
    suspend fun getSwingFeedback(@Path("analysisId") analysisId: String): SwingFeedbackResponseDto
    
    @POST("api/v1/user/feedback")
    suspend fun submitUserFeedback(@Body feedback: UserFeedbackDto)
    
    @GET("api/v1/coaching/tips")
    suspend fun getCoachingTips(
        @Query("user_level") userLevel: String,
        @Query("club_type") clubType: String
    ): List<CoachingTipDto>
    
    @POST("api/v1/user/{userId}/progress")
    suspend fun syncUserProgress(
        @Path("userId") userId: String,
        @Body progressData: UserProgressDto
    )
    
    @GET("api/v1/leaderboard")
    suspend fun getLeaderboard(
        @Query("category") category: String,
        @Query("timeframe") timeframe: String
    ): LeaderboardResponseDto
    
    @GET("api/v1/user/{userId}/achievements")
    suspend fun getUserAchievements(@Path("userId") userId: String): List<AchievementDto>
    
    @POST("api/v1/user/{userId}/achievement")
    suspend fun unlockAchievement(
        @Path("userId") userId: String,
        @Body achievement: AchievementDto
    )
    
    @GET("api/v1/swing/history/{userId}")
    suspend fun getSwingHistory(
        @Path("userId") userId: String,
        @Query("limit") limit: Int?,
        @Query("offset") offset: Int?
    ): SwingHistoryResponseDto
    
    @POST("api/v1/swing/favorite")
    suspend fun markSwingAsFavorite(@Body request: FavoriteSwingDto)
    
    @DELETE("api/v1/swing/favorite/{swingId}")
    suspend fun removeSwingFromFavorites(@Path("swingId") swingId: String)
    
    @GET("api/v1/user/{userId}/settings")
    suspend fun getUserSettings(@Path("userId") userId: String): UserSettingsDto
    
    @PUT("api/v1/user/{userId}/settings")
    suspend fun updateUserSettings(
        @Path("userId") userId: String,
        @Body settings: UserSettingsDto
    )
}