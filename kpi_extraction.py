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


# --- P2 KPI Functions ---

def extract_p2_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P2 (Takeaway) position."""
    kpis = []
    
    # Movement synchronization - check arm and shoulder movement
    sync_kpi = calculate_takeaway_synchronization_p2(swing_input)
    if sync_kpi: kpis.append(sync_kpi)
    
    # Club face angle proxy using wrist positions
    face_angle_kpi = calculate_club_face_angle_proxy_p2(swing_input)
    if face_angle_kpi: kpis.append(face_angle_kpi)
    
    # Tempo - rate of change from P1 to P2
    tempo_kpi = calculate_takeaway_tempo_p2(swing_input)
    if tempo_kpi: kpis.append(tempo_kpi)
    
    return kpis

def calculate_takeaway_synchronization_p2(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates synchronization between arm and shoulder movement during takeaway."""
    p_position = "P2"
    kpi_name = "Takeaway Synchronization Score"
    
    # Get P1 and P2 positions for comparison
    ls_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_SHOULDER)
    rs_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_SHOULDER)
    lw_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_WRIST)
    rw_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_WRIST)
    
    ls_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    rs_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    lw_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    rw_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_WRIST)
    
    if not all([ls_p1 is not None, rs_p1 is not None, lw_p1 is not None, rw_p1 is not None,
                ls_p2 is not None, rs_p2 is not None, lw_p2 is not None, rw_p2 is not None]):
        return None
    
    # Calculate shoulder center movement
    shoulder_center_p1 = get_midpoint(ls_p1, rs_p1)
    shoulder_center_p2 = get_midpoint(ls_p2, rs_p2)
    shoulder_movement = np.linalg.norm(shoulder_center_p2 - shoulder_center_p1)
    
    # Calculate wrist center movement
    wrist_center_p1 = get_midpoint(lw_p1, rw_p1)
    wrist_center_p2 = get_midpoint(lw_p2, rw_p2)
    wrist_movement = np.linalg.norm(wrist_center_p2 - wrist_center_p1)
    
    # Synchronization ratio - ideally arms and body move together
    if shoulder_movement > 0:
        sync_ratio = wrist_movement / shoulder_movement
        # Convert to percentage score (100% = perfect 1:1 ratio)
        sync_score = max(0, 100 - abs(sync_ratio - 1.0) * 100)
    else:
        sync_score = 0
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=sync_score,
        unit="%",
        ideal_range=(80.0, 100.0),
        notes="Synchronization between arm and shoulder movement during takeaway. 100% indicates perfect one-piece takeaway."
    )

def calculate_club_face_angle_proxy_p2(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Estimates club face angle using wrist positions as proxy."""
    p_position = "P2"
    kpi_name = "Club Face Angle Proxy"
    
    lw_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    rw_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_WRIST)
    
    if not all([lw_p2 is not None, rw_p2 is not None]):
        return None
    
    # Calculate wrist line angle in XZ plane (assuming Y is vertical)
    wrist_vector = rw_p2 - lw_p2
    wrist_angle_xz = math.degrees(math.atan2(wrist_vector[2], wrist_vector[0]))
    
    # Normalize to 0-90 range as proxy for face angle
    face_angle_proxy = abs(wrist_angle_xz) % 90
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=face_angle_proxy,
        unit="degrees",
        ideal_range=(10.0, 30.0),
        notes="Proxy for club face angle using wrist line orientation. Not actual face angle."
    )

def calculate_takeaway_tempo_p2(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates tempo of takeaway from P1 to P2."""
    p_position = "P2"
    kpi_name = "Takeaway Tempo"
    
    # Get phase timings
    p1_phase = get_phase_by_name(swing_input, "P1")
    p2_phase = get_phase_by_name(swing_input, p_position)
    
    if not p1_phase or not p2_phase:
        return None
    
    # Calculate time difference
    frame_diff = p2_phase['start_frame_index'] - p1_phase['end_frame_index']
    time_diff = frame_diff / swing_input['video_fps']
    
    # Get wrist movement for tempo calculation
    lw_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_WRIST)
    lw_p2 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    
    if not all([lw_p1 is not None, lw_p2 is not None]) or time_diff <= 0:
        return None
    
    movement_distance = np.linalg.norm(lw_p2 - lw_p1)
    tempo = movement_distance / time_diff if time_diff > 0 else 0
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=tempo,
        unit="m/s",
        ideal_range=(0.3, 0.8),
        notes="Speed of wrist movement during takeaway phase."
    )


# --- P3 KPI Functions ---

def extract_p3_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P3 (Halfway Back) position."""
    kpis = []
    
    # Lead arm position relative to ground
    arm_position_kpi = calculate_lead_arm_position_p3(swing_input)
    if arm_position_kpi: kpis.append(arm_position_kpi)
    
    # Weight transfer analysis
    weight_transfer_kpi = calculate_weight_transfer_p3(swing_input)
    if weight_transfer_kpi: kpis.append(weight_transfer_kpi)
    
    # Spine angle maintenance
    spine_angle_kpi = calculate_spine_angle_p3(swing_input)
    if spine_angle_kpi: kpis.append(spine_angle_kpi)
    
    return kpis

def calculate_lead_arm_position_p3(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates lead arm angle relative to horizontal at P3."""
    p_position = "P3"
    kpi_name = "Lead Arm Angle to Horizontal"
    
    # Assuming left arm is lead for right-handed golfer
    ls_p3 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    le_p3 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ELBOW)
    
    if not all([ls_p3 is not None, le_p3 is not None]):
        return None
    
    # Calculate arm vector
    arm_vector = le_p3 - ls_p3
    
    # Project onto XZ plane (horizontal plane assuming Y is vertical)
    arm_horizontal = np.array([arm_vector[0], 0, arm_vector[2]])
    
    # Calculate angle with horizontal
    if np.linalg.norm(arm_horizontal) > 0:
        angle_with_horizontal = calculate_angle_3d(
            ls_p3 + arm_horizontal, ls_p3, le_p3
        )
    else:
        angle_with_horizontal = 90.0
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_with_horizontal,
        unit="degrees",
        ideal_range=(85.0, 95.0),
        notes="Angle of lead arm relative to horizontal plane. Ideal is parallel to ground (~90 degrees)."
    )

