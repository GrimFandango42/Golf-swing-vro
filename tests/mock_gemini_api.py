"""
Mock Gemini API Module for SwingSync AI Testing.

This module provides comprehensive mock responses for Google's Gemini 2.5 Flash API
to enable testing without incurring API costs or requiring network connectivity.

Key Features:
- Realistic mock responses based on swing analysis context
- Configurable response patterns and variations
- Error simulation for testing error handling
- Performance simulation with configurable delays
- Response quality variation for testing edge cases
- Context-aware feedback generation based on faults
- Streaming response simulation
- Rate limiting simulation

Mock Response Types:
- Standard swing analysis feedback
- Real-time streaming responses
- Error conditions and edge cases
- Performance variations
- Different skill level adaptations
"""

import asyncio
import json
import random
import time
from typing import Dict, List, Any, Optional, Callable
from dataclasses import dataclass
from enum import Enum
from unittest.mock import AsyncMock, Mock

class MockResponseQuality(Enum):
    """Quality levels for mock responses"""
    EXCELLENT = "excellent"
    GOOD = "good"
    AVERAGE = "average"
    POOR = "poor"
    ERROR = "error"

class MockResponseMode(Enum):
    """Response modes for different testing scenarios"""
    REALISTIC = "realistic"
    FAST = "fast"
    SLOW = "slow"
    STREAMING = "streaming"
    ERROR_PRONE = "error_prone"

@dataclass
class MockGeminiConfig:
    """Configuration for mock Gemini API behavior"""
    response_quality: MockResponseQuality = MockResponseQuality.GOOD
    response_mode: MockResponseMode = MockResponseMode.REALISTIC
    response_delay_ms: Optional[int] = None
    error_rate: float = 0.0  # 0.0 to 1.0
    streaming_chunk_delay_ms: int = 50
    max_response_length: int = 2000
    include_technical_details: bool = True
    adapt_to_skill_level: bool = True

