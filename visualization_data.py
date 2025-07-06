"""
Data formatting for charts and graphs in SwingSync AI.

This module provides:
- Chart data preparation for various visualization types
- Time-series data formatting for trend analysis
- Statistical data aggregation for dashboard widgets
- Comparative analysis data structures
- Interactive visualization data formats
- Export data formatting for sharing
- Dashboard layout and configuration

Key Features:
- Standardized chart data formats (Chart.js, D3.js compatible)
- Performance trend visualization data
- Fault pattern chart data
- KPI comparison charts
- Progress tracking visualizations
- Goal achievement displays
- Comparative analysis charts
- Export-ready data formats
"""

from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional, Tuple, Union
from dataclasses import dataclass, asdict
from enum import Enum
import statistics
from collections import defaultdict, OrderedDict
import json

from sqlalchemy.orm import Session
from sqlalchemy import func, desc, and_

from database import (
    SwingSession, SwingAnalysisResult, BiomechanicalKPI,
    DetectedFault, User, SessionStatus
)
from analytics import AnalyticsEngine, TrendDirection
from progress_tracking import ProgressTracker, GoalStatus
from insights import InsightsEngine

class ChartType(Enum):
    """Types of charts supported."""
    LINE = "line"
    BAR = "bar"
    PIE = "pie"
    DOUGHNUT = "doughnut"
    RADAR = "radar"
    SCATTER = "scatter"
    AREA = "area"
    HEATMAP = "heatmap"

class TimeInterval(Enum):
    """Time intervals for aggregation."""
    DAILY = "daily"
    WEEKLY = "weekly"
    MONTHLY = "monthly"
    SESSION = "session"

@dataclass
class ChartDatapoint:
    """Single data point for charts."""
    x: Union[str, float, datetime]
    y: Union[float, int]
    label: Optional[str] = None
    color: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None

@dataclass
class ChartDataset:
    """Dataset for charts."""
    label: str
    data: List[ChartDatapoint]
    chart_type: ChartType = ChartType.LINE
    color: Optional[str] = None
    background_color: Optional[str] = None
    border_color: Optional[str] = None
    fill: bool = False
    tension: float = 0.4

@dataclass
class ChartConfiguration:
    """Complete chart configuration."""
    title: str
    datasets: List[ChartDataset]
    chart_type: ChartType
    x_axis_label: str
    y_axis_label: str
    options: Dict[str, Any]
    export_data: Optional[Dict[str, Any]] = None

@dataclass
class DashboardWidget:
    """Dashboard widget configuration."""
    id: str
    title: str
    type: str  # "chart", "metric", "table", "progress"
    size: str  # "small", "medium", "large", "full"
    data: Any
    refresh_interval: Optional[int] = None  # seconds
    last_updated: datetime = None