def calculate_weight_transfer_p3(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates weight transfer from P1 to P3."""
    p_position = "P3"
    kpi_name = "Weight Transfer to Trail Foot"
    
    # Compare weight distribution between P1 and P3
    weight_p1 = estimate_weight_distribution_p1(swing_input)
    
    # Calculate P3 weight distribution using same method
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)
    
    if not all([left_hip is not None, right_hip is not None, left_ankle is not None, right_ankle is not None]) or not weight_p1:
        return None
    
    mid_hip = get_midpoint(left_hip, right_hip)
    lead_ankle_x = left_ankle[0]
    trail_ankle_x = right_ankle[0]
    
    min_ankle_x = min(lead_ankle_x, trail_ankle_x)
    max_ankle_x = max(lead_ankle_x, trail_ankle_x)
    
    if (max_ankle_x - min_ankle_x) < 1e-6:
        return None
    
    com_x = mid_hip[0]
    relative_pos = (com_x - min_ankle_x) / (max_ankle_x - min_ankle_x)
    
    if lead_ankle_x < trail_ankle_x:
        weight_on_lead_p3 = (1.0 - np.clip(relative_pos, 0, 1)) * 100
    else:
        weight_on_lead_p3 = np.clip(relative_pos, 0, 1) * 100
    
    # Calculate transfer amount
    weight_transfer = weight_p1['value'] - weight_on_lead_p3
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=weight_transfer,
        unit="%",
        ideal_range=(10.0, 25.0),
        notes="Amount of weight transferred to trail foot from address position. Positive values indicate transfer to trail side."
    )

def calculate_spine_angle_p3(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates spine angle maintenance at P3."""
    p_position = "P3"
    kpi_name = "Spine Angle Maintenance"
    
    # Compare spine angle between P1 and P3
    spine_p1_kpi = calculate_hip_hinge_angle_p1(swing_input)
    
    # Calculate P3 spine angle using same method
    left_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    right_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([left_shoulder is not None, right_shoulder is not None, left_hip is not None, right_hip is not None]) or not spine_p1_kpi:
        return None
    
    mid_shoulder = get_midpoint(left_shoulder, right_shoulder)
    mid_hip = get_midpoint(left_hip, right_hip)
    torso_vector = mid_shoulder - mid_hip
    vertical_vector_world = np.array([0, 1, 0])
    
    angle_with_vertical = calculate_angle_3d(mid_shoulder + vertical_vector_world, mid_shoulder, mid_hip)
    
    # Calculate difference from P1
    spine_angle_change = abs(angle_with_vertical - spine_p1_kpi['value'])
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=spine_angle_change,
        unit="degrees",
        ideal_range=(0.0, 5.0),
        notes="Change in spine angle from address position. Lower values indicate better spine angle maintenance."
    )


# --- P5 KPI Functions ---

def extract_p5_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P5 (Early Downswing) position."""
    kpis = []
    
    # Hip rotation analysis
    hip_rotation_kpi = calculate_hip_rotation_p5(swing_input)
    if hip_rotation_kpi: kpis.append(hip_rotation_kpi)
    
    # Weight shift to lead side
    weight_shift_kpi = calculate_weight_shift_p5(swing_input)
    if weight_shift_kpi: kpis.append(weight_shift_kpi)
    
    # Club path analysis
    club_path_kpi = calculate_club_path_p5(swing_input)
    if club_path_kpi: kpis.append(club_path_kpi)
    
    return kpis

def calculate_hip_rotation_p5(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates hip rotation at P5 relative to P4."""
    p_position = "P5"
    kpi_name = "Hip Rotation from P4"
    
    # Get hip positions at P4 and P5
    lh_p4 = get_average_keypoint_position_for_phase(swing_input, "P4", KP_LEFT_HIP)
    rh_p4 = get_average_keypoint_position_for_phase(swing_input, "P4", KP_RIGHT_HIP)
    lh_p5 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    rh_p5 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([lh_p4 is not None, rh_p4 is not None, lh_p5 is not None, rh_p5 is not None]):
        return None
    
    # Calculate hip line vectors in XZ plane
    hip_line_p4 = np.array([rh_p4[0] - lh_p4[0], rh_p4[2] - lh_p4[2]])
    hip_line_p5 = np.array([rh_p5[0] - lh_p5[0], rh_p5[2] - lh_p5[2]])
    
    if np.linalg.norm(hip_line_p4) == 0 or np.linalg.norm(hip_line_p5) == 0:
        return None
    
    # Calculate angle between hip lines
    unit_vec_p4 = hip_line_p4 / np.linalg.norm(hip_line_p4)
    unit_vec_p5 = hip_line_p5 / np.linalg.norm(hip_line_p5)
    
    dot_product = np.dot(unit_vec_p4, unit_vec_p5)
    angle_rad = math.acos(np.clip(dot_product, -1.0, 1.0))
    angle_deg = math.degrees(angle_rad)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_deg,
        unit="degrees",
        ideal_range=(15.0, 30.0),
        notes="Hip rotation from top of backswing to early downswing. Indicates proper sequencing."
    )

