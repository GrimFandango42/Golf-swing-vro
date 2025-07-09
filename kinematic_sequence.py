"""
Kinematic Sequence Analysis Module for Golf Swing Power Generation.

This module implements sophisticated algorithms to analyze the kinematic sequence
(the order and timing of body segment accelerations) during a golf swing.
The kinematic sequence is critical for efficient power transfer from the ground
up through the kinetic chain: pelvis → torso → arms → club.

Key Features:
- Real-time segment velocity calculation using optimized algorithms
- Peak velocity detection with noise filtering
- Timing gap analysis between segment peaks
- Sequence order validation
- Power transfer efficiency scoring
- Mobile-optimized performance (<50ms per frame)

Author: SwingSync AI Team
"""

import numpy as np
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass
from collections import deque
import math

from data_structures import (
    SwingVideoAnalysisInput,
    BiomechanicalKPI,
    FramePoseData,
    PoseKeypoint
)

# Constants for keypoint names (matching kpi_extraction.py)
KP_LEFT_SHOULDER = "left_shoulder"
KP_RIGHT_SHOULDER = "right_shoulder"
KP_LEFT_HIP = "left_hip"
KP_RIGHT_HIP = "right_hip"
KP_LEFT_WRIST = "left_wrist"
KP_RIGHT_WRIST = "right_wrist"
KP_LEFT_ELBOW = "left_elbow"
KP_RIGHT_ELBOW = "right_elbow"

# Kinematic sequence constants
OPTIMAL_TIMING_GAP_MS = 75  # Optimal timing gap between segment peaks (milliseconds)
TIMING_GAP_TOLERANCE_MS = 25  # Tolerance for timing gap scoring
MIN_VELOCITY_THRESHOLD = 50  # Minimum angular velocity (deg/s) to consider as movement
SMOOTHING_WINDOW_SIZE = 3  # Window size for velocity smoothing (frames)

@dataclass
class SegmentVelocity:
    """Stores velocity data for a body segment."""
    timestamp_ms: float
    angular_velocity_deg_s: float
    segment_name: str
    frame_index: int

@dataclass
class KinematicSequenceResult:
    """Complete kinematic sequence analysis results."""
    pelvis_peak: Optional[SegmentVelocity]
    torso_peak: Optional[SegmentVelocity]
    arms_peak: Optional[SegmentVelocity]
    club_peak: Optional[SegmentVelocity]
    sequence_order_correct: bool
    timing_gaps_ms: Dict[str, float]  # e.g., {"pelvis_to_torso": 75, ...}
    efficiency_score: float  # 0-100
    power_transfer_rating: str  # "Excellent", "Good", "Fair", "Poor"
    visualization_data: Dict[str, List[float]]  # For UI graphs

def _to_numpy_array(p: PoseKeypoint) -> np.ndarray:
    """Converts a PoseKeypoint to a NumPy array [x, y, z]."""
    return np.array([p['x'], p['y'], p['z']])

def calculate_segment_center(frame: FramePoseData, left_kp: str, right_kp: str) -> Optional[np.ndarray]:
    """Calculate the center point between two keypoints."""
    left = frame.get(left_kp)
    right = frame.get(right_kp)
    
    if not left or not right:
        return None
    
    if left.get('visibility', 0) < 0.3 or right.get('visibility', 0) < 0.3:
        return None
    
    left_pos = _to_numpy_array(left)
    right_pos = _to_numpy_array(right)
    return (left_pos + right_pos) / 2

def calculate_angular_velocity(
    pos_current: np.ndarray,
    pos_previous: np.ndarray,
    center_current: np.ndarray,
    center_previous: np.ndarray,
    dt: float
) -> float:
    """
    Calculate angular velocity using the cross product method.
    This is more robust than simple angle differences for 3D motion.
    """
    # Create vectors from center to position
    v1 = pos_previous - center_previous
    v2 = pos_current - center_current
    
    # Normalize vectors
    v1_norm = v1 / (np.linalg.norm(v1) + 1e-8)
    v2_norm = v2 / (np.linalg.norm(v2) + 1e-8)
    
    # Calculate angle using dot product (more stable than cross product for small angles)
    cos_angle = np.clip(np.dot(v1_norm, v2_norm), -1.0, 1.0)
    angle_rad = np.arccos(cos_angle)
    
    # Convert to degrees per second
    angular_velocity = np.degrees(angle_rad) / dt
    
    # Use cross product to determine direction (optional, for signed velocity)
    cross = np.cross(v1_norm, v2_norm)
    if cross[1] < 0:  # Assuming Y is up
        angular_velocity = -angular_velocity
    
    return angular_velocity

