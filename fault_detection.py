"""
Module for Golf Swing Fault Detection Logic.

This module is responsible for identifying common golf swing faults by comparing
extracted Biomechanical KPIs (from `kpi_extraction.py`) against a predefined
set of rules known as the `FAULT_DIAGNOSIS_MATRIX`.

The core function `check_swing_faults` iterates through this matrix, evaluates
conditions based on KPI values, and generates a list of `DetectedFault` objects
if deviations are found.

The `FAULT_DIAGNOSIS_MATRIX` is central to this module. Each entry in the matrix
defines:
- The P-System position and biomechanical metric to check.
- The condition (e.g., outside a range, less than a threshold) that signifies a fault.
- Details of the fault to report (ID, name, description).
- A key to an LLM prompt template for generating coaching feedback.
- Optional severity calculation logic.

This matrix is designed to be extensible, allowing new fault detection rules
to be added as more KPIs become available or coaching knowledge is refined.
"""
from typing import List, Dict, Any, Optional

from data_structures import (
    SwingVideoAnalysisInput,
    BiomechanicalKPI,
    DetectedFault,
    FaultDiagnosisMatrixEntry, # Re-defined here for clarity, or could be imported if centralized
    KPIDeviation
)
from kpi_extraction import EXPECTED_KEYPOINTS # For reference if needed

# Re-defining FaultDiagnosisMatrixEntry for local use or it could be a shared model
# For simplicity, assuming it's defined as in data_structures.py
# from data_structures import FaultDiagnosisMatrixEntry

# --- Fault Diagnosis Matrix ---
# This matrix defines the rules for detecting faults.
# In a real application, this would likely be loaded from a configuration file (JSON, YAML) or database.

# Placeholders for KPIs not yet fully implemented in kpi_extraction.py
# These are used to demonstrate how the fault matrix would handle them.
PLACEHOLDER_LEAD_WRIST_ANGLE_P4 = "LeadWristAngleP4"
PLACEHOLDER_HIP_LATERAL_SWAY_P4 = "HipLateralSwayP4" # Positive for sway away from target (RH golfer)
PLACEHOLDER_SPINE_ANGLE_REVERSE_P4 = "SpineAngleReverseP4" # Positive if leaning towards target

