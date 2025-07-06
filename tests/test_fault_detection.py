"""
Enhanced Fault Detection Tests for SwingSync AI.

This module provides comprehensive tests for fault detection functionality including:
- Club-specific fault detection rules
- All P-position fault scenarios
- Severity calculation validation
- Performance testing for fault detection
- Enhanced biomechanical fault testing
- Real-time fault detection validation
"""

import unittest
import pytest
import sys
import os
import time
from typing import List, Dict, Any

# Adjust path to import from parent directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from fault_detection import (
    check_swing_faults, FAULT_DIAGNOSIS_MATRIX,
    classify_club_type, generate_club_specific_fault_matrix,
    _calculate_club_specific_severity, _evaluate_fault_condition
)
from kpi_extraction import extract_all_kpis
from data_structures import BiomechanicalKPI, DetectedFault
from tests.test_data_factory import (
    get_good_swing_input,
    get_insufficient_shoulder_turn_swing_input,
    get_excessive_hip_hinge_input
)
from tests.mock_data_factory import (
    create_realistic_swing, ClubType, SwingQuality,
    inject_swing_faults
)

class TestFaultDetection(unittest.TestCase):

    def test_no_faults_for_good_swing(self):
        good_swing_input = get_good_swing_input()
        kpis = extract_all_kpis(good_swing_input)

        # Add a placeholder for lead wrist angle that is within ideal range
        # FD005: PLACEHOLDER_LEAD_WRIST_ANGLE_P4, condition > 15.0
        # Ideal range for this KPI is typically around -5 to 5 (flat to bowed)
        kpis.append(BiomechanicalKPI(
            p_position="P4",
            kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4,
            value=0.0, # Good wrist angle
            unit="degrees",
            ideal_range=(-5.0, 5.0),
            notes="Test good wrist"
        ))

        faults = check_swing_faults(good_swing_input, kpis)

        # Print detected faults for debugging if any are found
        if faults:
            print("\nFaults detected in 'test_no_faults_for_good_swing':")
            for fault in faults:
                print(f"- {fault['fault_name']}: {fault['kpi_deviations'][0]['observed_value']}")

        self.assertEqual(len(faults), 0, "Expected no faults for a good swing based on current rules.")

    def test_detect_insufficient_shoulder_turn(self):
        bad_turn_swing = get_insufficient_shoulder_turn_swing_input() # P4 rot ~45 deg
        kpis = extract_all_kpis(bad_turn_swing)
        # Add good wrist KPI to not muddle this test
        kpis.append(BiomechanicalKPI(p_position="P4",kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4,value=0.0,unit="degrees",ideal_range=(-5.0,5.0),notes=""))

        faults = check_swing_faults(bad_turn_swing, kpis)

        self.assertTrue(any(f['fault_id'] == "INSUFFICIENT_SHOULDER_TURN_P4" for f in faults))
        # Check that other faults (like hip hinge) are NOT triggered if data is good for them
        self.assertFalse(any(f['fault_id'] == "IMPROPER_POSTURE_HIP_HINGE_P1" for f in faults))


    def test_detect_excessive_hip_hinge(self):
        bad_hinge_swing = get_excessive_hip_hinge_input() # P1 hinge > 45 deg
        kpis = extract_all_kpis(bad_hinge_swing)
        kpis.append(BiomechanicalKPI(p_position="P4",kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4,value=0.0,unit="degrees",ideal_range=(-5.0,5.0),notes=""))

        faults = check_swing_faults(bad_hinge_swing, kpis)
        self.assertTrue(any(f['fault_id'] == "IMPROPER_POSTURE_HIP_HINGE_P1" for f in faults))
        # Ensure shoulder turn is NOT flagged as faulty (it should be normal in this data)
        self.assertFalse(any(f['fault_id'] == "INSUFFICIENT_SHOULDER_TURN_P4" for f in faults))

    def test_detect_cupped_wrist_at_top(self):
        # Use good swing data but inject a bad wrist KPI
        swing_input = get_good_swing_input()
        kpis = extract_all_kpis(swing_input)

        # Remove any existing placeholder for this KPI if extract_all_kpis were to add it
        kpis = [kpi for kpi in kpis if kpi['kpi_name'] != PLACEHOLDER_LEAD_WRIST_ANGLE_P4]

        # Add a "cupped wrist" KPI
        # Rule FD005: PLACEHOLDER_LEAD_WRIST_ANGLE_P4, condition > 15.0 for fault
        kpis.append(BiomechanicalKPI(
            p_position="P4",
            kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4,
            value=25.0, # Cupped wrist (extension)
            unit="degrees",
            ideal_range=(-5.0, 5.0), # Flat to bowed
            notes="Test cupped wrist"
        ))

        faults = check_swing_faults(swing_input, kpis)

        found_fault = False
        for fault in faults:
            if fault['fault_id'] == "CUPPED_WRIST_AT_TOP_P4":
                found_fault = True
                self.assertEqual(fault['kpi_deviations'][0]['kpi_name'], PLACEHOLDER_LEAD_WRIST_ANGLE_P4)
                self.assertIn("25.0 degrees", fault['kpi_deviations'][0]['observed_value'])
                break
        self.assertTrue(found_fault, "CUPPED_WRIST_AT_TOP_P4 fault not detected.")

    def test_fault_structure(self):
        bad_turn_swing = get_insufficient_shoulder_turn_swing_input()
        kpis = extract_all_kpis(bad_turn_swing)
        kpis.append(BiomechanicalKPI(p_position="P4",kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4,value=0.0,unit="degrees",ideal_range=(-5.0,5.0),notes=""))

        faults = check_swing_faults(bad_turn_swing, kpis)

        shoulder_fault = next((f for f in faults if f['fault_id'] == "INSUFFICIENT_SHOULDER_TURN_P4"), None)
        self.assertIsNotNone(shoulder_fault)
        self.assertIsInstance(shoulder_fault, DetectedFault)
        self.assertEqual(shoulder_fault['fault_name'], "Insufficient Shoulder Turn at Top of Backswing")
        self.assertIn("P4", shoulder_fault['p_positions_implicated'])
        self.assertIsNotNone(shoulder_fault['llm_prompt_template_key'])
        self.assertTrue(len(shoulder_fault['kpi_deviations']) > 0)
        deviation = shoulder_fault['kpi_deviations'][0]
        self.assertEqual(deviation['kpi_name'], "Shoulder Rotation at P4 (relative to Address)")
        self.assertIn("degrees", deviation['observed_value']) # e.g. "45.0 degrees"
        self.assertIn("Ideal: greater than or equal to 80.0 degrees", deviation['ideal_value_or_range'])


