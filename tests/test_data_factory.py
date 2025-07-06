"""
Test Data Factory for SwingSync AI Unit and Integration Tests.

This module provides utility functions to generate mock `SwingVideoAnalysisInput`
objects and their constituent parts (like `FramePoseData`, `PoseKeypoint`).
These generated objects are used in test cases to simulate various swing scenarios,
such as:
- A "good" swing with KPIs expected to be within ideal ranges.
- Swings with specific characteristics designed to trigger particular fault
  detection rules (e.g., insufficient shoulder turn, excessive hip hinge).

Using a data factory helps ensure that tests are run with consistent and
well-defined input data, making tests more reliable and easier to debug.
It also centralizes the creation of complex test data objects.
"""
from typing import List, Dict, Any
import copy
import numpy as np

# Assuming data_structures.py and kpi_extraction.py are accessible in PYTHONPATH
# For tests, it's common to adjust sys.path or use package structures.
# If running tests from root, `from ..data_structures import ...` might be needed if tests is a package.
# For simplicity, assuming they can be imported directly for now.
from data_structures import SwingVideoAnalysisInput, FramePoseData, PSystemPhase, PoseKeypoint
from kpi_extraction import (
    KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER, KP_LEFT_HIP, KP_RIGHT_HIP,
    KP_LEFT_KNEE, KP_RIGHT_KNEE, KP_LEFT_ANKLE, KP_RIGHT_ANKLE,
    KP_LEFT_WRIST, KP_RIGHT_WRIST, KP_NOSE
)

DEFAULT_FPS = 60.0

def _make_kp(x: float, y: float, z: float, visibility: float = 1.0) -> PoseKeypoint:
    """Helper to create a PoseKeypoint dictionary."""
    return {"x": x, "y": y, "z": z, "visibility": visibility}

def create_default_frame_pose() -> FramePoseData:
    """
    Creates a default set of keypoints for a single frame, representing a
    reasonable 'address' posture. Y is up, X is right, Z is towards target/camera.
    Origin could be considered between the feet.
    """
    return {
        KP_NOSE: _make_kp(0, 1.6, 0.1),
        KP_LEFT_SHOULDER: _make_kp(-0.2, 1.4, 0), KP_RIGHT_SHOULDER: _make_kp(0.2, 1.4, 0),
        KP_LEFT_ELBOW: _make_kp(-0.35, 1.15, 0.05), KP_RIGHT_ELBOW: _make_kp(0.35, 1.15, 0.05),
        KP_LEFT_WRIST: _make_kp(-0.4, 0.9, 0.15), KP_RIGHT_WRIST: _make_kp(0.4, 0.9, 0.15),
        KP_LEFT_HIP: _make_kp(-0.15, 0.9, 0), KP_RIGHT_HIP: _make_kp(0.15, 0.9, 0),
        KP_LEFT_KNEE: _make_kp(-0.18, 0.5, 0.02), KP_RIGHT_KNEE: _make_kp(0.18, 0.5, 0.02),
        KP_LEFT_ANKLE: _make_kp(-0.2, 0.1, 0), KP_RIGHT_ANKLE: _make_kp(0.2, 0.1, 0),
    }

def create_p_system_classification(num_frames_per_phase: int = 10, total_phases: int = 10) -> List[PSystemPhase]:
    """Creates a generic P-System classification."""
    phases: List[PSystemPhase] = []
    current_frame = 0
    for i in range(1, total_phases + 1):
        phases.append({
            "phase_name": f"P{i}",
            "start_frame_index": current_frame,
            "end_frame_index": current_frame + num_frames_per_phase -1
        })
        current_frame += num_frames_per_phase
    return phases

def create_swing_input(
    session_id: str = "test_session",
    user_id: str = "test_user",
    club_used: str = "7-Iron",
    num_frames_total: int = 100, # Should match p_system_classification total frames
    custom_frames: Optional[List[FramePoseData]] = None,
    custom_p_system: Optional[List[PSystemPhase]] = None
) -> SwingVideoAnalysisInput:
    """
    Creates a SwingVideoAnalysisInput object.
    By default, creates a 10-phase swing with 10 frames per phase (100 total frames).
    Each frame will have the default_frame_pose unless custom_frames are provided.
    """
    frames = custom_frames
    if not frames:
        default_pose = create_default_frame_pose()
        frames = [copy.deepcopy(default_pose) for _ in range(num_frames_total)]

    p_system = custom_p_system
    if not p_system:
        num_phases = 10
        frames_per_phase = num_frames_total // num_phases
        p_system = create_p_system_classification(num_frames_per_phase, num_phases)
        # Adjust num_frames_total if it wasn't perfectly divisible
        num_frames_total = frames_per_phase * num_phases
        if len(frames) != num_frames_total: # Ensure frames list matches
             frames = [copy.deepcopy(frames[0] if frames else create_default_frame_pose()) for _ in range(num_frames_total)]


    return {
        "session_id": session_id,
        "user_id": user_id,
        "club_used": club_used,
        "frames": frames,
        "p_system_classification": p_system,
        "video_fps": DEFAULT_FPS
    }

