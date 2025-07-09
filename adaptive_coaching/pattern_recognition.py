"""
Pattern Recognition System - Advanced User Behavior Analysis

This module implements sophisticated pattern recognition algorithms to identify
user swing patterns, learning behaviors, and coaching effectiveness patterns.
It provides the intelligence behind understanding what each user needs and
how they learn best.

Key Features:
- Swing fault pattern analysis
- Learning behavior pattern detection
- Coaching effectiveness pattern recognition
- Temporal pattern analysis
- Anomaly detection for breakthrough moments
- Predictive pattern modeling

The system uses machine learning techniques to identify patterns that would
be impossible to detect manually, providing deep insights into user behavior.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict, deque
import json
import logging
from scipy import stats
from scipy.signal import find_peaks
from sklearn.preprocessing import StandardScaler
from sklearn.cluster import KMeans, DBSCAN
from sklearn.decomposition import PCA
from sklearn.ensemble import IsolationForest
import warnings
warnings.filterwarnings('ignore')

logger = logging.getLogger(__name__)

class PatternType(Enum):
    """Types of patterns that can be recognized"""
    SWING_FAULT = "swing_fault"
    LEARNING_BEHAVIOR = "learning_behavior"
    COACHING_EFFECTIVENESS = "coaching_effectiveness"
    PROGRESS_TRAJECTORY = "progress_trajectory"
    EMOTIONAL_STATE = "emotional_state"
    SESSION_TIMING = "session_timing"
    BREAKTHROUGH = "breakthrough"
    PLATEAU = "plateau"

class PatternConfidence(Enum):
    """Confidence levels for pattern recognition"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    VERY_HIGH = "very_high"

@dataclass
class RecognizedPattern:
    """A recognized pattern with metadata"""
    pattern_id: str
    user_id: str
    pattern_type: PatternType
    
    # Pattern details
    pattern_name: str
    description: str
    confidence: PatternConfidence
    confidence_score: float  # 0-1
    
    # Pattern data
    pattern_data: Dict[str, Any] = field(default_factory=dict)
    supporting_evidence: List[str] = field(default_factory=list)
    
    # Temporal information
    first_occurrence: datetime = field(default_factory=datetime.now)
    last_occurrence: datetime = field(default_factory=datetime.now)
    frequency: float = 0.0  # occurrences per session
    
    # Predictive information
    trend: str = "stable"  # "increasing", "decreasing", "stable", "cyclical"
    predicted_next_occurrence: Optional[datetime] = None
    
    # Impact assessment
    impact_score: float = 0.0  # 0-1, how much this pattern affects user
    actionable_insights: List[str] = field(default_factory=list)

@dataclass
class SwingPatternData:
    """Data structure for swing pattern analysis"""
    session_id: str
    user_id: str
    timestamp: datetime
    
    # Swing metrics
    faults: List[str] = field(default_factory=list)
    kpis: Dict[str, float] = field(default_factory=dict)
    overall_score: float = 0.0
    
    # Context
    club_used: str = ""
    session_duration: float = 0.0
    weather_conditions: Dict[str, Any] = field(default_factory=dict)

@dataclass
class LearningBehaviorData:
    """Data structure for learning behavior analysis"""
    interaction_id: str
    user_id: str
    timestamp: datetime
    
    # Interaction metrics
    response_time: float = 0.0
    engagement_level: float = 0.0
    satisfaction_score: float = 0.0
    learning_occurred: bool = False
    
    # Context
    coaching_approach: str = ""
    topic_discussed: str = ""
    emotional_state: str = "neutral"
    session_context: Dict[str, Any] = field(default_factory=dict)

