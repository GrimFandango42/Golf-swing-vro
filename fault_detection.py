"""
Module for Enhanced Golf Swing Fault Detection Logic with Club-Specific Rules.

This module is responsible for identifying common golf swing faults by comparing
extracted Biomechanical KPIs (from `kpi_extraction.py`) against dynamically
selected, club-specific fault detection rules.

The enhanced fault detection system includes:
- Club-specific fault detection matrices for different club types (Driver, Irons, Wedges)
- Dynamic rule selection based on the club_used field in input data
- Club-specific ideal ranges and thresholds for each biomechanical metric
- Sophisticated severity calculations with club-specific context
- Club-specific common faults and their detection patterns

Key Features:
- Driver: Optimized for upward angle of attack, specific weight distribution patterns
- Irons: Configured for steeper angle of attack, forward ball position requirements
- Wedges: Tuned for even steeper approach angles, centered ball position
- Dynamic fault matrix selection based on club type
- Enhanced severity scoring with club-specific context

The core function `check_swing_faults` now dynamically selects appropriate rules
based on the club_used field and applies club-specific thresholds and expectations.
"""
from typing import List, Dict, Any, Optional

from data_structures import (
    SwingVideoAnalysisInput,
    BiomechanicalKPI,
    DetectedFault,
    FaultDiagnosisMatrixEntry, # Re-defined here for clarity, or could be imported if centralized
    KPIDeviation
)
# from kpi_extraction import EXPECTED_KEYPOINTS # For reference if needed - removed to avoid numpy dependency

# Re-defining FaultDiagnosisMatrixEntry for local use or it could be a shared model
# For simplicity, assuming it's defined as in data_structures.py
# from data_structures import FaultDiagnosisMatrixEntry

# --- Fault Diagnosis Matrix ---
# This matrix defines the rules for detecting faults.
# In a real application, this would likely be loaded from a configuration file (JSON, YAML) or database.

# --- Club Type Classification ---

def classify_club_type(club_used: str) -> str:
    """
    Classifies the club into one of three main categories for fault detection.
    
    Args:
        club_used: String description of the club (e.g., "Driver", "7-Iron", "Sand Wedge")
    
    Returns:
        Club type category: "driver", "iron", or "wedge"
    """
    club_lower = club_used.lower().strip()
    
    # Driver classification
    if any(keyword in club_lower for keyword in ["driver", "1-wood", "1 wood"]):
        return "driver"
    
    # Wedge classification
    wedge_keywords = ["wedge", "sand", "lob", "gap", "pitching", "pw", "sw", "lw", "gw"]
    if any(keyword in club_lower for keyword in wedge_keywords):
        return "wedge"
    
    # Iron classification (includes hybrids and fairway woods as similar swing characteristics)
    iron_keywords = ["iron", "hybrid", "wood", "utility"]
    if any(keyword in club_lower for keyword in iron_keywords) or any(char.isdigit() for char in club_lower):
        return "iron"
    
    # Default to iron if unclear
    return "iron"

# --- Club-Specific Constants ---

# Club-specific weight distribution targets (lead foot percentage)
WEIGHT_DISTRIBUTION_TARGETS = {
    "driver": {"ideal": 40.0, "range": (35.0, 45.0)},    # More weight on trail foot for upward attack
    "iron": {"ideal": 50.0, "range": (45.0, 55.0)},      # Balanced for neutral attack
    "wedge": {"ideal": 55.0, "range": (50.0, 60.0)}      # Slightly forward for downward attack
}

# Club-specific hip hinge angle targets (degrees from vertical)
HIP_HINGE_TARGETS = {
    "driver": {"ideal": 35.0, "range": (30.0, 40.0)},    # Less hip hinge for driver
    "iron": {"ideal": 37.5, "range": (32.5, 42.5)},      # Standard hip hinge
    "wedge": {"ideal": 40.0, "range": (35.0, 45.0)}      # More hip hinge for control
}

# Club-specific shoulder rotation targets at P4 (degrees)
SHOULDER_ROTATION_TARGETS = {
    "driver": {"minimum": 85.0, "ideal": 95.0},           # Full turn for power
    "iron": {"minimum": 80.0, "ideal": 90.0},             # Good turn for consistency
    "wedge": {"minimum": 75.0, "ideal": 85.0}             # Shorter swing for control
}

