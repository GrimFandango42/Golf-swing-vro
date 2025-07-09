package com.swingsync.ai.domain.util

/**
 * Specific error types for swing analysis operations.
 * This provides more granular error handling for different failure scenarios.
 */
sealed class SwingAnalysisError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Camera related errors
     */
    sealed class CameraError(message: String, cause: Throwable? = null) : SwingAnalysisError(message, cause) {
        object CameraInitializationFailed : CameraError("Failed to initialize camera")
        object CameraPermissionDenied : CameraError("Camera permission denied")
        object CameraUnavailable : CameraError("Camera is not available")
        object CameraConfigurationFailed : CameraError("Failed to configure camera")
        class CameraOperationFailed(message: String, cause: Throwable? = null) : CameraError(message, cause)
    }
    
    /**
     * Pose detection related errors
     */
    sealed class PoseDetectionError(message: String, cause: Throwable? = null) : SwingAnalysisError(message, cause) {
        object MediaPipeInitializationFailed : PoseDetectionError("Failed to initialize MediaPipe")
        object PoseDetectionFailed : PoseDetectionError("Failed to detect pose")
        object InsufficientPoseData : PoseDetectionError("Insufficient pose data for analysis")
        object PoseDetectionTimeout : PoseDetectionError("Pose detection timed out")
        class PoseProcessingFailed(message: String, cause: Throwable? = null) : PoseDetectionError(message, cause)
    }
    
    /**
     * Swing analysis related errors
     */
    sealed class AnalysisError(message: String, cause: Throwable? = null) : SwingAnalysisError(message, cause) {
        object AnalysisServiceUnavailable : AnalysisError("Analysis service is unavailable")
        object AnalysisTimeout : AnalysisError("Analysis operation timed out")
        object InvalidSwingData : AnalysisError("Invalid swing data provided")
        object AnalysisProcessingFailed : AnalysisError("Failed to process swing analysis")
        class AnalysisComputationFailed(message: String, cause: Throwable? = null) : AnalysisError(message, cause)
    }
    
    /**
     * Network related errors
     */
    sealed class NetworkError(message: String, cause: Throwable? = null) : SwingAnalysisError(message, cause) {
        object NoInternetConnection : NetworkError("No internet connection available")
        object NetworkTimeout : NetworkError("Network operation timed out")
        object ServerError : NetworkError("Server error occurred")
        object ApiKeyInvalid : NetworkError("Invalid API key")
        object RateLimitExceeded : NetworkError("Rate limit exceeded")
        class NetworkOperationFailed(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    }
    
    /**
     * Database related errors
     */
    sealed class DatabaseError(message: String, cause: Throwable? = null) : SwingAnalysisError(message, cause) {
        object DatabaseInitializationFailed : DatabaseError("Failed to initialize database")
        object DatabaseOperationFailed : DatabaseError("Database operation failed")
        object DataNotFound : DatabaseError("Requested data not found")
        object DataCorrupted : DatabaseError("Data is corrupted")
        class DatabaseAccessFailed(message: String, cause: Throwable? = null) : DatabaseError(message, cause)
    }
    
    /**
     * Generic system errors
     */
    sealed class SystemError(message: String, cause: Throwable? = null) : SwingAnalysisError(message, cause) {
        object InsufficientMemory : SystemError("Insufficient memory available")
        object InsufficientStorageSpace : SystemError("Insufficient storage space")
        object PermissionDenied : SystemError("Required permission denied")
        object ServiceUnavailable : SystemError("Required service is unavailable")
        class UnexpectedError(message: String, cause: Throwable? = null) : SystemError(message, cause)
    }
}

/**
 * Extension function to convert generic exceptions to specific SwingAnalysisError types.
 */
