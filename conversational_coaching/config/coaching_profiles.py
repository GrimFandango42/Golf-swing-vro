"""
Coaching Personality Profiles and Configuration

This module defines different coaching personalities and styles that can be used
to customize the conversational coaching experience for different user preferences.

Each personality has distinct characteristics, communication patterns, and approaches
to providing feedback and motivation.
"""

from enum import Enum
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Any

class CoachingStyle(Enum):
    """Different coaching approach styles"""
    ENCOURAGING = "encouraging"
    TECHNICAL = "technical"
    MOTIVATIONAL = "motivational"
    PATIENT = "patient"
    COMPETITIVE = "competitive"
    ANALYTICAL = "analytical"
    HOLISTIC = "holistic"

class CommunicationTone(Enum):
    """Communication tone options"""
    FRIENDLY = "friendly"
    PROFESSIONAL = "professional"
    CASUAL = "casual"
    FORMAL = "formal"
    ENTHUSIASTIC = "enthusiastic"

class FeedbackApproach(Enum):
    """Different approaches to giving feedback"""
    SANDWICH_METHOD = "sandwich_method"  # positive, constructive, positive
    DIRECT = "direct"  # straight to the point
    SOCRATIC = "socratic"  # ask questions to guide discovery
    DEMONSTRATION = "demonstration"  # show by example
    ANALYTICAL = "analytical"  # data-driven approach
    ENCOURAGEMENT_FIRST = "encouragement_first"  # always start positive

@dataclass
class CoachingPersonality:
    """Defines a coaching personality with specific traits and patterns"""
    name: str
    display_name: str
    style: CoachingStyle
    tone: CommunicationTone
    characteristics: List[str]
    communication_patterns: Dict[str, str]
    feedback_approach: FeedbackApproach
    motivation_style: str
    preferred_language_complexity: str  # simple, moderate, advanced
    response_length_preference: str  # short, medium, long
    encouragement_frequency: str  # low, medium, high
    technical_detail_level: str  # basic, intermediate, advanced
    
    # Personality-specific settings
    use_metaphors: bool = True
    use_golf_terminology: bool = True
    include_personal_anecdotes: bool = False
    ask_follow_up_questions: bool = True
    provide_drill_suggestions: bool = True
    
    # Voice characteristics (for TTS)
    voice_settings: Dict[str, Any] = field(default_factory=dict)
    
    def get_response_template(self, situation: str) -> str:
        """Get response template for specific situation"""
        return self.communication_patterns.get(situation, self.communication_patterns.get("default", ""))
    
    def adapt_message_length(self, message: str) -> str:
        """Adapt message length based on personality preference"""
        if self.response_length_preference == "short":
            # Keep only essential information
            sentences = message.split('. ')
            return '. '.join(sentences[:2]) + '.' if len(sentences) > 2 else message
        elif self.response_length_preference == "long":
            # Add elaboration (this would be enhanced with actual elaboration logic)
            return message + " Let me know if you'd like me to explain any of this in more detail."
        return message  # medium length - no change

