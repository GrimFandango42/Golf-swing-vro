"""
Defines the core data structures for the SwingSync AI backend.

This module contains all TypedDict definitions used to structure data flowing
through the analysis pipeline, from raw input pose data to the final feedback output.
These structures ensure type consistency and provide a clear understanding of the
data expected and produced by various components.
"""
from typing import List, Dict, Tuple, TypedDict, Optional, Any

# --- Pose Estimation Data Structures ---

class PoseKeypoint(TypedDict):
    """
    Represents a single 3D keypoint from a pose estimation model.

    Attributes:
        x (float): The x-coordinate of the keypoint.
        y (float): The y-coordinate of the keypoint.
        z (float): The z-coordinate of the keypoint (depth).
        visibility (Optional[float]): Confidence score or visibility of the keypoint,
                                     typically provided by the pose estimation model.
                                     Ranges might vary (e.g., 0.0 to 1.0).
    """
    x: float
    y: float
    z: float
    visibility: Optional[float]  # Confidence score for the keypoint

# Using a dictionary for FramePoseData to easily access keypoints by name.
# Names could be e.g., "nose", "left_shoulder", "right_elbow", etc.
# These names should align with the output of the pose estimation model (e.g., MediaPipe Pose).
FramePoseData = Dict[str, PoseKeypoint]

class PSystemPhase(TypedDict):
    """Defines a single phase in the P-System classification."""
    phase_name: str  # e.g., "P1", "P2", ..., "P10"
    start_frame_index: int
    end_frame_index: int

class SwingVideoAnalysisInput(TypedDict):
    """
    Top-level input structure for the backend analysis.
    This data is expected to be sent from the mobile client after on-device processing.
    """
    session_id: str # Unique ID for this swing analysis session
    user_id: str
    club_used: str  # e.g., "Driver", "7-Iron", "Putter"
    frames: List[FramePoseData]
    p_system_classification: List[PSystemPhase]
    video_fps: float # Frames per second of the source video, for temporal analysis

# --- Biomechanical KPI & Fault Diagnosis Structures ---

class BiomechanicalKPI(TypedDict):
    """Represents a single calculated biomechanical Key Performance Indicator."""
    p_position: str  # P-System position this KPI relates to (e.g., "P1", "P4")
    kpi_name: str    # Name of the KPI (e.g., "Hip Hinge Angle", "Shoulder Rotation")
    value: Any       # Calculated value of the KPI
    unit: str        # Unit of the KPI (e.g., "degrees", "radians", "meters", "percentage")
    ideal_range: Optional[Tuple[float, float]] # Optional: ideal range for this KPI
    notes: Optional[str] # Optional: any notes about how it was calculated or its significance

class KPIDeviation(TypedDict):
    """Describes a specific KPI's deviation from its ideal."""
    kpi_name: str
    observed_value: Any
    ideal_value_or_range: Any
    p_position: str

class DetectedFault(TypedDict):
    """Represents a single swing fault identified by the system."""
    fault_id: str # A unique identifier for this type of fault (e.g., "F001", "OVER_THE_TOP")
    fault_name: str # User-friendly name (e.g., "Over-the-Top Move", "Cupped Left Wrist at Top")
    p_positions_implicated: List[str] # P-System phases where this fault is evident or has roots
    description: str # General description of what this fault is
    kpi_deviations: List[KPIDeviation] # List of specific KPI measurements that led to this diagnosis
    llm_prompt_template_key: str # Key to find the appropriate prompt in the FaultDiagnosisMatrix
    severity: Optional[float] # Optional: a score from 0.0 to 1.0 indicating severity

# --- Fault Diagnosis Matrix Structure ---
# The matrix itself will likely be a list of these entries, or a dictionary keyed by a unique ID.

class FaultDiagnosisMatrixEntry(TypedDict):
    """
    Defines an entry in the Fault Diagnosis Matrix.
    This structure links biomechanical metrics/conditions to specific faults and LLM prompt templates.
    """
    entry_id: str # Unique ID for this matrix entry
    p_position_focused: str # The primary P-Position this rule applies to
    biomechanical_metric_checked: str # e.g., "LeadWristFlexionExtensionDegrees"

    # Condition defines how to evaluate the metric.
    # Example: {"operator": "greater_than", "threshold": 20.0} for cupped wrist
    # Example: {"operator": "outside_range", "lower_bound": -5.0, "upper_bound": 5.0}
    condition_type: str # "greater_than", "less_than", "outside_range", "equals" etc.
    condition_values: Dict[str, Any] # e.g. {"threshold": 20.0} or {"lower_bound": -5.0, "upper_bound": 5.0}

    fault_to_report_id: str # The fault_id from DetectedFault that this condition triggers

    # LLM prompt template specific to this finding.
    # It can use placeholders like {observed_value}, {ideal_range}, {club_used}, {p_position_focused}
    # These placeholders will be filled before sending to the LLM.
    llm_prompt_template: str

    # Optional: Pointers to drills or corrective actions, could also be part of LLM generation
    suggested_drills_keys: Optional[List[str]]