class TestClubSpecificFaultDetection(unittest.TestCase):
    """Test club-specific fault detection functionality"""
    
    def test_club_type_classification(self):
        """Test club type classification function"""
        # Test driver classification
        self.assertEqual(classify_club_type("Driver"), "driver")
        self.assertEqual(classify_club_type("1-Wood"), "driver")
        
        # Test iron classification
        self.assertEqual(classify_club_type("7-Iron"), "iron")
        self.assertEqual(classify_club_type("4-Hybrid"), "iron")
        self.assertEqual(classify_club_type("3-Wood"), "iron")
        
        # Test wedge classification
        self.assertEqual(classify_club_type("Sand Wedge"), "wedge")
        self.assertEqual(classify_club_type("Pitching Wedge"), "wedge")
        self.assertEqual(classify_club_type("PW"), "wedge")
        
        # Test default classification
        self.assertEqual(classify_club_type("Unknown Club"), "iron")
    
    def test_club_specific_fault_matrix_generation(self):
        """Test generation of club-specific fault matrices"""
        for club_type in ["driver", "iron", "wedge"]:
            matrix = generate_club_specific_fault_matrix(club_type)
            
            self.assertIsInstance(matrix, list)
            self.assertGreater(len(matrix), 0)
            
            # Verify all entries have required fields
            for entry in matrix:
                self.assertIn("entry_id", entry)
                self.assertIn("fault_to_report_id", entry)
                self.assertIn("biomechanical_metric_checked", entry)
                self.assertIn("condition_type", entry)
                self.assertIn("condition_values", entry)
                self.assertIn("club_type", entry)
                self.assertEqual(entry["club_type"], club_type)
    
    @pytest.mark.parametrize("club_type", [ClubType.DRIVER, ClubType.MID_IRON, ClubType.WEDGE])
    def test_club_specific_fault_detection(self, club_type):
        """Test fault detection with club-specific rules"""
        # Create poor swing for specific club
        swing_data = create_realistic_swing(
            club_type=club_type,
            quality=SwingQuality.POOR,
            specific_faults=["insufficient_shoulder_turn", "excessive_hip_hinge"]
        )
        
        kpis = extract_all_kpis(swing_data)
        faults = check_swing_faults(swing_data, kpis)
        
        # Should detect some faults
        self.assertGreater(len(faults), 0, f"Should detect faults for poor {club_type.value} swing")
        
        # Check for club-specific fault IDs
        fault_ids = [fault["fault_id"] for fault in faults]
        club_type_str = {
            ClubType.DRIVER: "DRIVER",
            ClubType.MID_IRON: "IRON", 
            ClubType.WEDGE: "WEDGE"
        }[club_type]
        
        # Should have at least one club-specific fault
        club_specific_faults = [fid for fid in fault_ids if club_type_str in fid]
        self.assertGreater(len(club_specific_faults), 0, f"Should have {club_type.value}-specific faults")
    
    def test_fault_severity_calculation(self):
        """Test fault severity calculation with different scenarios"""
        # Create test rule
        test_rule = {
            "condition_type": "outside_range",
            "condition_values": {"lower_bound": 30.0, "upper_bound": 40.0},
            "biomechanical_metric_checked": "Hip Hinge Angle"
        }
        
        # Test values and expected severities
        test_cases = [
            (25.0, "driver", 0.3),  # Minor deviation
            (20.0, "driver", 0.5),  # Moderate deviation  
            (10.0, "driver", 0.7),  # Major deviation
            (50.0, "wedge", 0.5),   # Same deviation, different club
        ]
        
        for value, club_type, expected_min_severity in test_cases:
            severity = _calculate_club_specific_severity(value, test_rule, club_type)
            
            if severity is not None:
                self.assertGreaterEqual(severity, expected_min_severity * 0.8, 
                                      f"Severity too low for value {value} with {club_type}")
                self.assertLessEqual(severity, 1.0, "Severity should not exceed 1.0")
    
    def test_fault_condition_evaluation(self):
        """Test fault condition evaluation logic"""
        # Test outside_range condition
        range_rule = {
            "condition_type": "outside_range",
            "condition_values": {"lower_bound": 30.0, "upper_bound": 40.0}
        }
        
        self.assertTrue(_evaluate_fault_condition(25.0, range_rule))  # Below range
        self.assertTrue(_evaluate_fault_condition(45.0, range_rule))  # Above range
        self.assertFalse(_evaluate_fault_condition(35.0, range_rule))  # Within range
        
        # Test greater_than condition
        gt_rule = {
            "condition_type": "greater_than",
            "condition_values": {"threshold": 15.0}
        }
        
        self.assertTrue(_evaluate_fault_condition(20.0, gt_rule))
        self.assertFalse(_evaluate_fault_condition(10.0, gt_rule))
        
        # Test less_than condition
        lt_rule = {
            "condition_type": "less_than", 
            "condition_values": {"threshold": 80.0}
        }
        
        self.assertTrue(_evaluate_fault_condition(70.0, lt_rule))
        self.assertFalse(_evaluate_fault_condition(90.0, lt_rule))

