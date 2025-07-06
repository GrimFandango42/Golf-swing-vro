"""
Module for Biomechanical Key Performance Indicator (KPI) Extraction.

This module is responsible for calculating various biomechanical metrics (KPIs)
from the raw 3D pose data captured during a golf swing. These KPIs quantify
aspects of the golfer's posture, alignment, rotation, and other movement
characteristics at different phases of the swing (P-System).

The functions within this module typically take `SwingVideoAnalysisInput`
(which includes frame-by-frame pose data and P-System phase timings) and
return `BiomechanicalKPI` objects.

Key Assumptions:
- Coordinate System: Specific calculations may assume a certain orientation of
  coordinate axes (e.g., Y-axis is vertical). This should be consistent with the
  output of the pose estimation model.
- Keypoint Names: The functions expect specific keypoint names (e.g., 'left_shoulder',
  'right_hip') as defined in this module (e.g., `KP_LEFT_SHOULDER`). These must
  match the names provided by the pose estimation data.
- P-System Phases: KPIs are often calculated based on the average pose within a
  specific P-System phase (e.g., P1 for address, P4 for top of backswing).
"""
import math
import numpy as np
from typing import List, Optional, Dict, Tuple

# Assuming data_structures.py is in the same directory or accessible in PYTHONPATH
from data_structures import (
    PoseKeypoint,
    FramePoseData,
    PSystemPhase,
    SwingVideoAnalysisInput,
    BiomechanicalKPI
)

# --- Constants ---
# Define standard keypoint names that are expected from the pose estimation model.
# These align with common models like MediaPipe Pose.
KP_NOSE = "nose"
KP_LEFT_SHOULDER = "left_shoulder"
KP_RIGHT_SHOULDER = "right_shoulder"
KP_LEFT_ELBOW = "left_elbow"
KP_RIGHT_ELBOW = "right_elbow"
KP_LEFT_WRIST = "left_wrist"
KP_RIGHT_WRIST = "right_wrist"
KP_LEFT_HIP = "left_hip"
KP_RIGHT_HIP = "right_hip"
KP_LEFT_KNEE = "left_knee"
KP_RIGHT_KNEE = "right_knee"
KP_LEFT_ANKLE = "left_ankle"
KP_RIGHT_ANKLE = "right_ankle"
KP_LEFT_HEEL = "left_heel"
KP_RIGHT_HEEL = "right_heel"
KP_LEFT_FOOT_INDEX = "left_foot_index"
KP_RIGHT_FOOT_INDEX = "right_foot_index"

# A standard list of keypoints that might be expected.
# This can be tailored to the specific pose estimation model used.
EXPECTED_KEYPOINTS = [
    KP_NOSE, KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER, KP_LEFT_ELBOW, KP_RIGHT_ELBOW,
    KP_LEFT_WRIST, KP_RIGHT_WRIST, KP_LEFT_HIP, KP_RIGHT_HIP, KP_LEFT_KNEE,
    KP_RIGHT_KNEE, KP_LEFT_ANKLE, KP_RIGHT_ANKLE,
    # Add more if the model provides them, e.g., eyes, ears, heels, foot_index
]

# --- Helper Functions ---

def _to_numpy_array(p: PoseKeypoint) -> np.ndarray:
    """Converts a PoseKeypoint to a NumPy array [x, y, z]."""
    return np.array([p['x'], p['y'], p['z']])

def get_keypoint(frame_data: FramePoseData, keypoint_name: str) -> Optional[np.ndarray]:
    """Safely retrieves a keypoint as a NumPy array from frame data."""
    kp = frame_data.get(keypoint_name)
    if kp:
        # Basic check for visibility/confidence if available and meaningful
        if kp.get('visibility', 1.0) > 0.1: # Threshold can be tuned
            return _to_numpy_array(kp)
    return None

def get_phase_by_name(swing_input: SwingVideoAnalysisInput, phase_name: str) -> Optional[PSystemPhase]:
    """Finds a PSystemPhase by its name."""
    for phase in swing_input['p_system_classification']:
        if phase['phase_name'] == phase_name:
            return phase
    return None