# Club-specific knee flexion targets (degrees)
KNEE_FLEX_TARGETS = {
    "driver": {"range": (15.0, 25.0)},                    # Athletic posture
    "iron": {"range": (15.0, 25.0)},                      # Standard range
    "wedge": {"range": (18.0, 28.0)}                      # Slightly more flex for control
}

# Club-specific lead wrist angle targets at P4 (degrees - positive = cupped/extended)
LEAD_WRIST_TARGETS = {
    "driver": {"max_cupping": 8.0},                       # Less tolerance for cupping
    "iron": {"max_cupping": 10.0},                        # Standard tolerance
    "wedge": {"max_cupping": 12.0}                        # More tolerance for feel shots
}

# Placeholders for KPIs not yet fully implemented in kpi_extraction.py
# These are used to demonstrate how the fault matrix would handle them.
PLACEHOLDER_LEAD_WRIST_ANGLE_P4 = "Lead Wrist Angle at P4"
PLACEHOLDER_HIP_LATERAL_SWAY_P4 = "Hip Lateral Sway at P4" # Positive for sway away from target (RH golfer)
PLACEHOLDER_SPINE_ANGLE_REVERSE_P4 = "Reverse Spine Angle at P4" # Positive if leaning towards target

# --- Dynamic Fault Matrix Generation ---