def calculate_weight_shift_p5(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates weight shift toward lead foot at P5."""
    p_position = "P5"
    kpi_name = "Weight Shift to Lead Foot"
    
    # Calculate weight distribution at P5
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)
    
    if not all([left_hip is not None, right_hip is not None, left_ankle is not None, right_ankle is not None]):
        return None
    
    mid_hip = get_midpoint(left_hip, right_hip)
    lead_ankle_x = left_ankle[0]
    trail_ankle_x = right_ankle[0]
    
    min_ankle_x = min(lead_ankle_x, trail_ankle_x)
    max_ankle_x = max(lead_ankle_x, trail_ankle_x)
    
    if (max_ankle_x - min_ankle_x) < 1e-6:
        return None
    
    com_x = mid_hip[0]
    relative_pos = (com_x - min_ankle_x) / (max_ankle_x - min_ankle_x)
    
    if lead_ankle_x < trail_ankle_x:
        weight_on_lead_p5 = (1.0 - np.clip(relative_pos, 0, 1)) * 100
    else:
        weight_on_lead_p5 = np.clip(relative_pos, 0, 1) * 100
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=weight_on_lead_p5,
        unit="%",
        ideal_range=(60.0, 75.0),
        notes="Percentage of weight on lead foot during early downswing transition."
    )

def calculate_club_path_p5(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Estimates club path using wrist positions."""
    p_position = "P5"
    kpi_name = "Club Path Proxy"
    
    # Get wrist positions from P4 to P5
    lw_p4 = get_average_keypoint_position_for_phase(swing_input, "P4", KP_LEFT_WRIST)
    rw_p4 = get_average_keypoint_position_for_phase(swing_input, "P4", KP_RIGHT_WRIST)
    lw_p5 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    rw_p5 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_WRIST)
    
    if not all([lw_p4 is not None, rw_p4 is not None, lw_p5 is not None, rw_p5 is not None]):
        return None
    
    # Calculate club head proxy movement
    club_center_p4 = get_midpoint(lw_p4, rw_p4)
    club_center_p5 = get_midpoint(lw_p5, rw_p5)
    club_movement = club_center_p5 - club_center_p4
    
    # Calculate path angle in XZ plane
    path_angle = math.degrees(math.atan2(club_movement[2], club_movement[0]))
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=abs(path_angle),
        unit="degrees",
        ideal_range=(30.0, 60.0),
        notes="Proxy for club path using wrist center movement. Not actual club path."
    )


# --- P6 KPI Functions ---

def extract_p6_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P6 (Pre-Impact) position."""
    kpis = []
    
    # Lag angle analysis
    lag_angle_kpi = calculate_lag_angle_p6(swing_input)
    if lag_angle_kpi: kpis.append(lag_angle_kpi)
    
    # Body position at pre-impact
    body_position_kpi = calculate_body_position_p6(swing_input)
    if body_position_kpi: kpis.append(body_position_kpi)
    
    # Weight transfer completion
    weight_transfer_kpi = calculate_weight_transfer_completion_p6(swing_input)
    if weight_transfer_kpi: kpis.append(weight_transfer_kpi)
    
    return kpis

def calculate_lag_angle_p6(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates lag angle between lead arm and club shaft proxy."""
    p_position = "P6"
    kpi_name = "Lag Angle (Arm-Shaft)"
    
    # Using left arm and wrist for right-handed golfer
    ls_p6 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    le_p6 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ELBOW)
    lw_p6 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    
    if not all([ls_p6 is not None, le_p6 is not None, lw_p6 is not None]):
        return None
    
    # Calculate arm vector (shoulder to wrist)
    arm_vector = lw_p6 - ls_p6
    # Calculate forearm vector (elbow to wrist) as proxy for shaft
    forearm_vector = lw_p6 - le_p6
    
    # Calculate angle between vectors
    lag_angle = calculate_angle_3d(ls_p6, lw_p6, le_p6)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=lag_angle,
        unit="degrees",
        ideal_range=(90.0, 120.0),
        notes="Angle between lead arm and forearm as proxy for shaft lag. Larger angles indicate more lag."
    )

