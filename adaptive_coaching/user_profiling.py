"""
User Profiling System - Advanced Learning Style Detection

This module implements sophisticated user profiling and learning style detection
to understand how each user learns best. It analyzes behavior patterns, response
types, and interaction preferences to build comprehensive user profiles that
enable highly personalized coaching.

Key Features:
- Learning style detection and analysis
- Behavioral pattern profiling
- Preference inference from interactions
- Motivation type identification
- Communication style analysis
- Skill level assessment
- Progress pattern analysis
- Personality trait detection

The system creates a deep understanding of each user's unique learning
characteristics, enabling truly personalized coaching experiences.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
import json
import logging
from collections import defaultdict, Counter
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from scipy import stats
import re

logger = logging.getLogger(__name__)

class LearningStyle(Enum):
    """Learning style categories"""
    VISUAL = "visual"
    AUDITORY = "auditory"
    KINESTHETIC = "kinesthetic"
    ANALYTICAL = "analytical"
    GLOBAL = "global"
    SEQUENTIAL = "sequential"
    REFLECTIVE = "reflective"
    ACTIVE = "active"

class MotivationType(Enum):
    """User motivation types"""
    ACHIEVEMENT = "achievement"
    MASTERY = "mastery"
    COMPETITION = "competition"
    RECOGNITION = "recognition"
    PROGRESS = "progress"
    CHALLENGE = "challenge"
    SOCIAL = "social"
    INTRINSIC = "intrinsic"

class CommunicationStyle(Enum):
    """Communication preferences"""
    DIRECT = "direct"
    SUPPORTIVE = "supportive"
    ANALYTICAL = "analytical"
    ENCOURAGING = "encouraging"
    CHALLENGING = "challenging"
    PATIENT = "patient"
    ENERGETIC = "energetic"

class LearningPace(Enum):
    """Learning pace preferences"""
    SLOW = "slow"
    STEADY = "steady"
    FAST = "fast"
    VARIABLE = "variable"

class PersonalityTrait(Enum):
    """Personality traits affecting learning"""
    PERFECTIONIST = "perfectionist"
    PATIENT = "patient"
    IMPATIENT = "impatient"
    CONFIDENT = "confident"
    CAUTIOUS = "cautious"
    CURIOUS = "curious"
    FOCUSED = "focused"
    EASILY_DISTRACTED = "easily_distracted"

@dataclass
class UserProfile:
    """Comprehensive user profile"""
    user_id: str
    
    # Learning characteristics
    primary_learning_style: LearningStyle = LearningStyle.ANALYTICAL
    secondary_learning_style: Optional[LearningStyle] = None
    learning_style_confidence: float = 0.5
    
    # Motivation and goals
    motivation_type: MotivationType = MotivationType.PROGRESS
    motivation_strength: float = 0.5
    goal_orientation: str = "improvement"  # improvement, performance, mastery
    
    # Communication preferences
    communication_style: CommunicationStyle = CommunicationStyle.SUPPORTIVE
    preferred_feedback_style: str = "balanced"  # positive, constructive, balanced
    response_length_preference: str = "medium"  # short, medium, long
    
    # Learning preferences
    learning_pace: LearningPace = LearningPace.STEADY
    attention_span: float = 15.0  # minutes
    preferred_session_length: float = 20.0  # minutes
    
    # Technical preferences
    detail_preference: str = "moderate"  # minimal, moderate, detailed
    technical_depth: str = "intermediate"  # basic, intermediate, advanced
    explanation_style: str = "practical"  # theoretical, practical, mixed
    
    # Personality traits
    personality_traits: List[PersonalityTrait] = field(default_factory=list)
    confidence_level: float = 0.5
    frustration_tolerance: float = 0.5
    
    # Behavioral patterns
    typical_practice_times: List[str] = field(default_factory=list)
    consistency_score: float = 0.5
    engagement_patterns: Dict[str, float] = field(default_factory=dict)
    
    # Progress patterns
    improvement_velocity: float = 0.1
    plateau_frequency: float = 0.2
    breakthrough_indicators: List[str] = field(default_factory=list)
    
    # Social aspects
    coaching_relationship_preference: str = "collaborative"  # directive, collaborative, consultative
    feedback_timing_preference: str = "immediate"  # immediate, delayed, batch
    
    # Metadata
    profile_confidence: float = 0.5
    last_updated: datetime = field(default_factory=datetime.now)
    data_points_analyzed: int = 0

@dataclass
class LearningStyleIndicator:
    """Indicator for learning style detection"""
    indicator_type: str
    style: LearningStyle
    weight: float
    evidence: str
    confidence: float

@dataclass
class BehavioralPattern:
    """Detected behavioral pattern"""
    pattern_id: str
    user_id: str
    pattern_type: str
    description: str
    strength: float
    evidence: List[str] = field(default_factory=list)
    implications: List[str] = field(default_factory=list)

class UserProfilingSystem:
    """Main user profiling and learning style detection system"""
    
    def __init__(self):
        self.user_profiles: Dict[str, UserProfile] = {}
        
        # Learning style detection patterns
        self._initialize_detection_patterns()
        
        # Behavioral analysis
        self.behavioral_analyzer = BehavioralPatternAnalyzer()
        
        # Preference inference
        self.preference_inferrer = PreferenceInferrer()
        
        logger.info("User Profiling System initialized")
    
    def _initialize_detection_patterns(self):
        """Initialize patterns for learning style detection"""
        
        # Communication patterns that indicate learning styles
        self.communication_patterns = {
            LearningStyle.VISUAL: {
                "keywords": ["see", "picture", "imagine", "visualize", "show", "looks", "appears"],
                "phrases": ["i can see", "picture this", "looks like", "show me"],
                "preferences": ["demonstrations", "diagrams", "visual aids"]
            },
            LearningStyle.AUDITORY: {
                "keywords": ["hear", "sounds", "listen", "tell", "explain", "discuss"],
                "phrases": ["sounds good", "i hear you", "tell me more", "let's discuss"],
                "preferences": ["verbal explanations", "discussions", "audio feedback"]
            },
            LearningStyle.KINESTHETIC: {
                "keywords": ["feel", "touch", "hands-on", "practice", "try", "do"],
                "phrases": ["let me try", "i need to feel", "hands-on", "learn by doing"],
                "preferences": ["practice", "physical demonstrations", "trial and error"]
            },
            LearningStyle.ANALYTICAL: {
                "keywords": ["analyze", "data", "numbers", "metrics", "precise", "exact"],
                "phrases": ["what's the data", "show me the numbers", "how exactly"],
                "preferences": ["detailed analysis", "metrics", "step-by-step breakdown"]
            }
        }
        
        # Motivation indicators
        self.motivation_indicators = {
            MotivationType.ACHIEVEMENT: ["goal", "target", "achieve", "accomplish", "succeed"],
            MotivationType.MASTERY: ["perfect", "master", "understand", "learn", "improve"],
            MotivationType.COMPETITION: ["compete", "beat", "better than", "compare", "win"],
            MotivationType.RECOGNITION: ["praise", "acknowledge", "notice", "recognize", "appreciate"],
            MotivationType.PROGRESS: ["progress", "improve", "better", "development", "growth"],
            MotivationType.CHALLENGE: ["challenge", "difficult", "push", "harder", "test"]
        }
        
        # Response style indicators
        self.response_style_indicators = {
            "detailed_seeker": ["why", "how", "explain more", "details", "breakdown"],
            "quick_learner": ["got it", "understand", "quick", "fast", "next"],
            "methodical": ["step by step", "slowly", "one at a time", "carefully"],
            "impatient": ["faster", "quickly", "hurry", "speed up", "move on"]
        }
    
    def analyze_user(self, user_id: str, interaction_data: List[Dict],
                    performance_data: List[Dict] = None) -> UserProfile:
        """Comprehensive user analysis to build profile"""
        
        if user_id in self.user_profiles:
            profile = self.user_profiles[user_id]
        else:
            profile = UserProfile(user_id=user_id)
        
        # Analyze communication patterns
        learning_style_indicators = self._detect_learning_style(interaction_data)
        profile = self._update_learning_style(profile, learning_style_indicators)
        
        # Analyze motivation patterns
        motivation_indicators = self._detect_motivation_type(interaction_data)
        profile = self._update_motivation_type(profile, motivation_indicators)
        
        # Analyze communication preferences
        communication_prefs = self._analyze_communication_preferences(interaction_data)
        profile = self._update_communication_style(profile, communication_prefs)
        
        # Analyze behavioral patterns
        behavioral_patterns = self.behavioral_analyzer.analyze_patterns(user_id, interaction_data)
        profile = self._update_behavioral_patterns(profile, behavioral_patterns)
        
        # Analyze performance patterns if available
        if performance_data:
            performance_patterns = self._analyze_performance_patterns(performance_data)
            profile = self._update_performance_patterns(profile, performance_patterns)
        
        # Infer additional preferences
        inferred_prefs = self.preference_inferrer.infer_preferences(interaction_data, performance_data)
        profile = self._update_inferred_preferences(profile, inferred_prefs)
        
        # Calculate overall profile confidence
        profile.profile_confidence = self._calculate_profile_confidence(profile, len(interaction_data))
        profile.data_points_analyzed = len(interaction_data)
        profile.last_updated = datetime.now()
        
        # Store updated profile
        self.user_profiles[user_id] = profile
        
        return profile
    
    def _detect_learning_style(self, interaction_data: List[Dict]) -> List[LearningStyleIndicator]:
        """Detect learning style from interaction patterns"""
        
        indicators = []
        
        # Analyze user messages for learning style keywords
        user_messages = [item.get('user_message', '') for item in interaction_data if item.get('user_message')]
        all_text = ' '.join(user_messages).lower()
        
        for style, patterns in self.communication_patterns.items():
            score = 0.0
            evidence = []
            
            # Count keyword matches
            keyword_matches = sum(1 for keyword in patterns['keywords'] if keyword in all_text)
            if keyword_matches > 0:
                score += keyword_matches / len(patterns['keywords']) * 0.4
                evidence.append(f"Used {keyword_matches} {style.value} keywords")
            
            # Count phrase matches
            phrase_matches = sum(1 for phrase in patterns['phrases'] if phrase in all_text)
            if phrase_matches > 0:
                score += phrase_matches / len(patterns['phrases']) * 0.3
                evidence.append(f"Used {phrase_matches} {style.value} phrases")
            
            # Analyze response patterns
            response_pattern_score = self._analyze_response_patterns(interaction_data, style)
            score += response_pattern_score * 0.3
            
            if score > 0.1:  # Minimum threshold
                indicators.append(LearningStyleIndicator(
                    indicator_type="communication_analysis",
                    style=style,
                    weight=score,
                    evidence='; '.join(evidence),
                    confidence=min(score * 2, 1.0)
                ))
        
        return sorted(indicators, key=lambda x: x.weight, reverse=True)
    
    def _analyze_response_patterns(self, interaction_data: List[Dict], style: LearningStyle) -> float:
        """Analyze response patterns for learning style indicators"""
        
        score = 0.0
        
        for interaction in interaction_data:
            user_response = interaction.get('user_response', '').lower()
            
            # Response length patterns
            response_length = len(user_response.split())
            
            if style == LearningStyle.ANALYTICAL:
                # Analytical learners tend to ask detailed questions
                if any(word in user_response for word in ['why', 'how', 'explain', 'detail']):
                    score += 0.1
                if response_length > 20:  # Longer, more detailed responses
                    score += 0.05
            
            elif style == LearningStyle.VISUAL:
                # Visual learners ask for demonstrations
                if any(word in user_response for word in ['show', 'see', 'picture', 'demo']):
                    score += 0.1
            
            elif style == LearningStyle.KINESTHETIC:
                # Kinesthetic learners want to try things
                if any(word in user_response for word in ['try', 'practice', 'do', 'feel']):
                    score += 0.1
            
            # Response timing patterns (if available)
            response_time = interaction.get('response_time', 0)
            if style == LearningStyle.REFLECTIVE and response_time > 30:
                score += 0.05  # Reflective learners take more time
            elif style == LearningStyle.ACTIVE and response_time < 10:
                score += 0.05  # Active learners respond quickly
        
        return min(score, 0.5)  # Cap at 0.5
    
    def _detect_motivation_type(self, interaction_data: List[Dict]) -> Dict[MotivationType, float]:
        """Detect primary motivation type from interactions"""
        
        motivation_scores = defaultdict(float)
        user_messages = [item.get('user_message', '') for item in interaction_data if item.get('user_message')]
        all_text = ' '.join(user_messages).lower()
        
        for motivation_type, keywords in self.motivation_indicators.items():
            keyword_count = sum(1 for keyword in keywords if keyword in all_text)
            if keyword_count > 0:
                motivation_scores[motivation_type] = keyword_count / len(keywords)
        
        # Analyze goal-setting behavior
        goal_mentions = sum(1 for text in user_messages if any(word in text.lower() for word in ['goal', 'target', 'want to', 'hope to']))
        if goal_mentions > 0:
            motivation_scores[MotivationType.ACHIEVEMENT] += 0.2
        
        # Analyze question types
        how_questions = sum(1 for text in user_messages if 'how' in text.lower())
        why_questions = sum(1 for text in user_messages if 'why' in text.lower())
        
        if how_questions > why_questions:
            motivation_scores[MotivationType.PROGRESS] += 0.1
        else:
            motivation_scores[MotivationType.MASTERY] += 0.1
        
        return dict(motivation_scores)
    
    def _analyze_communication_preferences(self, interaction_data: List[Dict]) -> Dict[str, Any]:
        """Analyze communication style preferences"""
        
        preferences = {
            'response_length': 'medium',
            'detail_level': 'moderate',
            'feedback_style': 'balanced',
            'interaction_style': 'collaborative'
        }
        
        user_messages = [item.get('user_message', '') for item in interaction_data if item.get('user_message')]
        
        # Analyze response length preference
        total_length = sum(len(msg.split()) for msg in user_messages)
        avg_length = total_length / max(len(user_messages), 1)
        
        if avg_length < 5:
            preferences['response_length'] = 'short'
        elif avg_length > 20:
            preferences['response_length'] = 'long'
        
        # Analyze detail-seeking behavior
        detail_indicators = ['explain', 'why', 'how', 'detail', 'breakdown', 'specific']
        detail_count = sum(1 for msg in user_messages for indicator in detail_indicators if indicator in msg.lower())
        
        if detail_count > len(user_messages) * 0.3:
            preferences['detail_level'] = 'detailed'
        elif detail_count < len(user_messages) * 0.1:
            preferences['detail_level'] = 'minimal'
        
        # Analyze feedback style preference
        positive_seeking = sum(1 for msg in user_messages if any(word in msg.lower() for word in ['good', 'better', 'progress', 'improve']))
        critique_seeking = sum(1 for msg in user_messages if any(word in msg.lower() for word in ['wrong', 'fix', 'problem', 'mistake']))
        
        if positive_seeking > critique_seeking * 2:
            preferences['feedback_style'] = 'positive'
        elif critique_seeking > positive_seeking:
            preferences['feedback_style'] = 'constructive'
        
        return preferences
    
    def _update_learning_style(self, profile: UserProfile, indicators: List[LearningStyleIndicator]) -> UserProfile:
        """Update learning style based on indicators"""
        
        if not indicators:
            return profile
        
        # Primary learning style
        primary_indicator = indicators[0]
        profile.primary_learning_style = primary_indicator.style
        profile.learning_style_confidence = primary_indicator.confidence
        
        # Secondary learning style
        if len(indicators) > 1 and indicators[1].weight > 0.3:
            profile.secondary_learning_style = indicators[1].style
        
        return profile
    
    def _update_motivation_type(self, profile: UserProfile, motivation_scores: Dict[MotivationType, float]) -> UserProfile:
        """Update motivation type based on detected patterns"""
        
        if motivation_scores:
            primary_motivation = max(motivation_scores.keys(), key=lambda k: motivation_scores[k])
            profile.motivation_type = primary_motivation
            profile.motivation_strength = motivation_scores[primary_motivation]
        
        return profile
    
    def _update_communication_style(self, profile: UserProfile, preferences: Dict[str, Any]) -> UserProfile:
        """Update communication style preferences"""
        
        profile.response_length_preference = preferences.get('response_length', 'medium')
        profile.detail_preference = preferences.get('detail_level', 'moderate')
        profile.preferred_feedback_style = preferences.get('feedback_style', 'balanced')
        
        # Infer communication style
        if preferences.get('detail_level') == 'detailed':
            profile.communication_style = CommunicationStyle.ANALYTICAL
        elif preferences.get('feedback_style') == 'positive':
            profile.communication_style = CommunicationStyle.ENCOURAGING
        elif preferences.get('response_length') == 'short':
            profile.communication_style = CommunicationStyle.DIRECT
        else:
            profile.communication_style = CommunicationStyle.SUPPORTIVE
        
        return profile
    
    def _update_behavioral_patterns(self, profile: UserProfile, patterns: List[BehavioralPattern]) -> UserProfile:
        """Update profile based on behavioral patterns"""
        
        for pattern in patterns:
            if pattern.pattern_type == "consistency":
                profile.consistency_score = pattern.strength
            elif pattern.pattern_type == "engagement":
                profile.engagement_patterns[pattern.description] = pattern.strength
            elif pattern.pattern_type == "learning_pace":
                if pattern.strength > 0.7:
                    profile.learning_pace = LearningPace.FAST
                elif pattern.strength < 0.3:
                    profile.learning_pace = LearningPace.SLOW
                else:
                    profile.learning_pace = LearningPace.STEADY
        
        return profile
    
    def _analyze_performance_patterns(self, performance_data: List[Dict]) -> Dict[str, Any]:
        """Analyze performance data for patterns"""
        
        patterns = {}
        
        if not performance_data:
            return patterns
        
        # Convert to DataFrame for analysis
        df = pd.DataFrame(performance_data)
        
        # Improvement velocity
        if 'overall_score' in df.columns:
            scores = df['overall_score'].values
            if len(scores) > 1:
                # Calculate linear trend
                x = np.arange(len(scores))
                slope, intercept, r_value, p_value, std_err = stats.linregress(x, scores)
                patterns['improvement_velocity'] = max(0, slope)
                patterns['improvement_consistency'] = abs(r_value)
        
        # Plateau detection
        if 'overall_score' in df.columns and len(df) > 5:
            rolling_var = df['overall_score'].rolling(window=5).var()
            plateau_periods = (rolling_var < 1.0).sum() / len(rolling_var)
            patterns['plateau_frequency'] = plateau_periods
        
        # Breakthrough detection
        if 'overall_score' in df.columns:
            score_changes = df['overall_score'].diff()
            breakthrough_threshold = score_changes.std() * 2
            breakthroughs = (score_changes > breakthrough_threshold).sum()
            patterns['breakthrough_frequency'] = breakthroughs / len(df)
        
        return patterns
    
    def _update_performance_patterns(self, profile: UserProfile, patterns: Dict[str, Any]) -> UserProfile:
        """Update profile with performance patterns"""
        
        if 'improvement_velocity' in patterns:
            profile.improvement_velocity = patterns['improvement_velocity']
        
        if 'plateau_frequency' in patterns:
            profile.plateau_frequency = patterns['plateau_frequency']
        
        if 'breakthrough_frequency' in patterns:
            if patterns['breakthrough_frequency'] > 0.1:
                profile.breakthrough_indicators.append("frequent_breakthroughs")
        
        return profile
    
    def _update_inferred_preferences(self, profile: UserProfile, preferences: Dict[str, Any]) -> UserProfile:
        """Update profile with inferred preferences"""
        
        if 'attention_span' in preferences:
            profile.attention_span = preferences['attention_span']
        
        if 'session_length' in preferences:
            profile.preferred_session_length = preferences['session_length']
        
        if 'personality_traits' in preferences:
            profile.personality_traits = preferences['personality_traits']
        
        return profile
    
    def _calculate_profile_confidence(self, profile: UserProfile, data_points: int) -> float:
        """Calculate overall confidence in the profile"""
        
        confidence = 0.0
        
        # Base confidence from data quantity
        data_confidence = min(1.0, data_points / 20)  # Full confidence at 20+ interactions
        confidence += data_confidence * 0.4
        
        # Learning style confidence
        confidence += profile.learning_style_confidence * 0.3
        
        # Motivation confidence
        confidence += profile.motivation_strength * 0.2
        
        # Behavioral pattern confidence
        if profile.consistency_score > 0:
            confidence += 0.1
        
        return min(1.0, confidence)
    
    def get_coaching_recommendations(self, user_id: str) -> Dict[str, Any]:
        """Get coaching recommendations based on user profile"""
        
        if user_id not in self.user_profiles:
            return {"error": "User profile not found"}
        
        profile = self.user_profiles[user_id]
        
        recommendations = {
            "coaching_approach": self._recommend_coaching_approach(profile),
            "communication_style": self._recommend_communication_style(profile),
            "session_structure": self._recommend_session_structure(profile),
            "motivation_strategy": self._recommend_motivation_strategy(profile),
            "learning_optimization": self._recommend_learning_optimization(profile)
        }
        
        return recommendations
    
    def _recommend_coaching_approach(self, profile: UserProfile) -> Dict[str, Any]:
        """Recommend coaching approach based on profile"""
        
        approach = {
            "primary_style": "balanced",
            "emphasis": "improvement",
            "pacing": "steady",
            "interaction_style": "collaborative"
        }
        
        # Adjust based on learning style
        if profile.primary_learning_style == LearningStyle.ANALYTICAL:
            approach["primary_style"] = "technical"
            approach["emphasis"] = "analysis"
        elif profile.primary_learning_style == LearningStyle.VISUAL:
            approach["primary_style"] = "demonstrative"
            approach["emphasis"] = "visualization"
        elif profile.primary_learning_style == LearningStyle.KINESTHETIC:
            approach["primary_style"] = "experiential"
            approach["emphasis"] = "practice"
        
        # Adjust based on motivation type
        if profile.motivation_type == MotivationType.ACHIEVEMENT:
            approach["emphasis"] = "goal_achievement"
        elif profile.motivation_type == MotivationType.MASTERY:
            approach["emphasis"] = "skill_development"
        elif profile.motivation_type == MotivationType.COMPETITION:
            approach["emphasis"] = "performance"
        
        # Adjust pacing
        if profile.learning_pace == LearningPace.FAST:
            approach["pacing"] = "accelerated"
        elif profile.learning_pace == LearningPace.SLOW:
            approach["pacing"] = "patient"
        
        return approach
    
    def _recommend_communication_style(self, profile: UserProfile) -> Dict[str, Any]:
        """Recommend communication style"""
        
        style = {
            "tone": "supportive",
            "detail_level": profile.detail_preference,
            "response_length": profile.response_length_preference,
            "feedback_approach": profile.preferred_feedback_style
        }
        
        # Adjust tone based on communication style
        if profile.communication_style == CommunicationStyle.DIRECT:
            style["tone"] = "direct"
        elif profile.communication_style == CommunicationStyle.ENCOURAGING:
            style["tone"] = "enthusiastic"
        elif profile.communication_style == CommunicationStyle.ANALYTICAL:
            style["tone"] = "professional"
        
        return style
    
    def _recommend_session_structure(self, profile: UserProfile) -> Dict[str, Any]:
        """Recommend session structure"""
        
        structure = {
            "ideal_length": profile.preferred_session_length,
            "break_frequency": 1,  # breaks per session
            "focus_areas": 2,  # number of focus areas per session
            "practice_ratio": 0.7  # ratio of practice to instruction
        }
        
        # Adjust based on attention span
        if profile.attention_span < 10:
            structure["break_frequency"] = 2
            structure["focus_areas"] = 1
        elif profile.attention_span > 20:
            structure["break_frequency"] = 0
            structure["focus_areas"] = 3
        
        # Adjust based on learning style
        if profile.primary_learning_style == LearningStyle.KINESTHETIC:
            structure["practice_ratio"] = 0.8
        elif profile.primary_learning_style == LearningStyle.ANALYTICAL:
            structure["practice_ratio"] = 0.5  # More instruction
        
        return structure
    
    def _recommend_motivation_strategy(self, profile: UserProfile) -> Dict[str, Any]:
        """Recommend motivation strategy"""
        
        strategy = {
            "primary_motivator": "progress",
            "recognition_frequency": "moderate",
            "goal_setting_approach": "collaborative",
            "challenge_level": "moderate"
        }
        
        # Adjust based on motivation type
        if profile.motivation_type == MotivationType.ACHIEVEMENT:
            strategy["primary_motivator"] = "goals"
            strategy["goal_setting_approach"] = "specific"
        elif profile.motivation_type == MotivationType.RECOGNITION:
            strategy["recognition_frequency"] = "high"
        elif profile.motivation_type == MotivationType.CHALLENGE:
            strategy["challenge_level"] = "high"
        elif profile.motivation_type == MotivationType.MASTERY:
            strategy["primary_motivator"] = "understanding"
        
        return strategy
    
    def _recommend_learning_optimization(self, profile: UserProfile) -> Dict[str, Any]:
        """Recommend learning optimization strategies"""
        
        optimization = {
            "repetition_strategy": "spaced",
            "difficulty_progression": "gradual",
            "feedback_timing": "immediate",
            "practice_variety": "moderate"
        }
        
        # Adjust based on learning patterns
        if profile.improvement_velocity > 0.5:
            optimization["difficulty_progression"] = "accelerated"
        elif profile.improvement_velocity < 0.1:
            optimization["difficulty_progression"] = "very_gradual"
        
        if profile.plateau_frequency > 0.3:
            optimization["practice_variety"] = "high"
            optimization["repetition_strategy"] = "varied"
        
        return optimization
    
    def get_profile_summary(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive profile summary"""
        
        if user_id not in self.user_profiles:
            return {"error": "User profile not found"}
        
        profile = self.user_profiles[user_id]
        
        summary = {
            "user_id": user_id,
            "profile_confidence": profile.profile_confidence,
            "learning_characteristics": {
                "primary_learning_style": profile.primary_learning_style.value,
                "secondary_learning_style": profile.secondary_learning_style.value if profile.secondary_learning_style else None,
                "learning_pace": profile.learning_pace.value,
                "attention_span": profile.attention_span
            },
            "motivation_profile": {
                "motivation_type": profile.motivation_type.value,
                "motivation_strength": profile.motivation_strength,
                "goal_orientation": profile.goal_orientation
            },
            "communication_preferences": {
                "communication_style": profile.communication_style.value,
                "response_length": profile.response_length_preference,
                "detail_level": profile.detail_preference,
                "feedback_style": profile.preferred_feedback_style
            },
            "behavioral_patterns": {
                "consistency_score": profile.consistency_score,
                "improvement_velocity": profile.improvement_velocity,
                "plateau_frequency": profile.plateau_frequency
            },
            "personality_traits": [trait.value for trait in profile.personality_traits],
            "metadata": {
                "last_updated": profile.last_updated.isoformat(),
                "data_points_analyzed": profile.data_points_analyzed
            }
        }
        
        return summary