def generate_club_specific_fault_matrix(club_type: str) -> List[FaultDiagnosisMatrixEntry]:
    """
    Generates a club-specific fault detection matrix based on the club type.
    
    Args:
        club_type: The classified club type ("driver", "iron", or "wedge")
    
    Returns:
        List of fault detection matrix entries tailored for the specific club type
    """
    matrix = []
    
    # Club-specific hip hinge rule at P1
    hip_hinge_targets = HIP_HINGE_TARGETS[club_type]
    matrix.append({
        "entry_id": f"FD001_{club_type.upper()}",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Hip Hinge Angle (Spine from Vertical)",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": hip_hinge_targets["range"][0], 
                           "upper_bound": hip_hinge_targets["range"][1]},
        "fault_to_report_id": f"IMPROPER_POSTURE_HIP_HINGE_P1_{club_type.upper()}",
        "fault_name": f"Improper Hip Hinge at Address ({club_type.title()})",
        "fault_description": f"Your posture at address with a {club_type} shows incorrect forward tilt from the hips. "
                           f"For {club_type}s, optimal hip hinge should be between {hip_hinge_targets['range'][0]:.1f}° "
                           f"and {hip_hinge_targets['range'][1]:.1f}° for proper balance and swing mechanics.",
        "llm_prompt_template_key": f"IMPROPER_HIP_HINGE_P1_{club_type.upper()}_PROMPT",
        "club_type": club_type,
        "severity_levels": [
            {"threshold_from_ideal_percent": 15, "severity": 0.3},
            {"threshold_from_ideal_percent": 30, "severity": 0.6},
            {"threshold_from_ideal_percent": 50, "severity": 0.9},
        ]
    })
    
    # Club-specific knee flexion rules at P1
    knee_targets = KNEE_FLEX_TARGETS[club_type]
    for leg_side in ["Left", "Right"]:
        matrix.append({
            "entry_id": f"FD002_{leg_side[0]}_{club_type.upper()}",
            "p_position_focused": "P1",
            "biomechanical_metric_checked": f"{leg_side} Knee Flexion Angle",
            "condition_type": "outside_range",
            "condition_values": {"lower_bound": knee_targets["range"][0], 
                               "upper_bound": knee_targets["range"][1]},
            "fault_to_report_id": f"IMPROPER_KNEE_FLEX_P1_{leg_side.upper()}_{club_type.upper()}",
            "fault_name": f"Improper {leg_side} Knee Flex at Address ({club_type.title()})",
            "fault_description": f"Your {leg_side.lower()} knee flexion at address with a {club_type} is outside "
                               f"the optimal range. For {club_type}s, proper knee flex should be between "
                               f"{knee_targets['range'][0]:.1f}° and {knee_targets['range'][1]:.1f}° to maintain "
                               f"athletic posture and enable proper weight transfer.",
            "llm_prompt_template_key": f"IMPROPER_KNEE_FLEX_P1_{leg_side.upper()}_{club_type.upper()}_PROMPT",
            "club_type": club_type,
        })
    
    # Club-specific weight distribution rule at P1
    weight_targets = WEIGHT_DISTRIBUTION_TARGETS[club_type]
    matrix.append({
        "entry_id": f"FD003_{club_type.upper()}",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Estimated Weight Distribution (Lead Foot %)",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": weight_targets["range"][0], 
                           "upper_bound": weight_targets["range"][1]},
        "fault_to_report_id": f"IMPROPER_WEIGHT_DISTRIBUTION_P1_{club_type.upper()}",
        "fault_name": f"Improper Weight Distribution at Address ({club_type.title()})",
        "fault_description": f"Your weight distribution at address with a {club_type} is not optimal. "
                           f"For {club_type}s, weight should be distributed with {weight_targets['ideal']:.0f}% "
                           f"on the lead foot (range: {weight_targets['range'][0]:.0f}%-{weight_targets['range'][1]:.0f}%). "
                           f"This setup promotes the proper angle of attack for this club type.",
        "llm_prompt_template_key": f"IMPROPER_WEIGHT_DIST_P1_{club_type.upper()}_PROMPT",
        "club_type": club_type,
    })
    
    # Club-specific shoulder rotation rule at P4
    shoulder_targets = SHOULDER_ROTATION_TARGETS[club_type]
    matrix.append({
        "entry_id": f"FD004_{club_type.upper()}",
        "p_position_focused": "P4",
        "biomechanical_metric_checked": "Shoulder Rotation at P4 (relative to Address)",
        "condition_type": "less_than",
        "condition_values": {"threshold": shoulder_targets["minimum"]},
        "fault_to_report_id": f"INSUFFICIENT_SHOULDER_TURN_P4_{club_type.upper()}",
        "fault_name": f"Insufficient Shoulder Turn at Top ({club_type.title()})",
        "fault_description": f"Your shoulder rotation at the top of the backswing with a {club_type} appears restricted. "
                           f"For {club_type}s, a minimum of {shoulder_targets['minimum']:.0f}° shoulder turn "
                           f"(ideally {shoulder_targets['ideal']:.0f}°) is recommended for optimal power generation "
                           f"and proper swing sequence.",
        "llm_prompt_template_key": f"INSUFFICIENT_SHOULDER_TURN_P4_{club_type.upper()}_PROMPT",
        "club_type": club_type,
    })
    
    # Club-specific lead wrist rule at P4
    wrist_targets = LEAD_WRIST_TARGETS[club_type]
    matrix.append({
        "entry_id": f"FD005_{club_type.upper()}",
        "p_position_focused": "P4",
        "biomechanical_metric_checked": PLACEHOLDER_LEAD_WRIST_ANGLE_P4,
        "condition_type": "greater_than",
        "condition_values": {"threshold": wrist_targets["max_cupping"]},
        "fault_to_report_id": f"CUPPED_WRIST_AT_TOP_P4_{club_type.upper()}",
        "fault_name": f"Cupped Lead Wrist at Top ({club_type.title()})",
        "fault_description": f"Your lead wrist is excessively cupped at the top of the backswing with a {club_type}. "
                           f"For {club_type}s, the lead wrist should be no more than {wrist_targets['max_cupping']:.0f}° "
                           f"cupped to maintain proper clubface control and prevent slices or pushes.",
        "llm_prompt_template_key": f"CUPPED_WRIST_P4_{club_type.upper()}_PROMPT",
        "club_type": club_type,
    })
    
    # Universal faults that apply to all club types with same thresholds
    universal_faults = [
        {
            "entry_id": f"FD006_{club_type.upper()}",
            "p_position_focused": "P4",
            "biomechanical_metric_checked": PLACEHOLDER_HIP_LATERAL_SWAY_P4,
            "condition_type": "greater_than",
            "condition_values": {"threshold": 0.15},  # 15cm lateral sway
            "fault_to_report_id": f"HIP_SWAY_BACKSWING_{club_type.upper()}",
            "fault_name": f"Hip Sway During Backswing ({club_type.title()})",
            "fault_description": f"Your hips are swaying laterally during the backswing with your {club_type}, "
                               f"rather than rotating around a stable center. This can lead to inconsistent contact "
                               f"and reduced power generation.",
            "llm_prompt_template_key": f"HIP_SWAY_BACKSWING_{club_type.upper()}_PROMPT",
            "club_type": club_type,
        },
        {
            "entry_id": f"FD007_{club_type.upper()}",
            "p_position_focused": "P4",
            "biomechanical_metric_checked": PLACEHOLDER_SPINE_ANGLE_REVERSE_P4,
            "condition_type": "greater_than",
            "condition_values": {"threshold": 10.0},  # 10° reverse spine
            "fault_to_report_id": f"REVERSE_SPINE_ANGLE_P4_{club_type.upper()}",
            "fault_name": f"Reverse Spine Angle at Top ({club_type.title()})",
            "fault_description": f"At the top of your backswing with a {club_type}, your upper body is tilting "
                               f"towards the target (reverse spine angle). This common fault can cause inconsistent "
                               f"contact and reduce power while increasing injury risk.",
            "llm_prompt_template_key": f"REVERSE_SPINE_ANGLE_P4_{club_type.upper()}_PROMPT",
            "club_type": club_type,
        }
    ]
    
    matrix.extend(universal_faults)
    
    # Add club-specific advanced fault rules
    if club_type == "driver":
        matrix.extend(_get_driver_specific_faults())
    elif club_type == "wedge":
        matrix.extend(_get_wedge_specific_faults())
    else:  # iron
        matrix.extend(_get_iron_specific_faults())
    
    return matrix

