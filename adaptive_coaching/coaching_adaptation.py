"""
Coaching Adaptation Engine - Personalized Response Generation

This module implements the core adaptation engine that personalizes coaching
responses based on user patterns, learning effectiveness, and preferences.
It dynamically adjusts coaching style, content, and delivery to maximize
learning outcomes for each individual user.

Key Features:
- Real-time coaching style adaptation
- Effectiveness-based response optimization
- Personalized content generation
- Emotional state-aware coaching
- Learning style adaptation
- Progressive difficulty adjustment
- Context-aware response modification

The engine acts as the "brain" that makes coaching decisions, ensuring each
user receives the most effective personalized instruction possible.
"""

import json
import logging
import random
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
import numpy as np
from collections import defaultdict, deque

# Import from existing modules
try:
    from conversational_coaching.config.coaching_profiles import (
        COACHING_PERSONALITIES, CoachingPersonality, PersonalityAdapter
    )
except ImportError:
    logging.warning("Coaching profiles not available - using mock implementation")
    COACHING_PERSONALITIES = {}
    CoachingPersonality = None
    PersonalityAdapter = None

logger = logging.getLogger(__name__)

class AdaptationStrategy(Enum):
    """Different adaptation strategies"""
    EFFECTIVENESS_BASED = "effectiveness_based"
    PATTERN_BASED = "pattern_based"
    EMOTIONAL_STATE = "emotional_state"
    LEARNING_STYLE = "learning_style"
    PROGRESSIVE = "progressive"
    CONTEXTUAL = "contextual"

class ResponseType(Enum):
    """Types of coaching responses"""
    ENCOURAGEMENT = "encouragement"
    TECHNICAL_FEEDBACK = "technical_feedback"
    DRILL_SUGGESTION = "drill_suggestion"
    PROGRESS_RECOGNITION = "progress_recognition"
    GOAL_SETTING = "goal_setting"
    PROBLEM_SOLVING = "problem_solving"
    MOTIVATIONAL = "motivational"
    EDUCATIONAL = "educational"

