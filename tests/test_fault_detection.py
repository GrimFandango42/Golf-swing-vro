import unittest
import sys
import os

# Adjust path to import from parent directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from fault_detection import check_swing_faults, FAULT_DIAGNOSIS_MATRIX
from kpi_extraction import extract_all_kpis, PLACEHOLDER_LEAD_WRIST_ANGLE_P4
from data_structures import BiomechanicalKPI, DetectedFault
from tests.test_data_factory import (
    get_good_swing_input,
    get_insufficient_shoulder_turn_swing_input,
    get_excessive_hip_hinge_input
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


if __name__ == '__main__':
    unittest.main()
```
