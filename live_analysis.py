"""
Live Analysis Engine for Real-time Golf Swing Processing.

This module provides real-time frame-by-frame analysis capabilities for golf swing
data, optimized for low-latency feedback during live practice sessions and coaching.

Key Features:
- Frame-by-frame pose analysis with optimized algorithms
- Real-time KPI calculation and fault detection
- Streaming integration with existing analysis pipeline
- Memory-efficient processing for continuous operation
- Performance optimization for sub-100ms latency
- Adaptive analysis based on swing phase detection

Components:
- `LiveAnalysisEngine`: Main engine for real-time analysis
- `StreamingKPICalculator`: Optimized KPI calculation for streaming data
- `FrameAnalysisResult`: Result structure for individual frame analysis
- `SwingPhaseDetector`: Real-time swing phase identification
- `AdaptiveFaultDetector`: Dynamic fault detection with context awareness
"""

import asyncio
import time
import math
from typing import Dict, List, Optional, Any, Tuple, Union
from dataclasses import dataclass, field
from collections import deque, defaultdict
import numpy as np
from enum import Enum

from data_structures import (
    PoseKeypoint,
    FramePoseData,
    BiomechanicalKPI,
    DetectedFault,
    SwingVideoAnalysisInput
)
from kpi_extraction import (
    calculate_hip_hinge_angle_p1,
    calculate_knee_flexion_p1,
    calculate_shoulder_turn_p4,
    calculate_wrist_angle_p4,
    calculate_weight_distribution_p1_irons,
    calculate_hip_sway_backswing,
    calculate_spine_angle_p4
)
from fault_detection import check_swing_faults, FAULT_DIAGNOSIS_MATRIX

class SwingPhase(Enum):
    """Real-time swing phase detection"""
    SETUP = "setup"
    TAKEAWAY = "takeaway" 
    BACKSWING = "backswing"
    TOP_OF_SWING = "top_of_swing"
    DOWNSWING = "downswing"
    IMPACT = "impact"
    FOLLOW_THROUGH = "follow_through"
    FINISH = "finish"
    UNKNOWN = "unknown"