# Example of how the Fault Diagnosis Matrix could be stored (e.g., in a JSON file or database)
# fault_diagnosis_matrix_example: List[FaultDiagnosisMatrixEntry] = [
#     {
#         "entry_id": "FDM001",
#         "p_position_focused": "P4", # Top of Backswing
#         "biomechanical_metric_checked": "LeadWristAngle", # Assuming a single angle value now
#         "condition_type": "greater_than", # Positive values mean extension/cupping
#         "condition_values": {"threshold": 15.0}, # e.g., more than 15 degrees of extension
#         "fault_to_report_id": "CUPPED_WRIST_AT_TOP",
#         "llm_prompt_template": "At P4 (top of backswing), your lead wrist is cupped ({observed_value:.1f} degrees). Ideally, it should be flat (around 0 degrees) or slightly bowed (negative values). A cupped wrist often leads to an open clubface, causing slices or pushes. Try to feel the back of your lead hand pointing more towards the sky. A good drill is..."
#     },
#     # ... more entries
# ]

# --- LLM Interaction Structures ---

class LLMFeedbackRequest(TypedDict):
    """Data sent to the LLM interaction service."""
    session_id: str
    user_id: str
    club_used: str
    detected_faults: List[DetectedFault] # The system might send one primary fault or a few key ones
    # Optional: user skill level, goals, previous feedback, etc. for more personalization
    user_profile_context: Optional[Dict[str, Any]]

class LLMGeneratedTip(TypedDict):
    """A single piece of advice or drill from the LLM."""
    explanation: str
    tip: str
    drill_suggestion: Optional[str]
    # Potentially links to video/image resources for drills

class SwingAnalysisFeedback(TypedDict):
    """The final feedback structure returned to the client."""
    session_id: str
    summary_of_findings: str # Overall summary, possibly from LLM
    detailed_feedback: List[LLMGeneratedTip] # Tips for each major fault addressed
    # Include raw detected faults for debugging or more detailed display if needed
    raw_detected_faults: List[DetectedFault]
    # Visualisation hints can also be part of this if backend dictates some annotations
    visualisation_annotations: Optional[List[Dict[str, Any]]]

print("Data structures for SwingSync AI defined.")

"""
# Example Usage (for illustration, not part of the module itself)

# Sample pose data for one frame
sample_frame_pose: FramePoseData = {
    "nose": {"x": 0.1, "y": 1.5, "z": 0.3, "visibility": 0.99},
    "left_shoulder": {"x": -0.2, "y": 1.3, "z": 0.25, "visibility": 0.95},
    # ... other 31+ keypoints
}

# Sample P-System Classification
sample_p_system: List[PSystemPhase] = [
    {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 15},
    {"phase_name": "P2", "start_frame_index": 16, "end_frame_index": 30},
    # ... up to P10
]

# Sample input to the backend
sample_analysis_input: SwingVideoAnalysisInput = {
    "session_id": "sess_12345",
    "user_id": "user_abc",
    "club_used": "7-Iron",
    "frames": [sample_frame_pose] * 100, # List of 100 frames with the same pose for simplicity
    "p_system_classification": sample_p_system,
    "video_fps": 240.0
}

# Sample detected fault (would be output from fault detection logic)
sample_detected_fault: DetectedFault = {
    "fault_id": "CUPPED_WRIST_AT_TOP",
    "fault_name": "Cupped Left Wrist at Top of Backswing",
    "p_positions_implicated": ["P4"],
    "description": "The lead wrist is excessively extended (cupped) at the top of the backswing.",
    "kpi_deviations": [
        {
            "kpi_name": "LeadWristAngleP4",
            "observed_value": 25.0,
            "ideal_value_or_range": "Ideally flat (0°) or slightly bowed (<0°)",
            "p_position": "P4"
        }
    ],
    "llm_prompt_template_key": "CUPPED_WRIST_P4_PROMPT", # Key to look up in a prompt dictionary
    "severity": 0.8
}
"""
