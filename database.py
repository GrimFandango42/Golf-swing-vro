"""
Database configuration and models for SwingSync AI.

This module provides:
- Database connection and session management
- SQLAlchemy ORM models for users, swing sessions, and analysis data
- Database initialization and migration support
- Relationship management between entities

Models:
- User: User profiles with authentication and personal information
- UserPreferences: User-specific settings and preferences
- SwingSession: Individual swing analysis sessions
- SwingAnalysisResult: Detailed analysis results and feedback
- BiomechanicalKPI: Stored KPI measurements
- DetectedFault: Recorded swing faults
- UserGoal: User-defined improvement goals
- GoalMilestone: Milestones within goals
- Achievement: User achievements and badges
- TrainingPlan: Personalized training plans
"""

import os
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from sqlalchemy import (
    create_engine, Column, Integer, String, Float, DateTime, 
    Boolean, Text, JSON, ForeignKey, Enum as SQLEnum,
    UniqueConstraint, Index
)
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship, Session
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
import uuid
import enum

# Database configuration
DATABASE_URL = os.getenv(
    "DATABASE_URL", 
    "sqlite:///./swingsync.db"  # Default to SQLite for development
)

# Create engine with appropriate settings
if DATABASE_URL.startswith("sqlite"):
    engine = create_engine(
        DATABASE_URL, 
        connect_args={"check_same_thread": False},
        echo=True  # Set to False in production
    )
else:
    engine = create_engine(DATABASE_URL, echo=True)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Enums for database fields
class SkillLevel(enum.Enum):
    BEGINNER = "beginner"
    INTERMEDIATE = "intermediate"
    ADVANCED = "advanced"
    PROFESSIONAL = "professional"

class SessionStatus(enum.Enum):
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"