class MockGeminiAPI:
    """Mock implementation of Gemini 2.5 Flash API for testing"""
    
    def __init__(self, config: MockGeminiConfig = None):
        self.config = config or MockGeminiConfig()
        self.call_count = 0
        self.response_history = []
        self.fault_response_templates = self._load_fault_templates()
        self.skill_adaptations = self._load_skill_adaptations()
    
    def _load_fault_templates(self) -> Dict[str, Dict[str, str]]:
        """Load response templates for different fault types"""
        return {
            "insufficient_shoulder_turn": {
                "explanation": "Your backswing shows restricted shoulder rotation, limiting your power potential and affecting swing plane consistency.",
                "tip": "Focus on making a fuller shoulder turn while maintaining your spine angle. Think about turning your back to the target.",
                "drill": "Practice the 'Wall Drill': Stand with your back to a wall at address, then turn until your lead shoulder touches the wall.",
                "technical": "Shoulder rotation measured at {observed_value} degrees, ideal range is {ideal_range} degrees."
            },
            "excessive_hip_hinge": {
                "explanation": "Your setup position shows excessive forward bend from the hips, which can affect balance and swing mechanics.",
                "tip": "Stand more upright at address while maintaining athletic posture. Your spine should have a slight forward tilt, not excessive bend.",
                "drill": "Practice the 'Mirror Drill': Set up in front of a mirror and find the position where you look athletic but not hunched over.",
                "technical": "Hip hinge angle measured at {observed_value} degrees, ideal range is {ideal_range} degrees."
            },
            "cupped_wrist": {
                "explanation": "At the top of your backswing, your lead wrist is cupped (extended), which can lead to an open clubface and inconsistent ball striking.",
                "tip": "Focus on maintaining a flat or slightly bowed lead wrist position at the top. Feel like the back of your lead hand points toward the sky.",
                "drill": "Practice slow-motion swings feeling the lead wrist staying flat. Use the 'Logo Drill' - keep a logo on your glove facing the target.",
                "technical": "Lead wrist angle measured at {observed_value} degrees of cupping, ideal is flat to slightly bowed."
            },
            "lateral_sway": {
                "explanation": "Your swing shows lateral movement (sway) rather than rotational movement, which reduces power and consistency.",
                "tip": "Focus on rotating around a stable spine angle rather than sliding side to side. Your head should stay relatively centered.",
                "drill": "Practice with a wall or obstacle behind your trail side to prevent sway. Feel like you're turning in a barrel.",
                "technical": "Lateral sway measured at {observed_value}, ideal is minimal lateral movement (< 5cm)."
            },
            "reverse_spine_angle": {
                "explanation": "At the top of your backswing, your upper body tilts toward the target (reverse spine angle), which can cause inconsistent contact.",
                "tip": "Maintain your spine tilt away from the target throughout the backswing. Feel like your trail shoulder moves down and back.",
                "drill": "Practice the 'Spine Angle Drill': Keep a club across your shoulders and maintain the tilt as you turn.",
                "technical": "Reverse spine angle measured at {observed_value} degrees, should maintain original spine tilt."
            },
            "poor_weight_distribution": {
                "explanation": "Your weight distribution at address is not optimal for the club you're using, affecting your angle of attack and ball contact.",
                "tip": "Adjust your weight distribution based on the club: slightly favoring trail foot for driver, balanced for irons, slightly forward for wedges.",
                "drill": "Practice with a pressure plate or scale under each foot to feel proper weight distribution for different clubs.",
                "technical": "Weight distribution measured at {observed_value}% on lead foot, ideal for this club is {ideal_range}%."
            }
        }
    
    def _load_skill_adaptations(self) -> Dict[str, Dict[str, str]]:
        """Load skill level adaptations for responses"""
        return {
            "beginner": {
                "tone": "encouraging and simple",
                "focus": "basic fundamentals",
                "complexity": "low",
                "encouragement": "Remember, every golfer was a beginner once. Focus on one thing at a time and be patient with yourself."
            },
            "intermediate": {
                "tone": "instructional and detailed",
                "focus": "swing mechanics",
                "complexity": "medium", 
                "encouragement": "You're developing good fundamentals. These adjustments will help take your game to the next level."
            },
            "advanced": {
                "tone": "technical and precise",
                "focus": "fine-tuning",
                "complexity": "high",
                "encouragement": "Your swing shows good fundamentals. These refinements will help optimize your performance."
            },
            "professional": {
                "tone": "analytical and comprehensive",
                "focus": "optimization",
                "complexity": "very high",
                "encouragement": "These biomechanical insights can help maintain consistency under pressure."
            }
        }
    
    async def generate_feedback(self, 
                               swing_input: Dict[str, Any], 
                               detected_faults: List[Dict[str, Any]],
                               user_skill_level: str = "intermediate") -> Dict[str, Any]:
        """Generate mock feedback for swing analysis"""
        
        self.call_count += 1
        
        # Simulate API delay
        await self._simulate_delay()
        
        # Check for error simulation
        if self._should_simulate_error():
            raise Exception("Mock Gemini API error: Rate limit exceeded")
        
        # Generate response based on faults and configuration
        response = await self._generate_fault_based_response(
            swing_input, detected_faults, user_skill_level
        )
        
        # Store response history
        self.response_history.append({
            "timestamp": time.time(),
            "input_session_id": swing_input.get("session_id"),
            "fault_count": len(detected_faults),
            "response_length": len(response.get("summary_of_findings", "")),
            "skill_level": user_skill_level
        })
        
        return response
    
    async def generate_streaming_feedback(self,
                                        frame_analysis: Dict[str, Any],
                                        session_context: Dict[str, Any]) -> Dict[str, Any]:
        """Generate mock real-time streaming feedback"""
        
        await self._simulate_delay(base_delay_ms=50)  # Faster for streaming
        
        if self._should_simulate_error():
            return {"error": "Streaming analysis temporarily unavailable"}
        
        faults = frame_analysis.get("detected_faults", [])
        
        if not faults:
            return None
        
        # Get primary fault for streaming feedback
        primary_fault = faults[0]
        fault_type = self._identify_fault_type(primary_fault.get("fault_id", ""))
        
        if fault_type in self.fault_response_templates:
            template = self.fault_response_templates[fault_type]
            
            return {
                "type": "instant_tip",
                "primary_fault": primary_fault.get("fault_name", "Unknown"),
                "quick_tip": template["tip"],
                "severity": primary_fault.get("severity", 0.5),
                "confidence": random.uniform(0.8, 0.95),
                "timestamp": time.time()
            }
        
        return {
            "type": "generic_tip",
            "message": "Focus on maintaining good posture and balance throughout your swing.",
            "confidence": 0.7,
            "timestamp": time.time()
        }
    
    async def _generate_fault_based_response(self,
                                           swing_input: Dict[str, Any],
                                           detected_faults: List[Dict[str, Any]],
                                           user_skill_level: str) -> Dict[str, Any]:
        """Generate comprehensive response based on detected faults"""
        
        club_used = swing_input.get("club_used", "Unknown")
        skill_adaptation = self.skill_adaptations.get(user_skill_level, self.skill_adaptations["intermediate"])
        
        if not detected_faults:
            return self._generate_positive_feedback(club_used, skill_adaptation)
        
        # Generate summary
        summary = await self._generate_summary(detected_faults, club_used, skill_adaptation)
        
        # Generate detailed feedback for each fault
        detailed_feedback = []
        for fault in detected_faults[:3]:  # Limit to top 3 faults
            fault_feedback = await self._generate_fault_feedback(fault, skill_adaptation)
            if fault_feedback:
                detailed_feedback.append(fault_feedback)
        
        return {
            "summary_of_findings": summary,
            "detailed_feedback": detailed_feedback,
            "overall_assessment": self._generate_overall_assessment(detected_faults, skill_adaptation),
            "next_steps": self._generate_next_steps(detected_faults, skill_adaptation),
            "confidence_score": random.uniform(0.85, 0.95),
            "analysis_metadata": {
                "faults_analyzed": len(detected_faults),
                "skill_level": user_skill_level,
                "club_used": club_used,
                "response_quality": self.config.response_quality.value
            }
        }
    
    async def _generate_summary(self,
                               detected_faults: List[Dict[str, Any]],
                               club_used: str,
                               skill_adaptation: Dict[str, str]) -> str:
        """Generate analysis summary"""
        
        fault_count = len(detected_faults)
        primary_fault = detected_faults[0] if detected_faults else None
        
        if fault_count == 0:
            return f"Your {club_used} swing shows good fundamentals with solid mechanics throughout the motion."
        
        elif fault_count == 1:
            fault_name = primary_fault.get("fault_name", "swing issue")
            return f"Your {club_used} swing shows one primary area for improvement: {fault_name.lower()}. Addressing this will help improve consistency and performance."
        
        elif fault_count <= 3:
            primary_fault_name = primary_fault.get("fault_name", "swing issue")
            return f"Your {club_used} swing analysis reveals {fault_count} areas for improvement, with {primary_fault_name.lower()} being the primary focus. These adjustments will help optimize your swing mechanics."
        
        else:
            return f"Your {club_used} swing shows several areas for improvement. Let's focus on the most impactful changes first to build a solid foundation before addressing the finer details."
    
    async def _generate_fault_feedback(self,
                                     fault: Dict[str, Any],
                                     skill_adaptation: Dict[str, str]) -> Optional[Dict[str, str]]:
        """Generate detailed feedback for a specific fault"""
        
        fault_id = fault.get("fault_id", "")
        fault_type = self._identify_fault_type(fault_id)
        
        if fault_type not in self.fault_response_templates:
            return self._generate_generic_fault_feedback(fault, skill_adaptation)
        
        template = self.fault_response_templates[fault_type]
        
        # Extract KPI information for technical details
        kpi_deviations = fault.get("kpi_deviations", [])
        technical_info = ""
        
        if kpi_deviations and self.config.include_technical_details:
            kpi = kpi_deviations[0]
            observed_value = kpi.get("observed_value", "")
            ideal_range = kpi.get("ideal_value_or_range", "")
            technical_info = template.get("technical", "").format(
                observed_value=observed_value,
                ideal_range=ideal_range
            )
        
        # Adapt complexity based on skill level
        explanation = template["explanation"]
        if skill_adaptation["complexity"] == "low":
            explanation = self._simplify_explanation(explanation)
        elif skill_adaptation["complexity"] == "very high":
            explanation = self._enhance_explanation(explanation, technical_info)
        
        return {
            "explanation": explanation,
            "tip": template["tip"],
            "drill_suggestion": template["drill"],
            "technical_details": technical_info if self.config.include_technical_details else None,
            "priority": "high" if fault.get("severity", 0) > 0.7 else "medium"
        }
    
    def _generate_positive_feedback(self,
                                  club_used: str,
                                  skill_adaptation: Dict[str, str]) -> Dict[str, Any]:
        """Generate positive feedback for good swings"""
        
        positive_messages = [
            f"Excellent work! Your {club_used} swing demonstrates solid fundamentals and good sequence.",
            f"Your {club_used} swing shows strong mechanics with good balance and timing throughout.",
            f"Well done! Your {club_used} technique displays proper body rotation and club control.",
            f"Great swing! Your {club_used} shows consistent mechanics and good athletic positions."
        ]
        
        tips = [
            "Continue to practice this swing pattern to build muscle memory.",
            "Focus on maintaining this tempo and sequence in your practice sessions.",
            "Keep working on consistency with this solid foundation.",
            "This swing pattern will serve you well - stay committed to these fundamentals."
        ]
        
        return {
            "summary_of_findings": random.choice(positive_messages),
            "detailed_feedback": [
                {
                    "explanation": "Your swing fundamentals are solid and show good athletic movement patterns.",
                    "tip": random.choice(tips),
                    "drill_suggestion": "Continue with regular practice to maintain consistency.",
                    "priority": "maintenance"
                }
            ],
            "overall_assessment": "Positive",
            "next_steps": f"Keep practicing with your {club_used} to maintain this level of performance.",
            "confidence_score": random.uniform(0.90, 0.98)
        }
    
    def _identify_fault_type(self, fault_id: str) -> str:
        """Identify fault type from fault ID"""
        fault_mappings = {
            "INSUFFICIENT_SHOULDER_TURN": "insufficient_shoulder_turn",
            "IMPROPER_POSTURE_HIP_HINGE": "excessive_hip_hinge",
            "CUPPED_WRIST": "cupped_wrist",
            "HIP_SWAY": "lateral_sway",
            "REVERSE_SPINE": "reverse_spine_angle",
            "WEIGHT_DISTRIBUTION": "poor_weight_distribution"
        }
        
        for key, value in fault_mappings.items():
            if key in fault_id:
                return value
        
        return "generic"
    
    def _generate_generic_fault_feedback(self,
                                       fault: Dict[str, Any],
                                       skill_adaptation: Dict[str, str]) -> Dict[str, str]:
        """Generate generic feedback for unknown fault types"""
        
        fault_name = fault.get("fault_name", "swing issue")
        
        return {
            "explanation": f"Your swing shows an area for improvement related to {fault_name.lower()}.",
            "tip": "Focus on maintaining good fundamentals and consistent practice.",
            "drill_suggestion": "Work with a golf professional to address this specific area.",
            "priority": "medium"
        }
    
    def _generate_overall_assessment(self,
                                   detected_faults: List[Dict[str, Any]],
                                   skill_adaptation: Dict[str, str]) -> str:
        """Generate overall swing assessment"""
        
        fault_count = len(detected_faults)
        encouragement = skill_adaptation.get("encouragement", "")
        
        if fault_count == 0:
            return f"Excellent swing mechanics. {encouragement}"
        elif fault_count <= 2:
            return f"Good fundamentals with room for specific improvements. {encouragement}"
        elif fault_count <= 4:
            return f"Solid foundation with several areas to work on. {encouragement}"
        else:
            return f"Multiple areas for improvement - let's prioritize the most impactful changes. {encouragement}"
    
    def _generate_next_steps(self,
                           detected_faults: List[Dict[str, Any]],
                           skill_adaptation: Dict[str, str]) -> str:
        """Generate next steps recommendation"""
        
        if not detected_faults:
            return "Continue practicing to maintain consistency and consider working on advanced techniques."
        
        primary_fault = detected_faults[0]
        primary_fault_name = primary_fault.get("fault_name", "primary issue")
        
        if len(detected_faults) == 1:
            return f"Focus your practice sessions on {primary_fault_name.lower()}. Once improved, reassess your swing."
        else:
            return f"Start with {primary_fault_name.lower()} as your primary focus, then progress to the other areas systematically."
    
    def _simplify_explanation(self, explanation: str) -> str:
        """Simplify explanation for beginner skill level"""
        # Remove technical terms and complex concepts
        simplified = explanation.replace("biomechanical", "movement")
        simplified = simplified.replace("kinematic", "motion")
        simplified = simplified.replace("sequence", "timing")
        return simplified
    
    def _enhance_explanation(self, explanation: str, technical_info: str) -> str:
        """Enhance explanation for advanced skill levels"""
        if technical_info:
            return f"{explanation} {technical_info}"
        return explanation
    
    async def _simulate_delay(self, base_delay_ms: Optional[int] = None):
        """Simulate API response delay"""
        if self.config.response_mode == MockResponseMode.FAST:
            delay_ms = 10
        elif self.config.response_mode == MockResponseMode.SLOW:
            delay_ms = 2000
        elif base_delay_ms is not None:
            delay_ms = base_delay_ms
        elif self.config.response_delay_ms is not None:
            delay_ms = self.config.response_delay_ms
        else:
            # Realistic API delay
            delay_ms = random.uniform(150, 500)
        
        await asyncio.sleep(delay_ms / 1000.0)
    
    def _should_simulate_error(self) -> bool:
        """Determine if error should be simulated"""
        if self.config.response_mode == MockResponseMode.ERROR_PRONE:
            return random.random() < 0.1  # 10% error rate
        
        return random.random() < self.config.error_rate
    
    def get_call_statistics(self) -> Dict[str, Any]:
        """Get statistics about mock API usage"""
        return {
            "total_calls": self.call_count,
            "response_history_count": len(self.response_history),
            "average_response_length": sum(r.get("response_length", 0) for r in self.response_history) / max(len(self.response_history), 1),
            "skill_level_distribution": self._get_skill_level_distribution(),
            "fault_analysis_distribution": self._get_fault_distribution()
        }
    
    def _get_skill_level_distribution(self) -> Dict[str, int]:
        """Get distribution of skill levels in responses"""
        distribution = {}
        for response in self.response_history:
            skill_level = response.get("skill_level", "unknown")
            distribution[skill_level] = distribution.get(skill_level, 0) + 1
        return distribution
    
    def _get_fault_distribution(self) -> Dict[str, int]:
        """Get distribution of fault counts in responses"""
        distribution = {}
        for response in self.response_history:
            fault_count = response.get("fault_count", 0)
            key = f"{fault_count}_faults"
            distribution[key] = distribution.get(key, 0) + 1
        return distribution
    
    def reset_statistics(self):
        """Reset call statistics"""
        self.call_count = 0
        self.response_history = []