fun Exception.toSwingAnalysisError(): SwingAnalysisError {
    return when (this) {
        is SwingAnalysisError -> this
        is SecurityException -> SwingAnalysisError.SystemError.PermissionDenied
        is OutOfMemoryError -> SwingAnalysisError.SystemError.InsufficientMemory
        is java.net.SocketTimeoutException -> SwingAnalysisError.NetworkError.NetworkTimeout
        is java.net.UnknownHostException -> SwingAnalysisError.NetworkError.NoInternetConnection
        is java.io.IOException -> SwingAnalysisError.NetworkError.NetworkOperationFailed(
            message ?: "Network operation failed", 
            this
        )
        else -> SwingAnalysisError.SystemError.UnexpectedError(
            message ?: "Unexpected error occurred", 
            this
        )
    }
}

/**
 * Extension function to get user-friendly error messages.
 */
fun SwingAnalysisError.getUserFriendlyMessage(): String {
    return when (this) {
        // Camera errors
        is SwingAnalysisError.CameraError.CameraInitializationFailed -> "Camera could not be started. Please try again."
        is SwingAnalysisError.CameraError.CameraPermissionDenied -> "Camera permission is required for swing analysis."
        is SwingAnalysisError.CameraError.CameraUnavailable -> "Camera is not available. Please check if another app is using it."
        is SwingAnalysisError.CameraError.CameraConfigurationFailed -> "Camera setup failed. Please restart the app."
        
        // Pose detection errors
        is SwingAnalysisError.PoseDetectionError.MediaPipeInitializationFailed -> "Pose detection system could not be initialized."
        is SwingAnalysisError.PoseDetectionError.PoseDetectionFailed -> "Could not detect your pose. Please ensure good lighting and position."
        is SwingAnalysisError.PoseDetectionError.InsufficientPoseData -> "Not enough pose data captured. Please try a longer recording."
        is SwingAnalysisError.PoseDetectionError.PoseDetectionTimeout -> "Pose detection is taking too long. Please try again."
        
        // Analysis errors
        is SwingAnalysisError.AnalysisError.AnalysisServiceUnavailable -> "Analysis service is temporarily unavailable."
        is SwingAnalysisError.AnalysisError.AnalysisTimeout -> "Analysis is taking too long. Please try again."
        is SwingAnalysisError.AnalysisError.InvalidSwingData -> "Invalid swing data. Please record a new swing."
        is SwingAnalysisError.AnalysisError.AnalysisProcessingFailed -> "Failed to analyze your swing. Please try again."
        
        // Network errors
        is SwingAnalysisError.NetworkError.NoInternetConnection -> "No internet connection. Please check your network settings."
        is SwingAnalysisError.NetworkError.NetworkTimeout -> "Network request timed out. Please try again."
        is SwingAnalysisError.NetworkError.ServerError -> "Server error occurred. Please try again later."
        is SwingAnalysisError.NetworkError.ApiKeyInvalid -> "Invalid API configuration. Please contact support."
        is SwingAnalysisError.NetworkError.RateLimitExceeded -> "Too many requests. Please wait a moment and try again."
        
        // Database errors
        is SwingAnalysisError.DatabaseError.DatabaseInitializationFailed -> "Database initialization failed. Please restart the app."
        is SwingAnalysisError.DatabaseError.DatabaseOperationFailed -> "Failed to save data. Please try again."
        is SwingAnalysisError.DatabaseError.DataNotFound -> "Requested data not found."
        is SwingAnalysisError.DatabaseError.DataCorrupted -> "Data is corrupted. Please clear app data and try again."
        
        // System errors
        is SwingAnalysisError.SystemError.InsufficientMemory -> "Insufficient memory. Please close other apps and try again."
        is SwingAnalysisError.SystemError.InsufficientStorageSpace -> "Insufficient storage space. Please free up space and try again."
        is SwingAnalysisError.SystemError.PermissionDenied -> "Required permission denied. Please grant permissions in settings."
        is SwingAnalysisError.SystemError.ServiceUnavailable -> "Required service is unavailable. Please try again later."
        
        // Generic errors
        else -> "An unexpected error occurred. Please try again."
    }
}