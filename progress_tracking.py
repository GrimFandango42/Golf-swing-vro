"""
Progress tracking and goal management for SwingSync AI.

This module provides:
- Goal setting and tracking functionality
- Achievement and milestone management
- Progress monitoring with notifications
- Personal training program management
- Performance target setting and evaluation
- Habit tracking and consistency metrics
- Long-term development planning

Key Features:
- SMART goal creation and validation
- Automated progress calculation
- Achievement unlock system
- Personalized milestone generation
- Progress visualization data
- Coaching integration for goal setting
- Reward and motivation systems
"""

from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional, Union
from sqlalchemy.orm import Session
from sqlalchemy import func, and_, or_, desc
from dataclasses import dataclass, asdict
from enum import Enum
import uuid
import json

from database import (
    User, SwingSession, SwingAnalysisResult, BiomechanicalKPI,
    DetectedFault, SessionStatus, Base, Column, String, Float,
    DateTime, Boolean, Integer, JSON, ForeignKey, SQLEnum
)
from analytics import AnalyticsEngine, TrendDirection

class GoalType(Enum):
    """Types of goals users can set."""
    SCORE_IMPROVEMENT = "score_improvement"
    FAULT_REDUCTION = "fault_reduction" 
    KPI_TARGET = "kpi_target"
    CONSISTENCY = "consistency"
    FREQUENCY = "frequency"
    HANDICAP = "handicap"
    CUSTOM = "custom"

class GoalStatus(Enum):
    """Goal completion status."""
    ACTIVE = "active"
    COMPLETED = "completed"
    PAUSED = "paused"
    EXPIRED = "expired"
    CANCELLED = "cancelled"

class GoalPriority(Enum):
    """Goal priority levels."""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

class AchievementType(Enum):
    """Types of achievements."""
    MILESTONE = "milestone"
    STREAK = "streak"
    IMPROVEMENT = "improvement"
    CONSISTENCY = "consistency"
    SPECIAL = "special"

@dataclass
class GoalTarget:
    """Target specifications for a goal."""
    metric_name: str
    target_value: float
    current_value: Optional[float] = None
    unit: str = ""
    direction: str = "increase"  # "increase" or "decrease"

@dataclass
class GoalProgress:
    """Progress tracking for a goal."""
    goal_id: str
    progress_percentage: float
    days_remaining: int
    on_track: bool
    estimated_completion: Optional[datetime]
    trend: TrendDirection

# Database Models for Goals and Achievements

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