def calculate_body_position_p6(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates body position relative to setup at P6."""
    p_position = "P6"
    kpi_name = "Body Position Forward Shift"
    
    # Compare hip position between P1 and P6
    lh_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_HIP)
    rh_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_HIP)
    lh_p6 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    rh_p6 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([lh_p1 is not None, rh_p1 is not None, lh_p6 is not None, rh_p6 is not None]):
        return None
    
    hip_center_p1 = get_midpoint(lh_p1, rh_p1)
    hip_center_p6 = get_midpoint(lh_p6, rh_p6)
    
    # Calculate forward movement (assuming Z is target direction)
    forward_shift = hip_center_p6[2] - hip_center_p1[2]
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=forward_shift * 100,  # Convert to cm
        unit="cm",
        ideal_range=(2.0, 8.0),
        notes="Forward shift of body position from address. Positive values indicate movement toward target."
    )

def calculate_weight_transfer_completion_p6(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates weight transfer completion at P6."""
    p_position = "P6"
    kpi_name = "Weight Transfer Completion"
    
    # Calculate weight distribution at P6
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)
    
    if not all([left_hip is not None, right_hip is not None, left_ankle is not None, right_ankle is not None]):
        return None
    
    mid_hip = get_midpoint(left_hip, right_hip)
    lead_ankle_x = left_ankle[0]
    trail_ankle_x = right_ankle[0]
    
    min_ankle_x = min(lead_ankle_x, trail_ankle_x)
    max_ankle_x = max(lead_ankle_x, trail_ankle_x)
    
    if (max_ankle_x - min_ankle_x) < 1e-6:
        return None
    
    com_x = mid_hip[0]
    relative_pos = (com_x - min_ankle_x) / (max_ankle_x - min_ankle_x)
    
    if lead_ankle_x < trail_ankle_x:
        weight_on_lead_p6 = (1.0 - np.clip(relative_pos, 0, 1)) * 100
    else:
        weight_on_lead_p6 = np.clip(relative_pos, 0, 1) * 100
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=weight_on_lead_p6,
        unit="%",
        ideal_range=(80.0, 90.0),
        notes="Percentage of weight on lead foot just before impact."
    )


# --- P7 KPI Functions ---

def extract_p7_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P7 (Impact) position."""
    kpis = []
    
    # Impact position analysis
    impact_position_kpi = calculate_impact_position_p7(swing_input)
    if impact_position_kpi: kpis.append(impact_position_kpi)
    
    # Shaft lean at impact
    shaft_lean_kpi = calculate_shaft_lean_p7(swing_input)
    if shaft_lean_kpi: kpis.append(shaft_lean_kpi)
    
    # Hip rotation at impact
    hip_rotation_kpi = calculate_hip_rotation_impact_p7(swing_input)
    if hip_rotation_kpi: kpis.append(hip_rotation_kpi)
    
    return kpis

def calculate_impact_position_p7(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates impact position relative to setup."""
    p_position = "P7"
    kpi_name = "Impact Position Accuracy"
    
    # Compare wrist position between P1 and P7
    lw_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_WRIST)
    rw_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_WRIST)
    lw_p7 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    rw_p7 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_WRIST)
    
    if not all([lw_p1 is not None, rw_p1 is not None, lw_p7 is not None, rw_p7 is not None]):
        return None
    
    wrist_center_p1 = get_midpoint(lw_p1, rw_p1)
    wrist_center_p7 = get_midpoint(lw_p7, rw_p7)
    
    # Calculate deviation from original position
    position_deviation = np.linalg.norm(wrist_center_p7 - wrist_center_p1)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=position_deviation * 100,  # Convert to cm
        unit="cm",
        ideal_range=(0.0, 5.0),
        notes="Deviation of impact position from address position. Lower values indicate better consistency."
    )

def calculate_shaft_lean_p7(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates shaft lean at impact using wrist and elbow positions."""
    p_position = "P7"
    kpi_name = "Shaft Lean at Impact"
    
    # Using left arm for right-handed golfer
    le_p7 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ELBOW)
    lw_p7 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    
    if not all([le_p7 is not None, lw_p7 is not None]):
        return None
    
    # Calculate shaft vector (elbow to wrist as proxy)
    shaft_vector = lw_p7 - le_p7
    
    # Calculate angle with vertical
    vertical_vector = np.array([0, 1, 0])
    shaft_lean_angle = calculate_angle_3d(le_p7 + vertical_vector, le_p7, lw_p7)
    
    # Convert to forward lean (positive values)
    if shaft_vector[2] > 0:  # Forward lean (toward target)
        lean_value = 90 - shaft_lean_angle
    else:  # Backward lean
        lean_value = -(90 - shaft_lean_angle)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=lean_value,
        unit="degrees",
        ideal_range=(2.0, 8.0),
        notes="Forward shaft lean at impact. Positive values indicate forward lean toward target."
    )

def calculate_hip_rotation_impact_p7(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates hip rotation at impact relative to address."""
    p_position = "P7"
    kpi_name = "Hip Rotation at Impact"
    
    # Get hip positions at P1 and P7
    lh_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_HIP)
    rh_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_HIP)
    lh_p7 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    rh_p7 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([lh_p1 is not None, rh_p1 is not None, lh_p7 is not None, rh_p7 is not None]):
        return None
    
    # Calculate hip line vectors in XZ plane
    hip_line_p1 = np.array([rh_p1[0] - lh_p1[0], rh_p1[2] - lh_p1[2]])
    hip_line_p7 = np.array([rh_p7[0] - lh_p7[0], rh_p7[2] - lh_p7[2]])
    
    if np.linalg.norm(hip_line_p1) == 0 or np.linalg.norm(hip_line_p7) == 0:
        return None
    
    # Calculate angle between hip lines
    unit_vec_p1 = hip_line_p1 / np.linalg.norm(hip_line_p1)
    unit_vec_p7 = hip_line_p7 / np.linalg.norm(hip_line_p7)
    
    dot_product = np.dot(unit_vec_p1, unit_vec_p7)
    angle_rad = math.acos(np.clip(dot_product, -1.0, 1.0))
    angle_deg = math.degrees(angle_rad)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_deg,
        unit="degrees",
        ideal_range=(30.0, 45.0),
        notes="Hip rotation from address to impact position."
    )