class AdaptationConfidence(Enum):
    """Confidence levels for adaptations"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    VERY_HIGH = "very_high"

@dataclass
class AdaptationDecision:
    """Decision made by the adaptation engine"""
    decision_id: str
    user_id: str
    timestamp: datetime
    
    # Adaptation details
    strategy_used: AdaptationStrategy
    original_response: str
    adapted_response: str
    adaptation_reason: str
    
    # Confidence and effectiveness
    confidence: AdaptationConfidence
    confidence_score: float
    expected_effectiveness: float
    
    # Context
    user_context: Dict[str, Any] = field(default_factory=dict)
    session_context: Dict[str, Any] = field(default_factory=dict)
    
    # Tracking
    actual_effectiveness: Optional[float] = None
    user_feedback: Optional[Dict[str, Any]] = None

@dataclass
class CoachingContext:
    """Context information for coaching adaptation"""
    user_id: str
    session_id: str
    
    # Current state
    emotional_state: str = "neutral"
    energy_level: str = "medium"
    engagement_level: float = 0.5
    
    # Session information
    session_duration: float = 0.0
    previous_responses: List[str] = field(default_factory=list)
    current_topic: str = ""
    
    # User patterns
    learning_patterns: List[str] = field(default_factory=list)
    effectiveness_history: List[float] = field(default_factory=list)
    
    # Swing analysis context
    recent_swing_data: Dict[str, Any] = field(default_factory=dict)
    improvement_areas: List[str] = field(default_factory=list)
    
    # Goals and progress
    current_goals: List[str] = field(default_factory=list)
    recent_achievements: List[str] = field(default_factory=list)

class CoachingAdaptationEngine:
    """Main engine for adaptive coaching decisions"""
    
    def __init__(self, user_learning_engine=None, pattern_recognizer=None):
        self.user_learning_engine = user_learning_engine
        self.pattern_recognizer = pattern_recognizer
        
        # Adaptation history
        self.adaptation_history: Dict[str, List[AdaptationDecision]] = defaultdict(list)
        
        # Effectiveness tracking
        self.effectiveness_tracker: Dict[str, Dict[str, float]] = defaultdict(lambda: defaultdict(float))
        
        # Adaptation strategies
        self.adaptation_strategies = {
            AdaptationStrategy.EFFECTIVENESS_BASED: self._effectiveness_based_adaptation,
            AdaptationStrategy.PATTERN_BASED: self._pattern_based_adaptation,
            AdaptationStrategy.EMOTIONAL_STATE: self._emotional_state_adaptation,
            AdaptationStrategy.LEARNING_STYLE: self._learning_style_adaptation,
            AdaptationStrategy.PROGRESSIVE: self._progressive_adaptation,
            AdaptationStrategy.CONTEXTUAL: self._contextual_adaptation
        }
        
        # Response templates
        self._initialize_response_templates()
        
        logger.info("Coaching Adaptation Engine initialized")
    
    def _initialize_response_templates(self):
        """Initialize response adaptation templates"""
        
        self.response_templates = {
            ResponseType.ENCOURAGEMENT: {
                "high_energy": [
                    "That's fantastic! You're really crushing it today!",
                    "Incredible progress! Keep that energy up!",
                    "You're on fire! This is exactly what I want to see!"
                ],
                "medium_energy": [
                    "Great work! You're making solid progress.",
                    "That's really good! You're getting the hang of this.",
                    "Nice improvement! You should be proud of that."
                ],
                "low_energy": [
                    "Good work today. Every small step counts.",
                    "You're making progress. Keep going at your own pace.",
                    "That's a solid effort. You're moving in the right direction."
                ]
            },
            ResponseType.TECHNICAL_FEEDBACK: {
                "analytical": [
                    "Looking at your swing mechanics, I notice your {kpi} is at {value}. The optimal range is {range}.",
                    "Your kinematic sequence shows {observation}. This affects your {impact}.",
                    "The data indicates {finding}. To optimize this, try {suggestion}."
                ],
                "simple": [
                    "Your {area} needs some work. Here's what I notice: {observation}.",
                    "The main thing to focus on is your {area}. {simple_explanation}.",
                    "Let's work on your {area}. {basic_tip}."
                ],
                "visual": [
                    "Picture this: {metaphor}. That's what your swing should feel like.",
                    "Imagine {visual_analogy}. That's the motion we're after.",
                    "Think of it like {comparison}. Your swing should flow like that."
                ]
            },
            ResponseType.DRILL_SUGGESTION: {
                "beginner": [
                    "Here's a simple drill to help: {basic_drill}. Take your time with this.",
                    "Let's try this easy exercise: {drill}. Focus on feeling the motion.",
                    "Start with this basic drill: {drill}. Don't worry about speed."
                ],
                "intermediate": [
                    "This drill will help you improve: {drill}. Try to maintain {key_point}.",
                    "Practice this exercise: {drill}. Pay attention to {focus_area}.",
                    "Here's an effective drill: {drill}. Aim for {target_outcome}."
                ],
                "advanced": [
                    "For advanced practice: {complex_drill}. This will challenge your {skill_area}.",
                    "Try this demanding drill: {drill}. Focus on precision and {advanced_concept}.",
                    "Here's a sophisticated exercise: {drill}. Master this for {benefit}."
                ]
            }
        }
        
        # Emotional state modifiers
        self.emotional_modifiers = {
            "frustrated": {
                "prefix": "I understand this is challenging. ",
                "tone": "patient and reassuring",
                "length": "shorter",
                "encouragement": "extra"
            },
            "excited": {
                "prefix": "I love your enthusiasm! ",
                "tone": "energetic and positive",
                "length": "normal",
                "encouragement": "match_energy"
            },
            "confused": {
                "prefix": "Let me break this down for you. ",
                "tone": "clear and methodical",
                "length": "longer",
                "encouragement": "educational"
            },
            "confident": {
                "prefix": "You're in a great mindset! ",
                "tone": "supportive and challenging",
                "length": "normal",
                "encouragement": "goal_oriented"
            }
        }
    
    def adapt_response(self, user_id: str, original_response: str, 
                      coaching_context: CoachingContext) -> str:
        """Main entry point for response adaptation"""
        
        # Determine best adaptation strategy
        strategy = self._select_adaptation_strategy(user_id, coaching_context)
        
        # Apply adaptation
        adapted_response = self._apply_adaptation_strategy(
            strategy, user_id, original_response, coaching_context
        )
        
        # Record adaptation decision
        decision = self._record_adaptation_decision(
            user_id, strategy, original_response, adapted_response, coaching_context
        )
        
        return adapted_response
    
    def _select_adaptation_strategy(self, user_id: str, 
                                  coaching_context: CoachingContext) -> AdaptationStrategy:
        """Select the most appropriate adaptation strategy"""
        
        # Get user patterns and preferences
        user_profile = None
        if self.user_learning_engine:
            user_profile = self.user_learning_engine.get_or_create_profile(user_id)
        
        # Strategy selection logic
        
        # 1. Emotional state takes priority
        if coaching_context.emotional_state in ["frustrated", "confused", "excited"]:
            return AdaptationStrategy.EMOTIONAL_STATE
        
        # 2. If we have effectiveness data, use it
        if user_id in self.effectiveness_tracker and len(self.effectiveness_tracker[user_id]) > 3:
            return AdaptationStrategy.EFFECTIVENESS_BASED
        
        # 3. If we have user patterns, use them
        if self.pattern_recognizer and len(coaching_context.learning_patterns) > 0:
            return AdaptationStrategy.PATTERN_BASED
        
        # 4. If we know learning style, adapt to it
        if user_profile and user_profile.learning_style:
            return AdaptationStrategy.LEARNING_STYLE
        
        # 5. Default to contextual adaptation
        return AdaptationStrategy.CONTEXTUAL
    
    def _apply_adaptation_strategy(self, strategy: AdaptationStrategy, user_id: str,
                                 original_response: str, coaching_context: CoachingContext) -> str:
        """Apply the selected adaptation strategy"""
        
        adaptation_func = self.adaptation_strategies.get(strategy)
        if not adaptation_func:
            return original_response
        
        try:
            return adaptation_func(user_id, original_response, coaching_context)
        except Exception as e:
            logger.error(f"Error in adaptation strategy {strategy}: {e}")
            return original_response
    
    def _effectiveness_based_adaptation(self, user_id: str, original_response: str,
                                      coaching_context: CoachingContext) -> str:
        """Adapt based on what has been most effective for this user"""
        
        effectiveness_data = self.effectiveness_tracker[user_id]
        
        # Find most effective approaches
        best_approaches = sorted(effectiveness_data.items(), key=lambda x: x[1], reverse=True)
        
        if not best_approaches:
            return original_response
        
        best_approach = best_approaches[0][0]
        
        # Adapt response based on best approach
        if best_approach == "detailed":
            return self._make_response_detailed(original_response, coaching_context)
        elif best_approach == "encouraging":
            return self._make_response_encouraging(original_response)
        elif best_approach == "direct":
            return self._make_response_direct(original_response)
        elif best_approach == "technical":
            return self._make_response_technical(original_response, coaching_context)
        
        return original_response
    
    def _pattern_based_adaptation(self, user_id: str, original_response: str,
                                coaching_context: CoachingContext) -> str:
        """Adapt based on recognized user patterns"""
        
        if not self.pattern_recognizer:
            return original_response
        
        # Get user patterns
        patterns = self.pattern_recognizer.get_user_patterns(user_id)
        
        # Find relevant patterns
        relevant_patterns = [p for p in patterns if p.confidence_score > 0.7]
        
        if not relevant_patterns:
            return original_response
        
        # Apply adaptations based on patterns
        adapted_response = original_response
        
        for pattern in relevant_patterns:
            if pattern.pattern_type.value == "learning_behavior":
                if "fast_learner" in pattern.pattern_name:
                    adapted_response = self._increase_complexity(adapted_response)
                elif "slow_learner" in pattern.pattern_name:
                    adapted_response = self._simplify_response(adapted_response)
            
            elif pattern.pattern_type.value == "coaching_effectiveness":
                if "analytical" in pattern.pattern_name:
                    adapted_response = self._make_response_technical(adapted_response, coaching_context)
                elif "encouraging" in pattern.pattern_name:
                    adapted_response = self._make_response_encouraging(adapted_response)
        
        return adapted_response
    
    def _emotional_state_adaptation(self, user_id: str, original_response: str,
                                  coaching_context: CoachingContext) -> str:
        """Adapt based on current emotional state"""
        
        emotional_state = coaching_context.emotional_state
        
        if emotional_state not in self.emotional_modifiers:
            return original_response
        
        modifier = self.emotional_modifiers[emotional_state]
        
        # Apply emotional state modifications
        adapted_response = original_response
        
        # Add prefix if needed
        if modifier.get("prefix"):
            adapted_response = modifier["prefix"] + adapted_response
        
        # Adjust tone
        if modifier.get("tone") == "patient and reassuring":
            adapted_response = self._make_tone_patient(adapted_response)
        elif modifier.get("tone") == "energetic and positive":
            adapted_response = self._make_tone_energetic(adapted_response)
        elif modifier.get("tone") == "clear and methodical":
            adapted_response = self._make_tone_methodical(adapted_response)
        
        # Adjust length
        if modifier.get("length") == "shorter":
            adapted_response = self._shorten_response(adapted_response)
        elif modifier.get("length") == "longer":
            adapted_response = self._expand_response(adapted_response, coaching_context)
        
        # Adjust encouragement
        if modifier.get("encouragement") == "extra":
            adapted_response = self._add_extra_encouragement(adapted_response)
        elif modifier.get("encouragement") == "match_energy":
            adapted_response = self._match_energy_level(adapted_response)
        
        return adapted_response
    
    def _learning_style_adaptation(self, user_id: str, original_response: str,
                                 coaching_context: CoachingContext) -> str:
        """Adapt based on user's learning style"""
        
        if not self.user_learning_engine:
            return original_response
        
        user_profile = self.user_learning_engine.get_or_create_profile(user_id)
        learning_style = user_profile.learning_style
        
        if learning_style.value == "analytical":
            return self._make_response_analytical(original_response, coaching_context)
        elif learning_style.value == "visual":
            return self._make_response_visual(original_response)
        elif learning_style.value == "experiential":
            return self._make_response_experiential(original_response)
        elif learning_style.value == "patient":
            return self._make_response_patient(original_response)
        elif learning_style.value == "quick_tips":
            return self._make_response_concise(original_response)
        elif learning_style.value == "encouraging":
            return self._make_response_encouraging(original_response)
        elif learning_style.value == "direct":
            return self._make_response_direct(original_response)
        
        return original_response
    
    def _progressive_adaptation(self, user_id: str, original_response: str,
                              coaching_context: CoachingContext) -> str:
        """Adapt based on user's progress and skill level"""
        
        if not self.user_learning_engine:
            return original_response
        
        user_profile = self.user_learning_engine.get_or_create_profile(user_id)
        
        # Adjust complexity based on progress
        if user_profile.total_sessions < 5:
            # Beginner - simplify
            return self._simplify_response(original_response)
        elif user_profile.total_sessions < 20:
            # Intermediate - moderate complexity
            return self._moderate_complexity(original_response)
        else:
            # Advanced - can handle complex information
            return self._increase_complexity(original_response)
    
    def _contextual_adaptation(self, user_id: str, original_response: str,
                             coaching_context: CoachingContext) -> str:
        """Adapt based on current context"""
        
        adapted_response = original_response
        
        # Adapt based on session duration
        if coaching_context.session_duration > 20:
            # Long session - might be getting tired
            adapted_response = self._add_energy_boost(adapted_response)
        
        # Adapt based on engagement level
        if coaching_context.engagement_level < 0.5:
            # Low engagement - try to re-engage
            adapted_response = self._increase_engagement(adapted_response)
        
        # Adapt based on recent achievements
        if coaching_context.recent_achievements:
            adapted_response = self._acknowledge_achievements(adapted_response, coaching_context.recent_achievements)
        
        return adapted_response
    
    # Response modification methods
    
    def _make_response_detailed(self, response: str, coaching_context: CoachingContext) -> str:
        """Add technical details to response"""
        if coaching_context.recent_swing_data:
            kpis = coaching_context.recent_swing_data.get("kpis", {})
            if kpis:
                kpi_info = f" Your metrics show: {', '.join([f'{k}: {v}' for k, v in list(kpis.items())[:2]])}."
                response += kpi_info
        return response
    
    def _make_response_encouraging(self, response: str) -> str:
        """Add encouragement to response"""
        encouraging_phrases = [
            "You're doing great! ",
            "I'm really impressed with your progress! ",
            "Keep up the excellent work! ",
            "You should be proud of your improvement! "
        ]
        prefix = random.choice(encouraging_phrases)
        return prefix + response
    
    def _make_response_direct(self, response: str) -> str:
        """Make response more direct and concise"""
        # Remove hedging language
        response = response.replace("I think", "").replace("maybe", "").replace("perhaps", "")
        response = response.replace("You might want to", "").replace("You could", "You should")
        return response.strip()
    
    def _make_response_technical(self, response: str, coaching_context: CoachingContext) -> str:
        """Add technical information to response"""
        technical_terms = {
            "swing": "kinematic sequence",
            "rotation": "angular velocity",
            "position": "biomechanical alignment",
            "movement": "motor pattern"
        }
        
        for simple, technical in technical_terms.items():
            if simple in response.lower():
                response = response.replace(simple, f"{simple} ({technical})")
                break
        
        return response
    
    def _make_response_analytical(self, response: str, coaching_context: CoachingContext) -> str:
        """Make response more analytical"""
        if coaching_context.recent_swing_data:
            analysis = "Based on your swing analysis, "
            response = analysis + response
        return response
    
    def _make_response_visual(self, response: str) -> str:
        """Add visual metaphors to response"""
        visual_metaphors = [
            "Picture this: ",
            "Imagine ",
            "Visualize ",
            "Think of it like "
        ]
        
        metaphors = {
            "swing": "a smooth pendulum",
            "rotation": "a door opening",
            "balance": "a tree swaying in the wind",
            "follow-through": "throwing a ball"
        }
        
        for concept, metaphor in metaphors.items():
            if concept in response.lower():
                prefix = random.choice(visual_metaphors)
                response = response.replace(concept, f"{concept} - {prefix}{metaphor}")
                break
        
        return response
    
    def _make_response_experiential(self, response: str) -> str:
        """Make response more experiential"""
        experiential_prompts = [
            "Feel this: ",
            "Try this sensation: ",
            "Notice how it feels when ",
            "Experience the difference: "
        ]
        
        if "feel" not in response.lower():
            prefix = random.choice(experiential_prompts)
            response = prefix + response
        
        return response
    
    def _make_tone_patient(self, response: str) -> str:
        """Make tone more patient"""
        patient_modifiers = [
            "Take your time with this. ",
            "There's no rush. ",
            "Let's work through this step by step. "
        ]
        prefix = random.choice(patient_modifiers)
        return prefix + response
    
    def _make_tone_energetic(self, response: str) -> str:
        """Make tone more energetic"""
        # Add exclamation points and energetic language
        response = response.replace(".", "!")
        energetic_words = {
            "good": "fantastic",
            "nice": "awesome",
            "better": "much better",
            "improvement": "great improvement"
        }
        
        for word, energetic in energetic_words.items():
            response = response.replace(word, energetic)
        
        return response
    
    def _make_tone_methodical(self, response: str) -> str:
        """Make tone more methodical"""
        if "step" not in response.lower():
            response = "Here's what we'll do step by step: " + response
        return response
    
    def _shorten_response(self, response: str) -> str:
        """Shorten response for clarity"""
        sentences = response.split('. ')
        if len(sentences) > 2:
            return '. '.join(sentences[:2]) + '.'
        return response
    
    def _expand_response(self, response: str, coaching_context: CoachingContext) -> str:
        """Expand response with more detail"""
        expansion = " Let me explain this further: this will help you because it addresses the root cause of the issue."
        return response + expansion
    
    def _add_extra_encouragement(self, response: str) -> str:
        """Add extra encouragement"""
        encouragement = " Remember, every golfer goes through this. You're making progress even when it doesn't feel like it."
        return response + encouragement
    
    def _match_energy_level(self, response: str) -> str:
        """Match user's energy level"""
        return self._make_tone_energetic(response)
    
    def _simplify_response(self, response: str) -> str:
        """Simplify response for beginners"""
        # Replace complex terms with simple ones
        simplifications = {
            "biomechanical": "body position",
            "kinematic": "movement",
            "angular": "turning",
            "trajectory": "path"
        }
        
        for complex_term, simple_term in simplifications.items():
            response = response.replace(complex_term, simple_term)
        
        return response
    
    def _increase_complexity(self, response: str) -> str:
        """Increase complexity for advanced users"""
        # Add more technical detail
        complex_additions = {
            "swing": "swing mechanics and timing",
            "position": "biomechanical position",
            "movement": "kinematic chain"
        }
        
        for simple, complex in complex_additions.items():
            if simple in response and complex not in response:
                response = response.replace(simple, complex)
                break
        
        return response
    
    def _moderate_complexity(self, response: str) -> str:
        """Keep response at moderate complexity"""
        # Response is already at appropriate level
        return response
    
    def _add_energy_boost(self, response: str) -> str:
        """Add energy boost for long sessions"""
        energy_boost = "You're doing great staying focused! "
        return energy_boost + response
    
    def _increase_engagement(self, response: str) -> str:
        """Increase engagement for distracted users"""
        engagement_hooks = [
            "Here's something interesting: ",
            "You'll love this: ",
            "This is going to help you immediately: "
        ]
        prefix = random.choice(engagement_hooks)
        return prefix + response
    
    def _acknowledge_achievements(self, response: str, achievements: List[str]) -> str:
        """Acknowledge recent achievements"""
        if achievements:
            achievement_text = f"Building on your recent success with {achievements[0]}, "
            response = achievement_text + response
        return response
    
    def _record_adaptation_decision(self, user_id: str, strategy: AdaptationStrategy,
                                  original_response: str, adapted_response: str,
                                  coaching_context: CoachingContext) -> AdaptationDecision:
        """Record adaptation decision for learning"""
        
        decision_id = f"{user_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        decision = AdaptationDecision(
            decision_id=decision_id,
            user_id=user_id,
            timestamp=datetime.now(),
            strategy_used=strategy,
            original_response=original_response,
            adapted_response=adapted_response,
            adaptation_reason=f"Applied {strategy.value} adaptation",
            confidence=AdaptationConfidence.MEDIUM,
            confidence_score=0.7,
            expected_effectiveness=0.8,
            user_context=coaching_context.__dict__
        )
        
        self.adaptation_history[user_id].append(decision)
        
        return decision
    
    def update_effectiveness(self, user_id: str, decision_id: str, effectiveness_score: float):
        """Update effectiveness of an adaptation decision"""
        
        # Find the decision
        decisions = self.adaptation_history.get(user_id, [])
        decision = None
        for d in decisions:
            if d.decision_id == decision_id:
                decision = d
                break
        
        if not decision:
            return
        
        # Update effectiveness
        decision.actual_effectiveness = effectiveness_score
        
        # Update effectiveness tracker
        strategy_name = decision.strategy_used.value
        current_avg = self.effectiveness_tracker[user_id].get(strategy_name, 0.5)
        
        # Exponential moving average
        alpha = 0.3
        new_avg = alpha * effectiveness_score + (1 - alpha) * current_avg
        self.effectiveness_tracker[user_id][strategy_name] = new_avg
        
        logger.info(f"Updated effectiveness for {user_id}, strategy {strategy_name}: {new_avg}")
    
    def get_adaptation_insights(self, user_id: str) -> Dict[str, Any]:
        """Get insights about adaptation effectiveness"""
        
        decisions = self.adaptation_history.get(user_id, [])
        effectiveness_data = self.effectiveness_tracker.get(user_id, {})
        
        insights = {
            "total_adaptations": len(decisions),
            "strategy_effectiveness": dict(effectiveness_data),
            "most_effective_strategy": max(effectiveness_data.items(), key=lambda x: x[1])[0] if effectiveness_data else None,
            "average_effectiveness": sum(effectiveness_data.values()) / len(effectiveness_data) if effectiveness_data else 0,
            "recent_decisions": [
                {
                    "strategy": d.strategy_used.value,
                    "effectiveness": d.actual_effectiveness,
                    "timestamp": d.timestamp.isoformat()
                }
                for d in decisions[-5:]  # Last 5 decisions
            ]
        }
        
        return insights
    
    def get_personalized_coaching_style(self, user_id: str) -> Dict[str, Any]:
        """Get personalized coaching style recommendations"""
        
        effectiveness_data = self.effectiveness_tracker.get(user_id, {})
        
        # Default style
        style = {
            "primary_approach": "balanced",
            "communication_style": "friendly",
            "detail_level": "moderate",
            "encouragement_level": "medium",
            "adaptation_confidence": "medium"
        }
        
        if not effectiveness_data:
            return style
        
        # Determine best approaches
        if effectiveness_data.get("technical", 0) > 0.7:
            style["primary_approach"] = "technical"
            style["detail_level"] = "high"
        elif effectiveness_data.get("encouraging", 0) > 0.7:
            style["primary_approach"] = "encouraging"
            style["encouragement_level"] = "high"
        elif effectiveness_data.get("direct", 0) > 0.7:
            style["primary_approach"] = "direct"
            style["communication_style"] = "direct"
        
        # Set confidence based on data quality
        if len(effectiveness_data) > 10:
            style["adaptation_confidence"] = "high"
        elif len(effectiveness_data) > 5:
            style["adaptation_confidence"] = "medium"
        else:
            style["adaptation_confidence"] = "low"
        
        return style