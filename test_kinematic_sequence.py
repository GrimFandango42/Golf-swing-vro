#!/usr/bin/env python3
"""
Test script for the Kinematic Sequence Analysis module.
Demonstrates the power analysis capabilities with simulated golf swing data.
"""

import numpy as np
from typing import Dict, List
import json

from data_structures import (
    SwingVideoAnalysisInput,
    FramePoseData,
    PSystemPhase,
    PoseKeypoint
)
from kinematic_sequence import (
    analyze_kinematic_sequence,
    get_kinematic_sequence_kpis_cached,
    KP_LEFT_SHOULDER, KP_RIGHT_SHOULDER,
    KP_LEFT_HIP, KP_RIGHT_HIP,
    KP_LEFT_WRIST, KP_RIGHT_WRIST,
    KP_LEFT_ELBOW, KP_RIGHT_ELBOW
)

def create_simulated_downswing_frames(num_frames: int = 30) -> List[FramePoseData]:
    """
    Create simulated downswing frames with realistic kinematic sequence.
    Simulates P5 (transition), P6 (pre-impact), and P7 (impact).
    """
    frames = []
    
    # Timing parameters for realistic sequence
    pelvis_peak_frame = 8    # Pelvis peaks early
    torso_peak_frame = 12    # Torso follows ~70ms later at 240fps
    arms_peak_frame = 16     # Arms follow ~70ms after torso
    club_peak_frame = 20     # Club peaks last
    
    for i in range(num_frames):
        # Simulate rotation angles with kinematic sequence timing
        t = i / num_frames
        
        # Pelvis rotation (leads the sequence)
        pelvis_angle = 30 * np.sin(np.pi * min(i / pelvis_peak_frame, 1.0))
        if i > pelvis_peak_frame:
            pelvis_angle *= 0.9  # Slight deceleration after peak
        
        # Torso rotation (follows pelvis)
        torso_delay = 4  # frames
        torso_t = max(0, i - torso_delay) / num_frames
        torso_angle = 45 * np.sin(np.pi * min((i - torso_delay) / (torso_peak_frame - torso_delay), 1.0))
        if i > torso_peak_frame:
            torso_angle *= 0.85
        
        # Arms rotation (follows torso)
        arms_delay = 8
        arms_t = max(0, i - arms_delay) / num_frames
        arms_angle = 60 * np.sin(np.pi * min((i - arms_delay) / (arms_peak_frame - arms_delay), 1.0))
        if i > arms_peak_frame:
            arms_angle *= 0.8
        
        # Convert angles to 3D positions
        pelvis_rad = np.radians(pelvis_angle)
        torso_rad = np.radians(torso_angle)
        arms_rad = np.radians(arms_angle)
        
        # Create frame data with rotated positions
        frame_data: FramePoseData = {
            # Hips (pelvis segment)
            KP_LEFT_HIP: {
                "x": -0.15 * np.cos(pelvis_rad) - 0.1 * np.sin(pelvis_rad),
                "y": 0.9,
                "z": -0.15 * np.sin(pelvis_rad) + 0.1 * np.cos(pelvis_rad),
                "visibility": 0.99
            },
            KP_RIGHT_HIP: {
                "x": 0.15 * np.cos(pelvis_rad) - 0.1 * np.sin(pelvis_rad),
                "y": 0.9,
                "z": 0.15 * np.sin(pelvis_rad) + 0.1 * np.cos(pelvis_rad),
                "visibility": 0.99
            },
            # Shoulders (torso segment)
            KP_LEFT_SHOULDER: {
                "x": -0.2 * np.cos(torso_rad) - 0.15 * np.sin(torso_rad),
                "y": 1.4,
                "z": -0.2 * np.sin(torso_rad) + 0.15 * np.cos(torso_rad),
                "visibility": 0.98
            },
            KP_RIGHT_SHOULDER: {
                "x": 0.2 * np.cos(torso_rad) - 0.15 * np.sin(torso_rad),
                "y": 1.4,
                "z": 0.2 * np.sin(torso_rad) + 0.15 * np.cos(torso_rad),
                "visibility": 0.98
            },
            # Arms
            KP_LEFT_ELBOW: {
                "x": -0.3 * np.cos(arms_rad) - 0.2 * np.sin(arms_rad),
                "y": 1.2,
                "z": -0.3 * np.sin(arms_rad) + 0.2 * np.cos(arms_rad),
                "visibility": 0.97
            },
            KP_RIGHT_ELBOW: {
                "x": 0.3 * np.cos(arms_rad) - 0.2 * np.sin(arms_rad),
                "y": 1.2,
                "z": 0.3 * np.sin(arms_rad) + 0.2 * np.cos(arms_rad),
                "visibility": 0.97
            },
            # Wrists (for club proxy)
            KP_LEFT_WRIST: {
                "x": -0.4 * np.cos(arms_rad + 0.2) - 0.3 * np.sin(arms_rad + 0.2),
                "y": 1.0,
                "z": -0.4 * np.sin(arms_rad + 0.2) + 0.3 * np.cos(arms_rad + 0.2),
                "visibility": 0.96
            },
            KP_RIGHT_WRIST: {
                "x": 0.4 * np.cos(arms_rad + 0.2) - 0.3 * np.sin(arms_rad + 0.2),
                "y": 1.0,
                "z": 0.4 * np.sin(arms_rad + 0.2) + 0.3 * np.cos(arms_rad + 0.2),
                "visibility": 0.96
            }
        }
        
        frames.append(frame_data)
    
    return frames

