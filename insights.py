"""
AI-powered insights and recommendations for SwingSync AI.

This module provides:
- Intelligent analysis of swing patterns and trends
- Personalized improvement recommendations
- AI-driven coaching insights
- Predictive analytics for performance forecasting
- Comparative analysis with similar players
- Automated insight generation
- Smart recommendations for training focus

Key Features:
- LLM-powered insight generation using swing data
- Pattern recognition for identifying improvement opportunities
- Personalized coaching recommendations
- Performance prediction and trend analysis
- Comparative benchmarking
- Automated report generation
- Smart training program suggestions
"""

import json
import os
from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass, asdict
from enum import Enum
import statistics
from collections import defaultdict

from sqlalchemy.orm import Session
from sqlalchemy import func, desc, and_

from database import (
    User, SwingSession, SwingAnalysisResult, BiomechanicalKPI,
    DetectedFault, UserPreferences, SessionStatus
)
from analytics import AnalyticsEngine, TrendDirection, PerformanceMetrics, FaultPattern
from progress_tracking import ProgressTracker, GoalType, GoalTarget

# Import AI/LLM functionality (mock implementation for now)
try:
    import google.generativeai as genai
    GEMINI_AVAILABLE = True
except ImportError:
    GEMINI_AVAILABLE = False

class InsightType(Enum):
    """Types of insights that can be generated."""
    PERFORMANCE_SUMMARY = "performance_summary"
    IMPROVEMENT_RECOMMENDATION = "improvement_recommendation"
    TREND_ANALYSIS = "trend_analysis"
    COMPARATIVE_ANALYSIS = "comparative_analysis"
    TRAINING_FOCUS = "training_focus"
    GOAL_SUGGESTION = "goal_suggestion"
    TECHNIQUE_TIP = "technique_tip"
    PROGRESS_CELEBRATION = "progress_celebration"

class InsightPriority(Enum):
    """Priority levels for insights."""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

@dataclass
class Insight:
    """Structured insight with metadata."""
    type: InsightType
    priority: InsightPriority
    title: str
    description: str
    recommendation: str
    data_points: Dict[str, Any]
    confidence: float  # 0-1 confidence in the insight
    actionable_steps: List[str]
    timeframe: str  # Expected timeframe for seeing results
    created_at: datetime

@dataclass
class PersonalizedRecommendation:
    """Personalized training recommendation."""
    title: str
    description: str
    focus_areas: List[str]
    difficulty_level: str
    estimated_improvement: str
    time_commitment: str
    specific_drills: List[str]
    success_metrics: List[str]

@dataclass
class PerformancePrediction:
    """Predicted performance metrics."""
    timeframe_days: int
    predicted_score: float
    confidence_interval: Tuple[float, float]
    key_factors: List[str]
    risk_factors: List[str]
    opportunities: List[str]