def get_average_keypoint_position_for_phase(
    swing_input: SwingVideoAnalysisInput,
    phase_name: str,
    keypoint_name: str
) -> Optional[np.ndarray]:
    """
    Calculates the average 3D position of a keypoint over the frames of a given P-System phase.
    Returns None if the phase or keypoint is not found, or if no valid keypoints are available.
    """
    phase = get_phase_by_name(swing_input, phase_name)
    if not phase:
        print(f"Warning: Phase '{phase_name}' not found in p_system_classification.")
        return None

    positions = []
    for i in range(phase['start_frame_index'], phase['end_frame_index'] + 1):
        if i < len(swing_input['frames']):
            frame_data = swing_input['frames'][i]
            kp = get_keypoint(frame_data, keypoint_name)
            if kp is not None:
                positions.append(kp)
        else:
            print(f"Warning: Frame index {i} out of bounds for phase '{phase_name}'.")
            break # Avoid further out-of-bounds access

    if not positions:
        # print(f"Warning: No valid keypoints found for '{keypoint_name}' in phase '{phase_name}'.")
        return None

    return np.mean(positions, axis=0)


def calculate_angle_3d(p1: np.ndarray, p2: np.ndarray, p3: np.ndarray, degrees: bool = True) -> float:
    """
    Calculates the angle (in degrees or radians) between three 3D points (p1, p2, p3).
    The angle is formed by the vectors p2->p1 and p2->p3 at vertex p2.
    """
    v1 = p1 - p2
    v2 = p3 - p2

    dot_product = np.dot(v1, v2)
    norm_v1 = np.linalg.norm(v1)
    norm_v2 = np.linalg.norm(v2)

    if norm_v1 == 0 or norm_v2 == 0:
        # print("Warning: Zero length vector in angle calculation.")
        return 0.0 # Or handle as an error/None

    # Ensure the argument to arccos is within [-1, 1] to avoid NaN
    cos_angle = np.clip(dot_product / (norm_v1 * norm_v2), -1.0, 1.0)
    angle_rad = math.acos(cos_angle)

    return math.degrees(angle_rad) if degrees else angle_rad

def get_midpoint(p1: np.ndarray, p2: np.ndarray) -> np.ndarray:
    """Calculates the midpoint between two 3D points."""
    return (p1 + p2) / 2.0

def get_vector_projection_on_plane(vector: np.ndarray, plane_normal: np.ndarray) -> np.ndarray:
    """Projects a vector onto a plane defined by its normal."""
    plane_normal = plane_normal / np.linalg.norm(plane_normal) # Normalize
    projection_onto_normal = np.dot(vector, plane_normal) * plane_normal
    projected_vector = vector - projection_onto_normal
    return projected_vector

# --- KPI Calculation Functions ---

