"""
Streaming Endpoints for Real-time Golf Swing Analysis.

This module provides WebSocket endpoints and streaming logic for real-time golf swing
analysis, enabling low-latency feedback during practice sessions and live coaching.

Key Features:
- WebSocket endpoints for real-time data streaming
- Frame-by-frame video processing with immediate feedback
- Live coaching session management
- Real-time KPI monitoring and fault detection
- Streaming integration with Gemini 2.5 Flash for instant feedback
- Performance optimization for low-latency analysis

Endpoints:
- `/ws/stream/{user_id}` - Main streaming endpoint for real-time analysis
- `/ws/coaching/{session_id}` - Live coaching session endpoint
- `/ws/monitor/{user_id}` - Performance monitoring endpoint
- `/api/stream/sessions` - REST API for session management
"""

import asyncio
import json
import time
import uuid
from typing import Dict, List, Optional, Any, AsyncGenerator
from datetime import datetime, timedelta
import logging

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, HTTPException, Depends, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, validator
from pydantic.dataclasses import dataclass

from websocket_manager import connection_manager, MessageType, WebSocketMessage
from live_analysis import LiveAnalysisEngine, FrameAnalysisResult, StreamingKPICalculator
from feedback_generation import (
    StreamingFeedbackGenerator, 
    generate_realtime_feedback,
    FeedbackContext,
    FeedbackMode,
    UserSkillLevel
)
from data_structures import (
    PoseKeypoint,
    FramePoseData,
    SwingVideoAnalysisInput,
    SwingAnalysisFeedback
)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create router for streaming endpoints
router = APIRouter(prefix="/stream", tags=["streaming"])

# --- Pydantic Models for Streaming ---

class PoseKeypointStreaming(BaseModel):
    """Pose keypoint for streaming (compatible with data_structures.PoseKeypoint)"""
    x: float
    y: float
    z: float
    visibility: Optional[float] = None

class StreamingFrameData(BaseModel):
    """Real-time frame data for streaming analysis"""
    frame_index: int
    timestamp: float
    keypoints: Dict[str, PoseKeypointStreaming]
    frame_metadata: Dict[str, Any] = Field(default_factory=dict)

class StreamingSessionConfig(BaseModel):
    """Configuration for streaming analysis session"""
    user_id: str
    session_name: str = Field(default="Live Analysis Session")
    club_used: str = "Unknown"
    skill_level: UserSkillLevel = UserSkillLevel.INTERMEDIATE
    feedback_mode: FeedbackMode = FeedbackMode.STREAMING
    analysis_frequency: int = Field(default=5, description="Analyze every N frames")
    feedback_threshold: float = Field(default=0.6, description="Minimum fault severity for feedback")
    enable_real_time_kpis: bool = True
    enable_instant_feedback: bool = True
    target_latency_ms: int = Field(default=100, description="Target analysis latency in milliseconds")

class CoachingSessionConfig(BaseModel):
    """Configuration for live coaching session"""
    session_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    coach_user_id: str
    student_user_id: str
    session_name: str
    duration_minutes: int = 60
    focus_areas: List[str] = Field(default_factory=list)
    drill_sequence: List[str] = Field(default_factory=list)
    recording_enabled: bool = True

class StreamingStatsRequest(BaseModel):
    """Request for streaming statistics"""
    user_id: str
    time_range_minutes: int = 30

class RealtimeAnalysisRequest(BaseModel):
    """Request for real-time analysis"""
    frame_data: StreamingFrameData
    session_config: StreamingSessionConfig
    
    @validator('frame_data')
    def validate_frame_data(cls, v):
        if not v.keypoints:
            raise ValueError("Frame data must contain keypoints")
        return v

class PerformanceMetrics(BaseModel):
    """Real-time performance metrics"""
    session_id: str
    frames_processed: int
    average_latency_ms: float
    kpis_calculated: int
    faults_detected: int
    feedback_generated: int
    timestamp: float = Field(default_factory=time.time)

# --- Session Management ---

