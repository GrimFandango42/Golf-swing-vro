"""
User Learning Engine - Core Intelligence for Adaptive Coaching

This module implements the central learning engine that builds comprehensive
user profiles, tracks improvement patterns, and adapts coaching strategies
based on individual user behavior and preferences.

The engine learns:
- User swing patterns and common faults
- Coaching effectiveness and response preferences
- Learning style and pace preferences
- Progress patterns and improvement trajectories
- Emotional states and motivation triggers
- Session context and conversation history

Key Features:
- Real-time learning from user interactions
- Multi-dimensional user profiling
- Coaching effectiveness tracking
- Personalized adaptation strategies
- Long-term memory and context retention
"""

import json
import logging
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
import sqlite3
import pickle
from collections import defaultdict, deque

logger = logging.getLogger(__name__)

class LearningStyle(Enum):
    """Different learning style preferences"""
    VISUAL = "visual"
    ANALYTICAL = "analytical"
    EXPERIENTIAL = "experiential"
    PATIENT = "patient"
    QUICK_TIPS = "quick_tips"
    DETAILED = "detailed"
    ENCOURAGING = "encouraging"
    DIRECT = "direct"

class MotivationType(Enum):
    """User motivation triggers"""
    ACHIEVEMENT = "achievement"
    PROGRESS = "progress"
    COMPETITION = "competition"
    MASTERY = "mastery"
    SOCIAL = "social"
    CHALLENGE = "challenge"
    RECOGNITION = "recognition"

class ImprovementPhase(Enum):
    """User improvement phases"""
    BEGINNER = "beginner"
    RAPID_IMPROVEMENT = "rapid_improvement"
    PLATEAU = "plateau"
    BREAKTHROUGH = "breakthrough"
    ADVANCED = "advanced"
    MAINTAINING = "maintaining"

@dataclass
class UserProfile:
    """Comprehensive user profile with learning characteristics"""
    user_id: str
    
    # Learning preferences
    learning_style: LearningStyle = LearningStyle.ANALYTICAL
    motivation_type: MotivationType = MotivationType.PROGRESS
    improvement_phase: ImprovementPhase = ImprovementPhase.BEGINNER
    
    # Coaching preferences (learned from interactions)
    preferred_feedback_length: str = "medium"  # short, medium, long
    preferred_technical_detail: str = "intermediate"  # basic, intermediate, advanced
    preferred_encouragement_level: str = "medium"  # low, medium, high
    
    # Performance patterns
    common_faults: List[str] = field(default_factory=list)
    improvement_areas: List[str] = field(default_factory=list)
    strength_areas: List[str] = field(default_factory=list)
    
    # Learning patterns
    best_learning_times: List[str] = field(default_factory=list)  # time of day
    typical_session_length: float = 0.0  # minutes
    attention_span: float = 0.0  # minutes
    
    # Emotional patterns
    frustration_triggers: List[str] = field(default_factory=list)
    motivation_triggers: List[str] = field(default_factory=list)
    celebration_preferences: List[str] = field(default_factory=list)
    
    # Progress patterns
    improvement_velocity: float = 0.0  # rate of improvement
    consistency_score: float = 0.0  # how consistent performance is
    breakthrough_indicators: List[str] = field(default_factory=list)
    
    # Context and memory
    conversation_context: Dict[str, Any] = field(default_factory=dict)
    recent_goals: List[str] = field(default_factory=list)
    achieved_milestones: List[str] = field(default_factory=list)
    
    # Metadata
    created_at: datetime = field(default_factory=datetime.now)
    last_updated: datetime = field(default_factory=datetime.now)
    total_sessions: int = 0
    total_interactions: int = 0

@dataclass
class CoachingInteraction:
    """Record of coaching interaction and effectiveness"""
    interaction_id: str
    user_id: str
    timestamp: datetime
    
    # Interaction context
    coaching_approach: str
    feedback_type: str
    message_sent: str
    user_response: str
    
    # Effectiveness metrics
    user_satisfaction: float = 0.0  # 0-1 inferred from response
    engagement_level: float = 0.0  # 0-1 based on response length/detail
    learning_occurred: bool = False  # whether user showed understanding
    behavior_change: bool = False  # whether user modified behavior
    
    # Context
    session_context: Dict[str, Any] = field(default_factory=dict)
    emotional_state: str = "neutral"
    swing_improvement: bool = False