def calculate_hip_hinge_angle_p1(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """
    Calculates hip hinge (forward tilt) at P1 (Address).
    This is often measured as the angle of the torso relative to the vertical,
    or the angle between the torso line (mid_shoulder to mid_hip) and leg line (mid_hip to mid_ankle).
    For simplicity, we'll calculate the angle: mid_shoulder - mid_hip - mid_knee (spine to femur angle).
    A more upright posture would have a larger angle here.
    Alternatively, spine angle from vertical. Let's try spine angle from vertical.
    """
    p_position = "P1"
    kpi_name = "Hip Hinge Angle (Spine from Vertical)"

    # Get average keypoints for P1
    left_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    right_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)

    if not all([left_shoulder is not None, right_shoulder is not None, left_hip is not None, right_hip is not None]):
        # print(f"Debug: Missing keypoints for {kpi_name}")
        return None

    mid_shoulder = get_midpoint(left_shoulder, right_shoulder)
    mid_hip = get_midpoint(left_hip, right_hip)

    torso_vector = mid_shoulder - mid_hip

    # Define a vertical vector. Assuming Y is up, X is right, Z is towards camera/target.
    # This assumption is CRITICAL and depends on the pose estimation coordinate system.
    # If Z is up, change this to [0, 0, 1]
    vertical_vector_world = np.array([0, 1, 0]) # Assuming Y is up in world space.

    # Project torso_vector onto XY plane (sagittal plane if golfer faces Z or X axis)
    # For spine angle in sagittal view, we'd typically use a side-on camera.
    # Here, we calculate angle of torso vector with the global Y-axis.
    # A value of 0 means torso is vertical. 90 means horizontal.
    # Golfer's forward tilt means torso_vector will have a negative Y component if Y is up.
    # Angle with (0,1,0) for vertical. If torso is (0,-1,0) angle is 180. If (1,-1,0) angle is 135.
    # We want the angle of forward lean from vertical.
    # If torso vector is (x,y,z), angle from vertical (0,1,0) is acos(y / norm(torso_vector)).
    # A more intuitive measure: angle between torso and vertical vector, subtracted from 90 if we want tilt from horizontal.
    # Or, angle between torso_vector projected on sagittal plane and vertical.

    # Let's use the angle between the torso vector and the vertical_vector.
    # If perfectly upright, torso_vector might be [0, -length, 0] (if origin is above hips).
    # Or [0, length, 0] if mid_shoulder is above mid_hip in Y. Let's assume Y is 'up'.
    # mid_shoulder usually has larger Y than mid_hip. So torso_vector = [xs-xh, ys-yh, zs-zh] with ys-yh > 0.

    angle_with_vertical = calculate_angle_3d(mid_shoulder + vertical_vector_world, mid_shoulder, mid_hip)

    # This angle_with_vertical will be small if torso is aligned with vertical (e.g. standing straight up).
    # A typical golf posture has forward lean. So, angle_with_vertical will be > 0.
    # E.g. if leaning forward 30 degrees, this angle should be 30.
    # Let's verify: mid_hip = (0,0,0), mid_shoulder=(0, cos(30), -sin(30)) assuming lean in Z.
    # torso_vector = (0, cos(30), -sin(30)). vertical = (0,1,0).
    # angle = acos( (torso_vector . vertical) / (norm(torso_vector) * norm(vertical)) )
    #       = acos( cos(30) / 1*1 ) = 30 degrees. This seems correct.

    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_with_vertical,
        unit="degrees",
        ideal_range=(30.0, 45.0), # Example range from requirements
        notes="Angle of the torso (mid-shoulder to mid-hip) with the global vertical axis (Y-up assumed)."
    )


def calculate_knee_flex_p1(swing_input: SwingVideoAnalysisInput, leg_side: str) -> Optional[BiomechanicalKPI]:
    """Calculates knee flexion at P1 (Address) for the specified leg ('left' or 'right')."""
    p_position = "P1"
    kpi_name = f"{leg_side.capitalize()} Knee Flexion Angle"

    hip_kp_name = KP_LEFT_HIP if leg_side == "left" else KP_RIGHT_HIP
    knee_kp_name = KP_LEFT_KNEE if leg_side == "left" else KP_RIGHT_KNEE
    ankle_kp_name = KP_LEFT_ANKLE if leg_side == "left" else KP_RIGHT_ANKLE

    hip = get_average_keypoint_position_for_phase(swing_input, p_position, hip_kp_name)
    knee = get_average_keypoint_position_for_phase(swing_input, p_position, knee_kp_name)
    ankle = get_average_keypoint_position_for_phase(swing_input, p_position, ankle_kp_name)

    if not all([hip is not None, knee is not None, ankle is not None]):
        # print(f"Debug: Missing keypoints for {kpi_name}")
        return None

    # Angle is at the knee joint: hip-knee-ankle
    angle = calculate_angle_3d(hip, knee, ankle)
    # A straight leg is 180 degrees. Flexion reduces this angle.
    # Often, flexion is reported as deviation from straight: 180 - angle.
    flexion_value = 180.0 - angle

    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=flexion_value,
        unit="degrees",
        ideal_range=(15.0, 25.0), # Typical athletic stance knee flexion
        notes=f"Flexion of {leg_side} knee (180 - angle at hip-knee-ankle)."
    )