# --- P8 KPI Functions ---

def extract_p8_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P8 (Release) position."""
    kpis = []
    
    # Release extension analysis
    release_extension_kpi = calculate_release_extension_p8(swing_input)
    if release_extension_kpi: kpis.append(release_extension_kpi)
    
    # Follow-through analysis
    follow_through_kpi = calculate_follow_through_p8(swing_input)
    if follow_through_kpi: kpis.append(follow_through_kpi)
    
    # Extension quality
    extension_quality_kpi = calculate_extension_quality_p8(swing_input)
    if extension_quality_kpi: kpis.append(extension_quality_kpi)
    
    return kpis

def calculate_release_extension_p8(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates arm extension in release phase."""
    p_position = "P8"
    kpi_name = "Release Extension Angle"
    
    # Calculate arm extension angle
    ls_p8 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    le_p8 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ELBOW)
    lw_p8 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    
    if not all([ls_p8 is not None, le_p8 is not None, lw_p8 is not None]):
        return None
    
    # Calculate elbow angle (shoulder-elbow-wrist)
    elbow_angle = calculate_angle_3d(ls_p8, le_p8, lw_p8)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=elbow_angle,
        unit="degrees",
        ideal_range=(160.0, 180.0),
        notes="Elbow extension angle in release phase. Higher values indicate better extension."
    )

def calculate_follow_through_p8(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates follow-through momentum."""
    p_position = "P8"
    kpi_name = "Follow-Through Momentum"
    
    # Compare wrist positions between P7 and P8
    lw_p7 = get_average_keypoint_position_for_phase(swing_input, "P7", KP_LEFT_WRIST)
    lw_p8 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    
    if not all([lw_p7 is not None, lw_p8 is not None]):
        return None
    
    # Calculate distance moved in follow-through
    follow_through_distance = np.linalg.norm(lw_p8 - lw_p7)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=follow_through_distance * 100,  # Convert to cm
        unit="cm",
        ideal_range=(20.0, 40.0),
        notes="Distance moved by wrists from impact to release. Indicates follow-through momentum."
    )

def calculate_extension_quality_p8(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates quality of extension through impact zone."""
    p_position = "P8"
    kpi_name = "Extension Quality Score"
    
    # Compare arm length between P7 and P8
    ls_p7 = get_average_keypoint_position_for_phase(swing_input, "P7", KP_LEFT_SHOULDER)
    lw_p7 = get_average_keypoint_position_for_phase(swing_input, "P7", KP_LEFT_WRIST)
    ls_p8 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    lw_p8 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_WRIST)
    
    if not all([ls_p7 is not None, lw_p7 is not None, ls_p8 is not None, lw_p8 is not None]):
        return None
    
    arm_length_p7 = np.linalg.norm(lw_p7 - ls_p7)
    arm_length_p8 = np.linalg.norm(lw_p8 - ls_p8)
    
    # Calculate extension improvement
    extension_improvement = ((arm_length_p8 - arm_length_p7) / arm_length_p7) * 100
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=extension_improvement,
        unit="%",
        ideal_range=(5.0, 15.0),
        notes="Percentage improvement in arm extension from impact to release."
    )


# --- P9 KPI Functions ---

def extract_p9_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P9 (Finish) position."""
    kpis = []
    
    # Lead arm position at finish
    finish_arm_position_kpi = calculate_finish_arm_position_p9(swing_input)
    if finish_arm_position_kpi: kpis.append(finish_arm_position_kpi)
    
    # Balance assessment
    balance_kpi = calculate_balance_p9(swing_input)
    if balance_kpi: kpis.append(balance_kpi)
    
    # Rotation completion
    rotation_completion_kpi = calculate_rotation_completion_p9(swing_input)
    if rotation_completion_kpi: kpis.append(rotation_completion_kpi)
    
    return kpis

def calculate_finish_arm_position_p9(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates lead arm position at finish."""
    p_position = "P9"
    kpi_name = "Lead Arm Finish Position"
    
    ls_p9 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    le_p9 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ELBOW)
    
    if not all([ls_p9 is not None, le_p9 is not None]):
        return None
    
    # Calculate arm angle relative to horizontal
    arm_vector = le_p9 - ls_p9
    horizontal_vector = np.array([1, 0, 0])  # Assuming X is horizontal
    
    # Project arm vector onto horizontal plane
    arm_horizontal = np.array([arm_vector[0], 0, arm_vector[2]])
    
    if np.linalg.norm(arm_horizontal) > 0:
        angle_with_horizontal = calculate_angle_3d(
            ls_p9 + arm_horizontal, ls_p9, le_p9
        )
    else:
        angle_with_horizontal = 90.0
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_with_horizontal,
        unit="degrees",
        ideal_range=(85.0, 95.0),
        notes="Lead arm angle relative to horizontal at finish. Should be approximately parallel to ground."
    )