class PatternRecognizer:
    """Advanced pattern recognition engine"""
    
    def __init__(self, min_data_points: int = 5, confidence_threshold: float = 0.6):
        self.min_data_points = min_data_points
        self.confidence_threshold = confidence_threshold
        
        # Pattern storage
        self.recognized_patterns: Dict[str, List[RecognizedPattern]] = defaultdict(list)
        self.pattern_history: Dict[str, List[Any]] = defaultdict(list)
        
        # ML models
        self.scaler = StandardScaler()
        self.anomaly_detector = IsolationForest(contamination=0.1, random_state=42)
        
        # Pattern templates
        self._initialize_pattern_templates()
        
        logger.info("Pattern Recognizer initialized")
    
    def _initialize_pattern_templates(self):
        """Initialize pattern recognition templates"""
        
        self.swing_fault_patterns = {
            "chronic_fault": {
                "min_occurrences": 3,
                "time_window": timedelta(days=30),
                "severity_threshold": 0.6
            },
            "progressive_fault": {
                "trend_threshold": 0.3,
                "consistency_threshold": 0.7
            },
            "situational_fault": {
                "context_correlation": 0.8,
                "min_contexts": 2
            }
        }
        
        self.learning_behavior_patterns = {
            "fast_learner": {
                "improvement_rate": 0.8,
                "retention_rate": 0.9,
                "adaptation_speed": 0.7
            },
            "analytical_learner": {
                "detail_preference": 0.8,
                "question_frequency": 0.6,
                "technical_engagement": 0.7
            },
            "encouragement_dependent": {
                "encouragement_response": 0.8,
                "frustration_threshold": 0.3
            }
        }
        
        self.coaching_effectiveness_patterns = {
            "approach_preference": {
                "satisfaction_differential": 0.3,
                "engagement_differential": 0.2
            },
            "optimal_timing": {
                "time_correlation": 0.6,
                "consistency_threshold": 0.7
            }
        }
    
    def analyze_swing_patterns(self, user_id: str, swing_data: List[SwingPatternData]) -> List[RecognizedPattern]:
        """Analyze swing patterns for a user"""
        
        if len(swing_data) < self.min_data_points:
            return []
        
        patterns = []
        
        # Convert to DataFrame for analysis
        df = self._swing_data_to_dataframe(swing_data)
        
        # Analyze fault patterns
        patterns.extend(self._analyze_fault_patterns(user_id, df))
        
        # Analyze performance trends
        patterns.extend(self._analyze_performance_trends(user_id, df))
        
        # Analyze contextual patterns
        patterns.extend(self._analyze_contextual_patterns(user_id, df))
        
        # Store patterns
        self.recognized_patterns[user_id].extend(patterns)
        
        return patterns
    
    def _swing_data_to_dataframe(self, swing_data: List[SwingPatternData]) -> pd.DataFrame:
        """Convert swing data to DataFrame for analysis"""
        
        data = []
        for swing in swing_data:
            row = {
                'session_id': swing.session_id,
                'timestamp': swing.timestamp,
                'overall_score': swing.overall_score,
                'club_used': swing.club_used,
                'session_duration': swing.session_duration,
                'fault_count': len(swing.faults),
                'faults': json.dumps(swing.faults)
            }
            
            # Add KPIs as columns
            for kpi, value in swing.kpis.items():
                row[f'kpi_{kpi}'] = value
            
            data.append(row)
        
        df = pd.DataFrame(data)
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df.sort_values('timestamp')
        
        return df
    
    def _analyze_fault_patterns(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze fault patterns in swing data"""
        
        patterns = []
        
        # Extract all faults
        all_faults = []
        for faults_json in df['faults']:
            faults = json.loads(faults_json)
            all_faults.extend(faults)
        
        # Count fault occurrences
        fault_counts = pd.Series(all_faults).value_counts()
        
        # Identify chronic faults
        for fault, count in fault_counts.items():
            if count >= self.swing_fault_patterns["chronic_fault"]["min_occurrences"]:
                
                # Calculate pattern confidence
                frequency = count / len(df)
                confidence_score = min(frequency * 2, 1.0)
                
                if confidence_score >= self.confidence_threshold:
                    pattern = RecognizedPattern(
                        pattern_id=f"{user_id}_chronic_fault_{fault}",
                        user_id=user_id,
                        pattern_type=PatternType.SWING_FAULT,
                        pattern_name=f"Chronic {fault}",
                        description=f"User consistently shows {fault} fault pattern",
                        confidence=self._score_to_confidence(confidence_score),
                        confidence_score=confidence_score,
                        pattern_data={
                            "fault_name": fault,
                            "occurrence_count": count,
                            "frequency": frequency,
                            "pattern_type": "chronic"
                        },
                        frequency=frequency,
                        impact_score=confidence_score * 0.8,
                        actionable_insights=[
                            f"Focus on correcting {fault} as it's a persistent issue",
                            f"Create specific drills for {fault} improvement",
                            f"Monitor {fault} improvement over time"
                        ]
                    )
                    patterns.append(pattern)
        
        # Analyze fault progression
        patterns.extend(self._analyze_fault_progression(user_id, df))
        
        return patterns
    
    def _analyze_fault_progression(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze fault progression over time"""
        
        patterns = []
        
        # Group by time windows
        df['week'] = df['timestamp'].dt.isocalendar().week
        weekly_faults = df.groupby('week')['fault_count'].mean()
        
        if len(weekly_faults) >= 3:
            # Calculate trend
            weeks = list(range(len(weekly_faults)))
            slope, intercept, r_value, p_value, std_err = stats.linregress(weeks, weekly_faults.values)
            
            # Identify trend patterns
            if abs(r_value) > 0.6 and p_value < 0.05:
                if slope > 0:
                    trend = "increasing"
                    concern_level = "high"
                else:
                    trend = "decreasing"
                    concern_level = "low"
                
                pattern = RecognizedPattern(
                    pattern_id=f"{user_id}_fault_progression",
                    user_id=user_id,
                    pattern_type=PatternType.PROGRESS_TRAJECTORY,
                    pattern_name=f"Fault Progression - {trend.title()}",
                    description=f"User's fault frequency is {trend} over time",
                    confidence=self._score_to_confidence(abs(r_value)),
                    confidence_score=abs(r_value),
                    pattern_data={
                        "trend": trend,
                        "slope": slope,
                        "correlation": r_value,
                        "p_value": p_value
                    },
                    trend=trend,
                    impact_score=abs(r_value) * 0.7,
                    actionable_insights=[
                        f"Fault frequency is {trend} - adjust coaching approach",
                        f"{'Increase' if trend == 'increasing' else 'Maintain'} practice intensity",
                        f"Monitor progress closely"
                    ]
                )
                patterns.append(pattern)
        
        return patterns
    
    def _analyze_performance_trends(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze overall performance trends"""
        
        patterns = []
        
        if len(df) < 5:
            return patterns
        
        # Calculate moving averages
        df['score_ma'] = df['overall_score'].rolling(window=3).mean()
        
        # Identify breakthrough moments
        score_diff = df['overall_score'].diff()
        breakthrough_threshold = score_diff.std() * 2
        
        breakthroughs = df[score_diff > breakthrough_threshold]
        
        if len(breakthroughs) > 0:
            pattern = RecognizedPattern(
                pattern_id=f"{user_id}_breakthrough_moments",
                user_id=user_id,
                pattern_type=PatternType.BREAKTHROUGH,
                pattern_name="Breakthrough Moments",
                description="User shows significant improvement spikes",
                confidence=PatternConfidence.MEDIUM,
                confidence_score=0.7,
                pattern_data={
                    "breakthrough_count": len(breakthroughs),
                    "average_improvement": score_diff[score_diff > breakthrough_threshold].mean(),
                    "breakthrough_sessions": breakthroughs['session_id'].tolist()
                },
                impact_score=0.8,
                actionable_insights=[
                    "User shows capacity for significant improvement",
                    "Analyze breakthrough sessions for success factors",
                    "Replicate conditions that lead to breakthroughs"
                ]
            )
            patterns.append(pattern)
        
        # Identify plateau periods
        score_variance = df['overall_score'].rolling(window=5).var()
        plateau_threshold = score_variance.mean() * 0.3
        
        plateau_periods = df[score_variance < plateau_threshold]
        
        if len(plateau_periods) >= 3:
            pattern = RecognizedPattern(
                pattern_id=f"{user_id}_plateau_periods",
                user_id=user_id,
                pattern_type=PatternType.PLATEAU,
                pattern_name="Plateau Periods",
                description="User experiences periods of minimal improvement",
                confidence=PatternConfidence.MEDIUM,
                confidence_score=0.6,
                pattern_data={
                    "plateau_session_count": len(plateau_periods),
                    "average_variance": score_variance.mean(),
                    "plateau_sessions": plateau_periods['session_id'].tolist()
                },
                impact_score=0.6,
                actionable_insights=[
                    "User may need varied coaching approaches during plateaus",
                    "Introduce new challenges to break plateau patterns",
                    "Focus on motivation during flat periods"
                ]
            )
            patterns.append(pattern)
        
        return patterns
    
    def _analyze_contextual_patterns(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze contextual patterns (club, timing, etc.)"""
        
        patterns = []
        
        # Club-specific patterns
        if 'club_used' in df.columns:
            club_performance = df.groupby('club_used')['overall_score'].agg(['mean', 'count'])
            club_performance = club_performance[club_performance['count'] >= 3]
            
            if len(club_performance) > 1:
                # Find best and worst performing clubs
                best_club = club_performance['mean'].idxmax()
                worst_club = club_performance['mean'].idxmin()
                
                performance_gap = club_performance.loc[best_club, 'mean'] - club_performance.loc[worst_club, 'mean']
                
                if performance_gap > 0.2:  # Significant difference
                    pattern = RecognizedPattern(
                        pattern_id=f"{user_id}_club_performance_variation",
                        user_id=user_id,
                        pattern_type=PatternType.SWING_FAULT,
                        pattern_name="Club Performance Variation",
                        description="User shows different performance levels with different clubs",
                        confidence=PatternConfidence.MEDIUM,
                        confidence_score=0.7,
                        pattern_data={
                            "best_club": best_club,
                            "worst_club": worst_club,
                            "performance_gap": performance_gap,
                            "club_scores": club_performance.to_dict()
                        },
                        impact_score=0.7,
                        actionable_insights=[
                            f"Focus on improving {worst_club} technique",
                            f"Analyze what works well with {best_club}",
                            "Consider club-specific coaching approaches"
                        ]
                    )
                    patterns.append(pattern)
        
        return patterns
    
    def analyze_learning_behavior(self, user_id: str, behavior_data: List[LearningBehaviorData]) -> List[RecognizedPattern]:
        """Analyze learning behavior patterns"""
        
        if len(behavior_data) < self.min_data_points:
            return []
        
        patterns = []
        
        # Convert to DataFrame
        df = self._behavior_data_to_dataframe(behavior_data)
        
        # Analyze learning speed
        patterns.extend(self._analyze_learning_speed(user_id, df))
        
        # Analyze engagement patterns
        patterns.extend(self._analyze_engagement_patterns(user_id, df))
        
        # Analyze emotional patterns
        patterns.extend(self._analyze_emotional_patterns(user_id, df))
        
        # Store patterns
        self.recognized_patterns[user_id].extend(patterns)
        
        return patterns
    
    def _behavior_data_to_dataframe(self, behavior_data: List[LearningBehaviorData]) -> pd.DataFrame:
        """Convert behavior data to DataFrame"""
        
        data = []
        for behavior in behavior_data:
            row = {
                'interaction_id': behavior.interaction_id,
                'timestamp': behavior.timestamp,
                'response_time': behavior.response_time,
                'engagement_level': behavior.engagement_level,
                'satisfaction_score': behavior.satisfaction_score,
                'learning_occurred': behavior.learning_occurred,
                'coaching_approach': behavior.coaching_approach,
                'topic_discussed': behavior.topic_discussed,
                'emotional_state': behavior.emotional_state
            }
            data.append(row)
        
        df = pd.DataFrame(data)
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df.sort_values('timestamp')
        
        return df
    
    def _analyze_learning_speed(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze learning speed patterns"""
        
        patterns = []
        
        # Calculate learning rate
        learning_rate = df['learning_occurred'].mean()
        
        # Analyze improvement over time
        df['session_order'] = range(len(df))
        df['cumulative_learning'] = df['learning_occurred'].cumsum()
        
        if len(df) >= 5:
            # Calculate learning velocity
            slope, intercept, r_value, p_value, std_err = stats.linregress(
                df['session_order'], df['cumulative_learning']
            )
            
            # Categorize learning speed
            if slope > 0.8:
                speed_category = "fast"
                confidence = 0.8
            elif slope > 0.5:
                speed_category = "moderate"
                confidence = 0.6
            else:
                speed_category = "slow"
                confidence = 0.7
            
            pattern = RecognizedPattern(
                pattern_id=f"{user_id}_learning_speed",
                user_id=user_id,
                pattern_type=PatternType.LEARNING_BEHAVIOR,
                pattern_name=f"Learning Speed - {speed_category.title()}",
                description=f"User demonstrates {speed_category} learning pace",
                confidence=self._score_to_confidence(confidence),
                confidence_score=confidence,
                pattern_data={
                    "learning_rate": learning_rate,
                    "learning_velocity": slope,
                    "speed_category": speed_category,
                    "correlation": r_value
                },
                impact_score=confidence * 0.8,
                actionable_insights=[
                    f"Adapt pacing for {speed_category} learner",
                    f"{'Increase' if speed_category == 'fast' else 'Maintain'} content complexity",
                    f"Provide {'challenge' if speed_category == 'fast' else 'support'}"
                ]
            )
            patterns.append(pattern)
        
        return patterns
    
    def _analyze_engagement_patterns(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze engagement patterns"""
        
        patterns = []
        
        # Analyze engagement by coaching approach
        approach_engagement = df.groupby('coaching_approach')['engagement_level'].agg(['mean', 'count'])
        approach_engagement = approach_engagement[approach_engagement['count'] >= 2]
        
        if len(approach_engagement) > 1:
            best_approach = approach_engagement['mean'].idxmax()
            engagement_score = approach_engagement.loc[best_approach, 'mean']
            
            if engagement_score > 0.7:
                pattern = RecognizedPattern(
                    pattern_id=f"{user_id}_preferred_coaching_approach",
                    user_id=user_id,
                    pattern_type=PatternType.COACHING_EFFECTIVENESS,
                    pattern_name="Preferred Coaching Approach",
                    description=f"User engages best with {best_approach} approach",
                    confidence=PatternConfidence.HIGH,
                    confidence_score=0.8,
                    pattern_data={
                        "preferred_approach": best_approach,
                        "engagement_score": engagement_score,
                        "approach_scores": approach_engagement.to_dict()
                    },
                    impact_score=0.9,
                    actionable_insights=[
                        f"Prioritize {best_approach} coaching approach",
                        f"User responds well to {best_approach} style",
                        "Tailor communication to preferred approach"
                    ]
                )
                patterns.append(pattern)
        
        return patterns
    
    def _analyze_emotional_patterns(self, user_id: str, df: pd.DataFrame) -> List[RecognizedPattern]:
        """Analyze emotional state patterns"""
        
        patterns = []
        
        # Analyze emotional state frequency
        emotion_counts = df['emotional_state'].value_counts()
        
        # Identify dominant emotional states
        if len(emotion_counts) > 0:
            dominant_emotion = emotion_counts.index[0]
            emotion_frequency = emotion_counts.iloc[0] / len(df)
            
            if emotion_frequency > 0.4:  # Appears in 40%+ of interactions
                pattern = RecognizedPattern(
                    pattern_id=f"{user_id}_dominant_emotional_state",
                    user_id=user_id,
                    pattern_type=PatternType.EMOTIONAL_STATE,
                    pattern_name=f"Dominant Emotional State - {dominant_emotion.title()}",
                    description=f"User frequently shows {dominant_emotion} emotional state",
                    confidence=PatternConfidence.MEDIUM,
                    confidence_score=emotion_frequency,
                    pattern_data={
                        "dominant_emotion": dominant_emotion,
                        "frequency": emotion_frequency,
                        "emotion_distribution": emotion_counts.to_dict()
                    },
                    impact_score=emotion_frequency * 0.8,
                    actionable_insights=[
                        f"Adapt coaching for {dominant_emotion} users",
                        f"Monitor emotional state during sessions",
                        f"Use appropriate strategies for {dominant_emotion} state"
                    ]
                )
                patterns.append(pattern)
        
        return patterns
    
    def detect_anomalies(self, user_id: str, recent_data: List[Dict]) -> List[RecognizedPattern]:
        """Detect anomalous patterns that might indicate breakthroughs or problems"""
        
        if len(recent_data) < 10:
            return []
        
        patterns = []
        
        # Prepare data for anomaly detection
        features = []
        for data_point in recent_data:
            feature_vector = [
                data_point.get('overall_score', 0),
                data_point.get('engagement_level', 0),
                data_point.get('satisfaction_score', 0),
                data_point.get('fault_count', 0),
                data_point.get('learning_occurred', 0)
            ]
            features.append(feature_vector)
        
        # Fit anomaly detector
        features_scaled = self.scaler.fit_transform(features)
        anomaly_scores = self.anomaly_detector.fit_predict(features_scaled)
        
        # Identify anomalies
        anomaly_indices = np.where(anomaly_scores == -1)[0]
        
        if len(anomaly_indices) > 0:
            # Analyze anomalies
            anomaly_data = [recent_data[i] for i in anomaly_indices]
            
            # Determine if anomalies are positive or negative
            avg_score = np.mean([d.get('overall_score', 0) for d in recent_data])
            anomaly_scores_actual = [d.get('overall_score', 0) for d in anomaly_data]
            
            if np.mean(anomaly_scores_actual) > avg_score:
                anomaly_type = "positive"
                pattern_name = "Performance Breakthrough"
                description = "User shows unexpected performance improvement"
            else:
                anomaly_type = "negative"
                pattern_name = "Performance Anomaly"
                description = "User shows unexpected performance decline"
            
            pattern = RecognizedPattern(
                pattern_id=f"{user_id}_anomaly_{anomaly_type}",
                user_id=user_id,
                pattern_type=PatternType.BREAKTHROUGH if anomaly_type == "positive" else PatternType.SWING_FAULT,
                pattern_name=pattern_name,
                description=description,
                confidence=PatternConfidence.MEDIUM,
                confidence_score=0.7,
                pattern_data={
                    "anomaly_type": anomaly_type,
                    "anomaly_count": len(anomaly_indices),
                    "anomaly_sessions": [recent_data[i].get('session_id', '') for i in anomaly_indices]
                },
                impact_score=0.8,
                actionable_insights=[
                    f"Investigate {anomaly_type} performance anomaly",
                    f"{'Celebrate and analyze' if anomaly_type == 'positive' else 'Address underlying issues'}",
                    "Monitor closely for pattern continuation"
                ]
            )
            patterns.append(pattern)
        
        return patterns
    
    def _score_to_confidence(self, score: float) -> PatternConfidence:
        """Convert numeric score to confidence level"""
        if score >= 0.9:
            return PatternConfidence.VERY_HIGH
        elif score >= 0.7:
            return PatternConfidence.HIGH
        elif score >= 0.5:
            return PatternConfidence.MEDIUM
        else:
            return PatternConfidence.LOW
    
    def get_user_patterns(self, user_id: str, pattern_type: Optional[PatternType] = None) -> List[RecognizedPattern]:
        """Get recognized patterns for a user"""
        patterns = self.recognized_patterns.get(user_id, [])
        
        if pattern_type:
            patterns = [p for p in patterns if p.pattern_type == pattern_type]
        
        return sorted(patterns, key=lambda p: p.confidence_score, reverse=True)
    
    def get_pattern_insights(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive pattern insights for a user"""
        
        patterns = self.recognized_patterns.get(user_id, [])
        
        insights = {
            "pattern_summary": {
                "total_patterns": len(patterns),
                "high_confidence_patterns": len([p for p in patterns if p.confidence_score > 0.7]),
                "pattern_types": list(set([p.pattern_type.value for p in patterns]))
            },
            "top_patterns": [
                {
                    "name": p.pattern_name,
                    "type": p.pattern_type.value,
                    "confidence": p.confidence_score,
                    "impact": p.impact_score,
                    "insights": p.actionable_insights
                }
                for p in sorted(patterns, key=lambda x: x.confidence_score, reverse=True)[:5]
            ],
            "coaching_recommendations": [],
            "areas_of_focus": []
        }
        
        # Generate coaching recommendations based on patterns
        for pattern in patterns:
            if pattern.confidence_score > 0.7:
                insights["coaching_recommendations"].extend(pattern.actionable_insights)
        
        # Remove duplicates
        insights["coaching_recommendations"] = list(set(insights["coaching_recommendations"]))
        
        return insights
    
    def update_pattern_feedback(self, user_id: str, pattern_id: str, 
                               feedback: Dict[str, Any]):
        """Update pattern based on user feedback"""
        
        patterns = self.recognized_patterns.get(user_id, [])
        
        for pattern in patterns:
            if pattern.pattern_id == pattern_id:
                # Update confidence based on feedback
                if feedback.get("accurate", False):
                    pattern.confidence_score = min(pattern.confidence_score + 0.1, 1.0)
                else:
                    pattern.confidence_score = max(pattern.confidence_score - 0.2, 0.0)
                
                # Update confidence level
                pattern.confidence = self._score_to_confidence(pattern.confidence_score)
                
                # Remove pattern if confidence drops too low
                if pattern.confidence_score < 0.3:
                    self.recognized_patterns[user_id].remove(pattern)
                
                break