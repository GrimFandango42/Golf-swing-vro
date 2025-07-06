"""
Coaching Agent - Core Conversational AI for Golf Instruction

This module implements the main coaching agent that manages conversational interactions
for golf instruction, integrating with the existing SwingSync AI analysis pipeline.

Features:
- Natural conversation flow management
- Context-aware coaching responses
- Integration with swing analysis results
- Personality-driven coaching styles
- Multi-turn conversation capabilities
- Real-time coaching feedback
"""

import asyncio
import json
import logging
from typing import Dict, List, Optional, Any, AsyncGenerator
from datetime import datetime, timedelta
from enum import Enum
from dataclasses import dataclass, field

# Import from existing SwingSync modules
try:
    from feedback_generation import (
        generate_swing_analysis_feedback,
        StreamingFeedbackGenerator,
        FeedbackContext,
        FeedbackMode,
        UserSkillLevel
    )
    from data_structures import SwingAnalysisFeedback, LLMGeneratedTip
except ImportError:
    # Fallback for development
    logging.warning("SwingSync modules not available - using mock implementations")

logger = logging.getLogger(__name__)

class ConversationState(Enum):
    GREETING = "greeting"
    ACTIVE_COACHING = "active_coaching"
    SWING_ANALYSIS = "swing_analysis"
    DRILL_INSTRUCTION = "drill_instruction"
    GOAL_SETTING = "goal_setting"
    PROBLEM_SOLVING = "problem_solving"
    ENCOURAGEMENT = "encouragement"
    WRAP_UP = "wrap_up"

class CoachingMode(Enum):
    REAL_TIME = "real_time"      # Immediate feedback during practice
    ANALYTICAL = "analytical"    # Detailed post-swing analysis
    INSTRUCTIONAL = "instructional"  # Teaching specific techniques
    MOTIVATIONAL = "motivational"    # Focus on encouragement and goals

@dataclass
class ConversationMetrics:
    """Track conversation quality and engagement metrics"""
    total_exchanges: int = 0
    average_response_time: float = 0.0
    user_satisfaction_score: float = 0.0
    coaching_effectiveness: float = 0.0
    conversation_coherence: float = 0.0
    last_updated: datetime = field(default_factory=datetime.now)

