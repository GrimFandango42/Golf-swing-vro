"""
Kinematic Sequence Analysis Module (Lite Version - No NumPy).

This module implements kinematic sequence analysis without external dependencies.
Optimized for mobile deployment with pure Python implementations.
"""

import math
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass

from data_structures import (
    SwingVideoAnalysisInput,
    BiomechanicalKPI,
    FramePoseData,
    PoseKeypoint
)

# Constants for keypoint names
KP_LEFT_SHOULDER = "left_shoulder"
KP_RIGHT_SHOULDER = "right_shoulder"
KP_LEFT_HIP = "left_hip"
KP_RIGHT_HIP = "right_hip"
KP_LEFT_WRIST = "left_wrist"
KP_RIGHT_WRIST = "right_wrist"
KP_LEFT_ELBOW = "left_elbow"
KP_RIGHT_ELBOW = "right_elbow"

# Kinematic sequence constants
OPTIMAL_TIMING_GAP_MS = 75
TIMING_GAP_TOLERANCE_MS = 25
MIN_VELOCITY_THRESHOLD = 50

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
    timing_gaps_ms: Dict[str, float]
    efficiency_score: float
    power_transfer_rating: str
    visualization_data: Dict[str, List[float]]

def vector_subtract(v1: Tuple[float, float, float], v2: Tuple[float, float, float]) -> Tuple[float, float, float]:
    """Subtract two 3D vectors."""
    return (v1[0] - v2[0], v1[1] - v2[1], v1[2] - v2[2])

def vector_add(v1: Tuple[float, float, float], v2: Tuple[float, float, float]) -> Tuple[float, float, float]:
    """Add two 3D vectors."""
    return (v1[0] + v2[0], v1[1] + v2[1], v1[2] + v2[2])

def vector_scale(v: Tuple[float, float, float], scale: float) -> Tuple[float, float, float]:
    """Scale a 3D vector."""
    return (v[0] * scale, v[1] * scale, v[2] * scale)

def vector_magnitude(v: Tuple[float, float, float]) -> float:
    """Calculate magnitude of a 3D vector."""
    return math.sqrt(v[0]**2 + v[1]**2 + v[2]**2)

def vector_normalize(v: Tuple[float, float, float]) -> Tuple[float, float, float]:
    """Normalize a 3D vector."""
    mag = vector_magnitude(v)
    if mag < 1e-8:
        return (0, 0, 0)
    return vector_scale(v, 1.0 / mag)

def vector_dot(v1: Tuple[float, float, float], v2: Tuple[float, float, float]) -> float:
    """Dot product of two 3D vectors."""
    return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]

def pose_keypoint_to_tuple(p: PoseKeypoint) -> Tuple[float, float, float]:
    """Convert PoseKeypoint to tuple."""
    return (p['x'], p['y'], p['z'])

def calculate_segment_center(frame: FramePoseData, left_kp: str, right_kp: str) -> Optional[Tuple[float, float, float]]:
    """Calculate the center point between two keypoints."""
    left = frame.get(left_kp)
    right = frame.get(right_kp)
    
    if not left or not right:
        return None
    
    if left.get('visibility', 0) < 0.3 or right.get('visibility', 0) < 0.3:
        return None
    
    left_pos = pose_keypoint_to_tuple(left)
    right_pos = pose_keypoint_to_tuple(right)
    return vector_scale(vector_add(left_pos, right_pos), 0.5)

def calculate_angular_velocity(
    pos_current: Tuple[float, float, float],
    pos_previous: Tuple[float, float, float],
    center_current: Tuple[float, float, float],
    center_previous: Tuple[float, float, float],
    dt: float
) -> float:
    """Calculate angular velocity between two positions."""
    # Create vectors from center to position
    v1 = vector_subtract(pos_previous, center_previous)
    v2 = vector_subtract(pos_current, center_current)
    
    # Normalize vectors
    v1_norm = vector_normalize(v1)
    v2_norm = vector_normalize(v2)
    
    # Calculate angle using dot product
    cos_angle = max(-1.0, min(1.0, vector_dot(v1_norm, v2_norm)))
    angle_rad = math.acos(cos_angle)
    
    # Convert to degrees per second
    angular_velocity = math.degrees(angle_rad) / dt
    
    return angular_velocity

def smooth_velocity_data(velocities: List[float], window_size: int = 3) -> List[float]:
    """Apply moving average smoothing."""
    if len(velocities) <= window_size:
        return velocities
    
    smoothed = []
    half_window = window_size // 2
    
    for i in range(len(velocities)):
        start = max(0, i - half_window)
        end = min(len(velocities), i + half_window + 1)
        avg = sum(velocities[start:end]) / (end - start)
        smoothed.append(avg)
    
    return smoothed

def detect_peak_velocity(velocities: List[SegmentVelocity]) -> Optional[SegmentVelocity]:
    """Detect peak velocity in segment motion."""
    if not velocities:
        return None
    
    # Filter out low velocities
    significant = [v for v in velocities if abs(v.angular_velocity_deg_s) >= MIN_VELOCITY_THRESHOLD]
    
    if not significant:
        return None
    
    # Find maximum
    return max(significant, key=lambda v: abs(v.angular_velocity_deg_s))