class TestEnhancedFaultDetection(unittest.TestCase):
    """Enhanced fault detection tests with comprehensive scenarios"""
    
    def test_multiple_fault_scenarios(self):
        """Test detection of multiple faults in one swing"""
        # Create swing with multiple injected faults
        swing_data = create_realistic_swing(
            club_type=ClubType.MID_IRON,
            quality=SwingQuality.TERRIBLE,
            specific_faults=["insufficient_shoulder_turn", "excessive_hip_hinge", "cupped_wrist"]
        )
        
        kpis = extract_all_kpis(swing_data)
        faults = check_swing_faults(swing_data, kpis)
        
        # Should detect multiple faults
        self.assertGreaterEqual(len(faults), 2, "Should detect multiple faults in terrible swing")
        
        # Verify fault structure
        for fault in faults:
            self.assertIn("fault_id", fault)
            self.assertIn("fault_name", fault)
            self.assertIn("severity", fault)
            self.assertIn("kpi_deviations", fault)
            self.assertIsInstance(fault["kpi_deviations"], list)
    
    def test_fault_detection_performance(self):
        """Test fault detection performance"""
        swing_data = create_realistic_swing(quality=SwingQuality.POOR)
        kpis = extract_all_kpis(swing_data)
        
        # Warm up
        for _ in range(3):
            check_swing_faults(swing_data, kpis)
        
        # Measure performance
        start_time = time.perf_counter()
        iterations = 50
        
        for _ in range(iterations):
            faults = check_swing_faults(swing_data, kpis)
        
        end_time = time.perf_counter()
        avg_time_ms = ((end_time - start_time) / iterations) * 1000
        
        # Performance assertion
        self.assertLess(avg_time_ms, 50, f"Fault detection too slow: {avg_time_ms:.2f}ms average")
        print(f"Fault detection performance: {avg_time_ms:.2f}ms average")
    
    def test_fault_detection_with_edge_cases(self):
        """Test fault detection with edge case scenarios"""
        # Test with empty KPIs
        swing_data = create_realistic_swing(quality=SwingQuality.GOOD)
        empty_kpis = []
        
        faults = check_swing_faults(swing_data, empty_kpis)
        self.assertIsInstance(faults, list)
        self.assertEqual(len(faults), 0, "Should return empty list for no KPIs")
        
        # Test with invalid KPI values
        invalid_kpis = [
            {
                "p_position": "P1",
                "kpi_name": "Invalid KPI",
                "value": "invalid_value",  # Non-numeric
                "unit": "degrees"
            }
        ]
        
        faults_invalid = check_swing_faults(swing_data, invalid_kpis)
        self.assertIsInstance(faults_invalid, list)
    
    def test_fault_severity_ranges(self):
        """Test that fault severities are within expected ranges"""
        swing_data = create_realistic_swing(
            quality=SwingQuality.POOR,
            specific_faults=["insufficient_shoulder_turn", "excessive_hip_hinge"]
        )
        
        kpis = extract_all_kpis(swing_data)
        faults = check_swing_faults(swing_data, kpis)
        
        for fault in faults:
            severity = fault.get("severity")
            if severity is not None:
                self.assertGreaterEqual(severity, 0.0, "Severity should be non-negative")
                self.assertLessEqual(severity, 1.0, "Severity should not exceed 1.0")
                self.assertIsInstance(severity, (int, float), "Severity should be numeric")
    
    def test_fault_detection_consistency(self):
        """Test fault detection consistency across similar swings"""
        # Create multiple similar poor swings
        swings = []
        for i in range(5):
            swing = create_realistic_swing(
                session_id=f"consistency_test_{i}",
                club_type=ClubType.MID_IRON,
                quality=SwingQuality.POOR,
                specific_faults=["insufficient_shoulder_turn"]
            )
            swings.append(swing)
        
        # Analyze all swings
        fault_results = []
        for swing in swings:
            kpis = extract_all_kpis(swing)
            faults = check_swing_faults(swing, kpis)
            fault_results.append(faults)
        
        # Check consistency
        fault_counts = [len(faults) for faults in fault_results]
        
        # Should have similar fault counts (within reasonable variance)
        avg_faults = sum(fault_counts) / len(fault_counts)
        for count in fault_counts:
            variance = abs(count - avg_faults) / avg_faults if avg_faults > 0 else 0
            self.assertLess(variance, 0.5, f"Fault detection too inconsistent: {fault_counts}")

