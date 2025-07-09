"""
Main FastAPI application for the SwingSync AI backend.

This module sets up and runs the FastAPI web server, providing comprehensive
golf swing analysis with user authentication, data persistence, and real-time streaming.

Key Features:
- **User Management:** Registration, authentication, and profile management
- **Swing Analysis API:** Accepts POST requests with swing data for analysis
- **Real-time Streaming:** WebSocket endpoints for live golf swing analysis
- **Live Coaching:** WebSocket-based coaching sessions for real-time instruction
- **Data Persistence:** Stores swing sessions and analysis results in database
- **JWT Authentication:** Secure token-based authentication system
- **Data Validation:** Uses Pydantic models for automatic validation
- **Analysis Pipeline:** KPI extraction, fault detection, and AI feedback
- **Performance Monitoring:** Real-time KPI tracking and system metrics
- **API Documentation:** Interactive Swagger UI and ReDoc documentation

Streaming Features:
- **WebSocket Endpoints:** Real-time data streaming for live analysis
- **Frame-by-frame Processing:** Immediate feedback with sub-100ms latency
- **Session Management:** Live coaching session coordination
- **Gemini 2.5 Flash Integration:** Streaming AI feedback generation

Dependencies:
- `fastapi`: For building the API and WebSocket support
- `uvicorn`: For running the ASGI server
- `websockets`: For WebSocket communication
- `sqlalchemy`: For database operations
- `python-jose`: For JWT token handling
- Project modules: `data_structures`, `kpi_extraction`, `fault_detection`,
  `feedback_generation`, `database`, `user_management`, `streaming_endpoints`,
  `websocket_manager`, `live_analysis`

Environment Variables:
- `GEMINI_API_KEY`: For Google Gemini API integration
- `SECRET_KEY`: For JWT token signing
- `DATABASE_URL`: Database connection string
"""
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any
from sqlalchemy.orm import Session
from datetime import datetime, timezone
import json
import uuid

# Import core logic modules
from data_structures import (
    # SwingVideoAnalysisInput as SwingVideoAnalysisInputTypedDict, # Keep for reference
    PoseKeypoint as PoseKeypointTypedDict,
    FramePoseData as FramePoseDataTypedDict,
    PSystemPhase as PSystemPhaseTypedDict,
    SwingAnalysisFeedback, # This is already a TypedDict, FastAPI handles it for response
    LLMGeneratedTip, # Also a TypedDict for response
    DetectedFault as DetectedFaultTypedDict # Also for response
)
from kpi_extraction import extract_all_kpis
from fault_detection import check_swing_faults
from feedback_generation import generate_swing_analysis_feedback

# Import database and authentication modules
from database import (
    get_db, init_database, User, SwingSession, SwingAnalysisResult,
    BiomechanicalKPI, DetectedFault, SessionStatus, FaultSeverity
)
from sqlalchemy import func
from user_management import (
    UserRegistration, UserLogin, UserProfile, UserProfileUpdate,
    UserPreferencesModel, Token, create_user, login_user, refresh_access_token,
    get_current_user, get_current_active_user, get_current_user_optional,
    update_user_profile, get_user_preferences, update_user_preferences,
    is_email_available, is_username_available, change_password
)

# Import streaming and real-time analysis modules
try:
    from streaming_endpoints import router as streaming_router
    from websocket_manager import connection_manager
    from live_analysis import LiveAnalysisEngine
    STREAMING_AVAILABLE = True
except ImportError as e:
    print(f"Warning: Streaming modules not available: {e}")
    STREAMING_AVAILABLE = False

# --- Pydantic Models for Request Validation ---
# These mirror the TypedDicts from data_structures.py for FastAPI's validation

class PoseKeypointModel(BaseModel):
    x: float
    y: float
    z: float
    visibility: Optional[float] = None

