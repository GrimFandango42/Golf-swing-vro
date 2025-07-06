package com.swingsync.ai.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    
    private val _messageFlow = MutableSharedFlow<WebSocketMessage>()
    val messageFlow: SharedFlow<WebSocketMessage> = _messageFlow.asSharedFlow()
    
    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()
    
    fun connect(url: String, token: String) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connection opened")
                _connectionState.tryEmit(ConnectionState.CONNECTED)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("WebSocket message received: $text")
                try {
                    val message = gson.fromJson(text, WebSocketMessage::class.java)
                    _messageFlow.tryEmit(message)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing WebSocket message")
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket connection closing: $code $reason")
                _connectionState.tryEmit(ConnectionState.CLOSING)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket connection closed: $code $reason")
                _connectionState.tryEmit(ConnectionState.CLOSED)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket connection failed")
                _connectionState.tryEmit(ConnectionState.ERROR)
            }
        })
    }
    
    fun sendMessage(message: WebSocketMessage) {
        val json = gson.toJson(message)
        webSocket?.send(json)
        Timber.d("WebSocket message sent: $json")
    }
    
    fun sendPoseData(poseData: PoseDataMessage) {
        val message = WebSocketMessage(
            type = MessageType.POSE_DATA,
            data = gson.toJson(poseData)
        )
        sendMessage(message)
    }
    
    fun sendVideoFrame(frameData: VideoFrameMessage) {
        val message = WebSocketMessage(
            type = MessageType.VIDEO_FRAME,
            data = gson.toJson(frameData)
        )
        sendMessage(message)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _connectionState.tryEmit(ConnectionState.DISCONNECTED)
    }
    
    fun isConnected(): Boolean {
        return webSocket != null
    }
}

data class WebSocketMessage(
    val type: MessageType,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    POSE_DATA,
    VIDEO_FRAME,
    ANALYSIS_RESULT,
    COACHING_TIP,
    ERROR,
    HEARTBEAT
}

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    CLOSING,
    CLOSED,
    DISCONNECTED,
    ERROR
}

data class PoseDataMessage(
    val sessionId: String,
    val timestamp: Long,
    val landmarks: List<PoseLandmarkData>,
    val frameNumber: Int
)

data class VideoFrameMessage(
    val sessionId: String,
    val timestamp: Long,
    val frameData: String, // Base64 encoded frame
    val frameNumber: Int,
    val width: Int,
    val height: Int
)

data class AnalysisResultMessage(
    val sessionId: String,
    val timestamp: Long,
    val feedback: String,
    val score: Float,
    val phase: String,
    val corrections: List<String>
)

data class CoachingTipMessage(
    val sessionId: String,
    val timestamp: Long,
    val tip: String,
    val category: String,
    val priority: Int
)

data class ErrorMessage(
    val sessionId: String,
    val timestamp: Long,
    val error: String,
    val code: Int
)