#!/usr/bin/env python3
"""
Conversational Coaching System Demo

This script demonstrates the conversational coaching capabilities of the SwingSync AI
platform, showing how voice-driven coaching integrates with swing analysis.

Features demonstrated:
- Voice-to-text conversation processing
- Context-aware coaching responses
- Integration with swing analysis results
- Different coaching personalities
- Real-time conversation flow
"""

import asyncio
import json
import time
from typing import Dict, Any, Optional

# Import existing SwingSync components
try:
    from data_structures import SwingVideoAnalysisInput, SwingAnalysisFeedback
    from kpi_extraction import extract_all_kpis
    from fault_detection import check_swing_faults
    from feedback_generation import generate_swing_analysis_feedback, FeedbackContext, UserSkillLevel
    SWINGSYNC_AVAILABLE = True
except ImportError:
    print("Note: Running demo with mock SwingSync components")
    SWINGSYNC_AVAILABLE = False

# Import conversational coaching components
try:
    from conversational_coaching.conversation_engine.coaching_agent import CoachingAgent
    from conversational_coaching.voice_interface.speech_interface import VoiceInterface, VoiceSettings
    from conversational_coaching.config.coaching_profiles import COACHING_PERSONALITIES, PersonalitySelector
    COACHING_AVAILABLE = True
except ImportError:
    print("Warning: Conversational coaching modules not available")
    COACHING_AVAILABLE = False