class InsightsEngine:
    """Main engine for generating AI-powered insights and recommendations."""
    
    def __init__(self, db_session: Session):
        self.db = db_session
        self.analytics = AnalyticsEngine(db_session)
        self.progress_tracker = ProgressTracker(db_session)
        
        # Initialize Gemini AI if available
        if GEMINI_AVAILABLE and os.getenv("GEMINI_API_KEY"):
            genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
            self.model = genai.GenerativeModel('gemini-pro')
        else:
            self.model = None
    
    def generate_comprehensive_insights(
        self, 
        user_id: str, 
        days_back: int = 30
    ) -> List[Insight]:
        """Generate comprehensive insights for a user."""
        insights = []
        
        # Get user data
        user = self.db.query(User).filter(User.id == user_id).first()
        if not user:
            return insights
        
        # Get analytics data
        performance = self.analytics.get_user_performance_metrics(user_id, days_back)
        score_trend = self.analytics.analyze_score_trend(user_id, days_back)
        fault_patterns = self.analytics.analyze_fault_patterns(user_id, days_back)
        kpi_analyses = self.analytics.analyze_kpi_performance(user_id, days_back)
        
        # Generate different types of insights
        insights.extend(self._generate_performance_insights(user, performance, score_trend))
        insights.extend(self._generate_fault_insights(fault_patterns))
        insights.extend(self._generate_kpi_insights(kpi_analyses))
        insights.extend(self._generate_trend_insights(score_trend, performance))
        insights.extend(self._generate_goal_insights(user_id, performance, fault_patterns))
        insights.extend(self._generate_training_insights(user_id, performance, fault_patterns))
        
        # Sort by priority and confidence
        insights.sort(key=lambda x: (x.priority.value, -x.confidence), reverse=True)
        
        return insights[:10]  # Return top 10 insights
    
    def generate_personalized_recommendations(
        self, 
        user_id: str
    ) -> List[PersonalizedRecommendation]:
        """Generate personalized training recommendations."""
        recommendations = []
        
        # Get user data
        user = self.db.query(User).filter(User.id == user_id).first()
        preferences = self.db.query(UserPreferences).filter(UserPreferences.user_id == user_id).first()
        
        if not user:
            return recommendations
        
        # Get improvement insights
        improvement_data = self.analytics.get_improvement_insights(user_id, days_back=30)
        
        # Generate recommendations based on different aspects
        recommendations.extend(self._recommend_for_score_improvement(user, improvement_data))
        recommendations.extend(self._recommend_for_fault_reduction(improvement_data["fault_summary"]))
        recommendations.extend(self._recommend_for_consistency(improvement_data["performance_metrics"]))
        
        # Personalize based on user preferences and skill level
        if preferences:
            recommendations = self._personalize_recommendations(recommendations, user, preferences)
        
        return recommendations[:5]  # Return top 5 recommendations
    
    def predict_performance(
        self, 
        user_id: str, 
        timeframe_days: int = 30
    ) -> PerformancePrediction:
        """Predict future performance based on current trends."""
        # Get historical data
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=90)
        score_trend = self.analytics.analyze_score_trend(user_id, days_back=30)
        
        # Current average score
        current_score = performance.average_score or 60
        
        # Predict future score based on trend
        if score_trend.direction == TrendDirection.IMPROVING:
            improvement_rate = performance.improvement_rate
            predicted_improvement = improvement_rate * timeframe_days
            predicted_score = min(100, current_score + predicted_improvement)
        elif score_trend.direction == TrendDirection.DECLINING:
            decline_rate = abs(performance.improvement_rate)
            predicted_decline = decline_rate * timeframe_days
            predicted_score = max(0, current_score - predicted_decline)
        else:
            predicted_score = current_score
        
        # Calculate confidence interval (simplified)
        variance = performance.score_variance
        std_dev = variance ** 0.5 if variance > 0 else 5
        confidence_interval = (
            max(0, predicted_score - std_dev),
            min(100, predicted_score + std_dev)
        )
        
        # Identify key factors
        key_factors = self._identify_performance_factors(user_id)
        risk_factors = self._identify_risk_factors(user_id)
        opportunities = self._identify_opportunities(user_id)
        
        return PerformancePrediction(
            timeframe_days=timeframe_days,
            predicted_score=predicted_score,
            confidence_interval=confidence_interval,
            key_factors=key_factors,
            risk_factors=risk_factors,
            opportunities=opportunities
        )
    
    def generate_comparative_analysis(
        self, 
        user_id: str, 
        comparison_group: str = "skill_level"
    ) -> Dict[str, Any]:
        """Generate comparative analysis with similar users."""
        user = self.db.query(User).filter(User.id == user_id).first()
        if not user:
            return {"error": "User not found"}
        
        # Get user's performance
        user_performance = self.analytics.get_user_performance_metrics(user_id, days_back=90)
        
        # Find comparison group
        if comparison_group == "skill_level":
            comparison_users = self.db.query(User).filter(
                User.skill_level == user.skill_level,
                User.id != user_id
            ).limit(50).all()
        elif comparison_group == "handicap" and user.handicap:
            comparison_users = self.db.query(User).filter(
                User.handicap.between(user.handicap - 5, user.handicap + 5),
                User.id != user_id
            ).limit(50).all()
        else:
            comparison_users = self.db.query(User).filter(User.id != user_id).limit(100).all()
        
        if not comparison_users:
            return {"error": "No comparable users found"}
        
        # Calculate comparison metrics
        comparison_scores = []
        comparison_sessions = []
        
        for comp_user in comparison_users:
            comp_performance = self.analytics.get_user_performance_metrics(comp_user.id, days_back=90)
            if comp_performance.average_score:
                comparison_scores.append(comp_performance.average_score)
            comparison_sessions.append(comp_performance.sessions_count)
        
        if not comparison_scores:
            return {"error": "No performance data for comparison"}
        
        # Calculate percentiles
        user_score = user_performance.average_score or 0
        user_sessions = user_performance.sessions_count
        
        score_percentile = self._calculate_percentile(user_score, comparison_scores)
        session_percentile = self._calculate_percentile(user_sessions, comparison_sessions)
        
        return {
            "user_stats": {
                "average_score": user_score,
                "sessions_count": user_sessions,
                "consistency_score": user_performance.consistency_score
            },
            "comparison_group": {
                "type": comparison_group,
                "size": len(comparison_users),
                "average_score": statistics.mean(comparison_scores),
                "score_range": (min(comparison_scores), max(comparison_scores)),
                "average_sessions": statistics.mean(comparison_sessions)
            },
            "user_percentiles": {
                "score_percentile": score_percentile,
                "session_percentile": session_percentile
            },
            "relative_performance": self._get_relative_performance_description(score_percentile),
            "improvement_potential": self._calculate_improvement_potential(user_score, comparison_scores)
        }
    
    def generate_ai_coaching_insights(
        self, 
        user_id: str, 
        recent_session_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """Generate AI-powered coaching insights using LLM."""
        if not self.model:
            return {"error": "AI model not available"}
        
        # Gather data for AI analysis
        context_data = self._prepare_ai_context(user_id, recent_session_id)
        
        # Create prompt for AI analysis
        prompt = self._create_coaching_prompt(context_data)
        
        try:
            # Generate AI response
            response = self.model.generate_content(prompt)
            ai_insights = self._parse_ai_response(response.text)
            
            return {
                "ai_insights": ai_insights,
                "context_data": context_data,
                "generated_at": datetime.now(timezone.utc).isoformat()
            }
        except Exception as e:
            return {"error": f"AI generation failed: {str(e)}"}
    
    def create_insight_summary_report(
        self, 
        user_id: str, 
        days_back: int = 30
    ) -> Dict[str, Any]:
        """Create a comprehensive insight summary report."""
        # Get all insights
        insights = self.generate_comprehensive_insights(user_id, days_back)
        recommendations = self.generate_personalized_recommendations(user_id)
        prediction = self.predict_performance(user_id, timeframe_days=30)
        comparative = self.generate_comparative_analysis(user_id)
        
        # Get user info
        user = self.db.query(User).filter(User.id == user_id).first()
        
        # Create summary
        report = {
            "user_id": user_id,
            "user_name": f"{user.first_name} {user.last_name}" if user and user.first_name else "User",
            "report_period": {
                "days_back": days_back,
                "start_date": (datetime.now(timezone.utc) - timedelta(days=days_back)).isoformat(),
                "end_date": datetime.now(timezone.utc).isoformat()
            },
            "insights": {
                "total_insights": len(insights),
                "high_priority": len([i for i in insights if i.priority == InsightPriority.HIGH]),
                "critical_priority": len([i for i in insights if i.priority == InsightPriority.CRITICAL]),
                "insights_list": [asdict(insight) for insight in insights]
            },
            "recommendations": {
                "total_recommendations": len(recommendations),
                "recommendations_list": [asdict(rec) for rec in recommendations]
            },
            "performance_prediction": asdict(prediction),
            "comparative_analysis": comparative,
            "generated_at": datetime.now(timezone.utc).isoformat()
        }
        
        return report
    
    # Private helper methods
    
    def _generate_performance_insights(
        self, 
        user: User, 
        performance: PerformanceMetrics, 
        trend: TrendDirection
    ) -> List[Insight]:
        """Generate insights about overall performance."""
        insights = []
        
        if performance.average_score:
            # Performance level insight
            if performance.average_score >= 80:
                priority = InsightPriority.LOW
                title = "Excellent Performance Level"
                description = f"Your average score of {performance.average_score:.1f} indicates excellent swing technique."
                recommendation = "Focus on consistency and fine-tuning advanced techniques."
            elif performance.average_score >= 60:
                priority = InsightPriority.MEDIUM
                title = "Good Performance with Room for Improvement"
                description = f"Your average score of {performance.average_score:.1f} shows solid fundamentals."
                recommendation = "Work on eliminating common faults to reach the next level."
            else:
                priority = InsightPriority.HIGH
                title = "Significant Improvement Opportunity"
                description = f"Your average score of {performance.average_score:.1f} indicates areas for fundamental improvement."
                recommendation = "Focus on basic swing mechanics and consider professional instruction."
            
            insights.append(Insight(
                type=InsightType.PERFORMANCE_SUMMARY,
                priority=priority,
                title=title,
                description=description,
                recommendation=recommendation,
                data_points={"average_score": performance.average_score, "sessions_count": performance.sessions_count},
                confidence=0.9,
                actionable_steps=["Review recent session feedback", "Practice recommended drills", "Set specific improvement goals"],
                timeframe="2-4 weeks",
                created_at=datetime.now(timezone.utc)
            ))
        
        # Consistency insight
        if performance.consistency_score < 0.6:
            insights.append(Insight(
                type=InsightType.IMPROVEMENT_RECOMMENDATION,
                priority=InsightPriority.HIGH,
                title="Inconsistent Performance Pattern",
                description=f"Your consistency score of {performance.consistency_score:.2f} suggests variable swing execution.",
                recommendation="Focus on developing a repeatable swing through structured practice.",
                data_points={"consistency_score": performance.consistency_score, "score_variance": performance.score_variance},
                confidence=0.85,
                actionable_steps=["Practice with tempo drills", "Work on pre-shot routine", "Focus on balance and posture"],
                timeframe="4-6 weeks",
                created_at=datetime.now(timezone.utc)
            ))
        
        return insights
    
    def _generate_fault_insights(self, fault_patterns: List[FaultPattern]) -> List[Insight]:
        """Generate insights about fault patterns."""
        insights = []
        
        if not fault_patterns:
            return insights
        
        # Most frequent fault
        most_frequent = fault_patterns[0]
        if most_frequent.frequency_percentage > 30:
            insights.append(Insight(
                type=InsightType.IMPROVEMENT_RECOMMENDATION,
                priority=InsightPriority.HIGH,
                title=f"Address Recurring {most_frequent.fault_name}",
                description=f"This fault appears in {most_frequent.frequency_percentage:.1f}% of your swings.",
                recommendation=f"Prioritize correcting {most_frequent.fault_name} through targeted drills.",
                data_points={"fault_name": most_frequent.fault_name, "frequency": most_frequent.frequency_percentage},
                confidence=0.9,
                actionable_steps=["Study fault-specific drills", "Practice slow-motion corrections", "Get video feedback"],
                timeframe="3-4 weeks",
                created_at=datetime.now(timezone.utc)
            ))
        
        # Improving faults
        improving_faults = [fp for fp in fault_patterns if fp.trend == TrendDirection.IMPROVING]
        if improving_faults:
            fault_names = [f.fault_name for f in improving_faults[:3]]
            insights.append(Insight(
                type=InsightType.PROGRESS_CELEBRATION,
                priority=InsightPriority.MEDIUM,
                title="Great Progress on Fault Reduction",
                description=f"You're successfully reducing these faults: {', '.join(fault_names)}",
                recommendation="Continue current practice approach for these areas.",
                data_points={"improving_faults": fault_names},
                confidence=0.8,
                actionable_steps=["Maintain current practice routine", "Track continued improvement", "Apply same approach to other faults"],
                timeframe="Ongoing",
                created_at=datetime.now(timezone.utc)
            ))
        
        return insights
    
    def _generate_kpi_insights(self, kpi_analyses: List) -> List[Insight]:
        """Generate insights about KPI performance."""
        insights = []
        
        # Find KPIs with high deviation frequency
        problematic_kpis = [ka for ka in kpi_analyses if ka.deviation_frequency > 0.4]
        
        if problematic_kpis:
            kpi = problematic_kpis[0]  # Most problematic
            insights.append(Insight(
                type=InsightType.TECHNIQUE_TIP,
                priority=InsightPriority.MEDIUM,
                title=f"Improve {kpi.kpi_name} at {kpi.p_position}",
                description=f"This KPI deviates from ideal {kpi.deviation_frequency:.1%} of the time.",
                recommendation=f"Focus on drills that improve {kpi.kpi_name} during the {kpi.p_position} position.",
                data_points={"kpi_name": kpi.kpi_name, "p_position": kpi.p_position, "deviation_frequency": kpi.deviation_frequency},
                confidence=0.75,
                actionable_steps=["Practice position-specific drills", "Use mirror for position checks", "Get professional guidance"],
                timeframe="2-3 weeks",
                created_at=datetime.now(timezone.utc)
            ))
        
        return insights
    
    def _generate_trend_insights(self, score_trend, performance: PerformanceMetrics) -> List[Insight]:
        """Generate insights about performance trends."""
        insights = []
        
        if score_trend.direction == TrendDirection.IMPROVING and score_trend.statistical_significance:
            insights.append(Insight(
                type=InsightType.TREND_ANALYSIS,
                priority=InsightPriority.MEDIUM,
                title="Positive Performance Trend",
                description=f"Your scores are improving at {performance.improvement_rate:.2f} points per session.",
                recommendation="Maintain current practice approach as it's working well.",
                data_points={"improvement_rate": performance.improvement_rate, "trend_magnitude": score_trend.magnitude},
                confidence=score_trend.confidence,
                actionable_steps=["Continue current training", "Track progress consistently", "Set progressive goals"],
                timeframe="Ongoing",
                created_at=datetime.now(timezone.utc)
            ))
        elif score_trend.direction == TrendDirection.DECLINING:
            insights.append(Insight(
                type=InsightType.IMPROVEMENT_RECOMMENDATION,
                priority=InsightPriority.HIGH,
                title="Performance Decline Detected",
                description="Your recent scores show a declining trend that needs attention.",
                recommendation="Review recent changes in technique or practice routine.",
                data_points={"trend_direction": score_trend.direction.value, "change_percentage": score_trend.change_percentage},
                confidence=score_trend.confidence,
                actionable_steps=["Review recent sessions", "Return to proven techniques", "Consider professional consultation"],
                timeframe="1-2 weeks",
                created_at=datetime.now(timezone.utc)
            ))
        
        return insights
    
    def _generate_goal_insights(self, user_id: str, performance: PerformanceMetrics, fault_patterns: List[FaultPattern]) -> List[Insight]:
        """Generate goal-setting insights."""
        insights = []
        
        # Check if user has active goals
        active_goals = self.progress_tracker.get_user_goals(user_id, include_progress=False)
        
        if not active_goals:
            insights.append(Insight(
                type=InsightType.GOAL_SUGGESTION,
                priority=InsightPriority.MEDIUM,
                title="Set Performance Goals",
                description="Setting specific goals can accelerate your improvement.",
                recommendation="Consider setting a score improvement goal based on your current performance.",
                data_points={"current_score": performance.average_score, "has_goals": False},
                confidence=0.8,
                actionable_steps=["Review suggested goals", "Set SMART objectives", "Track progress regularly"],
                timeframe="Start immediately",
                created_at=datetime.now(timezone.utc)
            ))
        
        return insights
    
    def _generate_training_insights(self, user_id: str, performance: PerformanceMetrics, fault_patterns: List[FaultPattern]) -> List[Insight]:
        """Generate training-focused insights."""
        insights = []
        
        # Training frequency insight
        if performance.sessions_count < 8:  # Less than 2 per week over 30 days
            insights.append(Insight(
                type=InsightType.TRAINING_FOCUS,
                priority=InsightPriority.MEDIUM,
                title="Increase Practice Frequency",
                description=f"With only {performance.sessions_count} sessions in 30 days, more regular practice could accelerate improvement.",
                recommendation="Aim for at least 3 practice sessions per week for optimal progress.",
                data_points={"sessions_count": performance.sessions_count, "sessions_per_week": performance.sessions_count / 4},
                confidence=0.85,
                actionable_steps=["Schedule regular practice times", "Set practice reminders", "Start with shorter, frequent sessions"],
                timeframe="Immediate implementation",
                created_at=datetime.now(timezone.utc)
            ))
        
        return insights
    
    def _recommend_for_score_improvement(self, user: User, improvement_data: Dict[str, Any]) -> List[PersonalizedRecommendation]:
        """Generate recommendations for score improvement."""
        recommendations = []
        
        current_score = improvement_data["performance_metrics"].average_score or 60
        
        if current_score < 70:
            recommendations.append(PersonalizedRecommendation(
                title="Fundamental Swing Mechanics",
                description="Focus on building solid fundamentals to improve overall performance",
                focus_areas=["Setup and posture", "Backswing plane", "Impact position"],
                difficulty_level="Beginner",
                estimated_improvement="10-15 points in 6 weeks",
                time_commitment="3 sessions per week, 45 minutes each",
                specific_drills=["Mirror work for setup", "Slow motion swings", "Impact bag training"],
                success_metrics=["Consistent setup position", "Improved swing plane", "Better ball contact"]
            ))
        elif current_score < 85:
            recommendations.append(PersonalizedRecommendation(
                title="Consistency and Timing",
                description="Work on timing and consistency to achieve more reliable performance",
                focus_areas=["Tempo control", "Sequence timing", "Balance"],
                difficulty_level="Intermediate",
                estimated_improvement="5-10 points in 4 weeks",
                time_commitment="4 sessions per week, 30 minutes each",
                specific_drills=["Metronome training", "Balance drills", "Rhythm exercises"],
                success_metrics=["Lower score variance", "Better tempo consistency", "Improved balance"]
            ))
        
        return recommendations
    
    def _recommend_for_fault_reduction(self, fault_summary: Dict[str, Any]) -> List[PersonalizedRecommendation]:
        """Generate recommendations for reducing common faults."""
        recommendations = []
        
        if fault_summary.get("most_frequent"):
            most_frequent = fault_summary["most_frequent"]
            recommendations.append(PersonalizedRecommendation(
                title=f"Eliminate {most_frequent.fault_name}",
                description=f"Targeted approach to reduce your most common fault",
                focus_areas=[most_frequent.fault_name, "Related fundamentals"],
                difficulty_level="Intermediate",
                estimated_improvement=f"Reduce {most_frequent.fault_name} by 50% in 3 weeks",
                time_commitment="Daily practice, 15 minutes focused work",
                specific_drills=["Fault-specific corrections", "Exaggerated opposite movements", "Video feedback"],
                success_metrics=[f"Reduced {most_frequent.fault_name} frequency", "Improved technique awareness"]
            ))
        
        return recommendations
    
    def _recommend_for_consistency(self, performance: PerformanceMetrics) -> List[PersonalizedRecommendation]:
        """Generate recommendations for improving consistency."""
        recommendations = []
        
        if performance.consistency_score < 0.7:
            recommendations.append(PersonalizedRecommendation(
                title="Develop Consistent Swing Pattern",
                description="Build repeatable mechanics for more predictable results",
                focus_areas=["Pre-shot routine", "Swing tempo", "Key positions"],
                difficulty_level="Intermediate",
                estimated_improvement=f"Improve consistency from {performance.consistency_score:.2f} to 0.8+",
                time_commitment="Every practice session, focus on repetition",
                specific_drills=["Routine practice", "Tempo training", "Position checkpoints"],
                success_metrics=["Lower score variance", "More predictable ball flight", "Confident execution"]
            ))
        
        return recommendations
    
    def _personalize_recommendations(
        self, 
        recommendations: List[PersonalizedRecommendation], 
        user: User, 
        preferences: UserPreferences
    ) -> List[PersonalizedRecommendation]:
        """Personalize recommendations based on user profile and preferences."""
        # Adjust based on skill level
        skill_multiplier = {
            "beginner": 1.5,
            "intermediate": 1.0,
            "advanced": 0.8,
            "professional": 0.6
        }
        
        multiplier = skill_multiplier.get(user.skill_level.value if user.skill_level else "intermediate", 1.0)
        
        for rec in recommendations:
            # Adjust time commitments based on skill level
            if "weeks" in rec.time_commitment:
                # Extract and adjust weeks
                import re
                weeks_match = re.search(r'(\d+)\s*weeks?', rec.time_commitment)
                if weeks_match:
                    weeks = int(weeks_match.group(1))
                    adjusted_weeks = int(weeks * multiplier)
                    rec.time_commitment = rec.time_commitment.replace(
                        f"{weeks} week", f"{adjusted_weeks} week"
                    )
        
        return recommendations
    
    def _identify_performance_factors(self, user_id: str) -> List[str]:
        """Identify key factors affecting performance."""
        factors = []
        
        # Analyze session frequency
        recent_sessions = self.db.query(SwingSession).filter(
            SwingSession.user_id == user_id,
            SwingSession.created_at >= datetime.now(timezone.utc) - timedelta(days=30),
            SwingSession.session_status == SessionStatus.COMPLETED
        ).count()
        
        if recent_sessions >= 12:
            factors.append("High practice frequency")
        elif recent_sessions < 4:
            factors.append("Low practice frequency")
        
        # Analyze consistency
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=30)
        if performance.consistency_score > 0.8:
            factors.append("High consistency")
        elif performance.consistency_score < 0.6:
            factors.append("Inconsistent performance")
        
        # Analyze improvement trend
        trend = self.analytics.analyze_score_trend(user_id, days_back=30)
        if trend.direction == TrendDirection.IMPROVING:
            factors.append("Positive improvement trend")
        elif trend.direction == TrendDirection.DECLINING:
            factors.append("Recent performance decline")
        
        return factors
    
    def _identify_risk_factors(self, user_id: str) -> List[str]:
        """Identify factors that could negatively impact performance."""
        risk_factors = []
        
        # Low practice frequency
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=30)
        if performance.sessions_count < 4:
            risk_factors.append("Infrequent practice")
        
        # High fault frequency
        fault_patterns = self.analytics.analyze_fault_patterns(user_id, days_back=30)
        high_freq_faults = [fp for fp in fault_patterns if fp.frequency_percentage > 40]
        if high_freq_faults:
            risk_factors.append("Persistent swing faults")
        
        # Declining trend
        trend = self.analytics.analyze_score_trend(user_id, days_back=30)
        if trend.direction == TrendDirection.DECLINING:
            risk_factors.append("Declining performance trend")
        
        # Low consistency
        if performance.consistency_score < 0.5:
            risk_factors.append("Highly inconsistent performance")
        
        return risk_factors
    
    def _identify_opportunities(self, user_id: str) -> List[str]:
        """Identify opportunities for improvement."""
        opportunities = []
        
        # Analyze goals
        active_goals = self.progress_tracker.get_user_goals(user_id, include_progress=False)
        if not active_goals:
            opportunities.append("Set specific improvement goals")
        
        # Analyze fault patterns
        fault_patterns = self.analytics.analyze_fault_patterns(user_id, days_back=30)
        improving_faults = [fp for fp in fault_patterns if fp.trend == TrendDirection.IMPROVING]
        if improving_faults:
            opportunities.append("Build on successful fault reduction")
        
        # Analyze KPI improvements
        kpi_analyses = self.analytics.analyze_kpi_performance(user_id, days_back=30)
        improving_kpis = [ka for ka in kpi_analyses if ka.trend == TrendDirection.IMPROVING]
        if improving_kpis:
            opportunities.append("Leverage improving biomechanics")
        
        # Practice frequency opportunity
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=30)
        if performance.sessions_count < 8:
            opportunities.append("Increase practice frequency")
        
        return opportunities
    
    def _calculate_percentile(self, value: float, comparison_values: List[float]) -> float:
        """Calculate percentile ranking."""
        if not comparison_values:
            return 50.0
        
        below_value = sum(1 for v in comparison_values if v < value)
        equal_value = sum(1 for v in comparison_values if v == value)
        
        percentile = (below_value + 0.5 * equal_value) / len(comparison_values) * 100
        return round(percentile, 1)
    
    def _get_relative_performance_description(self, percentile: float) -> str:
        """Get description of relative performance."""
        if percentile >= 90:
            return "Exceptional - Top 10%"
        elif percentile >= 75:
            return "Above Average - Top 25%"
        elif percentile >= 50:
            return "Average - Above Median"
        elif percentile >= 25:
            return "Below Average - Bottom 50%"
        else:
            return "Needs Improvement - Bottom 25%"
    
    def _calculate_improvement_potential(self, user_score: float, comparison_scores: List[float]) -> str:
        """Calculate improvement potential description."""
        if not comparison_scores:
            return "Cannot determine"
        
        max_score = max(comparison_scores)
        top_10_percent = sorted(comparison_scores, reverse=True)[len(comparison_scores)//10]
        
        if user_score >= top_10_percent:
            return "Limited - Already in top tier"
        elif user_score >= max_score * 0.8:
            return "Moderate - Can reach top tier"
        else:
            potential_gain = top_10_percent - user_score
            return f"High - {potential_gain:.1f} points to top 10%"
    
    def _prepare_ai_context(self, user_id: str, recent_session_id: Optional[str]) -> Dict[str, Any]:
        """Prepare context data for AI analysis."""
        # Get user info
        user = self.db.query(User).filter(User.id == user_id).first()
        
        # Get performance data
        performance = self.analytics.get_user_performance_metrics(user_id, days_back=30)
        fault_patterns = self.analytics.analyze_fault_patterns(user_id, days_back=30)
        
        context = {
            "user_profile": {
                "skill_level": user.skill_level.value if user.skill_level else "unknown",
                "handicap": user.handicap,
                "preferred_hand": user.preferred_hand
            },
            "performance_summary": {
                "average_score": performance.average_score,
                "sessions_count": performance.sessions_count,
                "consistency_score": performance.consistency_score,
                "improvement_rate": performance.improvement_rate
            },
            "top_faults": [
                {
                    "name": fp.fault_name,
                    "frequency": fp.frequency_percentage,
                    "trend": fp.trend.value
                } for fp in fault_patterns[:3]
            ]
        }
        
        # Add recent session details if provided
        if recent_session_id:
            session = self.db.query(SwingSession).filter(SwingSession.id == recent_session_id).first()
            if session and session.analysis_results:
                context["recent_session"] = {
                    "score": session.analysis_results.overall_score,
                    "club_used": session.club_used,
                    "summary": session.analysis_results.summary_of_findings
                }
        
        return context
    
    def _create_coaching_prompt(self, context_data: Dict[str, Any]) -> str:
        """Create a prompt for AI coaching insights."""
        prompt = f"""
As a professional golf coach AI, analyze this player's data and provide personalized coaching insights:

Player Profile:
- Skill Level: {context_data['user_profile']['skill_level']}
- Handicap: {context_data['user_profile'].get('handicap', 'Unknown')}
- Preferred Hand: {context_data['user_profile'].get('preferred_hand', 'Unknown')}

Performance Summary (Last 30 days):
- Average Score: {context_data['performance_summary']['average_score']:.1f}
- Sessions Completed: {context_data['performance_summary']['sessions_count']}
- Consistency Score: {context_data['performance_summary']['consistency_score']:.2f}
- Improvement Rate: {context_data['performance_summary']['improvement_rate']:.2f} points/session

Top Swing Faults:
{chr(10).join(f"- {fault['name']}: {fault['frequency']:.1f}% frequency, trend: {fault['trend']}" for fault in context_data['top_faults'])}

Please provide:
1. Overall assessment of the player's current state
2. Top 3 priority areas for improvement
3. Specific drill recommendations
4. Expected timeline for seeing improvements
5. Motivational insights to keep the player engaged

Format your response as a structured coaching report.
"""
        return prompt
    
    def _parse_ai_response(self, ai_text: str) -> Dict[str, Any]:
        """Parse AI response into structured format."""
        # Simple parsing - in production, you might use more sophisticated NLP
        sections = ai_text.split('\n\n')
        
        parsed = {
            "overall_assessment": "",
            "priority_areas": [],
            "drill_recommendations": [],
            "timeline": "",
            "motivation": "",
            "raw_response": ai_text
        }
        
        # Extract sections based on keywords
        for section in sections:
            if "assessment" in section.lower() or "current state" in section.lower():
                parsed["overall_assessment"] = section.strip()
            elif "priority" in section.lower() or "improvement" in section.lower():
                parsed["priority_areas"] = [line.strip() for line in section.split('\n') if line.strip()]
            elif "drill" in section.lower() or "exercise" in section.lower():
                parsed["drill_recommendations"] = [line.strip() for line in section.split('\n') if line.strip()]
            elif "timeline" in section.lower() or "timeframe" in section.lower():
                parsed["timeline"] = section.strip()
            elif "motivat" in section.lower() or "engag" in section.lower():
                parsed["motivation"] = section.strip()
        
        return parsed