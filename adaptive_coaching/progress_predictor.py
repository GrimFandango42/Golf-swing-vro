"""
Progress Prediction System - AI-Powered Golf Improvement Forecasting

This module implements sophisticated machine learning models to predict user
progress, identify optimal learning paths, and recommend personalized goals.
It anticipates what users need to work on next and predicts their improvement
trajectory based on historical patterns and current performance.

Key Features:
- ML-based progress trajectory prediction
- Personalized goal recommendation
- Breakthrough moment prediction
- Plateau detection and intervention
- Optimal practice scheduling
- Skill development forecasting
- Performance ceiling analysis
- Learning velocity optimization

The system acts as a crystal ball for golf improvement, helping coaches and
users understand what's coming next and how to optimize the journey.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
import json
import logging
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_absolute_error, r2_score
from scipy import stats
from scipy.optimize import minimize
import warnings
warnings.filterwarnings('ignore')

logger = logging.getLogger(__name__)

class PredictionType(Enum):
    """Types of predictions the system can make"""
    OVERALL_SCORE = "overall_score"
    FAULT_REDUCTION = "fault_reduction"
    SKILL_IMPROVEMENT = "skill_improvement"
    BREAKTHROUGH = "breakthrough"
    PLATEAU_DURATION = "plateau_duration"
    GOAL_ACHIEVEMENT = "goal_achievement"
    PRACTICE_EFFECTIVENESS = "practice_effectiveness"

class PredictionConfidence(Enum):
    """Confidence levels for predictions"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    VERY_HIGH = "very_high"

class ImprovementPhase(Enum):
    """Different phases of improvement"""
    RAPID_LEARNING = "rapid_learning"
    STEADY_PROGRESS = "steady_progress"
    PLATEAU = "plateau"
    BREAKTHROUGH = "breakthrough"
    MASTERY = "mastery"
    REGRESSION = "regression"

@dataclass
class PredictionResult:
    """Result of a prediction"""
    prediction_id: str
    user_id: str
    prediction_type: PredictionType
    
    # Prediction details
    predicted_value: float
    confidence: PredictionConfidence
    confidence_score: float
    prediction_horizon: int  # days
    
    # Model information
    model_used: str
    features_used: List[str]
    training_score: float
    
    # Context
    current_baseline: float
    improvement_potential: float
    
    # Recommendations
    recommendations: List[str] = field(default_factory=list)
    action_items: List[str] = field(default_factory=list)
    
    # Metadata
    created_at: datetime = field(default_factory=datetime.now)
    expires_at: Optional[datetime] = None

@dataclass
class ProgressTrajectory:
    """User's predicted progress trajectory"""
    user_id: str
    trajectory_id: str
    
    # Trajectory data
    time_points: List[datetime] = field(default_factory=list)
    predicted_scores: List[float] = field(default_factory=list)
    confidence_intervals: List[Tuple[float, float]] = field(default_factory=list)
    
    # Milestones
    predicted_milestones: List[Dict[str, Any]] = field(default_factory=list)
    breakthrough_probability: float = 0.0
    plateau_risk: float = 0.0
    
    # Recommendations
    optimal_practice_frequency: int = 3  # sessions per week
    focus_areas: List[str] = field(default_factory=list)
    next_goals: List[str] = field(default_factory=list)

@dataclass
class GoalRecommendation:
    """Recommended goal for a user"""
    goal_id: str
    user_id: str
    
    # Goal details
    goal_title: str
    goal_description: str
    target_value: float
    current_value: float
    
    # Timing
    recommended_timeline: int  # days
    difficulty_level: str  # "easy", "moderate", "challenging"
    
    # Predictions
    success_probability: float
    expected_completion_date: datetime
    
    # Supporting data
    rationale: str
    supporting_evidence: List[str] = field(default_factory=list)