def estimate_weight_distribution_p1(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """
    Estimates weight distribution (lead vs trail foot) at P1 (Address).
    Simplified: uses midpoint of hips as proxy for Center of Mass (CoM).
    Compares CoM's X-position (assuming X is side-to-side) relative to ankle midpoints.
    This is a ROUGH estimate. True CoM is complex.
    """
    p_position = "P1"
    kpi_name = "Estimated Weight Distribution (Lead Foot %)"

    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)

    if not all([left_hip is not None, right_hip is not None, left_ankle is not None, right_ankle is not None]):
        # print(f"Debug: Missing keypoints for {kpi_name}")
        return None

    mid_hip = get_midpoint(left_hip, right_hip)

    # Assuming X is the side-to-side axis. Golfer faces Z or -Z.
    # Left side of golfer is typically negative X, right side is positive X if origin is center.
    # Or, depends on camera view. Let's assume standard coordinate system from pose estimator.
    # For a right-handed golfer, left_ankle.x should be less than right_ankle.x if X increases to the right.

    # Ensure lead/trail is correctly assigned (assuming right-handed golfer for now)
    # TODO: Need user handedness. Assuming right-handed: left is lead, right is trail.
    lead_ankle_x = left_ankle[0]
    trail_ankle_x = right_ankle[0]

    # Handle case where trail_ankle_x might be less than lead_ankle_x (e.g. if golfer is left-handed or facing away)
    # For simplicity, let's use min and max X for ankles to define stance width.
    min_ankle_x = min(lead_ankle_x, trail_ankle_x)
    max_ankle_x = max(lead_ankle_x, trail_ankle_x)

    if (max_ankle_x - min_ankle_x) < 1e-6: # Avoid division by zero if ankles are at same x
        return None

    # CoM proxy X position
    com_x = mid_hip[0]

    # Normalize CoM position within the stance width (0 to 1)
    # 0 = CoM over min_ankle_x, 1 = CoM over max_ankle_x
    relative_pos = (com_x - min_ankle_x) / (max_ankle_x - min_ankle_x)

    # If lead_ankle_x corresponds to min_ankle_x (typical for RH golfer):
    # weight_on_lead_foot_percentage = (1.0 - relative_pos) * 100
    # If lead_ankle_x corresponds to max_ankle_x (e.g. if X axis is flipped):
    # weight_on_lead_foot_percentage = relative_pos * 100

    # Assuming RH golfer, lead foot is left foot. If left_ankle_x < right_ankle_x,
    # then higher relative_pos means more weight on trail foot.
    # So, % on lead = (1 - relative_pos) * 100
    # If left_ankle_x > right_ankle_x (e.g. left-handed golfer from same view, or different coord system)
    # then higher relative_pos means more weight on lead foot.
    # So, % on lead = relative_pos * 100

    if lead_ankle_x < trail_ankle_x: # Typical RH setup
        weight_on_lead_foot_percentage = (1.0 - np.clip(relative_pos, 0, 1)) * 100
    else: # Potentially LH setup or flipped X axis
        weight_on_lead_foot_percentage = np.clip(relative_pos, 0, 1) * 100

    ideal_min, ideal_max = (45.0, 55.0) # For irons, as per requirements
    if swing_input['club_used'].lower() == "driver":
        ideal_min, ideal_max = (35.0, 45.0) # Lead foot % for driver (so 55-65% on trail)
        # The requirement says "40/60" towards trail for driver, meaning 40% on lead.

    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=weight_on_lead_foot_percentage,
        unit="%",
        ideal_range=(ideal_min, ideal_max),
        notes="Estimated % of weight on lead foot, using hip midpoint X relative to ankle X positions. Assumes RH golfer if not specified."
    )