class ProgressTracker:
    """Main class for progress tracking and goal management."""
    
    def __init__(self, db_session: Session):
        self.db = db_session
        self.analytics = AnalyticsEngine(db_session)
    
    def create_goal(
        self,
        user_id: str,
        title: str,
        description: str,
        goal_type: GoalType,
        target: GoalTarget,
        target_date: datetime,
        priority: GoalPriority = GoalPriority.MEDIUM
    ) -> UserGoal:
        """Create a new goal for the user."""
        goal = UserGoal(
            user_id=user_id,
            title=title,
            description=description,
            goal_type=goal_type,
            priority=priority,
            target_data=asdict(target),
            target_date=target_date
        )
        
        self.db.add(goal)
        self.db.commit()
        self.db.refresh(goal)
        
        # Create milestones if applicable
        self._create_automatic_milestones(goal)
        
        return goal
    
    def update_goal_progress(self, goal_id: str) -> GoalProgress:
        """Update and calculate progress for a goal."""
        goal = self.db.query(UserGoal).filter(UserGoal.id == goal_id).first()
        if not goal:
            raise ValueError(f"Goal {goal_id} not found")
        
        # Calculate current progress based on goal type
        current_value = self._calculate_current_value(goal)
        target = GoalTarget(**goal.target_data)
        target.current_value = current_value
        
        # Calculate progress percentage
        progress_percentage = self._calculate_progress_percentage(target)
        
        # Update goal in database
        goal.progress_percentage = progress_percentage
        goal.updated_at = datetime.now(timezone.utc)
        
        # Check if goal is completed
        if progress_percentage >= 100 and goal.status == GoalStatus.ACTIVE:
            goal.status = GoalStatus.COMPLETED
            goal.completed_date = datetime.now(timezone.utc)
            self._unlock_goal_achievement(goal)
        
        # Calculate remaining time
        days_remaining = (goal.target_date - datetime.now(timezone.utc)).days
        
        # Check if expired
        if days_remaining < 0 and goal.status == GoalStatus.ACTIVE:
            goal.status = GoalStatus.EXPIRED
        
        # Determine if on track
        time_percentage = self._calculate_time_percentage(goal)
        on_track = progress_percentage >= time_percentage - 10  # 10% tolerance
        
        # Estimate completion date
        estimated_completion = self._estimate_completion_date(goal, current_value)
        
        # Get trend
        trend = self._get_goal_trend(goal)
        
        self.db.commit()
        
        return GoalProgress(
            goal_id=goal_id,
            progress_percentage=progress_percentage,
            days_remaining=max(0, days_remaining),
            on_track=on_track,
            estimated_completion=estimated_completion,
            trend=trend
        )
    
    def get_user_goals(
        self,
        user_id: str,
        status: Optional[GoalStatus] = None,
        include_progress: bool = True
    ) -> List[Dict[str, Any]]:
        """Get all goals for a user."""
        query = self.db.query(UserGoal).filter(UserGoal.user_id == user_id)
        
        if status:
            query = query.filter(UserGoal.status == status)
        
        goals = query.order_by(UserGoal.priority.desc(), UserGoal.created_at.desc()).all()
        
        result = []
        for goal in goals:
            goal_dict = {
                "id": goal.id,
                "title": goal.title,
                "description": goal.description,
                "goal_type": goal.goal_type.value,
                "priority": goal.priority.value,
                "status": goal.status.value,
                "target_data": goal.target_data,
                "start_date": goal.start_date.isoformat(),
                "target_date": goal.target_date.isoformat(),
                "completed_date": goal.completed_date.isoformat() if goal.completed_date else None,
                "progress_percentage": goal.progress_percentage,
                "created_at": goal.created_at.isoformat()
            }
            
            if include_progress:
                try:
                    progress = self.update_goal_progress(goal.id)
                    goal_dict["progress"] = asdict(progress)
                except Exception as e:
                    goal_dict["progress_error"] = str(e)
            
            # Get milestones
            milestones = self.db.query(GoalMilestone).filter(
                GoalMilestone.goal_id == goal.id
            ).order_by(GoalMilestone.order_index).all()
            
            goal_dict["milestones"] = [
                {
                    "id": milestone.id,
                    "title": milestone.title,
                    "description": milestone.description,
                    "target_value": milestone.target_value,
                    "is_completed": milestone.is_completed,
                    "completed_date": milestone.completed_date.isoformat() if milestone.completed_date else None
                } for milestone in milestones
            ]
            
            result.append(goal_dict)
        
        return result
    
    def suggest_goals(self, user_id: str) -> List[Dict[str, Any]]:
        """Suggest goals based on user's performance and areas for improvement."""
        # Get analytics insights
        insights = self.analytics.get_improvement_insights(user_id, days_back=30)
        
        suggestions = []
        
        # Score improvement goal
        if insights["performance_metrics"].average_score:
            current_score = insights["performance_metrics"].average_score
            target_score = min(100, current_score + 10)  # Aim for 10 point improvement
            
            suggestions.append({
                "title": f"Improve Overall Score to {target_score}",
                "description": f"Increase your average swing score from {current_score:.1f} to {target_score}",
                "goal_type": GoalType.SCORE_IMPROVEMENT,
                "priority": GoalPriority.HIGH,
                "target": GoalTarget(
                    metric_name="overall_score",
                    target_value=target_score,
                    current_value=current_score,
                    unit="points",
                    direction="increase"
                ),
                "suggested_duration_days": 30
            })
        
        # Fault reduction goals
        for fault in insights["priority_areas"]["faults"][:2]:  # Top 2 priority faults
            suggestions.append({
                "title": f"Reduce {fault.fault_name}",
                "description": f"Decrease occurrence of {fault.fault_name} from {fault.frequency_percentage:.1f}% to under 10%",
                "goal_type": GoalType.FAULT_REDUCTION,
                "priority": GoalPriority.MEDIUM,
                "target": GoalTarget(
                    metric_name=fault.fault_name,
                    target_value=10.0,
                    current_value=fault.frequency_percentage,
                    unit="percentage",
                    direction="decrease"
                ),
                "suggested_duration_days": 45
            })
        
        # Consistency goal
        if insights["performance_metrics"].consistency_score < 0.8:
            suggestions.append({
                "title": "Improve Consistency",
                "description": "Achieve more consistent swing performance",
                "goal_type": GoalType.CONSISTENCY,
                "priority": GoalPriority.MEDIUM,
                "target": GoalTarget(
                    metric_name="consistency_score",
                    target_value=0.8,
                    current_value=insights["performance_metrics"].consistency_score,
                    unit="score",
                    direction="increase"
                ),
                "suggested_duration_days": 60
            })
        
        # Frequency goal
        if insights["performance_metrics"].sessions_count < 12:  # Less than 3 sessions per week
            suggestions.append({
                "title": "Practice More Regularly",
                "description": "Increase practice frequency to 3 sessions per week",
                "goal_type": GoalType.FREQUENCY,
                "priority": GoalPriority.LOW,
                "target": GoalTarget(
                    metric_name="sessions_per_week",
                    target_value=3.0,
                    current_value=insights["performance_metrics"].sessions_count / 4,  # Assuming 4 weeks
                    unit="sessions/week",
                    direction="increase"
                ),
                "suggested_duration_days": 28
            })
        
        return suggestions
    
    def get_achievements(self, user_id: str, unlocked_only: bool = False) -> List[Dict[str, Any]]:
        """Get user achievements."""
        query = self.db.query(Achievement).filter(Achievement.user_id == user_id)
        
        if unlocked_only:
            query = query.filter(Achievement.is_unlocked == True)
        
        achievements = query.order_by(Achievement.unlocked_date.desc()).all()
        
        return [
            {
                "id": achievement.id,
                "title": achievement.title,
                "description": achievement.description,
                "achievement_type": achievement.achievement_type.value,
                "badge_icon": achievement.badge_icon,
                "is_unlocked": achievement.is_unlocked,
                "unlocked_date": achievement.unlocked_date.isoformat() if achievement.unlocked_date else None,
                "requirements": achievement.requirements,
                "achievement_data": achievement.achievement_data
            } for achievement in achievements
        ]
    
    def check_achievements(self, user_id: str) -> List[Achievement]:
        """Check and unlock new achievements for a user."""
        newly_unlocked = []
        
        # Get user's performance data
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=365)
        sessions_count = performance.sessions_count
        
        # Define achievement criteria
        achievement_definitions = [
            {
                "title": "First Steps",
                "description": "Complete your first swing analysis",
                "type": AchievementType.MILESTONE,
                "badge_icon": "first_swing",
                "criteria": lambda: sessions_count >= 1
            },
            {
                "title": "Getting Consistent",
                "description": "Complete 10 swing analyses",
                "type": AchievementType.MILESTONE,
                "badge_icon": "ten_swings",
                "criteria": lambda: sessions_count >= 10
            },
            {
                "title": "Century Mark",
                "description": "Complete 100 swing analyses",
                "type": AchievementType.MILESTONE,
                "badge_icon": "hundred_swings",
                "criteria": lambda: sessions_count >= 100
            },
            {
                "title": "Score Master",
                "description": "Achieve a swing score of 90 or higher",
                "type": AchievementType.IMPROVEMENT,
                "badge_icon": "high_score",
                "criteria": lambda: performance.best_score and performance.best_score >= 90
            },
            {
                "title": "Consistency King",
                "description": "Maintain consistency score above 0.8 for 30 days",
                "type": AchievementType.CONSISTENCY,
                "badge_icon": "consistent",
                "criteria": lambda: performance.consistency_score >= 0.8 and sessions_count >= 15
            }
        ]
        
        # Check each achievement
        for achievement_def in achievement_definitions:
            # Check if already unlocked
            existing = self.db.query(Achievement).filter(
                Achievement.user_id == user_id,
                Achievement.title == achievement_def["title"]
            ).first()
            
            if existing and existing.is_unlocked:
                continue
            
            # Check criteria
            if achievement_def["criteria"]():
                if existing:
                    # Update existing achievement
                    existing.is_unlocked = True
                    existing.unlocked_date = datetime.now(timezone.utc)
                    newly_unlocked.append(existing)
                else:
                    # Create new achievement
                    achievement = Achievement(
                        user_id=user_id,
                        title=achievement_def["title"],
                        description=achievement_def["description"],
                        achievement_type=achievement_def["type"],
                        badge_icon=achievement_def["badge_icon"],
                        is_unlocked=True,
                        unlocked_date=datetime.now(timezone.utc),
                        achievement_data={"performance_data": asdict(performance)}
                    )
                    self.db.add(achievement)
                    newly_unlocked.append(achievement)
        
        self.db.commit()
        return newly_unlocked
    
    def create_training_plan(
        self,
        user_id: str,
        name: str,
        description: str,
        focus_areas: List[str],
        duration_weeks: int = 4,
        sessions_per_week: int = 3
    ) -> TrainingPlan:
        """Create a personalized training plan."""
        # Get user's improvement insights
        insights = self.analytics.get_improvement_insights(user_id)
        
        # Generate plan structure based on focus areas and insights
        plan_data = self._generate_training_plan_structure(
            focus_areas, insights, duration_weeks, sessions_per_week
        )
        
        # Deactivate any existing active plans
        self.db.query(TrainingPlan).filter(
            TrainingPlan.user_id == user_id,
            TrainingPlan.is_active == True
        ).update({"is_active": False})
        
        # Create new plan
        plan = TrainingPlan(
            user_id=user_id,
            name=name,
            description=description,
            plan_data=plan_data,
            duration_weeks=duration_weeks,
            sessions_per_week=sessions_per_week,
            is_active=True,
            started_date=datetime.now(timezone.utc)
        )
        
        self.db.add(plan)
        self.db.commit()
        self.db.refresh(plan)
        
        return plan
    
    def get_training_plan_progress(self, plan_id: str) -> Dict[str, Any]:
        """Get progress for a training plan."""
        plan = self.db.query(TrainingPlan).filter(TrainingPlan.id == plan_id).first()
        if not plan:
            raise ValueError(f"Training plan {plan_id} not found")
        
        if not plan.is_active or not plan.started_date:
            return {"status": "inactive"}
        
        # Calculate progress
        days_elapsed = (datetime.now(timezone.utc) - plan.started_date).days
        weeks_elapsed = days_elapsed / 7
        
        # Get completed sessions since plan started
        completed_sessions = self.db.query(SwingSession).filter(
            SwingSession.user_id == plan.user_id,
            SwingSession.created_at >= plan.started_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).count()
        
        # Calculate expected sessions
        expected_sessions = int(weeks_elapsed * plan.sessions_per_week)
        
        # Progress percentage
        total_expected_sessions = plan.duration_weeks * plan.sessions_per_week
        progress_percentage = min(100, (completed_sessions / total_expected_sessions) * 100)
        
        return {
            "plan_id": plan_id,
            "name": plan.name,
            "weeks_elapsed": weeks_elapsed,
            "current_week": min(plan.duration_weeks, int(weeks_elapsed) + 1),
            "completed_sessions": completed_sessions,
            "expected_sessions": expected_sessions,
            "on_track": completed_sessions >= expected_sessions * 0.8,  # 80% tolerance
            "progress_percentage": progress_percentage,
            "sessions_this_week": self._get_sessions_this_week(plan.user_id),
            "remaining_weeks": max(0, plan.duration_weeks - weeks_elapsed)
        }
    
    # Private helper methods
    
    def _create_automatic_milestones(self, goal: UserGoal) -> None:
        """Create automatic milestones for a goal."""
        target = GoalTarget(**goal.target_data)
        
        if goal.goal_type in [GoalType.SCORE_IMPROVEMENT, GoalType.KPI_TARGET]:
            # Create 3 milestones: 25%, 50%, 75% of target
            current = target.current_value or 0
            target_value = target.target_value
            diff = target_value - current
            
            milestones = [
                ("Quarter Way", "Achieve 25% of your goal", current + diff * 0.25),
                ("Half Way", "Achieve 50% of your goal", current + diff * 0.5),
                ("Almost There", "Achieve 75% of your goal", current + diff * 0.75)
            ]
            
            for i, (title, desc, value) in enumerate(milestones):
                milestone = GoalMilestone(
                    goal_id=goal.id,
                    title=title,
                    description=desc,
                    target_value=value,
                    order_index=i
                )
                self.db.add(milestone)
        
        self.db.commit()
    
    def _calculate_current_value(self, goal: UserGoal) -> Optional[float]:
        """Calculate current value for a goal based on its type."""
        if goal.goal_type == GoalType.SCORE_IMPROVEMENT:
            # Get average score from last 5 sessions
            recent_scores = self.db.query(SwingAnalysisResult.overall_score).join(
                SwingSession
            ).filter(
                SwingSession.user_id == goal.user_id,
                SwingSession.session_status == SessionStatus.COMPLETED,
                SwingAnalysisResult.overall_score.isnot(None)
            ).order_by(SwingSession.created_at.desc()).limit(5).all()
            
            if recent_scores:
                return sum(score[0] for score in recent_scores) / len(recent_scores)
        
        elif goal.goal_type == GoalType.FAULT_REDUCTION:
            target = GoalTarget(**goal.target_data)
            fault_name = target.metric_name
            
            # Calculate current fault frequency
            total_sessions = self.db.query(SwingSession).filter(
                SwingSession.user_id == goal.user_id,
                SwingSession.created_at >= goal.start_date,
                SwingSession.session_status == SessionStatus.COMPLETED
            ).count()
            
            if total_sessions == 0:
                return 0
            
            sessions_with_fault = self.db.query(SwingSession).join(DetectedFault).filter(
                SwingSession.user_id == goal.user_id,
                SwingSession.created_at >= goal.start_date,
                DetectedFault.fault_name == fault_name
            ).distinct().count()
            
            return (sessions_with_fault / total_sessions) * 100
        
        elif goal.goal_type == GoalType.CONSISTENCY:
            performance = self.analytics.get_user_performance_metrics(goal.user_id, days_back=30)
            return performance.consistency_score
        
        elif goal.goal_type == GoalType.FREQUENCY:
            # Sessions per week since goal started
            days_since_start = (datetime.now(timezone.utc) - goal.start_date).days
            weeks_since_start = max(1, days_since_start / 7)
            
            sessions_count = self.db.query(SwingSession).filter(
                SwingSession.user_id == goal.user_id,
                SwingSession.created_at >= goal.start_date,
                SwingSession.session_status == SessionStatus.COMPLETED
            ).count()
            
            return sessions_count / weeks_since_start
        
        return None
    
    def _calculate_progress_percentage(self, target: GoalTarget) -> float:
        """Calculate progress percentage for a target."""
        if target.current_value is None:
            return 0.0
        
        current = target.current_value
        target_value = target.target_value
        
        if target.direction == "increase":
            # For increasing metrics (scores, consistency)
            start_value = 0  # Assume starting from 0 if no baseline
            if target_value <= start_value:
                return 100.0
            progress = (current - start_value) / (target_value - start_value) * 100
        else:
            # For decreasing metrics (faults, errors)
            start_value = current + target_value  # Estimate starting point
            if start_value <= target_value:
                return 100.0
            progress = (start_value - current) / (start_value - target_value) * 100
        
        return max(0, min(100, progress))
    
    def _calculate_time_percentage(self, goal: UserGoal) -> float:
        """Calculate what percentage of time has elapsed for a goal."""
        total_time = (goal.target_date - goal.start_date).total_seconds()
        elapsed_time = (datetime.now(timezone.utc) - goal.start_date).total_seconds()
        
        if total_time <= 0:
            return 100.0
        
        return min(100, (elapsed_time / total_time) * 100)
    
    def _estimate_completion_date(self, goal: UserGoal, current_value: Optional[float]) -> Optional[datetime]:
        """Estimate when a goal will be completed based on current progress."""
        if not current_value or goal.progress_percentage <= 0:
            return None
        
        target = GoalTarget(**goal.target_data)
        
        # Simple linear projection
        days_elapsed = (datetime.now(timezone.utc) - goal.start_date).days
        if days_elapsed <= 0:
            return None
        
        progress_rate = goal.progress_percentage / days_elapsed  # Progress per day
        if progress_rate <= 0:
            return None
        
        remaining_progress = 100 - goal.progress_percentage
        days_to_completion = remaining_progress / progress_rate
        
        estimated_date = datetime.now(timezone.utc) + timedelta(days=days_to_completion)
        
        # Don't estimate beyond target date
        if estimated_date > goal.target_date:
            return goal.target_date
        
        return estimated_date
    
    def _get_goal_trend(self, goal: UserGoal) -> TrendDirection:
        """Get trend direction for a goal."""
        # Simple implementation - could be more sophisticated
        if goal.progress_percentage >= 75:
            return TrendDirection.IMPROVING
        elif goal.progress_percentage <= 25:
            return TrendDirection.DECLINING
        else:
            return TrendDirection.STABLE
    
    def _unlock_goal_achievement(self, goal: UserGoal) -> None:
        """Unlock achievement for completing a goal."""
        achievement = Achievement(
            user_id=goal.user_id,
            title=f"Goal Achieved: {goal.title}",
            description=f"Successfully completed the goal: {goal.title}",
            achievement_type=AchievementType.MILESTONE,
            badge_icon="goal_complete",
            is_unlocked=True,
            unlocked_date=datetime.now(timezone.utc),
            achievement_data={"goal_id": goal.id, "goal_type": goal.goal_type.value}
        )
        self.db.add(achievement)
    
    def _generate_training_plan_structure(
        self,
        focus_areas: List[str],
        insights: Dict[str, Any],
        duration_weeks: int,
        sessions_per_week: int
    ) -> Dict[str, Any]:
        """Generate training plan structure based on focus areas and insights."""
        plan_structure = {
            "focus_areas": focus_areas,
            "duration_weeks": duration_weeks,
            "sessions_per_week": sessions_per_week,
            "weekly_plans": []
        }
        
        # Generate weekly plans
        for week in range(1, duration_weeks + 1):
            weekly_plan = {
                "week": week,
                "theme": self._get_weekly_theme(week, focus_areas),
                "sessions": []
            }
            
            for session in range(1, sessions_per_week + 1):
                session_plan = {
                    "session": session,
                    "focus": focus_areas[(session - 1) % len(focus_areas)],
                    "drills": self._get_recommended_drills(focus_areas, week),
                    "targets": self._get_session_targets(insights, week)
                }
                weekly_plan["sessions"].append(session_plan)
            
            plan_structure["weekly_plans"].append(weekly_plan)
        
        return plan_structure
    
    def _get_weekly_theme(self, week: int, focus_areas: List[str]) -> str:
        """Get theme for a specific week."""
        themes = {
            1: "Foundation Building",
            2: "Skill Development", 
            3: "Integration & Consistency",
            4: "Performance & Assessment"
        }
        return themes.get(week, f"Week {week} - {focus_areas[0] if focus_areas else 'General'}")
    
    def _get_recommended_drills(self, focus_areas: List[str], week: int) -> List[str]:
        """Get recommended drills based on focus areas."""
        drill_library = {
            "backswing": ["Slow Motion Backswing", "Mirror Work", "Club Position Check"],
            "downswing": ["Hip Rotation Drill", "Tempo Training", "Impact Position"],
            "follow_through": ["Full Extension Drill", "Balance Finish", "Follow Through Hold"],
            "general": ["Full Swing Practice", "Rhythm Training", "Video Analysis"]
        }
        
        drills = []
        for area in focus_areas:
            if area.lower() in drill_library:
                drills.extend(drill_library[area.lower()][:2])  # 2 drills per area
        
        if not drills:
            drills = drill_library["general"]
        
        return drills[:3]  # Limit to 3 drills per session
    
    def _get_session_targets(self, insights: Dict[str, Any], week: int) -> Dict[str, Any]:
        """Get targets for a training session."""
        base_score = insights["performance_metrics"].average_score or 60
        target_improvement = week * 2  # 2 points per week
        
        return {
            "target_score": min(100, base_score + target_improvement),
            "focus_kpis": insights["priority_areas"]["kpis"][:2] if insights["priority_areas"]["kpis"] else [],
            "avoid_faults": insights["priority_areas"]["faults"][:2] if insights["priority_areas"]["faults"] else []
        }
    
    def _get_sessions_this_week(self, user_id: str) -> int:
        """Get number of sessions completed this week."""
        week_start = datetime.now(timezone.utc) - timedelta(days=datetime.now().weekday())
        
        return self.db.query(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= week_start,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).count()