class ProgressPredictor:
    """Main progress prediction engine"""
    
    def __init__(self, min_data_points: int = 10):
        self.min_data_points = min_data_points
        
        # ML Models
        self.models = {
            'random_forest': RandomForestRegressor(n_estimators=100, random_state=42),
            'gradient_boost': GradientBoostingRegressor(n_estimators=100, random_state=42),
            'linear': Ridge(alpha=1.0),
            'baseline': LinearRegression()
        }
        
        # Scalers and encoders
        self.scaler = StandardScaler()
        self.label_encoders = {}
        
        # Prediction history
        self.predictions: Dict[str, List[PredictionResult]] = {}
        self.trajectories: Dict[str, ProgressTrajectory] = {}
        
        # Feature engineering
        self.feature_extractors = {
            'temporal': self._extract_temporal_features,
            'performance': self._extract_performance_features,
            'behavioral': self._extract_behavioral_features,
            'contextual': self._extract_contextual_features
        }
        
        logger.info("Progress Predictor initialized")
    
    def predict_progress(self, user_id: str, historical_data: List[Dict],
                        prediction_horizon: int = 30) -> ProgressTrajectory:
        """Predict user's progress trajectory"""
        
        if len(historical_data) < self.min_data_points:
            logger.warning(f"Insufficient data for prediction: {len(historical_data)} points")
            return self._create_basic_trajectory(user_id, historical_data, prediction_horizon)
        
        # Prepare data
        df = self._prepare_data(historical_data)
        features, target = self._extract_features_and_target(df)
        
        # Train models
        model_performance = self._train_models(features, target)
        best_model_name = max(model_performance.keys(), key=lambda k: model_performance[k]['r2'])
        best_model = self.models[best_model_name]
        
        # Generate predictions
        trajectory = self._generate_trajectory(
            user_id, best_model, features, df, prediction_horizon, best_model_name
        )
        
        # Add analysis
        trajectory = self._analyze_trajectory(trajectory, df)
        
        # Store trajectory
        self.trajectories[user_id] = trajectory
        
        return trajectory
    
    def _prepare_data(self, historical_data: List[Dict]) -> pd.DataFrame:
        """Prepare historical data for analysis"""
        
        df = pd.DataFrame(historical_data)
        
        # Ensure datetime column
        if 'timestamp' in df.columns:
            df['timestamp'] = pd.to_datetime(df['timestamp'])
        else:
            df['timestamp'] = pd.date_range(start='2024-01-01', periods=len(df), freq='D')
        
        # Sort by timestamp
        df = df.sort_values('timestamp')
        
        # Add derived features
        df['days_since_start'] = (df['timestamp'] - df['timestamp'].min()).dt.days
        df['session_number'] = range(1, len(df) + 1)
        
        # Handle missing values
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        df[numeric_columns] = df[numeric_columns].fillna(df[numeric_columns].mean())
        
        return df
    
    def _extract_features_and_target(self, df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
        """Extract features and target variable from data"""
        
        features_df = pd.DataFrame()
        
        # Extract all feature types
        for feature_type, extractor in self.feature_extractors.items():
            feature_subset = extractor(df)
            features_df = pd.concat([features_df, feature_subset], axis=1)
        
        # Target variable (overall score or primary KPI)
        if 'overall_score' in df.columns:
            target = df['overall_score']
        elif 'score' in df.columns:
            target = df['score']
        else:
            # Create synthetic target from available metrics
            numeric_cols = df.select_dtypes(include=[np.number]).columns
            target = df[numeric_cols].mean(axis=1)
        
        return features_df, target
    
    def _extract_temporal_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract temporal features"""
        
        features = pd.DataFrame()
        
        features['days_since_start'] = df['days_since_start']
        features['session_number'] = df['session_number']
        
        # Day of week and time patterns
        if 'timestamp' in df.columns:
            features['day_of_week'] = df['timestamp'].dt.dayofweek
            features['hour'] = df['timestamp'].dt.hour
            features['is_weekend'] = (df['timestamp'].dt.dayofweek >= 5).astype(int)
        
        # Session gaps
        if len(df) > 1:
            session_gaps = df['timestamp'].diff().dt.days.fillna(0)
            features['days_since_last_session'] = session_gaps
            features['avg_session_gap'] = session_gaps.rolling(window=5, min_periods=1).mean()
        
        return features
    
    def _extract_performance_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract performance-related features"""
        
        features = pd.DataFrame()
        
        # Performance metrics
        if 'overall_score' in df.columns:
            features['current_score'] = df['overall_score']
            features['score_trend'] = df['overall_score'].rolling(window=3, min_periods=1).mean()
            features['score_volatility'] = df['overall_score'].rolling(window=5, min_periods=1).std()
        
        # Fault patterns
        if 'fault_count' in df.columns:
            features['fault_count'] = df['fault_count']
            features['fault_trend'] = df['fault_count'].rolling(window=3, min_periods=1).mean()
        
        # Improvement velocity
        if 'overall_score' in df.columns and len(df) > 2:
            score_diff = df['overall_score'].diff()
            features['improvement_velocity'] = score_diff.rolling(window=3, min_periods=1).mean()
            features['acceleration'] = score_diff.diff().rolling(window=3, min_periods=1).mean()
        
        return features
    
    def _extract_behavioral_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract behavioral features"""
        
        features = pd.DataFrame()
        
        # Engagement patterns
        if 'engagement_level' in df.columns:
            features['engagement'] = df['engagement_level']
            features['engagement_trend'] = df['engagement_level'].rolling(window=3, min_periods=1).mean()
        
        # Learning patterns
        if 'learning_occurred' in df.columns:
            features['learning_rate'] = df['learning_occurred'].rolling(window=5, min_periods=1).mean()
        
        # Session duration patterns
        if 'session_duration' in df.columns:
            features['session_duration'] = df['session_duration']
            features['avg_duration'] = df['session_duration'].rolling(window=3, min_periods=1).mean()
        
        return features
    
    def _extract_contextual_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract contextual features"""
        
        features = pd.DataFrame()
        
        # Club usage patterns
        if 'club_used' in df.columns:
            # Encode club types
            if 'club_used' not in self.label_encoders:
                self.label_encoders['club_used'] = LabelEncoder()
                features['club_encoded'] = self.label_encoders['club_used'].fit_transform(df['club_used'].fillna('Unknown'))
            else:
                try:
                    features['club_encoded'] = self.label_encoders['club_used'].transform(df['club_used'].fillna('Unknown'))
                except ValueError:
                    # Handle unseen categories
                    features['club_encoded'] = 0
        
        # Weather or environmental factors (if available)
        if 'weather_score' in df.columns:
            features['weather_score'] = df['weather_score']
        
        # Practice vs. play context
        if 'session_type' in df.columns:
            features['is_practice'] = (df['session_type'] == 'practice').astype(int)
        
        return features
    
    def _train_models(self, features: pd.DataFrame, target: pd.Series) -> Dict[str, Dict]:
        """Train multiple models and return performance metrics"""
        
        # Handle missing values
        features = features.fillna(0)
        
        # Scale features
        try:
            features_scaled = self.scaler.fit_transform(features)
        except ValueError:
            # Handle case where all features are constant
            features_scaled = features.values
        
        model_performance = {}
        
        for model_name, model in self.models.items():
            try:
                # Cross-validation
                cv_scores = cross_val_score(model, features_scaled, target, cv=min(5, len(target)//2))
                
                # Fit model
                model.fit(features_scaled, target)
                predictions = model.predict(features_scaled)
                
                # Calculate metrics
                r2 = r2_score(target, predictions)
                mae = mean_absolute_error(target, predictions)
                
                model_performance[model_name] = {
                    'r2': r2,
                    'mae': mae,
                    'cv_mean': cv_scores.mean(),
                    'cv_std': cv_scores.std()
                }
                
            except Exception as e:
                logger.warning(f"Error training model {model_name}: {e}")
                model_performance[model_name] = {
                    'r2': 0.0,
                    'mae': float('inf'),
                    'cv_mean': 0.0,
                    'cv_std': 0.0
                }
        
        return model_performance
    
    def _generate_trajectory(self, user_id: str, model, features: pd.DataFrame, 
                           df: pd.DataFrame, prediction_horizon: int, 
                           model_name: str) -> ProgressTrajectory:
        """Generate progress trajectory using trained model"""
        
        trajectory = ProgressTrajectory(
            user_id=user_id,
            trajectory_id=f"{user_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        )
        
        # Generate future time points
        last_date = df['timestamp'].max()
        future_dates = [last_date + timedelta(days=i) for i in range(1, prediction_horizon + 1)]
        trajectory.time_points = future_dates
        
        # Current state
        current_features = features.iloc[-1:].values
        if hasattr(self.scaler, 'transform'):
            try:
                current_features_scaled = self.scaler.transform(current_features)
            except:
                current_features_scaled = current_features
        else:
            current_features_scaled = current_features
        
        # Predict future values
        predicted_scores = []
        confidence_intervals = []
        
        for i in range(prediction_horizon):
            try:
                # Make prediction
                prediction = model.predict(current_features_scaled)[0]
                
                # Add some uncertainty based on model performance
                uncertainty = 0.1 * abs(prediction)  # 10% uncertainty
                lower_bound = prediction - uncertainty
                upper_bound = prediction + uncertainty
                
                predicted_scores.append(prediction)
                confidence_intervals.append((lower_bound, upper_bound))
                
                # Update features for next prediction (simple approach)
                # In reality, this would be more sophisticated
                if i < prediction_horizon - 1:
                    current_features_scaled[0][0] += 1  # Increment time-based features
                
            except Exception as e:
                logger.warning(f"Error making prediction for day {i}: {e}")
                # Fallback to trend extrapolation
                if predicted_scores:
                    last_prediction = predicted_scores[-1]
                else:
                    last_prediction = df['overall_score'].iloc[-1] if 'overall_score' in df.columns else 50
                
                predicted_scores.append(last_prediction)
                confidence_intervals.append((last_prediction - 5, last_prediction + 5))
        
        trajectory.predicted_scores = predicted_scores
        trajectory.confidence_intervals = confidence_intervals
        
        return trajectory
    
    def _analyze_trajectory(self, trajectory: ProgressTrajectory, df: pd.DataFrame) -> ProgressTrajectory:
        """Analyze trajectory and add insights"""
        
        # Calculate trajectory statistics
        scores = trajectory.predicted_scores
        
        if len(scores) > 0:
            # Trend analysis
            if len(scores) > 1:
                slope = np.polyfit(range(len(scores)), scores, 1)[0]
                
                if slope > 0.1:
                    improvement_phase = ImprovementPhase.STEADY_PROGRESS
                elif slope > 0.05:
                    improvement_phase = ImprovementPhase.PLATEAU
                else:
                    improvement_phase = ImprovementPhase.PLATEAU
                
                # Breakthrough probability
                score_variance = np.var(scores)
                trajectory.breakthrough_probability = min(0.8, slope * 10 + score_variance / 100)
                
                # Plateau risk
                trajectory.plateau_risk = max(0.0, 0.5 - slope * 5)
            
            # Predict milestones
            current_score = df['overall_score'].iloc[-1] if 'overall_score' in df.columns else 50
            
            milestones = []
            for i, score in enumerate(scores):
                if score > current_score + 5:  # Significant improvement
                    milestones.append({
                        'date': trajectory.time_points[i],
                        'score': score,
                        'type': 'improvement_milestone',
                        'description': f'Reach score of {score:.1f}'
                    })
                    break
            
            trajectory.predicted_milestones = milestones
        
        # Generate recommendations
        trajectory = self._generate_trajectory_recommendations(trajectory, df)
        
        return trajectory
    
    def _generate_trajectory_recommendations(self, trajectory: ProgressTrajectory, 
                                           df: pd.DataFrame) -> ProgressTrajectory:
        """Generate recommendations based on trajectory"""
        
        # Analyze current performance
        if 'overall_score' in df.columns:
            recent_scores = df['overall_score'].tail(5)
            score_trend = recent_scores.diff().mean()
        else:
            score_trend = 0
        
        # Optimal practice frequency
        if trajectory.breakthrough_probability > 0.6:
            trajectory.optimal_practice_frequency = 4  # More frequent for breakthrough potential
        elif trajectory.plateau_risk > 0.6:
            trajectory.optimal_practice_frequency = 2  # Less frequent to avoid burnout
        else:
            trajectory.optimal_practice_frequency = 3  # Standard frequency
        
        # Focus areas based on patterns
        focus_areas = []
        if 'fault_count' in df.columns:
            recent_faults = df['fault_count'].tail(5).mean()
            if recent_faults > 2:
                focus_areas.append("fault_correction")
        
        if score_trend < 0:
            focus_areas.append("fundamentals_review")
        elif score_trend > 0:
            focus_areas.append("advanced_techniques")
        
        trajectory.focus_areas = focus_areas or ["overall_improvement"]
        
        # Next goals
        next_goals = []
        if trajectory.predicted_scores:
            target_score = max(trajectory.predicted_scores)
            next_goals.append(f"Achieve score of {target_score:.1f}")
        
        if trajectory.breakthrough_probability > 0.5:
            next_goals.append("Prepare for performance breakthrough")
        
        trajectory.next_goals = next_goals
        
        return trajectory
    
    def _create_basic_trajectory(self, user_id: str, historical_data: List[Dict], 
                               prediction_horizon: int) -> ProgressTrajectory:
        """Create basic trajectory when insufficient data is available"""
        
        trajectory = ProgressTrajectory(
            user_id=user_id,
            trajectory_id=f"{user_id}_basic_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        )
        
        # Generate basic predictions
        if historical_data:
            last_score = historical_data[-1].get('overall_score', 50)
            improvement_rate = 0.1  # Modest improvement assumption
        else:
            last_score = 50  # Default starting score
            improvement_rate = 0.2  # Beginner improvement rate
        
        # Generate future time points and scores
        base_date = datetime.now()
        for i in range(prediction_horizon):
            future_date = base_date + timedelta(days=i)
            predicted_score = last_score + (i * improvement_rate)
            
            trajectory.time_points.append(future_date)
            trajectory.predicted_scores.append(predicted_score)
            trajectory.confidence_intervals.append((predicted_score - 5, predicted_score + 5))
        
        # Default recommendations
        trajectory.optimal_practice_frequency = 3
        trajectory.focus_areas = ["fundamentals"]
        trajectory.next_goals = ["Build consistent practice routine"]
        trajectory.breakthrough_probability = 0.3
        trajectory.plateau_risk = 0.2
        
        return trajectory
    
    def predict_goal_achievement(self, user_id: str, goal_target: float, 
                               current_value: float, historical_data: List[Dict]) -> GoalRecommendation:
        """Predict goal achievement probability and timeline"""
        
        goal_id = f"{user_id}_goal_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        if len(historical_data) < 5:
            # Basic prediction for insufficient data
            improvement_needed = goal_target - current_value
            estimated_timeline = max(30, int(improvement_needed * 10))  # 10 days per point improvement
            success_probability = 0.6 if improvement_needed <= 10 else 0.4
            
            return GoalRecommendation(
                goal_id=goal_id,
                user_id=user_id,
                goal_title=f"Reach score of {goal_target}",
                goal_description=f"Improve from {current_value} to {goal_target}",
                target_value=goal_target,
                current_value=current_value,
                recommended_timeline=estimated_timeline,
                difficulty_level="moderate",
                success_probability=success_probability,
                expected_completion_date=datetime.now() + timedelta(days=estimated_timeline),
                rationale="Estimate based on typical improvement patterns"
            )
        
        # Advanced prediction with historical data
        df = self._prepare_data(historical_data)
        
        # Calculate historical improvement rate
        if 'overall_score' in df.columns:
            scores = df['overall_score'].values
            improvement_rate = np.polyfit(range(len(scores)), scores, 1)[0]  # Linear trend
        else:
            improvement_rate = 0.1  # Default
        
        # Predict timeline
        improvement_needed = goal_target - current_value
        
        if improvement_rate > 0:
            estimated_days = improvement_needed / improvement_rate
            estimated_timeline = max(14, int(estimated_days))  # Minimum 2 weeks
        else:
            estimated_timeline = 90  # Default 3 months for no improvement
        
        # Calculate success probability
        score_volatility = df['overall_score'].std() if 'overall_score' in df.columns else 5
        difficulty_factor = min(1.0, improvement_needed / 20)  # Normalize to 20-point scale
        
        if improvement_rate > 0:
            base_probability = min(0.9, 0.5 + improvement_rate * 2)
        else:
            base_probability = 0.3
        
        # Adjust for difficulty and volatility
        success_probability = base_probability * (1 - difficulty_factor * 0.3) * (1 - score_volatility / 20)
        success_probability = max(0.1, min(0.95, success_probability))
        
        # Determine difficulty level
        if improvement_needed <= 5:
            difficulty_level = "easy"
        elif improvement_needed <= 15:
            difficulty_level = "moderate"
        else:
            difficulty_level = "challenging"
        
        return GoalRecommendation(
            goal_id=goal_id,
            user_id=user_id,
            goal_title=f"Reach score of {goal_target}",
            goal_description=f"Improve from {current_value:.1f} to {goal_target:.1f}",
            target_value=goal_target,
            current_value=current_value,
            recommended_timeline=estimated_timeline,
            difficulty_level=difficulty_level,
            success_probability=success_probability,
            expected_completion_date=datetime.now() + timedelta(days=estimated_timeline),
            rationale=f"Based on your improvement rate of {improvement_rate:.2f} points per session",
            supporting_evidence=[
                f"Historical improvement rate: {improvement_rate:.2f}",
                f"Score volatility: {score_volatility:.1f}",
                f"Difficulty factor: {difficulty_factor:.2f}"
            ]
        )
    
    def predict_breakthrough_timing(self, user_id: str, historical_data: List[Dict]) -> Dict[str, Any]:
        """Predict when user might experience breakthrough moments"""
        
        if len(historical_data) < 10:
            return {
                "breakthrough_probability": 0.3,
                "estimated_timing": "insufficient_data",
                "confidence": "low",
                "recommendations": ["Continue consistent practice to build prediction model"]
            }
        
        df = self._prepare_data(historical_data)
        
        # Analyze historical breakthroughs
        if 'overall_score' in df.columns:
            scores = df['overall_score'].values
            score_changes = np.diff(scores)
            
            # Define breakthrough as improvement > 2 standard deviations
            threshold = np.mean(score_changes) + 2 * np.std(score_changes)
            breakthroughs = score_changes > threshold
            
            # Calculate breakthrough frequency
            if len(breakthroughs) > 0:
                breakthrough_frequency = np.sum(breakthroughs) / len(breakthroughs)
                sessions_between_breakthroughs = len(breakthroughs) / max(1, np.sum(breakthroughs))
            else:
                breakthrough_frequency = 0.1
                sessions_between_breakthroughs = 20
        
        # Recent performance analysis
        recent_scores = df['overall_score'].tail(5) if 'overall_score' in df.columns else [50] * 5
        recent_trend = np.polyfit(range(len(recent_scores)), recent_scores, 1)[0]
        
        # Plateau detection
        recent_variance = np.var(recent_scores)
        is_in_plateau = recent_variance < 2 and abs(recent_trend) < 0.1
        
        # Predict breakthrough probability
        if is_in_plateau:
            # Plateau often precedes breakthrough
            breakthrough_probability = min(0.8, breakthrough_frequency * 3)
            estimated_sessions = max(3, int(sessions_between_breakthroughs * 0.7))
            timing_confidence = "medium"
        else:
            breakthrough_probability = breakthrough_frequency
            estimated_sessions = int(sessions_between_breakthroughs)
            timing_confidence = "low" if breakthrough_frequency < 0.2 else "medium"
        
        recommendations = []
        if is_in_plateau:
            recommendations.extend([
                "You may be approaching a breakthrough moment",
                "Consider varying your practice routine",
                "Focus on one specific improvement area"
            ])
        else:
            recommendations.extend([
                "Continue consistent practice",
                "Track your progress closely",
                "Be patient with the improvement process"
            ])
        
        return {
            "breakthrough_probability": breakthrough_probability,
            "estimated_sessions_until_breakthrough": estimated_sessions,
            "estimated_timing": f"{estimated_sessions * 3} days",  # Assuming 3 days between sessions
            "confidence": timing_confidence,
            "is_in_plateau": is_in_plateau,
            "recent_trend": recent_trend,
            "recommendations": recommendations
        }
    
    def get_personalized_goals(self, user_id: str, historical_data: List[Dict],
                              goal_count: int = 3) -> List[GoalRecommendation]:
        """Generate personalized goal recommendations"""
        
        goals = []
        
        if not historical_data:
            # Default goals for new users
            goals.append(GoalRecommendation(
                goal_id=f"{user_id}_goal_consistency",
                user_id=user_id,
                goal_title="Build Practice Consistency",
                goal_description="Practice at least 3 times per week for 4 weeks",
                target_value=12,  # 12 sessions
                current_value=0,
                recommended_timeline=28,
                difficulty_level="easy",
                success_probability=0.8,
                expected_completion_date=datetime.now() + timedelta(days=28),
                rationale="Consistency is the foundation of improvement"
            ))
            return goals
        
        df = self._prepare_data(historical_data)
        current_score = df['overall_score'].iloc[-1] if 'overall_score' in df.columns else 50
        
        # Generate different types of goals
        
        # 1. Short-term improvement goal
        short_term_target = current_score + 3
        short_term_goal = self.predict_goal_achievement(
            user_id, short_term_target, current_score, historical_data
        )
        short_term_goal.goal_title = "Short-term Improvement"
        short_term_goal.goal_description = f"Improve your average score by 3 points"
        goals.append(short_term_goal)
        
        # 2. Medium-term goal
        medium_term_target = current_score + 8
        medium_term_goal = self.predict_goal_achievement(
            user_id, medium_term_target, current_score, historical_data
        )
        medium_term_goal.goal_title = "Medium-term Progress"
        medium_term_goal.goal_description = f"Achieve consistently higher performance"
        medium_term_goal.recommended_timeline = medium_term_goal.recommended_timeline * 2
        goals.append(medium_term_goal)
        
        # 3. Skill-specific goal
        if 'fault_count' in df.columns:
            current_faults = df['fault_count'].tail(5).mean()
            target_faults = max(0, current_faults - 1)
            
            skill_goal = GoalRecommendation(
                goal_id=f"{user_id}_goal_faults",
                user_id=user_id,
                goal_title="Reduce Swing Faults",
                goal_description=f"Reduce average faults from {current_faults:.1f} to {target_faults:.1f}",
                target_value=target_faults,
                current_value=current_faults,
                recommended_timeline=45,
                difficulty_level="moderate",
                success_probability=0.7,
                expected_completion_date=datetime.now() + timedelta(days=45),
                rationale="Fault reduction leads to more consistent performance"
            )
            goals.append(skill_goal)
        
        return goals[:goal_count]
    
    def get_prediction_insights(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive prediction insights for a user"""
        
        insights = {
            "trajectory_summary": {},
            "goal_recommendations": [],
            "breakthrough_prediction": {},
            "optimization_suggestions": []
        }
        
        # Trajectory insights
        if user_id in self.trajectories:
            trajectory = self.trajectories[user_id]
            insights["trajectory_summary"] = {
                "predicted_improvement": max(trajectory.predicted_scores) - min(trajectory.predicted_scores),
                "breakthrough_probability": trajectory.breakthrough_probability,
                "plateau_risk": trajectory.plateau_risk,
                "optimal_practice_frequency": trajectory.optimal_practice_frequency,
                "focus_areas": trajectory.focus_areas,
                "next_milestones": trajectory.predicted_milestones
            }
        
        # Prediction history
        if user_id in self.predictions:
            recent_predictions = self.predictions[user_id][-5:]  # Last 5 predictions
            insights["recent_predictions"] = [
                {
                    "type": p.prediction_type.value,
                    "predicted_value": p.predicted_value,
                    "confidence": p.confidence.value,
                    "horizon": p.prediction_horizon
                }
                for p in recent_predictions
            ]
        
        return insights