class TestRealtimeFaultDetection(unittest.TestCase):
    """Test real-time fault detection capabilities"""
    
    def test_adaptive_fault_detection(self):
        """Test adaptive fault detection for real-time analysis"""
        from live_analysis import AdaptiveFaultDetector, SwingPhase
        
        detector = AdaptiveFaultDetector()
        
        # Create test KPIs with faults
        test_kpis = [
            {
                "p_position": "P1",
                "kpi_name": "Hip Hinge Angle (Spine from Vertical)",
                "value": 55.0,  # Excessive hip hinge
                "unit": "degrees",
                "ideal_range": (30.0, 40.0)
            },
            {
                "p_position": "P4", 
                "kpi_name": "Shoulder Rotation at P4 (relative to Address)",
                "value": 65.0,  # Insufficient shoulder turn
                "unit": "degrees",
                "ideal_range": (80.0, 105.0)
            }
        ]
        
        # Test fault detection
        faults = detector.detect_faults(test_kpis, SwingPhase.SETUP)
        
        self.assertIsInstance(faults, list)
        
        # Should detect some faults
        if faults:
            for fault in faults:
                self.assertIn("fault_id", fault)
                self.assertIn("severity", fault)
                self.assertTrue(fault.get("real_time_detection", False))
    
    def test_realtime_fault_detection_performance(self):
        """Test real-time fault detection performance"""
        from live_analysis import AdaptiveFaultDetector, SwingPhase
        
        detector = AdaptiveFaultDetector()
        
        # Create test KPIs
        test_kpis = create_mock_kpis("P1")
        
        # Measure performance
        start_time = time.perf_counter()
        iterations = 100
        
        for _ in range(iterations):
            faults = detector.detect_faults(test_kpis, SwingPhase.SETUP)
        
        end_time = time.perf_counter()
        avg_time_ms = ((end_time - start_time) / iterations) * 1000
        
        # Real-time requirement: should be under 20ms
        self.assertLess(avg_time_ms, 20, f"Real-time fault detection too slow: {avg_time_ms:.2f}ms")

if __name__ == '__main__':
    unittest.main()
