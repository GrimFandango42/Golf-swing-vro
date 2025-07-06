"""
Enhanced Mock Data Factory for SwingSync AI Comprehensive Testing.

This module provides advanced utility functions to generate realistic mock data
for testing all aspects of the golf swing analysis system, including:

- Realistic swing scenarios for all P-positions (P1-P10)
- Club-specific swing variations (Driver, Irons, Wedges)
- Streaming frame data for real-time testing
- User profile and session data for database testing
- Mock API responses for external service testing
- Performance testing data with varying quality and conditions

Key Features:
- Physics-based swing trajectory generation
- Club-specific biomechanical variations
- Fault injection for negative testing
- Streaming data simulation with realistic timing
- Database entity factories with relationships
- Mock Gemini API responses for testing without API costs
"""

import copy
import math
import random
import time
import uuid
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple, Union
from dataclasses import dataclass
from enum import Enum

import numpy as np

# Import project modules
from data_structures import (
    SwingVideoAnalysisInput, FramePoseData, PSystemPhase, PoseKeypoint,
    BiomechanicalKPI, DetectedFault, SwingAnalysisFeedback, LLMGeneratedTip
)

# Constants for realistic swing generation
DEFAULT_FPS = 60.0
SWING_DURATION_SECONDS = 2.5  # Typical swing duration
FRAMES_PER_SECOND = 60
TOTAL_SWING_FRAMES = int(SWING_DURATION_SECONDS * FRAMES_PER_SECOND)

# Physical constraints for realistic motion
SHOULDER_WIDTH = 0.4  # meters
HIP_WIDTH = 0.3  # meters
ARM_LENGTH = 0.6  # meters
TORSO_HEIGHT = 0.5  # meters

class SwingQuality(Enum):
    """Quality levels for generated swings"""
    EXCELLENT = "excellent"
    GOOD = "good"
    AVERAGE = "average"
    POOR = "poor"
    TERRIBLE = "terrible"

class ClubType(Enum):
    """Club types with different characteristics"""
    DRIVER = "driver"
    FAIRWAY_WOOD = "fairway_wood"
    HYBRID = "hybrid"
    LONG_IRON = "long_iron"
    MID_IRON = "mid_iron"
    SHORT_IRON = "short_iron"
    WEDGE = "wedge"
    PUTTER = "putter"

@dataclass
class SwingCharacteristics:
    """Characteristics that define a swing's biomechanics"""
    setup_weight_distribution: float  # 0-1, lead foot percentage
    backswing_shoulder_turn: float  # degrees
    backswing_hip_turn: float  # degrees
    hip_hinge_angle: float  # degrees from vertical
    knee_flex_lead: float  # degrees
    knee_flex_trail: float  # degrees
    lead_wrist_angle_top: float  # degrees (positive = cupped)
    swing_tempo: float  # multiplier for timing
    lateral_sway: float  # meters of lateral movement
    reverse_spine_angle: float  # degrees of reverse tilt

def _make_kp(x: float, y: float, z: float, visibility: float = 1.0) -> PoseKeypoint:
    """Helper to create a PoseKeypoint with optional noise"""
    return {"x": x, "y": y, "z": z, "visibility": visibility}

def add_realistic_noise(keypoint: PoseKeypoint, noise_level: float = 0.01) -> PoseKeypoint:
    """Add realistic pose estimation noise to keypoints"""
    noise_x = random.gauss(0, noise_level)
    noise_y = random.gauss(0, noise_level)
    noise_z = random.gauss(0, noise_level)
    visibility_noise = random.gauss(0, 0.05)
    
    return {
        "x": keypoint["x"] + noise_x,
        "y": keypoint["y"] + noise_y,
        "z": keypoint["z"] + noise_z,
        "visibility": max(0.0, min(1.0, keypoint.get("visibility", 1.0) + visibility_noise))
    }

