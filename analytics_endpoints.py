"""
API endpoints for analytics features in SwingSync AI.

This module provides:
- RESTful API endpoints for analytics data
- Progress tracking and goal management endpoints
- Insights and recommendations API
- Visualization data endpoints
- Export and sharing functionality
- Dashboard data endpoints
- Comparative analysis endpoints

Key Features:
- Comprehensive analytics REST API
- Authentication and authorization
- Data validation and error handling
- Performance optimization with caching
- Export capabilities for data sharing
- Real-time progress tracking
- Personalized insights delivery
"""

from fastapi import APIRouter, Depends, HTTPException, Query, Body, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, validator
from typing import List, Dict, Any, Optional, Union
from datetime import datetime, timedelta, timezone
from sqlalchemy.orm import Session
from enum import Enum
import json

# Import database and authentication
from database import get_db, User
from user_management import get_current_active_user

# Import analytics modules
from analytics import AnalyticsEngine, TrendDirection, MetricType
from progress_tracking import (
    ProgressTracker, GoalType, GoalPriority, GoalStatus, 
    AchievementType, GoalTarget
)
from insights import InsightsEngine, InsightType, InsightPriority
from visualization_data import (
    VisualizationDataEngine, ChartType, TimeInterval
)

# Create router
router = APIRouter(prefix="/analytics", tags=["Analytics"])

# Request/Response Models

class TimeRangeRequest(BaseModel):
    """Time range for analytics queries."""
    days_back: int = Field(30, ge=1, le=365, description="Number of days to look back")

class GoalCreateRequest(BaseModel):
    """Request model for creating goals."""
    title: str = Field(..., min_length=1, max_length=200)
    description: Optional[str] = Field(None, max_length=1000)
    goal_type: str = Field(..., description="Type of goal")
    priority: str = Field("medium", description="Goal priority")
    target_metric: str = Field(..., description="Target metric name")
    target_value: float = Field(..., description="Target value")
    target_unit: str = Field("", description="Unit of measurement")
    target_direction: str = Field("increase", description="Increase or decrease")
    target_date: datetime = Field(..., description="Target completion date")
    
    @validator('goal_type')
    def validate_goal_type(cls, v):
        valid_types = [t.value for t in GoalType]
        if v not in valid_types:
            raise ValueError(f"Invalid goal type. Must be one of: {valid_types}")
        return v
    
    @validator('priority')
    def validate_priority(cls, v):
        valid_priorities = [p.value for p in GoalPriority]
        if v not in valid_priorities:
            raise ValueError(f"Invalid priority. Must be one of: {valid_priorities}")
        return v

class ChartRequest(BaseModel):
    """Request model for chart data."""
    chart_type: str = Field(..., description="Type of chart")
    days_back: int = Field(30, ge=1, le=365)
    interval: str = Field("session", description="Time interval for aggregation")
    include_trend: bool = Field(True, description="Include trend line")
    
    @validator('chart_type')
    def validate_chart_type(cls, v):
        valid_types = [t.value for t in ChartType]
        if v not in valid_types:
            raise ValueError(f"Invalid chart type. Must be one of: {valid_types}")
        return v

class ExportRequest(BaseModel):
    """Request model for data export."""
    format_type: str = Field("json", description="Export format")
    days_back: int = Field(90, ge=1, le=365)
    include_charts: bool = Field(True, description="Include chart data")
    include_raw_data: bool = Field(False, description="Include raw session data")

class ComparisonRequest(BaseModel):
    """Request model for comparative analysis."""
    comparison_type: str = Field("skill_level", description="Type of comparison")
    user_ids: Optional[List[str]] = Field(None, description="Specific users to compare against")
    days_back: int = Field(90, ge=1, le=365)

# Analytics Endpoints