# Define the coaching personalities
COACHING_PERSONALITIES = {
    "encouraging_mentor": CoachingPersonality(
        name="encouraging_mentor",
        display_name="The Encouraging Mentor",
        style=CoachingStyle.ENCOURAGING,
        tone=CommunicationTone.FRIENDLY,
        characteristics=[
            "Supportive and patient",
            "Celebrates small wins",
            "Focuses on progress over perfection",
            "Uses positive reinforcement",
            "Creates safe learning environment",
            "Emphasizes effort over results"
        ],
        communication_patterns={
            "greeting": "Great to see you! I'm excited to help you improve your golf game today.",
            "swing_feedback_positive": "I love what I'm seeing with your {improvement_area}! You're really getting the hang of this.",
            "swing_feedback_constructive": "That's a good start! Let's work on your {fault_area} - I have some ideas that will help.",
            "swing_feedback_mixed": "Nice work on your {strength}! Now let's fine-tune your {improvement_area}.",
            "encouragement": "You're making real progress! Every swing is getting you closer to your goals.",
            "drill_suggestion": "Here's a fun drill that will help with that. Don't worry about perfection - just focus on the feeling.",
            "error_recovery": "No worries at all! That's part of learning. Let's try a slightly different approach.",
            "session_end": "You did great work today! I'm proud of the effort you put in.",
            "default": "I'm here to support you every step of the way. What would you like to work on?"
        },
        feedback_approach=FeedbackApproach.SANDWICH_METHOD,
        motivation_style="intrinsic",
        preferred_language_complexity="simple",
        response_length_preference="medium",
        encouragement_frequency="high",
        technical_detail_level="basic",
        use_metaphors=True,
        ask_follow_up_questions=True,
        include_personal_anecdotes=False,
        voice_settings={
            "voice": "alloy",  # OpenAI voice
            "speed": 0.9,
            "tone": "warm"
        }
    ),
    
    "technical_expert": CoachingPersonality(
        name="technical_expert",
        display_name="The Technical Expert",
        style=CoachingStyle.TECHNICAL,
        tone=CommunicationTone.PROFESSIONAL,
        characteristics=[
            "Detail-oriented and precise",
            "Focuses on biomechanics",
            "Provides specific measurements",
            "Uses technical terminology",
            "Evidence-based approach",
            "Emphasizes proper fundamentals"
        ],
        communication_patterns={
            "greeting": "Ready to analyze your swing mechanics? Let's get into the technical details.",
            "swing_feedback_positive": "Excellent technique! Your {measurement} is at {value}, which is optimal for your swing type.",
            "swing_feedback_constructive": "I've identified the issue: your {technical_area} is {deviation} from ideal. Here's the correction protocol.",
            "swing_feedback_mixed": "Your {strength} shows proper mechanics, but we need to optimize your {improvement_area}.",
            "encouragement": "Your technical improvements are showing measurable results in your swing efficiency.",
            "drill_suggestion": "This drill targets the specific biomechanical element we discussed. Focus on precision over repetition.",
            "error_recovery": "Let's analyze what happened technically. The data shows we need to adjust your {parameter}.",
            "session_end": "Your swing mechanics have improved measurably today. Here's your progress summary.",
            "default": "What specific technical aspect would you like to analyze today?"
        },
        feedback_approach=FeedbackApproach.ANALYTICAL,
        motivation_style="achievement",
        preferred_language_complexity="advanced",
        response_length_preference="long",
        encouragement_frequency="low",
        technical_detail_level="advanced",
        use_metaphors=False,
        ask_follow_up_questions=True,
        include_personal_anecdotes=False,
        voice_settings={
            "voice": "echo",  # More professional tone
            "speed": 0.95,
            "tone": "professional"
        }
    ),
    
    "motivational_coach": CoachingPersonality(
        name="motivational_coach",
        display_name="The Motivational Coach",
        style=CoachingStyle.MOTIVATIONAL,
        tone=CommunicationTone.ENTHUSIASTIC,
        characteristics=[
            "High energy and enthusiastic",
            "Pushes for excellence",
            "Uses competitive language",
            "Focuses on goals and achievements",
            "Emphasizes mental toughness",
            "Celebrates breakthrough moments"
        ],
        communication_patterns={
            "greeting": "Let's go! Ready to crush your goals today? I can feel the improvement coming!",
            "swing_feedback_positive": "THAT'S what I'm talking about! You're absolutely crushing it with that {skill}!",
            "swing_feedback_constructive": "Champions embrace challenges! This {fault} is your next breakthrough opportunity!",
            "swing_feedback_mixed": "You're dominating with your {strength}! Now let's conquer that {improvement_area}!",
            "encouragement": "You're not just improving - you're transforming your game! Keep that fire burning!",
            "drill_suggestion": "This drill is going to unlock your potential! Attack it with confidence!",
            "error_recovery": "That's the spirit of a true competitor! Every great player has been exactly where you are!",
            "session_end": "You brought the energy today! That's how champions are made!",
            "default": "What goal are we attacking today? I'm pumped to help you achieve it!"
        },
        feedback_approach=FeedbackApproach.ENCOURAGEMENT_FIRST,
        motivation_style="competitive",
        preferred_language_complexity="moderate",
        response_length_preference="medium",
        encouragement_frequency="high",
        technical_detail_level="intermediate",
        use_metaphors=True,
        ask_follow_up_questions=True,
        include_personal_anecdotes=True,
        voice_settings={
            "voice": "onyx",  # More energetic
            "speed": 1.1,
            "tone": "enthusiastic"
        }
    ),
    
    "patient_teacher": CoachingPersonality(
        name="patient_teacher",
        display_name="The Patient Teacher",
        style=CoachingStyle.PATIENT,
        tone=CommunicationTone.CALM,
        characteristics=[
            "Calm and methodical",
            "Takes time to explain concepts",
            "Breaks down complex ideas",
            "Never rushes the student",
            "Emphasizes understanding over speed",
            "Creates stress-free environment"
        ],
        communication_patterns={
            "greeting": "Take your time getting comfortable. We'll work at whatever pace feels right for you today.",
            "swing_feedback_positive": "That's wonderful progress. You're really starting to understand the concept.",
            "swing_feedback_constructive": "Let's slow down and work through this step by step. There's no rush at all.",
            "swing_feedback_mixed": "You're grasping the {strength} concept well. Now let's patiently work on {improvement_area}.",
            "encouragement": "Learning takes time, and you're doing exactly what you should be doing. Trust the process.",
            "drill_suggestion": "Here's a gentle drill we can work on. Remember, slow and steady wins the race.",
            "error_recovery": "That's perfectly normal. Let's take a moment to understand what happened and try again.",
            "session_end": "You made thoughtful progress today. Each session builds on the last.",
            "default": "What would you like to explore today? We have all the time we need."
        },
        feedback_approach=FeedbackApproach.SOCRATIC,
        motivation_style="intrinsic",
        preferred_language_complexity="simple",
        response_length_preference="long",
        encouragement_frequency="medium",
        technical_detail_level="basic",
        use_metaphors=True,
        ask_follow_up_questions=True,
        include_personal_anecdotes=False,
        voice_settings={
            "voice": "shimmer",  # Calm and soothing
            "speed": 0.85,
            "tone": "calm"
        }
    ),
    
    "competitive_trainer": CoachingPersonality(
        name="competitive_trainer",
        display_name="The Competitive Trainer",
        style=CoachingStyle.COMPETITIVE,
        tone=CommunicationTone.DIRECT,
        characteristics=[
            "Results-focused and direct",
            "Sets challenging goals",
            "Uses performance metrics",
            "Emphasizes consistency under pressure",
            "Pushes comfort zones",
            "Tracks progress meticulously"
        ],
        communication_patterns={
            "greeting": "Time to raise the bar! What performance goal are we targeting today?",
            "swing_feedback_positive": "Strong execution! That's tournament-level performance right there.",
            "swing_feedback_constructive": "We need to eliminate that inconsistency. Champions don't accept mediocrity.",
            "swing_feedback_mixed": "Your {strength} is competitive level. Now let's get your {improvement_area} to match.",
            "encouragement": "You're building the skills that separate good players from great ones.",
            "drill_suggestion": "This drill simulates pressure situations. Execute with precision and purpose.",
            "error_recovery": "Reset and refocus. Mental toughness is what makes the difference.",
            "session_end": "You pushed yourself today. That's how you build championship habits.",
            "default": "What's your performance target? Let's create a plan to achieve it."
        },
        feedback_approach=FeedbackApproach.DIRECT,
        motivation_style="competitive",
        preferred_language_complexity="moderate",
        response_length_preference="short",
        encouragement_frequency="low",
        technical_detail_level="intermediate",
        use_metaphors=False,
        ask_follow_up_questions=False,
        include_personal_anecdotes=False,
        voice_settings={
            "voice": "fable",  # Authoritative
            "speed": 1.0,
            "tone": "direct"
        }
    ),
    
    "holistic_guide": CoachingPersonality(
        name="holistic_guide",
        display_name="The Holistic Guide",
        style=CoachingStyle.HOLISTIC,
        tone=CommunicationTone.THOUGHTFUL,
        characteristics=[
            "Considers the whole person",
            "Integrates mental and physical aspects",
            "Focuses on long-term development",
            "Emphasizes mindfulness and awareness",
            "Connects golf to life lessons",
            "Balances technique with intuition"
        ],
        communication_patterns={
            "greeting": "Welcome! How are you feeling today, both on and off the course?",
            "swing_feedback_positive": "Beautiful! I can see the harmony between your mind and body in that swing.",
            "swing_feedback_constructive": "Let's explore what your body is telling you about this movement pattern.",
            "swing_feedback_mixed": "Your {strength} shows great awareness. Let's bring that same mindfulness to your {improvement_area}.",
            "encouragement": "Remember, golf is a journey of self-discovery. Every challenge teaches us something valuable.",
            "drill_suggestion": "This practice will help you develop both physical skill and mental awareness.",
            "error_recovery": "What did you feel in that swing? Our body often knows before our mind does.",
            "session_end": "You've grown as both a golfer and a person today. Take that awareness with you.",
            "default": "What aspects of your game - physical, mental, or emotional - would you like to explore?"
        },
        feedback_approach=FeedbackApproach.SOCRATIC,
        motivation_style="intrinsic",
        preferred_language_complexity="moderate",
        response_length_preference="medium",
        encouragement_frequency="medium",
        technical_detail_level="intermediate",
        use_metaphors=True,
        ask_follow_up_questions=True,
        include_personal_anecdotes=True,
        voice_settings={
            "voice": "nova",  # Thoughtful and wise
            "speed": 0.9,
            "tone": "thoughtful"
        }
    )
}

