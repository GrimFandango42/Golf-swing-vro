"""
Core analytics and statistical analysis functions for SwingSync AI.

This module provides:
- Statistical analysis of swing data and KPIs
- Trend analysis and improvement tracking
- Performance metrics calculation
- Fault pattern analysis
- Historical data processing
- Coaching effectiveness metrics
- Data aggregation and summarization

Key Features:
- Time-series analysis for improvement trends
- Statistical correlation between KPIs and scores
- Fault frequency and severity analysis
- Session comparison and benchmarking
- Progress tracking with statistical significance
- Anomaly detection in swing patterns
- Predictive analytics for improvement forecasting
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional, Tuple
from sqlalchemy.orm import Session
from sqlalchemy import func, and_, or_, desc, asc
from dataclasses import dataclass
from enum import Enum
import statistics
from collections import defaultdict, Counter

from database import (
    SwingSession, SwingAnalysisResult, BiomechanicalKPI, 
    DetectedFault, User, UserPreferences, SessionStatus,
    FaultSeverity
)

class TrendDirection(Enum):
    """Trend direction enumeration."""
    IMPROVING = "improving"
    DECLINING = "declining"
    STABLE = "stable"
    INSUFFICIENT_DATA = "insufficient_data"

class MetricType(Enum):
    """Analytics metric types."""
    SCORE = "overall_score"
    KPI = "kpi_value"
    FAULT_COUNT = "fault_count"
    FAULT_SEVERITY = "fault_severity"
    SESSION_FREQUENCY = "session_frequency"

@dataclass
class TrendAnalysis:
    """Result of trend analysis."""
    direction: TrendDirection
    magnitude: float  # 0-1 scale indicating strength of trend
    confidence: float  # Statistical confidence in trend
    period_days: int
    start_value: Optional[float]
    end_value: Optional[float]
    change_percentage: Optional[float]
    statistical_significance: bool

@dataclass
class PerformanceMetrics:
    """Performance metrics summary."""
    average_score: Optional[float]
    best_score: Optional[float]
    worst_score: Optional[float]
    score_variance: float
    improvement_rate: float  # Points per session
    consistency_score: float  # 0-1, higher is more consistent
    sessions_count: int
    active_days: int

@dataclass
class FaultPattern:
    """Fault pattern analysis result."""
    fault_name: str
    frequency: int
    frequency_percentage: float
    average_severity: float
    trend: TrendDirection
    sessions_affected: int
    improvement_needed: bool
    related_kpis: List[str]

@dataclass
class KPIAnalysis:
    """KPI analysis result."""
    kpi_name: str
    p_position: str
    average_value: float
    best_value: float
    worst_value: float
    variance: float
    trend: TrendDirection
    sessions_count: int
    correlation_with_score: float
    deviation_frequency: float  # How often it's outside ideal range

class AnalyticsEngine:
    """Core analytics engine for swing data analysis."""
    
    def __init__(self, db_session: Session):
        self.db = db_session
        
    def get_user_performance_metrics(
        self, 
        user_id: str, 
        days_back: int = 90
    ) -> PerformanceMetrics:
        """Calculate comprehensive performance metrics for a user."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get completed sessions
        sessions = self.db.query(SwingSession).join(SwingAnalysisResult).filter(
            SwingSession.user_id == user_id,
            SwingSession.session_status == SessionStatus.COMPLETED,
            SwingSession.created_at >= start_date
        ).order_by(SwingSession.created_at).all()
        
        if not sessions:
            return PerformanceMetrics(
                average_score=None, best_score=None, worst_score=None,
                score_variance=0.0, improvement_rate=0.0, consistency_score=0.0,
                sessions_count=0, active_days=0
            )
        
        # Extract scores
        scores = []
        dates = []
        for session in sessions:
            if session.analysis_results and session.analysis_results.overall_score:
                scores.append(session.analysis_results.overall_score)
                dates.append(session.created_at)
        
        if not scores:
            return PerformanceMetrics(
                average_score=None, best_score=None, worst_score=None,
                score_variance=0.0, improvement_rate=0.0, consistency_score=0.0,
                sessions_count=len(sessions), active_days=0
            )
        
        # Calculate basic statistics
        avg_score = statistics.mean(scores)
        best_score = max(scores)
        worst_score = min(scores)
        score_variance = statistics.variance(scores) if len(scores) > 1 else 0.0
        
        # Calculate improvement rate (linear regression slope)
        improvement_rate = self._calculate_improvement_rate(scores, dates)
        
        # Calculate consistency score (inverse of coefficient of variation)
        consistency_score = self._calculate_consistency_score(scores)
        
        # Calculate active days
        unique_dates = set(date.date() for date in dates)
        active_days = len(unique_dates)
        
        return PerformanceMetrics(
            average_score=avg_score,
            best_score=best_score,
            worst_score=worst_score,
            score_variance=score_variance,
            improvement_rate=improvement_rate,
            consistency_score=consistency_score,
            sessions_count=len(sessions),
            active_days=active_days
        )
    
    def analyze_score_trend(
        self, 
        user_id: str, 
        days_back: int = 30
    ) -> TrendAnalysis:
        """Analyze score improvement trend over time."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get scores with dates
        results = self.db.query(
            SwingAnalysisResult.overall_score,
            SwingSession.created_at
        ).join(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.session_status == SessionStatus.COMPLETED,
            SwingSession.created_at >= start_date,
            SwingAnalysisResult.overall_score.isnot(None)
        ).order_by(SwingSession.created_at).all()
        
        if len(results) < 3:
            return TrendAnalysis(
                direction=TrendDirection.INSUFFICIENT_DATA,
                magnitude=0.0, confidence=0.0, period_days=days_back,
                start_value=None, end_value=None, change_percentage=None,
                statistical_significance=False
            )
        
        scores = [r.overall_score for r in results]
        dates = [r.created_at for r in results]
        
        return self._analyze_trend(scores, dates, days_back)
    
    def analyze_fault_patterns(
        self, 
        user_id: str, 
        days_back: int = 90
    ) -> List[FaultPattern]:
        """Analyze patterns in detected faults."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Get all faults for the user
        faults = self.db.query(DetectedFault).join(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= start_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).all()
        
        if not faults:
            return []
        
        # Group faults by name
        fault_groups = defaultdict(list)
        for fault in faults:
            fault_groups[fault.fault_name].append(fault)
        
        total_sessions = self.db.query(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= start_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).count()
        
        patterns = []
        for fault_name, fault_list in fault_groups.items():
            frequency = len(fault_list)
            frequency_percentage = (frequency / total_sessions) * 100 if total_sessions > 0 else 0
            
            # Calculate average severity
            severities = [self._severity_to_numeric(f.severity) for f in fault_list]
            avg_severity = statistics.mean(severities) if severities else 0
            
            # Analyze trend (simplified - could be more sophisticated)
            recent_faults = [f for f in fault_list if 
                           f.created_at >= datetime.now(timezone.utc) - timedelta(days=14)]
            older_faults = [f for f in fault_list if 
                          f.created_at < datetime.now(timezone.utc) - timedelta(days=14)]
            
            trend = self._determine_fault_trend(len(recent_faults), len(older_faults))
            
            # Get related KPIs
            related_kpis = set()
            for fault in fault_list:
                if fault.kpi_deviations:
                    for kpi_dev in fault.kpi_deviations:
                        if isinstance(kpi_dev, dict) and 'kpi_name' in kpi_dev:
                            related_kpis.add(kpi_dev['kpi_name'])
            
            # Count unique sessions affected
            sessions_affected = len(set(f.session_id for f in fault_list))
            
            patterns.append(FaultPattern(
                fault_name=fault_name,
                frequency=frequency,
                frequency_percentage=frequency_percentage,
                average_severity=avg_severity,
                trend=trend,
                sessions_affected=sessions_affected,
                improvement_needed=frequency_percentage > 20 or avg_severity > 0.6,
                related_kpis=list(related_kpis)
            ))
        
        # Sort by frequency descending
        patterns.sort(key=lambda x: x.frequency, reverse=True)
        return patterns
    
    def analyze_kpi_performance(
        self, 
        user_id: str, 
        days_back: int = 90,
        kpi_name: Optional[str] = None
    ) -> List[KPIAnalysis]:
        """Analyze KPI performance over time."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        # Base query
        query = self.db.query(BiomechanicalKPI).join(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= start_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        )
        
        if kpi_name:
            query = query.filter(BiomechanicalKPI.kpi_name == kpi_name)
        
        kpis = query.all()
        
        if not kpis:
            return []
        
        # Group by KPI name and P-position
        kpi_groups = defaultdict(list)
        for kpi in kpis:
            key = f"{kpi.kpi_name}_{kpi.p_position}"
            kpi_groups[key].append(kpi)
        
        analyses = []
        for group_key, kpi_list in kpi_groups.items():
            kpi_name, p_position = group_key.rsplit('_', 1)
            
            values = [kpi.value for kpi in kpi_list if kpi.value is not None]
            if not values:
                continue
            
            # Basic statistics
            avg_value = statistics.mean(values)
            best_value = max(values)
            worst_value = min(values)
            variance = statistics.variance(values) if len(values) > 1 else 0
            
            # Trend analysis
            dates = [kpi.created_at for kpi in kpi_list]
            trend_analysis = self._analyze_trend(values, dates, days_back)
            
            # Correlation with overall scores
            correlation = self._calculate_kpi_score_correlation(user_id, kpi_name, p_position)
            
            # Deviation frequency
            deviations = sum(1 for kpi in kpi_list 
                           if kpi.deviation_from_ideal and abs(kpi.deviation_from_ideal) > 0.1)
            deviation_frequency = deviations / len(kpi_list) if kpi_list else 0
            
            analyses.append(KPIAnalysis(
                kpi_name=kpi_name,
                p_position=p_position,
                average_value=avg_value,
                best_value=best_value,
                worst_value=worst_value,
                variance=variance,
                trend=trend_analysis.direction,
                sessions_count=len(kpi_list),
                correlation_with_score=correlation,
                deviation_frequency=deviation_frequency
            ))
        
        return analyses
    
    def get_improvement_insights(
        self, 
        user_id: str, 
        days_back: int = 30
    ) -> Dict[str, Any]:
        """Generate comprehensive improvement insights."""
        performance = self.get_user_performance_metrics(user_id, days_back)
        score_trend = self.analyze_score_trend(user_id, days_back)
        fault_patterns = self.analyze_fault_patterns(user_id, days_back)
        kpi_analyses = self.analyze_kpi_performance(user_id, days_back)
        
        # Identify priority areas for improvement
        priority_faults = [fp for fp in fault_patterns if fp.improvement_needed][:3]
        problematic_kpis = [ka for ka in kpi_analyses if ka.deviation_frequency > 0.3][:3]
        
        # Calculate overall improvement score
        improvement_score = self._calculate_improvement_score(
            score_trend, performance, fault_patterns
        )
        
        return {
            "period_days": days_back,
            "improvement_score": improvement_score,
            "performance_metrics": performance,
            "score_trend": score_trend,
            "priority_areas": {
                "faults": priority_faults,
                "kpis": problematic_kpis
            },
            "fault_summary": {
                "total_fault_types": len(fault_patterns),
                "most_frequent": fault_patterns[0] if fault_patterns else None,
                "improving_faults": [fp for fp in fault_patterns if fp.trend == TrendDirection.IMPROVING],
                "declining_faults": [fp for fp in fault_patterns if fp.trend == TrendDirection.DECLINING]
            },
            "kpi_summary": {
                "total_kpis_tracked": len(kpi_analyses),
                "improving_kpis": [ka for ka in kpi_analyses if ka.trend == TrendDirection.IMPROVING],
                "declining_kpis": [ka for ka in kpi_analyses if ka.trend == TrendDirection.DECLINING]
            }
        }
    
    def compare_sessions(
        self, 
        session_id1: str, 
        session_id2: str
    ) -> Dict[str, Any]:
        """Compare two sessions in detail."""
        session1 = self.db.query(SwingSession).filter(SwingSession.id == session_id1).first()
        session2 = self.db.query(SwingSession).filter(SwingSession.id == session_id2).first()
        
        if not session1 or not session2:
            raise ValueError("One or both sessions not found")
        
        # Compare overall scores
        score1 = session1.analysis_results.overall_score if session1.analysis_results else None
        score2 = session2.analysis_results.overall_score if session2.analysis_results else None
        
        score_comparison = {
            "session1_score": score1,
            "session2_score": score2,
            "difference": (score2 - score1) if score1 and score2 else None,
            "improvement": score2 > score1 if score1 and score2 else None
        }
        
        # Compare KPIs
        kpi_comparison = self._compare_session_kpis(session1, session2)
        
        # Compare faults
        fault_comparison = self._compare_session_faults(session1, session2)
        
        return {
            "session1": {
                "id": session1.id,
                "created_at": session1.created_at,
                "club_used": session1.club_used
            },
            "session2": {
                "id": session2.id,
                "created_at": session2.created_at,
                "club_used": session2.club_used
            },
            "score_comparison": score_comparison,
            "kpi_comparison": kpi_comparison,
            "fault_comparison": fault_comparison
        }
    
    def calculate_coaching_effectiveness(
        self, 
        user_id: str, 
        days_back: int = 90
    ) -> Dict[str, Any]:
        """Calculate metrics for coaching effectiveness."""
        start_date = datetime.now(timezone.utc) - timedelta(days=days_back)
        
        sessions = self.db.query(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= start_date,
            SwingSession.session_status == SessionStatus.COMPLETED
        ).order_by(SwingSession.created_at).all()
        
        if len(sessions) < 2:
            return {"insufficient_data": True}
        
        # Track improvement over time
        scores = []
        fault_counts = []
        
        for session in sessions:
            if session.analysis_results:
                scores.append(session.analysis_results.overall_score or 0)
            fault_counts.append(len(session.detected_faults))
        
        # Calculate effectiveness metrics
        score_improvement = self._calculate_improvement_rate(scores, [s.created_at for s in sessions])
        fault_reduction_rate = self._calculate_improvement_rate(
            [-count for count in fault_counts],  # Negative because fewer faults is better
            [s.created_at for s in sessions]
        )
        
        # Engagement metrics
        session_frequency = len(sessions) / days_back * 7  # Sessions per week
        
        # Learning velocity (how quickly user improves)
        learning_velocity = self._calculate_learning_velocity(scores)
        
        return {
            "period_days": days_back,
            "total_sessions": len(sessions),
            "score_improvement_rate": score_improvement,
            "fault_reduction_rate": fault_reduction_rate,
            "session_frequency": session_frequency,
            "learning_velocity": learning_velocity,
            "effectiveness_score": self._calculate_coaching_effectiveness_score(
                score_improvement, fault_reduction_rate, session_frequency
            )
        }
    
    # Private helper methods
    
    def _calculate_improvement_rate(self, values: List[float], dates: List[datetime]) -> float:
        """Calculate improvement rate using linear regression."""
        if len(values) < 2:
            return 0.0
        
        # Convert dates to numeric values (days since first session)
        first_date = min(dates)
        x_values = [(date - first_date).days for date in dates]
        
        # Simple linear regression
        n = len(values)
        sum_x = sum(x_values)
        sum_y = sum(values)
        sum_xy = sum(x * y for x, y in zip(x_values, values))
        sum_x_squared = sum(x * x for x in x_values)
        
        # Calculate slope (improvement rate)
        denominator = n * sum_x_squared - sum_x * sum_x
        if denominator == 0:
            return 0.0
        
        slope = (n * sum_xy - sum_x * sum_y) / denominator
        return slope
    
    def _calculate_consistency_score(self, scores: List[float]) -> float:
        """Calculate consistency score (0-1, higher is more consistent)."""
        if len(scores) < 2:
            return 1.0
        
        mean_score = statistics.mean(scores)
        if mean_score == 0:
            return 0.0
        
        coefficient_of_variation = statistics.stdev(scores) / mean_score
        # Convert to 0-1 scale where 1 is most consistent
        consistency = max(0, 1 - coefficient_of_variation)
        return min(1.0, consistency)
    
    def _analyze_trend(
        self, 
        values: List[float], 
        dates: List[datetime], 
        period_days: int
    ) -> TrendAnalysis:
        """Analyze trend in values over time."""
        if len(values) < 3:
            return TrendAnalysis(
                direction=TrendDirection.INSUFFICIENT_DATA,
                magnitude=0.0, confidence=0.0, period_days=period_days,
                start_value=None, end_value=None, change_percentage=None,
                statistical_significance=False
            )
        
        # Calculate improvement rate
        improvement_rate = self._calculate_improvement_rate(values, dates)
        
        # Determine direction
        if abs(improvement_rate) < 0.1:  # Threshold for stable
            direction = TrendDirection.STABLE
        elif improvement_rate > 0:
            direction = TrendDirection.IMPROVING
        else:
            direction = TrendDirection.DECLINING
        
        # Calculate magnitude (normalized)
        magnitude = min(1.0, abs(improvement_rate) / 5.0)  # Normalize by assuming max 5 points/day improvement
        
        # Simple confidence calculation (based on data points and variance)
        confidence = min(1.0, len(values) / 10.0)  # More data points = higher confidence
        
        # Calculate change percentage
        start_value = values[0]
        end_value = values[-1]
        change_percentage = ((end_value - start_value) / start_value * 100) if start_value != 0 else 0
        
        # Statistical significance (simplified)
        statistical_significance = len(values) >= 5 and magnitude > 0.3
        
        return TrendAnalysis(
            direction=direction,
            magnitude=magnitude,
            confidence=confidence,
            period_days=period_days,
            start_value=start_value,
            end_value=end_value,
            change_percentage=change_percentage,
            statistical_significance=statistical_significance
        )
    
    def _severity_to_numeric(self, severity: FaultSeverity) -> float:
        """Convert fault severity to numeric value."""
        severity_map = {
            FaultSeverity.LOW: 0.25,
            FaultSeverity.MEDIUM: 0.5,
            FaultSeverity.HIGH: 0.75,
            FaultSeverity.CRITICAL: 1.0
        }
        return severity_map.get(severity, 0.5)
    
    def _determine_fault_trend(self, recent_count: int, older_count: int) -> TrendDirection:
        """Determine fault trend based on recent vs older occurrences."""
        if recent_count == 0 and older_count == 0:
            return TrendDirection.STABLE
        
        if older_count == 0:
            return TrendDirection.DECLINING  # New fault appeared
        
        ratio = recent_count / older_count
        if ratio < 0.7:
            return TrendDirection.IMPROVING  # Fault is decreasing
        elif ratio > 1.3:
            return TrendDirection.DECLINING  # Fault is increasing
        else:
            return TrendDirection.STABLE
    
    def _calculate_kpi_score_correlation(
        self, 
        user_id: str, 
        kpi_name: str, 
        p_position: str
    ) -> float:
        """Calculate correlation between KPI values and overall scores."""
        # Get KPI values and corresponding scores
        results = self.db.query(
            BiomechanicalKPI.value,
            SwingAnalysisResult.overall_score
        ).join(SwingSession).join(SwingAnalysisResult).filter(
            SwingSession.user_id == user_id,
            BiomechanicalKPI.kpi_name == kpi_name,
            BiomechanicalKPI.p_position == p_position,
            BiomechanicalKPI.value.isnot(None),
            SwingAnalysisResult.overall_score.isnot(None)
        ).all()
        
        if len(results) < 3:
            return 0.0
        
        kpi_values = [r.value for r in results]
        scores = [r.overall_score for r in results]
        
        # Calculate Pearson correlation coefficient
        return self._pearson_correlation(kpi_values, scores)
    
    def _pearson_correlation(self, x: List[float], y: List[float]) -> float:
        """Calculate Pearson correlation coefficient."""
        if len(x) != len(y) or len(x) < 2:
            return 0.0
        
        n = len(x)
        sum_x = sum(x)
        sum_y = sum(y)
        sum_xy = sum(xi * yi for xi, yi in zip(x, y))
        sum_x_squared = sum(xi * xi for xi in x)
        sum_y_squared = sum(yi * yi for yi in y)
        
        numerator = n * sum_xy - sum_x * sum_y
        denominator = ((n * sum_x_squared - sum_x * sum_x) * 
                      (n * sum_y_squared - sum_y * sum_y)) ** 0.5
        
        if denominator == 0:
            return 0.0
        
        correlation = numerator / denominator
        return max(-1.0, min(1.0, correlation))  # Clamp to [-1, 1]
    
    def _calculate_improvement_score(
        self, 
        score_trend: TrendAnalysis, 
        performance: PerformanceMetrics, 
        fault_patterns: List[FaultPattern]
    ) -> float:
        """Calculate overall improvement score (0-100)."""
        base_score = 50  # Start at neutral
        
        # Score trend contribution (40% weight)
        if score_trend.direction == TrendDirection.IMPROVING:
            base_score += 20 * score_trend.magnitude
        elif score_trend.direction == TrendDirection.DECLINING:
            base_score -= 20 * score_trend.magnitude
        
        # Consistency contribution (20% weight)
        base_score += 10 * performance.consistency_score
        
        # Fault improvement contribution (40% weight)
        improving_faults = sum(1 for fp in fault_patterns if fp.trend == TrendDirection.IMPROVING)
        declining_faults = sum(1 for fp in fault_patterns if fp.trend == TrendDirection.DECLINING)
        total_faults = len(fault_patterns)
        
        if total_faults > 0:
            fault_improvement_ratio = (improving_faults - declining_faults) / total_faults
            base_score += 20 * fault_improvement_ratio
        
        return max(0, min(100, base_score))
    
    def _compare_session_kpis(self, session1: SwingSession, session2: SwingSession) -> Dict[str, Any]:
        """Compare KPIs between two sessions."""
        kpis1 = {f"{kpi.kpi_name}_{kpi.p_position}": kpi.value for kpi in session1.biomechanical_kpis}
        kpis2 = {f"{kpi.kpi_name}_{kpi.p_position}": kpi.value for kpi in session2.biomechanical_kpis}
        
        common_kpis = set(kpis1.keys()) & set(kpis2.keys())
        
        improvements = []
        declines = []
        
        for kpi_key in common_kpis:
            value1 = kpis1[kpi_key]
            value2 = kpis2[kpi_key]
            if value1 and value2:
                change = value2 - value1
                change_percentage = (change / value1 * 100) if value1 != 0 else 0
                
                kpi_comparison = {
                    "kpi": kpi_key,
                    "session1_value": value1,
                    "session2_value": value2,
                    "change": change,
                    "change_percentage": change_percentage
                }
                
                if change > 0:
                    improvements.append(kpi_comparison)
                elif change < 0:
                    declines.append(kpi_comparison)
        
        return {
            "total_compared": len(common_kpis),
            "improvements": sorted(improvements, key=lambda x: abs(x["change_percentage"]), reverse=True),
            "declines": sorted(declines, key=lambda x: abs(x["change_percentage"]), reverse=True)
        }
    
    def _compare_session_faults(self, session1: SwingSession, session2: SwingSession) -> Dict[str, Any]:
        """Compare faults between two sessions."""
        faults1 = set(fault.fault_name for fault in session1.detected_faults)
        faults2 = set(fault.fault_name for fault in session2.detected_faults)
        
        new_faults = faults2 - faults1
        resolved_faults = faults1 - faults2
        persistent_faults = faults1 & faults2
        
        return {
            "session1_fault_count": len(faults1),
            "session2_fault_count": len(faults2),
            "new_faults": list(new_faults),
            "resolved_faults": list(resolved_faults),
            "persistent_faults": list(persistent_faults),
            "improvement": len(resolved_faults) > len(new_faults)
        }
    
    def _calculate_learning_velocity(self, scores: List[float]) -> float:
        """Calculate how quickly the user is learning (improvement acceleration)."""
        if len(scores) < 4:
            return 0.0
        
        # Calculate improvement rates for different periods
        early_scores = scores[:len(scores)//2]
        recent_scores = scores[len(scores)//2:]
        
        early_improvement = self._calculate_improvement_rate(
            early_scores, list(range(len(early_scores)))
        )
        recent_improvement = self._calculate_improvement_rate(
            recent_scores, list(range(len(recent_scores)))
        )
        
        # Learning velocity is the change in improvement rate
        return recent_improvement - early_improvement
    
    def _calculate_coaching_effectiveness_score(
        self, 
        score_improvement: float, 
        fault_reduction: float, 
        session_frequency: float
    ) -> float:
        """Calculate overall coaching effectiveness score (0-100)."""
        # Normalize components
        score_component = min(50, max(-50, score_improvement * 10))  # Score improvement
        fault_component = min(25, max(-25, fault_reduction * 5))      # Fault reduction
        engagement_component = min(25, session_frequency * 5)        # Session frequency
        
        effectiveness = 50 + score_component + fault_component + engagement_component
        return max(0, min(100, effectiveness))