class FaultSeverity(enum.Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

class GoalType(enum.Enum):
    SCORE_IMPROVEMENT = "score_improvement"
    FAULT_REDUCTION = "fault_reduction" 
    KPI_TARGET = "kpi_target"
    CONSISTENCY = "consistency"
    FREQUENCY = "frequency"
    HANDICAP = "handicap"
    CUSTOM = "custom"

class GoalStatus(enum.Enum):
    ACTIVE = "active"
    COMPLETED = "completed"
    PAUSED = "paused"
    EXPIRED = "expired"
    CANCELLED = "cancelled"

class GoalPriority(enum.Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

class AchievementType(enum.Enum):
    MILESTONE = "milestone"
    STREAK = "streak"
    IMPROVEMENT = "improvement"
    CONSISTENCY = "consistency"
    SPECIAL = "special"

# Database Models

class User(Base):
    """User model for authentication and profile management."""
    __tablename__ = "users"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    email = Column(String(255), unique=True, index=True, nullable=False)
    username = Column(String(100), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    
    # Profile information
    first_name = Column(String(100))
    last_name = Column(String(100))
    date_of_birth = Column(DateTime)
    skill_level = Column(SQLEnum(SkillLevel), default=SkillLevel.BEGINNER)
    handicap = Column(Float)
    preferred_hand = Column(String(10))  # "right" or "left"
    height_cm = Column(Float)
    weight_kg = Column(Float)
    
    # Account status
    is_active = Column(Boolean, default=True)
    is_verified = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    last_login = Column(DateTime(timezone=True))
    
    # Relationships
    preferences = relationship("UserPreferences", back_populates="user", uselist=False)
    swing_sessions = relationship("SwingSession", back_populates="user")
    
    def __repr__(self):
        return f"<User(id={self.id}, username={self.username}, email={self.email})>"

class UserPreferences(Base):
    """User preferences and settings."""
    __tablename__ = "user_preferences"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"), nullable=False, unique=True)
    
    # Analysis preferences
    preferred_units = Column(String(20), default="metric")  # "metric" or "imperial"
    feedback_detail_level = Column(String(20), default="detailed")  # "basic", "detailed", "advanced"
    focus_areas = Column(JSON)  # List of areas user wants to focus on
    
    # Notification preferences
    email_notifications = Column(Boolean, default=True)
    push_notifications = Column(Boolean, default=True)
    weekly_reports = Column(Boolean, default=True)
    
    # Goals and targets
    target_handicap = Column(Float)
    primary_goals = Column(JSON)  # List of user's primary improvement goals
    
    # Privacy settings
    share_data_for_research = Column(Boolean, default=False)
    public_profile = Column(Boolean, default=False)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    user = relationship("User", back_populates="preferences")
    
    def __repr__(self):
        return f"<UserPreferences(user_id={self.user_id})>"

class SwingSession(Base):
    """Individual swing analysis sessions."""
    __tablename__ = "swing_sessions"
    
    id = Column(String, primary_key=True)  # This will be the session_id from input
    user_id = Column(String, ForeignKey("users.id"), nullable=False)
    
    # Session metadata
    club_used = Column(String(50), nullable=False)
    session_status = Column(SQLEnum(SessionStatus), default=SessionStatus.PENDING)
    video_fps = Column(Float, nullable=False)
    total_frames = Column(Integer)
    
    # Video and processing information
    video_duration_seconds = Column(Float)
    processing_time_seconds = Column(Float)
    video_file_path = Column(String(500))  # Path to stored video file
    
    # P-System classification data
    p_system_phases = Column(JSON)  # Stored as JSON array
    
    # Raw pose data (considering storage efficiency)
    pose_data_file_path = Column(String(500))  # Path to stored pose data file
    pose_data_compressed = Column(Text)  # Compressed JSON for smaller datasets
    
    # Session timing
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    completed_at = Column(DateTime(timezone=True))
    
    # Location and conditions (optional)
    location = Column(String(200))
    weather_conditions = Column(JSON)
    course_conditions = Column(String(100))
    
    # Relationships
    user = relationship("User", back_populates="swing_sessions")
    analysis_results = relationship("SwingAnalysisResult", back_populates="session", uselist=False)
    biomechanical_kpis = relationship("BiomechanicalKPI", back_populates="session")
    detected_faults = relationship("DetectedFault", back_populates="session")
    
    # Indexes
    __table_args__ = (
        Index('idx_user_created', 'user_id', 'created_at'),
        Index('idx_status_created', 'session_status', 'created_at'),
    )
    
    def __repr__(self):
        return f"<SwingSession(id={self.id}, user_id={self.user_id}, club={self.club_used})>"

class SwingAnalysisResult(Base):
    """Complete analysis results and AI-generated feedback."""
    __tablename__ = "swing_analysis_results"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String, ForeignKey("swing_sessions.id"), nullable=False, unique=True)
    
    # Overall analysis summary
    summary_of_findings = Column(Text)
    overall_score = Column(Float)  # 0-100 overall swing quality score
    
    # Detailed feedback from LLM
    detailed_feedback = Column(JSON)  # Array of LLMGeneratedTip objects
    
    # Technical analysis data
    raw_detected_faults = Column(JSON)  # Array of DetectedFault objects
    visualisation_annotations = Column(JSON)  # Visual annotations for client
    
    # Analysis metadata
    analysis_version = Column(String(20), default="1.0")  # Track analysis algorithm version
    processing_notes = Column(Text)  # Any notes from processing
    confidence_score = Column(Float)  # Overall confidence in analysis (0-1)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    session = relationship("SwingSession", back_populates="analysis_results")
    
    def __repr__(self):
        return f"<SwingAnalysisResult(session_id={self.session_id}, score={self.overall_score})>"

class BiomechanicalKPI(Base):
    """Individual biomechanical KPI measurements."""
    __tablename__ = "biomechanical_kpis"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String, ForeignKey("swing_sessions.id"), nullable=False)
    
    # KPI identification
    p_position = Column(String(10), nullable=False)  # P1, P2, etc.
    kpi_name = Column(String(100), nullable=False)
    
    # KPI values
    value = Column(Float, nullable=False)
    unit = Column(String(20), nullable=False)
    
    # Ideal ranges and analysis
    ideal_min = Column(Float)
    ideal_max = Column(Float)
    deviation_from_ideal = Column(Float)
    
    # Additional context
    calculation_method = Column(String(100))
    notes = Column(Text)
    confidence = Column(Float)  # Confidence in the measurement
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    session = relationship("SwingSession", back_populates="biomechanical_kpis")
    
    # Indexes for efficient querying
    __table_args__ = (
        Index('idx_session_kpi', 'session_id', 'kpi_name'),
        Index('idx_p_position_kpi', 'p_position', 'kpi_name'),
    )
    
    def __repr__(self):
        return f"<BiomechanicalKPI(session_id={self.session_id}, kpi={self.kpi_name}, value={self.value})>"

class DetectedFault(Base):
    """Detected swing faults and their details."""
    __tablename__ = "detected_faults"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String, ForeignKey("swing_sessions.id"), nullable=False)
    
    # Fault identification
    fault_id = Column(String(50), nullable=False)  # e.g., "CUPPED_WRIST_AT_TOP"
    fault_name = Column(String(200), nullable=False)
    
    # Fault details
    description = Column(Text)
    severity = Column(SQLEnum(FaultSeverity), default=FaultSeverity.MEDIUM)
    severity_score = Column(Float)  # 0.0 to 1.0
    
    # P-System context
    p_positions_implicated = Column(JSON)  # Array of P-positions
    primary_p_position = Column(String(10))
    
    # KPI deviations that led to this fault
    kpi_deviations = Column(JSON)  # Array of KPIDeviation objects
    
    # LLM and feedback
    llm_prompt_template_key = Column(String(100))
    corrective_feedback = Column(Text)
    drill_suggestions = Column(JSON)  # Array of suggested drills
    
    # Analysis metadata
    detection_confidence = Column(Float)  # Confidence in fault detection
    impact_assessment = Column(Text)  # How this fault affects performance
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    session = relationship("SwingSession", back_populates="detected_faults")
    
    # Indexes
    __table_args__ = (
        Index('idx_session_fault', 'session_id', 'fault_id'),
        Index('idx_fault_severity', 'fault_id', 'severity'),
    )
    
    def __repr__(self):
        return f"<DetectedFault(session_id={self.session_id}, fault={self.fault_name}, severity={self.severity})>"