@dataclass
class LearningPattern:
    """Identified learning pattern for a user"""
    pattern_id: str
    user_id: str
    pattern_type: str  # "fault_tendency", "improvement_pattern", "coaching_preference"
    
    # Pattern data
    pattern_data: Dict[str, Any] = field(default_factory=dict)
    confidence: float = 0.0  # 0-1 confidence in pattern
    occurrences: int = 0
    
    # Temporal data
    first_observed: datetime = field(default_factory=datetime.now)
    last_observed: datetime = field(default_factory=datetime.now)
    trend: str = "stable"  # "increasing", "decreasing", "stable"

class UserLearningEngine:
    """Main engine for learning user patterns and adapting coaching"""
    
    def __init__(self, db_path: str = "user_learning.db"):
        self.db_path = db_path
        self.user_profiles: Dict[str, UserProfile] = {}
        self.interaction_history: Dict[str, List[CoachingInteraction]] = defaultdict(list)
        self.learning_patterns: Dict[str, List[LearningPattern]] = defaultdict(list)
        
        # Learning parameters
        self.learning_rate = 0.1
        self.pattern_confidence_threshold = 0.7
        self.min_interactions_for_pattern = 5
        
        # Initialize database
        self._init_database()
        
        # Load existing profiles
        self._load_profiles()
        
        logger.info("User Learning Engine initialized")
    
    def _init_database(self):
        """Initialize SQLite database for persistent storage"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # User profiles table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_profiles (
                user_id TEXT PRIMARY KEY,
                profile_data BLOB,
                created_at TIMESTAMP,
                last_updated TIMESTAMP
            )
        ''')
        
        # Interactions table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS coaching_interactions (
                interaction_id TEXT PRIMARY KEY,
                user_id TEXT,
                timestamp TIMESTAMP,
                interaction_data BLOB,
                FOREIGN KEY (user_id) REFERENCES user_profiles (user_id)
            )
        ''')
        
        # Learning patterns table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS learning_patterns (
                pattern_id TEXT PRIMARY KEY,
                user_id TEXT,
                pattern_type TEXT,
                pattern_data BLOB,
                confidence REAL,
                created_at TIMESTAMP,
                last_updated TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES user_profiles (user_id)
            )
        ''')
        
        conn.commit()
        conn.close()
    
    def _load_profiles(self):
        """Load user profiles from database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('SELECT user_id, profile_data FROM user_profiles')
        rows = cursor.fetchall()
        
        for user_id, profile_data in rows:
            try:
                profile = pickle.loads(profile_data)
                self.user_profiles[user_id] = profile
            except Exception as e:
                logger.error(f"Error loading profile for user {user_id}: {e}")
        
        conn.close()
        logger.info(f"Loaded {len(self.user_profiles)} user profiles")
    
    def _save_profile(self, user_id: str):
        """Save user profile to database"""
        if user_id not in self.user_profiles:
            return
        
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        profile = self.user_profiles[user_id]
        profile.last_updated = datetime.now()
        profile_data = pickle.dumps(profile)
        
        cursor.execute('''
            INSERT OR REPLACE INTO user_profiles 
            (user_id, profile_data, created_at, last_updated) 
            VALUES (?, ?, ?, ?)
        ''', (user_id, profile_data, profile.created_at, profile.last_updated))
        
        conn.commit()
        conn.close()
    
    def get_or_create_profile(self, user_id: str) -> UserProfile:
        """Get existing profile or create new one"""
        if user_id not in self.user_profiles:
            self.user_profiles[user_id] = UserProfile(user_id=user_id)
            self._save_profile(user_id)
        
        return self.user_profiles[user_id]
    
    def record_interaction(self, user_id: str, coaching_approach: str, 
                          feedback_type: str, message_sent: str, 
                          user_response: str = "", session_context: Dict = None) -> str:
        """Record a coaching interaction for learning"""
        
        interaction_id = f"{user_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{len(self.interaction_history[user_id])}"
        
        interaction = CoachingInteraction(
            interaction_id=interaction_id,
            user_id=user_id,
            timestamp=datetime.now(),
            coaching_approach=coaching_approach,
            feedback_type=feedback_type,
            message_sent=message_sent,
            user_response=user_response,
            session_context=session_context or {}
        )
        
        # Analyze interaction effectiveness
        interaction = self._analyze_interaction_effectiveness(interaction)
        
        # Store interaction
        self.interaction_history[user_id].append(interaction)
        
        # Update user profile based on interaction
        self._update_profile_from_interaction(user_id, interaction)
        
        # Save to database
        self._save_interaction(interaction)
        
        return interaction_id
    
    def _analyze_interaction_effectiveness(self, interaction: CoachingInteraction) -> CoachingInteraction:
        """Analyze the effectiveness of a coaching interaction"""
        
        user_response = interaction.user_response.lower()
        
        # Analyze user satisfaction from response
        satisfaction_indicators = {
            "positive": ["thanks", "helpful", "great", "good", "better", "understand", "clear"],
            "negative": ["confused", "don't understand", "not helpful", "frustrated", "unclear"],
            "neutral": ["ok", "yes", "no", "maybe"]
        }
        
        positive_count = sum(1 for word in satisfaction_indicators["positive"] if word in user_response)
        negative_count = sum(1 for word in satisfaction_indicators["negative"] if word in user_response)
        
        if positive_count > negative_count:
            interaction.user_satisfaction = min(0.8 + (positive_count * 0.1), 1.0)
        elif negative_count > positive_count:
            interaction.user_satisfaction = max(0.2 - (negative_count * 0.1), 0.0)
        else:
            interaction.user_satisfaction = 0.5
        
        # Analyze engagement level
        response_length = len(user_response)
        if response_length > 50:
            interaction.engagement_level = min(0.8 + (response_length / 200), 1.0)
        else:
            interaction.engagement_level = response_length / 100
        
        # Detect learning indicators
        learning_indicators = ["i see", "now i understand", "that makes sense", "got it", "i'll try"]
        interaction.learning_occurred = any(indicator in user_response for indicator in learning_indicators)
        
        # Detect emotional state
        emotional_indicators = {
            "frustrated": ["frustrated", "angry", "annoyed", "difficult", "hard"],
            "excited": ["excited", "great", "awesome", "amazing", "love"],
            "confident": ["confident", "ready", "feel good", "better"],
            "confused": ["confused", "don't understand", "unclear", "lost"]
        }
        
        for emotion, indicators in emotional_indicators.items():
            if any(indicator in user_response for indicator in indicators):
                interaction.emotional_state = emotion
                break
        
        return interaction
    
    def _update_profile_from_interaction(self, user_id: str, interaction: CoachingInteraction):
        """Update user profile based on interaction results"""
        
        profile = self.get_or_create_profile(user_id)
        profile.total_interactions += 1
        
        # Update learning preferences based on effectiveness
        if interaction.user_satisfaction > 0.7:
            # This approach worked well
            if interaction.coaching_approach == "detailed":
                profile.preferred_technical_detail = "advanced"
            elif interaction.coaching_approach == "encouraging":
                profile.preferred_encouragement_level = "high"
            elif interaction.coaching_approach == "direct":
                profile.preferred_feedback_length = "short"
        
        # Update emotional patterns
        if interaction.emotional_state == "frustrated":
            context = interaction.session_context.get("topic", "general")
            if context not in profile.frustration_triggers:
                profile.frustration_triggers.append(context)
        
        if interaction.emotional_state == "excited" and interaction.learning_occurred:
            context = interaction.session_context.get("coaching_approach", "general")
            if context not in profile.motivation_triggers:
                profile.motivation_triggers.append(context)
        
        # Update conversation context
        profile.conversation_context = interaction.session_context
        
        # Save updated profile
        self._save_profile(user_id)
    
    def _save_interaction(self, interaction: CoachingInteraction):
        """Save interaction to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        interaction_data = pickle.dumps(interaction)
        
        cursor.execute('''
            INSERT INTO coaching_interactions 
            (interaction_id, user_id, timestamp, interaction_data) 
            VALUES (?, ?, ?, ?)
        ''', (interaction.interaction_id, interaction.user_id, interaction.timestamp, interaction_data))
        
        conn.commit()
        conn.close()
    
    def analyze_swing_patterns(self, user_id: str, swing_analysis: Dict) -> Dict[str, Any]:
        """Analyze swing patterns and update user profile"""
        
        profile = self.get_or_create_profile(user_id)
        
        # Extract fault patterns
        if "raw_detected_faults" in swing_analysis:
            for fault in swing_analysis["raw_detected_faults"]:
                fault_name = fault.get("fault_name", "")
                if fault_name and fault_name not in profile.common_faults:
                    profile.common_faults.append(fault_name)
        
        # Analyze improvement patterns
        session_context = swing_analysis.get("session_context", {})
        if session_context.get("improvement_observed"):
            improvement_area = session_context.get("improvement_area", "")
            if improvement_area and improvement_area not in profile.strength_areas:
                profile.strength_areas.append(improvement_area)
        
        # Update profile
        self._save_profile(user_id)
        
        # Return personalized insights
        return self._generate_pattern_insights(user_id, swing_analysis)
    
    def _generate_pattern_insights(self, user_id: str, swing_analysis: Dict) -> Dict[str, Any]:
        """Generate personalized insights based on user patterns"""
        
        profile = self.user_profiles[user_id]
        
        insights = {
            "personalized_feedback": [],
            "progress_recognition": [],
            "next_focus_areas": [],
            "coaching_adaptations": {}
        }
        
        # Personalized feedback based on learning style
        if profile.learning_style == LearningStyle.ANALYTICAL:
            insights["coaching_adaptations"]["include_data"] = True
            insights["coaching_adaptations"]["technical_detail"] = "high"
        
        # Progress recognition based on patterns
        if profile.common_faults:
            current_faults = [f.get("fault_name", "") for f in swing_analysis.get("raw_detected_faults", [])]
            improved_faults = [f for f in profile.common_faults if f not in current_faults]
            
            if improved_faults:
                insights["progress_recognition"] = [
                    f"Great progress! You've improved your {fault}" for fault in improved_faults
                ]
        
        # Next focus areas based on patterns
        if len(profile.common_faults) > 2:
            # Focus on most frequent fault
            fault_counts = {}
            for interaction in self.interaction_history[user_id]:
                for fault in interaction.session_context.get("faults", []):
                    fault_counts[fault] = fault_counts.get(fault, 0) + 1
            
            if fault_counts:
                most_common_fault = max(fault_counts, key=fault_counts.get)
                insights["next_focus_areas"].append(most_common_fault)
        
        return insights
    
    def get_coaching_recommendations(self, user_id: str, context: Dict = None) -> Dict[str, Any]:
        """Get personalized coaching recommendations"""
        
        profile = self.get_or_create_profile(user_id)
        
        recommendations = {
            "coaching_style": "encouraging_mentor",  # default
            "feedback_approach": "balanced",
            "session_duration": 15,  # minutes
            "focus_areas": [],
            "motivation_strategy": "progress_focused",
            "communication_style": {}
        }
        
        # Adapt based on learning style
        if profile.learning_style == LearningStyle.ANALYTICAL:
            recommendations["coaching_style"] = "technical_expert"
            recommendations["feedback_approach"] = "detailed"
        elif profile.learning_style == LearningStyle.ENCOURAGING:
            recommendations["coaching_style"] = "encouraging_mentor"
            recommendations["feedback_approach"] = "supportive"
        elif profile.learning_style == LearningStyle.DIRECT:
            recommendations["coaching_style"] = "competitive_trainer"
            recommendations["feedback_approach"] = "direct"
        
        # Adapt communication style
        recommendations["communication_style"] = {
            "length": profile.preferred_feedback_length,
            "technical_detail": profile.preferred_technical_detail,
            "encouragement_level": profile.preferred_encouragement_level
        }
        
        # Adapt session duration based on attention span
        if profile.attention_span > 0:
            recommendations["session_duration"] = min(profile.attention_span, 30)
        
        # Focus areas based on patterns
        if profile.common_faults:
            # Focus on most problematic fault
            recommendations["focus_areas"] = profile.common_faults[:3]
        
        return recommendations
    
    def predict_user_needs(self, user_id: str, current_context: Dict = None) -> Dict[str, Any]:
        """Predict what the user needs based on patterns"""
        
        profile = self.get_or_create_profile(user_id)
        recent_interactions = self.interaction_history[user_id][-5:]  # Last 5 interactions
        
        predictions = {
            "likely_questions": [],
            "potential_frustrations": [],
            "improvement_opportunities": [],
            "motivation_needs": [],
            "next_milestones": []
        }
        
        # Predict questions based on patterns
        if profile.common_faults:
            predictions["likely_questions"] = [
                f"How can I fix my {fault}?" for fault in profile.common_faults[:2]
            ]
        
        # Predict frustrations
        if profile.frustration_triggers:
            current_topic = current_context.get("topic", "") if current_context else ""
            if current_topic in profile.frustration_triggers:
                predictions["potential_frustrations"] = [
                    f"User may get frustrated with {current_topic} - provide extra encouragement"
                ]
        
        # Predict improvement opportunities
        if profile.improvement_areas:
            predictions["improvement_opportunities"] = [
                f"Focus on {area} for breakthrough" for area in profile.improvement_areas
            ]
        
        # Predict motivation needs
        if profile.motivation_type == MotivationType.PROGRESS:
            predictions["motivation_needs"] = ["Show progress and improvement"]
        elif profile.motivation_type == MotivationType.ACHIEVEMENT:
            predictions["motivation_needs"] = ["Set specific goals and celebrate achievements"]
        
        return predictions
    
    def update_user_feedback(self, user_id: str, interaction_id: str, 
                           feedback_type: str, feedback_data: Dict):
        """Update user feedback for learning"""
        
        # Find the interaction
        user_interactions = self.interaction_history[user_id]
        interaction = None
        for int_obj in user_interactions:
            if int_obj.interaction_id == interaction_id:
                interaction = int_obj
                break
        
        if not interaction:
            return
        
        # Update interaction based on feedback
        if feedback_type == "satisfaction":
            interaction.user_satisfaction = feedback_data.get("rating", 0.5)
        elif feedback_type == "helpful":
            interaction.learning_occurred = feedback_data.get("helpful", False)
        elif feedback_type == "behavior_change":
            interaction.behavior_change = feedback_data.get("changed", False)
        
        # Update profile based on feedback
        self._update_profile_from_interaction(user_id, interaction)
    
    def get_user_insights(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive insights about a user"""
        
        profile = self.get_or_create_profile(user_id)
        interactions = self.interaction_history[user_id]
        
        insights = {
            "profile_summary": {
                "learning_style": profile.learning_style.value,
                "motivation_type": profile.motivation_type.value,
                "improvement_phase": profile.improvement_phase.value,
                "total_interactions": profile.total_interactions,
                "total_sessions": profile.total_sessions
            },
            "learning_patterns": {
                "common_faults": profile.common_faults,
                "strength_areas": profile.strength_areas,
                "frustration_triggers": profile.frustration_triggers,
                "motivation_triggers": profile.motivation_triggers
            },
            "coaching_preferences": {
                "feedback_length": profile.preferred_feedback_length,
                "technical_detail": profile.preferred_technical_detail,
                "encouragement_level": profile.preferred_encouragement_level
            },
            "effectiveness_metrics": {
                "average_satisfaction": 0.0,
                "engagement_level": 0.0,
                "learning_rate": 0.0
            }
        }
        
        # Calculate effectiveness metrics
        if interactions:
            insights["effectiveness_metrics"]["average_satisfaction"] = sum(
                i.user_satisfaction for i in interactions
            ) / len(interactions)
            
            insights["effectiveness_metrics"]["engagement_level"] = sum(
                i.engagement_level for i in interactions
            ) / len(interactions)
            
            insights["effectiveness_metrics"]["learning_rate"] = sum(
                1 for i in interactions if i.learning_occurred
            ) / len(interactions)
        
        return insights
    
    def reset_user_profile(self, user_id: str):
        """Reset user profile for fresh start"""
        if user_id in self.user_profiles:
            del self.user_profiles[user_id]
        
        # Clear from database
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('DELETE FROM user_profiles WHERE user_id = ?', (user_id,))
        cursor.execute('DELETE FROM coaching_interactions WHERE user_id = ?', (user_id,))
        cursor.execute('DELETE FROM learning_patterns WHERE user_id = ?', (user_id,))
        conn.commit()
        conn.close()
        
        logger.info(f"Reset profile for user {user_id}")