class CoachingAgent:
    """Main conversational coaching agent"""
    
    def __init__(self, 
                 personality_manager=None,
                 context_manager=None,
                 voice_interface=None,
                 streaming_feedback_generator=None):
        self.personality_manager = personality_manager
        self.context_manager = context_manager
        self.voice_interface = voice_interface
        self.streaming_feedback_generator = streaming_feedback_generator or StreamingFeedbackGenerator()
        
        # Conversation state management
        self.conversation_states: Dict[str, ConversationState] = {}
        self.conversation_metrics: Dict[str, ConversationMetrics] = {}
        self.active_topics: Dict[str, List[str]] = {}
        
        # Initialize conversation templates
        self._load_conversation_templates()
        
        logger.info("Coaching agent initialized")
    
    def _load_conversation_templates(self):
        """Load conversation templates and patterns"""
        self.conversation_templates = {
            "greeting": {
                "new_user": "Hi there! I'm your AI golf coach. I'm excited to help you improve your swing! What would you like to work on today?",
                "returning_user": "Welcome back, {name}! Ready to continue working on your golf game? I remember we were focusing on {last_topic}.",
                "session_start": "Great to see you! Let's get started with today's practice session. How are you feeling about your swing today?"
            },
            "swing_feedback": {
                "positive": "That's looking much better! I can see real improvement in your {improvement_area}.",
                "constructive": "I noticed something we can work on with your {fault_area}. Here's what I suggest...",
                "mixed": "Good progress with your {strength}, and I have some ideas to help with your {improvement_area}."
            },
            "encouragement": {
                "struggling": "I know this feels challenging right now, but you're making progress! Every great golfer has worked through these same fundamentals.",
                "plateau": "It's normal to feel like you're not improving as fast as you'd like. Let's try a different approach to break through this plateau.",
                "frustrated": "Take a deep breath! Golf is a journey, and every swing is a learning opportunity. You've got this!"
            },
            "drill_instruction": {
                "introduction": "Let me walk you through a drill that will help with {target_area}. This is one of my favorites because it really works!",
                "step_by_step": "Step {step_number}: {instruction}. Take your time with this - proper form is more important than speed.",
                "validation": "Perfect! That's exactly what I want to see. How did that feel to you?"
            }
        }
    
    async def start_conversation(self, user_id: str, session_id: str, 
                               user_preferences: Dict[str, Any] = None) -> str:
        """Start a new coaching conversation"""
        conversation_key = f"{user_id}:{session_id}"
        
        # Initialize conversation state
        self.conversation_states[conversation_key] = ConversationState.GREETING
        self.conversation_metrics[conversation_key] = ConversationMetrics()
        self.active_topics[conversation_key] = []
        
        # Get user context if available
        context = None
        if self.context_manager:
            context = await self.context_manager.get_context(user_id, session_id)
        
        # Generate personalized greeting
        greeting = await self._generate_greeting(user_id, context, user_preferences)
        
        # Update conversation history
        if context:
            context.add_message("assistant", greeting, {"conversation_state": "greeting"})
            await self.context_manager.save_context(context)
        
        logger.info(f"Started conversation for user {user_id}, session {session_id}")
        return greeting
    
    async def process_message(self, user_id: str, session_id: str, 
                            message: str, swing_analysis: Optional[Dict] = None,
                            voice_mode: bool = False) -> str:
        """Process user message and generate coaching response"""
        start_time = datetime.now()
        conversation_key = f"{user_id}:{session_id}"
        
        try:
            # Get conversation context
            context = None
            if self.context_manager:
                context = await self.context_manager.get_context(user_id, session_id)
            
            # Add user message to context
            if context:
                context.add_message("user", message, {
                    "voice_mode": voice_mode,
                    "has_swing_analysis": swing_analysis is not None
                })
            
            # Process voice commands if applicable
            if voice_mode and self.voice_interface:
                command_result = self.voice_interface.command_processor.process_command(message)
                if command_result["command"] != "conversation":
                    return await self._handle_voice_command(command_result, context)
            
            # Analyze user intent and emotional state
            intent_analysis = await self._analyze_user_intent(message, context)
            
            # Add swing analysis to context if provided
            if swing_analysis and context:
                context.add_swing_analysis(swing_analysis)
            
            # Generate contextual response
            response = await self._generate_contextual_response(
                context, intent_analysis, swing_analysis, voice_mode
            )
            
            # Add response to context
            if context:
                context.add_message("assistant", response, {
                    "intent": intent_analysis,
                    "conversation_state": self.conversation_states.get(conversation_key, ConversationState.ACTIVE_COACHING).value
                })
                await self.context_manager.save_context(context)
            
            # Update conversation metrics
            await self._update_conversation_metrics(conversation_key, start_time, response)
            
            return response
        
        except Exception as e:
            logger.error(f"Error processing message: {e}")
            return "I'm sorry, I had trouble processing that. Could you try rephrasing your question?"
    
    async def _generate_greeting(self, user_id: str, context, user_preferences: Dict) -> str:
        """Generate personalized greeting based on user history"""
        templates = self.conversation_templates["greeting"]
        
        # Determine greeting type
        if not context or not context.conversation_history:
            # New user
            greeting = templates["new_user"]
        else:
            # Returning user
            user_name = user_preferences.get("preferred_name", "")
            last_topic = self._get_last_topic(context)
            
            greeting = templates["returning_user"].format(
                name=user_name,
                last_topic=last_topic or "improving your swing"
            )
        
        # Personalize based on preferences
        if user_preferences.get("coaching_style") == "motivational":
            greeting += " I can't wait to see how much you've improved!"
        elif user_preferences.get("coaching_style") == "technical":
            greeting += " Let's dive into the technical aspects of your swing."
        
        return greeting
    
    async def _analyze_user_intent(self, message: str, context) -> Dict[str, Any]:
        """Analyze user intent and emotional state"""
        message_lower = message.lower()
        
        intent_analysis = {
            "primary_intent": "general_conversation",
            "emotional_state": "neutral",
            "urgency": "normal",
            "topic": "general",
            "confidence": 0.5
        }
        
        # Intent detection patterns
        intent_patterns = {
            "request_feedback": ["how did I do", "feedback", "analyze", "what do you think"],
            "ask_for_help": ["help", "struggling", "don't understand", "confused"],
            "request_drill": ["drill", "exercise", "practice", "what should I practice"],
            "express_frustration": ["frustrated", "angry", "not working", "difficult", "hard"],
            "seek_encouragement": ["discouraged", "giving up", "not good enough", "terrible"],
            "ask_question": ["why", "how", "what", "when", "where"],
            "report_progress": ["better", "improved", "getting", "feel like"],
            "set_goals": ["goal", "want to", "hope to", "target", "achieve"]
        }
        
        # Emotional state patterns
        emotion_patterns = {
            "frustrated": ["frustrated", "annoyed", "angry", "mad"],
            "discouraged": ["discouraged", "sad", "disappointed", "giving up"],
            "excited": ["excited", "great", "awesome", "amazing", "love"],
            "confident": ["confident", "ready", "feeling good", "on track"],
            "confused": ["confused", "don't understand", "unclear", "lost"]
        }
        
        # Detect intent
        for intent, patterns in intent_patterns.items():
            if any(pattern in message_lower for pattern in patterns):
                intent_analysis["primary_intent"] = intent
                intent_analysis["confidence"] = 0.8
                break
        
        # Detect emotional state
        for emotion, patterns in emotion_patterns.items():
            if any(pattern in message_lower for pattern in patterns):
                intent_analysis["emotional_state"] = emotion
                break
        
        # Detect topic
        topic_keywords = {
            "swing_mechanics": ["swing", "form", "technique", "mechanics"],
            "putting": ["putt", "putting", "green"],
            "driving": ["drive", "driver", "distance", "power"],
            "accuracy": ["accuracy", "straight", "direction", "aim"],
            "consistency": ["consistent", "consistency", "repeatable"]
        }
        
        for topic, keywords in topic_keywords.items():
            if any(keyword in message_lower for keyword in keywords):
                intent_analysis["topic"] = topic
                break
        
        return intent_analysis
    
    async def _generate_contextual_response(self, context, intent_analysis: Dict, 
                                          swing_analysis: Optional[Dict], 
                                          voice_mode: bool) -> str:
        """Generate contextual coaching response"""
        
        intent = intent_analysis["primary_intent"]
        emotional_state = intent_analysis["emotional_state"]
        topic = intent_analysis["topic"]
        
        # Handle different intents
        if intent == "request_feedback" and swing_analysis:
            return await self._generate_swing_feedback_response(swing_analysis, context, voice_mode)
        
        elif intent == "ask_for_help" or emotional_state == "confused":
            return await self._generate_help_response(topic, context)
        
        elif intent == "request_drill":
            return await self._generate_drill_response(topic, context)
        
        elif emotional_state in ["frustrated", "discouraged"]:
            return await self._generate_encouragement_response(emotional_state, context)
        
        elif intent == "ask_question":
            return await self._generate_educational_response(topic, context)
        
        elif intent == "report_progress":
            return await self._generate_progress_response(context)
        
        elif intent == "set_goals":
            return await self._generate_goal_setting_response(context)
        
        else:
            return await self._generate_conversational_response(context, intent_analysis)
    
    async def _generate_swing_feedback_response(self, swing_analysis: Dict, 
                                              context, voice_mode: bool) -> str:
        """Generate response based on swing analysis"""
        
        # Extract key information from analysis
        summary = swing_analysis.get("summary_of_findings", "")
        faults = swing_analysis.get("raw_detected_faults", [])
        detailed_feedback = swing_analysis.get("detailed_feedback", [])
        
        # Determine coaching approach based on results
        if not faults:
            # Great swing
            response = "Excellent work! That swing looked really solid. "
            if voice_mode:
                response += "I can see your hard work is paying off. What would you like to work on next?"
            else:
                response += "Your fundamentals are looking strong. " + summary
        
        elif len(faults) == 1:
            # Single issue to address
            fault = faults[0]
            fault_name = fault.get("fault_name", "").replace("_", " ").title()
            
            response = f"I noticed we can improve your {fault_name.lower()}. "
            
            if detailed_feedback and detailed_feedback[0]:
                tip = detailed_feedback[0].get("tip", "")
                response += tip[:100] + "..." if len(tip) > 100 else tip
            
            if voice_mode:
                response += " Would you like me to walk you through a drill for this?"
        
        else:
            # Multiple issues - prioritize
            primary_fault = max(faults, key=lambda f: f.get("severity", 0))
            fault_name = primary_fault.get("fault_name", "").replace("_", " ").title()
            
            response = f"I see a few things we can work on, but let's focus on your {fault_name.lower()} first. "
            response += "Once we get that dialed in, the other improvements will follow naturally."
        
        return response
    
    async def _generate_help_response(self, topic: str, context) -> str:
        """Generate helpful response for user questions"""
        
        help_responses = {
            "swing_mechanics": "I'd be happy to help with your swing mechanics! The key fundamentals are setup, takeaway, rotation, and follow-through. What specific part feels challenging?",
            "putting": "Putting is all about consistency and feel. The most important elements are alignment, tempo, and distance control. What aspect of putting would you like to focus on?",
            "driving": "For better driving, we want to focus on power transfer and accuracy. This comes from proper setup, rotation, and timing. Are you looking for more distance or better accuracy?",
            "accuracy": "Accuracy comes from consistent fundamentals and good course management. Let's work on your setup and alignment first. What's your typical miss pattern?",
            "general": "I'm here to help with any aspect of your golf game! Whether it's swing mechanics, course strategy, or mental approach, just let me know what you're working on."
        }
        
        return help_responses.get(topic, help_responses["general"])
    
    async def _generate_drill_response(self, topic: str, context) -> str:
        """Generate drill suggestions based on topic"""
        
        drill_responses = {
            "swing_mechanics": "Here's a great drill for swing mechanics: the 'Slow Motion Swing.' Take your normal setup, then swing at 25% speed, focusing on each position. This helps build muscle memory for proper form.",
            "putting": "Try the 'Gate Drill' for putting: Place two tees just wider than your putter head about 6 inches in front of the ball. Practice rolling the ball through the gate to improve your stroke path.",
            "driving": "For driving, try the 'Step and Swing' drill: Start with feet together, then step into your shot as you swing. This helps with weight transfer and timing.",
            "accuracy": "For accuracy, practice the 'Alignment Stick Drill': Place an alignment stick on the ground pointing at your target. This helps train proper setup and aim.",
            "general": "Let's start with the 'Balance and Tempo' drill: Make slow, balanced swings focusing on staying centered. This builds the foundation for everything else."
        }
        
        response = drill_responses.get(topic, drill_responses["general"])
        response += " Would you like me to break this down step by step?"
        
        return response
    
    async def _generate_encouragement_response(self, emotional_state: str, context) -> str:
        """Generate encouraging response based on emotional state"""
        
        encouragement_templates = self.conversation_templates["encouragement"]
        
        if emotional_state == "frustrated":
            response = encouragement_templates["frustrated"]
            response += " What specifically is feeling frustrating right now? Let's tackle it together."
        
        elif emotional_state == "discouraged":
            response = encouragement_templates["plateau"]
            response += " Remember, improvement in golf isn't always linear. Sometimes we need to take a step back to move forward."
        
        else:
            response = encouragement_templates["struggling"]
            response += " What part would you like to work on first?"
        
        return response
    
    async def _generate_educational_response(self, topic: str, context) -> str:
        """Generate educational response for user questions"""
        
        educational_content = {
            "swing_mechanics": "Golf swing mechanics involve a kinetic chain - energy transfers from the ground up through your legs, hips, torso, arms, and finally to the club. Each segment must work in sequence for maximum efficiency.",
            "putting": "Putting success comes from three key factors: reading the green correctly, starting the ball on the right line, and controlling distance. The stroke itself should be like a pendulum - smooth and consistent.",
            "driving": "Distance in driving comes from clubhead speed and solid contact. Speed is generated through proper sequence (lower body leads, upper body follows) and good tempo.",
            "accuracy": "Accuracy is primarily about consistency in your setup and swing path. Small variations in alignment or swing plane can cause big misses downrange."
        }
        
        return educational_content.get(topic, "That's a great question! Could you be more specific about what you'd like to learn about?")
    
    async def _generate_progress_response(self, context) -> str:
        """Generate response acknowledging user progress"""
        
        responses = [
            "That's fantastic to hear! Progress in golf is so rewarding. What feels different about your swing now?",
            "I love hearing about improvement! What do you think has made the biggest difference?",
            "Excellent! That hard work is paying off. How does it feel when you make good contact now?",
            "That's great progress! Building on success is the best way to continue improving."
        ]
        
        # Choose response based on conversation history
        import random
        return random.choice(responses)
    
    async def _generate_goal_setting_response(self, context) -> str:
        """Generate response for goal setting conversations"""
        
        return ("I love that you're setting goals! Specific, achievable goals are the key to improvement. "
                "What would you like to accomplish in the next few weeks? "
                "It could be technical, like improving your hip rotation, or practical, like breaking 90.")
    
    async def _generate_conversational_response(self, context, intent_analysis: Dict) -> str:
        """Generate general conversational response"""
        
        return ("I'm here to help you improve your golf game! "
                "Feel free to ask me about your swing, request drills, "
                "or just let me know what you're working on. "
                "What would you like to focus on today?")
    
    async def _handle_voice_command(self, command_result: Dict, context) -> str:
        """Handle specific voice commands"""
        
        command = command_result["command"]
        
        if command == "start_practice":
            return "Perfect! Let's start your practice session. I'll provide feedback as you go. Take a few practice swings when you're ready."
        
        elif command == "end_practice":
            return "Great job today! You're making real progress. Keep up the excellent work, and I'll see you next time!"
        
        elif command == "analyze_swing":
            return "I'd be happy to analyze your swing! Go ahead and take a swing, and I'll give you detailed feedback."
        
        elif command == "get_tips":
            return "I have some great tips for you! Based on your recent swings, I'd suggest focusing on your setup position. Good fundamentals lead to consistent results."
        
        elif command == "repeat":
            # Get last assistant message
            if context and context.conversation_history:
                last_messages = [msg for msg in context.conversation_history if msg["role"] == "assistant"]
                if last_messages:
                    return "I said: " + last_messages[-1]["content"]
            return "I'm sorry, I don't have anything to repeat right now."
        
        elif command == "slow_down":
            return "Of course! I'll speak more slowly and break things down into smaller steps."
        
        elif command == "be_quiet":
            return "No problem! I'll reduce my feedback and only speak when you ask me to."
        
        elif command == "help":
            if self.voice_interface:
                return self.voice_interface.command_processor.get_command_help()
            return "I can help you with swing analysis, drills, tips, and answering questions about golf technique."
        
        else:
            return "I understood that as a command, but I'm not sure how to help with that specific request."
    
    def _get_last_topic(self, context) -> Optional[str]:
        """Extract the last topic discussed from conversation history"""
        if not context or not context.conversation_history:
            return None
        
        # Look for topics in recent messages
        recent_messages = context.conversation_history[-5:]
        topics = []
        
        for message in recent_messages:
            content = message.get("content", "").lower()
            metadata = message.get("metadata", {})
            
            # Check metadata for topic
            if "topic" in metadata:
                topics.append(metadata["topic"])
            
            # Check content for topic keywords
            if "swing" in content:
                topics.append("swing mechanics")
            elif "putting" in content:
                topics.append("putting")
            elif "driving" in content:
                topics.append("driving")
        
        return topics[-1] if topics else None
    
    async def _update_conversation_metrics(self, conversation_key: str, 
                                         start_time: datetime, response: str):
        """Update conversation quality metrics"""
        if conversation_key not in self.conversation_metrics:
            self.conversation_metrics[conversation_key] = ConversationMetrics()
        
        metrics = self.conversation_metrics[conversation_key]
        
        # Update response time
        response_time = (datetime.now() - start_time).total_seconds()
        metrics.total_exchanges += 1
        metrics.average_response_time = (
            (metrics.average_response_time * (metrics.total_exchanges - 1) + response_time) / 
            metrics.total_exchanges
        )
        
        # Update coherence score (simplified)
        response_length = len(response)
        if 50 <= response_length <= 200:  # Optimal response length
            metrics.conversation_coherence = min(metrics.conversation_coherence + 0.1, 1.0)
        
        metrics.last_updated = datetime.now()
    
    async def get_conversation_summary(self, user_id: str, session_id: str) -> Dict[str, Any]:
        """Get summary of conversation session"""
        conversation_key = f"{user_id}:{session_id}"
        
        summary = {
            "session_id": session_id,
            "user_id": user_id,
            "metrics": self.conversation_metrics.get(conversation_key, ConversationMetrics()).__dict__,
            "topics_discussed": self.active_topics.get(conversation_key, []),
            "current_state": self.conversation_states.get(conversation_key, ConversationState.ACTIVE_COACHING).value
        }
        
        # Add context summary if available
        if self.context_manager:
            context = await self.context_manager.get_context(user_id, session_id)
            if context:
                summary["total_messages"] = len(context.conversation_history)
                summary["session_duration"] = (
                    datetime.now() - datetime.fromisoformat(context.conversation_history[0]["timestamp"])
                ).total_seconds() if context.conversation_history else 0
        
        return summary