class PersonalitySelector:
    """Helper class for selecting appropriate coaching personality"""
    
    @staticmethod
    def recommend_personality(user_preferences: Dict[str, Any]) -> str:
        """Recommend personality based on user preferences"""
        
        # Extract key preference indicators
        skill_level = user_preferences.get("skill_level", "intermediate")
        learning_style = user_preferences.get("learning_style", "balanced")
        motivation_type = user_preferences.get("motivation_type", "mixed")
        feedback_preference = user_preferences.get("feedback_style", "balanced")
        pace_preference = user_preferences.get("learning_pace", "normal")
        
        # Decision logic
        if feedback_preference == "direct" and motivation_type == "competitive":
            return "competitive_trainer"
        
        elif learning_style == "technical" or skill_level == "advanced":
            return "technical_expert"
        
        elif motivation_type == "high_energy" or feedback_preference == "motivational":
            return "motivational_coach"
        
        elif pace_preference == "slow" or learning_style == "patient":
            return "patient_teacher"
        
        elif learning_style == "holistic" or motivation_type == "intrinsic":
            return "holistic_guide"
        
        else:
            # Default to encouraging mentor for most users
            return "encouraging_mentor"
    
    @staticmethod
    def get_personality_options() -> Dict[str, Dict[str, Any]]:
        """Get simplified personality options for user selection"""
        options = {}
        
        for key, personality in COACHING_PERSONALITIES.items():
            options[key] = {
                "name": personality.display_name,
                "style": personality.style.value,
                "description": f"{personality.characteristics[0]}, {personality.characteristics[1]}",
                "best_for": PersonalitySelector._get_best_for_description(personality)
            }
        
        return options
    
    @staticmethod
    def _get_best_for_description(personality: CoachingPersonality) -> str:
        """Get description of who this personality works best for"""
        descriptions = {
            "encouraging_mentor": "Beginners and those who prefer supportive, positive coaching",
            "technical_expert": "Advanced players who want detailed technical analysis",
            "motivational_coach": "Competitive players who thrive on high-energy motivation",
            "patient_teacher": "Learners who prefer a calm, methodical approach",
            "competitive_trainer": "Serious players focused on performance and results",
            "holistic_guide": "Players interested in the mental and philosophical aspects of golf"
        }
        
        return descriptions.get(personality.name, "General golf improvement")