class FramePoseDataModel(BaseModel):
    # Using Dict[str, PoseKeypointModel] for dynamic keypoint names
    # Based on Pydantic docs, for arbitrary key names, this is one way.
    # Or define all possible keypoints as Optional fields if they are fixed.
    # For now, allowing any string key to map to a PoseKeypointModel.
    keypoints: Dict[str, PoseKeypointModel] = Field(..., alias="keypoints") # Ensure this field name is used in JSON

    # If you expect a fixed set of keypoints, define them like this:
    # nose: Optional[PoseKeypointModel] = None
    # left_shoulder: Optional[PoseKeypointModel] = None
    # ... and so on for all ~33 keypoints. This is more explicit.
    # For this example, using a dictionary is more flexible if keypoint names vary.
    # However, our kpi_extraction module EXPECTS specific names.
    # So, it's better to be explicit if possible, or validate expected keys exist.

    # Reverting to a simple Dict[str, PoseKeypointModel] as FramePoseData is Dict[str, PoseKeypoint]
    # Pydantic will expect a dictionary where keys are strings and values are PoseKeypointModel-like dicts.
    # This means the input JSON for a frame should be: {"nose": {"x":0,...}, "left_shoulder":{...}}
    # Let's make it a direct mapping for simplicity matching TypedDict
    # This requires the input JSON to be a dictionary of keypoint objects.
    # e.g. {"frames": [ {"left_shoulder": {"x":0,...}, ... }, ... ] }

    # Pydantic handles Dict[str, ModelType] well.
    # So, FramePoseData will be a dictionary where keys are keypoint names (strings)
    # and values are PoseKeypointModel instances.
    # This means the input `frames` list will contain dictionaries like:
    # { "nose": {"x":0,...}, "left_shoulder": {"x":0,...}, ... }
    # This matches our FramePoseData = Dict[str, PoseKeypoint] TypedDict.

    # No specific fields needed here if we treat it as Dict[str, PoseKeypointModel]
    # The validation will happen on the SwingVideoAnalysisInputModel's 'frames' field.


class PSystemPhaseModel(BaseModel):
    phase_name: str
    start_frame_index: int
    end_frame_index: int

class SwingVideoAnalysisInputModel(BaseModel):
    session_id: str
    user_id: str
    club_used: str
    # Each item in 'frames' should be a dictionary matching FramePoseData structure
    frames: List[Dict[str, PoseKeypointModel]] # List of {keypoint_name: PoseKeypointModel}
    p_system_classification: List[PSystemPhaseModel]
    video_fps: float

    # Convert back to TypedDict for internal use if necessary, though not strictly needed
    # as functions can often work with Pydantic models via .dict() or attribute access.
    # For this example, we'll convert to the TypedDicts our modules expect.
    def to_typed_dict(self) -> Dict[str, Any]: # Simplified to Dict for broader compatibility
        # Pydantic's .model_dump() is the modern way (previously .dict())
        return self.model_dump()


# --- FastAPI App ---
app = FastAPI(
    title="SwingSync AI API",
    description="Comprehensive golf swing analysis with user management and AI-powered feedback.",
    version="1.0.0"
)

# Add CORS middleware - Secure configuration for personal use
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",  # React dev server
        "http://127.0.0.1:3000",
        "http://localhost:8080",  # Alternative local dev
        "http://127.0.0.1:8080"
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept"],
)

# Include streaming endpoints if available
if STREAMING_AVAILABLE:
    app.include_router(streaming_router, prefix="/api/v1")
    print("✓ Streaming endpoints enabled")
else:
    print("⚠ Streaming endpoints disabled - missing dependencies")

# Include analytics endpoints
try:
    from analytics_endpoints import router as analytics_router
    app.include_router(analytics_router, prefix="/api/v1")
    print("✓ Analytics endpoints enabled")
except ImportError as e:
    print(f"⚠ Analytics endpoints disabled: {e}")

# Initialize database on startup
@app.on_event("startup")
async def startup_event():
    """Initialize database on application startup."""
    init_database()
    if STREAMING_AVAILABLE:
        print("✓ Real-time analysis engine initialized")

# Additional Pydantic models for new endpoints

class PasswordChange(BaseModel):
    """Password change request model."""
    old_password: str
    new_password: str = Field(..., min_length=8, max_length=100)

class SessionHistoryResponse(BaseModel):
    """Response model for session history."""
    sessions: List[Dict[str, Any]]
    total_count: int
    page: int
    per_page: int

class AnalysisStatsResponse(BaseModel):
    """Response model for user analysis statistics."""
    total_sessions: int
    avg_score: Optional[float]
    improvement_trend: Optional[str]
    common_faults: List[Dict[str, Any]]
    recent_activity: List[Dict[str, Any]]