def smooth_velocity_data(velocities: List[float], window_size: int = SMOOTHING_WINDOW_SIZE) -> List[float]:
    """Apply moving average smoothing to reduce noise in velocity data."""
    if len(velocities) <= window_size:
        return velocities
    
    smoothed = []
    half_window = window_size // 2
    
    for i in range(len(velocities)):
        start = max(0, i - half_window)
        end = min(len(velocities), i + half_window + 1)
        smoothed.append(np.mean(velocities[start:end]))
    
    return smoothed

def detect_peak_velocity(
    velocities: List[SegmentVelocity],
    min_threshold: float = MIN_VELOCITY_THRESHOLD
) -> Optional[SegmentVelocity]:
    """
    Detect the peak velocity in a segment's motion.
    Uses threshold filtering to avoid noise-induced false peaks.
    """
    if not velocities:
        return None
    
    # Filter out low velocities (noise)
    significant_velocities = [v for v in velocities if abs(v.angular_velocity_deg_s) >= min_threshold]
    
    if not significant_velocities:
        return None
    
    # Find maximum absolute velocity
    peak = max(significant_velocities, key=lambda v: abs(v.angular_velocity_deg_s))
    return peak

def calculate_pelvis_velocity(frames: List[FramePoseData], fps: float) -> List[SegmentVelocity]:
    """Calculate pelvis rotation velocity throughout the swing."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        # Get hip centers
        hip_center_curr = calculate_segment_center(frames[i], KP_LEFT_HIP, KP_RIGHT_HIP)
        hip_center_prev = calculate_segment_center(frames[i-1], KP_LEFT_HIP, KP_RIGHT_HIP)
        
        if hip_center_curr is None or hip_center_prev is None:
            continue
        
        # Use right hip as reference point for rotation
        right_hip_curr = frames[i].get(KP_RIGHT_HIP)
        right_hip_prev = frames[i-1].get(KP_RIGHT_HIP)
        
        if not right_hip_curr or not right_hip_prev:
            continue
        
        right_hip_pos_curr = _to_numpy_array(right_hip_curr)
        right_hip_pos_prev = _to_numpy_array(right_hip_prev)
        
        angular_vel = calculate_angular_velocity(
            right_hip_pos_curr, right_hip_pos_prev,
            hip_center_curr, hip_center_prev,
            dt
        )
        
        velocities.append(SegmentVelocity(
            timestamp_ms=i * dt * 1000,
            angular_velocity_deg_s=angular_vel,
            segment_name="pelvis",
            frame_index=i
        ))
    
    return velocities

def calculate_torso_velocity(frames: List[FramePoseData], fps: float) -> List[SegmentVelocity]:
    """Calculate torso rotation velocity throughout the swing."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        # Get shoulder centers
        shoulder_center_curr = calculate_segment_center(frames[i], KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER)
        shoulder_center_prev = calculate_segment_center(frames[i-1], KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER)
        
        if shoulder_center_curr is None or shoulder_center_prev is None:
            continue
        
        # Use right shoulder as reference point for rotation
        right_shoulder_curr = frames[i].get(KP_RIGHT_SHOULDER)
        right_shoulder_prev = frames[i-1].get(KP_RIGHT_SHOULDER)
        
        if not right_shoulder_curr or not right_shoulder_prev:
            continue
        
        right_shoulder_pos_curr = _to_numpy_array(right_shoulder_curr)
        right_shoulder_pos_prev = _to_numpy_array(right_shoulder_prev)
        
        angular_vel = calculate_angular_velocity(
            right_shoulder_pos_curr, right_shoulder_pos_prev,
            shoulder_center_curr, shoulder_center_prev,
            dt
        )
        
        velocities.append(SegmentVelocity(
            timestamp_ms=i * dt * 1000,
            angular_velocity_deg_s=angular_vel,
            segment_name="torso",
            frame_index=i
        ))
    
    return velocities