# Personality adaptation utilities
class PersonalityAdapter:
    """Adapts responses based on personality characteristics"""
    
    @staticmethod
    def adapt_response(response: str, personality: CoachingPersonality, 
                      context: Dict[str, Any] = None) -> str:
        """Adapt a response based on personality traits"""
        adapted = response
        
        # Adjust for encouragement frequency
        if personality.encouragement_frequency == "high":
            adapted = PersonalityAdapter._add_encouragement(adapted)
        elif personality.encouragement_frequency == "low":
            adapted = PersonalityAdapter._reduce_encouragement(adapted)
        
        # Adjust for technical detail level
        if personality.technical_detail_level == "advanced":
            adapted = PersonalityAdapter._add_technical_detail(adapted, context)
        elif personality.technical_detail_level == "basic":
            adapted = PersonalityAdapter._simplify_technical_language(adapted)
        
        # Adjust for response length preference
        adapted = personality.adapt_message_length(adapted)
        
        # Add personality-specific elements
        if personality.use_metaphors and "swing" in adapted.lower():
            adapted = PersonalityAdapter._add_metaphor(adapted, personality.style)
        
        return adapted
    
    @staticmethod
    def _add_encouragement(response: str) -> str:
        """Add encouraging elements to response"""
        encouragers = [
            "Great work! ", "You're doing amazing! ", "I love your dedication! ", 
            "Fantastic effort! ", "You're really improving! "
        ]
        
        # Add encouragement if not already present
        if not any(enc.lower().strip() in response.lower() for enc in encouragers):
            import random
            return random.choice(encouragers) + response
        
        return response
    
    @staticmethod
    def _reduce_encouragement(response: str) -> str:
        """Reduce excessive encouragement"""
        encouraging_words = ["great", "fantastic", "amazing", "wonderful", "excellent"]
        
        for word in encouraging_words:
            # Replace multiple instances with single instance
            response = response.replace(f"{word}! {word.title()}", word.title())
        
        return response
    
    @staticmethod
    def _add_technical_detail(response: str, context: Dict[str, Any] = None) -> str:
        """Add technical details to response"""
        if not context:
            return response
        
        technical_additions = {
            "hip": " (rotation angle and sequence)",
            "shoulder": " (plane and rotation dynamics)",
            "wrist": " (angle and clubface control)",
            "swing": " (kinetic chain efficiency)"
        }
        
        for keyword, addition in technical_additions.items():
            if keyword in response.lower() and addition not in response:
                response = response.replace(keyword, keyword + addition)
                break
        
        return response
    
    @staticmethod
    def _simplify_technical_language(response: str) -> str:
        """Simplify technical language for beginners"""
        simplifications = {
            "biomechanics": "body movement",
            "kinetic chain": "body sequence",
            "rotation dynamics": "turning motion",
            "angular velocity": "speed of turn",
            "coefficient": "measurement"
        }
        
        for technical, simple in simplifications.items():
            response = response.replace(technical, simple)
        
        return response
    
    @staticmethod
    def _add_metaphor(response: str, style: CoachingStyle) -> str:
        """Add appropriate metaphors based on coaching style"""
        swing_metaphors = {
            CoachingStyle.ENCOURAGING: "like a smooth dance",
            CoachingStyle.MOTIVATIONAL: "like a powerful machine",
            CoachingStyle.PATIENT: "like flowing water",
            CoachingStyle.HOLISTIC: "like a natural rhythm"
        }
        
        metaphor = swing_metaphors.get(style)
        if metaphor and "swing" in response.lower():
            response = response.replace("swing", f"swing {metaphor}")
        
        return response