class PhysicsBasedSwingGenerator:
    """Generates realistic swing motion using physics-based interpolation"""
    
    def __init__(self, characteristics: SwingCharacteristics, club_type: ClubType):
        self.char = characteristics
        self.club_type = club_type
        self.setup_keypoints = self._generate_setup_position()
        
    def _generate_setup_position(self) -> FramePoseData:
        """Generate realistic setup position based on characteristics"""
        # Base setup position
        setup = {
            "nose": _make_kp(0, 1.7, 0.1),
            "left_shoulder": _make_kp(-SHOULDER_WIDTH/2, 1.4, 0),
            "right_shoulder": _make_kp(SHOULDER_WIDTH/2, 1.4, 0),
            "left_elbow": _make_kp(-0.3, 1.2, 0.1),
            "right_elbow": _make_kp(0.3, 1.2, 0.1),
            "left_wrist": _make_kp(-0.35, 1.0, 0.2),
            "right_wrist": _make_kp(0.35, 1.0, 0.2),
            "left_hip": _make_kp(-HIP_WIDTH/2, 0.9, 0),
            "right_hip": _make_kp(HIP_WIDTH/2, 0.9, 0),
            "left_knee": _make_kp(-0.2, 0.5, 0.05),
            "right_knee": _make_kp(0.2, 0.5, 0.05),
            "left_ankle": _make_kp(-0.25, 0.1, 0),
            "right_ankle": _make_kp(0.25, 0.1, 0),
        }
        
        # Apply hip hinge angle
        hip_hinge_rad = math.radians(self.char.hip_hinge_angle)
        spine_tilt = math.sin(hip_hinge_rad) * TORSO_HEIGHT
        
        # Adjust shoulder positions for hip hinge
        setup["left_shoulder"]["z"] -= spine_tilt
        setup["right_shoulder"]["z"] -= spine_tilt
        
        # Apply knee flexion
        knee_drop_lead = math.sin(math.radians(self.char.knee_flex_lead)) * 0.1
        knee_drop_trail = math.sin(math.radians(self.char.knee_flex_trail)) * 0.1
        
        setup["left_knee"]["y"] -= knee_drop_lead
        setup["right_knee"]["y"] -= knee_drop_trail
        
        # Apply weight distribution (shift ankle positions)
        weight_shift = (self.char.setup_weight_distribution - 0.5) * 0.1
        setup["left_ankle"]["x"] -= weight_shift
        setup["right_ankle"]["x"] += weight_shift
        
        return setup
    
    def generate_swing_sequence(self, total_frames: int = TOTAL_SWING_FRAMES) -> List[FramePoseData]:
        """Generate complete swing sequence with realistic motion"""
        frames = []
        
        # Define key positions throughout swing
        key_positions = self._define_key_positions()
        
        # Generate frames using spline interpolation between key positions
        for frame_idx in range(total_frames):
            progress = frame_idx / (total_frames - 1)
            frame_data = self._interpolate_frame(progress, key_positions)
            frames.append(frame_data)
        
        return frames
    
    def _define_key_positions(self) -> Dict[float, FramePoseData]:
        """Define key positions at specific points in the swing"""
        positions = {}
        
        # P1 - Setup (0%)
        positions[0.0] = copy.deepcopy(self.setup_keypoints)
        
        # P2 - Takeaway (15%)
        positions[0.15] = self._generate_takeaway_position()
        
        # P3 - Halfway Back (35%)
        positions[0.35] = self._generate_halfway_back_position()
        
        # P4 - Top of Backswing (50%)
        positions[0.50] = self._generate_top_position()
        
        # P5 - Halfway Down (65%)
        positions[0.65] = self._generate_halfway_down_position()
        
        # P6 - Pre-Impact (80%)
        positions[0.80] = self._generate_pre_impact_position()
        
        # P7 - Impact (85%)
        positions[0.85] = self._generate_impact_position()
        
        # P8 - Post-Impact (90%)
        positions[0.90] = self._generate_post_impact_position()
        
        # P9 - Follow Through (95%)
        positions[0.95] = self._generate_follow_through_position()
        
        # P10 - Finish (100%)
        positions[1.0] = self._generate_finish_position()
        
        return positions
    
    def _generate_takeaway_position(self) -> FramePoseData:
        """Generate P2 takeaway position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Small shoulder rotation (10-15 degrees)
        rotation = math.radians(12)
        for side in ["left", "right"]:
            shoulder = frame[f"{side}_shoulder"]
            x, z = shoulder["x"], shoulder["z"]
            # Rotate around shoulder center
            new_x = x * math.cos(rotation) - z * math.sin(rotation)
            new_z = x * math.sin(rotation) + z * math.cos(rotation)
            shoulder["x"] = new_x
            shoulder["z"] = new_z
        
        return frame
    
    def _generate_halfway_back_position(self) -> FramePoseData:
        """Generate P3 halfway back position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Significant shoulder rotation (45 degrees)
        target_rotation = self.char.backswing_shoulder_turn * 0.5
        rotation = math.radians(target_rotation)
        
        # Apply shoulder rotation
        self._apply_shoulder_rotation(frame, rotation)
        
        # Begin hip rotation (20-30% of full turn)
        hip_rotation = math.radians(self.char.backswing_hip_turn * 0.3)
        self._apply_hip_rotation(frame, hip_rotation)
        
        return frame
    
    def _generate_top_position(self) -> FramePoseData:
        """Generate P4 top of backswing position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Full shoulder rotation
        shoulder_rotation = math.radians(self.char.backswing_shoulder_turn)
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        # Hip rotation (typically 45-50% of shoulder turn)
        hip_rotation = math.radians(self.char.backswing_hip_turn)
        self._apply_hip_rotation(frame, hip_rotation)
        
        # Apply lead wrist angle
        self._apply_wrist_angle(frame, self.char.lead_wrist_angle_top)
        
        # Apply lateral sway if present
        if self.char.lateral_sway > 0:
            self._apply_lateral_sway(frame, self.char.lateral_sway)
        
        # Apply reverse spine angle if present
        if self.char.reverse_spine_angle > 0:
            self._apply_reverse_spine_angle(frame, self.char.reverse_spine_angle)
        
        return frame
    
    def _generate_halfway_down_position(self) -> FramePoseData:
        """Generate P5 halfway down position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Shoulders beginning to unwind (70% of backswing rotation)
        shoulder_rotation = math.radians(self.char.backswing_shoulder_turn * 0.7)
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        # Hips leading (30% of backswing rotation)
        hip_rotation = math.radians(self.char.backswing_hip_turn * 0.3)
        self._apply_hip_rotation(frame, hip_rotation)
        
        return frame
    
    def _generate_pre_impact_position(self) -> FramePoseData:
        """Generate P6 pre-impact position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Shoulders continue unwinding (30% of backswing)
        shoulder_rotation = math.radians(self.char.backswing_shoulder_turn * 0.3)
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        # Hips nearly square (10% of backswing)
        hip_rotation = math.radians(self.char.backswing_hip_turn * 0.1)
        self._apply_hip_rotation(frame, hip_rotation)
        
        return frame
    
    def _generate_impact_position(self) -> FramePoseData:
        """Generate P7 impact position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Shoulders nearly square
        shoulder_rotation = math.radians(self.char.backswing_shoulder_turn * 0.1)
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        # Hips slightly open to target
        hip_rotation = math.radians(-10)  # Negative = open to target
        self._apply_hip_rotation(frame, hip_rotation)
        
        return frame
    
    def _generate_post_impact_position(self) -> FramePoseData:
        """Generate P8 post-impact position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Shoulders beginning to rotate through
        shoulder_rotation = math.radians(-20)  # Negative = through impact
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        # Hips open to target
        hip_rotation = math.radians(-30)
        self._apply_hip_rotation(frame, hip_rotation)
        
        return frame
    
    def _generate_follow_through_position(self) -> FramePoseData:
        """Generate P9 follow through position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Significant rotation through
        shoulder_rotation = math.radians(-60)
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        hip_rotation = math.radians(-45)
        self._apply_hip_rotation(frame, hip_rotation)
        
        # Raise arms
        frame["left_wrist"]["y"] += 0.4
        frame["right_wrist"]["y"] += 0.4
        
        return frame
    
    def _generate_finish_position(self) -> FramePoseData:
        """Generate P10 finish position"""
        frame = copy.deepcopy(self.setup_keypoints)
        
        # Full rotation through
        shoulder_rotation = math.radians(-90)
        self._apply_shoulder_rotation(frame, shoulder_rotation)
        
        hip_rotation = math.radians(-60)
        self._apply_hip_rotation(frame, hip_rotation)
        
        # High finish position
        frame["left_wrist"]["y"] += 0.6
        frame["right_wrist"]["y"] += 0.6
        
        return frame
    
    def _apply_shoulder_rotation(self, frame: FramePoseData, rotation: float):
        """Apply shoulder rotation to frame"""
        for side in ["left", "right"]:
            shoulder = frame[f"{side}_shoulder"]
            elbow = frame[f"{side}_elbow"]
            wrist = frame[f"{side}_wrist"]
            
            # Rotate around spine axis
            for joint in [shoulder, elbow, wrist]:
                x, z = joint["x"], joint["z"]
                joint["x"] = x * math.cos(rotation) - z * math.sin(rotation)
                joint["z"] = x * math.sin(rotation) + z * math.cos(rotation)
    
    def _apply_hip_rotation(self, frame: FramePoseData, rotation: float):
        """Apply hip rotation to frame"""
        for side in ["left", "right"]:
            hip = frame[f"{side}_hip"]
            knee = frame[f"{side}_knee"]
            ankle = frame[f"{side}_ankle"]
            
            for joint in [hip, knee, ankle]:
                x, z = joint["x"], joint["z"]
                joint["x"] = x * math.cos(rotation) - z * math.sin(rotation)
                joint["z"] = x * math.sin(rotation) + z * math.cos(rotation)
    
    def _apply_wrist_angle(self, frame: FramePoseData, angle: float):
        """Apply lead wrist angle (cupping/bowing)"""
        # Simplified wrist angle application
        wrist_offset = math.sin(math.radians(angle)) * 0.05
        frame["left_wrist"]["z"] += wrist_offset
    
    def _apply_lateral_sway(self, frame: FramePoseData, sway_amount: float):
        """Apply lateral sway to frame"""
        for keypoint_name in frame:
            if "hip" in keypoint_name or "knee" in keypoint_name or "ankle" in keypoint_name:
                frame[keypoint_name]["x"] += sway_amount
    
    def _apply_reverse_spine_angle(self, frame: FramePoseData, angle: float):
        """Apply reverse spine angle"""
        spine_offset = math.sin(math.radians(angle)) * 0.1
        frame["left_shoulder"]["z"] += spine_offset
        frame["right_shoulder"]["z"] += spine_offset
    
    def _interpolate_frame(self, progress: float, key_positions: Dict[float, FramePoseData]) -> FramePoseData:
        """Interpolate frame data between key positions"""
        # Find surrounding key positions
        prev_progress = 0.0
        next_progress = 1.0
        
        for key_progress in sorted(key_positions.keys()):
            if key_progress <= progress:
                prev_progress = key_progress
            elif key_progress > progress:
                next_progress = key_progress
                break
        
        # Get surrounding frames
        prev_frame = key_positions[prev_progress]
        next_frame = key_positions[next_progress]
        
        # Calculate interpolation factor
        if next_progress == prev_progress:
            interp_factor = 0.0
        else:
            interp_factor = (progress - prev_progress) / (next_progress - prev_progress)
        
        # Interpolate each keypoint
        interpolated_frame = {}
        for keypoint_name in prev_frame:
            prev_kp = prev_frame[keypoint_name]
            next_kp = next_frame[keypoint_name]
            
            interpolated_frame[keypoint_name] = {
                "x": prev_kp["x"] + (next_kp["x"] - prev_kp["x"]) * interp_factor,
                "y": prev_kp["y"] + (next_kp["y"] - prev_kp["y"]) * interp_factor,
                "z": prev_kp["z"] + (next_kp["z"] - prev_kp["z"]) * interp_factor,
                "visibility": prev_kp.get("visibility", 1.0)
            }
        
        return interpolated_frame