def calculate_pelvis_velocity(frames: List[FramePoseData], fps: float) -> List[SegmentVelocity]:
    """Calculate pelvis rotation velocity."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        hip_center_curr = calculate_segment_center(frames[i], KP_LEFT_HIP, KP_RIGHT_HIP)
        hip_center_prev = calculate_segment_center(frames[i-1], KP_LEFT_HIP, KP_RIGHT_HIP)
        
        if not hip_center_curr or not hip_center_prev:
            continue
        
        right_hip_curr = frames[i].get(KP_RIGHT_HIP)
        right_hip_prev = frames[i-1].get(KP_RIGHT_HIP)
        
        if not right_hip_curr or not right_hip_prev:
            continue
        
        right_hip_pos_curr = pose_keypoint_to_tuple(right_hip_curr)
        right_hip_pos_prev = pose_keypoint_to_tuple(right_hip_prev)
        
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
    """Calculate torso rotation velocity."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        shoulder_center_curr = calculate_segment_center(frames[i], KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER)
        shoulder_center_prev = calculate_segment_center(frames[i-1], KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER)
        
        if not shoulder_center_curr or not shoulder_center_prev:
            continue
        
        right_shoulder_curr = frames[i].get(KP_RIGHT_SHOULDER)
        right_shoulder_prev = frames[i-1].get(KP_RIGHT_SHOULDER)
        
        if not right_shoulder_curr or not right_shoulder_prev:
            continue
        
        right_shoulder_pos_curr = pose_keypoint_to_tuple(right_shoulder_curr)
        right_shoulder_pos_prev = pose_keypoint_to_tuple(right_shoulder_prev)
        
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
    """Calculate arms rotation velocity."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        shoulder_curr = frames[i].get(KP_LEFT_SHOULDER)
        shoulder_prev = frames[i-1].get(KP_LEFT_SHOULDER)
        wrist_curr = frames[i].get(KP_LEFT_WRIST)
        wrist_prev = frames[i-1].get(KP_LEFT_WRIST)
        
        if not all([shoulder_curr, shoulder_prev, wrist_curr, wrist_prev]):
            continue
        
        shoulder_pos_curr = pose_keypoint_to_tuple(shoulder_curr)
        shoulder_pos_prev = pose_keypoint_to_tuple(shoulder_prev)
        wrist_pos_curr = pose_keypoint_to_tuple(wrist_curr)
        wrist_pos_prev = pose_keypoint_to_tuple(wrist_prev)
        
        # Calculate arm vector angular velocity
        arm_vec_curr = vector_subtract(wrist_pos_curr, shoulder_pos_curr)
        arm_vec_prev = vector_subtract(wrist_pos_prev, shoulder_pos_prev)
        
        arm_vec_curr_norm = vector_normalize(arm_vec_curr)
        arm_vec_prev_norm = vector_normalize(arm_vec_prev)
        
        cos_angle = max(-1.0, min(1.0, vector_dot(arm_vec_curr_norm, arm_vec_prev_norm)))
        angle_rad = math.acos(cos_angle)
        angular_vel = math.degrees(angle_rad) / dt
        
        velocities.append(SegmentVelocity(
            timestamp_ms=i * dt * 1000,
            angular_velocity_deg_s=angular_vel,
            segment_name="arms",
            frame_index=i
        ))
    
    return velocities

def calculate_club_velocity(frames: List[FramePoseData], fps: float) -> List[SegmentVelocity]:
    """Calculate club velocity using wrist positions."""
    velocities = []
    dt = 1.0 / fps
    
    for i in range(1, len(frames)):
        wrist_center_curr = calculate_segment_center(frames[i], KP_LEFT_WRIST, KP_RIGHT_WRIST)
        wrist_center_prev = calculate_segment_center(frames[i-1], KP_LEFT_WRIST, KP_RIGHT_WRIST)
        
        if not wrist_center_curr or not wrist_center_prev:
            continue
        
        # Calculate linear velocity
        linear_vel = vector_magnitude(vector_subtract(wrist_center_curr, wrist_center_prev)) / dt
        
        # Convert to angular velocity approximation
        angular_vel = math.degrees(linear_vel) * 100  # Scale factor
        
        velocities.append(SegmentVelocity(
            timestamp_ms=i * dt * 1000,
            angular_velocity_deg_s=angular_vel,
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
    """Check if kinematic sequence follows correct order."""
    if not all([pelvis_peak, torso_peak, arms_peak, club_peak]):
        return False
    
    return (pelvis_peak.timestamp_ms <= torso_peak.timestamp_ms <= 
            arms_peak.timestamp_ms <= club_peak.timestamp_ms)

def calculate_timing_gaps(
    pelvis_peak: Optional[SegmentVelocity],
    torso_peak: Optional[SegmentVelocity],
    arms_peak: Optional[SegmentVelocity],
    club_peak: Optional[SegmentVelocity]
) -> Dict[str, float]:
    """Calculate timing gaps between peaks."""
    gaps = {}
    
    if pelvis_peak and torso_peak:
        gaps["pelvis_to_torso"] = torso_peak.timestamp_ms - pelvis_peak.timestamp_ms
    
    if torso_peak and arms_peak:
        gaps["torso_to_arms"] = arms_peak.timestamp_ms - torso_peak.timestamp_ms
    
    if arms_peak and club_peak:
        gaps["arms_to_club"] = club_peak.timestamp_ms - arms_peak.timestamp_ms
    
    return gaps

def calculate_efficiency_score(sequence_correct: bool, timing_gaps: Dict[str, float]) -> float:
    """Calculate efficiency score 0-100."""
    if not sequence_correct:
        return 0.0
    
    score = 100.0
    
    for gap_name, gap_ms in timing_gaps.items():
        deviation = abs(gap_ms - OPTIMAL_TIMING_GAP_MS)
        if deviation <= TIMING_GAP_TOLERANCE_MS:
            score -= deviation * 0.2
        else:
            score -= TIMING_GAP_TOLERANCE_MS * 0.2 + (deviation - TIMING_GAP_TOLERANCE_MS) * 0.5
    
    return max(0.0, min(100.0, score))

def get_power_transfer_rating(efficiency_score: float) -> str:
    """Convert efficiency score to rating."""
    if efficiency_score >= 90:
        return "Excellent"
    elif efficiency_score >= 75:
        return "Good"
    elif efficiency_score >= 60:
        return "Fair"
    else:
        return "Poor"

def analyze_kinematic_sequence(swing_input: SwingVideoAnalysisInput) -> KinematicSequenceResult:
    """Main kinematic sequence analysis function."""
    frames = swing_input['frames']
    fps = swing_input['video_fps']
    
    # Calculate velocities
    pelvis_velocities = calculate_pelvis_velocity(frames, fps)
    torso_velocities = calculate_torso_velocity(frames, fps)
    arms_velocities = calculate_arms_velocity(frames, fps)
    club_velocities = calculate_club_velocity(frames, fps)
    
    # Detect peaks
    pelvis_peak = detect_peak_velocity(pelvis_velocities)
    torso_peak = detect_peak_velocity(torso_velocities)
    arms_peak = detect_peak_velocity(arms_velocities)
    club_peak = detect_peak_velocity(club_velocities)
    
    # Check sequence
    sequence_correct = check_sequence_order(pelvis_peak, torso_peak, arms_peak, club_peak)
    
    # Calculate timing
    timing_gaps = calculate_timing_gaps(pelvis_peak, torso_peak, arms_peak, club_peak)
    
    # Calculate score
    efficiency_score = calculate_efficiency_score(sequence_correct, timing_gaps)
    
    # Get rating
    power_rating = get_power_transfer_rating(efficiency_score)
    
    # Prepare visualization data
    viz_data = {
        "timestamps_ms": [i * (1000.0 / fps) for i in range(len(frames))],
        "pelvis_velocity": smooth_velocity_data([v.angular_velocity_deg_s for v in pelvis_velocities]),
        "torso_velocity": smooth_velocity_data([v.angular_velocity_deg_s for v in torso_velocities]),
        "arms_velocity": smooth_velocity_data([v.angular_velocity_deg_s for v in arms_velocities]),
        "club_velocity": smooth_velocity_data([v.angular_velocity_deg_s for v in club_velocities])
    }
    
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

# KPI extraction functions
def calculate_kinematic_sequence_order_kpi(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculate sequence order KPI."""
    result = analyze_kinematic_sequence(swing_input)
    
    sequence_description = "Correct" if result.sequence_order_correct else "Incorrect"
    peak_order = []
    
    peaks = [
        (result.pelvis_peak, "Pelvis"),
        (result.torso_peak, "Torso"),
        (result.arms_peak, "Arms"),
        (result.club_peak, "Club")
    ]
    
    valid_peaks = [(p, name) for p, name in peaks if p is not None]
    valid_peaks.sort(key=lambda x: x[0].timestamp_ms)
    actual_order = " → ".join([name for _, name in valid_peaks])
    
    return BiomechanicalKPI(
        p_position="P5-P7",
        kpi_name="Kinematic Sequence Order",
        value=sequence_description,
        unit="sequence",
        ideal_range=None,
        notes=f"Actual order: {actual_order}. Ideal: Pelvis → Torso → Arms → Club"
    )

def calculate_kinematic_timing_efficiency_kpi(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculate timing efficiency KPI."""
    result = analyze_kinematic_sequence(swing_input)
    
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
    """Calculate power transfer rating KPI."""
    result = analyze_kinematic_sequence(swing_input)
    
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

# Cache for performance
_kinematic_cache = {}

def get_kinematic_sequence_kpis_cached(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Get all kinematic KPIs with caching."""
    session_id = swing_input['session_id']
    
    if session_id not in _kinematic_cache:
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

def clear_kinematic_cache():
    """Clear cache."""
    global _kinematic_cache
    _kinematic_cache = {}

if __name__ == "__main__":
    print("Kinematic Sequence Analysis Module (Lite) loaded successfully.")
    print("Ready for high-performance golf swing power analysis!")