class ConversationalCoachingDemo:
    """Demonstration of conversational coaching capabilities"""
    
    def __init__(self):
        self.coaching_agent = None
        self.voice_interface = None
        self.current_personality = "encouraging_mentor"
        self.conversation_history = []
        
        if COACHING_AVAILABLE:
            self._initialize_coaching_system()
        
        print("🏌️ SwingSync AI Conversational Coaching Demo")
        print("=" * 50)
    
    def _initialize_coaching_system(self):
        """Initialize the conversational coaching system"""
        try:
            # Initialize voice interface (would use real providers in production)
            self.voice_interface = VoiceInterface()
            
            # Initialize coaching agent
            self.coaching_agent = CoachingAgent(
                voice_interface=self.voice_interface
            )
            
            print("✅ Conversational coaching system initialized")
        
        except Exception as e:
            print(f"⚠️ Error initializing coaching system: {e}")
    
    async def demo_personality_selection(self):
        """Demonstrate different coaching personalities"""
        print("\n🎭 COACHING PERSONALITY DEMO")
        print("-" * 30)
        
        # Show available personalities
        print("Available coaching personalities:")
        for key, personality in COACHING_PERSONALITIES.items():
            print(f"  • {personality.display_name}: {personality.characteristics[0]}")
        
        # Demonstrate response differences for the same situation
        test_message = "I'm struggling with my hip rotation in the backswing"
        swing_analysis = {
            "summary_of_findings": "Hip rotation limited to 35 degrees, optimal range is 45-60 degrees",
            "raw_detected_faults": [
                {
                    "fault_name": "insufficient_hip_rotation",
                    "severity": 7,
                    "description": "Limited hip turn restricting power generation"
                }
            ]
        }
        
        print(f"\nTest scenario: '{test_message}'")
        print("Swing analysis: Limited hip rotation detected")
        print("\nPersonality responses:")
        
        for personality_key in ["encouraging_mentor", "technical_expert", "motivational_coach"]:
            if not COACHING_AVAILABLE:
                # Mock responses for demo
                responses = {
                    "encouraging_mentor": "That's a great observation! Hip rotation can feel tricky at first, but you're absolutely on the right track by noticing it. Let's work together to improve that turn - I have some gentle drills that will help you feel the proper motion.",
                    "technical_expert": "Analysis confirms suboptimal hip rotation at 35 degrees versus the ideal 45-60 degree range. This restriction reduces kinetic energy transfer by approximately 15%. I recommend implementing targeted mobility exercises and rotation drills.",
                    "motivational_coach": "YES! That's exactly the kind of awareness champions have! You've identified your power leak, and now we're going to UNLEASH that hip rotation! This is your breakthrough moment - let's attack this weakness and turn it into your strength!"
                }
                response = responses[personality_key]
            else:
                # Use actual coaching agent with different personalities
                self.current_personality = personality_key
                response = await self._mock_personality_response(test_message, swing_analysis, personality_key)
            
            personality = COACHING_PERSONALITIES[personality_key]
            print(f"\n  🗣️ {personality.display_name}:")
            print(f"     {response}")
    
    async def demo_conversation_flow(self):
        """Demonstrate natural conversation flow"""
        print("\n💬 CONVERSATION FLOW DEMO")
        print("-" * 30)
        
        if not COACHING_AVAILABLE:
            print("Using mock conversation responses...")
            await self._mock_conversation_flow()
            return
        
        # Simulate a coaching conversation
        conversation_scenarios = [
            {
                "user_message": "Hi, I'm ready to practice my golf swing",
                "context": "session_start"
            },
            {
                "user_message": "How did that swing look?",
                "context": "after_swing",
                "swing_analysis": {
                    "summary_of_findings": "Good tempo and balance, slight issue with wrist position",
                    "raw_detected_faults": [
                        {
                            "fault_name": "cupped_wrist_p4",
                            "severity": 5,
                            "description": "Lead wrist cupped at top of backswing"
                        }
                    ]
                }
            },
            {
                "user_message": "I don't understand what you mean by cupped wrist",
                "context": "clarification_request"
            },
            {
                "user_message": "Can you give me a drill to practice?",
                "context": "drill_request"
            },
            {
                "user_message": "That's feeling better! How's this one?",
                "context": "after_practice",
                "swing_analysis": {
                    "summary_of_findings": "Significant improvement in wrist position, excellent progress",
                    "raw_detected_faults": []
                }
            }
        ]
        
        print("Simulating coaching conversation:")
        
        for i, scenario in enumerate(conversation_scenarios, 1):
            print(f"\n  Step {i}: {scenario['context']}")
            print(f"  👤 User: {scenario['user_message']}")
            
            # Get coaching response
            response = await self._generate_coaching_response(
                scenario['user_message'],
                scenario.get('swing_analysis'),
                scenario['context']
            )
            
            print(f"  🤖 Coach: {response}")
            
            # Add to conversation history
            self.conversation_history.append({
                "user": scenario['user_message'],
                "coach": response,
                "context": scenario['context']
            })
            
            # Small delay for readability
            await asyncio.sleep(1)
    
    async def demo_voice_commands(self):
        """Demonstrate voice command processing"""
        print("\n🎤 VOICE COMMAND DEMO")
        print("-" * 25)
        
        voice_commands = [
            "Start practice session",
            "Analyze my swing", 
            "Give me some tips",
            "Help me with my setup",
            "What drills should I do?",
            "Slow down please",
            "End practice session"
        ]
        
        print("Processing voice commands:")
        
        for command in voice_commands:
            print(f"\n  🎤 Voice input: '{command}'")
            
            if COACHING_AVAILABLE and self.voice_interface:
                # Process with actual voice command processor
                command_result = self.voice_interface.command_processor.process_command(command)
                print(f"  📋 Detected intent: {command_result['command']}")
                print(f"  📊 Confidence: {command_result['confidence']:.2f}")
                
                # Generate response based on command
                if command_result['command'] != 'conversation':
                    response = await self._handle_voice_command(command_result)
                else:
                    response = await self._generate_coaching_response(command, None, "voice_command")
                
                print(f"  🤖 Response: {response}")
            else:
                # Mock voice command processing
                mock_responses = {
                    "Start practice session": "Great! Let's begin your practice. I'll provide feedback as we go.",
                    "Analyze my swing": "I'd be happy to analyze your swing. Go ahead and take a shot!",
                    "Give me some tips": "Focus on your setup position and tempo. Small improvements make big differences!",
                    "Help me with my setup": "Let's work on your stance and grip. These fundamentals are crucial for consistency.",
                    "What drills should I do?": "Try the slow-motion swing drill to build muscle memory for proper positions.",
                    "Slow down please": "Of course! I'll speak more slowly and break things down step by step.",
                    "End practice session": "Excellent work today! You're making real progress. See you next time!"
                }
                
                response = mock_responses.get(command, "I understand you want to work on your golf game!")
                print(f"  🤖 Mock response: {response}")
    
    async def demo_multimodal_integration(self):
        """Demonstrate integration with swing analysis"""
        print("\n🔗 MULTIMODAL INTEGRATION DEMO")
        print("-" * 35)
        
        # Create sample swing data
        sample_swing_data = self._create_sample_swing_data()
        
        print("Processing swing with conversational feedback:")
        print(f"  📊 Swing data: {len(sample_swing_data['frames'])} frames")
        print(f"  ⛳ Club: {sample_swing_data['club_used']}")
        
        if SWINGSYNC_AVAILABLE:
            # Process with actual SwingSync pipeline
            print("  🔄 Running KPI extraction...")
            kpis = extract_all_kpis(sample_swing_data)
            
            print("  🔍 Running fault detection...")
            faults = check_swing_faults(sample_swing_data, kpis)
            
            print("  🧠 Generating AI feedback...")
            feedback = generate_swing_analysis_feedback(sample_swing_data, faults)
            
            print(f"  📋 Analysis complete: {len(faults)} faults detected")
            
            # Generate conversational response
            if COACHING_AVAILABLE:
                conversational_response = await self._generate_coaching_response(
                    "How did I do with that swing?",
                    feedback,
                    "swing_analysis"
                )
                
                print(f"  🗣️ Conversational feedback: {conversational_response}")
            else:
                print("  🗣️ Mock conversational feedback: That swing showed good tempo! Let's work on your hip rotation for even better results.")
        
        else:
            # Mock analysis results
            print("  🔄 Mock analysis: Detected hip rotation issue")
            print("  🗣️ Mock conversational feedback: I can see you're working hard on that swing! Let's focus on getting those hips turning a bit more freely.")
    
    async def demo_real_time_coaching(self):
        """Demonstrate real-time coaching scenario"""
        print("\n⚡ REAL-TIME COACHING DEMO")
        print("-" * 30)
        
        print("Simulating real-time practice session:")
        
        real_time_scenarios = [
            {
                "swing_number": 1,
                "quality": "good",
                "feedback": "Nice tempo on that swing! I can see you're focusing on your fundamentals."
            },
            {
                "swing_number": 2,
                "quality": "needs_work",
                "feedback": "Let's work on keeping that head steady. Try feeling like your head is connected to a string from the ceiling."
            },
            {
                "swing_number": 3,
                "quality": "improved",
                "feedback": "Much better! That head position was perfect. How did that feel to you?"
            },
            {
                "swing_number": 4,
                "quality": "excellent",
                "feedback": "Outstanding! That's the swing we're looking for. You're really getting the hang of this!"
            }
        ]
        
        for scenario in real_time_scenarios:
            print(f"\n  🏌️ Swing #{scenario['swing_number']}")
            print(f"  📊 Quality: {scenario['quality']}")
            print(f"  🗣️ Real-time feedback: {scenario['feedback']}")
            
            # Simulate processing delay
            await asyncio.sleep(0.5)
        
        print("\n  📈 Session summary: 4 swings analyzed, clear improvement trend detected!")
    
    def demo_cost_analysis(self):
        """Demonstrate cost analysis for conversational coaching"""
        print("\n💰 COST ANALYSIS DEMO")
        print("-" * 22)
        
        # Cost estimates per hour of conversation
        cost_scenarios = {
            "Basic Package": {
                "STT": "Google Speech-to-Text ($0.96/hour)",
                "LLM": "Gemini 2.5 Flash ($0.12/hour)", 
                "TTS": "Google TTS ($0.14/hour)",
                "Total": "$1.22/hour"
            },
            "Premium Package": {
                "STT": "Google Speech-to-Text ($0.96/hour)",
                "LLM": "GPT-4 Streaming ($0.36/hour)",
                "TTS": "OpenAI TTS ($0.54/hour)",
                "Total": "$1.86/hour"
            },
            "Ultra Premium": {
                "STT": "Google Speech-to-Text ($0.96/hour)",
                "LLM": "GPT-4 Streaming ($0.36/hour)",
                "TTS": "ElevenLabs ($6.48/hour)",
                "Total": "$7.80/hour"
            }
        }
        
        print("Estimated costs for conversational coaching:")
        
        for package, costs in cost_scenarios.items():
            print(f"\n  📦 {package}:")
            for component, cost in costs.items():
                if component == "Total":
                    print(f"     ➡️ {component}: {cost}")
                else:
                    print(f"     • {component}: {cost}")
        
        print("\n  💡 Cost optimization strategies:")
        print("     • Cache common responses")
        print("     • Use cheaper models for simple interactions")
        print("     • Batch process when possible")
        print("     • Implement usage-based pricing tiers")
    
    async def _generate_coaching_response(self, message: str, swing_analysis: Optional[Dict], context: str) -> str:
        """Generate coaching response (mock or real)"""
        if COACHING_AVAILABLE and self.coaching_agent:
            try:
                response = await self.coaching_agent.process_message(
                    user_id="demo_user",
                    session_id="demo_session",
                    message=message,
                    swing_analysis=swing_analysis
                )
                return response
            except Exception as e:
                print(f"Error generating response: {e}")
                return self._get_mock_response(message, context)
        else:
            return self._get_mock_response(message, context)
    
    def _get_mock_response(self, message: str, context: str) -> str:
        """Get mock coaching response"""
        mock_responses = {
            "session_start": "Great to see you! I'm excited to help you improve your swing today. Let's start with some practice swings.",
            "after_swing": "That's looking good! I can see some nice fundamentals there. Let's work on fine-tuning your technique.",
            "clarification_request": "Great question! A cupped wrist means your lead wrist is bent backward at the top of your swing. Think of it like you're holding a tray - we want it flat instead.",
            "drill_request": "Here's a perfect drill for you: Practice slow-motion swings focusing on keeping your lead wrist flat. Start at 25% speed and gradually increase.",
            "after_practice": "Fantastic improvement! I can really see the difference in your technique. That's exactly what we're looking for!",
            "voice_command": "I understand you want to work on your golf game. What specific area would you like to focus on today?"
        }
        
        return mock_responses.get(context, "I'm here to help you improve your golf swing! What would you like to work on?")
    
    async def _mock_personality_response(self, message: str, swing_analysis: Dict, personality_key: str) -> str:
        """Mock response for different personalities"""
        # This would use the actual coaching agent with different personalities
        # For demo purposes, return the pre-written responses
        responses = {
            "encouraging_mentor": "That's a great observation! Hip rotation can feel tricky at first, but you're absolutely on the right track by noticing it. Let's work together to improve that turn - I have some gentle drills that will help you feel the proper motion.",
            "technical_expert": "Analysis confirms suboptimal hip rotation at 35 degrees versus the ideal 45-60 degree range. This restriction reduces kinetic energy transfer by approximately 15%. I recommend implementing targeted mobility exercises and rotation drills.",
            "motivational_coach": "YES! That's exactly the kind of awareness champions have! You've identified your power leak, and now we're going to UNLEASH that hip rotation! This is your breakthrough moment - let's attack this weakness and turn it into your strength!"
        }
        
        return responses.get(personality_key, "I'm here to help you improve!")
    
    async def _mock_conversation_flow(self):
        """Mock conversation flow for when coaching modules aren't available"""
        mock_conversation = [
            {
                "user": "Hi, I'm ready to practice my golf swing",
                "coach": "Welcome! I'm excited to help you improve today. Let's start with some practice swings and I'll give you feedback.",
                "context": "session_start"
            },
            {
                "user": "How did that swing look?",
                "coach": "That was a solid swing! I noticed your tempo was good, but let's work on your wrist position at the top of your backswing.",
                "context": "after_swing"
            },
            {
                "user": "I don't understand what you mean by cupped wrist",
                "coach": "Great question! A cupped wrist means your lead wrist bends backward. Think of holding a tray - we want your wrist flat like the tray, not cupped upward.",
                "context": "clarification_request"
            },
            {
                "user": "Can you give me a drill to practice?",
                "coach": "Absolutely! Try the 'flat wrist drill': Hold a small towel under your lead armpit and practice slow swings, keeping the towel in place and your wrist flat.",
                "context": "drill_request"
            },
            {
                "user": "That's feeling better! How's this one?",
                "coach": "Excellent improvement! That wrist position looked perfect. I can see you're really getting the feel for it. Keep up the great work!",
                "context": "after_practice"
            }
        ]
        
        for i, exchange in enumerate(mock_conversation, 1):
            print(f"\n  Step {i}: {exchange['context']}")
            print(f"  👤 User: {exchange['user']}")
            print(f"  🤖 Coach: {exchange['coach']}")
            await asyncio.sleep(1)
    
    async def _handle_voice_command(self, command_result: Dict) -> str:
        """Handle voice command (mock or real)"""
        # This would use the actual coaching agent's voice command handler
        command_responses = {
            "start_practice": "Perfect! Let's start your practice session. I'll provide feedback as you go.",
            "end_practice": "Great job today! You're making real progress. See you next time!",
            "analyze_swing": "I'd be happy to analyze your swing! Go ahead and take a shot.",
            "get_tips": "Focus on your fundamentals: setup, tempo, and balance. Small improvements make big differences!",
            "help": "I can help with swing analysis, drills, tips, and answering questions about golf technique."
        }
        
        return command_responses.get(command_result['command'], "I understand you want to work on your golf game!")
    
    def _create_sample_swing_data(self) -> Dict[str, Any]:
        """Create sample swing data for demo"""
        # Create minimal swing data structure
        def make_keypoint(x, y, z):
            return {"x": x, "y": y, "z": z, "visibility": 1.0}
        
        # Create a few frames of pose data
        frames = []
        for i in range(10):
            frame = {
                "left_shoulder": make_keypoint(-0.2, 1.4, -0.3),
                "right_shoulder": make_keypoint(0.2, 1.4, -0.3),
                "left_hip": make_keypoint(-0.15, 0.9, 0),
                "right_hip": make_keypoint(0.15, 0.9, 0),
                "left_knee": make_keypoint(-0.18, 0.4, 0.05),
                "right_knee": make_keypoint(0.18, 0.45, 0)
            }
            frames.append(frame)
        
        return {
            "session_id": "demo_session_001",
            "user_id": "demo_user",
            "club_used": "Driver",
            "frames": frames,
            "p_system_classification": [
                {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 9}
            ],
            "video_fps": 30.0
        }
    
    async def run_full_demo(self):
        """Run the complete demonstration"""
        print("Starting SwingSync AI Conversational Coaching Demo...\n")
        
        await self.demo_personality_selection()
        await self.demo_conversation_flow()
        await self.demo_voice_commands()
        await self.demo_multimodal_integration()
        await self.demo_real_time_coaching()
        self.demo_cost_analysis()
        
        print("\n" + "=" * 50)
        print("🎯 DEMO COMPLETE")
        print("\nKey capabilities demonstrated:")
        print("✅ Multiple coaching personalities")
        print("✅ Natural conversation flow")
        print("✅ Voice command processing")
        print("✅ Integration with swing analysis")
        print("✅ Real-time coaching feedback")
        print("✅ Cost-effective pricing models")
        
        print("\nNext steps for implementation:")
        print("🔧 Set up speech provider APIs")
        print("🔧 Train personality-specific models")
        print("🔧 Implement real-time streaming")
        print("🔧 Create user preference profiles")
        print("🔧 Deploy beta testing environment")

async def main():
    """Main demonstration function"""
    demo = ConversationalCoachingDemo()
    await demo.run_full_demo()

if __name__ == "__main__":
    print("🏌️‍♂️ SwingSync AI - Conversational Coaching Demo")
    print("=" * 50)
    
    # Run the demonstration
    asyncio.run(main())