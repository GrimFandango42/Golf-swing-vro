"""
Celebration and Progress Recognition System - Intelligent Achievement Recognition

This module implements a sophisticated system for recognizing user achievements,
celebrating progress, and maintaining motivation through intelligent milestone
detection and personalized recognition. It ensures users feel valued and
motivated by acknowledging their improvement journey.

Key Features:
- Intelligent milestone detection
- Personalized celebration styles
- Achievement classification and ranking
- Progress momentum tracking
- Motivation maintenance strategies
- Breakthrough moment recognition
- Personal best celebrations
- Goal achievement ceremonies
- Social sharing opportunities
- Long-term journey recognition

The system makes users feel proud of their progress and maintains motivation
through thoughtful, personalized recognition of their golf improvement journey.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from enum import Enum
import json
import logging
import random
from collections import defaultdict, deque

logger = logging.getLogger(__name__)

class AchievementType(Enum):
    """Types of achievements to recognize"""
    MILESTONE = "milestone"
    PERSONAL_BEST = "personal_best"
    BREAKTHROUGH = "breakthrough"
    CONSISTENCY = "consistency"
    IMPROVEMENT_STREAK = "improvement_streak"
    GOAL_ACHIEVEMENT = "goal_achievement"
    TECHNICAL_MASTERY = "technical_mastery"
    PERSISTENCE = "persistence"
    FIRST_TIME = "first_time"
    COMEBACK = "comeback"

class CelebrationStyle(Enum):
    """Different celebration styles"""
    ENTHUSIASTIC = "enthusiastic"
    SUPPORTIVE = "supportive"
    ANALYTICAL = "analytical"
    QUIET_RECOGNITION = "quiet_recognition"
    MOTIVATIONAL = "motivational"
    PERSONAL = "personal"
    SOCIAL = "social"

class ProgressMagnitude(Enum):
    """Magnitude of progress to calibrate celebration"""
    MINOR = "minor"
    MODERATE = "moderate"
    SIGNIFICANT = "significant"
    MAJOR = "major"
    EXCEPTIONAL = "exceptional"

@dataclass
class Achievement:
    """Individual achievement record"""
    achievement_id: str
    user_id: str
    achievement_type: AchievementType
    
    # Achievement details
    title: str
    description: str
    magnitude: ProgressMagnitude
    
    # Data supporting the achievement
    metric_improved: str
    previous_value: float
    new_value: float
    improvement_amount: float
    improvement_percentage: float
    
    # Context
    session_context: Dict[str, Any] = field(default_factory=dict)
    timeframe: str = ""  # "session", "day", "week", "month"
    
    # Recognition details
    celebration_message: str = ""
    celebration_style: CelebrationStyle = CelebrationStyle.SUPPORTIVE
    
    # Metadata
    achieved_at: datetime = field(default_factory=datetime.now)
    recognized_at: Optional[datetime] = None
    is_celebrated: bool = False
    
    # Social and sharing
    is_shareable: bool = True
    share_message: str = ""
    milestone_badge: Optional[str] = None

@dataclass
class ProgressMoment:
    """A moment of notable progress"""
    moment_id: str
    user_id: str
    moment_type: str
    
    # Progress details
    description: str
    significance_score: float
    progress_data: Dict[str, Any] = field(default_factory=dict)
    
    # Recognition
    recognition_message: str = ""
    encouragement_level: str = "medium"
    
    # Temporal context
    occurred_at: datetime = field(default_factory=datetime.now)
    session_id: Optional[str] = None

@dataclass
class MilestoneTracker:
    """Tracker for milestone progress"""
    user_id: str
    metric_name: str
    
    # Milestone definitions
    milestones: List[float] = field(default_factory=list)
    achieved_milestones: List[float] = field(default_factory=list)
    
    # Current progress
    current_value: float = 0.0
    next_milestone: Optional[float] = None
    progress_to_next: float = 0.0
    
    # Tracking
    last_updated: datetime = field(default_factory=datetime.now)
    tracking_enabled: bool = True

class CelebrationSystem:
    """Main celebration and progress recognition system"""
    
    def __init__(self, user_learning_engine=None):
        self.user_learning_engine = user_learning_engine
        
        # Achievement tracking
        self.user_achievements: Dict[str, List[Achievement]] = defaultdict(list)
        self.progress_moments: Dict[str, List[ProgressMoment]] = defaultdict(list)
        self.milestone_trackers: Dict[str, Dict[str, MilestoneTracker]] = defaultdict(dict)
        
        # Celebration templates and styles
        self._initialize_celebration_templates()
        
        # Achievement detection thresholds
        self._initialize_achievement_thresholds()
        
        logger.info("Celebration System initialized")
    
    def _initialize_celebration_templates(self):
        """Initialize celebration message templates"""
        
        self.celebration_templates = {
            AchievementType.PERSONAL_BEST: {
                CelebrationStyle.ENTHUSIASTIC: [
                    "🎉 PERSONAL BEST! You just achieved your best {metric} ever with {value}! That's absolutely incredible!",
                    "🏆 WOW! New personal record! Your {metric} of {value} is your best performance yet!",
                    "⭐ AMAZING! You've just set a new personal best! {improvement} better than before!"
                ],
                CelebrationStyle.SUPPORTIVE: [
                    "Wonderful! You've achieved a new personal best in {metric}. Your dedication is really paying off!",
                    "That's fantastic progress! Your new best {metric} of {value} shows real improvement.",
                    "Great achievement! You've surpassed your previous best by {improvement}."
                ],
                CelebrationStyle.ANALYTICAL: [
                    "Personal best achieved: {metric} improved to {value}, representing a {percentage}% improvement.",
                    "New performance peak recorded: {metric} = {value}, exceeding previous best by {improvement}.",
                    "Optimal performance achieved: {metric} shows {percentage}% improvement over baseline."
                ],
                CelebrationStyle.QUIET_RECOGNITION: [
                    "Nice work. You've achieved a new personal best in {metric}.",
                    "Good progress. Your {metric} has improved to {value}.",
                    "Well done. That's your best {metric} performance yet."
                ]
            },
            
            AchievementType.BREAKTHROUGH: {
                CelebrationStyle.ENTHUSIASTIC: [
                    "🚀 BREAKTHROUGH MOMENT! You've just made a major leap in your {metric}!",
                    "🔥 This is it! You've broken through to the next level! {improvement} improvement!",
                    "💥 GAME CHANGER! You've just achieved a breakthrough performance!"
                ],
                CelebrationStyle.SUPPORTIVE: [
                    "This is a real breakthrough! You've made significant progress in {metric}.",
                    "What a moment! You've broken through to a new level of performance.",
                    "Incredible breakthrough! Your hard work has led to this major improvement."
                ],
                CelebrationStyle.ANALYTICAL: [
                    "Significant performance breakthrough detected: {metric} improved by {percentage}%.",
                    "Statistical breakthrough achieved: {metric} shows improvement beyond normal variance.",
                    "Performance threshold exceeded: {metric} improvement indicates skill level advancement."
                ]
            },
            
            AchievementType.CONSISTENCY: {
                CelebrationStyle.ENTHUSIASTIC: [
                    "🎯 CONSISTENCY CHAMPION! {streak} sessions of great performance!",
                    "🔄 You're on fire! {streak} consistent sessions in a row!",
                    "⚡ Unstoppable! Your consistency over {timeframe} is remarkable!"
                ],
                CelebrationStyle.SUPPORTIVE: [
                    "Your consistency is really impressive! {streak} strong sessions shows dedication.",
                    "Great consistency! You've maintained excellent performance for {timeframe}.",
                    "Wonderful persistence! Your consistent effort is clearly paying off."
                ]
            },
            
            AchievementType.GOAL_ACHIEVEMENT: {
                CelebrationStyle.ENTHUSIASTIC: [
                    "🎯 GOAL ACHIEVED! You did it! {goal_name} is complete!",
                    "🏆 SUCCESS! You've accomplished your goal ahead of schedule!",
                    "⭐ MISSION ACCOMPLISHED! {goal_name} - goal achieved!"
                ],
                CelebrationStyle.SUPPORTIVE: [
                    "Congratulations! You've successfully achieved your goal: {goal_name}.",
                    "Well done! Your goal of {goal_name} is now complete.",
                    "Excellent work! You've reached your target for {goal_name}."
                ]
            }
        }
        
        # Progress moment templates
        self.progress_templates = {
            "improvement_noted": [
                "I'm seeing some nice improvement in your {area}!",
                "Good progress! Your {area} is getting better.",
                "That's better! Your {area} is improving nicely."
            ],
            "effort_recognition": [
                "I can see you're really working hard on this.",
                "Your dedication to improving is clear.",
                "The effort you're putting in is commendable."
            ],
            "momentum_building": [
                "You're building great momentum!",
                "Keep this up - you're on a roll!",
                "You're gaining excellent momentum!"
            ]
        }
    
    def _initialize_achievement_thresholds(self):
        """Initialize thresholds for achievement detection"""
        
        self.achievement_thresholds = {
            AchievementType.PERSONAL_BEST: {
                "min_improvement": 0.01,  # 1% minimum improvement
                "significance_threshold": 0.05  # 5% for significant achievement
            },
            AchievementType.BREAKTHROUGH: {
                "min_improvement": 0.15,  # 15% improvement for breakthrough
                "statistical_threshold": 2.0  # 2 standard deviations
            },
            AchievementType.CONSISTENCY: {
                "min_sessions": 3,  # Minimum sessions for consistency
                "variance_threshold": 0.1  # Low variance indicates consistency
            },
            AchievementType.IMPROVEMENT_STREAK: {
                "min_streak": 3,  # Minimum consecutive improvements
                "improvement_threshold": 0.02  # 2% minimum per session
            }
        }
        
        # Default milestones for different metrics
        self.default_milestones = {
            "overall_score": [50, 60, 70, 75, 80, 85, 90, 95],
            "consistency_score": [0.6, 0.7, 0.8, 0.85, 0.9, 0.95],
            "improvement_rate": [0.1, 0.2, 0.3, 0.5, 0.7, 1.0],
            "session_count": [5, 10, 20, 30, 50, 75, 100, 150, 200]
        }
    
    def analyze_session_for_achievements(self, user_id: str, session_data: Dict[str, Any],
                                       historical_data: List[Dict] = None) -> List[Achievement]:
        """Analyze session for potential achievements"""
        
        achievements = []
        
        # Personal best detection
        pb_achievements = self._detect_personal_bests(user_id, session_data, historical_data)
        achievements.extend(pb_achievements)
        
        # Breakthrough detection
        breakthrough_achievements = self._detect_breakthroughs(user_id, session_data, historical_data)
        achievements.extend(breakthrough_achievements)
        
        # Consistency tracking
        consistency_achievements = self._detect_consistency_achievements(user_id, session_data, historical_data)
        achievements.extend(consistency_achievements)
        
        # Goal achievement check
        goal_achievements = self._check_goal_achievements(user_id, session_data)
        achievements.extend(goal_achievements)
        
        # First-time achievements
        first_time_achievements = self._detect_first_time_achievements(user_id, session_data, historical_data)
        achievements.extend(first_time_achievements)
        
        # Update milestone trackers
        self._update_milestone_trackers(user_id, session_data)
        
        # Store achievements
        for achievement in achievements:
            self.user_achievements[user_id].append(achievement)
            achievement.recognized_at = datetime.now()
        
        return achievements
    
    def _detect_personal_bests(self, user_id: str, session_data: Dict, 
                              historical_data: List[Dict]) -> List[Achievement]:
        """Detect personal best achievements"""
        
        achievements = []
        
        if not historical_data:
            return achievements
        
        # Metrics to check for personal bests
        metrics_to_check = [
            ('overall_score', 'Overall Score'),
            ('consistency_score', 'Consistency'),
            ('improvement_rate', 'Improvement Rate')
        ]
        
        for metric_key, metric_name in metrics_to_check:
            current_value = session_data.get(metric_key)
            if current_value is None:
                continue
            
            # Get historical values
            historical_values = [data.get(metric_key) for data in historical_data 
                               if data.get(metric_key) is not None]
            
            if not historical_values:
                # First measurement - could be a "first achievement"
                continue
            
            previous_best = max(historical_values)
            improvement = current_value - previous_best
            improvement_percentage = (improvement / previous_best) * 100 if previous_best > 0 else 0
            
            # Check if this is a personal best
            if (improvement > 0 and 
                improvement >= self.achievement_thresholds[AchievementType.PERSONAL_BEST]["min_improvement"]):
                
                # Determine magnitude
                if improvement_percentage >= 20:
                    magnitude = ProgressMagnitude.EXCEPTIONAL
                elif improvement_percentage >= 10:
                    magnitude = ProgressMagnitude.MAJOR
                elif improvement_percentage >= 5:
                    magnitude = ProgressMagnitude.SIGNIFICANT
                elif improvement_percentage >= 2:
                    magnitude = ProgressMagnitude.MODERATE
                else:
                    magnitude = ProgressMagnitude.MINOR
                
                achievement = Achievement(
                    achievement_id=f"{user_id}_pb_{metric_key}_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                    user_id=user_id,
                    achievement_type=AchievementType.PERSONAL_BEST,
                    title=f"Personal Best: {metric_name}",
                    description=f"Achieved new personal best {metric_name} of {current_value:.2f}",
                    magnitude=magnitude,
                    metric_improved=metric_name,
                    previous_value=previous_best,
                    new_value=current_value,
                    improvement_amount=improvement,
                    improvement_percentage=improvement_percentage,
                    session_context=session_data
                )
                
                achievements.append(achievement)
        
        return achievements
    
    def _detect_breakthroughs(self, user_id: str, session_data: Dict,
                            historical_data: List[Dict]) -> List[Achievement]:
        """Detect breakthrough moments"""
        
        achievements = []
        
        if not historical_data or len(historical_data) < 5:
            return achievements
        
        # Look for breakthrough in overall score
        current_score = session_data.get('overall_score')
        if current_score is None:
            return achievements
        
        # Calculate recent average and variance
        recent_scores = [data.get('overall_score') for data in historical_data[-10:] 
                        if data.get('overall_score') is not None]
        
        if len(recent_scores) < 3:
            return achievements
        
        recent_mean = np.mean(recent_scores)
        recent_std = np.std(recent_scores)
        
        # Check for breakthrough (performance significantly above recent average)
        if recent_std > 0:
            z_score = (current_score - recent_mean) / recent_std
            
            if z_score >= self.achievement_thresholds[AchievementType.BREAKTHROUGH]["statistical_threshold"]:
                improvement_percentage = ((current_score - recent_mean) / recent_mean) * 100
                
                # Determine magnitude based on z-score
                if z_score >= 3.0:
                    magnitude = ProgressMagnitude.EXCEPTIONAL
                elif z_score >= 2.5:
                    magnitude = ProgressMagnitude.MAJOR
                else:
                    magnitude = ProgressMagnitude.SIGNIFICANT
                
                achievement = Achievement(
                    achievement_id=f"{user_id}_breakthrough_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                    user_id=user_id,
                    achievement_type=AchievementType.BREAKTHROUGH,
                    title="Performance Breakthrough!",
                    description=f"Achieved breakthrough performance with score of {current_score:.2f}",
                    magnitude=magnitude,
                    metric_improved="Overall Performance",
                    previous_value=recent_mean,
                    new_value=current_score,
                    improvement_amount=current_score - recent_mean,
                    improvement_percentage=improvement_percentage,
                    session_context=session_data
                )
                
                achievements.append(achievement)
        
        return achievements
    
    def _detect_consistency_achievements(self, user_id: str, session_data: Dict,
                                       historical_data: List[Dict]) -> List[Achievement]:
        """Detect consistency achievements"""
        
        achievements = []
        
        if not historical_data or len(historical_data) < 3:
            return achievements
        
        # Check recent consistency
        recent_sessions = historical_data[-5:] + [session_data]
        scores = [session.get('overall_score') for session in recent_sessions 
                 if session.get('overall_score') is not None]
        
        if len(scores) < 3:
            return achievements
        
        # Calculate consistency metrics
        mean_score = np.mean(scores)
        score_variance = np.var(scores)
        consistency_score = max(0, 1 - (score_variance / (mean_score ** 2))) if mean_score > 0 else 0
        
        # Check for consistency achievement
        threshold = self.achievement_thresholds[AchievementType.CONSISTENCY]["variance_threshold"]
        min_sessions = self.achievement_thresholds[AchievementType.CONSISTENCY]["min_sessions"]
        
        if (score_variance < threshold and 
            len(scores) >= min_sessions and 
            mean_score > 60):  # Minimum performance threshold
            
            # Determine magnitude based on consistency and performance level
            if consistency_score > 0.9 and mean_score > 80:
                magnitude = ProgressMagnitude.MAJOR
            elif consistency_score > 0.8 and mean_score > 70:
                magnitude = ProgressMagnitude.SIGNIFICANT
            else:
                magnitude = ProgressMagnitude.MODERATE
            
            achievement = Achievement(
                achievement_id=f"{user_id}_consistency_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                user_id=user_id,
                achievement_type=AchievementType.CONSISTENCY,
                title="Consistency Master!",
                description=f"Maintained consistent performance over {len(scores)} sessions",
                magnitude=magnitude,
                metric_improved="Consistency",
                previous_value=0,
                new_value=consistency_score,
                improvement_amount=consistency_score,
                improvement_percentage=consistency_score * 100,
                session_context={"consistency_score": consistency_score, "session_count": len(scores)},
                timeframe=f"{len(scores)} sessions"
            )
            
            achievements.append(achievement)
        
        return achievements
    
    def _check_goal_achievements(self, user_id: str, session_data: Dict) -> List[Achievement]:
        """Check for goal achievements"""
        
        achievements = []
        
        # This would integrate with the user's goal system
        # For now, we'll check for common goal patterns
        
        current_score = session_data.get('overall_score', 0)
        
        # Common score-based goals
        score_milestones = [60, 70, 75, 80, 85, 90]
        
        for milestone in score_milestones:
            # Check if user just achieved this milestone
            if (current_score >= milestone and 
                not self._has_achieved_milestone(user_id, f"score_{milestone}")):
                
                magnitude = ProgressMagnitude.SIGNIFICANT if milestone >= 80 else ProgressMagnitude.MODERATE
                
                achievement = Achievement(
                    achievement_id=f"{user_id}_goal_score_{milestone}_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                    user_id=user_id,
                    achievement_type=AchievementType.GOAL_ACHIEVEMENT,
                    title=f"Score Milestone: {milestone}",
                    description=f"Achieved target score of {milestone} points",
                    magnitude=magnitude,
                    metric_improved="Overall Score",
                    previous_value=milestone - 1,
                    new_value=current_score,
                    improvement_amount=current_score - milestone,
                    improvement_percentage=((current_score - milestone) / milestone) * 100,
                    session_context=session_data
                )
                
                achievements.append(achievement)
        
        return achievements
    
    def _detect_first_time_achievements(self, user_id: str, session_data: Dict,
                                      historical_data: List[Dict]) -> List[Achievement]:
        """Detect first-time achievements"""
        
        achievements = []
        
        # First time breaking certain thresholds
        current_score = session_data.get('overall_score', 0)
        
        thresholds = {
            50: "Breaking 50 - Great Start!",
            60: "Breaking 60 - Good Progress!",
            70: "Breaking 70 - Solid Performance!",
            80: "Breaking 80 - Excellent!",
            90: "Breaking 90 - Outstanding!"
        }
        
        for threshold, title in thresholds.items():
            if current_score >= threshold:
                # Check if this is the first time
                previous_scores = [data.get('overall_score', 0) for data in historical_data or []]
                if not any(score >= threshold for score in previous_scores):
                    
                    achievement = Achievement(
                        achievement_id=f"{user_id}_first_{threshold}_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                        user_id=user_id,
                        achievement_type=AchievementType.FIRST_TIME,
                        title=title,
                        description=f"First time achieving a score of {threshold} or higher",
                        magnitude=ProgressMagnitude.SIGNIFICANT,
                        metric_improved="Overall Score",
                        previous_value=max(previous_scores) if previous_scores else 0,
                        new_value=current_score,
                        improvement_amount=current_score - threshold,
                        improvement_percentage=((current_score - threshold) / threshold) * 100,
                        session_context=session_data
                    )
                    
                    achievements.append(achievement)
        
        return achievements
    
    def _has_achieved_milestone(self, user_id: str, milestone_id: str) -> bool:
        """Check if user has already achieved a specific milestone"""
        
        user_achievements = self.user_achievements.get(user_id, [])
        return any(milestone_id in achievement.achievement_id for achievement in user_achievements)
    
    def _update_milestone_trackers(self, user_id: str, session_data: Dict):
        """Update milestone tracking for the user"""
        
        for metric, milestones in self.default_milestones.items():
            current_value = session_data.get(metric)
            if current_value is None:
                continue
            
            if metric not in self.milestone_trackers[user_id]:
                self.milestone_trackers[user_id][metric] = MilestoneTracker(
                    user_id=user_id,
                    metric_name=metric,
                    milestones=milestones.copy()
                )
            
            tracker = self.milestone_trackers[user_id][metric]
            tracker.current_value = current_value
            
            # Update achieved milestones
            for milestone in milestones:
                if current_value >= milestone and milestone not in tracker.achieved_milestones:
                    tracker.achieved_milestones.append(milestone)
            
            # Find next milestone
            next_milestones = [m for m in milestones if m > current_value]
            if next_milestones:
                tracker.next_milestone = min(next_milestones)
                tracker.progress_to_next = (current_value / tracker.next_milestone) if tracker.next_milestone > 0 else 0
            else:
                tracker.next_milestone = None
                tracker.progress_to_next = 1.0
            
            tracker.last_updated = datetime.now()
    
    def celebrate_achievements(self, user_id: str, achievements: List[Achievement]) -> List[str]:
        """Generate celebration messages for achievements"""
        
        if not achievements:
            return []
        
        # Get user's preferred celebration style
        celebration_style = self._get_user_celebration_style(user_id)
        
        celebration_messages = []
        
        for achievement in achievements:
            message = self._generate_celebration_message(achievement, celebration_style)
            achievement.celebration_message = message
            achievement.celebration_style = celebration_style
            achievement.is_celebrated = True
            
            celebration_messages.append(message)
        
        return celebration_messages
    
    def _get_user_celebration_style(self, user_id: str) -> CelebrationStyle:
        """Get user's preferred celebration style"""
        
        # Get user profile if available
        if self.user_learning_engine:
            try:
                user_profile = self.user_learning_engine.get_or_create_profile(user_id)
                
                # Map learning style to celebration style
                if user_profile.learning_style.value == "analytical":
                    return CelebrationStyle.ANALYTICAL
                elif user_profile.motivation_type.value in ["achievement", "competition"]:
                    return CelebrationStyle.ENTHUSIASTIC
                elif user_profile.motivation_type.value == "mastery":
                    return CelebrationStyle.SUPPORTIVE
                elif user_profile.preferred_encouragement_level == "low":
                    return CelebrationStyle.QUIET_RECOGNITION
                
            except Exception as e:
                logger.warning(f"Could not get user profile: {e}")
        
        # Default to supportive style
        return CelebrationStyle.SUPPORTIVE
    
    def _generate_celebration_message(self, achievement: Achievement, 
                                    style: CelebrationStyle) -> str:
        """Generate personalized celebration message"""
        
        templates = self.celebration_templates.get(achievement.achievement_type, {})
        style_templates = templates.get(style, templates.get(CelebrationStyle.SUPPORTIVE, []))
        
        if not style_templates:
            # Fallback message
            return f"Congratulations! You've achieved: {achievement.title}"
        
        # Select template based on magnitude
        if achievement.magnitude in [ProgressMagnitude.MAJOR, ProgressMagnitude.EXCEPTIONAL]:
            template = style_templates[0] if len(style_templates) > 0 else style_templates[0]
        elif achievement.magnitude == ProgressMagnitude.SIGNIFICANT:
            template = style_templates[1] if len(style_templates) > 1 else style_templates[0]
        else:
            template = style_templates[-1] if len(style_templates) > 2 else style_templates[0]
        
        # Fill in template variables
        message = template.format(
            metric=achievement.metric_improved,
            value=f"{achievement.new_value:.2f}",
            improvement=f"{achievement.improvement_amount:.2f}",
            percentage=f"{achievement.improvement_percentage:.1f}",
            goal_name=achievement.title,
            streak=achievement.session_context.get('session_count', ''),
            timeframe=achievement.timeframe or 'recent sessions'
        )
        
        return message
    
    def detect_progress_moments(self, user_id: str, session_data: Dict,
                              conversation_context: Dict = None) -> List[ProgressMoment]:
        """Detect smaller progress moments for ongoing encouragement"""
        
        moments = []
        
        # Improvement in specific areas
        improvements = session_data.get('improvements', [])
        for improvement in improvements:
            moment = ProgressMoment(
                moment_id=f"{user_id}_progress_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                user_id=user_id,
                moment_type="improvement_noted",
                description=f"Improvement noted in {improvement}",
                significance_score=0.6,
                progress_data={"area": improvement},
                session_id=session_data.get('session_id')
            )
            moments.append(moment)
        
        # Effort recognition
        session_duration = session_data.get('session_duration', 0)
        if session_duration > 20:  # Long session indicates effort
            moment = ProgressMoment(
                moment_id=f"{user_id}_effort_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                user_id=user_id,
                moment_type="effort_recognition",
                description="Extended practice session shows dedication",
                significance_score=0.5,
                progress_data={"duration": session_duration},
                session_id=session_data.get('session_id')
            )
            moments.append(moment)
        
        # Engagement recognition
        if conversation_context:
            engagement = conversation_context.get('engagement_level', 0)
            if engagement > 0.8:
                moment = ProgressMoment(
                    moment_id=f"{user_id}_engagement_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                    user_id=user_id,
                    moment_type="momentum_building",
                    description="High engagement shows great momentum",
                    significance_score=0.7,
                    progress_data={"engagement": engagement},
                    session_id=session_data.get('session_id')
                )
                moments.append(moment)
        
        # Generate recognition messages
        for moment in moments:
            moment.recognition_message = self._generate_progress_message(moment)
        
        # Store moments
        self.progress_moments[user_id].extend(moments)
        
        return moments
    
    def _generate_progress_message(self, moment: ProgressMoment) -> str:
        """Generate message for progress moment"""
        
        templates = self.progress_templates.get(moment.moment_type, [])
        
        if not templates:
            return f"Good progress noted: {moment.description}"
        
        template = random.choice(templates)
        
        # Fill in template variables
        if moment.moment_type == "improvement_noted":
            area = moment.progress_data.get('area', 'your technique')
            return template.format(area=area)
        
        return template
    
    def get_celebration_summary(self, user_id: str, timeframe: str = "week") -> Dict[str, Any]:
        """Get summary of celebrations and progress for a timeframe"""
        
        end_date = datetime.now()
        
        if timeframe == "week":
            start_date = end_date - timedelta(days=7)
        elif timeframe == "month":
            start_date = end_date - timedelta(days=30)
        elif timeframe == "session":
            start_date = end_date - timedelta(hours=1)
        else:
            start_date = end_date - timedelta(days=7)
        
        # Filter achievements and moments
        user_achievements = self.user_achievements.get(user_id, [])
        user_moments = self.progress_moments.get(user_id, [])
        
        recent_achievements = [
            a for a in user_achievements 
            if a.achieved_at >= start_date
        ]
        
        recent_moments = [
            m for m in user_moments 
            if m.occurred_at >= start_date
        ]
        
        # Get milestone progress
        milestone_progress = {}
        for metric, tracker in self.milestone_trackers.get(user_id, {}).items():
            milestone_progress[metric] = {
                "current_value": tracker.current_value,
                "next_milestone": tracker.next_milestone,
                "progress_percentage": tracker.progress_to_next * 100,
                "milestones_achieved": len(tracker.achieved_milestones)
            }
        
        summary = {
            "timeframe": timeframe,
            "achievements": [
                {
                    "type": a.achievement_type.value,
                    "title": a.title,
                    "description": a.description,
                    "magnitude": a.magnitude.value,
                    "celebration_message": a.celebration_message,
                    "achieved_at": a.achieved_at.isoformat()
                }
                for a in recent_achievements
            ],
            "progress_moments": [
                {
                    "type": m.moment_type,
                    "description": m.description,
                    "message": m.recognition_message,
                    "significance": m.significance_score
                }
                for m in recent_moments
            ],
            "milestone_progress": milestone_progress,
            "total_achievements": len(recent_achievements),
            "total_progress_moments": len(recent_moments)
        }
        
        return summary
    
    def get_next_milestones(self, user_id: str) -> Dict[str, Any]:
        """Get information about upcoming milestones"""
        
        next_milestones = {}
        
        for metric, tracker in self.milestone_trackers.get(user_id, {}).items():
            if tracker.next_milestone:
                next_milestones[metric] = {
                    "metric_name": metric.replace('_', ' ').title(),
                    "current_value": tracker.current_value,
                    "next_milestone": tracker.next_milestone,
                    "progress_percentage": tracker.progress_to_next * 100,
                    "remaining": tracker.next_milestone - tracker.current_value,
                    "estimated_sessions": self._estimate_sessions_to_milestone(
                        tracker.current_value, tracker.next_milestone, metric
                    )
                }
        
        return next_milestones
    
    def _estimate_sessions_to_milestone(self, current: float, target: float, metric: str) -> int:
        """Estimate sessions needed to reach milestone"""
        
        # Simple estimation based on typical improvement rates
        improvement_rates = {
            "overall_score": 0.5,  # points per session
            "consistency_score": 0.02,  # improvement per session
            "improvement_rate": 0.01,  # rate improvement per session
            "session_count": 1  # obviously 1 per session
        }
        
        rate = improvement_rates.get(metric, 0.1)
        remaining = target - current
        
        if rate <= 0:
            return 999  # Unknown
        
        estimated = int(remaining / rate)
        return max(1, min(estimated, 50))  # Cap between 1 and 50 sessions
    
    def generate_motivation_message(self, user_id: str) -> str:
        """Generate motivational message based on recent progress"""
        
        recent_achievements = self.user_achievements.get(user_id, [])[-3:]  # Last 3 achievements
        recent_moments = self.progress_moments.get(user_id, [])[-5:]  # Last 5 moments
        
        if recent_achievements:
            latest_achievement = recent_achievements[-1]
            return f"Building on your recent {latest_achievement.title.lower()}, you're making excellent progress! Keep up the great work!"
        
        elif recent_moments:
            return "I'm seeing consistent progress in your practice. Every session is building toward your goals!"
        
        else:
            return "Every practice session is an opportunity for improvement. You're on the right path!"
    
    def get_celebration_insights(self, user_id: str) -> Dict[str, Any]:
        """Get insights about user's celebration and progress patterns"""
        
        user_achievements = self.user_achievements.get(user_id, [])
        user_moments = self.progress_moments.get(user_id, [])
        
        insights = {
            "achievement_summary": {
                "total_achievements": len(user_achievements),
                "achievement_types": {},
                "recent_achievements": len([a for a in user_achievements if a.achieved_at >= datetime.now() - timedelta(days=30)]),
                "biggest_improvement": 0.0
            },
            "progress_patterns": {
                "average_progress_moments_per_session": 0.0,
                "most_improved_area": "",
                "consistency_trends": "stable"
            },
            "motivation_insights": {
                "celebration_style": self._get_user_celebration_style(user_id).value,
                "response_to_achievements": "positive",
                "milestone_focus": ""
            }
        }
        
        # Achievement type distribution
        if user_achievements:
            type_counts = {}
            for achievement in user_achievements:
                type_name = achievement.achievement_type.value
                type_counts[type_name] = type_counts.get(type_name, 0) + 1
            
            insights["achievement_summary"]["achievement_types"] = type_counts
            insights["achievement_summary"]["biggest_improvement"] = max(
                (a.improvement_percentage for a in user_achievements), default=0.0
            )
        
        return insights