def calculate_shoulder_rotation_p4(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """
    Calculates shoulder rotation at P4 (Top of Backswing).
    Defined as the angle of the shoulder line (left_shoulder to right_shoulder)
    in the XY plane (top-down view) relative to its orientation at P1 (Address).
    Assumes Z is the up-axis for top-down view projection, or Y if camera is side-on.
    Let's assume Y is vertical, so we project onto XZ plane.
    """
    p_position = "P4"
    kpi_name = "Shoulder Rotation at P4 (relative to Address)"

    # Get P1 shoulder line for reference
    ls_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_SHOULDER)
    rs_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_SHOULDER)

    # Get P4 shoulder line
    ls_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    rs_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)

    if not all([ls_p1 is not None, rs_p1 is not None, ls_p4 is not None, rs_p4 is not None]):
        # print(f"Debug: Missing shoulder keypoints for {kpi_name}")
        return None

    # Project onto XZ plane (assuming Y is vertical)
    shoulder_line_p1_vec = np.array([rs_p1[0] - ls_p1[0], rs_p1[2] - ls_p1[2]]) # X and Z components
    shoulder_line_p4_vec = np.array([rs_p4[0] - ls_p4[0], rs_p4[2] - ls_p4[2]]) # X and Z components

    if np.linalg.norm(shoulder_line_p1_vec) == 0 or np.linalg.norm(shoulder_line_p4_vec) == 0:
        return None

    # Angle between these 2D vectors
    unit_vec_p1 = shoulder_line_p1_vec / np.linalg.norm(shoulder_line_p1_vec)
    unit_vec_p4 = shoulder_line_p4_vec / np.linalg.norm(shoulder_line_p4_vec)

    dot_product = np.dot(unit_vec_p1, unit_vec_p4)
    angle_rad = math.acos(np.clip(dot_product, -1.0, 1.0))
    angle_deg = math.degrees(angle_rad)

    # Determine direction of rotation (e.g. clockwise for RH golfer)
    # Cross product in 2D: v1.x*v2.y - v1.y*v2.x (here, y is our Z component)
    cross_product_z = unit_vec_p1[0]*unit_vec_p4[1] - unit_vec_p1[1]*unit_vec_p4[0]
    # For a RH golfer, backswing rotation is clockwise from top view.
    # If P1 shoulder line is along X-axis (e.g. [1,0]) and P4 is along Y-axis (e.g. [0,1] after 90 deg turn),
    # cross_product_z = 1*1 - 0*0 = 1. Positive for counter-clockwise by this formula.
    # Need to define "target line" or assume initial alignment.
    # If target line is along positive Z axis, P1 shoulders are along X. RH backswing turns shoulders clockwise.
    # P4 shoulder line might point towards negative X (for 90 deg turn if starting parallel to target).
    # This part needs careful definition of coordinate system and golfer setup.
    # For now, just return the magnitude.

    # The requirement is "around 90 degrees".
    # A common definition: if shoulders start square (vector along X axis, e.g. from (0,0) to (1,0) on XZ plane)
    # and rotate to be perpendicular (vector along Z axis, e.g. from (0,0) to (0,1) on XZ plane for 90 deg CW),
    # the angle change is 90.

    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_deg,
        unit="degrees",
        ideal_range=(80.0, 100.0), # e.g. ~90 degrees
        notes="Angle of shoulder line in XZ plane at P4 relative to P1. Assumes Y is vertical. Direction not determined."
    )


# --- Main function to extract all KPIs ---
def extract_all_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """
    Extracts all defined biomechanical KPIs from the swing analysis input.
    """
    all_kpis: List[BiomechanicalKPI] = []

    # P1 KPIs
    hip_hinge_p1 = calculate_hip_hinge_angle_p1(swing_input)
    if hip_hinge_p1: all_kpis.append(hip_hinge_p1)

    knee_flex_left_p1 = calculate_knee_flex_p1(swing_input, "left")
    if knee_flex_left_p1: all_kpis.append(knee_flex_left_p1)

    knee_flex_right_p1 = calculate_knee_flex_p1(swing_input, "right")
    if knee_flex_right_p1: all_kpis.append(knee_flex_right_p1)

    weight_dist_p1 = estimate_weight_distribution_p1(swing_input)
    if weight_dist_p1: all_kpis.append(weight_dist_p1)

    # P4 KPIs
    shoulder_rot_p4 = calculate_shoulder_rotation_p4(swing_input)
    if shoulder_rot_p4: all_kpis.append(shoulder_rot_p4)

    # TODO: Implement more KPIs as per requirements:
    # P1: Alignment, Ball Position (requires ball detection or manual input)
    # P2: Takeaway sync, Club Position/Face (requires club tracking or assumptions)
    # P4: Hip Rotation, Lead Wrist Position (complex, needs careful definition), Weight Transfer
    # P7: Body Position, Hand Position/Shaft Lean, Weight Transfer, Clubface Alignment (very hard from video alone)
    # P10: Balance, Full Rotation, Hand/Club Position

    print(f"Extracted {len(all_kpis)} KPIs.")
    return all_kpis