class BehavioralPatternAnalyzer:
    """Analyzer for behavioral patterns"""
    
    def analyze_patterns(self, user_id: str, interaction_data: List[Dict]) -> List[BehavioralPattern]:
        """Analyze behavioral patterns from interaction data"""
        
        patterns = []
        
        # Consistency pattern
        consistency_pattern = self._analyze_consistency(user_id, interaction_data)
        if consistency_pattern:
            patterns.append(consistency_pattern)
        
        # Engagement pattern
        engagement_pattern = self._analyze_engagement(user_id, interaction_data)
        if engagement_pattern:
            patterns.append(engagement_pattern)
        
        # Learning pace pattern
        pace_pattern = self._analyze_learning_pace(user_id, interaction_data)
        if pace_pattern:
            patterns.append(pace_pattern)
        
        return patterns
    
    def _analyze_consistency(self, user_id: str, interaction_data: List[Dict]) -> Optional[BehavioralPattern]:
        """Analyze consistency patterns"""
        
        if len(interaction_data) < 5:
            return None
        
        # Analyze session timing consistency
        timestamps = [datetime.fromisoformat(item['timestamp']) for item in interaction_data if 'timestamp' in item]
        
        if len(timestamps) < 3:
            return None
        
        # Calculate time gaps between sessions
        gaps = [(timestamps[i] - timestamps[i-1]).days for i in range(1, len(timestamps))]
        
        # Calculate consistency score
        gap_variance = np.var(gaps) if gaps else 0
        consistency_score = max(0, 1 - gap_variance / 10)  # Normalize variance
        
        return BehavioralPattern(
            pattern_id=f"{user_id}_consistency",
            user_id=user_id,
            pattern_type="consistency",
            description="Session timing consistency",
            strength=consistency_score,
            evidence=[f"Average gap: {np.mean(gaps):.1f} days", f"Variance: {gap_variance:.1f}"],
            implications=["Consistent users respond well to routine", "Inconsistent users need flexibility"]
        )
    
    def _analyze_engagement(self, user_id: str, interaction_data: List[Dict]) -> Optional[BehavioralPattern]:
        """Analyze engagement patterns"""
        
        engagement_scores = [item.get('engagement_level', 0.5) for item in interaction_data]
        
        if not engagement_scores:
            return None
        
        avg_engagement = np.mean(engagement_scores)
        engagement_trend = np.polyfit(range(len(engagement_scores)), engagement_scores, 1)[0] if len(engagement_scores) > 1 else 0
        
        return BehavioralPattern(
            pattern_id=f"{user_id}_engagement",
            user_id=user_id,
            pattern_type="engagement",
            description="User engagement level",
            strength=avg_engagement,
            evidence=[f"Average engagement: {avg_engagement:.2f}", f"Trend: {engagement_trend:.3f}"],
            implications=["High engagement enables advanced techniques", "Low engagement requires motivation strategies"]
        )
    
    def _analyze_learning_pace(self, user_id: str, interaction_data: List[Dict]) -> Optional[BehavioralPattern]:
        """Analyze learning pace patterns"""
        
        learning_indicators = [item.get('learning_occurred', False) for item in interaction_data]
        
        if not learning_indicators:
            return None
        
        learning_rate = sum(learning_indicators) / len(learning_indicators)
        
        # Analyze response times if available
        response_times = [item.get('response_time', 0) for item in interaction_data if item.get('response_time')]
        avg_response_time = np.mean(response_times) if response_times else 0
        
        # Determine pace category
        if learning_rate > 0.7 and avg_response_time < 15:
            pace_strength = 0.8
            pace_description = "Fast learner with quick responses"
        elif learning_rate > 0.5:
            pace_strength = 0.6
            pace_description = "Steady learner with good retention"
        else:
            pace_strength = 0.3
            pace_description = "Deliberate learner needs more time"
        
        return BehavioralPattern(
            pattern_id=f"{user_id}_learning_pace",
            user_id=user_id,
            pattern_type="learning_pace",
            description=pace_description,
            strength=pace_strength,
            evidence=[f"Learning rate: {learning_rate:.2f}", f"Avg response time: {avg_response_time:.1f}s"],
            implications=["Adjust pacing based on learning speed", "Provide appropriate challenge level"]
        )