def get_club_characteristics(club_type: ClubType) -> SwingCharacteristics:
    """Get default swing characteristics for different club types"""
    base_chars = {
        ClubType.DRIVER: SwingCharacteristics(
            setup_weight_distribution=0.42,  # More weight on trail foot
            backswing_shoulder_turn=95.0,
            backswing_hip_turn=42.0,
            hip_hinge_angle=32.0,
            knee_flex_lead=18.0,
            knee_flex_trail=20.0,
            lead_wrist_angle_top=2.0,
            swing_tempo=1.1,  # Slightly slower tempo
            lateral_sway=0.02,
            reverse_spine_angle=0.0
        ),
        ClubType.MID_IRON: SwingCharacteristics(
            setup_weight_distribution=0.52,  # Slightly forward
            backswing_shoulder_turn=88.0,
            backswing_hip_turn=40.0,
            hip_hinge_angle=35.0,
            knee_flex_lead=20.0,
            knee_flex_trail=20.0,
            lead_wrist_angle_top=0.0,
            swing_tempo=1.0,
            lateral_sway=0.01,
            reverse_spine_angle=0.0
        ),
        ClubType.WEDGE: SwingCharacteristics(
            setup_weight_distribution=0.58,  # More weight forward
            backswing_shoulder_turn=75.0,  # Shorter swing
            backswing_hip_turn=35.0,
            hip_hinge_angle=38.0,
            knee_flex_lead=22.0,
            knee_flex_trail=22.0,
            lead_wrist_angle_top=-2.0,  # Slightly bowed
            swing_tempo=0.9,  # Controlled tempo
            lateral_sway=0.005,
            reverse_spine_angle=0.0
        )
    }
    
    return base_chars.get(club_type, base_chars[ClubType.MID_IRON])