class StreamingSessionManager:
    """Manages active streaming sessions"""
    
    def __init__(self):
        self.active_sessions: Dict[str, Dict[str, Any]] = {}
        self.user_sessions: Dict[str, str] = {}  # user_id -> session_id
        self.session_stats: Dict[str, PerformanceMetrics] = {}
        self.analysis_engine = LiveAnalysisEngine()
    
    def create_session(self, config: StreamingSessionConfig) -> str:
        """Create new streaming session"""
        session_id = str(uuid.uuid4())
        
        session_data = {
            "id": session_id,
            "config": config,
            "created_at": time.time(),
            "frames_processed": 0,
            "last_frame_time": None,
            "analysis_buffer": [],
            "kpi_calculator": StreamingKPICalculator(),
            "feedback_generator": StreamingFeedbackGenerator(),
            "status": "active"
        }
        
        self.active_sessions[session_id] = session_data
        self.user_sessions[config.user_id] = session_id
        
        # Initialize performance metrics
        self.session_stats[session_id] = PerformanceMetrics(
            session_id=session_id,
            frames_processed=0,
            average_latency_ms=0.0,
            kpis_calculated=0,
            faults_detected=0,
            feedback_generated=0
        )
        
        logger.info(f"Created streaming session {session_id} for user {config.user_id}")
        return session_id
    
    def get_session(self, session_id: str) -> Optional[Dict[str, Any]]:
        """Get session data"""
        return self.active_sessions.get(session_id)
    
    def get_user_session(self, user_id: str) -> Optional[str]:
        """Get active session for user"""
        return self.user_sessions.get(user_id)
    
    def end_session(self, session_id: str) -> bool:
        """End streaming session"""
        if session_id not in self.active_sessions:
            return False
        
        session_data = self.active_sessions[session_id]
        user_id = session_data["config"].user_id
        
        # Clean up
        del self.active_sessions[session_id]
        if user_id in self.user_sessions:
            del self.user_sessions[user_id]
        
        logger.info(f"Ended streaming session {session_id}")
        return True
    
    async def process_frame(self, session_id: str, frame_data: StreamingFrameData) -> Optional[FrameAnalysisResult]:
        """Process frame for real-time analysis"""
        session_data = self.get_session(session_id)
        if not session_data:
            return None
        
        start_time = time.time()
        
        # Update session stats
        session_data["frames_processed"] += 1
        session_data["last_frame_time"] = frame_data.timestamp
        
        config = session_data["config"]
        
        # Check if we should analyze this frame
        if session_data["frames_processed"] % config.analysis_frequency != 0:
            return None
        
        # Perform real-time analysis
        try:
            analysis_result = await self.analysis_engine.analyze_frame(
                frame_data=frame_data,
                session_context=session_data,
                config=config
            )
            
            # Update performance metrics
            latency_ms = (time.time() - start_time) * 1000
            stats = self.session_stats[session_id]
            stats.frames_processed = session_data["frames_processed"]
            stats.average_latency_ms = (stats.average_latency_ms + latency_ms) / 2
            
            if analysis_result and analysis_result.kpis:
                stats.kpis_calculated += len(analysis_result.kpis)
            
            if analysis_result and analysis_result.detected_faults:
                stats.faults_detected += len(analysis_result.detected_faults)
            
            return analysis_result
            
        except Exception as e:
            logger.error(f"Error processing frame in session {session_id}: {e}")
            return None

# Global session manager
session_manager = StreamingSessionManager()

# --- WebSocket Endpoints ---

@router.websocket("/ws/{user_id}")
async def streaming_websocket(websocket: WebSocket, user_id: str):
    """
    Main WebSocket endpoint for real-time swing analysis streaming.
    
    Handles:
    - Real-time frame data streaming
    - Immediate analysis and feedback
    - KPI monitoring
    - Performance metrics
    """
    connection_id = await connection_manager.connect(
        websocket, 
        user_id, 
        {"endpoint": "streaming", "connected_at": time.time()}
    )
    
    logger.info(f"Started streaming connection for user {user_id}")
    
    try:
        while True:
            # Receive message from client
            message = await connection_manager.receive_message(connection_id)
            if not message:
                break
            
            # Handle different message types
            if message.type == MessageType.FRAME_DATA.value:
                await handle_frame_data(connection_id, message, user_id)
            elif message.type == MessageType.START_SESSION.value:
                await handle_start_streaming_session(connection_id, message, user_id)
            elif message.type == MessageType.END_SESSION.value:
                await handle_end_streaming_session(connection_id, message, user_id)
            elif message.type == "get_stats":
                await handle_get_stats(connection_id, message, user_id)
            
    except WebSocketDisconnect:
        logger.info(f"Streaming WebSocket disconnected for user {user_id}")
    except Exception as e:
        logger.error(f"Error in streaming WebSocket for user {user_id}: {e}")
    finally:
        # Clean up session if active
        session_id = session_manager.get_user_session(user_id)
        if session_id:
            session_manager.end_session(session_id)
        
        await connection_manager.disconnect(connection_id)