# --- Scenario-specific data generation ---

def get_good_swing_input(session_id="good_swing_01") -> SwingVideoAnalysisInput:
    """
    Returns a SwingVideoAnalysisInput that should result in 'good' KPIs
    according to the current rules (few to no faults).
    This involves setting up keypoints for P1 and P4 specifically.
    """
    num_frames_per_phase = 11 # P1: 0-10, P2: 11-21, ... P4: 33-43
    total_frames = num_frames_per_phase * 10
    p_system_phases = create_p_system_classification(num_frames_per_phase, 10)

    all_frames: List[FramePoseData] = [create_default_frame_pose() for _ in range(total_frames)]

    # Modify P1 frames (0-10) to have ideal posture
    # Default posture is already decent for hip hinge and knee flex based on kpi_extraction tests
    # Hip Hinge (Spine from Vertical): default around 35-40 deg. Ideal: (30,45)
    # Knee Flex: default around 20-23 deg. Ideal: (15,25)
    # Weight Dist: default 50%. Ideal: (45,55) for irons

    # Modify P4 frames (indices for P4: start=3*11=33, end=33+11-1=43)
    # Shoulder rotation P4: Ideal ~90 deg.
    # The default kpi_extraction test data for 90 deg rotation:
    # ls_x_p4 = 0, ls_z_p4 = -0.2; rs_x_p4 = 0, rs_z_p4 = 0.2 (relative to shoulder center at Y=1.4)
    # Absolute: L: (0, 1.4, -0.2), R: (0, 1.4, 0.2)
    # Hip rotation P4: Ideal ~45 deg. (similarly modify hip keypoints)
    p4_start_idx = p_system_phases[3]['start_frame_index'] # P4 is the 4th phase (index 3)
    p4_end_idx = p_system_phases[3]['end_frame_index']

    for i in range(p4_start_idx, p4_end_idx + 1):
        frame = copy.deepcopy(all_frames[i]) # Start with default P1-like pose
        # Apply 90 deg shoulder rotation
        frame[KP_LEFT_SHOULDER] = _make_kp(0, 1.4, -0.2)
        frame[KP_RIGHT_SHOULDER] = _make_kp(0, 1.4, 0.2)
        # Apply ~45 deg hip rotation (approx)
        frame[KP_LEFT_HIP] = _make_kp(-0.075, 0.9, -0.075) # x and z components for hips
        frame[KP_RIGHT_HIP] = _make_kp(0.075, 0.9, 0.075)
        all_frames[i] = frame

    return create_swing_input(
        session_id=session_id,
        custom_frames=all_frames,
        custom_p_system=p_system_phases,
        num_frames_total=total_frames
    )


def get_insufficient_shoulder_turn_swing_input(session_id="bad_shoulder_turn_01") -> SwingVideoAnalysisInput:
    """
    Swing input designed to trigger 'INSUFFICIENT_SHOULDER_TURN_P4'.
    P4 shoulder rotation will be ~45 degrees.
    """
    num_frames_per_phase = 11
    total_frames = num_frames_per_phase * 10
    p_system_phases = create_p_system_classification(num_frames_per_phase, 10)
    all_frames: List[FramePoseData] = [create_default_frame_pose() for _ in range(total_frames)]

    p4_start_idx = p_system_phases[3]['start_frame_index']
    p4_end_idx = p_system_phases[3]['end_frame_index']

    # Modify P4 frames for ~45 deg shoulder rotation
    # Original L: (-0.2, 1.4, 0), R: (0.2, 1.4, 0)
    # Rotated 45 deg CW: L_x = -0.2*cos(45) = -0.141, L_z = -0.2*sin(45) = -0.141
    # R_x = 0.2*cos(45) = 0.141, R_z = 0.2*sin(45) = 0.141
    for i in range(p4_start_idx, p4_end_idx + 1):
        frame = copy.deepcopy(all_frames[i])
        frame[KP_LEFT_SHOULDER] = _make_kp(-0.141, 1.4, -0.141)
        frame[KP_RIGHT_SHOULDER] = _make_kp(0.141, 1.4, 0.141)
        # Keep hips less rotated too, to be consistent
        frame[KP_LEFT_HIP] = _make_kp(-0.1, 0.9, -0.05)
        frame[KP_RIGHT_HIP] = _make_kp(0.1, 0.9, 0.05)
        all_frames[i] = frame

    return create_swing_input(
        session_id=session_id,
        custom_frames=all_frames,
        custom_p_system=p_system_phases,
        num_frames_total=total_frames
    )