@router.get("/performance/overview")
async def get_performance_overview(
    days_back: int = Query(30, ge=1, le=365),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get comprehensive performance overview."""
    try:
        analytics = AnalyticsEngine(db)
        
        # Get performance metrics
        performance = analytics.get_user_performance_metrics(current_user.id, days_back)
        score_trend = analytics.analyze_score_trend(current_user.id, days_back)
        improvement_insights = analytics.get_improvement_insights(current_user.id, days_back)
        
        return {
            "user_id": current_user.id,
            "period_days": days_back,
            "performance_metrics": {
                "average_score": performance.average_score,
                "best_score": performance.best_score,
                "worst_score": performance.worst_score,
                "score_variance": performance.score_variance,
                "improvement_rate": performance.improvement_rate,
                "consistency_score": performance.consistency_score,
                "sessions_count": performance.sessions_count,
                "active_days": performance.active_days
            },
            "trend_analysis": {
                "direction": score_trend.direction.value,
                "magnitude": score_trend.magnitude,
                "confidence": score_trend.confidence,
                "change_percentage": score_trend.change_percentage,
                "statistical_significance": score_trend.statistical_significance,
                "start_value": score_trend.start_value,
                "end_value": score_trend.end_value
            },
            "improvement_score": improvement_insights["improvement_score"],
            "priority_areas": improvement_insights["priority_areas"],
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating performance overview: {str(e)}"
        )

@router.get("/trends/score")
async def get_score_trends(
    days_back: int = Query(30, ge=1, le=365),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get detailed score trend analysis."""
    try:
        analytics = AnalyticsEngine(db)
        trend = analytics.analyze_score_trend(current_user.id, days_back)
        
        return {
            "user_id": current_user.id,
            "period_days": days_back,
            "trend_analysis": {
                "direction": trend.direction.value,
                "magnitude": trend.magnitude,
                "confidence": trend.confidence,
                "period_days": trend.period_days,
                "start_value": trend.start_value,
                "end_value": trend.end_value,
                "change_percentage": trend.change_percentage,
                "statistical_significance": trend.statistical_significance
            },
            "interpretation": _interpret_trend(trend),
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error analyzing score trends: {str(e)}"
        )

@router.get("/faults/patterns")
async def get_fault_patterns(
    days_back: int = Query(30, ge=1, le=365),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get fault pattern analysis."""
    try:
        analytics = AnalyticsEngine(db)
        fault_patterns = analytics.analyze_fault_patterns(current_user.id, days_back)
        
        return {
            "user_id": current_user.id,
            "period_days": days_back,
            "total_fault_types": len(fault_patterns),
            "fault_patterns": [
                {
                    "fault_name": fp.fault_name,
                    "frequency": fp.frequency,
                    "frequency_percentage": fp.frequency_percentage,
                    "average_severity": fp.average_severity,
                    "trend": fp.trend.value,
                    "sessions_affected": fp.sessions_affected,
                    "improvement_needed": fp.improvement_needed,
                    "related_kpis": fp.related_kpis
                } for fp in fault_patterns
            ],
            "summary": {
                "most_frequent": fault_patterns[0].fault_name if fault_patterns else None,
                "improving_faults": len([fp for fp in fault_patterns if fp.trend == TrendDirection.IMPROVING]),
                "declining_faults": len([fp for fp in fault_patterns if fp.trend == TrendDirection.DECLINING]),
                "critical_faults": len([fp for fp in fault_patterns if fp.improvement_needed])
            },
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error analyzing fault patterns: {str(e)}"
        )

@router.get("/kpis/performance")
async def get_kpi_performance(
    days_back: int = Query(30, ge=1, le=365),
    kpi_name: Optional[str] = Query(None, description="Specific KPI to analyze"),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get KPI performance analysis."""
    try:
        analytics = AnalyticsEngine(db)
        kpi_analyses = analytics.analyze_kpi_performance(current_user.id, days_back, kpi_name)
        
        return {
            "user_id": current_user.id,
            "period_days": days_back,
            "kpi_filter": kpi_name,
            "total_kpis": len(kpi_analyses),
            "kpi_analyses": [
                {
                    "kpi_name": ka.kpi_name,
                    "p_position": ka.p_position,
                    "average_value": ka.average_value,
                    "best_value": ka.best_value,
                    "worst_value": ka.worst_value,
                    "variance": ka.variance,
                    "trend": ka.trend.value,
                    "sessions_count": ka.sessions_count,
                    "correlation_with_score": ka.correlation_with_score,
                    "deviation_frequency": ka.deviation_frequency
                } for ka in kpi_analyses
            ],
            "summary": {
                "improving_kpis": len([ka for ka in kpi_analyses if ka.trend == TrendDirection.IMPROVING]),
                "declining_kpis": len([ka for ka in kpi_analyses if ka.trend == TrendDirection.DECLINING]),
                "problematic_kpis": len([ka for ka in kpi_analyses if ka.deviation_frequency > 0.3])
            },
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error analyzing KPI performance: {str(e)}"
        )

# Progress Tracking Endpoints

@router.get("/goals")
async def get_user_goals(
    status: Optional[str] = Query(None, description="Filter by goal status"),
    include_progress: bool = Query(True, description="Include progress calculations"),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get user's goals with progress tracking."""
    try:
        progress_tracker = ProgressTracker(db)
        
        goal_status = None
        if status:
            try:
                goal_status = GoalStatus(status)
            except ValueError:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Invalid status. Must be one of: {[s.value for s in GoalStatus]}"
                )
        
        goals = progress_tracker.get_user_goals(current_user.id, goal_status, include_progress)
        
        return {
            "user_id": current_user.id,
            "total_goals": len(goals),
            "goals": goals,
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error retrieving goals: {str(e)}"
        )

@router.post("/goals")
async def create_goal(
    goal_request: GoalCreateRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Create a new goal for the user."""
    try:
        progress_tracker = ProgressTracker(db)
        
        # Create goal target
        target = GoalTarget(
            metric_name=goal_request.target_metric,
            target_value=goal_request.target_value,
            unit=goal_request.target_unit,
            direction=goal_request.target_direction
        )
        
        # Create goal
        goal = progress_tracker.create_goal(
            user_id=current_user.id,
            title=goal_request.title,
            description=goal_request.description,
            goal_type=GoalType(goal_request.goal_type),
            target=target,
            target_date=goal_request.target_date,
            priority=GoalPriority(goal_request.priority)
        )
        
        return {
            "message": "Goal created successfully",
            "goal_id": goal.id,
            "goal": {
                "id": goal.id,
                "title": goal.title,
                "description": goal.description,
                "goal_type": goal.goal_type.value,
                "priority": goal.priority.value,
                "status": goal.status.value,
                "target_date": goal.target_date.isoformat(),
                "created_at": goal.created_at.isoformat()
            }
        }
    
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error creating goal: {str(e)}"
        )

@router.get("/goals/suggestions")
async def get_goal_suggestions(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get AI-generated goal suggestions."""
    try:
        progress_tracker = ProgressTracker(db)
        suggestions = progress_tracker.suggest_goals(current_user.id)
        
        return {
            "user_id": current_user.id,
            "total_suggestions": len(suggestions),
            "suggestions": suggestions,
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating goal suggestions: {str(e)}"
        )

@router.put("/goals/{goal_id}/progress")
async def update_goal_progress(
    goal_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Update progress for a specific goal."""
    try:
        progress_tracker = ProgressTracker(db)
        progress = progress_tracker.update_goal_progress(goal_id)
        
        return {
            "goal_id": goal_id,
            "progress": {
                "progress_percentage": progress.progress_percentage,
                "days_remaining": progress.days_remaining,
                "on_track": progress.on_track,
                "estimated_completion": progress.estimated_completion.isoformat() if progress.estimated_completion else None,
                "trend": progress.trend.value
            },
            "updated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error updating goal progress: {str(e)}"
        )

@router.get("/achievements")
async def get_achievements(
    unlocked_only: bool = Query(False, description="Get only unlocked achievements"),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get user achievements."""
    try:
        progress_tracker = ProgressTracker(db)
        achievements = progress_tracker.get_achievements(current_user.id, unlocked_only)
        
        # Check for new achievements
        new_achievements = progress_tracker.check_achievements(current_user.id)
        
        return {
            "user_id": current_user.id,
            "total_achievements": len(achievements),
            "unlocked_count": len([a for a in achievements if a["is_unlocked"]]),
            "new_achievements": len(new_achievements),
            "achievements": achievements,
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error retrieving achievements: {str(e)}"
        )

# Insights Endpoints

@router.get("/insights")
async def get_insights(
    days_back: int = Query(30, ge=1, le=365),
    priority_filter: Optional[str] = Query(None, description="Filter by priority level"),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get AI-powered insights and recommendations."""
    try:
        insights_engine = InsightsEngine(db)
        insights = insights_engine.generate_comprehensive_insights(current_user.id, days_back)
        
        # Filter by priority if requested
        if priority_filter:
            try:
                priority = InsightPriority(priority_filter)
                insights = [i for i in insights if i.priority == priority]
            except ValueError:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Invalid priority. Must be one of: {[p.value for p in InsightPriority]}"
                )
        
        return {
            "user_id": current_user.id,
            "period_days": days_back,
            "total_insights": len(insights),
            "priority_filter": priority_filter,
            "insights": [
                {
                    "type": insight.type.value,
                    "priority": insight.priority.value,
                    "title": insight.title,
                    "description": insight.description,
                    "recommendation": insight.recommendation,
                    "confidence": insight.confidence,
                    "actionable_steps": insight.actionable_steps,
                    "timeframe": insight.timeframe,
                    "data_points": insight.data_points,
                    "created_at": insight.created_at.isoformat()
                } for insight in insights
            ],
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating insights: {str(e)}"
        )

@router.get("/insights/recommendations")
async def get_personalized_recommendations(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get personalized training recommendations."""
    try:
        insights_engine = InsightsEngine(db)
        recommendations = insights_engine.generate_personalized_recommendations(current_user.id)
        
        return {
            "user_id": current_user.id,
            "total_recommendations": len(recommendations),
            "recommendations": [
                {
                    "title": rec.title,
                    "description": rec.description,
                    "focus_areas": rec.focus_areas,
                    "difficulty_level": rec.difficulty_level,
                    "estimated_improvement": rec.estimated_improvement,
                    "time_commitment": rec.time_commitment,
                    "specific_drills": rec.specific_drills,
                    "success_metrics": rec.success_metrics
                } for rec in recommendations
            ],
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating recommendations: {str(e)}"
        )

@router.get("/insights/prediction")
async def get_performance_prediction(
    timeframe_days: int = Query(30, ge=7, le=90, description="Prediction timeframe in days"),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get performance prediction based on current trends."""
    try:
        insights_engine = InsightsEngine(db)
        prediction = insights_engine.predict_performance(current_user.id, timeframe_days)
        
        return {
            "user_id": current_user.id,
            "prediction": {
                "timeframe_days": prediction.timeframe_days,
                "predicted_score": prediction.predicted_score,
                "confidence_interval": {
                    "lower": prediction.confidence_interval[0],
                    "upper": prediction.confidence_interval[1]
                },
                "key_factors": prediction.key_factors,
                "risk_factors": prediction.risk_factors,
                "opportunities": prediction.opportunities
            },
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating performance prediction: {str(e)}"
        )

# Visualization Endpoints

@router.get("/charts/score-trend")
async def get_score_trend_chart(
    chart_request: ChartRequest = Depends(),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get score trend chart data."""
    try:
        viz_engine = VisualizationDataEngine(db)
        
        # Convert string to enum
        interval = TimeInterval(chart_request.interval)
        
        chart_config = viz_engine.generate_score_trend_chart(
            current_user.id, 
            chart_request.days_back, 
            interval
        )
        
        return {
            "user_id": current_user.id,
            "chart_config": {
                "title": chart_config.title,
                "chart_type": chart_config.chart_type.value,
                "x_axis_label": chart_config.x_axis_label,
                "y_axis_label": chart_config.y_axis_label,
                "datasets": [
                    {
                        "label": ds.label,
                        "chart_type": ds.chart_type.value,
                        "data": [
                            {
                                "x": dp.x,
                                "y": dp.y,
                                "label": dp.label,
                                "metadata": dp.metadata
                            } for dp in ds.data
                        ],
                        "color": ds.color,
                        "background_color": ds.background_color,
                        "border_color": ds.border_color,
                        "fill": ds.fill,
                        "tension": ds.tension
                    } for ds in chart_config.datasets
                ],
                "options": chart_config.options,
                "export_data": chart_config.export_data
            },
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating score trend chart: {str(e)}"
        )

@router.get("/charts/fault-frequency")
async def get_fault_frequency_chart(
    days_back: int = Query(30, ge=1, le=365),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get fault frequency chart data."""
    try:
        viz_engine = VisualizationDataEngine(db)
        chart_config = viz_engine.generate_fault_frequency_chart(current_user.id, days_back)
        
        return {
            "user_id": current_user.id,
            "chart_config": _serialize_chart_config(chart_config),
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating fault frequency chart: {str(e)}"
        )

@router.get("/dashboard")
async def get_dashboard_data(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get complete dashboard data with widgets."""
    try:
        viz_engine = VisualizationDataEngine(db)
        widgets = viz_engine.generate_dashboard_widgets(current_user.id)
        
        return {
            "user_id": current_user.id,
            "total_widgets": len(widgets),
            "widgets": [
                {
                    "id": widget.id,
                    "title": widget.title,
                    "type": widget.type,
                    "size": widget.size,
                    "data": widget.data,
                    "refresh_interval": widget.refresh_interval,
                    "last_updated": widget.last_updated.isoformat() if widget.last_updated else None
                } for widget in widgets
            ],
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating dashboard data: {str(e)}"
        )

# Comparative Analysis Endpoints

@router.post("/comparison")
async def get_comparative_analysis(
    comparison_request: ComparisonRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get comparative performance analysis."""
    try:
        insights_engine = InsightsEngine(db)
        comparison = insights_engine.generate_comparative_analysis(
            current_user.id,
            comparison_request.comparison_type
        )
        
        return {
            "user_id": current_user.id,
            "comparison_type": comparison_request.comparison_type,
            "comparison_data": comparison,
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating comparative analysis: {str(e)}"
        )

# Export and Sharing Endpoints

@router.post("/export")
async def export_analytics_data(
    export_request: ExportRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Export comprehensive analytics data."""
    try:
        viz_engine = VisualizationDataEngine(db)
        export_data = viz_engine.export_analytics_data(
            current_user.id,
            export_request.format_type,
            export_request.days_back
        )
        
        # Add export metadata
        export_data["export_options"] = {
            "format_type": export_request.format_type,
            "include_charts": export_request.include_charts,
            "include_raw_data": export_request.include_raw_data
        }
        
        return export_data
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error exporting analytics data: {str(e)}"
        )

@router.get("/summary")
async def get_analytics_summary(
    days_back: int = Query(30, ge=1, le=365),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get comprehensive analytics summary report."""
    try:
        insights_engine = InsightsEngine(db)
        report = insights_engine.create_insight_summary_report(current_user.id, days_back)
        
        return report
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating analytics summary: {str(e)}"
        )

# Helper Functions

def _interpret_trend(trend) -> Dict[str, str]:
    """Interpret trend analysis for user-friendly display."""
    interpretations = {
        TrendDirection.IMPROVING: {
            "message": "Your performance is improving!",
            "advice": "Keep up the great work and maintain your current practice routine."
        },
        TrendDirection.DECLINING: {
            "message": "Your performance has been declining recently.",
            "advice": "Review your recent sessions and consider adjusting your practice approach."
        },
        TrendDirection.STABLE: {
            "message": "Your performance is stable.",
            "advice": "Consider setting new challenges to continue improving."
        },
        TrendDirection.INSUFFICIENT_DATA: {
            "message": "Not enough data to determine a clear trend.",
            "advice": "Complete more practice sessions to get better insights."
        }
    }
    
    return interpretations.get(trend.direction, {
        "message": "Unable to interpret trend.",
        "advice": "Continue practicing regularly for better insights."
    })

def _serialize_chart_config(chart_config) -> Dict[str, Any]:
    """Serialize chart configuration for JSON response."""
    return {
        "title": chart_config.title,
        "chart_type": chart_config.chart_type.value,
        "x_axis_label": chart_config.x_axis_label,
        "y_axis_label": chart_config.y_axis_label,
        "datasets": [
            {
                "label": ds.label,
                "chart_type": ds.chart_type.value,
                "data": [
                    {
                        "x": dp.x,
                        "y": dp.y,
                        "label": dp.label,
                        "color": dp.color,
                        "metadata": dp.metadata
                    } for dp in ds.data
                ],
                "color": ds.color,
                "background_color": ds.background_color,
                "border_color": ds.border_color,
                "fill": ds.fill,
                "tension": ds.tension
            } for ds in chart_config.datasets
        ],
        "options": chart_config.options,
        "export_data": chart_config.export_data
    }