@router.websocket("/ws/coaching/{session_id}")
async def coaching_websocket(websocket: WebSocket, session_id: str, user_id: str = Query(...)):
    """
    WebSocket endpoint for live coaching sessions.
    
    Enables real-time communication between coaches and students
    with synchronized swing analysis and feedback.
    """
    connection_id = await connection_manager.connect(
        websocket, 
        user_id, 
        {"endpoint": "coaching", "session_id": session_id}
    )
    
    # Join coaching session
    join_message = WebSocketMessage(
        type=MessageType.JOIN_SESSION.value,
        data={"session_id": session_id}
    )
    await connection_manager.message_router.route_message(connection_id, join_message)
    
    logger.info(f"User {user_id} joined coaching session {session_id}")
    
    try:
        while True:
            message = await connection_manager.receive_message(connection_id)
            if not message:
                break
            
            # Handle coaching-specific messages
            if message.type == MessageType.COACHING_TIP.value:
                await handle_coaching_tip(connection_id, message, session_id)
            elif message.type == MessageType.DRILL_SUGGESTION.value:
                await handle_drill_suggestion(connection_id, message, session_id)
            elif message.type == MessageType.FRAME_DATA.value:
                await handle_coaching_frame_data(connection_id, message, session_id)
            
    except WebSocketDisconnect:
        logger.info(f"Coaching WebSocket disconnected for user {user_id}")
    except Exception as e:
        logger.error(f"Error in coaching WebSocket: {e}")
    finally:
        await connection_manager.disconnect(connection_id)

@router.websocket("/ws/monitor/{user_id}")
async def monitoring_websocket(websocket: WebSocket, user_id: str):
    """
    WebSocket endpoint for performance monitoring and analytics.
    
    Provides real-time performance metrics, system health,
    and analysis statistics.
    """
    connection_id = await connection_manager.connect(
        websocket, 
        user_id, 
        {"endpoint": "monitoring"}
    )
    
    logger.info(f"Started monitoring connection for user {user_id}")
    
    try:
        # Start monitoring loop
        while True:
            # Send periodic performance updates
            await asyncio.sleep(5)  # Update every 5 seconds
            
            # Get session stats if user has active session
            session_id = session_manager.get_user_session(user_id)
            if session_id and session_id in session_manager.session_stats:
                stats = session_manager.session_stats[session_id]
                
                monitoring_message = WebSocketMessage(
                    type=MessageType.PERFORMANCE_METRICS.value,
                    data=stats.dict()
                )
                
                await connection_manager.send_message(connection_id, monitoring_message)
            
            # Check for incoming messages (non-blocking)
            try:
                message = await asyncio.wait_for(
                    connection_manager.receive_message(connection_id), 
                    timeout=0.1
                )
                if message and message.type == "get_system_stats":
                    await handle_system_stats_request(connection_id)
            except asyncio.TimeoutError:
                continue
            
    except WebSocketDisconnect:
        logger.info(f"Monitoring WebSocket disconnected for user {user_id}")
    except Exception as e:
        logger.error(f"Error in monitoring WebSocket: {e}")
    finally:
        await connection_manager.disconnect(connection_id)

# --- Message Handlers ---

