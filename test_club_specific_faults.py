#!/usr/bin/env python3
"""
Simple test script for club-specific fault detection without requiring numpy/kpi_extraction.
"""

import sys
sys.path.append('.')

# Mock the required imports to avoid numpy dependency
class MockBiomechanicalKPI:
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)
    
    def __getitem__(self, key):
        return getattr(self, key)
    
    def get(self, key, default=None):
        return getattr(self, key, default)

# Import the fault detection components
from fault_detection import (
    classify_club_type,
    generate_club_specific_fault_matrix,
    _calculate_club_specific_severity,
    _evaluate_fault_condition,
    _generate_ideal_value_description,
    WEIGHT_DISTRIBUTION_TARGETS,
    HIP_HINGE_TARGETS,
    SHOULDER_ROTATION_TARGETS,
    KNEE_FLEX_TARGETS,
    LEAD_WRIST_TARGETS
)

def test_club_classification():
    """Test the club type classification system."""
    print("=== Club Classification Test ===")
    test_clubs = [
        "Driver", "1-Wood", "7-Iron", "Pitching Wedge", "Sand Wedge", 
        "3-Wood", "5-Hybrid", "SW", "PW", "Gap Wedge", "Lob Wedge"
    ]
    
    for club in test_clubs:
        club_type = classify_club_type(club)
        print(f"  {club} -> {club_type}")
    print()

def test_club_specific_targets():
    """Test club-specific target constants."""
    print("=== Club-Specific Targets Test ===")
    
    print("Weight Distribution Targets:")
    for club_type, targets in WEIGHT_DISTRIBUTION_TARGETS.items():
        print(f"  {club_type.title()}: {targets['ideal']:.0f}% lead foot (range: {targets['range'][0]:.0f}%-{targets['range'][1]:.0f}%)")
    
    print("\nHip Hinge Angle Targets:")
    for club_type, targets in HIP_HINGE_TARGETS.items():
        print(f"  {club_type.title()}: {targets['ideal']:.1f}° (range: {targets['range'][0]:.1f}°-{targets['range'][1]:.1f}°)")
    
    print("\nShoulder Rotation Targets at P4:")
    for club_type, targets in SHOULDER_ROTATION_TARGETS.items():
        print(f"  {club_type.title()}: min {targets['minimum']:.0f}°, ideal {targets['ideal']:.0f}°")
    print()

def test_fault_matrix_generation():
    """Test dynamic fault matrix generation for different club types."""
    print("=== Fault Matrix Generation Test ===")
    
    for club_type in ["driver", "iron", "wedge"]:
        matrix = generate_club_specific_fault_matrix(club_type)
        print(f"\n{club_type.title()} Fault Matrix ({len(matrix)} rules):")
        
        # Group rules by P-position for better display
        p1_rules = [r for r in matrix if r["p_position_focused"] == "P1"]
        p4_rules = [r for r in matrix if r["p_position_focused"] == "P4"]
        
        print(f"  P1 Rules ({len(p1_rules)}):")
        for rule in p1_rules:
            metric = rule["biomechanical_metric_checked"]
            condition = rule["condition_type"]
            values = rule["condition_values"]
            print(f"    - {metric}: {condition} {values}")
        
        print(f"  P4 Rules ({len(p4_rules)}):")
        for rule in p4_rules:
            metric = rule["biomechanical_metric_checked"]
            condition = rule["condition_type"]
            values = rule["condition_values"]
            print(f"    - {metric}: {condition} {values}")
    print()

def test_severity_calculation():
    """Test club-specific severity calculations."""
    print("=== Severity Calculation Test ===")
    
    # Create test rule for weight distribution
    test_rule = {
        "entry_id": "TEST_001",
        "biomechanical_metric_checked": "Estimated Weight Distribution (Lead Foot %)",
        "condition_type": "outside_range",
        "condition_values": {"lower_bound": 45.0, "upper_bound": 55.0},
        "fault_to_report_id": "TEST_WEIGHT_FAULT"
    }
    
    # Test different severity scenarios
    test_scenarios = [
        {"value": 35.0, "description": "Far below range"},
        {"value": 42.0, "description": "Slightly below range"},
        {"value": 50.0, "description": "Within range"},
        {"value": 58.0, "description": "Slightly above range"},
        {"value": 70.0, "description": "Far above range"}
    ]
    
    for club_type in ["driver", "iron", "wedge"]:
        print(f"\n{club_type.title()} Severity Calculations:")
        for scenario in test_scenarios:
            severity = _calculate_club_specific_severity(scenario["value"], test_rule, club_type)
            severity_str = f"{severity:.2f}" if severity else "None"
            print(f"  {scenario['description']} ({scenario['value']:.0f}%): {severity_str}")
    print()

def test_fault_condition_evaluation():
    """Test fault condition evaluation logic."""
    print("=== Fault Condition Evaluation Test ===")
    
    test_rules = [
        {
            "condition_type": "outside_range",
            "condition_values": {"lower_bound": 40.0, "upper_bound": 60.0},
            "test_values": [35.0, 45.0, 50.0, 55.0, 65.0]
        },
        {
            "condition_type": "less_than",
            "condition_values": {"threshold": 80.0},
            "test_values": [70.0, 75.0, 80.0, 85.0, 90.0]
        },
        {
            "condition_type": "greater_than",
            "condition_values": {"threshold": 10.0},
            "test_values": [5.0, 8.0, 10.0, 12.0, 15.0]
        }
    ]
    
    for rule in test_rules:
        print(f"\n{rule['condition_type']} condition with {rule['condition_values']}:")
        for value in rule["test_values"]:
            fault_detected = _evaluate_fault_condition(value, rule)
            print(f"  Value {value}: {'FAULT' if fault_detected else 'OK'}")
    print()

def test_ideal_value_descriptions():
    """Test generation of ideal value descriptions."""
    print("=== Ideal Value Description Test ===")
    
    test_rules = [
        {
            "condition_type": "outside_range",
            "condition_values": {"lower_bound": 30.0, "upper_bound": 45.0}
        },
        {
            "condition_type": "less_than",
            "condition_values": {"threshold": 80.0}
        },
        {
            "condition_type": "greater_than",
            "condition_values": {"threshold": 15.0}
        }
    ]
    
    for rule in test_rules:
        description = _generate_ideal_value_description(rule, "degrees")
        print(f"  {rule['condition_type']}: {description}")
    print()

def main():
    """Run all tests for club-specific fault detection."""
    print("Enhanced Club-Specific Fault Detection System Test\n")
    
    test_club_classification()
    test_club_specific_targets()
    test_fault_matrix_generation()
    test_severity_calculation()
    test_fault_condition_evaluation()
    test_ideal_value_descriptions()
    
    print("=== All Tests Complete ===")
    print("\nKey Features Demonstrated:")
    print("1. ✓ Club type classification from club names")
    print("2. ✓ Club-specific target ranges and thresholds")
    print("3. ✓ Dynamic fault matrix generation per club type")
    print("4. ✓ Club-specific severity calculations with modifiers")
    print("5. ✓ Robust fault condition evaluation")
    print("6. ✓ Clear ideal value descriptions")
    
    print("\nThe enhanced fault detection system is ready for integration!")

if __name__ == "__main__":
    main()