class PreferenceInferrer:
    """System for inferring user preferences from behavior"""
    
    def infer_preferences(self, interaction_data: List[Dict], performance_data: List[Dict] = None) -> Dict[str, Any]:
        """Infer user preferences from behavioral data"""
        
        preferences = {}
        
        # Infer attention span
        preferences['attention_span'] = self._infer_attention_span(interaction_data)
        
        # Infer session length preference
        preferences['session_length'] = self._infer_session_length(interaction_data)
        
        # Infer personality traits
        preferences['personality_traits'] = self._infer_personality_traits(interaction_data)
        
        return preferences
    
    def _infer_attention_span(self, interaction_data: List[Dict]) -> float:
        """Infer attention span from session data"""
        
        session_durations = [item.get('session_duration', 0) for item in interaction_data if item.get('session_duration')]
        
        if not session_durations:
            return 15.0  # Default
        
        # Use median session duration as attention span estimate
        return np.median(session_durations)
    
    def _infer_session_length(self, interaction_data: List[Dict]) -> float:
        """Infer preferred session length"""
        
        # Look for optimal session lengths based on engagement
        session_data = [(item.get('session_duration', 0), item.get('engagement_level', 0.5)) 
                       for item in interaction_data if item.get('session_duration')]
        
        if not session_data:
            return 20.0  # Default
        
        # Find session length with highest average engagement
        length_engagement = defaultdict(list)
        for duration, engagement in session_data:
            length_bucket = round(duration / 5) * 5  # Round to nearest 5 minutes
            length_engagement[length_bucket].append(engagement)
        
        if length_engagement:
            avg_engagement = {length: np.mean(engagements) for length, engagements in length_engagement.items()}
            optimal_length = max(avg_engagement.keys(), key=lambda k: avg_engagement[k])
            return optimal_length
        
        return 20.0
    
    def _infer_personality_traits(self, interaction_data: List[Dict]) -> List[PersonalityTrait]:
        """Infer personality traits from interaction patterns"""
        
        traits = []
        
        # Analyze for perfectionist tendencies
        perfectionist_indicators = 0
        for item in interaction_data:
            user_msg = item.get('user_message', '').lower()
            if any(word in user_msg for word in ['perfect', 'exact', 'precise', 'correct', 'right']):
                perfectionist_indicators += 1
        
        if perfectionist_indicators > len(interaction_data) * 0.3:
            traits.append(PersonalityTrait.PERFECTIONIST)
        
        # Analyze for patience
        impatience_indicators = sum(1 for item in interaction_data 
                                  if any(word in item.get('user_message', '').lower() 
                                        for word in ['quick', 'fast', 'hurry', 'speed']))
        
        if impatience_indicators > len(interaction_data) * 0.2:
            traits.append(PersonalityTrait.IMPATIENT)
        else:
            traits.append(PersonalityTrait.PATIENT)
        
        # Analyze for curiosity
        question_count = sum(1 for item in interaction_data 
                           if '?' in item.get('user_message', ''))
        
        if question_count > len(interaction_data) * 0.4:
            traits.append(PersonalityTrait.CURIOUS)
        
        return traits