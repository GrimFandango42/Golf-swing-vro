"""
Coaching Effectiveness Tracker - Intelligent Feedback Loop System

This module implements comprehensive tracking and analysis of coaching effectiveness
to create a continuous improvement feedback loop. It measures what works for each
user, learns from successful and unsuccessful interactions, and automatically
optimizes coaching strategies over time.

Key Features:
- Real-time effectiveness measurement
- Multi-dimensional coaching evaluation
- A/B testing framework for coaching approaches
- Automatic strategy optimization
- Success pattern identification
- Failure analysis and recovery
- Coaching quality metrics
- Continuous learning algorithms

The system ensures that coaching gets better over time by learning what
works best for each individual user and continuously refining approaches.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
import json
import logging
import sqlite3
import pickle
from collections import defaultdict, deque
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score
import warnings
warnings.filterwarnings('ignore')

logger = logging.getLogger(__name__)

class EffectivenessMetric(Enum):
    """Types of effectiveness metrics"""
    USER_SATISFACTION = "user_satisfaction"
    LEARNING_OUTCOME = "learning_outcome"
    ENGAGEMENT_LEVEL = "engagement_level"
    BEHAVIOR_CHANGE = "behavior_change"
    GOAL_PROGRESS = "goal_progress"
    RETENTION = "retention"
    SWING_IMPROVEMENT = "swing_improvement"
    MOTIVATION_IMPACT = "motivation_impact"

class InteractionOutcome(Enum):
    """Possible outcomes of coaching interactions"""
    HIGHLY_EFFECTIVE = "highly_effective"
    EFFECTIVE = "effective"
    NEUTRAL = "neutral"
    INEFFECTIVE = "ineffective"
    COUNTERPRODUCTIVE = "counterproductive"

class CoachingStrategy(Enum):
    """Different coaching strategies to test"""
    ENCOURAGING = "encouraging"
    ANALYTICAL = "analytical"
    DIRECT = "direct"
    PATIENT = "patient"
    CHALLENGING = "challenging"
    SUPPORTIVE = "supportive"
    TECHNICAL = "technical"
    MOTIVATIONAL = "motivational"

@dataclass
class EffectivenessRecord:
    """Record of coaching effectiveness"""
    record_id: str
    user_id: str
    session_id: str
    interaction_id: str
    
    # Coaching details
    coaching_strategy: CoachingStrategy
    coaching_content: str
    context: Dict[str, Any] = field(default_factory=dict)
    
    # Effectiveness metrics
    user_satisfaction: float = 0.0
    learning_outcome: float = 0.0
    engagement_level: float = 0.0
    behavior_change: float = 0.0
    
    # Overall effectiveness
    overall_effectiveness: float = 0.0
    outcome_category: InteractionOutcome = InteractionOutcome.NEUTRAL
    
    # Measurement metadata
    measurement_method: str = "implicit"  # implicit, explicit, inferred
    confidence: float = 0.5
    
    # Temporal information
    recorded_at: datetime = field(default_factory=datetime.now)
    follow_up_measured_at: Optional[datetime] = None
    
    # Additional context
    user_emotional_state: str = "neutral"
    session_phase: str = "middle"  # beginning, middle, end
    previous_effectiveness: Optional[float] = None

@dataclass
class CoachingExperiment:
    """A/B testing experiment for coaching approaches"""
    experiment_id: str
    experiment_name: str
    user_id: str
    
    # Experiment design
    control_strategy: CoachingStrategy
    test_strategy: CoachingStrategy
    hypothesis: str
    
    # Experiment parameters
    target_sample_size: int = 20
    current_sample_size: int = 0
    success_metric: EffectivenessMetric = EffectivenessMetric.USER_SATISFACTION
    
    # Results
    control_results: List[float] = field(default_factory=list)
    test_results: List[float] = field(default_factory=list)
    
    # Analysis
    is_complete: bool = False
    statistical_significance: Optional[float] = None
    effect_size: Optional[float] = None
    winner: Optional[CoachingStrategy] = None
    
    # Metadata
    started_at: datetime = field(default_factory=datetime.now)
    completed_at: Optional[datetime] = None

@dataclass
class SuccessPattern:
    """Identified pattern of successful coaching"""
    pattern_id: str
    user_id: str
    
    # Pattern details
    pattern_name: str
    description: str
    conditions: Dict[str, Any] = field(default_factory=dict)
    
    # Success metrics
    success_rate: float = 0.0
    average_effectiveness: float = 0.0
    sample_size: int = 0
    
    # Pattern characteristics
    coaching_strategies: List[CoachingStrategy] = field(default_factory=list)
    optimal_contexts: List[str] = field(default_factory=list)
    user_states: List[str] = field(default_factory=list)
    
    # Recommendations
    when_to_use: str = ""
    when_to_avoid: str = ""
    
    # Metadata
    discovered_at: datetime = field(default_factory=datetime.now)
    last_validated: datetime = field(default_factory=datetime.now)
    confidence: float = 0.5

class EffectivenessTracker:
    """Main effectiveness tracking and optimization system"""
    
    def __init__(self, db_path: str = "effectiveness_tracking.db"):
        self.db_path = db_path
        
        # Data storage
        self.effectiveness_records: Dict[str, List[EffectivenessRecord]] = defaultdict(list)
        self.experiments: Dict[str, CoachingExperiment] = {}
        self.success_patterns: Dict[str, List[SuccessPattern]] = defaultdict(list)
        
        # ML models for prediction
        self.effectiveness_predictor = RandomForestClassifier(n_estimators=100, random_state=42)
        self.strategy_optimizer = LogisticRegression(random_state=42)
        self.scaler = StandardScaler()
        
        # Tracking parameters
        self.min_sample_size = 10
        self.significance_threshold = 0.05
        self.effect_size_threshold = 0.2
        
        # Initialize database and load data
        self._init_database()
        self._load_data()
        
        logger.info("Effectiveness Tracker initialized")
    
    def _init_database(self):
        """Initialize database for persistent storage"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # Effectiveness records table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS effectiveness_records (
                record_id TEXT PRIMARY KEY,
                user_id TEXT,
                session_id TEXT,
                interaction_id TEXT,
                record_data BLOB,
                overall_effectiveness REAL,
                recorded_at TIMESTAMP,
                INDEX(user_id, recorded_at)
            )
        ''')
        
        # Experiments table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS coaching_experiments (
                experiment_id TEXT PRIMARY KEY,
                user_id TEXT,
                experiment_data BLOB,
                is_complete BOOLEAN,
                started_at TIMESTAMP,
                completed_at TIMESTAMP
            )
        ''')
        
        # Success patterns table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS success_patterns (
                pattern_id TEXT PRIMARY KEY,
                user_id TEXT,
                pattern_data BLOB,
                success_rate REAL,
                discovered_at TIMESTAMP,
                last_validated TIMESTAMP
            )
        ''')
        
        conn.commit()
        conn.close()
    
    def _load_data(self):
        """Load existing data from database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # Load effectiveness records
        cursor.execute('SELECT user_id, record_data FROM effectiveness_records ORDER BY recorded_at')
        for user_id, record_data in cursor.fetchall():
            try:
                record = pickle.loads(record_data)
                self.effectiveness_records[user_id].append(record)
            except Exception as e:
                logger.error(f"Error loading effectiveness record: {e}")
        
        # Load experiments
        cursor.execute('SELECT experiment_id, experiment_data FROM coaching_experiments')
        for experiment_id, experiment_data in cursor.fetchall():
            try:
                experiment = pickle.loads(experiment_data)
                self.experiments[experiment_id] = experiment
            except Exception as e:
                logger.error(f"Error loading experiment: {e}")
        
        # Load success patterns
        cursor.execute('SELECT user_id, pattern_data FROM success_patterns')
        for user_id, pattern_data in cursor.fetchall():
            try:
                pattern = pickle.loads(pattern_data)
                self.success_patterns[user_id].append(pattern)
            except Exception as e:
                logger.error(f"Error loading success pattern: {e}")
        
        conn.close()
        logger.info(f"Loaded effectiveness data for {len(self.effectiveness_records)} users")
    
    def record_interaction_effectiveness(self, user_id: str, session_id: str, 
                                       interaction_id: str, coaching_strategy: CoachingStrategy,
                                       coaching_content: str, context: Dict = None) -> str:
        """Record effectiveness of a coaching interaction"""
        
        record_id = f"{user_id}_{session_id}_{interaction_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        record = EffectivenessRecord(
            record_id=record_id,
            user_id=user_id,
            session_id=session_id,
            interaction_id=interaction_id,
            coaching_strategy=coaching_strategy,
            coaching_content=coaching_content,
            context=context or {}
        )
        
        # Measure immediate effectiveness
        record = self._measure_immediate_effectiveness(record)
        
        # Store record
        self.effectiveness_records[user_id].append(record)
        self._save_effectiveness_record(record)
        
        # Update any running experiments
        self._update_experiments(user_id, record)
        
        # Trigger learning if we have enough data
        if len(self.effectiveness_records[user_id]) % 10 == 0:
            self._trigger_learning_update(user_id)
        
        return record_id
    
    def _measure_immediate_effectiveness(self, record: EffectivenessRecord) -> EffectivenessRecord:
        """Measure immediate effectiveness using available signals"""
        
        context = record.context
        
        # Extract effectiveness signals from context
        record.user_satisfaction = self._extract_satisfaction_signal(context)
        record.engagement_level = self._extract_engagement_signal(context)
        record.learning_outcome = self._extract_learning_signal(context)
        
        # Calculate overall effectiveness
        record.overall_effectiveness = (
            record.user_satisfaction * 0.4 +
            record.engagement_level * 0.3 +
            record.learning_outcome * 0.3
        )
        
        # Categorize outcome
        if record.overall_effectiveness >= 0.8:
            record.outcome_category = InteractionOutcome.HIGHLY_EFFECTIVE
        elif record.overall_effectiveness >= 0.6:
            record.outcome_category = InteractionOutcome.EFFECTIVE
        elif record.overall_effectiveness >= 0.4:
            record.outcome_category = InteractionOutcome.NEUTRAL
        elif record.overall_effectiveness >= 0.2:
            record.outcome_category = InteractionOutcome.INEFFECTIVE
        else:
            record.outcome_category = InteractionOutcome.COUNTERPRODUCTIVE
        
        return record
    
    def _extract_satisfaction_signal(self, context: Dict) -> float:
        """Extract user satisfaction signal from context"""
        
        satisfaction = 0.5  # Default neutral
        
        # Direct satisfaction feedback
        if 'user_satisfaction' in context:
            satisfaction = context['user_satisfaction']
        
        # Infer from user response
        elif 'user_response' in context:
            response = context['user_response'].lower()
            
            # Positive indicators
            positive_words = ['thanks', 'helpful', 'great', 'good', 'clear', 'understand', 'love']
            positive_count = sum(1 for word in positive_words if word in response)
            
            # Negative indicators
            negative_words = ['confused', 'unclear', 'not helpful', 'frustrated', 'difficult']
            negative_count = sum(1 for word in negative_words if word in response)
            
            if positive_count > negative_count:
                satisfaction = min(0.8, 0.5 + positive_count * 0.1)
            elif negative_count > positive_count:
                satisfaction = max(0.2, 0.5 - negative_count * 0.1)
        
        # Infer from response time
        elif 'response_time' in context:
            response_time = context['response_time']
            if response_time < 5:  # Very quick response
                satisfaction = 0.7
            elif response_time > 30:  # Long thinking time or disengagement
                satisfaction = 0.3
        
        return satisfaction
    
    def _extract_engagement_signal(self, context: Dict) -> float:
        """Extract engagement signal from context"""
        
        engagement = 0.5  # Default
        
        # Direct engagement measure
        if 'engagement_level' in context:
            engagement = context['engagement_level']
        
        # Infer from response length and quality
        elif 'user_response' in context:
            response = context['user_response']
            word_count = len(response.split())
            
            # Good engagement indicators
            if word_count > 20:  # Detailed response
                engagement += 0.2
            elif word_count < 3:  # Very short response
                engagement -= 0.2
            
            # Question asking indicates engagement
            if '?' in response:
                engagement += 0.1
            
            # Follow-up requests indicate engagement
            follow_up_indicators = ['tell me more', 'what about', 'how about', 'can you']
            if any(indicator in response.lower() for indicator in follow_up_indicators):
                engagement += 0.15
        
        # Infer from session duration
        elif 'session_duration' in context:
            duration = context['session_duration']
            if duration > 15:  # Stayed engaged for good duration
                engagement += 0.1
            elif duration < 5:  # Left quickly
                engagement -= 0.2
        
        return max(0, min(1, engagement))
    
    def _extract_learning_signal(self, context: Dict) -> float:
        """Extract learning signal from context"""
        
        learning = 0.5  # Default
        
        # Direct learning indicator
        if 'learning_occurred' in context:
            learning = 1.0 if context['learning_occurred'] else 0.3
        
        # Infer from understanding indicators
        elif 'user_response' in context:
            response = context['user_response'].lower()
            
            # Understanding indicators
            understanding_words = ['i see', 'got it', 'understand', 'makes sense', 'clear now']
            if any(phrase in response for phrase in understanding_words):
                learning = 0.8
            
            # Confusion indicators
            confusion_words = ['confused', "don't understand", 'unclear', 'lost']
            if any(phrase in response for phrase in confusion_words):
                learning = 0.2
            
            # Application indicators
            application_words = ["i'll try", 'will practice', 'going to work on']
            if any(phrase in response for phrase in application_words):
                learning = 0.9
        
        # Infer from subsequent performance
        elif 'performance_improvement' in context:
            improvement = context['performance_improvement']
            learning = min(1.0, 0.5 + improvement / 10)  # Scale improvement to learning score
        
        return learning
    
    def update_delayed_effectiveness(self, record_id: str, user_id: str, 
                                   delayed_metrics: Dict[str, float]):
        """Update effectiveness record with delayed measurements"""
        
        # Find the record
        user_records = self.effectiveness_records.get(user_id, [])
        record = None
        for r in user_records:
            if r.record_id == record_id:
                record = r
                break
        
        if not record:
            logger.warning(f"Record {record_id} not found for user {user_id}")
            return
        
        # Update with delayed metrics
        if 'behavior_change' in delayed_metrics:
            record.behavior_change = delayed_metrics['behavior_change']
        
        if 'goal_progress' in delayed_metrics:
            record.goal_progress = delayed_metrics.get('goal_progress', 0)
        
        if 'swing_improvement' in delayed_metrics:
            record.swing_improvement = delayed_metrics.get('swing_improvement', 0)
        
        # Recalculate overall effectiveness with delayed metrics
        record.overall_effectiveness = (
            record.user_satisfaction * 0.3 +
            record.engagement_level * 0.2 +
            record.learning_outcome * 0.2 +
            record.behavior_change * 0.2 +
            getattr(record, 'goal_progress', 0) * 0.1
        )
        
        record.follow_up_measured_at = datetime.now()
        
        # Save updated record
        self._save_effectiveness_record(record)
    
    def _save_effectiveness_record(self, record: EffectivenessRecord):
        """Save effectiveness record to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO effectiveness_records 
            (record_id, user_id, session_id, interaction_id, record_data, 
             overall_effectiveness, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (
            record.record_id,
            record.user_id,
            record.session_id,
            record.interaction_id,
            pickle.dumps(record),
            record.overall_effectiveness,
            record.recorded_at.isoformat()
        ))
        
        conn.commit()
        conn.close()
    
    def get_effectiveness_analysis(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive effectiveness analysis for a user"""
        
        records = self.effectiveness_records.get(user_id, [])
        
        if not records:
            return {"error": "No effectiveness data available"}
        
        analysis = {
            "overall_metrics": self._calculate_overall_metrics(records),
            "strategy_effectiveness": self._analyze_strategy_effectiveness(records),
            "temporal_trends": self._analyze_temporal_trends(records),
            "context_insights": self._analyze_context_patterns(records),
            "recommendations": self._generate_effectiveness_recommendations(records)
        }
        
        return analysis
    
    def _calculate_overall_metrics(self, records: List[EffectivenessRecord]) -> Dict[str, float]:
        """Calculate overall effectiveness metrics"""
        
        if not records:
            return {}
        
        return {
            "average_effectiveness": np.mean([r.overall_effectiveness for r in records]),
            "satisfaction_score": np.mean([r.user_satisfaction for r in records]),
            "engagement_score": np.mean([r.engagement_level for r in records]),
            "learning_score": np.mean([r.learning_outcome for r in records]),
            "total_interactions": len(records),
            "highly_effective_rate": sum(1 for r in records if r.outcome_category == InteractionOutcome.HIGHLY_EFFECTIVE) / len(records),
            "ineffective_rate": sum(1 for r in records if r.outcome_category in [InteractionOutcome.INEFFECTIVE, InteractionOutcome.COUNTERPRODUCTIVE]) / len(records)
        }
    
    def _analyze_strategy_effectiveness(self, records: List[EffectivenessRecord]) -> Dict[str, Dict[str, float]]:
        """Analyze effectiveness by coaching strategy"""
        
        strategy_stats = defaultdict(list)
        
        for record in records:
            strategy_stats[record.coaching_strategy.value].append(record.overall_effectiveness)
        
        return {
            strategy: {
                "average_effectiveness": np.mean(scores),
                "count": len(scores),
                "std_dev": np.std(scores),
                "success_rate": sum(1 for s in scores if s > 0.6) / len(scores)
            }
            for strategy, scores in strategy_stats.items()
            if len(scores) >= 3  # Minimum sample size
        }
    
    def _analyze_temporal_trends(self, records: List[EffectivenessRecord]) -> Dict[str, Any]:
        """Analyze effectiveness trends over time"""
        
        if len(records) < 5:
            return {"insufficient_data": True}
        
        # Sort by time
        sorted_records = sorted(records, key=lambda r: r.recorded_at)
        
        # Calculate moving average
        window_size = min(5, len(sorted_records))
        moving_avg = []
        
        for i in range(len(sorted_records) - window_size + 1):
            window_records = sorted_records[i:i + window_size]
            avg_effectiveness = np.mean([r.overall_effectiveness for r in window_records])
            moving_avg.append(avg_effectiveness)
        
        # Calculate trend
        if len(moving_avg) > 1:
            x = np.arange(len(moving_avg))
            trend_slope = np.polyfit(x, moving_avg, 1)[0]
        else:
            trend_slope = 0
        
        return {
            "trend_direction": "improving" if trend_slope > 0.01 else "declining" if trend_slope < -0.01 else "stable",
            "trend_slope": trend_slope,
            "current_effectiveness": sorted_records[-1].overall_effectiveness,
            "best_effectiveness": max(r.overall_effectiveness for r in sorted_records),
            "worst_effectiveness": min(r.overall_effectiveness for r in sorted_records)
        }
    
    def _analyze_context_patterns(self, records: List[EffectivenessRecord]) -> Dict[str, Any]:
        """Analyze effectiveness patterns by context"""
        
        context_analysis = {
            "emotional_state_impact": defaultdict(list),
            "session_phase_impact": defaultdict(list),
            "optimal_conditions": []
        }
        
        for record in records:
            # Emotional state impact
            if record.user_emotional_state:
                context_analysis["emotional_state_impact"][record.user_emotional_state].append(record.overall_effectiveness)
            
            # Session phase impact
            if record.session_phase:
                context_analysis["session_phase_impact"][record.session_phase].append(record.overall_effectiveness)
        
        # Calculate averages
        emotional_averages = {
            state: np.mean(scores) 
            for state, scores in context_analysis["emotional_state_impact"].items()
            if len(scores) >= 2
        }
        
        phase_averages = {
            phase: np.mean(scores)
            for phase, scores in context_analysis["session_phase_impact"].items()
            if len(scores) >= 2
        }
        
        return {
            "emotional_state_effectiveness": emotional_averages,
            "session_phase_effectiveness": phase_averages,
            "best_emotional_state": max(emotional_averages.keys(), key=lambda k: emotional_averages[k]) if emotional_averages else None,
            "best_session_phase": max(phase_averages.keys(), key=lambda k: phase_averages[k]) if phase_averages else None
        }
    
    def _generate_effectiveness_recommendations(self, records: List[EffectivenessRecord]) -> List[str]:
        """Generate recommendations based on effectiveness analysis"""
        
        recommendations = []
        
        if not records:
            return ["Insufficient data for recommendations"]
        
        # Strategy recommendations
        strategy_effectiveness = self._analyze_strategy_effectiveness(records)
        if strategy_effectiveness:
            best_strategy = max(strategy_effectiveness.keys(), 
                              key=lambda k: strategy_effectiveness[k]["average_effectiveness"])
            worst_strategy = min(strategy_effectiveness.keys(), 
                               key=lambda k: strategy_effectiveness[k]["average_effectiveness"])
            
            recommendations.append(f"Most effective strategy: {best_strategy}")
            recommendations.append(f"Least effective strategy: {worst_strategy} - consider avoiding")
        
        # Trend recommendations
        temporal_trends = self._analyze_temporal_trends(records)
        if temporal_trends.get("trend_direction") == "declining":
            recommendations.append("Effectiveness is declining - consider changing approach")
        elif temporal_trends.get("trend_direction") == "improving":
            recommendations.append("Effectiveness is improving - continue current approach")
        
        # Context recommendations
        context_patterns = self._analyze_context_patterns(records)
        if context_patterns.get("best_emotional_state"):
            recommendations.append(f"Most effective when user is {context_patterns['best_emotional_state']}")
        
        # Sample size recommendations
        if len(records) < 20:
            recommendations.append("Collect more interaction data for better insights")
        
        return recommendations
    
    def start_experiment(self, user_id: str, experiment_name: str, 
                        control_strategy: CoachingStrategy, test_strategy: CoachingStrategy,
                        hypothesis: str, target_sample_size: int = 20) -> str:
        """Start an A/B testing experiment"""
        
        experiment_id = f"{user_id}_{experiment_name}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        experiment = CoachingExperiment(
            experiment_id=experiment_id,
            experiment_name=experiment_name,
            user_id=user_id,
            control_strategy=control_strategy,
            test_strategy=test_strategy,
            hypothesis=hypothesis,
            target_sample_size=target_sample_size
        )
        
        self.experiments[experiment_id] = experiment
        self._save_experiment(experiment)
        
        logger.info(f"Started experiment {experiment_id} for user {user_id}")
        return experiment_id
    
    def _update_experiments(self, user_id: str, record: EffectivenessRecord):
        """Update running experiments with new data"""
        
        user_experiments = [exp for exp in self.experiments.values() 
                           if exp.user_id == user_id and not exp.is_complete]
        
        for experiment in user_experiments:
            if record.coaching_strategy == experiment.control_strategy:
                experiment.control_results.append(record.overall_effectiveness)
            elif record.coaching_strategy == experiment.test_strategy:
                experiment.test_results.append(record.overall_effectiveness)
            
            # Check if experiment is complete
            total_samples = len(experiment.control_results) + len(experiment.test_results)
            if total_samples >= experiment.target_sample_size:
                self._complete_experiment(experiment)
    
    def _complete_experiment(self, experiment: CoachingExperiment):
        """Complete and analyze an experiment"""
        
        experiment.is_complete = True
        experiment.completed_at = datetime.now()
        
        # Statistical analysis
        if len(experiment.control_results) > 0 and len(experiment.test_results) > 0:
            # T-test for significance
            from scipy import stats
            t_stat, p_value = stats.ttest_ind(experiment.control_results, experiment.test_results)
            experiment.statistical_significance = p_value
            
            # Effect size (Cohen's d)
            control_mean = np.mean(experiment.control_results)
            test_mean = np.mean(experiment.test_results)
            pooled_std = np.sqrt(
                ((len(experiment.control_results) - 1) * np.var(experiment.control_results) +
                 (len(experiment.test_results) - 1) * np.var(experiment.test_results)) /
                (len(experiment.control_results) + len(experiment.test_results) - 2)
            )
            experiment.effect_size = (test_mean - control_mean) / pooled_std if pooled_std > 0 else 0
            
            # Determine winner
            if p_value < self.significance_threshold and abs(experiment.effect_size) > self.effect_size_threshold:
                if test_mean > control_mean:
                    experiment.winner = experiment.test_strategy
                else:
                    experiment.winner = experiment.control_strategy
        
        self._save_experiment(experiment)
        logger.info(f"Completed experiment {experiment.experiment_id}")
    
    def _save_experiment(self, experiment: CoachingExperiment):
        """Save experiment to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO coaching_experiments 
            (experiment_id, user_id, experiment_data, is_complete, started_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (
            experiment.experiment_id,
            experiment.user_id,
            pickle.dumps(experiment),
            experiment.is_complete,
            experiment.started_at.isoformat(),
            experiment.completed_at.isoformat() if experiment.completed_at else None
        ))
        
        conn.commit()
        conn.close()
    
    def get_optimization_recommendations(self, user_id: str) -> Dict[str, Any]:
        """Get AI-powered optimization recommendations"""
        
        records = self.effectiveness_records.get(user_id, [])
        
        if len(records) < self.min_sample_size:
            return {
                "status": "insufficient_data",
                "recommendation": "Continue collecting interaction data",
                "confidence": 0.0
            }
        
        # Prepare features for ML model
        features, targets = self._prepare_ml_features(records)
        
        if len(features) == 0:
            return {
                "status": "feature_extraction_failed",
                "recommendation": "Unable to extract features for optimization",
                "confidence": 0.0
            }
        
        # Train effectiveness predictor
        try:
            X_train, X_test, y_train, y_test = train_test_split(features, targets, test_size=0.2, random_state=42)
            
            # Scale features
            X_train_scaled = self.scaler.fit_transform(X_train)
            X_test_scaled = self.scaler.transform(X_test)
            
            # Train model
            self.effectiveness_predictor.fit(X_train_scaled, y_train)
            
            # Get accuracy
            predictions = self.effectiveness_predictor.predict(X_test_scaled)
            accuracy = accuracy_score(y_test, predictions)
            
            # Get feature importance
            feature_names = self._get_feature_names()
            feature_importance = dict(zip(feature_names, self.effectiveness_predictor.feature_importances_))
            
            # Generate recommendations
            recommendations = self._generate_ml_recommendations(feature_importance, records)
            
            return {
                "status": "success",
                "model_accuracy": accuracy,
                "key_factors": sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)[:5],
                "recommendations": recommendations,
                "confidence": min(accuracy, 0.9)
            }
            
        except Exception as e:
            logger.error(f"Error in ML optimization: {e}")
            return {
                "status": "ml_error",
                "recommendation": "Fallback to statistical analysis",
                "confidence": 0.0
            }
    
    def _prepare_ml_features(self, records: List[EffectivenessRecord]) -> Tuple[List[List[float]], List[int]]:
        """Prepare features for ML models"""
        
        features = []
        targets = []
        
        for record in records:
            feature_vector = []
            
            # Strategy features (one-hot encoded)
            strategies = [s.value for s in CoachingStrategy]
            strategy_features = [1 if s == record.coaching_strategy.value else 0 for s in strategies]
            feature_vector.extend(strategy_features)
            
            # Context features
            context = record.context
            feature_vector.extend([
                context.get('session_duration', 0) / 60,  # Normalize to hours
                context.get('response_time', 0) / 60,     # Normalize to minutes
                1 if context.get('user_emotional_state') == 'positive' else 0,
                1 if context.get('user_emotional_state') == 'negative' else 0,
                context.get('engagement_level', 0.5),
                len(context.get('user_response', '').split()) / 20,  # Normalize word count
            ])
            
            # User state features
            feature_vector.extend([
                record.user_satisfaction,
                record.engagement_level,
                record.learning_outcome
            ])
            
            features.append(feature_vector)
            
            # Target: binary classification (effective vs ineffective)
            target = 1 if record.overall_effectiveness > 0.6 else 0
            targets.append(target)
        
        return features, targets
    
    def _get_feature_names(self) -> List[str]:
        """Get feature names for interpretation"""
        
        names = []
        
        # Strategy features
        strategies = [s.value for s in CoachingStrategy]
        names.extend([f"strategy_{s}" for s in strategies])
        
        # Context features
        names.extend([
            "session_duration", "response_time", "emotional_positive", 
            "emotional_negative", "engagement_level", "response_length"
        ])
        
        # User state features
        names.extend(["user_satisfaction", "engagement", "learning_outcome"])
        
        return names
    
    def _generate_ml_recommendations(self, feature_importance: Dict[str, float], 
                                   records: List[EffectivenessRecord]) -> List[str]:
        """Generate recommendations based on ML analysis"""
        
        recommendations = []
        
        # Top factors
        top_factors = sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)[:3]
        
        for factor, importance in top_factors:
            if factor.startswith("strategy_"):
                strategy = factor.replace("strategy_", "")
                recommendations.append(f"Strategy '{strategy}' is a key effectiveness factor (importance: {importance:.3f})")
            
            elif factor == "session_duration":
                avg_duration = np.mean([r.context.get('session_duration', 0) for r in records])
                recommendations.append(f"Session duration significantly impacts effectiveness. Current average: {avg_duration:.1f} minutes")
            
            elif factor == "engagement_level":
                recommendations.append(f"User engagement level is critical for effectiveness (importance: {importance:.3f})")
        
        return recommendations
    
    def _trigger_learning_update(self, user_id: str):
        """Trigger learning update when sufficient new data is available"""
        
        records = self.effectiveness_records.get(user_id, [])
        
        # Discover new success patterns
        new_patterns = self._discover_success_patterns(user_id, records)
        
        # Update existing patterns
        existing_patterns = self.success_patterns.get(user_id, [])
        for pattern in existing_patterns:
            self._validate_pattern(pattern, records)
        
        # Store new patterns
        for pattern in new_patterns:
            self.success_patterns[user_id].append(pattern)
            self._save_success_pattern(pattern)
        
        logger.info(f"Updated learning for user {user_id}: {len(new_patterns)} new patterns discovered")
    
    def _discover_success_patterns(self, user_id: str, records: List[EffectivenessRecord]) -> List[SuccessPattern]:
        """Discover new success patterns from data"""
        
        patterns = []
        
        # Pattern: High effectiveness with specific strategy
        strategy_success = defaultdict(list)
        for record in records:
            if record.overall_effectiveness > 0.7:
                strategy_success[record.coaching_strategy].append(record)
        
        for strategy, successful_records in strategy_success.items():
            if len(successful_records) >= 3:  # Minimum pattern size
                success_rate = len(successful_records) / sum(1 for r in records if r.coaching_strategy == strategy)
                
                if success_rate > 0.6:  # High success rate
                    pattern = SuccessPattern(
                        pattern_id=f"{user_id}_strategy_{strategy.value}",
                        user_id=user_id,
                        pattern_name=f"High Success with {strategy.value} Strategy",
                        description=f"User responds exceptionally well to {strategy.value} coaching approach",
                        success_rate=success_rate,
                        average_effectiveness=np.mean([r.overall_effectiveness for r in successful_records]),
                        sample_size=len(successful_records),
                        coaching_strategies=[strategy],
                        confidence=min(success_rate, len(successful_records) / 10)
                    )
                    patterns.append(pattern)
        
        return patterns
    
    def _validate_pattern(self, pattern: SuccessPattern, recent_records: List[EffectivenessRecord]):
        """Validate existing pattern against recent data"""
        
        # Find matching records
        matching_records = []
        for record in recent_records[-10:]:  # Last 10 records
            if record.coaching_strategy in pattern.coaching_strategies:
                matching_records.append(record)
        
        if matching_records:
            recent_success_rate = sum(1 for r in matching_records if r.overall_effectiveness > 0.6) / len(matching_records)
            
            # Update pattern confidence
            if recent_success_rate > pattern.success_rate * 0.8:  # Pattern holds
                pattern.confidence = min(1.0, pattern.confidence + 0.1)
            else:  # Pattern weakening
                pattern.confidence = max(0.0, pattern.confidence - 0.1)
            
            pattern.last_validated = datetime.now()
    
    def _save_success_pattern(self, pattern: SuccessPattern):
        """Save success pattern to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO success_patterns 
            (pattern_id, user_id, pattern_data, success_rate, discovered_at, last_validated)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (
            pattern.pattern_id,
            pattern.user_id,
            pickle.dumps(pattern),
            pattern.success_rate,
            pattern.discovered_at.isoformat(),
            pattern.last_validated.isoformat()
        ))
        
        conn.commit()
        conn.close()
    
    def get_effectiveness_insights(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive effectiveness insights"""
        
        insights = {
            "effectiveness_analysis": self.get_effectiveness_analysis(user_id),
            "optimization_recommendations": self.get_optimization_recommendations(user_id),
            "success_patterns": [
                {
                    "name": pattern.pattern_name,
                    "description": pattern.description,
                    "success_rate": pattern.success_rate,
                    "confidence": pattern.confidence
                }
                for pattern in self.success_patterns.get(user_id, [])
                if pattern.confidence > 0.5
            ],
            "active_experiments": [
                {
                    "name": exp.experiment_name,
                    "hypothesis": exp.hypothesis,
                    "progress": (len(exp.control_results) + len(exp.test_results)) / exp.target_sample_size,
                    "preliminary_winner": exp.winner.value if exp.winner else "undetermined"
                }
                for exp in self.experiments.values()
                if exp.user_id == user_id and not exp.is_complete
            ]
        }
        
        return insights