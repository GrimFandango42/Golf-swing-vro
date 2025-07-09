"""
Adaptive Coaching Integration - Unified Intelligent Coaching System

This module integrates the adaptive coaching system with existing personality-based
coaching to create a unified, intelligent coaching experience. It combines the
best of both systems to deliver truly personalized coaching that learns and
adapts over time while maintaining personality-driven consistency.

Key Features:
- Seamless integration with existing coaching personalities
- Dynamic personality adaptation based on user learning
- Intelligent coaching decision orchestration
- Unified user experience management
- Cross-system data synchronization
- Performance optimization and monitoring
- Fallback and redundancy management

The integration creates a coaching system that feels both consistent and
adaptive, providing users with a truly intelligent personal golf instructor.
"""

import asyncio
import logging
import json
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field

# Import adaptive coaching components
from .user_learning_engine import UserLearningEngine, UserProfile
from .pattern_recognition import PatternRecognizer, RecognizedPattern
from .coaching_adaptation import CoachingAdaptationEngine, CoachingContext
from .progress_predictor import ProgressPredictor, ProgressTrajectory
from .memory_context_manager import MemoryContextManager, ConversationContext
from .user_profiling import UserProfilingSystem, UserProfile as DetailedProfile
from .effectiveness_tracker import EffectivenessTracker, EffectivenessRecord
from .celebration_system import CelebrationSystem, Achievement

# Import existing coaching system components
try:
    from conversational_coaching.config.coaching_profiles import (
        COACHING_PERSONALITIES, CoachingPersonality, PersonalityAdapter,
        PersonalitySelector
    )
    from conversational_coaching.conversation_engine.coaching_agent import CoachingAgent
except ImportError:
    logging.warning("Existing coaching system not available - using mock implementations")
    COACHING_PERSONALITIES = {}
    CoachingPersonality = None
    PersonalityAdapter = None
    PersonalitySelector = None
    CoachingAgent = None

logger = logging.getLogger(__name__)

@dataclass
class AdaptiveCoachingConfig:
    """Configuration for adaptive coaching system"""
    # Integration settings
    enable_adaptive_learning: bool = True
    enable_pattern_recognition: bool = True
    enable_progress_prediction: bool = True
    enable_effectiveness_tracking: bool = True
    enable_celebration_system: bool = True
    
    # Adaptation thresholds
    adaptation_confidence_threshold: float = 0.7
    pattern_confidence_threshold: float = 0.6
    effectiveness_threshold: float = 0.5
    
    # Learning parameters
    learning_rate: float = 0.1
    adaptation_frequency: int = 10  # Adapt every N interactions
    memory_retention_days: int = 90
    
    # Integration parameters
    personality_override_threshold: float = 0.8
    fallback_to_personality: bool = True
    sync_frequency_minutes: int = 30

@dataclass
class CoachingDecision:
    """Decision made by the adaptive coaching system"""
    decision_id: str
    user_id: str
    session_id: str
    
    # Decision details
    chosen_approach: str
    personality_used: str
    adaptations_applied: List[str] = field(default_factory=list)
    
    # Supporting data
    user_context: Dict[str, Any] = field(default_factory=dict)
    adaptation_confidence: float = 0.5
    effectiveness_prediction: float = 0.5
    
    # Sources of information
    pattern_insights: List[str] = field(default_factory=list)
    progress_insights: List[str] = field(default_factory=list)
    memory_insights: List[str] = field(default_factory=list)
    
    # Metadata
    created_at: datetime = field(default_factory=datetime.now)

