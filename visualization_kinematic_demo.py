#!/usr/bin/env python3
"""
Visualization demo for kinematic sequence analysis.
Shows how to use the visualization data for creating stunning power transfer graphics.
"""

import json
from kinematic_sequence_lite import analyze_kinematic_sequence
from demo_kinematic_analysis import create_demo_swing_input, create_poor_sequence_demo

def create_visualization_config(result):
    """Create visualization configuration for the UI."""
    return {
        "chart_type": "multi_line_velocity",
        "title": "Kinematic Sequence Analysis - Power Transfer",
        "subtitle": f"Efficiency Score: {result.efficiency_score:.1f}% | Rating: {result.power_transfer_rating}",
        "x_axis": {
            "label": "Time (ms)",
            "data": result.visualization_data["timestamps_ms"]
        },
        "y_axis": {
            "label": "Angular Velocity (°/s)",
            "min": 0,
            "max": max([
                max(result.visualization_data["pelvis_velocity"]) if result.visualization_data["pelvis_velocity"] else 0,
                max(result.visualization_data["torso_velocity"]) if result.visualization_data["torso_velocity"] else 0,
                max(result.visualization_data["arms_velocity"]) if result.visualization_data["arms_velocity"] else 0,
                max(result.visualization_data["club_velocity"]) if result.visualization_data["club_velocity"] else 0
            ]) * 1.1
        },
        "series": [
            {
                "name": "Pelvis",
                "data": result.visualization_data["pelvis_velocity"],
                "color": "#FF6B6B",  # Red
                "line_width": 3,
                "show_peak": True,
                "peak_value": abs(result.pelvis_peak.angular_velocity_deg_s) if result.pelvis_peak else 0,
                "peak_time": result.pelvis_peak.timestamp_ms if result.pelvis_peak else 0
            },
            {
                "name": "Torso",
                "data": result.visualization_data["torso_velocity"],
                "color": "#4ECDC4",  # Teal
                "line_width": 3,
                "show_peak": True,
                "peak_value": abs(result.torso_peak.angular_velocity_deg_s) if result.torso_peak else 0,
                "peak_time": result.torso_peak.timestamp_ms if result.torso_peak else 0
            },
            {
                "name": "Arms",
                "data": result.visualization_data["arms_velocity"],
                "color": "#45B7D1",  # Blue
                "line_width": 3,
                "show_peak": True,
                "peak_value": abs(result.arms_peak.angular_velocity_deg_s) if result.arms_peak else 0,
                "peak_time": result.arms_peak.timestamp_ms if result.arms_peak else 0
            },
            {
                "name": "Club",
                "data": result.visualization_data["club_velocity"],
                "color": "#96CEB4",  # Green
                "line_width": 3,
                "show_peak": True,
                "peak_value": abs(result.club_peak.angular_velocity_deg_s) if result.club_peak else 0,
                "peak_time": result.club_peak.timestamp_ms if result.club_peak else 0
            }
        ],
        "annotations": [
            {
                "type": "sequence_order",
                "correct": result.sequence_order_correct,
                "ideal_sequence": "Pelvis → Torso → Arms → Club",
                "actual_sequence": get_actual_sequence_string(result)
            },
            {
                "type": "timing_gaps",
                "gaps": result.timing_gaps_ms,
                "optimal_gap": 75,
                "tolerance": 25
            }
        ],
        "interactive_features": {
            "zoom": True,
            "pan": True,
            "hover_details": True,
            "peak_markers": True,
            "sequence_arrows": True
        }
    }

def get_actual_sequence_string(result):
    """Get the actual sequence order as a string."""
    peaks = []
    if result.pelvis_peak:
        peaks.append((result.pelvis_peak.timestamp_ms, "Pelvis"))
    if result.torso_peak:
        peaks.append((result.torso_peak.timestamp_ms, "Torso"))
    if result.arms_peak:
        peaks.append((result.arms_peak.timestamp_ms, "Arms"))
    if result.club_peak:
        peaks.append((result.club_peak.timestamp_ms, "Club"))
    
    peaks.sort(key=lambda x: x[0])
    return " → ".join([name for _, name in peaks])

def create_power_transfer_animation_config(result):
    """Create animation configuration for power transfer visualization."""
    return {
        "animation_type": "power_flow",
        "duration_ms": 2000,
        "segments": [
            {
                "name": "pelvis",
                "start_time": result.pelvis_peak.timestamp_ms if result.pelvis_peak else 0,
                "peak_velocity": abs(result.pelvis_peak.angular_velocity_deg_s) if result.pelvis_peak else 0,
                "color": "#FF6B6B",
                "position": {"x": 0.5, "y": 0.3}  # Bottom center
            },
            {
                "name": "torso",
                "start_time": result.torso_peak.timestamp_ms if result.torso_peak else 0,
                "peak_velocity": abs(result.torso_peak.angular_velocity_deg_s) if result.torso_peak else 0,
                "color": "#4ECDC4",
                "position": {"x": 0.5, "y": 0.5}  # Center
            },
            {
                "name": "arms",
                "start_time": result.arms_peak.timestamp_ms if result.arms_peak else 0,
                "peak_velocity": abs(result.arms_peak.angular_velocity_deg_s) if result.arms_peak else 0,
                "color": "#45B7D1",
                "position": {"x": 0.5, "y": 0.7}  # Upper center
            },
            {
                "name": "club",
                "start_time": result.club_peak.timestamp_ms if result.club_peak else 0,
                "peak_velocity": abs(result.club_peak.angular_velocity_deg_s) if result.club_peak else 0,
                "color": "#96CEB4",
                "position": {"x": 0.7, "y": 0.8}  # Upper right
            }
        ],
        "flow_effects": {
            "show_energy_transfer": True,
            "particle_effects": True,
            "velocity_scaling": True,
            "timing_indicators": True
        }
    }