def _get_driver_specific_faults() -> List[FaultDiagnosisMatrixEntry]:
    """Returns driver-specific fault detection rules."""
    return [
        {
            "entry_id": "FD_DRIVER_001",
            "p_position_focused": "P1",
            "biomechanical_metric_checked": "Spine Angle at P1",
            "condition_type": "greater_than",
            "condition_values": {"threshold": 25.0},  # More upright for driver
            "fault_to_report_id": "EXCESSIVE_SPINE_TILT_DRIVER_P1",
            "fault_name": "Excessive Forward Spine Tilt (Driver)",
            "fault_description": "Your spine angle at address with the driver shows excessive forward tilt. "
                               "Drivers benefit from a more upright spine angle to promote an upward angle of attack.",
            "llm_prompt_template_key": "EXCESSIVE_SPINE_TILT_DRIVER_P1_PROMPT",
            "club_type": "driver",
        }
    ]

def _get_iron_specific_faults() -> List[FaultDiagnosisMatrixEntry]:
    """Returns iron-specific fault detection rules."""
    return [
        {
            "entry_id": "FD_IRON_001",
            "p_position_focused": "P1",
            "biomechanical_metric_checked": "Estimated Weight Distribution (Lead Foot %)",
            "condition_type": "less_than",
            "condition_values": {"threshold": 42.0},  # Too much weight on trail foot for irons
            "fault_to_report_id": "EXCESSIVE_TRAIL_WEIGHT_IRON_P1",
            "fault_name": "Excessive Trail Foot Weight (Iron)",
            "fault_description": "Your weight distribution shows too much weight on the trail foot for iron play. "
                               "Irons require more balanced or slightly forward weight for proper ball-first contact.",
            "llm_prompt_template_key": "EXCESSIVE_TRAIL_WEIGHT_IRON_P1_PROMPT",
            "club_type": "iron",
        }
    ]

def _get_wedge_specific_faults() -> List[FaultDiagnosisMatrixEntry]:
    """Returns wedge-specific fault detection rules."""
    return [
        {
            "entry_id": "FD_WEDGE_001",
            "p_position_focused": "P4",
            "biomechanical_metric_checked": "Shoulder Rotation at P4 (relative to Address)",
            "condition_type": "greater_than",
            "condition_values": {"threshold": 95.0},  # Too much turn for wedge control
            "fault_to_report_id": "EXCESSIVE_SHOULDER_TURN_WEDGE_P4",
            "fault_name": "Excessive Shoulder Turn (Wedge)",
            "fault_description": "Your shoulder rotation at the top is excessive for wedge play. "
                               "Wedges benefit from a more controlled, shorter backswing for better distance control.",
            "llm_prompt_template_key": "EXCESSIVE_SHOULDER_TURN_WEDGE_P4_PROMPT",
            "club_type": "wedge",
        }
    ]