# Global mock instance for testing
_global_mock_api = None

def get_mock_gemini_api(config: MockGeminiConfig = None) -> MockGeminiAPI:
    """Get global mock Gemini API instance"""
    global _global_mock_api
    if _global_mock_api is None or config is not None:
        _global_mock_api = MockGeminiAPI(config)
    return _global_mock_api

def create_mock_response(swing_context: str, fault_context: str, skill_level: str = "intermediate") -> Dict[str, Any]:
    """Create a quick mock response without full API simulation"""
    
    templates = {
        "good_swing": {
            "summary": f"Your {swing_context} swing demonstrates solid fundamentals with good mechanics throughout the motion.",
            "feedback": [
                {
                    "explanation": "Your swing shows good balance and timing with proper sequencing.",
                    "tip": "Continue practicing to maintain this consistency.",
                    "drill_suggestion": "Focus on repeating this swing pattern in practice."
                }
            ]
        },
        "poor_swing": {
            "summary": f"Your {swing_context} swing shows several areas for improvement that will help optimize performance.",
            "feedback": [
                {
                    "explanation": f"Analysis reveals issues with {fault_context} that affect consistency.",
                    "tip": "Focus on the primary fault first before addressing other areas.",
                    "drill_suggestion": "Work with a golf professional to address these specific issues."
                }
            ]
        }
    }
    
    template_type = "good_swing" if "good" in fault_context else "poor_swing"
    template = templates[template_type]
    
    return {
        "summary_of_findings": template["summary"],
        "detailed_feedback": template["feedback"],
        "confidence_score": random.uniform(0.8, 0.95),
        "processing_time_ms": random.uniform(150, 400)
    }

