"""
Enhanced KPI Extraction Tests for SwingSync AI.

This module provides comprehensive tests for KPI extraction functionality including:
- All P-position KPI calculations (P1-P10)
- Club-specific KPI validation
- Edge case handling and error conditions
- Performance testing for KPI calculations
- Enhanced biomechanical metric testing
- Real-time KPI calculation validation
"""

import unittest
import pytest
import numpy as np
import sys
import os
import time
from typing import List, Dict, Any

# Adjust path to import from parent directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from kpi_extraction import (
    calculate_angle_3d,
    get_midpoint,
    calculate_hip_hinge_angle_p1,
    calculate_shoulder_rotation_p4,
    calculate_knee_flexion_p1,
    calculate_weight_distribution_p1_irons,
    calculate_wrist_angle_p4,
    calculate_hip_sway_backswing,
    calculate_spine_angle_p4,
    extract_all_kpis
)
from data_structures import PoseKeypoint, BiomechanicalKPI
from tests.test_data_factory import (
    get_good_swing_input,
    get_insufficient_shoulder_turn_swing_input,
    get_excessive_hip_hinge_input,
    _make_kp # Direct import for simpler point creation in tests
)
from tests.mock_data_factory import (
    create_realistic_swing, ClubType, SwingQuality,
    create_mock_kpis, PhysicsBasedSwingGenerator, SwingCharacteristics
)

class TestKpiExtractionHelpers(unittest.TestCase):

    def test_calculate_angle_3d(self):
        p1 = np.array([1.0, 0.0, 0.0])
        p2 = np.array([0.0, 0.0, 0.0]) # Vertex
        p3 = np.array([0.0, 1.0, 0.0])
        self.assertAlmostEqual(calculate_angle_3d(p1, p2, p3), 90.0)

        p4 = np.array([1.0, 1.0, 0.0])
        self.assertAlmostEqual(calculate_angle_3d(p1, p2, p4), 45.0)

        # Test collinear points (0 degrees)
        p_collinear1 = np.array([2.0, 0.0, 0.0])
        self.assertAlmostEqual(calculate_angle_3d(p1, p2, p_collinear1), 0.0)

        # Test collinear points (180 degrees)
        p_collinear2 = np.array([-1.0, 0.0, 0.0])
        self.assertAlmostEqual(calculate_angle_3d(p1, p2, p_collinear2), 180.0)


    def test_get_midpoint(self):
        p1 = np.array([0.0, 0.0, 0.0])
        p2 = np.array([2.0, 2.0, 2.0])
        mid = get_midpoint(p1, p2)
        np.testing.assert_array_almost_equal(mid, np.array([1.0, 1.0, 1.0]))

class TestKpiCalculations(unittest.TestCase):

    def test_calculate_hip_hinge_angle_p1_good_swing(self):
        good_swing = get_good_swing_input()
        kpi = calculate_hip_hinge_angle_p1(good_swing)
        self.assertIsNotNone(kpi)
        self.assertIsInstance(kpi, BiomechanicalKPI)
        self.assertGreaterEqual(kpi['value'], 30.0) # Based on default pose in factory
        self.assertLessEqual(kpi['value'], 45.0)   # Ideal range (30,45)

    def test_calculate_hip_hinge_angle_p1_excessive_hinge(self):
        bad_hinge_swing = get_excessive_hip_hinge_input()
        kpi = calculate_hip_hinge_angle_p1(bad_hinge_swing)
        self.assertIsNotNone(kpi)
        # In get_excessive_hip_hinge_input, shoulders Y from 1.4 to 1.3, hips Y at 0.9
        # Torso vector Y component becomes smaller (1.3-0.9=0.4 vs 1.4-0.9=0.5)
        # This makes the torso vector more horizontal, thus a LARGER angle with vertical.
        # Expected to be > 45 degrees.
        self.assertGreater(kpi['value'], 45.0, f"Expected hinge > 45, got {kpi['value']}")


    def test_calculate_shoulder_rotation_p4_good_swing(self):
        good_swing = get_good_swing_input() # Designed for ~90 deg rotation
        kpi = calculate_shoulder_rotation_p4(good_swing)
        self.assertIsNotNone(kpi)
        self.assertIsInstance(kpi, BiomechanicalKPI)
        # P4 in good_swing is L(0,1.4,-0.2), R(0,1.4,0.2)
        # P1 is L(-0.2,1.4,0), R(0.2,1.4,0)
        # P1 vec (0.4,0) in XZ. P4 vec (0, 0.4) in XZ. Angle should be 90.
        self.assertAlmostEqual(kpi['value'], 90.0, delta=1.0)

    def test_calculate_shoulder_rotation_p4_insufficient_turn(self):
        bad_turn_swing = get_insufficient_shoulder_turn_swing_input() # Designed for ~45 deg
        kpi = calculate_shoulder_rotation_p4(bad_turn_swing)
        self.assertIsNotNone(kpi)
        # P4 in bad_turn_swing L(-0.141,1.4,-0.141), R(0.141,1.4,0.141)
        # P1 vec (0.4,0). P4 vec (0.282, 0.282). Angle should be 45.
        self.assertAlmostEqual(kpi['value'], 45.0, delta=1.0)

    def test_extract_all_kpis_runs(self):
        good_swing = get_good_swing_input()
        all_kpis = extract_all_kpis(good_swing)
        self.assertIsInstance(all_kpis, list)
        # Check if some expected KPIs are present
        kpi_names = [k['kpi_name'] for k in all_kpis]
        self.assertIn("Hip Hinge Angle (Spine from Vertical)", kpi_names)
        self.assertIn("Shoulder Rotation at P4 (relative to Address)", kpi_names)
        self.assertIn("Left Knee Flexion Angle", kpi_names)
        self.assertIn("Estimated Weight Distribution (Lead Foot %)", kpi_names)