def main():
    """Demonstrate visualization configuration generation."""
    print("SwingSync AI - Kinematic Sequence Visualization Demo")
    print("=" * 60)
    
    # Analyze professional sequence
    print("\n1. GENERATING PROFESSIONAL VISUALIZATION CONFIG...")
    pro_swing = create_demo_swing_input()
    pro_result = analyze_kinematic_sequence(pro_swing)
    pro_viz_config = create_visualization_config(pro_result)
    pro_animation_config = create_power_transfer_animation_config(pro_result)
    
    # Analyze poor sequence
    print("2. GENERATING POOR SEQUENCE VISUALIZATION CONFIG...")
    poor_swing = create_poor_sequence_demo()
    poor_result = analyze_kinematic_sequence(poor_swing)
    poor_viz_config = create_visualization_config(poor_result)
    poor_animation_config = create_power_transfer_animation_config(poor_result)
    
    # Save visualization configs
    print("3. SAVING VISUALIZATION CONFIGURATIONS...")
    
    with open('pro_kinematic_viz_config.json', 'w') as f:
        json.dump(pro_viz_config, f, indent=2)
    
    with open('poor_kinematic_viz_config.json', 'w') as f:
        json.dump(poor_viz_config, f, indent=2)
    
    with open('pro_animation_config.json', 'w') as f:
        json.dump(pro_animation_config, f, indent=2)
    
    with open('poor_animation_config.json', 'w') as f:
        json.dump(poor_animation_config, f, indent=2)
    
    # Display summary
    print("\n" + "="*60)
    print("VISUALIZATION CONFIGURATION SUMMARY")
    print("="*60)
    
    print(f"\nPROFESSIONAL SEQUENCE:")
    print(f"  Chart Type: {pro_viz_config['chart_type']}")
    print(f"  Data Points: {len(pro_viz_config['x_axis']['data'])}")
    print(f"  Series Count: {len(pro_viz_config['series'])}")
    print(f"  Sequence Order: {pro_viz_config['annotations'][0]['actual_sequence']}")
    print(f"  Interactive Features: {len(pro_viz_config['interactive_features'])}")
    
    print(f"\nPOOR SEQUENCE:")
    print(f"  Chart Type: {poor_viz_config['chart_type']}")
    print(f"  Data Points: {len(poor_viz_config['x_axis']['data'])}")
    print(f"  Series Count: {len(poor_viz_config['series'])}")
    print(f"  Sequence Order: {poor_viz_config['annotations'][0]['actual_sequence']}")
    print(f"  Interactive Features: {len(poor_viz_config['interactive_features'])}")
    
    print(f"\nANIMATION CONFIGS:")
    print(f"  Professional Animation Duration: {pro_animation_config['duration_ms']}ms")
    print(f"  Poor Sequence Animation Duration: {poor_animation_config['duration_ms']}ms")
    print(f"  Animated Segments: {len(pro_animation_config['segments'])}")
    print(f"  Flow Effects: {len(pro_animation_config['flow_effects'])}")
    
    # Show sample timing data
    print(f"\nTIMING ANALYSIS:")
    print(f"  Professional Gaps: {pro_result.timing_gaps_ms}")
    print(f"  Poor Sequence Gaps: {poor_result.timing_gaps_ms}")
    
    # Show peak velocities
    print(f"\nPEAK VELOCITIES (Professional):")
    for series in pro_viz_config['series']:
        if series['peak_value'] > 0:
            print(f"  {series['name']}: {series['peak_value']:.0f}°/s at {series['peak_time']:.0f}ms")
    
    print("\n" + "="*60)
    print("🎨 VISUALIZATION CONFIGURATIONS COMPLETE!")
    print("="*60)
    print("✅ Multi-line velocity charts: READY")
    print("✅ Power transfer animations: READY")
    print("✅ Interactive features: CONFIGURED")
    print("✅ Timing annotations: READY")
    print("✅ Peak markers: CONFIGURED")
    print("✅ Sequence flow indicators: READY")
    
    print("\n📊 Ready to create the most impressive golf power analysis visualization!")
    print("💡 UI developers can use these JSON configs to create stunning graphics!")

if __name__ == "__main__":
    main()