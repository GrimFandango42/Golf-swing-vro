"""
Conversational Coaching System for SwingSync AI

This module implements a comprehensive conversational coaching system that integrates
with the existing SwingSync AI platform to provide natural, voice-driven coaching
experiences for golf swing improvement.

Key Features:
- Natural language conversation with coaching personalities
- Voice input/output with real-time processing
- Context-aware coaching based on swing analysis history
- Multi-modal integration combining visual analysis with conversation
- Personalized coaching styles and adaptive responses
- Real-time conversation flow with interruption handling

Components:
- voice_interface: Speech-to-text and text-to-speech processing
- conversation_engine: Core conversation logic and coaching agent
- integration: Integration with existing SwingSync AI components
- personalization: Adaptive conversation and user profiling
- config: Configuration and coaching personality definitions

Usage:
    from conversational_coaching import CoachingAgent, VoiceInterface
    
    voice = VoiceInterface()
    coach = CoachingAgent(voice_interface=voice)
    
    response = await coach.process_message(
        user_id="user123",
        session_id="session456", 
        message="How did I do with that swing?"
    )
"""

__version__ = "1.0.0"
__author__ = "SwingSync AI Team"

# Core imports
from .conversation_engine.coaching_agent import CoachingAgent
from .voice_interface.speech_interface import VoiceInterface
from .conversation_engine.context_manager import ContextManager, ConversationContext
from .conversation_engine.personality_manager import PersonalityManager, CoachingPersonality
from .integration.streaming_integration import RealTimeConversationManager
from .integration.multimodal_processor import MultimodalProcessor

# Configuration imports
from .config.coaching_profiles import COACHING_PERSONALITIES, CoachingStyle
from .config.conversation_settings import ConversationSettings

__all__ = [
    "CoachingAgent",
    "VoiceInterface", 
    "ContextManager",
    "ConversationContext",
    "PersonalityManager",
    "CoachingPersonality",
    "RealTimeConversationManager",
    "MultimodalProcessor",
    "COACHING_PERSONALITIES",
    "CoachingStyle",
    "ConversationSettings"
]