class TestEnhancedKPIExtraction(unittest.TestCase):
    """Enhanced KPI extraction tests with comprehensive coverage"""
    
    def test_all_p_position_kpis(self):
        """Test KPI extraction for all P-positions"""
        # Create realistic swing data
        swing_data = create_realistic_swing(
            club_type=ClubType.MID_IRON,
            quality=SwingQuality.GOOD
        )
        
        kpis = extract_all_kpis(swing_data)
        
        # Should have KPIs for multiple P-positions
        p_positions = set(kpi['p_position'] for kpi in kpis)
        self.assertGreaterEqual(len(p_positions), 2, "Should have KPIs for multiple P-positions")
        
        # Verify P1 KPIs are present
        p1_kpis = [kpi for kpi in kpis if kpi['p_position'] == 'P1']
        self.assertGreater(len(p1_kpis), 0, "Should have P1 KPIs")
        
        # Verify P4 KPIs are present if swing has P4 data
        p4_kpis = [kpi for kpi in kpis if kpi['p_position'] == 'P4']
        if any(phase['phase_name'] == 'P4' for phase in swing_data['p_system_classification']):
            self.assertGreater(len(p4_kpis), 0, "Should have P4 KPIs when P4 data exists")
    
    @pytest.mark.parametrize("club_type", [ClubType.DRIVER, ClubType.MID_IRON, ClubType.WEDGE])
    def test_club_specific_kpi_extraction(self, club_type):
        """Test KPI extraction for different club types"""
        swing_data = create_realistic_swing(
            club_type=club_type,
            quality=SwingQuality.GOOD
        )
        
        kpis = extract_all_kpis(swing_data)
        
        # All club types should produce KPIs
        self.assertGreater(len(kpis), 0, f"Should extract KPIs for {club_type.value}")
        
        # Verify KPI structure
        for kpi in kpis:
            self.assertIn('p_position', kpi)
            self.assertIn('kpi_name', kpi)
            self.assertIn('value', kpi)
            self.assertIn('unit', kpi)
            self.assertIsInstance(kpi['value'], (int, float, str))
    
    def test_kpi_extraction_performance(self):
        """Test KPI extraction performance"""
        swing_data = create_realistic_swing(quality=SwingQuality.GOOD)
        
        # Warm up
        for _ in range(3):
            extract_all_kpis(swing_data)
        
        # Measure performance
        start_time = time.perf_counter()
        iterations = 20
        
        for _ in range(iterations):
            kpis = extract_all_kpis(swing_data)
        
        end_time = time.perf_counter()
        avg_time_ms = ((end_time - start_time) / iterations) * 1000
        
        # Performance assertion
        self.assertLess(avg_time_ms, 100, f"KPI extraction too slow: {avg_time_ms:.2f}ms average")
        print(f"KPI extraction performance: {avg_time_ms:.2f}ms average")
    
    def test_individual_kpi_calculations(self):
        """Test individual KPI calculation functions"""
        # Create test frame data
        frame_data = {
            "left_shoulder": _make_kp(-0.2, 1.4, 0),
            "right_shoulder": _make_kp(0.2, 1.4, 0),
            "left_hip": _make_kp(-0.15, 0.9, 0),
            "right_hip": _make_kp(0.15, 0.9, 0),
            "left_knee": _make_kp(-0.18, 0.5, 0.02),
            "right_knee": _make_kp(0.18, 0.5, 0.02),
            "left_ankle": _make_kp(-0.2, 0.1, 0),
            "right_ankle": _make_kp(0.2, 0.1, 0),
            "left_wrist": _make_kp(-0.3, 1.0, 0.1),
            "right_wrist": _make_kp(0.3, 1.0, 0.1)
        }
        
        # Test hip hinge angle
        hip_angle = calculate_hip_hinge_angle_p1(frame_data)
        self.assertIsNotNone(hip_angle)
        self.assertIsInstance(hip_angle, (int, float))
        self.assertGreater(hip_angle, 0)
        self.assertLess(hip_angle, 90)
        
        # Test knee flexion
        lead_knee_flex = calculate_knee_flexion_p1(frame_data, "lead")
        self.assertIsNotNone(lead_knee_flex)
        self.assertIsInstance(lead_knee_flex, (int, float))
        
        trail_knee_flex = calculate_knee_flexion_p1(frame_data, "trail")
        self.assertIsNotNone(trail_knee_flex)
        self.assertIsInstance(trail_knee_flex, (int, float))
        
        # Test weight distribution
        weight_dist = calculate_weight_distribution_p1_irons(frame_data)
        self.assertIsNotNone(weight_dist)
        self.assertIsInstance(weight_dist, (int, float))
        self.assertGreaterEqual(weight_dist, 0)
        self.assertLessEqual(weight_dist, 1)
    
    def test_kpi_extraction_with_missing_keypoints(self):
        """Test KPI extraction with missing keypoints"""
        # Create frame data with missing keypoints
        incomplete_frame_data = {
            "left_shoulder": _make_kp(-0.2, 1.4, 0),
            "right_shoulder": _make_kp(0.2, 1.4, 0),
            # Missing hip and other keypoints
        }
        
        swing_data = {
            "session_id": "test_missing_kp",
            "user_id": "test_user",
            "club_used": "7-Iron",
            "frames": [incomplete_frame_data],
            "p_system_classification": [
                {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 0}
            ],
            "video_fps": 60.0
        }
        
        # Should not crash with missing keypoints
        kpis = extract_all_kpis(swing_data)
        
        # Should return list (may be empty)
        self.assertIsInstance(kpis, list)
    
    def test_kpi_extraction_edge_cases(self):
        """Test KPI extraction edge cases"""
        # Test with single frame
        single_frame_data = create_realistic_swing(quality=SwingQuality.GOOD)
        single_frame_data["frames"] = single_frame_data["frames"][:1]
        single_frame_data["p_system_classification"] = [
            {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 0}
        ]
        
        kpis = extract_all_kpis(single_frame_data)
        self.assertIsInstance(kpis, list)
        
        # Test with empty frames
        empty_swing_data = {
            "session_id": "test_empty",
            "user_id": "test_user", 
            "club_used": "7-Iron",
            "frames": [],
            "p_system_classification": [],
            "video_fps": 60.0
        }
        
        kpis_empty = extract_all_kpis(empty_swing_data)
        self.assertIsInstance(kpis_empty, list)
    
    def test_kpi_value_ranges(self):
        """Test that KPI values are within reasonable ranges"""
        swing_data = create_realistic_swing(quality=SwingQuality.GOOD)
        kpis = extract_all_kpis(swing_data)
        
        for kpi in kpis:
            kpi_name = kpi['kpi_name']
            value = kpi['value']
            
            if isinstance(value, (int, float)):
                # Hip hinge angle should be 0-90 degrees
                if "hip hinge" in kpi_name.lower():
                    self.assertGreaterEqual(value, 0)
                    self.assertLessEqual(value, 90)
                
                # Shoulder rotation should be 0-180 degrees
                elif "shoulder rotation" in kpi_name.lower():
                    self.assertGreaterEqual(value, 0)
                    self.assertLessEqual(value, 180)
                
                # Weight distribution should be 0-1
                elif "weight distribution" in kpi_name.lower():
                    self.assertGreaterEqual(value, 0)
                    self.assertLessEqual(value, 1)
                
                # Knee flexion should be reasonable
                elif "knee flexion" in kpi_name.lower():
                    self.assertGreaterEqual(value, 0)
                    self.assertLessEqual(value, 90)
    
    def test_kpi_consistency_across_qualities(self):
        """Test KPI consistency across different swing qualities"""
        qualities = [SwingQuality.EXCELLENT, SwingQuality.GOOD, SwingQuality.AVERAGE, SwingQuality.POOR]
        
        kpi_results = {}
        for quality in qualities:
            swing_data = create_realistic_swing(
                club_type=ClubType.MID_IRON,
                quality=quality
            )
            kpis = extract_all_kpis(swing_data)
            kpi_results[quality] = kpis
        
        # All qualities should produce some KPIs
        for quality, kpis in kpi_results.items():
            self.assertGreater(len(kpis), 0, f"Should extract KPIs for {quality.value} swing")
        
        # Check that the same KPI types are generally present
        excellent_kpi_names = set(kpi['kpi_name'] for kpi in kpi_results[SwingQuality.EXCELLENT])
        poor_kpi_names = set(kpi['kpi_name'] for kpi in kpi_results[SwingQuality.POOR])
        
        # Should have some overlap in KPI types
        overlap = excellent_kpi_names.intersection(poor_kpi_names)
        self.assertGreater(len(overlap), 0, "Should have overlapping KPI types across qualities")