class UserGoal(Base):
    """User-defined goals for improvement."""
    __tablename__ = "user_goals"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"), nullable=False)
    
    # Goal definition
    title = Column(String(200), nullable=False)
    description = Column(String(1000))
    goal_type = Column(SQLEnum(GoalType), nullable=False)
    priority = Column(SQLEnum(GoalPriority), default=GoalPriority.MEDIUM)
    
    # Target specifications
    target_data = Column(JSON)  # GoalTarget data
    
    # Timeline
    start_date = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    target_date = Column(DateTime(timezone=True), nullable=False)
    completed_date = Column(DateTime(timezone=True))
    
    # Status and progress
    status = Column(SQLEnum(GoalStatus), default=GoalStatus.ACTIVE)
    progress_percentage = Column(Float, default=0.0)
    
    # Metadata
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    
    # Relationships
    user = relationship("User", backref="goals")
    milestones = relationship("GoalMilestone", back_populates="goal", cascade="all, delete-orphan")
    
    # Indexes
    __table_args__ = (
        Index('idx_user_goals', 'user_id', 'status'),
        Index('idx_goal_type', 'goal_type', 'status'),
    )
    
    def __repr__(self):
        return f"<UserGoal(id={self.id}, title={self.title}, status={self.status})>"

class GoalMilestone(Base):
    """Milestones within a goal."""
    __tablename__ = "goal_milestones"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    goal_id = Column(String, ForeignKey("user_goals.id"), nullable=False)
    
    # Milestone definition
    title = Column(String(200), nullable=False)
    description = Column(String(500))
    target_value = Column(Float, nullable=False)
    order_index = Column(Integer, default=0)
    
    # Status
    is_completed = Column(Boolean, default=False)
    completed_date = Column(DateTime(timezone=True))
    
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    
    # Relationships
    goal = relationship("UserGoal", back_populates="milestones")
    
    def __repr__(self):
        return f"<GoalMilestone(id={self.id}, title={self.title}, completed={self.is_completed})>"