# Legacy matrix kept for backwards compatibility
FAULT_DIAGNOSIS_MATRIX: List[FaultDiagnosisMatrixEntry] = [
    {
        "entry_id": "FD001",
        "p_position_focused": "P1",
        "biomechanical_metric_checked": "Hip Hinge Angle (Spine from Vertical)",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": 30.0, "upper_bound": 45.0},
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
]

# --- Enhanced Fault Detection Functions ---

def _calculate_club_specific_severity(
    kpi_value: float, 
    rule: FaultDiagnosisMatrixEntry, 
    club_type: str
) -> Optional[float]:
    """
    Calculates severity based on club-specific context and deviation from ideal values.
    
    Args:
        kpi_value: The observed KPI value
        rule: The fault detection rule being evaluated
        club_type: The type of club being used ("driver", "iron", "wedge")
    
    Returns:
        Severity score between 0.0 and 1.0, or None if no severity calculation applies
    """
    if "severity_levels" in rule and rule["severity_levels"]:
        return _calculate_severity_from_levels(kpi_value, rule)
    
    # Enhanced club-specific severity calculation
    condition_type = rule["condition_type"]
    cv = rule["condition_values"]
    
    # Calculate deviation based on condition type
    deviation_percent = 0.0
    base_severity = 0.0
    
    if condition_type == "outside_range":
        lower_bound = cv.get("lower_bound", 0)
        upper_bound = cv.get("upper_bound", 0)
        ideal_center = (lower_bound + upper_bound) / 2
        range_width = upper_bound - lower_bound
        
        if kpi_value < lower_bound:
            deviation_percent = abs(kpi_value - lower_bound) / range_width * 100
        elif kpi_value > upper_bound:
            deviation_percent = abs(kpi_value - upper_bound) / range_width * 100
        else:
            return None  # Within range, no fault
            
    elif condition_type in ["less_than", "greater_than"]:
        threshold = cv.get("threshold", 0)
        deviation_percent = abs(kpi_value - threshold) / max(abs(threshold), 1) * 100
    
    # Club-specific severity modifiers
    severity_modifier = 1.0
    if club_type == "driver":
        # Driver faults are often more critical for distance
        if "weight" in rule["biomechanical_metric_checked"].lower():
            severity_modifier = 1.2  # Weight distribution more critical for driver
        elif "shoulder" in rule["biomechanical_metric_checked"].lower():
            severity_modifier = 1.1  # Shoulder turn important for power
    elif club_type == "wedge":
        # Wedge faults affect precision more than power
        if "wrist" in rule["biomechanical_metric_checked"].lower():
            severity_modifier = 0.9  # Slightly more forgiving on wrist position
        elif "hip" in rule["biomechanical_metric_checked"].lower():
            severity_modifier = 1.1  # Hip control critical for short game
    
    # Base severity calculation
    if deviation_percent > 50:
        base_severity = 0.9
    elif deviation_percent > 30:
        base_severity = 0.7
    elif deviation_percent > 15:
        base_severity = 0.5
    elif deviation_percent > 5:
        base_severity = 0.3
    else:
        base_severity = 0.1
    
    # Apply club-specific modifier and clamp to [0, 1]
    final_severity = min(1.0, base_severity * severity_modifier)
    
    return final_severity if final_severity > 0.05 else None

def _calculate_severity_from_levels(kpi_value: float, rule: FaultDiagnosisMatrixEntry) -> Optional[float]:
    """
    Calculates severity using the rule's defined severity levels.
    Legacy function for backwards compatibility.
    """
    if "severity_levels" not in rule or not rule["severity_levels"]:
        return None
    
    # Simple implementation - this would need enhancement for percentage-based calculations
    ideal_lower = rule["condition_values"].get("lower_bound")
    ideal_upper = rule["condition_values"].get("upper_bound")
    
    deviation_abs = 0
    if rule["condition_type"] == "outside_range" and ideal_lower is not None and ideal_upper is not None:
        if kpi_value < ideal_lower:
            deviation_abs = ideal_lower - kpi_value
        elif kpi_value > ideal_upper:
            deviation_abs = kpi_value - ideal_upper
    elif rule["condition_type"] == "less_than":
        threshold = rule["condition_values"].get("threshold", 0)
        if kpi_value < threshold:
            deviation_abs = threshold - kpi_value
    elif rule["condition_type"] == "greater_than":
        threshold = rule["condition_values"].get("threshold", 0)
        if kpi_value > threshold:
            deviation_abs = kpi_value - threshold
    
    # Simple mapping for backwards compatibility
    if deviation_abs > 10: 
        return 0.8
    elif deviation_abs > 5: 
        return 0.5
    elif deviation_abs > 1: 
        return 0.2
    
    return None


def check_swing_faults(
    swing_input: SwingVideoAnalysisInput,
    extracted_kpis: List[BiomechanicalKPI]
) -> List[DetectedFault]:
    """
    Analyzes extracted KPIs against club-specific fault detection rules to identify swing faults.
    
    This enhanced version dynamically selects fault detection rules based on the club type
    and applies club-specific thresholds, ideal ranges, and severity calculations.

    Args:
        swing_input: The original input data for the swing analysis.
        extracted_kpis: A list of biomechanical KPIs calculated from the swing.

    Returns:
        A list of DetectedFault objects representing identified faults.
    """
    detected_faults: List[DetectedFault] = []
    kpis_map: Dict[str, BiomechanicalKPI] = {kpi['kpi_name']: kpi for kpi in extracted_kpis}
    
    # Determine club type and generate appropriate fault matrix
    club_type = classify_club_type(swing_input.get('club_used', 'iron'))
    fault_matrix = generate_club_specific_fault_matrix(club_type)
    
    print(f"Debug: Using {club_type} fault detection rules for club '{swing_input.get('club_used', 'unknown')}'")
    print(f"Debug: Generated {len(fault_matrix)} club-specific fault detection rules")

    for rule in fault_matrix:
        kpi_name_to_check = rule["biomechanical_metric_checked"]
        kpi = kpis_map.get(kpi_name_to_check)

        if not kpi:
            # Skip rule if required KPI is missing - this is normal for placeholder KPIs
            continue

        kpi_value = kpi['value']
        
        # Ensure kpi_value is numeric for comparisons
        if not isinstance(kpi_value, (int, float)):
            continue

        # Evaluate fault condition
        fault_detected_for_rule = _evaluate_fault_condition(kpi_value, rule)

        if fault_detected_for_rule:
            # Generate descriptive text for ideal values
            ideal_val_desc = _generate_ideal_value_description(rule, kpi['unit'])
            
            kpi_deviation = KPIDeviation(
                kpi_name=kpi_name_to_check,
                observed_value=f"{kpi_value:.1f} {kpi['unit']}",
                ideal_value_or_range=f"Ideal: {ideal_val_desc}",
                p_position=kpi['p_position']
            )

            # Calculate club-specific severity
            severity_score = _calculate_club_specific_severity(float(kpi_value), rule, club_type)

            fault = DetectedFault(
                fault_id=rule["fault_to_report_id"],
                fault_name=rule.get("fault_name", "Unknown Fault Name"),
                p_positions_implicated=[kpi['p_position']],
                description=rule.get("fault_description", "No description provided."),
                kpi_deviations=[kpi_deviation],
                llm_prompt_template_key=rule["llm_prompt_template_key"],
                severity=severity_score
            )
            detected_faults.append(fault)

    return detected_faults

def _evaluate_fault_condition(kpi_value: float, rule: FaultDiagnosisMatrixEntry) -> bool:
    """
    Evaluates whether a KPI value meets the fault condition defined in the rule.
    
    Args:
        kpi_value: The observed KPI value
        rule: The fault detection rule
    
    Returns:
        True if the fault condition is met, False otherwise
    """
    condition_type = rule["condition_type"]
    cv = rule["condition_values"]
    
    if condition_type == "outside_range":
        if "lower_bound" in cv and "upper_bound" in cv:
            return not (cv["lower_bound"] <= kpi_value <= cv["upper_bound"])
    elif condition_type == "less_than":
        if "threshold" in cv:
            return kpi_value < cv["threshold"]
    elif condition_type == "greater_than":
        if "threshold" in cv:
            return kpi_value > cv["threshold"]
    elif condition_type == "equals":
        if "value" in cv:
            return abs(kpi_value - cv["value"]) < 0.01  # Small tolerance for float comparison
    elif condition_type == "not_equals":
        if "value" in cv:
            return abs(kpi_value - cv["value"]) >= 0.01
    
    return False

def _generate_ideal_value_description(rule: FaultDiagnosisMatrixEntry, unit: str) -> str:
    """
    Generates a human-readable description of the ideal value range for a rule.
    
    Args:
        rule: The fault detection rule
        unit: The unit of measurement for the KPI
    
    Returns:
        A descriptive string of the ideal value or range
    """
    condition_type = rule["condition_type"]
    cv = rule["condition_values"]
    
    if condition_type == "outside_range":
        return f"between {cv['lower_bound']:.1f} and {cv['upper_bound']:.1f} {unit}"
    elif condition_type == "less_than":
        return f"greater than or equal to {cv['threshold']:.1f} {unit}"
    elif condition_type == "greater_than":
        return f"less than or equal to {cv['threshold']:.1f} {unit}"
    elif condition_type == "equals":
        return f"approximately {cv['value']:.1f} {unit}"
    elif condition_type == "not_equals":
        return f"not equal to {cv['value']:.1f} {unit}"
    
    return "within acceptable parameters"


if __name__ == '__main__':
    # Import KPI extraction for testing if available, otherwise skip
    try:
        from kpi_extraction import extract_all_kpis
        KPI_EXTRACTION_AVAILABLE = True
    except ImportError:
        print("Warning: KPI extraction module not available (numpy dependency). Running simplified test.")
        KPI_EXTRACTION_AVAILABLE = False

    print("=== Enhanced Club-Specific Fault Detection Testing ===\n")
    
    # Test club classification
    print("--- Testing Club Classification ---")
    test_clubs = ["Driver", "7-Iron", "Sand Wedge", "Pitching Wedge", "3-Wood", "5-Hybrid"]
    for club in test_clubs:
        club_type = classify_club_type(club)
        print(f"  {club} -> {club_type}")
    print()
    
    # Test fault matrix generation
    print("--- Testing Club-Specific Matrix Generation ---")
    for club_type in ["driver", "iron", "wedge"]:
        matrix = generate_club_specific_fault_matrix(club_type)
        print(f"{club_type.title()}: {len(matrix)} rules generated")
    print()
    
    if not KPI_EXTRACTION_AVAILABLE:
        print("Skipping full integration test due to missing dependencies.")
        print("Run 'python test_club_specific_faults.py' for comprehensive testing.")
        exit(0)

    # --- Create dummy SwingVideoAnalysisInput for testing different club types ---
    def _make_kp(x,y,z): return {"x":x, "y":y, "z":z, "visibility":1.0}

    # P1 Data with various faults for testing
    p1_frames_faulty = []
    for _ in range(11):
        frame_data: Dict[str, Any] = {
            "left_shoulder": _make_kp(-0.2, 1.4, -0.3),  # Simulating hip hinge issues
            "right_shoulder": _make_kp(0.2, 1.4, -0.3),
            "left_hip": _make_kp(-0.15, 0.9, 0), 
            "right_hip": _make_kp(0.15, 0.9, 0),
            "left_knee": _make_kp(-0.18, 0.4, 0.05),     # Excessive knee flex
            "right_knee": _make_kp(0.18, 0.45, 0),
            "left_ankle": _make_kp(-0.25, 0.1, 0),       # Weight distribution issues
            "right_ankle": _make_kp(0.15, 0.1, 0),
        }
        p1_frames_faulty.append(frame_data)

    # P4 Data with restricted shoulder turn
    p4_frames_faulty = []
    for _ in range(10):
        frame_data_p4: Dict[str, Any] = {
            "left_shoulder": _make_kp(-0.07, 1.4, -0.19),  # ~70 degree turn
            "right_shoulder": _make_kp(0.07, 1.4, 0.19),
            "left_hip": _make_kp(-0.1, 0.9, -0.08), 
            "right_hip": _make_kp(0.1, 0.9, -0.08),
            "left_knee": _make_kp(-0.18, 0.5, 0), 
            "right_knee": _make_kp(0.18, 0.5, 0),
            "left_ankle": _make_kp(-0.2, 0.1, 0), 
            "right_ankle": _make_kp(0.2, 0.1, 0),
            "left_wrist": _make_kp(-0.3, 1.5, -0.4),      # Cupped wrist position
        }
        p4_frames_faulty.append(frame_data_p4)

    # Test with different club types
    test_club_types = ["Driver", "7-Iron", "Sand Wedge"]
    
    for test_club in test_club_types:
        print(f"--- Testing {test_club} Fault Detection ---")
        
        sample_swing_input: SwingVideoAnalysisInput = {
            "session_id": f"test_swing_{test_club.lower().replace('-', '_')}",
            "user_id": "test_user",
            "club_used": test_club,
            "frames": p1_frames_faulty + p4_frames_faulty,
            "p_system_classification": [
                {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
                {"phase_name": "P4", "start_frame_index": 11, "end_frame_index": 20}
            ],
            "video_fps": 60.0
        }

        # Extract KPIs
        kpis_from_swing = extract_all_kpis(sample_swing_input)

        # Add placeholder KPIs for testing club-specific rules
        placeholder_kpis = [
            BiomechanicalKPI(
                p_position="P4",
                kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4,
                value=15.0,  # Will trigger different thresholds based on club type
                unit="degrees",
                ideal_range=(-5.0, 5.0),
                notes="Placeholder KPI for wrist angle testing."
            ),
            BiomechanicalKPI(
                p_position="P4",
                kpi_name=PLACEHOLDER_HIP_LATERAL_SWAY_P4,
                value=0.20,  # 20cm sway - should trigger fault for all clubs
                unit="meters",
                ideal_range=(0.0, 0.10),
                notes="Placeholder KPI for hip sway testing."
            ),
            BiomechanicalKPI(
                p_position="P4",
                kpi_name=PLACEHOLDER_SPINE_ANGLE_REVERSE_P4,
                value=12.0,  # Reverse spine angle - should trigger fault
                unit="degrees",
                ideal_range=(-5.0, 5.0),
                notes="Placeholder KPI for reverse spine testing."
            )
        ]
        
        kpis_from_swing.extend(placeholder_kpis)

        print(f"\nKPIs extracted for {test_club} ({len(kpis_from_swing)} total):")
        for kpi in kpis_from_swing:
            val_str = f"{kpi['value']:.1f}" if isinstance(kpi['value'], (int, float)) else str(kpi['value'])
            print(f"  - {kpi['kpi_name']} ({kpi['p_position']}): {val_str} {kpi['unit']}")

        # Check for faults with club-specific rules
        print(f"\nChecking for faults with {test_club}...")
        identified_faults = check_swing_faults(sample_swing_input, kpis_from_swing)

        if identified_faults:
            print(f"\n--- {len(identified_faults)} Club-Specific Fault(s) Detected: ---")
            for fault in identified_faults:
                print(f"  Fault ID: {fault['fault_id']}")
                print(f"  Name: {fault['fault_name']}")
                print(f"  Severity: {fault['severity']:.2f}" if fault['severity'] else "No severity calculated")
                print(f"  Description: {fault['description'][:100]}...")
                for dev in fault['kpi_deviations']:
                    print(f"    - {dev['kpi_name']}: {dev['observed_value']} | {dev['ideal_value_or_range']}")
                print("-" * 40)
        else:
            print(f"\nNo faults detected for {test_club} (unexpected with test data).")
        
        print("\n" + "="*60 + "\n")

    # Demonstrate matrix generation for different club types
    print("--- Club-Specific Matrix Generation Demo ---")
    for club_type in ["driver", "iron", "wedge"]:
        matrix = generate_club_specific_fault_matrix(club_type)
        print(f"\n{club_type.title()} Matrix: {len(matrix)} rules generated")
        print("Sample rules:")
        for i, rule in enumerate(matrix[:3]):  # Show first 3 rules
            print(f"  {i+1}. {rule['fault_name']} (ID: {rule['entry_id']})")
        if len(matrix) > 3:
            print(f"  ... and {len(matrix) - 3} more rules")

    print("\n=== Enhanced Fault Detection Testing Complete ===")

"""
Enhanced Club-Specific Fault Detection Summary:

Key Enhancements Made:
1. Club Type Classification: Automatic classification of clubs into driver/iron/wedge categories
2. Dynamic Fault Matrix Generation: Club-specific rules with appropriate thresholds
3. Club-Specific Targets: Different ideal ranges for each club type
4. Enhanced Severity Calculation: Club-specific severity modifiers
5. Comprehensive Rule Coverage: Basic rules + club-specific advanced rules

Club-Specific Features:
- Driver: Optimized for power generation, upward attack angle
- Iron: Balanced approach for consistency and ball-first contact  
- Wedge: Focus on control and precision for short game

The system now dynamically selects appropriate fault detection rules based on
the club_used field and applies club-specific expectations for optimal performance.
"""