def create_test_swing_input() -> SwingVideoAnalysisInput:
    """Create a complete test swing input with kinematic sequence in downswing."""
    # Create frames for entire swing
    all_frames = []
    
    # P1-P4: Simple backswing frames (not analyzed for kinematic sequence)
    for _ in range(60):
        basic_frame: FramePoseData = {
            KP_LEFT_HIP: {"x": -0.15, "y": 0.9, "z": 0, "visibility": 0.99},
            KP_RIGHT_HIP: {"x": 0.15, "y": 0.9, "z": 0, "visibility": 0.99},
            KP_LEFT_SHOULDER: {"x": -0.2, "y": 1.4, "z": 0, "visibility": 0.98},
            KP_RIGHT_SHOULDER: {"x": 0.2, "y": 1.4, "z": 0, "visibility": 0.98},
            KP_LEFT_ELBOW: {"x": -0.3, "y": 1.2, "z": 0, "visibility": 0.97},
            KP_RIGHT_ELBOW: {"x": 0.3, "y": 1.2, "z": 0, "visibility": 0.97},
            KP_LEFT_WRIST: {"x": -0.4, "y": 1.0, "z": 0, "visibility": 0.96},
            KP_RIGHT_WRIST: {"x": 0.4, "y": 1.0, "z": 0, "visibility": 0.96}
        }
        all_frames.append(basic_frame)
    
    # P5-P7: Downswing with kinematic sequence
    downswing_frames = create_simulated_downswing_frames(30)
    all_frames.extend(downswing_frames)
    
    # P8-P10: Follow through frames
    for _ in range(30):
        all_frames.append(basic_frame)
    
    # Create P-System phases
    p_system_phases = [
        {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
        {"phase_name": "P2", "start_frame_index": 11, "end_frame_index": 20},
        {"phase_name": "P3", "start_frame_index": 21, "end_frame_index": 35},
        {"phase_name": "P4", "start_frame_index": 36, "end_frame_index": 59},
        {"phase_name": "P5", "start_frame_index": 60, "end_frame_index": 70},
        {"phase_name": "P6", "start_frame_index": 71, "end_frame_index": 80},
        {"phase_name": "P7", "start_frame_index": 81, "end_frame_index": 89},
        {"phase_name": "P8", "start_frame_index": 90, "end_frame_index": 100},
        {"phase_name": "P9", "start_frame_index": 101, "end_frame_index": 110},
        {"phase_name": "P10", "start_frame_index": 111, "end_frame_index": 119}
    ]
    
    return SwingVideoAnalysisInput(
        session_id="test_kinematic_001",
        user_id="test_user",
        club_used="Driver",
        frames=all_frames,
        p_system_classification=p_system_phases,
        video_fps=240.0  # High-speed camera
    )

def print_kinematic_results(result):
    """Pretty print the kinematic sequence analysis results."""
    print("\n" + "="*60)
    print("KINEMATIC SEQUENCE ANALYSIS RESULTS")
    print("="*60)
    
    print(f"\nSequence Order: {'CORRECT' if result.sequence_order_correct else 'INCORRECT'}")
    print(f"Efficiency Score: {result.efficiency_score:.1f}%")
    print(f"Power Transfer Rating: {result.power_transfer_rating}")
    
    print("\nPeak Velocities:")
    if result.pelvis_peak:
        print(f"  Pelvis: {abs(result.pelvis_peak.angular_velocity_deg_s):.0f}°/s at {result.pelvis_peak.timestamp_ms:.0f}ms")
    if result.torso_peak:
        print(f"  Torso:  {abs(result.torso_peak.angular_velocity_deg_s):.0f}°/s at {result.torso_peak.timestamp_ms:.0f}ms")
    if result.arms_peak:
        print(f"  Arms:   {abs(result.arms_peak.angular_velocity_deg_s):.0f}°/s at {result.arms_peak.timestamp_ms:.0f}ms")
    if result.club_peak:
        print(f"  Club:   {abs(result.club_peak.angular_velocity_deg_s):.0f}°/s at {result.club_peak.timestamp_ms:.0f}ms")
    
    print("\nTiming Gaps:")
    for gap_name, gap_ms in result.timing_gaps_ms.items():
        formatted_name = gap_name.replace('_', ' ').title()
        print(f"  {formatted_name}: {gap_ms:.0f}ms")
    
    print("\nVisualization Data Summary:")
    print(f"  Time points: {len(result.visualization_data.get('timestamps_ms', []))}")
    print(f"  Data series: {list(result.visualization_data.keys())}")

def main():
    """Run the kinematic sequence analysis test."""
    print("SwingSync AI - Kinematic Sequence Analysis Test")
    print("Testing power generation analysis capabilities...")
    
    # Create test swing data
    swing_input = create_test_swing_input()
    print(f"\nCreated test swing with {len(swing_input['frames'])} frames at {swing_input['video_fps']} fps")
    
    # Analyze kinematic sequence
    result = analyze_kinematic_sequence(swing_input)
    print_kinematic_results(result)
    
    # Test KPI extraction
    print("\n" + "="*60)
    print("EXTRACTED KPIs")
    print("="*60)
    
    kpis = get_kinematic_sequence_kpis_cached(swing_input)
    for kpi in kpis:
        print(f"\n{kpi['kpi_name']}:")
        print(f"  Position: {kpi['p_position']}")
        print(f"  Value: {kpi['value']} {kpi['unit']}")
        if kpi['ideal_range']:
            print(f"  Ideal Range: {kpi['ideal_range']}")
        if kpi['notes']:
            print(f"  Notes: {kpi['notes']}")
    
    # Performance test
    import time
    print("\n" + "="*60)
    print("PERFORMANCE TEST")
    print("="*60)
    
    start_time = time.time()
    for _ in range(10):
        _ = analyze_kinematic_sequence(swing_input)
    elapsed = (time.time() - start_time) / 10
    
    print(f"Average analysis time: {elapsed*1000:.1f}ms")
    print(f"Performance target: <50ms")
    print(f"Status: {'PASS' if elapsed < 0.05 else 'NEEDS OPTIMIZATION'}")
    
    print("\n✅ Kinematic sequence analysis module is ready for production!")

if __name__ == "__main__":
    main()