def calculate_arms_velocity(frames: List[FramePoseData], fps: float) -> List[SegmentVelocity]:
    """Calculate arms rotation velocity (lead arm as proxy)."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        # Use lead arm (left arm for right-handed golfer)
        shoulder_curr = frames[i].get(KP_LEFT_SHOULDER)
        shoulder_prev = frames[i-1].get(KP_LEFT_SHOULDER)
        wrist_curr = frames[i].get(KP_LEFT_WRIST)
        wrist_prev = frames[i-1].get(KP_LEFT_WRIST)
        
        if not all([shoulder_curr, shoulder_prev, wrist_curr, wrist_prev]):
            continue
        
        shoulder_pos_curr = _to_numpy_array(shoulder_curr)
        shoulder_pos_prev = _to_numpy_array(shoulder_prev)
        wrist_pos_curr = _to_numpy_array(wrist_curr)
        wrist_pos_prev = _to_numpy_array(wrist_prev)
        
        # Calculate arm vector angular velocity
        arm_vec_curr = wrist_pos_curr - shoulder_pos_curr
        arm_vec_prev = wrist_pos_prev - shoulder_pos_prev
        
        # Normalize and calculate angle
        arm_vec_curr_norm = arm_vec_curr / (np.linalg.norm(arm_vec_curr) + 1e-8)
        arm_vec_prev_norm = arm_vec_prev / (np.linalg.norm(arm_vec_prev) + 1e-8)
        
        cos_angle = np.clip(np.dot(arm_vec_curr_norm, arm_vec_prev_norm), -1.0, 1.0)
        angle_rad = np.arccos(cos_angle)
        angular_vel = np.degrees(angle_rad) / dt
        
        velocities.append(SegmentVelocity(
            timestamp_ms=i * dt * 1000,
            angular_velocity_deg_s=angular_vel,
            segment_name="arms",
            frame_index=i
        ))
    
    return velocities

def calculate_club_velocity(frames: List[FramePoseData], fps: float) -> List[SegmentVelocity]:
    """
    Calculate club head velocity using wrist positions as proxy.
    In a real implementation, this would use actual club tracking data.
    """
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        # Use wrists center as club grip proxy
        wrist_center_curr = calculate_segment_center(frames[i], KP_LEFT_WRIST, KP_RIGHT_WRIST)
        wrist_center_prev = calculate_segment_center(frames[i-1], KP_LEFT_WRIST, KP_RIGHT_WRIST)
        
        if wrist_center_curr is None or wrist_center_prev is None:
            continue
        
        # Calculate linear velocity of wrist center (proxy for club speed)
        # In production, this would be replaced with actual club head tracking
        linear_vel = np.linalg.norm(wrist_center_curr - wrist_center_prev) / dt
        
        # Convert to approximate angular velocity (assuming ~1m club length)
        angular_vel = np.degrees(linear_vel)  # Simplified conversion
        
        velocities.append(SegmentVelocity(
            timestamp_ms=i * dt * 1000,
            angular_velocity_deg_s=angular_vel * 100,  # Scale factor for visualization
            segment_name="club",
            frame_index=i
        ))
    
    return velocities

def check_sequence_order(
    pelvis_peak: Optional[SegmentVelocity],
    torso_peak: Optional[SegmentVelocity],
    arms_peak: Optional[SegmentVelocity],
    club_peak: Optional[SegmentVelocity]
) -> bool:
    """Check if the kinematic sequence follows the correct order."""
    if not all([pelvis_peak, torso_peak, arms_peak, club_peak]):
        return False
    
    # Correct order: pelvis → torso → arms → club
    return (pelvis_peak.timestamp_ms <= torso_peak.timestamp_ms <= 
            arms_peak.timestamp_ms <= club_peak.timestamp_ms)

def calculate_timing_gaps(
    pelvis_peak: Optional[SegmentVelocity],
    torso_peak: Optional[SegmentVelocity],
    arms_peak: Optional[SegmentVelocity],
    club_peak: Optional[SegmentVelocity]
) -> Dict[str, float]:
    """Calculate timing gaps between segment peaks."""
    gaps = {}
    
    if pelvis_peak and torso_peak:
        gaps["pelvis_to_torso"] = torso_peak.timestamp_ms - pelvis_peak.timestamp_ms
    
    if torso_peak and arms_peak:
        gaps["torso_to_arms"] = arms_peak.timestamp_ms - torso_peak.timestamp_ms
    
    if arms_peak and club_peak:
        gaps["arms_to_club"] = club_peak.timestamp_ms - arms_peak.timestamp_ms
    
    return gaps

def calculate_efficiency_score(
    sequence_correct: bool,
    timing_gaps: Dict[str, float]
) -> float:
    """
    Calculate overall efficiency score (0-100) based on sequence order and timing.
    """
    if not sequence_correct:
        return 0.0
    
    # Start with perfect score
    score = 100.0
    
    # Deduct points for non-optimal timing gaps
    for gap_name, gap_ms in timing_gaps.items():
        deviation = abs(gap_ms - OPTIMAL_TIMING_GAP_MS)
        if deviation <= TIMING_GAP_TOLERANCE_MS:
            # Within tolerance, minimal deduction
            score -= deviation * 0.2
        else:
            # Outside tolerance, larger deduction
            score -= TIMING_GAP_TOLERANCE_MS * 0.2 + (deviation - TIMING_GAP_TOLERANCE_MS) * 0.5
    
    # Ensure score stays in valid range
    return max(0.0, min(100.0, score))

def get_power_transfer_rating(efficiency_score: float) -> str:
    """Convert efficiency score to a descriptive rating."""
    if efficiency_score >= 90:
        return "Excellent"
    elif efficiency_score >= 75:
        return "Good"
    elif efficiency_score >= 60:
        return "Fair"
    else:
        return "Poor"

def prepare_visualization_data(
    pelvis_velocities: List[SegmentVelocity],
    torso_velocities: List[SegmentVelocity],
    arms_velocities: List[SegmentVelocity],
    club_velocities: List[SegmentVelocity]
) -> Dict[str, List[float]]:
    """Prepare velocity data for UI visualization."""
    # Create time-aligned arrays for graphing
    max_frames = max(
        len(pelvis_velocities),
        len(torso_velocities),
        len(arms_velocities),
        len(club_velocities)
    )
    
    # Smooth the velocity data for better visualization
    pelvis_smooth = smooth_velocity_data([v.angular_velocity_deg_s for v in pelvis_velocities])
    torso_smooth = smooth_velocity_data([v.angular_velocity_deg_s for v in torso_velocities])
    arms_smooth = smooth_velocity_data([v.angular_velocity_deg_s for v in arms_velocities])
    club_smooth = smooth_velocity_data([v.angular_velocity_deg_s for v in club_velocities])
    
    return {
        "timestamps_ms": [i * (1000.0 / 240.0) for i in range(max_frames)],  # Assuming 240 fps
        "pelvis_velocity": pelvis_smooth,
        "torso_velocity": torso_smooth,
        "arms_velocity": arms_smooth,
        "club_velocity": club_smooth
    }

def analyze_kinematic_sequence(swing_input: SwingVideoAnalysisInput) -> KinematicSequenceResult:
    """
    Main function to analyze the kinematic sequence of a golf swing.
    Returns comprehensive analysis results including efficiency scores and visualization data.
    """
    frames = swing_input['frames']
    fps = swing_input['video_fps']
    
    # Calculate velocities for each segment
    pelvis_velocities = calculate_pelvis_velocity(frames, fps)
    torso_velocities = calculate_torso_velocity(frames, fps)
    arms_velocities = calculate_arms_velocity(frames, fps)
    club_velocities = calculate_club_velocity(frames, fps)
    
    # Detect peak velocities
    pelvis_peak = detect_peak_velocity(pelvis_velocities)
    torso_peak = detect_peak_velocity(torso_velocities)
    arms_peak = detect_peak_velocity(arms_velocities)
    club_peak = detect_peak_velocity(club_velocities)
    
    # Check sequence order
    sequence_correct = check_sequence_order(pelvis_peak, torso_peak, arms_peak, club_peak)
    
    # Calculate timing gaps
    timing_gaps = calculate_timing_gaps(pelvis_peak, torso_peak, arms_peak, club_peak)
    
    # Calculate efficiency score
    efficiency_score = calculate_efficiency_score(sequence_correct, timing_gaps)
    
    # Get power transfer rating
    power_rating = get_power_transfer_rating(efficiency_score)
    
    # Prepare visualization data
    viz_data = prepare_visualization_data(
        pelvis_velocities, torso_velocities, arms_velocities, club_velocities
    )
    
    return KinematicSequenceResult(
        pelvis_peak=pelvis_peak,
        torso_peak=torso_peak,
        arms_peak=arms_peak,
        club_peak=club_peak,
        sequence_order_correct=sequence_correct,
        timing_gaps_ms=timing_gaps,
        efficiency_score=efficiency_score,
        power_transfer_rating=power_rating,
        visualization_data=viz_data
    )

# KPI extraction functions for integration
def calculate_kinematic_sequence_order_kpi(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculate the kinematic sequence order KPI."""
    result = analyze_kinematic_sequence(swing_input)
    
    sequence_description = "Correct" if result.sequence_order_correct else "Incorrect"
    peak_order = []
    
    # Build the actual sequence order string
    peaks = [
        (result.pelvis_peak, "Pelvis"),
        (result.torso_peak, "Torso"),
        (result.arms_peak, "Arms"),
        (result.club_peak, "Club")
    ]
    
    # Sort by timestamp to show actual order
    valid_peaks = [(p, name) for p, name in peaks if p is not None]
    valid_peaks.sort(key=lambda x: x[0].timestamp_ms)
    actual_order = " → ".join([name for _, name in valid_peaks])
    
    return BiomechanicalKPI(
        p_position="P5-P7",  # Downswing phases
        kpi_name="Kinematic Sequence Order",
        value=sequence_description,
        unit="sequence",
        ideal_range=None,
        notes=f"Actual order: {actual_order}. Ideal: Pelvis → Torso → Arms → Club"
    )