def calculate_balance_p9(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates balance at finish position."""
    p_position = "P9"
    kpi_name = "Finish Balance Score"
    
    # Calculate center of mass position relative to support base
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)
    
    if not all([left_hip is not None, right_hip is not None, left_ankle is not None, right_ankle is not None]):
        return None
    
    hip_center = get_midpoint(left_hip, right_hip)
    ankle_center = get_midpoint(left_ankle, right_ankle)
    
    # Calculate lateral deviation of COM from support base
    lateral_deviation = abs(hip_center[0] - ankle_center[0])
    
    # Convert to balance score (100 = perfect balance)
    balance_score = max(0, 100 - lateral_deviation * 1000)  # Scale factor may need adjustment
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=balance_score,
        unit="%",
        ideal_range=(85.0, 100.0),
        notes="Balance score at finish position. Higher values indicate better balance."
    )

def calculate_rotation_completion_p9(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates rotation completion at finish."""
    p_position = "P9"
    kpi_name = "Rotation Completion"
    
    # Compare shoulder rotation between P1 and P9
    ls_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_SHOULDER)
    rs_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_SHOULDER)
    ls_p9 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    rs_p9 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    
    if not all([ls_p1 is not None, rs_p1 is not None, ls_p9 is not None, rs_p9 is not None]):
        return None
    
    # Calculate shoulder line vectors in XZ plane
    shoulder_line_p1 = np.array([rs_p1[0] - ls_p1[0], rs_p1[2] - ls_p1[2]])
    shoulder_line_p9 = np.array([rs_p9[0] - ls_p9[0], rs_p9[2] - ls_p9[2]])
    
    if np.linalg.norm(shoulder_line_p1) == 0 or np.linalg.norm(shoulder_line_p9) == 0:
        return None
    
    # Calculate total rotation angle
    unit_vec_p1 = shoulder_line_p1 / np.linalg.norm(shoulder_line_p1)
    unit_vec_p9 = shoulder_line_p9 / np.linalg.norm(shoulder_line_p9)
    
    dot_product = np.dot(unit_vec_p1, unit_vec_p9)
    angle_rad = math.acos(np.clip(dot_product, -1.0, 1.0))
    angle_deg = math.degrees(angle_rad)
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=angle_deg,
        unit="degrees",
        ideal_range=(120.0, 150.0),
        notes="Total shoulder rotation from address to finish position."
    )


# --- P10 KPI Functions ---

def extract_p10_kpis(swing_input: SwingVideoAnalysisInput) -> List[BiomechanicalKPI]:
    """Extracts KPIs for P10 (End of Swing) position."""
    kpis = []
    
    # Final balance assessment
    final_balance_kpi = calculate_final_balance_p10(swing_input)
    if final_balance_kpi: kpis.append(final_balance_kpi)
    
    # Weight distribution at end
    final_weight_distribution_kpi = calculate_final_weight_distribution_p10(swing_input)
    if final_weight_distribution_kpi: kpis.append(final_weight_distribution_kpi)
    
    # Stability assessment
    stability_kpi = calculate_stability_p10(swing_input)
    if stability_kpi: kpis.append(stability_kpi)
    
    return kpis

def calculate_final_balance_p10(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates final balance at end of swing."""
    p_position = "P10"
    kpi_name = "Final Balance Score"
    
    # Assess balance using multiple body segments
    left_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    right_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)
    
    if not all([left_shoulder is not None, right_shoulder is not None, left_hip is not None, 
                right_hip is not None, left_ankle is not None, right_ankle is not None]):
        return None
    
    # Calculate center points
    shoulder_center = get_midpoint(left_shoulder, right_shoulder)
    hip_center = get_midpoint(left_hip, right_hip)
    ankle_center = get_midpoint(left_ankle, right_ankle)
    
    # Calculate alignment of body segments (should be vertically aligned for good balance)
    shoulder_hip_deviation = abs(shoulder_center[0] - hip_center[0])
    hip_ankle_deviation = abs(hip_center[0] - ankle_center[0])
    
    # Calculate overall balance score
    total_deviation = shoulder_hip_deviation + hip_ankle_deviation
    balance_score = max(0, 100 - total_deviation * 500)  # Scale factor may need adjustment
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=balance_score,
        unit="%",
        ideal_range=(90.0, 100.0),
        notes="Final balance score based on body segment alignment. Higher values indicate better balance."
    )

def calculate_final_weight_distribution_p10(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates final weight distribution."""
    p_position = "P10"
    kpi_name = "Final Weight Distribution"
    
    # Calculate weight distribution at P10
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    left_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_ANKLE)
    right_ankle = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_ANKLE)
    
    if not all([left_hip is not None, right_hip is not None, left_ankle is not None, right_ankle is not None]):
        return None
    
    mid_hip = get_midpoint(left_hip, right_hip)
    lead_ankle_x = left_ankle[0]
    trail_ankle_x = right_ankle[0]
    
    min_ankle_x = min(lead_ankle_x, trail_ankle_x)
    max_ankle_x = max(lead_ankle_x, trail_ankle_x)
    
    if (max_ankle_x - min_ankle_x) < 1e-6:
        return None
    
    com_x = mid_hip[0]
    relative_pos = (com_x - min_ankle_x) / (max_ankle_x - min_ankle_x)
    
    if lead_ankle_x < trail_ankle_x:
        weight_on_lead_p10 = (1.0 - np.clip(relative_pos, 0, 1)) * 100
    else:
        weight_on_lead_p10 = np.clip(relative_pos, 0, 1) * 100
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=weight_on_lead_p10,
        unit="%",
        ideal_range=(85.0, 95.0),
        notes="Final weight distribution on lead foot. Should be predominantly on lead side."
    )