# Pytest fixtures for mock API
def mock_gemini_api_fixture(quality: MockResponseQuality = MockResponseQuality.GOOD):
    """Pytest fixture factory for mock Gemini API"""
    def _fixture():
        config = MockGeminiConfig(
            response_quality=quality,
            response_mode=MockResponseMode.FAST,  # Fast for testing
            error_rate=0.0  # No errors by default
        )
        return get_mock_gemini_api(config)
    return _fixture

if __name__ == "__main__":
    print("Mock Gemini API for SwingSync AI Testing")
    print("======================================")
    
    # Demonstration of mock API usage
    async def demo():
        # Create mock API
        config = MockGeminiConfig(
            response_quality=MockResponseQuality.GOOD,
            response_mode=MockResponseMode.REALISTIC
        )
        
        mock_api = MockGeminiAPI(config)
        
        # Mock swing input
        swing_input = {
            "session_id": "demo_session",
            "user_id": "demo_user",
            "club_used": "7-Iron"
        }
        
        # Mock detected faults
        detected_faults = [
            {
                "fault_id": "INSUFFICIENT_SHOULDER_TURN_P4",
                "fault_name": "Insufficient Shoulder Turn",
                "severity": 0.7,
                "kpi_deviations": [
                    {
                        "kpi_name": "Shoulder Rotation",
                        "observed_value": "65.0 degrees",
                        "ideal_value_or_range": "85-105 degrees"
                    }
                ]
            }
        ]
        
        # Generate feedback
        print("Generating mock feedback...")
        response = await mock_api.generate_feedback(swing_input, detected_faults, "intermediate")
        
        print(f"Summary: {response['summary_of_findings']}")
        print(f"Detailed feedback items: {len(response['detailed_feedback'])}")
        print(f"Confidence: {response['confidence_score']:.2f}")
        
        # Generate streaming feedback
        print("\nGenerating streaming feedback...")
        frame_analysis = {"detected_faults": detected_faults}
        streaming_response = await mock_api.generate_streaming_feedback(frame_analysis, {})
        
        if streaming_response:
            print(f"Streaming tip: {streaming_response.get('quick_tip', 'No tip')}")
        
        # Show statistics
        stats = mock_api.get_call_statistics()
        print(f"\nAPI Statistics: {stats['total_calls']} calls made")
    
    import asyncio
    asyncio.run(demo())