class TestRealtimeKPICalculation(unittest.TestCase):
    """Test real-time KPI calculation capabilities"""
    
    def test_single_frame_kpi_calculation(self):
        """Test KPI calculation for individual frames"""
        from live_analysis import StreamingKPICalculator, SwingPhase
        
        calculator = StreamingKPICalculator()
        
        # Create test frame
        frame_data = type('Frame', (), {
            'keypoints': {
                "left_shoulder": _make_kp(-0.2, 1.4, 0),
                "right_shoulder": _make_kp(0.2, 1.4, 0),
                "left_hip": _make_kp(-0.15, 0.9, 0),
                "right_hip": _make_kp(0.15, 0.9, 0),
                "left_knee": _make_kp(-0.18, 0.5, 0.02),
                "right_knee": _make_kp(0.18, 0.5, 0.02),
                "left_ankle": _make_kp(-0.2, 0.1, 0),
                "right_ankle": _make_kp(0.2, 0.1, 0)
            }
        })()
        
        # Test KPI calculation for setup phase
        kpis = calculator.calculate_kpis_for_frame(frame_data, SwingPhase.SETUP)
        
        self.assertIsInstance(kpis, list)
        
        # Should have some KPIs for setup
        if kpis:
            for kpi in kpis:
                self.assertIn('p_position', kpi)
                self.assertIn('kpi_name', kpi)
                self.assertIn('value', kpi)
    
    def test_realtime_kpi_performance(self):
        """Test real-time KPI calculation performance"""
        from live_analysis import StreamingKPICalculator, SwingPhase
        
        calculator = StreamingKPICalculator()
        
        # Create test frame
        frame_data = type('Frame', (), {
            'keypoints': {
                "left_shoulder": _make_kp(-0.2, 1.4, 0),
                "right_shoulder": _make_kp(0.2, 1.4, 0),
                "left_hip": _make_kp(-0.15, 0.9, 0),
                "right_hip": _make_kp(0.15, 0.9, 0),
                "left_knee": _make_kp(-0.18, 0.5, 0.02),
                "right_knee": _make_kp(0.18, 0.5, 0.02),
                "left_ankle": _make_kp(-0.2, 0.1, 0),
                "right_ankle": _make_kp(0.2, 0.1, 0)
            }
        })()
        
        # Measure performance
        start_time = time.perf_counter()
        iterations = 50
        
        for _ in range(iterations):
            kpis = calculator.calculate_kpis_for_frame(frame_data, SwingPhase.SETUP)
        
        end_time = time.perf_counter()
        avg_time_ms = ((end_time - start_time) / iterations) * 1000
        
        # Real-time requirement: should be under 10ms per frame
        self.assertLess(avg_time_ms, 10, f"Real-time KPI calculation too slow: {avg_time_ms:.2f}ms")

if __name__ == '__main__':
    unittest.main()