class AdaptiveCoachingOrchestrator:
    """Main orchestrator for the adaptive coaching system"""
    
    def __init__(self, config: AdaptiveCoachingConfig = None):
        self.config = config or AdaptiveCoachingConfig()
        
        # Initialize adaptive coaching components
        self.user_learning_engine = UserLearningEngine()
        self.pattern_recognizer = PatternRecognizer()
        self.coaching_adaptation_engine = CoachingAdaptationEngine(
            self.user_learning_engine, self.pattern_recognizer
        )
        self.progress_predictor = ProgressPredictor()
        self.memory_manager = MemoryContextManager()
        self.user_profiling_system = UserProfilingSystem()
        self.effectiveness_tracker = EffectivenessTracker()
        self.celebration_system = CelebrationSystem(self.user_learning_engine)
        
        # Integration with existing system
        self.personality_selector = PersonalitySelector() if PersonalitySelector else None
        self.coaching_agent = None  # Will be initialized when needed
        
        # Decision tracking
        self.coaching_decisions: Dict[str, List[CoachingDecision]] = {}
        
        # Performance monitoring
        self.performance_metrics = {
            "total_interactions": 0,
            "adaptation_success_rate": 0.0,
            "average_effectiveness": 0.0,
            "system_availability": 1.0
        }
        
        logger.info("Adaptive Coaching Orchestrator initialized")
    
    async def process_coaching_request(self, user_id: str, session_id: str,
                                     user_message: str, swing_analysis: Dict = None,
                                     context: Dict = None) -> Dict[str, Any]:
        """Main entry point for processing coaching requests"""
        
        try:
            # Create conversation context
            conversation_context = self._create_coaching_context(
                user_id, session_id, user_message, swing_analysis, context
            )
            
            # Make coaching decision
            coaching_decision = await self._make_coaching_decision(
                user_id, session_id, conversation_context
            )
            
            # Generate coaching response
            response = await self._generate_coaching_response(
                user_id, session_id, user_message, coaching_decision, conversation_context
            )
            
            # Process achievements and celebrations
            achievements = await self._process_achievements(
                user_id, session_id, swing_analysis, conversation_context
            )
            
            # Record effectiveness for learning
            await self._record_interaction_effectiveness(
                user_id, session_id, coaching_decision, response, conversation_context
            )
            
            # Update user learning
            await self._update_user_learning(
                user_id, user_message, response, swing_analysis, conversation_context
            )
            
            # Prepare response
            coaching_response = {
                "response": response,
                "achievements": achievements,
                "coaching_insights": self._extract_coaching_insights(coaching_decision),
                "progress_update": await self._get_progress_update(user_id),
                "next_recommendations": await self._get_next_recommendations(user_id),
                "system_confidence": coaching_decision.adaptation_confidence
            }
            
            # Update performance metrics
            self._update_performance_metrics(coaching_decision)
            
            return coaching_response
            
        except Exception as e:
            logger.error(f"Error in adaptive coaching: {e}")
            return await self._fallback_response(user_id, user_message, swing_analysis)
    
    def _create_coaching_context(self, user_id: str, session_id: str, 
                               user_message: str, swing_analysis: Dict,
                               context: Dict) -> CoachingContext:
        """Create comprehensive coaching context"""
        
        # Get conversation context
        conversation_context = self.memory_manager.get_conversation_context(session_id)
        if not conversation_context:
            conversation_context = self.memory_manager.create_conversation_context(session_id, user_id)
        
        # Get user context
        user_context = self.memory_manager.get_user_context(user_id)
        
        # Create coaching context
        coaching_context = CoachingContext(
            user_id=user_id,
            session_id=session_id,
            current_topic=context.get('topic', ''),
            emotional_state=context.get('emotional_state', 'neutral'),
            engagement_level=context.get('engagement_level', 0.5),
            session_duration=conversation_context.session_duration if conversation_context else 0,
            recent_swing_data=swing_analysis or {}
        )
        
        # Add user patterns
        user_patterns = self.pattern_recognizer.get_user_patterns(user_id)
        coaching_context.learning_patterns = [p.pattern_name for p in user_patterns]
        
        # Add effectiveness history
        effectiveness_analysis = self.effectiveness_tracker.get_effectiveness_analysis(user_id)
        if "overall_metrics" in effectiveness_analysis:
            coaching_context.effectiveness_history = [effectiveness_analysis["overall_metrics"]["average_effectiveness"]]
        
        return coaching_context
    
    async def _make_coaching_decision(self, user_id: str, session_id: str,
                                    coaching_context: CoachingContext) -> CoachingDecision:
        """Make intelligent coaching decision"""
        
        decision_id = f"{user_id}_{session_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        # Get user profile and learning data
        user_profile = self.user_learning_engine.get_or_create_profile(user_id)
        detailed_profile = await self._get_detailed_user_profile(user_id, coaching_context)
        
        # Get coaching recommendations from adaptive system
        adaptive_recommendations = self.user_learning_engine.get_coaching_recommendations(
            user_id, coaching_context.__dict__
        )
        
        # Get pattern-based insights
        pattern_insights = self._get_pattern_insights(user_id, coaching_context)
        
        # Get progress insights
        progress_insights = await self._get_progress_insights(user_id, coaching_context)
        
        # Choose personality based on adaptive learning
        chosen_personality = self._choose_adaptive_personality(
            user_id, adaptive_recommendations, detailed_profile
        )
        
        # Determine adaptations to apply
        adaptations = self._determine_adaptations(
            user_id, coaching_context, adaptive_recommendations
        )
        
        # Calculate confidence
        adaptation_confidence = self._calculate_adaptation_confidence(
            user_profile, pattern_insights, progress_insights
        )
        
        # Create decision
        decision = CoachingDecision(
            decision_id=decision_id,
            user_id=user_id,
            session_id=session_id,
            chosen_approach=adaptive_recommendations.get("coaching_style", "encouraging_mentor"),
            personality_used=chosen_personality,
            adaptations_applied=adaptations,
            user_context=coaching_context.__dict__,
            adaptation_confidence=adaptation_confidence,
            pattern_insights=[p.pattern_name for p in pattern_insights],
            progress_insights=progress_insights,
            memory_insights=self._get_memory_insights(user_id, session_id)
        )
        
        # Store decision
        if user_id not in self.coaching_decisions:
            self.coaching_decisions[user_id] = []
        self.coaching_decisions[user_id].append(decision)
        
        return decision
    
    async def _get_detailed_user_profile(self, user_id: str, 
                                       coaching_context: CoachingContext) -> DetailedProfile:
        """Get detailed user profile from profiling system"""
        
        # Get interaction data
        interaction_data = []
        user_memories = self.memory_manager.recall_memories(user_id, limit=20)
        
        for memory in user_memories:
            if memory.memory_type.value == "conversational":
                interaction_data.append({
                    "user_message": memory.content,
                    "timestamp": memory.created_at.isoformat(),
                    "engagement_level": 0.5  # Default
                })
        
        # Get performance data
        performance_data = []
        if coaching_context.recent_swing_data:
            performance_data.append(coaching_context.recent_swing_data)
        
        # Analyze user profile
        return self.user_profiling_system.analyze_user(user_id, interaction_data, performance_data)
    
    def _get_pattern_insights(self, user_id: str, coaching_context: CoachingContext) -> List[RecognizedPattern]:
        """Get pattern-based insights"""
        
        patterns = self.pattern_recognizer.get_user_patterns(user_id)
        
        # Filter for high-confidence patterns
        high_confidence_patterns = [
            p for p in patterns 
            if p.confidence_score >= self.config.pattern_confidence_threshold
        ]
        
        return high_confidence_patterns
    
    async def _get_progress_insights(self, user_id: str, 
                                   coaching_context: CoachingContext) -> List[str]:
        """Get progress prediction insights"""
        
        insights = []
        
        # Get user's progress trajectory
        if user_id in self.progress_predictor.trajectories:
            trajectory = self.progress_predictor.trajectories[user_id]
            
            if trajectory.breakthrough_probability > 0.6:
                insights.append("User approaching breakthrough moment")
            
            if trajectory.plateau_risk > 0.6:
                insights.append("User at risk of plateau - vary approach")
            
            insights.extend(trajectory.next_goals)
        
        return insights
    
    def _get_memory_insights(self, user_id: str, session_id: str) -> List[str]:
        """Get memory-based insights"""
        
        insights = []
        
        # Get relevant memories
        memories = self.memory_manager.recall_memories(user_id, limit=5)
        
        for memory in memories:
            if memory.importance.value in ["high", "critical"]:
                insights.append(f"Remember: {memory.content[:50]}...")
        
        return insights
    
    def _choose_adaptive_personality(self, user_id: str, 
                                   adaptive_recommendations: Dict,
                                   detailed_profile: DetailedProfile) -> str:
        """Choose personality based on adaptive learning"""
        
        # Get base recommendation from adaptive system
        base_personality = adaptive_recommendations.get("coaching_style", "encouraging_mentor")
        
        # Adapt based on detailed profile
        if detailed_profile.primary_learning_style.value == "analytical":
            return "technical_expert"
        elif detailed_profile.motivation_type.value == "competition":
            return "competitive_trainer"
        elif detailed_profile.communication_style.value == "encouraging":
            return "encouraging_mentor"
        elif detailed_profile.learning_pace.value == "slow":
            return "patient_teacher"
        elif detailed_profile.motivation_type.value in ["achievement", "challenge"]:
            return "motivational_coach"
        
        # Use personality selector if available
        if self.personality_selector and COACHING_PERSONALITIES:
            try:
                user_preferences = {
                    "skill_level": "intermediate",
                    "learning_style": detailed_profile.primary_learning_style.value,
                    "motivation_type": detailed_profile.motivation_type.value
                }
                return self.personality_selector.recommend_personality(user_preferences)
            except Exception as e:
                logger.warning(f"Error in personality selection: {e}")
        
        return base_personality
    
    def _determine_adaptations(self, user_id: str, coaching_context: CoachingContext,
                             adaptive_recommendations: Dict) -> List[str]:
        """Determine what adaptations to apply"""
        
        adaptations = []
        
        # Communication style adaptations
        comm_style = adaptive_recommendations.get("communication_style", {})
        if comm_style.get("length") == "short":
            adaptations.append("shorten_responses")
        elif comm_style.get("length") == "long":
            adaptations.append("expand_responses")
        
        if comm_style.get("technical_detail") == "high":
            adaptations.append("add_technical_detail")
        elif comm_style.get("technical_detail") == "low":
            adaptations.append("simplify_language")
        
        if comm_style.get("encouragement_level") == "high":
            adaptations.append("increase_encouragement")
        
        # Context-based adaptations
        if coaching_context.emotional_state == "frustrated":
            adaptations.append("patient_approach")
        elif coaching_context.emotional_state == "excited":
            adaptations.append("match_energy")
        
        if coaching_context.engagement_level < 0.5:
            adaptations.append("increase_engagement")
        
        return adaptations
    
    def _calculate_adaptation_confidence(self, user_profile: UserProfile,
                                       pattern_insights: List[RecognizedPattern],
                                       progress_insights: List[str]) -> float:
        """Calculate confidence in adaptation decisions"""
        
        confidence = 0.5  # Base confidence
        
        # Increase confidence based on interaction count
        if user_profile.total_interactions > 20:
            confidence += 0.2
        elif user_profile.total_interactions > 10:
            confidence += 0.1
        
        # Increase confidence based on pattern strength
        if pattern_insights:
            avg_pattern_confidence = sum(p.confidence_score for p in pattern_insights) / len(pattern_insights)
            confidence += avg_pattern_confidence * 0.2
        
        # Increase confidence based on progress insights
        if progress_insights:
            confidence += 0.1
        
        return min(1.0, confidence)
    
    async def _generate_coaching_response(self, user_id: str, session_id: str,
                                        user_message: str, coaching_decision: CoachingDecision,
                                        coaching_context: CoachingContext) -> str:
        """Generate the actual coaching response"""
        
        try:
            # Get base response from personality system
            if self.coaching_agent or CoachingAgent:
                if not self.coaching_agent:
                    self.coaching_agent = CoachingAgent()
                
                # Get base response
                base_response = await self.coaching_agent.process_message(
                    user_id, session_id, user_message, 
                    coaching_context.recent_swing_data
                )
            else:
                # Fallback response generation
                base_response = self._generate_fallback_response(user_message, coaching_context)
            
            # Apply adaptations
            adapted_response = self.coaching_adaptation_engine.adapt_response(
                user_id, base_response, coaching_context
            )
            
            return adapted_response
            
        except Exception as e:
            logger.error(f"Error generating coaching response: {e}")
            return self._generate_fallback_response(user_message, coaching_context)
    
    def _generate_fallback_response(self, user_message: str, 
                                  coaching_context: CoachingContext) -> str:
        """Generate fallback response when main system fails"""
        
        if coaching_context.recent_swing_data:
            return "I can see you're working on your swing. Let's focus on making steady progress with your fundamentals."
        
        return "I'm here to help you improve your golf game. What specific aspect would you like to work on today?"
    
    async def _process_achievements(self, user_id: str, session_id: str,
                                  swing_analysis: Dict, 
                                  coaching_context: CoachingContext) -> List[Dict[str, Any]]:
        """Process achievements and celebrations"""
        
        if not swing_analysis:
            return []
        
        try:
            # Detect achievements
            session_data = {
                "session_id": session_id,
                "overall_score": swing_analysis.get("overall_score", 0),
                "improvement_rate": swing_analysis.get("improvement_rate", 0),
                "consistency_score": swing_analysis.get("consistency_score", 0)
            }
            
            # Get historical data for comparison
            user_memories = self.memory_manager.recall_memories(
                user_id, memory_types=["episodic"], limit=10
            )
            historical_data = []
            for memory in user_memories:
                if "overall_score" in memory.structured_data:
                    historical_data.append(memory.structured_data)
            
            # Analyze for achievements
            achievements = self.celebration_system.analyze_session_for_achievements(
                user_id, session_data, historical_data
            )
            
            # Generate celebrations
            celebration_messages = self.celebration_system.celebrate_achievements(
                user_id, achievements
            )
            
            # Return achievement data
            return [
                {
                    "type": achievement.achievement_type.value,
                    "title": achievement.title,
                    "description": achievement.description,
                    "message": achievement.celebration_message,
                    "magnitude": achievement.magnitude.value
                }
                for achievement in achievements
            ]
            
        except Exception as e:
            logger.error(f"Error processing achievements: {e}")
            return []
    
    async def _record_interaction_effectiveness(self, user_id: str, session_id: str,
                                              coaching_decision: CoachingDecision,
                                              response: str, 
                                              coaching_context: CoachingContext):
        """Record interaction effectiveness for learning"""
        
        try:
            # Map decision to coaching strategy
            strategy_mapping = {
                "encouraging_mentor": "encouraging",
                "technical_expert": "technical",
                "motivational_coach": "motivational",
                "patient_teacher": "patient",
                "competitive_trainer": "direct"
            }
            
            from .effectiveness_tracker import CoachingStrategy
            strategy = CoachingStrategy.SUPPORTIVE  # Default
            
            if coaching_decision.chosen_approach in strategy_mapping:
                strategy_name = strategy_mapping[coaching_decision.chosen_approach]
                try:
                    strategy = CoachingStrategy(strategy_name)
                except ValueError:
                    strategy = CoachingStrategy.SUPPORTIVE
            
            # Record effectiveness
            record_id = self.effectiveness_tracker.record_interaction_effectiveness(
                user_id=user_id,
                session_id=session_id,
                interaction_id=coaching_decision.decision_id,
                coaching_strategy=strategy,
                coaching_content=response,
                context=coaching_context.__dict__
            )
            
            logger.debug(f"Recorded effectiveness for interaction {record_id}")
            
        except Exception as e:
            logger.error(f"Error recording effectiveness: {e}")
    
    async def _update_user_learning(self, user_id: str, user_message: str,
                                  coach_response: str, swing_analysis: Dict,
                                  coaching_context: CoachingContext):
        """Update user learning systems"""
        
        try:
            # Record interaction in learning engine
            self.user_learning_engine.record_interaction(
                user_id=user_id,
                coaching_approach=coaching_context.coaching_approach,
                feedback_type="conversational",
                message_sent=coach_response,
                user_response=user_message,
                session_context=coaching_context.__dict__
            )
            
            # Analyze swing patterns if available
            if swing_analysis:
                self.user_learning_engine.analyze_swing_patterns(user_id, swing_analysis)
            
            # Store important memories
            self.memory_manager.store_memory(
                user_id=user_id,
                content=f"User said: {user_message}",
                memory_type=self.memory_manager.MemoryType.CONVERSATIONAL,
                context_keys=[coaching_context.session_id]
            )
            
            self.memory_manager.store_memory(
                user_id=user_id,
                content=f"Coach responded: {coach_response}",
                memory_type=self.memory_manager.MemoryType.CONVERSATIONAL,
                context_keys=[coaching_context.session_id]
            )
            
        except Exception as e:
            logger.error(f"Error updating user learning: {e}")
    
    def _extract_coaching_insights(self, coaching_decision: CoachingDecision) -> Dict[str, Any]:
        """Extract insights from coaching decision"""
        
        return {
            "approach_used": coaching_decision.chosen_approach,
            "adaptations_applied": coaching_decision.adaptations_applied,
            "confidence_level": coaching_decision.adaptation_confidence,
            "pattern_insights": coaching_decision.pattern_insights,
            "progress_insights": coaching_decision.progress_insights
        }
    
    async def _get_progress_update(self, user_id: str) -> Dict[str, Any]:
        """Get user's progress update"""
        
        try:
            # Get milestone progress
            next_milestones = self.celebration_system.get_next_milestones(user_id)
            
            # Get recent progress
            progress_summary = self.celebration_system.get_celebration_summary(user_id, "week")
            
            return {
                "next_milestones": next_milestones,
                "recent_achievements": len(progress_summary.get("achievements", [])),
                "progress_moments": len(progress_summary.get("progress_moments", []))
            }
            
        except Exception as e:
            logger.error(f"Error getting progress update: {e}")
            return {}
    
    async def _get_next_recommendations(self, user_id: str) -> List[str]:
        """Get next coaching recommendations"""
        
        try:
            # Get predictions
            predictions = self.user_learning_engine.predict_user_needs(user_id)
            
            recommendations = []
            
            # Add improvement opportunities
            if "improvement_opportunities" in predictions:
                recommendations.extend(predictions["improvement_opportunities"])
            
            # Add motivation needs
            if "motivation_needs" in predictions:
                recommendations.extend(predictions["motivation_needs"])
            
            # Add pattern-based recommendations
            patterns = self.pattern_recognizer.get_user_patterns(user_id)
            for pattern in patterns[:2]:  # Top 2 patterns
                if pattern.actionable_insights:
                    recommendations.extend(pattern.actionable_insights[:1])
            
            return recommendations[:3]  # Limit to top 3 recommendations
            
        except Exception as e:
            logger.error(f"Error getting recommendations: {e}")
            return ["Continue practicing consistently", "Focus on fundamentals"]
    
    def _update_performance_metrics(self, coaching_decision: CoachingDecision):
        """Update system performance metrics"""
        
        self.performance_metrics["total_interactions"] += 1
        
        # Update adaptation success rate
        if coaching_decision.adaptation_confidence > self.config.adaptation_confidence_threshold:
            current_success_rate = self.performance_metrics["adaptation_success_rate"]
            total_interactions = self.performance_metrics["total_interactions"]
            
            # Update running average
            new_success_rate = ((current_success_rate * (total_interactions - 1)) + 1) / total_interactions
            self.performance_metrics["adaptation_success_rate"] = new_success_rate
    
    async def _fallback_response(self, user_id: str, user_message: str, 
                               swing_analysis: Dict) -> Dict[str, Any]:
        """Fallback response when adaptive system fails"""
        
        logger.warning(f"Using fallback response for user {user_id}")
        
        # Try to use basic personality system
        if COACHING_PERSONALITIES and self.personality_selector:
            try:
                personality = self.personality_selector.recommend_personality({})
                personality_obj = COACHING_PERSONALITIES.get(personality)
                
                if personality_obj:
                    response = personality_obj.get_response_template("default")
                    if response:
                        return {
                            "response": response,
                            "achievements": [],
                            "coaching_insights": {"approach_used": "fallback"},
                            "progress_update": {},
                            "next_recommendations": ["Continue practicing"],
                            "system_confidence": 0.3
                        }
            except Exception as e:
                logger.error(f"Error in fallback personality system: {e}")
        
        # Ultimate fallback
        return {
            "response": "I'm here to help you improve your golf game. Let's work on your fundamentals and keep practicing!",
            "achievements": [],
            "coaching_insights": {"approach_used": "emergency_fallback"},
            "progress_update": {},
            "next_recommendations": ["Continue practicing", "Focus on consistency"],
            "system_confidence": 0.2
        }
    
    def get_system_status(self) -> Dict[str, Any]:
        """Get overall system status and health"""
        
        return {
            "system_health": {
                "adaptive_learning_active": self.config.enable_adaptive_learning,
                "pattern_recognition_active": self.config.enable_pattern_recognition,
                "effectiveness_tracking_active": self.config.enable_effectiveness_tracking,
                "celebration_system_active": self.config.enable_celebration_system
            },
            "performance_metrics": self.performance_metrics,
            "user_statistics": {
                "total_users": len(self.user_learning_engine.user_profiles),
                "active_conversations": len(self.memory_manager.conversation_contexts),
                "total_patterns_recognized": sum(
                    len(patterns) for patterns in self.pattern_recognizer.recognized_patterns.values()
                )
            },
            "integration_status": {
                "personality_system_available": COACHING_PERSONALITIES is not None,
                "coaching_agent_available": CoachingAgent is not None,
                "fallback_enabled": self.config.fallback_to_personality
            }
        }
    
    def get_user_insights(self, user_id: str) -> Dict[str, Any]:
        """Get comprehensive insights for a specific user"""
        
        try:
            insights = {
                "learning_profile": self.user_learning_engine.get_user_insights(user_id),
                "patterns": self.pattern_recognizer.get_pattern_insights(user_id),
                "effectiveness": self.effectiveness_tracker.get_effectiveness_insights(user_id),
                "progress": self.celebration_system.get_celebration_insights(user_id),
                "memory": self.memory_manager.get_memory_insights(user_id),
                "recent_decisions": [
                    {
                        "approach": d.chosen_approach,
                        "confidence": d.adaptation_confidence,
                        "adaptations": d.adaptations_applied,
                        "timestamp": d.created_at.isoformat()
                    }
                    for d in self.coaching_decisions.get(user_id, [])[-5:]
                ]
            }
            
            return insights
            
        except Exception as e:
            logger.error(f"Error getting user insights: {e}")
            return {"error": "Unable to retrieve user insights"}

# Convenience function for easy integration
def create_adaptive_coaching_system(config: AdaptiveCoachingConfig = None) -> AdaptiveCoachingOrchestrator:
    """Create and configure adaptive coaching system"""
    
    return AdaptiveCoachingOrchestrator(config or AdaptiveCoachingConfig())

# Export main classes for external use
__all__ = [
    "AdaptiveCoachingOrchestrator",
    "AdaptiveCoachingConfig", 
    "CoachingDecision",
    "create_adaptive_coaching_system"
]