if __name__ == '__main__':
    # Create dummy SwingVideoAnalysisInput for testing

    # Create some dummy keypoints
    def _make_kp(x,y,z): return {"x":x, "y":y, "z":z, "visibility":1.0}

    # Frame 0-10: P1 Address (example keypoints)
    p1_frames = []
    for _ in range(11): # 11 frames for P1
        frame_data: FramePoseData = {
            KP_LEFT_SHOULDER: _make_kp(-0.2, 1.4, 0), KP_RIGHT_SHOULDER: _make_kp(0.2, 1.4, 0),
            KP_LEFT_HIP: _make_kp(-0.15, 0.9, 0), KP_RIGHT_HIP: _make_kp(0.15, 0.9, 0),
            KP_LEFT_KNEE: _make_kp(-0.18, 0.5, 0), KP_RIGHT_KNEE: _make_kp(0.18, 0.5, 0),
            KP_LEFT_ANKLE: _make_kp(-0.2, 0.1, 0), KP_RIGHT_ANKLE: _make_kp(0.2, 0.1, 0),
            KP_LEFT_WRIST: _make_kp(-0.3, 1.0, 0.2), KP_RIGHT_WRIST: _make_kp(0.3, 1.0, 0.2)
        }
        p1_frames.append(frame_data)

    # Frame 11-20: P4 Top of Backswing (example keypoints - shoulders rotated)
    # Simulating a 90-degree clockwise rotation around Y-axis for shoulders
    # Initial shoulder line: (-0.2, 1.4, 0) to (0.2, 1.4, 0) -> vector (0.4, 0, 0)
    # Rotated shoulder line (approx): e.g. left shoulder moves back, right shoulder moves forward
    # If center of rotation is (0, 1.4, 0)
    # Left shoulder from (-0.2,0) to (0, -0.2) in XZ plane. Right shoulder from (0.2,0) to (0,0.2)
    p4_frames = []
    for i in range(10): # 10 frames for P4
        # Simple linear interpolation for rotation for variety
        rot_factor = i / 9.0

        # P1 left shoulder (-0.2, 0), P1 right shoulder (0.2, 0) in XZ plane relative to shoulder center
        # P4 left shoulder (0, -0.2), P4 right shoulder (0, 0.2) for 90 deg CW rot
        ls_x_p4 = -0.2 * (1-rot_factor) + 0 * rot_factor
        ls_z_p4 = 0 * (1-rot_factor) -0.2 * rot_factor
        rs_x_p4 = 0.2 * (1-rot_factor) + 0 * rot_factor
        rs_z_p4 = 0 * (1-rot_factor) + 0.2 * rot_factor

        frame_data_p4: FramePoseData = {
            KP_LEFT_SHOULDER: _make_kp(ls_x_p4, 1.4, ls_z_p4), KP_RIGHT_SHOULDER: _make_kp(rs_x_p4, 1.4, rs_z_p4),
            KP_LEFT_HIP: _make_kp(-0.1, 0.9, -0.1), KP_RIGHT_HIP: _make_kp(0.1, 0.9, -0.1), # Hips also rotate a bit
            KP_LEFT_KNEE: _make_kp(-0.18, 0.5, 0), KP_RIGHT_KNEE: _make_kp(0.18, 0.5, 0), # Knees might change less
            KP_LEFT_ANKLE: _make_kp(-0.2, 0.1, 0), KP_RIGHT_ANKLE: _make_kp(0.2, 0.1, 0),
            KP_LEFT_WRIST: _make_kp(-0.1, 1.5, -0.3), KP_RIGHT_WRIST: _make_kp(0.2, 1.3, 0.3) # Wrists at top
        }
        p4_frames.append(frame_data_p4)

    sample_swing_input: SwingVideoAnalysisInput = {
        "session_id": "test_session_001",
        "user_id": "test_user",
        "club_used": "7-Iron",
        "frames": p1_frames + p4_frames, # Total 21 frames
        "p_system_classification": [
            {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
            # Skipping P2, P3 for this test
            {"phase_name": "P4", "start_frame_index": 11, "end_frame_index": 20}
        ],
        "video_fps": 60.0
    }

    print("--- Testing KPI Extraction ---")
    extracted_kpis = extract_all_kpis(sample_swing_input)
    for kpi in extracted_kpis:
        print(f"KPI: {kpi['kpi_name']} ({kpi['p_position']})")
        print(f"  Value: {kpi['value']:.2f} {kpi['unit']}")
        if kpi['ideal_range']:
            print(f"  Ideal: {kpi['ideal_range']}")
        if kpi['notes']:
            print(f"  Notes: {kpi['notes']}")

    print("\n--- Example: Hip Hinge P1 ---")
    hh_p1 = calculate_hip_hinge_angle_p1(sample_swing_input)
    if hh_p1: print(hh_p1)

    print("\n--- Example: Left Knee Flex P1 ---")
    lkf_p1 = calculate_knee_flex_p1(sample_swing_input, "left")
    if lkf_p1: print(lkf_p1)

    print("\n--- Example: Weight Dist P1 ---")
    wd_p1 = estimate_weight_distribution_p1(sample_swing_input)
    if wd_p1: print(wd_p1) # Expected: 50% with symmetric setup

    print("\n--- Example: Shoulder Rotation P4 ---")
    sr_p4 = calculate_shoulder_rotation_p4(sample_swing_input)
    if sr_p4: print(sr_p4) # Expected: close to 90 with the dummy data

    # Test with a P4 that has different shoulder rotation
    p4_frames_less_rotation = []
    for i in range(10):
        rot_factor = (i / 9.0) * 0.5 # Only 45 degree rotation
        ls_x_p4 = -0.2 * math.cos(math.radians(45*rot_factor)) + 0 * math.sin(math.radians(45*rot_factor))
        ls_z_p4 = -0.2 * math.sin(math.radians(45*rot_factor)) + 0 * math.cos(math.radians(45*rot_factor))
        rs_x_p4 = 0.2 * math.cos(math.radians(45*rot_factor)) - 0 * math.sin(math.radians(45*rot_factor))
        rs_z_p4 = 0.2 * math.sin(math.radians(45*rot_factor)) - 0 * math.cos(math.radians(45*rot_factor))
        # This rotation logic is a bit off for the desired effect, direct interpolation was simpler.
        # Reverting to simpler interpolation for test.
        # Target: left shoulder x=-0.141, z=-0.141; right shoulder x=0.141, z=0.141 (approx for 45 deg)
        ls_x_p4_target = -0.2 * math.cos(math.radians(45)) # -0.141
        ls_z_p4_target = -0.2 * math.sin(math.radians(45)) # -0.141
        rs_x_p4_target = 0.2 * math.cos(math.radians(45))  # 0.141
        rs_z_p4_target = 0.2 * math.sin(math.radians(45))  # 0.141

        ls_x_p4 = -0.2 * (1-rot_factor) + ls_x_p4_target * rot_factor
        ls_z_p4 = 0 * (1-rot_factor) + ls_z_p4_target * rot_factor
        rs_x_p4 = 0.2 * (1-rot_factor) + rs_x_p4_target * rot_factor
        rs_z_p4 = 0 * (1-rot_factor) + rs_z_p4_target * rot_factor

        frame_data_p4_less_rot: FramePoseData = {
            KP_LEFT_SHOULDER: _make_kp(ls_x_p4, 1.4, ls_z_p4), KP_RIGHT_SHOULDER: _make_kp(rs_x_p4, 1.4, rs_z_p4),
            KP_LEFT_HIP: _make_kp(-0.1, 0.9, -0.05), KP_RIGHT_HIP: _make_kp(0.1, 0.9, -0.05),
            KP_LEFT_KNEE: _make_kp(-0.18, 0.5, 0), KP_RIGHT_KNEE: _make_kp(0.18, 0.5, 0),
            KP_LEFT_ANKLE: _make_kp(-0.2, 0.1, 0), KP_RIGHT_ANKLE: _make_kp(0.2, 0.1, 0),
            KP_LEFT_WRIST: _make_kp(-0.2, 1.45, -0.2), KP_RIGHT_WRIST: _make_kp(0.15, 1.35, 0.25)
        }
        p4_frames_less_rotation.append(frame_data_p4_less_rot)

    sample_swing_input_less_rotation = sample_swing_input.copy()
    sample_swing_input_less_rotation["frames"] = p1_frames + p4_frames_less_rotation

    print("\n--- Example: Shoulder Rotation P4 (Less Rotation) ---")
    sr_p4_less = calculate_shoulder_rotation_p4(sample_swing_input_less_rotation)
    if sr_p4_less: print(sr_p4_less) # Expected: close to 45

"""