def calculate_stability_p10(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """Calculates stability at end of swing."""
    p_position = "P10"
    kpi_name = "End Position Stability"
    
    # Check if all frames in P10 show consistent position (low variance)
    phase = get_phase_by_name(swing_input, p_position)
    if not phase:
        return None
    
    hip_positions = []
    for i in range(phase['start_frame_index'], phase['end_frame_index'] + 1):
        if i < len(swing_input['frames']):
            frame_data = swing_input['frames'][i]
            lh = get_keypoint(frame_data, KP_LEFT_HIP)
            rh = get_keypoint(frame_data, KP_RIGHT_HIP)
            if lh is not None and rh is not None:
                hip_center = get_midpoint(lh, rh)
                hip_positions.append(hip_center)
    
    if len(hip_positions) < 2:
        return None
    
    # Calculate variance in hip position
    hip_positions_array = np.array(hip_positions)
    position_variance = np.var(hip_positions_array, axis=0)
    total_variance = np.sum(position_variance)
    
    # Convert to stability score (lower variance = higher stability)
    stability_score = max(0, 100 - total_variance * 10000)  # Scale factor may need adjustment
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=stability_score,
        unit="%",
        ideal_range=(85.0, 100.0),
        notes="Stability score based on position consistency during end phase. Higher values indicate less movement."
    )


def calculate_hip_lateral_sway_p4(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """
    Calculates hip lateral sway at P4 (Top of Backswing) relative to P1 (Address).
    Lateral sway is the side-to-side movement of the hip center during the backswing.
    Excessive sway can lead to inconsistent ball striking and loss of power.
    """
    p_position = "P4"
    kpi_name = "Hip Lateral Sway at P4"
    
    # Get hip positions for P1 (reference) and P4
    left_hip_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_HIP)
    right_hip_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_HIP)
    left_hip_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([left_hip_p1 is not None, right_hip_p1 is not None, 
                left_hip_p4 is not None, right_hip_p4 is not None]):
        return None
    
    # Calculate hip center for both positions
    hip_center_p1 = get_midpoint(left_hip_p1, right_hip_p1)
    hip_center_p4 = get_midpoint(left_hip_p4, right_hip_p4)
    
    # Lateral sway is the movement in the X direction (side-to-side)
    # Assuming X is the left-right axis
    lateral_sway = hip_center_p4[0] - hip_center_p1[0]
    
    # Convert to meaningful units (assuming coordinate system is in meters)
    # Positive sway typically indicates movement toward the trail side (right for RH golfer)
    # Negative sway indicates movement toward the lead side (left for RH golfer)
    sway_distance = abs(lateral_sway) * 100  # Convert to centimeters for readability
    
    # Determine direction for notes
    direction = "toward trail side" if lateral_sway > 0 else "toward lead side"
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=sway_distance,
        unit="cm",
        ideal_range=(0.0, 5.0),  # Minimal sway is ideal
        notes=f"Lateral hip movement from address to top of backswing. Movement {direction}. Excessive sway can cause inconsistency."
    )


def calculate_spine_angle_p1(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """
    Calculates spine angle at P1 (Address) in the sagittal plane.
    This measures the forward tilt of the spine from vertical, which is critical for proper setup.
    """
    p_position = "P1"
    kpi_name = "Spine Angle at Address (Sagittal)"
    
    # Get keypoints for spine calculation
    left_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    right_shoulder = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    left_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([left_shoulder is not None, right_shoulder is not None, 
                left_hip is not None, right_hip is not None]):
        return None
    
    # Calculate midpoints for spine representation
    mid_shoulder = get_midpoint(left_shoulder, right_shoulder)
    mid_hip = get_midpoint(left_hip, right_hip)
    
    # Spine vector (from hips to shoulders)
    spine_vector = mid_shoulder - mid_hip
    
    # Vertical reference vector (assuming Y is up)
    vertical_vector = np.array([0, 1, 0])
    
    # Calculate angle between spine and vertical in sagittal plane (YZ plane)
    # Project spine vector onto YZ plane to get side view
    spine_sagittal = np.array([0, spine_vector[1], spine_vector[2]])
    
    if np.linalg.norm(spine_sagittal) == 0:
        return None
    
    # Calculate angle from vertical
    spine_angle = calculate_angle_3d(
        mid_hip + vertical_vector,
        mid_hip,
        mid_hip + spine_sagittal
    )
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=spine_angle,
        unit="degrees",
        ideal_range=(25.0, 40.0),  # Proper forward spine tilt for iron shots
        notes="Forward tilt of spine from vertical at address. Critical for proper setup and rotation."
    )