class VisualizationDataEngine:
    """Main engine for generating visualization data."""
    
    def __init__(self, db_session: Session):
        self.db = db_session
        self.analytics = AnalyticsEngine(db_session)
        self.progress_tracker = ProgressTracker(db_session)
        self.insights = InsightsEngine(db_session)
    
    def generate_score_trend_chart(
        self, 
        user_id: str, 
        days_back: int = 30,
        interval: TimeInterval = TimeInterval.SESSION
    ) -> ChartConfiguration:
        """Generate score trend chart data."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get score data
        results = self.db.query(
            SwingAnalysisResult.overall_score,
            SwingSession.created_at,
            SwingSession.club_used
        ).join(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.session_status == SessionStatus.COMPLETED,
            SwingSession.created_at >= start_date,
            SwingAnalysisResult.overall_score.isnot(None)
        ).order_by(SwingSession.created_at).all()
        
        if not results:
            return self._empty_chart("Score Trend", "No data available")
        
        # Process data based on interval
        if interval == TimeInterval.SESSION:
            datapoints = [
                ChartDatapoint(
                    x=result.created_at.isoformat(),
                    y=result.overall_score,
                    label=f"Score: {result.overall_score}",
                    metadata={"club": result.club_used}
                ) for result in results
            ]
        else:
            # Aggregate by time interval
            aggregated_data = self._aggregate_by_interval(
                [(r.created_at, r.overall_score) for r in results],
                interval
            )
            datapoints = [
                ChartDatapoint(x=date, y=avg_score, label=f"Avg: {avg_score:.1f}")
                for date, avg_score in aggregated_data
            ]
        
        # Add trend line
        trend_line = self._calculate_trend_line([dp.y for dp in datapoints])
        
        datasets = [
            ChartDataset(
                label="Swing Scores",
                data=datapoints,
                chart_type=ChartType.LINE,
                color="#3B82F6",
                border_color="#3B82F6",
                background_color="#3B82F6",
                fill=False
            ),
            ChartDataset(
                label="Trend",
                data=[
                    ChartDatapoint(x=datapoints[0].x, y=trend_line[0]),
                    ChartDatapoint(x=datapoints[-1].x, y=trend_line[-1])
                ],
                chart_type=ChartType.LINE,
                color="#EF4444",
                border_color="#EF4444",
                fill=False,
                tension=0
            )
        ]
        
        return ChartConfiguration(
            title="Score Trend Analysis",
            datasets=datasets,
            chart_type=ChartType.LINE,
            x_axis_label="Time",
            y_axis_label="Score",
            options={
                "responsive": True,
                "scales": {
                    "y": {"min": 0, "max": 100}
                },
                "plugins": {
                    "legend": {"display": True},
                    "tooltip": {"mode": "index", "intersect": False}
                }
            },
            export_data={"raw_scores": [r.overall_score for r in results]}
        )
    
    def generate_fault_frequency_chart(
        self, 
        user_id: str, 
        days_back: int = 30
    ) -> ChartConfiguration:
        """Generate fault frequency pie chart."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get fault data
        fault_counts = self.db.query(
            DetectedFault.fault_name,
            func.count(DetectedFault.id).label('count')
        ).join(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= start_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).group_by(DetectedFault.fault_name).order_by(
            func.count(DetectedFault.id).desc()
        ).limit(8).all()  # Top 8 faults
        
        if not fault_counts:
            return self._empty_chart("Fault Frequency", "No faults detected")
        
        total_faults = sum(count for _, count in fault_counts)
        colors = self._generate_colors(len(fault_counts))
        
        datapoints = [
            ChartDatapoint(
                x=fault_name,
                y=count,
                label=f"{fault_name}: {count} ({count/total_faults*100:.1f}%)",
                color=colors[i],
                metadata={"percentage": count/total_faults*100}
            ) for i, (fault_name, count) in enumerate(fault_counts)
        ]
        
        dataset = ChartDataset(
            label="Fault Frequency",
            data=datapoints,
            chart_type=ChartType.PIE,
            color=colors
        )
        
        return ChartConfiguration(
            title="Most Common Swing Faults",
            datasets=[dataset],
            chart_type=ChartType.PIE,
            x_axis_label="",
            y_axis_label="",
            options={
                "responsive": True,
                "plugins": {
                    "legend": {"position": "bottom"},
                    "tooltip": {
                        "callbacks": {
                            "label": "function(context) { return context.label + ': ' + context.parsed + ' (' + (context.parsed/context.dataset.total*100).toFixed(1) + '%)'; }"
                        }
                    }
                }
            },
            export_data={"fault_summary": dict(fault_counts)}
        )
    
    def generate_kpi_comparison_chart(
        self, 
        user_id: str, 
        kpi_names: List[str],
        days_back: int = 30
    ) -> ChartConfiguration:
        """Generate KPI comparison radar chart."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get KPI data for each requested KPI
        kpi_data = {}
        for kpi_name in kpi_names:
            kpi_values = self.db.query(BiomechanicalKPI.value).join(SwingSession).filter(
                SwingSession.user_id == user_id,
                SwingSession.created_at >= start_date,
                BiomechanicalKPI.kpi_name == kpi_name,
                BiomechanicalKPI.value.isnot(None)
            ).all()
            
            if kpi_values:
                values = [v[0] for v in kpi_values]
                kpi_data[kpi_name] = {
                    "average": statistics.mean(values),
                    "best": max(values),
                    "worst": min(values),
                    "count": len(values)
                }
        
        if not kpi_data:
            return self._empty_chart("KPI Comparison", "No KPI data available")
        
        # Normalize values to 0-100 scale for radar chart
        normalized_data = {}
        for kpi_name, data in kpi_data.items():
            # Simple normalization (you might want more sophisticated scaling)
            normalized_data[kpi_name] = min(100, max(0, data["average"]))
        
        datapoints = [
            ChartDatapoint(x=kpi_name, y=value, label=f"{kpi_name}: {value:.1f}")
            for kpi_name, value in normalized_data.items()
        ]
        
        dataset = ChartDataset(
            label="KPI Performance",
            data=datapoints,
            chart_type=ChartType.RADAR,
            color="#10B981",
            background_color="rgba(16, 185, 129, 0.2)",
            border_color="#10B981",
            fill=True
        )
        
        return ChartConfiguration(
            title="KPI Performance Overview",
            datasets=[dataset],
            chart_type=ChartType.RADAR,
            x_axis_label="",
            y_axis_label="",
            options={
                "responsive": True,
                "scales": {
                    "r": {
                        "min": 0,
                        "max": 100,
                        "beginAtZero": True
                    }
                },
                "plugins": {
                    "legend": {"display": True}
                }
            },
            export_data={"kpi_details": kpi_data}
        )
    
    def generate_progress_chart(
        self, 
        user_id: str,
        goal_id: Optional[str] = None
    ) -> ChartConfiguration:
        """Generate goal progress chart."""
        goals = self.progress_tracker.get_user_goals(user_id, include_progress=True)
        
        if goal_id:
            goals = [g for g in goals if g["id"] == goal_id]
        
        if not goals:
            return self._empty_chart("Goal Progress", "No active goals")
        
        # Create progress chart
        datapoints = []
        colors = self._generate_colors(len(goals))
        
        for i, goal in enumerate(goals):
            progress = goal.get("progress_percentage", 0)
            datapoints.append(ChartDatapoint(
                x=goal["title"],
                y=progress,
                label=f"{goal['title']}: {progress:.1f}%",
                color=colors[i],
                metadata={
                    "goal_type": goal["goal_type"],
                    "status": goal["status"],
                    "target_date": goal["target_date"]
                }
            ))
        
        dataset = ChartDataset(
            label="Goal Progress",
            data=datapoints,
            chart_type=ChartType.BAR,
            color=colors
        )
        
        return ChartConfiguration(
            title="Goal Progress Tracking",
            datasets=[dataset],
            chart_type=ChartType.BAR,
            x_axis_label="Goals",
            y_axis_label="Progress (%)",
            options={
                "responsive": True,
                "scales": {
                    "y": {"min": 0, "max": 100}
                },
                "plugins": {
                    "legend": {"display": False}
                }
            },
            export_data={"goals_summary": goals}
        )
    
    def generate_session_heatmap(
        self, 
        user_id: str, 
        days_back: int = 90
    ) -> ChartConfiguration:
        """Generate practice session frequency heatmap."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get session data
        sessions = self.db.query(SwingSession.created_at).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= start_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).all()
        
        if not sessions:
            return self._empty_chart("Practice Heatmap", "No session data")
        
        # Create daily aggregation
        daily_counts = defaultdict(int)
        for session in sessions:
            date_key = session.created_at.date().isoformat()
            daily_counts[date_key] += 1
        
        # Create heatmap data
        current_date = start_date.date()
        end_date = datetime.now(timezone.utc).date()
        
        datapoints = []
        while current_date <= end_date:
            date_key = current_date.isoformat()
            count = daily_counts.get(date_key, 0)
            
            # Calculate intensity (0-1 scale)
            max_sessions_per_day = max(daily_counts.values()) if daily_counts else 1
            intensity = count / max_sessions_per_day if max_sessions_per_day > 0 else 0
            
            datapoints.append(ChartDatapoint(
                x=date_key,
                y=count,
                label=f"{date_key}: {count} sessions",
                metadata={
                    "intensity": intensity,
                    "weekday": current_date.strftime("%A")
                }
            ))
            
            current_date += timedelta(days=1)
        
        dataset = ChartDataset(
            label="Practice Sessions",
            data=datapoints,
            chart_type=ChartType.HEATMAP
        )
        
        return ChartConfiguration(
            title="Practice Session Heatmap",
            datasets=[dataset],
            chart_type=ChartType.HEATMAP,
            x_axis_label="Date",
            y_axis_label="Sessions",
            options={
                "responsive": True,
                "plugins": {
                    "legend": {"display": False}
                }
            },
            export_data={"daily_sessions": dict(daily_counts)}
        )
    
    def generate_comparative_chart(
        self, 
        user_id: str,
        comparison_user_ids: Optional[List[str]] = None
    ) -> ChartConfiguration:
        """Generate comparative performance chart."""
        # Get user performance
        user_performance = self.analytics.get_user_performance_metrics(user_id, days_back=30)
        
        # If no specific users provided, use similar skill level users
        if not comparison_user_ids:
            user = self.db.query(User).filter(User.id == user_id).first()
            if user and user.skill_level:
                similar_users = self.db.query(User).filter(
                    User.skill_level == user.skill_level,
                    User.id != user_id
                ).limit(10).all()
                comparison_user_ids = [u.id for u in similar_users]
        
        if not comparison_user_ids:
            return self._empty_chart("Performance Comparison", "No comparison data")
        
        # Get comparison data
        comparison_data = []
        for comp_user_id in comparison_user_ids:
            comp_performance = self.analytics.get_user_performance_metrics(comp_user_id, days_back=30)
            if comp_performance.average_score:
                comparison_data.append(comp_performance.average_score)
        
        if not comparison_data:
            return self._empty_chart("Performance Comparison", "No comparison data available")
        
        # Create comparison chart
        avg_comparison = statistics.mean(comparison_data)
        user_score = user_performance.average_score or 0
        
        datapoints = [
            ChartDatapoint(x="You", y=user_score, label=f"Your Score: {user_score:.1f}", color="#3B82F6"),
            ChartDatapoint(x="Similar Players", y=avg_comparison, label=f"Average: {avg_comparison:.1f}", color="#EF4444")
        ]
        
        dataset = ChartDataset(
            label="Performance Comparison",
            data=datapoints,
            chart_type=ChartType.BAR,
            color=["#3B82F6", "#EF4444"]
        )
        
        return ChartConfiguration(
            title="Performance vs Similar Players",
            datasets=[dataset],
            chart_type=ChartType.BAR,
            x_axis_label="",
            y_axis_label="Average Score",
            options={
                "responsive": True,
                "plugins": {
                    "legend": {"display": False}
                }
            },
            export_data={
                "user_score": user_score,
                "comparison_average": avg_comparison,
                "comparison_count": len(comparison_data)
            }
        )
    
    def generate_dashboard_widgets(self, user_id: str) -> List[DashboardWidget]:
        """Generate complete dashboard widgets."""
        widgets = []
        
        # Performance metrics widget
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=30)
        widgets.append(DashboardWidget(
            id="performance_metrics",
            title="Performance Overview",
            type="metric",
            size="medium",
            data={
                "average_score": performance.average_score,
                "best_score": performance.best_score,
                "sessions_count": performance.sessions_count,
                "consistency_score": performance.consistency_score,
                "improvement_rate": performance.improvement_rate
            },
            last_updated=datetime.now(timezone.utc)
        ))
        
        # Score trend chart widget
        score_chart = self.generate_score_trend_chart(user_id, days_back=30)
        widgets.append(DashboardWidget(
            id="score_trend",
            title="Score Trend",
            type="chart",
            size="large",
            data=asdict(score_chart),
            last_updated=datetime.now(timezone.utc)
        ))
        
        # Fault frequency widget
        fault_chart = self.generate_fault_frequency_chart(user_id, days_back=30)
        widgets.append(DashboardWidget(
            id="fault_frequency",
            title="Common Faults",
            type="chart",
            size="medium",
            data=asdict(fault_chart),
            last_updated=datetime.now(timezone.utc)
        ))
        
        # Goals progress widget
        goals = self.progress_tracker.get_user_goals(user_id, include_progress=True)
        active_goals = [g for g in goals if g["status"] == "active"]
        widgets.append(DashboardWidget(
            id="goals_progress",
            title="Goal Progress",
            type="progress",
            size="medium",
            data={
                "active_goals": len(active_goals),
                "goals": active_goals[:3]  # Top 3 goals
            },
            last_updated=datetime.now(timezone.utc)
        ))
        
        # Recent insights widget
        insights = self.insights.generate_comprehensive_insights(user_id, days_back=7)
        high_priority_insights = [i for i in insights if i.priority.value in ["high", "critical"]]
        widgets.append(DashboardWidget(
            id="recent_insights",
            title="Key Insights",
            type="table",
            size="large",
            data={
                "insights": [asdict(insight) for insight in high_priority_insights[:5]]
            },
            last_updated=datetime.now(timezone.utc)
        ))
        
        # Practice heatmap widget
        heatmap_chart = self.generate_session_heatmap(user_id, days_back=30)
        widgets.append(DashboardWidget(
            id="practice_heatmap",
            title="Practice Frequency",
            type="chart",
            size="full",
            data=asdict(heatmap_chart),
            last_updated=datetime.now(timezone.utc)
        ))
        
        return widgets
    
    def export_analytics_data(
        self, 
        user_id: str, 
        format_type: str = "json",
        days_back: int = 90
    ) -> Dict[str, Any]:
        """Export comprehensive analytics data for sharing."""
        # Get all relevant data
        performance = self.analytics.get_user_performance_metrics(user_id, days_back)
        score_trend = self.analytics.analyze_score_trend(user_id, days_back)
        fault_patterns = self.analytics.analyze_fault_patterns(user_id, days_back)
        kpi_analyses = self.analytics.analyze_kpi_performance(user_id, days_back)
        goals = self.progress_tracker.get_user_goals(user_id, include_progress=True)
        insights = self.insights.generate_comprehensive_insights(user_id, days_back)
        
        # Get user info
        user = self.db.query(User).filter(User.id == user_id).first()
        
        export_data = {
            "export_info": {
                "user_id": user_id,
                "user_name": f"{user.first_name} {user.last_name}" if user and user.first_name else "User",
                "export_date": datetime.now(timezone.utc).isoformat(),
                "period_days": days_back,
                "format": format_type
            },
            "performance_summary": {
                "average_score": performance.average_score,
                "best_score": performance.best_score,
                "worst_score": performance.worst_score,
                "sessions_count": performance.sessions_count,
                "consistency_score": performance.consistency_score,
                "improvement_rate": performance.improvement_rate,
                "active_days": performance.active_days
            },
            "trend_analysis": {
                "direction": score_trend.direction.value,
                "magnitude": score_trend.magnitude,
                "confidence": score_trend.confidence,
                "statistical_significance": score_trend.statistical_significance,
                "change_percentage": score_trend.change_percentage
            },
            "fault_patterns": [
                {
                    "fault_name": fp.fault_name,
                    "frequency": fp.frequency,
                    "frequency_percentage": fp.frequency_percentage,
                    "average_severity": fp.average_severity,
                    "trend": fp.trend.value,
                    "improvement_needed": fp.improvement_needed
                } for fp in fault_patterns
            ],
            "kpi_analysis": [
                {
                    "kpi_name": ka.kpi_name,
                    "p_position": ka.p_position,
                    "average_value": ka.average_value,
                    "trend": ka.trend.value,
                    "correlation_with_score": ka.correlation_with_score,
                    "deviation_frequency": ka.deviation_frequency
                } for ka in kpi_analyses
            ],
            "goals": [
                {
                    "title": goal["title"],
                    "goal_type": goal["goal_type"],
                    "status": goal["status"],
                    "progress_percentage": goal["progress_percentage"],
                    "target_date": goal["target_date"]
                } for goal in goals
            ],
            "insights": [
                {
                    "type": insight.type.value,
                    "priority": insight.priority.value,
                    "title": insight.title,
                    "description": insight.description,
                    "recommendation": insight.recommendation,
                    "confidence": insight.confidence,
                    "timeframe": insight.timeframe
                } for insight in insights
            ],
            "charts": {
                "score_trend": asdict(self.generate_score_trend_chart(user_id, days_back)),
                "fault_frequency": asdict(self.generate_fault_frequency_chart(user_id, days_back)),
                "progress_chart": asdict(self.generate_progress_chart(user_id))
            }
        }
        
        return export_data
    
    # Private helper methods
    
    def _empty_chart(self, title: str, message: str) -> ChartConfiguration:
        """Create empty chart configuration."""
        return ChartConfiguration(
            title=title,
            datasets=[],
            chart_type=ChartType.LINE,
            x_axis_label="",
            y_axis_label="",
            options={"plugins": {"title": {"display": True, "text": message}}},
            export_data={"message": message}
        )
    
    def _aggregate_by_interval(
        self, 
        data: List[Tuple[datetime, float]], 
        interval: TimeInterval
    ) -> List[Tuple[str, float]]:
        """Aggregate data by time interval."""
        aggregated = defaultdict(list)
        
        for date, value in data:
            if interval == TimeInterval.DAILY:
                key = date.date().isoformat()
            elif interval == TimeInterval.WEEKLY:
                # Get Monday of the week
                monday = date - timedelta(days=date.weekday())
                key = monday.date().isoformat()
            elif interval == TimeInterval.MONTHLY:
                key = f"{date.year}-{date.month:02d}"
            else:
                key = date.isoformat()
            
            aggregated[key].append(value)
        
        # Calculate averages
        result = []
        for key in sorted(aggregated.keys()):
            avg_value = statistics.mean(aggregated[key])
            result.append((key, avg_value))
        
        return result
    
    def _calculate_trend_line(self, values: List[float]) -> List[float]:
        """Calculate simple linear trend line."""
        if len(values) < 2:
            return values
        
        n = len(values)
        x_values = list(range(n))
        
        # Calculate linear regression
        sum_x = sum(x_values)
        sum_y = sum(values)
        sum_xy = sum(x * y for x, y in zip(x_values, values))
        sum_x_squared = sum(x * x for x in x_values)
        
        # Calculate slope and intercept
        denominator = n * sum_x_squared - sum_x * sum_x
        if denominator == 0:
            return values
        
        slope = (n * sum_xy - sum_x * sum_y) / denominator
        intercept = (sum_y - slope * sum_x) / n
        
        # Generate trend line values
        return [intercept + slope * x for x in x_values]
    
    def _generate_colors(self, count: int) -> List[str]:
        """Generate color palette for charts."""
        colors = [
            "#3B82F6",  # Blue
            "#EF4444",  # Red
            "#10B981",  # Green
            "#F59E0B",  # Yellow
            "#8B5CF6",  # Purple
            "#F97316",  # Orange
            "#06B6D4",  # Cyan
            "#84CC16",  # Lime
            "#EC4899",  # Pink
            "#6B7280"   # Gray
        ]
        
        # Repeat colors if needed
        while len(colors) < count:
            colors.extend(colors)
        
        return colors[:count]