"""
Adaptive Coaching System - Intelligent Personalization Engine

This module provides an AI-powered coaching system that learns from each user's
unique patterns, preferences, and improvement trajectory to deliver increasingly
personalized golf instruction.

The system includes:
- User pattern learning and behavior analysis
- Coaching effectiveness tracking and adaptation
- Progress prediction and goal recommendation
- Memory and context management for session continuity
- Celebration and progress recognition
- Integration with existing coaching personalities

The adaptive coaching system makes users feel like they have a personal golf
instructor who remembers everything about their game and gets smarter over time.
"""

from .user_learning_engine import UserLearningEngine
from .pattern_recognition import PatternRecognizer
from .coaching_adaptation import CoachingAdaptationEngine
from .progress_predictor import ProgressPredictor
from .memory_context_manager import MemoryContextManager
from .user_profiling import UserProfilingSystem
from .effectiveness_tracker import EffectivenessTracker
from .celebration_system import CelebrationSystem
from .adaptive_integration import (
    AdaptiveCoachingOrchestrator, 
    AdaptiveCoachingConfig,
    create_adaptive_coaching_system
)

__version__ = "1.0.0"
__all__ = [
    "UserLearningEngine",
    "PatternRecognizer", 
    "CoachingAdaptationEngine",
    "ProgressPredictor",
    "MemoryContextManager",
    "UserProfilingSystem", 
    "EffectivenessTracker",
    "CelebrationSystem",
    "AdaptiveCoachingOrchestrator",
    "AdaptiveCoachingConfig",
    "create_adaptive_coaching_system"
]