# --- Authentication Endpoints ---

@app.post("/auth/register", response_model=UserProfile, status_code=status.HTTP_201_CREATED)
async def register_user(
    user_data: UserRegistration,
    db: Session = Depends(get_db)
):
    """Register a new user account."""
    user = create_user(db, user_data)
    return UserProfile.from_orm(user)

@app.post("/auth/login", response_model=Token)
async def login_user_endpoint(
    login_data: UserLogin,
    db: Session = Depends(get_db)
):
    """Login user and return access token."""
    return login_user(db, login_data)

@app.post("/auth/refresh", response_model=Token)
async def refresh_token_endpoint(
    refresh_token: str,
    db: Session = Depends(get_db)
):
    """Refresh access token using refresh token."""
    return refresh_access_token(db, refresh_token)

@app.get("/auth/me", response_model=UserProfile)
async def get_current_user_profile(
    current_user: User = Depends(get_current_active_user)
):
    """Get current user's profile."""
    return UserProfile.from_orm(current_user)

@app.put("/auth/profile", response_model=UserProfile)
async def update_profile(
    profile_data: UserProfileUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Update user profile."""
    updated_user = update_user_profile(db, current_user, profile_data)
    return UserProfile.from_orm(updated_user)

@app.post("/auth/change-password")
async def change_user_password(
    password_data: PasswordChange,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Change user password."""
    change_password(db, current_user, password_data.old_password, password_data.new_password)
    return {"message": "Password changed successfully"}

@app.get("/auth/preferences", response_model=UserPreferencesModel)
async def get_preferences(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get user preferences."""
    preferences = get_user_preferences(db, current_user.id)
    if not preferences:
        raise HTTPException(status_code=404, detail="User preferences not found")
    return UserPreferencesModel.from_orm(preferences)

@app.put("/auth/preferences", response_model=UserPreferencesModel)
async def update_preferences(
    preferences_data: UserPreferencesModel,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Update user preferences."""
    preferences = update_user_preferences(db, current_user.id, preferences_data)
    return UserPreferencesModel.from_orm(preferences)

# --- Utility Endpoints ---

@app.get("/auth/check-email/{email}")
async def check_email_availability(
    email: str,
    db: Session = Depends(get_db)
):
    """Check if email is available for registration."""
    available = is_email_available(db, email)
    return {"email": email, "available": available}

@app.get("/auth/check-username/{username}")
async def check_username_availability(
    username: str,
    db: Session = Depends(get_db)
):
    """Check if username is available for registration."""
    available = is_username_available(db, username)
    return {"username": username, "available": available}

# --- Swing Analysis Endpoints ---

@app.post("/analyze_swing/", response_model=SwingAnalysisFeedback)
async def analyze_swing_endpoint(
    swing_input_model: SwingVideoAnalysisInputModel,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """
    Analyze golf swing with user authentication and data persistence.
    
    Processes swing data through the AI pipeline and saves results to database.
    """
    try:
        # Validate that the user_id in the request matches the authenticated user
        if swing_input_model.user_id != current_user.id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="User ID in request does not match authenticated user"
            )
        
        # Create swing session record
        swing_session = SwingSession(
            id=swing_input_model.session_id,
            user_id=current_user.id,
            club_used=swing_input_model.club_used,
            session_status=SessionStatus.PROCESSING,
            video_fps=swing_input_model.video_fps,
            total_frames=len(swing_input_model.frames),
            p_system_phases=swing_input_model.p_system_classification
        )
        
        db.add(swing_session)
        db.commit()
        
        # Convert Pydantic model to TypedDict format for analysis
        swing_input_dict = swing_input_model.to_typed_dict()
        
        # 1. Extract KPIs
        kpis = extract_all_kpis(swing_input_dict)
        
        # 2. Detect Faults
        faults = check_swing_faults(swing_input_dict, kpis)
        
        # 3. Generate Feedback
        feedback_result = generate_swing_analysis_feedback(swing_input_dict, faults)
        
        # Save analysis results to database
        analysis_result = SwingAnalysisResult(
            session_id=swing_input_model.session_id,
            summary_of_findings=feedback_result["summary_of_findings"],
            detailed_feedback=feedback_result["detailed_feedback"],
            raw_detected_faults=feedback_result["raw_detected_faults"],
            visualisation_annotations=feedback_result.get("visualisation_annotations"),
            confidence_score=0.95  # You can calculate this based on your analysis
        )
        
        db.add(analysis_result)
        
        # Save individual KPIs
        for kpi in kpis:
            kpi_record = BiomechanicalKPI(
                session_id=swing_input_model.session_id,
                p_position=kpi["p_position"],
                kpi_name=kpi["kpi_name"],
                value=float(kpi["value"]) if isinstance(kpi["value"], (int, float)) else 0.0,
                unit=kpi["unit"],
                notes=kpi.get("notes"),
                confidence=0.9  # You can calculate this based on your analysis
            )
            db.add(kpi_record)
        
        # Save detected faults
        for fault in faults:
            fault_record = DetectedFault(
                session_id=swing_input_model.session_id,
                fault_id=fault["fault_id"],
                fault_name=fault["fault_name"],
                description=fault["description"],
                severity=FaultSeverity.MEDIUM,  # Map from fault severity
                severity_score=fault.get("severity", 0.5),
                p_positions_implicated=fault["p_positions_implicated"],
                primary_p_position=fault["p_positions_implicated"][0] if fault["p_positions_implicated"] else None,
                kpi_deviations=fault["kpi_deviations"],
                llm_prompt_template_key=fault["llm_prompt_template_key"],
                detection_confidence=0.85
            )
            db.add(fault_record)
        
        # Update session status
        swing_session.session_status = SessionStatus.COMPLETED
        swing_session.completed_at = datetime.now(timezone.utc)
        
        db.commit()
        
        return feedback_result
        
    except HTTPException:
        # Re-raise HTTP exceptions
        db.rollback()
        raise
    except Exception as e:
        # Rollback database changes and update session status
        db.rollback()
        
        try:
            session = db.query(SwingSession).filter(
                SwingSession.id == swing_input_model.session_id
            ).first()
            if session:
                session.session_status = SessionStatus.FAILED
                db.commit()
        except:
            pass
        
        print(f"Error during swing analysis: {e}")
        raise HTTPException(
            status_code=500, 
            detail=f"An error occurred during analysis: {str(e)}"
        )

# --- User Data and Analytics Endpoints ---

@app.get("/user/sessions", response_model=SessionHistoryResponse)
async def get_user_sessions(
    page: int = 1,
    per_page: int = 20,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get user's swing session history with pagination."""
    offset = (page - 1) * per_page
    
    # Get total count
    total_count = db.query(SwingSession).filter(
        SwingSession.user_id == current_user.id
    ).count()
    
    # Get paginated sessions
    sessions = db.query(SwingSession).filter(
        SwingSession.user_id == current_user.id
    ).order_by(SwingSession.created_at.desc()).offset(offset).limit(per_page).all()
    
    session_data = []
    for session in sessions:
        session_dict = {
            "id": session.id,
            "club_used": session.club_used,
            "session_status": session.session_status.value,
            "video_fps": session.video_fps,
            "total_frames": session.total_frames,
            "created_at": session.created_at.isoformat(),
            "completed_at": session.completed_at.isoformat() if session.completed_at else None
        }
        
        # Add analysis results if available
        if session.analysis_results:
            session_dict["overall_score"] = session.analysis_results.overall_score
            session_dict["confidence_score"] = session.analysis_results.confidence_score
        
        session_data.append(session_dict)
    
    return SessionHistoryResponse(
        sessions=session_data,
        total_count=total_count,
        page=page,
        per_page=per_page
    )

@app.get("/user/session/{session_id}")
async def get_session_details(
    session_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get detailed information about a specific session."""
    session = db.query(SwingSession).filter(
        SwingSession.id == session_id,
        SwingSession.user_id == current_user.id
    ).first()
    
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    
    # Get related data
    analysis = session.analysis_results
    kpis = session.biomechanical_kpis
    faults = session.detected_faults
    
    return {
        "session": {
            "id": session.id,
            "club_used": session.club_used,
            "session_status": session.session_status.value,
            "video_fps": session.video_fps,
            "total_frames": session.total_frames,
            "created_at": session.created_at.isoformat(),
            "completed_at": session.completed_at.isoformat() if session.completed_at else None
        },
        "analysis": {
            "summary_of_findings": analysis.summary_of_findings if analysis else None,
            "overall_score": analysis.overall_score if analysis else None,
            "detailed_feedback": analysis.detailed_feedback if analysis else [],
            "confidence_score": analysis.confidence_score if analysis else None
        } if analysis else None,
        "kpis": [
            {
                "p_position": kpi.p_position,
                "kpi_name": kpi.kpi_name,
                "value": kpi.value,
                "unit": kpi.unit,
                "deviation_from_ideal": kpi.deviation_from_ideal
            } for kpi in kpis
        ],
        "faults": [
            {
                "fault_name": fault.fault_name,
                "description": fault.description,
                "severity": fault.severity.value,
                "severity_score": fault.severity_score,
                "p_positions_implicated": fault.p_positions_implicated
            } for fault in faults
        ]
    }

@app.get("/user/analytics", response_model=AnalysisStatsResponse)
async def get_user_analytics(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get user's analysis statistics and trends."""
    # Get total sessions
    total_sessions = db.query(SwingSession).filter(
        SwingSession.user_id == current_user.id,
        SwingSession.session_status == SessionStatus.COMPLETED
    ).count()
    
    # Get average score
    avg_score_result = db.query(func.avg(SwingAnalysisResult.overall_score)).join(
        SwingSession
    ).filter(SwingSession.user_id == current_user.id).scalar()
    
    avg_score = float(avg_score_result) if avg_score_result else None
    
    # Get common faults
    fault_counts = db.query(
        DetectedFault.fault_name,
        func.count(DetectedFault.id).label('count')
    ).join(SwingSession).filter(
        SwingSession.user_id == current_user.id
    ).group_by(DetectedFault.fault_name).order_by(
        func.count(DetectedFault.id).desc()
    ).limit(5).all()
    
    common_faults = [
        {"fault_name": fault[0], "count": fault[1]} 
        for fault in fault_counts
    ]
    
    # Get recent activity (last 5 sessions)
    recent_sessions = db.query(SwingSession).filter(
        SwingSession.user_id == current_user.id,
        SwingSession.session_status == SessionStatus.COMPLETED
    ).order_by(SwingSession.created_at.desc()).limit(5).all()
    
    recent_activity = [
        {
            "session_id": session.id,
            "club_used": session.club_used,
            "created_at": session.created_at.isoformat(),
            "score": session.analysis_results.overall_score if session.analysis_results else None
        } for session in recent_sessions
    ]
    
    return AnalysisStatsResponse(
        total_sessions=total_sessions,
        avg_score=avg_score,
        improvement_trend="improving",  # Calculate based on score trends
        common_faults=common_faults,
        recent_activity=recent_activity
    )

# --- Health and Information Endpoints ---

@app.get("/", summary="Root endpoint for health check and API information.")
async def read_root():
    return {
        "message": "Welcome to SwingSync AI API",
        "description": "Comprehensive golf swing analysis with user management",
        "version": "1.0.0",
        "features": [
            "User registration and authentication",
            "JWT token-based security",
            "Golf swing analysis with AI feedback", 
            "Biomechanical KPI extraction",
            "Swing fault detection",
            "Historical data tracking",
            "User preferences and analytics",
            "Progress tracking and goal management",
            "AI-powered insights and recommendations",
            "Performance visualization and trends",
            "Coaching effectiveness metrics",
            "Personalized training recommendations"
        ],
        "endpoints": {
            "documentation": "/docs",
            "alternative_docs": "/redoc",
            "authentication": "/auth/*",
            "analysis": "/analyze_swing/",
            "user_data": "/user/*",
            "analytics": "/api/v1/analytics/*"
        }
    }

@app.get("/health")
async def health_check():
    """Health check endpoint for monitoring."""
    health_data = {
        "status": "healthy",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "version": "1.0.0",
        "features": {
            "batch_analysis": True,
            "streaming_analysis": STREAMING_AVAILABLE,
            "user_management": True,
            "database": True,
            "analytics": True,
            "progress_tracking": True,
            "ai_insights": True,
            "visualization": True
        }
    }
    
    # Add streaming stats if available
    if STREAMING_AVAILABLE:
        try:
            connection_stats = connection_manager.get_connection_stats()
            health_data["streaming_stats"] = connection_stats
        except Exception as e:
            health_data["streaming_error"] = str(e)
    
    return health_data

@app.get("/streaming/status")
async def streaming_status():
    """Get detailed streaming system status."""
    if not STREAMING_AVAILABLE:
        raise HTTPException(
            status_code=503, 
            detail="Streaming functionality not available"
        )
    
    try:
        connection_stats = connection_manager.get_connection_stats()
        return {
            "streaming_enabled": True,
            "connection_stats": connection_stats,
            "endpoints": {
                "websocket_streaming": "/api/v1/stream/ws/{user_id}",
                "websocket_coaching": "/api/v1/stream/ws/coaching/{session_id}",
                "websocket_monitoring": "/api/v1/stream/ws/monitor/{user_id}",
                "rest_sessions": "/api/v1/stream/sessions",
                "system_stats": "/api/v1/stream/system/stats"
            },
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error getting streaming status: {str(e)}"
        )

# --- Application Setup and Documentation ---

"""
To run this application:

1. Install dependencies:
   pip install -r requirements.txt

2. Set environment variables:
   export GEMINI_API_KEY="your-gemini-api-key"
   export SECRET_KEY="your-secret-jwt-key" 
   export DATABASE_URL="sqlite:///./swingsync.db"  # or PostgreSQL URL

3. Initialize database:
   python database.py

4. Run application:
   uvicorn main:app --reload

5. Access API:
   - API: http://127.0.0.1:8000
   - Documentation: http://127.0.0.1:8000/docs
   - Alternative docs: http://127.0.0.1:8000/redoc

Key API Endpoints:
- POST /auth/register - Register new user
- POST /auth/login - User login  
- GET /auth/me - Get user profile
- POST /analyze_swing/ - Analyze golf swing (requires auth)
- GET /user/sessions - Get session history
- GET /user/analytics - Get user analytics

Analytics and Progress Tracking Endpoints:
- GET /api/v1/analytics/performance/overview - Performance overview
- GET /api/v1/analytics/trends/score - Score trend analysis
- GET /api/v1/analytics/faults/patterns - Fault pattern analysis
- GET /api/v1/analytics/kpis/performance - KPI performance analysis
- GET /api/v1/analytics/goals - Get user goals
- POST /api/v1/analytics/goals - Create new goal
- GET /api/v1/analytics/goals/suggestions - AI goal suggestions
- GET /api/v1/analytics/achievements - Get achievements
- GET /api/v1/analytics/insights - AI-powered insights
- GET /api/v1/analytics/insights/recommendations - Training recommendations
- GET /api/v1/analytics/insights/prediction - Performance predictions
- GET /api/v1/analytics/charts/score-trend - Score trend chart data
- GET /api/v1/analytics/charts/fault-frequency - Fault frequency chart
- GET /api/v1/analytics/dashboard - Complete dashboard data
- POST /api/v1/analytics/comparison - Comparative analysis
- POST /api/v1/analytics/export - Export analytics data
- GET /api/v1/analytics/summary - Comprehensive analytics summary

Real-time Streaming Endpoints (if enabled):
- WebSocket /api/v1/stream/ws/{user_id} - Real-time swing analysis
- WebSocket /api/v1/stream/ws/coaching/{session_id} - Live coaching
- WebSocket /api/v1/stream/ws/monitor/{user_id} - Performance monitoring
- POST /api/v1/stream/sessions - Create streaming session
- GET /api/v1/stream/system/stats - System statistics
- GET /streaming/status - Streaming system status
"""

if __name__ == "__main__":
    print("SwingSync AI API")
    print("================")
    print("Features:")
    print("- User authentication and profile management")
    print("- Golf swing analysis with AI feedback")
    print("- Data persistence and historical tracking")
    print("- RESTful API with comprehensive documentation")
    print("- Advanced analytics and progress tracking")
    print("- AI-powered insights and recommendations")
    print("- Goal setting and achievement tracking")
    print("- Performance visualization and trends")
    print("- Coaching effectiveness metrics")
    print()
    print("To run: uvicorn main:app --reload")
    print("Ensure environment variables are set:")
    print("- GEMINI_API_KEY (for AI feedback)")
    print("- SECRET_KEY (for JWT tokens)")
    print("- DATABASE_URL (for data persistence)")