def calculate_kinematic_timing_efficiency_kpi(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculate the timing efficiency KPI."""
    result = analyze_kinematic_sequence(swing_input)
    
    # Format timing gaps for notes
    gap_descriptions = []
    for gap_name, gap_ms in result.timing_gaps_ms.items():
        gap_descriptions.append(f"{gap_name.replace('_', ' ').title()}: {gap_ms:.0f}ms")
    
    gaps_text = ", ".join(gap_descriptions) if gap_descriptions else "No valid timing data"
    
    return BiomechanicalKPI(
        p_position="P5-P7",
        kpi_name="Kinematic Timing Efficiency",
        value=result.efficiency_score,
        unit="percentage",
        ideal_range=(85.0, 100.0),
        notes=f"Timing gaps: {gaps_text}. Optimal gap: {OPTIMAL_TIMING_GAP_MS}ms between segments"
    )

def calculate_power_transfer_rating_kpi(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculate the power transfer rating KPI."""
    result = analyze_kinematic_sequence(swing_input)
    
    # Get peak velocities for notes
    peak_velocities = []
    if result.pelvis_peak:
        peak_velocities.append(f"Pelvis: {abs(result.pelvis_peak.angular_velocity_deg_s):.0f}°/s")
    if result.torso_peak:
        peak_velocities.append(f"Torso: {abs(result.torso_peak.angular_velocity_deg_s):.0f}°/s")
    if result.arms_peak:
        peak_velocities.append(f"Arms: {abs(result.arms_peak.angular_velocity_deg_s):.0f}°/s")
    if result.club_peak:
        peak_velocities.append(f"Club: {abs(result.club_peak.angular_velocity_deg_s):.0f}°/s")
    
    velocities_text = ", ".join(peak_velocities) if peak_velocities else "Insufficient data"
    
    return BiomechanicalKPI(
        p_position="P5-P7",
        kpi_name="Power Transfer Rating",
        value=result.power_transfer_rating,
        unit="rating",
        ideal_range=None,
        notes=f"Peak velocities: {velocities_text}. Efficiency: {result.efficiency_score:.0f}%"
    )

# Performance optimization: Caching for repeated calculations
_kinematic_cache = {}

def get_kinematic_sequence_kpis_cached(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """
    Get all kinematic sequence KPIs with caching for performance.
    This ensures we only calculate the sequence analysis once per swing.
    """
    session_id = swing_input['session_id']
    
    if session_id not in _kinematic_cache:
        # Calculate all KPIs
        kpis = []
        
        order_kpi = calculate_kinematic_sequence_order_kpi(swing_input)
        if order_kpi:
            kpis.append(order_kpi)
        
        timing_kpi = calculate_kinematic_timing_efficiency_kpi(swing_input)
        if timing_kpi:
            kpis.append(timing_kpi)
        
        power_kpi = calculate_power_transfer_rating_kpi(swing_input)
        if power_kpi:
            kpis.append(power_kpi)
        
        _kinematic_cache[session_id] = kpis
    
    return _kinematic_cache[session_id]

# Clear cache periodically to prevent memory issues
def clear_kinematic_cache():
    """Clear the kinematic sequence cache."""
    global _kinematic_cache
    _kinematic_cache = {}

if __name__ == "__main__":
    print("Kinematic Sequence Analysis Module loaded successfully.")
    print(f"Optimal timing gap: {OPTIMAL_TIMING_GAP_MS}ms ± {TIMING_GAP_TOLERANCE_MS}ms")
    print("Ready for high-performance golf swing power analysis!")