async def handle_frame_data(connection_id: str, message: WebSocketMessage, user_id: str):
    """Handle incoming frame data for real-time analysis"""
    try:
        # Parse frame data
        frame_data = StreamingFrameData(**message.data)
        
        # Get or create session
        session_id = session_manager.get_user_session(user_id)
        if not session_id:
            await connection_manager.send_error(connection_id, "No active streaming session")
            return
        
        # Process frame
        analysis_result = await session_manager.process_frame(session_id, frame_data)
        
        if analysis_result:
            # Send analysis result
            result_message = WebSocketMessage(
                type=MessageType.ANALYSIS_RESULT.value,
                data=analysis_result.to_dict(),
                session_id=session_id
            )
            await connection_manager.send_message(connection_id, result_message)
            
            # Send feedback if any significant faults detected
            session_data = session_manager.get_session(session_id)
            if (analysis_result.detected_faults and 
                session_data["config"].enable_instant_feedback):
                
                feedback = await generate_instant_feedback(
                    analysis_result, 
                    session_data["config"]
                )
                
                if feedback:
                    feedback_message = WebSocketMessage(
                        type=MessageType.FEEDBACK.value,
                        data=feedback,
                        session_id=session_id
                    )
                    await connection_manager.send_message(connection_id, feedback_message)
                    
                    # Update stats
                    session_manager.session_stats[session_id].feedback_generated += 1
        
    except Exception as e:
        logger.error(f"Error handling frame data: {e}")
        await connection_manager.send_error(connection_id, f"Frame processing error: {str(e)}")

async def handle_start_streaming_session(connection_id: str, message: WebSocketMessage, user_id: str):
    """Handle request to start streaming session"""
    try:
        config_data = message.data.get("config", {})
        config_data["user_id"] = user_id
        config = StreamingSessionConfig(**config_data)
        
        # End existing session if any
        existing_session = session_manager.get_user_session(user_id)
        if existing_session:
            session_manager.end_session(existing_session)
        
        # Create new session
        session_id = session_manager.create_session(config)
        
        response = WebSocketMessage(
            type="streaming_session_started",
            data={
                "session_id": session_id,
                "config": config.dict(),
                "status": "active"
            }
        )
        await connection_manager.send_message(connection_id, response)
        
    except Exception as e:
        logger.error(f"Error starting streaming session: {e}")
        await connection_manager.send_error(connection_id, f"Failed to start session: {str(e)}")

async def handle_end_streaming_session(connection_id: str, message: WebSocketMessage, user_id: str):
    """Handle request to end streaming session"""
    session_id = session_manager.get_user_session(user_id)
    if session_id:
        session_manager.end_session(session_id)
        
        response = WebSocketMessage(
            type="streaming_session_ended",
            data={"session_id": session_id, "status": "ended"}
        )
        await connection_manager.send_message(connection_id, response)
    else:
        await connection_manager.send_error(connection_id, "No active session to end")

async def handle_get_stats(connection_id: str, message: WebSocketMessage, user_id: str):
    """Handle request for session statistics"""
    session_id = session_manager.get_user_session(user_id)
    if session_id and session_id in session_manager.session_stats:
        stats = session_manager.session_stats[session_id]
        
        response = WebSocketMessage(
            type="session_stats",
            data=stats.dict()
        )
        await connection_manager.send_message(connection_id, response)
    else:
        await connection_manager.send_error(connection_id, "No active session or stats available")

async def handle_coaching_tip(connection_id: str, message: WebSocketMessage, session_id: str):
    """Handle coaching tip from coach to student"""
    tip_data = message.data
    
    coaching_message = WebSocketMessage(
        type=MessageType.COACHING_TIP.value,
        data=tip_data,
        session_id=session_id
    )
    
    # Broadcast to all session participants
    await connection_manager.broadcast_to_session(session_id, coaching_message)

async def handle_drill_suggestion(connection_id: str, message: WebSocketMessage, session_id: str):
    """Handle drill suggestion from coach"""
    drill_data = message.data
    
    drill_message = WebSocketMessage(
        type=MessageType.DRILL_SUGGESTION.value,
        data=drill_data,
        session_id=session_id
    )
    
    # Broadcast to all session participants
    await connection_manager.broadcast_to_session(session_id, drill_message)

async def handle_coaching_frame_data(connection_id: str, message: WebSocketMessage, session_id: str):
    """Handle frame data in coaching session"""
    # Similar to handle_frame_data but broadcasts results to all session participants
    try:
        frame_data = StreamingFrameData(**message.data)
        
        # For coaching sessions, we might want to analyze differently
        # This could include more detailed analysis or specific coaching focuses
        
        # Broadcast frame data to all participants for synchronized viewing
        coaching_frame_message = WebSocketMessage(
            type="coaching_frame_update",
            data=frame_data.dict(),
            session_id=session_id
        )
        
        await connection_manager.broadcast_to_session(session_id, coaching_frame_message)
        
    except Exception as e:
        logger.error(f"Error handling coaching frame data: {e}")