def inject_swing_faults(characteristics: SwingCharacteristics, faults: List[str]) -> SwingCharacteristics:
    """Inject specific faults into swing characteristics"""
    modified_chars = copy.deepcopy(characteristics)
    
    for fault in faults:
        if fault == "insufficient_shoulder_turn":
            modified_chars.backswing_shoulder_turn *= 0.6  # Reduce by 40%
        elif fault == "excessive_hip_hinge":
            modified_chars.hip_hinge_angle += 15.0
        elif fault == "poor_knee_flex":
            modified_chars.knee_flex_lead *= 0.5
            modified_chars.knee_flex_trail *= 0.5
        elif fault == "cupped_wrist":
            modified_chars.lead_wrist_angle_top += 20.0
        elif fault == "lateral_sway":
            modified_chars.lateral_sway += 0.1
        elif fault == "reverse_spine":
            modified_chars.reverse_spine_angle += 15.0
        elif fault == "poor_weight_distribution":
            # Extreme weight distribution
            if modified_chars.setup_weight_distribution > 0.5:
                modified_chars.setup_weight_distribution = 0.75
            else:
                modified_chars.setup_weight_distribution = 0.25
    
    return modified_chars

def create_realistic_swing(
    session_id: str = None,
    user_id: str = "test_user",
    club_type: ClubType = ClubType.MID_IRON,
    quality: SwingQuality = SwingQuality.GOOD,
    specific_faults: List[str] = None,
    add_noise: bool = True,
    fps: float = DEFAULT_FPS
) -> SwingVideoAnalysisInput:
    """Create a realistic swing with specified characteristics"""
    
    if session_id is None:
        session_id = f"test_swing_{int(time.time())}"
    
    if specific_faults is None:
        specific_faults = []
    
    # Get base characteristics for club type
    characteristics = get_club_characteristics(club_type)
    
    # Modify characteristics based on quality
    if quality == SwingQuality.EXCELLENT:
        # Optimal characteristics
        pass
    elif quality == SwingQuality.GOOD:
        # Minor variations from ideal
        characteristics.backswing_shoulder_turn += random.uniform(-5, 5)
        characteristics.hip_hinge_angle += random.uniform(-2, 2)
    elif quality == SwingQuality.AVERAGE:
        # Moderate variations
        characteristics.backswing_shoulder_turn += random.uniform(-15, 10)
        characteristics.hip_hinge_angle += random.uniform(-5, 8)
        characteristics.lateral_sway += random.uniform(0, 0.03)
    elif quality == SwingQuality.POOR:
        # Significant issues
        characteristics.backswing_shoulder_turn += random.uniform(-25, -10)
        characteristics.hip_hinge_angle += random.uniform(-8, 15)
        characteristics.lateral_sway += random.uniform(0.02, 0.08)
    elif quality == SwingQuality.TERRIBLE:
        # Multiple major faults
        specific_faults.extend(["insufficient_shoulder_turn", "excessive_hip_hinge", "lateral_sway"])
    
    # Inject specific faults
    if specific_faults:
        characteristics = inject_swing_faults(characteristics, specific_faults)
    
    # Generate swing sequence
    generator = PhysicsBasedSwingGenerator(characteristics, club_type)
    frames = generator.generate_swing_sequence()
    
    # Add realistic noise if requested
    if add_noise:
        noise_level = 0.005 if quality in [SwingQuality.EXCELLENT, SwingQuality.GOOD] else 0.015
        frames = [
            {kp_name: add_realistic_noise(kp, noise_level) for kp_name, kp in frame.items()}
            for frame in frames
        ]
    
    # Create P-system classification
    p_system_phases = create_realistic_p_system_classification(len(frames))
    
    # Map club type to string
    club_names = {
        ClubType.DRIVER: "Driver",
        ClubType.FAIRWAY_WOOD: "3-Wood",
        ClubType.HYBRID: "4-Hybrid",
        ClubType.LONG_IRON: "4-Iron",
        ClubType.MID_IRON: "7-Iron",
        ClubType.SHORT_IRON: "9-Iron",
        ClubType.WEDGE: "Sand Wedge",
        ClubType.PUTTER: "Putter"
    }
    
    return {
        "session_id": session_id,
        "user_id": user_id,
        "club_used": club_names[club_type],
        "frames": frames,
        "p_system_classification": p_system_phases,
        "video_fps": fps
    }