def get_excessive_hip_hinge_input(session_id="bad_hip_hinge_01") -> SwingVideoAnalysisInput:
    """
    Swing input designed to trigger 'IMPROPER_POSTURE_HIP_HINGE_P1' (too much hinge).
    Hip hinge > 45 degrees. Default is ~35-40. We need to increase it.
    Increase by pushing shoulders more forward (larger -Z) or hips more back (smaller -Z).
    Or by lowering shoulders (smaller Y for shoulders).
    Let's lower shoulders relative to hips for P1.
    """
    num_frames_per_phase = 11
    total_frames = num_frames_per_phase * 10
    p_system_phases = create_p_system_classification(num_frames_per_phase, 10)
    all_frames: List[FramePoseData] = [create_default_frame_pose() for _ in range(total_frames)]

    p1_start_idx = p_system_phases[0]['start_frame_index']
    p1_end_idx = p_system_phases[0]['end_frame_index']

    for i in range(p1_start_idx, p1_end_idx + 1):
        frame = copy.deepcopy(all_frames[i])
        # Lower shoulders to increase hinge angle from vertical
        frame[KP_LEFT_SHOULDER]['y'] = 1.30 # Was 1.4
        frame[KP_RIGHT_SHOULDER]['y'] = 1.30 # Was 1.4
        # This should make torso vector point more downwards, increasing angle with vertical.
        all_frames[i] = frame

    # Also, ensure P4 is normal to not mix faults too much for this specific test case
    p4_start_idx = p_system_phases[3]['start_frame_index']
    p4_end_idx = p_system_phases[3]['end_frame_index']
    for i in range(p4_start_idx, p4_end_idx + 1):
        frame = copy.deepcopy(all_frames[i]) # Start with default P1-like pose
        frame[KP_LEFT_SHOULDER] = _make_kp(0, 1.4, -0.2) # Normal P4 shoulders
        frame[KP_RIGHT_SHOULDER] = _make_kp(0, 1.4, 0.2)
        all_frames[i] = frame


    return create_swing_input(
        session_id=session_id,
        custom_frames=all_frames,
        custom_p_system=p_system_phases,
        num_frames_total=total_frames
    )


if __name__ == '__main__':
    print("--- Test Data Factory Playground ---")

    default_swing = create_swing_input()
    print(f"Default swing: {len(default_swing['frames'])} frames, Club: {default_swing['club_used']}")
    print(f"P1: {default_swing['p_system_classification'][0]}")
    # print(f"Frame 0 pose: {default_swing['frames'][0][KP_LEFT_SHOULDER]}")

    good_swing = get_good_swing_input()
    print(f"\nGood swing: {len(good_swing['frames'])} frames, Session: {good_swing['session_id']}")
    # print(f"P1 frame 0 L Shoulder: {good_swing['frames'][0][KP_LEFT_SHOULDER]}")
    # p4_idx = good_swing['p_system_classification'][3]['start_frame_index']
    # print(f"P4 frame {p4_idx} L Shoulder: {good_swing['frames'][p4_idx][KP_LEFT_SHOULDER]}")


    bad_turn_swing = get_insufficient_shoulder_turn_swing_input()
    print(f"\nBad shoulder turn swing: {bad_turn_swing['session_id']}")
    # p4_idx_bad = bad_turn_swing['p_system_classification'][3]['start_frame_index']
    # print(f"P4 frame {p4_idx_bad} L Shoulder: {bad_turn_swing['frames'][p4_idx_bad][KP_LEFT_SHOULDER]}")

    bad_hinge_swing = get_excessive_hip_hinge_input()
    print(f"\nBad hip hinge swing: {bad_hinge_swing['session_id']}")
    # print(f"P1 frame 0 L Shoulder Y: {bad_hinge_swing['frames'][0][KP_LEFT_SHOULDER]['y']}")
    # print(f"P1 frame 0 L Hip Y: {bad_hinge_swing['frames'][0][KP_LEFT_HIP]['y']}")


    # You would typically import these functions in your test files.
    # Example: from tests.test_data_factory import get_good_swing_input
    print("\nTest data factory created. Use these functions in your unit tests.")

```