async def handle_system_stats_request(connection_id: str):
    """Handle request for system statistics"""
    stats = connection_manager.get_connection_stats()
    
    system_stats = {
        "connection_stats": stats,
        "active_streaming_sessions": len(session_manager.active_sessions),
        "total_frames_processed": sum(
            session["frames_processed"] 
            for session in session_manager.active_sessions.values()
        ),
        "timestamp": time.time()
    }
    
    response = WebSocketMessage(
        type="system_stats",
        data=system_stats
    )
    
    await connection_manager.send_message(connection_id, response)

# --- Helper Functions ---

async def generate_instant_feedback(
    analysis_result: FrameAnalysisResult, 
    config: StreamingSessionConfig
) -> Optional[Dict[str, Any]]:
    """Generate instant feedback for detected faults"""
    if not analysis_result.detected_faults:
        return None
    
    # Filter faults by severity threshold
    significant_faults = [
        fault for fault in analysis_result.detected_faults
        if fault.get('severity', 0) >= config.feedback_threshold * 10
    ]
    
    if not significant_faults:
        return None
    
    # Create minimal swing input for feedback generation
    swing_input = {
        "session_id": f"streaming_{int(time.time())}",
        "user_id": config.user_id,
        "club_used": config.club_used,
        "frames": [analysis_result.frame_data.keypoints],
        "p_system_classification": [],
        "video_fps": 30.0
    }
    
    try:
        # Use streaming feedback generator
        feedback_context = FeedbackContext(
            user_skill_level=config.skill_level,
            feedback_mode=FeedbackMode.STREAMING
        )
        
        feedback_result = await generate_realtime_feedback(
            swing_input, 
            significant_faults
        )
        
        if feedback_result and feedback_result.get('detailed_feedback'):
            return {
                "type": "instant_feedback",
                "fault_count": len(significant_faults),
                "primary_fault": significant_faults[0]['fault_name'],
                "feedback": feedback_result['detailed_feedback'][0],
                "timestamp": time.time()
            }
    
    except Exception as e:
        logger.error(f"Error generating instant feedback: {e}")
    
    return None

# --- REST API Endpoints ---

@router.post("/sessions")
async def create_streaming_session(config: StreamingSessionConfig):
    """Create a new streaming session via REST API"""
    try:
        session_id = session_manager.create_session(config)
        return JSONResponse({
            "session_id": session_id,
            "status": "created",
            "config": config.dict()
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/sessions/{session_id}")
async def get_session_info(session_id: str):
    """Get information about a streaming session"""
    session_data = session_manager.get_session(session_id)
    if not session_data:
        raise HTTPException(status_code=404, detail="Session not found")
    
    return JSONResponse({
        "session_id": session_id,
        "config": session_data["config"].dict(),
        "created_at": session_data["created_at"],
        "frames_processed": session_data["frames_processed"],
        "status": session_data["status"]
    })

@router.delete("/sessions/{session_id}")
async def end_streaming_session_api(session_id: str):
    """End a streaming session via REST API"""
    if session_manager.end_session(session_id):
        return JSONResponse({"status": "ended", "session_id": session_id})
    else:
        raise HTTPException(status_code=404, detail="Session not found")

@router.get("/sessions/{session_id}/stats")
async def get_session_stats(session_id: str):
    """Get performance statistics for a session"""
    if session_id not in session_manager.session_stats:
        raise HTTPException(status_code=404, detail="Session stats not found")
    
    stats = session_manager.session_stats[session_id]
    return JSONResponse(stats.dict())

@router.get("/system/stats")
async def get_system_stats():
    """Get system-wide statistics"""
    connection_stats = connection_manager.get_connection_stats()
    
    return JSONResponse({
        "connection_stats": connection_stats,
        "streaming_sessions": {
            "active": len(session_manager.active_sessions),
            "total_frames": sum(
                session["frames_processed"] 
                for session in session_manager.active_sessions.values()
            )
        },
        "timestamp": time.time()
    })

# Register WebSocket message handlers with connection manager
connection_manager.message_router.register_handler("frame_data", handle_frame_data)
connection_manager.message_router.register_handler("start_streaming_session", handle_start_streaming_session)
connection_manager.message_router.register_handler("end_streaming_session", handle_end_streaming_session)