FAULT_DIAGNOSIS_MATRIX: List[FaultDiagnosisMatrixEntry] = [
    {
        "entry_id": "FD001",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Hip Hinge Angle (Spine from Vertical)",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": 30.0, "upper_bound": 45.0}, # Ideal range from requirements
        "fault_to_report_id": "IMPROPER_POSTURE_HIP_HINGE_P1",
        "fault_name": "Improper Hip Hinge at Address",
        "fault_description": "Your posture at address shows an incorrect forward tilt from the hips (hip hinge). Too little hinge (standing too upright) or too much hinge (bending over too much) can negatively affect your balance, power, and swing mechanics throughout the entire motion.",
        "llm_prompt_template_key": "IMPROPER_HIP_HINGE_P1_PROMPT",
        "severity_levels": [
            {"threshold_from_ideal_percent": 20, "severity": 0.3},
            {"threshold_from_ideal_percent": 50, "severity": 0.6},
            {"threshold_from_ideal_percent": 100, "severity": 0.9},
        ]
    },
    {
        "entry_id": "FD002",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Left Knee Flexion Angle",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": 15.0, "upper_bound": 25.0},
        "fault_to_report_id": "IMPROPER_KNEE_FLEX_P1", # Can map to same general fault ID
        "fault_name": "Improper Knee Flex at Address (Lead Leg)",
        "fault_description": "The amount of flex in your lead knee at address is outside the optimal range. Correct knee flex is important for maintaining an athletic posture, enabling proper body rotation, and ensuring stability.",
        "llm_prompt_template_key": "IMPROPER_KNEE_FLEX_P1_LEAD_PROMPT",
    },
    {
        "entry_id": "FD003",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Right Knee Flexion Angle",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": 15.0, "upper_bound": 25.0},
        "fault_to_report_id": "IMPROPER_KNEE_FLEX_P1",
        "fault_name": "Improper Knee Flex at Address (Trail Leg)",
        "fault_description": "The amount of flex in your trail knee at address is outside the optimal range. Correct knee flex helps maintain balance, supports weight transfer, and contributes to a stable lower body during the swing.",
        "llm_prompt_template_key": "IMPROPER_KNEE_FLEX_P1_TRAIL_PROMPT",
    },
    {
        "entry_id": "FD004",
        "p_position_focused": "P4",
        "biomechanical_metric_checked": "Shoulder Rotation at P4 (relative to Address)",
        "condition_type": "less_than",
        "condition_values": {"threshold": 80.0}, # Ideal is ~90 deg, fault if < 80 deg
        "fault_to_report_id": "INSUFFICIENT_SHOULDER_TURN_P4",
        "fault_name": "Insufficient Shoulder Turn at Top of Backswing",
        "fault_description": "Your shoulder rotation at the top of the backswing (P4) appears restricted. A full shoulder turn (around 90 degrees for most players) is vital for generating maximum power and ensuring a properly sequenced downswing.",
        "llm_prompt_template_key": "INSUFFICIENT_SHOULDER_TURN_P4_PROMPT",
    },
    {
        "entry_id": "FD005",
        "p_position_focused": "P4",
        "biomechanical_metric_checked": PLACEHOLDER_LEAD_WRIST_ANGLE_P4,
        "condition_type": "greater_than", # Positive values = extension/cupping
        "condition_values": {"threshold": 10.0}, # Stricter: fault if > 10 deg cupped (was 15)
        "fault_to_report_id": "CUPPED_WRIST_AT_TOP_P4",
        "fault_name": "Cupped Lead Wrist at Top of Backswing",
        "fault_description": "Your lead wrist is excessively extended (cupped) at the top of the backswing (P4). This common fault often leads to an open clubface at impact, resulting in slices or pushed shots, and can reduce power.",
        "llm_prompt_template_key": "CUPPED_WRIST_P4_PROMPT",
    },
    {
        "entry_id": "FD006",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Estimated Weight Distribution (Lead Foot %)",
        # This rule should ideally be dynamic based on club_used.
        # For now, assuming irons (50/50). A more complex rule engine could handle this.
        "condition_type": "outside_range", # For irons
        "condition_values": {"lower_bound": 45.0, "upper_bound": 55.0},
        "fault_to_report_id": "IMPROPER_WEIGHT_DISTRIBUTION_P1_IRONS", # Specific for irons
        "fault_name": "Improper Weight Distribution at Address (Irons)",
        "fault_description": "Your weight distribution at address with an iron is not balanced correctly (ideally 50/50 on lead/trail foot). Proper balance is key for consistent strikes and effective weight transfer.",
        "llm_prompt_template_key": "IMPROPER_WEIGHT_DIST_P1_IRONS_PROMPT"
    },
    # Rule for Driver Weight Distribution (FD006B - example of club-specific)
    # To implement this properly, the fault detection logic would need to check swing_input['club_used']
    # or have separate KPI variants. For now, this is illustrative.
    # {
    #     "entry_id": "FD006B",
    #     "p_position_focused": "P1",
    #     "biomechanical_metric_checked": "Estimated Weight Distribution (Lead Foot %)",
    #     "condition_type": "outside_range", # For Driver, e.g. 40% lead, 60% trail
    #     "condition_values": {"lower_bound": 35.0, "upper_bound": 45.0},
    #     "fault_to_report_id": "IMPROPER_WEIGHT_DISTRIBUTION_P1_DRIVER",
    #     "fault_name": "Improper Weight Distribution at Address (Driver)",
    #     "fault_description": "Your weight distribution at address with a driver is not optimal (ideally around 40% on lead, 60% on trail). This setup promotes an upward angle of attack.",
    #     "llm_prompt_template_key": "IMPROPER_WEIGHT_DIST_P1_DRIVER_PROMPT"
    # },
    {
        "entry_id": "FD007",
        "p_position_focused": "P4", # Or during backswing P2-P4
        "biomechanical_metric_checked": PLACEHOLDER_HIP_LATERAL_SWAY_P4, # Sway = excessive lateral motion
        "condition_type": "greater_than", # Assuming positive value means sway away from target for RH
        "condition_values": {"threshold": 0.15}, # e.g., > 0.15 meters (15cm) of lateral hip shift from P1 center
        "fault_to_report_id": "HIP_SWAY_BACKSWING",
        "fault_name": "Hip Sway During Backswing",
        "fault_description": "Your hips appear to be swaying laterally (away from the target) during the backswing, rather than rotating around a stable center. This can lead to inconsistency, loss of power, and difficulty returning the club to the ball squarely.",
        "llm_prompt_template_key": "HIP_SWAY_BACKSWING_PROMPT",
    },
    {
        "entry_id": "FD008",
        "p_position_focused": "P4",
        "biomechanical_metric_checked": PLACEHOLDER_SPINE_ANGLE_REVERSE_P4, # Reverse spine = upper body tilt to target
        "condition_type": "greater_than", # Assuming positive value indicates reverse tilt towards target
        "condition_values": {"threshold": 10.0}, # e.g., > 10 degrees of spine tilt towards target
        "fault_to_report_id": "REVERSE_SPINE_ANGLE_P4",
        "fault_name": "Reverse Spine Angle at Top of Backswing",
        "fault_description": "At the top of your backswing (P4), your upper body appears to be tilting towards the target, known as a 'reverse spine angle'. This common fault can cause inconsistent contact, loss of power, and put strain on your lower back.",
        "llm_prompt_template_key": "REVERSE_SPINE_ANGLE_P4_PROMPT",
    },
]