class Achievement(Base):
    """User achievements and badges."""
    __tablename__ = "achievements"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"), nullable=False)
    
    # Achievement details
    title = Column(String(200), nullable=False)
    description = Column(String(500))
    achievement_type = Column(SQLEnum(AchievementType), nullable=False)
    badge_icon = Column(String(100))  # Icon identifier
    
    # Requirements and data
    requirements = Column(JSON)  # Achievement requirements
    achievement_data = Column(JSON)  # Associated data (scores, dates, etc.)
    
    # Status
    is_unlocked = Column(Boolean, default=False)
    unlocked_date = Column(DateTime(timezone=True))
    
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    
    # Relationships
    user = relationship("User", backref="achievements")
    
    # Indexes
    __table_args__ = (
        Index('idx_user_achievements', 'user_id', 'is_unlocked'),
        Index('idx_achievement_type', 'achievement_type', 'is_unlocked'),
    )
    
    def __repr__(self):
        return f"<Achievement(id={self.id}, title={self.title}, unlocked={self.is_unlocked})>"

class TrainingPlan(Base):
    """Personalized training plans."""
    __tablename__ = "training_plans"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"), nullable=False)
    
    # Plan details
    name = Column(String(200), nullable=False)
    description = Column(String(1000))
    difficulty_level = Column(String(20), default="intermediate")
    
    # Structure
    plan_data = Column(JSON)  # Detailed plan structure
    duration_weeks = Column(Integer, default=4)
    sessions_per_week = Column(Integer, default=3)
    
    # Progress
    is_active = Column(Boolean, default=False)
    started_date = Column(DateTime(timezone=True))
    completed_date = Column(DateTime(timezone=True))
    current_week = Column(Integer, default=1)
    
    # Metadata
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    
    # Relationships
    user = relationship("User", backref="training_plans")
    
    # Indexes
    __table_args__ = (
        Index('idx_user_training_plans', 'user_id', 'is_active'),
        Index('idx_training_plan_status', 'is_active', 'created_at'),
    )
    
    def __repr__(self):
        return f"<TrainingPlan(id={self.id}, name={self.name}, active={self.is_active})>"

# Database utility functions

def get_db() -> Session:
    """Dependency to get database session."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def create_tables():
    """Create all database tables."""
    Base.metadata.create_all(bind=engine)

def drop_tables():
    """Drop all database tables (use with caution!)."""
    Base.metadata.drop_all(bind=engine)

def init_database():
    """Initialize the database with tables and any required initial data."""
    create_tables()
    print("Database tables created successfully!")

# Database session context manager
class DatabaseSession:
    """Context manager for database sessions."""
    
    def __enter__(self) -> Session:
        self.db = SessionLocal()
        return self.db
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is not None:
            self.db.rollback()
        else:
            self.db.commit()
        self.db.close()

# Query helper functions

def get_user_by_email(db: Session, email: str) -> Optional[User]:
    """Get user by email address."""
    return db.query(User).filter(User.email == email).first()

def get_user_by_username(db: Session, username: str) -> Optional[User]:
    """Get user by username."""
    return db.query(User).filter(User.username == username).first()

def get_user_sessions(db: Session, user_id: str, limit: int = 50) -> List[SwingSession]:
    """Get user's swing sessions, ordered by most recent."""
    return db.query(SwingSession).filter(
        SwingSession.user_id == user_id
    ).order_by(SwingSession.created_at.desc()).limit(limit).all()

def get_session_with_results(db: Session, session_id: str) -> Optional[SwingSession]:
    """Get swing session with all related analysis results."""
    return db.query(SwingSession).filter(
        SwingSession.id == session_id
    ).first()

if __name__ == "__main__":
    # Initialize database when run directly
    init_database()
    print("SwingSync AI database initialized!")