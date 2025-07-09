#!/usr/bin/env python3
"""
Simple test of kinematic sequence KPI integration.
Tests that the new KPIs are properly integrated into the main extraction pipeline.
"""

from data_structures import SwingVideoAnalysisInput, FramePoseData, PSystemPhase
from kpi_extraction import extract_all_kpis

def create_minimal_test_input():
    """Create minimal valid test input."""
    # Create basic frame data
    frame_data = {
        "left_shoulder": {"x": -0.2, "y": 1.4, "z": 0, "visibility": 0.98},
        "right_shoulder": {"x": 0.2, "y": 1.4, "z": 0, "visibility": 0.98},
        "left_hip": {"x": -0.15, "y": 0.9, "z": 0, "visibility": 0.99},
        "right_hip": {"x": 0.15, "y": 0.9, "z": 0, "visibility": 0.99},
        "left_wrist": {"x": -0.4, "y": 1.0, "z": 0, "visibility": 0.96},
        "right_wrist": {"x": 0.4, "y": 1.0, "z": 0, "visibility": 0.96},
        "left_elbow": {"x": -0.3, "y": 1.2, "z": 0, "visibility": 0.97},
        "right_elbow": {"x": 0.3, "y": 1.2, "z": 0, "visibility": 0.97},
        "left_knee": {"x": -0.18, "y": 0.5, "z": 0, "visibility": 0.97},
        "right_knee": {"x": 0.18, "y": 0.5, "z": 0, "visibility": 0.97},
        "left_ankle": {"x": -0.2, "y": 0.1, "z": 0, "visibility": 0.96},
        "right_ankle": {"x": 0.2, "y": 0.1, "z": 0, "visibility": 0.96}
    }
    
    # Create frames (100 total)
    frames = [frame_data for _ in range(100)]
    
    # Create P-System phases
    p_system_phases = [
        {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
        {"phase_name": "P2", "start_frame_index": 11, "end_frame_index": 20},
        {"phase_name": "P3", "start_frame_index": 21, "end_frame_index": 30},
        {"phase_name": "P4", "start_frame_index": 31, "end_frame_index": 40},
        {"phase_name": "P5", "start_frame_index": 41, "end_frame_index": 50},
        {"phase_name": "P6", "start_frame_index": 51, "end_frame_index": 60},
        {"phase_name": "P7", "start_frame_index": 61, "end_frame_index": 70},
        {"phase_name": "P8", "start_frame_index": 71, "end_frame_index": 80},
        {"phase_name": "P9", "start_frame_index": 81, "end_frame_index": 90},
        {"phase_name": "P10", "start_frame_index": 91, "end_frame_index": 99}
    ]
    
    return SwingVideoAnalysisInput(
        session_id="test_integration_001",
        user_id="test_user",
        club_used="Driver",
        frames=frames,
        p_system_classification=p_system_phases,
        video_fps=240.0
    )

def main():
    print("Testing Kinematic Sequence KPI Integration...")
    print("="*60)
    
    # Create test input
    swing_input = create_minimal_test_input()
    
    # Extract all KPIs
    print("\nExtracting all KPIs...")
    all_kpis = extract_all_kpis(swing_input)
    
    # Filter for kinematic sequence KPIs
    kinematic_kpis = [kpi for kpi in all_kpis if "Kinematic" in kpi['kpi_name'] or "Power Transfer" in kpi['kpi_name']]
    
    print(f"\nTotal KPIs extracted: {len(all_kpis)}")
    print(f"Kinematic sequence KPIs: {len(kinematic_kpis)}")
    
    if kinematic_kpis:
        print("\n" + "="*60)
        print("KINEMATIC SEQUENCE KPIs FOUND:")
        print("="*60)
        for kpi in kinematic_kpis:
            print(f"\nKPI: {kpi['kpi_name']}")
            print(f"  P-Position: {kpi['p_position']}")
            print(f"  Value: {kpi['value']} {kpi['unit']}")
            if kpi.get('ideal_range'):
                print(f"  Ideal Range: {kpi['ideal_range']}")
            if kpi.get('notes'):
                print(f"  Notes: {kpi['notes'][:100]}...")  # First 100 chars
        
        print("\n✅ SUCCESS: Kinematic sequence KPIs are properly integrated!")
    else:
        print("\n❌ ERROR: No kinematic sequence KPIs found!")
    
    # Show sample of other KPIs
    print("\n" + "="*60)
    print("SAMPLE OF OTHER KPIs:")
    print("="*60)
    other_kpis = [kpi for kpi in all_kpis if kpi not in kinematic_kpis][:5]
    for kpi in other_kpis:
        print(f"- {kpi['kpi_name']} ({kpi['p_position']}): {kpi['value']} {kpi['unit']}")

if __name__ == "__main__":
    main()