def create_realistic_p_system_classification(total_frames: int) -> List[PSystemPhase]:
    """Create realistic P-system classification with proper timing"""
    # Typical P-system timing percentages
    p_timings = [
        ("P1", 0.0, 0.05),    # Address/Setup
        ("P2", 0.05, 0.15),   # Takeaway
        ("P3", 0.15, 0.35),   # Halfway Back
        ("P4", 0.35, 0.50),   # Top of Backswing
        ("P5", 0.50, 0.65),   # Halfway Down
        ("P6", 0.65, 0.80),   # Pre-Impact
        ("P7", 0.80, 0.85),   # Impact
        ("P8", 0.85, 0.90),   # Post-Impact
        ("P9", 0.90, 0.95),   # Follow Through
        ("P10", 0.95, 1.0),   # Finish
    ]
    
    phases = []
    for phase_name, start_pct, end_pct in p_timings:
        start_frame = int(start_pct * total_frames)
        end_frame = int(end_pct * total_frames) - 1
        
        phases.append({
            "phase_name": phase_name,
            "start_frame_index": start_frame,
            "end_frame_index": end_frame
        })
    
    return phases

# Streaming data generation
def create_streaming_frame_data(frame_index: int, timestamp: float, 
                               keypoints: FramePoseData) -> Dict[str, Any]:
    """Create streaming frame data structure"""
    return {
        "frame_index": frame_index,
        "timestamp": timestamp,
        "keypoints": {name: kp for name, kp in keypoints.items()},
        "frame_metadata": {
            "quality_score": random.uniform(0.7, 1.0),
            "processing_time_ms": random.uniform(5, 15),
            "pose_confidence": random.uniform(0.8, 1.0)
        }
    }