# --- Fault Detection Function ---

def _calculate_severity(kpi_value: float, rule: FaultDiagnosisMatrixEntry) -> Optional[float]:
    """
    Calculates severity based on the rule's severity_levels.
    This is a placeholder for more sophisticated severity calculation.
    """
    if "severity_levels" not in rule or not rule["severity_levels"]:
        return None # No severity logic defined for this rule

    # Example: if rule defines ideal_range in condition_values
    ideal_lower = rule["condition_values"].get("lower_bound")
    ideal_upper = rule["condition_values"].get("upper_bound")

    deviation_abs = 0
    if rule["condition_type"] == "outside_range" and ideal_lower is not None and ideal_upper is not None:
        if kpi_value < ideal_lower:
            deviation_abs = ideal_lower - kpi_value
        elif kpi_value > ideal_upper:
            deviation_abs = kpi_value - ideal_upper
    elif rule["condition_type"] == "less_than" and ideal_lower is not None: # Assuming threshold is effectively ideal_lower
         if kpi_value < ideal_lower: # kpi_value < threshold
            deviation_abs = ideal_lower - kpi_value
    elif rule["condition_type"] == "greater_than" and ideal_upper is not None: # Assuming threshold is effectively ideal_upper
        if kpi_value > ideal_upper: # kpi_value > threshold
            deviation_abs = kpi_value - ideal_upper

    # This is a very basic example. True severity might depend on % deviation, absolute, etc.
    # The current severity_levels in FD001 is based on percentage, which is more complex.
    # For now, let's use a simple mapping if deviation_abs is significant.
    if deviation_abs > 10: return 0.8 # High severity for >10 units deviation
    if deviation_abs > 5: return 0.5  # Medium
    if deviation_abs > 1: return 0.2  # Low

    return None


def check_swing_faults(
    swing_input: SwingVideoAnalysisInput,
    extracted_kpis: List[BiomechanicalKPI]
) -> List[DetectedFault]:
    """
    Analyzes extracted KPIs against the Fault Diagnosis Matrix to identify swing faults.

    Args:
        swing_input: The original input data for the swing analysis.
        extracted_kpis: A list of biomechanical KPIs calculated from the swing.

    Returns:
        A list of DetectedFault objects representing identified faults.
    """
    detected_faults: List[DetectedFault] = []
    kpis_map: Dict[str, BiomechanicalKPI] = {kpi['kpi_name']: kpi for kpi in extracted_kpis}

    for rule in FAULT_DIAGNOSIS_MATRIX:
        kpi_name_to_check = rule["biomechanical_metric_checked"]
        kpi = kpis_map.get(kpi_name_to_check)

        if not kpi:
            # print(f"Debug: KPI '{kpi_name_to_check}' needed for rule '{rule['entry_id']}' not found in extracted_kpis.")
            continue # Skip rule if required KPI is missing

        kpi_value = kpi['value']
        fault_detected_for_rule = False

        # Ensure kpi_value is float for comparisons if it's numeric
        if not isinstance(kpi_value, (int, float)):
            # print(f"Debug: KPI value for '{kpi_name_to_check}' is not numeric, skipping rule '{rule['entry_id']}'. Value: {kpi_value}")
            continue

        condition_type = rule["condition_type"]
        cv = rule["condition_values"] # condition_values from matrix entry

        if condition_type == "outside_range":
            if "lower_bound" in cv and "upper_bound" in cv:
                if not (cv["lower_bound"] <= kpi_value <= cv["upper_bound"]):
                    fault_detected_for_rule = True
        elif condition_type == "less_than":
            if "threshold" in cv:
                if kpi_value < cv["threshold"]:
                    fault_detected_for_rule = True
        elif condition_type == "greater_than":
            if "threshold" in cv:
                if kpi_value > cv["threshold"]:
                    fault_detected_for_rule = True
        # Add more conditions like "equals", "not_equals", "within_x_percent_of_ideal" etc. as needed

        if fault_detected_for_rule:
            ideal_val_desc = ""
            if condition_type == "outside_range":
                ideal_val_desc = f"between {cv['lower_bound']:.1f} and {cv['upper_bound']:.1f} {kpi['unit']}"
            elif condition_type == "less_than":
                ideal_val_desc = f"greater than or equal to {cv['threshold']:.1f} {kpi['unit']}"
            elif condition_type == "greater_than":
                ideal_val_desc = f"less than or equal to {cv['threshold']:.1f} {kpi['unit']}"

            kpi_deviation = KPIDeviation(
                kpi_name=kpi_name_to_check,
                observed_value=f"{kpi_value:.1f} {kpi['unit']}",
                ideal_value_or_range=f"Ideal: {ideal_val_desc}",
                p_position=kpi['p_position']
            )

            # Calculate severity (basic placeholder version)
            severity_score = _calculate_severity(float(kpi_value), rule)

            fault = DetectedFault(
                fault_id=rule["fault_to_report_id"],
                fault_name=rule.get("fault_name", "Unknown Fault Name"), # Use .get for optional fields
                p_positions_implicated=[kpi['p_position']], # Start with the KPI's P-position
                description=rule.get("fault_description", "No description provided."),
                kpi_deviations=[kpi_deviation],
                llm_prompt_template_key=rule["llm_prompt_template_key"],
                severity=severity_score
            )
            detected_faults.append(fault)

    return detected_faults