def calculate_reverse_spine_angle_p4(swing_input: SwingVideoAnalysisInput) -> Optional[BiomechanicalKPI]:
    """
    Calculates reverse spine angle at P4 (Top of Backswing).
    Reverse spine angle occurs when the spine tilts away from the target (right for RH golfer)
    at the top of the backswing. This is generally considered a fault that can lead to
    inconsistent ball striking and back problems.
    """
    p_position = "P4"
    kpi_name = "Reverse Spine Angle at P4"
    
    # Get spine keypoints for P1 (reference) and P4
    left_shoulder_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_SHOULDER)
    right_shoulder_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_SHOULDER)
    left_hip_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_LEFT_HIP)
    right_hip_p1 = get_average_keypoint_position_for_phase(swing_input, "P1", KP_RIGHT_HIP)
    
    left_shoulder_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_SHOULDER)
    right_shoulder_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_SHOULDER)
    left_hip_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_LEFT_HIP)
    right_hip_p4 = get_average_keypoint_position_for_phase(swing_input, p_position, KP_RIGHT_HIP)
    
    if not all([left_shoulder_p1 is not None, right_shoulder_p1 is not None,
                left_hip_p1 is not None, right_hip_p1 is not None,
                left_shoulder_p4 is not None, right_shoulder_p4 is not None,
                left_hip_p4 is not None, right_hip_p4 is not None]):
        return None
    
    # Calculate spine vectors for both positions
    mid_shoulder_p1 = get_midpoint(left_shoulder_p1, right_shoulder_p1)
    mid_hip_p1 = get_midpoint(left_hip_p1, right_hip_p1)
    spine_vector_p1 = mid_shoulder_p1 - mid_hip_p1
    
    mid_shoulder_p4 = get_midpoint(left_shoulder_p4, right_shoulder_p4)
    mid_hip_p4 = get_midpoint(left_hip_p4, right_hip_p4)
    spine_vector_p4 = mid_shoulder_p4 - mid_hip_p4
    
    # Project both spine vectors onto the frontal plane (XY plane, assuming Z is target line)
    spine_frontal_p1 = np.array([spine_vector_p1[0], spine_vector_p1[1], 0])
    spine_frontal_p4 = np.array([spine_vector_p4[0], spine_vector_p4[1], 0])
    
    if np.linalg.norm(spine_frontal_p1) == 0 or np.linalg.norm(spine_frontal_p4) == 0:
        return None
    
    # Calculate the change in spine angle in the frontal plane
    # This represents the lateral tilt of the spine
    angle_change = calculate_angle_3d(
        mid_hip_p4 + spine_frontal_p1,
        mid_hip_p4,
        mid_hip_p4 + spine_frontal_p4
    )
    
    # Determine direction of tilt
    # For RH golfer, positive X is typically toward target (left), negative X is away from target (right)
    # Reverse spine angle is tilting away from target (negative X direction for RH golfer)
    x_tilt_change = spine_frontal_p4[0] - spine_frontal_p1[0]
    
    # If the spine has tilted away from target (right for RH golfer), this is reverse spine angle
    reverse_spine_severity = -x_tilt_change if x_tilt_change < 0 else 0
    reverse_spine_angle = math.degrees(math.atan2(abs(x_tilt_change), spine_frontal_p4[1])) if spine_frontal_p4[1] != 0 else 0
    
    # Use the actual measured angle change, but note direction
    direction_note = "away from target (reverse spine)" if x_tilt_change < 0 else "toward target"
    
    return BiomechanicalKPI(
        p_position=p_position,
        kpi_name=kpi_name,
        value=reverse_spine_angle,
        unit="degrees",
        ideal_range=(0.0, 5.0),  # Minimal reverse spine angle is ideal
        notes=f"Lateral spine tilt at top of backswing. Current tilt: {direction_note}. Reverse spine angle (away from target) should be minimized."
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

    # Additional P1 KPIs
    spine_angle_p1 = calculate_spine_angle_p1(swing_input)
    if spine_angle_p1: all_kpis.append(spine_angle_p1)

    # P2 KPIs (Takeaway)
    p2_kpis = extract_p2_kpis(swing_input)
    all_kpis.extend(p2_kpis)

    # P3 KPIs (Halfway Back)
    p3_kpis = extract_p3_kpis(swing_input)
    all_kpis.extend(p3_kpis)

    # P4 KPIs (Top of Backswing)
    shoulder_rot_p4 = calculate_shoulder_rotation_p4(swing_input)
    if shoulder_rot_p4: all_kpis.append(shoulder_rot_p4)
    
    hip_sway_p4 = calculate_hip_lateral_sway_p4(swing_input)
    if hip_sway_p4: all_kpis.append(hip_sway_p4)
    
    reverse_spine_p4 = calculate_reverse_spine_angle_p4(swing_input)
    if reverse_spine_p4: all_kpis.append(reverse_spine_p4)

    # P5 KPIs (Early Downswing)
    p5_kpis = extract_p5_kpis(swing_input)
    all_kpis.extend(p5_kpis)

    # P6 KPIs (Pre-Impact)
    p6_kpis = extract_p6_kpis(swing_input)
    all_kpis.extend(p6_kpis)

    # P7 KPIs (Impact)
    p7_kpis = extract_p7_kpis(swing_input)
    all_kpis.extend(p7_kpis)

    # P8 KPIs (Release)
    p8_kpis = extract_p8_kpis(swing_input)
    all_kpis.extend(p8_kpis)

    # P9 KPIs (Finish)
    p9_kpis = extract_p9_kpis(swing_input)
    all_kpis.extend(p9_kpis)

    # P10 KPIs (End of Swing)
    p10_kpis = extract_p10_kpis(swing_input)
    all_kpis.extend(p10_kpis)

    print(f"Extracted {len(all_kpis)} KPIs across all P-positions.")
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

    print("\n--- Example: Lead Wrist Angle P4 ---")
    lwa_p4 = calculate_lead_wrist_angle_p4(sample_swing_input)
    if lwa_p4: print(lwa_p4)

    print("\n--- Example: Hip Lateral Sway P4 ---")
    hls_p4 = calculate_hip_lateral_sway_p4(sample_swing_input)
    if hls_p4: print(hls_p4)

    print("\n--- Example: Spine Angle P1 ---")
    sa_p1 = calculate_spine_angle_p1(sample_swing_input)
    if sa_p1: print(sa_p1)

    print("\n--- Example: Reverse Spine Angle P4 ---")
    rsa_p4 = calculate_reverse_spine_angle_p4(sample_swing_input)
    if rsa_p4: print(rsa_p4)