def generate_streaming_session(duration_seconds: float = 30.0, 
                               fps: float = DEFAULT_FPS) -> List[Dict[str, Any]]:
    """Generate a sequence of streaming frames for testing"""
    total_frames = int(duration_seconds * fps)
    streaming_frames = []
    
    # Create base swing for variation
    base_swing = create_realistic_swing(
        club_type=ClubType.MID_IRON,
        quality=SwingQuality.GOOD
    )
    
    start_time = time.time()
    
    for i in range(total_frames):
        # Cycle through swing frames or use setup position
        if i < len(base_swing["frames"]):
            keypoints = base_swing["frames"][i]
        else:
            # Use setup position with slight variations
            keypoints = base_swing["frames"][0]
            # Add small random movements
            for kp_name in keypoints:
                keypoints[kp_name]["x"] += random.uniform(-0.01, 0.01)
                keypoints[kp_name]["y"] += random.uniform(-0.005, 0.005)
                keypoints[kp_name]["z"] += random.uniform(-0.01, 0.01)
        
        timestamp = start_time + (i / fps)
        frame_data = create_streaming_frame_data(i, timestamp, keypoints)
        streaming_frames.append(frame_data)
    
    return streaming_frames

# Database entity factories
def create_mock_user(user_id: str = None, skill_level: str = "intermediate") -> Dict[str, Any]:
    """Create mock user data for database testing"""
    if user_id is None:
        user_id = str(uuid.uuid4())
    
    return {
        "id": user_id,
        "email": f"test_user_{int(time.time())}@example.com",
        "username": f"golfer_{random.randint(1000, 9999)}",
        "hashed_password": "hashed_password_123",
        "first_name": random.choice(["John", "Jane", "Mike", "Sarah", "David", "Emma"]),
        "last_name": random.choice(["Smith", "Johnson", "Williams", "Brown", "Jones"]),
        "date_of_birth": datetime.now() - timedelta(days=random.randint(18*365, 65*365)),
        "skill_level": skill_level,
        "handicap": random.uniform(0, 36) if skill_level != "professional" else random.uniform(-2, 5),
        "preferred_hand": random.choice(["right", "left"]),
        "height_cm": random.uniform(150, 200),
        "weight_kg": random.uniform(50, 120),
        "is_active": True,
        "is_verified": random.choice([True, False]),
        "created_at": datetime.now() - timedelta(days=random.randint(1, 365)),
        "last_login": datetime.now() - timedelta(hours=random.randint(1, 168))
    }