if __name__ == '__main__':
    from kpi_extraction import extract_all_kpis # For testing

    # --- Create dummy SwingVideoAnalysisInput for testing ---
    def _make_kp(x,y,z): return {"x":x, "y":y, "z":z, "visibility":1.0}

    # P1 Data: Hip Hinge: 50 deg (too much), Left Knee Flex: 30 deg (too much)
    # Shoulder Rotation P4: 70 deg (insufficient)
    # Weight Dist P1: 60% on lead (too much for irons)

    p1_frames_faulty = []
    for _ in range(11):
        frame_data: Dict[str, Any] = { # Changed FramePoseData to Dict for this test
            "left_shoulder": _make_kp(-0.2, 1.4, -0.3), # Simulating more bend by moving shoulders forward in Z
            "right_shoulder": _make_kp(0.2, 1.4, -0.3),
            "left_hip": _make_kp(-0.15, 0.9, 0), "right_hip": _make_kp(0.15, 0.9, 0),
            # More knee flex: knee further forward or lower
            "left_knee": _make_kp(-0.18, 0.4, 0.05), "right_knee": _make_kp(0.18, 0.45, 0), # Left knee more flexed
            "left_ankle": _make_kp(-0.25, 0.1, 0), "right_ankle": _make_kp(0.15, 0.1, 0), # Shifted left ankle for weight dist
        }
        p1_frames_faulty.append(frame_data)

    # P4 Data: Shoulders rotated only ~70 degrees.
    # For 70 deg rotation (approx): ls_x ~ -0.2*cos(70), ls_z ~ -0.2*sin(70)
    # cos(70)~0.34, sin(70)~0.94. ls_x ~ -0.068, ls_z ~ -0.188
    # rs_x ~ 0.068, rs_z ~ 0.188
    p4_frames_faulty = []
    for _ in range(10):
        frame_data_p4: Dict[str, Any] = {
            "left_shoulder": _make_kp(-0.07, 1.4, -0.19), "right_shoulder": _make_kp(0.07, 1.4, 0.19),
            "left_hip": _make_kp(-0.1, 0.9, -0.08), "right_hip": _make_kp(0.1, 0.9, -0.08),
            "left_knee": _make_kp(-0.18, 0.5, 0), "right_knee": _make_kp(0.18, 0.5, 0),
            "left_ankle": _make_kp(-0.2, 0.1, 0), "right_ankle": _make_kp(0.2, 0.1, 0),
             # Placeholder for wrist keypoints if LeadWristAngleP4 KPI was real
            "left_wrist": _make_kp(-0.3, 1.5, -0.4), # Example of a cupped wrist position
        }
        p4_frames_faulty.append(frame_data_p4)

    sample_swing_faulty_input: SwingVideoAnalysisInput = {
        "session_id": "test_faulty_swing_001",
        "user_id": "test_user_faulty",
        "club_used": "7-Iron", # Important for weight distribution check
        "frames": p1_frames_faulty + p4_frames_faulty,
        "p_system_classification": [
            {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
            {"phase_name": "P4", "start_frame_index": 11, "end_frame_index": 20}
        ],
        "video_fps": 60.0
    }

    print("--- Testing Fault Detection ---")
    # 1. Extract KPIs using the faulty swing data
    print("Extracting KPIs for faulty swing...")
    kpis_from_faulty_swing = extract_all_kpis(sample_swing_faulty_input)

    # Manually add a placeholder KPI for testing the "Cupped Wrist" rule,
    # as LeadWristAngleP4 is not implemented in kpi_extraction.py
    # This simulates that the KPI was extracted with a value indicating a fault.
    placeholder_cupped_wrist_kpi = BiomechanicalKPI(
        p_position="P4",
        kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4, # Matches matrix
        value=25.0, # Degrees of extension (cupping), rule FD005 triggers if > 15.0
        unit="degrees",
        ideal_range=(-5.0, 5.0), # Flat to slightly bowed
        notes="Placeholder KPI for cupped wrist."
    )
    kpis_from_faulty_swing.append(placeholder_cupped_wrist_kpi)

    print(f"\nKPIs extracted ({len(kpis_from_faulty_swing)} total):")
    for kpi in kpis_from_faulty_swing:
        # Type check for value before formatting, robustly handle non-numeric if any slip through
        val_str = f"{kpi['value']:.1f}" if isinstance(kpi['value'], (int, float)) else str(kpi['value'])
        print(f"  - {kpi['kpi_name']} ({kpi['p_position']}): {val_str} {kpi['unit']}")


    # 2. Check for faults
    print("\nChecking for faults...")
    identified_faults = check_swing_faults(sample_swing_faulty_input, kpis_from_faulty_swing)

    if identified_faults:
        print(f"\n--- {len(identified_faults)} Fault(s) Detected: ---")
        for fault in identified_faults:
            print(f"  Fault ID: {fault['fault_id']}")
            print(f"  Name: {fault['fault_name']}")
            print(f"  Description: {fault['description']}")
            print(f"  P-Positions: {fault['p_positions_implicated']}")
            print(f"  Severity: {fault['severity']}")
            print(f"  LLM Key: {fault['llm_prompt_template_key']}")
            for dev in fault['kpi_deviations']:
                print(f"    - Deviation: {dev['kpi_name']}")
                print(f"      Observed: {dev['observed_value']}")
                print(f"      {dev['ideal_value_or_range']}")
            print("-" * 20)
    else:
        print("\nNo faults detected with the current rules and data.")

    # Test with non-faulty data (using the test data from kpi_extraction)
    from kpi_extraction import sample_swing_input as non_faulty_swing_data
    print("\n--- Testing with Non-Faulty Swing Data ---")
    print("Extracting KPIs for non-faulty swing...")
    kpis_from_non_faulty_swing = extract_all_kpis(non_faulty_swing_data)
    # Add placeholder lead wrist KPI that is NOT faulty
    placeholder_good_wrist_kpi = BiomechanicalKPI(
        p_position="P4", kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4, value=0.0, unit="degrees",
        ideal_range=(-5.0, 5.0), notes="Placeholder KPI for good wrist."
    )
    kpis_from_non_faulty_swing.append(placeholder_good_wrist_kpi)

    print(f"\nKPIs extracted ({len(kpis_from_non_faulty_swing)} total):")
    # for kpi in kpis_from_non_faulty_swing:
    #     val_str = f"{kpi['value']:.1f}" if isinstance(kpi['value'], (int, float)) else str(kpi['value'])
    #     print(f"  - {kpi['kpi_name']} ({kpi['p_position']}): {val_str} {kpi['unit']}")

    print("\nChecking for faults (non-faulty data)...")
    non_faulty_results = check_swing_faults(non_faulty_swing_data, kpis_from_non_faulty_swing)
    if non_faulty_results:
        print(f"\n--- {len(non_faulty_results)} Fault(s) Detected (EXPECTED NONE OR FEW): ---")
        for fault in non_faulty_results:
            print(f"  Fault ID: {fault['fault_id']} - Name: {fault['fault_name']}")
            for dev in fault['kpi_deviations']:
                 print(f"    - Deviation: {dev['kpi_name']}, Observed: {dev['observed_value']}, {dev['ideal_value_or_range']}")
    else:
        print("\nNo faults detected with non-faulty data, as expected for most rules.")

"""