@dataclass
class FrameAnalysisResult:
    """Result of real-time frame analysis"""
    frame_index: int
    timestamp: float
    swing_phase: SwingPhase
    frame_data: Any  # StreamingFrameData
    kpis: List[BiomechanicalKPI] = field(default_factory=list)
    detected_faults: List[DetectedFault] = field(default_factory=list)
    phase_confidence: float = 0.0
    analysis_latency_ms: float = 0.0
    quality_score: float = 0.0  # Frame quality assessment
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization"""
        return {
            "frame_index": self.frame_index,
            "timestamp": self.timestamp,
            "swing_phase": self.swing_phase.value,
            "phase_confidence": self.phase_confidence,
            "analysis_latency_ms": self.analysis_latency_ms,
            "quality_score": self.quality_score,
            "kpis": [
                {
                    "p_position": kpi.p_position,
                    "kpi_name": kpi.kpi_name,
                    "value": kpi.value,
                    "unit": kpi.unit,
                    "ideal_range": kpi.ideal_range,
                    "notes": kpi.notes
                }
                for kpi in self.kpis
            ],
            "detected_faults": [
                {
                    "fault_id": fault.get("fault_id"),
                    "fault_name": fault.get("fault_name"),
                    "severity": fault.get("severity"),
                    "description": fault.get("description"),
                    "p_positions_implicated": fault.get("p_positions_implicated", [])
                }
                for fault in self.detected_faults
            ]
        }

class SwingPhaseDetector:
    """Real-time swing phase detection using pose landmarks"""
    
    def __init__(self, window_size: int = 5):
        self.window_size = window_size
        self.frame_history: deque = deque(maxlen=window_size)
        self.phase_history: deque = deque(maxlen=window_size)
        self.velocity_threshold = 0.1
        self.position_threshold = 0.05
    
    def detect_phase(self, frame_data: Any) -> Tuple[SwingPhase, float]:
        """Detect current swing phase from frame data"""
        keypoints = frame_data.keypoints
        
        # Store frame for history analysis
        self.frame_history.append(frame_data)
        
        if len(self.frame_history) < 2:
            return SwingPhase.SETUP, 0.5
        
        try:
            # Calculate key metrics for phase detection
            wrist_position = self._get_lead_wrist_position(keypoints)
            shoulder_rotation = self._estimate_shoulder_rotation(keypoints)
            hip_rotation = self._estimate_hip_rotation(keypoints)
            
            # Calculate velocities if we have enough history
            if len(self.frame_history) >= 2:
                prev_frame = self.frame_history[-2]
                prev_wrist = self._get_lead_wrist_position(prev_frame.keypoints)
                wrist_velocity = self._calculate_velocity(prev_wrist, wrist_position, 
                                                        frame_data.timestamp - prev_frame.timestamp)
            else:
                wrist_velocity = 0.0
            
            # Phase detection logic
            phase, confidence = self._classify_phase(
                wrist_position, shoulder_rotation, hip_rotation, wrist_velocity
            )
            
            # Smooth phase transitions
            smoothed_phase, smoothed_confidence = self._smooth_phase_detection(phase, confidence)
            
            self.phase_history.append(smoothed_phase)
            return smoothed_phase, smoothed_confidence
            
        except Exception as e:
            # Fallback to previous phase or setup if error
            if self.phase_history:
                return self.phase_history[-1], 0.3
            return SwingPhase.SETUP, 0.1
    
    def _get_lead_wrist_position(self, keypoints: Dict[str, PoseKeypoint]) -> Tuple[float, float, float]:
        """Get lead wrist position (assuming right-handed golfer)"""
        if "left_wrist" in keypoints:
            wrist = keypoints["left_wrist"]
            return (wrist["x"], wrist["y"], wrist["z"])
        return (0.0, 0.0, 0.0)
    
    def _estimate_shoulder_rotation(self, keypoints: Dict[str, PoseKeypoint]) -> float:
        """Estimate shoulder rotation angle"""
        if "left_shoulder" in keypoints and "right_shoulder" in keypoints:
            left = keypoints["left_shoulder"]
            right = keypoints["right_shoulder"]
            
            # Calculate angle in XZ plane (assuming Y is vertical)
            dx = right["x"] - left["x"]
            dz = right["z"] - left["z"]
            
            if abs(dx) > 0.001:  # Avoid division by zero
                angle = math.atan2(dz, dx)
                return math.degrees(angle)
        
        return 0.0
    
    def _estimate_hip_rotation(self, keypoints: Dict[str, PoseKeypoint]) -> float:
        """Estimate hip rotation angle"""
        if "left_hip" in keypoints and "right_hip" in keypoints:
            left = keypoints["left_hip"]
            right = keypoints["right_hip"]
            
            dx = right["x"] - left["x"]
            dz = right["z"] - left["z"]
            
            if abs(dx) > 0.001:
                angle = math.atan2(dz, dx)
                return math.degrees(angle)
        
        return 0.0
    
    def _calculate_velocity(self, pos1: Tuple[float, float, float], 
                          pos2: Tuple[float, float, float], dt: float) -> float:
        """Calculate 3D velocity magnitude"""
        if dt <= 0:
            return 0.0
        
        dx = pos2[0] - pos1[0]
        dy = pos2[1] - pos1[1]
        dz = pos2[2] - pos1[2]
        
        distance = math.sqrt(dx**2 + dy**2 + dz**2)
        return distance / dt
    
    def _classify_phase(self, wrist_pos: Tuple[float, float, float], 
                       shoulder_rot: float, hip_rot: float, velocity: float) -> Tuple[SwingPhase, float]:
        """Classify swing phase based on measurements"""
        x, y, z = wrist_pos
        
        # Phase classification rules (simplified for real-time processing)
        confidence = 0.7  # Default confidence
        
        # Setup phase: low velocity, neutral positions
        if velocity < 0.05 and abs(shoulder_rot) < 10 and abs(hip_rot) < 10:
            return SwingPhase.SETUP, 0.8
        
        # Takeaway: moderate velocity, slight rotation
        elif velocity < 0.2 and 0 < shoulder_rot < 20:
            return SwingPhase.TAKEAWAY, 0.7
        
        # Backswing: increasing rotation, hands moving back
        elif 20 <= shoulder_rot < 60 and z < -0.1:
            return SwingPhase.BACKSWING, 0.8
        
        # Top of swing: maximum rotation, brief pause
        elif shoulder_rot >= 60 and velocity < 0.1:
            return SwingPhase.TOP_OF_SWING, 0.9
        
        # Downswing: decreasing rotation, high velocity
        elif shoulder_rot > 30 and velocity > 0.3:
            return SwingPhase.DOWNSWING, 0.8
        
        # Impact: hands forward, high velocity
        elif velocity > 0.5 and z > -0.05:
            return SwingPhase.IMPACT, 0.7
        
        # Follow through: hands high, decreasing velocity
        elif y > 1.3 and velocity < 0.3:
            return SwingPhase.FOLLOW_THROUGH, 0.6
        
        # Finish: low velocity, hands high
        elif velocity < 0.1 and y > 1.4:
            return SwingPhase.FINISH, 0.8
        
        return SwingPhase.UNKNOWN, 0.3
    
    def _smooth_phase_detection(self, current_phase: SwingPhase, 
                               confidence: float) -> Tuple[SwingPhase, float]:
        """Smooth phase transitions to avoid rapid changes"""
        if not self.phase_history:
            return current_phase, confidence
        
        recent_phases = list(self.phase_history)[-3:]  # Look at last 3 phases
        
        # If current phase matches recent trend, increase confidence
        if len(recent_phases) >= 2 and all(p == current_phase for p in recent_phases[-2:]):
            confidence = min(0.95, confidence + 0.1)
        
        # If phase is very different from recent history, require higher confidence
        elif recent_phases and current_phase != recent_phases[-1]:
            if confidence < 0.6:
                return recent_phases[-1], 0.5  # Keep previous phase
        
        return current_phase, confidence

class StreamingKPICalculator:
    """Optimized KPI calculation for real-time streaming"""
    
    def __init__(self):
        self.calculation_cache = {}
        self.frame_buffer = deque(maxlen=10)  # Keep recent frames for context
    
    def calculate_kpis_for_frame(self, frame_data: Any, 
                                swing_phase: SwingPhase) -> List[BiomechanicalKPI]:
        """Calculate relevant KPIs for current frame based on swing phase"""
        kpis = []
        keypoints = frame_data.keypoints
        
        # Create frame data in expected format
        frame_pose_data = {name: kp for name, kp in keypoints.items()}
        
        try:
            # Calculate KPIs based on swing phase
            if swing_phase in [SwingPhase.SETUP, SwingPhase.TAKEAWAY]:
                kpis.extend(self._calculate_setup_kpis(frame_pose_data))
            elif swing_phase in [SwingPhase.BACKSWING, SwingPhase.TOP_OF_SWING]:
                kpis.extend(self._calculate_backswing_kpis(frame_pose_data))
            elif swing_phase in [SwingPhase.DOWNSWING, SwingPhase.IMPACT]:
                kpis.extend(self._calculate_impact_kpis(frame_pose_data))
            
            # Always calculate basic posture KPIs
            kpis.extend(self._calculate_posture_kpis(frame_pose_data))
            
        except Exception as e:
            # Log error but don't fail the analysis
            pass
        
        return kpis
    
    def _calculate_setup_kpis(self, frame_data: FramePoseData) -> List[BiomechanicalKPI]:
        """Calculate KPIs relevant to setup phase"""
        kpis = []
        
        try:
            # Hip hinge angle
            hip_angle = calculate_hip_hinge_angle_p1(frame_data)
            if hip_angle is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P1",
                    kpi_name="Hip Hinge Angle",
                    value=hip_angle,
                    unit="degrees",
                    ideal_range=(25.0, 35.0),
                    notes="Real-time setup analysis"
                ))
            
            # Knee flexion
            lead_knee_flex = calculate_knee_flexion_p1(frame_data, "lead")
            if lead_knee_flex is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P1",
                    kpi_name="Lead Knee Flexion",
                    value=lead_knee_flex,
                    unit="degrees",
                    ideal_range=(15.0, 25.0),
                    notes="Real-time setup analysis"
                ))
            
            trail_knee_flex = calculate_knee_flexion_p1(frame_data, "trail")
            if trail_knee_flex is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P1",
                    kpi_name="Trail Knee Flexion",
                    value=trail_knee_flex,
                    unit="degrees",
                    ideal_range=(15.0, 25.0),
                    notes="Real-time setup analysis"
                ))
            
            # Weight distribution (for irons)
            weight_dist = calculate_weight_distribution_p1_irons(frame_data)
            if weight_dist is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P1",
                    kpi_name="Weight Distribution",
                    value=weight_dist,
                    unit="ratio",
                    ideal_range=(0.45, 0.55),
                    notes="Real-time setup analysis"
                ))
                
        except Exception as e:
            pass
        
        return kpis
    
    def _calculate_backswing_kpis(self, frame_data: FramePoseData) -> List[BiomechanicalKPI]:
        """Calculate KPIs relevant to backswing phase"""
        kpis = []
        
        try:
            # Shoulder turn
            shoulder_turn = calculate_shoulder_turn_p4(frame_data)
            if shoulder_turn is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P4",
                    kpi_name="Shoulder Turn",
                    value=shoulder_turn,
                    unit="degrees",
                    ideal_range=(85.0, 105.0),
                    notes="Real-time backswing analysis"
                ))
            
            # Wrist angle
            wrist_angle = calculate_wrist_angle_p4(frame_data)
            if wrist_angle is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P4",
                    kpi_name="Lead Wrist Angle",
                    value=wrist_angle,
                    unit="degrees",
                    ideal_range=(-5.0, 5.0),
                    notes="Real-time backswing analysis"
                ))
            
            # Hip sway (using frames if available)
            if len(self.frame_buffer) >= 2:
                frames_list = [self.frame_buffer[-2], frame_data]
                hip_sway = calculate_hip_sway_backswing(frames_list)
                if hip_sway is not None:
                    kpis.append(BiomechanicalKPI(
                        p_position="P1-P4",
                        kpi_name="Hip Sway",
                        value=hip_sway,
                        unit="meters",
                        ideal_range=(0.0, 0.05),
                        notes="Real-time backswing analysis"
                    ))
            
        except Exception as e:
            pass
        
        # Store frame for future calculations
        self.frame_buffer.append(frame_data)
        
        return kpis
    
    def _calculate_impact_kpis(self, frame_data: FramePoseData) -> List[BiomechanicalKPI]:
        """Calculate KPIs relevant to impact phase"""
        kpis = []
        
        try:
            # For impact, we'd typically need more sophisticated calculations
            # For now, we'll use basic posture measurements
            
            # Spine angle at impact
            spine_angle = calculate_spine_angle_p4(frame_data)
            if spine_angle is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="P7",  # Impact position
                    kpi_name="Spine Angle",
                    value=spine_angle,
                    unit="degrees",
                    ideal_range=(25.0, 35.0),
                    notes="Real-time impact analysis"
                ))
                
        except Exception as e:
            pass
        
        return kpis
    
    def _calculate_posture_kpis(self, frame_data: FramePoseData) -> List[BiomechanicalKPI]:
        """Calculate basic posture KPIs for any phase"""
        kpis = []
        
        try:
            # Basic spine angle
            spine_angle = calculate_spine_angle_p4(frame_data)
            if spine_angle is not None:
                kpis.append(BiomechanicalKPI(
                    p_position="Real-time",
                    kpi_name="Spine Angle",
                    value=spine_angle,
                    unit="degrees",
                    ideal_range=(15.0, 45.0),
                    notes="Real-time posture check"
                ))
                
        except Exception as e:
            pass
        
        return kpis

class AdaptiveFaultDetector:
    """Dynamic fault detection with context awareness"""
    
    def __init__(self):
        self.fault_history = deque(maxlen=20)
        self.severity_threshold = 5.0  # Lower threshold for real-time detection
    
    def detect_faults(self, kpis: List[BiomechanicalKPI], 
                     swing_phase: SwingPhase) -> List[DetectedFault]:
        """Detect faults with adaptive thresholds based on swing phase"""
        detected_faults = []
        
        if not kpis:
            return detected_faults
        
        # Create minimal swing input for fault detection
        dummy_swing_input = {
            "session_id": "real_time",
            "user_id": "streaming",
            "club_used": "Unknown",
            "frames": [{}],  # Minimal frame data
            "p_system_classification": [],
            "video_fps": 30.0
        }
        
        try:
            # Use existing fault detection with filtered KPIs
            faults = check_swing_faults(dummy_swing_input, kpis)
            
            # Filter faults based on current swing phase and severity
            for fault in faults:
                fault_severity = fault.get('severity', 0)
                fault_phases = fault.get('p_positions_implicated', [])
                
                # Check if fault is relevant to current phase
                if self._is_fault_relevant_to_phase(fault_phases, swing_phase):
                    # Adjust severity for real-time context
                    adjusted_severity = self._adjust_severity_for_realtime(fault_severity, swing_phase)
                    
                    if adjusted_severity >= self.severity_threshold:
                        fault_copy = fault.copy()
                        fault_copy['severity'] = adjusted_severity
                        fault_copy['real_time_detection'] = True
                        detected_faults.append(fault_copy)
            
            # Store fault history for trend analysis
            self.fault_history.append({
                'timestamp': time.time(),
                'faults': detected_faults,
                'phase': swing_phase
            })
            
        except Exception as e:
            pass  # Don't fail on fault detection errors
        
        return detected_faults
    
    def _is_fault_relevant_to_phase(self, fault_phases: List[str], 
                                   current_phase: SwingPhase) -> bool:
        """Check if fault is relevant to current swing phase"""
        if not fault_phases:
            return True  # Generic fault
        
        # Map swing phases to P-positions
        phase_mapping = {
            SwingPhase.SETUP: ["P1"],
            SwingPhase.TAKEAWAY: ["P1", "P2"],
            SwingPhase.BACKSWING: ["P2", "P3", "P4"],
            SwingPhase.TOP_OF_SWING: ["P4"],
            SwingPhase.DOWNSWING: ["P5", "P6"],
            SwingPhase.IMPACT: ["P6", "P7"],
            SwingPhase.FOLLOW_THROUGH: ["P7", "P8"],
            SwingPhase.FINISH: ["P8", "P9", "P10"]
        }
        
        relevant_positions = phase_mapping.get(current_phase, [])
        
        # Check if any fault position matches current phase
        return any(pos in fault_phases for pos in relevant_positions)
    
    def _adjust_severity_for_realtime(self, original_severity: float, 
                                    swing_phase: SwingPhase) -> float:
        """Adjust fault severity for real-time context"""
        # Critical phases where faults are more important
        critical_phases = [SwingPhase.SETUP, SwingPhase.TOP_OF_SWING, SwingPhase.IMPACT]
        
        if swing_phase in critical_phases:
            return original_severity * 1.2  # Increase severity
        else:
            return original_severity * 0.9  # Slightly decrease for non-critical phases

class LiveAnalysisEngine:
    """Main engine for real-time golf swing analysis"""
    
    def __init__(self):
        self.phase_detector = SwingPhaseDetector()
        self.kpi_calculator = StreamingKPICalculator()
        self.fault_detector = AdaptiveFaultDetector()
        self.performance_stats = {
            'frames_analyzed': 0,
            'total_analysis_time': 0.0,
            'average_latency_ms': 0.0
        }
    
    async def analyze_frame(self, frame_data: Any, 
                           session_context: Dict[str, Any],
                           config: Any) -> Optional[FrameAnalysisResult]:
        """Analyze single frame for real-time feedback"""
        start_time = time.time()
        
        try:
            # 1. Detect swing phase
            swing_phase, phase_confidence = self.phase_detector.detect_phase(frame_data)
            
            # 2. Assess frame quality
            quality_score = self._assess_frame_quality(frame_data)
            
            # Skip analysis if frame quality is too low
            if quality_score < 0.3:
                return FrameAnalysisResult(
                    frame_index=frame_data.frame_index,
                    timestamp=frame_data.timestamp,
                    swing_phase=swing_phase,
                    frame_data=frame_data,
                    phase_confidence=phase_confidence,
                    quality_score=quality_score,
                    analysis_latency_ms=(time.time() - start_time) * 1000
                )
            
            # 3. Calculate KPIs for current phase
            kpis = []
            if config.enable_real_time_kpis:
                kpis = self.kpi_calculator.calculate_kpis_for_frame(frame_data, swing_phase)
            
            # 4. Detect faults
            detected_faults = []
            if kpis:
                detected_faults = self.fault_detector.detect_faults(kpis, swing_phase)
            
            # 5. Create analysis result
            analysis_latency = (time.time() - start_time) * 1000
            
            result = FrameAnalysisResult(
                frame_index=frame_data.frame_index,
                timestamp=frame_data.timestamp,
                swing_phase=swing_phase,
                frame_data=frame_data,
                kpis=kpis,
                detected_faults=detected_faults,
                phase_confidence=phase_confidence,
                analysis_latency_ms=analysis_latency,
                quality_score=quality_score
            )
            
            # 6. Update performance statistics
            self._update_performance_stats(analysis_latency)
            
            return result
            
        except Exception as e:
            # Return basic result on error
            return FrameAnalysisResult(
                frame_index=frame_data.frame_index,
                timestamp=frame_data.timestamp,
                swing_phase=SwingPhase.UNKNOWN,
                frame_data=frame_data,
                analysis_latency_ms=(time.time() - start_time) * 1000
            )
    
    def _assess_frame_quality(self, frame_data: Any) -> float:
        """Assess quality of pose data in frame"""
        keypoints = frame_data.keypoints
        
        if not keypoints:
            return 0.0
        
        # Check for essential keypoints
        essential_keypoints = [
            "left_shoulder", "right_shoulder",
            "left_hip", "right_hip",
            "left_knee", "right_knee"
        ]
        
        visibility_scores = []
        position_quality = []
        
        for kp_name in essential_keypoints:
            if kp_name in keypoints:
                kp = keypoints[kp_name]
                
                # Check visibility/confidence
                visibility = kp.get("visibility", 0.0)
                visibility_scores.append(visibility)
                
                # Check for reasonable position values
                x, y, z = kp.get("x", 0), kp.get("y", 0), kp.get("z", 0)
                if abs(x) < 10 and abs(y) < 10 and abs(z) < 10:  # Reasonable bounds
                    position_quality.append(1.0)
                else:
                    position_quality.append(0.0)
            else:
                visibility_scores.append(0.0)
                position_quality.append(0.0)
        
        if not visibility_scores:
            return 0.0
        
        # Calculate overall quality score
        avg_visibility = sum(visibility_scores) / len(visibility_scores)
        avg_position_quality = sum(position_quality) / len(position_quality)
        
        # Weighted combination
        quality_score = (avg_visibility * 0.7) + (avg_position_quality * 0.3)
        
        return min(1.0, max(0.0, quality_score))
    
    def _update_performance_stats(self, analysis_latency_ms: float):
        """Update performance statistics"""
        self.performance_stats['frames_analyzed'] += 1
        self.performance_stats['total_analysis_time'] += analysis_latency_ms
        
        # Calculate running average
        frames = self.performance_stats['frames_analyzed']
        total_time = self.performance_stats['total_analysis_time']
        self.performance_stats['average_latency_ms'] = total_time / frames
    
    def get_performance_stats(self) -> Dict[str, Any]:
        """Get current performance statistics"""
        return self.performance_stats.copy()
    
    def reset_stats(self):
        """Reset performance statistics"""
        self.performance_stats = {
            'frames_analyzed': 0,
            'total_analysis_time': 0.0,
            'average_latency_ms': 0.0
        }