def create_mock_session_data(user_id: str, session_id: str = None) -> Dict[str, Any]:
    """Create mock swing session data"""
    if session_id is None:
        session_id = str(uuid.uuid4())
    
    clubs = ["Driver", "3-Wood", "5-Iron", "7-Iron", "9-Iron", "Sand Wedge", "Pitching Wedge"]
    
    return {
        "id": session_id,
        "user_id": user_id,
        "club_used": random.choice(clubs),
        "session_status": random.choice(["completed", "pending", "processing"]),
        "video_fps": random.choice([30.0, 60.0, 120.0]),
        "total_frames": random.randint(100, 300),
        "video_duration_seconds": random.uniform(1.5, 4.0),
        "processing_time_seconds": random.uniform(2.0, 10.0),
        "created_at": datetime.now() - timedelta(hours=random.randint(1, 72)),
        "location": random.choice(["Driving Range", "Golf Course", "Indoor Simulator", None]),
        "weather_conditions": random.choice([
            {"temperature": 22, "wind": "light", "conditions": "sunny"},
            {"temperature": 18, "wind": "moderate", "conditions": "cloudy"},
            None
        ])
    }

# Mock API responses
def create_mock_gemini_response(fault_context: str, club_used: str = "7-Iron") -> Dict[str, Any]:
    """Create mock Gemini API response for testing"""
    tips = [
        {
            "explanation": f"Your swing with the {club_used} shows room for improvement in the analyzed area.",
            "tip": "Focus on maintaining proper posture throughout your swing sequence.",
            "drill_suggestion": "Practice slow-motion swings to feel the correct positions."
        },
        {
            "explanation": f"The biomechanical analysis of your {club_used} swing indicates specific areas for development.",
            "tip": "Work on creating a more athletic setup position at address.",
            "drill_suggestion": "Use a mirror to check your posture before each practice swing."
        }
    ]
    
    return {
        "summary_of_findings": f"Analysis of your {club_used} swing reveals specific areas for improvement that will help you achieve more consistent ball striking.",
        "detailed_feedback": [random.choice(tips)],
        "confidence_score": random.uniform(0.8, 0.95),
        "processing_time_ms": random.uniform(150, 500)
    }

