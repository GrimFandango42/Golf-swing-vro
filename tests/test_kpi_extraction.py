import unittest
import numpy as np
import sys
import os

# Adjust path to import from parent directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from kpi_extraction import (
    calculate_angle_3d,
    get_midpoint,
    calculate_hip_hinge_angle_p1,
    calculate_shoulder_rotation_p4,
    extract_all_kpis
)
from data_structures import PoseKeypoint, BiomechanicalKPI
from tests.test_data_factory import (
    get_good_swing_input,
    get_insufficient_shoulder_turn_swing_input,
    get_excessive_hip_hinge_input,
    _make_kp # Direct import for simpler point creation in tests
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


if __name__ == '__main__':
    unittest.main()
```
