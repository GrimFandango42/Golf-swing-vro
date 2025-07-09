"""
Adaptive Coaching System Demo

This demo showcases the intelligent adaptive coaching system that learns from
each user's unique patterns, preferences, and improvement trajectory to provide
increasingly personalized guidance.

The demo simulates realistic user interactions and demonstrates:
- Real-time learning from user behavior
- Pattern recognition and adaptation
- Progress prediction and goal setting
- Memory and context management
- Achievement recognition and celebration
- Coaching effectiveness optimization

Run this demo to see how the system becomes smarter over time!
"""

import asyncio
import json
import random
from datetime import datetime, timedelta
from typing import Dict, List, Any

# Import the adaptive coaching system
try:
    from adaptive_coaching.adaptive_integration import (
        AdaptiveCoachingOrchestrator, AdaptiveCoachingConfig, create_adaptive_coaching_system
    )
    from adaptive_coaching.user_learning_engine import LearningStyle, MotivationType
    from adaptive_coaching.pattern_recognition import PatternType
except ImportError as e:
    print(f"Error importing adaptive coaching: {e}")
    print("Make sure you're running from the project root directory")
    exit(1)

class AdaptiveCoachingDemo:
    """Demo class for showcasing adaptive coaching capabilities"""
    
    def __init__(self):
        self.adaptive_coach = create_adaptive_coaching_system()
        self.demo_users = self._create_demo_users()
        
    def _create_demo_users(self) -> Dict[str, Dict[str, Any]]:
        """Create diverse demo users with different characteristics"""
        
        return {
            "analytical_user": {
                "name": "Alex Chen",
                "characteristics": "Loves data and detailed analysis",
                "learning_style": LearningStyle.ANALYTICAL,
                "motivation": MotivationType.MASTERY,
                "personality_traits": ["perfectionist", "patient", "curious"],
                "typical_messages": [
                    "Can you show me the exact numbers from my swing analysis?",
                    "I want to understand the biomechanics behind this movement",
                    "What's my improvement rate compared to last week?",
                    "Break down the technical details for me"
                ]
            },
            
            "encouraging_user": {
                "name": "Sarah Johnson", 
                "characteristics": "Needs positive reinforcement and support",
                "learning_style": LearningStyle.VISUAL,
                "motivation": MotivationType.PROGRESS,
                "personality_traits": ["cautious", "encouraging", "social"],
                "typical_messages": [
                    "Am I getting better? I can't really tell",
                    "This feels really difficult today",
                    "I hope I'm doing this right",
                    "Can you show me what good form looks like?"
                ]
            },
            
            "competitive_user": {
                "name": "Mike Rodriguez",
                "characteristics": "Driven by competition and achievements", 
                "learning_style": LearningStyle.KINESTHETIC,
                "motivation": MotivationType.COMPETITION,
                "personality_traits": ["confident", "impatient", "goal-oriented"],
                "typical_messages": [
                    "How do I compare to other golfers at my level?",
                    "I want to beat my personal best score",
                    "Give me the most challenging drill you have",
                    "When will I be ready for the next level?"
                ]
            },
            
            "patient_user": {
                "name": "Eleanor Williams",
                "characteristics": "Methodical learner who prefers steady progress",
                "learning_style": LearningStyle.SEQUENTIAL,
                "motivation": MotivationType.INTRINSIC,
                "personality_traits": ["patient", "methodical", "reflective"],
                "typical_messages": [
                    "Let's take this step by step",
                    "I need more time to practice this",
                    "Can we slow down and focus on one thing?",
                    "I want to really understand this before moving on"
                ]
            }
        }
    
    async def run_demo(self):
        """Run the comprehensive demo"""
        
        print("🏌️ ADAPTIVE GOLF COACHING SYSTEM DEMO")
        print("=" * 50)
        print()
        
        # Show system overview
        await self._show_system_overview()
        
        # Demonstrate user onboarding
        await self._demo_user_onboarding()
        
        # Simulate learning progression
        await self._demo_learning_progression()
        
        # Show adaptation in action
        await self._demo_adaptive_responses()
        
        # Demonstrate achievement system
        await self._demo_achievement_system()
        
        # Show analytics and insights
        await self._demo_analytics_insights()
        
        print("\n🎉 DEMO COMPLETE!")
        print("The adaptive coaching system has demonstrated its ability to:")
        print("✅ Learn individual user patterns and preferences")
        print("✅ Adapt coaching style based on effectiveness")
        print("✅ Predict progress and recommend goals")
        print("✅ Remember context across sessions")
        print("✅ Celebrate achievements and maintain motivation")
        print("✅ Continuously improve through feedback loops")
    
    async def _show_system_overview(self):
        """Show system capabilities overview"""
        
        print("📊 SYSTEM OVERVIEW")
        print("-" * 30)
        
        status = self.adaptive_coach.get_system_status()
        
        print(f"🧠 Adaptive Learning: {'✅ Active' if status['system_health']['adaptive_learning_active'] else '❌ Inactive'}")
        print(f"🔍 Pattern Recognition: {'✅ Active' if status['system_health']['pattern_recognition_active'] else '❌ Inactive'}")
        print(f"📈 Effectiveness Tracking: {'✅ Active' if status['system_health']['effectiveness_tracking_active'] else '❌ Inactive'}")
        print(f"🎉 Achievement System: {'✅ Active' if status['system_health']['celebration_system_active'] else '❌ Inactive'}")
        
        print(f"\n📈 Performance Metrics:")
        metrics = status['performance_metrics']
        print(f"   Total Interactions: {metrics['total_interactions']}")
        print(f"   Adaptation Success Rate: {metrics['adaptation_success_rate']:.1%}")
        print(f"   System Availability: {metrics['system_availability']:.1%}")
        
        await self._pause()
    
    async def _demo_user_onboarding(self):
        """Demonstrate user onboarding process"""
        
        print("\n👋 USER ONBOARDING DEMO")
        print("-" * 30)
        
        # Simulate new user first session
        user = self.demo_users["analytical_user"]
        user_id = "demo_analytical"
        session_id = "session_001"
        
        print(f"New user: {user['name']} ({user['characteristics']})")
        
        # First interaction
        first_message = "Hi, I'm new to golf coaching apps. Can you help me improve my swing?"
        
        print(f"\nUser: {first_message}")
        
        # Simulate swing analysis data
        swing_data = {
            "overall_score": 45.2,
            "fault_count": 3,
            "consistency_score": 0.6,
            "improvement_rate": 0.0,
            "session_duration": 15.5
        }
        
        response = await self.adaptive_coach.process_coaching_request(
            user_id=user_id,
            session_id=session_id,
            user_message=first_message,
            swing_analysis=swing_data,
            context={"emotional_state": "neutral", "engagement_level": 0.7}
        )
        
        print(f"Coach: {response['response']}")
        print(f"System Confidence: {response['system_confidence']:.1%}")
        
        await self._pause()
    
    async def _demo_learning_progression(self):
        """Demonstrate how system learns over multiple sessions"""
        
        print("\n📚 LEARNING PROGRESSION DEMO")
        print("-" * 30)
        
        user_id = "demo_analytical"
        
        # Simulate 5 sessions showing progression
        sessions = [
            {
                "session_id": "session_002",
                "message": "I noticed my swing analysis shows a hip rotation issue. Can you explain the biomechanics?",
                "swing_data": {"overall_score": 48.1, "fault_count": 3, "consistency_score": 0.65},
                "context": {"emotional_state": "curious", "engagement_level": 0.9}
            },
            {
                "session_id": "session_003", 
                "message": "The technical details really help me understand. What's my exact improvement rate?",
                "swing_data": {"overall_score": 52.3, "fault_count": 2, "consistency_score": 0.72},
                "context": {"emotional_state": "confident", "engagement_level": 0.95}
            },
            {
                "session_id": "session_004",
                "message": "Can you break down the kinematic sequence data for my last 5 swings?",
                "swing_data": {"overall_score": 55.7, "fault_count": 2, "consistency_score": 0.78},
                "context": {"emotional_state": "focused", "engagement_level": 0.85}
            },
            {
                "session_id": "session_005",
                "message": "I want to see a statistical analysis of my progress over these sessions",
                "swing_data": {"overall_score": 58.9, "fault_count": 1, "consistency_score": 0.83},
                "context": {"emotional_state": "analytical", "engagement_level": 0.9}
            }
        ]
        
        print("Simulating learning progression over 4 sessions...")
        
        for i, session in enumerate(sessions, 2):
            print(f"\n--- Session {i} ---")
            print(f"User: {session['message']}")
            
            response = await self.adaptive_coach.process_coaching_request(
                user_id=user_id,
                session_id=session["session_id"],
                user_message=session["message"],
                swing_analysis=session["swing_data"],
                context=session["context"]
            )
            
            print(f"Coach: {response['response'][:100]}...")
            
            # Show system adaptations
            insights = response.get('coaching_insights', {})
            if insights.get('adaptations_applied'):
                print(f"🔧 Adaptations Applied: {', '.join(insights['adaptations_applied'])}")
            
            print(f"📊 Confidence: {response['system_confidence']:.1%}")
            
            await self._pause(0.5)
        
        # Show learned patterns
        user_insights = self.adaptive_coach.get_user_insights(user_id)
        learning_profile = user_insights.get('learning_profile', {})
        
        print(f"\n🧠 LEARNED USER PROFILE:")
        print(f"   Learning Style: {learning_profile.get('profile_summary', {}).get('learning_style', 'Unknown')}")
        print(f"   Motivation Type: {learning_profile.get('profile_summary', {}).get('motivation_type', 'Unknown')}")
        print(f"   Total Interactions: {learning_profile.get('profile_summary', {}).get('total_interactions', 0)}")
        
        await self._pause()
    
    async def _demo_adaptive_responses(self):
        """Demonstrate how responses adapt to different user types"""
        
        print("\n🎯 ADAPTIVE RESPONSE DEMO")
        print("-" * 30)
        print("Showing how the same question gets different responses based on user profile...")
        
        question = "My swing consistency seems inconsistent. What should I do?"
        swing_data = {"overall_score": 55, "fault_count": 2, "consistency_score": 0.65}
        
        for user_type, user_info in self.demo_users.items():
            print(f"\n--- {user_info['name']} ({user_type}) ---")
            print(f"Characteristics: {user_info['characteristics']}")
            print(f"Question: {question}")
            
            response = await self.adaptive_coach.process_coaching_request(
                user_id=f"demo_{user_type}",
                session_id="adaptation_demo",
                user_message=question,
                swing_analysis=swing_data,
                context={"emotional_state": "neutral", "engagement_level": 0.7}
            )
            
            print(f"Adaptive Response: {response['response']}")
            
            insights = response.get('coaching_insights', {})
            print(f"Approach Used: {insights.get('approach_used', 'default')}")
            
            await self._pause(1)
    
    async def _demo_achievement_system(self):
        """Demonstrate achievement recognition and celebration"""
        
        print("\n🏆 ACHIEVEMENT SYSTEM DEMO")
        print("-" * 30)
        
        user_id = "demo_competitive"
        
        # Simulate breakthrough performance
        breakthrough_data = {
            "overall_score": 72.5,  # Significant improvement
            "fault_count": 0,
            "consistency_score": 0.9,
            "improvement_rate": 0.15,
            "session_duration": 25
        }
        
        print("Simulating a breakthrough performance session...")
        print(f"Previous average score: ~58")
        print(f"Today's score: {breakthrough_data['overall_score']}")
        
        response = await self.adaptive_coach.process_coaching_request(
            user_id=user_id,
            session_id="breakthrough_session",
            user_message="How did I do today? I felt like something clicked!",
            swing_analysis=breakthrough_data,
            context={"emotional_state": "excited", "engagement_level": 0.95}
        )
        
        print(f"\nCoach Response: {response['response']}")
        
        # Show achievements
        achievements = response.get('achievements', [])
        if achievements:
            print(f"\n🎉 ACHIEVEMENTS UNLOCKED:")
            for achievement in achievements:
                print(f"   🏆 {achievement['title']}")
                print(f"      {achievement['description']}")
                print(f"      Magnitude: {achievement['magnitude']}")
                print(f"      Message: {achievement['message']}")
                print()
        
        # Show progress update
        progress = response.get('progress_update', {})
        if progress.get('next_milestones'):
            print(f"📈 NEXT MILESTONES:")
            for metric, milestone in progress['next_milestones'].items():
                print(f"   • {milestone['metric_name']}: {milestone['progress_percentage']:.1f}% to {milestone['next_milestone']}")
        
        await self._pause()
    
    async def _demo_analytics_insights(self):
        """Demonstrate analytics and insights capabilities"""
        
        print("\n📊 ANALYTICS & INSIGHTS DEMO")
        print("-" * 30)
        
        # Show insights for each demo user
        for user_type in ["analytical", "competitive"]:
            user_id = f"demo_{user_type}"
            user_info = self.demo_users[f"{user_type}_user"]
            
            print(f"\n--- {user_info['name']} Analytics ---")
            
            insights = self.adaptive_coach.get_user_insights(user_id)
            
            # Learning profile insights
            learning = insights.get('learning_profile', {})
            if learning:
                profile = learning.get('profile_summary', {})
                patterns = learning.get('learning_patterns', {})
                
                print(f"Learning Style: {profile.get('learning_style', 'Unknown')}")
                print(f"Motivation: {profile.get('motivation_type', 'Unknown')}")
                print(f"Total Sessions: {profile.get('total_sessions', 0)}")
                
                if patterns.get('common_faults'):
                    print(f"Common Issues: {', '.join(patterns['common_faults'][:3])}")
                
                if patterns.get('strength_areas'):
                    print(f"Strengths: {', '.join(patterns['strength_areas'][:3])}")
            
            # Effectiveness insights
            effectiveness = insights.get('effectiveness', {})
            if effectiveness.get('effectiveness_analysis'):
                analysis = effectiveness['effectiveness_analysis']
                overall = analysis.get('overall_metrics', {})
                
                print(f"Coaching Effectiveness: {overall.get('average_effectiveness', 0):.1%}")
                print(f"User Satisfaction: {overall.get('satisfaction_score', 0):.1%}")
                print(f"Engagement Level: {overall.get('engagement_score', 0):.1%}")
            
            # Recent decisions
            decisions = insights.get('recent_decisions', [])
            if decisions:
                print(f"Recent Coaching Approaches:")
                for decision in decisions[-3:]:
                    print(f"   • {decision['approach']} (confidence: {decision['confidence']:.1%})")
        
        await self._pause()
    
    async def _pause(self, seconds: float = 2.0):
        """Pause for dramatic effect"""
        await asyncio.sleep(seconds)
    
    async def run_interactive_demo(self):
        """Run interactive demo where user can ask questions"""
        
        print("\n🎮 INTERACTIVE DEMO MODE")
        print("-" * 30)
        print("You can now interact with the adaptive coaching system!")
        print("Type 'quit' to exit the demo.")
        print()
        
        user_id = "demo_interactive"
        session_id = "interactive_session"
        
        while True:
            try:
                user_input = input("\nYou: ").strip()
                
                if user_input.lower() in ['quit', 'exit', 'q']:
                    print("Thanks for trying the adaptive coaching demo!")
                    break
                
                if not user_input:
                    continue
                
                # Simulate some swing data
                swing_data = {
                    "overall_score": random.uniform(45, 75),
                    "fault_count": random.randint(0, 3),
                    "consistency_score": random.uniform(0.5, 0.9),
                    "improvement_rate": random.uniform(-0.1, 0.2)
                }
                
                response = await self.adaptive_coach.process_coaching_request(
                    user_id=user_id,
                    session_id=session_id,
                    user_message=user_input,
                    swing_analysis=swing_data,
                    context={"emotional_state": "neutral", "engagement_level": 0.7}
                )
                
                print(f"\nCoach: {response['response']}")
                
                # Show achievements if any
                if response.get('achievements'):
                    print(f"\n🎉 Achievement unlocked: {response['achievements'][0]['title']}")
                
                # Show recommendations
                recommendations = response.get('next_recommendations', [])
                if recommendations:
                    print(f"\n💡 Recommendations: {recommendations[0]}")
                
            except KeyboardInterrupt:
                print("\n\nDemo interrupted. Goodbye!")
                break
            except Exception as e:
                print(f"\nError: {e}")
                continue

async def main():
    """Main demo function"""
    
    print("Welcome to the Adaptive Golf Coaching System Demo!")
    print()
    print("Choose demo mode:")
    print("1. Full Demo (automated)")
    print("2. Interactive Demo")
    print("3. Both")
    
    choice = input("\nEnter choice (1-3): ").strip()
    
    demo = AdaptiveCoachingDemo()
    
    if choice in ['1', '3']:
        await demo.run_demo()
    
    if choice in ['2', '3']:
        await demo.run_interactive_demo()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nDemo terminated by user.")
    except Exception as e:
        print(f"Demo error: {e}")
        print("Make sure all dependencies are installed and you're running from the correct directory.")