def create_mock_kpis(p_position: str, club_type: ClubType = ClubType.MID_IRON) -> List[BiomechanicalKPI]:
    """Create realistic mock KPIs for testing"""
    kpis = []
    
    if p_position == "P1":
        kpis.extend([
            {
                "p_position": "P1",
                "kpi_name": "Hip Hinge Angle",
                "value": random.uniform(25, 45),
                "unit": "degrees",
                "ideal_range": (30, 40),
                "notes": "Angle of spine from vertical at address"
            },
            {
                "p_position": "P1",
                "kpi_name": "Lead Knee Flexion",
                "value": random.uniform(10, 30),
                "unit": "degrees",
                "ideal_range": (15, 25),
                "notes": "Flexion angle of lead knee at setup"
            },
            {
                "p_position": "P1",
                "kpi_name": "Weight Distribution",
                "value": random.uniform(0.3, 0.7),
                "unit": "ratio",
                "ideal_range": (0.45, 0.55),
                "notes": "Percentage of weight on lead foot"
            }
        ])
    elif p_position == "P4":
        kpis.extend([
            {
                "p_position": "P4",
                "kpi_name": "Shoulder Turn",
                "value": random.uniform(60, 110),
                "unit": "degrees",
                "ideal_range": (85, 105),
                "notes": "Shoulder rotation at top of backswing"
            },
            {
                "p_position": "P4",
                "kpi_name": "Hip Turn",
                "value": random.uniform(30, 60),
                "unit": "degrees",
                "ideal_range": (40, 50),
                "notes": "Hip rotation at top of backswing"
            },
            {
                "p_position": "P4",
                "kpi_name": "Lead Wrist Angle",
                "value": random.uniform(-10, 20),
                "unit": "degrees",
                "ideal_range": (-5, 5),
                "notes": "Lead wrist cupping/bowing at top"
            }
        ])
    
    return kpis

def create_performance_test_data(num_sessions: int = 100) -> List[SwingVideoAnalysisInput]:
    """Create large dataset for performance testing"""
    test_data = []
    
    club_types = list(ClubType)
    qualities = list(SwingQuality)
    
    for i in range(num_sessions):
        club_type = random.choice(club_types)
        quality = random.choice(qualities)
        
        # Add some specific fault scenarios
        specific_faults = []
        if random.random() < 0.3:  # 30% chance of specific faults
            fault_options = [
                "insufficient_shoulder_turn", "excessive_hip_hinge",
                "poor_knee_flex", "cupped_wrist", "lateral_sway"
            ]
            specific_faults = random.sample(fault_options, random.randint(1, 2))
        
        swing_data = create_realistic_swing(
            session_id=f"perf_test_{i}",
            user_id=f"test_user_{i % 20}",  # 20 different users
            club_type=club_type,
            quality=quality,
            specific_faults=specific_faults
        )
        
        test_data.append(swing_data)
    
    return test_data

if __name__ == "__main__":
    print("=== Enhanced Mock Data Factory Testing ===")
    
    # Test basic swing generation
    print("\n1. Testing basic swing generation...")
    basic_swing = create_realistic_swing()
    print(f"Generated swing with {len(basic_swing['frames'])} frames")
    print(f"Club: {basic_swing['club_used']}")
    print(f"P-system phases: {len(basic_swing['p_system_classification'])}")
    
    # Test different club types
    print("\n2. Testing different club types...")
    for club_type in [ClubType.DRIVER, ClubType.MID_IRON, ClubType.WEDGE]:
        swing = create_realistic_swing(club_type=club_type)
        print(f"{club_type.value}: {swing['club_used']} - {len(swing['frames'])} frames")
    
    # Test fault injection
    print("\n3. Testing fault injection...")
    faulty_swing = create_realistic_swing(
        quality=SwingQuality.POOR,
        specific_faults=["insufficient_shoulder_turn", "cupped_wrist"]
    )
    print(f"Generated faulty swing: {faulty_swing['session_id']}")
    
    # Test streaming data
    print("\n4. Testing streaming data generation...")
    streaming_data = generate_streaming_session(duration_seconds=5.0)
    print(f"Generated {len(streaming_data)} streaming frames")
    
    # Test database entities
    print("\n5. Testing database entity creation...")
    mock_user = create_mock_user()
    print(f"Mock user: {mock_user['username']} ({mock_user['skill_level']})")
    
    mock_session = create_mock_session_data(mock_user["id"])
    print(f"Mock session: {mock_session['club_used']} - {mock_session['session_status']}")
    
    # Test KPI generation
    print("\n6. Testing KPI generation...")
    mock_kpis_p1 = create_mock_kpis("P1")
    mock_kpis_p4 = create_mock_kpis("P4")
    print(f"Generated {len(mock_kpis_p1)} P1 KPIs and {len(mock_kpis_p4)} P4 KPIs")
    
    print("\n=== Mock Data Factory Testing Complete ===")